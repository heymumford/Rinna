package org.rinna.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all test types providing common functionality.
 * 
 * Features:
 * - Automatic test logging
 * - Test lifecycle management
 * - Common utilities for all tests
 */
@ExtendWith(TestExecutionTimeExtension.class)
public abstract class BaseTest {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected TestInfo testInfo;
    
    @BeforeEach
    public void setTestInfo(TestInfo testInfo) {
        this.testInfo = testInfo;
        logger.info("Starting test: {}", testInfo.getDisplayName());
    }
    
    /**
     * Helper method to get current test name.
     * 
     * @return Current test name
     */
    protected String getTestName() {
        return testInfo.getDisplayName();
    }
    
    /**
     * Helper method to get current test method name.
     * 
     * @return Current test method name
     */
    protected String getTestMethodName() {
        return testInfo.getTestMethod().orElseThrow().getName();
    }
    
    /**
     * Helper method to get current test class name.
     * 
     * @return Current test class name
     */
    protected String getTestClassName() {
        return testInfo.getTestClass().orElseThrow().getSimpleName();
    }
}