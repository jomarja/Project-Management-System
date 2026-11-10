package com.pms.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.pms.entity.Project;
import com.pms.entity.Tag;
import com.pms.entity.Task;
import com.pms.entity.TaskStatus;

// Runs the real queries against an in-memory H2 database
@DataJpaTest
class TaskRepositoryTest {

    private static final LocalDate TODAY = LocalDate.now();

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final TestEntityManager entityManager;

    private Long websiteId;

    @Autowired
    TaskRepositoryTest(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       TagRepository tagRepository,
                       TestEntityManager entityManager) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        Project website = projectRepository.save(project("Website"));
        Project mobile = projectRepository.save(project("Mobile"));
        Tag backend = tagRepository.save(tag("backend"));
        Tag urgent = tagRepository.save(tag("urgent"));

        taskRepository.save(task(website, "Login page", TaskStatus.TODO, TODAY.minusDays(1), backend, urgent));
        taskRepository.save(task(website, "Logout button", TaskStatus.DONE, TODAY.minusDays(3), backend));
        taskRepository.save(task(website, "Signup form", TaskStatus.IN_PROGRESS, TODAY.minusDays(2)));
        taskRepository.save(task(mobile, "Login screen", TaskStatus.IN_PROGRESS, TODAY.plusDays(1), urgent));
        websiteId = website.getId();

        // Write everything to the database and forget cached entities, so the queries really hit the database
        entityManager.flush();
        entityManager.clear();
    }

    // ---------- Method-name queries ----------

    @Test
    void findByTitleContainingIgnoreCase_returnsMatchingTasksPageByPage() {
        Page<Task> firstPage = taskRepository.findByTitleContainingIgnoreCase(
                "LOGIN", PageRequest.of(0, 1, Sort.by("title")));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Task::getTitle).containsExactly("Login page");
    }

    @Test
    void findByTitleContainingIgnoreCaseAndStatus_returnsOnlyTasksWithThatStatus() {
        Page<Task> page = taskRepository.findByTitleContainingIgnoreCaseAndStatus(
                "login", TaskStatus.IN_PROGRESS, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("Login screen");
    }

    @Test
    void findByProjectId_returnsAllTasksOfProject() {
        List<Task> tasks = taskRepository.findByProjectId(websiteId);

        assertThat(tasks).extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Login page", "Logout button", "Signup form");
    }

    @Test
    void findByProjectIdAndStatus_returnsTasksOfProjectWithThatStatus() {
        List<Task> tasks = taskRepository.findByProjectIdAndStatus(websiteId, TaskStatus.DONE);

        assertThat(tasks).extracting(Task::getTitle).containsExactly("Logout button");
    }

    // ---------- Custom @Query methods ----------

    @Test
    void findOverdueTasks_returnsUnfinishedTasksPastDueDate_earliestFirst() {
        List<Task> tasks = taskRepository.findOverdueTasks(TaskStatus.DONE, TODAY, Sort.by("dueDate"));

        assertThat(tasks).extracting(Task::getTitle).containsExactly("Signup form", "Login page");
    }

    @Test
    void findByTagName_returnsTasksWithThatTag_ignoringCase() {
        List<Task> tasks = taskRepository.findByTagName("BACKEND");

        assertThat(tasks).extracting(Task::getTitle).containsExactlyInAnyOrder("Login page", "Logout button");
    }

    @Test
    void findByTagName_returnsEmptyListForUnknownTag() {
        assertThat(taskRepository.findByTagName("frontend")).isEmpty();
    }

    // ---------- Sorting and relationships ----------

    @Test
    void findAll_withUnknownSortField_throwsPropertyReferenceException() {
        PageRequest sortedByUnknownField = PageRequest.of(0, 10, Sort.by("unknownField"));

        assertThatThrownBy(() -> taskRepository.findAll(sortedByUnknownField))
                .isInstanceOf(PropertyReferenceException.class);
    }

    @Test
    void deletingProject_alsoDeletesItsTasks() {
        projectRepository.delete(projectRepository.findById(websiteId).orElseThrow());
        entityManager.flush();

        assertThat(taskRepository.findAll()).extracting(Task::getTitle).containsExactly("Login screen");
        assertThat(tagRepository.count()).isEqualTo(2);
    }

    // ---------- Test data helpers ----------

    private static Project project(String name) {
        Project project = new Project();
        project.setName(name);
        return project;
    }

    private static Tag tag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setColor("#FF5733");
        return tag;
    }

    private static Task task(Project project, String title, TaskStatus status, LocalDate dueDate, Tag... tags) {
        Task task = new Task();
        task.setProject(project);
        task.setTitle(title);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setTags(new HashSet<>(List.of(tags)));
        return task;
    }
}
