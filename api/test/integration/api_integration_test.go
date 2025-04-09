// +build integration

/*
 * Integration tests for API interactions
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package integration

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gorilla/mux"
)

// TestAPIEndpoints_Integration tests API endpoints in an integration manner
func TestAPIEndpoints_Integration(t *testing.T) {
	// Set up router
	router := mux.NewRouter()
	
	// Add a test handler
	router.HandleFunc("/api/test", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"ok"}`))
	})
	
	// Start test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Create a client with a timeout
	client := &http.Client{
		Timeout: 5 * time.Second,
	}
	
	// Create a new request with context
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	
	req, err := http.NewRequestWithContext(ctx, "GET", server.URL+"/api/test", nil)
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Send the request
	resp, err := client.Do(req)
	if err != nil {
		t.Fatalf("Failed to send request: %v", err)
	}
	defer resp.Body.Close()
	
	// Check the response
	if resp.StatusCode != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
	}
	
	contentType := resp.Header.Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("Expected content type application/json, got %s", contentType)
	}
}

// TestAPIHandling_Integration tests API request handling with simulated dependencies
func TestAPIHandling_Integration(t *testing.T) {
	// Create a test handler that simulates API request handling with dependencies
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Simulate database check
		isAuthorized := true
		
		if !isAuthorized {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		
		// Simulate processing
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"message":"Request processed"}`))
	})
	
	// Create a test request
	req := httptest.NewRequest("GET", "/api/data", nil)
	recorder := httptest.NewRecorder()
	
	// Test the handler
	handler.ServeHTTP(recorder, req)
	
	// Check the response
	if recorder.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, recorder.Code)
	}
	
	contentType := recorder.Header().Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("Expected content type application/json, got %s", contentType)
	}
	
	expectedBody := `{"message":"Request processed"}`
	if recorder.Body.String() != expectedBody {
		t.Errorf("Expected body %s, got %s", expectedBody, recorder.Body.String())
	}
}