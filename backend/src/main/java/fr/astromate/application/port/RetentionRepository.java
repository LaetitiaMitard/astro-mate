package fr.astromate.application.port;

import java.time.Instant;

public interface RetentionRepository {
    void deleteExpired(Instant measurementCutoff, Instant eventCutoff);
}
