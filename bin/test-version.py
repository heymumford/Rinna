#!/usr/bin/env python3
"""
Simple test script to verify the version.properties integration.
This script demonstrates that Python components can read the version
from the central version.properties file.
"""

import sys
import os
from pathlib import Path

# Add the bin directory to the Python path
script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, script_dir)

# Import the config from rinna_config.py
try:
    from rinna_config import config
    
    # Get the version from the config
    version = config.get("project.version")
    
    # Print the version
    print(f"Rinna Project Version: {version}")
    print(f"This version is read from the central version.properties file.")
    
    # Demonstrate other config options
    print("\nOther configuration options:")
    print(f"Project Name: {config.get('project.name')}")
    print(f"Environment: {config.get('project.environment')}")
    print(f"Data Directory: {config.get_path('project.data_dir')}")
    
except ImportError as e:
    print(f"Error importing rinna_config: {e}")
    sys.exit(1)
except Exception as e:
    print(f"Error reading version: {e}")
    sys.exit(1)
    
print("\nVersion test completed successfully!")