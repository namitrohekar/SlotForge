package com.slotforge.exam.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrolmentRequest(@NotEmpty @Size(max = 500) Set<@NotNull Long> studentIds) {
}
