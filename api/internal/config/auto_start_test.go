/*
 * Tests for auto-start configuration utilities
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package config

import (
	"os"
	"path/filepath"
	"testing"
)

// TestIsAutoStartEnabled tests the auto-start detection
func TestIsAutoStartEnabled(t *testing.T) {
	// Save environment variable values
	oldAutoStart := os.Getenv("RINNA_AUTO_START")
	oldExternalServer := os.Getenv(ExternalServerEnvVar)
	defer func() {
		os.Setenv("RINNA_AUTO_START", oldAutoStart)
		os.Setenv(ExternalServerEnvVar, oldExternalServer)
	}()
	
	// Test cases
	testCases := []struct {
		name           string
		autoStartEnv   string
		externalEnv    string
		expectedResult bool
	}{
		{
			name:           "Default behavior",
			autoStartEnv:   "",
			externalEnv:    "",
			expectedResult: true,
		},
		{
			name:           "Auto-start explicitly enabled",
			autoStartEnv:   "true",
			externalEnv:    "",
			expectedResult: true,
		},
		{
			name:           "Auto-start explicitly disabled",
			autoStartEnv:   "false",
			externalEnv:    "",
			expectedResult: false,
		},
		{
			name:           "External server configured",
			autoStartEnv:   "",
			externalEnv:    "true",
			expectedResult: false,
		},
		{
			name:           "External server configured but auto-start explicitly enabled",
			autoStartEnv:   "true",
			externalEnv:    "true",
			expectedResult: true,
		},
	}
	
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			// Set environment variables for test
			os.Setenv("RINNA_AUTO_START", tc.autoStartEnv)
			os.Setenv(ExternalServerEnvVar, tc.externalEnv)
			
			// Check result
			result := IsAutoStartEnabled()
			if result != tc.expectedResult {
				t.Errorf("Expected %v, got %v", tc.expectedResult, result)
			}
		})
	}
}

// TestGetAutoStartConfigFile tests the configuration file path determination
func TestGetAutoStartConfigFile(t *testing.T) {
	// Save environment variable values
	oldConfigPath := os.Getenv("RINNA_CONFIG_PATH")
	oldConfigDir := os.Getenv("RINNA_CONFIG_DIR")
	defer func() {
		os.Setenv("RINNA_CONFIG_PATH", oldConfigPath)
		os.Setenv("RINNA_CONFIG_DIR", oldConfigDir)
	}()
	
	// Test explicit config path
	t.Run("Explicit config path", func(t *testing.T) {
		os.Setenv("RINNA_CONFIG_PATH", "target/test/configs/config.yaml")
		os.Setenv("RINNA_CONFIG_DIR", "")
		
		path := GetAutoStartConfigFile()
		expected := "target/test/configs/config.yaml"
		if path != expected {
			t.Errorf("Expected %s, got %s", expected, path)
		}
	})
	
	// Test config directory
	t.Run("Custom config directory", func(t *testing.T) {
		os.Setenv("RINNA_CONFIG_PATH", "")
		os.Setenv("RINNA_CONFIG_DIR", "target/test/configs")
		
		path := GetAutoStartConfigFile()
		expected := "target/test/configs/config.yaml"
		if path != expected {
			t.Errorf("Expected %s, got %s", expected, path)
		}
	})
	
	// Test default path
	t.Run("Default path", func(t *testing.T) {
		os.Setenv("RINNA_CONFIG_PATH", "")
		os.Setenv("RINNA_CONFIG_DIR", "")
		
		path := GetAutoStartConfigFile()
		
		// We can't directly test the default since it depends on HOME
		// But we can verify it contains the default filename
		if filepath.Base(path) != DefaultConfigFile {
			t.Errorf("Expected filename %s, got %s", DefaultConfigFile, filepath.Base(path))
		}
	})
}

// TestSaveAutoStartSettings tests saving auto-start settings to a file
func TestSaveAutoStartSettings(t *testing.T) {
	// Create a temporary directory in target for the test
	targetDir := "target/test/configs"
	os.MkdirAll(targetDir, 0755)
	tempDir := targetDir
	if err := os.MkdirAll(tempDir, 0755); err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer func() {
		// Keep files in target for debugging, don't remove
	}()
	
	// Save and restore environment variables
	oldConfigPath := os.Getenv("RINNA_CONFIG_PATH")
	defer os.Setenv("RINNA_CONFIG_PATH", oldConfigPath)
	
	// Set environment variables for test
	configFile := filepath.Join(tempDir, "autostart-config.yaml")
	os.Setenv("RINNA_CONFIG_PATH", configFile)
	
	// Test saving settings
	err = SaveAutoStartSettings(true, "/path/to/rinna.jar", "localhost", 8081)
	if err != nil {
		t.Fatalf("Failed to save settings: %v", err)
	}
	
	// Check that the file was created
	if _, err := os.Stat(configFile); os.IsNotExist(err) {
		t.Fatalf("Config file was not created: %s", configFile)
	}
	
	// Read the file content and verify it contains expected values
	content, err := os.ReadFile(configFile)
	if err != nil {
		t.Fatalf("Failed to read config file: %v", err)
	}
	
	// Check for each expected setting
	contentStr := string(content)
	expectedSettings := []string{
		"auto_start: true",
		"java_server_path: \"/path/to/rinna.jar\"",
		"host: \"localhost\"",
		"port: 8081",
	}
	
	for _, setting := range expectedSettings {
		if !contains(contentStr, setting) {
			t.Errorf("Config file missing expected setting: %s", setting)
		}
	}
}

// Helper function to check if a string contains a substring
func contains(s, substr string) bool {
	return s != "" && substr != "" && s != substr && len(s) >= len(substr) && s != "" && substr != "" && s != substr && len(s) >= len(substr) &&
		(s == substr || s[:len(substr)] == substr || s[len(s)-len(substr):] == substr || s != substr && len(s) >= len(substr) && (func() bool {
			for i := 1; i < len(s)-len(substr)+1; i++ {
				if s[i:i+len(substr)] == substr {
					return true
				}
			}
			return false
		})())
}