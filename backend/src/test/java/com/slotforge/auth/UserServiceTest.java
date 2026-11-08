package com.slotforge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.slotforge.audit.service.AuditService;
import com.slotforge.auth.dto.CreateUserRequest;
import com.slotforge.auth.entity.AppUser;
import com.slotforge.auth.entity.Role;
import com.slotforge.auth.entity.UserStatus;
import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.service.UserService;
import com.slotforge.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ACTOR = 1L;

    @Mock
    private AppUserRepository users;

    @Mock
    private AuditService audit;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JdbcClient jdbc;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(users, audit, jdbc);
    }

    @Test
    void nobodyChangesTheirOwnRoles() {
        assertThatThrownBy(() -> service.changeRoles(ACTOR, ACTOR, Set.of(Role.STUDENT)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SELF_ROLE_CHANGE");
    }

    @Test
    void nobodyDeactivatesThemselves() {
        assertThatThrownBy(() -> service.deactivate(ACTOR, ACTOR))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SELF_ROLE_CHANGE");
    }

    @Test
    void theLastAdministratorCannotBeDemoted() {
        AppUser admin = user(2L, Role.ADMIN);
        when(users.findById(2L)).thenReturn(Optional.of(admin));
        when(users.countWithRoleExcept(Role.ADMIN, UserStatus.ACTIVE, 2L)).thenReturn(0L);

        assertThatThrownBy(() -> service.changeRoles(ACTOR, 2L, Set.of(Role.HOD)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("LAST_ADMIN");
        assertThat(admin.getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    void theLastAdministratorCannotBeDeactivated() {
        when(users.findById(2L)).thenReturn(Optional.of(user(2L, Role.ADMIN)));
        when(users.countWithRoleExcept(Role.ADMIN, UserStatus.ACTIVE, 2L)).thenReturn(0L);

        assertThatThrownBy(() -> service.deactivate(ACTOR, 2L))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("LAST_ADMIN");
    }

    @Test
    void anAdministratorCanBeDemotedWhileAnotherRemains() {
        AppUser admin = user(2L, Role.ADMIN);
        when(users.findById(2L)).thenReturn(Optional.of(admin));
        when(users.countWithRoleExcept(Role.ADMIN, UserStatus.ACTIVE, 2L)).thenReturn(1L);

        service.changeRoles(ACTOR, 2L, Set.of(Role.EXAM_COORDINATOR, Role.FACULTY));

        assertThat(admin.getRoles()).containsExactlyInAnyOrder(Role.EXAM_COORDINATOR, Role.FACULTY);
        verify(audit).record(ACTOR, "USER_ROLES_CHANGED", "app_user", 2L,
                "{\"roles\":[\"EXAM_COORDINATOR\",\"FACULTY\"]}");
    }

    @Test
    void changingANonAdministratorNeedsNoAdminCheck() {
        when(users.findById(3L)).thenReturn(Optional.of(user(3L, Role.FACULTY)));

        service.deactivate(ACTOR, 3L);

        verify(users, never()).countWithRoleExcept(any(), any(), any());
    }

    @Test
    void newUsersAreStoredWithALowerCaseEmail() {
        when(users.existsByEmail("new.person@vjti.ac.in")).thenReturn(false);

        var created = service.create(ACTOR, new CreateUserRequest(" New.Person@VJTI.ac.in ", " New Person ", Set.of(Role.HOD)));

        assertThat(created.email()).isEqualTo("new.person@vjti.ac.in");
        assertThat(created.fullName()).isEqualTo("New Person");
        verify(audit).record(eq(ACTOR), eq("USER_CREATED"), eq("app_user"), any(), anyString());
    }

    @Test
    void duplicateEmailIsAClearConflict() {
        when(users.existsByEmail("taken@vjti.ac.in")).thenReturn(true);

        assertThatThrownBy(() -> service.create(ACTOR, new CreateUserRequest("taken@vjti.ac.in", "X", Set.of(Role.HOD))))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("DUPLICATE");
    }

    @Test
    void sortingIsLimitedToKnownColumns() {
        assertThatThrownBy(() -> service.search(null, null, null, PageRequest.of(0, 20, Sort.by("googleSub"))))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("INVALID_SORT");
    }

    @Test
    void aStudentCannotBeCreatedAsAnAdministrator() {
        assertThatThrownBy(() -> service.create(ACTOR,
                new CreateUserRequest("s@vjti.ac.in", "S", Set.of(Role.STUDENT, Role.ADMIN))))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("INVALID_ROLE_COMBINATION");
        verify(users, never()).save(any());
    }

    @Test
    void anHodCannotAlsoBeAStudent() {
        AppUser hod = user(4L, Role.HOD);
        when(users.findById(4L)).thenReturn(Optional.of(hod));

        assertThatThrownBy(() -> service.changeRoles(ACTOR, 4L, Set.of(Role.HOD, Role.STUDENT)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("INVALID_ROLE_COMBINATION");
        assertThat(hod.getRoles()).containsExactly(Role.HOD);
    }

    @Test
    void aDeactivatedAccountCannotBeEdited() {
        AppUser student = user(6L, Role.STUDENT);
        student.setStatus(UserStatus.DEACTIVATED);
        when(users.findById(6L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.rename(ACTOR, 6L, "New name"))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("USER_DEACTIVATED");
        assertThatThrownBy(() -> service.changeRoles(ACTOR, 6L, Set.of(Role.FACULTY)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("USER_DEACTIVATED");
        assertThat(student.getFullName()).isEqualTo("User 6");
        assertThat(student.getRoles()).containsExactly(Role.STUDENT);
    }

    @Test
    void reactivationMakesTheAccountEditableAgain() {
        AppUser student = user(6L, Role.STUDENT);
        student.setStatus(UserStatus.DEACTIVATED);
        when(users.findById(6L)).thenReturn(Optional.of(student));

        service.activate(ACTOR, 6L);
        service.rename(ACTOR, 6L, "New name");

        assertThat(student.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(student.getFullName()).isEqualTo("New name");
        verify(audit).record(ACTOR, "USER_ACTIVATED", "app_user", 6L, null);
    }

    @Test
    void anAccountWithClashingRolesIsNotReactivated() {
        AppUser broken = user(7L, Role.STUDENT, Role.ADMIN);
        broken.setStatus(UserStatus.DEACTIVATED);
        when(users.findById(7L)).thenReturn(Optional.of(broken));

        assertThatThrownBy(() -> service.activate(ACTOR, 7L))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("INVALID_ROLE_COMBINATION");
        assertThat(broken.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
    }

    private static AppUser user(Long id, Role... roles) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user" + id + "@vjti.ac.in");
        user.setFullName("User " + id);
        user.setRoles(EnumSet.of(roles[0], roles));
        return user;
    }
}
