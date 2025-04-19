package org.rinna.domain.model;

/**
 * Enumeration of possible history entry types for work items.
 * This enum defines the various types of changes that can be recorded in a work item's history.
 */
public enum HistoryEntryType {
    /**
     * Represents a change in the work item's state (e.g., from OPEN to IN_PROGRESS).
     */
    STATE_CHANGE,
    
    /**
     * Represents a change in the work item's assignee.
     */
    ASSIGNEE_CHANGE,
    
    /**
     * Represents a change in the work item's priority.
     */
    PRIORITY_CHANGE,
    
    /**
     * Represents a change in the work item's title.
     */
    TITLE_CHANGE,
    
    /**
     * Represents a change in the work item's description.
     */
    DESCRIPTION_CHANGE,
    
    /**
     * Represents the addition of a comment to the work item.
     */
    COMMENT_ADDED,
    
    /**
     * Represents the addition of an attachment to the work item.
     */
    ATTACHMENT_ADDED,
    
    /**
     * Represents the removal of an attachment from the work item.
     */
    ATTACHMENT_REMOVED,
    
    /**
     * Represents the addition of a tag to the work item.
     */
    TAG_ADDED,
    
    /**
     * Represents the removal of a tag from the work item.
     */
    TAG_REMOVED,
    
    /**
     * Represents the creation of the work item.
     */
    CREATED,
    
    /**
     * Represents the deletion of the work item.
     */
    DELETED,
    
    /**
     * Represents a link being established between this work item and another.
     */
    LINK_ADDED,
    
    /**
     * Represents a link being removed between this work item and another.
     */
    LINK_REMOVED,
    
    /**
     * Represents a custom or miscellaneous change not covered by other types.
     */
    CUSTOM;
    
    /**
     * Returns a human-readable description of the history entry type.
     *
     * @return A description of the history entry type
     */
    public String getDescription() {
        switch (this) {
            case STATE_CHANGE:
                return "State changed";
            case ASSIGNEE_CHANGE:
                return "Assignee changed";
            case PRIORITY_CHANGE:
                return "Priority changed";
            case TITLE_CHANGE:
                return "Title updated";
            case DESCRIPTION_CHANGE:
                return "Description updated";
            case COMMENT_ADDED:
                return "Comment added";
            case ATTACHMENT_ADDED:
                return "Attachment added";
            case ATTACHMENT_REMOVED:
                return "Attachment removed";
            case TAG_ADDED:
                return "Tag added";
            case TAG_REMOVED:
                return "Tag removed";
            case CREATED:
                return "Work item created";
            case DELETED:
                return "Work item deleted";
            case LINK_ADDED:
                return "Link added";
            case LINK_REMOVED:
                return "Link removed";
            case CUSTOM:
                return "Custom change";
            default:
                return "Unknown change";
        }
    }
}