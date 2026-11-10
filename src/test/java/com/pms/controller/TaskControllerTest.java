package com.pms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.pms.dto.TagResponse;
import com.pms.dto.TaskRequest;
import com.pms.dto.TaskResponse;
import com.pms.entity.TaskStatus;
import com.pms.exception.InvalidFileException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.service.TaskService;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final TaskResponse TASK = new TaskResponse(
            10L, "Login page", "Build the login page", TaskStatus.TODO, LocalDate.of(2026, 12, 1),
            null, 1L, List.of(new TagResponse(1L, "backend", "#FF5733")));

    @MockitoBean
    private TaskService taskService;

    private final MockMvc mockMvc;

    @Autowired
    TaskControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // ---------- Create / read / update / delete ----------

    @Test
    void getAllTasks_usesDefaultPaginationSortedById() throws Exception {
        Pageable expected = PageRequest.of(0, 10, Sort.by("id"));
        when(taskService.getAllTasks(expected)).thenReturn(new PageImpl<>(List.of(TASK), expected, 1));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Login page"))
                .andExpect(jsonPath("$.content[0].tags[0].name").value("backend"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getAllTasks_usesSortFromRequest() throws Exception {
        Pageable expected = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "dueDate"));
        when(taskService.getAllTasks(expected)).thenReturn(new PageImpl<>(List.of(TASK), expected, 1));

        mockMvc.perform(get("/api/tasks").param("size", "3").param("sort", "dueDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(3));
    }

    @Test
    void getTaskById_returnsTask() throws Exception {
        when(taskService.getTaskById(10L)).thenReturn(TASK);

        mockMvc.perform(get("/api/tasks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Login page"))
                .andExpect(jsonPath("$.description").value("Build the login page"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2026-12-01"))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.tags[0].color").value("#FF5733"));
    }

    @Test
    void getTaskById_returns404WhenTaskMissing() throws Exception {
        when(taskService.getTaskById(99L)).thenThrow(new ResourceNotFoundException("Task", 99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task not found with id 99"));
    }

    @Test
    void updateTask_returnsUpdatedTask() throws Exception {
        TaskRequest request = new TaskRequest("Login page", "Build the login page", TaskStatus.TODO, LocalDate.of(2026, 12, 1));
        when(taskService.updateTask(10L, request)).thenReturn(TASK);

        mockMvc.perform(put("/api/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Login page",
                                  "description": "Build the login page",
                                  "status": "TODO",
                                  "dueDate": "2026-12-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Login page"));
    }

    @Test
    void updateTask_returns400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(put("/api/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "", "status": "TODO" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Task title is required"));

        verifyNoInteractions(taskService);
    }

    @Test
    void deleteTask_returns204() throws Exception {
        mockMvc.perform(delete("/api/tasks/10"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(10L);
    }

    // ---------- Search and filters ----------

    @Test
    void searchTasks_passesTitleStatusAndPaging() throws Exception {
        Pageable expected = PageRequest.of(0, 5, Sort.by("id"));
        when(taskService.searchTasks("login", TaskStatus.TODO, expected))
                .thenReturn(new PageImpl<>(List.of(TASK), expected, 1));

        mockMvc.perform(get("/api/tasks/search")
                        .param("title", "login")
                        .param("status", "TODO")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Login page"));
    }

    @Test
    void searchTasks_withoutStatus_passesNoStatus() throws Exception {
        Pageable expected = PageRequest.of(0, 10, Sort.by("id"));
        when(taskService.searchTasks("login", null, expected))
                .thenReturn(new PageImpl<>(List.of(TASK), expected, 1));

        mockMvc.perform(get("/api/tasks/search").param("title", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Login page"));
    }

    @Test
    void searchTasks_returns400WhenTitleIsMissing() throws Exception {
        mockMvc.perform(get("/api/tasks/search"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void getOverdueTasks_returnsOverdueTasks() throws Exception {
        when(taskService.getOverdueTasks()).thenReturn(List.of(TASK));

        mockMvc.perform(get("/api/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Login page"));
    }

    @Test
    void getTasksByTag_passesTagNameToService() throws Exception {
        when(taskService.getTasksByTagName("backend")).thenReturn(List.of(TASK));

        mockMvc.perform(get("/api/tasks/by-tag").param("name", "backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Login page"));
    }

    // ---------- Tags and attachment ----------

    @Test
    void assignTags_returnsTaskWithTags() throws Exception {
        when(taskService.assignTags(10L, Set.of(1L, 2L))).thenReturn(TASK);

        mockMvc.perform(put("/api/tasks/10/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "tagIds": [1, 2] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0].name").value("backend"));
    }

    @Test
    void assignTags_returns400WhenTagIdsAreEmpty() throws Exception {
        mockMvc.perform(put("/api/tasks/10/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "tagIds": [] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.tagIds").value("At least one tag id is required"));

        verifyNoInteractions(taskService);
    }

    @Test
    void uploadAttachment_returnsTaskWithAttachmentPath() throws Exception {
        TaskResponse withAttachment = new TaskResponse(10L, "Login page", null, TaskStatus.TODO, null,
                "uploads/abc_spec.pdf", 1L, List.of());
        when(taskService.uploadAttachment(eq(10L), any(MultipartFile.class))).thenReturn(withAttachment);

        mockMvc.perform(multipart("/api/tasks/10/attachment").file(pdfFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentPath").value("uploads/abc_spec.pdf"));
    }

    @Test
    void uploadAttachment_returns400ForInvalidFile() throws Exception {
        when(taskService.uploadAttachment(eq(10L), any(MultipartFile.class)))
                .thenThrow(new InvalidFileException("Cannot upload an empty file"));

        mockMvc.perform(multipart("/api/tasks/10/attachment").file(pdfFile()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot upload an empty file"));
    }

    @Test
    void uploadAttachment_returns413WhenFileIsTooLarge() throws Exception {
        when(taskService.uploadAttachment(eq(10L), any(MultipartFile.class)))
                .thenThrow(new MaxUploadSizeExceededException(10L));

        mockMvc.perform(multipart("/api/tasks/10/attachment").file(pdfFile()))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.detail").value("File is too large"));
    }

    @Test
    void uploadAttachment_returns400WhenFileIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/tasks/10/attachment"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    private static MockMultipartFile pdfFile() {
        return new MockMultipartFile("file", "spec.pdf", "application/pdf", "pdf".getBytes());
    }
}
