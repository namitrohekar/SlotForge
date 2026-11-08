package com.slotforge.academic.dto;

import com.slotforge.academic.entity.Course;

public record CourseResponse(
        Long id, String code, String name, int totalSemesters, Long departmentId, String departmentCode,
        String departmentName, long curriculumCount) {

    public static CourseResponse from(Course course, long curriculumCount) {
        return new CourseResponse(course.getId(), course.getCode(), course.getName(), course.getTotalSemesters(),
                course.getDepartment().getId(), course.getDepartment().getCode(), course.getDepartment().getName(),
                curriculumCount);
    }
}
