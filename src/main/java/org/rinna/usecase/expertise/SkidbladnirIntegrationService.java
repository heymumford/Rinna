package org.rinna.usecase.expertise;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for integrating with the Skidbladnir package.
 * The Skidbladnir Integration Layer connects with the Skidbladnir package for test result
 * analysis and conversion, integrating test performance metrics as objective evidence.
 */
public interface SkidbladnirIntegrationService {
    
    /**
     * Initializes the Skidbladnir integration.
     *
     * @param sharedFolderPath the path to the shared folder with Skidbladnir
     * @return true if initialization was successful, false otherwise
     */
    boolean initialize(String sharedFolderPath);
    
    /**
     * Processes a test result file and converts it to an assessment.
     *
     * @param profileId the unique identifier of the person
     * @param testResultPath the path to the test result file
     * @return the ID of the created assessment
     */
    UUID processTestResult(UUID profileId, String testResultPath);
    
    /**
     * Processes a batch of test results and converts them to assessments.
     *
     * @param profileId the unique identifier of the person
     * @param testResultPaths the paths to the test result files
     * @return a map of test result paths to assessment IDs
     */
    Map<String, UUID> processTestResultBatch(UUID profileId, List<String> testResultPaths);
    
    /**
     * Maps a test pattern to a skill.
     *
     * @param testPattern a regex pattern matching test names
     * @param skillId the unique identifier of the skill
     * @param domainId the unique identifier of the domain
     * @param weightFactor the weight factor for this mapping
     * @return the unique identifier of the mapping
     */
    UUID createMapping(String testPattern, UUID skillId, UUID domainId, BigDecimal weightFactor);
    
    /**
     * Gets all mappings for a skill.
     *
     * @param skillId the unique identifier of the skill
     * @return a list of test pattern mappings for the skill
     */
    List<TestPatternMapping> getMappingsForSkill(UUID skillId);
    
    /**
     * Updates a test pattern mapping.
     *
     * @param mappingId the unique identifier of the mapping
     * @param testPattern the new test pattern
     * @param skillId the new skill ID
     * @param domainId the new domain ID
     * @param weightFactor the new weight factor
     * @return the updated mapping
     */
    TestPatternMapping updateMapping(UUID mappingId, 
                                    String testPattern, 
                                    UUID skillId, 
                                    UUID domainId, 
                                    BigDecimal weightFactor);
    
    /**
     * Deletes a test pattern mapping.
     *
     * @param mappingId the unique identifier of the mapping
     * @return true if the mapping was deleted, false otherwise
     */
    boolean deleteMapping(UUID mappingId);
    
    /**
     * Gets the current processing status of the Skidbladnir integration.
     *
     * @return the processing status
     */
    ProcessingStatus getProcessingStatus();
    
    /**
     * Configures the transformation pipeline for test results.
     *
     * @param configuration the configuration for the transformation pipeline
     * @return true if configuration was successful, false otherwise
     */
    boolean configureTransformationPipeline(Map<String, Object> configuration);
    
    /**
     * Manually triggers processing of pending test results.
     *
     * @return the number of results processed
     */
    int processPendingResults();
    
    /**
     * Gets the configuration for the Skidbladnir integration.
     *
     * @return the current configuration
     */
    Map<String, Object> getConfiguration();
    
    /**
     * Reprocesses a specific test result.
     *
     * @param testResultId the unique identifier of the test result
     * @return the ID of the created or updated assessment
     */
    UUID reprocessTestResult(UUID testResultId);
}

/**
 * Represents a mapping between a test pattern and a skill.
 */
record TestPatternMapping(
        UUID mappingId,
        String testPattern,
        UUID skillId,
        UUID domainId,
        BigDecimal weightFactor
) { }

/**
 * Represents the processing status of the Skidbladnir integration.
 */
record ProcessingStatus(
        int totalProcessed,
        int pending,
        int failed,
        String lastProcessedFile,
        String lastError,
        boolean isActive
) { }