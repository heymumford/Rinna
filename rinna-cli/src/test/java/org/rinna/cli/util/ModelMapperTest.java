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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.rinna.cli.domain.model.DefaultDomainWorkItem;
import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkItemType;
import org.rinna.cli.domain.model.DomainWorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for the ModelMapper utility class.
 */
@DisplayName("ModelMapper Tests")
class ModelMapperTest {

    @Test
    @DisplayName("Should not instantiate ModelMapper")
    void shouldNotInstantiateModelMapper() {
        // Try to access the constructor directly to verify it exists
        try {
            java.lang.reflect.Constructor<ModelMapper> constructor = 
                ModelMapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            
            // Try invoking it and expect an exception
            assertThrows(UnsupportedOperationException.class, () -> {
                constructor.newInstance();
            });
        } catch (NoSuchMethodException e) {
            fail("Constructor not found: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("State Conversion Tests")
    class StateConversionTests {
        @Test
        @DisplayName("Should convert CLI workflow state to domain workflow state string")
        void shouldConvertCliWorkflowStateToDomainWorkflowStateString() {
            // Test a few representative mappings
            assertEquals("FOUND", ModelMapper.toDomainWorkflowState(WorkflowState.CREATED));
            assertEquals("TO_DO", ModelMapper.toDomainWorkflowState(WorkflowState.READY));
            assertEquals("IN_PROGRESS", ModelMapper.toDomainWorkflowState(WorkflowState.IN_PROGRESS));
            assertEquals("DONE", ModelMapper.toDomainWorkflowState(WorkflowState.DONE));
            
            // Test that null input returns null output
            assertNull(ModelMapper.toDomainWorkflowState(null));
        }
        
        @Test
        @DisplayName("Should convert domain workflow state string to CLI workflow state")
        void shouldConvertDomainWorkflowStateStringToCliWorkflowState() {
            // Test a few representative mappings
            assertEquals(WorkflowState.FOUND, ModelMapper.toCliWorkflowState("FOUND"));
            assertEquals(WorkflowState.TO_DO, ModelMapper.toCliWorkflowState("TO_DO"));
            assertEquals(WorkflowState.IN_PROGRESS, ModelMapper.toCliWorkflowState("IN_PROGRESS"));
            assertEquals(WorkflowState.DONE, ModelMapper.toCliWorkflowState("DONE"));
            
            // Test special case: RELEASED maps to DONE in CLI
            assertEquals(WorkflowState.DONE, ModelMapper.toCliWorkflowState("RELEASED"));
            
            // Test fallback for unknown state
            assertEquals(WorkflowState.CREATED, ModelMapper.toCliWorkflowState("UNKNOWN_STATE"));
            
            // Test that null input returns null output
            assertNull(ModelMapper.toCliWorkflowState(null));
        }
        
        @Test
        @DisplayName("Should convert CLI priority to domain priority string")
        void shouldConvertCliPriorityToDomainPriorityString() {
            assertEquals("LOW", ModelMapper.toDomainPriority(Priority.LOW));
            assertEquals("MEDIUM", ModelMapper.toDomainPriority(Priority.MEDIUM));
            assertEquals("HIGH", ModelMapper.toDomainPriority(Priority.HIGH));
            assertEquals("CRITICAL", ModelMapper.toDomainPriority(Priority.CRITICAL));
            
            assertNull(ModelMapper.toDomainPriority(null));
        }
        
        @Test
        @DisplayName("Should convert domain priority string to CLI priority")
        void shouldConvertDomainPriorityStringToCliPriority() {
            assertEquals(Priority.LOW, ModelMapper.toCliPriority("LOW"));
            assertEquals(Priority.MEDIUM, ModelMapper.toCliPriority("MEDIUM"));
            assertEquals(Priority.HIGH, ModelMapper.toCliPriority("HIGH"));
            assertEquals(Priority.CRITICAL, ModelMapper.toCliPriority("CRITICAL"));
            
            // Test fallback for unknown priority
            assertEquals(Priority.MEDIUM, ModelMapper.toCliPriority("UNKNOWN_PRIORITY"));
            
            assertNull(ModelMapper.toCliPriority(null));
        }
        
        @Test
        @DisplayName("Should convert CLI work item type to domain work item type string")
        void shouldConvertCliWorkItemTypeToDomainWorkItemTypeString() {
            assertEquals("TASK", ModelMapper.toDomainWorkItemType(WorkItemType.TASK));
            assertEquals("BUG", ModelMapper.toDomainWorkItemType(WorkItemType.BUG));
            assertEquals("FEATURE", ModelMapper.toDomainWorkItemType(WorkItemType.FEATURE));
            
            assertNull(ModelMapper.toDomainWorkItemType(null));
        }
        
        @Test
        @DisplayName("Should convert domain work item type string to CLI work item type")
        void shouldConvertDomainWorkItemTypeStringToCliWorkItemType() {
            assertEquals(WorkItemType.TASK, ModelMapper.toCliWorkItemType("TASK"));
            assertEquals(WorkItemType.BUG, ModelMapper.toCliWorkItemType("BUG"));
            assertEquals(WorkItemType.FEATURE, ModelMapper.toCliWorkItemType("FEATURE"));
            
            // Test fallback for unknown type
            assertEquals(WorkItemType.TASK, ModelMapper.toCliWorkItemType("UNKNOWN_TYPE"));
            
            assertNull(ModelMapper.toCliWorkItemType(null));
        }
        
        @Test
        @DisplayName("Should map CLI WorkflowState to domain WorkflowState")
        void shouldMapCliWorkflowStateToDomainWorkflowState() {
            // Test core mappings
            DomainWorkflowState newState = StateMapper.toDomainState(WorkflowState.CREATED);
            assertNotNull(newState);
            
            DomainWorkflowState inProgressState = StateMapper.toDomainState(WorkflowState.IN_PROGRESS);
            assertEquals(DomainWorkflowState.IN_PROGRESS, inProgressState);
            
            DomainWorkflowState doneState = StateMapper.toDomainState(WorkflowState.DONE);
            assertEquals(DomainWorkflowState.DONE, doneState);
            
            // Test null value
            assertNull(StateMapper.toDomainState(null));
        }

        @Test
        @DisplayName("Should map domain WorkflowState to CLI WorkflowState")
        void shouldMapDomainWorkflowStateToCliWorkflowState() {
            // Test core mappings
            WorkflowState inProgressState = StateMapper.fromDomainState(DomainWorkflowState.IN_PROGRESS);
            assertEquals(WorkflowState.IN_PROGRESS, inProgressState);
            
            WorkflowState doneState = StateMapper.fromDomainState(DomainWorkflowState.DONE);
            assertEquals(WorkflowState.DONE, doneState);
            
            // Test null value
            assertNull(StateMapper.fromDomainState(null));
        }

        @Test
        @DisplayName("Should map CLI Priority to domain Priority")
        void shouldMapCliPriorityToDomainPriority() {
            assertEquals(DomainPriority.LOW, 
                    StateMapper.toDomainPriority(Priority.LOW));
            assertEquals(DomainPriority.MEDIUM, 
                    StateMapper.toDomainPriority(Priority.MEDIUM));
            assertEquals(DomainPriority.HIGH, 
                    StateMapper.toDomainPriority(Priority.HIGH));
            assertEquals(DomainPriority.CRITICAL, 
                    StateMapper.toDomainPriority(Priority.CRITICAL));
            
            // Test null value
            assertNull(StateMapper.toDomainPriority(null));
        }

        @Test
        @DisplayName("Should map domain Priority to CLI Priority")
        void shouldMapDomainPriorityToCliPriority() {
            assertEquals(Priority.LOW, 
                    StateMapper.fromDomainPriority(DomainPriority.LOW));
            assertEquals(Priority.MEDIUM, 
                    StateMapper.fromDomainPriority(DomainPriority.MEDIUM));
            assertEquals(Priority.HIGH, 
                    StateMapper.fromDomainPriority(DomainPriority.HIGH));
            assertEquals(Priority.CRITICAL, 
                    StateMapper.fromDomainPriority(DomainPriority.CRITICAL));
            
            // Test null value
            assertNull(StateMapper.fromDomainPriority(null));
        }

        @Test
        @DisplayName("Should map CLI WorkItemType to domain WorkItemType")
        void shouldMapCliWorkItemTypeToDomainWorkItemType() {
            assertEquals(DomainWorkItemType.BUG, 
                    StateMapper.toDomainType(WorkItemType.BUG));
            assertEquals(DomainWorkItemType.TASK, 
                    StateMapper.toDomainType(WorkItemType.TASK));
            assertEquals(DomainWorkItemType.FEATURE, 
                    StateMapper.toDomainType(WorkItemType.FEATURE));
            assertEquals(DomainWorkItemType.EPIC, 
                    StateMapper.toDomainType(WorkItemType.EPIC));
            
            // Test null value
            assertNull(StateMapper.toDomainType(null));
        }

        @Test
        @DisplayName("Should map domain WorkItemType to CLI WorkItemType")
        void shouldMapDomainWorkItemTypeToCliWorkItemType() {
            assertEquals(WorkItemType.BUG, 
                    StateMapper.fromDomainType(DomainWorkItemType.BUG));
            assertEquals(WorkItemType.TASK, 
                    StateMapper.fromDomainType(DomainWorkItemType.TASK));
            assertEquals(WorkItemType.FEATURE, 
                    StateMapper.fromDomainType(DomainWorkItemType.FEATURE));
            assertEquals(WorkItemType.EPIC, 
                    StateMapper.fromDomainType(DomainWorkItemType.EPIC));
            
            // Test null value
            assertNull(StateMapper.fromDomainType(null));
        }
    }

    @Nested
    @DisplayName("Work Item Conversion Tests")
    class WorkItemConversionTests {
        @Test
        @DisplayName("Should convert CLI work item to domain work item")
        void shouldConvertCliWorkItemToDomainWorkItem() {
            // Create a CLI WorkItem with all properties set
            WorkItem cliItem = new WorkItem();
            String id = UUID.randomUUID().toString();
            cliItem.setId(id);
            cliItem.setTitle("Test Work Item");
            cliItem.setDescription("This is a test work item");
            cliItem.setType(WorkItemType.BUG);
            cliItem.setPriority(Priority.HIGH);
            cliItem.setStatus(WorkflowState.IN_PROGRESS);
            cliItem.setAssignee("testuser");
            cliItem.setReporter("reporter");
            cliItem.setProject("test-project");
            cliItem.setCreated(LocalDateTime.now().minusDays(1));
            cliItem.setUpdated(LocalDateTime.now());

            // Convert to domain WorkItem
            DomainWorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
            
            // Verify all properties were mapped correctly
            assertEquals(UUID.fromString(id), domainItem.getId());
            assertEquals("Test Work Item", domainItem.getTitle());
            assertEquals("This is a test work item", domainItem.getDescription());
            assertEquals(DomainWorkItemType.BUG, domainItem.getType());
            assertEquals(DomainPriority.HIGH, domainItem.getPriority());
            assertEquals(DomainWorkflowState.IN_PROGRESS, domainItem.getState());
            assertEquals("testuser", domainItem.getAssignee());
            assertEquals("reporter", domainItem.getReporter());
            assertNotNull(domainItem.getCreatedAt());
            assertNotNull(domainItem.getUpdatedAt());
        }
        
        @Test
        @DisplayName("Should handle invalid UUID in CLI work item")
        void shouldHandleInvalidUuidInCliWorkItem() {
            // Given
            WorkItem cliItem = new WorkItem();
            cliItem.setId("not-a-uuid");
            cliItem.setTitle("Test Item");
            
            // When
            DomainWorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
            
            // Then
            assertNotNull(domainItem);
            assertNotNull(domainItem.getId()); // Should generate a new UUID
            assertEquals("Test Item", domainItem.getTitle());
        }
        
        @Test
        @DisplayName("Should handle null CLI work item")
        void shouldHandleNullCliWorkItem() {
            // When
            DomainWorkItem domainItem = ModelMapper.toDomainWorkItem(null);
            
            // Then
            assertNull(domainItem);
        }
        
        @Test
        @DisplayName("Should convert domain work item to CLI work item")
        void shouldConvertDomainWorkItemToCliWorkItem() {
            // Create a mock domain WorkItem
            UUID id = UUID.randomUUID();
            DefaultDomainWorkItem domainItem = new DefaultDomainWorkItem(id);
            domainItem.setTitle("Domain Test Item");
            domainItem.setDescription("This is a domain test item");
            domainItem.setType(DomainWorkItemType.FEATURE);
            domainItem.setPriority(DomainPriority.CRITICAL);
            domainItem.setState(DomainWorkflowState.IN_TEST);
            domainItem.setAssignee("domainuser");
            domainItem.setReporter("domainreporter");
            domainItem.setCreatedAt(Instant.now().minusSeconds(3600));
            domainItem.setUpdatedAt(Instant.now());
            
            // Convert to CLI WorkItem
            WorkItem cliItem = ModelMapper.toCliWorkItem(domainItem);
            
            // Verify all properties were mapped correctly
            assertEquals(id.toString(), cliItem.getId());
            assertEquals("Domain Test Item", cliItem.getTitle());
            assertEquals("This is a domain test item", cliItem.getDescription());
            assertEquals(WorkItemType.FEATURE, cliItem.getType());
            assertEquals(Priority.CRITICAL, cliItem.getPriority());
            assertEquals(WorkflowState.IN_TEST, cliItem.getStatus());
            assertEquals("domainuser", cliItem.getAssignee());
            assertEquals("domainreporter", cliItem.getReporter());
            assertNotNull(cliItem.getCreated());
            assertNotNull(cliItem.getUpdated());
        }
        
        @Test
        @DisplayName("Should handle null domain work item")
        void shouldHandleNullDomainWorkItem() {
            // When
            WorkItem cliItem = ModelMapper.toCliWorkItem(null);
            
            // Then
            assertNull(cliItem);
        }
        
        @Test
        @DisplayName("Should handle null values in domain work item")
        void shouldHandleNullValuesInDomainWorkItem() {
            // Given
            DefaultDomainWorkItem domainItem = new DefaultDomainWorkItem();
            // Don't set any values
            
            // When
            WorkItem cliItem = ModelMapper.toCliWorkItem(domainItem);
            
            // Then
            assertNotNull(cliItem);
            assertNotNull(cliItem.getId()); // Should be the UUID from DefaultDomainWorkItem constructor
            assertNull(cliItem.getTitle());
            assertNull(cliItem.getDescription());
            assertNull(cliItem.getType());
            assertNull(cliItem.getPriority());
            assertNull(cliItem.getState());
            assertNull(cliItem.getAssignee());
        }
        
        @Test
        @DisplayName("Should create domain WorkItem with default values when CLI WorkItem has null properties")
        void shouldCreateDomainWorkItemWithDefaultValuesWhenCliWorkItemHasNullProperties() {
            // Create a CLI WorkItem with minimal properties
            WorkItem cliItem = new WorkItem();
            cliItem.setTitle("Minimal Item");
            
            // Convert to domain WorkItem
            DomainWorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
            
            // Verify required properties and defaults
            assertNotNull(domainItem.getId()); // Should generate a new UUID
            assertEquals("Minimal Item", domainItem.getTitle());
            assertNull(domainItem.getDescription());
            assertNull(domainItem.getAssignee());
            assertNotNull(domainItem.getCreatedAt());
            assertNotNull(domainItem.getUpdatedAt());
        }
    }
    
    @Test
    @DisplayName("Should handle null values in all mapping methods")
    void shouldHandleNullValuesInAllMappingMethods() {
        // ModelMapper methods
        assertNull(ModelMapper.toDomainWorkflowState(null));
        assertNull(ModelMapper.toCliWorkflowState(null));
        assertNull(ModelMapper.toDomainPriority(null));
        assertNull(ModelMapper.toCliPriority(null));
        assertNull(ModelMapper.toDomainWorkItemType(null));
        assertNull(ModelMapper.toCliWorkItemType(null));
        assertNull(ModelMapper.toDomainWorkItem(null));
        assertNull(ModelMapper.toCliWorkItem(null));
        
        // StateMapper methods
        assertNull(StateMapper.toDomainState(null));
        assertNull(StateMapper.fromDomainState(null));
        assertNull(StateMapper.toDomainPriority(null));
        assertNull(StateMapper.fromDomainPriority(null));
        assertNull(StateMapper.toDomainType(null));
        assertNull(StateMapper.fromDomainType(null));
    }
}