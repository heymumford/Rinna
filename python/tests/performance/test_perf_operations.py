#!/usr/bin/env python3
"""
Performance tests for various operations.

Copyright (c) 2025 Eric C. Mumford (@heymumford)
This file is subject to the terms and conditions defined in
the LICENSE file, which is part of this source code package.
"""

import time
import unittest
import pytest


@pytest.mark.performance
class TestPerformanceOperations(unittest.TestCase):
    """Performance tests for various operations."""
    
    def setUp(self):
        """Set up test environment."""
        # Create test data
        self.test_data = [
            {
                "id": i,
                "title": f"Item {i}",
                "description": f"This is a test item {i}",
                "tags": ["tag1", "tag2", "keyword"] if i % 3 == 0 else ["tag1"],
                "status": "FOUND"
            }
            for i in range(10000)
        ]
    
    @pytest.mark.benchmark
    def test_search_performance(self):
        """Test search performance."""
        # Define test cases with expected result counts
        test_cases = [
            {"keyword": "Item 1", "expected_count": 1111},  # Exact match appears in multiple titles
            {"keyword": "Item", "expected_count": 10000},
            {"keyword": "test", "expected_count": 10000},
            {"keyword": "keyword", "expected_count": 3334},  # Every third item
            {"keyword": "nonexistent", "expected_count": 0}
        ]
        
        # Benchmark each test case
        for tc in test_cases:
            # Measure execution time
            start_time = time.time()
            results = self._search_items(self.test_data, tc["keyword"])
            end_time = time.time()
            
            # Verify results
            self.assertEqual(len(results), tc["expected_count"], 
                             f"Expected {tc['expected_count']} results for '{tc['keyword']}', got {len(results)}")
            
            # Print performance metrics
            elapsed = end_time - start_time
            print(f"\nSearch for '{tc['keyword']}' took {elapsed:.6f} seconds")
            
            # Assert performance is within acceptable limits
            # This is an arbitrary threshold and should be adjusted based on actual performance
            self.assertLess(elapsed, 0.1, f"Search for '{tc['keyword']}' took too long: {elapsed:.6f} seconds")
    
    @pytest.mark.benchmark
    def test_filter_performance(self):
        """Test filter performance."""
        # Define test cases with expected result counts
        test_cases = [
            {"status": "FOUND", "expected_count": 10000},
            {"tags": ["tag1"], "expected_count": 10000},
            {"tags": ["keyword"], "expected_count": 3334},  # Every third item
            {"status": "DONE", "expected_count": 0}
        ]
        
        # Benchmark each test case
        for tc in test_cases:
            # Measure execution time
            start_time = time.time()
            
            if "status" in tc:
                results = self._filter_by_status(self.test_data, tc["status"])
            elif "tags" in tc:
                results = self._filter_by_tags(self.test_data, tc["tags"])
            
            end_time = time.time()
            
            # Verify results
            criteria = tc.get("status", "") or tc.get("tags", [])
            self.assertEqual(len(results), tc["expected_count"], 
                             f"Expected {tc['expected_count']} results for filter {criteria}, got {len(results)}")
            
            # Print performance metrics
            elapsed = end_time - start_time
            print(f"\nFilter by {criteria} took {elapsed:.6f} seconds")
            
            # Assert performance is within acceptable limits
            self.assertLess(elapsed, 0.1, f"Filter by {criteria} took too long: {elapsed:.6f} seconds")
    
    @pytest.mark.benchmark
    def test_sort_performance(self):
        """Test sorting performance."""
        # Define test cases
        test_cases = [
            {"field": "id", "reverse": False},
            {"field": "id", "reverse": True},
            {"field": "title", "reverse": False},
            {"field": "title", "reverse": True}
        ]
        
        # Benchmark each test case
        for tc in test_cases:
            # Measure execution time
            start_time = time.time()
            results = self._sort_items(self.test_data, tc["field"], tc["reverse"])
            end_time = time.time()
            
            # Verify results
            self.assertEqual(len(results), len(self.test_data), 
                             f"Expected {len(self.test_data)} results after sorting, got {len(results)}")
            
            # Check that sorting was done correctly
            if len(results) > 1:
                if tc["reverse"]:
                    self.assertGreaterEqual(results[0][tc["field"]], results[1][tc["field"]])
                else:
                    self.assertLessEqual(results[0][tc["field"]], results[1][tc["field"]])
            
            # Print performance metrics
            elapsed = end_time - start_time
            print(f"\nSort by {tc['field']} (reverse={tc['reverse']}) took {elapsed:.6f} seconds")
            
            # Assert performance is within acceptable limits
            self.assertLess(elapsed, 0.2, f"Sort by {tc['field']} took too long: {elapsed:.6f} seconds")
    
    def _search_items(self, items, keyword):
        """
        Search items by keyword.
        
        This is a simple implementation for testing purposes.
        In a real application, this would be more sophisticated.
        """
        results = []
        for item in items:
            if (keyword.lower() in item["title"].lower() or
                keyword.lower() in item["description"].lower() or
                ("tags" in item and any(keyword.lower() in tag.lower() for tag in item["tags"]))):
                results.append(item)
        return results
    
    def _filter_by_status(self, items, status):
        """Filter items by status."""
        return [item for item in items if item.get("status") == status]
    
    def _filter_by_tags(self, items, tags):
        """Filter items by tags."""
        return [item for item in items if "tags" in item and any(tag in item["tags"] for tag in tags)]
    
    def _sort_items(self, items, field, reverse=False):
        """Sort items by a field."""
        return sorted(items, key=lambda x: x.get(field, ""), reverse=reverse)


if __name__ == "__main__":
    unittest.main()