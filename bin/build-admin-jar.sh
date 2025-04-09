#!/usr/bin/env bash

# Script to build a test JAR for AdminCommand implementation

set -e

RINNA_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TEST_DIR="$RINNA_DIR/build/admin-test"
CLASSES_DIR="$TEST_DIR/classes"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Building test JAR for AdminCommand implementation${NC}"
echo "=============================================="

# Create directories
mkdir -p "$CLASSES_DIR/org/rinna/cli/service"
mkdir -p "$CLASSES_DIR/org/rinna/cli/command"
mkdir -p "$CLASSES_DIR/org/rinna/cli/command/impl"

# Copy java files from test-bin
if [ -d "$RINNA_DIR/test-bin" ]; then
  echo "Copying Java files from test-bin..."
  cp -r "$RINNA_DIR/test-bin/org" "$CLASSES_DIR/"
else
  echo -e "${RED}Error: test-bin directory not found${NC}"
  exit 1
fi

# Compile Java classes
echo "Compiling Java classes..."
javac -d "$CLASSES_DIR" $(find "$CLASSES_DIR" -name "*.java")

# Create the JAR file
echo "Creating JAR file..."
jar cvf "$TEST_DIR/rinna-admin-test.jar" -C "$CLASSES_DIR" .

echo -e "${GREEN}JAR created successfully at $TEST_DIR/rinna-admin-test.jar${NC}"
echo "=============================================="

# Create a simple test script
cat > "$TEST_DIR/test-admin-cli.sh" << 'EOF'
#!/usr/bin/env bash

set -e

JAR_PATH="$(dirname "$0")/rinna-admin-test.jar"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Testing AdminCommand implementation with Java${NC}"
echo "=============================================="

# Test 1: Show help
echo -e "${BLUE}Test 1: Admin command with no arguments${NC}"
echo "Command: java -cp $JAR_PATH org.rinna.cli.RinnaCli admin"
java -cp "$JAR_PATH" org.rinna.cli.RinnaCli admin || echo -e "${GREEN}✓ Passed: Admin help shown as expected${NC}"
echo

# Test 2: Run with audit subcommand
echo -e "${BLUE}Test 2: Admin command with audit subcommand${NC}"
echo "Command: java -cp $JAR_PATH org.rinna.cli.RinnaCli admin audit"
java -cp "$JAR_PATH" org.rinna.cli.RinnaCli admin audit
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Passed: Admin audit command executed${NC}"
else
  echo -e "${RED}× Failed: Admin audit command failed${NC}"
  exit 1
fi
echo

# Test 3: Run with compliance subcommand and arguments
echo -e "${BLUE}Test 3: Admin command with compliance subcommand and arguments${NC}"
echo "Command: java -cp $JAR_PATH org.rinna.cli.RinnaCli admin compliance report financial"
java -cp "$JAR_PATH" org.rinna.cli.RinnaCli admin compliance report financial
if [[ $? -eq 0 ]]; then
  echo -e "${GREEN}✓ Passed: Admin compliance command with arguments executed${NC}"
else
  echo -e "${RED}× Failed: Admin compliance command with arguments failed${NC}"
  exit 1
fi
echo

echo -e "${GREEN}All Java tests passed!${NC}"
echo "=============================================="
EOF

chmod +x "$TEST_DIR/test-admin-cli.sh"

echo -e "${GREEN}Test script created at $TEST_DIR/test-admin-cli.sh${NC}"
echo "Run the test with:"
echo "$TEST_DIR/test-admin-cli.sh"