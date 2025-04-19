package org.rinna.base;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit extension that logs test execution time and helps identify slow tests.
 */
public class TestExecutionTimeExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionTimeExtension.class);
    private static final String START_TIME = "start_time";
    
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        getStore(context).put(START_TIME, System.currentTimeMillis());
    }
    
    @Override
    public void afterTestExecution(ExtensionContext context) {
        long startTime = getStore(context).get(START_TIME, long.class);
        long executionTime = System.currentTimeMillis() - startTime;
        
        String testMethod = context.getRequiredTestMethod().getName();
        String testClass = context.getRequiredTestClass().getSimpleName();
        
        if (executionTime > 1000) {
            // Log a warning for slow tests
            logger.warn("SLOW TEST: {}.{} took {}ms to execute", testClass, testMethod, executionTime);
        } else {
            logger.info("TEST TIME: {}.{} - {}ms", testClass, testMethod, executionTime);
        }
    }
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
}