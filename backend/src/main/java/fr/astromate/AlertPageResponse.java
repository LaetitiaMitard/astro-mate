package fr.astromate;

import java.util.List;

public record AlertPageResponse(
        List<DashboardResponse.Alert> items,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
