package com.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Data the client sends to create a tag
public record TagRequest(

        @NotBlank(message = "Tag name is required")
        @Size(max = 50, message = "Tag name must be at most 50 characters")
        String name,

        @NotBlank(message = "Tag color is required")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex code like #FF5733")
        String color
) {
}
