package org.rinna.cli.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.util.ModelMapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for the ModelMapper utility class.
 */
@DisplayName("ModelMapper Unit Tests")
public class ModelMapperTest {

    @Test
    @DisplayName("Should map CLI WorkflowState to domain WorkflowState")
    void shouldMapCliWorkflowStateToDomainWorkflowState() {
        // Test each CLI WorkflowState
        assertEquals(org.rinna.domain.model.WorkflowState.FOUND, 
                ModelMapper.toDomainWorkflowState(WorkflowState.CREATED));
        assertEquals(org.rinna.domain.model.WorkflowState.TO_DO, 
                ModelMapper.toDomainWorkflowState(WorkflowState.READY));
        assertEquals(org.rinna.domain.model.WorkflowState.IN_PROGRESS, 
                ModelMapper.toDomainWorkflowState(WorkflowState.IN_PROGRESS));
        assertEquals(org.rinna.domain.model.WorkflowState.IN_TEST, 
                ModelMapper.toDomainWorkflowState(WorkflowState.REVIEW));
        assertEquals(org.rinna.domain.model.WorkflowState.IN_TEST, 
                ModelMapper.toDomainWorkflowState(WorkflowState.TESTING));
        assertEquals(org.rinna.domain.model.WorkflowState.DONE, 
                ModelMapper.toDomainWorkflowState(WorkflowState.DONE));
        assertEquals(org.rinna.domain.model.WorkflowState.TO_DO, 
                ModelMapper.toDomainWorkflowState(WorkflowState.BLOCKED));
        
        // Test null value
        assertNull(ModelMapper.toDomainWorkflowState(null));
    }

    @Test
    @DisplayName("Should map domain WorkflowState to CLI WorkflowState")
    void shouldMapDomainWorkflowStateToCliWorkflowState() {
        // Test each domain WorkflowState
        assertEquals(WorkflowState.CREATED, 
                ModelMapper.toCliWorkflowState(org.rinna.domain.model.WorkflowState.FOUND));
        assertEquals(WorkflowState.READY, 
                ModelMapper.toCliWorkflowState(org.rinna.domain.model.WorkflowState.TRIAGED));
        assertEquals(WorkflowState.READY, 
                ModelMapper.toCliWorkflowState(org.rinna.domain.model.WorkflowState.TO_DO));
        assertEquals(WorkflowState.IN_PROGRESS, 
                ModelMapper.toCliWorkflowState(org.rinna.domain.model.WorkflowState.IN_PROGRESS));
        assertEquals(WorkflowState.TESTING, 
                ModelMapper.toCliWorkflowState(org.rinna.domain.model.WorkflowState.IN_TEST));
        assertEquals(WorkflowState.DONE, 
                ModelMapper.toCliWorkflowState(org.rinna.domain.model.WorkflowState.DONE));
        assertEquals(WorkflowState.DONE, 
                ModelMapper.toCliWorkflowState(org.rinna.domain.model.WorkflowState.RELEASED));
        
        // Test null value
        assertNull(ModelMapper.toCliWorkflowState(null));
    }

    @Test
    @DisplayName("Should map CLI Priority to domain Priority")
    void shouldMapCliPriorityToDomainPriority() {
        assertEquals(org.rinna.domain.model.Priority.LOW, 
                ModelMapper.toDomainPriority(Priority.LOW));
        assertEquals(org.rinna.domain.model.Priority.MEDIUM, 
                ModelMapper.toDomainPriority(Priority.MEDIUM));
        assertEquals(org.rinna.domain.model.Priority.HIGH, 
                ModelMapper.toDomainPriority(Priority.HIGH));
        assertEquals(org.rinna.domain.model.Priority.CRITICAL, 
                ModelMapper.toDomainPriority(Priority.CRITICAL));
        
        // Test null value
        assertNull(ModelMapper.toDomainPriority(null));
    }

    @Test
    @DisplayName("Should map domain Priority to CLI Priority")
    void shouldMapDomainPriorityToCliPriority() {
        assertEquals(Priority.LOW, 
                ModelMapper.toCliPriority(org.rinna.domain.model.Priority.LOW));
        assertEquals(Priority.MEDIUM, 
                ModelMapper.toCliPriority(org.rinna.domain.model.Priority.MEDIUM));
        assertEquals(Priority.HIGH, 
                ModelMapper.toCliPriority(org.rinna.domain.model.Priority.HIGH));
        assertEquals(Priority.CRITICAL, 
                ModelMapper.toCliPriority(org.rinna.domain.model.Priority.CRITICAL));
        
        // Test null value
        assertNull(ModelMapper.toCliPriority(null));
    }

