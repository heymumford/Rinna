/*
 * Demo application for secure token management
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/heymumford/rinna/api/internal/client"
	"github.com/heymumford/rinna/api/pkg/config"
)

const helpText = `
Rinna Secure Token Management Demo

Commands:
  generate <project-id> [--scope <scope>] [--days <expiration-days>]
  validate <token>
  revoke <token>
  list
  clean
  help

Options:
  --scope    Token scope (default: "api")
  --days     Token expiration days (default: 90)
`

func main() {
	if len(os.Args) < 2 {
		fmt.Println(helpText)
		os.Exit(1)
	}

	command := os.Args[1]

	switch command {
	case "generate":
		generateCmd()
	case "validate":
		validateCmd()
	case "revoke":
		revokeCmd()
	case "list":
		listCmd()
	case "clean":
		cleanCmd()
	case "help":
		fmt.Println(helpText)
	default:
		fmt.Printf("Unknown command: %s\n", command)
		fmt.Println(helpText)
		os.Exit(1)
	}
}

func generateCmd() {
	generateFlags := flag.NewFlagSet("generate", flag.ExitOnError)
	scope := generateFlags.String("scope", "api", "Token scope")
	days := generateFlags.Int("days", 90, "Token expiration days")

	if err := generateFlags.Parse(os.Args[2:]); err != nil {
		fmt.Printf("Error parsing flags: %v\n", err)
		os.Exit(1)
	}

	args := generateFlags.Args()
	if len(args) < 1 {
		fmt.Println("Project ID is required")
		os.Exit(1)
	}

	projectID := args[0]

	// Initialize API client
	apiClient, err := initAPIClient()
	if err != nil {
		fmt.Printf("Failed to initialize API client: %v\n", err)
		os.Exit(1)
	}

	// Generate token
	ctx := context.Background()
	token, err := apiClient.GenerateToken(ctx, projectID, *scope, *days)
	if err != nil {
		fmt.Printf("Failed to generate token: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("Generated token: %s\n", token)
	fmt.Printf("Project ID: %s\n", projectID)
	fmt.Printf("Scope: %s\n", *scope)
	fmt.Printf("Expires in: %d days\n", *days)
}

func validateCmd() {
	validateFlags := flag.NewFlagSet("validate", flag.ExitOnError)
	if err := validateFlags.Parse(os.Args[2:]); err != nil {
		fmt.Printf("Error parsing flags: %v\n", err)
		os.Exit(1)
	}

	args := validateFlags.Args()
	if len(args) < 1 {
		fmt.Println("Token is required")
		os.Exit(1)
	}

	token := args[0]

	// Initialize API client
	apiClient, err := initAPIClient()
	if err != nil {
		fmt.Printf("Failed to initialize API client: %v\n", err)
		os.Exit(1)
	}

	// Get token manager
	tokenManager, err := apiClient.javaClient.GetTokenManager()
	if err != nil {
		fmt.Printf("Failed to get token manager: %v\n", err)
		os.Exit(1)
	}

	// Validate token
	ctx := context.Background()
	isValid, err := tokenManager.ValidateToken(ctx, token)
	if err != nil {
		fmt.Printf("Failed to validate token: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("Token is valid: %v\n", isValid)

	// Get token info
	tokenInfo, err := tokenManager.GetTokenInfo(token)
	if err != nil {
		fmt.Printf("Failed to get token info: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("Project ID: %s\n", tokenInfo.ProjectID)
	fmt.Printf("Token type: %s\n", tokenInfo.TokenType)
	fmt.Printf("Scope: %s\n", tokenInfo.Scope)
	fmt.Printf("Issued at: %s\n", tokenInfo.IssuedAt.Format(time.RFC3339))
	fmt.Printf("Expires at: %s\n", tokenInfo.ExpiresAt.Format(time.RFC3339))
	fmt.Printf("Expiration: %s\n", time.Until(tokenInfo.ExpiresAt).Round(time.Hour))
}

func revokeCmd() {
	revokeFlags := flag.NewFlagSet("revoke", flag.ExitOnError)
	if err := revokeFlags.Parse(os.Args[2:]); err != nil {
		fmt.Printf("Error parsing flags: %v\n", err)
		os.Exit(1)
	}

	args := revokeFlags.Args()
	if len(args) < 1 {
		fmt.Println("Token is required")
		os.Exit(1)
	}

	token := args[0]

	// Initialize API client
	apiClient, err := initAPIClient()
	if err != nil {
		fmt.Printf("Failed to initialize API client: %v\n", err)
		os.Exit(1)
	}

	// Revoke token
	ctx := context.Background()
	if err := apiClient.RevokeToken(ctx, token); err != nil {
		fmt.Printf("Failed to revoke token: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("Token revoked: %s\n", token)
}

func listCmd() {
	// Initialize API client
	apiClient, err := initAPIClient()
	if err != nil {
		fmt.Printf("Failed to initialize API client: %v\n", err)
		os.Exit(1)
	}

	// List tokens
	ctx := context.Background()
	tokens, err := apiClient.ListTokens(ctx)
	if err != nil {
		fmt.Printf("Failed to list tokens: %v\n", err)
		os.Exit(1)
	}

	if len(tokens) == 0 {
		fmt.Println("No tokens found")
		return
	}

	fmt.Println("Tokens:")
	fmt.Println("--------------------------------------------------------")
	for _, tokenInfo := range tokens {
		fmt.Printf("Token: %s\n", tokenInfo.Token)
		fmt.Printf("  Project ID: %s\n", tokenInfo.ProjectID)
		fmt.Printf("  Type: %s\n", tokenInfo.TokenType)
		fmt.Printf("  Scope: %s\n", tokenInfo.Scope)
		fmt.Printf("  Valid: %v\n", tokenInfo.Valid)
		fmt.Printf("  Expires: %s (%s)\n", 
			tokenInfo.ExpiresAt.Format(time.RFC3339), 
			time.Until(tokenInfo.ExpiresAt).Round(time.Hour))
		fmt.Println("--------------------------------------------------------")
	}
}

func cleanCmd() {
	// Initialize API client
	apiClient, err := initAPIClient()
	if err != nil {
		fmt.Printf("Failed to initialize API client: %v\n", err)
		os.Exit(1)
	}

	// Clean expired tokens
	ctx := context.Background()
	if err := apiClient.CleanupTokens(ctx); err != nil {
		fmt.Printf("Failed to clean expired tokens: %v\n", err)
		os.Exit(1)
	}

	fmt.Println("Expired tokens cleaned")
}

func initAPIClient() (*client.APIClient, error) {
	// Load configuration
	cfg, err := config.LoadConfig()
	if err != nil {
		return nil, fmt.Errorf("failed to load configuration: %w", err)
	}

	// Create API client
	apiClient, err := client.NewAPIClient(cfg)
	if err != nil {
		return nil, fmt.Errorf("failed to create API client: %w", err)
	}

	return apiClient, nil
}