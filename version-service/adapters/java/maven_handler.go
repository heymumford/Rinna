/*
 * Maven POM file handler
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package java_adapter

import (
	"errors"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"

	"github.com/heymumford/Rinna/version-service/core"
)

// MavenFileHandler handles reading and writing version information to Maven POM files
type MavenFileHandler struct {
	ProjectRoot string
}

// NewMavenFileHandler creates a new Maven file handler
func NewMavenFileHandler(projectRoot string) *MavenFileHandler {
	return &MavenFileHandler{
		ProjectRoot: projectRoot,
	}
}

// ReadVersion reads the version from the root POM file
func (h *MavenFileHandler) ReadVersion() (*core.VersionInfo, error) {
	pomPath := filepath.Join(h.ProjectRoot, "pom.xml")
	
	// Check if the POM file exists
	if _, err := os.Stat(pomPath); os.IsNotExist(err) {
		return nil, fmt.Errorf("root POM file not found: %s", pomPath)
	}
	
	// Read the POM file
	content, err := os.ReadFile(pomPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read POM file: %w", err)
	}
	
	// Extract version using regex for simplicity
	re := regexp.MustCompile(`<version>([^<]+)</version>`)
	matches := re.FindSubmatch(content)
	if len(matches) < 2 {
		return nil, errors.New("version tag not found in POM file")
	}
	
	versionStr := string(matches[1])
	version, err := core.FromString(versionStr)
	if err != nil {
		return nil, err
	}
	
	return version, nil
}

// WriteVersion updates all POM files with the new version
func (h *MavenFileHandler) WriteVersion(version *core.VersionInfo) error {
	pomFiles, err := h.findAllPomFiles()
	if err != nil {
		return err
	}
	
	if len(pomFiles) == 0 {
		return errors.New("no POM files found")
	}
	
	for _, pomFile := range pomFiles {
		err := h.updatePomFile(pomFile, version)
		if err != nil {
			return err
		}
	}
	
	return nil
}

// VerifyVersion checks if all POM files have the correct version
func (h *MavenFileHandler) VerifyVersion(expectedVersion *core.VersionInfo) (bool, string, error) {
	pomFiles, err := h.findAllPomFiles()
	if err != nil {
		return false, "", err
	}
	
	for _, pomFile := range pomFiles {
		content, err := os.ReadFile(pomFile)
		if err != nil {
			continue
		}
		
		// Check for org.rinna groupId to only verify project-specific POMs
		if !h.isRinnaProject(content) {
			continue
		}
		
		// Extract project version
		reProjectVersion := regexp.MustCompile(`<groupId>org\.rinna</groupId>[\s\S]*?<version>([^<]+)</version>`)
		matches := reProjectVersion.FindSubmatch(content)
		if len(matches) >= 2 {
			versionStr := string(matches[1])
			if versionStr != expectedVersion.FullVersion {
				return false, fmt.Sprintf("POM file version mismatch in %s: %s (should be %s)", pomFile, versionStr, expectedVersion.FullVersion), nil
			}
		}
		
		// Extract parent version
		reParentVersion := regexp.MustCompile(`<parent>[\s\S]*?<groupId>org\.rinna</groupId>[\s\S]*?<version>([^<]+)</version>[\s\S]*?</parent>`)
		parentMatches := reParentVersion.FindSubmatch(content)
		if len(parentMatches) >= 2 {
			parentVersionStr := string(parentMatches[1])
			if parentVersionStr != expectedVersion.FullVersion {
				return false, fmt.Sprintf("POM parent version mismatch in %s: %s (should be %s)", pomFile, parentVersionStr, expectedVersion.FullVersion), nil
			}
		}
	}
	
	return true, "", nil
}

// Helper function to find all POM files in the project
func (h *MavenFileHandler) findAllPomFiles() ([]string, error) {
	pomFiles := make([]string, 0)
	
	err := filepath.WalkDir(h.ProjectRoot, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		
		if !d.IsDir() && d.Name() == "pom.xml" {
			pomFiles = append(pomFiles, path)
		}
		
		return nil
	})
	
	if err != nil {
		return nil, err
	}
	
	return pomFiles, nil
}

// Helper function to check if a POM file is for a Rinna project
func (h *MavenFileHandler) isRinnaProject(content []byte) bool {
	return regexp.MustCompile(`<groupId>org\.rinna</groupId>`).Match(content)
}

// Helper function to update a POM file with the new version
func (h *MavenFileHandler) updatePomFile(pomFile string, version *core.VersionInfo) error {
	content, err := os.ReadFile(pomFile)
	if err != nil {
		return err
	}
	
	// Only update Rinna project POM files
	if !h.isRinnaProject(content) {
		return nil
	}
	
	contentStr := string(content)
	
	// Update project version
	reProjectVersion := regexp.MustCompile(`(<groupId>org\.rinna</groupId>[\s\S]*?<version>)[^<]+(</version>)`)
	contentStr = reProjectVersion.ReplaceAllString(contentStr, fmt.Sprintf("${1}%s${2}", version.FullVersion))
	
	// Update parent version
	reParentVersion := regexp.MustCompile(`(<parent>[\s\S]*?<groupId>org\.rinna</groupId>[\s\S]*?<version>)[^<]+(</version>[\s\S]*?</parent>)`)
	contentStr = reParentVersion.ReplaceAllString(contentStr, fmt.Sprintf("${1}%s${2}", version.FullVersion))
	
	// Write the updated content back to the file
	err = os.WriteFile(pomFile, []byte(contentStr), 0644)
	if err != nil {
		return err
	}
	
	return nil
}