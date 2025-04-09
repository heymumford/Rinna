#!/bin/bash
# Script to run the Project Dashboard Demo

# Compile the PUI classes if needed
echo "Compiling PUI classes..."
mvn compile || { echo "Compilation failed"; exit 1; }

# Run the Project Dashboard Demo
echo "Starting Project Dashboard Demo..."
mvn exec:java -Dexec.mainClass="org.rinna.pui.examples.ProjectDashboardDemo" -Dexec.classpathScope=compile

echo "Demo completed"