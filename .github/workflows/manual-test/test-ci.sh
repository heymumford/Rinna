#\!/bin/bash
# Manual test script to simulate CI workflow

set -e
echo "Starting manual CI test..."

# Test directory is the current repo
cd /home/emumford/NativeLinuxProjects/Rinna

echo "Setting up JDK..."
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "Checking Java version..."
java -version

echo "Checking Go version..."
go version

echo "Running Java compilation..."
mvn -B -DskipTests compile

echo "Running Java test (RinnaTest only)..."
mvn -B test -Dtest=RinnaTest

echo "Running Go simple test..."
cd api && go test ./test/simple -v

echo "All checks passed\!"
