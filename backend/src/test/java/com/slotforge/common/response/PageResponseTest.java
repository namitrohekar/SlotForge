package com.slotforge.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void copiesPagingDetails() {
        var page = new PageImpl<>(List.of("c", "d"), PageRequest.of(1, 2), 5);

        var response = PageResponse.from(page);

        assertThat(response.items()).containsExactly("c", "d");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalItems()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void emptyPageHasNoItems() {
        var response = PageResponse.from(new PageImpl<String>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(response.items()).isEmpty();
        assertThat(response.totalItems()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}
