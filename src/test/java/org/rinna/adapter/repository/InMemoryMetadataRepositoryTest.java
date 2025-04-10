/*
 * Test class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.WorkItemMetadata;
import org.rinna.repository.MetadataRepository;

/**
 * Test class for the InMemoryMetadataRepository.
 */
public class InMemoryMetadataRepositoryTest {

    private MetadataRepository repository;
    private UUID workItem1Id;
    private UUID workItem2Id;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetadataRepository();
        ((InMemoryMetadataRepository) repository).clear(); // Clear the repository before each test
        workItem1Id = UUID.randomUUID();
        workItem2Id = UUID.randomUUID();
    }

    @Test
    void testSaveAndFindById() {
        // Create metadata
        WorkItemMetadata metadata = new WorkItemMetadata(workItem1Id, "key1", "value1");
        
        // Save it
        WorkItemMetadata savedMetadata = repository.save(metadata);
        assertNotNull(savedMetadata);
        assertEquals(metadata.getId(), savedMetadata.getId());
        
        // Find it by ID
        Optional<WorkItemMetadata> foundMetadata = repository.findById(metadata.getId());
        assertTrue(foundMetadata.isPresent());
        assertEquals(metadata.getId(), foundMetadata.get().getId());
        assertEquals(workItem1Id, foundMetadata.get().getWorkItemId());
        assertEquals("key1", foundMetadata.get().getKey());
        assertEquals("value1", foundMetadata.get().getValue());
    }
    
    @Test
    void testFindByIdNotFound() {
        Optional<WorkItemMetadata> notFoundMetadata = repository.findById(UUID.randomUUID());
        assertFalse(notFoundMetadata.isPresent());
    }
    
    @Test
    void testSaveExistingKeyOverwrites() {
        // Create metadata for a key
        WorkItemMetadata metadata1 = new WorkItemMetadata(workItem1Id, "key1", "value1");
        repository.save(metadata1);
        
        // Create new metadata with the same key but different value
        WorkItemMetadata metadata2 = new WorkItemMetadata(workItem1Id, "key1", "updated_value");
        repository.save(metadata2);
        
        // Find metadata by work item ID and key
        Optional<WorkItemMetadata> foundMetadata = repository.findByWorkItemIdAndKey(workItem1Id, "key1");
        assertTrue(foundMetadata.isPresent());
        assertEquals("updated_value", foundMetadata.get().getValue());
        
        // There should be only one metadata entry for this key
        List<WorkItemMetadata> allMetadata = repository.findByWorkItemId(workItem1Id);
        assertEquals(1, allMetadata.size());
    }
    
    @Test
    void testFindByWorkItemId() {
        // Create multiple metadata entries for the same work item
        WorkItemMetadata metadata1 = new WorkItemMetadata(workItem1Id, "key1", "value1");
        WorkItemMetadata metadata2 = new WorkItemMetadata(workItem1Id, "key2", "value2");
        WorkItemMetadata metadata3 = new WorkItemMetadata(workItem2Id, "key1", "value3");
        
        repository.save(metadata1);
        repository.save(metadata2);
        repository.save(metadata3);
        
        // Find metadata for work item 1
        List<WorkItemMetadata> workItem1Metadata = repository.findByWorkItemId(workItem1Id);
        assertEquals(2, workItem1Metadata.size());
        assertTrue(workItem1Metadata.stream().anyMatch(m -> m.getKey().equals("key1") && m.getValue().equals("value1")));
        assertTrue(workItem1Metadata.stream().anyMatch(m -> m.getKey().equals("key2") && m.getValue().equals("value2")));
        
        // Find metadata for work item 2
        List<WorkItemMetadata> workItem2Metadata = repository.findByWorkItemId(workItem2Id);
        assertEquals(1, workItem2Metadata.size());
        assertEquals("key1", workItem2Metadata.get(0).getKey());
        assertEquals("value3", workItem2Metadata.get(0).getValue());
        
        // Find metadata for non-existent work item
        List<WorkItemMetadata> nonExistentWorkItemMetadata = repository.findByWorkItemId(UUID.randomUUID());
        assertTrue(nonExistentWorkItemMetadata.isEmpty());
    }
    
    @Test
    void testFindByWorkItemIdAndKey() {
        // Create metadata
        WorkItemMetadata metadata1 = new WorkItemMetadata(workItem1Id, "key1", "value1");
        WorkItemMetadata metadata2 = new WorkItemMetadata(workItem1Id, "key2", "value2");
        
        repository.save(metadata1);
        repository.save(metadata2);
        
        // Find by work item ID and key
        Optional<WorkItemMetadata> foundMetadata = repository.findByWorkItemIdAndKey(workItem1Id, "key1");
        assertTrue(foundMetadata.isPresent());
        assertEquals("value1", foundMetadata.get().getValue());
        
        // Try to find with non-existent key
        Optional<WorkItemMetadata> notFoundMetadata = repository.findByWorkItemIdAndKey(workItem1Id, "key3");
        assertFalse(notFoundMetadata.isPresent());
    }
    
    @Test
    void testGetMetadataMap() {
        // Create multiple metadata entries
        WorkItemMetadata metadata1 = new WorkItemMetadata(workItem1Id, "key1", "value1");
        WorkItemMetadata metadata2 = new WorkItemMetadata(workItem1Id, "key2", "value2");
        
        repository.save(metadata1);
        repository.save(metadata2);
        
        // Get metadata map
        Map<String, String> metadataMap = repository.getMetadataMap(workItem1Id);
        assertEquals(2, metadataMap.size());
        assertEquals("value1", metadataMap.get("key1"));
        assertEquals("value2", metadataMap.get("key2"));
        
        // Get metadata map for work item with no metadata
        Map<String, String> emptyMap = repository.getMetadataMap(UUID.randomUUID());
        assertTrue(emptyMap.isEmpty());
    }
    
    @Test
    void testDeleteById() {
        // Create metadata
        WorkItemMetadata metadata = new WorkItemMetadata(workItem1Id, "key1", "value1");
        repository.save(metadata);
        
        // Verify it exists
        assertTrue(repository.findById(metadata.getId()).isPresent());
        
        // Delete it
        boolean deleted = repository.deleteById(metadata.getId());
        assertTrue(deleted);
        
        // Verify it's gone
        assertFalse(repository.findById(metadata.getId()).isPresent());
        
        // Try to delete non-existent metadata
        boolean nonExistentDeleted = repository.deleteById(UUID.randomUUID());
        assertFalse(nonExistentDeleted);
    }
    
    @Test
    void testDeleteByWorkItemId() {
        // Create multiple metadata entries for work item 1
        WorkItemMetadata metadata1 = new WorkItemMetadata(workItem1Id, "key1", "value1");
        WorkItemMetadata metadata2 = new WorkItemMetadata(workItem1Id, "key2", "value2");
        // Create metadata for work item 2
        WorkItemMetadata metadata3 = new WorkItemMetadata(workItem2Id, "key1", "value3");
        
        repository.save(metadata1);
        repository.save(metadata2);
        repository.save(metadata3);
        
        // Delete all metadata for work item 1
        int deleted = repository.deleteByWorkItemId(workItem1Id);
        assertEquals(2, deleted);
        
        // Verify work item 1 metadata is gone
        assertTrue(repository.findByWorkItemId(workItem1Id).isEmpty());
        
        // Verify work item 2 metadata still exists
        assertEquals(1, repository.findByWorkItemId(workItem2Id).size());
        
        // Try to delete metadata for non-existent work item
        int nonExistentDeleted = repository.deleteByWorkItemId(UUID.randomUUID());
        assertEquals(0, nonExistentDeleted);
    }
    
    @Test
    void testDeleteByWorkItemIdAndKey() {
        // Create multiple metadata entries
        WorkItemMetadata metadata1 = new WorkItemMetadata(workItem1Id, "key1", "value1");
        WorkItemMetadata metadata2 = new WorkItemMetadata(workItem1Id, "key2", "value2");
        
        repository.save(metadata1);
        repository.save(metadata2);
        
        // Delete one metadata entry
        boolean deleted = repository.deleteByWorkItemIdAndKey(workItem1Id, "key1");
        assertTrue(deleted);
        
        // Verify it's gone
        Optional<WorkItemMetadata> deletedMetadata = repository.findByWorkItemIdAndKey(workItem1Id, "key1");
        assertFalse(deletedMetadata.isPresent());
        
        // Verify other metadata still exists
        Optional<WorkItemMetadata> remainingMetadata = repository.findByWorkItemIdAndKey(workItem1Id, "key2");
        assertTrue(remainingMetadata.isPresent());
        
        // Try to delete non-existent metadata
        boolean nonExistentDeleted = repository.deleteByWorkItemIdAndKey(workItem1Id, "key3");
        assertFalse(nonExistentDeleted);
    }
    
    @Test
    void testFindAll() {
        // Create multiple metadata entries
        WorkItemMetadata metadata1 = new WorkItemMetadata(workItem1Id, "key1", "value1");
        WorkItemMetadata metadata2 = new WorkItemMetadata(workItem1Id, "key2", "value2");
        WorkItemMetadata metadata3 = new WorkItemMetadata(workItem2Id, "key1", "value3");
        
        repository.save(metadata1);
        repository.save(metadata2);
        repository.save(metadata3);
        
        // Find all metadata
        List<WorkItemMetadata> allMetadata = repository.findAll();
        assertEquals(3, allMetadata.size());
        assertTrue(allMetadata.stream().anyMatch(m -> m.getId().equals(metadata1.getId())));
        assertTrue(allMetadata.stream().anyMatch(m -> m.getId().equals(metadata2.getId())));
        assertTrue(allMetadata.stream().anyMatch(m -> m.getId().equals(metadata3.getId())));
    }
    
    @Test
    void testClear() {
        // Create metadata
        WorkItemMetadata metadata1 = new WorkItemMetadata(workItem1Id, "key1", "value1");
        WorkItemMetadata metadata2 = new WorkItemMetadata(workItem2Id, "key1", "value2");
        
        repository.save(metadata1);
        repository.save(metadata2);
        
        // Verify metadata exists
        assertEquals(2, repository.findAll().size());
        
        // Clear the repository
        ((InMemoryMetadataRepository) repository).clear();
        
        // Verify all metadata is gone
        assertTrue(repository.findAll().isEmpty());
    }
}