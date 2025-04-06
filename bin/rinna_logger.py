#!/usr/bin/env python3
"""
Standardized logging module for Rinna Python tools

This module provides consistent logging in Python that matches the standard
set by Java (SLF4J) and Go for the Rinna project.

Copyright (c) 2025 Eric C. Mumford (@heymumford)

Developed with analytical assistance from AI tools.
All rights reserved.

This source code is licensed under the MIT License
found in the LICENSE file in the root directory of this source tree.

Logging levels:
- ERROR: Critical issues that prevent proper functioning
- WARN: Potential problems that don't stop execution
- INFO: Important application lifecycle events and operations
- DEBUG: Detailed information for development and troubleshooting
- TRACE: Very fine-grained details for complex debugging

Example usage:

    from rinna_logger import get_logger

    logger = get_logger(__name__)
    
    logger.info("Application started")
    logger.debug("Processing data: %s", data)
    
    # With context fields
    logger.with_field("user_id", user_id).info("User logged in")
    
    # Multiple fields
    ctx = {"request_id": req_id, "client_ip": ip}
    logger.with_fields(ctx).info("Request received")
"""

import logging
import os
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Union, cast

# Add TRACE level to logging (lower than DEBUG)
TRACE = 5
logging.addLevelName(TRACE, "TRACE")

def trace(self, message, *args, **kwargs):
    """Log a message at TRACE level."""
    if self.isEnabledFor(TRACE):
        self._log(TRACE, message, args, **kwargs)

# Add trace method to Logger class
logging.Logger.trace = trace


class RinnaLogger:
    """Standardized logger for Rinna Python tools."""
    
    def __init__(self, name: str, level: int = logging.INFO) -> None:
        """Initialize the logger."""
        self.name = name
        self.fields: Dict[str, Any] = {}
        self.logger = logging.getLogger(name)
        self.logger.setLevel(level)
        
        # Check if handlers already configured
        if not self.logger.handlers:
            self._configure_default_handlers()
    
    def _configure_default_handlers(self) -> None:
        """Configure default console handler."""
        # Console handler
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setFormatter(self._get_formatter())
        self.logger.addHandler(console_handler)
        
        # File handler if log directory exists
        log_dir = os.environ.get("RINNA_LOG_DIR", str(Path.home() / ".rinna" / "logs"))
        os.makedirs(log_dir, exist_ok=True)
        
        file_handler = logging.FileHandler(
            os.path.join(log_dir, "rinna-python.log"), 
            mode="a"
        )
        file_handler.setFormatter(self._get_formatter())
        self.logger.addHandler(file_handler)
    
    def _get_formatter(self) -> logging.Formatter:
        """Get the log formatter."""
        return logging.Formatter(
            "%(asctime)s [%(levelname)s] [%(name)s] %(message)s",
            "%Y-%m-%dT%H:%M:%S%z"
        )
    
    def _format_fields(self) -> str:
        """Format context fields as string."""
        if not self.fields:
            return ""
        
        parts = []
        for key, value in self.fields.items():
            parts.append(f"{key}={value}")
        
        return " ".join(parts)
    
    def with_field(self, key: str, value: Any) -> "RinnaLogger":
        """Create a new logger with an additional field."""
        logger = RinnaLogger(self.name)
        logger.logger = self.logger
        
        # Copy existing fields
        logger.fields = dict(self.fields)
        
        # Add new field
        logger.fields[key] = value
        return logger
    
    def with_fields(self, fields: Dict[str, Any]) -> "RinnaLogger":
        """Create a new logger with additional fields."""
        logger = RinnaLogger(self.name)
        logger.logger = self.logger
        
        # Copy existing fields
        logger.fields = dict(self.fields)
        
        # Add new fields
        logger.fields.update(fields)
        return logger
    
    def _log(self, level: int, msg: str, *args, **kwargs) -> None:
        """Log a message with the appropriate level."""
        if args and isinstance(args[0], (list, tuple)):
            args = args[0]
            
        if self.fields:
            fields_str = self._format_fields()
            if args:
                msg = f"{msg % args} {fields_str}"
                self.logger.log(level, msg)
            else:
                msg = f"{msg} {fields_str}"
                self.logger.log(level, msg)
        else:
            self.logger.log(level, msg, *args, **kwargs)
    
    def trace(self, msg: str, *args, **kwargs) -> None:
        """Log a message at TRACE level."""
        self._log(TRACE, msg, *args, **kwargs)
    
    def debug(self, msg: str, *args, **kwargs) -> None:
        """Log a message at DEBUG level."""
        self._log(logging.DEBUG, msg, *args, **kwargs)
    
    def info(self, msg: str, *args, **kwargs) -> None:
        """Log a message at INFO level."""
        self._log(logging.INFO, msg, *args, **kwargs)
    
    def warn(self, msg: str, *args, **kwargs) -> None:
        """Log a message at WARN level."""
        self._log(logging.WARNING, msg, *args, **kwargs)
    
    def warning(self, msg: str, *args, **kwargs) -> None:
        """Alias for warn()."""
        self.warn(msg, *args, **kwargs)
    
    def error(self, msg: str, *args, **kwargs) -> None:
        """Log a message at ERROR level."""
        self._log(logging.ERROR, msg, *args, **kwargs)
    
    def fatal(self, msg: str, *args, **kwargs) -> None:
        """Log a message at FATAL/CRITICAL level."""
        self._log(logging.CRITICAL, msg, *args, **kwargs)
    
    def critical(self, msg: str, *args, **kwargs) -> None:
        """Alias for fatal()."""
        self.fatal(msg, *args, **kwargs)
    
    def exception(self, msg: str, *args, exc_info=True, **kwargs) -> None:
        """Log an exception with traceback at ERROR level."""
        if self.fields:
            fields_str = self._format_fields()
            if args:
                msg = f"{msg % args} {fields_str}"
                self.logger.exception(msg, exc_info=exc_info, **kwargs)
            else:
                msg = f"{msg} {fields_str}"
                self.logger.exception(msg, exc_info=exc_info, **kwargs)
        else:
            self.logger.exception(msg, *args, exc_info=exc_info, **kwargs)


