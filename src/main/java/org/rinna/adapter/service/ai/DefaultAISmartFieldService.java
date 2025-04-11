/*
 * Default implementation of AISmartFieldService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service.ai;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.rinna.domain.model.ai.AIFieldConfidence;
import org.rinna.domain.model.ai.AIFieldPriority;
import org.rinna.domain.model.ai.AIModelConfig;
import org.rinna.domain.model.ai.AISmartFieldPrediction;
import org.rinna.domain.model.ai.AIUserFeedback;
import org.rinna.domain.repository.ai.AIFeedbackRepository;
import org.rinna.domain.repository.ai.AIFieldConfidenceRepository;
import org.rinna.domain.repository.ai.AIFieldPriorityRepository;
import org.rinna.domain.repository.ai.AIModelConfigRepository;
import org.rinna.domain.repository.ai.AIPredictionRepository;
import org.rinna.usecase.ai.AISmartFieldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of AISmartFieldService.
 */
public class DefaultAISmartFieldService implements AISmartFieldService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAISmartFieldService.class);
    
    // Time window for analyzing historical predictions (in days)
    private static final int HISTORICAL_ANALYSIS_WINDOW_DAYS = 30;
    
    // Weight factors for personalized prediction scoring
    private static final double USER_PATTERN_WEIGHT = 0.4;
    private static final double HISTORICAL_ACCURACY_WEIGHT = 0.3;
    private static final double CONTEXT_RELEVANCE_WEIGHT = 0.2;
    private static final double FIELD_PRIORITY_WEIGHT = 0.1;
    
    // Cache expiration in milliseconds (10 minutes)
    private static final long PATTERN_CACHE_EXPIRATION_MS = 600000;
    
    private final AIPredictionRepository predictionRepository;
    private final AIFeedbackRepository feedbackRepository;
    private final AIFieldPriorityRepository fieldPriorityRepository;
    private final AIFieldConfidenceRepository fieldConfidenceRepository;
    private final AIModelConfigRepository modelConfigRepository;
    
    // In-memory cache for user patterns with expiration time
    private final Map<UUID, Map<String, Object>> userPatternCache = new HashMap<>();
    private final Map<UUID, Long> userPatternCacheTimestamps = new HashMap<>();
    
    // In-memory cache for work item context
    private final Map<UUID, Map<String, Object>> workItemContextCache = new HashMap<>();
    private final Map<UUID, Long> workItemContextCacheTimestamps = new HashMap<>();
    
    /**
     * Constructor.
     *
     * @param predictionRepository The prediction repository
     * @param feedbackRepository The feedback repository
     * @param fieldPriorityRepository The field priority repository
     * @param fieldConfidenceRepository The field confidence repository
     * @param modelConfigRepository The model configuration repository
     */
    public DefaultAISmartFieldService(
            AIPredictionRepository predictionRepository,
            AIFeedbackRepository feedbackRepository,
            AIFieldPriorityRepository fieldPriorityRepository,
            AIFieldConfidenceRepository fieldConfidenceRepository,
            AIModelConfigRepository modelConfigRepository) {
        this.predictionRepository = predictionRepository;
        this.feedbackRepository = feedbackRepository;
        this.fieldPriorityRepository = fieldPriorityRepository;
        this.fieldConfidenceRepository = fieldConfidenceRepository;
        this.modelConfigRepository = modelConfigRepository;
    }

    @Override
    public Map<String, Object> predictFieldValues(UUID workItemId, Map<String, Object> currentFields) {
        logger.debug("Predicting field values for work item {}", workItemId);
        
        // Store the current fields in context cache for later use
        updateWorkItemContext(workItemId, currentFields);
        
        // Calculate which fields need to be predicted
        List<String> fieldsToPredict = new ArrayList<>();
        for (String field : getSupportedFields()) {
            if (!currentFields.containsKey(field) || currentFields.get(field) == null 
                    || currentFields.get(field).toString().isEmpty()) {
                fieldsToPredict.add(field);
            }
        }
        
        // Get field priorities to predict most important fields first
        List<AIFieldPriority> priorities = fieldPriorityRepository.findAllSortedByPriorityScore(fieldsToPredict.size());
        Map<String, Double> priorityMap = new HashMap<>();
        for (AIFieldPriority priority : priorities) {
            priorityMap.put(priority.fieldName(), priority.priorityScore());
        }
        
        // Sort fields by priority for better user experience
        fieldsToPredict.sort((f1, f2) -> {
            double p1 = priorityMap.getOrDefault(f1, 0.5);
            double p2 = priorityMap.getOrDefault(f2, 0.5);
            return Double.compare(p2, p1); // Higher priority first
        });
        
        // Get user ID for personalization if available
        UUID userId = extractUserIdFromContext(currentFields);
        
        return predictSpecificFields(workItemId, userId, currentFields, fieldsToPredict);
    }

    @Override
    public Map<String, Object> predictSpecificFields(
            UUID workItemId, Map<String, Object> currentFields, List<String> fieldsToPredict) {
        // Extract user ID from context for personalization if available
        UUID userId = extractUserIdFromContext(currentFields);
        return predictSpecificFields(workItemId, userId, currentFields, fieldsToPredict);
    }
    
    /**
     * Enhanced implementation of specific field prediction with user personalization.
     *
     * @param workItemId The work item ID
     * @param userId The user ID for personalization, or null if not available
     * @param currentFields The current field values
     * @param fieldsToPredict The fields to predict
     * @return A map of field names to predicted values
     */
    private Map<String, Object> predictSpecificFields(
            UUID workItemId, UUID userId, Map<String, Object> currentFields, List<String> fieldsToPredict) {
        logger.debug("Predicting specific fields {} for work item {} with user context {}", 
                fieldsToPredict, workItemId, userId);
        
        // Store the current fields in context cache for later use
        updateWorkItemContext(workItemId, currentFields);
        
        Map<String, Object> predictions = new HashMap<>();
        
        // Get enabled models
        List<AIModelConfig> enabledModels = modelConfigRepository.findAllEnabled();
        if (enabledModels.isEmpty()) {
            logger.warn("No enabled AI models found for prediction");
            return predictions;
        }
        
        // Analyze user patterns if a user ID is provided
        Map<String, Object> userPatterns = userId != null ? 
                getUserPatterns(userId) : Collections.emptyMap();
        
        // For each field to predict
        for (String fieldName : fieldsToPredict) {
            // Get the model(s) that support this field and sort by confidence
            List<AIModelConfig> supportingModels = enabledModels.stream()
                    .filter(model -> model.supportedFields().contains(fieldName))
                    .sorted(Comparator.comparing(model -> 
                            fieldConfidenceRepository.findByFieldNameAndModelId(fieldName, model.modelId())
                                .map(AIFieldConfidence::acceptanceRate)
                                .orElse(0.5),
                            Comparator.reverseOrder()))
                    .collect(Collectors.toList());
            
            if (!supportingModels.isEmpty()) {
                // Get predictions from multiple models for comparison
                List<PredictionCandidate> candidates = new ArrayList<>();
                
                // Try up to 3 models for better predictions
                for (int i = 0; i < Math.min(3, supportingModels.size()); i++) {
                    AIModelConfig model = supportingModels.get(i);
                    
                    // Make prediction with current model
                    Object prediction = generatePrediction(workItemId, fieldName, currentFields, model);
                    if (prediction != null) {
                        double baseConfidence = calculateConfidence(fieldName, prediction, model.modelId());
                        
                        // Generate evidence and store it for scoring
                        Set<String> evidenceFactors = generateEvidenceFactors(fieldName, currentFields);
                        
                        // Calculate personalized confidence score
                        double personalizedConfidence = calculatePersonalizedConfidence(
                                userId, workItemId, fieldName, prediction, baseConfidence, userPatterns, currentFields);
                        
                        // Add to candidates
                        candidates.add(new PredictionCandidate(
                                prediction, 
                                personalizedConfidence,
                                model.modelId(),
                                evidenceFactors));
                    }
                }
                
                // Select the best prediction based on confidence
                if (!candidates.isEmpty()) {
                    // Sort by confidence score (descending)
                    candidates.sort(Comparator.comparing(PredictionCandidate::confidence).reversed());
                    
                    // Use the highest-confidence prediction
                    PredictionCandidate bestCandidate = candidates.get(0);
                    
                    // Store the prediction
                    AISmartFieldPrediction smartFieldPrediction = AISmartFieldPrediction.builder()
                            .workItemId(workItemId)
                            .fieldName(fieldName)
                            .predictedValue(bestCandidate.prediction)
                            .confidenceScore(bestCandidate.confidence)
                            .evidenceFactors(bestCandidate.evidenceFactors)
                            .modelId(bestCandidate.modelId)
                            .build();
                    
                    predictionRepository.save(smartFieldPrediction);
                    
                    // Add to results map
                    predictions.put(fieldName, bestCandidate.prediction);
                    
                    logger.debug("Predicted {} = {} for work item {} with confidence {}", 
                            fieldName, bestCandidate.prediction, workItemId, bestCandidate.confidence);
                    
                    // Update user patterns cache if we have a user ID
                    if (userId != null) {
                        updateUserPatternCache(userId, fieldName, bestCandidate.prediction);
                    }
                }
            } else {
                logger.warn("No model supports field {}", fieldName);
            }
        }
        
        return predictions;
    }
    
    /**
     * Private class to represent a prediction candidate with its metadata.
     */
    private static class PredictionCandidate {
        final Object prediction;
        final double confidence;
        final String modelId;
        final Set<String> evidenceFactors;
        
        PredictionCandidate(Object prediction, double confidence, String modelId, Set<String> evidenceFactors) {
            this.prediction = prediction;
            this.confidence = confidence;
            this.modelId = modelId;
            this.evidenceFactors = evidenceFactors;
        }
    }

    @Override
    public Optional<Double> getConfidenceScore(UUID predictionId) {
        return predictionRepository.findById(predictionId)
                .map(AISmartFieldPrediction::confidenceScore);
    }

    @Override
    public double getConfidenceScore(String fieldName, Object predictedValue) {
        // Get confidence metrics for the field from repository
        List<AIFieldConfidence> confidenceMetrics = fieldConfidenceRepository.findByFieldName(fieldName);
        
        if (confidenceMetrics.isEmpty()) {
            // No historical data, use default confidence
            return 0.5;
        }
        
        // Calculate average acceptance rate across models
        double avgAcceptanceRate = confidenceMetrics.stream()
                .mapToDouble(AIFieldConfidence::acceptanceRate)
                .average()
                .orElse(0.5);
        
        // Get field priority
        double fieldImportance = fieldPriorityRepository.findByFieldName(fieldName)
                .map(AIFieldPriority::priorityScore)
                .orElse(0.5);
        
        // Adjust confidence based on field importance
        double adjustedConfidence = (avgAcceptanceRate * 0.7) + (fieldImportance * 0.3);
        
        // Add small random variation to prevent all predictions having identical confidence
        double randomVariation = ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
        double finalConfidence = Math.min(1.0, Math.max(0.1, adjustedConfidence + randomVariation));
        
        return finalConfidence;
    }
    
    /**
     * Calculates a personalized confidence score based on user patterns and historical accuracy.
     *
     * @param userId The user ID, or null if not available
     * @param workItemId The work item ID
     * @param fieldName The field name
     * @param prediction The predicted value
     * @param baseConfidence The base confidence score from the model
     * @param userPatterns The user pattern data
     * @param currentFields The current field values
     * @return The personalized confidence score
     */
    private double calculatePersonalizedConfidence(
            UUID userId, 
            UUID workItemId, 
            String fieldName, 
            Object prediction, 
            double baseConfidence,
            Map<String, Object> userPatterns,
            Map<String, Object> currentFields) {
        
        // Start with the base confidence
        double personalizedScore = baseConfidence;
        
        // 1. Adjust based on user patterns (if user ID is available)
        if (userId != null && !userPatterns.isEmpty()) {
            double userPatternScore = calculateUserPatternScore(fieldName, prediction, userPatterns);
            personalizedScore = (personalizedScore * (1 - USER_PATTERN_WEIGHT)) + 
                                (userPatternScore * USER_PATTERN_WEIGHT);
        }
        
        // 2. Adjust based on historical accuracy
        double historicalAccuracyScore = calculateHistoricalAccuracyScore(fieldName, prediction);
        personalizedScore = (personalizedScore * (1 - HISTORICAL_ACCURACY_WEIGHT)) + 
                           (historicalAccuracyScore * HISTORICAL_ACCURACY_WEIGHT);
        
        // 3. Adjust based on context relevance
        double contextRelevanceScore = calculateContextRelevanceScore(fieldName, prediction, currentFields);
        personalizedScore = (personalizedScore * (1 - CONTEXT_RELEVANCE_WEIGHT)) + 
                           (contextRelevanceScore * CONTEXT_RELEVANCE_WEIGHT);
        
        // 4. Adjust based on field priority
        double fieldPriorityScore = calculateFieldPriorityScore(fieldName);
        personalizedScore = (personalizedScore * (1 - FIELD_PRIORITY_WEIGHT)) + 
                           (fieldPriorityScore * FIELD_PRIORITY_WEIGHT);
        
        // Ensure we're in valid range (0.0-1.0)
        return Math.min(1.0, Math.max(0.0, personalizedScore));
    }
    
    /**
     * Calculates a score based on how well this prediction aligns with the user's previous patterns.
     *
     * @param fieldName The field name
     * @param prediction The predicted value
     * @param userPatterns The user pattern data
     * @return A score between 0.0 and 1.0
     */
    private double calculateUserPatternScore(String fieldName, Object prediction, Map<String, Object> userPatterns) {
        // Check if we have a pattern for this field
        String patternKey = "pattern_" + fieldName;
        if (userPatterns.containsKey(patternKey)) {
            Object patternValue = userPatterns.get(patternKey);
            
            // If the prediction matches the user's common pattern exactly, high confidence
            if (Objects.equals(prediction, patternValue)) {
                return 0.9; // Strong match with user pattern
            }
            
            // For string values, check similarity
            if (prediction instanceof String && patternValue instanceof String) {
                String predString = (String) prediction;
                String patternString = (String) patternValue;
                
                // Check for substring or case-insensitive match
                if (predString.toLowerCase().contains(patternString.toLowerCase()) ||
                    patternString.toLowerCase().contains(predString.toLowerCase())) {
                    return 0.7; // Partial match with user pattern
                }
            }
            
            // For numeric values, check proximity
            if (prediction instanceof Number && patternValue instanceof Number) {
                double predValue = ((Number) prediction).doubleValue();
                double patternValue = ((Number) patternValue).doubleValue();
                double difference = Math.abs(predValue - patternValue);
                double maxValue = Math.max(Math.abs(predValue), Math.abs(patternValue));
                
                if (maxValue > 0) {
                    double relativeDiff = difference / maxValue;
                    // Close numeric values get higher scores
                    if (relativeDiff < 0.1) return 0.8;
                    if (relativeDiff < 0.25) return 0.6;
                    if (relativeDiff < 0.5) return 0.4;
                }
            }
            
            // Different from pattern but we have pattern data
            return 0.3;
        }
        
        // No pattern data for this field
        return 0.5;
    }
    
    /**
     * Calculates a score based on the historical accuracy of predictions for this field.
     *
     * @param fieldName The field name
     * @param prediction The predicted value
     * @return A score between 0.0 and 1.0
     */
    private double calculateHistoricalAccuracyScore(String fieldName, Object prediction) {
        // Get historical predictions for this field from the last month
        LocalDateTime startDate = LocalDateTime.now().minus(HISTORICAL_ANALYSIS_WINDOW_DAYS, ChronoUnit.DAYS);
        List<AISmartFieldPrediction> historicalPredictions = 
                predictionRepository.findByFieldNameSince(fieldName, startDate);
        
        if (historicalPredictions.isEmpty()) {
            return 0.5; // No historical data
        }
        
        // Check if we've made similar predictions before, and if they were accepted
        int matchCount = 0;
        int acceptedCount = 0;
        
        for (AISmartFieldPrediction historicalPrediction : historicalPredictions) {
            // Check if the prediction is similar
            if (Objects.equals(prediction, historicalPrediction.predictedValue())) {
                matchCount++;
                if (historicalPrediction.userAccepted()) {
                    acceptedCount++;
                }
            }
        }
        
        if (matchCount > 0) {
            // Calculate acceptance rate for this prediction value
            return (double) acceptedCount / matchCount;
        }
        
        // No matching historical predictions
        // Fall back to overall field acceptance rate
        return historicalPredictions.stream()
                .filter(AISmartFieldPrediction::userAccepted)
                .count() / (double) historicalPredictions.size();
    }
    
    /**
     * Calculates a score based on how relevant the prediction is to the current context.
     *
     * @param fieldName The field name
     * @param prediction The predicted value
     * @param currentFields The current field values
     * @return A score between 0.0 and 1.0
     */
    private double calculateContextRelevanceScore(String fieldName, Object prediction, Map<String, Object> currentFields) {
        // Get the evidence factors for this field
        Set<String> evidenceFactors = generateEvidenceFactors(fieldName, currentFields);
        
        // More evidence factors generally means a more contextually relevant prediction
        int evidenceCount = evidenceFactors.size();
        
        // Adjust score based on number of evidence factors
        // More factors generally means a more contextually informed prediction
        if (evidenceCount >= 10) return 0.9;
        if (evidenceCount >= 7) return 0.8;
        if (evidenceCount >= 5) return 0.7;
        if (evidenceCount >= 3) return 0.6;
        if (evidenceCount >= 1) return 0.5;
        
        return 0.4; // Very little evidence
    }
    
    /**
     * Calculates a score based on the priority of the field.
     *
     * @param fieldName The field name
     * @return A score between 0.0 and 1.0
     */
    private double calculateFieldPriorityScore(String fieldName) {
        // Get field priority from repository
        return fieldPriorityRepository.findByFieldName(fieldName)
                .map(AIFieldPriority::priorityScore)
                .orElse(0.5);
    }
    
    /**
     * Extracts a user ID from the current fields if available.
     *
     * @param currentFields The current field values
     * @return The user ID, or null if not available
     */
    private UUID extractUserIdFromContext(Map<String, Object> currentFields) {
        // Check for common user ID field names
        for (String fieldName : Arrays.asList("userId", "user_id", "createdBy", "created_by", "assignee", "reporter")) {
            if (currentFields.containsKey(fieldName) && currentFields.get(fieldName) != null) {
                Object value = currentFields.get(fieldName);
                try {
                    if (value instanceof UUID) {
                        return (UUID) value;
                    } else if (value instanceof String) {
                        return UUID.fromString(value.toString());
                    }
                } catch (IllegalArgumentException e) {
                    // Not a valid UUID, continue checking other fields
                }
            }
        }
        
        return null; // No user ID found
    }
    
    /**
     * Updates the work item context cache with current field values.
     *
     * @param workItemId The work item ID
     * @param currentFields The current field values
     */
    private void updateWorkItemContext(UUID workItemId, Map<String, Object> currentFields) {
        workItemContextCache.put(workItemId, new HashMap<>(currentFields));
        workItemContextCacheTimestamps.put(workItemId, System.currentTimeMillis());
    }
    
    /**
     * Gets the user patterns for a specific user, analyzing their historical patterns.
     *
     * @param userId The user ID
     * @return A map of pattern data
     */
    private Map<String, Object> getUserPatterns(UUID userId) {
        // Check if we have a recent cache entry
        if (userPatternCache.containsKey(userId)) {
            long timestamp = userPatternCacheTimestamps.getOrDefault(userId, 0L);
            if (System.currentTimeMillis() - timestamp < PATTERN_CACHE_EXPIRATION_MS) {
                // Cache is still fresh
                return userPatternCache.get(userId);
            }
        }
        
        // Generate new patterns
        Map<String, Object> patterns = analyzeUserPatterns(userId);
        
        // Update cache
        userPatternCache.put(userId, patterns);
        userPatternCacheTimestamps.put(userId, System.currentTimeMillis());
        
        return patterns;
    }
    
    /**
     * Analyzes a user's historical patterns to identify preferences and common values.
     *
     * @param userId The user ID
     * @return A map of pattern data
     */
    private Map<String, Object> analyzeUserPatterns(UUID userId) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Get user's feedback history
        List<AIUserFeedback> userFeedback = feedbackRepository.findByUserId(userId);
        
        // Exit early if no historical data
        if (userFeedback.isEmpty()) {
            return patterns;
        }
        
        // Group feedback by field name
        Map<String, List<AIUserFeedback>> feedbackByField = new HashMap<>();
        
        for (AIUserFeedback feedback : userFeedback) {
            // Get prediction to find field name
            Optional<AISmartFieldPrediction> predictionOpt = 
                    predictionRepository.findById(feedback.predictionId());
            
            if (predictionOpt.isPresent()) {
                AISmartFieldPrediction prediction = predictionOpt.get();
                String fieldName = prediction.fieldName();
                
                // Add to grouped map
                if (!feedbackByField.containsKey(fieldName)) {
                    feedbackByField.put(fieldName, new ArrayList<>());
                }
                feedbackByField.get(fieldName).add(feedback);
            }
        }
        
        // Analyze patterns for each field
        for (Map.Entry<String, List<AIUserFeedback>> entry : feedbackByField.entrySet()) {
            String fieldName = entry.getKey();
            List<AIUserFeedback> fieldFeedback = entry.getValue();
            
            // Find the most common accepted value
            Map<Object, Integer> valueFrequency = new HashMap<>();
            
            for (AIUserFeedback feedback : fieldFeedback) {
                if (feedback.type() == AIUserFeedback.FeedbackType.ACCEPTED) {
                    // Get prediction value
                    Optional<AISmartFieldPrediction> predictionOpt = 
                            predictionRepository.findById(feedback.predictionId());
                    
                    if (predictionOpt.isPresent()) {
                        Object value = predictionOpt.get().predictedValue();
                        valueFrequency.put(value, valueFrequency.getOrDefault(value, 0) + 1);
                    }
                } else if (feedback.type() == AIUserFeedback.FeedbackType.REPLACED && 
                          feedback.replacementValue() != null) {
                    // User provided their own value - these are even more valuable for learning patterns
                    Object value = feedback.replacementValue();
                    valueFrequency.put(value, valueFrequency.getOrDefault(value, 0) + 2); // Double weight
                }
            }
            
            // Find most frequently used value
            if (!valueFrequency.isEmpty()) {
                Object mostFrequentValue = valueFrequency.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
                
                if (mostFrequentValue != null) {
                    patterns.put("pattern_" + fieldName, mostFrequentValue);
                    int frequency = valueFrequency.get(mostFrequentValue);
                    patterns.put("pattern_" + fieldName + "_strength", 
                            Math.min(1.0, frequency / 10.0)); // Scale by frequency, cap at 1.0
                }
            }
            
            // Store acceptance rate for this field
            long acceptedCount = fieldFeedback.stream()
                    .filter(f -> f.type() == AIUserFeedback.FeedbackType.ACCEPTED)
                    .count();
            
            patterns.put("acceptance_rate_" + fieldName, 
                    (double) acceptedCount / fieldFeedback.size());
        }
        
        return patterns;
    }
    
    /**
     * Updates the user pattern cache with a new prediction.
     *
     * @param userId The user ID
     * @param fieldName The field name
     * @param prediction The predicted value
     */
    private void updateUserPatternCache(UUID userId, String fieldName, Object prediction) {
        // Only update if we already have a cache entry
        if (userPatternCache.containsKey(userId)) {
            Map<String, Object> patterns = userPatternCache.get(userId);
            
            // Update with sliding window approach - we gradually adjust the pattern
            String patternKey = "pattern_" + fieldName;
            String strengthKey = "pattern_" + fieldName + "_strength";
            
            if (patterns.containsKey(patternKey)) {
                // Get current pattern and strength
                Object currentPattern = patterns.get(patternKey);
                double strength = patterns.containsKey(strengthKey) ? 
                        ((Number) patterns.get(strengthKey)).doubleValue() : 0.5;
                
                // If prediction matches current pattern, increase strength
                if (Objects.equals(prediction, currentPattern)) {
                    patterns.put(strengthKey, Math.min(1.0, strength + 0.1));
                } else {
                    // Different prediction, consider updating pattern if strength is low
                    if (strength < 0.7) {
                        patterns.put(patternKey, prediction);
                        patterns.put(strengthKey, 0.3); // Reset strength for new pattern
                    }
                    // Otherwise, keep existing strong pattern
                }
            } else {
                // No existing pattern, add new one
                patterns.put(patternKey, prediction);
                patterns.put(strengthKey, 0.3); // Initial strength
            }
            
            // Reset cache timestamp
            userPatternCacheTimestamps.put(userId, System.currentTimeMillis());
        }
    }

    @Override
    public AIUserFeedback provideFeedback(
            UUID predictionId, 
            UUID userId, 
            AIUserFeedback.FeedbackType feedbackType, 
            String comment, 
            Object replacementValue) {
        logger.debug("User {} providing {} feedback for prediction {}", 
                userId, feedbackType, predictionId);
        
        // Ensure prediction exists
        Optional<AISmartFieldPrediction> predictionOpt = predictionRepository.findById(predictionId);
        if (predictionOpt.isEmpty()) {
            throw new IllegalArgumentException("Prediction not found: " + predictionId);
        }
        
        AISmartFieldPrediction prediction = predictionOpt.get();
        String fieldName = prediction.fieldName();
        String modelId = prediction.modelId();
        UUID workItemId = prediction.workItemId();
        
        // Create feedback
        AIUserFeedback feedback = AIUserFeedback.builder()
                .predictionId(predictionId)
                .userId(userId)
                .type(feedbackType)
                .comment(comment)
                .replacementValue(replacementValue)
                .build();
        
        // Save feedback
        AIUserFeedback savedFeedback = feedbackRepository.save(feedback);
        
        // Update prediction acceptance status
        boolean accepted = feedbackType == AIUserFeedback.FeedbackType.ACCEPTED;
        predictionRepository.updateAcceptanceStatus(predictionId, accepted);
        
        // Update confidence metrics
        fieldConfidenceRepository.updateWithFeedback(fieldName, modelId, feedbackType);
        
        // Update field priority based on feedback
        updateFieldPriorityBasedOnFeedback(fieldName, feedbackType);
        
        // Clear user pattern cache to ensure fresh analysis on next prediction
        if (userId != null) {
            if (userPatternCache.containsKey(userId)) {
                userPatternCache.remove(userId);
                userPatternCacheTimestamps.remove(userId);
            }
        }
        
        // If this was a replacement, learn from the provided value
        if (feedbackType == AIUserFeedback.FeedbackType.REPLACED && replacementValue != null) {
            learnFromUserReplacement(userId, workItemId, fieldName, replacementValue);
        }
        
        return savedFeedback;
    }
    
    /**
     * Updates field priority based on feedback to improve prediction focus.
     *
     * @param fieldName The field name
     * @param feedbackType The feedback type
     */
    private void updateFieldPriorityBasedOnFeedback(String fieldName, AIUserFeedback.FeedbackType feedbackType) {
        // Adjust field priority based on feedback type
        Optional<AIFieldPriority> priorityOpt = fieldPriorityRepository.findByFieldName(fieldName);
        
        if (priorityOpt.isPresent()) {
            AIFieldPriority priority = priorityOpt.get();
            double currentRating = priority.valueRating();
            
            // Adjust value rating based on feedback
            if (feedbackType == AIUserFeedback.FeedbackType.ACCEPTED) {
                // Positive feedback increases value rating (with diminishing returns)
                double increase = Math.max(0.05, 0.1 * (1.0 - currentRating));
                fieldPriorityRepository.updateValueRating(fieldName, Math.min(1.0, currentRating + increase));
            } else if (feedbackType == AIUserFeedback.FeedbackType.REJECTED) {
                // Negative feedback slightly decreases value rating
                fieldPriorityRepository.updateValueRating(fieldName, Math.max(0.1, currentRating - 0.05));
            } else if (feedbackType == AIUserFeedback.FeedbackType.REPLACED) {
                // Replacement is neutral-to-positive - user cares about the field
                // but our prediction wasn't right
                fieldPriorityRepository.updateValueRating(fieldName, Math.min(1.0, currentRating + 0.02));
            }
            
            // Recalculate overall priority score
            fieldPriorityRepository.recalculatePriorityScore(fieldName);
        }
    }
    
    /**
     * Learns from user-provided replacement values to improve future predictions.
     * 
     * @param userId The user ID
     * @param workItemId The work item ID
     * @param fieldName The field name
     * @param replacementValue The replacement value
     */
    private void learnFromUserReplacement(UUID userId, UUID workItemId, String fieldName, Object replacementValue) {
        // Get work item context to understand when this value is appropriate
        Map<String, Object> context = workItemContextCache.getOrDefault(workItemId, Collections.emptyMap());
        
        if (!context.isEmpty()) {
            // Create a new synthetic prediction with the user's value
            // This helps the system learn from user inputs
            AISmartFieldPrediction userValuePrediction = AISmartFieldPrediction.builder()
                    .workItemId(workItemId)
                    .fieldName(fieldName)
                    .predictedValue(replacementValue)
                    .confidenceScore(1.0) // User-provided values get perfect confidence
                    .evidenceFactors(generateEvidenceFactors(fieldName, context))
                    .modelId("user-feedback-model") // Special model ID for user-provided values
                    .userAccepted(true) // Auto-accept user's own value
                    .build();
            
            // Save the synthetic prediction for future learning
            predictionRepository.save(userValuePrediction);
            
            logger.debug("Created synthetic prediction from user replacement: field={}, value={}", 
                    fieldName, replacementValue);
        }
    }

    @Override
    public List<AISmartFieldPrediction> getRecentPredictions(UUID workItemId, int limit) {
        return predictionRepository.findMostRecentByWorkItemId(workItemId, limit);
    }

    @Override
    public List<AIFieldPriority> getFieldPriorities(int limit) {
        return fieldPriorityRepository.findAllSortedByPriorityScore(limit);
    }

    @Override
    public void trackFieldUsage(String fieldName) {
        // Get or create field priority
        Optional<AIFieldPriority> priorityOpt = fieldPriorityRepository.findByFieldName(fieldName);
        
        if (priorityOpt.isPresent()) {
            // Increment usage count
            fieldPriorityRepository.incrementUsageCount(fieldName);
        } else {
            // Create new field priority
            AIFieldPriority newPriority = AIFieldPriority.builder()
                    .fieldName(fieldName)
                    .usageCount(1)
                    .build();
            
            fieldPriorityRepository.save(newPriority);
        }
        
        // Recalculate priority score
        fieldPriorityRepository.recalculatePriorityScore(fieldName);
    }

    @Override
    public void updateFieldCompletionRate(String fieldName, double completionRate) {
        // Get or create field priority
        Optional<AIFieldPriority> priorityOpt = fieldPriorityRepository.findByFieldName(fieldName);
        
        if (priorityOpt.isPresent()) {
            // Update completion rate
            fieldPriorityRepository.updateCompletionRate(fieldName, completionRate);
        } else {
            // Create new field priority
            AIFieldPriority newPriority = AIFieldPriority.builder()
                    .fieldName(fieldName)
                    .completionRate(completionRate)
                    .build();
            
            fieldPriorityRepository.save(newPriority);
        }
        
        // Recalculate priority score
        fieldPriorityRepository.recalculatePriorityScore(fieldName);
    }

    @Override
    public void updateFieldValueRating(String fieldName, double valueRating) {
        // Get or create field priority
        Optional<AIFieldPriority> priorityOpt = fieldPriorityRepository.findByFieldName(fieldName);
        
        if (priorityOpt.isPresent()) {
            // Update value rating
            fieldPriorityRepository.updateValueRating(fieldName, valueRating);
        } else {
            // Create new field priority
            AIFieldPriority newPriority = AIFieldPriority.builder()
                    .fieldName(fieldName)
                    .valueRating(valueRating)
                    .build();
            
            fieldPriorityRepository.save(newPriority);
        }
        
        // Recalculate priority score
        fieldPriorityRepository.recalculatePriorityScore(fieldName);
    }

    @Override
    public List<String> getSupportedFields() {
        // Collect all supported fields from all models
        Set<String> allSupportedFields = new HashSet<>();
        
        // Get fields from registered models
        for (AIModelConfig model : modelConfigRepository.findAllEnabled()) {
            allSupportedFields.addAll(model.supportedFields());
        }
        
        // If no models are registered yet, return our standard set of supported fields
        if (allSupportedFields.isEmpty()) {
            allSupportedFields.addAll(Arrays.asList(
                "status", "priority", "assignee", "dueDate", "estimatedEffort", "tags",
                "cognitiveLoad", "cynefinDomain", "category", "workParadigm", 
                "completionPercentage", "outcome"
            ));
        }
        
        return new ArrayList<>(allSupportedFields);
    }
    
    @Override
    public Map<String, Map<String, Object>> getUserPatterns(UUID userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        
        // Ensure we have fresh user patterns
        Map<String, Object> rawPatterns = getUserPatterns(userId);
        
        // Convert to the expected output format
        Map<String, Map<String, Object>> result = new HashMap<>();
        
        // Group patterns by field
        for (Map.Entry<String, Object> entry : rawPatterns.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Skip non-pattern entries
            if (!key.startsWith("pattern_") || key.endsWith("_strength")) {
                continue;
            }
            
            // Extract field name
            String fieldName = key.substring("pattern_".length());
            
            // Create or get field map
            if (!result.containsKey(fieldName)) {
                result.put(fieldName, new HashMap<>());
            }
            
            Map<String, Object> fieldMap = result.get(fieldName);
            
            // Add value
            fieldMap.put("value", value);
            
            // Add strength if available
            String strengthKey = key + "_strength";
            if (rawPatterns.containsKey(strengthKey)) {
                fieldMap.put("strength", rawPatterns.get(strengthKey));
            }
            
            // Add acceptance rate if available
            String acceptanceRateKey = "acceptance_rate_" + fieldName;
            if (rawPatterns.containsKey(acceptanceRateKey)) {
                fieldMap.put("acceptanceRate", rawPatterns.get(acceptanceRateKey));
            }
        }
        
        return result;
    }
    
    @Override
    public void clearUserPatternCache(UUID userId) {
        if (userId != null) {
            userPatternCache.remove(userId);
            userPatternCacheTimestamps.remove(userId);
            logger.debug("Cleared user pattern cache for user: {}", userId);
        }
    }
    
    @Override
    public Optional<Set<String>> getPredictionEvidence(UUID predictionId) {
        return predictionRepository.findById(predictionId)
                .map(AISmartFieldPrediction::evidenceFactors);
    }
    
    /**
     * Generates a prediction for a field.
     *
     * @param workItemId The work item ID
     * @param fieldName The field name
     * @param currentFields The current field values
     * @param model The model configuration
     * @return The predicted value
     */
    private Object generatePrediction(
            UUID workItemId, String fieldName, Map<String, Object> currentFields, AIModelConfig model) {
        // In a real implementation, this would use ML models, historical data, etc.
        // We'll use a more sophisticated pattern-based approach that considers multiple factors
        
        logger.debug("Generating prediction for field {} in work item {}", fieldName, workItemId);
        
        // These are the enhanced field prediction methods
        switch (fieldName) {
            case "status":
                return predictStatus(currentFields);
            case "priority":
                return predictPriority(currentFields);
            case "assignee":
                return predictAssignee(currentFields);
            case "dueDate":
                return predictDueDate(currentFields);
            case "estimatedEffort":
                return predictEstimatedEffort(currentFields);
            case "tags":
                return predictTags(currentFields);
            case "cognitiveLoad":
                return predictCognitiveLoad(currentFields);
            case "cynefinDomain":
                return predictCynefinDomain(currentFields);
            case "category":
                return predictCategory(currentFields);
            case "workParadigm":
                return predictWorkParadigm(currentFields);
            case "completionPercentage":
                return predictCompletionPercentage(currentFields);
            case "outcome":
                return predictOutcome(currentFields);
            default:
                logger.warn("No prediction method available for field: {}", fieldName);
                return ""; // Default empty prediction
        }
    }
    
    /**
     * Generates evidence factors for a prediction.
     *
     * @param fieldName The field name
     * @param currentFields The current field values
     * @return The evidence factors
     */
    private Set<String> generateEvidenceFactors(String fieldName, Map<String, Object> currentFields) {
        Set<String> factors = new HashSet<>();
        
        // Add basic factors based on what fields are available
        for (Map.Entry<String, Object> entry : currentFields.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                factors.add("has_" + entry.getKey());
            }
        }
        
        // Add field-specific factors
        switch (fieldName) {
            case "status":
                addStatusFactors(factors, currentFields);
                break;
            case "priority":
                addPriorityFactors(factors, currentFields);
                break;
            case "assignee":
                addAssigneeFactors(factors, currentFields);
                break;
            case "dueDate":
                addDueDateFactors(factors, currentFields);
                break;
            case "estimatedEffort":
                addEstimatedEffortFactors(factors, currentFields);
                break;
            case "tags":
                addTagsFactors(factors, currentFields);
                break;
            case "cognitiveLoad":
                addCognitiveLoadFactors(factors, currentFields);
                break;
            case "cynefinDomain":
                addCynefinDomainFactors(factors, currentFields);
                break;
            case "category":
                addCategoryFactors(factors, currentFields);
                break;
            case "workParadigm":
                addWorkParadigmFactors(factors, currentFields);
                break;
            case "completionPercentage":
                addCompletionPercentageFactors(factors, currentFields);
                break;
            case "outcome":
                addOutcomeFactors(factors, currentFields);
                break;
        }
        
        return factors;
    }
    
    /**
     * Calculates confidence score for a prediction.
     *
     * @param fieldName The field name
     * @param prediction The prediction
     * @param modelId The model ID
     * @return The confidence score
     */
    private double calculateConfidence(String fieldName, Object prediction, String modelId) {
        // Get confidence metrics for the field and model
        Optional<AIFieldConfidence> confidenceOpt = 
                fieldConfidenceRepository.findByFieldNameAndModelId(fieldName, modelId);
        
        if (confidenceOpt.isPresent()) {
            AIFieldConfidence confidence = confidenceOpt.get();
            return confidence.acceptanceRate();
        }
        
        // No historical data, use base confidence based on field
        switch (fieldName) {
            case "status":
                return 0.8; // Status prediction is usually accurate
            case "priority":
                return 0.7;
            case "assignee":
                return 0.6;
            case "dueDate":
                return 0.5;
            case "estimatedHours":
                return 0.4;
            case "tags":
                return 0.3; // Tag prediction is less accurate
            default:
                return 0.5; // Default middle confidence
        }
    }
    
    // Simple prediction methods for demonstration
    
    /**
     * Predicts the status of a work item based on its type and other available fields.
     *
     * @param currentFields The current field values
     * @return The predicted status
     */
    private String predictStatus(Map<String, Object> currentFields) {
        // Get the type to determine typical workflow starting points
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            // Bugs usually need immediate attention
            if ("BUG".equalsIgnoreCase(type)) {
                // Check priority to refine the prediction
                if (currentFields.containsKey("priority")) {
                    String priority = currentFields.get("priority").toString();
                    if ("HIGH".equalsIgnoreCase(priority) || "CRITICAL".equalsIgnoreCase(priority)) {
                        return "IN_PROGRESS"; // High priority bugs should be worked on immediately
                    }
                }
                return "TO_DO"; // Default for bugs
            } 
            // Features often start in backlog for planning
            else if ("FEATURE".equalsIgnoreCase(type)) {
                return "BACKLOG";
            } 
            // Tasks are often ready to be worked on
            else if ("TASK".equalsIgnoreCase(type)) {
                return "TO_DO";
            }
            // Documentation tasks are often kept in backlog until needed
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                return "BACKLOG";
            }
        }
        
        // Check for dependencies - items with dependencies often can't start yet
        if (currentFields.containsKey("dependencies") && currentFields.get("dependencies") != null) {
            Object deps = currentFields.get("dependencies");
            if (deps instanceof Collection && !((Collection<?>) deps).isEmpty()) {
                return "BLOCKED";
            }
        }
        
        // Default status for new items
        return "TO_DO";
    }
    
    /**
     * Predicts the priority of a work item based on type and other metadata.
     *
     * @param currentFields The current field values
     * @return The predicted priority
     */
    private String predictPriority(Map<String, Object> currentFields) {
        // Bugs are often higher priority
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type)) {
                // Check if there's a description that might indicate severity
                if (currentFields.containsKey("description")) {
                    String description = currentFields.get("description").toString().toLowerCase();
                    
                    // Look for signals of critical issues
                    if (description.contains("crash") || 
                        description.contains("security") || 
                        description.contains("data loss") ||
                        description.contains("urgent") ||
                        description.contains("critical")) {
                        return "CRITICAL";
                    }
                    
                    // Look for signals of high priority issues
                    if (description.contains("important") || 
                        description.contains("significant") || 
                        description.contains("major")) {
                        return "HIGH";
                    }
                }
                
                // Default for bugs is HIGH
                return "HIGH";
            } 
            // Features often start as medium priority
            else if ("FEATURE".equalsIgnoreCase(type)) {
                return "MEDIUM";
            }
            // Tasks are often lower priority unless specified
            else if ("TASK".equalsIgnoreCase(type)) {
                return "MEDIUM";
            }
            // Documentation is often lower priority
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                return "LOW";
            }
        }
        
        // Check if this is related to a high-priority item
        if (currentFields.containsKey("relatedItems") && currentFields.get("relatedItems") != null) {
            // In a real implementation, we would lookup the related items and check their priorities
            // For now, we'll use a simple heuristic
            return "MEDIUM";
        }
        
        // Default priority
        return "MEDIUM";
    }
    
    /**
     * Predicts the assignee for a work item based on reporter, type, and other factors.
     *
     * @param currentFields The current field values
     * @return The predicted assignee
     */
    private String predictAssignee(Map<String, Object> currentFields) {
        // For many tickets, the reporter is also a good candidate for assignee
        if (currentFields.containsKey("reporter")) {
            // In a real implementation, we would check the reporter's current workload
            // and expertise to see if they're the best match
            return currentFields.get("reporter").toString();
        }
        
        // Look at the type and tags to determine suitable assignees
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            // In a real implementation, we'd look up team members with appropriate skills
            if ("BUG".equalsIgnoreCase(type)) {
                // For demo, assume "devteam" handles bugs
                return "devteam";
            } else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                // For demo, assume "docteam" handles documentation
                return "docteam";
            } else if ("TASK".equalsIgnoreCase(type) && 
                       currentFields.containsKey("tags") && 
                       currentFields.get("tags") != null) {
                // Check tags to determine appropriate assignee
                Object tagsObj = currentFields.get("tags");
                if (tagsObj instanceof Collection) {
                    Collection<?> tags = (Collection<?>) tagsObj;
                    if (tags.contains("backend")) {
                        return "backend-team";
                    } else if (tags.contains("frontend")) {
                        return "frontend-team";
                    } else if (tags.contains("design")) {
                        return "design-team";
                    }
                }
            }
        }
        
        // Default if no better match is found
        return "unassigned";
    }
    
    /**
     * Predicts a suitable due date based on type, priority, and estimated effort.
     *
     * @param currentFields The current field values
     * @return The predicted due date
     */
    private String predictDueDate(Map<String, Object> currentFields) {
        LocalDateTime now = LocalDateTime.now();
        
        // High priority items get shorter timeframes
        if (currentFields.containsKey("priority")) {
            String priority = currentFields.get("priority").toString();
            
            if ("CRITICAL".equalsIgnoreCase(priority)) {
                return now.plusDays(2).toString(); // Critical items due very soon
            } else if ("HIGH".equalsIgnoreCase(priority)) {
                return now.plusDays(5).toString(); // High priority items due soon
            } else if ("LOW".equalsIgnoreCase(priority)) {
                return now.plusDays(15).toString(); // Low priority can wait longer
            }
        }
        
        // Adjust based on estimated effort if available
        if (currentFields.containsKey("estimatedEffort")) {
            Object effortObj = currentFields.get("estimatedEffort");
            if (effortObj instanceof Number) {
                double effort = ((Number) effortObj).doubleValue();
                
                // Add 1 day per estimated point/hour, up to reasonable limits
                int days = Math.min(20, Math.max(3, (int) Math.ceil(effort)));
                return now.plusDays(days).toString();
            }
        }
        
        // Adjust based on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type)) {
                return now.plusDays(7).toString(); // Bugs should be fixed relatively quickly
            } else if ("FEATURE".equalsIgnoreCase(type)) {
                return now.plusDays(14).toString(); // Features may take longer
            } else if ("TASK".equalsIgnoreCase(type)) {
                return now.plusDays(5).toString(); // Tasks are usually smaller
            }
        }
        
        // Default due date: 10 days from now
        return now.plusDays(10).toString();
    }
    
    /**
     * Predicts the estimated effort for a work item based on type, description, and other factors.
     *
     * @param currentFields The current field values
     * @return The predicted estimated effort
     */
    private Double predictEstimatedEffort(Map<String, Object> currentFields) {
        // Base estimate on the type of work
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type)) {
                // Check description complexity for bugs
                if (currentFields.containsKey("description")) {
                    String description = currentFields.get("description").toString();
                    int wordCount = description.split("\\s+").length;
                    
                    // More detailed bug reports often indicate more complex issues
                    if (wordCount > 200) {
                        return 8.0; // Complex bugs
                    } else if (wordCount > 100) {
                        return 5.0; // Moderate bugs
                    }
                }
                return 3.0; // Default for bugs
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                // Check for complexity indicators in features
                if (currentFields.containsKey("description")) {
                    String description = currentFields.get("description").toString().toLowerCase();
                    
                    // Look for signals of complexity
                    if (description.contains("complex") || 
                        description.contains("difficult") || 
                        description.contains("major change")) {
                        return 13.0; // Complex features
                    }
                }
                return 8.0; // Default for features
            }
            else if ("TASK".equalsIgnoreCase(type)) {
                return 3.0; // Standard tasks
            }
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                return 2.0; // Documentation typically takes less time
            }
        }
        
        // Consider cognitive load if available
        if (currentFields.containsKey("cognitiveLoad") && currentFields.get("cognitiveLoad") != null) {
            Object loadObj = currentFields.get("cognitiveLoad");
            if (loadObj instanceof Number) {
                int load = ((Number) loadObj).intValue();
                
                // Higher cognitive load often correlates with higher effort
                if (load >= 8) {
                    return 13.0; // Very complex
                } else if (load >= 5) {
                    return 8.0; // Moderately complex
                } else {
                    return 3.0; // Simpler
                }
            }
        }
        
        // Default estimate
        return 5.0;
    }
    
    /**
     * Predicts relevant tags for a work item based on its description, type, and other metadata.
     *
     * @param currentFields The current field values
     * @return A list of predicted tags
     */
    private List<String> predictTags(Map<String, Object> currentFields) {
        List<String> tags = new ArrayList<>();
        
        // Add tag based on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            tags.add(type.toLowerCase());
        }
        
        // Add tag based on priority
        if (currentFields.containsKey("priority")) {
            String priority = currentFields.get("priority").toString();
            tags.add("priority-" + priority.toLowerCase());
        }
        
        // Add tags based on cynefin domain if available
        if (currentFields.containsKey("cynefinDomain")) {
            Object domainObj = currentFields.get("cynefinDomain");
            if (domainObj != null) {
                tags.add("domain-" + domainObj.toString().toLowerCase());
            }
        }
        
        // Add tags based on work paradigm if available
        if (currentFields.containsKey("workParadigm")) {
            Object paradigmObj = currentFields.get("workParadigm");
            if (paradigmObj != null) {
                tags.add("paradigm-" + paradigmObj.toString().toLowerCase());
            }
        }
        
        // Extract potential tags from description
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            
            // Look for specific technical keywords
            if (description.contains("frontend") || description.contains("ui") || description.contains("interface")) {
                tags.add("frontend");
            }
            
            if (description.contains("backend") || description.contains("api") || description.contains("database")) {
                tags.add("backend");
            }
            
            if (description.contains("test") || description.contains("qa")) {
                tags.add("testing");
            }
            
            if (description.contains("security") || description.contains("auth")) {
                tags.add("security");
            }
            
            if (description.contains("performance") || description.contains("speed") || description.contains("optimize")) {
                tags.add("performance");
            }
            
            if (description.contains("refactor") || description.contains("technical debt")) {
                tags.add("refactoring");
            }
        }
        
        // Add project-related tags
        if (currentFields.containsKey("projectKey")) {
            String project = currentFields.get("projectKey").toString();
            tags.add("project-" + project.toLowerCase());
        }
        
        return tags;
    }
    
    /**
     * Predicts the cognitive load of a work item based on description complexity, type, and other factors.
     *
     * @param currentFields The current field values
     * @return The predicted cognitive load (1-10)
     */
    private Integer predictCognitiveLoad(Map<String, Object> currentFields) {
        int baseLoad = 5; // Start at middle of the scale (1-10)
        
        // Different types have different baseline complexity
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type)) {
                baseLoad = 6; // Bugs often require more investigation
            } else if ("FEATURE".equalsIgnoreCase(type)) {
                baseLoad = 7; // Features typically require more creative thinking
            } else if ("TASK".equalsIgnoreCase(type)) {
                baseLoad = 4; // Tasks are often more straightforward
            } else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                baseLoad = 3; // Documentation is usually less complex
            }
        }
        
        // Adjust based on cynefin domain if available
        if (currentFields.containsKey("cynefinDomain")) {
            Object domainObj = currentFields.get("cynefinDomain");
            if (domainObj != null) {
                String domain = domainObj.toString();
                
                if ("COMPLEX".equalsIgnoreCase(domain)) {
                    baseLoad += 2; // Complex domain adds cognitive load
                } else if ("COMPLICATED".equalsIgnoreCase(domain)) {
                    baseLoad += 1; // Complicated domain adds some load
                } else if ("CHAOTIC".equalsIgnoreCase(domain)) {
                    baseLoad += 3; // Chaotic domain adds significant load
                } else if ("SIMPLE".equalsIgnoreCase(domain)) {
                    baseLoad -= 1; // Simple domain reduces load
                }
            }
        }
        
        // Adjust based on description length/complexity if available
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString();
            int wordCount = description.split("\\s+").length;
            
            // Longer descriptions often indicate more complexity
            if (wordCount > 300) {
                baseLoad += 2;
            } else if (wordCount > 150) {
                baseLoad += 1;
            } else if (wordCount < 50) {
                baseLoad -= 1;
            }
            
            // Check for indicators of complexity in the description
            String lowerDesc = description.toLowerCase();
            if (lowerDesc.contains("complex") || 
                lowerDesc.contains("difficult") || 
                lowerDesc.contains("challenging")) {
                baseLoad += 1;
            }
        }
        
        // Ensure the load stays within bounds (1-10)
        return Math.min(10, Math.max(1, baseLoad));
    }
    
    /**
     * Predicts the Cynefin domain for a work item based on description, type, and other metadata.
     *
     * @param currentFields The current field values
     * @return The predicted Cynefin domain
     */
    private String predictCynefinDomain(Map<String, Object> currentFields) {
        // Look for explicit domain indicators in the description
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            
            // Complex domain indicators
            if (description.contains("uncertain") || 
                description.contains("unpredictable") || 
                description.contains("complex") || 
                description.contains("emergent") || 
                description.contains("multiple stakeholders") || 
                description.contains("dependencies") || 
                description.contains("experiment") || 
                description.contains("learning")) {
                return "COMPLEX";
            }
            
            // Complicated domain indicators
            if (description.contains("analysis") || 
                description.contains("expert") || 
                description.contains("specialized knowledge") || 
                description.contains("technical challenge") || 
                description.contains("requires expertise")) {
                return "COMPLICATED";
            }
            
            // Chaotic domain indicators
            if (description.contains("critical failure") || 
                description.contains("emergency") || 
                description.contains("immediate action") || 
                description.contains("crisis") || 
                description.contains("urgent") || 
                description.contains("fire-fighting")) {
                return "CHAOTIC";
            }
            
            // Simple domain indicators
            if (description.contains("straightforward") || 
                description.contains("routine") || 
                description.contains("well-known") || 
                description.contains("standard procedure") || 
                description.contains("best practice")) {
                return "SIMPLE";
            }
        }
        
        // Predict based on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type)) {
                // Check priority for bugs - critical bugs tend to be chaotic
                if (currentFields.containsKey("priority")) {
                    String priority = currentFields.get("priority").toString();
                    if ("CRITICAL".equalsIgnoreCase(priority)) {
                        return "CHAOTIC";
                    } else if ("HIGH".equalsIgnoreCase(priority)) {
                        return "COMPLICATED";
                    }
                }
                return "COMPLICATED"; // Default for bugs
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                return "COMPLEX"; // Features generally involve discovery
            }
            else if ("TASK".equalsIgnoreCase(type)) {
                return "SIMPLE"; // Tasks are typically well-defined
            }
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                return "SIMPLE"; // Documentation is often straightforward
            }
        }
        
        // Use cognitive load if available
        if (currentFields.containsKey("cognitiveLoad") && currentFields.get("cognitiveLoad") != null) {
            Object loadObj = currentFields.get("cognitiveLoad");
            if (loadObj instanceof Number) {
                int load = ((Number) loadObj).intValue();
                
                if (load >= 8) {
                    return "COMPLEX"; // High cognitive load suggests complexity
                } else if (load <= 3) {
                    return "SIMPLE"; // Low cognitive load suggests simplicity
                }
            }
        }
        
        // Default to COMPLICATED as a middle ground
        return "COMPLICATED";
    }
    
    /**
     * Predicts the origin category for a work item based on its description, type, and other metadata.
     *
     * @param currentFields The current field values
     * @return The predicted origin category
     */
    private String predictCategory(Map<String, Object> currentFields) {
        // Look for category indicators in the description
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            
            // PROD (Product) indicators
            if (description.contains("customer") || 
                description.contains("user experience") || 
                description.contains("ux") || 
                description.contains("product requirement") || 
                description.contains("feature request") || 
                description.contains("market")) {
                return "PROD";
            }
            
            // ARCH (Architecture) indicators
            if (description.contains("architecture") || 
                description.contains("design pattern") || 
                description.contains("system design") || 
                description.contains("technical foundation") || 
                description.contains("framework")) {
                return "ARCH";
            }
            
            // DEV (Development) indicators
            if (description.contains("implement") || 
                description.contains("code") || 
                description.contains("develop") || 
                description.contains("programming") || 
                description.contains("refactor") || 
                description.contains("function")) {
                return "DEV";
            }
            
            // TEST indicators
            if (description.contains("test") || 
                description.contains("qa") || 
                description.contains("quality") || 
                description.contains("validation") || 
                description.contains("verification") || 
                description.contains("assert")) {
                return "TEST";
            }
            
            // OPS (Operations) indicators
            if (description.contains("deploy") || 
                description.contains("infrastructure") || 
                description.contains("monitoring") || 
                description.contains("performance") || 
                description.contains("server") || 
                description.contains("devops")) {
                return "OPS";
            }
            
            // DOC (Documentation) indicators
            if (description.contains("document") || 
                description.contains("explain") || 
                description.contains("instructions") || 
                description.contains("guide") || 
                description.contains("manual")) {
                return "DOC";
            }
            
            // CROSS (Cross-functional) indicators
            if (description.contains("multiple teams") || 
                description.contains("cross-functional") || 
                description.contains("coordination") || 
                description.contains("integration") || 
                description.contains("collaboration")) {
                return "CROSS";
            }
        }
        
        // Predict based on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type)) {
                return "DEV"; // Bugs are typically development work
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                return "PROD"; // Features are typically product-driven
            }
            else if ("TASK".equalsIgnoreCase(type) && currentFields.containsKey("tags")) {
                // Look at tags for clues about the category
                Object tagsObj = currentFields.get("tags");
                if (tagsObj instanceof Collection) {
                    Collection<?> tags = (Collection<?>) tagsObj;
                    
                    if (tags.contains("test") || tags.contains("qa")) {
                        return "TEST";
                    } else if (tags.contains("devops") || tags.contains("infrastructure")) {
                        return "OPS";
                    } else if (tags.contains("design") || tags.contains("architecture")) {
                        return "ARCH";
                    } else if (tags.contains("documentation")) {
                        return "DOC";
                    }
                }
                
                return "DEV"; // Default for tasks
            }
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                return "DOC";
            }
        }
        
        // Default to DEV as most common category
        return "DEV";
    }
    
    /**
     * Predicts the work paradigm for a work item based on its description, type, and other metadata.
     *
     * @param currentFields The current field values
     * @return The predicted work paradigm
     */
    private String predictWorkParadigm(Map<String, Object> currentFields) {
        // Look for paradigm indicators in the description
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            
            // PROJECT paradigm indicators
            if (description.contains("project") || 
                description.contains("milestone") || 
                description.contains("initiative") || 
                description.contains("timeline") || 
                description.contains("deliverable")) {
                return "PROJECT";
            }
            
            // OPERATIONAL paradigm indicators
            if (description.contains("ongoing") || 
                description.contains("maintenance") || 
                description.contains("support") || 
                description.contains("operating") || 
                description.contains("routine") || 
                description.contains("continuous")) {
                return "OPERATIONAL";
            }
            
            // EXPLORATORY paradigm indicators
            if (description.contains("research") || 
                description.contains("innovation") || 
                description.contains("experiment") || 
                description.contains("explore") || 
                description.contains("prototype") || 
                description.contains("discovery")) {
                return "EXPLORATORY";
            }
            
            // GOVERNANCE paradigm indicators
            if (description.contains("compliance") || 
                description.contains("regulation") || 
                description.contains("policy") || 
                description.contains("governance") || 
                description.contains("standard") || 
                description.contains("audit")) {
                return "GOVERNANCE";
            }
        }
        
        // Predict based on category if available
        if (currentFields.containsKey("category")) {
            String category = currentFields.get("category").toString();
            
            if ("ARCH".equalsIgnoreCase(category) || "PROD".equalsIgnoreCase(category)) {
                return "PROJECT"; // Architecture and Product work often aligns with project paradigm
            } 
            else if ("OPS".equalsIgnoreCase(category)) {
                return "OPERATIONAL"; // Operations work is typically operational
            }
            else if ("TEST".equalsIgnoreCase(category)) {
                // Testing can be project or operational depending on context
                return "PROJECT";
            }
        }
        
        // Predict based on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type)) {
                return "OPERATIONAL"; // Bug fixing is typically operational
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                return "PROJECT"; // Features are typically project-based
            }
            else if ("TASK".equalsIgnoreCase(type)) {
                // Tasks can be any paradigm, but project is most common
                return "PROJECT";
            }
        }
        
        // Predict based on cynefin domain if available
        if (currentFields.containsKey("cynefinDomain")) {
            String domain = currentFields.get("cynefinDomain").toString();
            
            if ("COMPLEX".equalsIgnoreCase(domain)) {
                return "EXPLORATORY"; // Complex problems often require exploration
            } 
            else if ("SIMPLE".equalsIgnoreCase(domain)) {
                return "OPERATIONAL"; // Simple problems are often operational
            }
            else if ("COMPLICATED".equalsIgnoreCase(domain)) {
                return "PROJECT"; // Complicated problems often fit project paradigm
            }
        }
        
        // Default to PROJECT as most common paradigm
        return "PROJECT";
    }
    
    /**
     * Predicts the completion percentage for a work item based on its status and other metadata.
     *
     * @param currentFields The current field values
     * @return The predicted completion percentage (0-100)
     */
    private Integer predictCompletionPercentage(Map<String, Object> currentFields) {
        // Use status as primary indicator if available
        if (currentFields.containsKey("status")) {
            String status = currentFields.get("status").toString();
            
            if ("BACKLOG".equalsIgnoreCase(status)) {
                return 0; // Items in backlog haven't started
            } 
            else if ("TO_DO".equalsIgnoreCase(status)) {
                return 0; // To-do items haven't started
            }
            else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                return 50; // In progress items are roughly half done
            }
            else if ("REVIEW".equalsIgnoreCase(status)) {
                return 80; // Review items are mostly done
            }
            else if ("DONE".equalsIgnoreCase(status)) {
                return 100; // Done items are complete
            }
            else if ("BLOCKED".equalsIgnoreCase(status)) {
                // Blocked items vary - check if there was previous progress
                return 30; // Assume some progress before being blocked
            }
        }
        
        // Look at due date vs. current date if available
        if (currentFields.containsKey("dueDate") && currentFields.get("dueDate") != null) {
            try {
                LocalDateTime dueDate = LocalDateTime.parse(currentFields.get("dueDate").toString());
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdAt = currentFields.containsKey("createdAt") ? 
                    LocalDateTime.parse(currentFields.get("createdAt").toString()) : 
                    now.minusDays(10); // Assume created 10 days ago if unknown
                
                // Calculate progress based on timeline
                long totalDuration = java.time.Duration.between(createdAt, dueDate).toDays();
                long elapsedDuration = java.time.Duration.between(createdAt, now).toDays();
                
                if (totalDuration > 0) {
                    // Calculate percentage based on elapsed time
                    int timeBasedPercentage = (int) (elapsedDuration * 100 / totalDuration);
                    
                    // Cap at 90% since time alone doesn't indicate completion
                    return Math.min(90, Math.max(0, timeBasedPercentage));
                }
            } catch (Exception e) {
                // If parsing fails, fall back to other methods
                logger.debug("Failed to parse dates for completion percentage prediction", e);
            }
        }
        
        // Default based on type if no other indicators
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type) || "TASK".equalsIgnoreCase(type)) {
                return 20; // Assume some initial progress for bugs and tasks
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                return 10; // Features typically start with less progress
            }
        }
        
        // Default if no other indicators
        return 0;
    }
    
    /**
     * Predicts an appropriate outcome description for a work item based on its type and description.
     *
     * @param currentFields The current field values
     * @return The predicted outcome description
     */
    private String predictOutcome(Map<String, Object> currentFields) {
        StringBuilder outcome = new StringBuilder();
        
        // Base outcome on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            
            if ("BUG".equalsIgnoreCase(type)) {
                outcome.append("Issue is resolved and verified. ");
                
                // Add specifics from description if available
                if (currentFields.containsKey("description")) {
                    String description = currentFields.get("description").toString();
                    
                    // Extract key phrases to include in outcome
                    if (description.contains("crash") || description.contains("exception")) {
                        outcome.append("Application no longer crashes or throws exceptions. ");
                    }
                    if (description.contains("data") || description.contains("corruption")) {
                        outcome.append("Data integrity is restored and verified. ");
                    }
                    if (description.contains("performance") || description.contains("slow")) {
                        outcome.append("Performance is restored to expected levels. ");
                    }
                    if (description.contains("security")) {
                        outcome.append("Security vulnerability is patched and verified. ");
                    }
                }
                
                outcome.append("Automated tests verify the fix and prevent regression.");
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                outcome.append("Feature is implemented according to requirements, fully tested, and ready for release. ");
                
                // Add specifics from description if available
                if (currentFields.containsKey("description")) {
                    String description = currentFields.get("description").toString();
                    
                    // Extract key phrases to include in outcome
                    if (description.contains("user") || description.contains("experience")) {
                        outcome.append("Users can successfully complete the intended workflow. ");
                    }
                    if (description.contains("performance") || description.contains("speed")) {
                        outcome.append("Performance metrics meet or exceed expectations. ");
                    }
                    if (description.contains("integration")) {
                        outcome.append("Integration with existing systems is verified. ");
                    }
                }
                
                outcome.append("Documentation is updated and the feature is demonstrated to stakeholders.");
            }
            else if ("TASK".equalsIgnoreCase(type)) {
                outcome.append("Task is completed successfully and all acceptance criteria are met. ");
                
                // Add specifics based on category if available
                if (currentFields.containsKey("category")) {
                    String category = currentFields.get("category").toString();
                    
                    if ("DEV".equalsIgnoreCase(category)) {
                        outcome.append("Code changes are reviewed, tested, and merged. ");
                    }
                    else if ("OPS".equalsIgnoreCase(category)) {
                        outcome.append("Operational metrics confirm successful implementation. ");
                    }
                    else if ("TEST".equalsIgnoreCase(category)) {
                        outcome.append("Test coverage meets requirements and all tests pass. ");
                    }
                    else if ("DOC".equalsIgnoreCase(category)) {
                        outcome.append("Documentation is reviewed, approved, and published. ");
                    }
                }
            }
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                outcome.append("Documentation is complete, accurate, and accessible to the intended audience. ");
                outcome.append("Content is reviewed for clarity and correctness, and published in the appropriate location.");
            }
        }
        
        // If we couldn't generate a specific outcome, use a generic one
        if (outcome.length() == 0) {
            outcome.append("Work is completed successfully according to requirements and accepted by stakeholders.");
        }
        
        return outcome.toString();
    }
    
    // Evidence factor methods
    
    private void addStatusFactors(Set<String> factors, Map<String, Object> currentFields) {
        if (currentFields.containsKey("type")) {
            factors.add("status_based_on_type");
            
            // Add detailed type-specific factors
            String type = currentFields.get("type").toString();
            factors.add("item_type_" + type.toLowerCase());
        }
        
        if (currentFields.containsKey("priority")) {
            factors.add("status_influenced_by_priority");
            
            // High/critical priority often means immediate action
            String priority = currentFields.get("priority").toString();
            if ("HIGH".equalsIgnoreCase(priority) || "CRITICAL".equalsIgnoreCase(priority)) {
                factors.add("high_priority_needs_attention");
            }
        }
        
        // Check for dependencies
        if (currentFields.containsKey("dependencies") && currentFields.get("dependencies") != null) {
            Object deps = currentFields.get("dependencies");
            if (deps instanceof Collection && !((Collection<?>) deps).isEmpty()) {
                factors.add("has_dependencies_may_block");
            }
        }
    }
    
    private void addPriorityFactors(Set<String> factors, Map<String, Object> currentFields) {
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("priority_based_on_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                factors.add("bug_gets_high_priority");
            } else if ("FEATURE".equalsIgnoreCase(type)) {
                factors.add("feature_gets_medium_priority");
            } else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                factors.add("documentation_gets_low_priority");
            }
        }
        
        // Check description for priority signals
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            
            if (description.contains("crash") || description.contains("security") || 
                description.contains("data loss") || description.contains("urgent")) {
                factors.add("critical_keywords_in_description");
            }
            
            if (description.contains("important") || description.contains("significant")) {
                factors.add("importance_keywords_in_description");
            }
        }
        
        // Check for related items
        if (currentFields.containsKey("relatedItems") && currentFields.get("relatedItems") != null) {
            factors.add("priority_influenced_by_related_items");
        }
    }
    
    private void addAssigneeFactors(Set<String> factors, Map<String, Object> currentFields) {
        if (currentFields.containsKey("reporter")) {
            factors.add("reporter_is_assignee");
        }
        
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("assignee_based_on_item_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                factors.add("bug_assigned_to_dev_team");
            } else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                factors.add("documentation_assigned_to_doc_team");
            }
        }
        
        // Check tags for team indicators
        if (currentFields.containsKey("tags") && currentFields.get("tags") != null) {
            Object tagsObj = currentFields.get("tags");
            if (tagsObj instanceof Collection) {
                Collection<?> tags = (Collection<?>) tagsObj;
                
                if (tags.contains("backend")) {
                    factors.add("backend_tag_suggests_backend_team");
                } else if (tags.contains("frontend")) {
                    factors.add("frontend_tag_suggests_frontend_team");
                } else if (tags.contains("design")) {
                    factors.add("design_tag_suggests_design_team");
                }
            }
        }
    }
    
    private void addDueDateFactors(Set<String> factors, Map<String, Object> currentFields) {
        // Basic due date factor
        factors.add("standard_due_date_applied");
        
        // Due date influenced by priority
        if (currentFields.containsKey("priority")) {
            String priority = currentFields.get("priority").toString();
            factors.add("due_date_influenced_by_priority");
            
            if ("CRITICAL".equalsIgnoreCase(priority)) {
                factors.add("critical_priority_short_deadline");
            } else if ("HIGH".equalsIgnoreCase(priority)) {
                factors.add("high_priority_medium_deadline");
            } else if ("LOW".equalsIgnoreCase(priority)) {
                factors.add("low_priority_extended_deadline");
            }
        }
        
        // Due date influenced by estimated effort
        if (currentFields.containsKey("estimatedEffort")) {
            factors.add("due_date_based_on_estimated_effort");
        }
        
        // Due date influenced by type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("due_date_influenced_by_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                factors.add("bug_needs_quicker_resolution");
            } else if ("FEATURE".equalsIgnoreCase(type)) {
                factors.add("feature_has_longer_timeframe");
            }
        }
    }
    
    private void addEstimatedEffortFactors(Set<String> factors, Map<String, Object> currentFields) {
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("effort_based_on_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                factors.add("bug_standard_effort");
            } else if ("FEATURE".equalsIgnoreCase(type)) {
                factors.add("feature_higher_effort");
            } else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                factors.add("documentation_lower_effort");
            }
        }
        
        // Check description for complexity signals
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            factors.add("effort_influenced_by_description");
            
            // Estimate complexity from description length
            int wordCount = description.split("\\s+").length;
            if (wordCount > 200) {
                factors.add("long_description_suggests_complexity");
            } else if (wordCount < 50) {
                factors.add("short_description_suggests_simplicity");
            }
            
            // Keywords suggesting complexity
            if (description.contains("complex") || description.contains("difficult")) {
                factors.add("complexity_keywords_detected");
            }
        }
        
        // Consider cognitive load if available
        if (currentFields.containsKey("cognitiveLoad") && currentFields.get("cognitiveLoad") != null) {
            factors.add("effort_influenced_by_cognitive_load");
        }
    }
    
    private void addTagsFactors(Set<String> factors, Map<String, Object> currentFields) {
        if (currentFields.containsKey("type")) {
            factors.add("tag_based_on_type");
        }
        
        if (currentFields.containsKey("priority")) {
            factors.add("tag_based_on_priority");
        }
        
        if (currentFields.containsKey("cynefinDomain")) {
            factors.add("tag_based_on_cynefin_domain");
        }
        
        if (currentFields.containsKey("workParadigm")) {
            factors.add("tag_based_on_work_paradigm");
        }
        
        if (currentFields.containsKey("projectKey")) {
            factors.add("tag_includes_project");
        }
        
        // Check description for technical keywords
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            factors.add("tags_extracted_from_description");
            
            if (description.contains("frontend") || description.contains("ui")) {
                factors.add("frontend_keyword_detected");
            }
            
            if (description.contains("backend") || description.contains("api")) {
                factors.add("backend_keyword_detected");
            }
            
            if (description.contains("test") || description.contains("qa")) {
                factors.add("testing_keyword_detected");
            }
            
            if (description.contains("security")) {
                factors.add("security_keyword_detected");
            }
            
            if (description.contains("performance") || description.contains("optimize")) {
                factors.add("performance_keyword_detected");
            }
        }
    }
    
    private void addCognitiveLoadFactors(Set<String> factors, Map<String, Object> currentFields) {
        // Base cognitive load on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("cognitive_load_based_on_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                factors.add("bug_higher_investigation_load");
            } else if ("FEATURE".equalsIgnoreCase(type)) {
                factors.add("feature_higher_creative_load");
            } else if ("TASK".equalsIgnoreCase(type)) {
                factors.add("task_moderate_cognitive_load");
            } else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                factors.add("documentation_lower_cognitive_load");
            }
        }
        
        // Consider cynefin domain for complexity assessment
        if (currentFields.containsKey("cynefinDomain")) {
            String domain = currentFields.get("cynefinDomain").toString();
            factors.add("load_influenced_by_cynefin_domain");
            
            if ("COMPLEX".equalsIgnoreCase(domain)) {
                factors.add("complex_domain_high_cognitive_load");
            } else if ("COMPLICATED".equalsIgnoreCase(domain)) {
                factors.add("complicated_domain_moderate_cognitive_load");
            } else if ("CHAOTIC".equalsIgnoreCase(domain)) {
                factors.add("chaotic_domain_very_high_cognitive_load");
            } else if ("SIMPLE".equalsIgnoreCase(domain)) {
                factors.add("simple_domain_low_cognitive_load");
            }
        }
        
        // Check description complexity
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString();
            factors.add("load_influenced_by_description_complexity");
            
            int wordCount = description.split("\\s+").length;
            if (wordCount > 300) {
                factors.add("very_detailed_description_high_load");
            } else if (wordCount > 150) {
                factors.add("detailed_description_moderate_load");
            } else if (wordCount < 50) {
                factors.add("brief_description_lower_load");
            }
            
            // Check for complexity terms
            String lowerDesc = description.toLowerCase();
            if (lowerDesc.contains("complex") || lowerDesc.contains("difficult")) {
                factors.add("complexity_terms_in_description");
            }
        }
    }
    
    private void addCynefinDomainFactors(Set<String> factors, Map<String, Object> currentFields) {
        // Check description for domain indicators
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            factors.add("domain_classified_from_description");
            
            // Complex domain indicators
            if (description.contains("uncertain") || description.contains("unpredictable") || 
                description.contains("emergent") || description.contains("experiment")) {
                factors.add("complex_domain_keywords_detected");
            }
            
            // Complicated domain indicators
            if (description.contains("analysis") || description.contains("expert") || 
                description.contains("specialized knowledge")) {
                factors.add("complicated_domain_keywords_detected");
            }
            
            // Chaotic domain indicators
            if (description.contains("emergency") || description.contains("immediate action") || 
                description.contains("crisis") || description.contains("urgent")) {
                factors.add("chaotic_domain_keywords_detected");
            }
            
            // Simple domain indicators
            if (description.contains("straightforward") || description.contains("routine") || 
                description.contains("well-known")) {
                factors.add("simple_domain_keywords_detected");
            }
        }
        
        // Consider type and priority for domain classification
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("domain_influenced_by_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                // High priority bugs are often chaotic
                if (currentFields.containsKey("priority")) {
                    String priority = currentFields.get("priority").toString();
                    if ("CRITICAL".equalsIgnoreCase(priority)) {
                        factors.add("critical_bug_suggests_chaotic_domain");
                    } else if ("HIGH".equalsIgnoreCase(priority)) {
                        factors.add("high_priority_bug_suggests_complicated_domain");
                    }
                }
                
                factors.add("bug_suggests_complicated_domain");
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                factors.add("feature_suggests_complex_domain");
            }
            else if ("TASK".equalsIgnoreCase(type)) {
                factors.add("task_suggests_simple_domain");
            }
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                factors.add("documentation_suggests_simple_domain");
            }
        }
        
        // Use cognitive load as an indicator if available
        if (currentFields.containsKey("cognitiveLoad") && currentFields.get("cognitiveLoad") != null) {
            Object loadObj = currentFields.get("cognitiveLoad");
            if (loadObj instanceof Number) {
                int load = ((Number) loadObj).intValue();
                factors.add("domain_influenced_by_cognitive_load");
                
                if (load >= 8) {
                    factors.add("high_cognitive_load_suggests_complex_domain");
                } else if (load <= 3) {
                    factors.add("low_cognitive_load_suggests_simple_domain");
                }
            }
        }
    }
    
    private void addCategoryFactors(Set<String> factors, Map<String, Object> currentFields) {
        // Check description for category indicators
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            factors.add("category_classified_from_description");
            
            // PROD indicators
            if (description.contains("customer") || description.contains("user experience") || 
                description.contains("product requirement")) {
                factors.add("product_keywords_detected");
            }
            
            // ARCH indicators
            if (description.contains("architecture") || description.contains("design pattern") || 
                description.contains("system design")) {
                factors.add("architecture_keywords_detected");
            }
            
            // DEV indicators
            if (description.contains("implement") || description.contains("code") || 
                description.contains("develop")) {
                factors.add("development_keywords_detected");
            }
            
            // TEST indicators
            if (description.contains("test") || description.contains("qa") || 
                description.contains("validation")) {
                factors.add("testing_keywords_detected");
            }
            
            // OPS indicators
            if (description.contains("deploy") || description.contains("infrastructure") || 
                description.contains("monitoring")) {
                factors.add("operations_keywords_detected");
            }
            
            // DOC indicators
            if (description.contains("document") || description.contains("explain") || 
                description.contains("guide")) {
                factors.add("documentation_keywords_detected");
            }
            
            // CROSS indicators
            if (description.contains("multiple teams") || description.contains("cross-functional") || 
                description.contains("coordination")) {
                factors.add("cross_functional_keywords_detected");
            }
        }
        
        // Predict based on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("category_influenced_by_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                factors.add("bug_suggests_dev_category");
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                factors.add("feature_suggests_prod_category");
            }
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                factors.add("documentation_type_suggests_doc_category");
            }
        }
        
        // Check tags for category indicators
        if (currentFields.containsKey("tags") && currentFields.get("tags") != null) {
            Object tagsObj = currentFields.get("tags");
            if (tagsObj instanceof Collection) {
                Collection<?> tags = (Collection<?>) tagsObj;
                factors.add("category_influenced_by_tags");
                
                if (tags.contains("test") || tags.contains("qa")) {
                    factors.add("testing_tag_suggests_test_category");
                } else if (tags.contains("devops") || tags.contains("infrastructure")) {
                    factors.add("devops_tag_suggests_ops_category");
                } else if (tags.contains("design") || tags.contains("architecture")) {
                    factors.add("design_tag_suggests_arch_category");
                } else if (tags.contains("documentation")) {
                    factors.add("documentation_tag_suggests_doc_category");
                }
            }
        }
    }
    
    private void addWorkParadigmFactors(Set<String> factors, Map<String, Object> currentFields) {
        // Check description for paradigm indicators
        if (currentFields.containsKey("description")) {
            String description = currentFields.get("description").toString().toLowerCase();
            factors.add("paradigm_classified_from_description");
            
            // PROJECT paradigm indicators
            if (description.contains("project") || description.contains("milestone") || 
                description.contains("initiative") || description.contains("deliverable")) {
                factors.add("project_keywords_detected");
            }
            
            // OPERATIONAL paradigm indicators
            if (description.contains("ongoing") || description.contains("maintenance") || 
                description.contains("support") || description.contains("routine")) {
                factors.add("operational_keywords_detected");
            }
            
            // EXPLORATORY paradigm indicators
            if (description.contains("research") || description.contains("innovation") || 
                description.contains("experiment") || description.contains("prototype")) {
                factors.add("exploratory_keywords_detected");
            }
            
            // GOVERNANCE paradigm indicators
            if (description.contains("compliance") || description.contains("regulation") || 
                description.contains("policy") || description.contains("governance")) {
                factors.add("governance_keywords_detected");
            }
        }
        
        // Predict based on category if available
        if (currentFields.containsKey("category")) {
            String category = currentFields.get("category").toString();
            factors.add("paradigm_influenced_by_category");
            
            if ("ARCH".equalsIgnoreCase(category) || "PROD".equalsIgnoreCase(category)) {
                factors.add("arch_or_prod_suggests_project_paradigm");
            } 
            else if ("OPS".equalsIgnoreCase(category)) {
                factors.add("ops_suggests_operational_paradigm");
            }
        }
        
        // Predict based on type
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("paradigm_influenced_by_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                factors.add("bug_suggests_operational_paradigm");
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                factors.add("feature_suggests_project_paradigm");
            }
        }
        
        // Predict based on cynefin domain if available
        if (currentFields.containsKey("cynefinDomain")) {
            String domain = currentFields.get("cynefinDomain").toString();
            factors.add("paradigm_influenced_by_cynefin_domain");
            
            if ("COMPLEX".equalsIgnoreCase(domain)) {
                factors.add("complex_domain_suggests_exploratory_paradigm");
            } 
            else if ("SIMPLE".equalsIgnoreCase(domain)) {
                factors.add("simple_domain_suggests_operational_paradigm");
            }
            else if ("COMPLICATED".equalsIgnoreCase(domain)) {
                factors.add("complicated_domain_suggests_project_paradigm");
            }
        }
    }
    
    private void addCompletionPercentageFactors(Set<String> factors, Map<String, Object> currentFields) {
        // Status is primary indicator of completion
        if (currentFields.containsKey("status")) {
            String status = currentFields.get("status").toString();
            factors.add("completion_based_on_status");
            
            if ("BACKLOG".equalsIgnoreCase(status) || "TO_DO".equalsIgnoreCase(status)) {
                factors.add("backlog_or_todo_zero_completion");
            } 
            else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                factors.add("in_progress_partial_completion");
            }
            else if ("REVIEW".equalsIgnoreCase(status)) {
                factors.add("review_mostly_complete");
            }
            else if ("DONE".equalsIgnoreCase(status)) {
                factors.add("done_fully_complete");
            }
            else if ("BLOCKED".equalsIgnoreCase(status)) {
                factors.add("blocked_partial_completion");
            }
        }
        
        // Timeline analysis if dates are available
        if (currentFields.containsKey("dueDate") && currentFields.containsKey("createdAt")) {
            factors.add("completion_estimated_from_timeline");
        }
        
        // Type can provide baseline expectations
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("completion_influenced_by_type");
        }
    }
    
    private void addOutcomeFactors(Set<String> factors, Map<String, Object> currentFields) {
        // Type is primary driver of outcome template
        if (currentFields.containsKey("type")) {
            String type = currentFields.get("type").toString();
            factors.add("outcome_based_on_type");
            
            if ("BUG".equalsIgnoreCase(type)) {
                factors.add("bug_outcome_is_resolution");
                
                // Add specific outcome factors based on description
                if (currentFields.containsKey("description")) {
                    String description = currentFields.get("description").toString();
                    
                    if (description.contains("crash") || description.contains("exception")) {
                        factors.add("crash_resolution_in_outcome");
                    }
                    if (description.contains("data") || description.contains("corruption")) {
                        factors.add("data_integrity_in_outcome");
                    }
                    if (description.contains("performance") || description.contains("slow")) {
                        factors.add("performance_restoration_in_outcome");
                    }
                    if (description.contains("security")) {
                        factors.add("security_fix_in_outcome");
                    }
                }
            } 
            else if ("FEATURE".equalsIgnoreCase(type)) {
                factors.add("feature_outcome_is_implementation");
                
                // Add specific outcome factors based on description
                if (currentFields.containsKey("description")) {
                    String description = currentFields.get("description").toString();
                    
                    if (description.contains("user") || description.contains("experience")) {
                        factors.add("user_experience_in_outcome");
                    }
                    if (description.contains("performance") || description.contains("speed")) {
                        factors.add("performance_metrics_in_outcome");
                    }
                    if (description.contains("integration")) {
                        factors.add("integration_verification_in_outcome");
                    }
                }
            }
            else if ("TASK".equalsIgnoreCase(type)) {
                factors.add("task_outcome_is_completion");
                
                // Add category-specific outcome factors
                if (currentFields.containsKey("category")) {
                    String category = currentFields.get("category").toString();
                    
                    if ("DEV".equalsIgnoreCase(category)) {
                        factors.add("dev_task_code_review_in_outcome");
                    }
                    else if ("OPS".equalsIgnoreCase(category)) {
                        factors.add("ops_task_metrics_in_outcome");
                    }
                    else if ("TEST".equalsIgnoreCase(category)) {
                        factors.add("test_task_coverage_in_outcome");
                    }
                    else if ("DOC".equalsIgnoreCase(category)) {
                        factors.add("doc_task_publishing_in_outcome");
                    }
                }
            }
            else if ("DOCUMENTATION".equalsIgnoreCase(type)) {
                factors.add("documentation_outcome_is_publication");
            }
        }
    }
}