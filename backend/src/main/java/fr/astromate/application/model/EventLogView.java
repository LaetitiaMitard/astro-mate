package fr.astromate.application.model;

import java.util.List;

public record EventLogView(
        List<SystemDetailView.Event> items,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
