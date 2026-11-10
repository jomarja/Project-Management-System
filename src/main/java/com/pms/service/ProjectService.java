package com.pms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.dto.ProjectRequest;
import com.pms.dto.ProjectResponse;
import com.pms.entity.Project;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.ProjectRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable).map(ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        return ProjectResponse.from(findProject(id));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProject(id);
        project.setName(request.name());
        project.setDescription(request.description());
        // No save() needed: changes to a loaded entity are written when the transaction commits
        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        // The project's tasks are deleted too (cascade on Project.tasks)
        projectRepository.delete(findProject(id));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
