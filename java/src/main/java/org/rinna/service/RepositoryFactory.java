/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.service;

import org.rinna.adapter.repository.FileBasedFeatureFlagRepository;
import org.rinna.adapter.repository.InMemoryFeatureFlagRepository;
import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.adapter.repository.InMemoryMetadataRepository;
import org.rinna.adapter.repository.InMemoryQueueRepository;
import org.rinna.adapter.repository.InMemoryReleaseRepository;
import org.rinna.adapter.repository.InMemoryTransformationInstanceRepository;
import org.rinna.adapter.repository.InMemoryTransformationTemplateRepository;
import org.rinna.adapter.repository.InMemoryUnifiedWorkItemRepository;
import org.rinna.domain.repository.TransformationInstanceRepository;
import org.rinna.domain.repository.TransformationTemplateRepository;
import org.rinna.domain.repository.UnifiedWorkItemRepository;
import org.rinna.domain.repository.ai.AIFeedbackRepository;
import org.rinna.domain.repository.ai.AIFieldConfidenceRepository;
import org.rinna.domain.repository.ai.AIFieldPriorityRepository;
import org.rinna.domain.repository.ai.AIModelConfigRepository;
import org.rinna.domain.repository.ai.AIPredictionRepository;
import org.rinna.repository.FeatureFlagRepository;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.MetadataRepository;
import org.rinna.repository.QueueRepository;
import org.rinna.repository.ReleaseRepository;

/**
 * Factory for creating repository instances.
 * This centralizes repository creation to ensure consistent use of repositories
 * throughout the application.
 */
public final class RepositoryFactory {
    
    // Singleton instances for in-memory repositories
    private static ItemRepository itemRepository;
    private static MetadataRepository metadataRepository;
    private static QueueRepository queueRepository;
    private static ReleaseRepository releaseRepository;
    private static UnifiedWorkItemRepository unifiedWorkItemRepository;
    private static FeatureFlagRepository featureFlagRepository;
    private static TransformationTemplateRepository transformationTemplateRepository;
    private static TransformationInstanceRepository transformationInstanceRepository;
    
    // AI repositories (managed by AIServiceFactory)
    private static AIPredictionRepository aiPredictionRepository;
    private static AIFeedbackRepository aiFeedbackRepository;
    private static AIFieldPriorityRepository aiFieldPriorityRepository;
    private static AIFieldConfidenceRepository aiFieldConfidenceRepository;
    private static AIModelConfigRepository aiModelConfigRepository;
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RepositoryFactory() {
        // Utility class should not be instantiated
    }
    
    /**
     * Creates or returns an ItemRepository instance.
     *
     * @return the item repository
     */
    public static synchronized ItemRepository createItemRepository() {
        if (itemRepository == null) {
            itemRepository = new InMemoryItemRepository();
        }
        return itemRepository;
    }
    
    /**
     * Creates or returns a MetadataRepository instance.
     *
     * @return the metadata repository
     */
    public static synchronized MetadataRepository createMetadataRepository() {
        if (metadataRepository == null) {
            metadataRepository = new InMemoryMetadataRepository();
        }
        return metadataRepository;
    }
    
    /**
     * Creates or returns a QueueRepository instance.
     *
     * @return the queue repository
     */
    public static synchronized QueueRepository createQueueRepository() {
        if (queueRepository == null) {
            queueRepository = new InMemoryQueueRepository();
        }
        return queueRepository;
    }
    
    /**
     * Creates or returns a ReleaseRepository instance.
     *
     * @return the release repository
     */
    public static synchronized ReleaseRepository createReleaseRepository() {
        if (releaseRepository == null) {
            releaseRepository = new InMemoryReleaseRepository();
        }
        return releaseRepository;
    }
    
    /**
     * Gets the default instance of UnifiedWorkItemRepository.
     * 
     * @return The default instance
     */
    public static synchronized UnifiedWorkItemRepository getUnifiedWorkItemRepository() {
        if (unifiedWorkItemRepository == null) {
            unifiedWorkItemRepository = new InMemoryUnifiedWorkItemRepository();
        }
        
        return unifiedWorkItemRepository;
    }
    
    /**
     * Sets the default instance of UnifiedWorkItemRepository.
     * 
     * @param repository The repository to set as default
     */
    public static synchronized void setUnifiedWorkItemRepository(UnifiedWorkItemRepository repository) {
        unifiedWorkItemRepository = repository;
    }
    
