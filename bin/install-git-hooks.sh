#!/bin/bash
#
# install-git-hooks.sh - Install Git hooks for the Rinna project
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Path to the project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOOKS_DIR="$PROJECT_ROOT/bin/hooks"
GIT_HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

# Create the Git hooks directory if it doesn't exist
mkdir -p "$GIT_HOOKS_DIR"

# Install the hooks
install_hook() {
  local hook_name="$1"
  local source_file="$HOOKS_DIR/$hook_name"
  local target_file="$GIT_HOOKS_DIR/$hook_name"
  
  if [ -f "$source_file" ]; then
    echo "Installing $hook_name hook..."
    if [ -f "$target_file" ] && diff -q "$source_file" "$target_file" > /dev/null; then
      echo "✅ $hook_name hook already installed (files are identical)"
    else
      cp "$source_file" "$target_file"
      chmod +x "$target_file"
      echo "✅ $hook_name hook installed successfully"
    fi
  else
    echo "❌ Source hook file not found: $source_file"
    return 1
  fi
}

# Install the pre-commit hook
install_hook "pre-commit"

# Install the post-push hook
install_hook "post-push"

echo "Git hooks installation complete!"

# Instructions for the post-push hook
echo ""
echo "NOTE: The post-push hook will automatically increment the build number after each push."
echo "      To make this work, you need to add a post-push hook manually or configure Git to run it."
echo ""
echo "      Option 1: Set up a Git alias for push that runs the post-push hook:"
echo "      git config --local alias.pushbuild '!git push \$@ && .git/hooks/post-push'"
echo ""
echo "      Option 2: For automated CI environments, you can call bin/increment-build.sh directly."
echo ""
echo "      Run 'bin/install-git-hooks.sh --configure' to automatically configure the Git alias."

# Configure git alias if requested
if [ "$1" == "--configure" ]; then
  git config --local alias.pushbuild '!git push "$@" && .git/hooks/post-push'
  echo "✅ Git alias 'pushbuild' configured. Use 'git pushbuild' instead of 'git push' to auto-increment build numbers."
fi

exit 0