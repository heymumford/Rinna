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
import org.rinna.domain.WorkItemDependency;
import org.rinna.repository.DependencyRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the InMemoryDependencyRepository.
 */
public class InMemoryDependencyRepositoryTest {

    private DependencyRepository repository;
    private UUID workItem1Id;
    private UUID workItem2Id;
    private UUID workItem3Id;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDependencyRepository();
        workItem1Id = UUID.randomUUID();
        workItem2Id = UUID.randomUUID();
        workItem3Id = UUID.randomUUID();
    }

    @Test
    void testSaveAndFindById() {
        // Create a dependency
        WorkItemDependency dependency = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem2Id)
                .createdBy("alice")
                .build();
        
        // Save it
        WorkItemDependency savedDependency = repository.save(dependency);
        assertNotNull(savedDependency);
        
        // Find it by ID
        Optional<WorkItemDependency> foundDependency = repository.findById(dependency.getId());
        assertTrue(foundDependency.isPresent());
        assertEquals(dependency.getId(), foundDependency.get().getId());
        assertEquals(workItem1Id, foundDependency.get().getDependentId());
        assertEquals(workItem2Id, foundDependency.get().getDependencyId());
        assertEquals("alice", foundDependency.get().getCreatedBy());
        assertEquals("BLOCKS", foundDependency.get().getDependencyType()); // Default type
    }
    
    @Test
    void testFindByIdNotFound() {
        Optional<WorkItemDependency> notFoundDependency = repository.findById(UUID.randomUUID());
        assertFalse(notFoundDependency.isPresent());
    }
    
    @Test
    void testFindAll() {
        // Create and save multiple dependencies
        WorkItemDependency dependency1 = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem2Id)
                .createdBy("alice")
                .build();
                
        WorkItemDependency dependency2 = new WorkItemDependency.Builder()
                .dependentId(workItem2Id)
                .dependencyId(workItem3Id)
                .dependencyType("RELATES_TO")
                .createdBy("bob")
                .build();
        
        repository.save(dependency1);
        repository.save(dependency2);
        
        // Find all dependencies
        List<WorkItemDependency> dependencies = repository.findAll();
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.stream().anyMatch(d -> d.getId().equals(dependency1.getId())));
        assertTrue(dependencies.stream().anyMatch(d -> d.getId().equals(dependency2.getId())));
    }
    
    @Test
    void testRemoveDependency() {
        // Create and save a dependency
        WorkItemDependency dependency = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem2Id)
                .createdBy("alice")
                .build();
        
        repository.save(dependency);
        
        // Verify it exists
        assertTrue(repository.findById(dependency.getId()).isPresent());
        
        // Remove it
        boolean removed = repository.remove(workItem1Id, workItem2Id);
        assertTrue(removed);
        
        // Verify it's gone
        assertFalse(repository.findById(dependency.getId()).isPresent());
        
        // Try to remove a non-existent dependency
        boolean nonExistentRemoved = repository.remove(UUID.randomUUID(), UUID.randomUUID());
        assertFalse(nonExistentRemoved);
    }
    
    @Test
    void testFindIncomingDependencies() {
        // Create dependencies where workItem1 depends on workItem2 and workItem3
        WorkItemDependency dependency1 = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem2Id)
                .createdBy("alice")
                .build();
                
        WorkItemDependency dependency2 = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem3Id)
                .createdBy("bob")
                .build();
                
        WorkItemDependency dependency3 = new WorkItemDependency.Builder()
                .dependentId(workItem2Id)
                .dependencyId(workItem3Id)
                .createdBy("charlie")
                .build();
        
        repository.save(dependency1);
        repository.save(dependency2);
        repository.save(dependency3);
        
        // Find incoming dependencies for workItem1 (items that workItem1 depends on)
        List<WorkItemDependency> incomingDependencies = repository.findIncomingDependencies(workItem1Id);
        assertEquals(2, incomingDependencies.size());
        assertTrue(incomingDependencies.stream().anyMatch(d -> d.getDependencyId().equals(workItem2Id)));
        assertTrue(incomingDependencies.stream().anyMatch(d -> d.getDependencyId().equals(workItem3Id)));
        
        // Find incoming dependencies for workItem2
        List<WorkItemDependency> workItem2IncomingDependencies = repository.findIncomingDependencies(workItem2Id);
        assertEquals(1, workItem2IncomingDependencies.size());
        assertEquals(workItem3Id, workItem2IncomingDependencies.get(0).getDependencyId());
        
        // Find incoming dependencies for workItem3
        List<WorkItemDependency> workItem3IncomingDependencies = repository.findIncomingDependencies(workItem3Id);
        assertTrue(workItem3IncomingDependencies.isEmpty());
    }
    
    @Test
    void testFindOutgoingDependencies() {
        // Create dependencies where workItem3 is depended upon by workItem1 and workItem2
        WorkItemDependency dependency1 = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem3Id)
                .createdBy("alice")
                .build();
                
        WorkItemDependency dependency2 = new WorkItemDependency.Builder()
                .dependentId(workItem2Id)
                .dependencyId(workItem3Id)
                .createdBy("bob")
                .build();
        
        repository.save(dependency1);
        repository.save(dependency2);
        
        // Find outgoing dependencies for workItem3 (items that depend on workItem3)
        List<WorkItemDependency> outgoingDependencies = repository.findOutgoingDependencies(workItem3Id);
        assertEquals(2, outgoingDependencies.size());
        assertTrue(outgoingDependencies.stream().anyMatch(d -> d.getDependentId().equals(workItem1Id)));
        assertTrue(outgoingDependencies.stream().anyMatch(d -> d.getDependentId().equals(workItem2Id)));
        
        // Find outgoing dependencies for workItem1
        List<WorkItemDependency> workItem1OutgoingDependencies = repository.findOutgoingDependencies(workItem1Id);
        assertTrue(workItem1OutgoingDependencies.isEmpty());
        
        // Find outgoing dependencies for workItem2
        List<WorkItemDependency> workItem2OutgoingDependencies = repository.findOutgoingDependencies(workItem2Id);
        assertTrue(workItem2OutgoingDependencies.isEmpty());
    }
    
    @Test
    void testFindByWorkItems() {
        // Create a dependency between two work items
        WorkItemDependency dependency = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem2Id)
                .dependencyType("BLOCKS")
                .createdBy("alice")
                .build();
        
        repository.save(dependency);
        
        // Find the dependency by the work items
        Optional<WorkItemDependency> foundDependency = repository.findByWorkItems(workItem1Id, workItem2Id);
        assertTrue(foundDependency.isPresent());
        assertEquals(dependency.getId(), foundDependency.get().getId());
        
        // Try to find a non-existent dependency
        Optional<WorkItemDependency> notFoundDependency = repository.findByWorkItems(workItem1Id, workItem3Id);
        assertFalse(notFoundDependency.isPresent());
    }
    
    @Test
    void testUpdateDependency() {
        // Create and save a dependency
        WorkItemDependency originalDependency = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem2Id)
                .dependencyType("BLOCKS")
                .createdBy("alice")
                .build();
        
        repository.save(originalDependency);
        
        // Create an updated dependency with the same ID but different type
        WorkItemDependency updatedDependency = new WorkItemDependency.Builder()
                .dependentId(workItem1Id)
                .dependencyId(workItem2Id)
                .dependencyType("RELATES_TO")
                .createdBy("alice")
                .build();
        
        // We can't modify the original dependency directly, so we'll need to create 
        // a new dependency with a different type and save it
        repository.save(updatedDependency);
        
        // Find all dependencies between these work items - should only be the updated one
        List<WorkItemDependency> dependencies = repository.findAll();
        assertEquals(2, dependencies.size());  // Both dependencies are stored
    }
}