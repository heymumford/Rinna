/*
 * Service implementation test for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.adapter.service.DefaultWorkstreamService;
import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.Workstream;
import org.rinna.domain.model.WorkstreamCreateRequest;
import org.rinna.domain.model.WorkstreamRecord;
import org.rinna.domain.model.WorkstreamStatus;
import org.rinna.domain.repository.WorkstreamRepository;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.WorkstreamService;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultWorkstreamServiceTest {

    @Mock
    private WorkstreamRepository workstreamRepository;
    
    @Mock
    private ItemService itemService;
    
    private WorkstreamService workstreamService;
    
    private UUID workstreamId;
    private Workstream testWorkstream;
    private WorkstreamCreateRequest createRequest;
    
    @BeforeEach
    void setUp() {
        workstreamService = new DefaultWorkstreamService(workstreamRepository, itemService);
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
    }
    
    @Test
    @DisplayName("Should create a workstream")
    void createWorkstream() {
        when(workstreamRepository.create(any(WorkstreamCreateRequest.class))).thenReturn(testWorkstream);
        
        Workstream result = workstreamService.createWorkstream(createRequest);
        
        assertNotNull(result);
        assertEquals(workstreamId, result.getId());
        verify(workstreamRepository).create(createRequest);
    }
    
    @Test
    @DisplayName("Should update a workstream")
    void updateWorkstream() {
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.save(testWorkstream)).thenReturn(testWorkstream);
        
        Workstream result = workstreamService.updateWorkstream(testWorkstream);
        
        assertNotNull(result);
        assertEquals(workstreamId, result.getId());
        verify(workstreamRepository).save(testWorkstream);
    }
    
    @Test
    @DisplayName("Should throw exception when updating non-existent workstream")
    void updateNonExistentWorkstream() {
        UUID nonExistentId = UUID.randomUUID();
        WorkstreamRecord nonExistent = new WorkstreamRecord(
            nonExistentId,
            "Non-existent",
            "Description",
            "owner",
            WorkstreamStatus.DRAFT.name(),
            Priority.LOW,
            Instant.now(),
            Instant.now(),
            null,
            null,
            false,
            null
        );
        
        when(workstreamRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> workstreamService.updateWorkstream(nonExistent));
        verify(workstreamRepository, never()).save(any(Workstream.class));
    }
    
    @Test
    @DisplayName("Should find a workstream by ID")
    void findWorkstreamById() {
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        
        Optional<Workstream> result = workstreamService.findWorkstreamById(workstreamId);
        
        assertTrue(result.isPresent());
        assertEquals(workstreamId, result.get().getId());
    }
    
    @Test
    @DisplayName("Should return empty when finding non-existent workstream")
    void findNonExistentWorkstream() {
        UUID nonExistentId = UUID.randomUUID();
        when(workstreamRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        Optional<Workstream> result = workstreamService.findWorkstreamById(nonExistentId);
        
        assertFalse(result.isPresent());
    }
    
    @Test
    @DisplayName("Should find all workstreams")
    void findAllWorkstreams() {
        List<Workstream> workstreams = Arrays.asList(testWorkstream);
        when(workstreamRepository.findAll()).thenReturn(workstreams);
        
        List<Workstream> result = workstreamService.findAllWorkstreams();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workstreamId, result.get(0).getId());
    }
    
    @Test
    @DisplayName("Should find workstreams by status")
    void findWorkstreamsByStatus() {
        String status = WorkstreamStatus.ACTIVE.name();
        List<Workstream> workstreams = Arrays.asList(testWorkstream);
        when(workstreamRepository.findByStatus(status)).thenReturn(workstreams);
        
        List<Workstream> result = workstreamService.findWorkstreamsByStatus(status);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workstreamId, result.get(0).getId());
        assertEquals(status, result.get(0).getStatus());
    }
    
    @Test
    @DisplayName("Should find workstreams by owner")
    void findWorkstreamsByOwner() {
        String owner = "testowner";
        List<Workstream> workstreams = Arrays.asList(testWorkstream);
        when(workstreamRepository.findByOwner(owner)).thenReturn(workstreams);
        
        List<Workstream> result = workstreamService.findWorkstreamsByOwner(owner);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workstreamId, result.get(0).getId());
        assertEquals(owner, result.get(0).getOwner());
    }
    
    @Test
    @DisplayName("Should find workstreams by priority")
    void findWorkstreamsByPriority() {
        Priority priority = Priority.HIGH;
        List<Workstream> workstreams = Arrays.asList(testWorkstream);
        when(workstreamRepository.findByPriority(priority)).thenReturn(workstreams);
        
        List<Workstream> result = workstreamService.findWorkstreamsByPriority(priority);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workstreamId, result.get(0).getId());
        assertEquals(priority, result.get(0).getPriority());
    }
    
    @Test
    @DisplayName("Should find workstreams by CYNEFIN domain")
    void findWorkstreamsByCynefinDomain() {
        CynefinDomain domain = CynefinDomain.COMPLEX;
        List<Workstream> workstreams = Arrays.asList(testWorkstream);
        when(workstreamRepository.findByCynefinDomain(domain)).thenReturn(workstreams);
        
        List<Workstream> result = workstreamService.findWorkstreamsByCynefinDomain(domain);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workstreamId, result.get(0).getId());
        assertTrue(result.get(0).getCynefinDomain().isPresent());
        assertEquals(domain, result.get(0).getCynefinDomain().get());
    }
    
    @Test
    @DisplayName("Should find cross-project workstreams")
    void findCrossProjectWorkstreams() {
        List<Workstream> workstreams = Arrays.asList(testWorkstream);
        when(workstreamRepository.findCrossProjectWorkstreams()).thenReturn(workstreams);
        
        List<Workstream> result = workstreamService.findCrossProjectWorkstreams();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workstreamId, result.get(0).getId());
        assertTrue(result.get(0).isCrossProject());
    }
    
    @Test
    @DisplayName("Should delete a workstream")
    void deleteWorkstream() {
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        
        boolean result = workstreamService.deleteWorkstream(workstreamId);
        
        assertTrue(result);
        verify(workstreamRepository).deleteById(workstreamId);
    }
    
    @Test
    @DisplayName("Should return false when deleting non-existent workstream")
    void deleteNonExistentWorkstream() {
        UUID nonExistentId = UUID.randomUUID();
        when(workstreamRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        boolean result = workstreamService.deleteWorkstream(nonExistentId);
        
        assertFalse(result);
        verify(workstreamRepository, never()).deleteById(nonExistentId);
    }
    
    @Test
    @DisplayName("Should associate a project with a workstream")
    void associateProject() {
        UUID projectId = UUID.randomUUID();
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.associateProject(workstreamId, projectId)).thenReturn(true);
        
        boolean result = workstreamService.associateProject(workstreamId, projectId);
        
        assertTrue(result);
        verify(workstreamRepository).associateProject(workstreamId, projectId);
    }
    
    @Test
    @DisplayName("Should return false when associating project with non-existent workstream")
    void associateProjectWithNonExistentWorkstream() {
        UUID nonExistentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(workstreamRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        boolean result = workstreamService.associateProject(nonExistentId, projectId);
        
        assertFalse(result);
        verify(workstreamRepository, never()).associateProject(nonExistentId, projectId);
    }
    
    @Test
    @DisplayName("Should associate multiple projects with a workstream")
    void associateProjects() {
        UUID projectId1 = UUID.randomUUID();
        UUID projectId2 = UUID.randomUUID();
        Set<UUID> projectIds = new HashSet<>(Arrays.asList(projectId1, projectId2));
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.associateProject(eq(workstreamId), any(UUID.class))).thenReturn(true);
        
        int result = workstreamService.associateProjects(workstreamId, projectIds);
        
        assertEquals(2, result);
        verify(workstreamRepository, times(2)).associateProject(eq(workstreamId), any(UUID.class));
    }
    
    @Test
    @DisplayName("Should dissociate a project from a workstream")
    void dissociateProject() {
        UUID projectId = UUID.randomUUID();
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.dissociateProject(workstreamId, projectId)).thenReturn(true);
        
        boolean result = workstreamService.dissociateProject(workstreamId, projectId);
        
        assertTrue(result);
        verify(workstreamRepository).dissociateProject(workstreamId, projectId);
    }
    
    @Test
    @DisplayName("Should get projects for a workstream")
    void getProjectsForWorkstream() {
        UUID projectId = UUID.randomUUID();
        List<UUID> projectIds = Arrays.asList(projectId);
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.findProjectIdsByWorkstreamId(workstreamId)).thenReturn(projectIds);
        
        List<UUID> result = workstreamService.getProjectsForWorkstream(workstreamId);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(projectId, result.get(0));
    }
    
    @Test
    @DisplayName("Should get workstreams for a project")
    void getWorkstreamsForProject() {
        UUID projectId = UUID.randomUUID();
        List<Workstream> workstreams = Arrays.asList(testWorkstream);
        
        when(workstreamRepository.findByProject(projectId)).thenReturn(workstreams);
        
        List<Workstream> result = workstreamService.getWorkstreamsForProject(projectId);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workstreamId, result.get(0).getId());
    }
    
    @Test
    @DisplayName("Should associate a work item with a workstream")
    void associateWorkItem() {
        UUID workItemId = UUID.randomUUID();
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.associateWorkItem(workstreamId, workItemId)).thenReturn(true);
        
        boolean result = workstreamService.associateWorkItem(workstreamId, workItemId);
        
        assertTrue(result);
        verify(workstreamRepository).associateWorkItem(workstreamId, workItemId);
    }
    
    @Test
    @DisplayName("Should associate multiple work items with a workstream")
    void associateWorkItems() {
        UUID workItemId1 = UUID.randomUUID();
        UUID workItemId2 = UUID.randomUUID();
        Set<UUID> workItemIds = new HashSet<>(Arrays.asList(workItemId1, workItemId2));
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.associateWorkItem(eq(workstreamId), any(UUID.class))).thenReturn(true);
        
        int result = workstreamService.associateWorkItems(workstreamId, workItemIds);
        
        assertEquals(2, result);
        verify(workstreamRepository, times(2)).associateWorkItem(eq(workstreamId), any(UUID.class));
    }
    
    @Test
    @DisplayName("Should dissociate a work item from a workstream")
    void dissociateWorkItem() {
        UUID workItemId = UUID.randomUUID();
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.dissociateWorkItem(workstreamId, workItemId)).thenReturn(true);
        
        boolean result = workstreamService.dissociateWorkItem(workstreamId, workItemId);
        
        assertTrue(result);
        verify(workstreamRepository).dissociateWorkItem(workstreamId, workItemId);
    }
    
    @Test
    @DisplayName("Should get work items for a workstream")
    void getWorkItemsForWorkstream() {
        UUID workItemId = UUID.randomUUID();
        List<UUID> workItemIds = Arrays.asList(workItemId);
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.findWorkItemIdsByWorkstreamId(workstreamId)).thenReturn(workItemIds);
        
        List<UUID> result = workstreamService.getWorkItemsForWorkstream(workstreamId);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workItemId, result.get(0));
    }
    
    @Test
    @DisplayName("Should get workstreams for a work item")
    void getWorkstreamsForWorkItem() {
        UUID workItemId = UUID.randomUUID();
        List<Workstream> workstreams = Arrays.asList(testWorkstream);
        
        when(workstreamRepository.findByWorkItem(workItemId)).thenReturn(workstreams);
        
        List<Workstream> result = workstreamService.getWorkstreamsForWorkItem(workItemId);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(workstreamId, result.get(0).getId());
    }
    
    @Test
    @DisplayName("Should update a workstream's status")
    void updateWorkstreamStatus() {
        String newStatus = WorkstreamStatus.COMPLETED.name();
        WorkstreamRecord updatedWorkstream = ((WorkstreamRecord) testWorkstream).withStatus(newStatus);
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.save(any(Workstream.class))).thenReturn(updatedWorkstream);
        
        Workstream result = workstreamService.updateWorkstreamStatus(workstreamId, newStatus);
        
        assertNotNull(result);
        assertEquals(newStatus, result.getStatus());
        verify(workstreamRepository).save(any(Workstream.class));
    }
    
    @Test
    @DisplayName("Should update a workstream's priority")
    void updateWorkstreamPriority() {
        Priority newPriority = Priority.LOW;
        WorkstreamRecord updatedWorkstream = ((WorkstreamRecord) testWorkstream).withPriority(newPriority);
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.save(any(Workstream.class))).thenReturn(updatedWorkstream);
        
        Workstream result = workstreamService.updateWorkstreamPriority(workstreamId, newPriority);
        
        assertNotNull(result);
        assertEquals(newPriority, result.getPriority());
        verify(workstreamRepository).save(any(Workstream.class));
    }
    
    @Test
    @DisplayName("Should update a workstream's CYNEFIN domain")
    void updateWorkstreamCynefinDomain() {
        CynefinDomain newDomain = CynefinDomain.COMPLICATED;
        WorkstreamRecord updatedWorkstream = ((WorkstreamRecord) testWorkstream).withCynefinDomain(newDomain);
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.save(any(Workstream.class))).thenReturn(updatedWorkstream);
        
        Workstream result = workstreamService.updateWorkstreamCynefinDomain(workstreamId, newDomain);
        
        assertNotNull(result);
        assertTrue(result.getCynefinDomain().isPresent());
        assertEquals(newDomain, result.getCynefinDomain().get());
        verify(workstreamRepository).save(any(Workstream.class));
    }
    
    @Test
    @DisplayName("Should update a workstream's cross-project status")
    void updateWorkstreamCrossProject() {
        boolean newCrossProject = false;
        WorkstreamRecord updatedWorkstream = ((WorkstreamRecord) testWorkstream).withCrossProject(newCrossProject);
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.save(any(Workstream.class))).thenReturn(updatedWorkstream);
        
        Workstream result = workstreamService.updateWorkstreamCrossProject(workstreamId, newCrossProject);
        
        assertNotNull(result);
        assertEquals(newCrossProject, result.isCrossProject());
        verify(workstreamRepository).save(any(Workstream.class));
    }
    
    @Test
    @DisplayName("Should suggest a CYNEFIN domain based on workstream context")
    void suggestCynefinDomain() {
        UUID projectId1 = UUID.randomUUID();
        UUID projectId2 = UUID.randomUUID();
        UUID workItemId1 = UUID.randomUUID();
        UUID workItemId2 = UUID.randomUUID();
        
        List<UUID> projectIds = Arrays.asList(projectId1, projectId2);
        List<UUID> workItemIds = Arrays.asList(workItemId1, workItemId2);
        
        when(workstreamRepository.findById(workstreamId)).thenReturn(Optional.of(testWorkstream));
        when(workstreamRepository.findProjectIdsByWorkstreamId(workstreamId)).thenReturn(projectIds);
        when(workstreamRepository.findWorkItemIdsByWorkstreamId(workstreamId)).thenReturn(workItemIds);
        
        CynefinDomain result = workstreamService.suggestCynefinDomain(workstreamId);
        
        assertNotNull(result);
        // The result will depend on the algorithm implementation,
        // but we expect it to be one of the valid CYNEFIN domains
        assertTrue(Arrays.asList(CynefinDomain.values()).contains(result));
    }
    
    @Test
    @DisplayName("Should throw exception when suggesting domain for non-existent workstream")
    void suggestCynefinDomainForNonExistentWorkstream() {
        UUID nonExistentId = UUID.randomUUID();
        when(workstreamRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> workstreamService.suggestCynefinDomain(nonExistentId));
    }
    
    @Test
    @DisplayName("Should suggest workstreams for a work item")
    void suggestWorkstreamsForWorkItem() {
        UUID workItemId = UUID.randomUUID();
        
        when(workstreamRepository.findAll()).thenReturn(Collections.singletonList(testWorkstream));
        
        List<Workstream> result = workstreamService.suggestWorkstreamsForWorkItem(workItemId);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}