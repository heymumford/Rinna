// Package main provides a simple health check test utility
package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"time"
)

// run performs the health check and returns an exit code
func run() int {
	// Wait a bit for the server to start up
	time.Sleep(1 * time.Second)

	// Make a request to the health endpoint with context
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, "http://localhost:8080/health", nil)
	if err != nil {
		fmt.Printf("Error creating request: %v\n", err)
		return 1
	}

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		fmt.Printf("Error making request: %v\n", err)
		return 1
	}

	// Close response body with error handling
	defer func() {
		closeErr := resp.Body.Close()
		if closeErr != nil {
			fmt.Printf("Error closing response body: %v\n", closeErr)
		}
	}()

	// Read the response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Printf("Error reading response: %v\n", err)
		return 1
	}

	// Pretty print the response
	var prettyJSON bytes.Buffer
	if err := json.Indent(&prettyJSON, body, "", "  "); err != nil {
		fmt.Printf("Error formatting JSON: %v\n", err)
		fmt.Println(string(body))
	} else {
		fmt.Println(prettyJSON.String())
	}

	return 0
}

func main() {
	os.Exit(run())
}
