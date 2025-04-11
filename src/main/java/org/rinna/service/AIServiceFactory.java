/*
 * Factory for AI services - Lightweight local implementation
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rinna.adapter.repository.ai.InMemoryAIFeedbackRepository;
import org.rinna.adapter.repository.ai.InMemoryAIFieldConfidenceRepository;
import org.rinna.adapter.repository.ai.InMemoryAIFieldPriorityRepository;
import org.rinna.adapter.repository.ai.InMemoryAIModelConfigRepository;
import org.rinna.adapter.repository.ai.InMemoryAIPredictionRepository;
import org.rinna.adapter.service.ai.DefaultAIModelManagementService;
import org.rinna.adapter.service.ai.DefaultAISmartFieldService;
import org.rinna.domain.model.ai.AIModelConfig;
import org.rinna.domain.repository.ai.AIFeedbackRepository;
import org.rinna.domain.repository.ai.AIFieldConfidenceRepository;
import org.rinna.domain.repository.ai.AIFieldPriorityRepository;
import org.rinna.domain.repository.ai.AIModelConfigRepository;
import org.rinna.domain.repository.ai.AIPredictionRepository;
import org.rinna.usecase.ai.AIModelManagementService;
import org.rinna.usecase.ai.AISmartFieldService;

/**
 * Factory for creating and accessing AI services.
 * 
 * This implementation provides a lightweight, totally local AI service with minimal footprint:
 * - No external API calls or network dependencies
 * - No large model weights or GPU requirements
 * - Pattern-based prediction rather than deep learning
 * - In-memory storage with minimal resource consumption
 * - Domain-specific knowledge of Ryorin-Do principles
 */
public final class AIServiceFactory {
    
    private static final Logger LOGGER = Logger.getLogger(AIServiceFactory.class.getName());
    
    private static final AIPredictionRepository PREDICTION_REPOSITORY = new InMemoryAIPredictionRepository();
    private static final AIFeedbackRepository FEEDBACK_REPOSITORY = new InMemoryAIFeedbackRepository();
    private static final AIFieldPriorityRepository FIELD_PRIORITY_REPOSITORY = new InMemoryAIFieldPriorityRepository();
    private static final AIFieldConfidenceRepository FIELD_CONFIDENCE_REPOSITORY = new InMemoryAIFieldConfidenceRepository();
    private static final AIModelConfigRepository MODEL_CONFIG_REPOSITORY = new InMemoryAIModelConfigRepository();
    
    private static final AISmartFieldService SMART_FIELD_SERVICE = new DefaultAISmartFieldService(
            PREDICTION_REPOSITORY,
            FEEDBACK_REPOSITORY,
            FIELD_PRIORITY_REPOSITORY,
            FIELD_CONFIDENCE_REPOSITORY,
            MODEL_CONFIG_REPOSITORY);
    
    private static final AIModelManagementService MODEL_MANAGEMENT_SERVICE = new DefaultAIModelManagementService(
            MODEL_CONFIG_REPOSITORY,
            FIELD_CONFIDENCE_REPOSITORY);
    
    // Tracks whether the Ryorin-Do knowledge has been loaded
    private static boolean isRyorinDoKnowledgeLoaded = false;
    
    // Maximum memory usage in MB (very conservative limit)
    private static final int MAX_MEMORY_USAGE_MB = 50;
    
    private AIServiceFactory() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Gets the AI smart field service.
     *
     * @return The AI smart field service
     */
    public static AISmartFieldService getSmartFieldService() {
        ensureInitialized();
        return SMART_FIELD_SERVICE;
    }
    
    /**
     * Gets the AI model management service.
     *
     * @return The AI model management service
     */
    public static AIModelManagementService getModelManagementService() {
        ensureInitialized();
        return MODEL_MANAGEMENT_SERVICE;
    }
    
    /**
     * Gets the prediction repository.
     *
     * @return The prediction repository
     */
    public static AIPredictionRepository getPredictionRepository() {
        return PREDICTION_REPOSITORY;
    }
    
    /**
     * Gets the feedback repository.
     *
     * @return The feedback repository
     */
    public static AIFeedbackRepository getFeedbackRepository() {
        return FEEDBACK_REPOSITORY;
    }
    
