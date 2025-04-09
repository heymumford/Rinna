/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.performance.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rinna.base.PerformanceTest;
import org.rinna.cli.command.GrepCommand;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.Priority;
import org.rinna.domain.SearchResult;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.performance.benchmark.PerformanceBenchmark;
import org.rinna.performance.benchmark.TimingResult;
import org.rinna.service.DefaultSearchService;
import org.rinna.service.InMemoryItemService;
import org.rinna.service.ItemServiceFactory;
import org.rinna.service.SearchServiceFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the GrepCommand class.
 * These tests evaluate the performance characteristics of the grep command.
 */
@DisplayName("GrepCommand Performance Tests")
public class GrepCommandPerformanceTest extends PerformanceTest {
    
    private InMemoryItemService itemService;
    private DefaultSearchService searchService;
    
    private List<UUID> testItemIds;
    private Random random = new Random();
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    // Number of work items to create for performance testing
    private static final int SMALL_DATASET = 100;
    private static final int MEDIUM_DATASET = 1000;
    private static final int LARGE_DATASET = 5000;
    
    // Performance thresholds (in milliseconds)
    private static final long ACCEPTABLE_SMALL_DATASET_TIME = 50;
    private static final long ACCEPTABLE_MEDIUM_DATASET_TIME = 200;
    private static final long ACCEPTABLE_LARGE_DATASET_TIME = 500;
    
    // Data provider for performance tests with different data sizes
    static Stream<Arguments> datasetSizes() {
        return Stream.of(
            Arguments.of(SMALL_DATASET, ACCEPTABLE_SMALL_DATASET_TIME),
            Arguments.of(MEDIUM_DATASET, ACCEPTABLE_MEDIUM_DATASET_TIME),
            Arguments.of(LARGE_DATASET, ACCEPTABLE_LARGE_DATASET_TIME)
        );
    }
    
    // Data provider for testing different search patterns
    static Stream<Arguments> searchPatterns() {
        return Stream.of(
            // pattern, datasetSize, expectedMaxTime
            Arguments.of("common", MEDIUM_DATASET, ACCEPTABLE_MEDIUM_DATASET_TIME),
            Arguments.of("rare", MEDIUM_DATASET, ACCEPTABLE_MEDIUM_DATASET_TIME),
            Arguments.of("nonexistent", MEDIUM_DATASET, ACCEPTABLE_MEDIUM_DATASET_TIME),
            Arguments.of("a.*b.*c", MEDIUM_DATASET, ACCEPTABLE_MEDIUM_DATASET_TIME * 2) // Regex is slower
        );
    }
    
    @BeforeEach
    void setUp() {
        // Redirect System.out to avoid cluttering test output
        System.setOut(new PrintStream(outputCaptor));
        
        // Create real services with in-memory storage
        itemService = (InMemoryItemService) ItemServiceFactory.createItemService();
        searchService = (DefaultSearchService) SearchServiceFactory.createSearchService();
        
        // Register services with ServiceManager
        ServiceRegistry.registerService(ItemService.class, itemService);
        ServiceRegistry.registerService(SearchService.class, searchService);
        
        // Create default small test dataset
        createTestDataset(SMALL_DATASET);
    }
    
    void tearDown() {
        System.setOut(originalOut);
        
        // Clear test data
        itemService.clearAll();
    }
    
