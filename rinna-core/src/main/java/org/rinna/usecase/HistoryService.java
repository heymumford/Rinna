package org.rinna.usecase;

import java.util.Date;
import java.util.List;

/**
 * Service for managing work item history.
 */
public interface HistoryService {
    /**
     * Add a history entry to a work item.
     *
     * @param workItemId the ID of the work item
     * @param type the type of history entry
     * @param details the details of the change
     * @param user the user who made the change
     * @return the ID of the new history entry
     */
    String addHistoryEntry(String workItemId, HistoryEntryType type, String details, String user);
    
    /**
     * Get the history for a work item.
     *
     * @param workItemId the ID of the work item
     * @return list of history entries
     */
    List<HistoryEntry> getHistory(String workItemId);
    
    /**
     * Get a specific history entry.
     *
     * @param entryId the ID of the history entry
     * @return the history entry
     */
    HistoryEntry getHistoryEntry(String entryId);
    
    /**
     * A history entry for a work item.
     */
    interface HistoryEntry {
        /**
         * Get the ID of the history entry.
         *
         * @return the history entry ID
         */
        String getId();
        
        /**
         * Get the ID of the work item.
         *
         * @return the work item ID
         */
        String getWorkItemId();
        
        /**
         * Get the type of history entry.
         *
         * @return the history entry type
         */
        HistoryEntryType getType();
        
        /**
         * Get the details of the change.
         *
         * @return the change details
         */
        String getDetails();
        
        /**
         * Get the user who made the change.
         *
         * @return the user
         */
        String getUser();
        
        /**
         * Get the timestamp of the change.
         *
         * @return the timestamp
         */
        Date getTimestamp();
    }
    
    /**
     * Types of history entries.
     */
    enum HistoryEntryType {
        CREATED,
        STATUS_CHANGED,
        PRIORITY_CHANGED,
        ASSIGNED,
        UNASSIGNED,
        COMMENT_ADDED,
        COMMENT_UPDATED,
        COMMENT_DELETED,
        DEPENDENCY_ADDED,
        DEPENDENCY_REMOVED,
        FIELD_UPDATED,
        ATTACHMENT_ADDED,
        ATTACHMENT_REMOVED,
        OTHER
    }
}