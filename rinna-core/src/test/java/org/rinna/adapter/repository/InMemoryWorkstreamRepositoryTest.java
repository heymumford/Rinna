/*
 * Repository test for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.*;
import org.rinna.domain.repository.WorkstreamRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryWorkstreamRepositoryTest {

    private WorkstreamRepository repository;
    private UUID workstreamId;
    private Workstream testWorkstream;
    private WorkstreamCreateRequest createRequest;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up InMemoryWorkstreamRepositoryTest...");
        repository = new InMemoryWorkstreamRepository();
        workstreamId = UUID.randomUUID();
        
        // Create a test workstream
        testWorkstream = new WorkstreamRecord(
            workstreamId,
            "Test Workstream",
            "A test workstream for unit testing",
            "testowner",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            Instant.now(),
            Instant.now(),
            null,
            CynefinDomain.COMPLEX,
            true,
            Instant.now().plusSeconds(86400)
        );
        
        // Create a test create request
        createRequest = new WorkstreamCreateRequest.Builder()
            .name("New Workstream")
            .description("A newly created workstream")
            .owner("newowner")
            .status(WorkstreamStatus.DRAFT)
            .priority(Priority.MEDIUM)
            .cynefinDomain(CynefinDomain.COMPLICATED)
            .crossProject(true)
            .build();
        
        System.out.println("Test setup complete with ID: " + workstreamId);
    }

    @Test
    @DisplayName("Should create a new workstream")
    void createWorkstream() {
        Workstream created = repository.create(createRequest);
        
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("New Workstream", created.getName());
        assertEquals("A newly created workstream", created.getDescription());
        assertEquals("newowner", created.getOwner());
        assertEquals(WorkstreamStatus.DRAFT.name(), created.getStatus());
        assertEquals(Priority.MEDIUM, created.getPriority());
        assertTrue(created.getCynefinDomain().isPresent());
        assertEquals(CynefinDomain.COMPLICATED, created.getCynefinDomain().get());
        assertTrue(created.isCrossProject());
        
        // Verify it was stored
        Optional<Workstream> found = repository.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Should save an existing workstream")
    void saveWorkstream() {
        // First save the original
        Workstream saved = repository.save(testWorkstream);
        assertEquals(testWorkstream.getId(), saved.getId());
        
        // Now update and save
        WorkstreamRecord updated = ((WorkstreamRecord) saved).withName("Updated Name");
        Workstream result = repository.save(updated);
        
        assertEquals("Updated Name", result.getName());
        
        // Verify it was stored
        Optional<Workstream> found = repository.findById(workstreamId);
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
    }
    
    @Test
    @DisplayName("Should find a workstream by ID")
    void findById() {
        repository.save(testWorkstream);
        
        Optional<Workstream> found = repository.findById(workstreamId);
        
        assertTrue(found.isPresent());
        assertEquals(workstreamId, found.get().getId());
        assertEquals("Test Workstream", found.get().getName());
    }
    
    @Test
    @DisplayName("Should return empty for non-existent ID")
    void findByIdNotFound() {
        Optional<Workstream> found = repository.findById(UUID.randomUUID());
        
        assertFalse(found.isPresent());
    }
    
    @Test
    @DisplayName("Should find all workstreams")
    void findAll() {
        repository.save(testWorkstream);
        Workstream another = repository.create(createRequest);
        
        List<Workstream> all = repository.findAll();
        
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(w -> w.getId().equals(workstreamId)));
        assertTrue(all.stream().anyMatch(w -> w.getId().equals(another.getId())));
    }
    
    @Test
    @DisplayName("Should find workstreams by status")
    void findByStatus() {
        repository.save(testWorkstream);
        Workstream anotherActive = new WorkstreamRecord(
            UUID.randomUUID(),
            "Another Active",
            "Another active workstream",
            "testowner",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            Instant.now(),
            Instant.now(),
            null,
            CynefinDomain.COMPLEX,
            true,
            null
        );
        repository.save(anotherActive);
        repository.create(createRequest); // This one is DRAFT
        
        List<Workstream> active = repository.findByStatus(WorkstreamStatus.ACTIVE.name());
        
        assertEquals(2, active.size());
        assertTrue(active.stream().allMatch(w -> w.getStatus().equals(WorkstreamStatus.ACTIVE.name())));
    }
    
    @Test
    @DisplayName("Should find workstreams by owner")
    void findByOwner() {
        repository.save(testWorkstream);
        Workstream anotherByOwner = new WorkstreamRecord(
            UUID.randomUUID(),
            "Another by Owner",
            "Another workstream by the same owner",
            "testowner",
            WorkstreamStatus.COMPLETED.name(),
            Priority.LOW,
            Instant.now(),
            Instant.now(),
            null,
            null,
            false,
            null
        );
        repository.save(anotherByOwner);
        repository.create(createRequest); // This one has "newowner"
        
        List<Workstream> byOwner = repository.findByOwner("testowner");
        
        assertEquals(2, byOwner.size());
        assertTrue(byOwner.stream().allMatch(w -> w.getOwner().equals("testowner")));
    }
    
    @Test
    @DisplayName("Should find workstreams by priority")
    void findByPriority() {
        repository.save(testWorkstream); // HIGH priority
        repository.create(createRequest); // MEDIUM priority
        
        List<Workstream> highPriority = repository.findByPriority(Priority.HIGH);
        
        assertEquals(1, highPriority.size());
        assertEquals(Priority.HIGH, highPriority.get(0).getPriority());
    }
    
    @Test
    @DisplayName("Should find workstreams by CYNEFIN domain")
    void findByCynefinDomain() {
        repository.save(testWorkstream); // COMPLEX domain
        repository.create(createRequest); // COMPLICATED domain
        
        List<Workstream> complexDomain = repository.findByCynefinDomain(CynefinDomain.COMPLEX);
        
        assertEquals(1, complexDomain.size());
        assertTrue(complexDomain.get(0).getCynefinDomain().isPresent());
        assertEquals(CynefinDomain.COMPLEX, complexDomain.get(0).getCynefinDomain().get());
    }
    
    @Test
    @DisplayName("Should find cross-project workstreams")
    void findCrossProjectWorkstreams() {
        repository.save(testWorkstream); // cross-project true
        
        Workstream nonCrossProject = new WorkstreamRecord(
            UUID.randomUUID(),
            "Non-Cross Project",
            "A workstream that's not cross-project",
            "testowner",
            WorkstreamStatus.ACTIVE.name(),
            Priority.MEDIUM,
            Instant.now(),
            Instant.now(),
            null,
            null,
            false,
            null
        );
        repository.save(nonCrossProject);
        
        List<Workstream> crossProject = repository.findCrossProjectWorkstreams();
        
        assertEquals(1, crossProject.size());
        assertTrue(crossProject.get(0).isCrossProject());
    }
    
    @Test
    @DisplayName("Should delete a workstream by ID")
    void deleteById() {
        repository.save(testWorkstream);
        assertTrue(repository.findById(workstreamId).isPresent());
        
        repository.deleteById(workstreamId);
        
        assertFalse(repository.findById(workstreamId).isPresent());
    }
    
    @Test
    @DisplayName("Should handle project associations")
    void projectAssociations() {
        repository.save(testWorkstream);
        UUID projectId1 = UUID.randomUUID();
        UUID projectId2 = UUID.randomUUID();
        
        // Associate projects
        boolean associated1 = repository.associateProject(workstreamId, projectId1);
        boolean associated2 = repository.associateProject(workstreamId, projectId2);
        
        assertTrue(associated1);
        assertTrue(associated2);
        
        // Find projects
        List<UUID> projects = repository.findProjectIdsByWorkstreamId(workstreamId);
        
        assertEquals(2, projects.size());
        assertTrue(projects.contains(projectId1));
        assertTrue(projects.contains(projectId2));
        
        // Find workstreams by project
        List<Workstream> workstreams = repository.findByProject(projectId1);
        
        assertEquals(1, workstreams.size());
        assertEquals(workstreamId, workstreams.get(0).getId());
        
        // Dissociate project
        boolean dissociated = repository.dissociateProject(workstreamId, projectId1);
        
        assertTrue(dissociated);
        
        // Check dissociated
        projects = repository.findProjectIdsByWorkstreamId(workstreamId);
        assertEquals(1, projects.size());
        assertFalse(projects.contains(projectId1));
        assertTrue(projects.contains(projectId2));
    }
    
    @Test
    @DisplayName("Should handle work item associations")
    void workItemAssociations() {
        repository.save(testWorkstream);
        UUID workItemId1 = UUID.randomUUID();
        UUID workItemId2 = UUID.randomUUID();
        
        // Associate work items
        boolean associated1 = repository.associateWorkItem(workstreamId, workItemId1);
        boolean associated2 = repository.associateWorkItem(workstreamId, workItemId2);
        
        assertTrue(associated1);
        assertTrue(associated2);
        
        // Find work items
        List<UUID> workItems = repository.findWorkItemIdsByWorkstreamId(workstreamId);
        
        assertEquals(2, workItems.size());
        assertTrue(workItems.contains(workItemId1));
        assertTrue(workItems.contains(workItemId2));
        
        // Find workstreams by work item
        List<Workstream> workstreams = repository.findByWorkItem(workItemId1);
        
        assertEquals(1, workstreams.size());
        assertEquals(workstreamId, workstreams.get(0).getId());
        
        // Dissociate work item
        boolean dissociated = repository.dissociateWorkItem(workstreamId, workItemId1);
        
        assertTrue(dissociated);
        
        // Check dissociated
        workItems = repository.findWorkItemIdsByWorkstreamId(workstreamId);
        assertEquals(1, workItems.size());
        assertFalse(workItems.contains(workItemId1));
        assertTrue(workItems.contains(workItemId2));
    }
    
    @Test
    @DisplayName("Should find workstreams by organization")
    void findByOrganization() {
        UUID orgId = UUID.randomUUID();
        
        Workstream withOrg = new WorkstreamRecord(
            UUID.randomUUID(),
            "Org Workstream",
            "A workstream with an organization",
            "testowner",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            Instant.now(),
            Instant.now(),
            orgId,
            null,
            false,
            null
        );
        repository.save(withOrg);
        repository.save(testWorkstream); // No org
        
        List<Workstream> byOrg = repository.findByOrganization(orgId);
        
        assertEquals(1, byOrg.size());
        assertTrue(byOrg.get(0).getOrganizationId().isPresent());
        assertEquals(orgId, byOrg.get(0).getOrganizationId().get());
    }
}