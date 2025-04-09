package org.rinna.domain.repository;

import org.rinna.domain.model.macro.MacroDefinition;
import org.rinna.domain.model.macro.MacroExecution;
import org.rinna.domain.model.macro.ScheduledExecution;
import org.rinna.domain.model.macro.TriggerType;

import java.util.List;
import java.util.Map;

/**
 * Repository interface for managing macro definitions, executions, and schedules.
 */
public interface MacroRepository {
    /**
     * Creates a new macro definition.
     *
     * @param macro the macro definition to create
     * @return the created macro definition with ID assigned
     */
    MacroDefinition create(MacroDefinition macro);
    
    /**
     * Retrieves a macro definition by ID.
     *
     * @param id the macro ID
     * @return the macro definition, or null if not found
     */
    MacroDefinition findById(String id);
    
    /**
     * Updates an existing macro definition.
     *
     * @param macro the updated macro definition
     * @return the updated macro definition
     */
    MacroDefinition update(MacroDefinition macro);
    
    /**
     * Deletes a macro definition.
     *
     * @param id the macro ID
     * @return true if the macro was deleted, false if it didn't exist
     */
    boolean delete(String id);
    
    /**
     * Lists all macro definitions.
     *
     * @return a list of all macro definitions
     */
    List<MacroDefinition> findAll();
    
    /**
     * Lists macro definitions matching the given filters.
     *
     * @param filters the filters to apply
     * @return a list of matching macro definitions
     */
    List<MacroDefinition> findByFilters(Map<String, String> filters);
    
    /**
     * Finds macros that can be triggered by the given trigger type.
     *
     * @param triggerType the trigger type
     * @return a list of matching macro definitions
     */
    List<MacroDefinition> findByTriggerType(TriggerType triggerType);
    
    /**
     * Saves an execution record.
     *
     * @param execution the execution record to save
     * @return the saved execution record
     */
    MacroExecution saveExecution(MacroExecution execution);
    
    /**
     * Retrieves an execution record by ID.
     *
     * @param id the execution ID
     * @return the execution record, or null if not found
     */
    MacroExecution findExecutionById(String id);
    
    /**
     * Retrieves execution records for a macro.
     *
     * @param macroId the macro ID
     * @param limit the maximum number of records to return
     * @return a list of execution records
     */
    List<MacroExecution> findExecutionsByMacroId(String macroId, int limit);
    
    /**
     * Saves a scheduled execution.
     *
     * @param scheduledExecution the scheduled execution to save
     * @return the saved scheduled execution
     */
    ScheduledExecution saveScheduledExecution(ScheduledExecution scheduledExecution);
    
    /**
     * Retrieves all scheduled executions.
     *
     * @return a list of all scheduled executions
     */
    List<ScheduledExecution> findAllScheduledExecutions();
    
    /**
     * Retrieves scheduled executions for a macro.
     *
     * @param macroId the macro ID
     * @return a list of scheduled executions
     */
    List<ScheduledExecution> findScheduledExecutionsByMacroId(String macroId);
    
    /**
     * Deletes a scheduled execution.
     *
     * @param id the scheduled execution ID
     * @return true if the scheduled execution was deleted, false if it didn't exist
     */
    boolean deleteScheduledExecution(String id);
    
    /**
     * Deletes all scheduled executions for a macro.
     *
     * @param macroId the macro ID
     * @return the number of scheduled executions deleted
     */
    int deleteScheduledExecutionsByMacroId(String macroId);
}