package com.pms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pms.entity.Task;
import com.pms.entity.TaskStatus;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // ---------- Method-name queries: Spring builds the query from the method name ----------

    // Tasks whose title contains the given text (case-insensitive)
    Page<Task> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Same as above, but only tasks with the given status
    Page<Task> findByTitleContainingIgnoreCaseAndStatus(String title, TaskStatus status, Pageable pageable);

    // All tasks of one project
    List<Task> findByProjectId(Long projectId);

    // Tasks of one project that have the given status
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    // ---------- Custom @Query methods: we write the JPQL ourselves ----------

    // Tasks whose due date has passed and that are not completed yet
    @Query("SELECT t FROM Task t WHERE t.status <> :completedStatus AND t.dueDate < :date")
    List<Task> findOverdueTasks(@Param("completedStatus") TaskStatus completedStatus,
                                @Param("date") LocalDate date,
                                Sort sort);

    // Tasks that have a tag with the given name (case-insensitive)
    @Query("SELECT DISTINCT t FROM Task t JOIN t.tags tag WHERE LOWER(tag.name) = LOWER(:tagName)")
    List<Task> findByTagName(@Param("tagName") String tagName);
}
