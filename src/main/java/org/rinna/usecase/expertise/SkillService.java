package org.rinna.usecase.expertise;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.expertise.LevelCriteria;
import org.rinna.domain.model.expertise.Skill;
import org.rinna.domain.model.expertise.SkillDomain;

/**
 * Service interface for managing skills and skill domains in the Rinna Expertise Rating System.
 */
public interface SkillService {
    
    /**
     * Retrieves a skill by its ID.
     *
     * @param skillId the unique identifier of the skill
     * @return an optional containing the skill if found, empty otherwise
     */
    Optional<Skill> getSkillById(UUID skillId);
    
    /**
     * Retrieves a skill domain by its ID.
     *
     * @param domainId the unique identifier of the skill domain
     * @return an optional containing the skill domain if found, empty otherwise
     */
    Optional<SkillDomain> getDomainById(UUID domainId);
    
    /**
     * Retrieves all skills in a domain.
     *
     * @param domainId the unique identifier of the skill domain
     * @return a list of skills in the domain
     */
    List<Skill> getSkillsByDomain(UUID domainId);
    
    /**
     * Retrieves all top-level skill domains (domains without a parent).
     *
     * @return a list of top-level skill domains
     */
    List<SkillDomain> getTopLevelDomains();
    
    /**
     * Retrieves all subdomains of a domain.
     *
     * @param parentDomainId the unique identifier of the parent domain
     * @return a list of subdomains
     */
    List<SkillDomain> getSubdomains(UUID parentDomainId);
    
    /**
     * Creates a new skill.
     *
     * @param name the name of the skill
     * @param description the description of the skill
     * @param domainId the unique identifier of the skill domain
     * @param objectiveAssessmentId optional unique identifier of the objective assessment
     * @param levelCriteria list of level criteria for the skill
     * @param relatedSkills list of related skill IDs
     * @param commonMisconceptions list of common misconceptions about the skill
     * @return the newly created skill
     */
    Skill createSkill(String name,
                     String description,
                     UUID domainId,
                     UUID objectiveAssessmentId,
                     List<LevelCriteria> levelCriteria,
                     List<UUID> relatedSkills,
                     List<String> commonMisconceptions);
    
    /**
     * Creates a new skill domain.
     *
     * @param name the name of the domain
     * @param description the description of the domain
     * @param parentDomainId optional unique identifier of the parent domain
     * @param levelDescriptors list of level descriptors for the domain
     * @return the newly created skill domain
     */
    SkillDomain createDomain(String name,
                           String description,
                           UUID parentDomainId,
                           List<SkillDomain.LevelDescriptor> levelDescriptors);
    
    /**
     * Updates an existing skill.
     *
     * @param skillId the unique identifier of the skill
     * @param name the new name
     * @param description the new description
     * @param objectiveAssessmentId the new objective assessment ID
     * @param levelCriteria the new level criteria
     * @param relatedSkills the new related skills
     * @param commonMisconceptions the new common misconceptions
     * @return the updated skill
     */
    Skill updateSkill(UUID skillId,
                     String name,
                     String description,
                     UUID objectiveAssessmentId,
                     List<LevelCriteria> levelCriteria,
                     List<UUID> relatedSkills,
                     List<String> commonMisconceptions);
    
    /**
     * Updates an existing skill domain.
     *
     * @param domainId the unique identifier of the domain
     * @param name the new name
     * @param description the new description
     * @param parentDomainId the new parent domain ID
     * @param levelDescriptors the new level descriptors
     * @return the updated skill domain
     */
    SkillDomain updateDomain(UUID domainId,
                           String name,
                           String description,
                           UUID parentDomainId,
                           List<SkillDomain.LevelDescriptor> levelDescriptors);
    
    /**
     * Adds a skill to a domain.
     *
     * @param skillId the unique identifier of the skill
     * @param domainId the unique identifier of the domain
     * @return the updated skill domain
     */
    SkillDomain addSkillToDomain(UUID skillId, UUID domainId);
    
    /**
     * Removes a skill from a domain.
     *
     * @param skillId the unique identifier of the skill
     * @param domainId the unique identifier of the domain
     * @return the updated skill domain
     */
    SkillDomain removeSkillFromDomain(UUID skillId, UUID domainId);
    
    /**
     * Searches for skills by name or description.
     *
     * @param query the search query
     * @return a list of skills matching the query
     */
    List<Skill> searchSkills(String query);
    
    /**
     * Searches for skill domains by name or description.
     *
     * @param query the search query
     * @return a list of skill domains matching the query
     */
    List<SkillDomain> searchDomains(String query);
    
    /**
     * Gets skills that are related to a specific skill.
     *
     * @param skillId the unique identifier of the skill
     * @return a list of related skills
     */
    List<Skill> getRelatedSkills(UUID skillId);
    
    /**
     * Gets the full hierarchy path of a domain.
     *
     * @param domainId the unique identifier of the domain
     * @return a list of domains representing the hierarchy path, from top-level to the specified domain
     */
    List<SkillDomain> getDomainHierarchy(UUID domainId);
    
    /**
     * Deletes a skill.
     *
     * @param skillId the unique identifier of the skill
     * @return true if the skill was deleted, false otherwise
     */
    boolean deleteSkill(UUID skillId);
    
    /**
     * Deletes a skill domain.
     *
     * @param domainId the unique identifier of the domain
     * @param deleteContainedSkills whether to also delete skills in the domain
     * @return true if the domain was deleted, false otherwise
     */
    boolean deleteDomain(UUID domainId, boolean deleteContainedSkills);
}