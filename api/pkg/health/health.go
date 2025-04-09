/*
 * Health check handlers for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package health

import (
	"encoding/json"
	"net/http"
	"os"
	"runtime"
	"sync"
	"time"

	"github.com/gorilla/mux"
)

// Version information is imported from version.go in this package

// ServiceStatus represents the status of a service
type ServiceStatus struct {
	Status    string `json:"status"`
	Message   string `json:"message,omitempty"`
	Timestamp string `json:"timestamp"`
}

// HealthResponse represents the health check response
type HealthResponse struct {
	Status      string                  `json:"status"`
	Timestamp   string                  `json:"timestamp"`
	Version     string                  `json:"version"`
	CommitSHA   string                  `json:"commitSha"`
	BuildTime   string                  `json:"buildTime"`
	GoVersion   string                  `json:"goVersion"`
	Hostname    string                  `json:"hostname"`
	Environment string                  `json:"environment,omitempty"`
	Uptime      string                  `json:"uptime,omitempty"`
	Memory      *MemoryStats            `json:"memory,omitempty"`
	Services    map[string]ServiceStatus `json:"services"`
}

// MemoryStats provides information about memory usage
type MemoryStats struct {
	Alloc      uint64 `json:"alloc"`      // Bytes allocated and still in use
	TotalAlloc uint64 `json:"totalAlloc"` // Bytes allocated (even if freed)
	Sys        uint64 `json:"sys"`        // Bytes obtained from system
	NumGC      uint32 `json:"numGC"`      // Number of completed GC cycles
}

// DependencyChecker defines the interface for checking dependencies
type DependencyChecker interface {
	CheckHealth() ServiceStatus
}

// JavaServiceChecker checks the health of the Java service
type JavaServiceChecker struct {
	// Config would normally be here
}

// CheckHealth checks if the Java service is healthy
func (c *JavaServiceChecker) CheckHealth() ServiceStatus {
	// This is a simplified version for testing
	// In a real implementation, we would make an actual API call
	// For now, we'll just return a successful status
	
	return ServiceStatus{
		Status:    "ok",
		Timestamp: time.Now().Format(time.RFC3339),
	}
}

// cacheEntry is a cached health check result
type cacheEntry struct {
	status      ServiceStatus
	expiration  time.Time
}

// dependencyCache manages cached health check results
type dependencyCache struct {
	mutex    sync.RWMutex
	entries  map[string]cacheEntry
	ttl      time.Duration
}

// get retrieves a cached value if it exists and is not expired
func (c *dependencyCache) get(key string) (ServiceStatus, bool) {
	c.mutex.RLock()
	defer c.mutex.RUnlock()
	
	entry, exists := c.entries[key]
	if !exists {
		return ServiceStatus{}, false
	}
	
	// Check if entry has expired
	if time.Now().After(entry.expiration) {
		return ServiceStatus{}, false
	}
	
	return entry.status, true
}

// set stores a value in the cache with an expiration time
func (c *dependencyCache) set(key string, status ServiceStatus) {
	c.mutex.Lock()
	defer c.mutex.Unlock()
	
	c.entries[key] = cacheEntry{
		status:     status,
		expiration: time.Now().Add(c.ttl),
	}
}

// Handler handles health check requests
type Handler struct {
	javaChecker DependencyChecker
	startTime   time.Time
	environment string
	cache       *dependencyCache
}

// NewHandler creates a new health check handler
func NewHandler(javaChecker DependencyChecker) *Handler {
	// Get environment from env var, default to development
	env := os.Getenv("RINNA_ENVIRONMENT")
	if env == "" {
		env = "development"
	}
	
	// Set default cache TTL to 30 seconds
	// In a production environment, this would be configurable
	cacheTTL := 30 * time.Second
	
	// Retrieve from environment if available
	if ttlEnv := os.Getenv("HEALTH_CACHE_TTL"); ttlEnv != "" {
		if parsed, err := time.ParseDuration(ttlEnv); err == nil {
			cacheTTL = parsed
		}
	}
	
	return &Handler{
		javaChecker: javaChecker,
		startTime:   time.Now(),
		environment: env,
		cache: &dependencyCache{
			entries: make(map[string]cacheEntry),
			ttl:     cacheTTL,
		},
	}
}

// RegisterRoutes registers health check routes
func (h *Handler) RegisterRoutes(router *mux.Router) {
	router.HandleFunc("/health", h.HandleHealth).Methods(http.MethodGet)
	router.HandleFunc("/health/live", h.HandleLiveness).Methods(http.MethodGet)
	router.HandleFunc("/health/ready", h.HandleReadiness).Methods(http.MethodGet)
}

// getOrUpdateDependencyStatus gets the dependency status from cache or updates it
func (h *Handler) getOrUpdateDependencyStatus(name string, checker DependencyChecker) ServiceStatus {
	// Try to get from cache first
	if status, found := h.cache.get(name); found {
		return status
	}
	
	// Not in cache, make the actual check
	status := checker.CheckHealth()
	
	// Store in cache for future requests
	h.cache.set(name, status)
	
	return status
}

// HandleHealth handles health check requests with detailed information
func (h *Handler) HandleHealth(w http.ResponseWriter, r *http.Request) {
	// Get hostname for debugging
	hostname, _ := os.Hostname()
	if hostname == "" {
		hostname = "unknown"
	}

	// Check Java service connectivity (using cache)
	javaStatus := h.getOrUpdateDependencyStatus("java", h.javaChecker)

	// Calculate uptime
	uptime := time.Since(h.startTime).String()

	// Collect memory statistics
	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)
	
	// Determine overall status based on dependencies
	status := "ok" 
	if javaStatus.Status != "ok" {
		status = "degraded"
	}

	// Create response
	response := HealthResponse{
		Status:      status,
		Timestamp:   time.Now().Format(time.RFC3339),
		Version:     Version,
		CommitSHA:   CommitSHA,
		BuildTime:   BuildTime,
		GoVersion:   runtime.Version(),
		Hostname:    hostname,
		Environment: h.environment,
		Uptime:      uptime,
		Memory: &MemoryStats{
			Alloc:      memStats.Alloc,
			TotalAlloc: memStats.TotalAlloc,
			Sys:        memStats.Sys,
			NumGC:      memStats.NumGC,
		},
		Services: map[string]ServiceStatus{
			"java": javaStatus,
		},
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}

// HandleLiveness handles liveness probe requests (is the service running)
func (h *Handler) HandleLiveness(w http.ResponseWriter, r *http.Request) {
	// Liveness check does not need to check dependencies
	// It only verifies the API server is running
	response := map[string]string{
		"status":    "ok",
		"timestamp": time.Now().Format(time.RFC3339),
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}

// HandleReadiness handles readiness probe requests (is the service ready to serve requests)
func (h *Handler) HandleReadiness(w http.ResponseWriter, r *http.Request) {
	// Check if Java service is available (using cache)
	javaStatus := h.getOrUpdateDependencyStatus("java", h.javaChecker)
	
	// Determine overall status based on dependencies
	status := "ok"
	httpStatus := http.StatusOK
	
	if javaStatus.Status != "ok" {
		status = "degraded"
		httpStatus = http.StatusServiceUnavailable
	}
	
	response := map[string]interface{}{
		"status":    status,
		"timestamp": time.Now().Format(time.RFC3339),
		"services": map[string]ServiceStatus{
			"java": javaStatus,
		},
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(httpStatus)
	json.NewEncoder(w).Encode(response)
}