#!/usr/bin/env bash
#
# cleanup-legacy-files.sh - Identify and remove legacy Java files
#
# PURPOSE: Safely identify and remove deprecated, duplicate, or legacy Java files
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

# Error handling
error() {
  echo -e "${RED}Error: $1${NC}" >&2
  exit 1
}

# Help message
help() {
  cat << EOF
${BLUE}cleanup-legacy-files.sh${NC} - Identify and remove legacy Java files

Usage: cleanup-legacy-files.sh [options]

Options:
  --analyze          Only analyze and report issues (default)
  --create-branch    Create a new git branch for cleanup
  --remove           Actually remove the identified files
  --backup           Create backups before removing files
  --help             Show this help message

Examples:
  ./bin/cleanup-legacy-files.sh                # Analyze and report issues
  ./bin/cleanup-legacy-files.sh --create-branch=cleanup-legacy  # Create branch and analyze
  ./bin/cleanup-legacy-files.sh --remove --backup  # Remove files with backup
EOF
}

# Default settings
ANALYZE_ONLY=true
CREATE_BRANCH=""
REMOVE_FILES=false
CREATE_BACKUP=false

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --analyze)
      ANALYZE_ONLY=true
      shift
      ;;
    --create-branch=*)
      CREATE_BRANCH="${1#*=}"
      shift
      ;;
    --remove)
      REMOVE_FILES=true
      ANALYZE_ONLY=false
      shift
      ;;
    --backup)
      CREATE_BACKUP=true
      shift
      ;;
    --help)
      help
      exit 0
      ;;
    *)
      error "Unknown option: $1. Use --help for usage information."
      ;;
  esac
done

cd "$RINNA_DIR" || error "Failed to change to the Rinna directory."

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}      Legacy File Cleanup Utility                     ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# Create branch if requested
if [[ -n "$CREATE_BRANCH" ]]; then
  echo -e "${BLUE}Creating new branch: ${YELLOW}$CREATE_BRANCH${NC}"
  # Check if branch already exists
  if git show-ref --verify --quiet "refs/heads/$CREATE_BRANCH"; then
    error "Branch '$CREATE_BRANCH' already exists. Choose a different branch name."
  fi
  
  # Create and checkout new branch
  git checkout -b "$CREATE_BRANCH" || error "Failed to create branch: $CREATE_BRANCH"
  echo -e "${GREEN}Successfully created and checked out branch: $CREATE_BRANCH${NC}"
fi

# Create backup directory
BACKUP_DIR=""
if [[ "$CREATE_BACKUP" == "true" ]]; then
  TIMESTAMP=$(date +"%Y%m%d%H%M%S")
  BACKUP_DIR="$RINNA_DIR/backup/legacy-cleanup-$TIMESTAMP"
  mkdir -p "$BACKUP_DIR"
  echo -e "${BLUE}Created backup directory: ${YELLOW}$BACKUP_DIR${NC}"
fi

# Define functions to check if a file is still in use
file_is_imported() {
  local file_path="$1"
  local class_name=$(basename "$file_path" .java)
  local result=$(grep -l "import .*$class_name" --include="*.java" -r . | grep -v "$file_path" | grep -v "backup")
  
  if [[ -n "$result" ]]; then
    return 0 # File is imported (true)
  else
    return 1 # File is not imported (false)
  fi
}

file_has_instances() {
  local file_path="$1"
  local class_name=$(basename "$file_path" .java)
  local result=$(grep -l "new $class_name" --include="*.java" -r . | grep -v "$file_path" | grep -v "backup")
  
  if [[ -n "$result" ]]; then
    return 0 # File has instances (true)
  else
    return 1 # File is not instantiated (false)
  fi
}

file_has_references() {
  local file_path="$1"
  local class_name=$(basename "$file_path" .java)
  local result=$(grep -l "$class_name" --include="*.java" -r . | grep -v "$file_path" | grep -v "backup")
  
  if [[ -n "$result" ]]; then
    return 0 # File has references (true)
  else
    return 1 # File has no references (false)
  fi
}

