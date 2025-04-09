/*
 * Domain entity interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Interface representing a comment on a work item.
 */
public interface Comment {
    /**
     * Returns the unique identifier of this comment.
     *
     * @return the comment's UUID
     */
    UUID id();
    
    /**
     * Returns the ID of the work item this comment is associated with.
     *
     * @return the work item's UUID
     */
    UUID workItemId();
    
    /**
     * Returns the author of this comment.
     *
     * @return the comment author
     */
    String author();
    
    /**
     * Returns the text content of this comment.
     *
     * @return the comment text
     */
    String text();
    
    /**
     * Returns the timestamp when this comment was created.
     *
     * @return the creation timestamp
     */
    Instant timestamp();
    
    /**
     * Returns the type of this comment.
     *
     * @return the comment type
     */
    CommentType type();
}
