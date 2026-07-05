package com.example.taskmanager.task;

public record TaskFilter(TaskStatus status) {

    public static TaskFilter empty() {
        return new TaskFilter(null);
    }
}

