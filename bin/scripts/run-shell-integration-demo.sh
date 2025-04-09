#!/bin/bash

# Script to run the Shell Integration Layer demo for Rinna PUI
# This demonstrates the shell integration layer for PUI operations

# Set up environment
source activate-java.sh 2>/dev/null || echo "No Java environment activation found, using system Java"

# Ensure we're in the project directory
cd "$(dirname "$0")" || exit 1

# Compile the demo
echo "Compiling Shell Integration demo..."
mvn compile -q

# Run the demo
echo "Starting Shell Integration Layer demo..."
echo "This demo shows how PUI components integrate with the shell environment."
echo "The ShellIntegrationLayer provides a unified API for all shell-related functionality:"
echo "- Command execution and tracking"
echo "- Environment variable management"
echo "- Shell script generation"
echo "- Command completion and history"
echo "- Process management"
echo "- Asynchronous command execution"
echo ""
echo "In the demo, you can:"
echo "- Use the shell console to execute commands"
echo "- Use direct shell commands with ! prefix"
echo "- See command history being tracked"
echo "- Generate shell scripts from executed commands"
echo "- See environment variables being managed"
echo ""
echo "Press Enter to continue..."
read -r

# Use Java to run the demo
java -cp target/classes org.rinna.pui.examples.ShellIntegrationDemo

# Exit with the status of the Java command
exit $?