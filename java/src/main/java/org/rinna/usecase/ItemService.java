package org.rinna.usecase;

/**
 * Compatibility interface for ItemService.
 * @deprecated Use org.rinna.domain.service.ItemService instead.
 */
@Deprecated(forRemoval = true)
public interface ItemService extends org.rinna.domain.service.ItemService {
    // This interface is intentionally empty as it's just a bridge
}
