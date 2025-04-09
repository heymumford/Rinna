#!/bin/bash
# Script to run the Context-Aware Search Interface Demo

# Compile the PUI classes if needed
echo "Compiling PUI classes..."
mvn compile || { echo "Compilation failed"; exit 1; }

# Run the Search Interface Demo
echo "Starting Context-Aware Search Interface Demo..."
mvn exec:java -Dexec.mainClass="org.rinna.pui.examples.SearchInterfaceDemo" -Dexec.classpathScope=compile

echo "Demo completed"