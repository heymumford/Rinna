/*
 * Domain service for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model.macro;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Evaluator for MacroConditions that handles regex pattern matching with safety mechanisms
 * to prevent ReDoS (Regular Expression Denial of Service) attacks.
 * <p>
 * This evaluator implements safeguards against problematic regex patterns by:
 * <ul>
 *   <li>Validating regex patterns before use</li>
 *   <li>Enforcing size limits on patterns and input text</li>
 *   <li>Using timeouts to prevent excessive processing time</li>
 *   <li>Executing potentially dangerous patterns in separate threads for isolation</li>
 * </ul>
 */
public class MacroConditionEvaluator {
    // Constants for security limits
    private static final int MAX_PATTERN_LENGTH = 1000;
    private static final int MAX_INPUT_LENGTH = 1_000_000;
    private static final long DEFAULT_TIMEOUT_MS = 500; // 500ms timeout for regex matching
    
    // Executor for running potentially dangerous regex operations with timeouts
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Evaluates a MacroCondition against a context map.
     * Handles all condition types including FIELD_MATCHES safely.
     *
     * @param condition the condition to evaluate
     * @param context the context map containing field values
     * @return true if the condition matches, false otherwise
     * @throws IllegalArgumentException if there's an issue with the condition or fields
     */
    public static boolean evaluate(MacroCondition condition, Map<String, Object> context) {
        if (condition == null) {
            return true; // Null conditions are considered to match
        }
        
        // Handle logical conditions first
        if (condition.isLogicalCondition()) {
            return evaluateLogicalCondition(condition, context);
        }
        
        // For field conditions, get the field value from context
        String fieldName = condition.getField();
        if (fieldName == null || !context.containsKey(fieldName)) {
            return false; // Field not found in context
        }
        
        Object contextValue = context.get(fieldName);
        if (contextValue == null) {
            // Null field values only match null pattern values
            return condition.getValue() == null;
        }
        
        String fieldValue = contextValue.toString();
        
        // Now handle different field condition types
        switch (condition.getType()) {
            case FIELD_EQUALS:
                return fieldValue.equals(condition.getValue() != null ? condition.getValue().toString() : null);
                
            case FIELD_NOT_EQUALS:
                return !fieldValue.equals(condition.getValue() != null ? condition.getValue().toString() : null);
                
            case FIELD_CONTAINS:
                return condition.getValue() != null && 
                       fieldValue.contains(condition.getValue().toString());
                
            case FIELD_MATCHES:
                return evaluateRegexMatch(fieldValue, 
                         condition.getValue() != null ? condition.getValue().toString() : "");
                
            case FIELD_GREATER_THAN:
                // Implementation would depend on field type
                return false;
                
            case FIELD_LESS_THAN:
                // Implementation would depend on field type
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Evaluates logical conditions (AND, OR, NOT) recursively.
     *
     * @param condition the logical condition to evaluate
     * @param context the context map
     * @return the result of the logical evaluation
     */
    private static boolean evaluateLogicalCondition(MacroCondition condition, Map<String, Object> context) {
        switch (condition.getType()) {
            case AND:
                // All subconditions must match
                return condition.getSubConditions().stream()
                        .allMatch(sub -> evaluate(sub, context));
                
            case OR:
                // At least one subcondition must match
                return condition.getSubConditions().stream()
                        .anyMatch(sub -> evaluate(sub, context));
                
            case NOT:
                // Negate the subcondition
                return condition.getSubConditions().size() > 0 && 
                       !evaluate(condition.getSubConditions().get(0), context);
                
            default:
                return false;
        }
    }
    
    /**
     * Safely evaluates a regex pattern match with protections against ReDoS attacks.
     *
     * @param input the input string to match against
     * @param patternString the regex pattern string
     * @return true if the pattern matches the input
     * @throws IllegalArgumentException if the pattern is invalid or too complex
     */
    private static boolean evaluateRegexMatch(String input, String patternString) {
        // Security check: Pattern length
        if (patternString == null || patternString.length() > MAX_PATTERN_LENGTH) {
            throw new IllegalArgumentException(
                "Regex pattern is null or exceeds maximum length of " + MAX_PATTERN_LENGTH + " characters");
        }
        
        // Security check: Input length
        if (input == null || input.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException(
                "Input string is null or exceeds maximum length of " + MAX_INPUT_LENGTH + " characters");
        }
        
        // Basic validation of common "evil" patterns before even trying to compile
        validatePatternSafety(patternString);
        
        // Compile the pattern
        final Pattern pattern;
        try {
            pattern = Pattern.compile(patternString);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
        }
        
        // Run the pattern matching with a timeout
        return executeWithTimeout(() -> pattern.matcher(input).find(), DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * Validates a regex pattern for known problematic patterns that could lead to ReDoS.
     *
     * @param patternString the pattern to validate
     * @throws IllegalArgumentException if the pattern contains potentially dangerous constructs
     */
    private static void validatePatternSafety(String patternString) {
        // Naive check for nested repetition constructs like (a+)+
        if (patternString.matches(".*\\([^)]*[+*]\\)[+*].*")) {
            throw new IllegalArgumentException("Pattern contains potentially unsafe nested repetition");
        }
        
        // Check for evil patterns like (a|a?)+
        if (patternString.matches(".*\\([^)]*\\|[^)]*\\?\\)[+*].*")) {
            throw new IllegalArgumentException("Pattern contains potentially unsafe alternation with optional elements");
        }
        
        // Check for backreferences with quantifiers
        if (patternString.matches(".*\\\\\\d[+*].*")) {
            throw new IllegalArgumentException("Pattern contains backreferences with quantifiers");
        }
    }
    
    /**
     * Executes a callable with a timeout to prevent excessive processing time.
     *
     * @param <T> the return type of the callable
     * @param callable the callable to execute
     * @param timeoutMs the timeout in milliseconds
     * @return the result of the callable
     * @throws IllegalArgumentException if execution times out or fails
     */
    private static <T> T executeWithTimeout(Callable<T> callable, long timeoutMs) {
        Future<T> future = executor.submit(callable);
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalArgumentException("Regex evaluation timed out after " + timeoutMs + "ms");
        } catch (Exception e) {
            throw new IllegalArgumentException("Error during regex evaluation: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown hook to clean up the executor service when the application exits.
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdownNow();
        }));
    }
}