package com.slotforge.student.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.slotforge.student.dto.MyExamsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Scoped to the signed-in account, so a student can never read another student's papers by changing an id. */
@Service
@RequiredArgsConstructor
public class MyExamsService {

    private static final MyExamsResponse NOT_ON_THE_ROSTER = new MyExamsResponse(null, null, null, null, List.of());

    private final JdbcClient jdbc;

    /** Published timetables only: a draft may still change, so nobody is told about it yet. */
    @Transactional(readOnly = true)
    public MyExamsResponse exams(Long userId) {
        return jdbc.sql("""
                SELECT st.id, st.roll_no, st.full_name, c.name, b.name
                FROM student st
                JOIN batch b ON b.id = st.batch_id
                JOIN curriculum cu ON cu.id = b.curriculum_id
                JOIN course c ON c.id = cu.course_id
                WHERE st.user_id = :user""")
                .param("user", userId)
                .query((row, index) -> new MyExamsResponse(row.getString(2), row.getString(3), row.getString(4), row.getString(5),
                        papers(row.getLong(1))))
                .optional()
                .orElse(NOT_ON_THE_ROSTER);
    }

    private List<MyExamsResponse.Exam> papers(long studentId) {
        return jdbc.sql("""
                SELECT s.name, t.slot_date, t.start_time, t.end_time, su.code, su.name, h.code, h.name, h.building
                FROM student_hall_assignment seat
                JOIN exam_schedule es ON es.id = seat.exam_schedule_id
                JOIN schedule_version v ON v.id = es.version_id AND v.status = 'PUBLISHED'
                JOIN exam_session s ON s.id = v.session_id AND s.status <> 'CANCELLED'
                JOIN time_slot t ON t.id = es.time_slot_id
                JOIN examination x ON x.id = es.examination_id
                JOIN subject su ON su.id = x.subject_id
                JOIN hall_allocation a ON a.id = seat.hall_allocation_id
                JOIN exam_hall h ON h.id = a.hall_id
                WHERE seat.student_id = :student
                ORDER BY t.slot_date, t.start_time""")
                .param("student", studentId)
                .query((row, index) -> new MyExamsResponse.Exam(row.getString(1), row.getObject(2, LocalDate.class),
                        row.getObject(3, LocalTime.class), row.getObject(4, LocalTime.class), row.getString(5), row.getString(6),
                        row.getString(7), row.getString(8), row.getString(9)))
                .list();
    }
}
