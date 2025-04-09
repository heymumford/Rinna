/*
 * Adapter implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.rinna.domain.model.DefaultRelease;
import org.rinna.domain.model.Release;
import org.rinna.domain.repository.ReleaseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the ReleaseRepository interface.
 */
public class InMemoryReleaseRepository implements ReleaseRepository {
    private final Map<UUID, Release> releases = new HashMap<>();
    
    @Override
    public Release save(Release release) {
        Objects.requireNonNull(release, "Release cannot be null");
        
        // Copy the release to ensure immutability
        DefaultRelease.Builder builder = new DefaultRelease.Builder()
                .id(release.getId())
                .version(release.getVersion())
                .description(release.getDescription())
                .createdAt(release.getCreatedAt())
                .workItems(release.getWorkItems());
        
        Release savedRelease = builder.build();
        releases.put(savedRelease.getId(), savedRelease);
        
        return savedRelease;
    }
    
    @Override
    public Optional<Release> findById(UUID id) {
        return Optional.ofNullable(releases.get(id));
    }
    
    @Override
    public Optional<Release> findByVersion(String version) {
        return releases.values().stream()
                .filter(r -> r.getVersion().equals(version))
                .findFirst();
    }
    
    @Override
    public List<Release> findAll() {
        return new ArrayList<>(releases.values());
    }
    
    @Override
    public void deleteById(UUID id) {
        releases.remove(id);
    }
}