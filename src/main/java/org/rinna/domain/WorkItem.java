package org.rinna.domain;

/**
 * Compatibility interface for WorkItem that extends the new interface.
 * This allows old code to use the new implementation and vice versa.
 * @deprecated Use org.rinna.domain.model.WorkItem instead.
 */
@Deprecated(forRemoval = true)
public interface WorkItem extends org.rinna.domain.model.WorkItem {
    // This interface is intentionally empty as it's just a bridge
    // between old code and new code
}