    /**
     * Gets the field priority repository.
     *
     * @return The field priority repository
     */
    public static AIFieldPriorityRepository getFieldPriorityRepository() {
        return FIELD_PRIORITY_REPOSITORY;
    }
    
    /**
     * Gets the field confidence repository.
     *
     * @return The field confidence repository
     */
    public static AIFieldConfidenceRepository getFieldConfidenceRepository() {
        return FIELD_CONFIDENCE_REPOSITORY;
    }
    
    /**
     * Gets the model configuration repository.
     *
     * @return The model configuration repository
     */
    public static AIModelConfigRepository getModelConfigRepository() {
        return MODEL_CONFIG_REPOSITORY;
    }
    
    /**
     * Ensures the AI services are initialized before use.
     */
    private static void ensureInitialized() {
        if (MODEL_CONFIG_REPOSITORY.findAll().isEmpty()) {
            initialize();
        }
    }
    
    /**
     * Initializes the AI services with lightweight models and Ryorin-Do knowledge.
     */
    public static void initialize() {
        // Check current memory usage
        long usedMemoryMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        LOGGER.info("Current memory usage before AI initialization: " + usedMemoryMB + "MB");
        
        if (usedMemoryMB > MAX_MEMORY_USAGE_MB) {
            LOGGER.warning("Memory usage exceeds threshold. Using minimal AI configuration.");
            initializeMinimalModel();
        } else {
            initializeStandardModels();
            loadRyorinDoKnowledge();
        }
        
        // Log memory usage after initialization
        usedMemoryMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        LOGGER.info("Memory usage after AI initialization: " + usedMemoryMB + "MB");
    }
    
    /**
     * Initializes a minimal model configuration when resources are constrained.
     */
    private static void initializeMinimalModel() {
        // Register a single lightweight model for all fields
        modelManagementService.registerModel(
                "minimal-model",
                "Minimal Prediction Model",
                "1.0.0",
                "MINIMAL",
                List.of("status", "priority", "assignee", "cynefinDomain", "category", 
                       "workParadigm", "cognitiveLoad", "completionPercentage", "outcome", "tags"),
                Map.of(
                        "confidenceThreshold", 0.5,
                        "maxPredictions", 3,
                        "lowResourceMode", true
                )
        );
    }
    
    /**
     * Initializes the standard model configuration.
     */
    private static void initializeStandardModels() {
        // Register base model for common fields
        modelManagementService.registerModel(
                "default-model",
                "Default Prediction Model",
                "1.0.0",
                "DEFAULT",
                List.of("status", "priority", "assignee", "dueDate", "estimatedEffort", "tags"),
                Map.of(
                        "confidenceThreshold", 0.6,
                        "maxPredictions", 5,
                        "memoryFootprintMB", 10
                )
        );
        
        // Register model for Cynefin and Work Classification fields
        modelManagementService.registerModel(
                "classification-model",
                "Work Classification Model",
                "1.0.0",
                "CLASSIFICATION",
                List.of("cynefinDomain", "category", "workParadigm", "cognitiveLoad"),
                Map.of(
                        "confidenceThreshold", 0.7,
                        "contextualAnalysis", true,
                        "textAnalysisWeight", 0.8,
                        "memoryFootprintMB", 15
                )
        );
        
        // Register model for outcome and completion fields
        modelManagementService.registerModel(
                "progress-model",
                "Work Progress & Outcome Model",
                "1.0.0",
                "PROGRESS",
                List.of("completionPercentage", "outcome"),
                Map.of(
                        "confidenceThreshold", 0.6,
                        "historyAnalysis", true,
                        "statusBasedPrediction", true,
                        "memoryFootprintMB", 8
                )
        );
        
        // Register Ryorin-Do specific model with embedded domain knowledge
        modelManagementService.registerModel(
                "ryorindo-model",
                "Ryorin-Do Knowledge Model",
                "1.0.0",
                "DOMAIN_SPECIFIC",
                List.of("cynefinDomain", "workParadigm", "cognitiveLoad", "category", "outcome"),
                Map.of(
                        "confidenceThreshold", 0.8,
                        "domainKnowledge", "ryorindo",
                        "memoryFootprintMB", 12
                )
        );
    }
    
