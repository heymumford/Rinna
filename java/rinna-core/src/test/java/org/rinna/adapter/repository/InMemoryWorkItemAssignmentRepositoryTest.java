/*
 * Test class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.service.WorkItemAssignmentRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the InMemoryWorkItemAssignmentRepository.
 */
public class InMemoryWorkItemAssignmentRepositoryTest {

    private InMemoryWorkItemAssignmentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryWorkItemAssignmentRepository();
        repository.clear();
    }

    @Test
    void testAssignWorkItem() {
        UUID unitId = UUID.randomUUID();
        String memberId = "alice";
        UUID workItemId = UUID.randomUUID();
        
        // Assign work item
        boolean result = repository.assignWorkItem(unitId, memberId, workItemId);
        assertTrue(result);
        
        // Verify assignment
        List<UUID> workItems = repository.findWorkItemsByMember(unitId, memberId);
        assertEquals(1, workItems.size());
        assertEquals(workItemId, workItems.get(0));
        
        String assignedMember = repository.findMemberByWorkItem(unitId, workItemId);
        assertEquals(memberId, assignedMember);
    }
    
    @Test
    void testAssignWorkItemAlreadyAssigned() {
        UUID unitId = UUID.randomUUID();
        String member1 = "alice";
        String member2 = "bob";
        UUID workItemId = UUID.randomUUID();
        
        // Assign work item to member1
        repository.assignWorkItem(unitId, member1, workItemId);
        
        // Assign same work item to member2
        boolean result = repository.assignWorkItem(unitId, member2, workItemId);
        assertTrue(result);
        
        // Verify assignment changed
        String assignedMember = repository.findMemberByWorkItem(unitId, workItemId);
        assertEquals(member2, assignedMember);
        
        // Verify member1 no longer has the work item
        List<UUID> member1WorkItems = repository.findWorkItemsByMember(unitId, member1);
        assertTrue(member1WorkItems.isEmpty());
        
        // Verify member2 has the work item
        List<UUID> member2WorkItems = repository.findWorkItemsByMember(unitId, member2);
        assertEquals(1, member2WorkItems.size());
        assertEquals(workItemId, member2WorkItems.get(0));
    }
    
    @Test
    void testUnassignWorkItem() {
        UUID unitId = UUID.randomUUID();
        String memberId = "alice";
        UUID workItemId = UUID.randomUUID();
        
        // Assign work item
        repository.assignWorkItem(unitId, memberId, workItemId);
        
        // Unassign work item
        boolean result = repository.unassignWorkItem(unitId, memberId, workItemId);
        assertTrue(result);
        
        // Verify unassignment
        List<UUID> workItems = repository.findWorkItemsByMember(unitId, memberId);
        assertTrue(workItems.isEmpty());
        
        String assignedMember = repository.findMemberByWorkItem(unitId, workItemId);
        assertNull(assignedMember);
    }
    
    @Test
    void testUnassignWorkItemNotAssigned() {
        UUID unitId = UUID.randomUUID();
        String memberId = "alice";
        UUID workItemId = UUID.randomUUID();
        
        // Unassign a work item that was never assigned
        boolean result = repository.unassignWorkItem(unitId, memberId, workItemId);
        assertFalse(result);
    }
    
    @Test
    void testFindWorkItemsByMember() {
        UUID unitId = UUID.randomUUID();
        String memberId = "alice";
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        
        // Assign multiple work items
        repository.assignWorkItem(unitId, memberId, workItem1);
        repository.assignWorkItem(unitId, memberId, workItem2);
        
        // Find work items
        List<UUID> workItems = repository.findWorkItemsByMember(unitId, memberId);
        assertEquals(2, workItems.size());
        assertTrue(workItems.contains(workItem1));
        assertTrue(workItems.contains(workItem2));
    }
    
    @Test
    void testFindMemberByWorkItem() {
        UUID unitId = UUID.randomUUID();
        String memberId = "alice";
        UUID workItemId = UUID.randomUUID();
        
        // Assign work item
        repository.assignWorkItem(unitId, memberId, workItemId);
        
        // Find member
        String assignedMember = repository.findMemberByWorkItem(unitId, workItemId);
        assertEquals(memberId, assignedMember);
    }
    
    @Test
    void testClearAssignments() {
        UUID unitId = UUID.randomUUID();
        String member1 = "alice";
        String member2 = "bob";
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        
        // Assign work items
        repository.assignWorkItem(unitId, member1, workItem1);
        repository.assignWorkItem(unitId, member2, workItem2);
        
        // Clear all assignments
        boolean result = repository.clearAssignments(unitId);
        assertTrue(result);
        
        // Verify all assignments are cleared
        List<UUID> member1WorkItems = repository.findWorkItemsByMember(unitId, member1);
        assertTrue(member1WorkItems.isEmpty());
        
        List<UUID> member2WorkItems = repository.findWorkItemsByMember(unitId, member2);
        assertTrue(member2WorkItems.isEmpty());
        
        String assignedMember1 = repository.findMemberByWorkItem(unitId, workItem1);
        assertNull(assignedMember1);
        
        String assignedMember2 = repository.findMemberByWorkItem(unitId, workItem2);
        assertNull(assignedMember2);
    }
    
    @Test
    void testClearMemberAssignments() {
        UUID unitId = UUID.randomUUID();
        String member1 = "alice";
        String member2 = "bob";
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        UUID workItem3 = UUID.randomUUID();
        
        // Assign work items
        repository.assignWorkItem(unitId, member1, workItem1);
        repository.assignWorkItem(unitId, member1, workItem2);
        repository.assignWorkItem(unitId, member2, workItem3);
        
        // Clear member1's assignments
        boolean result = repository.clearMemberAssignments(unitId, member1);
        assertTrue(result);
        
        // Verify member1's assignments are cleared
        List<UUID> member1WorkItems = repository.findWorkItemsByMember(unitId, member1);
        assertTrue(member1WorkItems.isEmpty());
        
        // Verify member2's assignments are intact
        List<UUID> member2WorkItems = repository.findWorkItemsByMember(unitId, member2);
        assertEquals(1, member2WorkItems.size());
        assertEquals(workItem3, member2WorkItems.get(0));
    }
    
    @Test
    void testFindAssignedMembers() {
        UUID unitId = UUID.randomUUID();
        String member1 = "alice";
        String member2 = "bob";
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        
        // Assign work items
        repository.assignWorkItem(unitId, member1, workItem1);
        repository.assignWorkItem(unitId, member2, workItem2);
        
        // Find assigned members
        List<String> members = repository.findAssignedMembers(unitId);
        assertEquals(2, members.size());
        assertTrue(members.contains(member1));
        assertTrue(members.contains(member2));
    }
    
    @Test
    void testFindAssignedWorkItems() {
        UUID unitId = UUID.randomUUID();
        String member1 = "alice";
        String member2 = "bob";
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        
        // Assign work items
        repository.assignWorkItem(unitId, member1, workItem1);
        repository.assignWorkItem(unitId, member2, workItem2);
        
        // Find assigned work items
        List<UUID> workItems = repository.findAssignedWorkItems(unitId);
        assertEquals(2, workItems.size());
        assertTrue(workItems.contains(workItem1));
        assertTrue(workItems.contains(workItem2));
    }
    
    @Test
    void testIsWorkItemAssigned() {
        UUID unitId = UUID.randomUUID();
        String memberId = "alice";
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        
        // Assign one work item
        repository.assignWorkItem(unitId, memberId, workItem1);
        
        // Check if work items are assigned
        assertTrue(repository.isWorkItemAssigned(unitId, workItem1));
        assertFalse(repository.isWorkItemAssigned(unitId, workItem2));
    }
    
    @Test
    void testGetAssignmentCount() {
        UUID unitId = UUID.randomUUID();
        String memberId = "alice";
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        
        // Initial count should be 0
        assertEquals(0, repository.getAssignmentCount(unitId, memberId));
        
        // Assign work items
        repository.assignWorkItem(unitId, memberId, workItem1);
        assertEquals(1, repository.getAssignmentCount(unitId, memberId));
        
        repository.assignWorkItem(unitId, memberId, workItem2);
        assertEquals(2, repository.getAssignmentCount(unitId, memberId));
        
        // Unassign a work item
        repository.unassignWorkItem(unitId, memberId, workItem1);
        assertEquals(1, repository.getAssignmentCount(unitId, memberId));
    }
    
    @Test
    void testGetMemberAssignmentCounts() {
        UUID unitId = UUID.randomUUID();
        String member1 = "alice";
        String member2 = "bob";
        
        // Assign work items
        repository.assignWorkItem(unitId, member1, UUID.randomUUID());
        repository.assignWorkItem(unitId, member1, UUID.randomUUID());
        repository.assignWorkItem(unitId, member2, UUID.randomUUID());
        
        // Get assignment counts
        Map<String, Integer> counts = repository.getMemberAssignmentCounts(unitId);
        assertEquals(2, counts.size());
        assertEquals(2, counts.get(member1));
        assertEquals(1, counts.get(member2));
    }
    
    @Test
    void testGetTopAssignedMembers() {
        UUID unitId = UUID.randomUUID();
        String member1 = "alice";
        String member2 = "bob";
        String member3 = "charlie";
        
        // Assign work items
        repository.assignWorkItem(unitId, member1, UUID.randomUUID());
        repository.assignWorkItem(unitId, member1, UUID.randomUUID());
        repository.assignWorkItem(unitId, member2, UUID.randomUUID());
        repository.assignWorkItem(unitId, member3, UUID.randomUUID());
        repository.assignWorkItem(unitId, member3, UUID.randomUUID());
        repository.assignWorkItem(unitId, member3, UUID.randomUUID());
        
        // Get top 2 assigned members
        Map<String, Integer> topMembers = repository.getTopAssignedMembers(unitId, 2);
        assertEquals(2, topMembers.size());
        assertTrue(topMembers.containsKey(member3));
        assertTrue(topMembers.containsKey(member1));
        assertFalse(topMembers.containsKey(member2));
        assertEquals(3, topMembers.get(member3));
        assertEquals(2, topMembers.get(member1));
    }
    
    @Test
    void testMultipleOrganizationalUnits() {
        UUID unit1 = UUID.randomUUID();
        UUID unit2 = UUID.randomUUID();
        String memberId = "alice";
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        
        // Assign work items to different units
        repository.assignWorkItem(unit1, memberId, workItem1);
        repository.assignWorkItem(unit2, memberId, workItem2);
        
        // Verify assignments are separate
        List<UUID> unit1WorkItems = repository.findWorkItemsByMember(unit1, memberId);
        assertEquals(1, unit1WorkItems.size());
        assertEquals(workItem1, unit1WorkItems.get(0));
        
        List<UUID> unit2WorkItems = repository.findWorkItemsByMember(unit2, memberId);
        assertEquals(1, unit2WorkItems.size());
        assertEquals(workItem2, unit2WorkItems.get(0));
    }
}