#!/bin/bash

set -e

echo "Running CLI Tests directly with Java..."

cd "$(dirname "$0")/.."
PROJECT_ROOT=$(pwd)

# Create test class list
TEST_CLASSES=(
  "org.rinna.cli.unit.ViewCommandTest"
  "org.rinna.cli.component.CommandExecutionTest"
  "org.rinna.cli.integration.CliServiceIntegrationTest"
  "org.rinna.cli.acceptance.WorkflowAcceptanceTest"
  "org.rinna.cli.performance.CliPerformanceTest"
)

# Generate classpath
CLI_JAR="$PROJECT_ROOT/rinna-cli/target/rinna-cli.jar"
CORE_JAR="$PROJECT_ROOT/rinna-core/target/rinna-core-1.6.3.jar"
JUNIT_CP="$(find "$HOME/.m2/repository" -name "*junit-jupiter-*.jar" -type f | tr '\n' ':')$(find "$HOME/.m2/repository" -name "*apiguardian-*.jar" -type f | tr '\n' ':')$(find "$HOME/.m2/repository" -name "*opentest4j-*.jar" -type f | tr '\n' ':')$(find "$HOME/.m2/repository" -name "*junit-platform-*.jar" -type f | tr '\n' ':')"
MOCKITO_CP="$(find "$HOME/.m2/repository" -name "*mockito-*.jar" -type f | tr '\n' ':')"
PICOCLI_CP="$(find "$HOME/.m2/repository" -name "*picocli-*.jar" -type f | tr '\n' ':')"

CLASSPATH="$PROJECT_ROOT/rinna-cli/target/classes:$PROJECT_ROOT/rinna-cli/target/test-classes:$CORE_JAR:$CLI_JAR:$JUNIT_CP:$MOCKITO_CP:$PICOCLI_CP"

# Run each test class separately
for TEST_CLASS in "${TEST_CLASSES[@]}"; do
  echo "Running test: $TEST_CLASS"
  
  # Execute test with JUnit directly
  java -cp "$CLASSPATH" org.junit.platform.console.ConsoleLauncher --select-class "$TEST_CLASS" || echo "Test failed: $TEST_CLASS"
done

echo "CLI Tests execution completed!"