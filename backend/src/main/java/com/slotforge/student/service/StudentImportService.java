package com.slotforge.student.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.slotforge.audit.service.AuditService;
import com.slotforge.common.csv.Csv;
import com.slotforge.common.csv.ImportReport;
import com.slotforge.common.csv.ImportReport.RowError;
import com.slotforge.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Adds a roster from a spreadsheet. Every row is checked first, and nothing is saved unless every row is good. */
@Service
@RequiredArgsConstructor
public class StudentImportService {

    public static final List<String> COLUMNS = List.of("roll_no", "full_name", "email", "programme", "batch");

    private static final Pattern ROLL = Pattern.compile("[A-Za-z0-9][A-Za-z0-9/-]{0,29}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private record NewStudent(long batchId, String rollNo, String fullName, String email) {
    }

    private final JdbcClient jdbc;
    private final AuditService audit;

    @Transactional
    public ImportReport importStudents(Long actorId, MultipartFile file) {
        List<Csv.Row> rows = Csv.parse(text(file), COLUMNS);
        Map<String, List<Long>> batches = batches();
        List<RowError> errors = new ArrayList<>();
        List<NewStudent> students = new ArrayList<>();
        Map<String, Integer> rollSeen = new HashMap<>();
        Map<String, Integer> emailSeen = new HashMap<>();
        for (Csv.Row row : rows) {
            String roll = row.get("roll_no").toUpperCase(Locale.ROOT);
            String name = row.get("full_name");
            String email = row.get("email").toLowerCase(Locale.ROOT);
            String programme = row.get("programme").toUpperCase(Locale.ROOT);
            String batch = row.get("batch");
            List<String> problems = new ArrayList<>();
            if (!ROLL.matcher(roll).matches()) problems.add("roll_no must be 1 to 30 letters, digits, hyphens or slashes");
            if (name.isEmpty() || name.length() > 150) problems.add("full_name is needed, up to 150 characters");
            if (email.length() > 254 || !EMAIL.matcher(email).matches()) problems.add("email is not a valid address");
            List<Long> matches = batches.getOrDefault(programme + "|" + batch.toLowerCase(Locale.ROOT), List.of());
            if (matches.isEmpty()) problems.add("no batch \"" + batch + "\" in programme \"" + programme + "\"");
            if (matches.size() > 1) problems.add("programme " + programme + " has more than one batch called " + batch + ", add this student by hand");
            Integer sameRoll = rollSeen.putIfAbsent(roll, row.line());
            if (sameRoll != null) problems.add("roll_no " + roll + " is also on line " + sameRoll);
            Integer sameEmail = emailSeen.putIfAbsent(email, row.line());
            if (sameEmail != null) problems.add("email " + email + " is also on line " + sameEmail);
            if (problems.isEmpty()) students.add(new NewStudent(matches.get(0), roll, name, email));
            else errors.add(new RowError(row.line(), String.join("; ", problems)));
        }
        alreadyOnTheRoster(rows, errors);
        if (!errors.isEmpty()) {
            errors.sort(java.util.Comparator.comparingInt(RowError::line));
            return ImportReport.rejected(rows.size(), errors);
        }

        for (NewStudent student : students) {
            jdbc.sql("INSERT INTO student (batch_id, roll_no, full_name, email) VALUES (:batch, :roll, :name, :email)")
                    .param("batch", student.batchId()).param("roll", student.rollNo()).param("name", student.fullName()).param("email", student.email())
                    .update();
        }
        audit.record(actorId, "STUDENTS_IMPORTED", "student", null, "{\"imported\":" + students.size() + "}");
        return ImportReport.accepted(rows.size(), students.size());
    }

    private void alreadyOnTheRoster(List<Csv.Row> rows, List<RowError> errors) {
        Set<String> rolls = new HashSet<>();
        Set<String> emails = new HashSet<>();
        rows.forEach(row -> {
            rolls.add(row.get("roll_no").toUpperCase(Locale.ROOT));
            emails.add(row.get("email").toLowerCase(Locale.ROOT));
        });
        Set<String> takenRolls = new HashSet<>(jdbc.sql("SELECT upper(roll_no) FROM student WHERE upper(roll_no) IN (:rolls)")
                .param("rolls", rolls).query(String.class).list());
        Set<String> takenEmails = new HashSet<>(jdbc.sql("SELECT email FROM student WHERE email IN (:emails)")
                .param("emails", emails).query(String.class).list());
        for (Csv.Row row : rows) {
            String roll = row.get("roll_no").toUpperCase(Locale.ROOT);
            String email = row.get("email").toLowerCase(Locale.ROOT);
            if (takenRolls.contains(roll)) errors.add(new RowError(row.line(), "roll_no " + roll + " is already on the roster"));
            if (takenEmails.contains(email)) errors.add(new RowError(row.line(), "email " + email + " is already on the roster"));
        }
    }

    /** Batch ids keyed by programme code and batch name, as a spreadsheet names them. */
    private Map<String, List<Long>> batches() {
        Map<String, List<Long>> byName = new HashMap<>();
        jdbc.sql("""
                SELECT upper(c.code), lower(b.name), b.id
                FROM batch b JOIN curriculum cu ON cu.id = b.curriculum_id JOIN course c ON c.id = cu.course_id""")
                .query((row, index) -> byName.computeIfAbsent(row.getString(1) + "|" + row.getString(2), ignored -> new ArrayList<>()).add(row.getLong(3)))
                .list();
        return byName;
    }

    public static String text(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.unprocessable("INVALID_FILE", "Choose a CSV file to import.");
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw ApiException.unprocessable("INVALID_FILE", "The file could not be read. Save it as CSV and try again.");
        }
    }
}
