/*
 * Unit tests for the DefaultRyorindoWorkItem class
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.base.UnitTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DefaultRyorindoWorkItem class.
 */
@DisplayName("DefaultRyorindoWorkItem Tests")
public class DefaultRyorindoWorkItemTest extends UnitTest {
    
    private WorkItemRecord baseWorkItem;
    private DefaultRyorindoWorkItem ryorindoWorkItem;
    
    @BeforeEach
    void setUp() {
        // Create a basic work item to enhance
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        baseWorkItem = new WorkItemRecord(
            id,
            "Test Work Item",
            "Description for test work item",
            WorkItemType.TASK,
            WorkflowState.FOUND,
            Priority.MEDIUM,
            "testuser@example.com",
            now,
            now,
            null, // no parent
            UUID.randomUUID(), // project ID
            "PUBLIC",
            false
        );
        
        // Create the Ryorin-do enhanced work item
        ryorindoWorkItem = new DefaultRyorindoWorkItem(baseWorkItem);
    }
    
    @Test
    @DisplayName("Should correctly wrap the base work item")
    void shouldCorrectlyWrapBaseWorkItem() {
        assertSame(baseWorkItem, ryorindoWorkItem.getWorkItem());
        assertEquals(baseWorkItem.getTitle(), ryorindoWorkItem.getWorkItem().getTitle());
        assertEquals(baseWorkItem.getDescription(), ryorindoWorkItem.getWorkItem().getDescription());
    }
    
    @Test
    @DisplayName("Should correctly manage CYNEFIN domain")
    void shouldCorrectlyManageCynefinDomain() {
        // Initially no domain is set
        assertTrue(ryorindoWorkItem.getCynefinDomain().isEmpty());
        
        // Set the domain
        ryorindoWorkItem.setCynefinDomain(CynefinDomain.COMPLEX);
        
        // Verify the domain is set correctly
        assertTrue(ryorindoWorkItem.getCynefinDomain().isPresent());
        assertEquals(CynefinDomain.COMPLEX, ryorindoWorkItem.getCynefinDomain().get());
    }
    
    @Test
    @DisplayName("Should correctly manage work paradigm")
    void shouldCorrectlyManageWorkParadigm() {
        // Initially no paradigm is set
        assertTrue(ryorindoWorkItem.getWorkParadigm().isEmpty());
        
        // Set the paradigm
        ryorindoWorkItem.setWorkParadigm(WorkParadigm.EXPERIMENT);
        
        // Verify the paradigm is set correctly
        assertTrue(ryorindoWorkItem.getWorkParadigm().isPresent());
        assertEquals(WorkParadigm.EXPERIMENT, ryorindoWorkItem.getWorkParadigm().get());
    }
    
    @Test
    @DisplayName("Should correctly manage assignees")
    void shouldCorrectlyManageAssignees() {
        // Initially only the base assignee is present
        List<String> assignees = ryorindoWorkItem.getAssignees();
        assertEquals(1, assignees.size());
        assertEquals("testuser@example.com", assignees.get(0));
        
        // Add an assignee
        ryorindoWorkItem.addAssignee("developer@example.com");
        
        // Verify the assignee was added
        assignees = ryorindoWorkItem.getAssignees();
        assertEquals(2, assignees.size());
        assertTrue(assignees.contains("developer@example.com"));
        
        // Remove an assignee
        ryorindoWorkItem.removeAssignee("testuser@example.com");
        
        // Verify the assignee was removed
        assignees = ryorindoWorkItem.getAssignees();
        assertEquals(1, assignees.size());
        assertEquals("developer@example.com", assignees.get(0));
    }
    
    @Test
    @DisplayName("Should correctly manage due date")
    void shouldCorrectlyManageDueDate() {
        // Initially no due date is set
        assertTrue(ryorindoWorkItem.getDueDate().isEmpty());
        
        // Set the due date
        Instant dueDate = Instant.now().plusSeconds(86400); // one day in the future
        ryorindoWorkItem.setDueDate(dueDate);
        
        // Verify the due date is set correctly
        assertTrue(ryorindoWorkItem.getDueDate().isPresent());
        assertEquals(dueDate, ryorindoWorkItem.getDueDate().get());
    }
    
