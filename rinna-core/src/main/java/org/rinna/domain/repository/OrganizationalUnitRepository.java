/*
 * Repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.OrganizationalUnitCreateRequest;
import org.rinna.domain.model.OrganizationalUnitType;
import org.rinna.domain.model.WorkParadigm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing organizational units.
 */
public interface OrganizationalUnitRepository {
    
    /**
     * Creates a new organizational unit.
     *
     * @param request the create request
     * @return the created organizational unit
     */
    OrganizationalUnit create(OrganizationalUnitCreateRequest request);
    
    /**
     * Finds an organizational unit by its ID.
     *
     * @param id the ID of the organizational unit
     * @return an Optional containing the organizational unit, or empty if not found
     */
    Optional<OrganizationalUnit> findById(UUID id);
    
    /**
     * Finds all organizational units.
     *
     * @return a list of all organizational units
     */
    List<OrganizationalUnit> findAll();
    
    /**
     * Finds organizational units by type.
     *
     * @param type the type of organizational units to find
     * @return a list of organizational units with the given type
     */
    List<OrganizationalUnit> findByType(OrganizationalUnitType type);
    
    /**
     * Finds organizational units by parent.
     *
     * @param parentId the parent ID of organizational units to find
     * @return a list of organizational units with the given parent ID
     */
    List<OrganizationalUnit> findByParent(UUID parentId);
    
    /**
     * Finds organizational units by owner.
     *
     * @param owner the owner of organizational units to find
     * @return a list of organizational units with the given owner
     */
    List<OrganizationalUnit> findByOwner(String owner);
    
    /**
     * Finds organizational units that have a member.
     *
     * @param memberId the ID of the member
     * @return a list of organizational units that have the given member
     */
    List<OrganizationalUnit> findByMember(String memberId);
    
    /**
     * Finds organizational units by domain expertise.
     *
     * @param domain the domain expertise
     * @return a list of organizational units with the given domain expertise
     */
    List<OrganizationalUnit> findByDomainExpertise(CynefinDomain domain);
    
    /**
     * Finds organizational units by work paradigm.
     *
     * @param paradigm the work paradigm
     * @return a list of organizational units with the given work paradigm
     */
    List<OrganizationalUnit> findByWorkParadigm(WorkParadigm paradigm);
    
    /**
     * Finds organizational units by a tag.
     *
     * @param tag the tag
     * @return a list of organizational units with the given tag
     */
    List<OrganizationalUnit> findByTag(String tag);
    
    /**
     * Finds active organizational units.
     *
     * @return a list of active organizational units
     */
    List<OrganizationalUnit> findActive();
    
    /**
     * Finds inactive organizational units.
     *
     * @return a list of inactive organizational units
     */
    List<OrganizationalUnit> findInactive();
    
    /**
     * Finds organizational units with available cognitive capacity.
     *
     * @param minCapacity the minimum available capacity
     * @return a list of organizational units with at least the given available capacity
     */
    List<OrganizationalUnit> findWithAvailableCapacity(int minCapacity);
    
    /**
     * Finds organizational units that are at or above cognitive capacity threshold.
     *
     * @param capacityThresholdPercent the threshold percentage (0-100)
     * @return a list of organizational units at or above the threshold
     */
    List<OrganizationalUnit> findAtCapacityThreshold(int capacityThresholdPercent);
    
    /**
     * Saves changes to an organizational unit.
     *
     * @param organizationalUnit the organizational unit to save
     * @return the saved organizational unit
     */
    OrganizationalUnit save(OrganizationalUnit organizationalUnit);
    
    /**
     * Deletes an organizational unit by ID.
     *
     * @param id the ID of the organizational unit to delete
     */
    void deleteById(UUID id);
    
    /**
     * Associates a work item with an organizational unit.
     *
     * @param organizationalUnitId the organizational unit ID
     * @param workItemId the work item ID
     * @return true if successful
     */
    boolean associateWorkItem(UUID organizationalUnitId, UUID workItemId);
    
    /**
     * Dissociates a work item from an organizational unit.
     *
     * @param organizationalUnitId the organizational unit ID
     * @param workItemId the work item ID
     * @return true if successful
     */
    boolean dissociateWorkItem(UUID organizationalUnitId, UUID workItemId);
    
    /**
     * Finds all work items assigned to an organizational unit.
     *
     * @param organizationalUnitId the organizational unit ID
     * @return a list of work item IDs assigned to the organizational unit
     */
    List<UUID> findWorkItemIdsByOrganizationalUnitId(UUID organizationalUnitId);
    
    /**
     * Associates a workstream with an organizational unit.
     *
     * @param organizationalUnitId the organizational unit ID
     * @param workstreamId the workstream ID
     * @return true if successful
     */
    boolean associateWorkstream(UUID organizationalUnitId, UUID workstreamId);
    
    /**
     * Dissociates a workstream from an organizational unit.
     *
     * @param organizationalUnitId the organizational unit ID
     * @param workstreamId the workstream ID
     * @return true if successful
     */
    boolean dissociateWorkstream(UUID organizationalUnitId, UUID workstreamId);
    
    /**
     * Finds all workstreams associated with an organizational unit.
     *
     * @param organizationalUnitId the organizational unit ID
     * @return a list of workstream IDs associated with the organizational unit
     */
    List<UUID> findWorkstreamIdsByOrganizationalUnitId(UUID organizationalUnitId);
    
    /**
     * Updates the cognitive load of an organizational unit.
     *
     * @param organizationalUnitId the organizational unit ID
     * @param newLoad the new cognitive load
     * @return the updated organizational unit
     */
    OrganizationalUnit updateCognitiveLoad(UUID organizationalUnitId, int newLoad);
    
    /**
     * Finds the organizational unit that owns a work item.
     *
     * @param workItemId the work item ID
     * @return an Optional containing the organizational unit, or empty if not found
     */
    Optional<OrganizationalUnit> findOwningUnitForWorkItem(UUID workItemId);
}