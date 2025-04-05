#!/bin/bash

# Script to start the Rinna Java API server
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Set default port
PORT=8081
if [ "$1" != "" ]; then
  PORT=$1
fi

# Load Java environment if activate-java.sh exists
if [ -f "$PROJECT_ROOT/activate-java.sh" ]; then
  source "$PROJECT_ROOT/activate-java.sh"
fi

# Ensure Java 21 is in use
if command -v java-switch &> /dev/null; then
  java-switch 21
fi

echo "Building Rinna Core..."
cd "$PROJECT_ROOT" && mvn -pl rinna-core -am clean compile

echo "Starting Rinna Java API server on port $PORT..."

# Get the classpath directly from Maven
CLASSPATH=$(mvn -pl rinna-core dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)
CLASSPATH="$PROJECT_ROOT/rinna-core/target/classes:$CLASSPATH"

# Run the Java application directly with the --enable-preview flag
cd "$PROJECT_ROOT" && java --enable-preview -cp "$CLASSPATH" org.rinna.Rinna "$PORT"