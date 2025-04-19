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
import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.OrganizationalUnitCreateRequest;
import org.rinna.domain.model.OrganizationalUnitType;
import org.rinna.domain.model.WorkParadigm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the InMemoryOrganizationalUnitRepository.
 */
public class InMemoryOrganizationalUnitRepositoryTest {

    private InMemoryOrganizationalUnitRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrganizationalUnitRepository();
        repository.clear();
    }

    @Test
    void testCreateAndFindById() {
        // Create a new organizational unit
        OrganizationalUnitCreateRequest request = OrganizationalUnitCreateRequest.builder()
                .name("Engineering Team")
                .description("Core engineering team")
                .type(OrganizationalUnitType.TEAM)
                .owner("John Smith")
                .cognitiveCapacity(100)
                .addMember("Alice")
                .addMember("Bob")
                .addDomainExpertise(CynefinDomain.COMPLICATED)
                .addDomainExpertise(CynefinDomain.COMPLEX)
                .addWorkParadigm(WorkParadigm.ENGINEERING)
                .addTag("backend")
                .build();

        OrganizationalUnit created = repository.create(request);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Engineering Team", created.getName());
        assertEquals("Core engineering team", created.getDescription());
        assertEquals(OrganizationalUnitType.TEAM, created.getType());
        assertEquals("John Smith", created.getOwner());
        assertEquals(100, created.getCognitiveCapacity());
        assertEquals(0, created.getCurrentCognitiveLoad());
        assertEquals(2, created.getMembers().size());
        assertTrue(created.getMembers().contains("Alice"));
        assertTrue(created.getMembers().contains("Bob"));
        assertEquals(2, created.getDomainExpertise().size());
        assertTrue(created.getDomainExpertise().contains(CynefinDomain.COMPLICATED));
        assertTrue(created.getDomainExpertise().contains(CynefinDomain.COMPLEX));
        assertEquals(1, created.getWorkParadigms().size());
        assertTrue(created.getWorkParadigms().contains(WorkParadigm.ENGINEERING));
        assertEquals(1, created.getTags().size());
        assertTrue(created.getTags().contains("backend"));
        assertTrue(created.isActive());

        // Test findById
        Optional<OrganizationalUnit> found = repository.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    void testFindAll() {
        // Create multiple organizational units
        OrganizationalUnit unit1 = repository.create(createTestRequest("Team 1", OrganizationalUnitType.TEAM));
        OrganizationalUnit unit2 = repository.create(createTestRequest("Department 1", OrganizationalUnitType.DEPARTMENT));
        OrganizationalUnit unit3 = repository.create(createTestRequest("Squad 1", OrganizationalUnitType.SQUAD));

        List<OrganizationalUnit> allUnits = repository.findAll();
        assertEquals(3, allUnits.size());
        assertTrue(allUnits.stream().anyMatch(u -> u.getId().equals(unit1.getId())));
        assertTrue(allUnits.stream().anyMatch(u -> u.getId().equals(unit2.getId())));
        assertTrue(allUnits.stream().anyMatch(u -> u.getId().equals(unit3.getId())));
    }

    @Test
    void testFindByType() {
        // Create multiple organizational units with different types
        OrganizationalUnit team1 = repository.create(createTestRequest("Team 1", OrganizationalUnitType.TEAM));
        OrganizationalUnit team2 = repository.create(createTestRequest("Team 2", OrganizationalUnitType.TEAM));
        OrganizationalUnit dept = repository.create(createTestRequest("Department 1", OrganizationalUnitType.DEPARTMENT));
        OrganizationalUnit squad = repository.create(createTestRequest("Squad 1", OrganizationalUnitType.SQUAD));

        List<OrganizationalUnit> teams = repository.findByType(OrganizationalUnitType.TEAM);
        assertEquals(2, teams.size());
        assertTrue(teams.stream().allMatch(u -> u.getType() == OrganizationalUnitType.TEAM));

        List<OrganizationalUnit> departments = repository.findByType(OrganizationalUnitType.DEPARTMENT);
        assertEquals(1, departments.size());
        assertEquals(dept.getId(), departments.get(0).getId());

        List<OrganizationalUnit> squads = repository.findByType(OrganizationalUnitType.SQUAD);
        assertEquals(1, squads.size());
        assertEquals(squad.getId(), squads.get(0).getId());
    }

    @Test
    void testFindByParent() {
        // Create a parent unit
        OrganizationalUnit parent = repository.create(createTestRequest("Department", OrganizationalUnitType.DEPARTMENT));

        // Create child units
        OrganizationalUnitCreateRequest childRequest1 = OrganizationalUnitCreateRequest.builder()
                .name("Team 1")
                .type(OrganizationalUnitType.TEAM)
                .owner("John Smith")
                .parentId(parent.getId())
                .build();
        
        OrganizationalUnitCreateRequest childRequest2 = OrganizationalUnitCreateRequest.builder()
                .name("Team 2")
                .type(OrganizationalUnitType.TEAM)
                .owner("Jane Doe")
                .parentId(parent.getId())
                .build();

        OrganizationalUnit child1 = repository.create(childRequest1);
        OrganizationalUnit child2 = repository.create(childRequest2);

        // Test findByParent
        List<OrganizationalUnit> children = repository.findByParent(parent.getId());
        assertEquals(2, children.size());
        assertTrue(children.stream().anyMatch(u -> u.getId().equals(child1.getId())));
        assertTrue(children.stream().anyMatch(u -> u.getId().equals(child2.getId())));
    }

    @Test
    void testFindByOwner() {
        // Create units with different owners
        OrganizationalUnit unit1 = repository.create(createTestRequestWithOwner("Team 1", "John Smith"));
        OrganizationalUnit unit2 = repository.create(createTestRequestWithOwner("Team 2", "John Smith"));
        OrganizationalUnit unit3 = repository.create(createTestRequestWithOwner("Team 3", "Jane Doe"));

        // Test findByOwner
        List<OrganizationalUnit> johnUnits = repository.findByOwner("John Smith");
        assertEquals(2, johnUnits.size());
        assertTrue(johnUnits.stream().allMatch(u -> "John Smith".equals(u.getOwner())));

        List<OrganizationalUnit> janeUnits = repository.findByOwner("Jane Doe");
        assertEquals(1, janeUnits.size());
        assertEquals(unit3.getId(), janeUnits.get(0).getId());
    }

    @Test
    void testFindByMember() {
        // Create units with different members
        OrganizationalUnit unit1 = repository.create(createTestRequestWithMembers("Team 1", Arrays.asList("Alice", "Bob")));
        OrganizationalUnit unit2 = repository.create(createTestRequestWithMembers("Team 2", Arrays.asList("Bob", "Charlie")));
        OrganizationalUnit unit3 = repository.create(createTestRequestWithMembers("Team 3", Arrays.asList("David")));

        // Test findByMember
        List<OrganizationalUnit> aliceUnits = repository.findByMember("Alice");
        assertEquals(1, aliceUnits.size());
        assertEquals(unit1.getId(), aliceUnits.get(0).getId());

        List<OrganizationalUnit> bobUnits = repository.findByMember("Bob");
        assertEquals(2, bobUnits.size());
        assertTrue(bobUnits.stream().anyMatch(u -> u.getId().equals(unit1.getId())));
        assertTrue(bobUnits.stream().anyMatch(u -> u.getId().equals(unit2.getId())));
    }

    @Test
    void testSaveAndUpdate() {
        // Create a unit
        OrganizationalUnit unit = repository.create(createTestRequest("Team 1", OrganizationalUnitType.TEAM));
        
        // Update the unit
        OrganizationalUnit updated = repository.save(
            ((org.rinna.domain.model.OrganizationalUnitRecord) unit)
                .withName("Updated Team")
                .withDescription("Updated description")
        );
        
        // Verify the update
        assertEquals("Updated Team", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        
        // Verify using findById
        Optional<OrganizationalUnit> found = repository.findById(unit.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated Team", found.get().getName());
        assertEquals("Updated description", found.get().getDescription());
    }

    @Test
    void testDeleteById() {
        // Create a unit
        OrganizationalUnit unit = repository.create(createTestRequest("Team 1", OrganizationalUnitType.TEAM));
        
        // Verify it exists
        assertTrue(repository.findById(unit.getId()).isPresent());
        
        // Delete it
        repository.deleteById(unit.getId());
        
        // Verify it's gone
        assertFalse(repository.findById(unit.getId()).isPresent());
    }

    @Test
    void testFindActiveAndInactive() {
        // Create active and inactive units
        OrganizationalUnit active1 = repository.create(createTestRequestWithActive("Active 1", true));
        OrganizationalUnit active2 = repository.create(createTestRequestWithActive("Active 2", true));
        OrganizationalUnit inactive = repository.create(createTestRequestWithActive("Inactive", false));
        
        // Test findActive
        List<OrganizationalUnit> activeUnits = repository.findActive();
        assertEquals(2, activeUnits.size());
        assertTrue(activeUnits.stream().allMatch(OrganizationalUnit::isActive));
        
        // Test findInactive
        List<OrganizationalUnit> inactiveUnits = repository.findInactive();
        assertEquals(1, inactiveUnits.size());
        assertFalse(inactiveUnits.get(0).isActive());
        assertEquals(inactive.getId(), inactiveUnits.get(0).getId());
    }

    @Test
    void testUpdateCognitiveLoad() {
        // Create a unit
        OrganizationalUnit unit = repository.create(createTestRequestWithCapacity("Team", 100));
        assertEquals(0, unit.getCurrentCognitiveLoad());
        
        // Update the cognitive load
        OrganizationalUnit updated = repository.updateCognitiveLoad(unit.getId(), 75);
        
        // Verify the update
        assertEquals(75, updated.getCurrentCognitiveLoad());
        assertEquals(100, updated.getCognitiveCapacity());
        
        // Verify using findById
        Optional<OrganizationalUnit> found = repository.findById(unit.getId());
        assertTrue(found.isPresent());
        assertEquals(75, found.get().getCurrentCognitiveLoad());
    }

    @Test
    void testFindWithAvailableCapacity() {
        // Create units with different capacities and loads
        OrganizationalUnit unit1 = repository.create(createTestRequestWithCapacity("Team 1", 100));
        repository.updateCognitiveLoad(unit1.getId(), 50);
        
        OrganizationalUnit unit2 = repository.create(createTestRequestWithCapacity("Team 2", 100));
        repository.updateCognitiveLoad(unit2.getId(), 80);
        
        OrganizationalUnit unit3 = repository.create(createTestRequestWithCapacity("Team 3", 100));
        repository.updateCognitiveLoad(unit3.getId(), 0);
        
        // Test findWithAvailableCapacity
        List<OrganizationalUnit> unitsWithCapacity30 = repository.findWithAvailableCapacity(30);
        assertEquals(2, unitsWithCapacity30.size());
        assertTrue(unitsWithCapacity30.stream().anyMatch(u -> u.getId().equals(unit1.getId())));
        assertTrue(unitsWithCapacity30.stream().anyMatch(u -> u.getId().equals(unit3.getId())));
        
        List<OrganizationalUnit> unitsWithCapacity60 = repository.findWithAvailableCapacity(60);
        assertEquals(1, unitsWithCapacity60.size());
        assertEquals(unit3.getId(), unitsWithCapacity60.get(0).getId());
    }

    @Test
    void testFindAtCapacityThreshold() {
        // Create units with different capacities and loads
        OrganizationalUnit unit1 = repository.create(createTestRequestWithCapacity("Team 1", 100));
        repository.updateCognitiveLoad(unit1.getId(), 50);
        
        OrganizationalUnit unit2 = repository.create(createTestRequestWithCapacity("Team 2", 100));
        repository.updateCognitiveLoad(unit2.getId(), 80);
        
        OrganizationalUnit unit3 = repository.create(createTestRequestWithCapacity("Team 3", 100));
        repository.updateCognitiveLoad(unit3.getId(), 90);
        
        // Test findAtCapacityThreshold
        List<OrganizationalUnit> unitsAbove75Percent = repository.findAtCapacityThreshold(75);
        assertEquals(2, unitsAbove75Percent.size());
        assertTrue(unitsAbove75Percent.stream().anyMatch(u -> u.getId().equals(unit2.getId())));
        assertTrue(unitsAbove75Percent.stream().anyMatch(u -> u.getId().equals(unit3.getId())));
        
        List<OrganizationalUnit> unitsAbove85Percent = repository.findAtCapacityThreshold(85);
        assertEquals(1, unitsAbove85Percent.size());
        assertEquals(unit3.getId(), unitsAbove85Percent.get(0).getId());
    }

    @Test
    void testWorkItemAssociations() {
        // Create a unit
        OrganizationalUnit unit = repository.create(createTestRequest("Team", OrganizationalUnitType.TEAM));
        
        // Create work item IDs
        UUID workItem1 = UUID.randomUUID();
        UUID workItem2 = UUID.randomUUID();
        
        // Associate work items
        assertTrue(repository.associateWorkItem(unit.getId(), workItem1));
        assertTrue(repository.associateWorkItem(unit.getId(), workItem2));
        
        // Test findWorkItemIdsByOrganizationalUnitId
        List<UUID> workItems = repository.findWorkItemIdsByOrganizationalUnitId(unit.getId());
        assertEquals(2, workItems.size());
        assertTrue(workItems.contains(workItem1));
        assertTrue(workItems.contains(workItem2));
        
        // Test dissociateWorkItem
        assertTrue(repository.dissociateWorkItem(unit.getId(), workItem1));
        
        workItems = repository.findWorkItemIdsByOrganizationalUnitId(unit.getId());
        assertEquals(1, workItems.size());
        assertTrue(workItems.contains(workItem2));
    }

    @Test
    void testWorkstreamAssociations() {
        // Create a unit
        OrganizationalUnit unit = repository.create(createTestRequest("Team", OrganizationalUnitType.TEAM));
        
        // Create workstream IDs
        UUID workstream1 = UUID.randomUUID();
        UUID workstream2 = UUID.randomUUID();
        
        // Associate workstreams
        assertTrue(repository.associateWorkstream(unit.getId(), workstream1));
        assertTrue(repository.associateWorkstream(unit.getId(), workstream2));
        
        // Test findWorkstreamIdsByOrganizationalUnitId
        List<UUID> workstreams = repository.findWorkstreamIdsByOrganizationalUnitId(unit.getId());
        assertEquals(2, workstreams.size());
        assertTrue(workstreams.contains(workstream1));
        assertTrue(workstreams.contains(workstream2));
        
        // Test dissociateWorkstream
        assertTrue(repository.dissociateWorkstream(unit.getId(), workstream1));
        
        workstreams = repository.findWorkstreamIdsByOrganizationalUnitId(unit.getId());
        assertEquals(1, workstreams.size());
        assertTrue(workstreams.contains(workstream2));
    }

    @Test
    void testWorkItemOwnership() {
        // Create a unit
        OrganizationalUnit unit = repository.create(createTestRequest("Team", OrganizationalUnitType.TEAM));
        
        // Create work item ID
        UUID workItemId = UUID.randomUUID();
        
        // Set ownership
        assertTrue(repository.setOwningUnitForWorkItem(workItemId, unit.getId()));
        
        // Test findOwningUnitForWorkItem
        Optional<OrganizationalUnit> owningUnit = repository.findOwningUnitForWorkItem(workItemId);
        assertTrue(owningUnit.isPresent());
        assertEquals(unit.getId(), owningUnit.get().getId());
        
        // Verify work item is also associated with the unit
        List<UUID> workItems = repository.findWorkItemIdsByOrganizationalUnitId(unit.getId());
        assertEquals(1, workItems.size());
        assertTrue(workItems.contains(workItemId));
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
    
    private OrganizationalUnitCreateRequest createTestRequestWithOwner(String name, String owner) {
        return OrganizationalUnitCreateRequest.builder()
                .name(name)
                .description("Description for " + name)
                .type(OrganizationalUnitType.TEAM)
                .owner(owner)
                .cognitiveCapacity(100)
                .build();
    }
    
    private OrganizationalUnitCreateRequest createTestRequestWithMembers(String name, List<String> members) {
        OrganizationalUnitCreateRequest.Builder builder = OrganizationalUnitCreateRequest.builder()
                .name(name)
                .description("Description for " + name)
                .type(OrganizationalUnitType.TEAM)
                .owner("Default Owner")
                .cognitiveCapacity(100);
        
        for (String member : members) {
            builder.addMember(member);
        }
        
        return builder.build();
    }
    
    private OrganizationalUnitCreateRequest createTestRequestWithActive(String name, boolean active) {
        return OrganizationalUnitCreateRequest.builder()
                .name(name)
                .description("Description for " + name)
                .type(OrganizationalUnitType.TEAM)
                .owner("Default Owner")
                .cognitiveCapacity(100)
                .active(active)
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