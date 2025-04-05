/*
 * Tests for health check handlers for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gorilla/mux"
)

// TestHealthCheck tests the health check handler
func TestHealthCheck(t *testing.T) {
	// Create a request to pass to our handler
	req, err := http.NewRequest("GET", "/health", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create a ResponseRecorder to record the response
	rr := httptest.NewRecorder()
	handler := http.HandlerFunc(HealthCheckHandler)

	// Call the handler directly
	handler.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	// Check the content type
	contentType := rr.Header().Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("handler returned wrong content type: got %v want %v",
			contentType, "application/json")
	}

	// Parse the response body
	var response HealthResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Basic validation of the response
	if response.Status != "ok" {
		t.Errorf("handler returned wrong status: got %v want %v",
			response.Status, "ok")
	}

	if response.GoVersion == "" {
		t.Errorf("handler returned empty GoVersion")
	}

	if response.Timestamp == "" {
		t.Errorf("handler returned empty Timestamp")
	}

	if _, ok := response.Services["java"]; !ok {
		t.Errorf("handler did not return java service status")
	}
}

// TestLivenessCheck tests the liveness check handler
func TestLivenessCheck(t *testing.T) {
	// Create a request to pass to our handler
	req, err := http.NewRequest("GET", "/health/live", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create a ResponseRecorder to record the response
	rr := httptest.NewRecorder()
	handler := http.HandlerFunc(LivenessCheckHandler)

	// Call the handler directly
	handler.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	// Check the content type
	contentType := rr.Header().Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("handler returned wrong content type: got %v want %v",
			contentType, "application/json")
	}

	// Parse the response body
	var response map[string]string
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Basic validation of the response
	if response["status"] != "ok" {
		t.Errorf("handler returned wrong status: got %v want %v",
			response["status"], "ok")
	}

	if response["timestamp"] == "" {
		t.Errorf("handler returned empty timestamp")
	}
}

// TestReadinessCheck tests the readiness check handler
func TestReadinessCheck(t *testing.T) {
	// Create a request to pass to our handler
	req, err := http.NewRequest("GET", "/health/ready", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create a ResponseRecorder to record the response
	rr := httptest.NewRecorder()
	handler := http.HandlerFunc(ReadinessCheckHandler)

	// Call the handler directly
	handler.ServeHTTP(rr, req)

	// Check the status code - we expect OK since our mock Java service is "up"
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	// Check the content type
	contentType := rr.Header().Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("handler returned wrong content type: got %v want %v",
			contentType, "application/json")
	}

	// Parse the response body
	var response map[string]interface{}
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Basic validation of the response
	if response["status"] != "ok" {
		t.Errorf("handler returned wrong status: got %v want %v",
			response["status"], "ok")
	}

	if response["timestamp"] == "" {
		t.Errorf("handler returned empty timestamp")
	}

	// Check services map exists
	services, ok := response["services"].(map[string]interface{})
	if !ok {
		t.Errorf("handler did not return services map")
	}

	// Check java service status
	javaService, ok := services["java"].(map[string]interface{})
	if !ok {
		t.Errorf("handler did not return java service status")
	}

	if javaService["status"] != "ok" {
		t.Errorf("handler returned wrong java service status: got %v want %v",
			javaService["status"], "ok")
	}
}

// TestHealthRoutes tests that all health routes are registered correctly
func TestHealthRoutes(t *testing.T) {
	// Create a new router
	router := mux.NewRouter()
	
	// Register health routes
	RegisterHealthRoutes(router)
	
	// Test cases for routes that should exist
	testRoutes := []struct {
		method string
		path   string
	}{
		{"GET", "/health"},
		{"GET", "/health/live"},
		{"GET", "/health/ready"},
	}
	
	for _, tc := range testRoutes {
		// Create a request to test the route
		req, err := http.NewRequest(tc.method, tc.path, nil)
		if err != nil {
			t.Fatal(err)
		}
		
		// Test that the route matches
		var match mux.RouteMatch
		if !router.Match(req, &match) {
			t.Errorf("route %s %s not found", tc.method, tc.path)
		}
	}
	
	// Test a route that should not exist
	req, err := http.NewRequest("POST", "/health", nil)
	if err != nil {
		t.Fatal(err)
	}
	
	var match mux.RouteMatch
	if router.Match(req, &match) {
		t.Errorf("unexpected route match for POST /health")
	}
}

// TestNegativeJavaService tests the readiness check when Java service is down
func TestNegativeJavaService(t *testing.T) {
	// Override the CheckJavaServiceFunc function to simulate a failed check
	originalCheckJavaService := CheckJavaServiceFunc
	defer func() { CheckJavaServiceFunc = originalCheckJavaService }()
	
	CheckJavaServiceFunc = func() ServiceStatus {
		return ServiceStatus{
			Status:    "error",
			Message:   "Java service is unreachable",
			Timestamp: "2025-01-01T00:00:00Z",
		}
	}
	
	// Create a request to pass to our handler
	req, err := http.NewRequest("GET", "/health/ready", nil)
	if err != nil {
		t.Fatal(err)
	}
	
	// Create a ResponseRecorder to record the response
	rr := httptest.NewRecorder()
	handler := http.HandlerFunc(ReadinessCheckHandler)
	
	// Call the handler directly
	handler.ServeHTTP(rr, req)
	
	// When Java service is down, we expect a 503 Service Unavailable
	if status := rr.Code; status != http.StatusServiceUnavailable {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusServiceUnavailable)
	}
	
	// Parse the response body
	var response map[string]interface{}
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}
	
	// Check that status is degraded
	if response["status"] != "degraded" {
		t.Errorf("handler returned wrong status: got %v want %v",
			response["status"], "degraded")
	}
	
	// Check services map exists
	services, ok := response["services"].(map[string]interface{})
	if !ok {
		t.Errorf("handler did not return services map")
	}
	
	// Check java service status
	javaService, ok := services["java"].(map[string]interface{})
	if !ok {
		t.Errorf("handler did not return java service status")
	}
	
	if javaService["status"] != "error" {
		t.Errorf("handler returned wrong java service status: got %v want %v",
			javaService["status"], "error")
	}
	
	if javaService["message"] != "Java service is unreachable" {
		t.Errorf("handler returned wrong java service message: got %v want %v",
			javaService["message"], "Java service is unreachable")
	}
}