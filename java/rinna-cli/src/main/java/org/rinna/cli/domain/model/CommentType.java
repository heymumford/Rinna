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

/**
 * Enum representing different types of comments.
 */
public enum CommentType {
    /**
     * A standard comment.
     */
    STANDARD,
    
    /**
     * A system-generated comment.
     */
    SYSTEM,
    
    /**
     * A private comment only visible to certain users.
     */
    PRIVATE,
    
    /**
     * A comment that contains code.
     */
    CODE,
    
    /**
     * A comment that contains a link to an attachment.
     */
    ATTACHMENT
}