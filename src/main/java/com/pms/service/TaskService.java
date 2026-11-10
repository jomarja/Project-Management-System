package com.pms.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final FileStorageService fileStorageService;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       TagRepository tagRepository,
                       FileStorageService fileStorageService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
        this.fileStorageService = fileStorageService;
    }

    // ---------- Create / read / update / delete ----------

    @Transactional
    public TaskResponse createTask(Long projectId, TaskRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        Task task = new Task();
        task.setProject(project);
        copyRequestToTask(request, task);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return TaskResponse.from(findTask(id));
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTask(id);
        copyRequestToTask(request, task);
        // No save() needed: changes to a loaded entity are written when the transaction commits
        return TaskResponse.from(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.delete(findTask(id));
    }

    // ---------- Search and filters ----------

    // Status is optional: without it, only the title is used
    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasks(String title, TaskStatus status, Pageable pageable) {
        Page<Task> tasks = (status == null)
                ? taskRepository.findByTitleContainingIgnoreCase(title, pageable)
                : taskRepository.findByTitleContainingIgnoreCaseAndStatus(title, status, pageable);
        return tasks.map(TaskResponse::from);
    }

    // Status is optional: without it, all tasks of the project are returned
    @Transactional(readOnly = true)
    public List<TaskResponse> getProjectTasks(Long projectId, TaskStatus status) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }

        List<Task> tasks = (status == null)
                ? taskRepository.findByProjectId(projectId)
                : taskRepository.findByProjectIdAndStatus(projectId, status);
        return toResponses(tasks);
    }

    // Tasks not DONE yet whose due date is before today, earliest first
    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasks() {
        List<Task> tasks = taskRepository.findOverdueTasks(TaskStatus.DONE, LocalDate.now(), Sort.by("dueDate"));
        return toResponses(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByTagName(String tagName) {
        return toResponses(taskRepository.findByTagName(tagName));
    }

    // ---------- Tags and attachment ----------

    // Replaces the task's tags with the given ones
    @Transactional
    public TaskResponse assignTags(Long taskId, Set<Long> tagIds) {
        Task task = findTask(taskId);

        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new ResourceNotFoundException("One or more tags were not found: " + tagIds);
        }

        task.setTags(new HashSet<>(tags));
        return TaskResponse.from(task);
    }

    // Saves the file on disk and remembers its path in the database
    @Transactional
    public TaskResponse uploadAttachment(Long taskId, MultipartFile file) {
        Task task = findTask(taskId);
        task.setAttachmentPath(fileStorageService.store(file));
        return TaskResponse.from(task);
    }

    // ---------- Helpers ----------

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private void copyRequestToTask(TaskRequest request, Task task) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
    }

    private List<TaskResponse> toResponses(List<Task> tasks) {
        return tasks.stream()
                .map(TaskResponse::from)
                .toList();
    }
}
