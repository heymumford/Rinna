#!/usr/bin/env python3
"""
Python logging script for the multi-language logging system.

This script is called by the Java MultiLanguageLogger to log messages
from Java code to the Python logging system.

Usage:
    python3 log_python.py --level INFO --name org.rinna.Main --message "Hello, world!" --field key1=value1 --field key2=value2

Arguments:
    --level    Log level (TRACE, DEBUG, INFO, WARN, ERROR)
    --name     Logger name
    --message  Message to log
    --field    Context field in the format key=value (can be specified multiple times)
"""

import argparse
import os
import sys
from pathlib import Path

# Add the project's bin directory to the path
script_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.dirname(script_dir)
sys.path.insert(0, project_root)

# Import rinna_logger from bin
from bin.rinna_logger import get_logger, configure_logging

def parse_args():
    """Parse command-line arguments."""
    parser = argparse.ArgumentParser(description="Python logging script for the multi-language logging system")
    parser.add_argument("--level", required=True, help="Log level (TRACE, DEBUG, INFO, WARN, ERROR)")
    parser.add_argument("--name", required=True, help="Logger name")
    parser.add_argument("--message", required=True, help="Message to log")
    parser.add_argument("--field", action="append", help="Context field in the format key=value")
    return parser.parse_args()

def parse_field(field_str):
    """Parse a field string in the format key=value."""
    if "=" not in field_str:
        print(f"Warning: Invalid field format '{field_str}', expected key=value", file=sys.stderr)
        return None, None
    
    parts = field_str.split("=", 1)
    key = parts[0].strip()
    value = parts[1].strip()
    
    # Validate key (non-empty and alphanumeric with underscores)
    if not key:
        print("Warning: Empty field key found, skipping", file=sys.stderr)
        return None, None
    
    if not is_valid_field_key(key):
        print(f"Warning: Invalid field key '{key}', using sanitized version", file=sys.stderr)
        key = sanitize_field_key(key)
        
    return key, value

def is_valid_field_key(key):
    """Check if a field key is valid (alphanumeric with underscores)."""
    return all(c.isalnum() or c == '_' for c in key)

def sanitize_field_key(key):
    """Convert an invalid field key to a valid one."""
    return ''.join(c if c.isalnum() or c == '_' else '_' for c in key)

def ensure_log_directory(log_dir):
    """Ensure the log directory exists."""
    try:
        # Check if directory exists
        if os.path.exists(log_dir):
            if not os.path.isdir(log_dir):
                print(f"Warning: Path exists but is not a directory: {log_dir}", file=sys.stderr)
                return False
            return True
            
        # Create directory with parents
        os.makedirs(log_dir, mode=0o755, exist_ok=True)
        return True
    except Exception as e:
        print(f"Error creating log directory: {e}", file=sys.stderr)
        return False

def main():
    """Main function."""
    args = parse_args()
    
    # Get log directory
    log_dir = os.environ.get("RINNA_LOG_DIR", str(Path.home() / ".rinna" / "logs"))
    
    # Ensure log directory exists
    ensure_log_directory(log_dir)
    
    # Configure logging
    configure_logging()
    
    # Get logger
    logger = get_logger(args.name)
    
    # Parse fields with validation
    fields = {}
    if args.field:
        for field_str in args.field:
            key, value = parse_field(field_str)
            if key and value:
                fields[key] = value
    
    # Add fields to logger
    if fields:
        logger = logger.with_fields(fields)
    
    # Log message with appropriate level
    level = args.level.upper()
    if level == "TRACE":
        logger.trace(args.message)
    elif level == "DEBUG":
        logger.debug(args.message)
    elif level == "INFO":
        logger.info(args.message)
    elif level == "WARN" or level == "WARNING":
        logger.warn(args.message)
    elif level == "ERROR":
        logger.error(args.message)
    else:
        logger.info(f"Unknown log level '{level}', defaulting to INFO: {args.message}")

if __name__ == "__main__":
    # Check if any arguments were provided
    if len(sys.argv) > 1:
        main()
    else:
        # Self-test if no arguments
        print("Python Logger Bridge Self-Test")
        print("=============================")
        configure_logging()
        
        logger = get_logger("python_bridge_test")
        
        # Basic logging
        logger.trace("This is a TRACE message")
        logger.debug("This is a DEBUG message")
        logger.info("This is an INFO message")
        logger.warn("This is a WARN message")
        logger.error("This is an ERROR message")
        
        # With context fields
        logger.with_field("request_id", "12345").info("Message with request ID")
        
        # With multiple fields
        fields = {
            "user_id": "user-123",
            "action": "login",
            "client_ip": "192.168.1.1"
        }
        logger.with_fields(fields).info("Message with multiple fields")