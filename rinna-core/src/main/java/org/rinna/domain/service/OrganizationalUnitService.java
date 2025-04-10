/*
 * Service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.OrganizationalUnitCreateRequest;
import org.rinna.domain.model.OrganizationalUnitType;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.model.Workstream;

/**
 * Service interface for managing organizational units.
 */
public interface OrganizationalUnitService {
    
    /**
     * Creates a new organizational unit.
     *
     * @param request the create request
     * @return the created organizational unit
     */
    OrganizationalUnit createOrganizationalUnit(OrganizationalUnitCreateRequest request);
    
    /**
     * Updates an existing organizational unit.
     *
     * @param unit the updated organizational unit
     * @return the updated organizational unit
     */
    OrganizationalUnit updateOrganizationalUnit(OrganizationalUnit unit);
    
    /**
     * Finds an organizational unit by its ID.
     *
     * @param id the ID of the organizational unit
     * @return an Optional containing the organizational unit, or empty if not found
     */
    Optional<OrganizationalUnit> findOrganizationalUnitById(UUID id);
    
    /**
     * Finds all organizational units.
     *
     * @return a list of all organizational units
     */
    List<OrganizationalUnit> findAllOrganizationalUnits();
    
    /**
     * Finds organizational units by type.
     *
     * @param type the type of organizational units to find
     * @return a list of organizational units with the given type
     */
    List<OrganizationalUnit> findOrganizationalUnitsByType(OrganizationalUnitType type);
    
    /**
     * Finds organizational units by parent.
     *
     * @param parentId the parent ID of organizational units to find
     * @return a list of organizational units with the given parent ID
     */
    List<OrganizationalUnit> findOrganizationalUnitsByParent(UUID parentId);
    
    /**
     * Finds the complete organizational hierarchy starting from a root unit.
     *
     * @param rootId the ID of the root organizational unit
     * @return a hierarchical structure of organizational units
     */
    OrganizationalHierarchy getOrganizationalHierarchy(UUID rootId);
    
    /**
     * Deletes an organizational unit by ID.
     *
     * @param id the ID of the organizational unit to delete
     * @return true if successful, false if the unit was not found
     */
    boolean deleteOrganizationalUnit(UUID id);
    
    /**
     * Finds work items assigned to an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @return a list of work items assigned to the organizational unit
     */
    List<WorkItem> findWorkItemsByOrganizationalUnit(UUID unitId);
    
    /**
     * Finds workstreams associated with an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @return a list of workstreams associated with the organizational unit
     */
    List<Workstream> findWorkstreamsByOrganizationalUnit(UUID unitId);
    
    /**
     * Assigns a work item to an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @param workItemId the ID of the work item
     * @return true if successful
     */
    boolean assignWorkItem(UUID unitId, UUID workItemId);
    
    /**
     * Unassigns a work item from an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @param workItemId the ID of the work item
     * @return true if successful
     */
    boolean unassignWorkItem(UUID unitId, UUID workItemId);
    
    /**
     * Updates the cognitive load calculation for an organizational unit.
     * This recalculates the load based on all assigned work items.
     *
     * @param unitId the ID of the organizational unit
     * @return the updated organizational unit
     */
    OrganizationalUnit updateCognitiveLoadCalculation(UUID unitId);
    
    /**
     * Finds organizational units that have available capacity for new work.
     *
     * @param requiredCapacity the required capacity
     * @return a list of organizational units with sufficient available capacity
     */
    List<OrganizationalUnit> findUnitsWithAvailableCapacity(int requiredCapacity);
    
    /**
     * Finds organizational units that are at risk of overload (above a threshold).
     *
     * @param thresholdPercent the threshold percentage (0-100)
     * @return a list of organizational units at or above the threshold
     */
    List<OrganizationalUnit> findOverloadedUnits(int thresholdPercent);
    
    /**
     * Suggests the most suitable organizational unit for a work item based on
     * domain expertise, work paradigm, cognitive load, and other factors.
     *
     * @param workItemId the ID of the work item
     * @return a list of organizational units ranked by suitability
     */
    List<OrganizationalUnit> suggestUnitsForWorkItem(UUID workItemId);
    
    /**
     * Adds a new domain expertise to an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @param domain the domain expertise to add
     * @return the updated organizational unit
     */
    OrganizationalUnit addDomainExpertise(UUID unitId, CynefinDomain domain);
    
    /**
     * Adds a new work paradigm to an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @param paradigm the work paradigm to add
     * @return the updated organizational unit
     */
    OrganizationalUnit addWorkParadigm(UUID unitId, WorkParadigm paradigm);
    
    /**
     * Adds a member to an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @param memberId the ID of the member to add
     * @return the updated organizational unit
     */
    OrganizationalUnit addMember(UUID unitId, String memberId);
    
    /**
     * Removes a member from an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @param memberId the ID of the member to remove
     * @return the updated organizational unit
     */
    OrganizationalUnit removeMember(UUID unitId, String memberId);
    
    /**
     * Sets a unit as the owner of a work item.
     *
     * @param unitId the ID of the organizational unit
     * @param workItemId the ID of the work item
     * @return true if successful
     */
    boolean setAsOwningUnit(UUID unitId, UUID workItemId);
    
    /**
     * Finds the organizational unit that owns a work item.
     *
     * @param workItemId the ID of the work item
     * @return an Optional containing the organizational unit, or empty if not found
     */
    Optional<OrganizationalUnit> findOwningUnit(UUID workItemId);
    
    /**
     * Calculates the cognitive impact of assigning a work item to an organizational unit.
     *
     * @param unitId the ID of the organizational unit
     * @param workItemId the ID of the work item
     * @return the estimated cognitive load after assignment
     */
    int calculateCognitiveImpact(UUID unitId, UUID workItemId);
    
    /**
     * Class representing the organizational hierarchy.
     */
    class OrganizationalHierarchy {
        private final OrganizationalUnit unit;
        private final List<OrganizationalHierarchy> children;
        
        public OrganizationalHierarchy(OrganizationalUnit unit, List<OrganizationalHierarchy> children) {
            this.unit = unit;
            this.children = children;
        }
        
        public OrganizationalUnit getUnit() {
            return unit;
        }
        
        public List<OrganizationalHierarchy> getChildren() {
            return children;
        }
    }
}