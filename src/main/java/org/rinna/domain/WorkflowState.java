package org.rinna.domain;

/**
 * Compatibility class for WorkflowState that delegates to the new enum.
 * @deprecated Use org.rinna.domain.model.WorkflowState instead.
 */
@Deprecated(forRemoval = true)
public enum WorkflowState {
    NEW, IN_PROGRESS, TESTING, DONE, BLOCKED;

    public org.rinna.domain.model.WorkflowState toNewState() {
        return org.rinna.domain.model.WorkflowState.valueOf(this.name());
    }

    public static WorkflowState fromNewState(org.rinna.domain.model.WorkflowState newState) {
        return WorkflowState.valueOf(newState.name());
    }
}
