package com.slotforge.schedule;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.schedule.controller.PublishedTimetableController;
import com.slotforge.schedule.dto.PublishedTimetableResponse;
import com.slotforge.schedule.service.ScheduleQueryService;
import com.slotforge.student.controller.MyExamsController;
import com.slotforge.student.dto.MyExamsResponse;
import com.slotforge.student.service.MyExamsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PublishedTimetableController.class, MyExamsController.class})
@Import(SecurityConfig.class)
class NoticeApiTest {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 7);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ScheduleQueryService schedules;

    @MockitoBean
    private MyExamsService exams;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void everySignedInRoleCanReadThePublishedNotice() throws Exception {
        when(schedules.published()).thenReturn(List.of(new PublishedTimetableResponse(3, "End Semester", DAY, DAY.plusDays(11), null,
                List.of(new PublishedTimetableResponse.Paper(DAY, TEN, TEN.plusHours(3), "MA101", "Mathematics", List.of("AUD-1", "EE-LH1"))))));

        for (Role role : Role.values()) {
            mvc.perform(get("/api/published-timetables").with(authentication(signedIn(10L + role.ordinal(), role))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].papers[0].halls.length()").value(2));
        }
    }

    @Test
    void theNoticeStillNeedsASignIn() throws Exception {
        mvc.perform(get("/api/published-timetables")).andExpect(status().isUnauthorized());
    }

    @Test
    void aStudentGetsTheirOwnPapersAndNobodyElseHasAny() throws Exception {
        when(exams.exams(6L)).thenReturn(new MyExamsResponse("26BCE001", "A Student", "B.Tech Computer Engineering", "2026-30",
                List.of(new MyExamsResponse.Exam("End Semester", DAY, TEN, TEN.plusHours(3), "MA101", "Mathematics", "AUD-1", "Auditorium", "Main Building"))));

        mvc.perform(get("/api/me/exams").with(authentication(signedIn(6L, Role.STUDENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollNo").value("26BCE001"))
                .andExpect(jsonPath("$.exams[0].hallCode").value("AUD-1"));
        mvc.perform(get("/api/me/exams").with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/me/exams").with(authentication(signedIn(5L, Role.INVIGILATOR)))).andExpect(status().isForbidden());
    }

    private Authentication signedIn(Long id, Role role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user" + id + "@vjti.ac.in");
        user.setFullName("User " + id);
        user.setRoles(EnumSet.of(role));
        when(users.findById(id)).thenReturn(Optional.of(user));
        return AppPrincipal.authenticationFor(user);
    }
}
