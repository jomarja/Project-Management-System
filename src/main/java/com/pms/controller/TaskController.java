package com.pms.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pms.dto.TagAssignmentRequest;
import com.pms.dto.TaskRequest;
import com.pms.dto.TaskResponse;
import com.pms.entity.TaskStatus;
import com.pms.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // Example: GET /api/tasks?page=0&size=5&sort=dueDate,desc
    @GetMapping
    public Page<TaskResponse> getAllTasks(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return taskService.getAllTasks(pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    // Example: GET /api/tasks/search?title=login&status=IN_PROGRESS&page=0&size=5 (status is optional)
    @GetMapping("/search")
    public Page<TaskResponse> searchTasks(@RequestParam String title,
                                          @RequestParam(required = false) TaskStatus status,
                                          @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return taskService.searchTasks(title, status, pageable);
    }

    @GetMapping("/overdue")
    public List<TaskResponse> getOverdueTasks() {
        return taskService.getOverdueTasks();
    }

    // Example: GET /api/tasks/by-tag?name=backend
    @GetMapping("/by-tag")
    public List<TaskResponse> getTasksByTag(@RequestParam("name") String tagName) {
        return taskService.getTasksByTagName(tagName);
    }

    // Body example: { "tagIds": [1, 2] }
    @PutMapping("/{id}/tags")
    public TaskResponse assignTags(@PathVariable Long id, @Valid @RequestBody TagAssignmentRequest request) {
        return taskService.assignTags(id, request.tagIds());
    }

    // Send as multipart/form-data with the file in a field named "file"
    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TaskResponse uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return taskService.uploadAttachment(id, file);
    }
}
