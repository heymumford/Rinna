/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItemType;
import org.rinna.cli.domain.model.DomainWorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StateMapper Tests")
class StateMapperTest {

    @Test
    @DisplayName("Should not instantiate StateMapper")
    void shouldNotInstantiateStateMapper() {
        // Try to access the constructor directly to verify it exists
        try {
            java.lang.reflect.Constructor<StateMapper> constructor = 
                StateMapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            
            // Try invoking it and expect an exception
            assertThrows(Exception.class, () -> {
                constructor.newInstance();
            });
        } catch (NoSuchMethodException e) {
            fail("Constructor not found: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("WorkflowState Mapping Tests")
    class WorkflowStateMappingTests {
        @Test
        @DisplayName("Should map CLI WorkflowState to core WorkflowState string")
        void shouldMapCliWorkflowStateToCoreStateString() {
            // Test a few representative mappings
            assertEquals("FOUND", StateMapper.toCoreState(WorkflowState.CREATED));
            assertEquals("TO_DO", StateMapper.toCoreState(WorkflowState.READY));
            assertEquals("IN_PROGRESS", StateMapper.toCoreState(WorkflowState.IN_PROGRESS));
            assertEquals("IN_TEST", StateMapper.toCoreState(WorkflowState.TESTING));
            assertEquals("DONE", StateMapper.toCoreState(WorkflowState.DONE));
            
            // Test that null input returns null output
            assertNull(StateMapper.toCoreState(null));
        }
        
        @Test
        @DisplayName("Should map core WorkflowState string to CLI WorkflowState")
        void shouldMapCoreStateStringToCliWorkflowState() {
            // Test a few representative mappings
            assertEquals(WorkflowState.FOUND, StateMapper.fromCoreState("FOUND"));
            assertEquals(WorkflowState.TO_DO, StateMapper.fromCoreState("TO_DO"));
            assertEquals(WorkflowState.IN_PROGRESS, StateMapper.fromCoreState("IN_PROGRESS"));
            assertEquals(WorkflowState.IN_TEST, StateMapper.fromCoreState("IN_TEST"));
            assertEquals(WorkflowState.DONE, StateMapper.fromCoreState("DONE"));
            
            // Test special case: RELEASED maps to DONE in CLI
            assertEquals(WorkflowState.DONE, StateMapper.fromCoreState("RELEASED"));
            
            // Test fallback for unknown state
            assertEquals(WorkflowState.CREATED, StateMapper.fromCoreState("UNKNOWN_STATE"));
            
            // Test case insensitivity
            assertEquals(WorkflowState.IN_PROGRESS, StateMapper.fromCoreState("in_progress"));
            
            // Test that null input returns null output
            assertNull(StateMapper.fromCoreState(null));
        }
        
        @Test
        @DisplayName("Should map CLI WorkflowState to DomainWorkflowState")
        void shouldMapCliWorkflowStateToDomainWorkflowState() {
            // Test a few representative mappings
            assertEquals(DomainWorkflowState.NEW, StateMapper.toDomainState(WorkflowState.CREATED));
            assertEquals(DomainWorkflowState.IN_PROGRESS, StateMapper.toDomainState(WorkflowState.IN_PROGRESS));
            assertEquals(DomainWorkflowState.IN_TEST, StateMapper.toDomainState(WorkflowState.TESTING));
            assertEquals(DomainWorkflowState.DONE, StateMapper.toDomainState(WorkflowState.DONE));
            
            // Test that null input returns null output
            assertNull(StateMapper.toDomainState(null));
        }
        
        @Test
        @DisplayName("Should map DomainWorkflowState to CLI WorkflowState")
        void shouldMapDomainWorkflowStateToCliWorkflowState() {
            // Test a few representative mappings
            assertEquals(WorkflowState.IN_PROGRESS, StateMapper.fromDomainState(DomainWorkflowState.IN_PROGRESS));
            assertEquals(WorkflowState.IN_TEST, StateMapper.fromDomainState(DomainWorkflowState.IN_TEST));
            assertEquals(WorkflowState.DONE, StateMapper.fromDomainState(DomainWorkflowState.DONE));
            
            // Test that null input returns null output
            assertNull(StateMapper.fromDomainState(null));
        }
    }
    
    @Nested
    @DisplayName("Priority Mapping Tests")
    class PriorityMappingTests {
        @Test
        @DisplayName("Should map CLI Priority to core Priority string")
        void shouldMapCliPriorityToCoreString() {
            assertEquals("LOW", StateMapper.toCorePriority(Priority.LOW));
            assertEquals("MEDIUM", StateMapper.toCorePriority(Priority.MEDIUM));
            assertEquals("HIGH", StateMapper.toCorePriority(Priority.HIGH));
            assertEquals("CRITICAL", StateMapper.toCorePriority(Priority.CRITICAL));
            
            assertNull(StateMapper.toCorePriority(null));
        }
        
        @Test
        @DisplayName("Should map core Priority string to CLI Priority")
        void shouldMapCoreStringToCliPriority() {
            assertEquals(Priority.LOW, StateMapper.fromCorePriority("LOW"));
            assertEquals(Priority.MEDIUM, StateMapper.fromCorePriority("MEDIUM"));
            assertEquals(Priority.HIGH, StateMapper.fromCorePriority("HIGH"));
            assertEquals(Priority.CRITICAL, StateMapper.fromCorePriority("CRITICAL"));
            
            // Test fallback for unknown priority
            assertEquals(Priority.MEDIUM, StateMapper.fromCorePriority("UNKNOWN_PRIORITY"));
            
            // Test case insensitivity
            assertEquals(Priority.HIGH, StateMapper.fromCorePriority("high"));
            
            assertNull(StateMapper.fromCorePriority(null));
        }
        
        @Test
        @DisplayName("Should map CLI Priority to DomainPriority")
        void shouldMapCliPriorityToDomainPriority() {
            assertEquals(DomainPriority.LOW, StateMapper.toDomainPriority(Priority.LOW));
            assertEquals(DomainPriority.MEDIUM, StateMapper.toDomainPriority(Priority.MEDIUM));
            assertEquals(DomainPriority.HIGH, StateMapper.toDomainPriority(Priority.HIGH));
            assertEquals(DomainPriority.CRITICAL, StateMapper.toDomainPriority(Priority.CRITICAL));
            
            // Test that null input returns null output
            assertNull(StateMapper.toDomainPriority(null));
        }
        
        @Test
        @DisplayName("Should map DomainPriority to CLI Priority")
        void shouldMapDomainPriorityToCliPriority() {
            assertEquals(Priority.LOW, StateMapper.fromDomainPriority(DomainPriority.LOW));
            assertEquals(Priority.MEDIUM, StateMapper.fromDomainPriority(DomainPriority.MEDIUM));
            assertEquals(Priority.HIGH, StateMapper.fromDomainPriority(DomainPriority.HIGH));
            assertEquals(Priority.CRITICAL, StateMapper.fromDomainPriority(DomainPriority.CRITICAL));
            
            // Test that null input returns null output
            assertNull(StateMapper.fromDomainPriority(null));
        }
    }
    
    @Nested
    @DisplayName("WorkItemType Mapping Tests")
    class WorkItemTypeMappingTests {
        @Test
        @DisplayName("Should map CLI WorkItemType to core WorkItemType string")
        void shouldMapCliWorkItemTypeToCoreString() {
            assertEquals("TASK", StateMapper.toCoreType(WorkItemType.TASK));
            assertEquals("BUG", StateMapper.toCoreType(WorkItemType.BUG));
            assertEquals("FEATURE", StateMapper.toCoreType(WorkItemType.FEATURE));
            assertEquals("EPIC", StateMapper.toCoreType(WorkItemType.EPIC));
            
            assertNull(StateMapper.toCoreType(null));
        }
        
        @Test
        @DisplayName("Should map core WorkItemType string to CLI WorkItemType")
        void shouldMapCoreStringToCliWorkItemType() {
            assertEquals(WorkItemType.TASK, StateMapper.fromCoreType("TASK"));
            assertEquals(WorkItemType.BUG, StateMapper.fromCoreType("BUG"));
            assertEquals(WorkItemType.FEATURE, StateMapper.fromCoreType("FEATURE"));
            assertEquals(WorkItemType.EPIC, StateMapper.fromCoreType("EPIC"));
            
            // Test fallback for unknown type
            assertEquals(WorkItemType.TASK, StateMapper.fromCoreType("UNKNOWN_TYPE"));
            
            // Test case insensitivity
            assertEquals(WorkItemType.BUG, StateMapper.fromCoreType("bug"));
            
            assertNull(StateMapper.fromCoreType(null));
        }
        
        @Test
        @DisplayName("Should map CLI WorkItemType to DomainWorkItemType")
        void shouldMapCliWorkItemTypeToDomainWorkItemType() {
            assertEquals(DomainWorkItemType.TASK, StateMapper.toDomainType(WorkItemType.TASK));
            assertEquals(DomainWorkItemType.BUG, StateMapper.toDomainType(WorkItemType.BUG));
            assertEquals(DomainWorkItemType.FEATURE, StateMapper.toDomainType(WorkItemType.FEATURE));
            assertEquals(DomainWorkItemType.EPIC, StateMapper.toDomainType(WorkItemType.EPIC));
            
            // Test that null input returns null output
            assertNull(StateMapper.toDomainType(null));
        }
        
        @Test
        @DisplayName("Should map DomainWorkItemType to CLI WorkItemType")
        void shouldMapDomainWorkItemTypeToCliWorkItemType() {
            assertEquals(WorkItemType.TASK, StateMapper.fromDomainType(DomainWorkItemType.TASK));
            assertEquals(WorkItemType.BUG, StateMapper.fromDomainType(DomainWorkItemType.BUG));
            assertEquals(WorkItemType.FEATURE, StateMapper.fromDomainType(DomainWorkItemType.FEATURE));
            assertEquals(WorkItemType.EPIC, StateMapper.fromDomainType(DomainWorkItemType.EPIC));
            
            // Test that null input returns null output
            assertNull(StateMapper.fromDomainType(null));
        }
    }
    
    @Test
    @DisplayName("Should handle null inputs for all mapping methods")
    void shouldHandleNullInputsForAllMappingMethods() {
        assertNull(StateMapper.toCoreState(null));
        assertNull(StateMapper.fromCoreState(null));
        assertNull(StateMapper.toCoreType(null));
        assertNull(StateMapper.fromCoreType(null));
        assertNull(StateMapper.toCorePriority(null));
        assertNull(StateMapper.fromCorePriority(null));
        
        assertNull(StateMapper.toDomainState(null));
        assertNull(StateMapper.fromDomainState(null));
        assertNull(StateMapper.toDomainType(null));
        assertNull(StateMapper.fromDomainType(null));
        assertNull(StateMapper.toDomainPriority(null));
        assertNull(StateMapper.fromDomainPriority(null));
    }
}