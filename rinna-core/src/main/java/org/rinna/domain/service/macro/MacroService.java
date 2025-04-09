package org.rinna.domain.service.macro;

import org.rinna.domain.model.macro.MacroDefinition;
import org.rinna.domain.model.macro.MacroExecution;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing macro definitions and executions.
 */
public interface MacroService {
    /**
     * Creates a new macro definition.
     *
     * @param macro the macro definition to create
     * @return the created macro definition with ID assigned
     */
    MacroDefinition createMacro(MacroDefinition macro);
    
    /**
     * Retrieves a macro definition by ID.
     *
     * @param id the macro ID
     * @return the macro definition, or null if not found
     */
    MacroDefinition getMacro(String id);
    
    /**
     * Lists macro definitions matching the given filters.
     *
     * @param filters the filters to apply
     * @return a list of matching macro definitions
     */
    List<MacroDefinition> listMacros(Map<String, String> filters);
    
    /**
     * Updates an existing macro definition.
     *
     * @param id the macro ID
     * @param macro the updated macro definition
     * @return the updated macro definition
     */
    MacroDefinition updateMacro(String id, MacroDefinition macro);
    
    /**
     * Deletes a macro definition.
     *
     * @param id the macro ID
     */
    void deleteMacro(String id);
    
    /**
     * Enables a macro definition.
     *
     * @param id the macro ID
     */
    void enableMacro(String id);
    
    /**
     * Disables a macro definition.
     *
     * @param id the macro ID
     */
    void disableMacro(String id);
    
    /**
     * Executes a macro manually with optional parameters.
     *
     * @param macroId the macro ID
     * @param params the execution parameters
     * @return the execution record
     */
    MacroExecution executeManually(String macroId, Map<String, Object> params);
    
    /**
     * Retrieves an execution record by ID.
     *
     * @param executionId the execution ID
     * @return the execution record, or null if not found
     */
    MacroExecution getExecution(String executionId);
    
    /**
     * Retrieves the execution history for a macro.
     *
     * @param macroId the macro ID
     * @param limit the maximum number of records to return
     * @return a list of execution records
     */
    List<MacroExecution> getExecutionHistory(String macroId, int limit);
}