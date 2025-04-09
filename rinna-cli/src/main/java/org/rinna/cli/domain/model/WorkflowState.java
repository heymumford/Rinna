/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.model;

/**
 * Enum representing different states in the workflow.
 */
public enum WorkflowState {
    /**
     * The work item has been created but not yet started.
     */
    BACKLOG,
    
    /**
     * The work item has been planned for the current iteration.
     */
    PLANNED,
    
    /**
     * The work item is currently being worked on.
     */
    IN_PROGRESS,
    
    /**
     * The work item is blocked and waiting for something.
     */
    BLOCKED,
    
    /**
     * The work item has been completed and is awaiting review.
     */
    REVIEW,
    
    /**
     * The work item is being tested.
     */
    TESTING,
    
    /**
     * The work item has been completed and verified.
     */
    DONE,
    
    /**
     * The work item has been released to production.
     */
    RELEASED,
    
    /**
     * The work item has been archived.
     */
    ARCHIVED
}