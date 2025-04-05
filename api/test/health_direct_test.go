/*
 * Direct tests for health endpoints
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

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/pkg/health"
)

// TestHealthDirectEndpoint tests the health endpoint directly
func TestHealthDirectEndpoint(t *testing.T) {
	// Create a router for testing
	router := mux.NewRouter()
	
	// Create a health handler with a mock checker
	mockChecker := &MockJavaChecker{status: "ok"}
	healthHandler := health.NewHandler(mockChecker)
	healthHandler.RegisterRoutes(router)

	// Create a request to the health endpoint
	req, err := http.NewRequest(http.MethodGet, "/health", nil)
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}

	// Create a response recorder
	rr := httptest.NewRecorder()

	// Serve the request
	router.ServeHTTP(rr, req)

	// Log the response for debugging
	t.Logf("GET /health %s %s", req.RemoteAddr, rr.Body.String())

	// Decode the response body
	var response health.HealthResponse
	if err := json.NewDecoder(rr.Body).Decode(&response); err != nil {
		t.Fatalf("Failed to decode response body: %v", err)
	}

	// Check required fields
	if response.Version == "" {
		t.Errorf("Version field is empty")
	}
	
	if response.GoVersion == "" {
		t.Errorf("GoVersion field is empty")
	}
	
	if response.Uptime == "" {
		t.Errorf("Uptime field is empty")
	}
	
	if response.Memory == nil {
		t.Errorf("Memory stats are nil")
	} else {
		if response.Memory.Alloc == 0 {
			t.Errorf("Memory.Alloc is zero")
		}
		if response.Memory.Sys == 0 {
			t.Errorf("Memory.Sys is zero")
		}
	}
	
	if response.Environment == "" {
		t.Errorf("Environment field is empty")
	}
	
	if len(response.Services) == 0 {
		t.Errorf("Services map is empty")
	}
	
	if javaStatus, ok := response.Services["java"]; !ok {
		t.Errorf("Java service status not found")
	} else if javaStatus.Status != "ok" {
		t.Errorf("Java service status is not ok: %s", javaStatus.Status)
	}
}