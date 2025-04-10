/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.api.test.karate;

import com.intuit.karate.junit5.Karate;

/**
 * JUnit 5 runner for Karate API tests.
 * This class organizes test executions into logical groups,
 * allowing for selective running of test suites.
 */
class KarateTests {
    
    /**
     * Run all Work Item API tests
     */
    @Karate.Test
    Karate workItemTests() {
        return Karate.run("workitems").relativeTo(getClass());
    }
    
    /**
     * Run all API tests
     */
    @Karate.Test
    Karate allTests() {
        return Karate.run().relativeTo(getClass());
    }
    
    /**
     * Run only smoke tests
     */
    @Karate.Test
    Karate smokeTests() {
        return Karate.run().tags("@smoke").relativeTo(getClass());
    }
    
    /**
     * Run only CRUD operation tests
     */
    @Karate.Test
    Karate crudTests() {
        return Karate.run().tags("@crud").relativeTo(getClass());
    }
    
    /**
     * Run only security-related tests
     */
    @Karate.Test
    Karate securityTests() {
        return Karate.run().tags("@security").relativeTo(getClass());
    }
    
    /**
     * Run only performance tests
     */
    @Karate.Test
    Karate performanceTests() {
        return Karate.run().tags("@performance").relativeTo(getClass());
    }
    
    /**
     * Run tests with parallel execution for faster feedback
     */
    @Karate.Test
    Karate parallelTests() {
        return Karate.run().tags("@smoke,@crud").parallel(5);
    }
    
    /**
     * Run only negative test cases
     */
    @Karate.Test
    Karate negativeTests() {
        return Karate.run().tags("@negative").relativeTo(getClass());
    }
}