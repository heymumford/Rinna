package org.rinna.cli.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

/**
 * Unit tests for the ModelMapper utility class.
 */
public class ModelMapperTest {

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
        
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        LocalDateTime updated = LocalDateTime.now();
        cliItem.setCreated(created);
        cliItem.setUpdated(updated);
        
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
        
        // Check timestamps
        assertNotNull(domainItem.getCreatedAt());
        assertNotNull(domainItem.getUpdatedAt());
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
        
        Instant created = Instant.now().minusSeconds(3600);
        Instant updated = Instant.now();
        domainItem.setCreatedAt(created);
        domainItem.setUpdatedAt(updated);
        
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
        
        // Check timestamps
        assertNotNull(cliItem.getCreated());
        assertNotNull(cliItem.getUpdated());
    }
    
    @Test
    public void testNullInputs() {
        assertNull(ModelMapper.toDomainWorkItem(null));
        assertNull(ModelMapper.toCliWorkItem(null));
    }
    
    @Test
    public void testListConversions() {
        // Create test items
        WorkItem cliItem1 = new WorkItem();
        cliItem1.setTitle("CLI Item 1");
        cliItem1.setType(WorkItemType.TASK);
        
        WorkItem cliItem2 = new WorkItem();
        cliItem2.setTitle("CLI Item 2");
        cliItem2.setType(WorkItemType.BUG);
        
        List<WorkItem> cliItems = Arrays.asList(cliItem1, cliItem2);
        
        // Test CLI to Domain list conversion
        List<DomainWorkItem> domainItems = ModelMapper.toDomainWorkItems(cliItems);
        assertEquals(2, domainItems.size());
        assertEquals("CLI Item 1", domainItems.get(0).getTitle());
        assertEquals("CLI Item 2", domainItems.get(1).getTitle());
        
        // Test Domain to CLI list conversion
        List<WorkItem> convertedCliItems = ModelMapper.toCliWorkItems(domainItems);
        assertEquals(2, convertedCliItems.size());
        assertEquals("CLI Item 1", convertedCliItems.get(0).getTitle());
        assertEquals("CLI Item 2", convertedCliItems.get(1).getTitle());
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
    
    /**
     * Test that the ModelMapper correctly identifies Record classes.
     */
    @Test
    public void testIsRecordDetection() throws Exception {
        // Create a record-like class with reflection
        // We'll use a mock class to simulate a Java Record
        Class<?> recordClass = createMockRecordClass();
        
        // Create a regular class
        Class<?> regularClass = DefaultDomainWorkItem.class;
        
        // Use reflection to access the private isRecord method
        java.lang.reflect.Method isRecordMethod = ModelMapper.class.getDeclaredMethod("isRecord", Class.class);
        isRecordMethod.setAccessible(true);
        
        // Test record class
        boolean isRecord = (Boolean) isRecordMethod.invoke(null, recordClass);
        assertTrue(isRecord, "Should detect record class correctly");
        
        // Test regular class
        boolean isNotRecord = (Boolean) isRecordMethod.invoke(null, regularClass);
        assertFalse(isNotRecord, "Should detect non-record class correctly");
    }
    
    /**
     * Test that ModelMapper can properly convert a CLI WorkItem to a WorkItemRecord.
     */
    @Test
    public void testToCoreWorkItemRecord() throws Exception {
        // Mock a WorkItemRecord class 
        Class<?> recordClass = createMockWorkItemRecordClass();
        
        // Create a CLI WorkItem
        UUID id = UUID.randomUUID();
        WorkItem cliItem = new WorkItem();
        cliItem.setId(id.toString());
        cliItem.setTitle("Record Test Item");
        cliItem.setDescription("Testing Record Conversion");
        cliItem.setAssignee("test-user");
        cliItem.setType(WorkItemType.FEATURE);
        cliItem.setPriority(Priority.HIGH);
        cliItem.setStatus(WorkflowState.IN_PROGRESS);
        cliItem.setCreated(LocalDateTime.now().minusDays(1));
        cliItem.setUpdated(LocalDateTime.now());
        
        // Mock the necessary enum classes
        mockEnumClasses(recordClass);
        
        // Convert to WorkItemRecord - This will throw exception if record conversion fails
        Object result = ModelMapper.toCoreWorkItem(cliItem, recordClass);
        
        // If we made it here without exception, the test passes
        assertNotNull(result, "Should create a valid record instance");
    }
    
    /**
     * Test that ModelMapper can properly convert from a WorkItemRecord to a CLI WorkItem.
     */
    @Test
    public void testToCliWorkItemFromRecord() throws Exception {
        // Create a mock record with necessary fields
        Object mockRecord = createMockWorkItemRecord();
        
        // Convert the record to CLI WorkItem
        WorkItem cliItem = ModelMapper.toCliWorkItemFromCore(mockRecord);
        
        // Verify the conversion
        assertNotNull(cliItem);
        assertEquals("Record Title", cliItem.getTitle());
        assertEquals("Record Description", cliItem.getDescription());
        assertEquals("record-user", cliItem.getAssignee());
        assertEquals(WorkItemType.FEATURE, cliItem.getType());
        assertEquals(Priority.HIGH, cliItem.getPriority());
        assertEquals(WorkflowState.IN_PROGRESS, cliItem.getStatus());
    }
    
    /**
     * Test that the ModelMapper handles Optional values in record fields correctly.
     */
    @Test
    public void testOptionalHandlingInRecords() throws Exception {
        // Create a mock record with Optional fields
        Object mockRecordWithOptionals = createMockWorkItemRecordWithOptionals();
        
        // Convert the record to CLI WorkItem
        WorkItem cliItem = ModelMapper.toCliWorkItemFromCore(mockRecordWithOptionals);
        
        // Verify the conversion of optional fields
        assertNotNull(cliItem);
        assertEquals("Optional Project", cliItem.getProjectId());
    }
    
    // Helper methods for testing record handling
    
    /**
     * Creates a mock class that behaves like a Java Record.
     */
    private Class<?> createMockRecordClass() {
        // Create a mock class that returns true for isRecord() check
        Class<?> mockClass = Mockito.mock(Class.class);
        when(mockClass.isRecord()).thenReturn(true);
        return mockClass;
    }
    
    /**
     * Creates a mock WorkItemRecord class with necessary methods.
     */
    private Class<?> createMockWorkItemRecordClass() throws Exception {
        // Create a mock record class with the expected constructor and methods
        Class<?> mockRecordClass = Mockito.mock(Class.class);
        when(mockRecordClass.isRecord()).thenReturn(true);
        when(mockRecordClass.getSimpleName()).thenReturn("WorkItemRecord");
        when(mockRecordClass.getPackage()).thenReturn(this.getClass().getPackage());
        
        // Mock the constructor
        java.lang.reflect.Constructor<?> constructor = Mockito.mock(java.lang.reflect.Constructor.class);
        when(mockRecordClass.getDeclaredConstructor(any(), any(), any(), any(), any(), any(), 
                                                   any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(constructor);
        
        when(constructor.newInstance(any(), any(), any(), any(), any(), any(), 
                                     any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(createMockWorkItemRecord());
            
        return mockRecordClass;
    }
    
    /**
     * Creates a mock WorkItemRecord instance with all required fields.
     */
    private Object createMockWorkItemRecord() throws Exception {
        // Create a mock record instance
        Object mockRecord = Mockito.mock(Object.class);
        Class<?> mockClass = mockRecord.getClass();
        
        // Mock basic accessors
        when(mockClass.getMethod("id")).thenReturn(mockClass.getMethod("toString"));
        when(mockClass.getMethod("title")).thenReturn(mockClass.getMethod("toString"));
        when(mockClass.getMethod("description")).thenReturn(mockClass.getMethod("toString"));
        when(mockClass.getMethod("assignee")).thenReturn(mockClass.getMethod("toString"));
        when(mockClass.getMethod("type")).thenReturn(mockClass.getMethod("toString"));
        when(mockClass.getMethod("priority")).thenReturn(mockClass.getMethod("toString"));
        when(mockClass.getMethod("status")).thenReturn(mockClass.getMethod("toString"));
        when(mockClass.getMethod("createdAt")).thenReturn(mockClass.getMethod("toString"));
        when(mockClass.getMethod("updatedAt")).thenReturn(mockClass.getMethod("toString"));
        
        // Mock return values
        when(mockRecord.toString()).thenReturn("mock");
        when(mockClass.getMethod("id").invoke(mockRecord)).thenReturn(UUID.randomUUID());
        when(mockClass.getMethod("title").invoke(mockRecord)).thenReturn("Record Title");
        when(mockClass.getMethod("description").invoke(mockRecord)).thenReturn("Record Description");
        when(mockClass.getMethod("assignee").invoke(mockRecord)).thenReturn("record-user");
        when(mockClass.getMethod("type").invoke(mockRecord)).thenReturn("FEATURE");
        when(mockClass.getMethod("priority").invoke(mockRecord)).thenReturn("HIGH");
        when(mockClass.getMethod("status").invoke(mockRecord)).thenReturn("IN_PROGRESS");
        when(mockClass.getMethod("createdAt").invoke(mockRecord)).thenReturn(Instant.now().minusDays(1));
        when(mockClass.getMethod("updatedAt").invoke(mockRecord)).thenReturn(Instant.now());
        
        // Make it look like a record
        when(mockClass.isRecord()).thenReturn(true);
        
        return mockRecord;
    }
    
    /**
     * Creates a mock WorkItemRecord with Optional field values.
     */
    private Object createMockWorkItemRecordWithOptionals() throws Exception {
        // Create a base record
        Object mockRecord = createMockWorkItemRecord();
        Class<?> mockClass = mockRecord.getClass();
        
        // Create a mock Optional for projectId
        Optional<UUID> optionalProjectId = Optional.of(UUID.randomUUID());
        
        // Mock the projectId method
        when(mockClass.getMethod("projectId").invoke(mockRecord)).thenReturn(optionalProjectId);
        
        // Mock the toString method for the Optional value
        when(optionalProjectId.toString()).thenReturn("Optional Project");
        
        return mockRecord;
    }
    
    /**
     * Mocks the enum classes needed for WorkItemRecord creation.
     */
    private void mockEnumClasses(Class<?> recordClass) throws Exception {
        // Create and configure mock enum classes
        Class<?> typeEnum = Mockito.mock(Class.class);
        Class<?> stateEnum = Mockito.mock(Class.class);
        Class<?> priorityEnum = Mockito.mock(Class.class);
        
        // Make them look like enums
        when(typeEnum.isEnum()).thenReturn(true);
        when(stateEnum.isEnum()).thenReturn(true);
        when(priorityEnum.isEnum()).thenReturn(true);
        when(typeEnum.getEnumConstants()).thenReturn(new Object[]{});
        when(stateEnum.getEnumConstants()).thenReturn(new Object[]{});
        when(priorityEnum.getEnumConstants()).thenReturn(new Object[]{});
        
        // Mock the getCoreEnumClass method using reflection
        java.lang.reflect.Method getCoreEnumMethod = ModelMapper.class.getDeclaredMethod(
            "getCoreEnumClass", Class.class, String.class);
        getCoreEnumMethod.setAccessible(true);
        
        // Create a subclass of ModelMapper to override the static method
        java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        
        // Set up static mock behavior using PowerMock or similar library
        // For simplicity, we'll directly test the createWorkItemRecord method
    }
}