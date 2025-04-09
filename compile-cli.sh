#!/bin/bash
# Compile the Rinna CLI code for testing

# Create directories
mkdir -p rinna-cli/target/classes

# Find Java files in the CLI project
SRC_DIR="rinna-cli/src/main/java"
TARGET_DIR="rinna-cli/target/classes"

echo "Compiling Rinna CLI code..."
echo "Source directory: $SRC_DIR"
echo "Target directory: $TARGET_DIR"

# Compile Java files with all dependencies
MAVEN_REPO="$HOME/.m2/repository"
PICOCLI_JAR=$(find $MAVEN_REPO -name "picocli-*.jar" -type f | head -1)
JACKSON_JAR=$(find $MAVEN_REPO -name "jackson-databind-*.jar" -type f | head -1)
JACKSON_CORE_JAR=$(find $MAVEN_REPO -name "jackson-core-*.jar" -type f | head -1)
JACKSON_ANNOTATIONS_JAR=$(find $MAVEN_REPO -name "jackson-annotations-*.jar" -type f | head -1)
OKHTTP_JAR=$(find $MAVEN_REPO -name "okhttp-*.jar" -type f | head -1)
CONFIG_JAR=$(find $MAVEN_REPO -name "config-*.jar" -type f | head -1)
CORE_JAR="../rinna-core/target/rinna-core-1.8.1.jar"

# Create classpath
CLASSPATH="$PICOCLI_JAR:$JACKSON_JAR:$JACKSON_CORE_JAR:$JACKSON_ANNOTATIONS_JAR:$OKHTTP_JAR:$CONFIG_JAR:$CORE_JAR:$SRC_DIR:$TARGET_DIR"

# Output classpath for debugging
echo "Using classpath: $CLASSPATH"

# Compile Java files with dependencies
find $SRC_DIR -name "*.java" -print | xargs javac -d $TARGET_DIR -cp "$CLASSPATH" -verbose

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
else
    echo "Compilation failed."
    exit 1
fi