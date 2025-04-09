// +build performance

/*
 * Performance benchmarks for API response times
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package performance

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/mux"
)

// BenchmarkAPIResponse benchmarks the API response time
func BenchmarkAPIResponse(b *testing.B) {
	// Set up the router and handler
	router := mux.NewRouter()
	router.HandleFunc("/api/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"ok"}`))
	})

	// Reset the timer to exclude setup time
	b.ResetTimer()

	// Run the benchmark
	for i := 0; i < b.N; i++ {
		req := httptest.NewRequest("GET", "/api/health", nil)
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			b.Fatalf("Expected status code %d, got %d", http.StatusOK, rec.Code)
		}
	}
}

// BenchmarkComplexQuery benchmarks a complex query operation
func BenchmarkComplexQuery(b *testing.B) {
	// Set up test data
	data := generateTestData(1000)

	// Reset the timer to exclude setup time
	b.ResetTimer()

	// Run the benchmark
	for i := 0; i < b.N; i++ {
		result := complexQuery(data, "keyword", 10)
		if len(result) == 0 {
			b.Fatal("Expected non-empty result")
		}
	}
}

// BenchmarkConcurrentRequests benchmarks handling multiple concurrent requests
func BenchmarkConcurrentRequests(b *testing.B) {
	// Set up the router and handler
	router := mux.NewRouter()
	router.HandleFunc("/api/data", func(w http.ResponseWriter, r *http.Request) {
		// Simulate some processing time
		time.Sleep(1 * time.Millisecond)
		
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"data":"response"}`))
	})

	server := httptest.NewServer(router)
	defer server.Close()

	// Create a client
	client := &http.Client{Timeout: 5 * time.Second}

	// Reset the timer to exclude setup time
	b.ResetTimer()

	// Configure for concurrent execution
	b.SetParallelism(100)
	
	// Run the benchmark
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			resp, err := client.Get(server.URL + "/api/data")
			if err != nil {
				b.Fatalf("Request failed: %v", err)
			}
			resp.Body.Close()
			
			if resp.StatusCode != http.StatusOK {
				b.Fatalf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
			}
		}
	})
}

// Helper functions

// generateTestData generates test data for benchmarking
func generateTestData(count int) []map[string]interface{} {
	result := make([]map[string]interface{}, count)
	
	for i := 0; i < count; i++ {
		item := map[string]interface{}{
			"id":    i,
			"title": fmt.Sprintf("Item %d", i),
			"tags":  []string{"tag1", "tag2", "keyword"},
			"metadata": map[string]string{
				"created": time.Now().Format(time.RFC3339),
				"author":  "test-user",
			},
		}
		
		result[i] = item
	}
	
	return result
}

// complexQuery simulates a complex query operation
func complexQuery(data []map[string]interface{}, keyword string, limit int) []map[string]interface{} {
	result := make([]map[string]interface{}, 0, limit)
	
	for _, item := range data {
		// Check if the item contains the keyword in title
		if title, ok := item["title"].(string); ok && strings.Contains(title, keyword) {
			result = append(result, item)
		}
		
		// Check if the item contains the keyword in tags
		if tags, ok := item["tags"].([]string); ok {
			for _, tag := range tags {
				if tag == keyword && len(result) < limit {
					result = append(result, item)
					break
				}
			}
		}
		
		if len(result) >= limit {
			break
		}
	}
	
	return result
}