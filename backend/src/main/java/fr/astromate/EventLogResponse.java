package fr.astromate;

import java.util.List;

public record EventLogResponse(
        List<SystemDetailResponse.Event> items,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
