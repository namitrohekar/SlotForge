package com.slotforge.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One subject placed in a curriculum: in which semester, and whether it is an elective. */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CurriculumSubject {

    @Column(name = "subject_id")
    private Long subjectId;

    private int semesterNo;

    @Column(name = "is_elective")
    private boolean elective;
}
