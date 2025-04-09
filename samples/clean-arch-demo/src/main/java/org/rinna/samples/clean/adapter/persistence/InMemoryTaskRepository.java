package org.rinna.samples.clean.adapter.persistence;

import org.rinna.samples.clean.domain.entity.Task;
import org.rinna.samples.clean.domain.usecase.TaskRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the task repository
 */
public class InMemoryTaskRepository implements TaskRepository {
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public Task save(Task task) {
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void delete(String id) {
        tasks.remove(id);
    }
}
