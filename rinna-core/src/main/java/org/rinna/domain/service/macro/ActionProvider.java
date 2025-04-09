package org.rinna.domain.service.macro;

import org.rinna.domain.model.macro.ActionResult;
import org.rinna.domain.model.macro.ActionType;
import org.rinna.domain.model.macro.ExecutionContext;
import org.rinna.domain.model.macro.MacroAction;

/**
 * Interface for action providers that can execute specific types of actions.
 */
public interface ActionProvider {
    /**
     * Gets the action type provided by this provider.
     *
     * @return the action type
     */
    ActionType getProvidedType();
    
    /**
     * Executes the given action with the given execution context.
     *
     * @param action the action to execute
     * @param context the execution context
     * @return the action execution result
     */
    ActionResult execute(MacroAction action, ExecutionContext context);
}