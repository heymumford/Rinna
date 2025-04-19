/*
 * Default implementation of AIModelManagementService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.rinna.domain.model.ai.AIFieldConfidence;
import org.rinna.domain.model.ai.AIModelConfig;
import org.rinna.domain.repository.ai.AIFieldConfidenceRepository;
import org.rinna.domain.repository.ai.AIModelConfigRepository;
import org.rinna.usecase.ai.AIModelManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of AIModelManagementService.
 */
public class DefaultAIModelManagementService implements AIModelManagementService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAIModelManagementService.class);
    
    private final AIModelConfigRepository modelConfigRepository;
    private final AIFieldConfidenceRepository fieldConfidenceRepository;
    
    /**
     * Constructor.
     *
     * @param modelConfigRepository The model configuration repository
     * @param fieldConfidenceRepository The field confidence repository
     */
    public DefaultAIModelManagementService(
            AIModelConfigRepository modelConfigRepository,
            AIFieldConfidenceRepository fieldConfidenceRepository) {
        this.modelConfigRepository = modelConfigRepository;
        this.fieldConfidenceRepository = fieldConfidenceRepository;
    }

    @Override
    public AIModelConfig registerModel(
            String modelId,
            String modelName,
            String modelVersion,
            String modelType,
            List<String> supportedFields,
            Map<String, Object> parameters) {
        logger.info("Registering model {} ({})", modelName, modelId);
        
        // Check if model already exists
        Optional<AIModelConfig> existingModel = modelConfigRepository.findByModelId(modelId);
        if (existingModel.isPresent()) {
            throw new IllegalArgumentException("Model with ID " + modelId + " already exists");
        }
        
        // Create model config
        AIModelConfig modelConfig = AIModelConfig.builder()
                .modelId(modelId)
                .modelName(modelName)
                .modelVersion(modelVersion)
                .modelType(modelType)
                .supportedFields(supportedFields)
                .parameters(parameters)
                .enabled(true)
                .build();
        
        // Save model config
        AIModelConfig savedConfig = modelConfigRepository.save(modelConfig);
        
        // Initialize confidence records for each supported field
        for (String fieldName : supportedFields) {
            AIFieldConfidence confidence = AIFieldConfidence.builder()
                    .fieldName(fieldName)
                    .modelId(modelId)
                    .build();
            
            fieldConfidenceRepository.save(confidence);
        }
        
        return savedConfig;
    }

    @Override
    public Optional<AIModelConfig> getModelConfig(String modelId) {
        return modelConfigRepository.findByModelId(modelId);
    }

    @Override
    public List<AIModelConfig> getAllModelConfigs() {
        return modelConfigRepository.findAll();
    }

    @Override
    public List<AIModelConfig> getEnabledModelConfigs() {
        return modelConfigRepository.findAllEnabled();
    }

    @Override
    public Optional<AIModelConfig> setModelEnabled(String modelId, boolean enabled) {
        logger.info("{} model {} ", enabled ? "Enabling" : "Disabling", modelId);
        return modelConfigRepository.updateEnabledStatus(modelId, enabled);
    }

    @Override
    public Optional<AIModelConfig> updateModelParameters(String modelId, Map<String, Object> parameters) {
        logger.info("Updating parameters for model {}", modelId);
        return modelConfigRepository.updateParameters(modelId, parameters);
    }

    @Override
    public Optional<AIModelConfig> updateSupportedFields(String modelId, List<String> supportedFields) {
        logger.info("Updating supported fields for model {}", modelId);
        
        // Get current model config
        Optional<AIModelConfig> modelOpt = modelConfigRepository.findByModelId(modelId);
        if (modelOpt.isEmpty()) {
            return Optional.empty();
        }
        
        AIModelConfig model = modelOpt.get();
        
        // Find fields that are new
        List<String> existingFields = model.supportedFields();
        List<String> newFields = supportedFields.stream()
                .filter(field -> !existingFields.contains(field))
                .collect(Collectors.toList());
        
        // Create confidence records for new fields
        for (String fieldName : newFields) {
            AIFieldConfidence confidence = AIFieldConfidence.builder()
                    .fieldName(fieldName)
                    .modelId(modelId)
                    .build();
            
            fieldConfidenceRepository.save(confidence);
        }
        
        // Update model config
        return modelConfigRepository.updateSupportedFields(modelId, supportedFields);
    }

    @Override
    public List<AIFieldConfidence> getFieldConfidence(String fieldName) {
        return fieldConfidenceRepository.findByFieldName(fieldName);
    }

    @Override
    public List<AIFieldConfidence> getModelConfidence(String modelId) {
        return fieldConfidenceRepository.findByModelId(modelId);
    }

    @Override
    public Map<String, Map<String, Object>> getFieldPerformanceMetrics() {
        Map<String, Map<String, Object>> metrics = new HashMap<>();
        
        // Process all field confidence records
        Map<String, List<AIFieldConfidence>> confidenceByField = 
                fieldConfidenceRepository.findAll().stream()
                        .collect(Collectors.groupingBy(AIFieldConfidence::fieldName));
        
        // Calculate metrics for each field
        for (Map.Entry<String, List<AIFieldConfidence>> entry : confidenceByField.entrySet()) {
            String fieldName = entry.getKey();
            List<AIFieldConfidence> confidenceRecords = entry.getValue();
            
            // Calculate average metrics
            double avgAcceptanceRate = confidenceRecords.stream()
                    .mapToDouble(AIFieldConfidence::acceptanceRate)
                    .average()
                    .orElse(0.0);
            
            int totalPredictions = confidenceRecords.stream()
                    .mapToInt(AIFieldConfidence::predictionCount)
                    .sum();
            
            int totalAccepted = confidenceRecords.stream()
                    .mapToInt(AIFieldConfidence::acceptedCount)
                    .sum();
            
            int totalModified = confidenceRecords.stream()
                    .mapToInt(AIFieldConfidence::modifiedCount)
                    .sum();
            
            int totalRejected = confidenceRecords.stream()
                    .mapToInt(AIFieldConfidence::rejectedCount)
                    .sum();
            
            // Create metrics map
            Map<String, Object> fieldMetrics = new HashMap<>();
            fieldMetrics.put("averageAcceptanceRate", avgAcceptanceRate);
            fieldMetrics.put("totalPredictions", totalPredictions);
            fieldMetrics.put("totalAccepted", totalAccepted);
            fieldMetrics.put("totalModified", totalModified);
            fieldMetrics.put("totalRejected", totalRejected);
            fieldMetrics.put("supportedByModels", confidenceRecords.size());
            
            metrics.put(fieldName, fieldMetrics);
        }
        
        return metrics;
    }

    @Override
    public Map<String, Map<String, Object>> getModelPerformanceMetrics() {
        Map<String, Map<String, Object>> metrics = new HashMap<>();
        
        // Process all model confidence records
        Map<String, List<AIFieldConfidence>> confidenceByModel = 
                fieldConfidenceRepository.findAll().stream()
                        .collect(Collectors.groupingBy(AIFieldConfidence::modelId));
        
        // Get model configs
        Map<String, AIModelConfig> modelConfigs = modelConfigRepository.findAll().stream()
                .collect(Collectors.toMap(AIModelConfig::modelId, config -> config));
        
        // Calculate metrics for each model
        for (Map.Entry<String, List<AIFieldConfidence>> entry : confidenceByModel.entrySet()) {
            String modelId = entry.getKey();
            List<AIFieldConfidence> confidenceRecords = entry.getValue();
            
            // Calculate average metrics
            double avgAcceptanceRate = confidenceRecords.stream()
                    .mapToDouble(AIFieldConfidence::acceptanceRate)
                    .average()
                    .orElse(0.0);
            
            int totalPredictions = confidenceRecords.stream()
                    .mapToInt(AIFieldConfidence::predictionCount)
                    .sum();
            
            int totalAccepted = confidenceRecords.stream()
                    .mapToInt(AIFieldConfidence::acceptedCount)
                    .sum();
            
            int totalModified = confidenceRecords.stream()
                    .mapToInt(AIFieldConfidence::modifiedCount)
                    .sum();
            
            int totalRejected = confidenceRecords.stream()
                    .mapToInt(AIFieldConfidence::rejectedCount)
                    .sum();
            
            // Create metrics map
            Map<String, Object> modelMetrics = new HashMap<>();
            modelMetrics.put("averageAcceptanceRate", avgAcceptanceRate);
            modelMetrics.put("totalPredictions", totalPredictions);
            modelMetrics.put("totalAccepted", totalAccepted);
            modelMetrics.put("totalModified", totalModified);
            modelMetrics.put("totalRejected", totalRejected);
            modelMetrics.put("supportedFields", confidenceRecords.size());
            
            // Add model info
            AIModelConfig modelConfig = modelConfigs.get(modelId);
            if (modelConfig != null) {
                modelMetrics.put("name", modelConfig.modelName());
                modelMetrics.put("version", modelConfig.modelVersion());
                modelMetrics.put("type", modelConfig.modelType());
                modelMetrics.put("enabled", modelConfig.enabled());
            }
            
            metrics.put(modelId, modelMetrics);
        }
        
        return metrics;
    }

    @Override
    public boolean unregisterModel(String modelId) {
        logger.info("Unregistering model {}", modelId);
        
        // Get model config
        Optional<AIModelConfig> modelOpt = modelConfigRepository.findByModelId(modelId);
        if (modelOpt.isEmpty()) {
            logger.warn("Model {} not found for unregistration", modelId);
            return false;
        }
        
        AIModelConfig model = modelOpt.get();
        
        // Delete all confidence records for this model
        List<AIFieldConfidence> confidenceRecords = fieldConfidenceRepository.findByModelId(modelId);
        for (AIFieldConfidence confidence : confidenceRecords) {
            fieldConfidenceRepository.delete(confidence.id());
        }
        
        // Delete model config
        modelConfigRepository.delete(model.id());
        
        return true;
    }
}