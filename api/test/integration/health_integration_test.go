/*
 * Integration tests for health check API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package integration

import (
	"context"
	"encoding/json"
	"net/http"
	"os"
	"os/exec"
	"testing"
	"time"
)

// TestHealthEndpoints tests the health check API
func TestHealthEndpoints(t *testing.T) {
	// Skip if environment variable is not set
	if os.Getenv("RUN_INTEGRATION_TESTS") != "true" {
		t.Skip("Skipping integration test. Set RUN_INTEGRATION_TESTS=true to run")
	}

	// Start the server (on a different port to avoid conflicts)
	port := "8081"
	cmd := exec.Command("../../bin/healthcheck", "-port", port)
	err := cmd.Start()
	if err != nil {
		t.Fatalf("Failed to start server: %v", err)
	}
	defer cmd.Process.Kill()

	// Give the server time to start
	time.Sleep(1 * time.Second)

	// Create a client with a timeout
	client := &http.Client{
		Timeout: 5 * time.Second,
	}

	// Test cases
	testCases := []struct {
		name           string
		endpoint       string
		expectedStatus int
	}{
		{
			name:           "Health endpoint",
			endpoint:       "/health",
			expectedStatus: http.StatusOK,
		},
		{
			name:           "Liveness endpoint",
			endpoint:       "/health/live",
			expectedStatus: http.StatusOK,
		},
		{
			name:           "Readiness endpoint",
			endpoint:       "/health/ready",
			expectedStatus: http.StatusOK,
		},
	}

	// Run tests
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			// Create a new request with context
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()

			req, err := http.NewRequestWithContext(ctx, "GET", "http://localhost:"+port+tc.endpoint, nil)
			if err != nil {
				t.Fatalf("Failed to create request: %v", err)
			}

			// Send the request
			resp, err := client.Do(req)
			if err != nil {
				t.Fatalf("Failed to send request: %v", err)
			}
			defer resp.Body.Close()

			// Check the status code
			if resp.StatusCode != tc.expectedStatus {
				t.Errorf("Expected status code %d, got %d", tc.expectedStatus, resp.StatusCode)
			}

			// Check the content type
			contentType := resp.Header.Get("Content-Type")
			if contentType != "application/json" {
				t.Errorf("Expected content type application/json, got %s", contentType)
			}

			// Decode the response
			var body map[string]interface{}
			if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
				t.Fatalf("Failed to decode response: %v", err)
			}

			// Check for status field
			status, ok := body["status"].(string)
			if !ok {
				t.Errorf("Response does not contain status field or it's not a string")
			} else if status != "ok" {
				t.Errorf("Expected status to be 'ok', got '%s'", status)
			}
		})
	}
}

// TestHealthEndpointsManually is a manual test that can be run separately
// to check the health endpoints without auto-starting the server
func TestHealthEndpointsManually(t *testing.T) {
	// Skip by default
	if os.Getenv("RUN_MANUAL_TESTS") != "true" {
		t.Skip("Skipping manual test. Set RUN_MANUAL_TESTS=true to run")
	}

	// Create a client with a timeout
	client := &http.Client{
		Timeout: 5 * time.Second,
	}

	// Test the health endpoint
	resp, err := client.Get("http://localhost:8080/health")
	if err != nil {
		t.Fatalf("Failed to send request: %v", err)
	}
	defer resp.Body.Close()

	// Check the status code
	if resp.StatusCode != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
	}

	// Check the content type
	contentType := resp.Header.Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("Expected content type application/json, got %s", contentType)
	}

	// Decode the response
	var body map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		t.Fatalf("Failed to decode response: %v", err)
	}

	// Print the response for manual inspection
	t.Logf("Response: %v", body)
}