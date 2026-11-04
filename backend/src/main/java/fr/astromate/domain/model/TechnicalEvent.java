package fr.astromate.domain.model;

import java.time.Instant;

public record TechnicalEvent(
        String metricKey, String systemId, String level,
        String message, Instant occurredAt, String category
) {
}
