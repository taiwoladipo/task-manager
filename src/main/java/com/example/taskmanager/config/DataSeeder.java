package com.example.taskmanager.config;

import com.example.taskmanager.project.Project;
import com.example.taskmanager.project.ProjectRepository;
import com.example.taskmanager.task.Task;
import com.example.taskmanager.task.TaskRepository;
import com.example.taskmanager.task.TaskStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Configuration
public class DataSeeder {

    private static final UUID DEFAULT_PROJECT_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Bean
    CommandLineRunner seedProjects(ProjectRepository projectRepository, TaskRepository taskRepository) {
        return args -> {
            Project defaultProject = insertProjects(projectRepository);
            insertTasks(taskRepository, defaultProject);
        };
    }

    private Project insertProjects(ProjectRepository projectRepository) {
        if (projectRepository.count() > 0) {
            return projectRepository.findById(DEFAULT_PROJECT_ID).orElse(null);
        }

        Project project = new Project();
        project.setId(DEFAULT_PROJECT_ID);
        project.setName("Tasks API");
        project.setCreatedAt(Instant.parse("2025-09-28T08:00:00Z"));
        return projectRepository.save(project);
    }

    private void insertTasks(TaskRepository taskRepository, Project project) {
        if (taskRepository.count() > 0) {
            return;
        }

        if (project == null) {
            throw new IllegalStateException("Default project not found for task seeding");
        }

        Task authTask = new Task();
        authTask.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        authTask.setTitle("Implement User Authentication");
        authTask.setAuthor("Alice Johnson");
        authTask.setDescription("Create a secure user authentication system using JWT.");
        authTask.setProject(project);
        authTask.setStatus(TaskStatus.PENDING);
        authTask.setCreatedAt(Instant.parse("2025-09-29T13:23:16Z"));
        authTask.setUpdatedAt(Instant.parse("2025-09-29T13:23:16Z"));

        Task dbSchemaTask = new Task();
        dbSchemaTask.setId( UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        dbSchemaTask.setTitle("Design Database Schema");
        dbSchemaTask.setAuthor("Bob Smith");
        dbSchemaTask.setDescription("Draft the database schema for the project.");
        dbSchemaTask.setProject(project);
        dbSchemaTask.setStatus(TaskStatus.IN_PROGRESS);
        dbSchemaTask.setCreatedAt(Instant.parse("2025-09-28T10:15:30Z"));
        dbSchemaTask.setUpdatedAt(Instant.parse("2025-09-29T09:00:00Z"));

        taskRepository.saveAll(List.of(authTask, dbSchemaTask));
    }
}

