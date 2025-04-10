package org.rinna.usecase;

import java.util.Date;
import java.util.List;

/**
 * Service for managing comments on work items.
 */
public interface CommentService {
    /**
     * Add a comment to a work item.
     *
     * @param workItemId the ID of the work item
     * @param text the comment text
     * @param author the author of the comment
     * @return the ID of the new comment
     */
    String addComment(String workItemId, String text, String author);
    
    /**
     * Get all comments for a work item.
     *
     * @param workItemId the ID of the work item
     * @return list of comments
     */
    List<Comment> getComments(String workItemId);
    
    /**
     * Get a specific comment.
     *
     * @param commentId the ID of the comment
     * @return the comment
     */
    Comment getComment(String commentId);
    
    /**
     * Update a comment.
     *
     * @param commentId the ID of the comment
     * @param newText the new text
     * @return the updated comment
     */
    Comment updateComment(String commentId, String newText);
    
    /**
     * Delete a comment.
     *
     * @param commentId the ID of the comment
     * @return true if the comment was deleted, false otherwise
     */
    boolean deleteComment(String commentId);
    
    /**
     * A comment on a work item.
     */
    interface Comment {
        /**
         * Get the ID of the comment.
         *
         * @return the comment ID
         */
        String getId();
        
        /**
         * Get the ID of the work item.
         *
         * @return the work item ID
         */
        String getWorkItemId();
        
        /**
         * Get the text of the comment.
         *
         * @return the comment text
         */
        String getText();
        
        /**
         * Get the author of the comment.
         *
         * @return the author
         */
        String getAuthor();
        
        /**
         * Get the creation date of the comment.
         *
         * @return the creation date
         */
        Date getCreatedAt();
        
        /**
         * Get the last modification date of the comment.
         *
         * @return the last modification date
         */
        Date getUpdatedAt();
    }
}