/*
 * Example health check handlers with Swagger annotations
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"encoding/json"
	"net/http"
	"time"
)

// This file demonstrates how to add Swagger annotations to API handlers

// ServiceHealthStatus represents the health status of a service for documentation
// swagger:model ServiceHealthStatus
type ServiceHealthStatus struct {
	// Status of the service (UP, DOWN, DEGRADED)
	// example: UP
	Status string `json:"status"`

	// Optional message with additional details
	// example: Service is running normally
	Message string `json:"message,omitempty"`

	// ISO8601 timestamp of when the status was checked
	// example: 2025-04-08T12:30:45Z
	Timestamp string `json:"timestamp"`
}

// HealthCheckResponse represents the complete health check response
// swagger:model HealthCheckResponse
type HealthCheckResponse struct {
	// Overall status of the API service
	// example: OK
	Status string `json:"status"`

	// ISO8601 timestamp of when the check was performed
	// example: 2025-04-08T12:30:45Z
	Timestamp string `json:"timestamp"`

	// Version of the application
	// example: 1.4.1
	Version string `json:"version"`

	// Git commit SHA
	// example: a1b2c3d4e5f6g7h8i9j0
	CommitSHA string `json:"commitSha"`

	// When the application was built
	// example: 2025-04-07T10:15:30Z
	BuildTime string `json:"buildTime"`

	// Go version used to build the application
	// example: go1.21.0
	GoVersion string `json:"goVersion"`

	// Hostname of the server
	// example: rinna-api-server-1
	Hostname string `json:"hostname"`

	// Status of individual services
	Services map[string]ServiceHealthStatus `json:"services"`
}

// swagger:route GET /health health getHealth
// Get system health status
// Returns detailed information about the health of all system components
// Responses:
//   200: HealthCheckResponse
//   500: ErrorResponse

// swagger:route GET /health/live health getLiveness
// Liveness probe
// Simple check to verify the service is running
// Responses:
//   200: LivenessResponse
//   500: ErrorResponse

// swagger:route GET /health/ready health getReadiness
// Readiness probe
// Check if service is ready to accept requests
// Responses:
//   200: ReadinessResponse
//   503: ErrorResponse

// LivenessResponse represents a simple response for liveness checks
// swagger:model LivenessResponse
type LivenessResponse struct {
	// Status of the service (UP)
	// example: UP
	Status string `json:"status"`

	// ISO8601 timestamp of when the check was performed
	// example: 2025-04-08T12:30:45Z
	Timestamp string `json:"timestamp"`
}

// ReadinessResponse represents a response for readiness checks
// swagger:model ReadinessResponse
type ReadinessResponse struct {
	// Status of the service (READY, NOT_READY)
	// example: READY
	Status string `json:"status"`

	// ISO8601 timestamp of when the check was performed
	// example: 2025-04-08T12:30:45Z
	Timestamp string `json:"timestamp"`

	// Status of individual services
	Services map[string]ServiceHealthStatus `json:"services"`
}

// ErrorResponse represents an error response
// swagger:model ErrorResponse
type ErrorResponse struct {
	// HTTP status code
	// example: 500
	Code int `json:"code"`

	// Error message
	// example: Internal server error
	Message string `json:"message"`

	// Optional additional error details
	Details []string `json:"details,omitempty"`
}

// ExampleLivenessHandler demonstrates a handler with Swagger annotations
func ExampleLivenessHandler(w http.ResponseWriter, r *http.Request) {
	// This is just an example function and is not actually registered with the router
	
	response := LivenessResponse{
		Status:    "UP",
		Timestamp: time.Now().Format(time.RFC3339),
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}

/*
To implement Swagger documentation for the entire API:

1. Add swagger:meta annotation to a main package comment
2. Add swagger:route annotations to handlers
3. Add swagger:model annotations to model types
4. Add tags and examples to struct fields
5. Use go-swagger to generate the Swagger specification:
   swagger generate spec -o ./swagger.json

Recommended workflow:
1. Start with the basic swagger.yaml file
2. Add annotations to handlers and models
3. Generate a complete specification with go-swagger
4. Validate the generated specification
5. Create an API documentation page with Swagger UI or ReDoc
*/