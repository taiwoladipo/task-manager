package com.example.taskmanager.project;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public List<Project> getProjects() {
        return repository.findAll();
    }

    public Project createProject(Project payload) {
        validatePayload(payload);
        Project project = new Project();
        project.setName(payload.getName().trim());
        return repository.save(project);
    }

    private void validatePayload(Project payload) {
        if (payload == null || payload.getName() == null || payload.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
    }
}

