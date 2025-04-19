package org.rinna.domain.model.macro;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Records the execution of a macro, including trigger context, 
 * execution status, and results of each action.
 */
public class MacroExecution {
    private String id;                        // Execution ID
    private String macroId;                   // Reference to the macro
    private TriggerContext triggerContext;    // What triggered the execution
    private ExecutionStatus status;           // Current status
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<ActionResult> actionResults; // Results of each action
    private Map<String, Object> variables;    // Variables during execution
    private String errorMessage;              // If execution failed

    /**
     * Default constructor.
     */
    public MacroExecution() {
        this.actionResults = new ArrayList<>();
        this.variables = new HashMap<>();
        this.status = ExecutionStatus.PENDING;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Constructor with essential fields.
     *
     * @param id the execution ID
     * @param macroId the macro ID
     * @param triggerContext the trigger context
     */
    public MacroExecution(String id, String macroId, TriggerContext triggerContext) {
        this();
        this.id = id;
        this.macroId = macroId;
        this.triggerContext = triggerContext;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<ActionResult> getActionResults() {
        return actionResults;
    }

    public void setActionResults(List<ActionResult> actionResults) {
        this.actionResults = actionResults != null ? actionResults : new ArrayList<>();
    }

    /**
     * Adds an action result.
     *
     * @param result the action result to add
     */
    public void addActionResult(ActionResult result) {
        if (result != null) {
            this.actionResults.add(result);
        }
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Starts the execution.
     */
    public void start() {
        this.status = ExecutionStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Completes the execution successfully.
     */
    public void complete() {
        this.status = ExecutionStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
    }

    /**
     * Marks the execution as failed.
     *
     * @param errorMessage the error message
     */
    public void fail(String errorMessage) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
    }

    /**
     * Cancels the execution.
     */
    public void cancel() {
        this.status = ExecutionStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }

    /**
     * Calculates the duration of this execution in milliseconds.
     *
     * @return the duration in milliseconds, or -1 if not complete
     */
    public long getDurationMillis() {
        if (startTime == null || endTime == null) {
            return -1;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroExecution that = (MacroExecution) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MacroExecution{" +
                "id='" + id + '\'' +
                ", macroId='" + macroId + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", hasError=" + (errorMessage != null && !errorMessage.isEmpty()) +
                '}';
    }
}