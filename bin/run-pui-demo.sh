#!/bin/bash

# Script to run PUI demos

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT" || exit 1

# ANSI color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Rinna Pragmatic User Interface (PUI) Demo ===${NC}"
echo

if [ $# -eq 0 ]; then
    echo -e "${YELLOW}Available demos:${NC}"
    echo "  1. simple - Simple PUI demo with basic layout"
    echo "  2. workitem - Work item management demo"
    echo
    echo "Usage: $0 <demo-name>"
    exit 0
fi

DEMO=$1

# Ensure the project is compiled
echo -e "${BLUE}Compiling project...${NC}"
mvn compile -q

case $DEMO in
    simple)
        echo -e "${GREEN}Running Simple PUI Demo...${NC}"
        mvn exec:java -Dexec.mainClass="org.rinna.pui.examples.SimplePUIDemo" -q
        ;;
    workitem)
        echo -e "${GREEN}Running Work Item PUI Demo...${NC}"
        mvn exec:java -Dexec.mainClass="org.rinna.pui.examples.WorkItemViewDemo" -q
        ;;
    *)
        echo -e "${YELLOW}Unknown demo: $DEMO${NC}"
        echo "Available demos: simple, workitem"
        exit 1
        ;;
esac

echo -e "${GREEN}Demo completed.${NC}"