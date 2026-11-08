package com.slotforge.academic.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The complete placement of subjects in a curriculum. A wrapper so each entry is validated. */
public record CurriculumSubjectsRequest(@NotNull List<@Valid @NotNull CurriculumSubjectRequest> subjects) {
}
