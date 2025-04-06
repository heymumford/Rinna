#!/bin/bash
set -e

# Find Java 21 if not explicitly set
if [ -z "$JAVA_HOME" ]; then
  # Try common locations
  if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  elif [ -d "/usr/lib/jvm/temurin-21-jdk" ]; then
    export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk
  elif [ -d "/opt/jdk-21" ]; then
    export JAVA_HOME=/opt/jdk-21
  else
    echo "Warning: Java 21 not found in common locations. Using system default."
  fi
  
  # Add Java to PATH if found
  if [ -n "$JAVA_HOME" ]; then
    export PATH=$JAVA_HOME/bin:$PATH
  fi
fi

# Print environment info
echo "Environment Information:"
echo "======================="
echo "Using Java: $(java -version 2>&1 | head -1)"
echo "Using Go: $(go version 2>&1)"
echo "Using Python: $(python --version 2>&1)"
echo "Working directory: $(pwd)"
echo "======================="

# Determine if running in CI
CI_MODE=${CI:-false}
if [ "$CI_MODE" = "true" ]; then
  echo "Running in CI mode with minimal tests..."
  TEST_SCOPE="minimal"
else
  # Default to full tests in local development
  TEST_SCOPE=${1:-"full"}
fi

# Navigate to project root
cd "$(dirname "$0")/.."
PROJECT_ROOT=$(pwd)

# Build and test Java modules
echo "Building Java core..."
if [ "$TEST_SCOPE" = "minimal" ]; then
  mvn -B clean compile -pl rinna-core
  echo "Running minimal Java tests..."
  cd rinna-core
  mvn -B test -Dtest=DocumentServiceTest
  cd $PROJECT_ROOT
else
  echo "Running full Java tests..."
  mvn clean install -pl rinna-core
fi

# Build and test Go API
echo "Building Go API..."
cd api
if [ -f "./build.sh" ]; then
  ./build.sh
else
  echo "Build script not found, using go build..."
  go build -v ./cmd/rinnasrv
fi

if [ "$TEST_SCOPE" = "minimal" ]; then
  echo "Running minimal Go tests..."
  go test -v ./pkg/health
else
  echo "Running all Go tests..."
  go test -v ./...
fi

cd $PROJECT_ROOT

# Run Python quality checks and tests
echo "Running Python quality checks and tests..."
# First run quality checks
if [ -f "./bin/python-quality" ]; then
  if [ "$TEST_SCOPE" = "minimal" ]; then
    # Run only linting in minimal mode
    ./bin/python-quality lint
  else
    # Run all checks in full mode
    ./bin/python-quality all
  fi
else
  echo "Python quality script not found, skipping quality checks..."
fi

# Then run Python unit tests
if [ -d "./python" ]; then
  echo "Running Python tests..."
  if [ "$TEST_SCOPE" = "minimal" ]; then
    # Run minimal Python tests in CI
    python -m pytest python/tests/test_config_manager.py -v
  else
    # Run all Python tests
    python -m pytest python/tests -v
    # Also run any additional test files
    if [ -f "./bin/test_c4_diagrams.py" ]; then
      python -m unittest bin/test_c4_diagrams.py
    fi
  fi
else
  echo "Python directory not found, skipping Python tests..."
fi

echo "All tests completed successfully!"