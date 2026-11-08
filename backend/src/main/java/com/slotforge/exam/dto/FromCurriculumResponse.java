package com.slotforge.exam.dto;

/** electivesAwaitingStudents counts new elective papers, which students join individually. */
public record FromCurriculumResponse(int created, int enrolled, int electivesAwaitingStudents) {
}
