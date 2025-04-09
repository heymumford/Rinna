package org.rinna.domain.model.macro;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an event that can trigger macro executions.
 */
public class TriggerEvent {
    private String id;                      // Unique event ID
    private TriggerType type;               // Type of trigger event
    private String source;                  // Source of the event (e.g., user ID, system component)
    private LocalDateTime timestamp;        // When the event occurred
    private Map<String, Object> payload;    // Event-specific data

    /**
     * Default constructor.
     */
    public TriggerEvent() {
        this.id = UUID.randomUUID().toString();
        this.payload = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with type and source.
     *
     * @param type the trigger type
     * @param source the event source
     */
    public TriggerEvent(TriggerType type, String source) {
        this();
        this.type = type;
        this.source = source;
    }

    /**
     * Constructor with type, source, and payload.
     *
     * @param type the trigger type
     * @param source the event source
     * @param payload the event payload
     */
    public TriggerEvent(TriggerType type, String source, Map<String, Object> payload) {
        this(type, source);
        if (payload != null) {
            this.payload.putAll(payload);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TriggerType getType() {
        return type;
    }

    public void setType(TriggerType type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload != null ? payload : new HashMap<>();
    }

    /**
     * Gets a payload value.
     *
     * @param key the payload key
     * @return the value, or null if not found
     */
    public Object getPayloadValue(String key) {
        return payload.get(key);
    }

    /**
     * Gets a payload value as a string.
     *
     * @param key the payload key
     * @return the value as a string, or null if not found
     */
    public String getPayloadValueAsString(String key) {
        Object value = getPayloadValue(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Sets a payload value.
     *
     * @param key the payload key
     * @param value the value to set
     */
    public void setPayloadValue(String key, Object value) {
        if (key != null && value != null) {
            this.payload.put(key, value);
        }
    }

    /**
     * Creates a TriggerEvent for a manual execution.
     *
     * @param userId the ID of the user who initiated the execution
     * @param macroId the ID of the macro to execute
     * @return a new TriggerEvent instance
     */
    public static TriggerEvent forManualExecution(String userId, String macroId) {
        TriggerEvent event = new TriggerEvent(TriggerType.MANUAL, userId);
        event.setPayloadValue("macroId", macroId);
        return event;
    }

    /**
     * Creates a TriggerEvent for a scheduled execution.
     *
     * @param macroId the ID of the macro to execute
     * @return a new TriggerEvent instance
     */
    public static TriggerEvent forScheduledExecution(String macroId) {
        TriggerEvent event = new TriggerEvent(TriggerType.SCHEDULED, "scheduler");
        event.setPayloadValue("macroId", macroId);
        return event;
    }

    /**
     * Creates a TriggerEvent for a work item event.
     *
     * @param triggerType the specific work item trigger type
     * @param itemId the ID of the work item
     * @param userId the ID of the user who caused the event
     * @return a new TriggerEvent instance
     */
    public static TriggerEvent forWorkItemEvent(TriggerType triggerType, String itemId, String userId) {
        TriggerEvent event = new TriggerEvent(triggerType, userId);
        event.setPayloadValue("itemId", itemId);
        return event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriggerEvent that = (TriggerEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TriggerEvent{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}