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
 * Interface representing a single history entry for a work item.
 * A history entry can be a comment, a state change, or other event.
 */
public interface HistoryEntry {
    /**
     * Returns the unique identifier of this history entry.
     *
     * @return the entry's UUID
     */
    UUID id();
    
    /**
     * Returns the ID of the work item this entry is associated with.
     *
     * @return the work item's UUID
     */
    UUID workItemId();
    
    /**
     * Returns the type of this history entry.
     *
     * @return the entry type
     */
    HistoryEntryType type();
    
    /**
     * Returns the user who created this entry, if applicable.
     *
     * @return the user, or null if this is a system entry
     */
    String user();
    
    /**
     * Returns the timestamp when this entry was created.
     *
     * @return the creation timestamp
     */
    Instant timestamp();
    
    /**
     * Returns the description or content of this entry.
     *
     * @return the entry content
     */
    String content();
    
    /**
     * Returns additional data associated with this entry, if any.
     * This can include before/after values for changes, etc.
     *
     * @return the additional data, or null if none
     */
    String additionalData();
}
