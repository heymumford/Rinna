package org.rinna.usecase.expertise;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.expertise.Assessment;
import org.rinna.domain.model.expertise.AssessmentContext;
import org.rinna.domain.model.expertise.RelationshipType;
import org.rinna.domain.model.expertise.VerificationType;
import org.rinna.domain.model.expertise.Visibility;

/**
 * Service interface for managing assessments in the Rinna Expertise Rating System.
 * The Assessment Coordinator is responsible for collecting, managing, and validating
 * assessments from various sources.
 */
public interface AssessmentService {
    
    /**
     * Retrieves an assessment by its ID.
     *
     * @param assessmentId the unique identifier of the assessment
     * @return an optional containing the assessment if found, empty otherwise
     */
    Optional<Assessment> getAssessmentById(UUID assessmentId);
    
    /**
     * Retrieves all assessments for a specific expertise rating.
     *
     * @param ratingId the unique identifier of the expertise rating
     * @return a list of assessments for the rating
     */
    List<Assessment> getAssessmentsByRatingId(UUID ratingId);
    
    /**
     * Retrieves all assessments made by a specific assessor.
     *
     * @param assessorId the unique identifier of the assessor
     * @return a list of assessments made by the assessor
     */
    List<Assessment> getAssessmentsByAssessorId(UUID assessorId);
    
    /**
     * Creates a new assessment for a skill.
     *
     * @param ratingId the unique identifier of the expertise rating
     * @param assessorId the unique identifier of the assessor
     * @param rating the rating value (0-10)
     * @param qualitativeReview optional qualitative feedback
     * @param context the context of the assessment
     * @param visibility the visibility of the assessment
     * @param verificationType the verification type for the assessment
     * @return the newly created assessment
     */
    Assessment createAssessment(UUID ratingId, 
                               UUID assessorId, 
                               BigDecimal rating, 
                               String qualitativeReview,
                               AssessmentContext context,
                               Visibility visibility,
                               VerificationType verificationType);
    
    /**
     * Updates an existing assessment.
     *
     * @param assessmentId the unique identifier of the assessment
     * @param rating the new rating value (0-10)
     * @param qualitativeReview the new qualitative feedback
     * @return the updated assessment
     */
    Assessment updateAssessment(UUID assessmentId, BigDecimal rating, String qualitativeReview);
    
    /**
     * Updates the visibility of an assessment.
     *
     * @param assessmentId the unique identifier of the assessment
     * @param visibility the new visibility setting
     * @return the updated assessment
     */
    Assessment updateVisibility(UUID assessmentId, Visibility visibility);
    
    /**
     * Deletes an assessment.
     *
     * @param assessmentId the unique identifier of the assessment
     * @return true if the assessment was deleted, false otherwise
     */
    boolean deleteAssessment(UUID assessmentId);
    
    /**
     * Initiates an assessment request workflow to collect feedback from multiple assessors.
     *
     * @param ratingId the unique identifier of the expertise rating
     * @param assessorIds the list of assessor IDs to request assessments from
     * @param relationshipType the relationship type of the assessors to the profile
     * @return a unique identifier for the assessment request batch
     */
    UUID requestAssessments(UUID ratingId, List<UUID> assessorIds, RelationshipType relationshipType);
    
    /**
     * Retrieves the status of an assessment request batch.
     *
     * @param requestId the unique identifier of the assessment request batch
     * @return a map of assessor IDs to their assessment status
     */
    AssessmentRequestStatus getAssessmentRequestStatus(UUID requestId);
    
    /**
     * Creates a self-assessment for a skill.
     *
     * @param ratingId the unique identifier of the expertise rating
     * @param profileId the unique identifier of the person (self-assessor)
     * @param rating the rating value (0-10)
     * @param qualitativeReview qualitative feedback
     * @return the newly created self-assessment
     */
    Assessment createSelfAssessment(UUID ratingId, UUID profileId, BigDecimal rating, String qualitativeReview);
    
    /**
     * Creates an assessment from objective test results.
     *
     * @param ratingId the unique identifier of the expertise rating
     * @param testResultId the unique identifier of the test result
     * @param rating the calculated rating value (0-10)
     * @param metadata additional metadata about the assessment
     * @return the newly created tool-verified assessment
     */
    Assessment createToolVerifiedAssessment(UUID ratingId, 
                                           UUID testResultId, 
                                           BigDecimal rating,
                                           String summary,
                                           String testType);
    
    /**
     * Gets all assessments for a specific organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of assessments in the organization context
     */
    List<Assessment> getAssessmentsByOrganization(UUID organizationId);
    
    /**
     * Gets all assessments with a specific verification type.
     *
     * @param verificationType the verification type
     * @return a list of assessments with the specified verification type
     */
    List<Assessment> getAssessmentsByVerificationType(VerificationType verificationType);
    
    /**
     * Gets all assessments with a specific relationship type.
     *
     * @param relationshipType the relationship type
     * @return a list of assessments with the specified relationship type
     */
    List<Assessment> getAssessmentsByRelationshipType(RelationshipType relationshipType);
}

/**
 * Represents the status of an assessment request batch.
 */
record AssessmentRequestStatus(
        UUID requestId,
        int totalRequested,
        int completed,
        int pending,
        int declined,
        List<UUID> completedAssessmentIds
) { }