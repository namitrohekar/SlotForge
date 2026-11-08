package com.slotforge.academic.dto;

import com.slotforge.academic.entity.Batch;

public record BatchResponse(
        Long id, String name, int admissionYear, int currentSemester, Long curriculumId, String curriculumName,
        Long courseId, String courseCode, String courseName, int totalSemesters, long studentCount) {

    public static BatchResponse from(Batch batch, long studentCount) {
        var curriculum = batch.getCurriculum();
        var course = curriculum.getCourse();
        return new BatchResponse(batch.getId(), batch.getName(), batch.getAdmissionYear(), batch.getCurrentSemester(),
                curriculum.getId(), curriculum.getName(), course.getId(), course.getCode(), course.getName(),
                course.getTotalSemesters(), studentCount);
    }
}
