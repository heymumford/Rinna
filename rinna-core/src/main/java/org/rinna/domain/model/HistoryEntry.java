package org.rinna.domain.model;

import java.time.LocalDateTime;

/**
 * Represents an entry in the history log of a work item.
 * This class is used to track changes and transitions in the lifecycle of work items.
 */
public class HistoryEntry {
    private String id;
    private String workItemId;
    private HistoryEntryType type;
    private String fromState;
    private String toState;
    private String user;
    private LocalDateTime timestamp;
    private String comment;
    private String details;

    /**
     * Default constructor for serialization.
     */
    public HistoryEntry() {
    }

    /**
     * Creates a new HistoryEntry instance for a state transition.
     *
     * @param workItemId The ID of the work item
     * @param fromState The previous state
     * @param toState The new state
     * @param user The user who made the change
     * @param comment Optional comment about the change
     */
    public HistoryEntry(String workItemId, String fromState, String toState, String user, String comment) {
        this.id = java.util.UUID.randomUUID().toString();
        this.workItemId = workItemId;
        this.type = HistoryEntryType.STATE_CHANGE;
        this.fromState = fromState;
        this.toState = toState;
        this.user = user;
        this.timestamp = LocalDateTime.now();
        this.comment = comment;
    }

    /**
     * Creates a new HistoryEntry instance with a specific type.
     *
     * @param workItemId The ID of the work item
     * @param type The type of history entry
     * @param user The user who made the change
     * @param details Details about the change
     */
    public HistoryEntry(String workItemId, HistoryEntryType type, String user, String details) {
        this.id = java.util.UUID.randomUUID().toString();
        this.workItemId = workItemId;
        this.type = type;
        this.user = user;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    /**
     * Gets the entry ID.
     *
     * @return The entry ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the entry ID.
     *
     * @param id The entry ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the work item ID.
     *
     * @return The work item ID
     */
    public String getWorkItemId() {
        return workItemId;
    }

    /**
     * Sets the work item ID.
     *
     * @param workItemId The work item ID
     */
    public void setWorkItemId(String workItemId) {
        this.workItemId = workItemId;
    }

    /**
     * Gets the entry type.
     *
     * @return The entry type
     */
    public HistoryEntryType getType() {
        return type;
    }

    /**
     * Sets the entry type.
     *
     * @param type The entry type
     */
    public void setType(HistoryEntryType type) {
        this.type = type;
    }

    /**
     * Gets the previous state.
     *
     * @return The previous state
     */
    public String getFromState() {
        return fromState;
    }

    /**
     * Sets the previous state.
     *
     * @param fromState The previous state
     */
    public void setFromState(String fromState) {
        this.fromState = fromState;
    }

    /**
     * Gets the new state.
     *
     * @return The new state
     */
    public String getToState() {
        return toState;
    }

    /**
     * Sets the new state.
     *
     * @param toState The new state
     */
    public void setToState(String toState) {
        this.toState = toState;
    }

    /**
     * Gets the user who made the change.
     *
     * @return The user
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user who made the change.
     *
     * @param user The user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Gets the timestamp when the change was made.
     *
     * @return The timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the change was made.
     *
     * @param timestamp The timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the comment about the change.
     *
     * @return The comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment about the change.
     *
     * @param comment The comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Gets the details about the change.
     *
     * @return The details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the details about the change.
     *
     * @param details The details
     */
    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
                "id='" + id + '\'' +
                ", workItemId='" + workItemId + '\'' +
                ", type=" + type +
                ", fromState='" + fromState + '\'' +
                ", toState='" + toState + '\'' +
                ", user='" + user + '\'' +
                ", timestamp=" + timestamp +
                ", comment='" + comment + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}