    /**
     * Creates or returns a FeatureFlagRepository instance.
     * By default, this uses a file-based repository to persist feature flags.
     *
     * @return the feature flag repository
     */
    public static synchronized FeatureFlagRepository getFeatureFlagRepository() {
        if (featureFlagRepository == null) {
            // Use file-based repository for persistence
            featureFlagRepository = new FileBasedFeatureFlagRepository();
        }
        
        return featureFlagRepository;
    }
    
    /**
     * Sets the default instance of FeatureFlagRepository.
     * 
     * @param repository The repository to set as default
     */
    public static synchronized void setFeatureFlagRepository(FeatureFlagRepository repository) {
        featureFlagRepository = repository;
    }
    
    /**
     * Creates an in-memory FeatureFlagRepository instance.
     * This is primarily used for testing.
     *
     * @return the in-memory feature flag repository
     */
    public static synchronized FeatureFlagRepository createInMemoryFeatureFlagRepository() {
        return new InMemoryFeatureFlagRepository();
    }
    
    /**
     * Gets the default instance of TransformationTemplateRepository.
     * 
     * @return The default instance
     */
    public static synchronized TransformationTemplateRepository getTransformationTemplateRepository() {
        if (transformationTemplateRepository == null) {
            transformationTemplateRepository = new InMemoryTransformationTemplateRepository();
        }
        
        return transformationTemplateRepository;
    }
    
    /**
     * Sets the default instance of TransformationTemplateRepository.
     * 
     * @param repository The repository to set as default
     */
    public static synchronized void setTransformationTemplateRepository(TransformationTemplateRepository repository) {
        transformationTemplateRepository = repository;
    }
    
    /**
     * Gets the default instance of TransformationInstanceRepository.
     * 
     * @return The default instance
     */
    public static synchronized TransformationInstanceRepository getTransformationInstanceRepository() {
        if (transformationInstanceRepository == null) {
            transformationInstanceRepository = new InMemoryTransformationInstanceRepository();
        }
        
        return transformationInstanceRepository;
    }
    
    /**
     * Sets the default instance of TransformationInstanceRepository.
     * 
     * @param repository The repository to set as default
     */
    public static synchronized void setTransformationInstanceRepository(TransformationInstanceRepository repository) {
        transformationInstanceRepository = repository;
    }
    
    /**
     * Gets the AI prediction repository from AIServiceFactory.
     * 
     * @return The AI prediction repository
     */
    public static synchronized AIPredictionRepository getAIPredictionRepository() {
        if (aiPredictionRepository == null) {
            aiPredictionRepository = AIServiceFactory.getPredictionRepository();
        }
        return aiPredictionRepository;
    }
    
    /**
     * Gets the AI feedback repository from AIServiceFactory.
     * 
     * @return The AI feedback repository
     */
    public static synchronized AIFeedbackRepository getAIFeedbackRepository() {
        if (aiFeedbackRepository == null) {
            aiFeedbackRepository = AIServiceFactory.getFeedbackRepository();
        }
        return aiFeedbackRepository;
    }
    
    /**
     * Gets the AI field priority repository from AIServiceFactory.
     * 
     * @return The AI field priority repository
     */
    public static synchronized AIFieldPriorityRepository getAIFieldPriorityRepository() {
        if (aiFieldPriorityRepository == null) {
            aiFieldPriorityRepository = AIServiceFactory.getFieldPriorityRepository();
        }
        return aiFieldPriorityRepository;
    }
    
    /**
     * Gets the AI field confidence repository from AIServiceFactory.
     * 
     * @return The AI field confidence repository
     */
    public static synchronized AIFieldConfidenceRepository getAIFieldConfidenceRepository() {
        if (aiFieldConfidenceRepository == null) {
            aiFieldConfidenceRepository = AIServiceFactory.getFieldConfidenceRepository();
        }
        return aiFieldConfidenceRepository;
    }
    
    /**
     * Gets the AI model config repository from AIServiceFactory.
     * 
     * @return The AI model config repository
     */
    public static synchronized AIModelConfigRepository getAIModelConfigRepository() {
        if (aiModelConfigRepository == null) {
            aiModelConfigRepository = AIServiceFactory.getModelConfigRepository();
        }
        return aiModelConfigRepository;
    }
    
    /**
     * Initializes all repositories, including AI repositories.
     */
    public static synchronized void initializeAll() {
        // Initialize AI services and repositories
        AIServiceFactory.initialize();
    }
}