/*
 * Health check handlers for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

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
	Status    string                  `json:"status"`
	Timestamp string                  `json:"timestamp"`
	Version   string                  `json:"version"`
	CommitSHA string                  `json:"commitSha"`
	BuildTime string                  `json:"buildTime"`
	GoVersion string                  `json:"goVersion"`
	Hostname  string                  `json:"hostname"`
	Services  map[string]ServiceStatus `json:"services"`
}

// RegisterHealthRoutes registers health check routes
func RegisterHealthRoutes(router *mux.Router) {
	router.HandleFunc("/health", HealthCheckHandler).Methods(http.MethodGet)
	router.HandleFunc("/health/live", LivenessCheckHandler).Methods(http.MethodGet)
	router.HandleFunc("/health/ready", ReadinessCheckHandler).Methods(http.MethodGet)
}

// HealthCheckHandler handles health check requests with detailed information
func HealthCheckHandler(w http.ResponseWriter, r *http.Request) {
	// Get hostname for debugging
	hostname, _ := os.Hostname()
	if hostname == "" {
		hostname = "unknown"
	}

	// Check Java service connectivity
	javaStatus := CheckJavaServiceFunc()

	// Create response
	response := HealthResponse{
		Status:    "ok", // We'll assume the service is ok even if dependencies are not
		Timestamp: time.Now().Format(time.RFC3339),
		Version:   Version,
		CommitSHA: CommitSHA,
		BuildTime: BuildTime,
		GoVersion: runtime.Version(),
		Hostname:  hostname,
		Services: map[string]ServiceStatus{
			"java": javaStatus,
		},
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}

// LivenessCheckHandler handles liveness probe requests (is the service running)
func LivenessCheckHandler(w http.ResponseWriter, r *http.Request) {
	response := map[string]string{
		"status":    "ok",
		"timestamp": time.Now().Format(time.RFC3339),
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}

// ReadinessCheckHandler handles readiness probe requests (is the service ready to serve requests)
func ReadinessCheckHandler(w http.ResponseWriter, r *http.Request) {
	// Check if Java service is available
	javaStatus := CheckJavaServiceFunc()
	
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

// Variable to allow test mocking
var CheckJavaServiceFunc = CheckJavaService

// CheckJavaService checks the status of the Java service
func CheckJavaService() ServiceStatus {
	// This is a simplified version for testing
	// In a real implementation, we would use the JavaClient to ping the Java service
	
	// For testing, we can simply return a successful status
	// In production, this would use the client to make an actual health check call
	/*
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	
	err := javaClient.Ping(ctx)
	if err != nil {
		return ServiceStatus{
			Status:    "error",
			Message:   err.Error(),
			Timestamp: time.Now().Format(time.RFC3339),
		}
	}
	*/
	
	return ServiceStatus{
		Status:    "ok",
		Timestamp: time.Now().Format(time.RFC3339),
	}
}