    /**
     * Creates a test dataset of the specified size.
     *
     * @param size the number of work items to create
     */
    private void createTestDataset(int size) {
        testItemIds = new ArrayList<>(size);
        
        // Content generation templates
        String[] titleTemplates = {
            "API %s implementation task",
            "UI component for %s screen",
            "Database schema for %s",
            "Documentation for %s feature",
            "Create %s workflow",
            "Testing framework for %s",
            "Performance optimization of %s",
            "Security review of %s component",
            "Fix %s validation bug",
            "Improve %s error handling"
        };
        
        String[] descriptionTemplates = {
            "Implement the %s feature with proper error handling and validation",
            "Create comprehensive documentation for the %s API endpoints",
            "Design and implement a database schema for storing %s data",
            "Develop UI components for the %s section of the application",
            "Optimize performance of the %s operation to improve response time",
            "Fix critical bug in the %s validation routine that causes errors",
            "Create unit and integration tests for the %s functionality",
            "Review and improve security measures for the %s component",
            "Refactor the %s code to follow best practices and improve maintainability",
            "Add monitoring and logging to the %s service for better observability"
        };
        
        String[] features = {
            "user management", "authentication", "authorization", "reporting", "dashboard",
            "analytics", "notification", "messaging", "payment", "billing", "invoice",
            "subscription", "customer", "product", "order", "shipping", "inventory",
            "logging", "monitoring", "backup", "recovery", "search", "filter", "sorting",
            "pagination", "export", "import", "integration", "api", "common"
        };
        
        // Create work items
        for (int i = 0; i < size; i++) {
            String feature = features[i % features.length];
            
            // Insert "common" term in some work items for consistent searching
            if (i % 10 == 0) {
                feature = "common " + feature;
            }
            
            // Insert "rare" term in few work items
            if (i % 100 == 0) {
                feature = "rare " + feature;
            }
            
            // Create item with randomized metadata
            String title = String.format(
                    titleTemplates[i % titleTemplates.length], 
                    feature);
            
            String description = String.format(
                    descriptionTemplates[i % descriptionTemplates.length], 
                    feature);
            
            WorkItemType type = WorkItemType.values()[i % WorkItemType.values().length];
            Priority priority = Priority.values()[i % Priority.values().length];
            String assignee = "dev" + (i % 10);
            
            UUID itemId = itemService.createWorkItem(title, description, type, priority, assignee);
            testItemIds.add(itemId);
            
            // Set random state
            WorkflowState state = WorkflowState.values()[i % WorkflowState.values().length];
            itemService.updateState(itemId, state, "system");
        }
    }
    
    @Test
    @DisplayName("GrepCommand basic performance test")
    void grepCommandBasicPerformanceTest() {
        // Create command
        GrepCommand grepCommand = new GrepCommand();
        grepCommand.setPattern("common");
        
        // Measure execution time
        PerformanceBenchmark benchmark = new PerformanceBenchmark();
        TimingResult result = benchmark.measureExecutionTime(() -> {
            grepCommand.call();
            return null;
        });
        
        // Verify acceptable performance
        assertTrue(result.getExecutionTimeMs() < ACCEPTABLE_SMALL_DATASET_TIME,
                "Basic grep should complete within " + ACCEPTABLE_SMALL_DATASET_TIME + "ms on small dataset");
        
        // Log performance metrics
        logPerformanceMetric("grep_basic", result.getExecutionTimeMs());
    }
    
    @ParameterizedTest
    @MethodSource("datasetSizes")
    @DisplayName("GrepCommand should scale linearly with dataset size")
    void grepCommandShouldScaleLinearlyWithDatasetSize(int datasetSize, long expectedMaxTime) {
        // Create dataset of specified size
        createTestDataset(datasetSize);
        
        // Create command
        GrepCommand grepCommand = new GrepCommand();
        grepCommand.setPattern("common");
        
        // Measure execution time
        PerformanceBenchmark benchmark = new PerformanceBenchmark();
        TimingResult result = benchmark.measureExecutionTime(() -> {
            grepCommand.call();
            return null;
        });
        
        // Verify performance within expected range
        assertTrue(result.getExecutionTimeMs() < expectedMaxTime,
                "Grep should complete within " + expectedMaxTime + "ms on dataset size " + datasetSize);
        
        // Log performance metrics by dataset size
        logPerformanceMetric("grep_dataset_" + datasetSize, result.getExecutionTimeMs());
    }
    
    @ParameterizedTest
    @MethodSource("searchPatterns")
    @DisplayName("GrepCommand performance with different search patterns")
    void grepCommandPerformanceWithDifferentSearchPatterns(
            String pattern, int datasetSize, long expectedMaxTime) {
        
        // Create dataset of specified size
        createTestDataset(datasetSize);
        
        // Create command
        GrepCommand grepCommand = new GrepCommand();
        grepCommand.setPattern(pattern);
        
        // Measure execution time
        PerformanceBenchmark benchmark = new PerformanceBenchmark();
        TimingResult result = benchmark.measureExecutionTime(() -> {
            grepCommand.call();
            return null;
        });
        
        // Verify performance within expected range
        assertTrue(result.getExecutionTimeMs() < expectedMaxTime,
                "Grep with pattern '" + pattern + "' should complete within " + 
                expectedMaxTime + "ms on dataset size " + datasetSize);
        
        // Log performance metrics by pattern
        logPerformanceMetric("grep_pattern_" + pattern.replaceAll("[^a-zA-Z0-9]", "_"), 
                result.getExecutionTimeMs());
    }
    
