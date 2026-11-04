package fr.astromate.application.model;

public record SystemSummaryView(
        String id,
        String name,
        String description,
        String status,
        String availability,
        String updatedAt
) {
}
