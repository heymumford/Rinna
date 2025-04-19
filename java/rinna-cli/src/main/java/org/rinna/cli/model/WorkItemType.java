package org.rinna.cli.model;

/**
 * Types of work items in the system.
 */
public enum WorkItemType {
    BUG,
    TASK,
    FEATURE,
    SPIKE,
    STORY,
    EPIC;

    /**
     * Convert from string ignoring case.
     *
     * @param value the string value to convert
     * @return the corresponding WorkItemType or null if not found
     */
    public static WorkItemType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
