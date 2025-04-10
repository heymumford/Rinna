/*
 * FlexibleWorkItemService - Service interface for flexible work item operations
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemCreateRequest;

/**
 * Service interface for flexible work item operations.
 * This service provides capabilities for working with custom fields and templates for work items.
 */
public interface FlexibleWorkItemService {

    /**
     * Adds custom fields to an existing work item.
     *
     * @param workItemId the ID of the work item
     * @param customFields the custom fields to add
     * @return the updated work item
     * @throws IllegalArgumentException if the work item does not exist
     */
    WorkItem addCustomFields(UUID workItemId, Map<String, String> customFields);
    
    /**
     * Creates a new work item with custom fields.
     *
     * @param requestBuilder the builder for the work item create request
     * @param customFields the custom fields to add to the new work item
     * @return the created work item
     */
    WorkItem createWithCustomFields(WorkItemCreateRequest.Builder requestBuilder, Map<String, String> customFields);
    
    /**
     * Finds work items by a custom field value.
     *
     * @param field the name of the custom field
     * @param value the value of the custom field
     * @return a list of work items with the given custom field value
     */
    List<WorkItem> findByCustomField(String field, String value);
    
    /**
     * Creates a new work item from a template.
     *
     * @param templateName the name of the template to use
     * @param providedFields the fields provided by the user
     * @return the created work item
     * @throws IllegalArgumentException if the template does not exist or required fields are missing
     */
    WorkItem createFromTemplate(String templateName, Map<String, String> providedFields);
    
    /**
     * Creates a new template for work items.
     *
     * @param templateName the name of the template
     * @param definition the template definition
     * @return true if the template was created successfully
     * @throws IllegalArgumentException if a template with the given name already exists
     */
    boolean createTemplate(String templateName, Map<String, Object> definition);
    
    /**
     * Updates an existing template.
     *
     * @param templateName the name of the template to update
     * @param definition the new template definition
     * @return true if the template was updated successfully
     * @throws IllegalArgumentException if the template does not exist
     */
    boolean updateTemplate(String templateName, Map<String, Object> definition);
    
    /**
     * Gets a template by name.
     *
     * @param templateName the name of the template
     * @return the template definition
     * @throws IllegalArgumentException if the template does not exist
     */
    Map<String, Object> getTemplate(String templateName);
    
    /**
     * Bulk updates custom fields for work items matching a specific field value.
     *
     * @param field the field to match
     * @param value the value to match
     * @param updateFields the fields to update
     * @return the number of work items updated
     */
    int bulkUpdateCustomFields(String field, String value, Map<String, String> updateFields);
}