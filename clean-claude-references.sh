#!/bin/bash

# Script to remove Claude references from git commit messages

set -e

# Create a backup branch if it doesn't exist
if ! git show-ref --verify --quiet refs/heads/before-claude-cleanup; then
  echo "Creating backup branch 'before-claude-cleanup'"
  git branch before-claude-cleanup
else
  echo "Backup branch 'before-claude-cleanup' already exists"
fi

# List of strings to remove
CLAUDE_PATTERNS=(
  "🤖 Generated with \\[Claude Code\\]\\(https://claude.ai/code\\)"
  "Co-Authored-By: Claude <noreply@anthropic.com>"
  "Anthropic"
  "anthropic"
  "Claude"
  "claude"
)

# Build filter-branch command to remove all patterns
FILTER_CMD=""
for pattern in "${CLAUDE_PATTERNS[@]}"; do
  FILTER_CMD+="s/$pattern//g; "
done

# Use git filter-branch to rewrite history
echo "Rewriting git history to remove Claude references..."
git filter-branch --force --msg-filter \
  "sed -e '$FILTER_CMD'" \
  --tag-name-filter cat -- --all

echo ""
echo "Cleanup complete. A backup branch 'before-claude-cleanup' has been created."
echo "If everything looks good, you may want to force push with:"
echo "  git push --force-with-lease origin main"
echo ""
echo "To remove the refs/original backup, run:"
echo "  git update-ref -d refs/original/refs/heads/main"
echo "  git reflog expire --expire=now --all"
echo "  git gc --prune=now"
echo ""
echo "WARNING: Force pushing will rewrite history on the remote repository."
echo "Make sure your teammates are aware of this change if they have cloned the repository."