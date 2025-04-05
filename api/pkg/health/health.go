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
	"time"

	"github.com/gorilla/mux"
)

// Build information - these will be set during compilation
var (
	Version   = "dev"
	CommitSHA = "unknown"
	BuildTime = "unknown"
)

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

// Handler handles health check requests
type Handler struct {
	javaChecker DependencyChecker
	startTime   time.Time
	environment string
}

// NewHandler creates a new health check handler
func NewHandler(javaChecker DependencyChecker) *Handler {
	// Get environment from env var, default to development
	env := os.Getenv("RINNA_ENVIRONMENT")
	if env == "" {
		env = "development"
	}
	
	return &Handler{
		javaChecker: javaChecker,
		startTime:   time.Now(),
		environment: env,
	}
}

// RegisterRoutes registers health check routes
func (h *Handler) RegisterRoutes(router *mux.Router) {
	router.HandleFunc("/health", h.HandleHealth).Methods(http.MethodGet)
	router.HandleFunc("/health/live", h.HandleLiveness).Methods(http.MethodGet)
	router.HandleFunc("/health/ready", h.HandleReadiness).Methods(http.MethodGet)
}

// HandleHealth handles health check requests with detailed information
func (h *Handler) HandleHealth(w http.ResponseWriter, r *http.Request) {
	// Get hostname for debugging
	hostname, _ := os.Hostname()
	if hostname == "" {
		hostname = "unknown"
	}

	// Check Java service connectivity
	javaStatus := h.javaChecker.CheckHealth()

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
	// Check if Java service is available
	javaStatus := h.javaChecker.CheckHealth()
	
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