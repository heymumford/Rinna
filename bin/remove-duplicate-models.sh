#!/usr/bin/env bash
#
# remove-duplicate-models.sh - Remove duplicate model files
#
# PURPOSE: Remove duplicate model files that exist in both src/main and rinna-core/src/main
#

set -e

# Determine script and project directories
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
RINNA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

cd "$RINNA_DIR" || error "Failed to change to the Rinna directory."

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}      Duplicate Model Files Removal Utility           ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Create a backup directory
TIMESTAMP=$(date +"%Y%m%d%H%M%S")
BACKUP_DIR="$RINNA_DIR/backup/duplicate-models-$TIMESTAMP"
mkdir -p "$BACKUP_DIR"
echo -e "${BLUE}Created backup directory: ${YELLOW}$BACKUP_DIR${NC}"

# Function to back up a file
backup_file() {
  local file_path="$1"
  local relative_path="${file_path#$RINNA_DIR/}"
  local backup_path="$BACKUP_DIR/$relative_path"
  
  mkdir -p "$(dirname "$backup_path")"
  cp "$file_path" "$backup_path"
  echo -e "${CYAN}Backed up: $relative_path${NC}"
}

# Remove lower priority duplicate files
# Prioritize: 
# 1. rinna-core/src/main over src/main 
# 2. domain/model over domain

# Scan for duplicate WorkItemRecord.java files
echo -e "\n${BLUE}Removing duplicate WorkItemRecord files...${NC}"

# Backup and remove src/main/java/org/rinna/domain/WorkItemRecord.java
if [ -f "$RINNA_DIR/src/main/java/org/rinna/domain/WorkItemRecord.java" ]; then
  backup_file "$RINNA_DIR/src/main/java/org/rinna/domain/WorkItemRecord.java"
  rm "$RINNA_DIR/src/main/java/org/rinna/domain/WorkItemRecord.java"
  echo -e "${GREEN}Removed duplicate file: src/main/java/org/rinna/domain/WorkItemRecord.java${NC}"
fi

# Backup and remove src/main/java/org/rinna/domain/model/WorkItemRecord.java
if [ -f "$RINNA_DIR/src/main/java/org/rinna/domain/model/WorkItemRecord.java" ]; then
  backup_file "$RINNA_DIR/src/main/java/org/rinna/domain/model/WorkItemRecord.java"
  rm "$RINNA_DIR/src/main/java/org/rinna/domain/model/WorkItemRecord.java"
  echo -e "${GREEN}Removed duplicate file: src/main/java/org/rinna/domain/model/WorkItemRecord.java${NC}"
fi

# Scan for duplicate DefaultWorkItem.java files
echo -e "\n${BLUE}Removing duplicate DefaultWorkItem files...${NC}"

# Keep rinna-core/domain/model/DefaultWorkItem.java
# Remove others

# Backup and remove src/main/java/org/rinna/domain/DefaultWorkItem.java
if [ -f "$RINNA_DIR/src/main/java/org/rinna/domain/DefaultWorkItem.java" ]; then
  backup_file "$RINNA_DIR/src/main/java/org/rinna/domain/DefaultWorkItem.java"
  rm "$RINNA_DIR/src/main/java/org/rinna/domain/DefaultWorkItem.java"
  echo -e "${GREEN}Removed duplicate file: src/main/java/org/rinna/domain/DefaultWorkItem.java${NC}"
fi

# Backup and remove src/main/java/org/rinna/domain/model/DefaultWorkItem.java
if [ -f "$RINNA_DIR/src/main/java/org/rinna/domain/model/DefaultWorkItem.java" ]; then
  backup_file "$RINNA_DIR/src/main/java/org/rinna/domain/model/DefaultWorkItem.java"
  rm "$RINNA_DIR/src/main/java/org/rinna/domain/model/DefaultWorkItem.java"
  echo -e "${GREEN}Removed duplicate file: src/main/java/org/rinna/domain/model/DefaultWorkItem.java${NC}"
fi

# Backup and remove src/main/java/org/rinna/model/DefaultWorkItem.java
if [ -f "$RINNA_DIR/src/main/java/org/rinna/model/DefaultWorkItem.java" ]; then
  backup_file "$RINNA_DIR/src/main/java/org/rinna/model/DefaultWorkItem.java"
  rm "$RINNA_DIR/src/main/java/org/rinna/model/DefaultWorkItem.java"
  echo -e "${GREEN}Removed duplicate file: src/main/java/org/rinna/model/DefaultWorkItem.java${NC}"
fi

# Keep rinna-core/src/main/java/org/rinna/domain/model/DefaultWorkItem.java as the canonical version

# Also clean up src/test DefaultWorkItem
if [ -f "$RINNA_DIR/src/test/java/org/rinna/domain/model/DefaultWorkItem.java" ]; then
  backup_file "$RINNA_DIR/src/test/java/org/rinna/domain/model/DefaultWorkItem.java"
  rm "$RINNA_DIR/src/test/java/org/rinna/domain/model/DefaultWorkItem.java"
  echo -e "${GREEN}Removed test file: src/test/java/org/rinna/domain/model/DefaultWorkItem.java${NC}"
fi

echo -e "\n${BLUE}=====================================================${NC}"
echo -e "${BLUE}                    Summary                           ${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo -e "${GREEN}Removed duplicate files and created backups${NC}"
echo -e "${CYAN}Backups created in: $BACKUP_DIR${NC}"
echo -e "${YELLOW}You should now run 'mvn compile' to verify there are no errors${NC}"