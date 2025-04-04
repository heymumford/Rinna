/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.entity.DefaultRelease;
import org.rinna.domain.entity.Release;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.repository.ReleaseRepository;
import org.rinna.domain.usecase.ItemService;
import org.rinna.domain.usecase.ReleaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of the ReleaseService interface.
 */
public class DefaultReleaseService implements ReleaseService {
    private static final int MAX_PATCH_VERSION = 999;
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
    
    private final ReleaseRepository releaseRepository;
    private final ItemService itemService;
    
    /**
     * Constructs a new DefaultReleaseService with the given repositories.
     *
     * @param releaseRepository the release repository
     * @param itemService the item service
     */
    public DefaultReleaseService(ReleaseRepository releaseRepository, ItemService itemService) {
        this.releaseRepository = Objects.requireNonNull(releaseRepository, "Release repository cannot be null");
        this.itemService = Objects.requireNonNull(itemService, "Item service cannot be null");
    }
    
    @Override
    public Release createRelease(String version, String description) {
        validateVersionFormat(version);
        
        // Ensure no duplicate versions
        Optional<Release> existingRelease = releaseRepository.findByVersion(version);
        if (existingRelease.isPresent()) {
            throw new IllegalArgumentException("Release with version " + version + " already exists");
        }
        
        DefaultRelease.Builder builder = new DefaultRelease.Builder()
                .version(version)
                .description(description);
        
        Release release = builder.build();
        return releaseRepository.save(release);
    }
    
    @Override
    public Release createNextMinorVersion(UUID releaseId, String description) {
        Release baseRelease = findReleaseById(releaseId);
        String baseVersion = baseRelease.getVersion();
        
        Matcher matcher = VERSION_PATTERN.matcher(baseVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + baseVersion);
        }
        
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        
        // Increment minor version, reset patch to 0
        String newVersion = major + "." + (minor + 1) + ".0";
        
        return createRelease(newVersion, description);
    }
    
    @Override
    public Release createNextPatchVersion(UUID releaseId, String description) {
        Release baseRelease = findReleaseById(releaseId);
        String baseVersion = baseRelease.getVersion();
        
        Matcher matcher = VERSION_PATTERN.matcher(baseVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + baseVersion);
        }
        
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        
        // Check if patch version would exceed maximum
        if (patch >= MAX_PATCH_VERSION) {
            throw new IllegalStateException("Patch version would exceed maximum of " + MAX_PATCH_VERSION);
        }
        
        // Increment patch version
        String newVersion = major + "." + minor + "." + (patch + 1);
        
        return createRelease(newVersion, description);
    }
    
    @Override
    public Release createNextMajorVersion(UUID releaseId, String description) {
        Release baseRelease = findReleaseById(releaseId);
        String baseVersion = baseRelease.getVersion();
        
        Matcher matcher = VERSION_PATTERN.matcher(baseVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + baseVersion);
        }
        
        int major = Integer.parseInt(matcher.group(1));
        
        // Increment major version, reset minor and patch to 0
        String newVersion = (major + 1) + ".0.0";
        
        return createRelease(newVersion, description);
    }
    
    @Override
    public void addWorkItem(UUID releaseId, UUID workItemId) {
        // Verify work item exists
        if (!itemService.findById(workItemId).isPresent()) {
            throw new IllegalArgumentException("Work item with ID " + workItemId + " does not exist");
        }
        
        Release release = findReleaseById(releaseId);
        
        // Create a new release with the work item added
        List<UUID> workItems = new ArrayList<>(release.getWorkItems());
        if (!workItems.contains(workItemId)) {
            workItems.add(workItemId);
        }
        
        DefaultRelease.Builder builder = new DefaultRelease.Builder()
                .id(release.getId())
                .version(release.getVersion())
                .description(release.getDescription())
                .createdAt(release.getCreatedAt())
                .workItems(workItems);
        
        releaseRepository.save(builder.build());
    }
    
    @Override
    public void removeWorkItem(UUID releaseId, UUID workItemId) {
        Release release = findReleaseById(releaseId);
        
        // Create a new release with the work item removed
        List<UUID> workItems = new ArrayList<>(release.getWorkItems());
        workItems.remove(workItemId);
        
        DefaultRelease.Builder builder = new DefaultRelease.Builder()
                .id(release.getId())
                .version(release.getVersion())
                .description(release.getDescription())
                .createdAt(release.getCreatedAt())
                .workItems(workItems);
        
        releaseRepository.save(builder.build());
    }
    
    @Override
    public boolean containsWorkItem(UUID releaseId, UUID workItemId) {
        Release release = findReleaseById(releaseId);
        return release.getWorkItems().contains(workItemId);
    }
    
    @Override
    public List<WorkItem> getWorkItems(UUID releaseId) {
        Release release = findReleaseById(releaseId);
        
        return release.getWorkItems().stream()
                .map(itemService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Release> findById(UUID id) {
        return releaseRepository.findById(id);
    }
    
    @Override
    public Optional<Release> findByVersion(String version) {
        return releaseRepository.findByVersion(version);
    }
    
    @Override
    public List<Release> findAll() {
        return releaseRepository.findAll();
    }
    
    private Release findReleaseById(UUID releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("Release with ID " + releaseId + " does not exist"));
    }
    
    private void validateVersionFormat(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException("Version must follow semantic versioning format (major.minor.patch)");
        }
    }
}