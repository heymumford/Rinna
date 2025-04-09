// +build unit

/*
 * Unit tests for version utilities
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package unit

import (
	"fmt"
	"testing"
)

// Test parsing a version string
func TestParseVersion(t *testing.T) {
	// Test cases
	testCases := []struct {
		name          string
		versionString string
		expectedMajor int
		expectedMinor int
		expectedPatch int
		expectError   bool
	}{
		{
			name:          "Valid version",
			versionString: "1.2.3",
			expectedMajor: 1,
			expectedMinor: 2,
			expectedPatch: 3,
			expectError:   false,
		},
		{
			name:          "Zero version",
			versionString: "0.0.0",
			expectedMajor: 0,
			expectedMinor: 0,
			expectedPatch: 0,
			expectError:   false,
		},
		{
			name:          "Invalid version",
			versionString: "not.a.version",
			expectError:   true,
		},
		{
			name:          "Missing parts",
			versionString: "1.2",
			expectError:   true,
		},
	}

	// Execute tests
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			// This is a simplified example - in a real test, we would call an actual function
			major, minor, patch, err := parseVersionSimple(tc.versionString)

			// Check error expectation
			if tc.expectError && err == nil {
				t.Error("Expected an error but got none")
			}
			if !tc.expectError && err != nil {
				t.Errorf("Did not expect an error but got: %v", err)
			}

			// Only check version components if no error was expected
			if !tc.expectError {
				if major != tc.expectedMajor {
					t.Errorf("Expected major version %d, got %d", tc.expectedMajor, major)
				}
				if minor != tc.expectedMinor {
					t.Errorf("Expected minor version %d, got %d", tc.expectedMinor, minor)
				}
				if patch != tc.expectedPatch {
					t.Errorf("Expected patch version %d, got %d", tc.expectedPatch, patch)
				}
			}
		})
	}
}

// Simple function to parse a version string
// In a real implementation, this would be in a separate package
func parseVersionSimple(version string) (int, int, int, error) {
	var major, minor, patch int
	_, err := fmt.Sscanf(version, "%d.%d.%d", &major, &minor, &patch)
	if err != nil {
		return 0, 0, 0, fmt.Errorf("failed to parse version: %w", err)
	}
	return major, minor, patch, nil
}