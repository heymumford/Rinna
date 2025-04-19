/*
 * Project handlers for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/go/src/client"
	"github.com/heymumford/rinna/go/src/models"
)

// ProjectHandler handles project-related requests
type ProjectHandler struct {
	javaClient *client.JavaClient
}

// NewProjectHandler creates a new project handler
func NewProjectHandler(javaClient *client.JavaClient) *ProjectHandler {
	return &ProjectHandler{
		javaClient: javaClient,
	}
}

// RegisterProjectRoutes registers project-related routes
func RegisterProjectRoutes(router *mux.Router, javaClient *client.JavaClient) {
	handler := NewProjectHandler(javaClient)
	
	router.HandleFunc("/projects", handler.ListProjects).Methods(http.MethodGet)
	router.HandleFunc("/projects", handler.CreateProject).Methods(http.MethodPost)
	router.HandleFunc("/projects/{key}", handler.GetProject).Methods(http.MethodGet)
	router.HandleFunc("/projects/{key}", handler.UpdateProject).Methods(http.MethodPut)
	router.HandleFunc("/projects/{key}/workitems", handler.GetProjectWorkItems).Methods(http.MethodGet)
}

// ListProjects handles listing projects
func (h *ProjectHandler) ListProjects(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	activeOnlyStr := r.URL.Query().Get("activeOnly")
	pageStr := r.URL.Query().Get("page")
	pageSizeStr := r.URL.Query().Get("pageSize")

	// Set default values
	page := 1
	pageSize := 10
	activeOnly := false

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

	// Parse activeOnly parameter
	if activeOnlyStr != "" {
		parsedActiveOnly, err := strconv.ParseBool(activeOnlyStr)
		if err == nil {
			activeOnly = parsedActiveOnly
		}
	}

	// Call the Java service
	response, err := h.javaClient.ListProjects(r.Context(), page, pageSize, activeOnly)
	if err != nil {
		http.Error(w, "Failed to list projects: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// CreateProject handles creating a project
func (h *ProjectHandler) CreateProject(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.ProjectCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Validate request
	if request.Key == "" {
		http.Error(w, "Key is required", http.StatusBadRequest)
		return
	}
	if request.Name == "" {
		http.Error(w, "Name is required", http.StatusBadRequest)
		return
	}

	// Call the Java service
	project, err := h.javaClient.CreateProject(r.Context(), request)
	if err != nil {
		http.Error(w, "Failed to create project: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(project)
}

// GetProject handles getting a project
func (h *ProjectHandler) GetProject(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Call the Java service
	project, err := h.javaClient.GetProject(r.Context(), key)
	if err != nil {
		if err.Error() == "project not found: "+key {
			http.Error(w, "Project not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get project: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(project)
}

// UpdateProject handles updating a project
func (h *ProjectHandler) UpdateProject(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Parse request body
	var request models.ProjectUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Call the Java service
	project, err := h.javaClient.UpdateProject(r.Context(), key, request)
	if err != nil {
		if err.Error() == "project not found: "+key {
			http.Error(w, "Project not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to update project: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(project)
}

// GetProjectWorkItems handles getting work items for a project
func (h *ProjectHandler) GetProjectWorkItems(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

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
	response, err := h.javaClient.GetProjectWorkItems(r.Context(), key, status, page, pageSize)
	if err != nil {
		if err.Error() == "project not found: "+key {
			http.Error(w, "Project not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get project work items: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}