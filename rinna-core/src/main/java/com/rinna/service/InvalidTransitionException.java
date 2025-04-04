package com.rinna.service;

/**
 * Exception thrown when an invalid workflow transition is attempted.
 */
public class InvalidTransitionException extends Exception {
    
    /**
     * Constructs a new InvalidTransitionException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidTransitionException(String message) {
        super(message);
    }
}