package org.rinna.cli.model;

import java.time.LocalDateTime;

/**
 * Represents a comment on a work item.
 * This class is used to track comments and feedback associated with work items.
 */
public class Comment {
    private String id;
    private String workItemId;
    private String author;
    private String text;
    private LocalDateTime timestamp;
    private boolean edited;
    private LocalDateTime editedTimestamp;

    /**
     * Default constructor for serialization.
     */
    public Comment() {
    }

    /**
     * Creates a new Comment instance.
     *
     * @param workItemId The ID of the work item this comment belongs to
     * @param author The author of the comment
     * @param text The comment text
     */
    public Comment(String workItemId, String author, String text) {
        this.id = java.util.UUID.randomUUID().toString();
        this.workItemId = workItemId;
        this.author = author;
        this.text = text;
        this.timestamp = LocalDateTime.now();
        this.edited = false;
    }

    /**
     * Gets the comment ID.
     *
     * @return The comment ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the comment ID.
     *
     * @param id The comment ID
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
     * Gets the author of the comment.
     *
     * @return The author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author of the comment.
     *
     * @param author The author
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Gets the comment text.
     *
     * @return The comment text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the comment text.
     *
     * @param text The comment text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the timestamp when the comment was created.
     *
     * @return The timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the comment was created.
     *
     * @param timestamp The timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Checks if the comment has been edited.
     *
     * @return true if edited, false otherwise
     */
    public boolean isEdited() {
        return edited;
    }

    /**
     * Sets whether the comment has been edited.
     *
     * @param edited true if edited, false otherwise
     */
    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    /**
     * Gets the timestamp when the comment was last edited.
     *
     * @return The edited timestamp
     */
    public LocalDateTime getEditedTimestamp() {
        return editedTimestamp;
    }

    /**
     * Sets the timestamp when the comment was last edited.
     *
     * @param editedTimestamp The edited timestamp
     */
    public void setEditedTimestamp(LocalDateTime editedTimestamp) {
        this.editedTimestamp = editedTimestamp;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", workItemId='" + workItemId + '\'' +
                ", author='" + author + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                ", edited=" + edited +
                ", editedTimestamp=" + editedTimestamp +
                '}';
    }
}