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

	"github.com/gorilla/mux"
)

// RegisterProjectRoutes registers project-related routes
func RegisterProjectRoutes(router *mux.Router) {
	router.HandleFunc("/projects", ListProjectsHandler).Methods(http.MethodGet)
	router.HandleFunc("/projects", CreateProjectHandler).Methods(http.MethodPost)
	router.HandleFunc("/projects/{key}", GetProjectHandler).Methods(http.MethodGet)
	router.HandleFunc("/projects/{key}", UpdateProjectHandler).Methods(http.MethodPut)
	router.HandleFunc("/projects/{key}/workitems", GetProjectWorkItemsHandler).Methods(http.MethodGet)
}

// ListProjectsHandler handles listing projects
func ListProjectsHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"items": []map[string]interface{}{
			{
				"id":          "123e4567-e89b-12d3-a456-426614174000",
				"key":         "DEMO",
				"name":        "Demo Project",
				"description": "A demo project for testing",
				"active":      true,
				"createdAt":   "2025-01-01T00:00:00Z",
				"updatedAt":   "2025-01-01T00:00:00Z",
			},
		},
		"totalCount": 1,
	})
}

// CreateProjectHandler handles creating a project
func CreateProjectHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"id":          "123e4567-e89b-12d3-a456-426614174000",
		"key":         "NEW",
		"name":        "New Project",
		"description": "A new project",
		"active":      true,
		"createdAt":   "2025-01-01T00:00:00Z",
		"updatedAt":   "2025-01-01T00:00:00Z",
	})
}

// GetProjectHandler handles getting a project
func GetProjectHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"id":          "123e4567-e89b-12d3-a456-426614174000",
		"key":         key,
		"name":        "Demo Project",
		"description": "A demo project for testing",
		"active":      true,
		"createdAt":   "2025-01-01T00:00:00Z",
		"updatedAt":   "2025-01-01T00:00:00Z",
	})
}

// UpdateProjectHandler handles updating a project
func UpdateProjectHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"id":          "123e4567-e89b-12d3-a456-426614174000",
		"key":         key,
		"name":        "Updated Project",
		"description": "An updated project",
		"active":      true,
		"createdAt":   "2025-01-01T00:00:00Z",
		"updatedAt":   "2025-01-01T00:00:00Z",
	})
}

// GetProjectWorkItemsHandler handles getting work items for a project
func GetProjectWorkItemsHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement

	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Temporary mock response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"projectKey": key,
		"items":      []interface{}{},
		"totalCount": 0,
		"page":       1,
		"pageSize":   10,
	})
}