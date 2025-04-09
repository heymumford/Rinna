#!/bin/bash
# Script to run the Dependency Graph Demo

# Compile the PUI classes if needed
echo "Compiling PUI classes..."
mvn compile || { echo "Compilation failed"; exit 1; }

# Run the Dependency Graph Demo
echo "Starting Dependency Graph Demo..."
mvn exec:java -Dexec.mainClass="org.rinna.pui.examples.DependencyGraphDemo" -Dexec.classpathScope=compile

echo "Demo completed"