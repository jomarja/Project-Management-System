package com.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Data the client sends to create or update a project
public record ProjectRequest(

        @NotBlank(message = "Project name is required")
        @Size(max = 100, message = "Project name must be at most 100 characters")
        String name,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description
) {
}
