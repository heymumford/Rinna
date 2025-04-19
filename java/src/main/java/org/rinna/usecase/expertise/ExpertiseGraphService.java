package org.rinna.usecase.expertise;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing the expertise graph in the Rinna Expertise Rating System.
 * The Expertise Graph maintains a network representation of expertise connections,
 * visualizes expertise relationships, identifies gaps, and generates recommendations.
 */
public interface ExpertiseGraphService {
    
    /**
     * Gets expertise connections for a person.
     * This returns other people who have assessed or been assessed by the specified person.
     *
     * @param profileId the unique identifier of the person
     * @return a list of expertise connections
     */
    List<ExpertiseConnection> getExpertiseConnections(UUID profileId);
    
    /**
     * Gets expertise connections within an organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a map of profile IDs to their expertise connections
     */
    Map<UUID, List<ExpertiseConnection>> getOrganizationExpertiseGraph(UUID organizationId);
    
    /**
     * Gets top experts for a skill.
     *
     * @param skillId the unique identifier of the skill
     * @param limit the maximum number of experts to return
     * @return a list of profile IDs of the top experts, ordered by expertise level
     */
    List<UUID> getTopExpertsForSkill(UUID skillId, int limit);
    
    /**
     * Gets top experts for a domain.
     *
     * @param domainId the unique identifier of the domain
     * @param limit the maximum number of experts to return
     * @return a list of profile IDs of the top experts, ordered by expertise level
     */
    List<UUID> getTopExpertsForDomain(UUID domainId, int limit);
    
    /**
     * Identifies expertise gaps in an organization.
     * This method analyzes the expertise distribution across an organization
     * and identifies skills or domains where expertise is lacking.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of expertise gaps
     */
    List<ExpertiseGap> identifyExpertiseGaps(UUID organizationId);
    
    /**
     * Generates expertise recommendations for a person.
     * This method analyzes a person's current expertise and generates
     * recommendations for skills they might want to develop.
     *
     * @param profileId the unique identifier of the person
     * @return a list of expertise recommendations
     */
    List<ExpertiseRecommendation> generateRecommendations(UUID profileId);
    
    /**
     * Calculates expertise diversity for an organization.
     * This method analyzes the distribution of expertise across different
     * domains within an organization and returns diversity metrics.
     *
     * @param organizationId the unique identifier of the organization
     * @return expertise diversity metrics
     */
    ExpertiseDiversity calculateExpertiseDiversity(UUID organizationId);
    
    /**
     * Finds people with similar expertise profiles.
     *
     * @param profileId the unique identifier of the person
     * @param limit the maximum number of similar profiles to return
     * @return a list of similar profiles with similarity scores
     */
    List<SimilarProfile> findSimilarProfiles(UUID profileId, int limit);
    
    /**
     * Generates a visual representation of the expertise graph.
     *
     * @param organizationId the unique identifier of the organization
     * @param format the format of the visualization (e.g., "json", "svg", "dot")
     * @return the graph visualization in the specified format
     */
    String generateGraphVisualization(UUID organizationId, String format);
    
    /**
     * Identifies mentorship opportunities based on expertise gaps and connections.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of mentorship opportunities
     */
    List<MentorshipOpportunity> identifyMentorshipOpportunities(UUID organizationId);
    
    /**
     * Analyzes the expertise network to identify key connectors.
     * These are people who connect different expertise domains within an organization.
     *
     * @param organizationId the unique identifier of the organization
     * @return a list of key connectors with their centrality scores
     */
    List<KeyConnector> identifyKeyConnectors(UUID organizationId);
}

/**
 * Represents a connection between two people in the expertise graph.
 */
record ExpertiseConnection(
        UUID sourceProfileId,
        UUID targetProfileId,
        List<UUID> sharedSkillIds,
        int assessmentCount,
        double averageRating,
        ConnectionType connectionType
) { }

/**
 * The type of connection between two people.
 */
enum ConnectionType {
    ASSESSOR_TO_ASSESSEE,
    ASSESSEE_TO_ASSESSOR,
    MUTUAL,
    MENTOR_TO_MENTEE,
    MENTEE_TO_MENTOR
}

/**
 * Represents an expertise gap in an organization.
 */
record ExpertiseGap(
        UUID skillId,
        String skillName,
        UUID domainId,
        String domainName,
        int expertCount,
        double averageExpertiseLevel,
        double requiredCoverage,
        double actualCoverage,
        GapSeverity severity
) { }

/**
 * The severity of an expertise gap.
 */
enum GapSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Represents an expertise recommendation for a person.
 */
record ExpertiseRecommendation(
        UUID skillId,
        String skillName,
        UUID domainId,
        String domainName,
        double relevanceScore,
        String reason,
        List<UUID> relatedExistingSkillIds
) { }

/**
 * Represents expertise diversity metrics for an organization.
 */
record ExpertiseDiversity(
        double domainCoverageScore,
        Map<UUID, Double> domainDistribution,
        double skillDepthScore,
        double skillBreadthScore,
        double overallDiversityScore,
        List<UUID> underrepresentedDomainIds,
        List<UUID> overrepresentedDomainIds
) { }

/**
 * Represents a profile with similar expertise.
 */
record SimilarProfile(
        UUID profileId,
        String name,
        double similarityScore,
        List<UUID> commonSkillIds,
        List<UUID> complementarySkillIds
) { }

/**
 * Represents a mentorship opportunity.
 */
record MentorshipOpportunity(
        UUID mentorProfileId,
        String mentorName,
        UUID menteeProfileId,
        String menteeName,
        List<UUID> skillIds,
        double compatibilityScore,
        String rationale
) { }

/**
 * Represents a key connector in the expertise network.
 */
record KeyConnector(
        UUID profileId,
        String name,
        double centralityScore,
        int connectionCount,
        List<UUID> bridgedDomainIds,
        Map<String, Double> domainExpertiseSummary
) { }