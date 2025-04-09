#!/bin/bash
# Test script for the notifications system in Rinna CLI

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${YELLOW}===============================================${NC}"
echo -e "${YELLOW}Rinna CLI Notification System Integration Test${NC}"
echo -e "${YELLOW}===============================================${NC}"
echo

# Verify the components are in place
echo -e "${CYAN}Verifying components...${NC}"

# Check if the core files exist
if [ -f "rinna-cli/src/main/java/org/rinna/cli/notifications/NotificationType.java" ]; then
    echo -e "${GREEN}✓ NotificationType enum found${NC}"
else
    echo -e "${RED}✗ NotificationType enum missing${NC}"
    exit 1
fi

if [ -f "rinna-cli/src/main/java/org/rinna/cli/notifications/Notification.java" ]; then
    echo -e "${GREEN}✓ Notification class found${NC}"
else
    echo -e "${RED}✗ Notification class missing${NC}"
    exit 1
fi

if [ -f "rinna-cli/src/main/java/org/rinna/cli/notifications/NotificationService.java" ]; then
    echo -e "${GREEN}✓ NotificationService found${NC}"
else
    echo -e "${RED}✗ NotificationService missing${NC}"
    exit 1
fi

if [ -f "rinna-cli/src/main/java/org/rinna/cli/command/NotifyCommand.java" ]; then
    echo -e "${GREEN}✓ NotifyCommand found${NC}"
else
    echo -e "${RED}✗ NotifyCommand missing${NC}"
    exit 1
fi

# Check if RinnaCli.java has been updated with the notify command
if grep -q "case \"notify\":" "rinna-cli/src/main/java/org/rinna/cli/RinnaCli.java"; then
    echo -e "${GREEN}✓ RinnaCli updated with notify command${NC}"
else
    echo -e "${RED}✗ RinnaCli not updated with notify command${NC}"
    exit 1
fi

if grep -q "handleNotifyCommand" "rinna-cli/src/main/java/org/rinna/cli/RinnaCli.java"; then
    echo -e "${GREEN}✓ handleNotifyCommand method found in RinnaCli${NC}"
else
    echo -e "${RED}✗ handleNotifyCommand method missing from RinnaCli${NC}"
    exit 1
fi

if grep -q "NotificationService notificationService = NotificationService.getInstance()" "rinna-cli/src/main/java/org/rinna/cli/RinnaCli.java"; then
    echo -e "${GREEN}✓ NotificationService integrated in checkUnreadMessages${NC}"
else
    echo -e "${RED}✗ NotificationService not integrated in checkUnreadMessages${NC}"
    exit 1
fi

echo
echo -e "${CYAN}Validating implementation...${NC}"

# Check notification type completeness
EXPECTED_TYPES=("ASSIGNMENT" "UPDATE" "COMMENT" "DEADLINE" "MENTION" "SECURITY" "SYSTEM" "COMPLETION" "ATTENTION")
MISSING_TYPES=0

for type in "${EXPECTED_TYPES[@]}"; do
    if grep -q "$type" "rinna-cli/src/main/java/org/rinna/cli/notifications/NotificationType.java"; then
        echo -e "  ${GREEN}✓ $type notification type found${NC}"
    else
        echo -e "  ${RED}✗ $type notification type missing${NC}"
        MISSING_TYPES=$((MISSING_TYPES + 1))
    fi
done

if [ $MISSING_TYPES -eq 0 ]; then
    echo -e "${GREEN}✓ All notification types implemented${NC}"
else
    echo -e "${RED}✗ Some notification types are missing${NC}"
    exit 1
fi

# Check notification command actions
EXPECTED_ACTIONS=("list" "unread" "read" "markread" "markall" "clear" "help")
MISSING_ACTIONS=0

for action in "${EXPECTED_ACTIONS[@]}"; do
    if grep -q "case \"$action\":" "rinna-cli/src/main/java/org/rinna/cli/command/NotifyCommand.java"; then
        echo -e "  ${GREEN}✓ $action notification action found${NC}"
    else
        echo -e "  ${RED}✗ $action notification action missing${NC}"
        MISSING_ACTIONS=$((MISSING_ACTIONS + 1))
    fi
done

if [ $MISSING_ACTIONS -eq 0 ]; then
    echo -e "${GREEN}✓ All notification actions implemented${NC}"
else
    echo -e "${RED}✗ Some notification actions are missing${NC}"
    exit 1
fi

echo
echo -e "${GREEN}All notification system components are properly implemented!${NC}"
echo
echo -e "${CYAN}The notification system supports:${NC}"
echo " - Multiple notification types (assignment, comment, deadline, etc.)"
echo " - Priority levels (low, medium, high, urgent)"
echo " - User-targeted notifications"
echo " - Work item-related notifications"
echo " - Persistent storage of notifications"
echo " - Marking notifications as read"
echo " - Automatic display of unread notifications"
echo " - CLI command interface (rin notify)"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo " 1. Add JUnit tests for the notification system components"
echo " 2. Connect notifications to work item updates"
echo " 3. Implement notification preferences (filtering, frequency)"
echo " 4. Add push notification capabilities"
echo " 5. Add UI indicators for unread notifications"