#!/usr/bin/env python3
"""
Unified configuration module for Rinna Python tools

This module provides access to the centralized Rinna configuration system
from Python code. It loads configuration from the following sources:

1. Environment variables with RINNA_ prefix
2. Python-specific YAML configuration file
3. Default values defined in code

Example usage:

    from rinna_config import config
    
    # Access configuration values
    api_key = config.get("python.diagrams.lucidchart.api_key")
    project_name = config.get("project.name")
    
    # Access with default values
    port = config.get_int("go.api.port", 8080)
    enabled = config.get_bool("java.backend.enable_swagger", True)
"""

import os
import sys, os
import yaml
from pathlib import Path
from typing import Any, Dict, List, Optional, Union


class RinnaConfig:
    """Unified configuration for Rinna Python tools."""
    
    ENV_PREFIX = "RINNA_"
    DEFAULT_CONFIG_DIR = os.path.expanduser("~/.rinna/config")
    PYTHON_CONFIG_PATH = "python/config.yaml"
    
    def __init__(self) -> None:
        """Initialize the configuration."""
        self._config: Dict[str, Any] = {}
        self._loaded = False
    
    def _ensure_loaded(self) -> None:
        """Ensure the configuration is loaded."""
        if not self._loaded:
            self.reload()
    
    def reload(self) -> None:
        """Reload the configuration from all sources."""
        self._config = {}
        self._load_config()
        self._loaded = True
    
    def _load_config(self) -> None:
        """Load the configuration from all sources."""
        # Get config directory from environment variable or use default
        config_dir = os.environ.get(f"{self.ENV_PREFIX}PROJECT_CONFIG_DIR", self.DEFAULT_CONFIG_DIR)
        
        # Build the path to the Python config file
        config_file = os.path.join(config_dir, self.PYTHON_CONFIG_PATH)
        
        # Load from YAML file if it exists
        if os.path.isfile(config_file):
            try:
                with open(config_file, 'r') as f:
                    self._config = yaml.safe_load(f) or {}
                print(f"Loaded configuration from {config_file}")
            except Exception as e:
                print(f"Warning: Failed to load configuration from {config_file}: {e}", file=sys.stderr)
        else:
            print(f"Warning: Python configuration file not found: {config_file}")
            print("Run 'rin config generate' to create it.")
        
        # Set default values
        self._set_defaults()
    
    def _set_defaults(self) -> None:
        """Set default values for missing configuration."""
        # Read version from version.properties file
        project_version = self._read_version_from_properties()
        
        defaults = {
            "project": {
                "name": "Rinna",
                "version": project_version,
                "environment": "development",
                "data_dir": os.path.expanduser("~/.rinna/data"),
                "temp_dir": os.path.expanduser("~/.rinna/temp"),
                "config_dir": os.path.expanduser("~/.rinna/config"),
            },
            "python": {
                "diagrams": {
                    "output_dir": os.path.expanduser("~/.rinna/data/diagrams"),
                    "lucidchart": {
                        "api_key": "",
                        "token": "",
                    }
                }
            }
        }
        
        # Merge defaults with loaded config
        self._merge_defaults(defaults)
        
    def _read_version_from_properties(self) -> str:
        """Read version from the central version.properties file."""
        # Find the project root directory
        script_dir = os.path.dirname(os.path.abspath(__file__))
        project_root = os.path.abspath(os.path.join(script_dir, ".."))
        version_file = os.path.join(project_root, "version.properties")
        
        default_version = "1.0.0"  # Fallback version
        
        if not os.path.isfile(version_file):
            print(f"Warning: version.properties file not found at {version_file}", file=sys.stderr)
            return default_version
            
        try:
            # Read version from properties file
            with open(version_file, 'r') as f:
                for line in f:
                    line = line.strip()
                    if line.startswith("version="):
                        return line.split("=", 1)[1].strip()
            
            print("Warning: version not found in version.properties", file=sys.stderr)
            return default_version
            
        except Exception as e:
            print(f"Error reading version from properties file: {e}", file=sys.stderr)
            return default_version
    
    def _merge_defaults(self, defaults: Dict[str, Any], path: str = "") -> None:
        """Recursively merge defaults into the configuration."""
        for key, value in defaults.items():
            full_key = f"{path}.{key}" if path else key
            
            # If value is a dict, merge recursively
            if isinstance(value, dict):
                current_value = self._get_value_at_path(self._config, full_key.split('.'))
                if current_value is None or not isinstance(current_value, dict):
                    self._set_value_at_path(self._config, full_key.split('.'), {})
                self._merge_defaults(value, full_key)
            else:
                # Only set if not already set
                if not self._get_value_at_path(self._config, full_key.split('.')):
                    self._set_value_at_path(self._config, full_key.split('.'), value)
    
    def _get_value_at_path(self, obj: Dict[str, Any], path: List[str]) -> Any:
        """Get a value from a nested dictionary by path."""
        if not path:
            return obj
        
        key, *rest = path
        if not isinstance(obj, dict) or key not in obj:
            return None
        
        return self._get_value_at_path(obj[key], rest)
    
    def _set_value_at_path(self, obj: Dict[str, Any], path: List[str], value: Any) -> None:
        """Set a value in a nested dictionary by path."""
        if not path:
            return
        
        key, *rest = path
        if not rest:
            obj[key] = value
            return
        
        if key not in obj:
            obj[key] = {}
        
        self._set_value_at_path(obj[key], rest, value)
    
    def _env_var_name(self, key: str) -> str:
        """Convert a configuration key to an environment variable name."""
        return f"{self.ENV_PREFIX}{key.replace('.', '_').upper()}"
    
    def get(self, key: str, default: Any = None) -> Any:
        """Get a configuration value."""
        self._ensure_loaded()
        
        # First check environment variables
        env_var = self._env_var_name(key)
        if env_var in os.environ:
            return os.environ[env_var]
        
        # Then check the loaded config
        value = self._get_value_at_path(self._config, key.split('.'))
        
        # Finally use the default
        return value if value is not None else default
    
    def get_int(self, key: str, default: int = 0) -> int:
        """Get a configuration value as an integer."""
        value = self.get(key)
        
        if value is None:
            return default
        
        try:
            return int(value)
        except (ValueError, TypeError):
            print(f"Warning: Invalid integer value for key {key}: {value}", file=sys.stderr)
            return default
    
    def get_bool(self, key: str, default: bool = False) -> bool:
        """Get a configuration value as a boolean."""
        value = self.get(key)
        
        if value is None:
            return default
        
        if isinstance(value, bool):
            return value
        
        if isinstance(value, str):
            return value.lower() in ('true', 'yes', '1', 'y', 'on')
        
        return bool(value)
    
    def get_float(self, key: str, default: float = 0.0) -> float:
        """Get a configuration value as a float."""
        value = self.get(key)
        
        if value is None:
            return default
        
        try:
            return float(value)
        except (ValueError, TypeError):
            print(f"Warning: Invalid float value for key {key}: {value}", file=sys.stderr)
            return default
    
    def get_list(self, key: str, default: Optional[List[Any]] = None) -> List[Any]:
        """Get a configuration value as a list."""
        if default is None:
            default = []
            
        value = self.get(key)
        
        if value is None:
            return default
        
        if isinstance(value, list):
            return value
        
        try:
            return [value]
        except (ValueError, TypeError):
            print(f"Warning: Invalid list value for key {key}: {value}", file=sys.stderr)
            return default
    
    def get_path(self, key: str, default: Optional[str] = None) -> str:
        """Get a configuration value as a file path with environment variables expanded."""
        value = self.get(key)
        
        if value is None:
            return os.path.expanduser(os.path.expandvars(default)) if default else ""
        
        return os.path.expanduser(os.path.expandvars(value))


# Create a singleton instance
config = RinnaConfig()

if __name__ == "__main__":
    # Print some basic configuration when the module is run directly
    print(f"Rinna Project: {config.get('project.name')} v{config.get('project.version')}")
    print(f"Environment: {config.get('project.environment')}")
    print(f"Data Directory: {config.get_path('project.data_dir')}")
    
    # Print command-line arguments if provided
    import argparse
    
    parser = argparse.ArgumentParser(description="Rinna Python Configuration Tool")
    parser.add_argument("key", nargs="?", help="Configuration key to retrieve")
    parser.add_argument("--reload", action="store_true", help="Force reload of configuration")
    
    args = parser.parse_args()
    
    if args.reload:
        config.reload()
        print("Configuration reloaded")
    
    if args.key:
        value = config.get(args.key)
        if value is not None:
            print(f"{args.key} = {value}")
        else:
            print(f"Configuration key not found: {args.key}")