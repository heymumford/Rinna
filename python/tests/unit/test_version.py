#!/usr/bin/env python3
"""
Unit tests for version utilities.

Copyright (c) 2025 Eric C. Mumford (@heymumford)
This file is subject to the terms and conditions defined in
the LICENSE file, which is part of this source code package.
"""

import unittest
import pytest


@pytest.mark.unit
class TestVersionUtils(unittest.TestCase):
    """Unit tests for version utilities."""

    def test_parse_version(self):
        """Test parsing a version string."""
        # This is a demo test - in a real implementation we would import a function
        version_str = "1.2.3"
        major, minor, patch = self._parse_version(version_str)
        
        self.assertEqual(major, 1)
        self.assertEqual(minor, 2)
        self.assertEqual(patch, 3)
    
    def test_invalid_version(self):
        """Test parsing an invalid version string."""
        with self.assertRaises(ValueError):
            self._parse_version("not.a.version")
    
    def test_incomplete_version(self):
        """Test parsing an incomplete version string."""
        with self.assertRaises(ValueError):
            self._parse_version("1.2")
    
    def _parse_version(self, version_str):
        """
        Parse a semantic version string.
        
        Args:
            version_str: A version string in the format "major.minor.patch"
            
        Returns:
            A tuple of (major, minor, patch) as integers
            
        Raises:
            ValueError if the version string is not valid
        """
        parts = version_str.split(".")
        if len(parts) != 3:
            raise ValueError(f"Invalid version format: {version_str}")
        
        try:
            return int(parts[0]), int(parts[1]), int(parts[2])
        except ValueError:
            raise ValueError(f"Version parts must be integers: {version_str}")


if __name__ == "__main__":
    unittest.main()