package com.slotforge.student.dto;

import com.slotforge.student.entity.Student;

public record StudentResponse(
        Long id, String rollNo, String fullName, String email, Long batchId, String batchName, String courseCode,
        String courseName, int currentSemester, long enrolmentCount) {

    public static StudentResponse from(Student student, long enrolmentCount) {
        var batch = student.getBatch();
        var course = batch.getCurriculum().getCourse();
        return new StudentResponse(student.getId(), student.getRollNo(), student.getFullName(), student.getEmail(),
                batch.getId(), batch.getName(), course.getCode(), course.getName(), batch.getCurrentSemester(), enrolmentCount);
    }
}
