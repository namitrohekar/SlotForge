package com.slotforge.exam.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Who may approve a session's timetable, worked out from who is sitting it, so nobody can choose their own approver.
 * Students of one department: that department's head. Students of several: the Controller of Examinations.
 * A department counts only with at least one student in ten, so a backlog student does not change who approves.
 */
@Service
@RequiredArgsConstructor
public class ApprovalAuthority {

    /** userIds is empty and names is null while the post is vacant. */
    public record Approver(String post, String names, Set<Long> userIds) {

        public boolean vacant() {
            return userIds.isEmpty();
        }
    }

    private record Department(String code, Long headId, String headName, int students) {
    }

    private final JdbcClient jdbc;

    public Approver of(long sessionId) {
        List<Department> departments = jdbc.sql("""
                SELECT d.code, h.id, h.full_name, count(DISTINCT st.id)
                FROM examination x
                JOIN examination_enrolment e ON e.examination_id = x.id
                JOIN student st ON st.id = e.student_id AND st.active
                JOIN batch b ON b.id = st.batch_id
                JOIN curriculum cu ON cu.id = b.curriculum_id
                JOIN course c ON c.id = cu.course_id
                JOIN department d ON d.id = c.department_id
                LEFT JOIN app_user h ON h.id = d.head_user_id AND h.status = 'ACTIVE'
                WHERE x.session_id = :session AND x.status = 'READY'
                GROUP BY d.code, h.id, h.full_name""")
                .param("session", sessionId)
                .query((row, index) -> new Department(row.getString(1), row.getObject(2, Long.class), row.getString(3), row.getInt(4)))
                .list();
        int total = departments.stream().mapToInt(Department::students).sum();
        List<Department> owners = departments.stream().filter(department -> department.students() * 10 >= total).toList();
        if (owners.size() == 1) {
            Department only = owners.get(0);
            String post = "Head of " + only.code();
            return only.headId() == null ? new Approver(post, null, Set.of()) : new Approver(post, only.headName(), Set.of(only.headId()));
        }
        return controller();
    }

    private Approver controller() {
        Map<Long, String> controllers = new LinkedHashMap<>();
        jdbc.sql("""
                SELECT u.id, u.full_name
                FROM app_user u JOIN user_role r ON r.user_id = u.id
                WHERE r.role = 'CONTROLLER' AND u.status = 'ACTIVE'
                ORDER BY u.full_name""")
                .query((row, index) -> controllers.put(row.getLong(1), row.getString(2)))
                .list();
        return new Approver("Controller of Examinations",
                controllers.isEmpty() ? null : String.join(" or ", controllers.values()), controllers.keySet());
    }
}
