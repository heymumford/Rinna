/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.OrganizationalUnitCreateRequest;
import org.rinna.domain.model.OrganizationalUnitRecord;
import org.rinna.domain.model.OrganizationalUnitType;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.repository.OrganizationalUnitRepository;

/**
 * In-memory implementation of the OrganizationalUnitRepository interface.
 * For testing and demonstration purposes only.
 */
public class InMemoryOrganizationalUnitRepository implements OrganizationalUnitRepository {
    
    private final Map<UUID, OrganizationalUnit> units = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> unitToWorkItems = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> workItemToUnits = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> unitToWorkstreams = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> workstreamToUnits = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> workItemToOwningUnit = new ConcurrentHashMap<>();
    
    @Override
    public OrganizationalUnit create(OrganizationalUnitCreateRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        OrganizationalUnit unit = new OrganizationalUnitRecord(
            id,
            request.name(),
            request.description(),
            request.type(),
            request.parentId(),
            request.owner(),
            now,
            now,
            request.cognitiveCapacity(),
            0, // Initial cognitive load is 0
            request.members(),
            request.active(),
            request.domainExpertise(),
            request.workParadigms(),
            request.tags()
        );
        
        return save(unit);
    }
    
    @Override
    public Optional<OrganizationalUnit> findById(UUID id) {
        return Optional.ofNullable(units.get(id));
    }
    
    @Override
    public List<OrganizationalUnit> findAll() {
        return new ArrayList<>(units.values());
    }
    
