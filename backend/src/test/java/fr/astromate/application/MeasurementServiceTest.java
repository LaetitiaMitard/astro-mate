package fr.astromate.application;

import fr.astromate.application.port.MeasurementChanges;
import fr.astromate.application.port.MeasurementRepository;
import fr.astromate.application.port.Transactions;
import fr.astromate.application.service.MeasurementService;
import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.model.TechnicalEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MeasurementServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private final MeasurementRepository repository = mock(MeasurementRepository.class);
    private final MeasurementChanges changes = mock(MeasurementChanges.class);
    private final Transactions transactions = mock(Transactions.class);
    private MeasurementService service;

    @BeforeEach
    void setUp() {
        doAnswer(call -> {
            call.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactions).execute(any(Runnable.class));
        when(repository.findMeasurement("oxygen")).thenReturn(Optional.of(new MeasurementSnapshot(
                "oxygen", "life-support", "oxygen-level", "Oxygen", 21, "% vol.",
                NOW.minusSeconds(1).toString())));
        when(repository.availability("life-support")).thenReturn("ONLINE");
        service = new MeasurementService(repository, changes, transactions, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsAThresholdAlertUsingPortsWithoutSpringOrSqlite() {
        service.recordMeasurement("oxygen", 18, NOW);

        verify(repository).saveMeasurement("oxygen", 18, NOW);
        verify(repository).createAlert(any(MeasurementSnapshot.class), eq(MetricStatus.WARNING),
                eq("Seuil d’avertissement dépassé pour Oxygen."), eq(NOW));
        verify(repository).updateCompatibilityProjection("oxygen", 18, NOW);
        verify(changes).publish();
    }

    @Test
    void ignoresOfflineTelemetryWithoutPublishing() {
        when(repository.availability("life-support")).thenReturn("OFFLINE");

        service.recordMeasurement("oxygen", 18, NOW);

        verify(repository, never()).saveMeasurement(anyString(), anyDouble(), any());
        verifyNoInteractions(changes);
    }

    @Test
    void deduplicatesInvalidDataEpisodesWithoutOverwritingTheLastValue() {
        when(repository.startInvalidData("oxygen", NOW)).thenReturn(true, false);

        service.recordMeasurement("oxygen", -1, NOW);
        service.recordMeasurement("oxygen", -1, NOW);

        verify(repository, never()).saveMeasurement(anyString(), anyDouble(), any());
        verify(repository, times(1)).appendEvent(argThat(
                event -> event.category().equals("DATA") && event.level().equals("WARNING")));
        verify(changes, times(1)).publish();
    }

    @Test
    void rejectsOlderMeasurementsBeforeWritingAnything() {
        assertThatThrownBy(() -> service.recordMeasurement("oxygen", 21, NOW.minusSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plus récente");
        verify(repository, never()).saveMeasurement(anyString(), anyDouble(), any());
        verify(repository, never()).appendEvent(any(TechnicalEvent.class));
        verifyNoInteractions(changes);
    }
}
