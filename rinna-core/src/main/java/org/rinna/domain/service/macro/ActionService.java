package org.rinna.domain.service.macro;

import org.rinna.domain.model.macro.ActionResult;
import org.rinna.domain.model.macro.MacroAction;
import org.rinna.domain.model.macro.ExecutionContext;

/**
 * Service interface for executing macro actions.
 */
public interface ActionService {
    /**
     * Registers an action provider.
     *
     * @param provider the action provider to register
     */
    void registerActionProvider(ActionProvider provider);
    
    /**
     * Executes an action with the given execution context.
     *
     * @param action the action to execute
     * @param context the execution context
     * @return the action execution result
     */
    ActionResult executeAction(MacroAction action, ExecutionContext context);
}