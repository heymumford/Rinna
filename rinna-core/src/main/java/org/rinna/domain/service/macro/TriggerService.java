package org.rinna.domain.service.macro;

import org.rinna.domain.model.macro.MacroDefinition;
import org.rinna.domain.model.macro.TriggerEvent;

import java.util.List;

/**
 * Service interface for managing macro triggers and events.
 */
public interface TriggerService {
    /**
     * Registers a trigger provider.
     *
     * @param provider the trigger provider to register
     */
    void registerTriggerProvider(TriggerProvider provider);
    
    /**
     * Processes a trigger event and executes matching macros.
     *
     * @param event the trigger event to process
     */
    void processEvent(TriggerEvent event);
    
    /**
     * Finds macros that match the given trigger event.
     *
     * @param event the trigger event
     * @return a list of matching macro definitions
     */
    List<MacroDefinition> findMatchingMacros(TriggerEvent event);
}