package com.chiaseyeuthuong.common.sort;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SortParamUtils {

    private SortParamUtils() {
    }

    public static int normalizePageNumber(int page) {
        return Math.max(page - 1, 0);
    }

    public static int normalizePageSize(int size, int defaultSize) {
        return size > 0 ? size : defaultSize;
    }

    public static Sort.Direction resolveDirection(String sortDir, Sort.Direction defaultDirection) {
        if (!StringUtils.hasText(sortDir)) {
            return defaultDirection;
        }
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.DESC;
        }
        return defaultDirection;
    }

    public static String resolveSortAlias(String sortBy, Map<String, String> fieldMappings,
                                          Map<String, String> unsafeMappings, String defaultAlias) {
        Map<String, String> supportedMappings = new LinkedHashMap<>();
        supportedMappings.putAll(fieldMappings);
        supportedMappings.putAll(unsafeMappings);

        if (!StringUtils.hasText(sortBy)) {
            return defaultAlias;
        }

        String normalizedSortBy = sortBy.trim();
        return supportedMappings.containsKey(normalizedSortBy) ? normalizedSortBy : defaultAlias;
    }

    public static Sort buildSort(Map<String, String> fieldMappings,
                                 Map<String, String> unsafeMappings,
                                 String sortBy,
                                 String sortDir,
                                 String defaultAlias,
                                 Sort.Direction defaultDirection,
                                 String tieBreakerProperty) {
        String resolvedAlias = resolveSortAlias(sortBy, fieldMappings, unsafeMappings, defaultAlias);
        Sort.Direction resolvedDirection = resolveDirection(sortDir, defaultDirection);

        Sort sort;
        if (unsafeMappings.containsKey(resolvedAlias)) {
            sort = JpaSort.unsafe(resolvedDirection, unsafeMappings.get(resolvedAlias));
        } else {
            String property = fieldMappings.getOrDefault(resolvedAlias, fieldMappings.get(defaultAlias));
            sort = Sort.by(new Sort.Order(resolvedDirection, property));
        }

        if (StringUtils.hasText(tieBreakerProperty)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, tieBreakerProperty));
        }
        return sort;
    }

    public static Pageable buildPageRequest(int page, int size, Sort sort, int defaultSize) {
        return PageRequest.of(normalizePageNumber(page), normalizePageSize(size, defaultSize), sort);
    }
}
