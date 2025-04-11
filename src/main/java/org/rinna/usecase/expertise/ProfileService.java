package org.rinna.usecase.expertise;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.rinna.domain.model.expertise.PersonProfile;

/**
 * Service interface for managing person profiles in the Rinna Expertise Rating System.
 */
public interface ProfileService {
    
    /**
     * Retrieves a profile by its ID.
     *
     * @param profileId the unique identifier of the profile
     * @return an optional containing the profile if found, empty otherwise
     */
    Optional<PersonProfile> getProfileById(UUID profileId);
    
    /**
     * Retrieves a profile by email.
     *
     * @param email the email address of the profile
     * @return an optional containing the profile if found, empty otherwise
     */
    Optional<PersonProfile> getProfileByEmail(String email);
    
    /**
     * Retrieves profiles by organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of profiles in the organization
     */
    List<PersonProfile> getProfilesByOrganization(UUID organizationId);
    
    /**
     * Creates a new profile.
     *
     * @param name the name of the person
     * @param email the email address of the person
     * @param publicKey optional public key for the person
     * @param organizationIds the set of organization IDs the person belongs to
     * @param externalIds optional map of external system IDs for the person
     * @return the newly created profile
     */
    PersonProfile createProfile(String name,
                               String email,
                               String publicKey,
                               Set<UUID> organizationIds,
                               Map<String, String> externalIds);
    
    /**
     * Updates an existing profile.
     *
     * @param profileId the unique identifier of the profile
     * @param name the new name
     * @param email the new email address
     * @param publicKey the new public key
     * @param organizationIds the new set of organization IDs
     * @return the updated profile
     */
    PersonProfile updateProfile(UUID profileId,
                               String name,
                               String email,
                               String publicKey,
                               Set<UUID> organizationIds);
    
    /**
     * Adds an external ID to a profile.
     *
     * @param profileId the unique identifier of the profile
     * @param system the external system name
     * @param externalId the ID in the external system
     * @return the updated profile
     */
    PersonProfile addExternalId(UUID profileId, String system, String externalId);
    
    /**
     * Removes an external ID from a profile.
     *
     * @param profileId the unique identifier of the profile
     * @param system the external system name
     * @return the updated profile
     */
    PersonProfile removeExternalId(UUID profileId, String system);
    
    /**
     * Adds metadata to a profile.
     *
     * @param profileId the unique identifier of the profile
     * @param key the metadata key
     * @param value the metadata value
     * @return the updated profile
     */
    PersonProfile addMetadata(UUID profileId, String key, Object value);
    
    /**
     * Removes metadata from a profile.
     *
     * @param profileId the unique identifier of the profile
     * @param key the metadata key
     * @return the updated profile
     */
    PersonProfile removeMetadata(UUID profileId, String key);
    
    /**
     * Adds an organization to a profile.
     *
     * @param profileId the unique identifier of the profile
     * @param organizationId the unique identifier of the organization
     * @return the updated profile
     */
    PersonProfile addOrganization(UUID profileId, UUID organizationId);
    
    /**
     * Removes an organization from a profile.
     *
     * @param profileId the unique identifier of the profile
     * @param organizationId the unique identifier of the organization
     * @return the updated profile
     */
    PersonProfile removeOrganization(UUID profileId, UUID organizationId);
    
    /**
     * Searches for profiles by name or email.
     *
     * @param query the search query
     * @return a list of profiles matching the query
     */
    List<PersonProfile> searchProfiles(String query);
    
    /**
     * Gets a profile by external ID.
     *
     * @param system the external system name
     * @param externalId the ID in the external system
     * @return an optional containing the profile if found, empty otherwise
     */
    Optional<PersonProfile> getProfileByExternalId(String system, String externalId);
    
    /**
     * Deletes a profile.
     *
     * @param profileId the unique identifier of the profile
     * @return true if the profile was deleted, false otherwise
     */
    boolean deleteProfile(UUID profileId);
    
    /**
     * Gets the expertise summary for a profile.
     * This method aggregates all expertise ratings for a profile and returns
     * a summary of the person's expertise across different domains.
     *
     * @param profileId the unique identifier of the profile
     * @return a map of domain IDs to expertise summaries
     */
    Map<UUID, ExpertiseSummary> getExpertiseSummary(UUID profileId);
}

/**
 * Represents a summary of a person's expertise in a domain.
 */
record ExpertiseSummary(
        UUID domainId,
        String domainName,
        double averageLevel,
        int totalSkills,
        int certifiedSkills,
        List<UUID> topSkillIds
) { }