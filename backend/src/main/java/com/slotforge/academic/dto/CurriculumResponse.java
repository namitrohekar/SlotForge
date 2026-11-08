package com.slotforge.academic.dto;

import com.slotforge.academic.entity.Curriculum;

public record CurriculumResponse(
        Long id, String name, int effectiveFromYear, Long courseId, String courseCode, String courseName,
        int totalSemesters, long subjectCount, long batchCount) {

    public static CurriculumResponse from(Curriculum curriculum, long subjectCount, long batchCount) {
        var course = curriculum.getCourse();
        return new CurriculumResponse(curriculum.getId(), curriculum.getName(), curriculum.getEffectiveFromYear(),
                course.getId(), course.getCode(), course.getName(), course.getTotalSemesters(), subjectCount, batchCount);
    }
}
