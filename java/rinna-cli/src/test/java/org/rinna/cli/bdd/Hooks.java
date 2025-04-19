/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.bdd;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber hooks for setup and teardown.
 */
public class Hooks {
    
    private final TestContext testContext = TestContext.getInstance();
    
    @Before
    public void setUp(Scenario scenario) {
        // Reset test context
        testContext.clearState();
        testContext.resetCapturedOutput();
        
        // Redirect console output for capturing
        testContext.redirectConsoleOutput();
        
        // Set up mock services
        testContext.setupMockServices();
        
        // Initialize test data
        initializeTestData();
    }
    
    @After
    public void tearDown(Scenario scenario) {
        // Restore console output
        testContext.restoreConsoleOutput();
        
        // Add captured output to scenario if there is an error
        if (scenario.isFailed()) {
            String standardOutput = testContext.getStandardOutput();
            String errorOutput = testContext.getErrorOutput();
            
            if (!standardOutput.isEmpty()) {
                scenario.log("Standard Output:\n" + standardOutput);
            }
            
            if (!errorOutput.isEmpty()) {
                scenario.log("Error Output:\n" + errorOutput);
            }
        }
    }
    
    /**
     * Initializes test data for BDD scenarios.
     */
    private void initializeTestData() {
        // Create sample work items
        testContext.getMockItemService().createSampleItems();
    }
}