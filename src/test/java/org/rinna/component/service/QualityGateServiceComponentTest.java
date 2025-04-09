/*
 * Component tests for QualityGateService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.component.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.adapter.repository.InMemoryQualityGateRepository;
import org.rinna.adapter.service.DefaultQualityGateService;
import org.rinna.component.base.ComponentTest;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.QualityGateRepository;
import org.rinna.usecase.QualityGateService;
import org.rinna.usecase.WorkflowService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Component tests for QualityGateService.
 * These tests verify the behavior of the QualityGateService component
 * with real implementations of in-module dependencies and mocks for external dependencies.
 */
@DisplayName("Quality Gate Service Component Tests")
class QualityGateServiceComponentTest extends ComponentTest {

    private QualityGateService qualityGateService;
    
    private QualityGateRepository qualityGateRepository;
    
    @Mock
    private ItemRepository itemRepository;
    
    @Mock
    private WorkflowService workflowService;
    
    @Mock
    private WorkItem mockWorkItem;
    
    private UUID workItemId;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Use real implementation of QualityGateRepository
        qualityGateRepository = new InMemoryQualityGateRepository();
        
        // Setup the QualityGateService with real and mock dependencies
        qualityGateService = new DefaultQualityGateService(
                qualityGateRepository, itemRepository, workflowService);
        
        // Setup mock work item
        workItemId = UUID.randomUUID();
        when(mockWorkItem.getId()).thenReturn(workItemId);
        when(mockWorkItem.getTitle()).thenReturn("Test Work Item");
        when(mockWorkItem.getStatus()).thenReturn(WorkflowState.IN_PROGRESS);
        
