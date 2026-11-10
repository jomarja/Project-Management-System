package com.pms.dto;

import java.time.LocalDateTime;

import com.pms.entity.Project;

// Project data returned to the client
public record ProjectResponse(Long id, String name, String description, LocalDateTime createdAt) {

    // Converts a Project entity into a response object
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt());
    }
}
