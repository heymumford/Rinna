package org.rinna.domain;

/**
 * Compatibility class for WorkItemType that delegates to the new enum.
 * @deprecated Use org.rinna.domain.model.WorkItemType instead.
 */
@Deprecated(forRemoval = true)
public enum WorkItemType {
    BUG, FEATURE, CHORE, GOAL;

    public org.rinna.domain.model.WorkItemType toNewType() {
        return org.rinna.domain.model.WorkItemType.valueOf(this.name());
    }

    public static WorkItemType fromNewType(org.rinna.domain.model.WorkItemType newType) {
        return WorkItemType.valueOf(newType.name());
    }
}
