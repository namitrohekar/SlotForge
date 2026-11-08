package com.slotforge.exam;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.Optional;

import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.exam.controller.EnrolmentController;
import com.slotforge.exam.controller.ExamSessionController;
import com.slotforge.exam.controller.ExaminationController;
import com.slotforge.exam.controller.TimeSlotController;
import com.slotforge.exam.service.EnrolmentService;
import com.slotforge.exam.service.ExamSessionService;
import com.slotforge.exam.service.ExaminationService;
import com.slotforge.exam.service.TimeSlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ExamSessionController.class, TimeSlotController.class, ExaminationController.class, EnrolmentController.class})
@Import(SecurityConfig.class)
class ExamSetupApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ExamSessionService sessions;

    @MockitoBean
    private TimeSlotService slots;

    @MockitoBean
    private ExaminationService examinations;

    @MockitoBean
    private EnrolmentService enrolments;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void studentsCannotSeeSessionsBeingPlanned() throws Exception {
        mvc.perform(get("/api/exam-sessions").with(authentication(signedIn(6L, Role.STUDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyCannotCreateSessions() throws Exception {
        mvc.perform(post("/api/exam-sessions").with(csrf()).with(authentication(signedIn(4L, Role.FACULTY)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"S\",\"startDate\":\"2026-12-07\",\"endDate\":\"2026-12-19\",\"maxExamsPerStudentPerDay\":1,\"heavyGapDays\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyCannotReadWhoIsEnrolled() throws Exception {
        mvc.perform(get("/api/examinations/1/enrolments").with(authentication(signedIn(4L, Role.FACULTY))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aHeadOfDepartmentCannotChangeEnrolment() throws Exception {
        mvc.perform(post("/api/examinations/1/enrolments").with(csrf()).with(authentication(signedIn(3L, Role.HOD)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"studentIds\":[1]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionFieldsAreValidated() throws Exception {
        mvc.perform(post("/api/exam-sessions").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"startDate\":\"2026-12-07\",\"maxExamsPerStudentPerDay\":0,\"heavyGapDays\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    void slotTemplatesAreValidated() throws Exception {
        mvc.perform(post("/api/exam-sessions/1/time-slots/generate").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2026-12-07\",\"to\":\"2026-12-19\",\"weekdays\":[8],\"times\":[{\"start\":\"10:00\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void aPaperMustBeASensibleLength() throws Exception {
        mvc.perform(post("/api/exam-sessions/1/examinations/from-curriculum").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"batchIds\":[],\"durationMinutes\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
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
