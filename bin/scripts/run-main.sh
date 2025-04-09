#!/bin/bash

# Compile the Main class and its dependencies
echo "Compiling Main class..."
mkdir -p target/classes
javac -d target/classes src/main/java/org/rinna/Main.java

# Run the Main class with the specified arguments
echo "Running Main class..."
java -cp target/classes org.rinna.Main "$@"