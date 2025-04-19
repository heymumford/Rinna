package org.rinna.domain;

/**
 * Compatibility interface for Release that extends the new interface.
 * @deprecated Use org.rinna.domain.model.Release instead.
 */
@Deprecated(forRemoval = true)
public interface Release extends org.rinna.domain.model.Release {
    // This interface is intentionally empty as it's just a bridge
    // between old code and new code
}
