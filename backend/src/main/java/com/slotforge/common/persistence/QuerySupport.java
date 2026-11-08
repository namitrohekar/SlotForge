package com.slotforge.common.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.slotforge.common.exception.ApiException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

/** Shared rules for list endpoints: a sort whitelist and safe "contains" search. */
public final class QuerySupport {

    private QuerySupport() {
    }

    // Unknown sort fields would otherwise reach Hibernate and fail as a 500.
    public static void requireSortable(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SORT", "Cannot sort by " + order.getProperty() + ".");
            }
        }
    }

    /** A lower-case LIKE pattern with % and _ escaped by backslash, so they match literally. */
    public static String containsPattern(String text) {
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** Turns grouped rows of [id, count] into a lookup, skipping the query when there is nothing to count. */
    public static Map<Long, Long> counts(Collection<Long> ids, Function<Collection<Long>, List<Object[]>> query) {
        if (ids.isEmpty()) return Map.of();
        return query.apply(ids).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> ((Number) row[1]).longValue()));
    }
}
