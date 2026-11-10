package com.pms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pms.dto.ProjectRequest;
import com.pms.dto.ProjectResponse;
import com.pms.dto.TaskRequest;
import com.pms.dto.TaskResponse;
import com.pms.entity.Project;
import com.pms.entity.TaskStatus;
import com.pms.exception.ResourceNotFoundException;
import com.pms.service.ProjectService;
import com.pms.service.TaskService;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    private static final ProjectResponse PROJECT =
            new ProjectResponse(1L, "Website", "Company site", LocalDateTime.of(2026, 11, 10, 12, 30, 15));

    private static final TaskResponse TASK =
            new TaskResponse(10L, "Login page", null, TaskStatus.TODO, LocalDate.of(2026, 12, 1), null, 1L, List.of());

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private TaskService taskService;

    private final MockMvc mockMvc;

    @Autowired
    ProjectControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // ---------- Projects ----------

    @Test
    void createProject_returns201WithCreatedProject() throws Exception {
        when(projectService.createProject(new ProjectRequest("Website", "Company site"))).thenReturn(PROJECT);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Website", "description": "Company site" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Website"))
                .andExpect(jsonPath("$.description").value("Company site"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-10T12:30:15"));
    }

    @Test
    void createProject_returns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": " " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Project name is required"));

        verifyNoInteractions(projectService);
    }

    @Test
    void getAllProjects_usesPageSizeAndSortFromRequest() throws Exception {
        when(projectService.getAllProjects(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(PROJECT), invocation.getArgument(0), 6));

        mockMvc.perform(get("/api/projects")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Website"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(6))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        assertThat(capturedPageable()).isEqualTo(PageRequest.of(1, 5, Sort.by("name")));
    }

    @Test
    void getAllProjects_usesNewestFirstByDefault() throws Exception {
        when(projectService.getAllProjects(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(PROJECT), invocation.getArgument(0), 1));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk());

        assertThat(capturedPageable()).isEqualTo(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void getAllProjects_returns400ForUnknownSortField() throws Exception {
        when(projectService.getAllProjects(any(Pageable.class)))
                .thenThrow(new PropertyReferenceException("unknown", TypeInformation.of(Project.class), List.of()));

        mockMvc.perform(get("/api/projects").param("sort", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProjectById_returnsProject() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(PROJECT);

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Website"));
    }

    @Test
    void getProjectById_returns404WhenProjectMissing() throws Exception {
        when(projectService.getProjectById(99L)).thenThrow(new ResourceNotFoundException("Project", 99L));

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project not found with id 99"));
    }

    @Test
    void updateProject_returnsUpdatedProject() throws Exception {
        ProjectResponse updated = new ProjectResponse(1L, "New name", null, PROJECT.createdAt());
        when(projectService.updateProject(1L, new ProjectRequest("New name", null))).thenReturn(updated);

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "New name" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));
    }

    @Test
    void updateProject_returns400WhenNameIsTooLong() throws Exception {
        String longName = "a".repeat(101);

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"" + longName + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Project name must be at most 100 characters"));

        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_returns204() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(1L);
    }

    // ---------- Tasks inside a project ----------

    @Test
    void createTask_returns201WithCreatedTask() throws Exception {
        TaskRequest request = new TaskRequest("Login page", null, TaskStatus.TODO, LocalDate.of(2026, 12, 1));
        when(taskService.createTask(1L, request)).thenReturn(TASK);

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Login page", "status": "TODO", "dueDate": "2026-12-01" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.dueDate").value("2026-12-01"));
    }

    @Test
    void createTask_returns400WhenStatusIsMissing() throws Exception {
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Login page" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").value("Status is required (TODO, IN_PROGRESS or DONE)"));

        verifyNoInteractions(taskService);
    }

    @Test
    void getProjectTasks_passesStatusFilterToService() throws Exception {
        when(taskService.getProjectTasks(1L, TaskStatus.TODO)).thenReturn(List.of(TASK));

        mockMvc.perform(get("/api/projects/1/tasks").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Login page"));
    }

    @Test
    void getProjectTasks_withoutStatus_passesNoFilter() throws Exception {
        when(taskService.getProjectTasks(1L, null)).thenReturn(List.of(TASK));

        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getProjectTasks_returns400ForUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/projects/1/tasks").param("status", "WRONG"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectService).getAllProjects(pageableCaptor.capture());
        return pageableCaptor.getValue();
    }
}