# Function to analyze and potentially remove a file
analyze_file() {
  local file_path="$1"
  local reason="$2"
  local class_name=$(basename "$file_path" .java)
  
  echo -e "\n${MAGENTA}Analyzing ${class_name}${NC} (${file_path})"
  echo -e "${CYAN}Reason for consideration:${NC} $reason"
  
  # Check if file is used
  if file_is_imported "$file_path"; then
    echo -e "${YELLOW}Warning: File is imported in other files${NC}"
    echo -e "Imported in:"
    grep -l "import .*$class_name" --include="*.java" -r . | grep -v "$file_path" | grep -v "backup" | head -5
    echo -e "${RED}Not safe to remove due to imports${NC}"
    return 1
  elif file_has_instances "$file_path"; then
    echo -e "${YELLOW}Warning: Class is instantiated in other files${NC}"
    echo -e "Instantiated in:"
    grep -l "new $class_name" --include="*.java" -r . | grep -v "$file_path" | grep -v "backup" | head -5
    echo -e "${RED}Not safe to remove due to instantiations${NC}"
    return 1
  elif file_has_references "$file_path"; then
    echo -e "${YELLOW}Warning: Class is referenced in other files${NC}"
    echo -e "Referenced in:"
    grep -l "$class_name" --include="*.java" -r . | grep -v "$file_path" | grep -v "backup" | head -5
    
    # Check if all references are in imports or instantiations
    local references_count=$(grep -l "$class_name" --include="*.java" -r . | grep -v "$file_path" | grep -v "backup" | wc -l)
    echo -e "${YELLOW}Total references: $references_count${NC}"
    
    # For files with @Deprecated, check if they can be removed
    if grep -q "@Deprecated" "$file_path"; then
      echo -e "${YELLOW}Note: File is marked as @Deprecated${NC}"
      if [[ "$ANALYZE_ONLY" == "false" && "$REMOVE_FILES" == "true" ]]; then
        echo -e "${RED}Warning: This is a deprecated file with references.${NC}"
        echo -e "${RED}Not automatically removing to avoid breaking changes.${NC}"
        return 1
      fi
    fi
    
    echo -e "${YELLOW}This file is referenced and might not be safe to remove${NC}"
    return 1
  else
    echo -e "${GREEN}File appears to be unused${NC}"
    
    # Handle backup and removal
    if [[ "$ANALYZE_ONLY" == "false" && "$REMOVE_FILES" == "true" ]]; then
      if [[ "$CREATE_BACKUP" == "true" && -n "$BACKUP_DIR" ]]; then
        mkdir -p "$BACKUP_DIR/$(dirname "${file_path#$RINNA_DIR/}")"
        cp "$file_path" "$BACKUP_DIR/$(dirname "${file_path#$RINNA_DIR/}")"
        echo -e "${CYAN}Created backup at: $BACKUP_DIR/${file_path#$RINNA_DIR/}${NC}"
      fi
      
      rm "$file_path"
      echo -e "${GREEN}Removed file: $file_path${NC}"
    else
      echo -e "${YELLOW}File marked for potential removal (use --remove to actually delete)${NC}"
    fi
    return 0
  fi
}

# Find and analyze deprecated files
echo -e "\n${BLUE}Looking for deprecated files...${NC}"
DEPRECATED_FILES=$(grep -l "@Deprecated" --include="*.java" -r "$RINNA_DIR" | grep -v "backup")
REMOVED_COUNT=0
KEPT_COUNT=0

echo -e "${BLUE}Found ${#DEPRECATED_FILES[@]} deprecated files${NC}"
for file in $DEPRECATED_FILES; do
  if analyze_file "$file" "Marked with @Deprecated"; then
    ((REMOVED_COUNT++))
  else
    ((KEPT_COUNT++))
  fi
done

# Find and analyze DefaultWorkItem classes which should be replaced by WorkItemRecord
echo -e "\n${BLUE}Looking for DefaultWorkItem classes...${NC}"
DEFAULT_WORKITEM_FILES=$(find "$RINNA_DIR" -name "DefaultWorkItem.java" -not -path "*/backup/*")
for file in $DEFAULT_WORKITEM_FILES; do
  if analyze_file "$file" "Legacy DefaultWorkItem (should use WorkItemRecord)"; then
    ((REMOVED_COUNT++))
  else
    ((KEPT_COUNT++))
  fi
done

# Find potential duplicate model files
echo -e "\n${BLUE}Looking for duplicate model files...${NC}"
MODEL_FILES=$(find "$RINNA_DIR" -name "*.java" -path "*/domain/model/*" -not -path "*/backup/*")
for file in $MODEL_FILES; do
  # Check for duplicate in domain directory
  base_name=$(basename "$file")
  parent_dir=$(dirname "$file")
  grandparent_dir=$(dirname "$parent_dir")
  potential_duplicate="$grandparent_dir/$base_name"
  
  if [[ -f "$potential_duplicate" ]]; then
    echo -e "\n${MAGENTA}Found potential duplicate:${NC}"
    echo -e "1: $file"
    echo -e "2: $potential_duplicate"
    
    # Check if content is the same
    if diff -q "$file" "$potential_duplicate" >/dev/null; then
      echo -e "${YELLOW}Files are identical${NC}"
      
      # Prefer to keep files in domain/model
      if analyze_file "$potential_duplicate" "Duplicate of file in domain/model directory"; then
        ((REMOVED_COUNT++))
      else
        ((KEPT_COUNT++))
      fi
    else
      echo -e "${YELLOW}Files differ in content - manual review required${NC}"
    fi
  fi
done

# Find files in backup directory that are not needed
echo -e "\n${BLUE}Looking for backup files that could be cleaned up...${NC}"
if [[ -d "$RINNA_DIR/backup" ]]; then
  # Only list these files for manual removal - don't auto-remove
  echo -e "${YELLOW}Consider cleaning up these backup directories:${NC}"
  find "$RINNA_DIR/backup" -name "*.java" -type f | sort | head -10
  echo -e "${YELLOW}(potentially many more files in backup directories)${NC}"
else
  echo -e "${GREEN}No backup directory found${NC}"
fi

# Summary
echo -e "\n${BLUE}=====================================================${NC}"
echo -e "${BLUE}                    Summary                           ${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo -e "Files marked for removal: ${GREEN}$REMOVED_COUNT${NC}"
echo -e "Files kept due to references: ${YELLOW}$KEPT_COUNT${NC}"

if [[ "$ANALYZE_ONLY" == "true" ]]; then
  echo -e "\n${YELLOW}This was an analysis run only. No files were actually removed.${NC}"
  echo -e "${YELLOW}Use --remove to remove the identified files.${NC}"
elif [[ "$REMOVE_FILES" == "true" ]]; then
  echo -e "\n${GREEN}Successfully removed $REMOVED_COUNT files.${NC}"
  if [[ "$CREATE_BACKUP" == "true" && -n "$BACKUP_DIR" ]]; then
    echo -e "${CYAN}Backups created in: $BACKUP_DIR${NC}"
  fi
fi

if [[ -n "$CREATE_BRANCH" ]]; then
  echo -e "\n${BLUE}Created branch: ${YELLOW}$CREATE_BRANCH${NC}"
  echo -e "${BLUE}You can now test your changes with:${NC}"
  echo -e "  ${CYAN}./bin/build.sh --skip-quality${NC}"
  echo -e "${BLUE}And commit them with:${NC}"
  echo -e "  ${CYAN}git add -A && git commit -m \"Remove legacy and duplicate files\"${NC}"
fi