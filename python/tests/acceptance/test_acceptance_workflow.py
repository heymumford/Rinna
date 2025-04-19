#!/usr/bin/env python3
"""
Acceptance tests for workflow scenarios.

Copyright (c) 2025 Eric C. Mumford (@heymumford)
This file is subject to the terms and conditions defined in
the LICENSE file, which is part of this source code package.
"""

import unittest
from unittest.mock import MagicMock, patch

import pytest


@pytest.mark.acceptance
class TestWorkflowAcceptance(unittest.TestCase):
    """Acceptance tests for workflow scenarios."""

    def setUp(self):
        """Set up test environment."""
        print("\nSCENARIO: User manages work items through a complete workflow")

    def test_work_item_lifecycle(self):
        """Test the complete lifecycle of a work item."""
        # This test simulates a user interacting with the system through multiple steps

        print("\nGIVEN a user has logged in to the system")
        api_client = self._create_mocked_api_client()
        user_interface = UserInterface(api_client)

        print("WHEN the user creates a new work item")
        item_data = {
            "title": "Implement new feature",
            "description": "Add support for user profiles",
            "type": "FEATURE",
            "priority": "MEDIUM",
        }
        item = user_interface.create_work_item(item_data)

        print("THEN the work item is created with the correct status")
        self.assertEqual(item["status"], "FOUND")
        self.assertEqual(item["title"], "Implement new feature")

        print("AND WHEN the user triages the work item")
        updated_item = user_interface.update_work_item(
            item["id"], {"status": "TRIAGED"}
        )

        print("THEN the work item status is updated")
        self.assertEqual(updated_item["status"], "TRIAGED")

        print("AND WHEN the user assigns the work item")
        updated_item = user_interface.update_work_item(item["id"], {"assignee": "dev1"})

        print("THEN the work item is assigned correctly")
        self.assertEqual(updated_item["assignee"], "dev1")

        print("AND WHEN the developer starts working on the item")
        updated_item = user_interface.update_work_item(item["id"], {"status": "IN_DEV"})

        print("THEN the work item status is updated to in development")
        self.assertEqual(updated_item["status"], "IN_DEV")

        print("AND WHEN the developer completes the work")
        updated_item = user_interface.update_work_item(
            item["id"], {"status": "IN_TEST"}
        )

        print("THEN the work item status is updated to in testing")
        self.assertEqual(updated_item["status"], "IN_TEST")

        print("AND WHEN the tester approves the work")
        updated_item = user_interface.update_work_item(item["id"], {"status": "DONE"})

        print("THEN the work item is marked as done")
        self.assertEqual(updated_item["status"], "DONE")

    def test_release_management(self):
        """Test the release management process."""
        print("\nSCENARIO: User creates and manages a release")

        print("\nGIVEN a user has logged in and several work items exist")
        api_client = self._create_mocked_api_client()
        user_interface = UserInterface(api_client)

        # In a real test, we would create actual work items
        # For this example, we'll use the mocked responses

        print("WHEN the user creates a new release")
        release_data = {"name": "1.0.0", "description": "Initial release"}
        release = user_interface.create_release(release_data)

        print("THEN the release is created successfully")
        self.assertEqual(release["name"], "1.0.0")
        self.assertEqual(release["status"], "PLANNED")

        print("AND WHEN the user adds work items to the release")
        updated_release = user_interface.add_items_to_release(
            release["id"], ["123", "124"]
        )

        print("THEN the work items are added to the release")
        self.assertEqual(len(updated_release["items"]), 2)

        print("AND WHEN the user starts the release")
        updated_release = user_interface.update_release(
            release["id"], {"status": "IN_PROGRESS"}
        )

        print("THEN the release status is updated")
        self.assertEqual(updated_release["status"], "IN_PROGRESS")

    def _create_mocked_api_client(self):
        """Create a mocked API client for testing."""
        client = MagicMock()

        # Mock responses for work item operations
        client.create_work_item.return_value = {
            "id": "123",
            "title": "Implement new feature",
            "description": "Add support for user profiles",
            "type": "FEATURE",
            "priority": "MEDIUM",
            "status": "FOUND",
        }

        # Mock update work item to return the updated item
        def update_work_item(item_id, data):
            response = {
                "id": item_id,
                "title": "Implement new feature",
                "description": "Add support for user profiles",
                "type": "FEATURE",
                "priority": "MEDIUM",
            }
            response.update(data)
            return response

        client.update_work_item.side_effect = update_work_item

        # Mock responses for release operations
        client.create_release.return_value = {
            "id": "rel-1",
            "name": "1.0.0",
            "description": "Initial release",
            "status": "PLANNED",
            "items": [],
        }

        # Mock update release to return the updated release
        def update_release(release_id, data):
            response = {
                "id": release_id,
                "name": "1.0.0",
                "description": "Initial release",
                "items": [],
            }
            response.update(data)
            return response

        client.update_release.side_effect = update_release

        # Mock adding items to a release
        def add_items_to_release(release_id, item_ids):
            return {
                "id": release_id,
                "name": "1.0.0",
                "description": "Initial release",
                "status": "PLANNED",
                "items": [{"id": id} for id in item_ids],
            }

        client.add_items_to_release.side_effect = add_items_to_release

        return client


class UserInterface:
    """
    Simulates a user interface for the application.

    In a real implementation, this would be a separate component.
    """

    def __init__(self, api_client):
        """Initialize the user interface."""
        self.api_client = api_client

    def create_work_item(self, data):
        """Create a new work item."""
        return self.api_client.create_work_item(data)

    def update_work_item(self, item_id, data):
        """Update a work item."""
        return self.api_client.update_work_item(item_id, data)

    def create_release(self, data):
        """Create a new release."""
        return self.api_client.create_release(data)

    def update_release(self, release_id, data):
        """Update a release."""
        return self.api_client.update_release(release_id, data)

    def add_items_to_release(self, release_id, item_ids):
        """Add work items to a release."""
        return self.api_client.add_items_to_release(release_id, item_ids)


if __name__ == "__main__":
    unittest.main()
