#!/usr/bin/env python3
"""
Integration tests for API interactions.

Copyright (c) 2025 Eric C. Mumford (@heymumford)
This file is subject to the terms and conditions defined in
the LICENSE file, which is part of this source code package.
"""

import json
import unittest
from unittest.mock import MagicMock, patch

import pytest


@pytest.mark.integration
class TestAPIIntegration(unittest.TestCase):
    """Integration tests for API interactions."""

    def setUp(self):
        """Set up test environment."""
        # In a real test, we might set up a test API server
        # For this example, we'll mock the HTTP client
        self.mock_response = MagicMock()
        self.mock_response.status_code = 200
        self.mock_response.json.return_value = {"status": "ok"}

    def test_api_client_integration(self):
        """Test API client integration with the API server."""
        # In a real test, we would import a real API client
        # For this example, we'll create a simple client implementation

        # Mock the requests library
        with patch("requests.get", return_value=self.mock_response):
            # Create an API client
            client = APIClient("http://localhost:8080", "test-token")

            # Call the API
            response = client.get_health()

            # Verify the response
            self.assertEqual(response["status"], "ok")

    def test_api_workflow_integration(self):
        """Test an API workflow involving multiple components."""
        # In this test, we integrate several components:
        # - API client
        # - Authentication
        # - Response handling

        # Set up mock responses for a sequence of API calls
        mock_responses = [
            # Authentication response
            MagicMock(status_code=200, json=lambda: {"token": "test-token"}),
            # Create item response
            MagicMock(
                status_code=201, json=lambda: {"id": "123", "title": "Test Item"}
            ),
            # Update item response
            MagicMock(
                status_code=200,
                json=lambda: {"id": "123", "title": "Test Item", "status": "DONE"},
            ),
        ]

        # Mock the requests library to return the sequence of responses
        with patch("requests.post") as mock_post, patch("requests.put") as mock_put:
            mock_post.side_effect = mock_responses[:2]
            mock_put.return_value = mock_responses[2]

            # Create a workflow component that uses the API client
            workflow = WorkflowComponent("http://localhost:8080")

            # Execute the workflow
            result = workflow.create_and_complete_item("Test Item")

            # Verify the workflow completed successfully
            self.assertEqual(result["status"], "DONE")
            self.assertEqual(result["id"], "123")


class APIClient:
    """
    Simple API client for testing.

    In a real implementation, this would be a separate component.
    """

    def __init__(self, base_url, token=None):
        """Initialize the API client."""
        self.base_url = base_url
        self.token = token

    def get_health(self):
        """Get health status from the API."""
        import requests

        headers = {}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"

        response = requests.get(f"{self.base_url}/health", headers=headers)
        response.raise_for_status()
        return response.json()


class WorkflowComponent:
    """
    Component that implements a workflow using the API.

    In a real implementation, this would be a separate component.
    """

    def __init__(self, api_url):
        """Initialize the workflow component."""
        self.api_url = api_url
        self.token = None

    def _authenticate(self):
        """Authenticate with the API."""
        import requests

        response = requests.post(
            f"{self.api_url}/auth", json={"username": "test", "password": "password"}
        )
        response.raise_for_status()
        self.token = response.json()["token"]

    def _create_item(self, title):
        """Create a new item via the API."""
        import requests

        headers = {"Authorization": f"Bearer {self.token}"}
        response = requests.post(
            f"{self.api_url}/items", headers=headers, json={"title": title}
        )
        response.raise_for_status()
        return response.json()

    def _complete_item(self, item_id):
        """Mark an item as complete via the API."""
        import requests

        headers = {"Authorization": f"Bearer {self.token}"}
        response = requests.put(
            f"{self.api_url}/items/{item_id}", headers=headers, json={"status": "DONE"}
        )
        response.raise_for_status()
        return response.json()

    def create_and_complete_item(self, title):
        """Create an item and mark it as complete."""
        self._authenticate()
        item = self._create_item(title)
        return self._complete_item(item["id"])


if __name__ == "__main__":
    unittest.main()
