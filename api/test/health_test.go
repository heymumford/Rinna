/*
 * Integration tests for health endpoints
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/pkg/health"
)

// MockJavaChecker is a mock implementation of health.DependencyChecker
type MockJavaChecker struct {
	status  string
	message string
}

// CheckHealth returns a predefined status
func (m *MockJavaChecker) CheckHealth() health.ServiceStatus {
	return health.ServiceStatus{
		Status:    m.status,
		Message:   m.message,
		Timestamp: time.Now().Format(time.RFC3339),
	}
}

// TestHealthIntegration tests the health endpoint in an integration-like manner
func TestHealthIntegration(t *testing.T) {
	// Create a router for testing
	router := mux.NewRouter()
	
	// Create a health handler with a mock checker that always returns ok
	mockChecker := &MockJavaChecker{status: "ok"}
	healthHandler := health.NewHandler(mockChecker)
	healthHandler.RegisterRoutes(router)

	// Start a test server
	server := httptest.NewServer(router)
	defer server.Close()

	// Test cases
	testCases := []struct {
		name           string
		endpoint       string
		expectedStatus int
	}{
		{
			name:           "Health check",
			endpoint:       "/health",
			expectedStatus: http.StatusOK,
		},
		{
			name:           "Liveness check",
			endpoint:       "/health/live",
			expectedStatus: http.StatusOK,
		},
		{
			name:           "Readiness check",
			endpoint:       "/health/ready",
			expectedStatus: http.StatusOK,
		},
	}

	// Run the tests
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			// Make a request to the test server
			resp, err := http.Get(server.URL + tc.endpoint)
			if err != nil {
				t.Fatalf("Failed to make request: %v", err)
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

			// Decode the response body
			var body map[string]interface{}
			if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
				t.Fatalf("Failed to decode response body: %v", err)
			}

			// Check that the status field exists and is "ok"
			status, ok := body["status"].(string)
			if !ok {
				t.Errorf("Response does not contain status field or it's not a string")
			} else if status != "ok" {
				t.Errorf("Expected status to be 'ok', got '%s'", status)
			}

			// Check that the timestamp field exists
			if _, ok := body["timestamp"].(string); !ok {
				t.Errorf("Response does not contain timestamp field or it's not a string")
			}
			
			// Specific checks for the /health endpoint
			if tc.endpoint == "/health" {
				// Check version info fields
				requiredFields := []string{"version", "commitSha", "buildTime", "goVersion", "hostname"}
				for _, field := range requiredFields {
					if _, ok := body[field].(string); !ok {
						t.Errorf("Health response missing required field: %s", field)
					}
				}
				
				// Check environment
				if env, ok := body["environment"].(string); !ok || env == "" {
					t.Errorf("Health response missing or empty environment field")
				}
				
				// Check uptime
				if uptime, ok := body["uptime"].(string); !ok || uptime == "" {
					t.Errorf("Health response missing or empty uptime field")
				}
				
				// Check memory stats
				memoryStats, ok := body["memory"].(map[string]interface{})
				if !ok {
					t.Errorf("Health response missing memory stats")
				} else {
					memoryFields := []string{"alloc", "totalAlloc", "sys", "numGC"}
					for _, field := range memoryFields {
						if _, ok := memoryStats[field]; !ok {
							t.Errorf("Memory stats missing required field: %s", field)
						}
					}
				}
			}
		})
	}
}

// TestNegativeReadinessIntegration tests the readiness endpoint with a failing dependency
func TestNegativeReadinessIntegration(t *testing.T) {
	// Create a router for testing
	router := mux.NewRouter()
	
	// Create a health handler with a mock checker that returns an error
	mockChecker := &MockJavaChecker{
		status: "error",
		message: "Java service is down",
	}
	healthHandler := health.NewHandler(mockChecker)
	healthHandler.RegisterRoutes(router)

	// Create a request to the readiness endpoint
	req, err := http.NewRequest(http.MethodGet, "/health/ready", nil)
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}

	// Create a response recorder
	rr := httptest.NewRecorder()

	// Serve the request
	router.ServeHTTP(rr, req)

	// Check the status code
	if rr.Code != http.StatusServiceUnavailable {
		t.Errorf("Expected status code %d, got %d", http.StatusServiceUnavailable, rr.Code)
	}

	// Check the content type
	contentType := rr.Header().Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("Expected content type application/json, got %s", contentType)
	}

	// Decode the response body
	var body map[string]interface{}
	if err := json.NewDecoder(rr.Body).Decode(&body); err != nil {
		t.Fatalf("Failed to decode response body: %v", err)
	}

	// Check that the status field is "degraded"
	status, ok := body["status"].(string)
	if !ok {
		t.Errorf("Response does not contain status field or it's not a string")
	} else if status != "degraded" {
		t.Errorf("Expected status to be 'degraded', got '%s'", status)
	}

	// Check the services field
	services, ok := body["services"].(map[string]interface{})
	if !ok {
		t.Errorf("Response does not contain services field or it's not an object")
		return
	}

	// Check the Java service status
	javaService, ok := services["java"].(map[string]interface{})
	if !ok {
		t.Errorf("Response does not contain java service or it's not an object")
		return
	}

	// Check that the Java service status is "error"
	javaStatus, ok := javaService["status"].(string)
	if !ok {
		t.Errorf("Java service does not contain status field or it's not a string")
	} else if javaStatus != "error" {
		t.Errorf("Expected Java service status to be 'error', got '%s'", javaStatus)
	}

	// Check that the Java service message is correct
	javaMessage, ok := javaService["message"].(string)
	if !ok {
		t.Errorf("Java service does not contain message field or it's not a string")
	} else if javaMessage != "Java service is down" {
		t.Errorf("Expected Java service message to be 'Java service is down', got '%s'", javaMessage)
	}
}