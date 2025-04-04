/*
 * BDD test hooks for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Hooks for Cucumber scenarios.
 * This class defines actions to take before and after scenarios.
 */
public class Hooks {
    private final TestContext context;
    
    /**
     * Constructs a new Hooks with the given test context.
     *
     * @param context the test context
     */
    public Hooks(TestContext context) {
        this.context = context;
    }
    
    /**
     * Sets up the test context before each scenario.
     *
     * @param scenario the Cucumber scenario
     */
    @Before
    public void setUp(Scenario scenario) {
        // Clear any previous exceptions
        context.clearException();
    }
    
    /**
     * Cleans up after each scenario.
     *
     * @param scenario the Cucumber scenario
     */
    @After
    public void tearDown(Scenario scenario) {
        // Check if there was an exception that wasn't expected
        if (context.getException() != null && !scenario.isFailed()) {
            scenario.log("Unexpected exception: " + context.getException().getMessage());
            scenario.log("Stack trace: " + stackTraceToString(context.getException()));
        }
    }
    
    /**
     * Converts a stack trace to a string.
     *
     * @param e the exception
     * @return the stack trace as a string
     */
    private String stackTraceToString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}