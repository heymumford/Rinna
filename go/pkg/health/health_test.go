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
	"os"
	"sync/atomic"
	"testing"
	"time"

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

// CountingDependencyChecker keeps track of how many times CheckHealth is called
type CountingDependencyChecker struct {
	callCount  *int64
	status     string
	message    string
	delay      time.Duration
	shouldFail bool
}

// CheckHealth counts calls and returns a predefined status after optional delay
func (c *CountingDependencyChecker) CheckHealth() ServiceStatus {
	// Increment the call count atomically
	if c.callCount != nil {
		atomic.AddInt64(c.callCount, 1)
	}
	
	// Apply delay if specified
	if c.delay > 0 {
		time.Sleep(c.delay)
	}
	
	// Determine status based on configuration
	status := c.status
	if c.shouldFail {
		status = "error"
	}
	
	return ServiceStatus{
		Status:    status,
		Message:   c.message,
		Timestamp: time.Now().Format(time.RFC3339),
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

// TestCachingFunctionality tests the caching functionality of the health handler
func TestCachingFunctionality(t *testing.T) {
	// Set a known TTL for testing
	const testTTL = "100ms" // Short TTL for quick testing
	os.Setenv("HEALTH_CACHE_TTL", testTTL)
	defer os.Unsetenv("HEALTH_CACHE_TTL")
	
	// Create a counter to track calls
	var callCount int64
	
	// Create a counting dependency checker
	javaChecker := &CountingDependencyChecker{
		callCount: &callCount,
		status:    "ok",
		message:   "Java service is running",
	}
	
	// Create the handler with the counting checker
	handler := NewHandler(javaChecker)
	
	// Create a router and register routes
	router := mux.NewRouter()
	handler.RegisterRoutes(router)
	
	// Create a test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Test scenario 1: Multiple requests within cache TTL
	// Make first request - should query the dependency
	resp, err := http.Get(server.URL + "/health")
	if err != nil {
		t.Fatalf("Failed to make first request: %v", err)
	}
	resp.Body.Close()
	
	// Verify the dependency was checked exactly once
	initialCallCount := atomic.LoadInt64(&callCount)
	if initialCallCount != 1 {
		t.Errorf("Expected initial call count to be 1, got %d", initialCallCount)
	}
	
	// Make multiple requests - should use cache
	for i := 0; i < 5; i++ {
		resp, err := http.Get(server.URL + "/health")
		if err != nil {
			t.Fatalf("Failed to make request %d: %v", i, err)
		}
		resp.Body.Close()
	}
	
	// Check if dependency checker was called more than once
	afterCachedCalls := atomic.LoadInt64(&callCount)
	if afterCachedCalls > initialCallCount {
		t.Errorf("Caching not working: dependency checker called %d times when it should have been 1", afterCachedCalls)
	} else {
		t.Logf("Cache working correctly: dependency called exactly %d time despite 6 total requests", initialCallCount)
	}
	
	// Test scenario 2: Wait for cache to expire, then request again
	ttl, _ := time.ParseDuration(testTTL)
	
	// Wait slightly longer than the TTL
	time.Sleep(ttl + 10*time.Millisecond)
	
	// Make another request - should trigger a new check
	resp, err = http.Get(server.URL + "/health")
	if err != nil {
		t.Fatalf("Failed to make post-expiration request: %v", err)
	}
	resp.Body.Close()
	
	// Verify the dependency checker was called again
	finalCallCount := atomic.LoadInt64(&callCount)
	if finalCallCount <= initialCallCount {
		t.Errorf("Cache did not expire: dependency checker still called only %d times after TTL expiration", finalCallCount)
	} else {
		t.Logf("Cache expiration working: dependency checker called %d times total", finalCallCount)
	}
}

// TestReadinessEndpointCaching tests that the readiness endpoint uses caching
func TestReadinessEndpointCaching(t *testing.T) {
	// Set a short TTL for testing
	os.Setenv("HEALTH_CACHE_TTL", "200ms")
	defer os.Unsetenv("HEALTH_CACHE_TTL")
	
	// Create a counter to track calls
	var callCount int64
	
	// Create a counting dependency checker with a delay
	// This helps verify that cached responses are faster
	javaChecker := &CountingDependencyChecker{
		callCount: &callCount,
		status:    "ok",
		delay:     50 * time.Millisecond, // Add delay to make uncached calls noticeably slower
	}
	
	// Create the handler with the counting checker
	handler := NewHandler(javaChecker)
	
	// Create a router and register routes
	router := mux.NewRouter()
	handler.RegisterRoutes(router)
	
	// Create a test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Make first request to /health/ready to prime the cache
	startTime := time.Now()
	resp, err := http.Get(server.URL + "/health/ready")
	firstRequestDuration := time.Since(startTime)
	if err != nil {
		t.Fatalf("Failed to make first request: %v", err)
	}
	resp.Body.Close()
	
	// Make second request immediately - should use cache and be faster
	startTime = time.Now()
	resp, err = http.Get(server.URL + "/health/ready")
	secondRequestDuration := time.Since(startTime)
	if err != nil {
		t.Fatalf("Failed to make second request: %v", err)
	}
	resp.Body.Close()
	
	// Verify the second request was significantly faster
	if secondRequestDuration > firstRequestDuration/2 {
		t.Errorf("Caching not working efficiently for readiness endpoint: first=%v, second=%v", 
			firstRequestDuration, secondRequestDuration)
	} else {
		t.Logf("Readiness endpoint caching working: uncached=%v, cached=%v", 
			firstRequestDuration, secondRequestDuration)
	}
	
	// Verify the counter was only incremented once
	count := atomic.LoadInt64(&callCount)
	if count != 1 {
		t.Errorf("Expected call count to be 1, got %d", count)
	}
	
	// Test shared cache between endpoints - call /health endpoint which should use the same cache
	resp, err = http.Get(server.URL + "/health")
	if err != nil {
		t.Fatalf("Failed to make health request: %v", err)
	}
	resp.Body.Close()
	
	// Call count should still be 1 if cache is shared
	countAfterHealth := atomic.LoadInt64(&callCount)
	if countAfterHealth > count {
		t.Errorf("Cache not shared between endpoints: call count increased from %d to %d", 
			count, countAfterHealth)
	} else {
		t.Logf("Cache correctly shared between endpoints")
	}
}