package com.pms.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Ids of the tags that should be assigned to a task, e.g. { "tagIds": [1, 2] }
public record TagAssignmentRequest(

        @NotEmpty(message = "At least one tag id is required")
        Set<@NotNull Long> tagIds
) {
}
