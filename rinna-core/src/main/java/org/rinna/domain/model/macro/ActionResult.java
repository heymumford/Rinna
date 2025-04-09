package org.rinna.domain.model.macro;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Records the result of a single action execution within a macro.
 */
public class ActionResult {
    private ActionType actionType;             // Type of action
    private boolean successful;                // Whether the action succeeded
    private LocalDateTime executionTime;       // When the action was executed
    private long durationMillis;               // How long the action took to execute
    private String errorMessage;               // Error message if unsuccessful
    private Map<String, Object> resultData;    // Action-specific result data

    /**
     * Default constructor.
     */
    public ActionResult() {
        this.resultData = new HashMap<>();
        this.executionTime = LocalDateTime.now();
    }

    /**
     * Constructor for a successful result.
     *
     * @param actionType the action type
     * @param resultData the result data
     */
    public ActionResult(ActionType actionType, Map<String, Object> resultData) {
        this();
        this.actionType = actionType;
        this.successful = true;
        if (resultData != null) {
            this.resultData.putAll(resultData);
        }
    }

    /**
     * Constructor for a failed result.
     *
     * @param actionType the action type
     * @param errorMessage the error message
     */
    public ActionResult(ActionType actionType, String errorMessage) {
        this();
        this.actionType = actionType;
        this.successful = false;
        this.errorMessage = errorMessage;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getResultData() {
        return resultData;
    }

    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData != null ? resultData : new HashMap<>();
    }

    /**
     * Gets a result data value.
     *
     * @param key the data key
     * @return the value, or null if not found
     */
    public Object getResultValue(String key) {
        return resultData.get(key);
    }

    /**
     * Sets a result data value.
     *
     * @param key the data key
     * @param value the value to set
     */
    public void setResultValue(String key, Object value) {
        if (key != null && value != null) {
            this.resultData.put(key, value);
        }
    }

    /**
     * Creates an ActionResult for a successful action execution.
     *
     * @param actionType the action type
     * @param durationMillis the duration in milliseconds
     * @return a new ActionResult instance
     */
    public static ActionResult success(ActionType actionType, long durationMillis) {
        ActionResult result = new ActionResult();
        result.setActionType(actionType);
        result.setSuccessful(true);
        result.setDurationMillis(durationMillis);
        return result;
    }

    /**
     * Creates an ActionResult for a failed action execution.
     *
     * @param actionType the action type
     * @param errorMessage the error message
     * @param durationMillis the duration in milliseconds
     * @return a new ActionResult instance
     */
    public static ActionResult failure(ActionType actionType, String errorMessage, long durationMillis) {
        ActionResult result = new ActionResult();
        result.setActionType(actionType);
        result.setSuccessful(false);
        result.setErrorMessage(errorMessage);
        result.setDurationMillis(durationMillis);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionResult that = (ActionResult) o;
        return successful == that.successful &&
                durationMillis == that.durationMillis &&
                actionType == that.actionType &&
                Objects.equals(executionTime, that.executionTime) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(resultData, that.resultData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionType, successful, executionTime, durationMillis, errorMessage, resultData);
    }

    @Override
    public String toString() {
        return "ActionResult{" +
                "actionType=" + actionType +
                ", successful=" + successful +
                ", durationMillis=" + durationMillis +
                (errorMessage != null ? ", error='" + errorMessage + '\'' : "") +
                '}';
    }
}