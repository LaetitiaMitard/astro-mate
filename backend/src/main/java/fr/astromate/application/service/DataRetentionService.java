package fr.astromate.application.service;

import fr.astromate.application.port.RetentionRepository;
import fr.astromate.application.port.Transactions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class DataRetentionService {
    private static final Duration MEASUREMENT_RETENTION = Duration.ofHours(6);
    private static final Duration EVENT_RETENTION = Duration.ofDays(7);
    private final RetentionRepository repository;
    private final Transactions transactions;
    private final Clock clock;

    public DataRetentionService(RetentionRepository repository, Transactions transactions, Clock clock) {
        this.repository = repository;
        this.transactions = transactions;
        this.clock = clock;
    }

    public void applyRetention() {
        applyRetentionAt(clock.instant());
    }

    public void applyRetentionAt(Instant now) {
        transactions.execute(() -> repository.deleteExpired(
                now.minus(MEASUREMENT_RETENTION), now.minus(EVENT_RETENTION)));
    }
}
