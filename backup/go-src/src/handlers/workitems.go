/*
 * Work item handlers for the Rinna API
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
	"github.com/heymumford/rinna/api/internal/client"
	"github.com/heymumford/rinna/api/internal/models"
)

// WorkItemHandler handles work item-related requests
type WorkItemHandler struct {
	javaClient *client.JavaClient
}

// NewWorkItemHandler creates a new work item handler
func NewWorkItemHandler(javaClient *client.JavaClient) *WorkItemHandler {
	return &WorkItemHandler{
		javaClient: javaClient,
	}
}

// RegisterWorkItemRoutes registers work item routes
func RegisterWorkItemRoutes(router *mux.Router, javaClient *client.JavaClient) {
	// Create handler with dependencies
	handler := NewWorkItemHandler(javaClient)

	// Register routes
	router.HandleFunc("/workitems", handler.ListWorkItems).Methods(http.MethodGet)
	router.HandleFunc("/workitems", handler.CreateWorkItem).Methods(http.MethodPost)
	router.HandleFunc("/workitems/{id}", handler.GetWorkItem).Methods(http.MethodGet)
	router.HandleFunc("/workitems/{id}", handler.UpdateWorkItem).Methods(http.MethodPut)
	router.HandleFunc("/workitems/{id}/transitions", handler.TransitionWorkItem).Methods(http.MethodPost)
}

// ListWorkItems lists work items
func (h *WorkItemHandler) ListWorkItems(w http.ResponseWriter, r *http.Request) {
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
	response, err := h.javaClient.ListWorkItems(r.Context(), status, page, pageSize)
	if err != nil {
		http.Error(w, "Failed to list work items: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// CreateWorkItem creates a new work item
func (h *WorkItemHandler) CreateWorkItem(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.WorkItemCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Validate request
	if request.Title == "" {
		http.Error(w, "Title is required", http.StatusBadRequest)
		return
	}

	// Set default values
	if request.Type == "" {
		request.Type = models.WorkItemTypeFeature
	}
	if request.Priority == "" {
		request.Priority = models.PriorityMedium
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

// GetWorkItem gets a work item by ID
func (h *WorkItemHandler) GetWorkItem(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Call the Java service
	workItem, err := h.javaClient.GetWorkItem(r.Context(), id)
	if err != nil {
		if err.Error() == "work item not found: "+id {
			http.Error(w, "Work item not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}

// UpdateWorkItem updates a work item
func (h *WorkItemHandler) UpdateWorkItem(w http.ResponseWriter, r *http.Request) {
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
		if err.Error() == "work item not found: "+id {
			http.Error(w, "Work item not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to update work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}

// TransitionWorkItem transitions a work item
func (h *WorkItemHandler) TransitionWorkItem(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Parse request body
	var request models.WorkItemTransitionRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Validate request
	if request.ToState == "" {
		http.Error(w, "ToState is required", http.StatusBadRequest)
		return
	}

	// Call the Java service
	workItem, err := h.javaClient.TransitionWorkItem(r.Context(), id, request)
	if err != nil {
		if err.Error() == "work item not found: "+id {
			http.Error(w, "Work item not found", http.StatusNotFound)
			return
		}
		if err.Error() == "invalid transition" {
			http.Error(w, "Invalid state transition", http.StatusBadRequest)
			return
		}
		http.Error(w, "Failed to transition work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}