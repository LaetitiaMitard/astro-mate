package fr.astromate.application.model;

import java.util.List;

public record AlertPageView(
        List<DashboardView.Alert> items,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
