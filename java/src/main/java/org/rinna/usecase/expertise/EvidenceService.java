package org.rinna.usecase.expertise;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.expertise.Evidence;
import org.rinna.domain.model.expertise.EvidenceType;
import org.rinna.domain.model.expertise.VerificationMethod;
import org.rinna.domain.model.expertise.VerificationStatus;
import org.rinna.domain.model.expertise.Visibility;

/**
 * Service interface for managing evidence in the Rinna Expertise Rating System.
 * The Evidence Manager is responsible for collecting, verifying, and managing
 * evidence supporting expertise claims.
 */
public interface EvidenceService {
    
    /**
     * Retrieves evidence by its ID.
     *
     * @param evidenceId the unique identifier of the evidence
     * @return an optional containing the evidence if found, empty otherwise
     */
    Optional<Evidence> getEvidenceById(UUID evidenceId);
    
    /**
     * Retrieves all evidence for a person.
     *
     * @param profileId the unique identifier of the person
     * @return a list of evidence for the person
     */
    List<Evidence> getEvidenceByProfileId(UUID profileId);
    
    /**
     * Retrieves all evidence for a person's specific skill.
     *
     * @param profileId the unique identifier of the person
     * @param skillId the unique identifier of the skill
     * @return a list of evidence for the person's skill
     */
    List<Evidence> getEvidenceForSkill(UUID profileId, UUID skillId);
    
    /**
     * Creates new evidence for a skill.
     *
     * @param profileId the unique identifier of the person
     * @param skillId the unique identifier of the skill
     * @param type the type of evidence
     * @param title the title of the evidence
     * @param description the description of the evidence
     * @param url optional URL for the evidence
     * @param visibility the visibility of the evidence
     * @param metadata additional metadata about the evidence
     * @return the newly created evidence
     */
    Evidence createEvidence(UUID profileId, 
                          UUID skillId, 
                          EvidenceType type, 
                          String title,
                          String description,
                          String url,
                          Visibility visibility,
                          Map<String, Object> metadata);
    
    /**
     * Updates existing evidence.
     *
     * @param evidenceId the unique identifier of the evidence
     * @param title the new title
     * @param description the new description
     * @param url the new URL
     * @param visibility the new visibility
     * @return the updated evidence
     */
    Evidence updateEvidence(UUID evidenceId, 
                          String title,
                          String description,
                          String url,
                          Visibility visibility);
    
    /**
     * Verifies evidence using the specified method.
     *
     * @param evidenceId the unique identifier of the evidence
     * @param verificationMethod the method used for verification
     * @param verifierId the unique identifier of the verifier
     * @return the verified evidence
     */
    Evidence verifyEvidence(UUID evidenceId, 
                          VerificationMethod verificationMethod,
                          UUID verifierId);
    
    /**
     * Rejects evidence with a reason.
     *
     * @param evidenceId the unique identifier of the evidence
     * @param reason the reason for rejection
     * @param verifierId the unique identifier of the verifier
     * @return the rejected evidence
     */
    Evidence rejectEvidence(UUID evidenceId, String reason, UUID verifierId);
    
    /**
     * Deletes evidence.
     *
     * @param evidenceId the unique identifier of the evidence
     * @return true if the evidence was deleted, false otherwise
     */
    boolean deleteEvidence(UUID evidenceId);
    
    /**
     * Links evidence to a certificate.
     *
     * @param evidenceId the unique identifier of the evidence
     * @param certificateId the unique identifier of the certificate
     * @return the updated evidence
     */
    Evidence linkToCertificate(UUID evidenceId, UUID certificateId);
    
    /**
     * Gets all evidence of a specific type.
     *
     * @param type the type of evidence
     * @return a list of evidence of the specified type
     */
    List<Evidence> getEvidenceByType(EvidenceType type);
    
    /**
     * Gets all evidence with a specific verification status.
     *
     * @param status the verification status
     * @return a list of evidence with the specified status
     */
    List<Evidence> getEvidenceByVerificationStatus(VerificationStatus status);
    
    /**
     * Gets all evidence verified using a specific method.
     *
     * @param method the verification method
     * @return a list of evidence verified using the specified method
     */
    List<Evidence> getEvidenceByVerificationMethod(VerificationMethod method);
    
    /**
     * Imports evidence from an external system.
     *
     * @param profileId the unique identifier of the person
     * @param skillId the unique identifier of the skill
     * @param systemName the name of the external system
     * @param externalId the ID of the evidence in the external system
     * @param type the type of evidence
     * @return the imported evidence
     */
    Evidence importExternalEvidence(UUID profileId, 
                                  UUID skillId,
                                  String systemName,
                                  String externalId,
                                  EvidenceType type);
    
    /**
     * Gets all pending evidence that needs verification.
     *
     * @return a list of pending evidence
     */
    List<Evidence> getPendingEvidence();
    
    /**
     * Calculates the strength of evidence for a skill.
     * This method evaluates all evidence for a skill and returns a score
     * representing the overall strength of the evidence.
     *
     * @param profileId the unique identifier of the person
     * @param skillId the unique identifier of the skill
     * @return a score between 0 and 1 representing evidence strength
     */
    double calculateEvidenceStrength(UUID profileId, UUID skillId);
}