# Configure root logger
def configure_logging(level: int = logging.INFO, 
                     log_dir: Optional[str] = None,
                     log_file: Optional[str] = None) -> None:
    """Configure global logging settings."""
    # Set default level for all loggers
    logging.getLogger().setLevel(level)
    
    # Clear existing handlers
    root = logging.getLogger()
    for handler in root.handlers[:]:
        root.removeHandler(handler)
    
    # Configure console handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(logging.Formatter(
        "%(asctime)s [%(levelname)s] [%(name)s] %(message)s",
        "%Y-%m-%dT%H:%M:%S%z"
    ))
    root.addHandler(console_handler)
    
    # Configure file handler if requested
    if log_file:
        if log_dir:
            # Ensure log directory exists
            os.makedirs(log_dir, exist_ok=True)
            log_path = os.path.join(log_dir, log_file)
        else:
            log_dir = os.environ.get("RINNA_LOG_DIR", str(Path.home() / ".rinna" / "logs"))
            os.makedirs(log_dir, exist_ok=True)
            log_path = os.path.join(log_dir, log_file)
        
        file_handler = logging.FileHandler(log_path, mode="a")
        file_handler.setFormatter(logging.Formatter(
            "%(asctime)s [%(levelname)s] [%(name)s] %(message)s",
            "%Y-%m-%dT%H:%M:%S%z"
        ))
        root.addHandler(file_handler)


# Cache for loggers
_loggers: Dict[str, RinnaLogger] = {}

def get_logger(name: str) -> RinnaLogger:
    """Get a RinnaLogger instance for the given name."""
    if name not in _loggers:
        _loggers[name] = RinnaLogger(name)
    return _loggers[name]


# Default loggers
logger = get_logger("rinna")


if __name__ == "__main__":
    # Example usage
    configure_logging(level=TRACE)
    
    log = get_logger("example")
    log.info("This is an info message")
    log.debug("This is a debug message")
    log.trace("This is a trace message")
    log.warn("This is a warning message")
    log.error("This is an error message")
    
    # With context fields
    log.with_field("request_id", "12345").info("Processing request")
    
    # Multiple fields
    ctx = {
        "user_id": "user-123",
        "client_ip": "192.168.1.1",
        "action": "login"
    }
    log.with_fields(ctx).info("User action")
    
    # Log an exception
    try:
        result = 10 / 0
    except Exception as e:
        log.exception("An error occurred during calculation")