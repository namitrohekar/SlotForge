package com.slotforge.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SinglePageAppFilterTest {

    @Test
    void pageAddressesGoToTheApp() {
        assertThat(SinglePageAppFilter.isAppPage("GET", "/login")).isTrue();
        assertThat(SinglePageAppFilter.isAppPage("GET", "/sessions/13/timetable")).isTrue();
        assertThat(SinglePageAppFilter.isAppPage("GET", "/print/sessions/13/versions/26/seating")).isTrue();
    }

    @Test
    void everythingElsePassesThrough() {
        assertThat(SinglePageAppFilter.isAppPage("GET", "/")).as("served as the welcome page").isFalse();
        assertThat(SinglePageAppFilter.isAppPage("GET", "/api/auth/me")).isFalse();
        assertThat(SinglePageAppFilter.isAppPage("GET", "/api")).isFalse();
        assertThat(SinglePageAppFilter.isAppPage("GET", "/oauth2/authorization/google")).isFalse();
        assertThat(SinglePageAppFilter.isAppPage("GET", "/login/oauth2/code/google")).isFalse();
        assertThat(SinglePageAppFilter.isAppPage("GET", "/assets/index-abc.js")).isFalse();
        assertThat(SinglePageAppFilter.isAppPage("GET", "/favicon.svg")).isFalse();
        assertThat(SinglePageAppFilter.isAppPage("POST", "/login")).isFalse();
    }
}
