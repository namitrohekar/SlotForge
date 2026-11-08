package com.slotforge.exam.dto;

import java.time.LocalDate;

import com.slotforge.exam.entity.Examination;
import com.slotforge.exam.entity.ExaminationStatus;

public record ExaminationResponse(
        Long id, Long subjectId, String subjectCode, String subjectName, String departmentCode, boolean heavy,
        int durationMinutes, LocalDate earliestDate, LocalDate latestDate, ExaminationStatus status, long enrolled) {

    public static ExaminationResponse from(Examination exam, long enrolled) {
        var subject = exam.getSubject();
        return new ExaminationResponse(exam.getId(), subject.getId(), subject.getCode(), subject.getName(),
                subject.getDepartment().getCode(), subject.isHeavy(), exam.getDurationMinutes(), exam.getEarliestDate(),
                exam.getLatestDate(), exam.getStatus(), enrolled);
    }
}
