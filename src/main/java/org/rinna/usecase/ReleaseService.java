package org.rinna.usecase;

/**
 * Compatibility interface for ReleaseService.
 * @deprecated Use org.rinna.domain.service.ReleaseService instead.
 */
@Deprecated(forRemoval = true)
public interface ReleaseService extends org.rinna.domain.service.ReleaseService {
    // This interface is intentionally empty as it's just a bridge
}
