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
        return None, None
    
    parts = field_str.split("=", 1)
    return parts[0], parts[1]

def main():
    """Main function."""
    args = parse_args()
    
    # Configure logging
    configure_logging()
    
    # Get logger
    logger = get_logger(args.name)
    
    # Parse fields
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