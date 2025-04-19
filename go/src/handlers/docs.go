/*
 * API Documentation handlers for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"net/http"
	"path/filepath"
	
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/go/pkg/logger"
)

// RegisterDocumentationRoutes registers routes for API documentation
func RegisterDocumentationRoutes(r *mux.Router, basePath string) {
	apiDocsPath := filepath.Join(basePath, "docs")
	swaggerUIPath := filepath.Join(apiDocsPath, "swagger-ui")
	
	// Serve Swagger UI static files
	r.PathPrefix("/api/docs/swagger-ui/").Handler(http.StripPrefix("/api/docs/swagger-ui/", 
		http.FileServer(http.Dir(swaggerUIPath))))
	
	// Serve Swagger JSON
	r.HandleFunc("/api/docs/swagger.json", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, filepath.Join(apiDocsPath, "swagger.json"))
	})
	
	// Redirect /api/docs to Swagger UI
	r.HandleFunc("/api/docs", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/api/docs/swagger-ui/", http.StatusFound)
	})
	
	// Redirect /api/docs/ to Swagger UI
	r.HandleFunc("/api/docs/", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/api/docs/swagger-ui/", http.StatusFound)
	})
	
	// Serve YAML for direct downloading
	r.HandleFunc("/api/docs/swagger.yaml", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/x-yaml")
		w.Header().Set("Content-Disposition", "attachment; filename=swagger.yaml")
		http.ServeFile(w, r, filepath.Join(basePath, "swagger.yaml"))
	})
	
	logger.Info("API documentation routes registered", logger.Field("base_path", basePath))
}