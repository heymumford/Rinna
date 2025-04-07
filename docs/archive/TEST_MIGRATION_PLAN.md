# [ARCHIVED] Test Migration Plan

> **Note**: This document is archived and has been replaced by the [Unified Test Approach](../testing/UNIFIED_TEST_APPROACH.md) document. The migration described in this plan has been completed.

This document outlines the plan for migrating existing tests to our new unified test structure.

## Current Issues

Our test suite currently has several inconsistencies:

1. **Inconsistent tagging**: Tests are tagged using direct JUnit 5 `@Tag` annotations, inheritance from base classes, and duplicated tag classes
2. **Scattered locations**: Test files are duplicated across src/test, rinna-core/src/test, and module-specific test directories
3. **Mixed naming conventions**: Inconsistent test class naming makes it hard to identify test types
4. **Redundant configurations**: Multiple parallel implementations of test runners and configurations

## Migration Strategy

We'll adopt a phased approach to minimize disruption while systematically improving test organization:

### Phase 1: Standardize Base Classes and Tagging (1-2 days)

1. Implement consistent base classes in `src/test/java/org/rinna/base/`
2. Ensure all test classes have proper `@Tag` annotations
3. Update build scripts to recognize standardized tags

### Phase 2: Reorganize Directory Structure (2-3 days)

1. Create new directory structure with proper categorization
2. Move unit tests to `src/test/java/org/rinna/unit/`
3. Move component tests to `src/test/java/org/rinna/component/`
4. Move integration tests to `src/test/java/org/rinna/integration/`
5. Move acceptance tests to `src/test/java/org/rinna/acceptance/`
6. Move performance tests to `src/test/java/org/rinna/performance/`

### Phase 3: Update Test Naming (1-2 days)

1. Rename test classes to follow consistent conventions:
   - Unit tests: `*Test.java`
   - Component tests: `*ComponentTest.java`
   - Integration tests: `*IntegrationTest.java`
   - Acceptance tests: `*AcceptanceTest.java`
   - Performance tests: `*PerformanceTest.java`

### Phase 4: Standardize Test Execution (1-2 days)

1. Update Maven profiles for consistent test selection
2. Implement unified `rin test` command for all test execution
3. Update CI pipeline to use standardized test categories

### Phase 5: Documentation and Training (1 day)

1. Update test documentation with new standards
2. Create examples for each test category
3. Train team on the new test organization

## Test Migration Guide

For each existing test class:

1. **Identify the test type**:
   - If testing a single class in isolation → Unit Test
   - If testing multiple classes in a module → Component Test
   - If testing interaction between modules → Integration Test
   - If testing complete workflows → Acceptance Test
   - If testing performance characteristics → Performance Test

2. **Update class declaration**:
   ```java
   // From:
   public class SomeTest extends AnyExistingBaseClass {
     // test methods
   }
   
   // To:
   @Tag("unit") // Or appropriate tag
   public class SomeTest extends UnitTest { // Or appropriate base class
     // test methods
   }
   ```

3. **Move to the correct package**:
   - Unit tests → `org.rinna.unit.*`
   - Component tests → `org.rinna.component.*`
   - etc.

4. **Rename if necessary** to follow naming conventions

## Example Migrations

### Example 1: Unit Test

```java
// Before: src/test/java/org/rinna/model/DefaultWorkItemTest.java
package org.rinna.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultWorkItemTest {
    @Test
    public void testCreateFromRequest() {
        // test code
    }
}

// After: src/test/java/org/rinna/unit/model/DefaultWorkItemTest.java
package org.rinna.unit.model;

import org.junit.jupiter.api.Test;
import org.rinna.base.UnitTest;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class DefaultWorkItemTest extends UnitTest {
    @Test
    public void testCreateFromRequest() {
        // test code
    }
}
```

### Example 2: Acceptance Test

```java
// Before: src/test/java/org/rinna/bdd/WorkflowSteps.java
package org.rinna.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

public class WorkflowSteps {
    @Given("a workflow with {int} steps")
    public void aWorkflowWithSteps(int steps) {
        // implementation
    }
    
    // more step definitions
}

// After: src/test/java/org/rinna/acceptance/steps/WorkflowSteps.java
package org.rinna.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.rinna.base.AcceptanceTest;

@Tag("acceptance")
@Tag("bdd")
public class WorkflowSteps extends AcceptanceTest {
    @Given("a workflow with {int} steps")
    public void aWorkflowWithSteps(int steps) {
        // implementation
    }
    
    // more step definitions
}
```

## Timeline

- Phase 1: Days 1-2
- Phase 2: Days 3-5
- Phase 3: Days 6-7
- Phase 4: Days 8-9
- Phase 5: Day 10

Total migration time: Approximately 10 working days