package org.rinna.cli.unit;

import org.junit.jupiter.api.Test;
import org.rinna.cli.domain.model.DefaultDomainWorkItem;
import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkItemType;
import org.rinna.cli.domain.model.DomainWorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.util.ModelMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for the ModelMapper utility class.
 * This class just tests the basic functionality without external dependencies.
 */
public class SimpleModelMapperTest {

    @Test
    public void testToDomainWorkItem() {
        // Arrange
        UUID id = UUID.randomUUID();
        WorkItem cliItem = new WorkItem();
        cliItem.setId(id.toString());
        cliItem.setTitle("Test Item");
        cliItem.setDescription("Test Description");
        cliItem.setAssignee("test-user");
        cliItem.setReporter("test-reporter");
        cliItem.setType(WorkItemType.TASK);
        cliItem.setPriority(Priority.HIGH);
        cliItem.setStatus(WorkflowState.IN_PROGRESS);
        
        // Act
        DomainWorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
        
        // Assert
        assertNotNull(domainItem);
        assertEquals(id, domainItem.getId());
        assertEquals("Test Item", domainItem.getTitle());
        assertEquals("Test Description", domainItem.getDescription());
        assertEquals("test-user", domainItem.getAssignee());
        assertEquals("test-reporter", domainItem.getReporter());
        assertEquals(DomainWorkItemType.TASK, domainItem.getType());
        assertEquals(DomainPriority.HIGH, domainItem.getPriority());
        assertEquals(DomainWorkflowState.IN_PROGRESS, domainItem.getState());
    }
    
    @Test
    public void testToCliWorkItem() {
        // Arrange
        UUID id = UUID.randomUUID();
        DefaultDomainWorkItem domainItem = new DefaultDomainWorkItem(id);
        domainItem.setTitle("Domain Test Item");
        domainItem.setDescription("Domain Test Description");
        domainItem.setAssignee("domain-user");
        domainItem.setReporter("domain-reporter");
        domainItem.setType(DomainWorkItemType.BUG);
        domainItem.setPriority(DomainPriority.MEDIUM);
        domainItem.setState(DomainWorkflowState.TO_DO);
        
        // Act
        WorkItem cliItem = ModelMapper.toCliWorkItem(domainItem);
        
        // Assert
        assertNotNull(cliItem);
        assertEquals(id.toString(), cliItem.getId());
        assertEquals("Domain Test Item", cliItem.getTitle());
        assertEquals("Domain Test Description", cliItem.getDescription());
        assertEquals("domain-user", cliItem.getAssignee());
        assertEquals("domain-reporter", cliItem.getReporter());
        assertEquals(WorkItemType.BUG, cliItem.getType());
        assertEquals(Priority.MEDIUM, cliItem.getPriority());
        assertEquals(WorkflowState.TO_DO, cliItem.getStatus());
    }
    
    @Test
    public void testNullInputs() {
        assertNull(ModelMapper.toDomainWorkItem(null));
        assertNull(ModelMapper.toCliWorkItem(null));
    }
    
    @Test
    public void testStateMapperMethods() {
        // Test state mapping
        assertEquals(WorkflowState.TO_DO, ModelMapper.toCliWorkflowState("TO_DO"));
        assertEquals("TO_DO", ModelMapper.toDomainWorkflowState(WorkflowState.TO_DO));
        
        // Test priority mapping
        assertEquals(Priority.HIGH, ModelMapper.toCliPriority("HIGH"));
        assertEquals("HIGH", ModelMapper.toDomainPriority(Priority.HIGH));
        
        // Test type mapping
        assertEquals(WorkItemType.BUG, ModelMapper.toCliWorkItemType("BUG"));
        assertEquals("BUG", ModelMapper.toDomainWorkItemType(WorkItemType.BUG));
    }
    
    @Test
    public void testUUIDHandling() {
        // Valid UUID
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";
        UUID result1 = ModelMapper.toUUID(validUuid);
        assertEquals(UUID.fromString(validUuid), result1);
        
        // Invalid UUID
        String invalidUuid = "not-a-uuid";
        UUID result2 = ModelMapper.toUUID(invalidUuid);
        assertNotNull(result2);
        
        // Null and empty inputs
        assertNotNull(ModelMapper.toUUID(null));
        assertNotNull(ModelMapper.toUUID(""));
    }
}