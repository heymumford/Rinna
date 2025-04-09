/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.Workstream;
import org.rinna.domain.model.WorkstreamCreateRequest;
import org.rinna.domain.model.WorkstreamRecord;
import org.rinna.domain.repository.WorkstreamRepository;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.WorkstreamService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of the WorkstreamService interface.
 * Provides business functionality for managing workstreams and their
 * relationships with projects and work items.
 */
public class DefaultWorkstreamService implements WorkstreamService {
    
    private final WorkstreamRepository workstreamRepository;
    private final ItemService itemService;
    
    /**
     * Creates a new DefaultWorkstreamService instance.
     *
     * @param workstreamRepository the repository for workstreams
     * @param itemService the service for work items
     */
    public DefaultWorkstreamService(WorkstreamRepository workstreamRepository, ItemService itemService) {
        this.workstreamRepository = workstreamRepository;
        this.itemService = itemService;
    }
    
    @Override
    public Workstream createWorkstream(WorkstreamCreateRequest request) {
        return workstreamRepository.create(request);
    }
    
    @Override
    public Workstream updateWorkstream(Workstream workstream) {
        // Verify the workstream exists
        if (!workstreamRepository.findById(workstream.getId()).isPresent()) {
            throw new IllegalArgumentException("Workstream not found: " + workstream.getId());
        }
        
        return workstreamRepository.save(workstream);
    }
    
    @Override
    public Optional<Workstream> findWorkstreamById(UUID id) {
        return workstreamRepository.findById(id);
    }
    
    @Override
    public List<Workstream> findAllWorkstreams() {
        return workstreamRepository.findAll();
    }
    
    @Override
    public List<Workstream> findWorkstreamsByStatus(String status) {
        return workstreamRepository.findByStatus(status);
    }
    
    @Override
    public List<Workstream> findWorkstreamsByOwner(String owner) {
        return workstreamRepository.findByOwner(owner);
    }
    
    @Override
    public List<Workstream> findWorkstreamsByPriority(Priority priority) {
        return workstreamRepository.findByPriority(priority);
    }
    
    @Override
    public List<Workstream> findWorkstreamsByCynefinDomain(CynefinDomain domain) {
        return workstreamRepository.findByCynefinDomain(domain);
    }
    
    @Override
    public List<Workstream> findCrossProjectWorkstreams() {
        return workstreamRepository.findCrossProjectWorkstreams();
    }
    
    @Override
    public boolean deleteWorkstream(UUID id) {
        Optional<Workstream> workstream = workstreamRepository.findById(id);
        if (!workstream.isPresent()) {
            return false;
        }
        
        workstreamRepository.deleteById(id);
        return true;
    }
    
    @Override
    public boolean associateProject(UUID workstreamId, UUID projectId) {
        if (!workstreamRepository.findById(workstreamId).isPresent()) {
            return false;
        }
        
        return workstreamRepository.associateProject(workstreamId, projectId);
    }
    
