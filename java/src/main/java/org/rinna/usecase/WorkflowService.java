package org.rinna.usecase;

/**
 * Compatibility interface for WorkflowService.
 * @deprecated Use org.rinna.domain.service.WorkflowService instead.
 */
@Deprecated(forRemoval = true)
public interface WorkflowService extends org.rinna.domain.service.WorkflowService {
    // This interface is intentionally empty as it's just a bridge
}
