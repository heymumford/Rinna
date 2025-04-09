package org.rinna.domain.model.macro;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains contextual information about what triggered a macro execution.
 */
public class TriggerContext {
    private TriggerType triggerType;           // Type of trigger
    private String source;                     // Source of the trigger (e.g., user ID, system component)
    private LocalDateTime triggerTime;         // When the trigger occurred
    private Map<String, Object> contextData;   // Trigger-specific data

    /**
     * Default constructor.
     */
    public TriggerContext() {
        this.contextData = new HashMap<>();
        this.triggerTime = LocalDateTime.now();
    }

    /**
     * Constructor with essential fields.
     *
     * @param triggerType the trigger type
     * @param source the trigger source
     */
    public TriggerContext(TriggerType triggerType, String source) {
        this();
        this.triggerType = triggerType;
        this.source = source;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(LocalDateTime triggerTime) {
        this.triggerTime = triggerTime;
    }

    public Map<String, Object> getContextData() {
        return contextData;
    }

    public void setContextData(Map<String, Object> contextData) {
        this.contextData = contextData != null ? contextData : new HashMap<>();
    }

    /**
     * Gets a context data value.
     *
     * @param key the data key
     * @return the value, or null if not found
     */
    public Object getContextValue(String key) {
        return contextData.get(key);
    }

    /**
     * Sets a context data value.
     *
     * @param key the data key
     * @param value the value to set
     */
    public void setContextValue(String key, Object value) {
        if (key != null && value != null) {
            this.contextData.put(key, value);
        }
    }

    /**
     * Creates a TriggerContext for a manual execution.
     *
     * @param userId the ID of the user who initiated the execution
     * @return a new TriggerContext instance
     */
    public static TriggerContext forManualExecution(String userId) {
        return new TriggerContext(TriggerType.MANUAL, userId);
    }

    /**
     * Creates a TriggerContext for a scheduled execution.
     *
     * @return a new TriggerContext instance
     */
    public static TriggerContext forScheduledExecution() {
        return new TriggerContext(TriggerType.SCHEDULED, "scheduler");
    }

    /**
     * Creates a TriggerContext for a work item event.
     *
     * @param triggerType the specific work item trigger type
     * @param itemId the ID of the work item
     * @param userId the ID of the user who caused the event
     * @return a new TriggerContext instance
     */
    public static TriggerContext forWorkItemEvent(TriggerType triggerType, String itemId, String userId) {
        TriggerContext context = new TriggerContext(triggerType, userId);
        context.setContextValue("itemId", itemId);
        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriggerContext that = (TriggerContext) o;
        return triggerType == that.triggerType &&
                Objects.equals(source, that.source) &&
                Objects.equals(triggerTime, that.triggerTime) &&
                Objects.equals(contextData, that.contextData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triggerType, source, triggerTime, contextData);
    }

    @Override
    public String toString() {
        return "TriggerContext{" +
                "triggerType=" + triggerType +
                ", source='" + source + '\'' +
                ", triggerTime=" + triggerTime +
                '}';
    }
}