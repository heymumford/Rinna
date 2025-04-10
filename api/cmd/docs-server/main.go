/*
 * API Documentation server for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package main

import (
	"flag"
	"fmt"
	"html/template"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/russross/blackfriday/v2"
)

// Markdown to HTML renderer
func renderMarkdown(w http.ResponseWriter, r *http.Request, filePath string, title string) {
	// Read markdown file
	content, err := ioutil.ReadFile(filePath)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error reading file: %v", err), http.StatusInternalServerError)
		return
	}

	// Convert markdown to HTML
	html := blackfriday.Run(content)

	// HTML template for rendering
	tmpl := `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{{.Title}} - Rinna API Documentation</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.6;
      color: #333;
      max-width: 900px;
      margin: 0 auto;
      padding: 20px;
    }
    .header {
      background-color: #f8f9fa;
      border-bottom: 1px solid #e9ecef;
      margin-bottom: 20px;
      padding: 10px 20px;
    }
    .header h1 {
      font-size: 24px;
      color: #1a1a1a;
      margin: 0;
    }
    .header p {
      color: #6c757d;
      margin: 5px 0 0 0;
    }
    .footer {
      margin-top: 40px;
      border-top: 1px solid #e9ecef;
      padding: 20px;
      text-align: center;
      font-size: 12px;
      color: #6c757d;
    }
    pre {
      background-color: #f8f9fa;
      border: 1px solid #e9ecef;
      border-radius: 4px;
      padding: 10px;
      overflow-x: auto;
    }
    code {
      font-family: SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
      padding: 0.2em 0.4em;
      background-color: #f8f9fa;
      border-radius: 3px;
    }
    pre code {
      padding: 0;
      background-color: transparent;
    }
    blockquote {
      border-left: 4px solid #e9ecef;
      padding-left: 1em;
      margin-left: 0;
      color: #6c757d;
    }
    table {
      border-collapse: collapse;
      width: 100%;
      margin-bottom: 1rem;
    }
    table, th, td {
      border: 1px solid #e9ecef;
    }
    th, td {
      padding: 0.75rem;
      text-align: left;
    }
    th {
      background-color: #f8f9fa;
    }
    .content {
      padding: 20px;
    }
    .content img {
      max-width: 100%;
      height: auto;
    }
    .nav {
      margin-bottom: 20px;
    }
    .nav a {
      display: inline-block;
      padding: 8px 16px;
      margin-right: 10px;
      background-color: #f8f9fa;
      border-radius: 4px;
      text-decoration: none;
      color: #0074d9;
    }
    .nav a:hover {
      background-color: #e9ecef;
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>{{.Title}}</h1>
    <p>Rinna API Documentation - v1.6.6</p>
  </div>

  <div class="nav">
    <a href="/api/docs">Swagger UI</a>
    <a href="/api/docs/examples">Examples</a>
    <a href="/api/docs/security-guide">Security Guide</a>
    <a href="/api/docs/swagger.yaml">Download YAML</a>
    <a href="/api/docs/swagger.json">Download JSON</a>
  </div>

  <div class="content">
    {{.Content}}
  </div>

  <div class="footer">
    <p>&copy; 2025 Eric C. Mumford - Licensed under MIT</p>
    <p>Rinna: A clean, compact solution for product, project, development, and quality management!</p>
  </div>
</body>
</html>`

	// Parse and execute template
	t, err := template.New("markdown").Parse(tmpl)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error parsing template: %v", err), http.StatusInternalServerError)
		return
	}

	data := struct {
		Title   string
		Content template.HTML
	}{
		Title:   title,
		Content: template.HTML(html),
	}

	w.Header().Set("Content-Type", "text/html")
	err = t.Execute(w, data)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error executing template: %v", err), http.StatusInternalServerError)
		return
	}
}

// Index page handler
func indexHandler(apiDir string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		tmpl := `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Rinna API Documentation</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.6;
      color: #333;
      max-width: 900px;
      margin: 0 auto;
      padding: 20px;
    }
    .header {
      background-color: #f8f9fa;
      border-bottom: 1px solid #e9ecef;
      margin-bottom: 20px;
      padding: 10px 20px;
    }
    .header h1 {
      font-size: 28px;
      color: #1a1a1a;
      margin: 0;
    }
    .header p {
      color: #6c757d;
      margin: 5px 0 0 0;
      font-size: 16px;
    }
    .main-content {
      display: flex;
      flex-wrap: wrap;
      gap: 20px;
    }
    .card {
      flex: 1 1 300px;
      border: 1px solid #e9ecef;
      border-radius: 4px;
      padding: 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.05);
      background-color: white;
    }
    .card h2 {
      margin-top: 0;
      color: #0074d9;
    }
    .card p {
      margin-bottom: 15px;
    }
    .card a {
      display: inline-block;
      padding: 8px 16px;
      background-color: #0074d9;
      color: white;
      text-decoration: none;
      border-radius: 4px;
    }
    .card a:hover {
      background-color: #0061b7;
    }
    .footer {
      margin-top: 40px;
      border-top: 1px solid #e9ecef;
      padding: 20px;
      text-align: center;
      font-size: 12px;
      color: #6c757d;
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>Rinna API Documentation</h1>
    <p>Version 1.6.6 - Developer-friendly API for workflow management</p>
  </div>

  <div class="main-content">
    <div class="card">
      <h2>Swagger UI</h2>
      <p>Interactive documentation for exploring the API endpoints, parameters, and responses.</p>
      <a href="/api/docs/swagger-ui/">Open Swagger UI</a>
    </div>
    
    <div class="card">
      <h2>API Examples</h2>
      <p>Code examples showing how to use the API in different programming languages.</p>
      <a href="/api/docs/examples">View Examples</a>
    </div>
    
    <div class="card">
      <h2>Security Guide</h2>
      <p>Comprehensive guide to implementing secure API interactions.</p>
      <a href="/api/docs/security-guide">Read Security Guide</a>
    </div>
  </div>
  
  <div class="main-content" style="margin-top: 20px;">
    <div class="card">
      <h2>API Specification</h2>
      <p>Download the API specification to use with your tools.</p>
      <a href="/api/docs/swagger.json" style="margin-right: 10px;">Download JSON</a>
      <a href="/api/docs/swagger.yaml">Download YAML</a>
    </div>
    
    <div class="card">
      <h2>Getting Started</h2>
      <p>Quick guide to starting with the Rinna API.</p>
      <p>1. Get your API token<br>2. Make your first API call<br>3. Build your integration</p>
      <a href="https://github.com/heymumford/Rinna">View on GitHub</a>
    </div>
  </div>

  <div class="footer">
    <p>&copy; 2025 Eric C. Mumford - Licensed under MIT</p>
    <p>Rinna: A clean, compact solution for product, project, development, and quality management!</p>
  </div>
</body>
</html>`

		w.Header().Set("Content-Type", "text/html")
		fmt.Fprint(w, tmpl)
	}
}

func main() {
	// Parse command line flags
	port := flag.Int("port", 8080, "Server port")
	flag.Parse()

	// Set up the API directory
	apiDir := os.Getenv("RINNA_API_DIR")
	if apiDir == "" {
		// Use the executable directory as fallback
		exePath, err := os.Executable()
		if err != nil {
			log.Fatalf("Failed to get executable path: %v", err)
		}
		apiDir = filepath.Dir(filepath.Dir(exePath))
	}

	// Create router
	r := http.NewServeMux()

	// Serve Swagger UI static files
	r.Handle("/api/docs/swagger-ui/", http.StripPrefix("/api/docs/swagger-ui/",
		http.FileServer(http.Dir(filepath.Join(apiDir, "docs", "swagger-ui")))))

	// Serve Swagger JSON
	r.HandleFunc("/api/docs/swagger.json", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, filepath.Join(apiDir, "docs", "swagger.json"))
	})

	// Serve Swagger YAML
	r.HandleFunc("/api/docs/swagger.yaml", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/x-yaml")
		w.Header().Set("Content-Disposition", "attachment; filename=swagger.yaml")
		http.ServeFile(w, r, filepath.Join(apiDir, "swagger.yaml"))
	})

	// Serve the examples markdown as HTML
	r.HandleFunc("/api/docs/examples", func(w http.ResponseWriter, r *http.Request) {
		renderMarkdown(w, r, filepath.Join(apiDir, "docs", "examples.md"), "API Examples")
	})

	// Serve the security guide markdown as HTML
	r.HandleFunc("/api/docs/security-guide", func(w http.ResponseWriter, r *http.Request) {
		renderMarkdown(w, r, filepath.Join(apiDir, "docs", "security-guide.md"), "API Security Guide")
	})

	// Redirect /api/docs to index page
	r.HandleFunc("/api/docs", indexHandler(apiDir))

	// Redirect /api/docs/ to index page
	r.HandleFunc("/api/docs/", indexHandler(apiDir))

	// Root path: redirect to docs
	r.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/" {
			http.Redirect(w, r, "/api/docs", http.StatusFound)
		} else {
			http.NotFound(w, r)
		}
	})

	// Server address
	addr := fmt.Sprintf(":%d", *port)

	fmt.Printf("Starting documentation server on http://localhost%s\n", addr)
	fmt.Printf("Swagger UI: http://localhost%s/api/docs/swagger-ui/\n", addr)
	fmt.Printf("API Examples: http://localhost%s/api/docs/examples\n", addr)
	fmt.Printf("Security Guide: http://localhost%s/api/docs/security-guide\n", addr)
	fmt.Printf("Swagger JSON: http://localhost%s/api/docs/swagger.json\n", addr)
	fmt.Printf("Swagger YAML: http://localhost%s/api/docs/swagger.yaml\n", addr)

	// Start server
	if err := http.ListenAndServe(addr, r); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}