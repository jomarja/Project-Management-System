package com.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.pms.dto.ProjectRequest;
import com.pms.dto.ProjectResponse;
import com.pms.entity.Project;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_savesAndReturnsProject() {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProjectResponse response = projectService.createProject(new ProjectRequest("Website", "Company site"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Website");
        assertThat(response.description()).isEqualTo("Company site");
    }

    @Test
    void getAllProjects_returnsRequestedPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(project(1L)), pageable, 1));

        Page<ProjectResponse> page = projectService.getAllProjects(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(ProjectResponse::name).containsExactly("Website");
    }

    @Test
    void getProjectById_returnsProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project(1L)));

        ProjectResponse response = projectService.getProjectById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Website");
    }

    @Test
    void getProjectById_throwsWhenProjectMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id 99");
    }

    @Test
    void updateProject_changesNameAndDescription() {
        Project project = project(1L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.updateProject(1L, new ProjectRequest("New name", "New description"));

        assertThat(project.getName()).isEqualTo("New name");
        assertThat(project.getDescription()).isEqualTo("New description");
        assertThat(response.name()).isEqualTo("New name");
    }

    @Test
    void updateProject_throwsWhenProjectMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject(99L, new ProjectRequest("Name", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteProject_deletesExistingProject() {
        Project project = project(1L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        projectService.deleteProject(1L);

        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_throwsWhenProjectMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(projectRepository, never()).delete(any());
    }

    private static Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setName("Website");
        project.setDescription("Company site");
        return project;
    }
}
