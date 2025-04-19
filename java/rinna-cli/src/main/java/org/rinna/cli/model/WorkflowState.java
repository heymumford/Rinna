package org.rinna.cli.model;

/**
 * Valid workflow states for work items.
 */
public enum WorkflowState {
    CREATED,
    READY,
    IN_PROGRESS,
    REVIEW,
    TESTING,
    DONE,
    BLOCKED,
    // Additional states needed for compatibility
    FOUND,      // Initial state for bugs
    TRIAGED,    // Bugs have been reviewed and prioritized
    TO_DO,      // Ready to be worked on
    IN_TEST;    // Being tested

    /**
     * Convert from string ignoring case.
     *
     * @param value the string value to convert
     * @return the corresponding WorkflowState or null if not found
     */
    public static WorkflowState fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Maps a core module WorkflowState to a CLI module WorkflowState.
     * This is used for adapting between the two modules.
     *
     * @param coreState the core module state
     * @return the equivalent CLI module state
     */
    public static WorkflowState fromCoreState(String coreState) {
        if (coreState == null) {
            return null;
        }
        
        switch (coreState.toUpperCase()) {
            case "FOUND":
                return FOUND;
            case "TRIAGED":
                return TRIAGED;
            case "TO_DO":
                return TO_DO;
            case "IN_PROGRESS":
                return IN_PROGRESS;
            case "IN_TEST":
                return IN_TEST;
            case "DONE":
                return DONE;
            case "RELEASED":
                return DONE; // Map RELEASED to DONE in CLI
            default:
                try {
                    return valueOf(coreState.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return CREATED; // Default fallback
                }
        }
    }
}
