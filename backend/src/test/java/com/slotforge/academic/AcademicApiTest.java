package com.slotforge.academic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.slotforge.academic.controller.BatchController;
import com.slotforge.academic.controller.CourseController;
import com.slotforge.academic.controller.CurriculumController;
import com.slotforge.academic.controller.DepartmentController;
import com.slotforge.academic.dto.CurriculumSubjectRequest;
import com.slotforge.academic.dto.DepartmentResponse;
import com.slotforge.academic.service.BatchService;
import com.slotforge.academic.service.CourseService;
import com.slotforge.academic.service.CurriculumService;
import com.slotforge.academic.service.DepartmentService;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.AppPrincipal;
import com.slotforge.auth.security.SecurityConfig;
import com.slotforge.common.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({DepartmentController.class, CourseController.class, CurriculumController.class, BatchController.class})
@Import(SecurityConfig.class)
class AcademicApiTest {

    private static final String DEPARTMENT = "{\"code\":\"CEIT\",\"name\":\"Computer Engineering & Information Technology\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DepartmentService departments;

    @MockitoBean
    private CourseService courses;

    @MockitoBean
    private CurriculumService curricula;

    @MockitoBean
    private BatchService batches;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void teachingStaffCanReadDepartments() throws Exception {
        when(departments.search(eq(null), any())).thenReturn(new PageResponse<>(
                List.of(new DepartmentResponse(1L, "CEIT", "Computer Engineering & Information Technology", 3, 12)), 0, 20, 1, 1));

        mvc.perform(get("/api/departments").with(authentication(signedIn(4L, Role.HOD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].courseCount").value(3));
    }

    @Test
    void studentsCannotSeeTheAcademicStructure() throws Exception {
        mvc.perform(get("/api/courses").with(authentication(signedIn(6L, Role.STUDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyCannotChangeIt() throws Exception {
        mvc.perform(post("/api/departments").with(csrf()).with(authentication(signedIn(5L, Role.FACULTY)))
                        .contentType(MediaType.APPLICATION_JSON).content(DEPARTMENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdministratorCanChangeItAsACoordinator() throws Exception {
        when(departments.create(eq(1L), any()))
                .thenReturn(new DepartmentResponse(9L, "CEIT", "Computer Engineering & Information Technology", 0, 0));

        mvc.perform(post("/api/departments").with(csrf()).with(authentication(signedIn(1L, Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON).content(DEPARTMENT))
                .andExpect(status().isCreated());
    }

    @Test
    void codesAndSemesterCountsAreValidated() throws Exception {
        mvc.perform(post("/api/courses").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":1,\"code\":\"BT CE!\",\"name\":\"B.Tech\",\"totalSemesters\":13}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void everyPlacementIsValidated() throws Exception {
        mvc.perform(put("/api/curricula/1/subjects").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjects\":[{\"subjectId\":5,\"semesterNo\":0},{\"semesterNo\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'subjects[0].semesterNo')].message")
                        .value("must be greater than or equal to 1"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'subjects[1].subjectId')].message")
                        .value("must not be null"));
    }

    @Test
    void aMissingSubjectIsNamed() throws Exception {
        mvc.perform(put("/api/curricula/1/subjects").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"subjects\":[{\"semesterNo\":2,\"elective\":true}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("subjects[0].subjectId"));
    }

    @Test
    void aSemesterThatIsNotANumberIsNamed() throws Exception {
        mvc.perform(put("/api/curricula/1/subjects").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjects\":[{\"subjectId\":5,\"semesterNo\":\"three\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("subjects[0].semesterNo"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be a whole number"));
    }

    @Test
    void electiveMayBeLeftOutAndMeansCore() throws Exception {
        when(curricula.replaceSubjects(eq(2L), eq(1L), any())).thenReturn(List.of());

        mvc.perform(put("/api/curricula/1/subjects").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"subjects\":[{\"subjectId\":5,\"semesterNo\":1}]}"))
                .andExpect(status().isOk());
        verify(curricula).replaceSubjects(2L, 1L, List.of(new CurriculumSubjectRequest(5L, 1, false)));
    }

    @Test
    void batchFieldsAreValidated() throws Exception {
        mvc.perform(post("/api/batches").with(csrf()).with(authentication(signedIn(2L, Role.EXAM_COORDINATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curriculumId\":1,\"name\":\"\",\"admissionYear\":1999,\"currentSemester\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
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
