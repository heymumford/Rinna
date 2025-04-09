package org.rinna.adapter.service.macro;

import org.rinna.domain.model.macro.*;
import org.rinna.domain.repository.MacroRepository;
import org.rinna.domain.service.macro.MacroService;
import org.rinna.domain.service.macro.SchedulerService;
import org.rinna.domain.service.macro.TriggerService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of the MacroService interface.
 */
public class DefaultMacroService implements MacroService {
    private final MacroRepository macroRepository;
    private final TriggerService triggerService;
    private final SchedulerService schedulerService;

    /**
     * Constructor with required dependencies.
     *
     * @param macroRepository the macro repository
     * @param triggerService the trigger service
     * @param schedulerService the scheduler service
     */
    public DefaultMacroService(
            MacroRepository macroRepository,
            TriggerService triggerService,
            SchedulerService schedulerService) {
        this.macroRepository = macroRepository;
        this.triggerService = triggerService;
        this.schedulerService = schedulerService;
    }

    @Override
    public MacroDefinition createMacro(MacroDefinition macro) {
        // Basic validation
        if (macro == null) {
            throw new IllegalArgumentException("Macro cannot be null");
        }
        
        if (macro.getName() == null || macro.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Macro name cannot be empty");
        }
        
        if (macro.getTrigger() == null) {
            throw new IllegalArgumentException("Macro trigger cannot be null");
        }
        
        if (macro.getActions() == null || macro.getActions().isEmpty()) {
            throw new IllegalArgumentException("Macro must have at least one action");
        }
        
        // Create the macro
        MacroDefinition createdMacro = macroRepository.create(macro);
        
        // Schedule the macro if needed
        if (macro.getSchedule() != null && 
            (macro.getTrigger().getType() == TriggerType.SCHEDULED || 
             macro.getTrigger().getType() == TriggerType.ONE_TIME)) {
            schedulerService.scheduleMacro(createdMacro.getId(), macro.getSchedule());
        }
        
        return createdMacro;
    }

    @Override
    public MacroDefinition getMacro(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Macro ID cannot be null");
        }
        
        return macroRepository.findById(id);
    }

    @Override
    public List<MacroDefinition> listMacros(Map<String, String> filters) {
        if (filters == null) {
            filters = Collections.emptyMap();
        }
        
        return macroRepository.findByFilters(filters);
    }

    @Override
    public MacroDefinition updateMacro(String id, MacroDefinition macro) {
        if (id == null) {
            throw new IllegalArgumentException("Macro ID cannot be null");
        }
        
        if (macro == null) {
            throw new IllegalArgumentException("Macro cannot be null");
        }
        
        // Ensure the IDs match
        if (!id.equals(macro.getId())) {
            throw new IllegalArgumentException("Macro ID mismatch");
        }
        
        // Verify the macro exists
        MacroDefinition existingMacro = macroRepository.findById(id);
        if (existingMacro == null) {
            throw new IllegalArgumentException("Macro not found: " + id);
        }
        
        // Basic validation
        if (macro.getName() == null || macro.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Macro name cannot be empty");
        }
        
        if (macro.getTrigger() == null) {
            throw new IllegalArgumentException("Macro trigger cannot be null");
        }
        
        if (macro.getActions() == null || macro.getActions().isEmpty()) {
            throw new IllegalArgumentException("Macro must have at least one action");
        }
        
        // Handle schedule changes
        if (existingMacro.getSchedule() != null && 
            (macro.getSchedule() == null || !existingMacro.getSchedule().equals(macro.getSchedule()))) {
            // Cancel existing schedule
            schedulerService.cancelScheduledMacro(id);
        }
        
        if (macro.getSchedule() != null && 
            (macro.getTrigger().getType() == TriggerType.SCHEDULED || 
             macro.getTrigger().getType() == TriggerType.ONE_TIME)) {
            // Schedule with new schedule
            schedulerService.scheduleMacro(id, macro.getSchedule());
        }
        
        // Update the macro
        return macroRepository.update(macro);
    }

    @Override
    public void deleteMacro(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Macro ID cannot be null");
        }
        
        // Cancel any scheduled executions
        schedulerService.cancelScheduledMacro(id);
        
        // Delete the macro
        macroRepository.delete(id);
    }

    @Override
    public void enableMacro(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Macro ID cannot be null");
        }
        
        MacroDefinition macro = macroRepository.findById(id);
        if (macro == null) {
            throw new IllegalArgumentException("Macro not found: " + id);
        }
        
        macro.setEnabled(true);
        macroRepository.update(macro);
        
        // If the macro has a schedule, ensure it's scheduled
        if (macro.getSchedule() != null && 
            (macro.getTrigger().getType() == TriggerType.SCHEDULED || 
             macro.getTrigger().getType() == TriggerType.ONE_TIME)) {
            schedulerService.scheduleMacro(id, macro.getSchedule());
        }
    }

    @Override
    public void disableMacro(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Macro ID cannot be null");
        }
        
        MacroDefinition macro = macroRepository.findById(id);
        if (macro == null) {
            throw new IllegalArgumentException("Macro not found: " + id);
        }
        
        macro.setEnabled(false);
        macroRepository.update(macro);
        
        // Cancel any scheduled executions
        schedulerService.cancelScheduledMacro(id);
    }

    @Override
    public MacroExecution executeManually(String macroId, Map<String, Object> params) {
        if (macroId == null) {
            throw new IllegalArgumentException("Macro ID cannot be null");
        }
        
        MacroDefinition macro = macroRepository.findById(macroId);
        if (macro == null) {
            throw new IllegalArgumentException("Macro not found: " + macroId);
        }
        
        if (!macro.isEnabled()) {
            throw new IllegalStateException("Cannot execute disabled macro: " + macroId);
        }
        
        if (params == null) {
            params = new HashMap<>();
        }
        
        // Create a manual trigger event
        TriggerEvent event = TriggerEvent.forManualExecution("system", macroId);
        event.getPayload().putAll(params);
        
        // Process the event
        triggerService.processEvent(event);
        
        // Return the latest execution
        List<MacroExecution> executions = macroRepository.findExecutionsByMacroId(macroId, 1);
        return executions.isEmpty() ? null : executions.get(0);
    }

    @Override
    public MacroExecution getExecution(String executionId) {
        if (executionId == null) {
            throw new IllegalArgumentException("Execution ID cannot be null");
        }
        
        return macroRepository.findExecutionById(executionId);
    }

    @Override
    public List<MacroExecution> getExecutionHistory(String macroId, int limit) {
        if (macroId == null) {
            throw new IllegalArgumentException("Macro ID cannot be null");
        }
        
        if (limit <= 0) {
            limit = 10; // Default limit
        }
        
        return macroRepository.findExecutionsByMacroId(macroId, limit);
    }
}