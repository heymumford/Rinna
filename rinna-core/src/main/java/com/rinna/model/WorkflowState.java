package com.rinna.model;

/**
 * Defines the workflow states an item can transition through.
 */
public enum WorkflowState {
    /**
     * Item initially identified.
     */
    FOUND,
    
    /**
     * Item assessed and prioritized.
     */
    TRIAGED,
    
    /**
     * Item ready to be worked on.
     */
    TO_DO,
    
    /**
     * Item currently being worked on.
     */
    IN_PROGRESS,
    
    /**
     * Item under verification.
     */
    IN_TEST,
    
    /**
     * Item completed.
     */
    DONE
}