    @Test
    @DisplayName("Should map CLI WorkItemType to domain WorkItemType")
    void shouldMapCliWorkItemTypeToDomainWorkItemType() {
        assertEquals(org.rinna.domain.model.WorkItemType.BUG, 
                ModelMapper.toDomainWorkItemType(WorkItemType.BUG));
        assertEquals(org.rinna.domain.model.WorkItemType.CHORE, 
                ModelMapper.toDomainWorkItemType(WorkItemType.TASK));
        assertEquals(org.rinna.domain.model.WorkItemType.CHORE, 
                ModelMapper.toDomainWorkItemType(WorkItemType.SPIKE));
        assertEquals(org.rinna.domain.model.WorkItemType.FEATURE, 
                ModelMapper.toDomainWorkItemType(WorkItemType.FEATURE));
        assertEquals(org.rinna.domain.model.WorkItemType.FEATURE, 
                ModelMapper.toDomainWorkItemType(WorkItemType.STORY));
        assertEquals(org.rinna.domain.model.WorkItemType.GOAL, 
                ModelMapper.toDomainWorkItemType(WorkItemType.EPIC));
        
        // Test null value
        assertNull(ModelMapper.toDomainWorkItemType(null));
    }

    @Test
    @DisplayName("Should map domain WorkItemType to CLI WorkItemType")
    void shouldMapDomainWorkItemTypeToCliWorkItemType() {
        assertEquals(WorkItemType.BUG, 
                ModelMapper.toCliWorkItemType(org.rinna.domain.model.WorkItemType.BUG));
        assertEquals(WorkItemType.TASK, 
                ModelMapper.toCliWorkItemType(org.rinna.domain.model.WorkItemType.CHORE));
        assertEquals(WorkItemType.FEATURE, 
                ModelMapper.toCliWorkItemType(org.rinna.domain.model.WorkItemType.FEATURE));
        assertEquals(WorkItemType.EPIC, 
                ModelMapper.toCliWorkItemType(org.rinna.domain.model.WorkItemType.GOAL));
        
        // Test null value
        assertNull(ModelMapper.toCliWorkItemType(null));
    }

    @Test
    @DisplayName("Should convert CLI WorkItem to domain WorkItem")
    void shouldConvertCliWorkItemToDomainWorkItem() {
        // Create a CLI WorkItem with all properties set
        WorkItem cliItem = new WorkItem();
        String id = UUID.randomUUID().toString();
        cliItem.setId(id);
        cliItem.setTitle("Test Work Item");
        cliItem.setDescription("This is a test work item");
        cliItem.setType(WorkItemType.BUG);
        cliItem.setPriority(Priority.HIGH);
        cliItem.setState(WorkflowState.IN_PROGRESS);
        cliItem.setAssignee("testuser");
        cliItem.setProject("test-project");
        cliItem.setDueDate(LocalDate.now().plusDays(7));

        // Convert to domain WorkItem
        org.rinna.domain.model.WorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
        
        // Verify all properties were mapped correctly
        assertEquals(UUID.fromString(id), domainItem.id());
        assertEquals("Test Work Item", domainItem.title());
        assertEquals("This is a test work item", domainItem.description());
        assertEquals(org.rinna.domain.model.WorkItemType.BUG, domainItem.type());
        assertEquals(org.rinna.domain.model.Priority.HIGH, domainItem.priority());
        assertEquals(org.rinna.domain.model.WorkflowState.IN_PROGRESS, domainItem.state());
        assertEquals("testuser", domainItem.assignee());
        
        // Verify metadata
        Map<String, Object> metadata = domainItem.metadata();
        assertNotNull(metadata);
        assertEquals("test-project", metadata.get("project"));
        assertTrue(metadata.get("dueDate").toString().contains(LocalDate.now().plusDays(7).toString()));
        
        // Test null value
        assertNull(ModelMapper.toDomainWorkItem(null));
    }

