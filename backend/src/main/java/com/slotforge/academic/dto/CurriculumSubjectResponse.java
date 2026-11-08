package com.slotforge.academic.dto;

public record CurriculumSubjectResponse(
        Long subjectId, String code, String name, boolean heavy, String departmentCode, int semesterNo,
        boolean elective) {
}
