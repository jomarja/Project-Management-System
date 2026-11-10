package com.pms.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import com.pms.dto.ProjectRequest;
import com.pms.dto.ProjectResponse;
import com.pms.dto.TaskRequest;
import com.pms.dto.TaskResponse;
import com.pms.entity.TaskStatus;
import com.pms.service.ProjectService;
import com.pms.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    // Constructor injection: Spring passes the services in automatically
    public ProjectController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request) {
        return projectService.createProject(request);
    }

    // Example: GET /api/projects?page=0&size=5&sort=name,asc (default: newest first)
    @GetMapping
    public Page<ProjectResponse> getAllProjects(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.getAllProjects(pageable);
    }

    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    // ---------- Tasks inside a project ----------

    @PostMapping("/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable Long projectId, @Valid @RequestBody TaskRequest request) {
        return taskService.createTask(projectId, request);
    }

    // Example: GET /api/projects/1/tasks?status=DONE (status is optional)
    @GetMapping("/{projectId}/tasks")
    public List<TaskResponse> getProjectTasks(@PathVariable Long projectId,
                                              @RequestParam(required = false) TaskStatus status) {
        return taskService.getProjectTasks(projectId, status);
    }
}
