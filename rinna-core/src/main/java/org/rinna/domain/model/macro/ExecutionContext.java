package org.rinna.domain.model.macro;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains contextual information for a macro execution, including the trigger context
 * and execution-specific variables.
 */
public class ExecutionContext {
    private String executionId;               // ID of the current execution
    private String macroId;                   // ID of the macro being executed
    private TriggerContext triggerContext;    // Context of the trigger that started the execution
    private Map<String, Object> variables;    // Execution variables
    private LocalDateTime startTime;          // When the execution started

    /**
     * Default constructor.
     */
    public ExecutionContext() {
        this.variables = new HashMap<>();
        this.startTime = LocalDateTime.now();
    }

    /**
     * Constructor with essential fields.
     *
     * @param executionId the execution ID
     * @param macroId the macro ID
     * @param triggerContext the trigger context
     */
    public ExecutionContext(String executionId, String macroId, TriggerContext triggerContext) {
        this();
        this.executionId = executionId;
        this.macroId = macroId;
        this.triggerContext = triggerContext;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getMacroId() {
        return macroId;
    }

    public void setMacroId(String macroId) {
        this.macroId = macroId;
    }

    public TriggerContext getTriggerContext() {
        return triggerContext;
    }

    public void setTriggerContext(TriggerContext triggerContext) {
        this.triggerContext = triggerContext;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables != null ? variables : new HashMap<>();
    }

    /**
     * Gets a variable value.
     *
     * @param key the variable name
     * @return the variable value, or null if not found
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * Gets a variable value as a string.
     *
     * @param key the variable name
     * @return the variable value as a string, or null if not found
     */
    public String getVariableAsString(String key) {
        Object value = getVariable(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Sets a variable value.
     *
     * @param key the variable name
     * @param value the variable value
     */
    public void setVariable(String key, Object value) {
        if (key != null) {
            if (value != null) {
                this.variables.put(key, value);
            } else {
                this.variables.remove(key);
            }
        }
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Creates a new execution context for a manual execution.
     *
     * @param executionId the execution ID
     * @param macroId the macro ID
     * @param userId the ID of the user who initiated the execution
     * @return a new execution context
     */
    public static ExecutionContext forManualExecution(String executionId, String macroId, String userId) {
        TriggerContext triggerContext = TriggerContext.forManualExecution(userId);
        return new ExecutionContext(executionId, macroId, triggerContext);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionContext that = (ExecutionContext) o;
        return Objects.equals(executionId, that.executionId) &&
                Objects.equals(macroId, that.macroId) &&
                Objects.equals(triggerContext, that.triggerContext) &&
                Objects.equals(startTime, that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId, macroId, triggerContext, startTime);
    }

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "executionId='" + executionId + '\'' +
                ", macroId='" + macroId + '\'' +
                ", startTime=" + startTime +
                ", variables=" + variables.size() +
                '}';
    }
}