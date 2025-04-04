package com.rinna.model;

/**
 * Defines the different types of work items supported by Rinna.
 */
public enum WorkItemType {
    /**
     * High-level software development objective.
     */
    GOAL,
    
    /**
     * Incremental, deliverable functionality.
     */
    FEATURE,
    
    /**
     * Unexpected software issue requiring correction.
     */
    BUG,
    
    /**
     * Non-functional task ensuring system health and stability.
     */
    CHORE
}