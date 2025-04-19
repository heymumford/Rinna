package org.rinna.service;

import org.rinna.adapter.repository.InMemoryVocabularyMapRepository;
import org.rinna.adapter.service.DefaultVocabularyMapService;
import org.rinna.domain.repository.VocabularyMapRepository;
import org.rinna.usecase.VocabularyMapService;

/**
 * Factory for creating VocabularyMapService instances.
 */
public final class VocabularyMapServiceFactory {
    private static VocabularyMapService instance;
    private static VocabularyMapRepository repository;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private VocabularyMapServiceFactory() {
        // This constructor is intentionally empty
    }
    
    /**
     * Gets a singleton instance of the VocabularyMapService.
     * 
     * @return the VocabularyMapService instance
     */
    public static synchronized VocabularyMapService getInstance() {
        if (instance == null) {
            if (repository == null) {
                repository = new InMemoryVocabularyMapRepository();
            }
            instance = new DefaultVocabularyMapService(repository);
        }
        return instance;
    }
    
    /**
     * Sets the repository to use for creating VocabularyMapService instances.
     * 
     * @param vocabularyMapRepository the repository to use
     */
    public static void setRepository(VocabularyMapRepository vocabularyMapRepository) {
        repository = vocabularyMapRepository;
        instance = null; // Reset the instance to force recreation with the new repository
    }
    
    /**
     * Resets the singleton instance.
     */
    public static void reset() {
        instance = null;
        repository = null;
    }
}