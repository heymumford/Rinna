package org.rinna.domain.model.macro;

/**
 * Represents the possible states of a macro execution.
 */
public enum ExecutionStatus {
    PENDING("Pending"),        // Scheduled but not started
    RUNNING("Running"),        // Currently executing
    COMPLETED("Completed"),    // Successfully completed
    FAILED("Failed"),          // Failed execution
    CANCELLED("Cancelled");    // Manually cancelled

    private final String displayName;

    ExecutionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this status indicates the execution is finished.
     *
     * @return true if the execution is finished
     */
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * Checks if this status indicates the execution is active.
     *
     * @return true if the execution is active
     */
    public boolean isActive() {
        return this == PENDING || this == RUNNING;
    }

    /**
     * Checks if this status indicates a successful execution.
     *
     * @return true if the execution was successful
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}