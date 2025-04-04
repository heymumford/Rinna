#!/bin/bash
# Script to run BDD tests efficiently
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -e

# Configuration
CORE_MODULE="rinna-core"
FEATURE_DIR="src/test/resources/features"
TARGET_DIR="target/test-classes"

# Parse command line arguments
TEST_SCOPE="all"
FAIL_FAST=false
DEBUG=false

while [[ $# -gt 0 ]]; do
  case $1 in
    workflow|release|input)
      TEST_SCOPE="$1"
      shift
      ;;
    --fail-fast)
      FAIL_FAST=true
      shift
      ;;
    --debug)
      DEBUG=true
      shift
      ;;
    --help)
      echo "Usage: $(basename $0) [workflow|release|input] [--fail-fast] [--debug]"
      echo
      echo "Options:"
      echo "  workflow     Run only workflow tests"
      echo "  release      Run only release tests"
      echo "  input        Run only input interface tests"
      echo "  --fail-fast  Stop at first failure"
      echo "  --debug      Run with debug output enabled"
      echo 
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

# Ensure test resources are properly copied
echo "Setting up test resources..."
mkdir -p ${CORE_MODULE}/${TARGET_DIR}/features
cp -r ${CORE_MODULE}/src/test/resources/* ${CORE_MODULE}/${TARGET_DIR}/

# Configure Maven command
MVN_CMD="./mvnw"
MVN_OPTS=""

if $FAIL_FAST; then
  MVN_OPTS="${MVN_OPTS} -Dsurefire.skipAfterFailureCount=1"
fi

if $DEBUG; then
  MVN_OPTS="${MVN_OPTS} -Dcucumber.execution.dry-run=false -Dcucumber.execution.debug=true"
fi

# Run the BDD tests based on scope
case $TEST_SCOPE in
  workflow)
    echo "Running workflow BDD tests..."
    ${MVN_CMD} ${MVN_OPTS} -Dtest=CucumberRunner test
    ;;
  release)
    echo "Running release BDD tests..."
    ${MVN_CMD} ${MVN_OPTS} -Dtest=ReleaseRunner test
    ;;
  input)
    echo "Running input interface BDD tests..."
    ${MVN_CMD} ${MVN_OPTS} -Dtest=InputInterfaceRunner test
    ;;
  all)
    echo "Running all BDD tests..."
    ${MVN_CMD} ${MVN_OPTS} -Dtest=CucumberRunner,ReleaseRunner,InputInterfaceRunner test
    ;;
esac

# Print summary of test results if available
if [ -d "${CORE_MODULE}/target/surefire-reports" ]; then
  echo "========== BDD TEST SUMMARY =========="
  TOTAL_FEATURES=$(grep -r "Feature:" ${CORE_MODULE}/${FEATURE_DIR} | wc -l)
  TOTAL_SCENARIOS=$(grep -r "Scenario:" ${CORE_MODULE}/${FEATURE_DIR} | wc -l)
  FAILED_TESTS=$(find ${CORE_MODULE}/target/surefire-reports -name "TEST-*.xml" -exec grep -l "<failure" {} \; | wc -l)
  PASSED_TESTS=$((TOTAL_SCENARIOS - FAILED_TESTS))
  
  echo "Features: ${TOTAL_FEATURES}"
  echo "Scenarios: ${TOTAL_SCENARIOS}"
  echo "Passed: ${PASSED_TESTS}"
  echo "Failed: ${FAILED_TESTS}"
  echo "======================================="
fi

exit 0