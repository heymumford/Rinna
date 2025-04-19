package org.rinna.usecase.expertise;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.expertise.ExpertiseRating;

/**
 * Service interface for managing expertise ratings in the Rinna Expertise Rating System.
 * The Rating Engine is responsible for calculating expertise ratings based on assessments,
 * managing rating history, and providing confidence scores.
 */
public interface RatingService {
    
    /**
     * Retrieves an expertise rating by its ID.
     *
     * @param ratingId the unique identifier of the rating
     * @return an optional containing the rating if found, empty otherwise
     */
    Optional<ExpertiseRating> getRatingById(UUID ratingId);
    
    /**
     * Retrieves all expertise ratings for a person.
     *
     * @param profileId the unique identifier of the person
     * @return a list of expertise ratings for the person
     */
    List<ExpertiseRating> getRatingsByProfileId(UUID profileId);
    
    /**
     * Retrieves a person's expertise rating for a specific skill.
     *
     * @param profileId the unique identifier of the person
     * @param skillId the unique identifier of the skill
     * @return an optional containing the rating if found, empty otherwise
     */
    Optional<ExpertiseRating> getRatingForSkill(UUID profileId, UUID skillId);
    
    /**
     * Creates a new expertise rating.
     *
     * @param profileId the unique identifier of the person
     * @param skillId the unique identifier of the skill
     * @param initialLevel the initial expertise level (0-10)
     * @return the newly created expertise rating
     */
    ExpertiseRating createRating(UUID profileId, UUID skillId, BigDecimal initialLevel);
    
    /**
     * Updates an existing expertise rating with a new level.
     *
     * @param ratingId the unique identifier of the rating
     * @param newLevel the new expertise level (0-10)
     * @param reason the reason for the update
     * @return the updated expertise rating
     */
    ExpertiseRating updateRatingLevel(UUID ratingId, BigDecimal newLevel, String reason);
    
    /**
     * Calculates a weighted expertise rating based on assessments.
     * This is the core algorithm that processes all assessments and evidence
     * to determine the final expertise level and confidence score.
     *
     * @param ratingId the unique identifier of the rating
     * @return the updated expertise rating with recalculated level and confidence
     */
    ExpertiseRating calculateRating(UUID ratingId);
    
    /**
     * Adds an assessment to an expertise rating and recalculates the rating.
     *
     * @param ratingId the unique identifier of the rating
     * @param assessmentId the unique identifier of the assessment to add
     * @return the updated expertise rating
     */
    ExpertiseRating addAssessment(UUID ratingId, UUID assessmentId);
    
    /**
     * Removes an assessment from an expertise rating and recalculates the rating.
     *
     * @param ratingId the unique identifier of the rating
     * @param assessmentId the unique identifier of the assessment to remove
     * @return the updated expertise rating
     */
    ExpertiseRating removeAssessment(UUID ratingId, UUID assessmentId);
    
    /**
     * Adds evidence to an expertise rating and recalculates the rating.
     *
     * @param ratingId the unique identifier of the rating
     * @param evidenceId the unique identifier of the evidence to add
     * @return the updated expertise rating
     */
    ExpertiseRating addEvidence(UUID ratingId, UUID evidenceId);
    
    /**
     * Removes evidence from an expertise rating and recalculates the rating.
     *
     * @param ratingId the unique identifier of the rating
     * @param evidenceId the unique identifier of the evidence to remove
     * @return the updated expertise rating
     */
    ExpertiseRating removeEvidence(UUID ratingId, UUID evidenceId);
    
    /**
     * Links a certificate to an expertise rating.
     *
     * @param ratingId the unique identifier of the rating
     * @param certificateId the unique identifier of the certificate
     * @return the updated expertise rating
     */
    ExpertiseRating linkCertificate(UUID ratingId, UUID certificateId);
    
    /**
     * Gets all expertise ratings matching the specified minimum confidence score.
     *
     * @param minConfidence the minimum confidence score (0-1)
     * @return a list of expertise ratings with confidence scores at or above the minimum
     */
    List<ExpertiseRating> getRatingsByMinimumConfidence(BigDecimal minConfidence);
    
    /**
     * Gets all expertise ratings for a specific skill across all profiles.
     *
     * @param skillId the unique identifier of the skill
     * @return a list of expertise ratings for the skill
     */
    List<ExpertiseRating> getRatingsBySkill(UUID skillId);
    
    /**
     * Gets all expertise ratings for a specific domain across all profiles.
     *
     * @param domainId the unique identifier of the domain
     * @return a list of expertise ratings in the domain
     */
    List<ExpertiseRating> getRatingsByDomain(UUID domainId);
}