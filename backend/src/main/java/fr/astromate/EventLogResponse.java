package fr.astromate;

import java.util.List;

public record EventLogResponse(
        List<SystemDetailResponse.Event> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
