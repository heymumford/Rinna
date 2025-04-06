/*
 * README file handler
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package bash_adapter

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"

	"github.com/heymumford/Rinna/version-service/core"
)

// ReadmeFileHandler handles reading and writing version information to README files
type ReadmeFileHandler struct {
	ReadmePath string
}

// NewReadmeFileHandler creates a new README file handler
func NewReadmeFileHandler(projectRoot string) *ReadmeFileHandler {
	return &ReadmeFileHandler{
		ReadmePath: filepath.Join(projectRoot, "README.md"),
	}
}

// ReadVersion reads the version from the README file
func (h *ReadmeFileHandler) ReadVersion() (*core.VersionInfo, error) {
	// Check if the README file exists
	if _, err := os.Stat(h.ReadmePath); os.IsNotExist(err) {
		return nil, fmt.Errorf("README file not found: %s", h.ReadmePath)
	}
	
	// Read the README file
	content, err := os.ReadFile(h.ReadmePath)
	if err != nil {
		return nil, fmt.Errorf("failed to read README file: %w", err)
	}
	
	// Extract version using regex
	re := regexp.MustCompile(`\[!\[Version\]\(https://img\.shields\.io/badge/version-([^-]+)-blue\.svg\)\]`)
	matches := re.FindSubmatch(content)
	if len(matches) < 2 {
		// Try alternate patterns
		re = regexp.MustCompile(`version-([0-9]+\.[0-9]+\.[0-9]+)-blue`)
		matches = re.FindSubmatch(content)
	}
	
	if len(matches) < 2 {
		return nil, fmt.Errorf("version not found in README file")
	}
	
	versionStr := string(matches[1])
	version, err := core.FromString(versionStr)
	if err != nil {
		return nil, err
	}
	
	return version, nil
}

// WriteVersion updates the README file with the new version
func (h *ReadmeFileHandler) WriteVersion(version *core.VersionInfo) error {
	// Check if the README file exists
	if _, err := os.Stat(h.ReadmePath); os.IsNotExist(err) {
		return fmt.Errorf("README file not found: %s", h.ReadmePath)
	}
	
	// Read the README file
	content, err := os.ReadFile(h.ReadmePath)
	if err != nil {
		return fmt.Errorf("failed to read README file: %w", err)
	}
	
	contentStr := string(content)
	
	// Update badge version
	reBadge := regexp.MustCompile(`(\[!\[Version\]\(https://img\.shields\.io/badge/version-)([^-]+)(-blue\.svg\)\])`)
	contentStr = reBadge.ReplaceAllString(contentStr, fmt.Sprintf("${1}%s${3}", version.FullVersion))
	
	// Update alternate version patterns
	reAlt := regexp.MustCompile(`(version-)([0-9]+\.[0-9]+\.[0-9]+)(-blue)`)
	contentStr = reAlt.ReplaceAllString(contentStr, fmt.Sprintf("${1}%s${3}", version.FullVersion))
	
	// Update Maven example if it exists
	reMaven := regexp.MustCompile(`(<version>)([0-9]+\.[0-9]+\.[0-9]+)(</version>)`)
	contentStr = reMaven.ReplaceAllString(contentStr, fmt.Sprintf("${1}%s${3}", version.FullVersion))
	
	// Write the updated content back to the file
	err = os.WriteFile(h.ReadmePath, []byte(contentStr), 0644)
	if err != nil {
		return fmt.Errorf("failed to write README file: %w", err)
	}
	
	return nil
}

// VerifyVersion checks if the README file has the correct version
func (h *ReadmeFileHandler) VerifyVersion(expectedVersion *core.VersionInfo) (bool, string, error) {
	version, err := h.ReadVersion()
	if err != nil {
		return false, fmt.Sprintf("Failed to read README version: %s", err), nil
	}
	
	if version.FullVersion != expectedVersion.FullVersion {
		return false, fmt.Sprintf("README version mismatch: %s (should be %s)", version.FullVersion, expectedVersion.FullVersion), nil
	}
	
	return true, "", nil
}