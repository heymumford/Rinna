/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.Workstream;
import org.rinna.domain.model.WorkstreamCreateRequest;
import org.rinna.domain.model.WorkstreamRecord;
import org.rinna.domain.repository.WorkstreamRepository;

/**
 * In-memory implementation of the WorkstreamRepository interface.
 * For testing and demonstration purposes only.
 */
public class InMemoryWorkstreamRepository implements WorkstreamRepository {
    
    private final Map<UUID, Workstream> workstreams = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> workstreamToProjects = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> projectToWorkstreams = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> workstreamToWorkItems = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> workItemToWorkstreams = new ConcurrentHashMap<>();
    
    @Override
    public Workstream create(WorkstreamCreateRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        Workstream workstream = new WorkstreamRecord(
            id,
            request.name(),
            request.description(),
            request.owner(),
            request.status(),
            request.priority(),
            now,
            now,
            request.getOrganizationId().orElse(null),
            request.getCynefinDomain().orElse(null),
            request.crossProject(),
            request.getTargetDate().orElse(null)
        );
        
        return save(workstream);
    }
    
    @Override
    public Optional<Workstream> findById(UUID id) {
        return Optional.ofNullable(workstreams.get(id));
    }
    
    @Override
    public List<Workstream> findAll() {
        return new ArrayList<>(workstreams.values());
    }
    
    @Override
    public List<Workstream> findByStatus(String status) {
        return workstreams.values().stream()
                .filter(w -> Objects.equals(status, w.getStatus()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Workstream> findByOwner(String owner) {
        return workstreams.values().stream()
                .filter(w -> Objects.equals(owner, w.getOwner()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Workstream> findByPriority(Priority priority) {
        return workstreams.values().stream()
                .filter(w -> w.getPriority() == priority)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Workstream> findByOrganization(UUID organizationId) {
        return workstreams.values().stream()
                .filter(w -> w.getOrganizationId().isPresent() 
                          && Objects.equals(organizationId, w.getOrganizationId().get()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Workstream> findByCynefinDomain(CynefinDomain domain) {
        return workstreams.values().stream()
                .filter(w -> w.getCynefinDomain().isPresent() 
                          && w.getCynefinDomain().get() == domain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Workstream> findCrossProjectWorkstreams() {
        return workstreams.values().stream()
                .filter(Workstream::isCrossProject)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Workstream> findByProject(UUID projectId) {
        Set<UUID> workstreamIds = projectToWorkstreams.getOrDefault(projectId, Collections.emptySet());
        return workstreamIds.stream()
                .map(workstreams::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Workstream> findByWorkItem(UUID workItemId) {
        Set<UUID> workstreamIds = workItemToWorkstreams.getOrDefault(workItemId, Collections.emptySet());
        return workstreamIds.stream()
                .map(workstreams::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    public Workstream save(Workstream workstream) {
        workstreams.put(workstream.getId(), workstream);
        return workstream;
    }
    
    @Override
    public void deleteById(UUID id) {
        // Remove workstream
        workstreams.remove(id);
        
        // Clean up project associations
        Set<UUID> projectIds = workstreamToProjects.getOrDefault(id, Collections.emptySet());
        for (UUID projectId : projectIds) {
            Set<UUID> projectWorkstreams = projectToWorkstreams.get(projectId);
            if (projectWorkstreams != null) {
                projectWorkstreams.remove(id);
                if (projectWorkstreams.isEmpty()) {
                    projectToWorkstreams.remove(projectId);
                }
            }
        }
        workstreamToProjects.remove(id);
        
        // Clean up work item associations
        Set<UUID> workItemIds = workstreamToWorkItems.getOrDefault(id, Collections.emptySet());
        for (UUID workItemId : workItemIds) {
            Set<UUID> itemWorkstreams = workItemToWorkstreams.get(workItemId);
            if (itemWorkstreams != null) {
                itemWorkstreams.remove(id);
                if (itemWorkstreams.isEmpty()) {
                    workItemToWorkstreams.remove(workItemId);
                }
            }
        }
        workstreamToWorkItems.remove(id);
    }
    
    @Override
    public boolean associateWorkItem(UUID workstreamId, UUID workItemId) {
        if (!workstreams.containsKey(workstreamId)) {
            return false;
        }
        
        // Associate workstream -> work item
        workstreamToWorkItems.computeIfAbsent(workstreamId, k -> new HashSet<>()).add(workItemId);
        
        // Associate work item -> workstream
        workItemToWorkstreams.computeIfAbsent(workItemId, k -> new HashSet<>()).add(workstreamId);
        
        return true;
    }
    
    @Override
    public boolean dissociateWorkItem(UUID workstreamId, UUID workItemId) {
        boolean removed = false;
        
        // Remove from workstream -> work items
        Set<UUID> workItems = workstreamToWorkItems.get(workstreamId);
        if (workItems != null) {
            removed = workItems.remove(workItemId);
            if (workItems.isEmpty()) {
                workstreamToWorkItems.remove(workstreamId);
            }
        }
        
        // Remove from work item -> workstreams
        Set<UUID> itemWorkstreams = workItemToWorkstreams.get(workItemId);
        if (itemWorkstreams != null) {
            itemWorkstreams.remove(workstreamId);
            if (itemWorkstreams.isEmpty()) {
                workItemToWorkstreams.remove(workItemId);
            }
        }
        
        return removed;
    }
    
    @Override
    public List<UUID> findWorkItemIdsByWorkstreamId(UUID workstreamId) {
        return new ArrayList<>(workstreamToWorkItems.getOrDefault(workstreamId, Collections.emptySet()));
    }
    
    @Override
    public boolean associateProject(UUID workstreamId, UUID projectId) {
        if (!workstreams.containsKey(workstreamId)) {
            return false;
        }
        
        // Associate workstream -> project
        workstreamToProjects.computeIfAbsent(workstreamId, k -> new HashSet<>()).add(projectId);
        
        // Associate project -> workstream
        projectToWorkstreams.computeIfAbsent(projectId, k -> new HashSet<>()).add(workstreamId);
        
        return true;
    }
    
    @Override
    public boolean dissociateProject(UUID workstreamId, UUID projectId) {
        boolean removed = false;
        
        // Remove from workstream -> projects
        Set<UUID> projects = workstreamToProjects.get(workstreamId);
        if (projects != null) {
            removed = projects.remove(projectId);
            if (projects.isEmpty()) {
                workstreamToProjects.remove(workstreamId);
            }
        }
        
        // Remove from project -> workstreams
        Set<UUID> projectWorkstreams = projectToWorkstreams.get(projectId);
        if (projectWorkstreams != null) {
            projectWorkstreams.remove(workstreamId);
            if (projectWorkstreams.isEmpty()) {
                projectToWorkstreams.remove(projectId);
            }
        }
        
        return removed;
    }
    
    @Override
    public List<UUID> findProjectIdsByWorkstreamId(UUID workstreamId) {
        return new ArrayList<>(workstreamToProjects.getOrDefault(workstreamId, Collections.emptySet()));
    }
    
    /**
     * Clears all data from the repository (for testing).
     */
    public void clear() {
        workstreams.clear();
        workstreamToProjects.clear();
        projectToWorkstreams.clear();
        workstreamToWorkItems.clear();
        workItemToWorkstreams.clear();
    }
}