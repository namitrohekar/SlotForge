package com.slotforge.academic.dto;

import com.slotforge.academic.entity.Department;

public record DepartmentResponse(Long id, String code, String name, long courseCount, long subjectCount, Long headUserId, String headName) {

    public static DepartmentResponse from(Department department, long courseCount, long subjectCount) {
        return new DepartmentResponse(department.getId(), department.getCode(), department.getName(), courseCount,
                subjectCount, department.getHead() == null ? null : department.getHead().getId(),
                department.getHead() == null ? null : department.getHead().getFullName());
    }
}
