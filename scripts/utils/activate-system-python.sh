#!/usr/bin/env bash
# System Python environment setup for Rinna
#
# Set Python environment variables
export PYTHONPATH="$RINNA_DIR:$RINNA_DIR/python"

# Use the correct Python version
if command -v python3.8 &>/dev/null; then
  alias python=python3.8
  alias pip=pip3.8
else
  # Fall back to python3
  alias python=python3
  alias pip=pip3
fi

# Add local bin directory to PATH if it exists
if [[ -d "$HOME/.local/bin" ]]; then
  export PATH="$HOME/.local/bin:$PATH"
fi

echo "System Python environment configured"
python --version