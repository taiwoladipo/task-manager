package com.example.taskmanager.task;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.taskmanager.task.TaskStatus.fromValue;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, List<Task>> getTasks(@RequestParam(required = false) String status) {
        TaskStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = fromValue(status);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
        }
        return Map.of("tasks", service.getTasks(new TaskFilter(parsedStatus)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable UUID id, HttpServletResponse response) {
        Task task = service.getTaskById(id);
        response.setHeader("ETag", service.computeEtag(task));
        return ResponseEntity.ok(task);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@RequestBody Task payload) {
        return service.createTask(payload);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable UUID id,
            @RequestHeader("ETag") String eTag,
            HttpServletResponse response,
            @RequestBody Task payload
    ) {
        Task task = service.updateTask(id, payload, eTag);
        Task refreshed = service.getTaskById(task.getId());
        response.setHeader("ETag", service.computeEtag(refreshed));
        return ResponseEntity.ok(refreshed);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteTask(@PathVariable UUID id) {
        service.deleteTask(id);
        return Map.of("success", "true");
    }
}

