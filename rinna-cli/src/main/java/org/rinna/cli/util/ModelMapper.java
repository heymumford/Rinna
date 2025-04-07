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

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping between CLI model classes and domain model classes.
 * This class provides methods to convert between different model representations.
 */
public final class ModelMapper {

    // Private constructor to prevent instantiation
    private ModelMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Maps from CLI WorkflowState to domain WorkflowState.
     *
     * @param state the CLI WorkflowState
     * @return the domain WorkflowState
     */
    public static org.rinna.domain.model.WorkflowState toDomainWorkflowState(WorkflowState state) {
        if (state == null) {
            return null;
        }
        
        // Map CLI workflow states to domain model workflow states
        switch (state) {
            case CREATED:
                return org.rinna.domain.model.WorkflowState.FOUND;
            case READY:
                return org.rinna.domain.model.WorkflowState.TO_DO;
            case IN_PROGRESS:
                return org.rinna.domain.model.WorkflowState.IN_PROGRESS;
            case REVIEW:
                return org.rinna.domain.model.WorkflowState.IN_TEST;
            case TESTING:
                return org.rinna.domain.model.WorkflowState.IN_TEST;
            case DONE:
                return org.rinna.domain.model.WorkflowState.DONE;
            case BLOCKED:
                return org.rinna.domain.model.WorkflowState.TO_DO; // No direct mapping for BLOCKED
            default:
                return org.rinna.domain.model.WorkflowState.FOUND;
        }
    }

    /**
     * Maps from domain WorkflowState to CLI WorkflowState.
     *
     * @param state the domain WorkflowState
     * @return the CLI WorkflowState
     */
    public static WorkflowState toCliWorkflowState(org.rinna.domain.model.WorkflowState state) {
        if (state == null) {
            return null;
        }
        
        // Map domain model workflow states to CLI workflow states
        switch (state) {
            case FOUND:
                return WorkflowState.CREATED;
            case TRIAGED:
                return WorkflowState.READY;
            case TO_DO:
                return WorkflowState.READY;
            case IN_PROGRESS:
                return WorkflowState.IN_PROGRESS;
            case IN_TEST:
                return WorkflowState.TESTING;
            case DONE:
                return WorkflowState.DONE;
            case RELEASED:
                return WorkflowState.DONE;
            default:
                return WorkflowState.CREATED;
        }
    }

    /**
     * Maps from CLI Priority to domain Priority.
     *
     * @param priority the CLI Priority
     * @return the domain Priority
     */
    public static org.rinna.domain.model.Priority toDomainPriority(Priority priority) {
        if (priority == null) {
            return null;
        }
        return org.rinna.domain.model.Priority.valueOf(priority.name());
    }

    /**
     * Maps from domain Priority to CLI Priority.
     *
     * @param priority the domain Priority
     * @return the CLI Priority
     */
    public static Priority toCliPriority(org.rinna.domain.model.Priority priority) {
        if (priority == null) {
            return null;
        }
        return Priority.valueOf(priority.name());
    }

    /**
     * Maps from CLI WorkItemType to domain WorkItemType.
     * 
     * @param type the CLI WorkItemType
     * @return the domain WorkItemType
     */
    public static org.rinna.domain.model.WorkItemType toDomainWorkItemType(WorkItemType type) {
        if (type == null) {
            return null;
        }
        
        // Map CLI work item types to domain model work item types
        switch (type) {
            case BUG:
                return org.rinna.domain.model.WorkItemType.BUG;
            case TASK:
            case SPIKE:
                return org.rinna.domain.model.WorkItemType.CHORE;
            case FEATURE:
            case STORY:
                return org.rinna.domain.model.WorkItemType.FEATURE;
            case EPIC:
                return org.rinna.domain.model.WorkItemType.GOAL;
            default:
                return org.rinna.domain.model.WorkItemType.CHORE;
        }
    }

    /**
     * Maps from domain WorkItemType to CLI WorkItemType.
     *
     * @param type the domain WorkItemType
     * @return the CLI WorkItemType
     */
    public static WorkItemType toCliWorkItemType(org.rinna.domain.model.WorkItemType type) {
        if (type == null) {
            return null;
        }
        
        // Map domain model work item types to CLI work item types
        switch (type) {
            case BUG:
                return WorkItemType.BUG;
            case CHORE:
                return WorkItemType.TASK;
            case FEATURE:
                return WorkItemType.FEATURE;
            case GOAL:
                return WorkItemType.EPIC;
            default:
                return WorkItemType.TASK;
        }
    }

    /**
     * Creates a domain WorkItem from a CLI WorkItem.
     *
     * @param cliItem the CLI WorkItem
     * @return a domain WorkItem implementation
     */
    public static org.rinna.domain.model.WorkItem toDomainWorkItem(final WorkItem cliItem) {
        if (cliItem == null) {
            return null;
        }
        
        return new org.rinna.domain.model.WorkItem() {
            private final UUID id = cliItem.getId() != null ? UUID.fromString(cliItem.getId()) : UUID.randomUUID();
            
            @Override
            public UUID id() {
                return id;
            }
            
            @Override
            public String title() {
                return cliItem.getTitle();
            }
            
            @Override
            public String description() {
                return cliItem.getDescription();
            }
            
            @Override
            public org.rinna.domain.model.WorkItemType type() {
                return cliItem.getType() != null ? 
                    toDomainWorkItemType(cliItem.getType()) : 
                    org.rinna.domain.model.WorkItemType.CHORE;
            }
            
            @Override
            public org.rinna.domain.model.Priority priority() {
                return cliItem.getPriority() != null ? 
                    toDomainPriority(cliItem.getPriority()) : 
                    org.rinna.domain.model.Priority.MEDIUM;
            }
            
            @Override
            public org.rinna.domain.model.WorkflowState state() {
                return cliItem.getState() != null ? 
                    toDomainWorkflowState(cliItem.getState()) : 
                    org.rinna.domain.model.WorkflowState.FOUND;
            }
            
            @Override
            public String assignee() {
                return cliItem.getAssignee();
            }
            
            @Override
            public Map<String, Object> metadata() {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("project", cliItem.getProject());
                if (cliItem.getDueDate() != null) {
                    metadata.put("dueDate", cliItem.getDueDate().toString());
                }
                return metadata;
            }
        };
    }

    /**
     * Creates a CLI WorkItem from a domain WorkItem.
     *
     * @param domainItem the domain WorkItem
     * @return a CLI WorkItem
     */
    public static WorkItem toCliWorkItem(org.rinna.domain.model.WorkItem domainItem) {
        if (domainItem == null) {
            return null;
        }
        
        WorkItem cliItem = new WorkItem();
        cliItem.setId(domainItem.id().toString());
        cliItem.setTitle(domainItem.title());
        cliItem.setDescription(domainItem.description());
        cliItem.setType(toCliWorkItemType(domainItem.type()));
        cliItem.setPriority(toCliPriority(domainItem.priority()));
        cliItem.setState(toCliWorkflowState(domainItem.state()));
        cliItem.setAssignee(domainItem.assignee());
        
        // Extract metadata
        Map<String, Object> metadata = domainItem.metadata();
        if (metadata != null) {
            if (metadata.containsKey("project")) {
                cliItem.setProject(metadata.get("project").toString());
            }
            
            if (metadata.containsKey("dueDate")) {
                try {
                    cliItem.setDueDate(java.time.LocalDate.parse(metadata.get("dueDate").toString()));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }
        
        return cliItem;
    }
}