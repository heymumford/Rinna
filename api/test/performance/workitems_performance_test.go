/*
 * Performance tests for work item API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package performance

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/models"
)

// BenchmarkWorkItemList benchmarks the list work items endpoint
func BenchmarkWorkItemList(b *testing.B) {
	// Create server and client with mock
	handler, server, mockClient := setupWorkItemTestEnvironment()
	defer server.Close()

	// Create work item list response
	mockClient.ListWorkItemsResponse = &models.WorkItemListResponse{
		Items:      createMockWorkItems(50),
		TotalCount: 50,
		Page:       1,
		PageSize:   50,
	}

	// Perform benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		req, _ := http.NewRequest("GET", "/workitems", nil)
		rr := httptest.NewRecorder()
		handler.ListWorkItems(rr, req)
	}
}

// BenchmarkWorkItemCreate benchmarks the create work item endpoint
func BenchmarkWorkItemCreate(b *testing.B) {
	// Create server and client with mock
	handler, server, mockClient := setupWorkItemTestEnvironment()
	defer server.Close()

	// Setup response
	mockClient.CreateWorkItemResponse = createMockWorkItem(uuid.New().String())

	// Create request body
	requestBody := models.WorkItemCreateRequest{
		Title:       "Benchmark Work Item",
		Description: "This is a benchmark work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
	}

	// Marshal request body
	bodyBytes, _ := json.Marshal(requestBody)

	// Perform benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		req, _ := http.NewRequest("POST", "/workitems", bytes.NewReader(bodyBytes))
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		handler.CreateWorkItem(rr, req)
	}
}

// BenchmarkWorkItemGet benchmarks the get work item endpoint
func BenchmarkWorkItemGet(b *testing.B) {
	// Create server and client with mock
	handler, server, mockClient := setupWorkItemTestEnvironment()
	defer server.Close()

	// Create router with mux vars
	router := mux.NewRouter()
	router.HandleFunc("/workitems/{id}", handler.GetWorkItem)

	// Setup response
	id := uuid.New().String()
	mockClient.GetWorkItemResponse = createMockWorkItem(id)

	// Perform benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		req, _ := http.NewRequest("GET", "/workitems/"+id, nil)
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
	}
}

// BenchmarkWorkItemUpdate benchmarks the update work item endpoint
func BenchmarkWorkItemUpdate(b *testing.B) {
	// Create server and client with mock
	handler, server, mockClient := setupWorkItemTestEnvironment()
	defer server.Close()

	// Create router with mux vars
	router := mux.NewRouter()
	router.HandleFunc("/workitems/{id}", handler.UpdateWorkItem)

	// Setup response
	id := uuid.New().String()
	mockClient.UpdateWorkItemResponse = createMockWorkItem(id)

	// Create request body
	title := "Updated Benchmark Work Item"
	requestBody := models.WorkItemUpdateRequest{
		Title: &title,
	}

	// Marshal request body
	bodyBytes, _ := json.Marshal(requestBody)

	// Perform benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		req, _ := http.NewRequest("PUT", "/workitems/"+id, bytes.NewReader(bodyBytes))
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
	}
}

// BenchmarkWorkItemTransition benchmarks the transition work item endpoint
func BenchmarkWorkItemTransition(b *testing.B) {
	// Create server and client with mock
	handler, server, mockClient := setupWorkItemTestEnvironment()
	defer server.Close()

	// Create router with mux vars
	router := mux.NewRouter()
	router.HandleFunc("/workitems/{id}/transitions", handler.TransitionWorkItem)

	// Setup response
	id := uuid.New().String()
	mockClient.TransitionWorkItemResponse = createMockWorkItem(id)

	// Create request body
	requestBody := models.WorkItemTransitionRequest{
		ToState: models.WorkflowStateDone,
		Comment: "Benchmark transition",
	}

	// Marshal request body
	bodyBytes, _ := json.Marshal(requestBody)

	// Perform benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		req, _ := http.NewRequest("POST", "/workitems/"+id+"/transitions", bytes.NewReader(bodyBytes))
		req.Header.Set("Content-Type", "application/json")
		rr := httptest.NewRecorder()
		router.ServeHTTP(rr, req)
	}
}

// BenchmarkWorkItemConcurrentRequests benchmarks concurrent requests to the work item API
func BenchmarkWorkItemConcurrentRequests(b *testing.B) {
	// Create server and client with mock
	handler, server, mockClient := setupWorkItemTestEnvironment()
	defer server.Close()

	// Create router with mux vars
	router := mux.NewRouter()
	router.HandleFunc("/workitems", handler.ListWorkItems).Methods("GET")
	router.HandleFunc("/workitems/{id}", handler.GetWorkItem).Methods("GET")

	// Setup responses
	mockClient.ListWorkItemsResponse = &models.WorkItemListResponse{
		Items:      createMockWorkItems(50),
		TotalCount: 50,
		Page:       1,
		PageSize:   50,
	}

	id := uuid.New().String()
	mockClient.GetWorkItemResponse = createMockWorkItem(id)

	// Concurrent requests
	const numConcurrent = 100

	// Perform benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		var wg sync.WaitGroup
		wg.Add(numConcurrent)

		for j := 0; j < numConcurrent; j++ {
			go func(j int) {
				defer wg.Done()

				if j%2 == 0 {
					// List request
					req, _ := http.NewRequest("GET", "/workitems", nil)
					rr := httptest.NewRecorder()
					router.ServeHTTP(rr, req)
				} else {
					// Get request
					req, _ := http.NewRequest("GET", "/workitems/"+id, nil)
					rr := httptest.NewRecorder()
					router.ServeHTTP(rr, req)
				}
			}(j)
		}

		wg.Wait()
	}
}

// TestWorkItemAPILatency tests the latency of work item API endpoints
func TestWorkItemAPILatency(t *testing.T) {
	// Skip in short mode
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	// Create server and client with mock
	handler, server, mockClient := setupWorkItemTestEnvironment()
	defer server.Close()

	// Create router with mux vars
	router := mux.NewRouter()
	router.HandleFunc("/workitems", handler.ListWorkItems).Methods("GET")
	router.HandleFunc("/workitems", handler.CreateWorkItem).Methods("POST")
	router.HandleFunc("/workitems/{id}", handler.GetWorkItem).Methods("GET")
	router.HandleFunc("/workitems/{id}", handler.UpdateWorkItem).Methods("PUT")
	router.HandleFunc("/workitems/{id}/transitions", handler.TransitionWorkItem).Methods("POST")

	// Setup responses
	mockClient.ListWorkItemsResponse = &models.WorkItemListResponse{
		Items:      createMockWorkItems(50),
		TotalCount: 50,
		Page:       1,
		PageSize:   50,
	}

	id := uuid.New().String()
	mockClient.GetWorkItemResponse = createMockWorkItem(id)
	mockClient.CreateWorkItemResponse = createMockWorkItem(uuid.New().String())
	mockClient.UpdateWorkItemResponse = createMockWorkItem(id)
	mockClient.TransitionWorkItemResponse = createMockWorkItem(id)

	// Latency test parameters
	const (
		numRequests      = 1000
		maxAvgLatency    = 10 * time.Millisecond
		maxP95Latency    = 20 * time.Millisecond
		numConcurrent    = 10
		requestsPerGroup = numRequests / numConcurrent
	)

	// Run latency tests for each endpoint
	endpoints := []struct {
		name    string
		execute func() time.Duration
	}{
		{
			name: "ListWorkItems",
			execute: func() time.Duration {
				start := time.Now()
				req, _ := http.NewRequest("GET", "/workitems", nil)
				rr := httptest.NewRecorder()
				router.ServeHTTP(rr, req)
				return time.Since(start)
			},
		},
		{
			name: "GetWorkItem",
			execute: func() time.Duration {
				start := time.Now()
				req, _ := http.NewRequest("GET", "/workitems/"+id, nil)
				rr := httptest.NewRecorder()
				router.ServeHTTP(rr, req)
				return time.Since(start)
			},
		},
		{
			name: "CreateWorkItem",
			execute: func() time.Duration {
				// Create request body
				requestBody := models.WorkItemCreateRequest{
					Title:       "Latency Test Work Item",
					Description: "This is a latency test work item",
					Type:        models.WorkItemTypeFeature,
					Priority:    models.PriorityMedium,
				}

				// Marshal request body
				bodyBytes, _ := json.Marshal(requestBody)

				start := time.Now()
				req, _ := http.NewRequest("POST", "/workitems", bytes.NewReader(bodyBytes))
				req.Header.Set("Content-Type", "application/json")
				rr := httptest.NewRecorder()
				router.ServeHTTP(rr, req)
				return time.Since(start)
			},
		},
		{
			name: "UpdateWorkItem",
			execute: func() time.Duration {
				// Create request body
				title := "Updated Latency Test Work Item"
				requestBody := models.WorkItemUpdateRequest{
					Title: &title,
				}

				// Marshal request body
				bodyBytes, _ := json.Marshal(requestBody)

				start := time.Now()
				req, _ := http.NewRequest("PUT", "/workitems/"+id, bytes.NewReader(bodyBytes))
				req.Header.Set("Content-Type", "application/json")
				rr := httptest.NewRecorder()
				router.ServeHTTP(rr, req)
				return time.Since(start)
			},
		},
		{
			name: "TransitionWorkItem",
			execute: func() time.Duration {
				// Create request body
				requestBody := models.WorkItemTransitionRequest{
					ToState: models.WorkflowStateDone,
					Comment: "Latency test transition",
				}

				// Marshal request body
				bodyBytes, _ := json.Marshal(requestBody)

				start := time.Now()
				req, _ := http.NewRequest("POST", "/workitems/"+id+"/transitions", bytes.NewReader(bodyBytes))
				req.Header.Set("Content-Type", "application/json")
				rr := httptest.NewRecorder()
				router.ServeHTTP(rr, req)
				return time.Since(start)
			},
		},
	}

	// Run tests for each endpoint
	for _, endpoint := range endpoints {
		t.Run(endpoint.name, func(t *testing.T) {
			latencies := make([]time.Duration, 0, numRequests)

			// Create channels for collecting results
			results := make(chan time.Duration, numRequests)
			ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
			defer cancel()

			// Start goroutines
			var wg sync.WaitGroup
			wg.Add(numConcurrent)

			for i := 0; i < numConcurrent; i++ {
				go func() {
					defer wg.Done()
					for j := 0; j < requestsPerGroup; j++ {
						select {
						case <-ctx.Done():
							return
						default:
							latency := endpoint.execute()
							results <- latency
						}
					}
				}()
			}

			// Wait for all goroutines to finish
			wg.Wait()
			close(results)

			// Collect results
			for latency := range results {
				latencies = append(latencies, latency)
			}

			// Calculate statistics
			var totalLatency time.Duration
			for _, latency := range latencies {
				totalLatency += latency
			}
			avgLatency := totalLatency / time.Duration(len(latencies))

			// Sort latencies for percentile calculation
			sortLatencies(latencies)

			// Calculate p95
			p95Index := int(float64(len(latencies)) * 0.95)
			p95Latency := latencies[p95Index]

			// Log results
			t.Logf("Endpoint: %s", endpoint.name)
			t.Logf("  Sample size: %d", len(latencies))
			t.Logf("  Average latency: %v", avgLatency)
			t.Logf("  P95 latency: %v", p95Latency)

			// Verify requirements
			if avgLatency > maxAvgLatency {
				t.Errorf("Average latency too high: %v (max: %v)", avgLatency, maxAvgLatency)
			}
			if p95Latency > maxP95Latency {
				t.Errorf("P95 latency too high: %v (max: %v)", p95Latency, maxP95Latency)
			}
		})
	}

	// Concurrent mixed workload test
	t.Run("ConcurrentMixedWorkload", func(t *testing.T) {
		// Start timer
		startTime := time.Now()

		// Create channels for collecting results
		latencies := make([]time.Duration, 0, numRequests)
		results := make(chan time.Duration, numRequests)
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()

		// Start goroutines
		var wg sync.WaitGroup
		wg.Add(numConcurrent)

		for i := 0; i < numConcurrent; i++ {
			go func(groupID int) {
				defer wg.Done()
				for j := 0; j < requestsPerGroup; j++ {
					select {
					case <-ctx.Done():
						return
					default:
						// Choose endpoint based on request index
						requestType := (groupID + j) % 5
						var latency time.Duration

						switch requestType {
						case 0:
							latency = endpoints[0].execute() // List
						case 1:
							latency = endpoints[1].execute() // Get
						case 2:
							latency = endpoints[2].execute() // Create
						case 3:
							latency = endpoints[3].execute() // Update
						case 4:
							latency = endpoints[4].execute() // Transition
						}

						results <- latency
					}
				}
			}(i)
		}

		// Wait for all goroutines to finish
		wg.Wait()
		totalDuration := time.Since(startTime)
		close(results)

		// Collect results
		for latency := range results {
			latencies = append(latencies, latency)
		}

		// Calculate statistics
		var totalLatency time.Duration
		for _, latency := range latencies {
			totalLatency += latency
		}
		avgLatency := totalLatency / time.Duration(len(latencies))

		// Sort latencies for percentile calculation
		sortLatencies(latencies)

		// Calculate p95
		p95Index := int(float64(len(latencies)) * 0.95)
		p95Latency := latencies[p95Index]

		// Calculate throughput
		throughput := float64(len(latencies)) / totalDuration.Seconds()

		// Log results
		t.Logf("Mixed Workload Performance:")
		t.Logf("  Total duration: %v", totalDuration)
		t.Logf("  Sample size: %d", len(latencies))
		t.Logf("  Average latency: %v", avgLatency)
		t.Logf("  P95 latency: %v", p95Latency)
		t.Logf("  Throughput: %.2f requests/second", throughput)

		// Verify requirements
		const (
			maxMixedAvgLatency = 15 * time.Millisecond
			maxMixedP95Latency = 30 * time.Millisecond
			minThroughput      = 100.0 // requests per second
		)

		if avgLatency > maxMixedAvgLatency {
			t.Errorf("Mixed workload average latency too high: %v (max: %v)", avgLatency, maxMixedAvgLatency)
		}
		if p95Latency > maxMixedP95Latency {
			t.Errorf("Mixed workload P95 latency too high: %v (max: %v)", p95Latency, maxMixedP95Latency)
		}
		if throughput < minThroughput {
			t.Errorf("Mixed workload throughput too low: %.2f req/s (min: %.2f)", throughput, minThroughput)
		}
	})
}

// Helper function to sort latencies for percentile calculation
func sortLatencies(latencies []time.Duration) {
	for i := 0; i < len(latencies)-1; i++ {
		for j := i + 1; j < len(latencies); j++ {
			if latencies[i] > latencies[j] {
				latencies[i], latencies[j] = latencies[j], latencies[i]
			}
		}
	}
}

// Helper functions for creating test environments and mock data

// MockJavaClientForWorkItems is a mock implementation of the Java client for testing work item handlers
type MockJavaClientForWorkItems struct {
	// Mock responses
	ListWorkItemsResponse        *models.WorkItemListResponse
	GetWorkItemResponse          *models.WorkItem
	CreateWorkItemResponse       *models.WorkItem
	UpdateWorkItemResponse       *models.WorkItem
	TransitionWorkItemResponse   *models.WorkItem

	// Error responses
	ListWorkItemsError          error
	GetWorkItemError            error
	CreateWorkItemError         error
	UpdateWorkItemError         error
	TransitionWorkItemError     error
}

// ListWorkItems mocks the ListWorkItems method
func (m *MockJavaClientForWorkItems) ListWorkItems(ctx context.Context, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	if m.ListWorkItemsError != nil {
		return nil, m.ListWorkItemsError
	}
	return m.ListWorkItemsResponse, nil
}

// CreateWorkItem mocks the CreateWorkItem method
func (m *MockJavaClientForWorkItems) CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error) {
	if m.CreateWorkItemError != nil {
		return nil, m.CreateWorkItemError
	}
	return m.CreateWorkItemResponse, nil
}

// GetWorkItem mocks the GetWorkItem method
func (m *MockJavaClientForWorkItems) GetWorkItem(ctx context.Context, id string) (*models.WorkItem, error) {
	if m.GetWorkItemError != nil {
		return nil, m.GetWorkItemError
	}
	return m.GetWorkItemResponse, nil
}

// UpdateWorkItem mocks the UpdateWorkItem method
func (m *MockJavaClientForWorkItems) UpdateWorkItem(ctx context.Context, id string, request models.WorkItemUpdateRequest) (*models.WorkItem, error) {
	if m.UpdateWorkItemError != nil {
		return nil, m.UpdateWorkItemError
	}
	return m.UpdateWorkItemResponse, nil
}

// TransitionWorkItem mocks the TransitionWorkItem method
func (m *MockJavaClientForWorkItems) TransitionWorkItem(ctx context.Context, id string, request models.WorkItemTransitionRequest) (*models.WorkItem, error) {
	if m.TransitionWorkItemError != nil {
		return nil, m.TransitionWorkItemError
	}
	return m.TransitionWorkItemResponse, nil
}

// testWorkItemHandler implements WorkItemHandler but uses our mock client
type testWorkItemHandler struct {
	javaClient *MockJavaClientForWorkItems
}

// ListWorkItems handles list work items requests
func (h *testWorkItemHandler) ListWorkItems(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	status := r.URL.Query().Get("status")
	pageStr := r.URL.Query().Get("page")
	pageSizeStr := r.URL.Query().Get("pageSize")

	// Set default values
	page := 1
	pageSize := 10

	// Parse page parameter
	if pageStr != "" {
		fmt.Sscanf(pageStr, "%d", &page)
	}

	// Parse pageSize parameter
	if pageSizeStr != "" {
		fmt.Sscanf(pageSizeStr, "%d", &pageSize)
	}

	// Call the Java service
	response, err := h.javaClient.ListWorkItems(r.Context(), status, page, pageSize)
	if err != nil {
		http.Error(w, "Failed to list work items: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetWorkItem handles get work item by ID requests
func (h *testWorkItemHandler) GetWorkItem(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Call the Java service
	workItem, err := h.javaClient.GetWorkItem(r.Context(), id)
	if err != nil {
		http.Error(w, "Failed to get work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}

// CreateWorkItem handles create work item requests
func (h *testWorkItemHandler) CreateWorkItem(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.WorkItemCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Call the Java service
	workItem, err := h.javaClient.CreateWorkItem(r.Context(), request)
	if err != nil {
		http.Error(w, "Failed to create work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(workItem)
}

// UpdateWorkItem handles update work item requests
func (h *testWorkItemHandler) UpdateWorkItem(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Parse request body
	var request models.WorkItemUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Call the Java service
	workItem, err := h.javaClient.UpdateWorkItem(r.Context(), id, request)
	if err != nil {
		http.Error(w, "Failed to update work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}

// TransitionWorkItem handles transition work item requests
func (h *testWorkItemHandler) TransitionWorkItem(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Parse request body
	var request models.WorkItemTransitionRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Call the Java service
	workItem, err := h.javaClient.TransitionWorkItem(r.Context(), id, request)
	if err != nil {
		http.Error(w, "Failed to transition work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}

// setupWorkItemTestEnvironment creates a test environment for work item handlers
func setupWorkItemTestEnvironment() (*testWorkItemHandler, *httptest.Server, *MockJavaClientForWorkItems) {
	// Create mock client
	mockClient := &MockJavaClientForWorkItems{}

	// Create handler
	handler := &testWorkItemHandler{
		javaClient: mockClient,
	}

	// Create server
	router := mux.NewRouter()
	router.HandleFunc("/workitems", handler.ListWorkItems).Methods("GET")
	router.HandleFunc("/workitems", handler.CreateWorkItem).Methods("POST")
	router.HandleFunc("/workitems/{id}", handler.GetWorkItem).Methods("GET")
	router.HandleFunc("/workitems/{id}", handler.UpdateWorkItem).Methods("PUT")
	router.HandleFunc("/workitems/{id}/transitions", handler.TransitionWorkItem).Methods("POST")

	server := httptest.NewServer(router)

	return handler, server, mockClient
}

// createMockWorkItem creates a mock work item with the given ID
func createMockWorkItem(id string) *models.WorkItem {
	uid, _ := uuid.Parse(id)
	return &models.WorkItem{
		ID:          uid,
		Title:       "Test Work Item",
		Description: "This is a test work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		Status:      models.WorkflowStateInDev,
		Assignee:    "test-user",
		ProjectID:   "TEST",
		CreatedAt:   time.Now().Add(-24 * time.Hour),
		UpdatedAt:   time.Now(),
	}
}

// createMockWorkItems creates a list of mock work items
func createMockWorkItems(count int) []models.WorkItem {
	items := make([]models.WorkItem, count)
	for i := 0; i < count; i++ {
		items[i] = *createMockWorkItem(uuid.New().String())
		items[i].Title = fmt.Sprintf("Test Work Item %d", i)
		
		// Alternate types, priorities, and statuses
		switch i % 3 {
		case 0:
			items[i].Type = models.WorkItemTypeFeature
		case 1:
			items[i].Type = models.WorkItemTypeBug
		case 2:
			items[i].Type = models.WorkItemTypeChore
		}
		
		switch i % 3 {
		case 0:
			items[i].Priority = models.PriorityLow
		case 1:
			items[i].Priority = models.PriorityMedium
		case 2:
			items[i].Priority = models.PriorityHigh
		}
		
		switch i % 6 {
		case 0:
			items[i].Status = models.WorkflowStateFound
		case 1:
			items[i].Status = models.WorkflowStateTriaged
		case 2:
			items[i].Status = models.WorkflowStateInDev
		case 3:
			items[i].Status = models.WorkflowStateTesting
		case 4:
			items[i].Status = models.WorkflowStateDone
		case 5:
			items[i].Status = models.WorkflowStateClosed
		}
	}
	return items
}