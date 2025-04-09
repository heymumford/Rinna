/*
 * Component tests for FlexibleWorkItemService
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
import org.rinna.adapter.service.DefaultFlexibleWorkItemService;
import org.rinna.component.base.ComponentTest;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemCreateRequest;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.TemplateRepository;
import org.rinna.usecase.FlexibleWorkItemService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Component tests for FlexibleWorkItemService.
 * These tests verify the behavior of the FlexibleWorkItemService component
 * with real implementations of in-module dependencies and mocks for external dependencies.
 */
@DisplayName("Flexible Work Item Service Component Tests")
class FlexibleWorkItemServiceComponentTest extends ComponentTest {

    private FlexibleWorkItemService flexibleWorkItemService;
    
    @Mock
    private ItemRepository itemRepository;
    
    @Mock
    private TemplateRepository templateRepository;
    
    @Mock
    private WorkItem mockWorkItem;
    
    private UUID workItemId;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup the service with mocked dependencies
        flexibleWorkItemService = new DefaultFlexibleWorkItemService(itemRepository, templateRepository);
        
        // Setup mock work item
        workItemId = UUID.randomUUID();
        when(mockWorkItem.getId()).thenReturn(workItemId);
        when(mockWorkItem.getTitle()).thenReturn("Test Work Item");
        when(mockWorkItem.getType()).thenReturn(WorkItemType.FEATURE);
        when(mockWorkItem.getStatus()).thenReturn(WorkflowState.FOUND);
        
