package org.rinna.samples.clean.domain.usecase;

import org.rinna.samples.clean.domain.entity.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Task service - implements use cases for the domain
 */
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task createTask(String title, String description) {
        Task task = new Task(UUID.randomUUID().toString(), title, description);
        return taskRepository.save(task);
    }

    public Optional<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> completeTask(String id) {
        return taskRepository.findById(id)
                .map(task -> {
                    task.setCompleted(true);
                    return taskRepository.save(task);
                });
    }

    public void deleteTask(String id) {
        taskRepository.delete(id);
    }
}