    /**
     * Loads Ryorin-Do knowledge from documentation files into the AI service.
     * This enables the AI to make predictions based on Ryorin-Do principles.
     */
    private static void loadRyorinDoKnowledge() {
        if (isRyorinDoKnowledgeLoaded) {
            return;
        }
        
        try {
            // Load core Ryorin-Do documentation
            Path ryorinDoPath = Paths.get("docs/ryorin-do-RDSITWM1.2-a-standard-for-information-technology-work-management.md");
            String ryorinDoContent = "";
            
            if (Files.exists(ryorinDoPath)) {
                ryorinDoContent = Files.readString(ryorinDoPath);
                LOGGER.info("Loaded Ryorin-Do knowledge from main documentation file");
            } else {
                LOGGER.warning("Ryorin-Do main documentation file not found, using built-in knowledge");
            }
            
            // Extract key concepts for the AI model to use in predictions
            Map<String, List<String>> ryorinDoKnowledgeMap = extractRyorinDoKnowledge(ryorinDoContent);
            
            // Update model parameters with the knowledge
            AIModelConfig ryorinDoModel = MODEL_CONFIG_REPOSITORY.findById("ryorindo-model").orElse(null);
            if (ryorinDoModel != null) {
                Map<String, Object> updatedParams = new HashMap<>(ryorinDoModel.parameters());
                updatedParams.put("cynefinDomains", ryorinDoKnowledgeMap.get("cynefinDomains"));
                updatedParams.put("workParadigms", ryorinDoKnowledgeMap.get("workParadigms"));
                updatedParams.put("cognitiveFactors", ryorinDoKnowledgeMap.get("cognitiveFactors"));
                
                MODEL_MANAGEMENT_SERVICE.updateModelParameters("ryorindo-model", updatedParams);
                LOGGER.info("Updated Ryorin-Do model with extracted knowledge");
            }
            
            isRyorinDoKnowledgeLoaded = true;
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading Ryorin-Do knowledge", e);
        }
    }
    
    /**
     * Extracts key Ryorin-Do concepts from documentation text.
     * 
     * @param content The documentation content
     * @return A map of key Ryorin-Do concepts organized by category
     */
    private static Map<String, List<String>> extractRyorinDoKnowledge(String content) {
        Map<String, List<String>> knowledge = new HashMap<>();
        
        // Default cynefin domains from Ryorin-Do standard
        knowledge.put("cynefinDomains", List.of(
            "CLEAR:Cause and effect are obvious; best practices apply",
            "COMPLICATED:Known unknowns; requires analysis and expertise",
            "COMPLEX:Unknown unknowns; cause and effect in retrospect",
            "CHAOTIC:No clear cause and effect; needs immediate action"
        ));
        
        // Work paradigms from Ryorin-Do
        knowledge.put("workParadigms", List.of(
            "PROJECT:Defined deliverables with start and end dates",
            "OPERATIONAL:Ongoing maintenance and support activities",
            "EXPLORATORY:Research and innovation with uncertain outcomes",
            "GOVERNANCE:Compliance and regulatory activities"
        ));
        
        // Cognitive factors for workload assessment
        knowledge.put("cognitiveFactors", List.of(
            "WORKING_MEMORY:Limited capacity of around four items",
            "ATTENTION_SPAN:Decreasing in digital age due to distractions",
            "INFORMATION_OVERLOAD:Exceeding processing capacity leads to stress",
            "COGNITIVE_BIAS:Systematic patterns of deviation from rationality"
        ));
        
        return knowledge;
    }
    
    /**
     * Resets the AI service state - primarily used for testing.
     */
    public static void reset() {
        PREDICTION_REPOSITORY.clear();
        FEEDBACK_REPOSITORY.clear();
        FIELD_PRIORITY_REPOSITORY.clear();
        FIELD_CONFIDENCE_REPOSITORY.clear();
        MODEL_CONFIG_REPOSITORY.clear();
        isRyorinDoKnowledgeLoaded = false;
    }
}