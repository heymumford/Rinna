/*
 * Core version entity and rules
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package core

import (
	"fmt"
	"strconv"
	"strings"
	"time"
)

// VersionInfo represents the complete version information for the project
type VersionInfo struct {
	// Core version components
	Major       int    `json:"major"`
	Minor       int    `json:"minor"`
	Patch       int    `json:"patch"`
	Qualifier   string `json:"qualifier,omitempty"`
	FullVersion string `json:"fullVersion"`

	// Release information
	LastUpdated  string `json:"lastUpdated"`
	ReleaseType  string `json:"releaseType"`
	BuildNumber  int    `json:"buildNumber"`
	BuildTime    string `json:"buildTime"`
	GitCommit    string `json:"gitCommit,omitempty"`
	IsConsistent bool   `json:"isConsistent"`
}

// Version information set during build process
var (
	Version   = "1.9.1"
	CommitSHA = "runtime"
	BuildTime = "2025-04-09T20:14:56Z"
)

// NewVersionInfo creates a new VersionInfo with default values
func NewVersionInfo() *VersionInfo {
	now := time.Now().UTC()
	return &VersionInfo{
		Major:        1,
		Minor:        9,
		Patch:        1,
		FullVersion:  Version,
		LastUpdated:  "2025-04-11",
		ReleaseType:  "RELEASE",
		BuildNumber:  508,
		BuildTime:    BuildTime,
		GitCommit:    CommitSHA,
		IsConsistent: true,
	}
}

// FromString parses a version string into a VersionInfo
func FromString(version string) (*VersionInfo, error) {
	vi := NewVersionInfo()
	
	// Parse version components
	parts := strings.Split(version, ".")
	if len(parts) < 3 {
		return nil, fmt.Errorf("invalid version format: %s (expected X.Y.Z)", version)
	}
	
	var err error
	vi.Major, err = strconv.Atoi(parts[0])
	if err != nil {
		return nil, fmt.Errorf("invalid major version: %s", parts[0])
	}
	
	vi.Minor, err = strconv.Atoi(parts[1])
	if err != nil {
		return nil, fmt.Errorf("invalid minor version: %s", parts[1])
	}
	
	// Handle patch version with potential qualifier
	patchParts := strings.Split(parts[2], "-")
	vi.Patch, err = strconv.Atoi(patchParts[0])
	if err != nil {
		return nil, fmt.Errorf("invalid patch version: %s", patchParts[0])
	}
	
	if len(patchParts) > 1 {
		vi.Qualifier = strings.Join(patchParts[1:], "-")
		vi.FullVersion = fmt.Sprintf("%d.%d.%d-%s", vi.Major, vi.Minor, vi.Patch, vi.Qualifier)
	} else {
		vi.FullVersion = fmt.Sprintf("%d.%d.%d", vi.Major, vi.Minor, vi.Patch)
	}
	
	return vi, nil
}

// ToString returns the version as a string
func (v *VersionInfo) ToString() string {
	return v.FullVersion
}

// BumpMajor increments the major version and resets minor and patch to 0
func (v *VersionInfo) BumpMajor() {
	v.Major++
	v.Minor = 0
	v.Patch = 0
	v.updateFullVersion()
}

// BumpMinor increments the minor version and resets patch to 0
func (v *VersionInfo) BumpMinor() {
	v.Minor++
	v.Patch = 0
	v.updateFullVersion()
}

// BumpPatch increments the patch version
func (v *VersionInfo) BumpPatch() {
	v.Patch++
	v.updateFullVersion()
}

// updateFullVersion updates the FullVersion field based on components
func (v *VersionInfo) updateFullVersion() {
	if v.Qualifier != "" {
		v.FullVersion = fmt.Sprintf("%d.%d.%d-%s", v.Major, v.Minor, v.Patch, v.Qualifier)
	} else {
		v.FullVersion = fmt.Sprintf("%d.%d.%d", v.Major, v.Minor, v.Patch)
	}
}

// SetReleaseType sets the release type and updates the timestamp
func (v *VersionInfo) SetReleaseType(releaseType string) {
	v.ReleaseType = releaseType
	v.LastUpdated = time.Now().UTC().Format("2006-01-02")
	v.BuildTime = time.Now().UTC().Format("2006-01-02T15:04:05Z")
}

// IsRelease returns true if this is a release version
func (v *VersionInfo) IsRelease() bool {
	return v.ReleaseType == "RELEASE"
}

// IsSnapshot returns true if this is a snapshot version
func (v *VersionInfo) IsSnapshot() bool {
	return v.ReleaseType == "SNAPSHOT"
}

// ShouldCreateGitHubRelease determines if a version should have a GitHub release
func (v *VersionInfo) ShouldCreateGitHubRelease() bool {
	// Major or minor versions always get releases
	if v.Patch == 0 {
		return true
	}
	
	// If it's explicitly marked as a release
	if v.IsRelease() {
		return true
	}
	
	// Default to false for regular patch versions
	return false
}