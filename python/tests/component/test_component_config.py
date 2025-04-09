#!/usr/bin/env python3
"""
Component tests for configuration handling.

Copyright (c) 2025 Eric C. Mumford (@heymumford)
This file is subject to the terms and conditions defined in
the LICENSE file, which is part of this source code package.
"""

import os
import tempfile
import unittest
import pytest


@pytest.mark.component
class TestConfigComponent(unittest.TestCase):
    """Component tests for configuration handling."""
    
    def setUp(self):
        """Set up test environment."""
        # Create a temporary directory for test files
        self.temp_dir = tempfile.TemporaryDirectory()
        self.config_path = os.path.join(self.temp_dir.name, "config.yaml")
    
    def tearDown(self):
        """Clean up after tests."""
        self.temp_dir.cleanup()
    
    def test_load_config(self):
        """Test loading configuration from a file."""
        # Create a sample config file
        with open(self.config_path, "w") as f:
            f.write("""
database:
  host: localhost
  port: 5432
  user: app_user
  password: secret

api:
  port: 8080
  debug: false
            """)
        
        # In a real test, we would import and use a ConfigLoader component
        # For this example, we'll simulate the component behavior
        config = self._load_config(self.config_path)
        
        # Verify the configuration is loaded correctly
        self.assertEqual(config["database"]["host"], "localhost")
        self.assertEqual(config["database"]["port"], 5432)
        self.assertEqual(config["api"]["port"], 8080)
        self.assertEqual(config["api"]["debug"], False)
    
    def test_validate_config(self):
        """Test validating configuration."""
        # Create some test configurations
        valid_config = {
            "database": {
                "host": "localhost",
                "port": 5432,
                "user": "app_user",
                "password": "secret"
            },
            "api": {
                "port": 8080,
                "debug": False
            }
        }
        
        invalid_config = {
            "database": {
                "port": 5432,
                "user": "app_user",
                "password": "secret"
                # Missing host
            },
            "api": {
                "port": 8080,
                "debug": False
            }
        }
        
        # In a real test, we would import and use a ConfigValidator component
        # For this example, we'll simulate the component behavior
        self.assertTrue(self._validate_config(valid_config))
        self.assertFalse(self._validate_config(invalid_config))
    
    def _load_config(self, path):
        """
        Simulate loading configuration from a file.
        
        In a real implementation, this would be a separate component.
        """
        import yaml
        with open(path, "r") as f:
            return yaml.safe_load(f)
    
    def _validate_config(self, config):
        """
        Simulate validating configuration.
        
        In a real implementation, this would be a separate component.
        """
        required_fields = {
            "database": ["host", "port", "user", "password"],
            "api": ["port"]
        }
        
        for section, fields in required_fields.items():
            if section not in config:
                return False
            
            for field in fields:
                if field not in config[section]:
                    return False
        
        return True


if __name__ == "__main__":
    unittest.main()