package fr.astromate.infrastructure.web;

public record SystemSummaryResponse(
        String id,
        String name,
        String description,
        String status,
        String availability,
        String updatedAt
) {
}
