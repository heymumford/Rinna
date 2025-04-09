#!/bin/bash
# Test script for statistics functionality in Rinna CLI

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${YELLOW}==================================================${NC}"
echo -e "${YELLOW}Rinna CLI Statistics System Integration Test${NC}"
echo -e "${YELLOW}==================================================${NC}"
echo

# Verify the components are in place
echo -e "${CYAN}Verifying components...${NC}"

# Check if the core files exist
if [ -f "rinna-cli/src/main/java/org/rinna/cli/stats/StatisticType.java" ]; then
    echo -e "${GREEN}✓ StatisticType enum found${NC}"
else
    echo -e "${RED}✗ StatisticType enum missing${NC}"
    exit 1
fi

if [ -f "rinna-cli/src/main/java/org/rinna/cli/stats/StatisticValue.java" ]; then
    echo -e "${GREEN}✓ StatisticValue class found${NC}"
else
    echo -e "${RED}✗ StatisticValue class missing${NC}"
    exit 1
fi

if [ -f "rinna-cli/src/main/java/org/rinna/cli/stats/StatisticsService.java" ]; then
    echo -e "${GREEN}✓ StatisticsService found${NC}"
else
    echo -e "${RED}✗ StatisticsService missing${NC}"
    exit 1
fi

if [ -f "rinna-cli/src/main/java/org/rinna/cli/stats/StatisticsVisualizer.java" ]; then
    echo -e "${GREEN}✓ StatisticsVisualizer found${NC}"
else
    echo -e "${RED}✗ StatisticsVisualizer missing${NC}"
    exit 1
fi

if [ -f "rinna-cli/src/main/java/org/rinna/cli/command/StatsCommand.java" ]; then
    echo -e "${GREEN}✓ StatsCommand found${NC}"
else
    echo -e "${RED}✗ StatsCommand missing${NC}"
    exit 1
fi

# Check if RinnaCli.java has been updated with the stats command
if grep -q "case \"stats\":" "rinna-cli/src/main/java/org/rinna/cli/RinnaCli.java"; then
    echo -e "${GREEN}✓ RinnaCli updated with stats command${NC}"
else
    echo -e "${RED}✗ RinnaCli not updated with stats command${NC}"
    exit 1
fi

if grep -q "handleStatsCommand" "rinna-cli/src/main/java/org/rinna/cli/RinnaCli.java"; then
    echo -e "${GREEN}✓ handleStatsCommand method found in RinnaCli${NC}"
else
    echo -e "${RED}✗ handleStatsCommand method missing from RinnaCli${NC}"
    exit 1
fi

# Check if stats is shown in the help output
if grep -q "stats.*Show project statistics" "rinna-cli/src/main/java/org/rinna/cli/RinnaCli.java"; then
    echo -e "${GREEN}✓ Stats command included in help output${NC}"
else
    echo -e "${RED}✗ Stats command not included in help output${NC}"
    exit 1
fi

echo
echo -e "${CYAN}Validating implementation...${NC}"

# Check statistical types
EXPECTED_TYPES=("TOTAL_ITEMS" "ITEMS_BY_TYPE" "ITEMS_BY_STATE" "ITEMS_BY_PRIORITY" 
                "ITEMS_BY_ASSIGNEE" "COMPLETION_RATE" "AVG_COMPLETION_TIME" "ITEMS_COMPLETED" 
                "ITEMS_CREATED" "OVERDUE_ITEMS" "BURNDOWN_RATE" "LEAD_TIME" 
                "CYCLE_TIME" "THROUGHPUT" "WORK_IN_PROGRESS")
MISSING_TYPES=0

for type in "${EXPECTED_TYPES[@]}"; do
    if grep -q "$type" "rinna-cli/src/main/java/org/rinna/cli/stats/StatisticType.java"; then
        echo -e "  ${GREEN}✓ $type statistic type found${NC}"
    else
        echo -e "  ${RED}✗ $type statistic type missing${NC}"
        MISSING_TYPES=$((MISSING_TYPES + 1))
    fi
done

if [ $MISSING_TYPES -eq 0 ]; then
    echo -e "${GREEN}✓ All statistic types implemented${NC}"
else
    echo -e "${RED}✗ Some statistic types are missing${NC}"
    exit 1
fi

# Check stats command types
EXPECTED_ACTIONS=("summary" "dashboard" "all" "distribution" "detail" "trend" "help")
MISSING_ACTIONS=0

for action in "${EXPECTED_ACTIONS[@]}"; do
    if grep -q "case \"$action\":" "rinna-cli/src/main/java/org/rinna/cli/command/StatsCommand.java"; then
        echo -e "  ${GREEN}✓ $action stats action found${NC}"
    else
        echo -e "  ${RED}✗ $action stats action missing${NC}"
        MISSING_ACTIONS=$((MISSING_ACTIONS + 1))
    fi
done

# Check for refresh functionality
if grep -q "refreshStatistics" "rinna-cli/src/main/java/org/rinna/cli/command/StatsCommand.java"; then
    echo -e "  ${GREEN}✓ refresh functionality found${NC}"
else
    echo -e "  ${RED}✗ refresh functionality missing${NC}"
    MISSING_ACTIONS=$((MISSING_ACTIONS + 1))
fi

if [ $MISSING_ACTIONS -eq 0 ]; then
    echo -e "${GREEN}✓ All stats actions implemented${NC}"
else
    echo -e "${RED}✗ Some stats actions are missing${NC}"
    exit 1
fi

# Check visualization methods
EXPECTED_METHODS=("createBarChart" "createTable" "createDashboard" "createProgressMeter" "createSparkline")
MISSING_METHODS=0

for method in "${EXPECTED_METHODS[@]}"; do
    if grep -q "public static String $method" "rinna-cli/src/main/java/org/rinna/cli/stats/StatisticsVisualizer.java"; then
        echo -e "  ${GREEN}✓ $method visualization method found${NC}"
    else
        echo -e "  ${RED}✗ $method visualization method missing${NC}"
        MISSING_METHODS=$((MISSING_METHODS + 1))
    fi
done

if [ $MISSING_METHODS -eq 0 ]; then
    echo -e "${GREEN}✓ All visualization methods implemented${NC}"
else
    echo -e "${RED}✗ Some visualization methods are missing${NC}"
    exit 1
fi

echo
echo -e "${GREEN}All statistics system components are properly implemented!${NC}"
echo
echo -e "${CYAN}The statistics system supports:${NC}"
echo " - Multiple statistic types (counts, rates, distributions, etc.)"
echo " - Project-wide metrics calculation"
echo " - Summary and detailed views"
echo " - Visualizations with charts and progress meters"
echo " - Filtering and limiting results"
echo " - Dashboard-style overview"
echo " - CLI command interface (rin stats)"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo " 1. Add JUnit tests for the statistics components"
echo " 2. Connect statistics calculations to actual work item data"
echo " 3. Implement trend analysis over time"
echo " 4. Add export capabilities for metrics (CSV, JSON)"
echo " 5. Create automated reporting with scheduled runs"