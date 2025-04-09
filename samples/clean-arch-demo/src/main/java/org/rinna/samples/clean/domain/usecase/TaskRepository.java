package org.rinna.samples.clean.domain.usecase;

import org.rinna.samples.clean.domain.entity.Task;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Tasks - part of the domain boundary
 */
public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(String id);
    List<Task> findAll();
    void delete(String id);
}
