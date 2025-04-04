#!/bin/bash
# Script to run BDD tests

# Copy feature files to target directory
mkdir -p rinna-core/target/test-classes/features
cp -r rinna-core/src/test/resources/* rinna-core/target/test-classes/

# Run the BDD tests
if [ "$1" == "workflow" ]; then
  echo "Running workflow BDD tests..."
  ./mvnw -Dtest=CucumberRunner test
elif [ "$1" == "release" ]; then
  echo "Running release BDD tests..."
  ./mvnw -Dtest=ReleaseRunner test
else
  echo "Running all BDD tests..."
  ./mvnw -Dtest=CucumberRunner,ReleaseRunner test
fi