/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.service.WorkItemAssignmentRepository;

/**
 * In-memory implementation of the WorkItemAssignmentRepository interface.
 * This repository stores work item assignments for the Ryorin-do framework.
 */
public class InMemoryWorkItemAssignmentRepository implements WorkItemAssignmentRepository {

    // Maps organizational unit ID to a map of member IDs to sets of work item IDs
    private final Map<UUID, Map<String, Set<UUID>>> unitMemberAssignments = new ConcurrentHashMap<>();
    
    // Maps organizational unit ID to a map of work item IDs to member IDs
    private final Map<UUID, Map<UUID, String>> unitWorkItemAssignments = new ConcurrentHashMap<>();
    
    @Override
    public boolean assignWorkItem(UUID unitId, String memberId, UUID workItemId) {
        if (unitId == null || memberId == null || workItemId == null) {
            return false;
        }
        
        // First, check if this work item is already assigned to another member
        String currentAssignee = findMemberByWorkItem(unitId, workItemId);
        if (currentAssignee != null && !currentAssignee.equals(memberId)) {
            // Unassign from the current assignee first
            unassignWorkItem(unitId, currentAssignee, workItemId);
        }
        
        // Add to member -> work items mapping
        unitMemberAssignments
                .computeIfAbsent(unitId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(memberId, k -> new HashSet<>())
                .add(workItemId);
        
        // Add to work item -> member mapping
        unitWorkItemAssignments
                .computeIfAbsent(unitId, k -> new ConcurrentHashMap<>())
                .put(workItemId, memberId);
        
        return true;
    }
    
    @Override
    public boolean unassignWorkItem(UUID unitId, String memberId, UUID workItemId) {
        if (unitId == null || memberId == null || workItemId == null) {
            return false;
        }
        
        boolean removed = false;
        
        // Remove from member -> work items mapping
        Map<String, Set<UUID>> memberMap = unitMemberAssignments.get(unitId);
        if (memberMap != null) {
            Set<UUID> workItems = memberMap.get(memberId);
            if (workItems != null) {
                removed = workItems.remove(workItemId);
                
                // Clean up empty sets
                if (workItems.isEmpty()) {
                    memberMap.remove(memberId);
                    
                    // Clean up empty maps
                    if (memberMap.isEmpty()) {
                        unitMemberAssignments.remove(unitId);
                    }
                }
            }
        }
        
        // Remove from work item -> member mapping
        Map<UUID, String> workItemMap = unitWorkItemAssignments.get(unitId);
        if (workItemMap != null) {
            String assignedMember = workItemMap.get(workItemId);
            if (assignedMember != null && assignedMember.equals(memberId)) {
                workItemMap.remove(workItemId);
                removed = true;
                
                // Clean up empty maps
                if (workItemMap.isEmpty()) {
                    unitWorkItemAssignments.remove(unitId);
                }
            }
        }
        
        return removed;
    }
    
    @Override
    public List<UUID> findWorkItemsByMember(UUID unitId, String memberId) {
        if (unitId == null || memberId == null) {
            return Collections.emptyList();
        }
        
        Map<String, Set<UUID>> memberMap = unitMemberAssignments.get(unitId);
        if (memberMap == null) {
            return Collections.emptyList();
        }
        
        Set<UUID> workItems = memberMap.get(memberId);
        if (workItems == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(workItems);
    }
    
    @Override
    public String findMemberByWorkItem(UUID unitId, UUID workItemId) {
        if (unitId == null || workItemId == null) {
            return null;
        }
        
        Map<UUID, String> workItemMap = unitWorkItemAssignments.get(unitId);
        if (workItemMap == null) {
            return null;
        }
        
        return workItemMap.get(workItemId);
    }
    
    @Override
    public boolean clearAssignments(UUID unitId) {
        if (unitId == null) {
            return false;
        }
        
        unitMemberAssignments.remove(unitId);
        unitWorkItemAssignments.remove(unitId);
        
        return true;
    }
    
    @Override
    public boolean clearMemberAssignments(UUID unitId, String memberId) {
        if (unitId == null || memberId == null) {
            return false;
        }
        
        // Get the work items assigned to this member
        List<UUID> workItems = findWorkItemsByMember(unitId, memberId);
        
        // Remove member from member map
        Map<String, Set<UUID>> memberMap = unitMemberAssignments.get(unitId);
        if (memberMap != null) {
            memberMap.remove(memberId);
            
            // Clean up empty maps
            if (memberMap.isEmpty()) {
                unitMemberAssignments.remove(unitId);
            }
        }
        
        // Remove all work item -> member mappings for this member
        Map<UUID, String> workItemMap = unitWorkItemAssignments.get(unitId);
        if (workItemMap != null && !workItems.isEmpty()) {
            for (UUID workItemId : workItems) {
                String assignedMember = workItemMap.get(workItemId);
                if (assignedMember != null && assignedMember.equals(memberId)) {
                    workItemMap.remove(workItemId);
                }
            }
            
            // Clean up empty maps
            if (workItemMap.isEmpty()) {
                unitWorkItemAssignments.remove(unitId);
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> findAssignedMembers(UUID unitId) {
        if (unitId == null) {
            return Collections.emptyList();
        }
        
        Map<String, Set<UUID>> memberMap = unitMemberAssignments.get(unitId);
        if (memberMap == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(memberMap.keySet());
    }
    
    @Override
    public List<UUID> findAssignedWorkItems(UUID unitId) {
        if (unitId == null) {
            return Collections.emptyList();
        }
        
        Map<UUID, String> workItemMap = unitWorkItemAssignments.get(unitId);
        if (workItemMap == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(workItemMap.keySet());
    }
    
    @Override
    public boolean isWorkItemAssigned(UUID unitId, UUID workItemId) {
        if (unitId == null || workItemId == null) {
            return false;
        }
        
        Map<UUID, String> workItemMap = unitWorkItemAssignments.get(unitId);
        if (workItemMap == null) {
            return false;
        }
        
        return workItemMap.containsKey(workItemId);
    }
    
    @Override
    public int getAssignmentCount(UUID unitId, String memberId) {
        if (unitId == null || memberId == null) {
            return 0;
        }
        
        Map<String, Set<UUID>> memberMap = unitMemberAssignments.get(unitId);
        if (memberMap == null) {
            return 0;
        }
        
        Set<UUID> workItems = memberMap.get(memberId);
        if (workItems == null) {
            return 0;
        }
        
        return workItems.size();
    }
    
    /**
     * Clears all assignments (for testing purposes).
     */
    public void clear() {
        unitMemberAssignments.clear();
        unitWorkItemAssignments.clear();
    }
    
    /**
     * Gets a map of member IDs to their assigned work item counts for a specific organizational unit.
     * This is useful for reporting and load balancing.
     *
     * @param unitId the organizational unit ID
     * @return a map of member IDs to assignment counts
     */
    public Map<String, Integer> getMemberAssignmentCounts(UUID unitId) {
        if (unitId == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Set<UUID>> memberMap = unitMemberAssignments.get(unitId);
        if (memberMap == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : memberMap.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        
        return counts;
    }
    
    /**
     * Gets the members with the highest number of assignments for a specific organizational unit.
     * This is useful for identifying potential bottlenecks.
     *
     * @param unitId the organizational unit ID
     * @param limit the maximum number of members to return
     * @return a map of member IDs to assignment counts, sorted by count (descending)
     */
    public Map<String, Integer> getTopAssignedMembers(UUID unitId, int limit) {
        Map<String, Integer> counts = getMemberAssignmentCounts(unitId);
        
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        HashMap::new
                ));
    }
}