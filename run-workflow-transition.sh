#!/bin/bash
# Script to run the Workflow Transition Demo

# Compile the PUI classes if needed
echo "Compiling PUI classes..."
mvn compile || { echo "Compilation failed"; exit 1; }

# Run the Workflow Transition Demo
echo "Starting Workflow Transition Demo..."
mvn exec:java -Dexec.mainClass="org.rinna.pui.examples.WorkflowTransitionDemo" -Dexec.classpathScope=compile

echo "Demo completed"