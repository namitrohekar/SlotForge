package com.slotforge.academic.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** elective is optional and means "core" when left out. Jackson 3 rejects a missing primitive, so it is a Boolean. */
public record CurriculumSubjectRequest(@NotNull Long subjectId, @NotNull @Min(1) Integer semesterNo, Boolean elective) {

    public CurriculumSubjectRequest {
        if (elective == null) elective = false;
    }
}
