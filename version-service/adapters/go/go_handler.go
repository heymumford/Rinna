/*
 * Go version file handler
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package go_adapter

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"time"

	"github.com/heymumford/Rinna/version-service/core"
)

// GoFileHandler handles reading and writing version information to Go files
type GoFileHandler struct {
	FilePaths []string
}

// NewGoFileHandler creates a new Go file handler
func NewGoFileHandler(filePaths []string) *GoFileHandler {
	return &GoFileHandler{
		FilePaths: filePaths,
	}
}

// ReadVersion reads the version from Go files
func (h *GoFileHandler) ReadVersion() (*core.VersionInfo, error) {
	if len(h.FilePaths) == 0 {
		return nil, errors.New("no Go file paths provided")
	}
	
	// Try each file until one is successfully read
	for _, filePath := range h.FilePaths {
		if _, err := os.Stat(filePath); os.IsNotExist(err) {
			continue
		}
		
		content, err := os.ReadFile(filePath)
		if err != nil {
			continue
		}
		
		// Extract version from the content
		re := regexp.MustCompile(`Version\s*=\s*"([^"]*)"`)
		matches := re.FindSubmatch(content)
		if len(matches) >= 2 {
			versionStr := string(matches[1])
			version, err := core.FromString(versionStr)
			if err != nil {
				return nil, err
			}
			
			// Extract build time
			reBuildTime := regexp.MustCompile(`BuildTime\s*=\s*"([^"]*)"`)
			buildTimeMatches := reBuildTime.FindSubmatch(content)
			if len(buildTimeMatches) >= 2 {
				version.BuildTime = string(buildTimeMatches[1])
			}
			
			// Extract git commit
			reCommit := regexp.MustCompile(`CommitSHA\s*=\s*"([^"]*)"`)
			commitMatches := reCommit.FindSubmatch(content)
			if len(commitMatches) >= 2 {
				version.GitCommit = string(commitMatches[1])
			}
			
			return version, nil
		}
	}
	
	return nil, errors.New("no valid Go version file found")
}

// WriteVersion writes the version to Go files
func (h *GoFileHandler) WriteVersion(version *core.VersionInfo) error {
	if len(h.FilePaths) == 0 {
		return errors.New("no Go file paths provided")
	}
	
	currentTime := time.Now().UTC().Format(time.RFC3339)
	success := false
	
	for _, filePath := range h.FilePaths {
		// Create the directory if it doesn't exist
		dir := filepath.Dir(filePath)
		err := os.MkdirAll(dir, 0755)
		if err != nil {
			continue
		}
		
		// If the file doesn't exist, create it
		if _, err := os.Stat(filePath); os.IsNotExist(err) {
			packageName := filepath.Base(filepath.Dir(filePath))
			content := generateGoVersionFile(packageName, version, currentTime)
			err = os.WriteFile(filePath, []byte(content), 0644)
			if err != nil {
				continue
			}
			success = true
		} else {
			// Read the existing file
			content, err := os.ReadFile(filePath)
			if err != nil {
				continue
			}
			
			// Update the version
			contentStr := string(content)
			reVersion := regexp.MustCompile(`(Version\s*=\s*)"[^"]*"`)
			contentStr = reVersion.ReplaceAllString(contentStr, fmt.Sprintf("${1}\"%s\"", version.FullVersion))
			
			// Update the build time
			reBuildTime := regexp.MustCompile(`(BuildTime\s*=\s*)"[^"]*"`)
			contentStr = reBuildTime.ReplaceAllString(contentStr, fmt.Sprintf("${1}\"%s\"", currentTime))
			
			// Write the updated content
			err = os.WriteFile(filePath, []byte(contentStr), 0644)
			if err != nil {
				continue
			}
			success = true
		}
	}
	
	if !success {
		return errors.New("failed to write to any Go version file")
	}
	
	return nil
}

// VerifyVersion checks if the Go files have the correct version
func (h *GoFileHandler) VerifyVersion(expectedVersion *core.VersionInfo) (bool, string, error) {
	version, err := h.ReadVersion()
	if err != nil {
		return false, fmt.Sprintf("Failed to read Go version: %s", err), nil
	}
	
	if version.FullVersion != expectedVersion.FullVersion {
		return false, fmt.Sprintf("Go version mismatch: %s (should be %s)", version.FullVersion, expectedVersion.FullVersion), nil
	}
	
	return true, "", nil
}

// Helper function to generate a new Go version file
func generateGoVersionFile(packageName string, version *core.VersionInfo, buildTime string) string {
	return fmt.Sprintf(`/*
 * Version information for the Rinna API
 *
 * Copyright (c) %d Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package %s

// These values are set during the build process
var (
	Version   = "%s"
	CommitSHA = "runtime"
	BuildTime = "%s"
)
`, time.Now().Year(), packageName, version.FullVersion, buildTime)
}