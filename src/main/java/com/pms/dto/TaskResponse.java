package com.pms.dto;

import java.time.LocalDate;
import java.util.List;

import com.pms.entity.Task;
import com.pms.entity.TaskStatus;

// Task data returned to the client
public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        String attachmentPath,
        Long projectId,
        List<TagResponse> tags
) {

    // Converts a Task entity into a response object
    public static TaskResponse from(Task task) {
        List<TagResponse> tags = task.getTags().stream()
                .map(TagResponse::from)
                .toList();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getAttachmentPath(),
                task.getProject().getId(),
                tags);
    }
}
