/*
 * Test class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.repository.QualityGateRepository;

/**
 * Test class for the InMemoryQualityGateRepository.
 */
public class InMemoryQualityGateRepositoryTest {

    private QualityGateRepository repository;
    private String projectId1;
    private String projectId2;
    private UUID workItemId1;
    private UUID workItemId2;

    @BeforeEach
    void setUp() {
        repository = new InMemoryQualityGateRepository();
        projectId1 = "project-123";
        projectId2 = "project-456";
        workItemId1 = UUID.randomUUID();
        workItemId2 = UUID.randomUUID();
    }

    @Test
    void testSaveAndFindConfiguration() {
        // Create a configuration
        Map<String, Object> config = new HashMap<>();
        config.put("minTestCoverage", 80);
        config.put("maxComplexity", 15);
        config.put("requiredReviewers", 2);
        
        // Save the configuration
        boolean saved = repository.save(projectId1, config);
        assertTrue(saved);
        
        // Find the configuration
        Optional<Map<String, Object>> foundConfig = repository.findByProjectId(projectId1);
        assertTrue(foundConfig.isPresent());
        assertEquals(80, foundConfig.get().get("minTestCoverage"));
        assertEquals(15, foundConfig.get().get("maxComplexity"));
        assertEquals(2, foundConfig.get().get("requiredReviewers"));
    }
    
    @Test
    void testConfigurationNotFound() {
        Optional<Map<String, Object>> notFoundConfig = repository.findByProjectId("non-existent-project");
        assertFalse(notFoundConfig.isPresent());
    }
    
    @Test
    void testUpdateConfiguration() {
        // Create and save initial configuration
        Map<String, Object> initialConfig = new HashMap<>();
        initialConfig.put("minTestCoverage", 80);
        initialConfig.put("maxComplexity", 15);
        repository.save(projectId1, initialConfig);
        
        // Create updated configuration
        Map<String, Object> updatedConfig = new HashMap<>();
        updatedConfig.put("minTestCoverage", 90);
        updatedConfig.put("maxComplexity", 10);
        updatedConfig.put("requiredReviewers", 3);
        
        // Update the configuration
        boolean updated = repository.save(projectId1, updatedConfig);
        assertTrue(updated);
        
        // Find the updated configuration
        Optional<Map<String, Object>> foundConfig = repository.findByProjectId(projectId1);
        assertTrue(foundConfig.isPresent());
        assertEquals(90, foundConfig.get().get("minTestCoverage"));
        assertEquals(10, foundConfig.get().get("maxComplexity"));
        assertEquals(3, foundConfig.get().get("requiredReviewers"));
    }
    
    @Test
    void testDeleteConfiguration() {
        // Create and save a configuration
        Map<String, Object> config = new HashMap<>();
        config.put("minTestCoverage", 80);
        repository.save(projectId1, config);
        
        // Verify the configuration exists
        assertTrue(repository.findByProjectId(projectId1).isPresent());
        
        // Delete the configuration
        boolean deleted = repository.delete(projectId1);
        assertTrue(deleted);
        
        // Verify the configuration was deleted
        assertFalse(repository.findByProjectId(projectId1).isPresent());
        
        // Try to delete a non-existent configuration
        boolean nonExistentDeleted = repository.delete("non-existent-project");
        assertFalse(nonExistentDeleted);
    }
    
    @Test
    void testSaveAndFindHistoryEntry() {
        // Create a history entry
        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("gate", "code-review");
        historyEntry.put("status", "passed");
        historyEntry.put("timestamp", System.currentTimeMillis());
        historyEntry.put("details", Map.of("reviewers", List.of("alice", "bob"), "comments", 5));
        
        // Save the history entry
        UUID entryId = repository.saveHistoryEntry(workItemId1, historyEntry);
        assertNotNull(entryId);
        
        // Find the history entry
        List<Map<String, Object>> historyEntries = repository.findHistoryByWorkItemId(workItemId1);
        assertEquals(1, historyEntries.size());
        
        Map<String, Object> foundEntry = historyEntries.get(0);
        assertEquals(entryId, foundEntry.get("id"));
        assertEquals("code-review", foundEntry.get("gate"));
        assertEquals("passed", foundEntry.get("status"));
        assertTrue(foundEntry.containsKey("timestamp"));
        
        Map<String, Object> details = (Map<String, Object>) foundEntry.get("details");
        assertNotNull(details);
        assertEquals(5, details.get("comments"));
        
        @SuppressWarnings("unchecked")
        List<String> reviewers = (List<String>) details.get("reviewers");
        assertNotNull(reviewers);
        assertEquals(2, reviewers.size());
        assertTrue(reviewers.contains("alice"));
        assertTrue(reviewers.contains("bob"));
    }
    
    @Test
    void testFindHistoryEmpty() {
        List<Map<String, Object>> historyEntries = repository.findHistoryByWorkItemId(UUID.randomUUID());
        assertTrue(historyEntries.isEmpty());
    }
    
