package com.slotforge.planning;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.planning.controller.PlanningController;
import com.slotforge.planning.service.PlanningService;
import com.slotforge.schedule.controller.ScheduleVersionController;
import com.slotforge.schedule.service.ScheduleQueryService;
import com.slotforge.schedule.service.ScheduleWorkflowService;
import com.slotforge.schedule.validation.ScheduleValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PlanningController.class, ScheduleVersionController.class})
@Import(SecurityConfig.class)
class ScheduleApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PlanningService planning;

    @MockitoBean
    private ScheduleQueryService schedules;

    @MockitoBean
    private ScheduleWorkflowService workflow;

    @MockitoBean
    private ScheduleValidator validator;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void aHeadOfDepartmentCanReviewDraftsButNotGenerate() throws Exception {
        when(schedules.versions(1L)).thenReturn(List.of());

        mvc.perform(get("/api/exam-sessions/1/schedule-versions").with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/exam-sessions/1/planning/runs").with(csrf()).with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void seatingListsAreForThePeopleWhoMaySeeTheStudentRoster() throws Exception {
        when(schedules.seating(1L)).thenReturn(List.of());

        mvc.perform(get("/api/schedule-versions/1/seating").with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))).andExpect(status().isOk());
        mvc.perform(get("/api/schedule-versions/1/seating").with(authentication(signedIn(4L, Role.FACULTY)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/schedule-versions/1/seating").with(authentication(signedIn(6L, Role.STUDENT)))).andExpect(status().isForbidden());
    }

    @Test
    void preflightAndTheBaselineAreForThePeopleWhoPlanAndReview() throws Exception {
        when(planning.preflight(1L)).thenReturn(List.of());

        mvc.perform(get("/api/exam-sessions/1/planning/preflight").with(authentication(signedIn(3L, Role.HOD)))).andExpect(status().isOk());
        mvc.perform(get("/api/exam-sessions/1/planning/preflight").with(authentication(signedIn(4L, Role.FACULTY)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/exam-sessions/1/planning/baseline").with(authentication(signedIn(6L, Role.STUDENT)))).andExpect(status().isForbidden());
    }

    @Test
    void onlyApproversHavePastDecisions() throws Exception {
        when(schedules.decidedBy(3L)).thenReturn(List.of());

        mvc.perform(get("/api/schedule-versions/decided-by-me").with(authentication(signedIn(3L, Role.HOD)))).andExpect(status().isOk());
        mvc.perform(get("/api/schedule-versions/decided-by-me").with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))).andExpect(status().isForbidden());
    }

    @Test
    void facultyCannotSeeDrafts() throws Exception {
        mvc.perform(get("/api/schedule-versions/1/entries").with(authentication(signedIn(4L, Role.FACULTY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void timetableProblemsAreForThePeopleWhoReviewDrafts() throws Exception {
        when(validator.problems(1L)).thenReturn(List.of("MA101 has 1 student without a seat."));

        mvc.perform(get("/api/schedule-versions/1/violations").with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("MA101 has 1 student without a seat."));
        mvc.perform(get("/api/schedule-versions/1/violations").with(authentication(signedIn(4L, Role.FACULTY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentsCannotSeeDrafts() throws Exception {
        mvc.perform(get("/api/exam-sessions/1/schedule-versions").with(authentication(signedIn(6L, Role.STUDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyCoordinatorsDeleteDrafts() throws Exception {
        mvc.perform(delete("/api/schedule-versions/1").with(csrf()).with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyCoordinatorsSendForApproval() throws Exception {
        mvc.perform(post("/api/schedule-versions/1/submit").with(csrf()).with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/schedule-versions/1/submit").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR))))
                .andExpect(status().isNoContent());
    }

    @Test
    void onlyAHeadOfDepartmentApproves() throws Exception {
        for (Role role : List.of(Role.ADMIN, Role.EXAM_COORDINATOR, Role.FACULTY)) {
            mvc.perform(post("/api/schedule-versions/1/review").with(csrf()).with(authentication(signedIn(1L, role))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/schedule-versions/1/review").with(csrf()).with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isNoContent());
    }

    @Test
    void theApprovalQueueIsForHeadsOfDepartment() throws Exception {
        mvc.perform(get("/api/schedule-versions/awaiting-approval").with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/schedule-versions/awaiting-approval").with(authentication(signedIn(2L, Role.EXAM_COORDINATOR))))
                .andExpect(status().isForbidden());
    }

    @Test
    void dutiesAtRiskAreForCoordinators() throws Exception {
        mvc.perform(get("/api/schedule-versions/duties-at-risk").with(authentication(signedIn(2L, Role.EXAM_COORDINATOR))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/schedule-versions/duties-at-risk").with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void theApproverCannotAlsoPublish() throws Exception {
        mvc.perform(post("/api/schedule-versions/1/publish").with(csrf()).with(authentication(signedIn(3L, Role.HOD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendingBackNeedsAReason() throws Exception {
        mvc.perform(post("/api/schedule-versions/1/return").with(csrf()).with(authentication(signedIn(3L, Role.HOD)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("comment"));
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
