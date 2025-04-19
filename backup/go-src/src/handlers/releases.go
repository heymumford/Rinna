/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */

package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/client"
	"github.com/heymumford/rinna/api/internal/models"
)

// ReleaseHandler handles release-related requests
type ReleaseHandler struct {
	javaClient *client.JavaClient
}

// NewReleaseHandler creates a new release handler
func NewReleaseHandler(javaClient *client.JavaClient) *ReleaseHandler {
	return &ReleaseHandler{
		javaClient: javaClient,
	}
}

// RegisterReleaseRoutes registers release-related routes
func RegisterReleaseRoutes(router *mux.Router, javaClient *client.JavaClient) {
	handler := NewReleaseHandler(javaClient)
	
	router.HandleFunc("/releases", handler.ListReleases).Methods(http.MethodGet)
	router.HandleFunc("/releases", handler.CreateRelease).Methods(http.MethodPost)
	router.HandleFunc("/releases/{id}", handler.GetRelease).Methods(http.MethodGet)
	router.HandleFunc("/releases/{id}", handler.UpdateRelease).Methods(http.MethodPut)
	router.HandleFunc("/releases/{id}/workitems", handler.GetReleaseWorkItems).Methods(http.MethodGet)
}

// ListReleases handles listing releases
func (h *ReleaseHandler) ListReleases(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	status := r.URL.Query().Get("status")
	pageStr := r.URL.Query().Get("page")
	pageSizeStr := r.URL.Query().Get("pageSize")

	// Set default values
	page := 1
	pageSize := 10

	// Parse page parameter
	if pageStr != "" {
		parsedPage, err := strconv.Atoi(pageStr)
		if err != nil || parsedPage < 1 {
			http.Error(w, "Invalid page parameter", http.StatusBadRequest)
			return
		}
		page = parsedPage
	}

	// Parse pageSize parameter
	if pageSizeStr != "" {
		parsedPageSize, err := strconv.Atoi(pageSizeStr)
		if err != nil || parsedPageSize < 1 || parsedPageSize > 100 {
			http.Error(w, "Invalid pageSize parameter (must be between 1 and 100)", http.StatusBadRequest)
			return
		}
		pageSize = parsedPageSize
	}

	// Call the Java service
	response, err := h.javaClient.ListReleases(r.Context(), page, pageSize, status)
	if err != nil {
		http.Error(w, "Failed to list releases: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// CreateRelease handles creating a release
func (h *ReleaseHandler) CreateRelease(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.ReleaseCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Validate request
	if request.Name == "" {
		http.Error(w, "Name is required", http.StatusBadRequest)
		return
	}
	if request.Version == "" {
		http.Error(w, "Version is required", http.StatusBadRequest)
		return
	}

	// Set default values if needed
	if request.Status == "" {
		request.Status = models.ReleaseStatusPlanned
	}

	// Call the Java service
	release, err := h.javaClient.CreateRelease(r.Context(), request)
	if err != nil {
		http.Error(w, "Failed to create release: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(release)
}

// GetRelease handles getting a release
func (h *ReleaseHandler) GetRelease(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Call the Java service
	release, err := h.javaClient.GetRelease(r.Context(), id)
	if err != nil {
		if err.Error() == "release not found: "+id {
			http.Error(w, "Release not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get release: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(release)
}

// UpdateRelease handles updating a release
func (h *ReleaseHandler) UpdateRelease(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Parse request body
	var request models.ReleaseUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Call the Java service
	release, err := h.javaClient.UpdateRelease(r.Context(), id, request)
	if err != nil {
		if err.Error() == "release not found: "+id {
			http.Error(w, "Release not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to update release: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(release)
}

// GetReleaseWorkItems handles getting work items for a release
func (h *ReleaseHandler) GetReleaseWorkItems(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Parse query parameters
	status := r.URL.Query().Get("status")
	pageStr := r.URL.Query().Get("page")
	pageSizeStr := r.URL.Query().Get("pageSize")

	// Set default values
	page := 1
	pageSize := 10

	// Parse page parameter
	if pageStr != "" {
		parsedPage, err := strconv.Atoi(pageStr)
		if err != nil || parsedPage < 1 {
			http.Error(w, "Invalid page parameter", http.StatusBadRequest)
			return
		}
		page = parsedPage
	}

	// Parse pageSize parameter
	if pageSizeStr != "" {
		parsedPageSize, err := strconv.Atoi(pageSizeStr)
		if err != nil || parsedPageSize < 1 || parsedPageSize > 100 {
			http.Error(w, "Invalid pageSize parameter (must be between 1 and 100)", http.StatusBadRequest)
			return
		}
		pageSize = parsedPageSize
	}

	// Call the Java service
	response, err := h.javaClient.GetReleaseWorkItems(r.Context(), id, status, page, pageSize)
	if err != nil {
		if err.Error() == "release not found: "+id {
			http.Error(w, "Release not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get release work items: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}