    @Test
    @DisplayName("Should correctly manage effort estimates")
    void shouldCorrectlyManageEffortEstimates() {
        // Initially no effort estimates are set
        assertTrue(ryorindoWorkItem.getEstimatedEffort().isEmpty());
        assertTrue(ryorindoWorkItem.getActualEffort().isEmpty());
        
        // Set the estimates
        ryorindoWorkItem.setEstimatedEffort(8.0);
        ryorindoWorkItem.setActualEffort(10.5);
        
        // Verify the estimates are set correctly
        assertTrue(ryorindoWorkItem.getEstimatedEffort().isPresent());
        assertEquals(8.0, ryorindoWorkItem.getEstimatedEffort().get());
        
        assertTrue(ryorindoWorkItem.getActualEffort().isPresent());
        assertEquals(10.5, ryorindoWorkItem.getActualEffort().get());
    }
    
    @Test
    @DisplayName("Should correctly manage outcome and key results")
    void shouldCorrectlyManageOutcomeAndKeyResults() {
        // Initially no outcome or key results are set
        assertTrue(ryorindoWorkItem.getOutcome().isEmpty());
        assertTrue(ryorindoWorkItem.getKeyResults().isEmpty());
        
        // Set the outcome and key results
        ryorindoWorkItem.setOutcome("Improve user login experience");
        ryorindoWorkItem.addKeyResult("Login time reduced by 50%");
        ryorindoWorkItem.addKeyResult("Failed login attempts reduced by 30%");
        
        // Verify the outcome and key results are set correctly
        assertTrue(ryorindoWorkItem.getOutcome().isPresent());
        assertEquals("Improve user login experience", ryorindoWorkItem.getOutcome().get());
        
        List<String> keyResults = ryorindoWorkItem.getKeyResults();
        assertEquals(2, keyResults.size());
        assertTrue(keyResults.contains("Login time reduced by 50%"));
        assertTrue(keyResults.contains("Failed login attempts reduced by 30%"));
    }
    
    @Test
    @DisplayName("Should correctly manage dependencies and related items")
    void shouldCorrectlyManageDependenciesAndRelatedItems() {
        // Initially no dependencies or related items are set
        assertTrue(ryorindoWorkItem.getDependencies().isEmpty());
        assertTrue(ryorindoWorkItem.getRelatedItems().isEmpty());
        
        // Create some IDs
        UUID dependency1 = UUID.randomUUID();
        UUID dependency2 = UUID.randomUUID();
        UUID related1 = UUID.randomUUID();
        
        // Add the dependencies and related items
        ryorindoWorkItem.addDependency(dependency1);
        ryorindoWorkItem.addDependency(dependency2);
        ryorindoWorkItem.addRelatedItem(related1);
        
        // Verify they are set correctly
        List<UUID> dependencies = ryorindoWorkItem.getDependencies();
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains(dependency1));
        assertTrue(dependencies.contains(dependency2));
        
