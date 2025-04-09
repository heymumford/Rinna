/*
 * Repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing work item assignments to members within organizational units.
 * This is a key component for the individual-level cognitive load tracking in the Ryorin-do framework.
 */
public interface WorkItemAssignmentRepository {
    
    /**
     * Assigns a work item to a specific member within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @param memberId the member ID
     * @param workItemId the work item ID
     * @return true if the assignment was successful
     */
    boolean assignWorkItem(UUID unitId, String memberId, UUID workItemId);
    
    /**
     * Unassigns a work item from a specific member within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @param memberId the member ID
     * @param workItemId the work item ID
     * @return true if the unassignment was successful
     */
    boolean unassignWorkItem(UUID unitId, String memberId, UUID workItemId);
    
    /**
     * Finds all work items assigned to a specific member within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @param memberId the member ID
     * @return a list of assigned work item IDs
     */
    List<UUID> findWorkItemsByMember(UUID unitId, String memberId);
    
    /**
     * Finds the member assigned to a specific work item within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @param workItemId the work item ID
     * @return the member ID, or null if not assigned
     */
    String findMemberByWorkItem(UUID unitId, UUID workItemId);
    
    /**
     * Clears all assignments for a specific organizational unit.
     *
     * @param unitId the organizational unit ID
     * @return true if successful
     */
    boolean clearAssignments(UUID unitId);
    
    /**
     * Clears all assignments for a specific member within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @param memberId the member ID
     * @return true if successful
     */
    boolean clearMemberAssignments(UUID unitId, String memberId);
    
    /**
     * Finds all members with assigned work items within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @return a list of member IDs
     */
    List<String> findAssignedMembers(UUID unitId);
    
    /**
     * Finds all work items that are assigned to any member within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @return a list of work item IDs
     */
    List<UUID> findAssignedWorkItems(UUID unitId);
    
    /**
     * Checks if a work item is assigned to any member within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @param workItemId the work item ID
     * @return true if the work item is assigned to any member
     */
    boolean isWorkItemAssigned(UUID unitId, UUID workItemId);
    
    /**
     * Gets the number of work items assigned to a specific member within an organizational unit.
     *
     * @param unitId the organizational unit ID
     * @param memberId the member ID
     * @return the number of assigned work items
     */
    int getAssignmentCount(UUID unitId, String memberId);
}