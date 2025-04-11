package org.rinna.usecase.expertise;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.expertise.Certificate;

/**
 * Service interface for managing certificates in the Rinna Expertise Rating System.
 * The Certificate Authority is responsible for issuing, verifying, and managing the
 * lifecycle of expertise certificates.
 */
public interface CertificateService {
    
    /**
     * Retrieves a certificate by its ID.
     *
     * @param certificateId the unique identifier of the certificate
     * @return an optional containing the certificate if found, empty otherwise
     */
    Optional<Certificate> getCertificateById(UUID certificateId);
    
    /**
     * Retrieves all certificates for a person.
     *
     * @param profileId the unique identifier of the person
     * @return a list of certificates for the person
     */
    List<Certificate> getCertificatesByProfileId(UUID profileId);
    
    /**
     * Retrieves a person's certificate for a specific skill.
     *
     * @param profileId the unique identifier of the person
     * @param skillId the unique identifier of the skill
     * @return an optional containing the certificate if found, empty otherwise
     */
    Optional<Certificate> getCertificateForSkill(UUID profileId, UUID skillId);
    
    /**
     * Issues a new certificate for a skill.
     *
     * @param profileId the unique identifier of the person
     * @param skillId the unique identifier of the skill
     * @param level the certified expertise level (0-10)
     * @param issuerOrganizationId the unique identifier of the issuing organization
     * @param expirationDate optional expiration date for the certificate
     * @param renewalRequirements optional requirements for renewing the certificate
     * @param evidenceIds list of evidence IDs supporting the certificate
     * @return the newly issued certificate
     */
    Certificate issueCertificate(UUID profileId, 
                               UUID skillId, 
                               BigDecimal level, 
                               UUID issuerOrganizationId,
                               LocalDateTime expirationDate,
                               String renewalRequirements,
                               List<UUID> evidenceIds);
    
    /**
     * Verifies a certificate using its blockchain hash.
     *
     * @param certificateId the unique identifier of the certificate
     * @return true if the certificate is valid, false otherwise
     */
    boolean verifyCertificate(UUID certificateId);
    
    /**
     * Verifies a certificate using its blockchain hash and verification URI.
     *
     * @param blockchainHash the blockchain hash of the certificate
     * @param verificationUri the verification URI for the certificate
     * @return true if the certificate is valid, false otherwise
     */
    boolean verifyCertificateByHash(String blockchainHash, String verificationUri);
    
    /**
     * Revokes a certificate.
     *
     * @param certificateId the unique identifier of the certificate
     * @param reason the reason for revocation
     * @return the revoked certificate
     */
    Certificate revokeCertificate(UUID certificateId, String reason);
    
    /**
     * Renews a certificate with a new expiration date.
     *
     * @param certificateId the unique identifier of the certificate
     * @param newExpirationDate the new expiration date
     * @return the renewed certificate
     */
    Certificate renewCertificate(UUID certificateId, LocalDateTime newExpirationDate);
    
    /**
     * Updates a certificate with a new level.
     *
     * @param certificateId the unique identifier of the certificate
     * @param newLevel the new expertise level (0-10)
     * @return the updated certificate
     */
    Certificate updateCertificateLevel(UUID certificateId, BigDecimal newLevel);
    
    /**
     * Gets all certificates issued by a specific organization.
     *
     * @param issuerOrganizationId the unique identifier of the issuing organization
     * @return a list of certificates issued by the organization
     */
    List<Certificate> getCertificatesByIssuer(UUID issuerOrganizationId);
    
    /**
     * Gets all certificates for a specific skill across all profiles.
     *
     * @param skillId the unique identifier of the skill
     * @return a list of certificates for the skill
     */
    List<Certificate> getCertificatesBySkill(UUID skillId);
    
    /**
     * Gets all certificates that are expired or will expire by the given date.
     *
     * @param expirationDate the date by which certificates will expire
     * @return a list of certificates that are expired or will expire
     */
    List<Certificate> getExpiredCertificates(LocalDateTime expirationDate);
    
    /**
     * Generates a blockchain hash for a certificate.
     *
     * @param certificate the certificate to hash
     * @return the blockchain hash
     */
    String generateBlockchainHash(Certificate certificate);
    
    /**
     * Registers a certificate hash on the blockchain.
     *
     * @param certificateId the unique identifier of the certificate
     * @param hash the hash to register
     * @return the transaction ID or other confirmation
     */
    String registerOnBlockchain(UUID certificateId, String hash);
    
    /**
     * Generates a verification URI for a certificate.
     *
     * @param certificateId the unique identifier of the certificate
     * @return the verification URI
     */
    String generateVerificationUri(UUID certificateId);
}