        List<UUID> relatedItems = ryorindoWorkItem.getRelatedItems();
        assertEquals(1, relatedItems.size());
        assertTrue(relatedItems.contains(related1));
    }
    
    @Test
    @DisplayName("Should correctly manage cognitive load assessment")
    void shouldCorrectlyManageCognitiveLoadAssessment() {
        // Initially no cognitive load is set
        assertTrue(ryorindoWorkItem.getCognitiveLoadAssessment().isEmpty());
        
        // Set the cognitive load
        ryorindoWorkItem.setCognitiveLoadAssessment(7);
        
        // Verify it is set correctly
        assertTrue(ryorindoWorkItem.getCognitiveLoadAssessment().isPresent());
        assertEquals(7, ryorindoWorkItem.getCognitiveLoadAssessment().get());
        
        // Verify validation
        assertThrows(IllegalArgumentException.class, () -> ryorindoWorkItem.setCognitiveLoadAssessment(11));
        assertThrows(IllegalArgumentException.class, () -> ryorindoWorkItem.setCognitiveLoadAssessment(0));
    }
    
    @Test
    @DisplayName("Should correctly manage AI recommendations")
    void shouldCorrectlyManageAiRecommendations() {
        // Initially no recommendations are set
        assertTrue(ryorindoWorkItem.getAiRecommendations().isEmpty());
        
        // Add recommendations
        ryorindoWorkItem.addAiRecommendation("Consider breaking this into smaller tasks");
        ryorindoWorkItem.addAiRecommendation("This task may require pairing with another developer");
        
        // Verify they are set correctly
        List<String> recommendations = ryorindoWorkItem.getAiRecommendations();
        assertEquals(2, recommendations.size());
        assertTrue(recommendations.contains("Consider breaking this into smaller tasks"));
        assertTrue(recommendations.contains("This task may require pairing with another developer"));
    }
    
    @Test
    @DisplayName("Should correctly manage knowledge links")
    void shouldCorrectlyManageKnowledgeLinks() {
        // Initially no knowledge links are set
        assertTrue(ryorindoWorkItem.getKnowledgeLinks().isEmpty());
        
        // Add knowledge links
        ryorindoWorkItem.addKnowledgeLink("https://wiki.example.com/authentication");
        ryorindoWorkItem.addKnowledgeLink("https://api.example.com/docs");
        
        // Verify they are set correctly
        List<String> links = ryorindoWorkItem.getKnowledgeLinks();
        assertEquals(2, links.size());
        assertTrue(links.contains("https://wiki.example.com/authentication"));
        assertTrue(links.contains("https://api.example.com/docs"));
    }
    
    @Test
    @DisplayName("Should correctly manage workstream associations")
    void shouldCorrectlyManageWorkstreamAssociations() {
        // Initially no workstreams are associated
        assertTrue(ryorindoWorkItem.getWorkstreamIds().isEmpty());
        
        // Add workstream associations
        UUID workstream1 = UUID.randomUUID();
        UUID workstream2 = UUID.randomUUID();
        ryorindoWorkItem.addWorkstream(workstream1);
        ryorindoWorkItem.addWorkstream(workstream2);
        
        // Verify they are set correctly
        List<UUID> workstreams = ryorindoWorkItem.getWorkstreamIds();
        assertEquals(2, workstreams.size());
        assertTrue(workstreams.contains(workstream1));
        assertTrue(workstreams.contains(workstream2));
    }
    
    @Test
    @DisplayName("Should correctly manage allocation percentage")
    void shouldCorrectlyManageAllocationPercentage() {
        // Initially no allocation is set
        assertTrue(ryorindoWorkItem.getAllocation().isEmpty());
        
        // Set the allocation
        ryorindoWorkItem.setAllocation(75);
        
        // Verify it is set correctly
        assertTrue(ryorindoWorkItem.getAllocation().isPresent());
        assertEquals(75, ryorindoWorkItem.getAllocation().get());
        
        // Verify validation
        assertThrows(IllegalArgumentException.class, () -> ryorindoWorkItem.setAllocation(101));
        assertThrows(IllegalArgumentException.class, () -> ryorindoWorkItem.setAllocation(-1));
    }
    
    @Test
    @DisplayName("Should correctly manage category")
    void shouldCorrectlyManageCategory() {
        // Initially no category is set
        assertTrue(ryorindoWorkItem.getCategory().isEmpty());
        
        // Set the category
        ryorindoWorkItem.setCategory("PROD");
        
        // Verify it is set correctly
        assertTrue(ryorindoWorkItem.getCategory().isPresent());
        assertEquals("PROD", ryorindoWorkItem.getCategory().get());
    }
    
    @Test
    @DisplayName("Should use CYNEFIN domain to determine approach and characteristics")
    void shouldUseCynefinDomainToDetermineApproachAndCharacteristics() {
        // Set the domain to Complex
        ryorindoWorkItem.setCynefinDomain(CynefinDomain.COMPLEX);
        
        // Verify the approach and characteristics
        assertTrue(ryorindoWorkItem.getRecommendedApproach().isPresent());
        assertTrue(ryorindoWorkItem.getRecommendedApproach().get().contains("Probe"));
        
        assertFalse(ryorindoWorkItem.requiresExpertAnalysis());
        assertTrue(ryorindoWorkItem.requiresExperimentation());
        assertFalse(ryorindoWorkItem.requiresImmediateAction());
    }
}