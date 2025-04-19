package org.rinna.cli.model;

/**
 * Priority levels for work items.
 */
public enum Priority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW;

    /**
     * Convert from string ignoring case.
     *
     * @param value the string value to convert
     * @return the corresponding Priority or null if not found
     */
    public static Priority fromString(String value) {
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
