/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.OrganizationalUnitCreateRequest;
import org.rinna.domain.model.OrganizationalUnitRecord;
import org.rinna.domain.model.OrganizationalUnitType;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.model.Workstream;
import org.rinna.domain.repository.OrganizationalUnitRepository;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.OrganizationalUnitService;
import org.rinna.domain.service.WorkstreamService;

/**
 * Default implementation of the OrganizationalUnitService interface.
 */
public class DefaultOrganizationalUnitService implements OrganizationalUnitService {

    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final ItemService itemService;
    private final WorkstreamService workstreamService;
    
    /**
     * Constructor for DefaultOrganizationalUnitService.
     *
     * @param organizationalUnitRepository the organizational unit repository
     * @param itemService the item service
     * @param workstreamService the workstream service
     */
    public DefaultOrganizationalUnitService(
            OrganizationalUnitRepository organizationalUnitRepository,
            ItemService itemService,
            WorkstreamService workstreamService) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.itemService = itemService;
        this.workstreamService = workstreamService;
    }
    
    @Override
    public OrganizationalUnit createOrganizationalUnit(OrganizationalUnitCreateRequest request) {
        return organizationalUnitRepository.create(request);
    }
    
    @Override
    public OrganizationalUnit updateOrganizationalUnit(OrganizationalUnit unit) {
        return organizationalUnitRepository.save(unit);
    }
    
    @Override
    public Optional<OrganizationalUnit> findOrganizationalUnitById(UUID id) {
        return organizationalUnitRepository.findById(id);
    }
    
    @Override
    public List<OrganizationalUnit> findAllOrganizationalUnits() {
        return organizationalUnitRepository.findAll();
    }
    
    @Override
    public List<OrganizationalUnit> findOrganizationalUnitsByType(OrganizationalUnitType type) {
        return organizationalUnitRepository.findByType(type);
    }
    
    @Override
    public List<OrganizationalUnit> findOrganizationalUnitsByParent(UUID parentId) {
        return organizationalUnitRepository.findByParent(parentId);
    }
    
    @Override
    public OrganizationalHierarchy getOrganizationalHierarchy(UUID rootId) {
        Optional<OrganizationalUnit> rootOptional = organizationalUnitRepository.findById(rootId);
        if (!rootOptional.isPresent()) {
            return null;
        }
        
        return buildHierarchy(rootOptional.get());
    }
    
    private OrganizationalHierarchy buildHierarchy(OrganizationalUnit unit) {
        List<OrganizationalUnit> children = organizationalUnitRepository.findByParent(unit.getId());
        List<OrganizationalHierarchy> childHierarchies = children.stream()
                .map(this::buildHierarchy)
                .collect(Collectors.toList());
        
        return new OrganizationalHierarchy(unit, childHierarchies);
    }
    
    @Override
    public boolean deleteOrganizationalUnit(UUID id) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(id);
        if (!unitOptional.isPresent()) {
            return false;
        }
        
        // Delete the unit
        organizationalUnitRepository.deleteById(id);
        return true;
    }
    
    @Override
    public List<WorkItem> findWorkItemsByOrganizationalUnit(UUID unitId) {
        List<UUID> workItemIds = organizationalUnitRepository.findWorkItemIdsByOrganizationalUnitId(unitId);
        
        List<WorkItem> workItems = new ArrayList<>();
        for (UUID workItemId : workItemIds) {
            itemService.findById(workItemId).ifPresent(workItems::add);
        }
        
        return workItems;
    }
    
    @Override
    public List<Workstream> findWorkstreamsByOrganizationalUnit(UUID unitId) {
        List<UUID> workstreamIds = organizationalUnitRepository.findWorkstreamIdsByOrganizationalUnitId(unitId);
        
        List<Workstream> workstreams = new ArrayList<>();
        for (UUID workstreamId : workstreamIds) {
            workstreamService.findWorkstreamById(workstreamId).ifPresent(workstreams::add);
        }
        
        return workstreams;
    }
    
    @Override
    public boolean assignWorkItem(UUID unitId, UUID workItemId) {
        return organizationalUnitRepository.associateWorkItem(unitId, workItemId);
    }
    
    @Override
    public boolean unassignWorkItem(UUID unitId, UUID workItemId) {
        return organizationalUnitRepository.dissociateWorkItem(unitId, workItemId);
    }
    
    @Override
    public OrganizationalUnit updateCognitiveLoadCalculation(UUID unitId) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        if (!unitOptional.isPresent()) {
            return null;
        }
        
        // Get all work items assigned to this unit
        List<WorkItem> workItems = findWorkItemsByOrganizationalUnit(unitId);
        
        // Calculate the total cognitive load
        int totalLoad = calculateTotalCognitiveLoad(workItems);
        
        // Update the unit with the new cognitive load
        return organizationalUnitRepository.updateCognitiveLoad(unitId, totalLoad);
    }
    
    private int calculateTotalCognitiveLoad(List<WorkItem> workItems) {
        int totalLoad = 0;
        for (WorkItem workItem : workItems) {
            // The cognitive load of a work item depends on its complexity and size
            // This is a simplified calculation, in a real system this would be more sophisticated
            int itemLoad = getCognitiveLoadForWorkItem(workItem);
            totalLoad += itemLoad;
        }
        return totalLoad;
    }
    
    private int getCognitiveLoadForWorkItem(WorkItem workItem) {
        // This is a simplified calculation based on the work item's type and priority
        // In a real system, this would take into account more factors and data
        
        int baseLoad = 5; // Base load for any work item
        
        // Adjust based on work item type
        switch (workItem.getType()) {
            case BUG:
                baseLoad += 5;
                break;
            case FEATURE:
                baseLoad += 10;
                break;
            case EPIC:
                baseLoad += 20;
                break;
            case STORY:
                baseLoad += 8;
                break;
            case TASK:
                baseLoad += 3;
                break;
            default:
                // No adjustment for other types
                break;
        }
        
        // Adjust based on priority
        switch (workItem.getPriority()) {
            case CRITICAL:
                baseLoad += 10;
                break;
            case HIGH:
                baseLoad += 5;
                break;
            case MEDIUM:
                baseLoad += 3;
                break;
            case LOW:
                baseLoad += 1;
                break;
            default:
                // No adjustment for other priorities
                break;
        }
        
        return baseLoad;
    }
    
    @Override
    public List<OrganizationalUnit> findUnitsWithAvailableCapacity(int requiredCapacity) {
        return organizationalUnitRepository.findWithAvailableCapacity(requiredCapacity);
    }
    
    @Override
    public List<OrganizationalUnit> findOverloadedUnits(int thresholdPercent) {
        return organizationalUnitRepository.findAtCapacityThreshold(thresholdPercent);
    }
    
    @Override
    public List<OrganizationalUnit> suggestUnitsForWorkItem(UUID workItemId) {
        Optional<WorkItem> workItemOptional = itemService.findById(workItemId);
        if (!workItemOptional.isPresent()) {
            return Collections.emptyList();
        }
        
        WorkItem workItem = workItemOptional.get();
        
        // Find all active organizational units
        List<OrganizationalUnit> activeUnits = organizationalUnitRepository.findActive();
        
        // Score each unit based on various factors
        Map<UUID, Integer> unitScores = new HashMap<>();
        for (OrganizationalUnit unit : activeUnits) {
            int score = calculateUnitSuitabilityScore(unit, workItem);
            unitScores.put(unit.getId(), score);
        }
        
        // Sort units by score (descending)
        return activeUnits.stream()
                .sorted(Comparator.comparingInt(u -> -unitScores.getOrDefault(u.getId(), 0)))
                .collect(Collectors.toList());
    }
    
    private int calculateUnitSuitabilityScore(OrganizationalUnit unit, WorkItem workItem) {
        int score = 0;
        
        // Factor 1: Domain expertise match
        if (workItem.getCynefinDomain().isPresent() && 
                unit.getDomainExpertise().contains(workItem.getCynefinDomain().get())) {
            score += 50; // Major factor
        }
        
        // Factor 2: Work paradigm match
        if (workItem.getWorkParadigm().isPresent() && 
                unit.getWorkParadigms().contains(workItem.getWorkParadigm().get())) {
            score += 30; // Important factor
        }
        
        // Factor 3: Cognitive capacity
        int requiredCapacity = getCognitiveLoadForWorkItem(workItem);
        int availableCapacity = unit.getCognitiveCapacity() - unit.getCurrentCognitiveLoad();
        
        if (availableCapacity >= requiredCapacity) {
            score += 20; // Has enough capacity
            
            // Bonus for having optimal capacity (not too much, not too little)
            double utilizationRatio = (double) (unit.getCurrentCognitiveLoad() + requiredCapacity) / unit.getCognitiveCapacity();
            if (utilizationRatio >= 0.7 && utilizationRatio <= 0.9) {
                score += 10; // Optimal utilization range
            }
        } else {
            // Penalize for overloading
            score -= 50;
        }
        
        // Factor 4: Team size (prefer larger teams for complex work)
        if (workItem.getCynefinDomain().isPresent() && 
                workItem.getCynefinDomain().get() == CynefinDomain.COMPLEX) {
            if (unit.getMembers().size() >= 3) {
                score += 15; // Larger team is better for complex work
            }
        }
        
        // Factor 5: Previous associations
        // If this unit is already working on related items, it might be a good fit
        List<WorkItem> unitItems = findWorkItemsByOrganizationalUnit(unit.getId());
        boolean hasRelatedItems = unitItems.stream()
                .anyMatch(item -> itemsAreRelated(item, workItem));
        
        if (hasRelatedItems) {
            score += 25; // Significant bonus for working on related items
        }
        
        return score;
    }
    
    private boolean itemsAreRelated(WorkItem item1, WorkItem item2) {
        // Items are related if they are in the same project, have the same type, 
        // or share the same CYNEFIN domain or work paradigm
        
        // Same project
        if (item1.getProjectId().equals(item2.getProjectId())) {
            return true;
        }
        
        // Same type
        if (item1.getType() == item2.getType()) {
            return true;
        }
        
        // Same CYNEFIN domain
        if (item1.getCynefinDomain().isPresent() && item2.getCynefinDomain().isPresent() &&
                item1.getCynefinDomain().get() == item2.getCynefinDomain().get()) {
            return true;
        }
        
        // Same work paradigm
        if (item1.getWorkParadigm().isPresent() && item2.getWorkParadigm().isPresent() &&
                item1.getWorkParadigm().get() == item2.getWorkParadigm().get()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public OrganizationalUnit addDomainExpertise(UUID unitId, CynefinDomain domain) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        if (!unitOptional.isPresent()) {
            return null;
        }
        
        OrganizationalUnit unit = unitOptional.get();
        if (unit instanceof OrganizationalUnitRecord) {
            OrganizationalUnitRecord record = (OrganizationalUnitRecord) unit;
            OrganizationalUnit updated = record.withAddedDomainExpertise(domain);
            return organizationalUnitRepository.save(updated);
        }
        
        return unit;
    }
    
    @Override
    public OrganizationalUnit addWorkParadigm(UUID unitId, WorkParadigm paradigm) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        if (!unitOptional.isPresent()) {
            return null;
        }
        
        OrganizationalUnit unit = unitOptional.get();
        if (unit instanceof OrganizationalUnitRecord) {
            OrganizationalUnitRecord record = (OrganizationalUnitRecord) unit;
            OrganizationalUnit updated = record.withAddedWorkParadigm(paradigm);
            return organizationalUnitRepository.save(updated);
        }
        
        return unit;
    }
    
    @Override
    public OrganizationalUnit addMember(UUID unitId, String memberId) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        if (!unitOptional.isPresent()) {
            return null;
        }
        
        OrganizationalUnit unit = unitOptional.get();
        if (unit instanceof OrganizationalUnitRecord) {
            OrganizationalUnitRecord record = (OrganizationalUnitRecord) unit;
            OrganizationalUnit updated = record.withAddedMember(memberId);
            return organizationalUnitRepository.save(updated);
        }
        
        return unit;
    }
    
    @Override
    public OrganizationalUnit removeMember(UUID unitId, String memberId) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        if (!unitOptional.isPresent()) {
            return null;
        }
        
        OrganizationalUnit unit = unitOptional.get();
        if (unit instanceof OrganizationalUnitRecord) {
            OrganizationalUnitRecord record = (OrganizationalUnitRecord) unit;
            OrganizationalUnit updated = record.withRemovedMember(memberId);
            return organizationalUnitRepository.save(updated);
        }
        
        return unit;
    }
    
    @Override
    public boolean setAsOwningUnit(UUID unitId, UUID workItemId) {
        // We're using a specific implementation method from InMemoryOrganizationalUnitRepository
        if (organizationalUnitRepository instanceof org.rinna.adapter.repository.InMemoryOrganizationalUnitRepository) {
            org.rinna.adapter.repository.InMemoryOrganizationalUnitRepository repo = 
                    (org.rinna.adapter.repository.InMemoryOrganizationalUnitRepository) organizationalUnitRepository;
            return repo.setOwningUnitForWorkItem(workItemId, unitId);
        }
        
        // Generic fallback for other repository implementations
        // First check if the unit exists
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        if (!unitOptional.isPresent()) {
            return false;
        }
        
        // Ensure the work item is associated with the unit
        boolean associated = organizationalUnitRepository.associateWorkItem(unitId, workItemId);
        
        return associated;
    }
    
    @Override
    public Optional<OrganizationalUnit> findOwningUnit(UUID workItemId) {
        return organizationalUnitRepository.findOwningUnitForWorkItem(workItemId);
    }
    
    @Override
    public int calculateCognitiveImpact(UUID unitId, UUID workItemId) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        Optional<WorkItem> workItemOptional = itemService.findById(workItemId);
        
        if (!unitOptional.isPresent() || !workItemOptional.isPresent()) {
            return -1; // Invalid input
        }
        
        OrganizationalUnit unit = unitOptional.get();
        WorkItem workItem = workItemOptional.get();
        
        int workItemLoad = getCognitiveLoadForWorkItem(workItem);
        return unit.getCurrentCognitiveLoad() + workItemLoad;
    }
}