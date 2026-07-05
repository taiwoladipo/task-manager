package com.example.taskmanager.task;

import com.example.taskmanager.project.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository repository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository repository, ProjectRepository projectRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
    }

    public List<Task> getAllTasks() {
        return repository.findAll();
    }

    public List<Task> getTasks(TaskFilter filter) {
        return repository.findAll(TaskSpecification.from(filter));
    }

    public Task getTaskById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
    }

    public Task createTask(Task request) {
        validatePayload(request);
        checkTitleAuthorUnique(request.getTitle().trim(), request.getAuthor().trim(), null);
        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setAuthor(request.getAuthor().trim());
        task.setDescription(trimToNull(request.getDescription()));
        task.setProject(request.getProject());
        task.setStatus(resolveStatus(request.getStatus()));
        return repository.saveAndFlush(task);
    }

    public Task updateTask(UUID id, Task request, String eTag) {
        validatePayload(request);
        Task task = getTaskById(id);

        if (eTag == null || eTag.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ETag header is required");
        }
        if (!computeEtag(task).equals(eTag)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task has been updated by another user.");
        }

        checkTitleAuthorUnique(request.getTitle().trim(), request.getAuthor().trim(), id);
        task.setTitle(request.getTitle().trim());
        task.setAuthor(request.getAuthor().trim());
        task.setDescription(trimToNull(request.getDescription()));
        task.setProject(request.getProject());
        task.setStatus(resolveStatus(request.getStatus()));
        return repository.save(task);
    }

    public String computeEtag(Task task) {
        String raw = String.valueOf(task.getVersion());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to generate ETag", ex);
        }
    }

    public void deleteTask(UUID id) {
        Task task = getTaskById(id);
        repository.delete(task);
    }

    private void checkTitleAuthorUnique(String title, String author, UUID id) {
        UUID sentinel = id != null ? id : new UUID(0, 0);
        if (repository.existsByTitleAndAuthorAndIdNot(title, author, sentinel)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A task with title '" + title + "' by '" + author + "' already exists");
        }
    }

    private void validatePayload(Task payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (isNullOrEmpty(payload.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (isNullOrEmpty(payload.getAuthor())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "author is required");
        }
        if (payload.getProject() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project is required");
        }
        if (!projectRepository.existsById(payload.getProject().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found");
        }
    }

    private TaskStatus resolveStatus(TaskStatus status) {
        return status == null ? TaskStatus.PENDING : status;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}

