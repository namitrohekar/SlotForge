package com.slotforge.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

/** The JSON shape of every paged list. Spring's own Page JSON is not a stable contract. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
