package com.slotforge.hall.dto;

import com.slotforge.hall.entity.ExamHall;
import com.slotforge.hall.entity.HallStatus;

public record HallResponse(
        Long id, String code, String name, String building, Integer floor, int capacity, HallStatus status,
        long allocationCount) {

    public static HallResponse from(ExamHall hall, long allocationCount) {
        return new HallResponse(hall.getId(), hall.getCode(), hall.getName(), hall.getBuilding(), hall.getFloor(),
                hall.getCapacity(), hall.getStatus(), allocationCount);
    }
}
