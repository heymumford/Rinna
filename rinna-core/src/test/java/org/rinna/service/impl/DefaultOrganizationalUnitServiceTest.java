/*
 * Test class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.adapter.repository.InMemoryOrganizationalUnitRepository;
import org.rinna.adapter.service.DefaultOrganizationalUnitService;
import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.OrganizationalUnitCreateRequest;
import org.rinna.domain.model.OrganizationalUnitRecord;
import org.rinna.domain.model.OrganizationalUnitType;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.OrganizationalUnitService;
import org.rinna.domain.service.WorkstreamService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for the DefaultOrganizationalUnitService.
 */
public class DefaultOrganizationalUnitServiceTest {

    private InMemoryOrganizationalUnitRepository repository;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private WorkstreamService workstreamService;
    
    private OrganizationalUnitService service;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new InMemoryOrganizationalUnitRepository();
        repository.clear();
        service = new DefaultOrganizationalUnitService(repository, itemService, workstreamService);
    }
    
    @Test
    void testCreateAndFindOrganizationalUnit() {
        // Create a test request
        OrganizationalUnitCreateRequest request = OrganizationalUnitCreateRequest.builder()
                .name("Engineering Team")
                .description("Core engineering team")
                .type(OrganizationalUnitType.TEAM)
                .owner("John Smith")
                .cognitiveCapacity(100)
                .build();
        
        // Create the organizational unit
        OrganizationalUnit unit = service.createOrganizationalUnit(request);
        assertNotNull(unit);
        assertNotNull(unit.getId());
        assertEquals("Engineering Team", unit.getName());
        
        // Find the organizational unit by ID
        Optional<OrganizationalUnit> found = service.findOrganizationalUnitById(unit.getId());
        assertTrue(found.isPresent());
        assertEquals(unit.getId(), found.get().getId());
    }
    
    @Test
    void testUpdateOrganizationalUnit() {
        // Create an organizational unit
        OrganizationalUnit unit = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Original Name")
                        .description("Original description")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("John Smith")
                        .build()
        );
        
        // Update the organizational unit
        OrganizationalUnit updated = service.updateOrganizationalUnit(
                ((OrganizationalUnitRecord) unit).withName("Updated Name")
        );
        
        assertEquals("Updated Name", updated.getName());
        
        // Verify the update was persisted
        Optional<OrganizationalUnit> found = service.findOrganizationalUnitById(unit.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
    }
    
    @Test
    void testFindAllOrganizationalUnits() {
        // Create multiple organizational units
        OrganizationalUnit unit1 = service.createOrganizationalUnit(createTestRequest("Team 1", OrganizationalUnitType.TEAM));
        OrganizationalUnit unit2 = service.createOrganizationalUnit(createTestRequest("Department 1", OrganizationalUnitType.DEPARTMENT));
        
        // Find all organizational units
        List<OrganizationalUnit> units = service.findAllOrganizationalUnits();
        assertEquals(2, units.size());
        assertTrue(units.stream().anyMatch(u -> u.getId().equals(unit1.getId())));
        assertTrue(units.stream().anyMatch(u -> u.getId().equals(unit2.getId())));
    }
    
    @Test
    void testFindOrganizationalUnitsByType() {
        // Create multiple organizational units with different types
        OrganizationalUnit team = service.createOrganizationalUnit(createTestRequest("Team", OrganizationalUnitType.TEAM));
        OrganizationalUnit dept = service.createOrganizationalUnit(createTestRequest("Department", OrganizationalUnitType.DEPARTMENT));
        
        // Find by type
        List<OrganizationalUnit> teams = service.findOrganizationalUnitsByType(OrganizationalUnitType.TEAM);
        assertEquals(1, teams.size());
        assertEquals(team.getId(), teams.get(0).getId());
        
        List<OrganizationalUnit> departments = service.findOrganizationalUnitsByType(OrganizationalUnitType.DEPARTMENT);
        assertEquals(1, departments.size());
        assertEquals(dept.getId(), departments.get(0).getId());
    }
    
    @Test
    void testFindOrganizationalUnitsByParent() {
        // Create a parent unit
        OrganizationalUnit parent = service.createOrganizationalUnit(createTestRequest("Parent", OrganizationalUnitType.DEPARTMENT));
        
        // Create child units
        OrganizationalUnit child1 = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Child 1")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("John Smith")
                        .parentId(parent.getId())
                        .build()
        );
        
        OrganizationalUnit child2 = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Child 2")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("Jane Doe")
                        .parentId(parent.getId())
                        .build()
        );
        
        // Find by parent
        List<OrganizationalUnit> children = service.findOrganizationalUnitsByParent(parent.getId());
        assertEquals(2, children.size());
        assertTrue(children.stream().anyMatch(u -> u.getId().equals(child1.getId())));
        assertTrue(children.stream().anyMatch(u -> u.getId().equals(child2.getId())));
    }
    
    @Test
    void testDeleteOrganizationalUnit() {
        // Create an organizational unit
        OrganizationalUnit unit = service.createOrganizationalUnit(createTestRequest("Test Unit", OrganizationalUnitType.TEAM));
        
        // Verify it exists
        assertTrue(service.findOrganizationalUnitById(unit.getId()).isPresent());
        
        // Delete it
        boolean deleted = service.deleteOrganizationalUnit(unit.getId());
        assertTrue(deleted);
        
        // Verify it's gone
        assertFalse(service.findOrganizationalUnitById(unit.getId()).isPresent());
    }
    
    @Test
    void testGetOrganizationalHierarchy() {
        // Create a hierarchy
        OrganizationalUnit root = service.createOrganizationalUnit(createTestRequest("Root", OrganizationalUnitType.DEPARTMENT));
        
        OrganizationalUnit child1 = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Child 1")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("John Smith")
                        .parentId(root.getId())
                        .build()
        );
        
        OrganizationalUnit child2 = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Child 2")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("Jane Doe")
                        .parentId(root.getId())
                        .build()
        );
        
        OrganizationalUnit grandchild = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Grandchild")
                        .type(OrganizationalUnitType.SQUAD)
                        .owner("Bob Smith")
                        .parentId(child1.getId())
                        .build()
        );
        
        // Get the hierarchy
        OrganizationalUnitService.OrganizationalHierarchy hierarchy = service.getOrganizationalHierarchy(root.getId());
        
        // Verify the hierarchy
        assertNotNull(hierarchy);
        assertEquals(root.getId(), hierarchy.getUnit().getId());
        assertEquals(2, hierarchy.getChildren().size());
        
        OrganizationalUnitService.OrganizationalHierarchy child1Hierarchy = hierarchy.getChildren().stream()
                .filter(h -> h.getUnit().getId().equals(child1.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(child1Hierarchy);
        assertEquals(1, child1Hierarchy.getChildren().size());
        assertEquals(grandchild.getId(), child1Hierarchy.getChildren().get(0).getUnit().getId());
    }
    
    @Test
    void testWorkItemAssociations() {
        // Create a unit
        OrganizationalUnit unit = service.createOrganizationalUnit(createTestRequest("Test Unit", OrganizationalUnitType.TEAM));
        
        // Create mock work items
        UUID workItem1Id = UUID.randomUUID();
        UUID workItem2Id = UUID.randomUUID();
        
        WorkItem workItem1 = mock(WorkItem.class);
        when(workItem1.getId()).thenReturn(workItem1Id);
        when(itemService.findById(workItem1Id)).thenReturn(Optional.of(workItem1));
        
        WorkItem workItem2 = mock(WorkItem.class);
        when(workItem2.getId()).thenReturn(workItem2Id);
        when(itemService.findById(workItem2Id)).thenReturn(Optional.of(workItem2));
        
        // Assign work items
        assertTrue(service.assignWorkItem(unit.getId(), workItem1Id));
        assertTrue(service.assignWorkItem(unit.getId(), workItem2Id));
        
        // Find work items by unit
        List<WorkItem> workItems = service.findWorkItemsByOrganizationalUnit(unit.getId());
        assertEquals(2, workItems.size());
        assertTrue(workItems.contains(workItem1));
        assertTrue(workItems.contains(workItem2));
        
        // Unassign a work item
        assertTrue(service.unassignWorkItem(unit.getId(), workItem1Id));
        
        // Verify the unassignment
        workItems = service.findWorkItemsByOrganizationalUnit(unit.getId());
        assertEquals(1, workItems.size());
        assertTrue(workItems.contains(workItem2));
    }
    
    @Test
    void testCognitiveLoadCalculation() {
        // Create a unit
        OrganizationalUnit unit = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Test Unit")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("John Smith")
                        .cognitiveCapacity(100)
                        .build()
        );
        
        // Create mock work items with different types and priorities
        UUID workItem1Id = UUID.randomUUID();
        UUID workItem2Id = UUID.randomUUID();
        
        WorkItem workItem1 = mock(WorkItem.class);
        when(workItem1.getId()).thenReturn(workItem1Id);
        when(workItem1.getType()).thenReturn(WorkItemType.FEATURE);
        when(workItem1.getPriority()).thenReturn(Priority.HIGH);
        when(itemService.findById(workItem1Id)).thenReturn(Optional.of(workItem1));
        
        WorkItem workItem2 = mock(WorkItem.class);
        when(workItem2.getId()).thenReturn(workItem2Id);
        when(workItem2.getType()).thenReturn(WorkItemType.BUG);
        when(workItem2.getPriority()).thenReturn(Priority.CRITICAL);
        when(itemService.findById(workItem2Id)).thenReturn(Optional.of(workItem2));
        
        // Assign work items
        service.assignWorkItem(unit.getId(), workItem1Id);
        service.assignWorkItem(unit.getId(), workItem2Id);
        
        // Update cognitive load calculation
        OrganizationalUnit updated = service.updateCognitiveLoadCalculation(unit.getId());
        
        // The cognitive load should be the sum of the loads of the two work items
        // For a FEATURE with HIGH priority: 5 (base) + 10 (feature) + 5 (high) = 20
        // For a BUG with CRITICAL priority: 5 (base) + 5 (bug) + 10 (critical) = 20
        // Total: 40
        assertEquals(40, updated.getCurrentCognitiveLoad());
        
        // Test cognitive impact calculation
        UUID newWorkItemId = UUID.randomUUID();
        WorkItem newWorkItem = mock(WorkItem.class);
        when(newWorkItem.getId()).thenReturn(newWorkItemId);
        when(newWorkItem.getType()).thenReturn(WorkItemType.EPIC);
        when(newWorkItem.getPriority()).thenReturn(Priority.MEDIUM);
        when(itemService.findById(newWorkItemId)).thenReturn(Optional.of(newWorkItem));
        
        // For an EPIC with MEDIUM priority: 5 (base) + 20 (epic) + 3 (medium) = 28
        // Current load + new item load = 40 + 28 = 68
        int impact = service.calculateCognitiveImpact(unit.getId(), newWorkItemId);
        assertEquals(68, impact);
    }
    
    @Test
    void testFindUnitsWithAvailableCapacity() {
        // Create units with different capacities and loads
        OrganizationalUnit unit1 = service.createOrganizationalUnit(createTestRequestWithCapacity("Unit 1", 100));
        OrganizationalUnit unit2 = service.createOrganizationalUnit(createTestRequestWithCapacity("Unit 2", 50));
        
        // Set cognitive loads
        repository.updateCognitiveLoad(unit1.getId(), 70);
        repository.updateCognitiveLoad(unit2.getId(), 40);
        
        // Find units with available capacity >= 30
        List<OrganizationalUnit> units = service.findUnitsWithAvailableCapacity(30);
        assertEquals(1, units.size());
        assertEquals(unit1.getId(), units.get(0).getId());
        
        // Find units with available capacity >= 10
        units = service.findUnitsWithAvailableCapacity(10);
        assertEquals(2, units.size());
    }
    
    @Test
    void testFindOverloadedUnits() {
        // Create units with different capacities and loads
        OrganizationalUnit unit1 = service.createOrganizationalUnit(createTestRequestWithCapacity("Unit 1", 100));
        OrganizationalUnit unit2 = service.createOrganizationalUnit(createTestRequestWithCapacity("Unit 2", 50));
        
        // Set cognitive loads
        repository.updateCognitiveLoad(unit1.getId(), 70);
        repository.updateCognitiveLoad(unit2.getId(), 40);
        
        // Find units at or above 75% capacity
        List<OrganizationalUnit> units = service.findOverloadedUnits(75);
        assertEquals(1, units.size());
        assertEquals(unit2.getId(), units.get(0).getId());
        
        // Find units at or above 60% capacity
        units = service.findOverloadedUnits(60);
        assertEquals(2, units.size());
    }
    
    @Test
    void testSuggestUnitsForWorkItem() {
        // Create units with different domain expertise and work paradigms
        OrganizationalUnit unit1 = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Unit 1")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("John Smith")
                        .cognitiveCapacity(100)
                        .addDomainExpertise(CynefinDomain.COMPLICATED)
                        .addWorkParadigm(WorkParadigm.ENGINEERING)
                        .build()
        );
        
        OrganizationalUnit unit2 = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Unit 2")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("Jane Doe")
                        .cognitiveCapacity(100)
                        .addDomainExpertise(CynefinDomain.COMPLEX)
                        .addWorkParadigm(WorkParadigm.PRODUCT)
                        .build()
        );
        
        // Create a mock work item that matches unit1's expertise
        UUID workItemId = UUID.randomUUID();
        WorkItem workItem = mock(WorkItem.class);
        when(workItem.getId()).thenReturn(workItemId);
        when(workItem.getType()).thenReturn(WorkItemType.FEATURE);
        when(workItem.getPriority()).thenReturn(Priority.HIGH);
        when(workItem.getCynefinDomain()).thenReturn(Optional.of(CynefinDomain.COMPLICATED));
        when(workItem.getWorkParadigm()).thenReturn(Optional.of(WorkParadigm.ENGINEERING));
        when(workItem.getProjectId()).thenReturn(UUID.randomUUID());
        when(itemService.findById(workItemId)).thenReturn(Optional.of(workItem));
        
        // Get suggestions
        List<OrganizationalUnit> suggestions = service.suggestUnitsForWorkItem(workItemId);
        
        // Unit1 should be the top suggestion
        assertEquals(2, suggestions.size());
        assertEquals(unit1.getId(), suggestions.get(0).getId());
    }
    
    @Test
    void testModifyingOrganizationalUnitAttributes() {
        // Create a unit
        OrganizationalUnit unit = service.createOrganizationalUnit(
                OrganizationalUnitCreateRequest.builder()
                        .name("Test Unit")
                        .type(OrganizationalUnitType.TEAM)
                        .owner("John Smith")
                        .build()
        );
        
        // Add domain expertise
        OrganizationalUnit updated = service.addDomainExpertise(unit.getId(), CynefinDomain.COMPLICATED);
        assertEquals(1, updated.getDomainExpertise().size());
        assertTrue(updated.getDomainExpertise().contains(CynefinDomain.COMPLICATED));
        
        // Add work paradigm
        updated = service.addWorkParadigm(unit.getId(), WorkParadigm.ENGINEERING);
        assertEquals(1, updated.getWorkParadigms().size());
        assertTrue(updated.getWorkParadigms().contains(WorkParadigm.ENGINEERING));
        
        // Add member
        updated = service.addMember(unit.getId(), "Alice");
        assertEquals(1, updated.getMembers().size());
        assertTrue(updated.getMembers().contains("Alice"));
        
        // Add another member
        updated = service.addMember(unit.getId(), "Bob");
        assertEquals(2, updated.getMembers().size());
        assertTrue(updated.getMembers().contains("Bob"));
        
        // Remove member
        updated = service.removeMember(unit.getId(), "Alice");
        assertEquals(1, updated.getMembers().size());
        assertFalse(updated.getMembers().contains("Alice"));
        assertTrue(updated.getMembers().contains("Bob"));
    }
    
    @Test
    void testWorkItemOwnership() {
        // Create a unit
        OrganizationalUnit unit = service.createOrganizationalUnit(createTestRequest("Test Unit", OrganizationalUnitType.TEAM));
        
        // Create a mock work item
        UUID workItemId = UUID.randomUUID();
        
        // Set ownership
        boolean result = service.setAsOwningUnit(unit.getId(), workItemId);
        assertTrue(result);
        
        // Find owning unit
        Optional<OrganizationalUnit> owningUnit = service.findOwningUnit(workItemId);
        assertTrue(owningUnit.isPresent());
        assertEquals(unit.getId(), owningUnit.get().getId());
    }
    
    // Helper methods to create test requests
    private OrganizationalUnitCreateRequest createTestRequest(String name, OrganizationalUnitType type) {
        return OrganizationalUnitCreateRequest.builder()
                .name(name)
                .description("Description for " + name)
                .type(type)
                .owner("Default Owner")
                .cognitiveCapacity(100)
                .build();
    }
    
    private OrganizationalUnitCreateRequest createTestRequestWithCapacity(String name, int capacity) {
        return OrganizationalUnitCreateRequest.builder()
                .name(name)
                .description("Description for " + name)
                .type(OrganizationalUnitType.TEAM)
                .owner("Default Owner")
                .cognitiveCapacity(capacity)
                .build();
    }
}