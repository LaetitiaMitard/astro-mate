package fr.astromate.infrastructure.web;

import java.util.List;

public record EventLogResponse(
        List<SystemDetailResponse.Event> items,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
