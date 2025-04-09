package org.rinna.domain;

/**
 * Compatibility class for Priority that delegates to the new enum.
 * @deprecated Use org.rinna.domain.model.Priority instead.
 */
@Deprecated(forRemoval = true)
public enum Priority {
    CRITICAL, HIGH, MEDIUM, LOW;

    public org.rinna.domain.model.Priority toNewPriority() {
        return org.rinna.domain.model.Priority.valueOf(this.name());
    }

    public static Priority fromNewPriority(org.rinna.domain.model.Priority newPriority) {
        return Priority.valueOf(newPriority.name());
    }
}
