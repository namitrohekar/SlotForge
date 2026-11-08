package com.slotforge.academic.dto;

import com.slotforge.academic.entity.Subject;

public record SubjectResponse(
        Long id, String code, String name, boolean heavy, Long departmentId, String departmentCode,
        String departmentName, long curriculumCount) {

    public static SubjectResponse from(Subject subject, long curriculumCount) {
        return new SubjectResponse(subject.getId(), subject.getCode(), subject.getName(), subject.isHeavy(),
                subject.getDepartment().getId(), subject.getDepartment().getCode(), subject.getDepartment().getName(),
                curriculumCount);
    }
}