    @Test
    @DisplayName("Should convert domain WorkItem to CLI WorkItem")
    void shouldConvertDomainWorkItemToCliWorkItem() {
        // Create a mock domain WorkItem
        UUID id = UUID.randomUUID();
        org.rinna.domain.model.WorkItem domainItem = new org.rinna.domain.model.WorkItem() {
            @Override
            public UUID id() {
                return id;
            }
            
            @Override
            public String title() {
                return "Domain Test Item";
            }
            
            @Override
            public String description() {
                return "This is a domain test item";
            }
            
            @Override
            public org.rinna.domain.model.WorkItemType type() {
                return org.rinna.domain.model.WorkItemType.FEATURE;
            }
            
            @Override
            public org.rinna.domain.model.Priority priority() {
                return org.rinna.domain.model.Priority.CRITICAL;
            }
            
            @Override
            public org.rinna.domain.model.WorkflowState state() {
                return org.rinna.domain.model.WorkflowState.IN_TEST;
            }
            
            @Override
            public String assignee() {
                return "domainuser";
            }
            
            @Override
            public Map<String, Object> metadata() {
                return Map.of(
                    "project", "domain-project",
                    "dueDate", LocalDate.now().plusDays(14).toString()
                );
            }
        };
        
        // Convert to CLI WorkItem
        WorkItem cliItem = ModelMapper.toCliWorkItem(domainItem);
        
        // Verify all properties were mapped correctly
        assertEquals(id.toString(), cliItem.getId());
        assertEquals("Domain Test Item", cliItem.getTitle());
        assertEquals("This is a domain test item", cliItem.getDescription());
        assertEquals(WorkItemType.FEATURE, cliItem.getType());
        assertEquals(Priority.CRITICAL, cliItem.getPriority());
        assertEquals(WorkflowState.TESTING, cliItem.getState());
        assertEquals("domainuser", cliItem.getAssignee());
        assertEquals("domain-project", cliItem.getProject());
        assertNotNull(cliItem.getDueDate());
        
        // Test null value
        assertNull(ModelMapper.toCliWorkItem(null));
    }

    @Test
    @DisplayName("Should handle null values in all mapping methods")
    void shouldHandleNullValuesInAllMappingMethods() {
        assertNull(ModelMapper.toDomainWorkflowState(null));
        assertNull(ModelMapper.toCliWorkflowState(null));
        assertNull(ModelMapper.toDomainPriority(null));
        assertNull(ModelMapper.toCliPriority(null));
        assertNull(ModelMapper.toDomainWorkItemType(null));
        assertNull(ModelMapper.toCliWorkItemType(null));
        assertNull(ModelMapper.toDomainWorkItem(null));
        assertNull(ModelMapper.toCliWorkItem(null));
    }

    @Test
    @DisplayName("Should create domain WorkItem with default values when CLI WorkItem has null properties")
    void shouldCreateDomainWorkItemWithDefaultValuesWhenCliWorkItemHasNullProperties() {
        // Create a CLI WorkItem with minimal properties
        WorkItem cliItem = new WorkItem();
        cliItem.setTitle("Minimal Item");
        
        // Convert to domain WorkItem
        org.rinna.domain.model.WorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
        
        // Verify required properties and defaults
        assertNotNull(domainItem.id()); // Should generate a new UUID
        assertEquals("Minimal Item", domainItem.title());
        assertNull(domainItem.description());
        assertEquals(org.rinna.domain.model.WorkItemType.CHORE, domainItem.type());
        assertEquals(org.rinna.domain.model.Priority.MEDIUM, domainItem.priority());
        assertEquals(org.rinna.domain.model.WorkflowState.FOUND, domainItem.state());
        assertNull(domainItem.assignee());
        assertNotNull(domainItem.metadata());
    }

    @Test
    @DisplayName("Should not instantiate ModelMapper")
    void shouldNotInstantiateModelMapper() {
        // The ModelMapper has a private constructor that throws UnsupportedOperationException
        Exception exception = assertThrows(UnsupportedOperationException.class, () -> {
            // Use reflection to call the private constructor
            java.lang.reflect.Constructor<ModelMapper> constructor = 
                ModelMapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        
        assertEquals("Utility class cannot be instantiated", exception.getMessage());
    }
}