        // Setup ItemRepository mock
        when(itemRepository.findById(workItemId)).thenReturn(Optional.of(mockWorkItem));
    }
    
    @Test
    @DisplayName("Should add custom fields to work item")
    void shouldAddCustomFieldsToWorkItem() {
        // Given
        Map<String, String> existingMetadata = new HashMap<>();
        when(mockWorkItem.getMetadata()).thenReturn(existingMetadata);
        
        Map<String, String> newFields = new HashMap<>();
        newFields.put("estimated_hours", "16");
        newFields.put("security_review", "true");
        newFields.put("target_customers", "premium,enterprise");
        
        when(itemRepository.updateMetadata(eq(workItemId), any(Map.class))).thenReturn(mockWorkItem);
        
        // When
        WorkItem result = flexibleWorkItemService.addCustomFields(workItemId, newFields);
        
        // Then
        assertNotNull(result);
        verify(itemRepository).updateMetadata(eq(workItemId), argThat(map -> 
            "16".equals(map.get("estimated_hours")) &&
            "true".equals(map.get("security_review")) &&
            "premium,enterprise".equals(map.get("target_customers"))
        ));
    }
    
    @Test
    @DisplayName("Should throw exception when adding custom fields to non-existent work item")
    void shouldThrowExceptionWhenAddingCustomFieldsToNonExistentWorkItem() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(itemRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        Map<String, String> newFields = new HashMap<>();
        newFields.put("estimated_hours", "16");
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            flexibleWorkItemService.addCustomFields(nonExistentId, newFields);
        });
    }
    
    @Test
    @DisplayName("Should merge custom fields with existing metadata")
    void shouldMergeCustomFieldsWithExistingMetadata() {
        // Given
        Map<String, String> existingMetadata = new HashMap<>();
        existingMetadata.put("existing_field", "existing_value");
        existingMetadata.put("field_to_update", "old_value");
        when(mockWorkItem.getMetadata()).thenReturn(existingMetadata);
        
        Map<String, String> newFields = new HashMap<>();
        newFields.put("new_field", "new_value");
        newFields.put("field_to_update", "new_value");
        
        when(itemRepository.updateMetadata(eq(workItemId), any(Map.class))).thenReturn(mockWorkItem);
        
        // When
        WorkItem result = flexibleWorkItemService.addCustomFields(workItemId, newFields);
        
        // Then
        assertNotNull(result);
        verify(itemRepository).updateMetadata(eq(workItemId), argThat(map -> 
            "existing_value".equals(map.get("existing_field")) &&
            "new_value".equals(map.get("field_to_update")) &&
            "new_value".equals(map.get("new_field"))
        ));
    }
    
    @Test
    @DisplayName("Should create work item with custom fields")
    void shouldCreateWorkItemWithCustomFields() {
        // Given
        WorkItemCreateRequest.Builder requestBuilder = new WorkItemCreateRequest.Builder()
                .title("New Feature")
                .type(WorkItemType.FEATURE);
        
        Map<String, String> customFields = new HashMap<>();
        customFields.put("estimated_hours", "24");
        customFields.put("priority_level", "1");
        
        when(itemRepository.create(any(WorkItemCreateRequest.class))).thenReturn(mockWorkItem);
        
        // When
        WorkItem result = flexibleWorkItemService.createWithCustomFields(requestBuilder, customFields);
        
        // Then
        assertNotNull(result);
        verify(itemRepository).create(any(WorkItemCreateRequest.class));
        verify(itemRepository).updateMetadata(eq(workItemId), argThat(map -> 
            "24".equals(map.get("estimated_hours")) &&
            "1".equals(map.get("priority_level"))
        ));
    }
    
    @Test
    @DisplayName("Should find work items by custom field")
    void shouldFindWorkItemsByCustomField() {
        // Given
        when(itemRepository.findByCustomField("team", "backend")).thenReturn(Collections.singletonList(mockWorkItem));
        
        // When
        List<WorkItem> results = flexibleWorkItemService.findByCustomField("team", "backend");
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(mockWorkItem, results.get(0));
    }
    
    @Test
    @DisplayName("Should create work item from template")
    void shouldCreateWorkItemFromTemplate() {
        // Given
        String templateName = "SecurityBug";
        Map<String, Object> templateDefinition = new HashMap<>();
        templateDefinition.put("type", WorkItemType.BUG);
        templateDefinition.put("priority", "HIGH");
        
        Map<String, Object> requiredFields = new HashMap<>();
        requiredFields.put("security_impact", true);
        requiredFields.put("remediation_steps", true);
        templateDefinition.put("required_fields", requiredFields);
        
        Map<String, Object> defaultValues = new HashMap<>();
        defaultValues.put("security_impact", "UNKNOWN");
        templateDefinition.put("default_values", defaultValues);
        
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(templateDefinition));
        when(itemRepository.create(any(WorkItemCreateRequest.class))).thenReturn(mockWorkItem);
        
        Map<String, String> providedFields = new HashMap<>();
        providedFields.put("title", "SQL Injection vulnerability");
        providedFields.put("security_impact", "HIGH");
        providedFields.put("remediation_steps", "Sanitize user input");
        
        // When
        WorkItem result = flexibleWorkItemService.createFromTemplate(templateName, providedFields);
        
        // Then
        assertNotNull(result);
        verify(itemRepository).create(argThat(request -> 
            request.title().equals("SQL Injection vulnerability") &&
            request.type() == WorkItemType.BUG
        ));
        verify(itemRepository).updateMetadata(eq(workItemId), argThat(map -> 
            "HIGH".equals(map.get("security_impact")) &&
            "Sanitize user input".equals(map.get("remediation_steps"))
        ));
    }
    
    @Test
    @DisplayName("Should throw exception when required template fields are missing")
    void shouldThrowExceptionWhenRequiredTemplateFieldsAreMissing() {
        // Given
        String templateName = "SecurityBug";
        Map<String, Object> templateDefinition = new HashMap<>();
        templateDefinition.put("type", WorkItemType.BUG);
        
        Map<String, Object> requiredFields = new HashMap<>();
        requiredFields.put("security_impact", true);
        requiredFields.put("remediation_steps", true);
        templateDefinition.put("required_fields", requiredFields);
        
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(templateDefinition));
        
        Map<String, String> providedFields = new HashMap<>();
        providedFields.put("title", "SQL Injection vulnerability");
        providedFields.put("security_impact", "HIGH");
        // Missing "remediation_steps"
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            flexibleWorkItemService.createFromTemplate(templateName, providedFields);
        });
    }
    
    @Test
    @DisplayName("Should apply default values from template")
    void shouldApplyDefaultValuesFromTemplate() {
        // Given
        String templateName = "SecurityBug";
        Map<String, Object> templateDefinition = new HashMap<>();
        templateDefinition.put("type", WorkItemType.BUG);
        templateDefinition.put("priority", "HIGH");
        
        Map<String, Object> requiredFields = new HashMap<>();
        requiredFields.put("security_impact", true);
        templateDefinition.put("required_fields", requiredFields);
        
        Map<String, Object> defaultValues = new HashMap<>();
        defaultValues.put("security_impact", "UNKNOWN");
        defaultValues.put("cve_number", "");
        templateDefinition.put("default_values", defaultValues);
        
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(templateDefinition));
        when(itemRepository.create(any(WorkItemCreateRequest.class))).thenReturn(mockWorkItem);
        
        Map<String, String> providedFields = new HashMap<>();
        providedFields.put("title", "SQL Injection vulnerability");
        // Not providing security_impact, should use default
        
        // When
        WorkItem result = flexibleWorkItemService.createFromTemplate(templateName, providedFields);
        
        // Then
        assertNotNull(result);
        verify(itemRepository).updateMetadata(eq(workItemId), argThat(map -> 
            "UNKNOWN".equals(map.get("security_impact"))
        ));
    }
    
    @Test
    @DisplayName("Should bulk update custom fields")
    void shouldBulkUpdateCustomFields() {
        // Given
        List<WorkItem> matchingItems = Arrays.asList(mockWorkItem);
        when(itemRepository.findByCustomField("component", "authentication")).thenReturn(matchingItems);
        
        Map<String, String> updateFields = new HashMap<>();
        updateFields.put("security_review", "required");
        
        // When
        int updatedCount = flexibleWorkItemService.bulkUpdateCustomFields("component", "authentication", updateFields);
        
        // Then
        assertEquals(1, updatedCount);
        verify(itemRepository).updateMetadata(eq(workItemId), argThat(map -> 
            "required".equals(map.get("security_review"))
        ));
    }
}