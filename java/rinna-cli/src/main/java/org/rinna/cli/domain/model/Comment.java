/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a comment on a work item.
 */
public interface Comment {
    
    /**
     * Gets the ID of the comment.
     *
     * @return the comment ID
     */
    UUID id();
    
    /**
     * Gets the ID of the work item this comment belongs to.
     *
     * @return the work item ID
     */
    UUID workItemId();
    
    /**
     * Gets the user who wrote the comment.
     *
     * @return the user
     */
    String user();
    
    /**
     * Gets the timestamp when the comment was made.
     *
     * @return the timestamp
     */
    Instant timestamp();
    
    /**
     * Gets the text of the comment.
     *
     * @return the text
     */
    String text();
    
    /**
     * Gets the type of the comment.
     *
     * @return the comment type
     */
    CommentType type();
}