    @Test
    void testMultipleHistoryEntries() {
        // Create and save multiple history entries
        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("gate", "code-review");
        entry1.put("status", "passed");
        entry1.put("timestamp", System.currentTimeMillis());
        
        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("gate", "test-coverage");
        entry2.put("status", "failed");
        entry2.put("timestamp", System.currentTimeMillis());
        entry2.put("details", Map.of("actual", 75, "required", 80));
        
        UUID entryId1 = repository.saveHistoryEntry(workItemId1, entry1);
        UUID entryId2 = repository.saveHistoryEntry(workItemId1, entry2);
        
        // Find all history entries
        List<Map<String, Object>> historyEntries = repository.findHistoryByWorkItemId(workItemId1);
        assertEquals(2, historyEntries.size());
        
        // Verify entries are returned correctly
        assertTrue(historyEntries.stream().anyMatch(e -> e.get("id").equals(entryId1)));
        assertTrue(historyEntries.stream().anyMatch(e -> e.get("id").equals(entryId2)));
        
        Optional<Map<String, Object>> foundEntry2 = historyEntries.stream()
                .filter(e -> e.get("id").equals(entryId2))
                .findFirst();
        
        assertTrue(foundEntry2.isPresent());
        assertEquals("test-coverage", foundEntry2.get().get("gate"));
        assertEquals("failed", foundEntry2.get().get("status"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) foundEntry2.get().get("details");
        assertNotNull(details);
        assertEquals(75, details.get("actual"));
        assertEquals(80, details.get("required"));
    }
    
    @Test
    void testHistoryEntriesForDifferentWorkItems() {
        // Create and save history entries for different work items
        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("gate", "code-review");
        entry1.put("status", "passed");
        
        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("gate", "test-coverage");
        entry2.put("status", "passed");
        
        repository.saveHistoryEntry(workItemId1, entry1);
        repository.saveHistoryEntry(workItemId2, entry2);
        
        // Verify entries are separated by work item
        List<Map<String, Object>> workItem1Entries = repository.findHistoryByWorkItemId(workItemId1);
        assertEquals(1, workItem1Entries.size());
        assertEquals("code-review", workItem1Entries.get(0).get("gate"));
        
        List<Map<String, Object>> workItem2Entries = repository.findHistoryByWorkItemId(workItemId2);
        assertEquals(1, workItem2Entries.size());
        assertEquals("test-coverage", workItem2Entries.get(0).get("gate"));
    }
    
    @Test
    void testMultipleProjects() {
        // Create and save configurations for different projects
        Map<String, Object> config1 = new HashMap<>();
        config1.put("minTestCoverage", 80);
        
        Map<String, Object> config2 = new HashMap<>();
        config2.put("minTestCoverage", 90);
        
        repository.save(projectId1, config1);
        repository.save(projectId2, config2);
        
        // Verify configurations are separated by project
        Optional<Map<String, Object>> project1Config = repository.findByProjectId(projectId1);
        assertTrue(project1Config.isPresent());
        assertEquals(80, project1Config.get().get("minTestCoverage"));
        
        Optional<Map<String, Object>> project2Config = repository.findByProjectId(projectId2);
        assertTrue(project2Config.isPresent());
        assertEquals(90, project2Config.get().get("minTestCoverage"));
    }
    
    @Test
    void testConfigurationDefensiveCopy() {
        // Create and save a configuration
        Map<String, Object> config = new HashMap<>();
        config.put("minTestCoverage", 80);
        repository.save(projectId1, config);
        
        // Modify the original map
        config.put("minTestCoverage", 90);
        
        // Verify the stored configuration is not affected
        Optional<Map<String, Object>> foundConfig = repository.findByProjectId(projectId1);
        assertTrue(foundConfig.isPresent());
        assertEquals(80, foundConfig.get().get("minTestCoverage"));
        
        // Modify the returned map
        foundConfig.get().put("minTestCoverage", 70);
        
        // Verify the stored configuration is not affected
        Optional<Map<String, Object>> refetchedConfig = repository.findByProjectId(projectId1);
        assertTrue(refetchedConfig.isPresent());
        assertEquals(80, refetchedConfig.get().get("minTestCoverage"));
    }
    
    @Test
    void testHistoryDefensiveCopy() {
        // Create a history entry with nested structures
        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("gate", "code-review");
        historyEntry.put("status", "passed");
        historyEntry.put("details", new HashMap<>(Map.of("reviewers", new ArrayList<>(List.of("alice", "bob")))));
        
        // Save the entry
        repository.saveHistoryEntry(workItemId1, historyEntry);
        
        // Modify the original map
        historyEntry.put("status", "failed");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) historyEntry.get("details");
        @SuppressWarnings("unchecked")
        List<String> reviewers = (List<String>) details.get("reviewers");
        reviewers.add("charlie");
        
        // Find the history entry
        List<Map<String, Object>> historyEntries = repository.findHistoryByWorkItemId(workItemId1);
        assertEquals(1, historyEntries.size());
        
        // Verify the stored entry is not affected by changes to the original map
        Map<String, Object> foundEntry = historyEntries.get(0);
        assertEquals("passed", foundEntry.get("status"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> foundDetails = (Map<String, Object>) foundEntry.get("details");
        assertNotNull(foundDetails);
        
        @SuppressWarnings("unchecked")
        List<String> foundReviewers = (List<String>) foundDetails.get("reviewers");
        assertNotNull(foundReviewers);
        assertEquals(2, foundReviewers.size());
        assertFalse(foundReviewers.contains("charlie"));
        
        // Modify the returned map
        foundEntry.put("status", "failed");
        @SuppressWarnings("unchecked")
        List<String> modifiedReviewers = (List<String>) ((Map<String, Object>) foundEntry.get("details")).get("reviewers");
        modifiedReviewers.add("dave");
        
        // Find the history entry again
        List<Map<String, Object>> refetchedEntries = repository.findHistoryByWorkItemId(workItemId1);
        assertEquals(1, refetchedEntries.size());
        
        // Verify the stored entry is not affected by changes to the returned map
        Map<String, Object> refetchedEntry = refetchedEntries.get(0);
        assertEquals("passed", refetchedEntry.get("status"));
        
        @SuppressWarnings("unchecked")
        List<String> refetchedReviewers = (List<String>) ((Map<String, Object>) refetchedEntry.get("details")).get("reviewers");
        assertNotNull(refetchedReviewers);
        assertEquals(2, refetchedReviewers.size());
        assertFalse(refetchedReviewers.contains("dave"));
    }
}