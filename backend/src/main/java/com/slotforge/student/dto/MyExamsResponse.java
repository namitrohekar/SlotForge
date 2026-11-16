package com.slotforge.student.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** rollNo and the other identity fields are null when the account is not linked to anyone on the roster. */
public record MyExamsResponse(String rollNo, String fullName, String programme, String batch, List<Exam> exams) {

    public record Exam(
            String sessionName, LocalDate date, LocalTime start, LocalTime end, String paperCode, String paperName,
            String hallCode, String hallName, String building) {
    }
}
