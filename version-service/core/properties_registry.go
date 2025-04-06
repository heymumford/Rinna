/*
 * Properties-based version registry implementation
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package core

import (
	"bufio"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

// PropertiesRegistry implements the VersionRegistry interface using a properties file
type PropertiesRegistry struct {
	PropertiesPath string
	ProjectRoot    string
	Handlers       []FileHandler
}

// NewPropertiesRegistry creates a new PropertiesRegistry with the given properties file path
func NewPropertiesRegistry(propertiesPath string, projectRoot string) (*PropertiesRegistry, error) {
	if propertiesPath == "" {
		return nil, errors.New("properties file path cannot be empty")
	}
	
	if projectRoot == "" {
		return nil, errors.New("project root path cannot be empty")
	}
	
	return &PropertiesRegistry{
		PropertiesPath: propertiesPath,
		ProjectRoot:    projectRoot,
		Handlers:       make([]FileHandler, 0),
	}, nil
}

// RegisterHandler adds a language-specific file handler
func (r *PropertiesRegistry) RegisterHandler(handler FileHandler) {
	r.Handlers = append(r.Handlers, handler)
}

// GetVersion returns the current version information from the properties file
func (r *PropertiesRegistry) GetVersion() (*VersionInfo, error) {
	// Check if the properties file exists
	if _, err := os.Stat(r.PropertiesPath); os.IsNotExist(err) {
		// Create a default properties file if it doesn't exist
		defaultVersion := NewVersionInfo()
		err = r.writePropertiesFile(defaultVersion)
		if err != nil {
			return nil, fmt.Errorf("failed to create default properties file: %w", err)
		}
		return defaultVersion, nil
	}
	
	// Read the properties file
	properties, err := r.readPropertiesFile()
	if err != nil {
		return nil, err
	}
	
	// Parse the version info from the properties
	version, err := r.parseVersionInfo(properties)
	if err != nil {
		return nil, err
	}
	
	return version, nil
}

// SetVersion sets a specific version
func (r *PropertiesRegistry) SetVersion(version *VersionInfo) error {
	// Update the properties file
	err := r.writePropertiesFile(version)
	if err != nil {
		return err
	}
	
	// Update all language-specific files
	err = r.updateAllHandlers(version)
	if err != nil {
		return err
	}
	
	return nil
}

// BumpMajor increments the major version
func (r *PropertiesRegistry) BumpMajor() (*VersionInfo, error) {
	version, err := r.GetVersion()
	if err != nil {
		return nil, err
	}
	
	version.BumpMajor()
	version.BuildNumber = 1
	version.LastUpdated = time.Now().UTC().Format("2006-01-02")
	version.BuildTime = time.Now().UTC().Format("2006-01-02T15:04:05Z")
	
	err = r.SetVersion(version)
	if err != nil {
		return nil, err
	}
	
	return version, nil
}

// BumpMinor increments the minor version
func (r *PropertiesRegistry) BumpMinor() (*VersionInfo, error) {
	version, err := r.GetVersion()
	if err != nil {
		return nil, err
	}
	
	version.BumpMinor()
	version.BuildNumber = 1
	version.LastUpdated = time.Now().UTC().Format("2006-01-02")
	version.BuildTime = time.Now().UTC().Format("2006-01-02T15:04:05Z")
	
	err = r.SetVersion(version)
	if err != nil {
		return nil, err
	}
	
	return version, nil
}

// BumpPatch increments the patch version
func (r *PropertiesRegistry) BumpPatch() (*VersionInfo, error) {
	version, err := r.GetVersion()
	if err != nil {
		return nil, err
	}
	
	version.BumpPatch()
	version.BuildNumber = 1
	version.LastUpdated = time.Now().UTC().Format("2006-01-02")
	version.BuildTime = time.Now().UTC().Format("2006-01-02T15:04:05Z")
	
	err = r.SetVersion(version)
	if err != nil {
		return nil, err
	}
	
	return version, nil
}

// VerifyConsistency checks if all version files are consistent
func (r *PropertiesRegistry) VerifyConsistency() (bool, []string, error) {
	version, err := r.GetVersion()
	if err != nil {
		return false, nil, err
	}
	
	inconsistencies := make([]string, 0)
	isConsistent := true
	
	// Check each handler for consistency
	for _, handler := range r.Handlers {
		ok, message, err := handler.VerifyVersion(version)
		if err != nil {
			return false, nil, err
		}
		if !ok {
			isConsistent = false
			inconsistencies = append(inconsistencies, message)
		}
	}
	
	return isConsistent, inconsistencies, nil
}

// UpdateAllFiles updates all files to match the current version
func (r *PropertiesRegistry) UpdateAllFiles() error {
	version, err := r.GetVersion()
	if err != nil {
		return err
	}
	
	return r.updateAllHandlers(version)
}

// CreateRelease creates a release for the current version
func (r *PropertiesRegistry) CreateRelease(message string, forceGithub bool) error {
	version, err := r.GetVersion()
	if err != nil {
		return err
	}
	
	// Set release type to RELEASE
	version.ReleaseType = "RELEASE"
	err = r.writePropertiesFile(version)
	if err != nil {
		return err
	}
	
	// Create a git tag
	err = r.CreateGitTag(message)
	if err != nil {
		return err
	}
	
	// Create GitHub release if appropriate
	if forceGithub || version.ShouldCreateGitHubRelease() {
		err = r.createGitHubRelease(version, message)
		if err != nil {
			return err
		}
	}
	
	return nil
}

// CreateGitTag creates a git tag for the current version
func (r *PropertiesRegistry) CreateGitTag(message string) error {
	version, err := r.GetVersion()
	if err != nil {
		return err
	}
	
	tagName := fmt.Sprintf("v%s", version.FullVersion)
	
	if message == "" {
		message = fmt.Sprintf("Release version %s", version.FullVersion)
	}
	
	// Create the git tag
	cmd := exec.Command("git", "tag", "-a", tagName, "-m", message)
	cmd.Dir = r.ProjectRoot
	err = cmd.Run()
	if err != nil {
		return fmt.Errorf("failed to create git tag: %w", err)
	}
	
	return nil
}

// Helper function to create a GitHub release using the gh CLI
func (r *PropertiesRegistry) createGitHubRelease(version *VersionInfo, message string) error {
	if message == "" {
		message = fmt.Sprintf("Release version %s", version.FullVersion)
	}
	
	tagName := fmt.Sprintf("v%s", version.FullVersion)
	
	// Check if gh CLI is available
	_, err := exec.LookPath("gh")
	if err != nil {
		return fmt.Errorf("GitHub CLI (gh) not found, cannot create release")
	}
	
	// Create the GitHub release
	cmd := exec.Command("gh", "release", "create", tagName, "-t", fmt.Sprintf("Rinna %s", version.FullVersion), "-n", message)
	cmd.Dir = r.ProjectRoot
	err = cmd.Run()
	if err != nil {
		return fmt.Errorf("failed to create GitHub release: %w", err)
	}
	
	return nil
}

// Helper function to read the properties file
func (r *PropertiesRegistry) readPropertiesFile() (map[string]string, error) {
	file, err := os.Open(r.PropertiesPath)
	if err != nil {
		return nil, fmt.Errorf("failed to open properties file: %w", err)
	}
	defer file.Close()
	
	properties := make(map[string]string)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		line = strings.TrimSpace(line)
		
		// Skip comments and empty lines
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		
		// Parse key-value pairs
		parts := strings.SplitN(line, "=", 2)
		if len(parts) == 2 {
			key := strings.TrimSpace(parts[0])
			value := strings.TrimSpace(parts[1])
			properties[key] = value
		}
	}
	
	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("failed to read properties file: %w", err)
	}
	
	return properties, nil
}

// Helper function to write the properties file
func (r *PropertiesRegistry) writePropertiesFile(version *VersionInfo) error {
	// Ensure directory exists
	dir := filepath.Dir(r.PropertiesPath)
	err := os.MkdirAll(dir, 0755)
	if err != nil {
		return fmt.Errorf("failed to create directory for properties file: %w", err)
	}
	
	// Create the properties content
	content := "# Rinna Project Version Information\n"
	content += "# This file serves as the single source of truth for version information across the project\n\n"
	
	content += "# Core version information\n"
	content += fmt.Sprintf("version=%s\n", version.FullVersion)
	content += fmt.Sprintf("version.major=%d\n", version.Major)
	content += fmt.Sprintf("version.minor=%d\n", version.Minor)
	content += fmt.Sprintf("version.patch=%d\n", version.Patch)
	content += fmt.Sprintf("version.qualifier=%s\n", version.Qualifier)
	content += fmt.Sprintf("version.full=%s\n\n", version.FullVersion)
	
	content += "# Release information\n"
	content += fmt.Sprintf("lastUpdated=%s\n", version.LastUpdated)
	content += fmt.Sprintf("releaseType=%s\n", version.ReleaseType)
	content += fmt.Sprintf("buildNumber=%d\n\n", version.BuildNumber)
	
	content += "# Build information\n"
	content += fmt.Sprintf("build.timestamp=%s\n", version.BuildTime)
	content += fmt.Sprintf("build.git.commit=%s\n", version.GitCommit)
	
	// Write the file
	err = os.WriteFile(r.PropertiesPath, []byte(content), 0644)
	if err != nil {
		return fmt.Errorf("failed to write properties file: %w", err)
	}
	
	return nil
}

// Helper function to parse version info from properties
func (r *PropertiesRegistry) parseVersionInfo(properties map[string]string) (*VersionInfo, error) {
	version := NewVersionInfo()
	
	// Parse core version components
	if fullVersion, ok := properties["version"]; ok {
		version.FullVersion = fullVersion
		
		// Parse from the full version
		v, err := FromString(fullVersion)
		if err != nil {
			return nil, err
		}
		
		version.Major = v.Major
		version.Minor = v.Minor
		version.Patch = v.Patch
		version.Qualifier = v.Qualifier
	} else {
		return nil, errors.New("version property not found")
	}
	
	// Parse other fields
	if value, ok := properties["lastUpdated"]; ok {
		version.LastUpdated = value
	}
	
	if value, ok := properties["releaseType"]; ok {
		version.ReleaseType = value
	}
	
	if value, ok := properties["buildNumber"]; ok {
		fmt.Sscanf(value, "%d", &version.BuildNumber)
	}
	
	if value, ok := properties["build.timestamp"]; ok {
		version.BuildTime = value
	}
	
	if value, ok := properties["build.git.commit"]; ok {
		version.GitCommit = value
	}
	
	return version, nil
}

// Helper function to update all handlers
func (r *PropertiesRegistry) updateAllHandlers(version *VersionInfo) error {
	for _, handler := range r.Handlers {
		err := handler.WriteVersion(version)
		if err != nil {
			return err
		}
	}
	
	return nil
}