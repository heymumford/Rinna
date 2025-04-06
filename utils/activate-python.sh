#!/usr/bin/env bash
#
# activate-python.sh - Activate Python virtual environment for Rinna
#

# Activate the virtual environment
if [[ -f "/home/emumford/NativeLinuxProjects/Rinna/.venv/bin/activate" ]]; then
    source "/home/emumford/NativeLinuxProjects/Rinna/.venv/bin/activate"
    
    # Set project environment variables
    export RINNA_ROOT="/home/emumford/NativeLinuxProjects/Rinna"
    export RINNA_VERSION=$(cat "/home/emumford/NativeLinuxProjects/Rinna/.venv/version" 2>/dev/null || echo "unknown")
    
    # Show version info
    if python -c "import rinna_venv_info" &>/dev/null; then
        python -m rinna_venv_info
    else
        echo "Rinna Python environment activated (version $RINNA_VERSION)"
    fi
    
    echo "Run 'deactivate' to exit the virtual environment"
else
    echo "Error: Virtual environment not found at /home/emumford/NativeLinuxProjects/Rinna/.venv"
    echo "Run 'bin/setup-python.sh' to create it"
    return 1
fi
