#!/bin/bash
# Copy feature files to the target directory for testing
mkdir -p rinna-core/target/test-classes/features
cp rinna-core/src/test/resources/features/* rinna-core/target/test-classes/features/
cp rinna-core/src/test/resources/cucumber.properties rinna-core/target/test-classes/
cp rinna-core/src/test/resources/junit-platform.properties rinna-core/target/test-classes/
echo "Feature files copied to target directory"