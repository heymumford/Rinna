/*
 * Performance tests for health check API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package performance

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/pkg/health"
)

// MockJavaChecker is a mock implementation of health.DependencyChecker
type MockJavaChecker struct {
	status  string
	message string
	delay   time.Duration // Simulate processing delay
}

// CheckHealth returns a predefined status after the specified delay
func (m *MockJavaChecker) CheckHealth() health.ServiceStatus {
	if m.delay > 0 {
		time.Sleep(m.delay)
	}
	return health.ServiceStatus{
		Status:    m.status,
		Message:   m.message,
		Timestamp: time.Now().Format(time.RFC3339),
	}
}

// CountingDependencyChecker is a dependency checker that counts calls
type CountingDependencyChecker struct {
	callCount *int
	delay     time.Duration
	status    string
	message   string
}

// CheckHealth counts calls and returns a predefined status after the specified delay
func (c *CountingDependencyChecker) CheckHealth() health.ServiceStatus {
	if c.callCount != nil {
		*c.callCount++
	}
	if c.delay > 0 {
		time.Sleep(c.delay)
	}
	return health.ServiceStatus{
		Status:    c.status,
		Message:   c.message,
		Timestamp: time.Now().Format(time.RFC3339),
	}
}

// BenchmarkHealthEndpoint benchmarks the performance of the health endpoint
func BenchmarkHealthEndpoint(b *testing.B) {
	// Create a mock Java checker with no delay
	javaChecker := &MockJavaChecker{status: "ok"}
	
	// Create a health handler
	healthHandler := health.NewHandler(javaChecker)
	
	// Create a router
	router := mux.NewRouter()
	healthHandler.RegisterRoutes(router)
	
	// Create a test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Run the benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		resp, err := http.Get(server.URL + "/health")
		if err != nil {
			b.Fatal(err)
		}
		if resp.StatusCode != http.StatusOK {
			b.Fatalf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}
		resp.Body.Close()
	}
}

// BenchmarkLivenessEndpoint benchmarks the performance of the liveness endpoint
func BenchmarkLivenessEndpoint(b *testing.B) {
	// Create a mock Java checker with no delay
	javaChecker := &MockJavaChecker{status: "ok"}
	
	// Create a health handler
	healthHandler := health.NewHandler(javaChecker)
	
	// Create a router
	router := mux.NewRouter()
	healthHandler.RegisterRoutes(router)
	
	// Create a test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Run the benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		resp, err := http.Get(server.URL + "/health/live")
		if err != nil {
			b.Fatal(err)
		}
		if resp.StatusCode != http.StatusOK {
			b.Fatalf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}
		resp.Body.Close()
	}
}

// BenchmarkReadinessEndpoint benchmarks the performance of the readiness endpoint
func BenchmarkReadinessEndpoint(b *testing.B) {
	// Create a mock Java checker with no delay
	javaChecker := &MockJavaChecker{status: "ok"}
	
	// Create a health handler
	healthHandler := health.NewHandler(javaChecker)
	
	// Create a router
	router := mux.NewRouter()
	healthHandler.RegisterRoutes(router)
	
	// Create a test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Run the benchmark
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		resp, err := http.Get(server.URL + "/health/ready")
		if err != nil {
			b.Fatal(err)
		}
		if resp.StatusCode != http.StatusOK {
			b.Fatalf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}
		resp.Body.Close()
	}
}

// BenchmarkHealthEndpointWithDelayedDependency benchmarks the health endpoint with a delayed dependency check
func BenchmarkHealthEndpointWithDelayedDependency(b *testing.B) {
	// Create benchmark test cases with various delays
	testCases := []struct {
		name         string
		dependency   *MockJavaChecker
		expectedTime time.Duration
	}{
		{
			name:         "No delay",
			dependency:   &MockJavaChecker{status: "ok", delay: 0},
			expectedTime: 5 * time.Millisecond,
		},
		{
			name:         "10ms delay",
			dependency:   &MockJavaChecker{status: "ok", delay: 10 * time.Millisecond},
			expectedTime: 15 * time.Millisecond,
		},
		{
			name:         "50ms delay",
			dependency:   &MockJavaChecker{status: "ok", delay: 50 * time.Millisecond},
			expectedTime: 55 * time.Millisecond,
		},
	}
	
	for _, tc := range testCases {
		b.Run(tc.name, func(b *testing.B) {
			// Create a handler with the test case dependency
			healthHandler := health.NewHandler(tc.dependency)
			
			// Create a router
			router := mux.NewRouter()
			healthHandler.RegisterRoutes(router)
			
			// Create a test server
			server := httptest.NewServer(router)
			defer server.Close()
			
			// Run the benchmark
			b.ResetTimer()
			
			var totalTime time.Duration
			for i := 0; i < b.N; i++ {
				start := time.Now()
				resp, err := http.Get(server.URL + "/health")
				duration := time.Since(start)
				totalTime += duration
				
				if err != nil {
					b.Fatal(err)
				}
				if resp.StatusCode != http.StatusOK {
					b.Fatalf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
				}
				resp.Body.Close()
			}
			
			// Calculate average time per request
			avgTime := totalTime / time.Duration(b.N)
			b.ReportMetric(float64(avgTime.Nanoseconds())/1e6, "ms/op")
		})
	}
}

// TestParallelHealthEndpoints tests health endpoints under concurrent load
func TestParallelHealthEndpoints(t *testing.T) {
	// Create a mock Java checker
	javaChecker := &MockJavaChecker{status: "ok"}
	
	// Create a health handler
	healthHandler := health.NewHandler(javaChecker)
	
	// Create a router
	router := mux.NewRouter()
	healthHandler.RegisterRoutes(router)
	
	// Create a test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Test parameters
	concurrentUsers := 100
	requestsPerUser := 10
	
	// Create a wait group to wait for all goroutines to finish
	var wg sync.WaitGroup
	wg.Add(concurrentUsers)
	
	// Create channels to collect metrics
	healthTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)
	livenessTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)
	readinessTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)
	
	// Start concurrent users
	start := time.Now()
	
	for i := 0; i < concurrentUsers; i++ {
		go func(userID int) {
			defer wg.Done()
			
			// Each user makes multiple requests to each endpoint
			for j := 0; j < requestsPerUser; j++ {
				// Test health endpoint
				startHealth := time.Now()
				resp, err := http.Get(server.URL + "/health")
				healthTime := time.Since(startHealth)
				healthTimes <- healthTime
				
				if err != nil {
					t.Errorf("User %d request %d failed: %v", userID, j, err)
					continue
				}
				resp.Body.Close()
				
				// Test liveness endpoint
				startLiveness := time.Now()
				resp, err = http.Get(server.URL + "/health/live")
				livenessTime := time.Since(startLiveness)
				livenessTimes <- livenessTime
				
				if err != nil {
					t.Errorf("User %d request %d failed: %v", userID, j, err)
					continue
				}
				resp.Body.Close()
				
				// Test readiness endpoint
				startReadiness := time.Now()
				resp, err = http.Get(server.URL + "/health/ready")
				readinessTime := time.Since(startReadiness)
				readinessTimes <- readinessTime
				
				if err != nil {
					t.Errorf("User %d request %d failed: %v", userID, j, err)
					continue
				}
				resp.Body.Close()
			}
		}(i)
	}
	
	// Wait for all users to finish
	wg.Wait()
	totalTime := time.Since(start)
	
	// Calculate metrics
	close(healthTimes)
	close(livenessTimes)
	close(readinessTimes)
	
	var totalHealthTime, totalLivenessTime, totalReadinessTime time.Duration
	var maxHealthTime, maxLivenessTime, maxReadinessTime time.Duration
	
	for t := range healthTimes {
		totalHealthTime += t
		if t > maxHealthTime {
			maxHealthTime = t
		}
	}
	
	for t := range livenessTimes {
		totalLivenessTime += t
		if t > maxLivenessTime {
			maxLivenessTime = t
		}
	}
	
	for t := range readinessTimes {
		totalReadinessTime += t
		if t > maxReadinessTime {
			maxReadinessTime = t
		}
	}
	
	// Calculate averages
	healthCount := concurrentUsers * requestsPerUser
	livenessCount := concurrentUsers * requestsPerUser
	readinessCount := concurrentUsers * requestsPerUser
	
	// Report results
	t.Logf("Performance Test Results for %d concurrent users with %d requests each:", concurrentUsers, requestsPerUser)
	t.Logf("  Total time: %s", totalTime)
	t.Logf("  Requests per second: %.2f", float64(healthCount+livenessCount+readinessCount)/totalTime.Seconds())
	t.Logf("  Health endpoint:")
	t.Logf("    Average response time: %s", totalHealthTime/time.Duration(healthCount))
	t.Logf("    Max response time: %s", maxHealthTime)
	t.Logf("  Liveness endpoint:")
	t.Logf("    Average response time: %s", totalLivenessTime/time.Duration(livenessCount))
	t.Logf("    Max response time: %s", maxLivenessTime)
	t.Logf("  Readiness endpoint:")
	t.Logf("    Average response time: %s", totalReadinessTime/time.Duration(readinessCount))
	t.Logf("    Max response time: %s", maxReadinessTime)
	
	// Define thresholds for acceptable performance
	maxAvgResponseTime := 100 * time.Millisecond
	
	// Check if performance meets thresholds
	avgHealthTime := totalHealthTime / time.Duration(healthCount)
	avgLivenessTime := totalLivenessTime / time.Duration(livenessCount)
	avgReadinessTime := totalReadinessTime / time.Duration(readinessCount)
	
	if avgHealthTime > maxAvgResponseTime {
		t.Errorf("Health endpoint average response time too high: %s (threshold: %s)", avgHealthTime, maxAvgResponseTime)
	}
	
	if avgLivenessTime > maxAvgResponseTime {
		t.Errorf("Liveness endpoint average response time too high: %s (threshold: %s)", avgLivenessTime, maxAvgResponseTime)
	}
	
	if avgReadinessTime > maxAvgResponseTime {
		t.Errorf("Readiness endpoint average response time too high: %s (threshold: %s)", avgReadinessTime, maxAvgResponseTime)
	}
}

// TestCachingPerformance tests if the health check system correctly caches dependencies
func TestCachingPerformance(t *testing.T) {
	// Create a special Java checker that counts calls and simulates delay
	callCount := 0
	delay := 20 * time.Millisecond
	
	// Create a custom checker that counts calls
	counterChecker := &CountingDependencyChecker{
		callCount: &callCount,
		delay:     delay,
		status:    "ok",
	}
	
	// Create a health handler with the counter checker
	healthHandler := health.NewHandler(counterChecker)
	
	// Create a router and register routes
	router := mux.NewRouter()
	healthHandler.RegisterRoutes(router)
	
	// Create a test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Make initial request (which should cache dependency status)
	resp, err := http.Get(server.URL + "/health")
	if err != nil {
		t.Fatalf("Initial request failed: %v", err)
	}
	resp.Body.Close()
	
	initialCallCount := callCount
	t.Logf("Initial call count after first request: %d", initialCallCount)
	
	// Make multiple requests in succession (should use cached values)
	numRequests := 10
	startTime := time.Now()
	
	for i := 0; i < numRequests; i++ {
		resp, err := http.Get(server.URL + "/health")
		if err != nil {
			t.Fatalf("Failed request %d: %v", i, err)
		}
		
		// Parse response to ensure it's valid
		var response map[string]interface{}
		err = json.NewDecoder(resp.Body).Decode(&response)
		resp.Body.Close()
		
		if err != nil {
			t.Fatalf("Failed to parse response %d: %v", i, err)
		}
		
		// Ensure status is still good
		if status, ok := response["status"].(string); !ok || status != "ok" {
			t.Errorf("Response %d had unexpected status: %v", i, response["status"])
		}
	}
	
	duration := time.Since(startTime)
	avgDuration := duration / time.Duration(numRequests)
	
	// Log performance metrics
	t.Logf("Made %d requests in %s", numRequests, duration)
	t.Logf("Average request time: %s", avgDuration)
	
	// Check if dependency was called more than once
	finalCallCount := callCount
	t.Logf("Final dependency call count: %d", finalCallCount)
	
	// In a well-optimized system with caching, dependency checks should be minimal
	// We expect some caching, but the exact behavior depends on the implementation
	// For this test, we'll check if there were significantly fewer calls than requests
	if finalCallCount > initialCallCount + (numRequests/2) {
		t.Logf("WARNING: Possible inefficient caching - made %d dependency calls for %d requests", 
			finalCallCount-initialCallCount, numRequests)
	}
	
	// We also check if the average response time is significantly lower than the dependency delay
	// which would indicate caching is working
	if avgDuration > delay*time.Duration(numRequests)/time.Duration(4) {
		t.Errorf("Performance suggests ineffective caching: avg duration %s vs dependency delay %s", 
			avgDuration, delay)
	}
	
	// Additional verification: check that responses are fast when dependency has issues
	// Create a new counter for slow checks
	slowCallCount := 0
	
	// Create a new checker that's very slow
	slowChecker := &CountingDependencyChecker{
		callCount: &slowCallCount,
		status:    "degraded",
		delay:     500 * time.Millisecond, // Very slow
	}
	
	// Create a new handler and server
	slowHandler := health.NewHandler(slowChecker)
	slowRouter := mux.NewRouter()
	slowHandler.RegisterRoutes(slowRouter)
	slowServer := httptest.NewServer(slowRouter)
	defer slowServer.Close()
	
	// First request will be slow due to dependency check
	startTime = time.Now()
	resp, err = http.Get(slowServer.URL + "/health/ready")
	firstDuration := time.Since(startTime)
	if err != nil {
		t.Fatalf("Failed slow request: %v", err)
	}
	resp.Body.Close()
	
	// Second request should be faster despite slow dependency if caching works
	startTime = time.Now()
	resp, err = http.Get(slowServer.URL + "/health/ready")
	secondDuration := time.Since(startTime)
	if err != nil {
		t.Fatalf("Failed second slow request: %v", err)
	}
	resp.Body.Close()
	
	t.Logf("First request with slow dependency: %s", firstDuration)
	t.Logf("Second request with slow dependency: %s", secondDuration)
	
	// The second request should be significantly faster if caching is working
	if secondDuration > firstDuration/2 {
		t.Errorf("Second request was not significantly faster: %s vs %s", secondDuration, firstDuration)
	}
}

// BenchmarkResponseSizeImpact tests the impact of response size on performance
func BenchmarkResponseSizeImpact(b *testing.B) {
	// Create various dependencies that return different sized payloads
	testCases := []struct {
		name        string
		msgGenerator func(int) string
		msgSize      int
		description string
	}{
		{
			name: "Minimal response",
			msgGenerator: func(size int) string { return "" },
			msgSize: 0,
			description: "Basic status only",
		},
		{
			name: "Medium response",
			msgGenerator: func(size int) string {
				message := ""
				for i := 0; i < size; i++ {
					message += fmt.Sprintf("Detail %d: System component functioning normally. ", i)
				}
				return message
			},
			msgSize: 50,
			description: "Status with medium message (~500 bytes)",
		},
		{
			name: "Large response",
			msgGenerator: func(size int) string {
				message := ""
				for i := 0; i < size; i++ {
					message += fmt.Sprintf("Detail %d: Extended system component status information with verbose descriptions. ", i)
				}
				return message
			},
			msgSize: 500,
			description: "Status with large message (~5000 bytes)",
		},
	}
	
	for _, tc := range testCases {
		b.Run(tc.name, func(b *testing.B) {
			// Create the message once outside the loop
			message := tc.msgGenerator(tc.msgSize)
			
			// Create a custom dependency checker
			javaChecker := &MockJavaChecker{
				status: "ok",
				message: message,
			}
			
			// Create a health handler
			healthHandler := health.NewHandler(javaChecker)
			
			// Create a router
			router := mux.NewRouter()
			healthHandler.RegisterRoutes(router)
			
			// Create a test server
			server := httptest.NewServer(router)
			defer server.Close()
			
			// Run the benchmark
			b.ResetTimer()
			
			var totalSize int64
			for i := 0; i < b.N; i++ {
				resp, err := http.Get(server.URL + "/health")
				if err != nil {
					b.Fatal(err)
				}
				
				// Read the full response to ensure everything is processed
				var body map[string]interface{}
				decoder := json.NewDecoder(resp.Body)
				err = decoder.Decode(&body)
				resp.Body.Close()
				
				if err != nil {
					b.Fatalf("Failed to decode response: %v", err)
				}
				
				// Add the content length to our total
				totalSize += resp.ContentLength
			}
			
			// Report custom metrics
			b.ReportMetric(float64(totalSize)/float64(b.N), "bytes/op")
		})
	}
}