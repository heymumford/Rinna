/*
 * API Documentation handlers tests for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	
	"github.com/gorilla/mux"
)

func TestRegisterDocumentationRoutes(t *testing.T) {
	// Create a temporary directory for test files
	tmpDir, err := ioutil.TempDir("", "docs-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)
	
	// Create test swagger files
	docsDir := filepath.Join(tmpDir, "docs")
	swaggerUIDir := filepath.Join(docsDir, "swagger-ui")
	
	err = os.MkdirAll(swaggerUIDir, 0755)
	if err != nil {
		t.Fatalf("Failed to create swagger-ui dir: %v", err)
	}
	
	// Create a test swagger.json file
	jsonContent := `{"swagger":"2.0","info":{"title":"Test API"}}`
	err = ioutil.WriteFile(filepath.Join(docsDir, "swagger.json"), []byte(jsonContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write swagger.json: %v", err)
	}
	
	// Create a test swagger.yaml file
	yamlContent := `swagger: '2.0'
info:
  title: Test API`
	err = ioutil.WriteFile(filepath.Join(tmpDir, "swagger.yaml"), []byte(yamlContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write swagger.yaml: %v", err)
	}
	
	// Create a test index.html file for swagger-ui
	indexContent := `<!DOCTYPE html><html><body><h1>Swagger UI</h1></body></html>`
	err = ioutil.WriteFile(filepath.Join(swaggerUIDir, "index.html"), []byte(indexContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write index.html: %v", err)
	}
	
	// Create router and register routes
	r := mux.NewRouter()
	RegisterDocumentationRoutes(r, tmpDir)
	
	// Create test server
	ts := httptest.NewServer(r)
	defer ts.Close()
	
	// Test cases
	testCases := []struct {
		name           string
		path           string
		expectedStatus int
		expectedBody   string
		expectedHeader map[string]string
	}{
		{
			name:           "Swagger UI index redirect",
			path:           "/api/docs",
			expectedStatus: http.StatusFound,
			expectedHeader: map[string]string{
				"Location": "/api/docs/swagger-ui/",
			},
		},
		{
			name:           "Swagger UI index redirect with trailing slash",
			path:           "/api/docs/",
			expectedStatus: http.StatusFound,
			expectedHeader: map[string]string{
				"Location": "/api/docs/swagger-ui/",
			},
		},
		{
			name:           "Swagger JSON",
			path:           "/api/docs/swagger.json",
			expectedStatus: http.StatusOK,
			expectedBody:   jsonContent,
			expectedHeader: map[string]string{
				"Content-Type": "application/json",
			},
		},
		{
			name:           "Swagger YAML",
			path:           "/api/docs/swagger.yaml",
			expectedStatus: http.StatusOK,
			expectedBody:   yamlContent,
			expectedHeader: map[string]string{
				"Content-Type":        "application/x-yaml",
				"Content-Disposition": "attachment; filename=swagger.yaml",
			},
		},
	}
	
	// Run tests
	client := http.Client{
		// Don't follow redirects
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			return http.ErrUseLastResponse
		},
	}
	
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			resp, err := client.Get(ts.URL + tc.path)
			if err != nil {
				t.Fatalf("Failed to get %s: %v", tc.path, err)
			}
			defer resp.Body.Close()
			
			// Check status code
			if resp.StatusCode != tc.expectedStatus {
				t.Errorf("Expected status %d, got %d", tc.expectedStatus, resp.StatusCode)
			}
			
			// Check headers
			for k, v := range tc.expectedHeader {
				if resp.Header.Get(k) != v {
					t.Errorf("Expected header %s: %s, got %s", k, v, resp.Header.Get(k))
				}
			}
			
			// Check body if expected
			if tc.expectedBody != "" {
				body, err := ioutil.ReadAll(resp.Body)
				if err != nil {
					t.Fatalf("Failed to read body: %v", err)
				}
				
				if string(body) != tc.expectedBody {
					t.Errorf("Expected body %s, got %s", tc.expectedBody, string(body))
				}
			}
		})
	}
}