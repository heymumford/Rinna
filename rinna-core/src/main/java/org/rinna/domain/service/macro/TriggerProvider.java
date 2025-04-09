package org.rinna.domain.service.macro;

import org.rinna.domain.model.macro.MacroTrigger;
import org.rinna.domain.model.macro.TriggerEvent;
import org.rinna.domain.model.macro.TriggerType;

/**
 * Interface for trigger providers that can determine if a trigger matches an event.
 */
public interface TriggerProvider {
    /**
     * Gets the trigger type provided by this provider.
     *
     * @return the trigger type
     */
    TriggerType getProvidedType();
    
    /**
     * Checks if the given trigger matches the given event.
     *
     * @param event the trigger event
     * @param trigger the macro trigger
     * @return true if the trigger matches the event
     */
    boolean matches(TriggerEvent event, MacroTrigger trigger);
}