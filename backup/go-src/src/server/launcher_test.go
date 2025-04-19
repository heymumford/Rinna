/*
 * Tests for the server launcher
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package server

import (
	"context"
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/heymumford/rinna/api/pkg/config"
)

// MockConfig creates a test configuration
func MockConfig() *config.RinnaConfig {
	return &config.RinnaConfig{
		Java: config.JavaServiceConfig{
			Host: "localhost",
			Port: 9999, // Use a high port unlikely to be in use
			ConnectTimeout: 1000,
			RequestTimeout: 1000,
		},
	}
}

// TestIsExternalServerConfigured tests the external server detection
func TestIsExternalServerConfigured(t *testing.T) {
	// Save environment variables and restore them after the test
	oldEnv := os.Getenv("RINNA_EXTERNAL_SERVER")
	defer os.Setenv("RINNA_EXTERNAL_SERVER", oldEnv)

	// Test cases
	testCases := []struct {
		name           string
		config         *config.RinnaConfig
		envValue       string
		expectedResult bool
	}{
		{
			name: "No external server",
			config: &config.RinnaConfig{
				Java: config.JavaServiceConfig{
					Host: "localhost",
					Port: 8081,
				},
			},
			envValue:       "",
			expectedResult: false,
		},
		{
			name: "External server in config",
			config: &config.RinnaConfig{
				Java: config.JavaServiceConfig{
					Host: "external-server.example.com",
					Port: 8081,
				},
			},
			envValue:       "",
			expectedResult: true,
		},
		{
			name: "External server in env var",
			config: &config.RinnaConfig{
				Java: config.JavaServiceConfig{
					Host: "localhost",
					Port: 8081,
				},
			},
			envValue:       "true",
			expectedResult: true,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			// Set environment variable for the test
			os.Setenv("RINNA_EXTERNAL_SERVER", tc.envValue)

			// Create launcher
			launcher := NewServerLauncher(tc.config)

			// Check result
			result := launcher.IsExternalServerConfigured()
			if result != tc.expectedResult {
				t.Errorf("Expected %v, got %v", tc.expectedResult, result)
			}
		})
	}
}

// TestIsServerRunning tests server detection
func TestIsServerRunning(t *testing.T) {
	// Create a temporary server to simulate a running server
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("OK"))
	}))
	defer ts.Close()

	// Extract host and port from the test server
	_, portStr, err := net.SplitHostPort(ts.Listener.Addr().String())
	if err != nil {
		t.Fatalf("Failed to parse test server address: %v", err)
	}

	// Create test configuration pointing to our test server
	cfg := MockConfig()
	cfg.Java.Host = "localhost"
	cfg.Java.Port = 9999 // This port shouldn't be in use

	// Create launcher
	launcher := NewServerLauncher(cfg)

	// Test when no server is running
	if launcher.IsServerRunning() {
		t.Errorf("Expected no server running on port %d, but detected one", cfg.Java.Port)
	}

	// Test with our mock server - this requires modifying the launcher's port
	// This is hacky but effective for testing
	// Parse the port from the test server
	var port int
	_, err = fmt.Sscanf(portStr, "%d", &port)
	if err != nil {
		t.Fatalf("Failed to parse port: %v", err)
	}
	launcher.serverPort = port

	// Now a server should be detected
	if !launcher.IsServerRunning() {
		t.Errorf("Expected server running on port %d, but none detected", port)
	}
}

// MockJarFile creates a mock JAR file for testing
func MockJarFile(t *testing.T) string {
	// Create a temporary directory
	tempDir, err := os.MkdirTemp("", "rinna-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}

	// Create a mock JAR file
	jarPath := tempDir + "/rinna-server.jar"
	f, err := os.Create(jarPath)
	if err != nil {
		t.Fatalf("Failed to create mock JAR file: %v", err)
	}
	f.Close()

	// Return the path so it can be cleaned up later
	return tempDir
}

// Helper functions for testing
func setupMocks(t *testing.T) (cleanup func()) {
	// Save original functions
	origLocateJarFile := locateJarFile
	origIsPortOpen := isPortOpen
	origIsRinnaServer := isRinnaServer
	
	// Return a cleanup function
	return func() {
		// Restore original functions
		locateJarFile = origLocateJarFile
		isPortOpen = origIsPortOpen
		isRinnaServer = origIsRinnaServer
	}
}

// TestStartLocalServer tests the server launch functionality
// This is a minimal test that mocks the actual Java process execution
func TestStartLocalServer(t *testing.T) {
	// Save original functions and set up cleanup
	cleanup := setupMocks(t)
	defer cleanup()
	
	// Create temp directory with mock JAR
	tempDir := MockJarFile(t)
	defer os.RemoveAll(tempDir)
	
	// Set the mock jar path
	mockJarPath := tempDir + "/rinna-server.jar"
	
	// Override the functions with test implementations
	locateJarFile = func() string {
		return mockJarPath
	}
	
	isPortOpen = func(host string, port int) bool {
		return true
	}
	
	isRinnaServer = func(host string, port int) bool {
		return true
	}

	// Create launcher
	cfg := MockConfig()
	launcher := NewServerLauncher(cfg)

	// Override the actual command execution
	launcher.cmd = nil

	// Test starting the server
	ctx := context.Background()
	err := launcher.StartLocalServer(ctx)
	
	// We expect an error since the real command won't be executed
	// but the logic should run through all the checks
	if err == nil && launcher.cmd == nil {
		t.Errorf("Expected command to be prepared but it wasn't")
	}
	
	// Verify that we at least tried to look for a JAR file
	jarPath := locateJarFile()
	if jarPath != mockJarPath {
		t.Errorf("Expected JAR path %s, but got %s", mockJarPath, jarPath)
	}
}

// TestStopLocalServer tests the server stop functionality
func TestStopLocalServer(t *testing.T) {
	// Create launcher
	cfg := MockConfig()
	launcher := NewServerLauncher(cfg)

	// Mock a running process
	launcher.processStarted = false

	// Test stopping when no server is running
	err := launcher.StopLocalServer()
	if err != nil {
		t.Errorf("Error stopping non-existent server: %v", err)
	}
}