    @Override
    public List<OrganizationalUnit> findByType(OrganizationalUnitType type) {
        return units.values().stream()
                .filter(u -> u.getType() == type)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findByParent(UUID parentId) {
        return units.values().stream()
                .filter(u -> u.getParentId().isPresent() && Objects.equals(parentId, u.getParentId().get()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findByOwner(String owner) {
        return units.values().stream()
                .filter(u -> Objects.equals(owner, u.getOwner()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findByMember(String memberId) {
        return units.values().stream()
                .filter(u -> u.getMembers().contains(memberId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findByDomainExpertise(CynefinDomain domain) {
        return units.values().stream()
                .filter(u -> u.getDomainExpertise().contains(domain))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findByWorkParadigm(WorkParadigm paradigm) {
        return units.values().stream()
                .filter(u -> u.getWorkParadigms().contains(paradigm))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findByTag(String tag) {
        return units.values().stream()
                .filter(u -> u.getTags().contains(tag))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findActive() {
        return units.values().stream()
                .filter(OrganizationalUnit::isActive)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findInactive() {
        return units.values().stream()
                .filter(u -> !u.isActive())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findWithAvailableCapacity(int minCapacity) {
        return units.values().stream()
                .filter(u -> (u.getCognitiveCapacity() - u.getCurrentCognitiveLoad()) >= minCapacity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationalUnit> findAtCapacityThreshold(int capacityThresholdPercent) {
        return units.values().stream()
                .filter(u -> {
                    if (u.getCognitiveCapacity() == 0) return false;
                    double percentUsed = ((double) u.getCurrentCognitiveLoad() / u.getCognitiveCapacity()) * 100;
                    return percentUsed >= capacityThresholdPercent;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public OrganizationalUnit save(OrganizationalUnit unit) {
        units.put(unit.getId(), unit);
        return unit;
    }
    
    @Override
    public void deleteById(UUID id) {
        // Remove organizational unit
        units.remove(id);
        
        // Clean up work item associations
        Set<UUID> workItemIds = unitToWorkItems.getOrDefault(id, Collections.emptySet());
        for (UUID workItemId : workItemIds) {
            Set<UUID> itemUnits = workItemToUnits.get(workItemId);
            if (itemUnits != null) {
                itemUnits.remove(id);
                if (itemUnits.isEmpty()) {
                    workItemToUnits.remove(workItemId);
                }
            }
            
            // If this unit owned the work item, remove the ownership
            if (Objects.equals(workItemToOwningUnit.get(workItemId), id)) {
                workItemToOwningUnit.remove(workItemId);
            }
        }
        unitToWorkItems.remove(id);
        
        // Clean up workstream associations
        Set<UUID> workstreamIds = unitToWorkstreams.getOrDefault(id, Collections.emptySet());
        for (UUID workstreamId : workstreamIds) {
            Set<UUID> streamUnits = workstreamToUnits.get(workstreamId);
            if (streamUnits != null) {
                streamUnits.remove(id);
                if (streamUnits.isEmpty()) {
                    workstreamToUnits.remove(workstreamId);
                }
            }
        }
        unitToWorkstreams.remove(id);
    }
    
    @Override
    public boolean associateWorkItem(UUID organizationalUnitId, UUID workItemId) {
        if (!units.containsKey(organizationalUnitId)) {
            return false;
        }
        
        // Associate unit -> work item
        unitToWorkItems.computeIfAbsent(organizationalUnitId, k -> new HashSet<>()).add(workItemId);
        
        // Associate work item -> unit
        workItemToUnits.computeIfAbsent(workItemId, k -> new HashSet<>()).add(organizationalUnitId);
        
        return true;
    }
    
    @Override
    public boolean dissociateWorkItem(UUID organizationalUnitId, UUID workItemId) {
        boolean removed = false;
        
        // Remove from unit -> work items
        Set<UUID> workItems = unitToWorkItems.get(organizationalUnitId);
        if (workItems != null) {
            removed = workItems.remove(workItemId);
            if (workItems.isEmpty()) {
                unitToWorkItems.remove(organizationalUnitId);
            }
        }
        
        // Remove from work item -> units
        Set<UUID> itemUnits = workItemToUnits.get(workItemId);
        if (itemUnits != null) {
            itemUnits.remove(organizationalUnitId);
            if (itemUnits.isEmpty()) {
                workItemToUnits.remove(workItemId);
            }
        }
        
        // If this unit owned the work item, remove the ownership
        if (Objects.equals(workItemToOwningUnit.get(workItemId), organizationalUnitId)) {
            workItemToOwningUnit.remove(workItemId);
        }
        
        return removed;
    }
    
    @Override
    public List<UUID> findWorkItemIdsByOrganizationalUnitId(UUID organizationalUnitId) {
        return new ArrayList<>(unitToWorkItems.getOrDefault(organizationalUnitId, Collections.emptySet()));
    }
    
    @Override
    public boolean associateWorkstream(UUID organizationalUnitId, UUID workstreamId) {
        if (!units.containsKey(organizationalUnitId)) {
            return false;
        }
        
        // Associate unit -> workstream
        unitToWorkstreams.computeIfAbsent(organizationalUnitId, k -> new HashSet<>()).add(workstreamId);
        
        // Associate workstream -> unit
        workstreamToUnits.computeIfAbsent(workstreamId, k -> new HashSet<>()).add(organizationalUnitId);
        
        return true;
    }
    
    @Override
    public boolean dissociateWorkstream(UUID organizationalUnitId, UUID workstreamId) {
        boolean removed = false;
        
        // Remove from unit -> workstreams
        Set<UUID> workstreams = unitToWorkstreams.get(organizationalUnitId);
        if (workstreams != null) {
            removed = workstreams.remove(workstreamId);
            if (workstreams.isEmpty()) {
                unitToWorkstreams.remove(organizationalUnitId);
            }
        }
        
        // Remove from workstream -> units
        Set<UUID> streamUnits = workstreamToUnits.get(workstreamId);
        if (streamUnits != null) {
            streamUnits.remove(organizationalUnitId);
            if (streamUnits.isEmpty()) {
                workstreamToUnits.remove(workstreamId);
            }
        }
        
        return removed;
    }
    
    @Override
    public List<UUID> findWorkstreamIdsByOrganizationalUnitId(UUID organizationalUnitId) {
        return new ArrayList<>(unitToWorkstreams.getOrDefault(organizationalUnitId, Collections.emptySet()));
    }
    
    @Override
    public OrganizationalUnit updateCognitiveLoad(UUID organizationalUnitId, int newLoad) {
        OrganizationalUnit unit = units.get(organizationalUnitId);
        if (unit == null) {
            return null;
        }
        
        // Create a new updated unit with the new cognitive load
        OrganizationalUnitRecord updatedUnit = ((OrganizationalUnitRecord) unit).withCurrentCognitiveLoad(newLoad);
        
        // Save the updated unit
        return save(updatedUnit);
    }
    
    @Override
    public Optional<OrganizationalUnit> findOwningUnitForWorkItem(UUID workItemId) {
        UUID owningUnitId = workItemToOwningUnit.get(workItemId);
        if (owningUnitId != null) {
            return Optional.ofNullable(units.get(owningUnitId));
        }
        return Optional.empty();
    }
    
    /**
     * Sets the owning unit for a work item.
     *
     * @param workItemId the work item ID
     * @param organizationalUnitId the organizational unit ID
     * @return true if successful
     */
    public boolean setOwningUnitForWorkItem(UUID workItemId, UUID organizationalUnitId) {
        if (!units.containsKey(organizationalUnitId)) {
            return false;
        }
        
        workItemToOwningUnit.put(workItemId, organizationalUnitId);
        
        // Also ensure the work item is associated with the unit
        associateWorkItem(organizationalUnitId, workItemId);
        
        return true;
    }
    
    /**
     * Clears all data from the repository (for testing).
     */
    public void clear() {
        units.clear();
        unitToWorkItems.clear();
        workItemToUnits.clear();
        unitToWorkstreams.clear();
        workstreamToUnits.clear();
        workItemToOwningUnit.clear();
    }
}