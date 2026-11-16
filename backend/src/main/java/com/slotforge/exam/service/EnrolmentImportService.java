package com.slotforge.exam.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.csv.Csv;
import com.slotforge.common.csv.ImportReport;
import com.slotforge.common.csv.ImportReport.RowError;
import com.slotforge.student.service.StudentImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Enrols students in a session's papers from a spreadsheet of roll number and subject code. All or nothing. */
@Service
@RequiredArgsConstructor
public class EnrolmentImportService {

    public static final List<String> COLUMNS = List.of("roll_no", "subject_code");

    private final JdbcClient jdbc;
    private final AuditService audit;
    private final ExamSessionService sessions;
    private final ExaminationService examinations;

    @Transactional
    public ImportReport importEnrolments(Long actorId, Long sessionId, MultipartFile file) {
        sessions.requirePlanning(sessionId);
        List<Csv.Row> rows = Csv.parse(StudentImportService.text(file), COLUMNS);

        Map<String, Long> papers = new HashMap<>();
        jdbc.sql("SELECT upper(su.code), x.id FROM examination x JOIN subject su ON su.id = x.subject_id WHERE x.session_id = :session")
                .param("session", sessionId)
                .query((row, index) -> papers.put(row.getString(1), row.getLong(2))).list();
        Set<String> rolls = new HashSet<>();
        rows.forEach(row -> rolls.add(row.get("roll_no").toUpperCase(Locale.ROOT)));
        Map<String, Long> students = new HashMap<>();
        Set<String> inactive = new HashSet<>();
        jdbc.sql("SELECT upper(roll_no), id, active FROM student WHERE upper(roll_no) IN (:rolls)").param("rolls", rolls)
                .query((row, index) -> row.getBoolean(3) ? students.put(row.getString(1), row.getLong(2)) : inactive.add(row.getString(1)))
                .list();

        List<RowError> errors = new ArrayList<>();
        List<long[]> pairs = new ArrayList<>();
        for (Csv.Row row : rows) {
            String roll = row.get("roll_no").toUpperCase(Locale.ROOT);
            String subject = row.get("subject_code").toUpperCase(Locale.ROOT);
            List<String> problems = new ArrayList<>();
            if (inactive.contains(roll)) problems.add("student " + roll + " is deactivated");
            else if (!students.containsKey(roll)) problems.add("no student has roll_no \"" + row.get("roll_no") + "\"");
            if (!papers.containsKey(subject)) problems.add("this session has no paper for subject \"" + row.get("subject_code") + "\"");
            if (problems.isEmpty()) pairs.add(new long[] {papers.get(subject), students.get(roll)});
            else errors.add(new RowError(row.line(), String.join("; ", problems)));
        }
        if (!errors.isEmpty()) return ImportReport.rejected(rows.size(), errors);

        int added = 0;
        Set<Long> touched = new HashSet<>();
        for (long[] pair : pairs) {
            added += jdbc.sql("INSERT INTO examination_enrolment (examination_id, student_id) VALUES (:exam, :student) ON CONFLICT DO NOTHING")
                    .param("exam", pair[0]).param("student", pair[1]).update();
            touched.add(pair[0]);
        }
        touched.forEach(exam -> examinations.refreshStatus(examinations.find(exam)));
        audit.record(actorId, "ENROLMENT_IMPORTED", "exam_session", sessionId, "{\"added\":" + added + "}");
        return ImportReport.accepted(rows.size(), added);
    }
}
