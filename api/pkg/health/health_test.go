/*
 * Tests for health check handlers
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package health

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gorilla/mux"
)

// MockDependencyChecker is a mock implementation of DependencyChecker
type MockDependencyChecker struct {
	status  string
	message string
}

// CheckHealth returns a predefined status
func (m *MockDependencyChecker) CheckHealth() ServiceStatus {
	return ServiceStatus{
		Status:    m.status,
		Message:   m.message,
		Timestamp: "2025-01-01T00:00:00Z",
	}
}

// TestHealthHandler tests the health handler
func TestHealthHandler(t *testing.T) {
	// Create a mock dependency checker with a healthy status
	javaChecker := &MockDependencyChecker{
		status: "ok",
	}

	// Create a handler with the mock checker
	handler := NewHandler(javaChecker)

	// Create a request to pass to the handler
	req, err := http.NewRequest("GET", "/health", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create a ResponseRecorder to record the response
	rr := httptest.NewRecorder()

	// Call the handler
	handler.HandleHealth(rr, req)

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

	if javaStatus, ok := response.Services["java"]; !ok {
		t.Errorf("handler did not return java service status")
	} else if javaStatus.Status != "ok" {
		t.Errorf("handler returned wrong java status: got %v want %v",
			javaStatus.Status, "ok")
	}
}

// TestLivenessHandler tests the liveness handler
func TestLivenessHandler(t *testing.T) {
	// Create a mock dependency checker
	javaChecker := &MockDependencyChecker{
		status: "ok",
	}

	// Create a handler with the mock checker
	handler := NewHandler(javaChecker)

	// Create a request to pass to the handler
	req, err := http.NewRequest("GET", "/health/live", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create a ResponseRecorder to record the response
	rr := httptest.NewRecorder()

	// Call the handler
	handler.HandleLiveness(rr, req)

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

// TestReadinessHandler tests the readiness handler with a healthy dependency
func TestReadinessHandler(t *testing.T) {
	// Create a mock dependency checker with a healthy status
	javaChecker := &MockDependencyChecker{
		status: "ok",
	}

	// Create a handler with the mock checker
	handler := NewHandler(javaChecker)

	// Create a request to pass to the handler
	req, err := http.NewRequest("GET", "/health/ready", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create a ResponseRecorder to record the response
	rr := httptest.NewRecorder()

	// Call the handler
	handler.HandleReadiness(rr, req)

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

	// Check that the services map exists
	services, ok := response["services"].(map[string]interface{})
	if !ok {
		t.Errorf("handler did not return services map")
	}

	// Check that the java service status exists
	javaService, ok := services["java"].(map[string]interface{})
	if !ok {
		t.Errorf("handler did not return java service status")
	}

	// Check that the java service status is ok
	if javaService["status"] != "ok" {
		t.Errorf("handler returned wrong java service status: got %v want %v",
			javaService["status"], "ok")
	}
}

// TestReadinessHandlerUnhealthy tests the readiness handler with an unhealthy dependency
func TestReadinessHandlerUnhealthy(t *testing.T) {
	// Create a mock dependency checker with an unhealthy status
	javaChecker := &MockDependencyChecker{
		status:  "error",
		message: "Java service is unreachable",
	}

	// Create a handler with the mock checker
	handler := NewHandler(javaChecker)

	// Create a request to pass to the handler
	req, err := http.NewRequest("GET", "/health/ready", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create a ResponseRecorder to record the response
	rr := httptest.NewRecorder()

	// Call the handler
	handler.HandleReadiness(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusServiceUnavailable {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusServiceUnavailable)
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
	if response["status"] != "degraded" {
		t.Errorf("handler returned wrong status: got %v want %v",
			response["status"], "degraded")
	}

	if response["timestamp"] == "" {
		t.Errorf("handler returned empty timestamp")
	}

	// Check that the services map exists
	services, ok := response["services"].(map[string]interface{})
	if !ok {
		t.Errorf("handler did not return services map")
	}

	// Check that the java service status exists
	javaService, ok := services["java"].(map[string]interface{})
	if !ok {
		t.Errorf("handler did not return java service status")
	}

	// Check that the java service status is error
	if javaService["status"] != "error" {
		t.Errorf("handler returned wrong java service status: got %v want %v",
			javaService["status"], "error")
	}

	// Check that the java service message is correct
	if javaService["message"] != "Java service is unreachable" {
		t.Errorf("handler returned wrong java service message: got %v want %v",
			javaService["message"], "Java service is unreachable")
	}
}

// TestRouteRegistration tests that all routes are registered correctly
func TestRouteRegistration(t *testing.T) {
	// Create a mock dependency checker
	javaChecker := &MockDependencyChecker{
		status: "ok",
	}

	// Create a handler with the mock checker
	handler := NewHandler(javaChecker)

	// Create a router
	router := mux.NewRouter()

	// Register the routes
	handler.RegisterRoutes(router)

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