        // Setup ItemRepository mock
        when(itemRepository.findById(workItemId)).thenReturn(Optional.of(mockWorkItem));
    }
    
    @Test
    @DisplayName("Should create quality gate configuration for a project")
    void shouldCreateQualityGateConfigurationForProject() {
        // Given
        String projectId = "test-project";
        Map<String, Object> gateConfig = new HashMap<>();
        
        // Add gate for IN_PROGRESS to IN_TEST transition
        Map<String, Object> inProgressToInTestGate = new HashMap<>();
        inProgressToInTestGate.put("requiredChecks", Arrays.asList("UnitTestsPassing", "CodeCoverageMinimum"));
        gateConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, inProgressToInTestGate);
        
        // Add gate for IN_TEST to DONE transition
        Map<String, Object> inTestToDoneGate = new HashMap<>();
        inTestToDoneGate.put("requiredChecks", Arrays.asList("IntegrationTestsPassing", "SecurityScanComplete"));
        gateConfig.put(WorkflowState.IN_TEST + "_to_" + WorkflowState.DONE, inTestToDoneGate);
        
        // When
        boolean result = qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // Then
        assertTrue(result);
        Optional<Map<String, Object>> savedConfig = qualityGateRepository.findByProjectId(projectId);
        assertTrue(savedConfig.isPresent());
        assertEquals(gateConfig, savedConfig.get());
    }
    
    @Test
    @DisplayName("Should update existing quality gate configuration")
    void shouldUpdateExistingQualityGateConfiguration() {
        // Given
        String projectId = "test-project";
        
        // Create initial configuration
        Map<String, Object> initialConfig = new HashMap<>();
        Map<String, Object> initialGate = new HashMap<>();
        initialGate.put("requiredChecks", Arrays.asList("UnitTestsPassing"));
        initialConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, initialGate);
        qualityGateService.createQualityGateConfiguration(projectId, initialConfig);
        
        // Updated configuration
        Map<String, Object> updatedConfig = new HashMap<>();
        Map<String, Object> updatedGate = new HashMap<>();
        updatedGate.put("requiredChecks", Arrays.asList("UnitTestsPassing", "CodeCoverageMinimum"));
        updatedConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, updatedGate);
        
        // When
        boolean result = qualityGateService.updateQualityGateConfiguration(projectId, updatedConfig);
        
        // Then
        assertTrue(result);
        Optional<Map<String, Object>> savedConfig = qualityGateRepository.findByProjectId(projectId);
        assertTrue(savedConfig.isPresent());
        assertEquals(updatedConfig, savedConfig.get());
    }
    
    @Test
    @DisplayName("Should validate transition with passing quality gates")
    void shouldValidateTransitionWithPassingQualityGates() {
        // Given
        String projectId = "test-project";
        
        // Setup quality gate configuration
        Map<String, Object> gateConfig = new HashMap<>();
        Map<String, Object> gate = new HashMap<>();
        gate.put("requiredChecks", Arrays.asList("UnitTestsPassing", "CodeCoverageMinimum"));
        gateConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, gate);
        qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // Setup check results
        Map<String, Boolean> checkResults = new HashMap<>();
        checkResults.put("UnitTestsPassing", true);
        checkResults.put("CodeCoverageMinimum", true);
        
        // When
        boolean result = qualityGateService.validateTransition(workItemId, projectId, 
                WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST, checkResults);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should fail validation with failing quality gates")
    void shouldFailValidationWithFailingQualityGates() {
        // Given
        String projectId = "test-project";
        
        // Setup quality gate configuration
        Map<String, Object> gateConfig = new HashMap<>();
        Map<String, Object> gate = new HashMap<>();
        gate.put("requiredChecks", Arrays.asList("UnitTestsPassing", "CodeCoverageMinimum"));
        gateConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, gate);
        qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // Setup check results with a failing check
        Map<String, Boolean> checkResults = new HashMap<>();
        checkResults.put("UnitTestsPassing", true);
        checkResults.put("CodeCoverageMinimum", false);
        
        // When
        boolean result = qualityGateService.validateTransition(workItemId, projectId, 
                WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST, checkResults);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should fail validation when missing required checks")
    void shouldFailValidationWhenMissingRequiredChecks() {
        // Given
        String projectId = "test-project";
        
        // Setup quality gate configuration
        Map<String, Object> gateConfig = new HashMap<>();
        Map<String, Object> gate = new HashMap<>();
        gate.put("requiredChecks", Arrays.asList("UnitTestsPassing", "CodeCoverageMinimum"));
        gateConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, gate);
        qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // Setup check results with a missing check
        Map<String, Boolean> checkResults = new HashMap<>();
        checkResults.put("UnitTestsPassing", true);
        // Missing CodeCoverageMinimum
        
        // When
        boolean result = qualityGateService.validateTransition(workItemId, projectId, 
                WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST, checkResults);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should pass validation when no quality gate exists for transition")
    void shouldPassValidationWhenNoQualityGateExistsForTransition() {
        // Given
        String projectId = "test-project";
        
        // Setup quality gate configuration for a different transition
        Map<String, Object> gateConfig = new HashMap<>();
        Map<String, Object> gate = new HashMap<>();
        gate.put("requiredChecks", Arrays.asList("UnitTestsPassing"));
        gateConfig.put(WorkflowState.IN_TEST + "_to_" + WorkflowState.DONE, gate);
        qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // When - validate a transition that has no gate
        boolean result = qualityGateService.validateTransition(workItemId, projectId, 
                WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST, Collections.emptyMap());
        
        // Then - should pass because there's no gate for this transition
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should pass validation when project has no quality gates")
    void shouldPassValidationWhenProjectHasNoQualityGates() {
        // Given
        String projectId = "test-project";
        // No quality gates configured
        
        // When
        boolean result = qualityGateService.validateTransition(workItemId, projectId, 
                WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST, Collections.emptyMap());
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should transition work item with passing quality gates")
    void shouldTransitionWorkItemWithPassingQualityGates() {
        // Given
        String projectId = "test-project";
        
        // Setup quality gate configuration
        Map<String, Object> gateConfig = new HashMap<>();
        Map<String, Object> gate = new HashMap<>();
        gate.put("requiredChecks", Arrays.asList("UnitTestsPassing"));
        gateConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, gate);
        qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // Setup check results
        Map<String, Boolean> checkResults = new HashMap<>();
        checkResults.put("UnitTestsPassing", true);
        
        // Setup workflow service mock
        when(workflowService.transition(workItemId, WorkflowState.IN_TEST)).thenReturn(mockWorkItem);
        
        // When
        WorkItem result = qualityGateService.transitionWithQualityGate(workItemId, projectId, 
                WorkflowState.IN_TEST, checkResults);
        
        // Then
        assertNotNull(result);
        verify(workflowService).transition(workItemId, WorkflowState.IN_TEST);
    }
    
    @Test
    @DisplayName("Should throw exception when transitioning with failing quality gates")
    void shouldThrowExceptionWhenTransitioningWithFailingQualityGates() {
        // Given
        String projectId = "test-project";
        
        // Setup quality gate configuration
        Map<String, Object> gateConfig = new HashMap<>();
        Map<String, Object> gate = new HashMap<>();
        gate.put("requiredChecks", Arrays.asList("UnitTestsPassing"));
        gateConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, gate);
        qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // Setup check results with a failing check
        Map<String, Boolean> checkResults = new HashMap<>();
        checkResults.put("UnitTestsPassing", false);
        
        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            qualityGateService.transitionWithQualityGate(workItemId, projectId, 
                    WorkflowState.IN_TEST, checkResults);
        });
        
        // Verify workflowService was not called
        verify(workflowService, never()).transition(any(), any());
    }
    
    @Test
    @DisplayName("Should get required checks for a transition")
    void shouldGetRequiredChecksForTransition() {
        // Given
        String projectId = "test-project";
        
        // Setup quality gate configuration
        Map<String, Object> gateConfig = new HashMap<>();
        Map<String, Object> gate = new HashMap<>();
        gate.put("requiredChecks", Arrays.asList("UnitTestsPassing", "CodeCoverageMinimum"));
        gateConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, gate);
        qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // When
        List<String> requiredChecks = qualityGateService.getRequiredChecks(projectId, 
                WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST);
        
        // Then
        assertNotNull(requiredChecks);
        assertEquals(2, requiredChecks.size());
        assertTrue(requiredChecks.contains("UnitTestsPassing"));
        assertTrue(requiredChecks.contains("CodeCoverageMinimum"));
    }
    
    @Test
    @DisplayName("Should return empty list when no quality gate exists for transition")
    void shouldReturnEmptyListWhenNoQualityGateExistsForTransition() {
        // Given
        String projectId = "test-project";
        
        // When
        List<String> requiredChecks = qualityGateService.getRequiredChecks(projectId, 
                WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST);
        
        // Then
        assertNotNull(requiredChecks);
        assertTrue(requiredChecks.isEmpty());
    }
    
    @Test
    @DisplayName("Should bypass quality gate with valid reason")
    void shouldBypassQualityGateWithValidReason() {
        // Given
        String projectId = "test-project";
        
        // Setup quality gate configuration
        Map<String, Object> gateConfig = new HashMap<>();
        Map<String, Object> gate = new HashMap<>();
        gate.put("requiredChecks", Arrays.asList("UnitTestsPassing"));
        gateConfig.put(WorkflowState.IN_PROGRESS + "_to_" + WorkflowState.IN_TEST, gate);
        qualityGateService.createQualityGateConfiguration(projectId, gateConfig);
        
        // Setup check results with a failing check
        Map<String, Boolean> checkResults = new HashMap<>();
        checkResults.put("UnitTestsPassing", false);
        
        // Setup workflow service mock
        when(workflowService.transition(workItemId, WorkflowState.IN_TEST)).thenReturn(mockWorkItem);
        
        // When
        WorkItem result = qualityGateService.bypassQualityGate(workItemId, projectId, 
                WorkflowState.IN_TEST, "Critical security fix", "manager1");
        
        // Then
        assertNotNull(result);
        verify(workflowService).transition(workItemId, WorkflowState.IN_TEST);
        
        // Verify the bypass was recorded
        verify(itemRepository).updateMetadata(eq(workItemId), argThat(map -> 
            map.containsKey("quality_gate_bypassed") &&
            map.containsKey("bypass_reason") &&
            map.containsKey("bypassed_by")
        ));
    }
}