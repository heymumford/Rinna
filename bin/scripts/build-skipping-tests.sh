#!/bin/bash
# Script to build the project but skip all test compilation and tests

# Install the parent POM first
cd /home/emumford/NativeLinuxProjects/Rinna
mvn -N install -DskipTests
echo "Parent POM installed successfully"

# Build rinna-core
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-core
mvn install -DskipTests -Dexec.skip=true -P skip-quality -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true
echo "rinna-core built successfully"

# Build rinna-cli
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-cli
mvn install -DskipTests -Dexec.skip=true -P skip-quality -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true
echo "rinna-cli built successfully"

echo "All modules built successfully! Skipped all tests."
cd /home/emumford/NativeLinuxProjects/Rinna