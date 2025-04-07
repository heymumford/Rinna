# Rinna Testing Framework

This document provides an overview of the standardized testing framework for the Rinna project.

## Implementation Status

We have implemented a standardized test approach with the following components:

1. **Unified Directory Structure**:
   - `src/test/java/org/rinna/unit/` - Unit tests
   - `src/test/java/org/rinna/component/` - Component tests
   - `src/test/java/org/rinna/integration/` - Integration tests
   - `src/test/java/org/rinna/acceptance/` - Acceptance tests
   - `src/test/java/org/rinna/performance/` - Performance tests

2. **Standardized Test Tagging**:
   - `@Tag("unit")` for unit tests
   - `@Tag("component")` for component tests
   - `@Tag("integration")` for integration tests
   - `@Tag("acceptance")` for acceptance tests
   - `@Tag("performance")` for performance tests
   - `@Tag("admin")` for admin functionality tests

3. **Standardized File Naming**:
   - Unit tests: `*Test.java`
   - Component tests: `*ComponentTest.java`
   - Integration tests: `*IntegrationTest.java`
   - Acceptance tests: `*AcceptanceTest.java`
   - Performance tests: `*PerformanceTest.java`

4. **Test Runner**:
   - `bin/rin-test` script for running tests by category

## Key Documents

1. [Unified Test Approach](UNIFIED_TEST_APPROACH.md) - The high-level standardized approach to testing
2. [Test Migration Plan](TEST_MIGRATION_PLAN.md) - Steps for migrating existing tests to the new structure
3. [Testing Strategy](TESTING_STRATEGY.md) - Overall testing strategy for Rinna
4. [Admin Testing Guide](ADMIN_TESTING.md) - Guide for running and maintaining admin functionality tests

## Running Tests

Use the `bin/rin-test` script to run tests by category:

```bash
# Run all tests
./bin/rin-test

# Run specific test categories
./bin/rin-test unit
./bin/rin-test component
./bin/rin-test integration
./bin/rin-test acceptance
./bin/rin-test performance
./bin/rin-test admin

# Run fast tests (unit + component)
./bin/rin-test fast

# Run tests in parallel
./bin/rin-test unit --parallel

# Run tests with specific tags
./bin/rin-test --tag=unit
./bin/rin-test --tag=admin

# Run admin tests using the dedicated script
./bin/run-admin-tests.sh             # All admin tests
./bin/run-admin-tests.sh --config    # Configuration tests only
./bin/run-admin-tests.sh --integration # Maven & server tests only
./bin/run-admin-tests.sh --project   # Project management tests only
```

## Test Writing Examples

### Unit Test Example

```java
package org.rinna.unit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("Service Unit Tests")
public class ServiceTest {
    @Test
    @DisplayName("Should perform operation correctly")
    void shouldPerformOperationCorrectly() {
        // Test implementation
    }
}
```

### Component Test Example

```java
package org.rinna.component.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("component")
@DisplayName("Service Component Tests")
public class ServiceComponentTest {
    @Test
    @DisplayName("Should integrate with other components")
    void shouldIntegrateWithOtherComponents() {
        // Test implementation
    }
}
```

## BDD Testing

BDD tests are placed in the acceptance layer with the following structure:

1. Feature files in `src/test/resources/features/`
2. Step definitions in `src/test/java/org/rinna/acceptance/steps/` or `src/test/java/org/rinna/bdd/`
3. Test runners in `src/test/java/org/rinna/acceptance/` or `src/test/java/org/rinna/bdd/`

For admin functionality testing specifically, see the [Admin Testing Guide](ADMIN_TESTING.md) for details on:
- Running admin-specific BDD tests
- Understanding the admin testing structure
- Extending admin functionality tests

## Maven Integration

Maven profiles are configured to run tests by category:

```xml
<profile>
  <id>unit-tests</id>
  <properties>
    <test.groups>unit</test.groups>
  </properties>
</profile>
```

To use these profiles with Maven directly:

```bash
mvn test -P unit-tests
mvn test -P component-tests
mvn verify -P integration-tests
mvn verify -P acceptance-tests
mvn verify -P performance-tests
```