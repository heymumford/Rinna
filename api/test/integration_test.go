/*
 * Integration tests for the Rinna API server
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package test

import (
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/handlers"
	"github.com/heymumford/rinna/api/internal/middleware"
)

func TestHealthEndpoint(t *testing.T) {
	// Create router
	r := mux.NewRouter()
	r.Use(middleware.Logging)
	r.Use(middleware.RequestID)

	// Register health route
	handlers.RegisterHealthRoutes(r)

	// Create a test server
	ts := httptest.NewServer(r)
	defer ts.Close()

	// Make a request to the health endpoint
	resp, err := http.Get(ts.URL + "/health")
	if err != nil {
		t.Fatalf("Failed to make request: %v", err)
	}
	defer resp.Body.Close()

	// Check status code
	if resp.StatusCode != http.StatusOK {
		t.Errorf("Expected status 200, got %d", resp.StatusCode)
	}
}

func TestMain(m *testing.M) {
	// Setup
	setup()

	// Run tests
	code := m.Run()

	// Teardown
	teardown()

	os.Exit(code)
}

func setup() {
	// Setup code before running tests
}

func teardown() {
	// Cleanup code after running tests
}
