package com.krushikranti.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Pagination and sorting utilities.
 */
public final class PaginationUtil {

    private PaginationUtil() {
    }

    public static PageRequest buildPageRequest(int page, int size, String sortBy, String sortDir) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(Math.max(page, 0), Math.min(size, 100), sort);
    }

    public static <T> boolean isLastPage(Page<T> page) {
        return page.getNumber() >= page.getTotalPages() - 1;
    }
}
