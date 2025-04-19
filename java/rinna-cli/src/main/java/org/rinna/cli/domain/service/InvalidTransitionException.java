/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.service;

/**
 * Domain exception for invalid workflow transitions - used to minimize dependency issues
 * during CLI module migration. This is a temporary class that matches
 * the core domain InvalidTransitionException.
 */
public class InvalidTransitionException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidTransitionException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public InvalidTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}