    @Override
    public int associateProjects(UUID workstreamId, Set<UUID> projectIds) {
        if (!workstreamRepository.findById(workstreamId).isPresent()) {
            return 0;
        }
        
        int successCount = 0;
        for (UUID projectId : projectIds) {
            boolean success = workstreamRepository.associateProject(workstreamId, projectId);
            if (success) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    @Override
    public boolean dissociateProject(UUID workstreamId, UUID projectId) {
        if (!workstreamRepository.findById(workstreamId).isPresent()) {
            return false;
        }
        
        return workstreamRepository.dissociateProject(workstreamId, projectId);
    }
    
    @Override
    public List<UUID> getProjectsForWorkstream(UUID workstreamId) {
        if (!workstreamRepository.findById(workstreamId).isPresent()) {
            return Collections.emptyList();
        }
        
        return workstreamRepository.findProjectIdsByWorkstreamId(workstreamId);
    }
    
    @Override
    public List<Workstream> getWorkstreamsForProject(UUID projectId) {
        return workstreamRepository.findByProject(projectId);
    }
    
    @Override
    public boolean associateWorkItem(UUID workstreamId, UUID workItemId) {
        if (!workstreamRepository.findById(workstreamId).isPresent()) {
            return false;
        }
        
        return workstreamRepository.associateWorkItem(workstreamId, workItemId);
    }
    
    @Override
    public int associateWorkItems(UUID workstreamId, Set<UUID> workItemIds) {
        if (!workstreamRepository.findById(workstreamId).isPresent()) {
            return 0;
        }
        
        int successCount = 0;
        for (UUID workItemId : workItemIds) {
            boolean success = workstreamRepository.associateWorkItem(workstreamId, workItemId);
            if (success) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    @Override
    public boolean dissociateWorkItem(UUID workstreamId, UUID workItemId) {
        if (!workstreamRepository.findById(workstreamId).isPresent()) {
            return false;
        }
        
        return workstreamRepository.dissociateWorkItem(workstreamId, workItemId);
    }
    
    @Override
    public List<UUID> getWorkItemsForWorkstream(UUID workstreamId) {
        if (!workstreamRepository.findById(workstreamId).isPresent()) {
            return Collections.emptyList();
        }
        
        return workstreamRepository.findWorkItemIdsByWorkstreamId(workstreamId);
    }
    
    @Override
    public List<Workstream> getWorkstreamsForWorkItem(UUID workItemId) {
        return workstreamRepository.findByWorkItem(workItemId);
    }
    
    @Override
    public Workstream updateWorkstreamStatus(UUID workstreamId, String status) {
        Optional<Workstream> optionalWorkstream = workstreamRepository.findById(workstreamId);
        if (!optionalWorkstream.isPresent()) {
            throw new IllegalArgumentException("Workstream not found: " + workstreamId);
        }
        
        Workstream workstream = optionalWorkstream.get();
        if (workstream instanceof WorkstreamRecord) {
            WorkstreamRecord record = (WorkstreamRecord) workstream;
            return workstreamRepository.save(record.withStatus(status));
        } else {
            throw new UnsupportedOperationException("Unsupported Workstream implementation: " + workstream.getClass().getName());
        }
    }
    
    @Override
    public Workstream updateWorkstreamPriority(UUID workstreamId, Priority priority) {
        Optional<Workstream> optionalWorkstream = workstreamRepository.findById(workstreamId);
        if (!optionalWorkstream.isPresent()) {
            throw new IllegalArgumentException("Workstream not found: " + workstreamId);
        }
        
        Workstream workstream = optionalWorkstream.get();
        if (workstream instanceof WorkstreamRecord) {
            WorkstreamRecord record = (WorkstreamRecord) workstream;
            return workstreamRepository.save(record.withPriority(priority));
        } else {
            throw new UnsupportedOperationException("Unsupported Workstream implementation: " + workstream.getClass().getName());
        }
    }
    
    @Override
    public Workstream updateWorkstreamCynefinDomain(UUID workstreamId, CynefinDomain domain) {
        Optional<Workstream> optionalWorkstream = workstreamRepository.findById(workstreamId);
        if (!optionalWorkstream.isPresent()) {
            throw new IllegalArgumentException("Workstream not found: " + workstreamId);
        }
        
        Workstream workstream = optionalWorkstream.get();
        if (workstream instanceof WorkstreamRecord) {
            WorkstreamRecord record = (WorkstreamRecord) workstream;
            return workstreamRepository.save(record.withCynefinDomain(domain));
        } else {
            throw new UnsupportedOperationException("Unsupported Workstream implementation: " + workstream.getClass().getName());
        }
    }
    
    @Override
    public Workstream updateWorkstreamCrossProject(UUID workstreamId, boolean crossProject) {
        Optional<Workstream> optionalWorkstream = workstreamRepository.findById(workstreamId);
        if (!optionalWorkstream.isPresent()) {
            throw new IllegalArgumentException("Workstream not found: " + workstreamId);
        }
        
        Workstream workstream = optionalWorkstream.get();
        if (workstream instanceof WorkstreamRecord) {
            WorkstreamRecord record = (WorkstreamRecord) workstream;
            return workstreamRepository.save(record.withCrossProject(crossProject));
        } else {
            throw new UnsupportedOperationException("Unsupported Workstream implementation: " + workstream.getClass().getName());
        }
    }
    
    @Override
    public CynefinDomain suggestCynefinDomain(UUID workstreamId) {
        Optional<Workstream> optionalWorkstream = workstreamRepository.findById(workstreamId);
        if (!optionalWorkstream.isPresent()) {
            throw new IllegalArgumentException("Workstream not found: " + workstreamId);
        }
        
        List<UUID> projectIds = workstreamRepository.findProjectIdsByWorkstreamId(workstreamId);
        List<UUID> workItemIds = workstreamRepository.findWorkItemIdsByWorkstreamId(workstreamId);
        
        // An algorithm to suggest an appropriate CYNEFIN domain based on the workstream's context
        // This is a simplified implementation for demonstration purposes
        
        // If the workstream spans multiple projects, it's likely complex
        if (projectIds.size() > 1) {
            return CynefinDomain.COMPLEX;
        }
        
        // If the workstream has many work items, it might be complicated
        if (workItemIds.size() > 10) {
            return CynefinDomain.COMPLICATED;
        }
        
        // Default to obvious for simpler workstreams
        return CynefinDomain.OBVIOUS;
    }
    
    @Override
    public List<Workstream> suggestWorkstreamsForWorkItem(UUID workItemId) {
        // In a real implementation, we would use the ItemService to get details about the work item
        // and then use those details to find suitable workstreams
        
        // For now, we'll implement a simple algorithm that suggests workstreams based on:
        // 1. Workstreams that already contain similar work items
        // 2. Workstreams that match the work item's CYNEFIN domain
        // 3. Active workstreams
        
        // Get all workstreams and sort them by relevance
        List<Workstream> allWorkstreams = workstreamRepository.findAll();
        List<Workstream> workstreamsContainingItem = workstreamRepository.findByWorkItem(workItemId);
        
        // Create a list to hold suggested workstreams with their relevance score
        List<ScoredWorkstream> scoredWorkstreams = new ArrayList<>();
        
        for (Workstream workstream : allWorkstreams) {
            int score = 0;
            
            // Workstreams that already contain similar work items get a higher score
            List<UUID> workItemsInWorkstream = workstreamRepository.findWorkItemIdsByWorkstreamId(workstream.getId());
            score += workItemsInWorkstream.size();
            
            // Active workstreams get a higher score
            if ("ACTIVE".equalsIgnoreCase(workstream.getStatus())) {
                score += 5;
            }
            
            // Cross-project workstreams might be more suitable
            if (workstream.isCrossProject()) {
                score += 3;
            }
            
            scoredWorkstreams.add(new ScoredWorkstream(workstream, score));
        }
        
        // Sort by score (descending)
        scoredWorkstreams.sort(Comparator.comparing(ScoredWorkstream::getScore).reversed());
        
        // Extract workstreams from scored workstreams
        return scoredWorkstreams.stream()
                .map(ScoredWorkstream::getWorkstream)
                .collect(Collectors.toList());
    }
    
    /**
     * Helper class to associate a workstream with a relevance score.
     */
    private static class ScoredWorkstream {
        private final Workstream workstream;
        private final int score;
        
        ScoredWorkstream(Workstream workstream, int score) {
            this.workstream = workstream;
            this.score = score;
        }
        
        Workstream getWorkstream() {
            return workstream;
        }
        
        int getScore() {
            return score;
        }
    }
}