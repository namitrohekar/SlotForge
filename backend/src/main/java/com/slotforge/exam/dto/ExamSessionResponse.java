package com.slotforge.exam.dto;

import java.time.LocalDate;

import com.slotforge.exam.entity.ExamSession;
import com.slotforge.exam.entity.SessionStatus;

public record ExamSessionResponse(
        Long id, String name, LocalDate startDate, LocalDate endDate, SessionStatus status, int maxExamsPerStudentPerDay,
        int heavyGapDays, int studentsPerInvigilator, int maxDutiesPerInvigilator, long slotCount, long examinationCount,
        SessionProgress progress) {

    public static ExamSessionResponse from(ExamSession session, long slotCount, long examinationCount) {
        return from(session, slotCount, examinationCount, null);
    }

    public static ExamSessionResponse from(ExamSession session, long slotCount, long examinationCount, SessionProgress progress) {
        return new ExamSessionResponse(session.getId(), session.getName(), session.getStartDate(), session.getEndDate(),
                session.getStatus(), session.getMaxExamsPerStudentPerDay(), session.getHeavyGapDays(),
                session.getStudentsPerInvigilator(), session.getMaxDutiesPerInvigilator(), slotCount, examinationCount, progress);
    }
}
