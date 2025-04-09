"""
Pytest configuration for the Rinna project.

Registers custom markers and fixtures for testing.
"""

import pytest


def pytest_configure(config):
    """Register custom pytest markers."""
    config.addinivalue_line("markers", "unit: mark a test as a unit test")
    config.addinivalue_line(
        "markers", "component: mark a test as a component test"
    )
    config.addinivalue_line(
        "markers", "integration: mark a test as an integration test"
    )
    config.addinivalue_line(
        "markers", "acceptance: mark a test as an acceptance test"
    )
    config.addinivalue_line(
        "markers", "performance: mark a test as a performance test"
    )
    config.addinivalue_line(
        "markers", "benchmark: mark a test for performance benchmarking"
    )
