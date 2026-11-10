package com.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import com.pms.dto.TagResponse;
import com.pms.dto.TaskRequest;
import com.pms.dto.TaskResponse;
import com.pms.entity.Project;
import com.pms.entity.Tag;
import com.pms.entity.Task;
import com.pms.entity.TaskStatus;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.ProjectRepository;
import com.pms.repository.TagRepository;
import com.pms.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 12, 1);

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private TaskService taskService;

    private final Project website = project(1L);

    // ---------- Create / read / update / delete ----------

    @Test
    void createTask_savesTaskInsideProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(website));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        TaskResponse response = taskService.createTask(1L, taskRequest("Login page", TaskStatus.TODO));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Login page");
        assertThat(response.description()).isEqualTo("Build the login page");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.dueDate()).isEqualTo(DUE_DATE);
        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.tags()).isEmpty();
    }

    @Test
    void createTask_throwsWhenProjectMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(99L, taskRequest("Login page", TaskStatus.TODO)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id 99");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void getAllTasks_returnsRequestedPage() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("dueDate"));
        when(taskRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(task(10L, "Login page")), pageable, 1));

        Page<TaskResponse> page = taskService.getAllTasks(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(TaskResponse::title).containsExactly("Login page");
    }

    @Test
    void getTaskById_returnsTask() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task(10L, "Login page")));

        TaskResponse response = taskService.getTaskById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Login page");
    }

    @Test
    void getTaskById_throwsWhenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found with id 99");
    }

    @Test
    void updateTask_changesAllFields() {
        Task task = task(10L, "Old title");
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.updateTask(10L, taskRequest("New title", TaskStatus.DONE));

        assertThat(task.getTitle()).isEqualTo("New title");
        assertThat(task.getDescription()).isEqualTo("Build the login page");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(response.title()).isEqualTo("New title");
    }

    @Test
    void updateTask_throwsWhenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(99L, taskRequest("Title", TaskStatus.TODO)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTask_deletesExistingTask() {
        Task task = task(10L, "Login page");
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        taskService.deleteTask(10L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_throwsWhenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(taskRepository, never()).delete(any());
    }

    // ---------- Search and filters ----------

    @Test
    void searchTasks_withoutStatus_searchesByTitleOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        when(taskRepository.findByTitleContainingIgnoreCase("login", pageable))
                .thenReturn(new PageImpl<>(List.of(task(10L, "Login page")), pageable, 1));

        Page<TaskResponse> page = taskService.searchTasks("login", null, pageable);

        assertThat(page.getContent()).extracting(TaskResponse::title).containsExactly("Login page");
        verify(taskRepository, never()).findByTitleContainingIgnoreCaseAndStatus(any(), any(), any());
    }

    @Test
    void searchTasks_withStatus_searchesByTitleAndStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(taskRepository.findByTitleContainingIgnoreCaseAndStatus("login", TaskStatus.TODO, pageable))
                .thenReturn(new PageImpl<>(List.of(task(10L, "Login page")), pageable, 1));

        Page<TaskResponse> page = taskService.searchTasks("login", TaskStatus.TODO, pageable);

        assertThat(page.getContent()).extracting(TaskResponse::title).containsExactly("Login page");
        verify(taskRepository, never()).findByTitleContainingIgnoreCase(any(), any());
    }

    @Test
    void getProjectTasks_withoutStatus_returnsAllTasksOfProject() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(task(10L, "Login page"), task(11L, "Signup")));

        List<TaskResponse> tasks = taskService.getProjectTasks(1L, null);

        assertThat(tasks).extracting(TaskResponse::title).containsExactly("Login page", "Signup");
        verify(taskRepository, never()).findByProjectIdAndStatus(any(), any());
    }

    @Test
    void getProjectTasks_withStatus_filtersByStatus() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.findByProjectIdAndStatus(1L, TaskStatus.DONE)).thenReturn(List.of(task(10L, "Login page")));

        List<TaskResponse> tasks = taskService.getProjectTasks(1L, TaskStatus.DONE);

        assertThat(tasks).extracting(TaskResponse::title).containsExactly("Login page");
        verify(taskRepository, never()).findByProjectId(any());
    }

    @Test
    void getProjectTasks_throwsWhenProjectMissing() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.getProjectTasks(99L, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id 99");
        verifyNoInteractions(taskRepository);
    }

    @Test
    void getOverdueTasks_excludesDoneTasksAndSortsByDueDate() {
        when(taskRepository.findOverdueTasks(eq(TaskStatus.DONE), any(LocalDate.class), eq(Sort.by("dueDate"))))
                .thenReturn(List.of(task(10L, "Login page")));
        LocalDate before = LocalDate.now();

        List<TaskResponse> tasks = taskService.getOverdueTasks();

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(taskRepository).findOverdueTasks(eq(TaskStatus.DONE), dateCaptor.capture(), eq(Sort.by("dueDate")));
        assertThat(dateCaptor.getValue()).isBetween(before, LocalDate.now());
        assertThat(tasks).extracting(TaskResponse::title).containsExactly("Login page");
    }

    @Test
    void getTasksByTagName_returnsMatchingTasks() {
        when(taskRepository.findByTagName("backend")).thenReturn(List.of(task(10L, "Login page")));

        List<TaskResponse> tasks = taskService.getTasksByTagName("backend");

        assertThat(tasks).extracting(TaskResponse::title).containsExactly("Login page");
    }

    // ---------- Tags and attachment ----------

    @Test
    void assignTags_replacesTagsOfTask() {
        Task task = task(10L, "Login page");
        task.getTags().add(tag(5L, "old"));
        Set<Long> tagIds = Set.of(1L, 2L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(tagRepository.findAllById(tagIds)).thenReturn(List.of(tag(1L, "backend"), tag(2L, "urgent")));

        TaskResponse response = taskService.assignTags(10L, tagIds);

        assertThat(task.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder("backend", "urgent");
        assertThat(response.tags()).extracting(TagResponse::name).containsExactlyInAnyOrder("backend", "urgent");
    }

    @Test
    void assignTags_throwsWhenSomeTagIsMissing() {
        Task task = task(10L, "Login page");
        Set<Long> tagIds = Set.of(1L, 99L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(tagRepository.findAllById(tagIds)).thenReturn(List.of(tag(1L, "backend")));

        assertThatThrownBy(() -> taskService.assignTags(10L, tagIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageStartingWith("One or more tags were not found");
        assertThat(task.getTags()).isEmpty();
    }

    @Test
    void assignTags_throwsWhenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assignTags(99L, Set.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tagRepository);
    }

    @Test
    void uploadAttachment_storesFileAndSavesItsPath() {
        Task task = task(10L, "Login page");
        MockMultipartFile file = new MockMultipartFile("file", "spec.pdf", "application/pdf", "pdf".getBytes());
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(fileStorageService.store(file)).thenReturn("uploads/abc_spec.pdf");

        TaskResponse response = taskService.uploadAttachment(10L, file);

        assertThat(task.getAttachmentPath()).isEqualTo("uploads/abc_spec.pdf");
        assertThat(response.attachmentPath()).isEqualTo("uploads/abc_spec.pdf");
    }

    @Test
    void uploadAttachment_throwsWhenTaskMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "spec.pdf", "application/pdf", "pdf".getBytes());
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.uploadAttachment(99L, file))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(fileStorageService);
    }

    // ---------- Test data helpers ----------

    private static TaskRequest taskRequest(String title, TaskStatus status) {
        return new TaskRequest(title, "Build the login page", status, DUE_DATE);
    }

    private Task task(Long id, String title) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setStatus(TaskStatus.TODO);
        task.setProject(website);
        return task;
    }

    private static Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setName("Website");
        return project;
    }

    private static Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        tag.setColor("#FF5733");
        return tag;
    }
}
