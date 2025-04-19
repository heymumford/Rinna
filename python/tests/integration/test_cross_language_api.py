import json
import os
import unittest
import uuid
from datetime import datetime

import requests


class TestCrossLanguageApi(unittest.TestCase):
    """Test Python integration with Go API server."""

    @classmethod
    def setUpClass(cls):
        """Set up test environment once for all tests."""
        # Get API port from environment or use default
        cls.api_port = os.environ.get("RINNA_TEST_API_PORT", "8085")
        cls.api_url = f"http://localhost:{cls.api_port}/api"

        # Check if API is available
        try:
            response = requests.get(f"{cls.api_url}/health")
            if response.status_code != 200:
                raise unittest.SkipTest(
                    f"API server not available: status {response.status_code}"
                )
        except Exception as e:
            raise unittest.SkipTest(f"API server not available: {str(e)}")

        # Test item created during tests (for cleanup)
        cls.test_item_id = None

    @classmethod
    def tearDownClass(cls):
        """Clean up after all tests."""
        # Clean up test item if created
        if cls.test_item_id:
            try:
                requests.delete(f"{cls.api_url}/workitems/{cls.test_item_id}")
            except Exception as e:
                print(f"Warning: Failed to clean up test item: {str(e)}")

    def test_01_create_work_item_from_python(self):
        """Test creating a work item via Python client to Go API."""
        # Create unique test data
        test_id = str(uuid.uuid4())[:8]

        # Prepare work item data
        work_item = {
            "title": f"Python-Go test item {test_id}",
            "type": "FEATURE",
            "priority": "MEDIUM",
            "description": "Created by Python test",
        }

        # Send request to API
        response = requests.post(
            f"{self.api_url}/workitems",
            json=work_item,
            headers={"Content-Type": "application/json"},
        )

        # Verify response
        self.assertEqual(
            201,
            response.status_code,
            f"Expected 201 Created, got {response.status_code}",
        )

        # Save item ID for later tests
        response_data = response.json()
        self.assertIn("id", response_data, "Response should contain ID")
        self.__class__.test_item_id = response_data["id"]

    def test_02_retrieve_work_item_with_python(self):
        """Test retrieving a work item created earlier."""
        # Skip if no test item was created
        if not self.__class__.test_item_id:
            self.skipTest("No test item available to retrieve")

        # Retrieve the item
        response = requests.get(
            f"{self.api_url}/workitems/{self.__class__.test_item_id}"
        )

        # Verify response
        self.assertEqual(
            200, response.status_code, f"Expected 200 OK, got {response.status_code}"
        )

        # Verify item data
        item = response.json()
        self.assertEqual(self.__class__.test_item_id, item["id"], "ID should match")
        self.assertTrue(
            item["title"].startswith("Python-Go test item"), "Title should match"
        )
        self.assertEqual("FEATURE", item["type"], "Type should match")
        self.assertEqual("MEDIUM", item["priority"], "Priority should match")
        self.assertEqual(
            "Created by Python test", item["description"], "Description should match"
        )

    def test_03_update_work_item_from_python(self):
        """Test updating a work item via Python client."""
        # Skip if no test item was created
        if not self.__class__.test_item_id:
            self.skipTest("No test item available to update")

        # Prepare update data
        update_data = {
            "state": "IN_PROGRESS",
            "description": f"Updated by Python test at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        }

        # Send update request
        response = requests.put(
            f"{self.api_url}/workitems/{self.__class__.test_item_id}",
            json=update_data,
            headers={"Content-Type": "application/json"},
        )

        # Verify response
        self.assertEqual(
            200, response.status_code, f"Expected 200 OK, got {response.status_code}"
        )

        # Verify update was applied
        get_response = requests.get(
            f"{self.api_url}/workitems/{self.__class__.test_item_id}"
        )
        updated_item = get_response.json()

        self.assertEqual(
            "IN_PROGRESS", updated_item["state"], "State should be updated"
        )
        self.assertTrue(
            "Updated by Python test at" in updated_item["description"],
            "Description should be updated",
        )

    def test_04_search_work_items_from_python(self):
        """Test searching for work items with Python client."""
        # Skip if no test item was created
        if not self.__class__.test_item_id:
            self.skipTest("No test item available for search test")

        # Search for test item by type
        response = requests.get(f"{self.api_url}/workitems?type=FEATURE")

        # Verify response
        self.assertEqual(
            200, response.status_code, f"Expected 200 OK, got {response.status_code}"
        )

        # Verify search results
        items = response.json()
        self.assertIsInstance(items, list, "Response should be a list")

        # Find our test item in results
        found = False
        for item in items:
            if item["id"] == self.__class__.test_item_id:
                found = True
                break

        self.assertTrue(found, "Test item should be found in search results")

    def test_05_validate_error_handling(self):
        """Test error handling between Python client and Go API."""
        # Test 404 for non-existent item
        response = requests.get(f"{self.api_url}/workitems/non-existent-id")
        self.assertEqual(
            404, response.status_code, "Should return 404 for non-existent item"
        )

        # Verify error response structure
        error_data = response.json()
        self.assertIn(
            "error", error_data, "Error response should contain 'error' field"
        )

        # Test validation error for invalid item
        invalid_item = {"type": "INVALID_TYPE"}  # Missing required fields

        response = requests.post(
            f"{self.api_url}/workitems",
            json=invalid_item,
            headers={"Content-Type": "application/json"},
        )

        self.assertGreaterEqual(
            response.status_code, 400, "Should return error status for invalid item"
        )

        # Verify error response structure
        error_data = response.json()
        self.assertIn(
            "error", error_data, "Error response should contain 'error' field"
        )


if __name__ == "__main__":
    unittest.main()
