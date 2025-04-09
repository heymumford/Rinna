#!/bin/bash

# Script to run the Command Mirror demo for Rinna PUI
# This demonstrates SUSBS-compliant command mirroring between PUI operations and shell commands

# Set up environment
source activate-java.sh 2>/dev/null || echo "No Java environment activation found, using system Java"

# Ensure we're in the project directory
cd "$(dirname "$0")" || exit 1

# Compile the demo
echo "Compiling Command Mirror demo..."
mvn compile -q

# Run the demo
echo "Starting Command Mirror Demo..."
echo "This demo shows SUSBS-compliant command mirroring between PUI operations and shell commands."
echo ""
echo "The demo provides bidirectional conversion between:"
echo "1. PUI Operations (e.g., workitem.list) → Shell Commands (e.g., rin list --state=IN_PROGRESS)"
echo "2. Shell Commands → PUI Operations with parameters"
echo ""
echo "Key features demonstrated:"
echo "- Comprehensive mapping of all PUI operations to shell commands"
echo "- Parameter extraction and substitution"
echo "- Regex-based command parsing"
echo "- Bidirectional conversion"
echo ""
echo "In the demo, you can:"
echo "- Select from a list of supported operations"
echo "- Convert PUI operations to shell commands"
echo "- Convert shell commands to PUI operations"
echo "- Edit operations and parameters for testing"
echo ""
echo "Press Enter to continue..."
read -r

# Use Java to run the demo
java -cp target/classes org.rinna.pui.examples.CommandMirrorDemo

# Exit with the status of the Java command
exit $?