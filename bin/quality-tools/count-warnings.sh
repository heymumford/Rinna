#!/usr/bin/env bash
#
# count-warnings.sh - Count checkstyle warnings
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Run checkstyle and count warnings
echo -e "${BLUE}Running checkstyle to count warnings...${NC}"
WARNINGS=$(/home/emumford/NativeLinuxProjects/Rinna/bin/quality-check checkstyle 2>&1 | grep -c '\[WARN\]')

echo -e "${YELLOW}Total checkstyle warnings: ${WARNINGS}${NC}"

# Count by type
echo -e "\n${BLUE}Breaking down warnings by category:${NC}"
LINELENWARNS=$(/home/emumford/NativeLinuxProjects/Rinna/bin/quality-check checkstyle 2>&1 | grep -c '\[LineLength\]')
echo -e "Line length warnings: ${GREEN}${LINELENWARNS}${NC}"

IMPORTWARNS=$(/home/emumford/NativeLinuxProjects/Rinna/bin/quality-check checkstyle 2>&1 | grep -c 'ImportOrder\|UnusedImports')
echo -e "Import ordering/unused warnings: ${GREEN}${IMPORTWARNS}${NC}"

UTILITYWARNS=$(/home/emumford/NativeLinuxProjects/Rinna/bin/quality-check checkstyle 2>&1 | grep -c 'HideUtilityClassConstructor\|FinalClass')
echo -e "Utility class warnings: ${GREEN}${UTILITYWARNS}${NC}"

NAMINGWARNS=$(/home/emumford/NativeLinuxProjects/Rinna/bin/quality-check checkstyle 2>&1 | grep -c 'ConstantName\|MemberName\|MethodName')
echo -e "Naming convention warnings: ${GREEN}${NAMINGWARNS}${NC}"

JAVADOCWARNS=$(/home/emumford/NativeLinuxProjects/Rinna/bin/quality-check checkstyle 2>&1 | grep -c 'JavadocMethod\|JavadocVariable\|JavadocStyle')
echo -e "Javadoc warnings: ${GREEN}${JAVADOCWARNS}${NC}"

WHITESPACEWARNS=$(/home/emumford/NativeLinuxProjects/Rinna/bin/quality-check checkstyle 2>&1 | grep -c 'Whitespace\|NoWhitespace\|OperatorWrap')
echo -e "Whitespace warnings: ${GREEN}${WHITESPACEWARNS}${NC}"

OTHERWARNS=$(($WARNINGS - $LINELENWARNS - $IMPORTWARNS - $UTILITYWARNS - $NAMINGWARNS - $JAVADOCWARNS - $WHITESPACEWARNS))
echo -e "Other warnings: ${GREEN}${OTHERWARNS}${NC}"