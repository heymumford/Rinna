#!/bin/bash
# Source this file to activate the project's Java environment
# Usage: source activate-java.sh

# Load environment variables
if [ -f .env ]; then
  while read line; do
    if [[ ! $line =~ ^# && $line != "" ]]; then
      export "$line"
    fi
  done < .env
  
  echo "Activated Java environment for this project"
  echo "JAVA_HOME: $JAVA_HOME"
  java -version
else
  echo "Error: .env file not found"
  return 1
fi
