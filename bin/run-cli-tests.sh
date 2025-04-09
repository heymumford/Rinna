#!/bin/bash

set -e

echo "Running CLI Tests directly..."

# Use the maven-based approach instead of direct compilation
# This ensures all dependencies are properly resolved
mvn test -pl rinna-cli

# Report success if maven completed successfully
if [ $? -eq 0 ]; then
    echo "CLI tests completed successfully!"
else
    echo "CLI tests failed!"
    exit 1
fi
