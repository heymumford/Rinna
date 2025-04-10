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

import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItemType;
import org.rinna.cli.domain.model.DomainWorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

/**
 * Utility class for mapping between different enum types from different modules.
 * This helps with the decoupling of the CLI module from the core module.
 */
public final class StateMapper {
    
    private StateMapper() {
        // Utility class, should not be instantiated
    }
    
    /**
     * Maps a CLI WorkflowState to a core WorkflowState string.
     *
     * @param cliState the CLI module state
     * @return the equivalent core module state string
     */
    public static String toCoreState(WorkflowState cliState) {
        if (cliState == null) {
            return null;
        }
        
        switch (cliState) {
            case CREATED:
                return "FOUND";
            case READY:
                return "TO_DO";
            case IN_PROGRESS:
                return "IN_PROGRESS";
            case REVIEW:
                return "IN_TEST";
            case TESTING:
                return "IN_TEST";
            case DONE:
                return "DONE";
            case BLOCKED:
                return "TO_DO"; // No direct mapping for BLOCKED
            case FOUND:
                return "FOUND";
            case TRIAGED:
                return "TRIAGED";
            case TO_DO:
                return "TO_DO";
            case IN_TEST:
                return "IN_TEST";
            default:
                return cliState.name();
        }
    }
    
    /**
     * Maps a core WorkflowState string to a CLI WorkflowState.
     *
     * @param coreState the core module state string
     * @return the equivalent CLI module state
     */
    public static WorkflowState fromCoreState(String coreState) {
        if (coreState == null) {
            return null;
        }
        
        switch (coreState.toUpperCase()) {
            case "FOUND":
                return WorkflowState.FOUND;
            case "TRIAGED":
                return WorkflowState.TRIAGED;
            case "TO_DO":
                return WorkflowState.TO_DO;
            case "IN_PROGRESS":
                return WorkflowState.IN_PROGRESS;
            case "IN_TEST":
                return WorkflowState.IN_TEST;
            case "DONE":
                return WorkflowState.DONE;
            case "RELEASED":
                return WorkflowState.DONE; // Map RELEASED to DONE in CLI
            default:
                try {
                    return WorkflowState.valueOf(coreState.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return WorkflowState.CREATED; // Default fallback
                }
        }
    }
    
    /**
     * Maps a CLI Priority to a core Priority string.
     *
     * @param cliPriority the CLI module priority
     * @return the equivalent core module priority string
     */
    public static String toCorePriority(Priority cliPriority) {
        if (cliPriority == null) {
            return null;
        }
        return cliPriority.name();
    }
    
    /**
     * Maps a core Priority string to a CLI Priority.
     *
     * @param corePriority the core module priority string
     * @return the equivalent CLI module priority
     */
    public static Priority fromCorePriority(String corePriority) {
        if (corePriority == null) {
            return null;
        }
        
        try {
            return Priority.valueOf(corePriority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Priority.MEDIUM; // Default fallback
        }
    }
    
    /**
     * Maps a CLI WorkItemType to a core WorkItemType string.
     *
     * @param cliType the CLI module work item type
     * @return the equivalent core module work item type string
     */
    public static String toCoreType(WorkItemType cliType) {
        if (cliType == null) {
            return null;
        }
        return cliType.name();
    }
    
    /**
     * Maps a core WorkItemType string to a CLI WorkItemType.
     *
     * @param coreType the core module work item type string
     * @return the equivalent CLI module work item type
     */
    public static WorkItemType fromCoreType(String coreType) {
        if (coreType == null) {
            return null;
        }
        
        try {
            return WorkItemType.valueOf(coreType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WorkItemType.TASK; // Default fallback
        }
    }
    
    /**
     * Maps a DomainWorkflowState to a CLI WorkflowState.
     *
     * @param domainState the domain workflow state
     * @return the equivalent CLI workflow state
     */
    public static WorkflowState fromDomainState(DomainWorkflowState domainState) {
        if (domainState == null) {
            return null;
        }
        
        return fromCoreState(domainState.name());
    }
    
    /**
     * Maps a CLI WorkflowState to a DomainWorkflowState.
     *
     * @param cliState the CLI workflow state
     * @return the equivalent domain workflow state
     */
    public static DomainWorkflowState toDomainState(WorkflowState cliState) {
        if (cliState == null) {
            return null;
        }
        
        String coreState = toCoreState(cliState);
        try {
            return DomainWorkflowState.valueOf(coreState);
        } catch (IllegalArgumentException e) {
            return DomainWorkflowState.NEW; // Default fallback
        }
    }
    
    /**
     * Maps a DomainPriority to a CLI Priority.
     *
     * @param domainPriority the domain priority
     * @return the equivalent CLI priority
     */
    public static Priority fromDomainPriority(DomainPriority domainPriority) {
        if (domainPriority == null) {
            return null;
        }
        
        return fromCorePriority(domainPriority.name());
    }
    
    /**
     * Maps a CLI Priority to a DomainPriority.
     *
     * @param cliPriority the CLI priority
     * @return the equivalent domain priority
     */
    public static DomainPriority toDomainPriority(Priority cliPriority) {
        if (cliPriority == null) {
            return null;
        }
        
        try {
            return DomainPriority.valueOf(cliPriority.name());
        } catch (IllegalArgumentException e) {
            return DomainPriority.MEDIUM; // Default fallback
        }
    }
    
    /**
     * Maps a DomainWorkItemType to a CLI WorkItemType.
     *
     * @param domainType the domain work item type
     * @return the equivalent CLI work item type
     */
    public static WorkItemType fromDomainType(DomainWorkItemType domainType) {
        if (domainType == null) {
            return null;
        }
        
        return fromCoreType(domainType.name());
    }
    
    /**
     * Maps a CLI WorkItemType to a DomainWorkItemType.
     *
     * @param cliType the CLI work item type
     * @return the equivalent domain work item type
     */
    public static DomainWorkItemType toDomainType(WorkItemType cliType) {
        if (cliType == null) {
            return null;
        }
        
        try {
            return DomainWorkItemType.valueOf(cliType.name());
        } catch (IllegalArgumentException e) {
            return DomainWorkItemType.TASK; // Default fallback
        }
    }
}