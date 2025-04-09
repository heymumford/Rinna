package org.rinna.base;

import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base class for all tests.
 */
public abstract class BaseTest {
    
    protected static Logger logger;
    
    @BeforeAll
    static void setupBaseTest() {
        // Get logger for the actual test class
        logger = LoggerFactory.getLogger(BaseTest.class);
    }
}
