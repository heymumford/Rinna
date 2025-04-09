package performance

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gorilla/mux"
	"github.com/stretchr/testify/assert"
)

// Mock handlers for performance testing
func mockHealthCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"status": "healthy"})
}

func mockPerfCreateWorkItem(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"id": "WI-789"})
}

func mockPerfGetWorkItem(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"id":        "WI-789",
		"title":     "Performance test item",
		"type":      "TASK",
		"priority":  "MEDIUM",
		"status":    "FOUND",
		"projectId": "perf-test",
	})
}

// BenchmarkAPIResponse benchmarks API response times
func BenchmarkAPIResponse(b *testing.B) {
	// Setup router with mock handlers
	r := mux.NewRouter()
	r.HandleFunc("/api/v1/health", mockHealthCheck).Methods("GET")
	r.HandleFunc("/api/v1/workitems", mockPerfCreateWorkItem).Methods("POST")
	r.HandleFunc("/api/v1/workitems/{id}", mockPerfGetWorkItem).Methods("GET")

	// Create a test server
	server := httptest.NewServer(r)
	defer server.Close()

	// Benchmark health endpoint
	b.Run("Health endpoint", func(b *testing.B) {
		b.ResetTimer()
		for i := 0; i < b.N; i++ {
			resp, err := http.Get(server.URL + "/api/v1/health")
			if err != nil || resp.StatusCode != http.StatusOK {
				b.Fail()
			}
			resp.Body.Close()
		}
	})

	// Create a test work item for subsequent benchmarks
	workItem := map[string]interface{}{
		"title":     "Performance test item",
		"type":      "TASK",
		"priority":  "MEDIUM",
		"projectId": "perf-test",
	}
	data, _ := json.Marshal(workItem)
	resp, _ := http.Post(server.URL+"/api/v1/workitems", "application/json", bytes.NewBuffer(data))
	var createResp struct {
		ID string `json:"id"`
	}
	json.NewDecoder(resp.Body).Decode(&createResp)
	resp.Body.Close()
	workItemID := createResp.ID

	// Benchmark get work item endpoint
	b.Run("Get work item endpoint", func(b *testing.B) {
		b.ResetTimer()
		for i := 0; i < b.N; i++ {
			resp, err := http.Get(server.URL + "/api/v1/workitems/" + workItemID)
			if err != nil || resp.StatusCode != http.StatusOK {
				b.Fail()
			}
			resp.Body.Close()
		}
	})

	// Benchmark create work item endpoint
	b.Run("Create work item endpoint", func(b *testing.B) {
		b.ResetTimer()
		for i := 0; i < b.N; i++ {
			workItem := map[string]interface{}{
				"title":     fmt.Sprintf("Performance test item %d", i),
				"type":      "TASK",
				"priority":  "MEDIUM",
				"projectId": "perf-test",
			}
			data, _ := json.Marshal(workItem)
			resp, err := http.Post(server.URL+"/api/v1/workitems", "application/json", bytes.NewBuffer(data))
			if err != nil || resp.StatusCode != http.StatusCreated {
				b.Fail()
			}
			resp.Body.Close()
		}
	})
}

// TestAPIResponseTime tests API response time thresholds
func TestAPIResponseTime(t *testing.T) {
	// Define performance thresholds
	const (
		maxHealthCheckResponseTime   = 50 * time.Millisecond
		maxWorkItemGetResponseTime   = 100 * time.Millisecond
		maxWorkItemCreateResponseTime = 150 * time.Millisecond
	)

	// Setup router with handlers
	r := mux.NewRouter()
	r.HandleFunc("/api/v1/health", mockHealthCheck).Methods("GET")
	r.HandleFunc("/api/v1/workitems", mockPerfCreateWorkItem).Methods("POST")
	r.HandleFunc("/api/v1/workitems/{id}", mockPerfGetWorkItem).Methods("GET")

	// Create a test server
	server := httptest.NewServer(r)
	defer server.Close()

	// Test health endpoint response time
	t.Run("Health endpoint response time", func(t *testing.T) {
		start := time.Now()
		resp, err := http.Get(server.URL + "/api/v1/health")
		duration := time.Since(start)
		
		assert.NoError(t, err)
		assert.Equal(t, http.StatusOK, resp.StatusCode)
		resp.Body.Close()
		
		// Check response time
		assert.Less(t, duration, maxHealthCheckResponseTime, 
			"Health check response time should be less than %v, but was %v", 
			maxHealthCheckResponseTime, duration)
	})

	// Create a test work item for subsequent tests
	workItem := map[string]interface{}{
		"title":     "Performance test item",
		"type":      "TASK",
		"priority":  "MEDIUM",
		"projectId": "perf-test",
	}
	data, _ := json.Marshal(workItem)
	
	// Test create work item response time
	t.Run("Create work item response time", func(t *testing.T) {
		start := time.Now()
		resp, err := http.Post(server.URL+"/api/v1/workitems", "application/json", bytes.NewBuffer(data))
		duration := time.Since(start)
		
		assert.NoError(t, err)
		assert.Equal(t, http.StatusCreated, resp.StatusCode)
		
		// Parse response to get work item ID
		var createResp struct {
			ID string `json:"id"`
		}
		err = json.NewDecoder(resp.Body).Decode(&createResp)
		assert.NoError(t, err)
		resp.Body.Close()
		
		workItemID := createResp.ID
		assert.NotEmpty(t, workItemID)
		
		// Check response time
		assert.Less(t, duration, maxWorkItemCreateResponseTime, 
			"Create work item response time should be less than %v, but was %v", 
			maxWorkItemCreateResponseTime, duration)
		
		// Test get work item response time
		t.Run("Get work item response time", func(t *testing.T) {
			start := time.Now()
			resp, err := http.Get(server.URL + "/api/v1/workitems/" + workItemID)
			duration := time.Since(start)
			
			assert.NoError(t, err)
			assert.Equal(t, http.StatusOK, resp.StatusCode)
			resp.Body.Close()
			
			// Check response time
			assert.Less(t, duration, maxWorkItemGetResponseTime, 
				"Get work item response time should be less than %v, but was %v", 
				maxWorkItemGetResponseTime, duration)
		})
	})
}