    @Test
    @DisplayName("GrepCommand options should not significantly impact performance")
    void grepCommandOptionsShouldNotSignificantlyImpactPerformance() {
        // Create dataset
        createTestDataset(MEDIUM_DATASET);
        
        // Base execution: standard search
        GrepCommand baseCommand = new GrepCommand();
        baseCommand.setPattern("common");
        
        PerformanceBenchmark benchmark = new PerformanceBenchmark();
        TimingResult baseResult = benchmark.measureExecutionTime(() -> {
            baseCommand.call();
            return null;
        });
        
        // Option 1: case-sensitive
        GrepCommand caseSensitiveCommand = new GrepCommand();
        caseSensitiveCommand.setPattern("common");
        caseSensitiveCommand.setCaseSensitive(true);
        
        TimingResult caseSensitiveResult = benchmark.measureExecutionTime(() -> {
            caseSensitiveCommand.call();
            return null;
        });
        
        // Option 2: count only
        GrepCommand countCommand = new GrepCommand();
        countCommand.setPattern("common");
        countCommand.setCountOnly(true);
        
        TimingResult countResult = benchmark.measureExecutionTime(() -> {
            countCommand.call();
            return null;
        });
        
        // Verify that options don't drastically slow down execution
        // Allow for 20% variance due to JVM optimization, GC, etc.
        double caseSensitiveRatio = (double) caseSensitiveResult.getExecutionTimeMs() / baseResult.getExecutionTimeMs();
        double countRatio = (double) countResult.getExecutionTimeMs() / baseResult.getExecutionTimeMs();
        
        assertTrue(caseSensitiveRatio < 1.2, 
                "Case-sensitive search should not be significantly slower than base search");
        assertTrue(countRatio < 1.2, 
                "Count-only search should not be significantly slower than base search");
        
        // Log performance impact of options
        logPerformanceMetric("grep_base", baseResult.getExecutionTimeMs());
        logPerformanceMetric("grep_case_sensitive", caseSensitiveResult.getExecutionTimeMs());
        logPerformanceMetric("grep_count", countResult.getExecutionTimeMs());
    }
    
    @Test
    @DisplayName("GrepCommand should handle context search efficiently")
    void grepCommandShouldHandleContextSearchEfficiently() {
        // Create larger dataset for this test
        createTestDataset(MEDIUM_DATASET);
        
        // Create command with context
        GrepCommand contextCommand = new GrepCommand();
        contextCommand.setPattern("common");
        contextCommand.setContext(3);  // 3 lines before and after
        
        // Measure execution time
        PerformanceBenchmark benchmark = new PerformanceBenchmark();
        TimingResult result = benchmark.measureExecutionTime(() -> {
            contextCommand.call();
            return null;
        });
        
        // Verify performance - allow slightly more time for context search
        long contextExpectedTime = ACCEPTABLE_MEDIUM_DATASET_TIME * 2;
        assertTrue(result.getExecutionTimeMs() < contextExpectedTime,
                "Context grep should complete within " + contextExpectedTime + "ms");
        
        // Log performance metrics
        logPerformanceMetric("grep_context", result.getExecutionTimeMs());
    }
    
    @Test
    @DisplayName("GrepCommand should handle large result sets efficiently")
    void grepCommandShouldHandleLargeResultSetsEfficiently() {
        // Create dataset with many matches
        createTestDataset(LARGE_DATASET);
        
        // Search for a common term that will appear in many items
        GrepCommand grepCommand = new GrepCommand();
        grepCommand.setPattern("implementation"); // Should match many items
        
        // Measure execution time
        PerformanceBenchmark benchmark = new PerformanceBenchmark();
        TimingResult result = benchmark.measureExecutionTime(() -> {
            grepCommand.call();
            return null;
        });
        
        // Verify performance within expected range
        assertTrue(result.getExecutionTimeMs() < ACCEPTABLE_LARGE_DATASET_TIME,
                "Grep with many matches should complete within " + ACCEPTABLE_LARGE_DATASET_TIME + "ms");
        
        // Log performance metrics
        logPerformanceMetric("grep_large_resultset", result.getExecutionTimeMs());
    }
    
    /**
     * Logs a performance metric for later analysis.
     *
     * @param metricName the name of the metric
     * @param value the measured value in milliseconds
     */
    private void logPerformanceMetric(String metricName, long value) {
        System.out.println("[PERFORMANCE] " + metricName + ": " + value + "ms");
        
        // In a real system, these would be sent to a metrics collection system
        // Here we just print them for demonstration purposes
    }
}