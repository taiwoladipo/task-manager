package com.example.taskmanager.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    //  Spring Data JPA derived query that checks if a task exists with the given title and author, excluding task ID if available
    boolean existsByTitleAndAuthorAndIdNot(String title, String author, UUID id);
}

