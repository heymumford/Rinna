/*
 * Release handlers for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/gorilla/mux"
)

// RegisterReleaseRoutes registers release-related routes
func RegisterReleaseRoutes(router *mux.Router) {
	router.HandleFunc("/releases", ListReleasesHandler).Methods(http.MethodGet)
	router.HandleFunc("/releases", CreateReleaseHandler).Methods(http.MethodPost)
	router.HandleFunc("/releases/{id}", GetReleaseHandler).Methods(http.MethodGet)
	router.HandleFunc("/releases/{id}", UpdateReleaseHandler).Methods(http.MethodPut)
	router.HandleFunc("/releases/{id}/workitems", GetReleaseWorkItemsHandler).Methods(http.MethodGet)
}

// ListReleasesHandler handles listing releases
func ListReleasesHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"items":      []interface{}{},
		"totalCount": 0,
		"page":       1,
		"pageSize":   10,
	})
}

// CreateReleaseHandler handles creating a release
func CreateReleaseHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"id":          "rel-1234",
		"name":        "Version 1.0",
		"description": "Initial release",
		"status":      "PLANNED",
		"startDate":   "2025-01-01",
		"releaseDate": "2025-02-01",
		"createdAt":   "2025-01-01T00:00:00Z",
		"updatedAt":   "2025-01-01T00:00:00Z",
	})
}

// GetReleaseHandler handles getting a release
func GetReleaseHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"id":          id,
		"name":        "Version 1.0",
		"description": "Initial release",
		"status":      "PLANNED",
		"startDate":   "2025-01-01",
		"releaseDate": "2025-02-01",
		"createdAt":   "2025-01-01T00:00:00Z",
		"updatedAt":   "2025-01-01T00:00:00Z",
	})
}

// UpdateReleaseHandler handles updating a release
func UpdateReleaseHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"id":          id,
		"name":        "Version 1.0",
		"description": "Initial release",
		"status":      "PLANNED",
		"startDate":   "2025-01-01",
		"releaseDate": "2025-02-01",
		"createdAt":   "2025-01-01T00:00:00Z",
		"updatedAt":   "2025-01-01T00:00:00Z",
	})
}

// GetReleaseWorkItemsHandler handles getting work items for a release
func GetReleaseWorkItemsHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"releaseId":  id,
		"items":      []interface{}{},
		"totalCount": 0,
		"page":       1,
		"pageSize":   10,
	})
}