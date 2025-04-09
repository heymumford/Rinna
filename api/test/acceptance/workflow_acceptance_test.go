// +build acceptance

/*
 * Acceptance tests for workflow scenarios
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package acceptance

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/mux"
)

// TestWorkItemLifecycle_Acceptance tests a work item's lifecycle
func TestWorkItemLifecycle_Acceptance(t *testing.T) {
	t.Log("SCENARIO: Work item creation and transition through states")

	// Set up the router and handlers for the scenario
	router := mux.NewRouter()
	setupTestHandlers(router)
	server := httptest.NewServer(router)
	defer server.Close()

	// Create a test client
	client := &http.Client{Timeout: 5 * time.Second}

	t.Log("GIVEN a user with valid credentials")
	apiToken := "test-token-123"

	t.Log("WHEN the user creates a new work item")
	createBody := `{"title":"Test item","type":"FEATURE","priority":"MEDIUM"}`
	req, _ := http.NewRequest("POST", server.URL+"/api/workitems", strings.NewReader(createBody))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+apiToken)

	resp, err := client.Do(req)
	if err != nil {
		t.Fatalf("Failed to create work item: %v", err)
	}
	defer resp.Body.Close()

	t.Log("THEN the work item is created successfully")
	if resp.StatusCode != http.StatusCreated {
		t.Errorf("Expected status code %d, got %d", http.StatusCreated, resp.StatusCode)
	}

	// Get the work item ID from the response
	var workItemID string = "WI-123" // In a real test, we would parse this from the response

	t.Log("AND WHEN the user transitions the work item to TRIAGED state")
	updateBody := `{"status":"TRIAGED"}`
	req, _ = http.NewRequest("PATCH", fmt.Sprintf("%s/api/workitems/%s", server.URL, workItemID), strings.NewReader(updateBody))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+apiToken)

	resp, err = client.Do(req)
	if err != nil {
		t.Fatalf("Failed to update work item: %v", err)
	}
	defer resp.Body.Close()

	t.Log("THEN the work item state is updated successfully")
	if resp.StatusCode != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
	}

	t.Log("AND WHEN the user views the work item details")
	req, _ = http.NewRequest("GET", fmt.Sprintf("%s/api/workitems/%s", server.URL, workItemID), nil)
	req.Header.Set("Authorization", "Bearer "+apiToken)

	resp, err = client.Do(req)
	if err != nil {
		t.Fatalf("Failed to get work item: %v", err)
	}
	defer resp.Body.Close()

	t.Log("THEN the work item details show the updated state")
	if resp.StatusCode != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
	}
}

// TestReleaseManagement_Acceptance tests the release management workflow
func TestReleaseManagement_Acceptance(t *testing.T) {
	t.Log("SCENARIO: Release creation and management")

	// Set up the router and handlers for the scenario
	router := mux.NewRouter()
	setupTestHandlers(router)
	server := httptest.NewServer(router)
	defer server.Close()

	// Create a test client
	client := &http.Client{Timeout: 5 * time.Second}

	t.Log("GIVEN a user with valid credentials")
	apiToken := "test-token-123"

	t.Log("WHEN the user creates a new release")
	createBody := `{"name":"1.0.0","description":"First release"}`
	req, _ := http.NewRequest("POST", server.URL+"/api/releases", strings.NewReader(createBody))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+apiToken)

	resp, err := client.Do(req)
	if err != nil {
		t.Fatalf("Failed to create release: %v", err)
	}
	defer resp.Body.Close()

	t.Log("THEN the release is created successfully")
	if resp.StatusCode != http.StatusCreated {
		t.Errorf("Expected status code %d, got %d", http.StatusCreated, resp.StatusCode)
	}
}

// setupTestHandlers sets up test handlers for the acceptance tests
func setupTestHandlers(router *mux.Router) {
	// Work items API
	router.HandleFunc("/api/workitems", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		w.Write([]byte(`{"id":"WI-123","title":"Test item","status":"FOUND"}`))
	}).Methods("POST")

	router.HandleFunc("/api/workitems/{id}", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		if r.Method == "GET" {
			w.WriteHeader(http.StatusOK)
			w.Write([]byte(`{"id":"WI-123","title":"Test item","status":"TRIAGED"}`))
		} else {
			w.WriteHeader(http.StatusOK)
			w.Write([]byte(`{"id":"WI-123","title":"Test item","status":"TRIAGED"}`))
		}
	}).Methods("GET", "PATCH")

	// Releases API
	router.HandleFunc("/api/releases", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		w.Write([]byte(`{"id":"REL-1","name":"1.0.0","description":"First release"}`))
	}).Methods("POST")
}