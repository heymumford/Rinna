/*
 * DefaultFlexibleWorkItemService - Default implementation of FlexibleWorkItemService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemCreateRequest;
import org.rinna.domain.WorkItemType;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.TemplateRepository;
import org.rinna.usecase.FlexibleWorkItemService;

import java.util.*;

/**
 * Default implementation of the FlexibleWorkItemService interface.
 * This implementation provides capabilities for working with custom fields and templates for work items.
 */
public class DefaultFlexibleWorkItemService implements FlexibleWorkItemService {

    private final ItemRepository itemRepository;
    private final TemplateRepository templateRepository;
    
    /**
     * Creates a new DefaultFlexibleWorkItemService with the given repositories.
     *
     * @param itemRepository the repository for work items
     * @param templateRepository the repository for work item templates
     */
    public DefaultFlexibleWorkItemService(ItemRepository itemRepository, TemplateRepository templateRepository) {
        this.itemRepository = itemRepository;
        this.templateRepository = templateRepository;
    }
    
    @Override
    public WorkItem addCustomFields(UUID workItemId, Map<String, String> customFields) {
        // Check if the work item exists
        Optional<WorkItem> optionalItem = itemRepository.findById(workItemId);
        if (optionalItem.isEmpty()) {
            throw new IllegalArgumentException("Work item not found: " + workItemId);
        }
        
        WorkItem workItem = optionalItem.get();
        
        // Merge existing metadata with new custom fields
        Map<String, String> existingMetadata = new HashMap<>(
                workItem.getMetadata() != null ? workItem.getMetadata() : new HashMap<>());
        existingMetadata.putAll(customFields);
        
        // Update the work item's metadata
        return itemRepository.updateMetadata(workItemId, existingMetadata);
    }
    
    @Override
    public WorkItem createWithCustomFields(WorkItemCreateRequest.Builder requestBuilder, Map<String, String> customFields) {
        // Create the work item
        WorkItemCreateRequest request = requestBuilder.build();
        WorkItem workItem = itemRepository.create(request);
        
        // Add custom fields
        return addCustomFields(workItem.getId(), customFields);
    }
    
    @Override
    public List<WorkItem> findByCustomField(String field, String value) {
        return itemRepository.findByCustomField(field, value);
    }
    
    @Override
    public WorkItem createFromTemplate(String templateName, Map<String, String> providedFields) {
        // Get the template
        Optional<Map<String, Object>> optionalTemplate = templateRepository.findByName(templateName);
        if (optionalTemplate.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        Map<String, Object> template = optionalTemplate.get();
        
        // Validate required fields
        validateRequiredFields(template, providedFields);
        
        // Create a request builder with template defaults
        WorkItemCreateRequest.Builder requestBuilder = new WorkItemCreateRequest.Builder();
        
        // Set title from provided fields
        if (providedFields.containsKey("title")) {
            requestBuilder.title(providedFields.get("title"));
        } else {
            throw new IllegalArgumentException("Title is required");
        }
        
        // Set type from template
        if (template.containsKey("type")) {
            requestBuilder.type((WorkItemType) template.get("type"));
        }
        
        // Set priority from template
        if (template.containsKey("priority")) {
            requestBuilder.priority(template.get("priority").toString());
        }
        
        // Create the work item
        WorkItem workItem = itemRepository.create(requestBuilder.build());
        
        // Prepare custom fields by combining template defaults and provided fields
        Map<String, String> customFields = new HashMap<>();
        
        // Add default values from template
        if (template.containsKey("default_values")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> defaultValues = (Map<String, Object>) template.get("default_values");
            for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
                customFields.put(entry.getKey(), entry.getValue().toString());
            }
        }
        
        // Add provided fields (overriding defaults)
        for (Map.Entry<String, String> entry : providedFields.entrySet()) {
            // Skip title as it's already set in the request
            if (!entry.getKey().equals("title")) {
                customFields.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Add custom fields to the work item
        return addCustomFields(workItem.getId(), customFields);
    }
    
    @Override
    public boolean createTemplate(String templateName, Map<String, Object> definition) {
        // Check if the template already exists
        if (templateRepository.exists(templateName)) {
            throw new IllegalArgumentException("Template already exists: " + templateName);
        }
        
        // Validate the template definition
        validateTemplateDefinition(definition);
        
        // Save the template
        return templateRepository.save(templateName, definition);
    }
    
    @Override
    public boolean updateTemplate(String templateName, Map<String, Object> definition) {
        // Check if the template exists
        if (!templateRepository.exists(templateName)) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        // Validate the template definition
        validateTemplateDefinition(definition);
        
        // Update the template
        return templateRepository.save(templateName, definition);
    }
    
    @Override
    public Map<String, Object> getTemplate(String templateName) {
        // Get the template
        Optional<Map<String, Object>> optionalTemplate = templateRepository.findByName(templateName);
        if (optionalTemplate.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        return optionalTemplate.get();
    }
    
    @Override
    public int bulkUpdateCustomFields(String field, String value, Map<String, String> updateFields) {
        // Find all work items matching the field value
        List<WorkItem> matchingItems = itemRepository.findByCustomField(field, value);
        
        // Update each matching item
        int updatedCount = 0;
        for (WorkItem item : matchingItems) {
            addCustomFields(item.getId(), updateFields);
            updatedCount++;
        }
        
        return updatedCount;
    }
    
    /**
     * Validates that all required fields are provided.
     *
     * @param template the template definition
     * @param providedFields the fields provided by the user
     * @throws IllegalArgumentException if required fields are missing
     */
    private void validateRequiredFields(Map<String, Object> template, Map<String, String> providedFields) {
        if (!template.containsKey("required_fields")) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> requiredFields = (Map<String, Object>) template.get("required_fields");
        
        for (Map.Entry<String, Object> entry : requiredFields.entrySet()) {
            String fieldName = entry.getKey();
            boolean isRequired = (boolean) entry.getValue();
            
            if (isRequired && !providedFields.containsKey(fieldName)) {
                // Check if there's a default value
                boolean hasDefault = template.containsKey("default_values") && 
                        ((Map<?, ?>) template.get("default_values")).containsKey(fieldName);
                
                if (!hasDefault) {
                    throw new IllegalArgumentException("Required field missing: " + fieldName);
                }
            }
        }
    }
    
    /**
     * Validates a template definition.
     *
     * @param definition the template definition to validate
     * @throws IllegalArgumentException if the definition is invalid
     */
    private void validateTemplateDefinition(Map<String, Object> definition) {
        // Validate template structure
        // This is a simplified validation, in a real implementation we would do more thorough validation
        
        // Validate required_fields
        if (definition.containsKey("required_fields")) {
            if (!(definition.get("required_fields") instanceof Map)) {
                throw new IllegalArgumentException("required_fields must be a map");
            }
        }
        
        // Validate default_values
        if (definition.containsKey("default_values")) {
            if (!(definition.get("default_values") instanceof Map)) {
                throw new IllegalArgumentException("default_values must be a map");
            }
        }
    }
}