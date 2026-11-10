package com.pms.dto;

import java.time.LocalDate;

import com.pms.entity.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Data the client sends to create or update a task
public record TaskRequest(

        @NotBlank(message = "Task title is required")
        @Size(max = 150, message = "Task title must be at most 150 characters")
        String title,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotNull(message = "Status is required (TODO, IN_PROGRESS or DONE)")
        TaskStatus status,

        // Optional, format: "2026-12-31"
        LocalDate dueDate
) {
}
