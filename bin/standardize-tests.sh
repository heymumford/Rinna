#!/bin/bash
#
# standardize-tests.sh - Script to reorganize tests according to the unified testing approach
#

set -e

BASEDIR=$(dirname "$(readlink -f "$0")")/..
cd "$BASEDIR"

echo "Standardizing Rinna test organization..."

# Create standardized directory structure
mkdir -p src/test/java/org/rinna/unit
mkdir -p src/test/java/org/rinna/component
mkdir -p src/test/java/org/rinna/integration
mkdir -p src/test/java/org/rinna/acceptance/steps
mkdir -p src/test/java/org/rinna/performance
mkdir -p src/test/java/org/rinna/base
mkdir -p src/test/resources/testdata

# Create base test classes
cat > src/test/java/org/rinna/base/BaseTest.java << 'EOF'
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
EOF

cat > src/test/java/org/rinna/base/UnitTest.java << 'EOF'
package org.rinna.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for unit tests.
 * 
 * Unit tests:
 * - Test individual units of code in isolation
 * - Should be fast and focused
 * - Use mocks for dependencies
 * - Should run in parallel for speed
 */
@Tag("unit")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_METHOD)
public abstract class UnitTest extends BaseTest {
}
EOF

cat > src/test/java/org/rinna/base/ComponentTest.java << 'EOF'
package org.rinna.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for component tests.
 * 
 * Component tests:
 * - Test behavior of components that work together
 * - Test within module boundaries
 * - Use real implementations for in-module dependencies
 * - Mock external dependencies
 */
@Tag("component")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_METHOD)
public abstract class ComponentTest extends BaseTest {
}
EOF

cat > src/test/java/org/rinna/base/IntegrationTest.java << 'EOF'
package org.rinna.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for integration tests.
 * 
 * Integration tests:
 * - Test integration between modules or external dependencies
 * - Use real implementations where practical
 * - May require more complex setup/teardown
 */
@Tag("integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class IntegrationTest extends BaseTest {
}
EOF

cat > src/test/java/org/rinna/base/AcceptanceTest.java << 'EOF'
package org.rinna.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for acceptance tests.
 * 
 * Acceptance tests:
 * - Verify system meets business requirements
 * - End-to-end workflows
 * - Usually use the full system
 */
@Tag("acceptance")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AcceptanceTest extends BaseTest {
}
EOF

cat > src/test/java/org/rinna/base/PerformanceTest.java << 'EOF'
package org.rinna.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for performance tests.
 * 
 * Performance tests:
 * - Verify system performance meets requirements
 * - Test throughput, response time, resource usage
 * - Should be isolated from other tests
 */
@Tag("performance")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class PerformanceTest extends BaseTest {
}
EOF

# Create a test runner script
cat > bin/rin-test << 'EOF'
#!/bin/bash
#
# rin-test - Unified test runner for Rinna
#

BASEDIR=$(dirname "$(readlink -f "$0")")/..
cd "$BASEDIR"

function show_help() {
  echo "Usage: rin test [category] [options]"
  echo
  echo "Run tests for the Rinna project."
  echo
  echo "Categories:"
  echo "  unit           Run unit tests"
  echo "  component      Run component tests"
  echo "  integration    Run integration tests"
  echo "  acceptance     Run acceptance tests" 
  echo "  performance    Run performance tests"
  echo "  fast           Run fast tests (unit + component)"
  echo "  all            Run all tests"
  echo
  echo "Options:"
  echo "  --parallel     Run tests in parallel mode"
  echo "  --verbose      Show verbose output"
  echo "  --tag=<tag>    Run tests with specific tag"
  echo
  exit 0
}

if [ "$1" == "help" ] || [ "$1" == "--help" ]; then
  show_help
fi

# Default values
CATEGORY="all"
PARALLEL=""
VERBOSE=""
TAG=""

# Parse arguments
for arg in "$@"; do
  case $arg in
    unit|component|integration|acceptance|performance|fast|all)
      CATEGORY="$arg"
      ;;
    --parallel)
      PARALLEL="-Djunit.jupiter.execution.parallel.enabled=true"
      ;;
    --verbose)
      VERBOSE="-Dsurefire.showSuccess=true"
      ;;
    --tag=*)
      TAG="-Dgroups=${arg#*=}"
      ;;
  esac
done

# Execute tests based on category
case $CATEGORY in
  unit)
    echo "Running unit tests..."
    mvn test -P unit-tests $PARALLEL $VERBOSE $TAG
    ;;
  component)
    echo "Running component tests..."
    mvn test -P component-tests $PARALLEL $VERBOSE $TAG
    ;;
  integration)
    echo "Running integration tests..."
    mvn verify -P integration-tests $VERBOSE $TAG
    ;;
  acceptance)
    echo "Running acceptance tests..."
    mvn verify -P acceptance-tests $VERBOSE $TAG
    ;;
  performance)
    echo "Running performance tests..."
    mvn verify -P performance-tests $VERBOSE $TAG
    ;;
  fast)
    echo "Running fast tests (unit + component)..."
    mvn test -P fast-tests $PARALLEL $VERBOSE $TAG
    ;;
  all)
    echo "Running all tests..."
    mvn verify $VERBOSE $TAG
    ;;
esac

exit $?
EOF

chmod +x bin/rin-test

# Update Maven pom.xml to include test profiles
# This is a simple placeholder - you would need to properly merge this with existing pom.xml
cat > test-profiles.xml << 'EOF'
<!-- Test profiles for the unified test approach -->
<profile>
  <id>unit-tests</id>
  <properties>
    <test.groups>unit</test.groups>
  </properties>
</profile>
<profile>
  <id>component-tests</id>
  <properties>
    <test.groups>component</test.groups>
  </properties>
</profile>
<profile>
  <id>integration-tests</id>
  <properties>
    <test.groups>integration</test.groups>
  </properties>
</profile>
<profile>
  <id>acceptance-tests</id>
  <properties>
    <test.groups>acceptance</test.groups>
  </properties>
</profile>
<profile>
  <id>performance-tests</id>
  <properties>
    <test.groups>performance</test.groups>
  </properties>
</profile>
<profile>
  <id>fast-tests</id>
  <properties>
    <test.groups>unit | component</test.groups>
  </properties>
</profile>
EOF

echo "Test standardization scripts and files created."
echo "Next steps:"
echo "1. Review the created base test classes"
echo "2. Integrate the test profiles into your main pom.xml"
echo "3. Use the 'rin-test' script to run tests with the new structure"
echo "4. Gradually migrate existing tests to the new organization"