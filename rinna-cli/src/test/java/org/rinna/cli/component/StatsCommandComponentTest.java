/**
 * Component test for StatsCommand with a focus on service integration.
 * 
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.StatsCommand;
import org.rinna.cli.service.*;
import org.rinna.cli.stats.StatisticType;
import org.rinna.cli.stats.StatisticValue;
import org.rinna.cli.stats.StatisticsService;
import org.rinna.cli.stats.StatisticsVisualizer;

/**
 * Component tests for the StatsCommand, focusing on integration with 
 * dependent services and operation tracking.
 */
public class StatsCommandComponentTest {

    private AutoCloseable closeable;
    private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut = System.out;
    private StatsCommand statsCommand;
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private ConfigurationService mockConfigService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private AuthorizationService mockAuthorizationService;

    @Mock
    private StatisticsService mockStatsService;
    
    private static final String OPERATION_ID = "test-operation-id";
    
    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outContent));
        
        // Standard mocking setup
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getAuthorizationService()).thenReturn(mockAuthorizationService);
        
        // Authentication
        when(mockAuthorizationService.isAuthenticated()).thenReturn(true);
        when(mockAuthorizationService.hasPermission(anyString())).thenReturn(true);
        
        // Common configuration
        when(mockConfigService.getStringValue(anyString(), anyString())).thenReturn("text");
        
        // MetadataService standard setup
        when(mockMetadataService.startOperation(eq("stats"), eq("READ"), any())).thenReturn(OPERATION_ID);
        
        // Setup mock statistics
        setupMockStatistics();
        
        try (var staticMock = mockStatic(StatisticsService.class)) {
            staticMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
            statsCommand = new StatsCommand(mockServiceManager);
        }
    }
    
    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        closeable.close();
    }
    
    /**
     * Creates some sample statistics for testing.
     */
    private void setupMockStatistics() {
        // Create summary statistics
        List<StatisticValue> summaryStats = Arrays.asList(
            new StatisticValue(StatisticType.TOTAL_ITEMS, 25.0, "items", "Total work items"),
            new StatisticValue(StatisticType.COMPLETION_RATE, 48.0, "%", "Completion rate"),
            new StatisticValue(StatisticType.WORK_IN_PROGRESS, 8.0, "items", "Work in progress"),
            new StatisticValue(StatisticType.OVERDUE_ITEMS, 3.0, "items", "Overdue items"),
            new StatisticValue(StatisticType.THROUGHPUT, 2.5, "items/day", "Throughput (items/day, last 7 days)")
        );
        
        // Create distribution statistics
        Map<String, Double> typeDistribution = new HashMap<>();
        typeDistribution.put("TASK", 12.0);
        typeDistribution.put("BUG", 8.0);
        typeDistribution.put("FEATURE", 5.0);
        
        Map<String, Double> stateDistribution = new HashMap<>();
        stateDistribution.put("OPEN", 9.0);
        stateDistribution.put("IN_PROGRESS", 8.0);
        stateDistribution.put("DONE", 8.0);
        
        Map<String, Double> priorityDistribution = new HashMap<>();
        priorityDistribution.put("LOW", 7.0);
        priorityDistribution.put("MEDIUM", 10.0);
        priorityDistribution.put("HIGH", 6.0);
        priorityDistribution.put("CRITICAL", 2.0);
        
        Map<String, Double> assigneeDistribution = new HashMap<>();
        assigneeDistribution.put("alice", 8.0);
        assigneeDistribution.put("bob", 6.0);
        assigneeDistribution.put("charlie", 5.0);
        assigneeDistribution.put("Unassigned", 6.0);
        
        StatisticValue typeStats = StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_TYPE, typeDistribution, "Work items by type");
        
        StatisticValue stateStats = StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_STATE, stateDistribution, "Work items by state");
        
        StatisticValue priorityStats = StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_PRIORITY, priorityDistribution, "Work items by priority");
        
        StatisticValue assigneeStats = StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_ASSIGNEE, assigneeDistribution, "Work items by assignee");
        
        // Create all statistics
        List<StatisticValue> allStats = new ArrayList<>(summaryStats);
        allStats.add(typeStats);
        allStats.add(stateStats);
        allStats.add(priorityStats);
        allStats.add(assigneeStats);
        allStats.add(new StatisticValue(StatisticType.AVG_COMPLETION_TIME, 4.5, "days", "Average completion time"));
        allStats.add(new StatisticValue(StatisticType.LEAD_TIME, 6.2, "days", "Lead time (creation to completion)"));
        allStats.add(new StatisticValue(StatisticType.CYCLE_TIME, 3.8, "days", "Cycle time (in progress to completion)"));
        allStats.add(new StatisticValue(StatisticType.BURNDOWN_RATE, 1.8, "items/day", "Burndown rate (items completed per day, last 14 days)"));
        
        // Configure mock statistics service
        when(mockStatsService.getSummaryStatistics()).thenReturn(summaryStats);
        when(mockStatsService.getAllStatistics()).thenReturn(allStats);
        when(mockStatsService.getStatistic(StatisticType.ITEMS_BY_TYPE)).thenReturn(typeStats);
        when(mockStatsService.getStatistic(StatisticType.ITEMS_BY_STATE)).thenReturn(stateStats);
        when(mockStatsService.getStatistic(StatisticType.ITEMS_BY_PRIORITY)).thenReturn(priorityStats);
        when(mockStatsService.getStatistic(StatisticType.ITEMS_BY_ASSIGNEE)).thenReturn(assigneeStats);
        
        // Setup individual statistics
        for (StatisticValue stat : allStats) {
            when(mockStatsService.getStatistic(stat.getType())).thenReturn(stat);
        }
    }
    
    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should integrate with MetadataService for operation tracking")
        void shouldIntegrateWithMetadataServiceForOperationTracking() {
            // Given
            try (var tableVisualizerMock = mockStatic(StatisticsVisualizer.class)) {
                tableVisualizerMock.when(() -> StatisticsVisualizer.createTable(any())).thenReturn("MOCK TABLE");
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify operation was started with correct parameters
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("stats"), eq("READ"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("summary", params.get("type"));
                assertEquals("table", params.get("format"));
                assertEquals(false, params.get("json_output"));
                assertEquals(false, params.get("verbose"));
                
                // Verify operation was completed with result data
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("summary", result.get("type"));
                assertEquals("text", result.get("format"));
                assertEquals("summary", result.get("view"));
            }
        }
        
        @Test
        @DisplayName("Should integrate with StatisticsService to get summary statistics")
        void shouldIntegrateWithStatisticsServiceToGetSummaryStatistics() {
            // Given
            try (var tableVisualizerMock = mockStatic(StatisticsVisualizer.class)) {
                tableVisualizerMock.when(() -> StatisticsVisualizer.createTable(any())).thenReturn("MOCK TABLE");
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify statistics service interaction
                verify(mockStatsService).getSummaryStatistics();
                
                // Verify table visualizer was used
                tableVisualizerMock.verify(() -> StatisticsVisualizer.createTable(any()));
                
                // Verify output contains table
                assertTrue(outContent.toString().contains("MOCK TABLE"));
            }
        }
        
        @Test
        @DisplayName("Should integrate with StatisticsService to get all statistics")
        void shouldIntegrateWithStatisticsServiceToGetAllStatistics() {
            // Given
            try (var tableVisualizerMock = mockStatic(StatisticsVisualizer.class)) {
                tableVisualizerMock.when(() -> StatisticsVisualizer.createTable(any())).thenReturn("MOCK TABLE");
                
                statsCommand.setType("all");
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify statistics service interaction
                verify(mockStatsService).getAllStatistics();
                
                // Verify table visualizer was used
                tableVisualizerMock.verify(() -> StatisticsVisualizer.createTable(any()));
                
                // Verify output contains table
                assertTrue(outContent.toString().contains("MOCK TABLE"));
            }
        }
        
        @Test
        @DisplayName("Should integrate with StatisticsService to refresh statistics")
        void shouldIntegrateWithStatisticsServiceToRefreshStatistics() {
            // Given
            statsCommand.setType("refresh");
            
            // When
            int exitCode = statsCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify statistics service interaction
            verify(mockStatsService).refreshStatistics();
            
            // Verify output contains confirmation
            assertTrue(outContent.toString().contains("Statistics refreshed"));
            
            // Verify operation tracking
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any());
        }
    }
    
    @Nested
    @DisplayName("Command Option Integration Tests")
    class CommandOptionIntegrationTests {
        
        @Test
        @DisplayName("Should record JSON format option in operation tracking")
        void shouldRecordJsonFormatOptionInOperationTracking() {
            // Given
            statsCommand.setJsonOutput(true);
            
            // When
            int exitCode = statsCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation was started with correct parameters
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("stats"), eq("READ"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("summary", params.get("type"));
            assertEquals("json", params.get("format"));
            assertEquals(true, params.get("json_output"));
            
            // Verify operation was completed with result data
            ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
            
            Map<String, Object> result = resultCaptor.getValue();
            assertEquals("json", result.get("format"));
        }
        
        @Test
        @DisplayName("Should record limit option in operation tracking")
        void shouldRecordLimitOptionInOperationTracking() {
            // Given
            try (var barChartMock = mockStatic(StatisticsVisualizer.class)) {
                barChartMock.when(() -> StatisticsVisualizer.createBarChart(any(), anyInt())).thenReturn("MOCK CHART");
                
                statsCommand.setType("distribution");
                statsCommand.setLimit(5);
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify operation was started with correct parameters
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("stats"), eq("READ"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("distribution", params.get("type"));
                assertEquals(5, params.get("limit"));
                
                // Verify bar chart was created with limit
                barChartMock.verify(() -> StatisticsVisualizer.createBarChart(any(), eq(5)), atLeastOnce());
            }
        }
        
        @Test
        @DisplayName("Should record verbose option in operation tracking")
        void shouldRecordVerboseOptionInOperationTracking() {
            // Given
            try (var tableVisualizerMock = mockStatic(StatisticsVisualizer.class)) {
                tableVisualizerMock.when(() -> StatisticsVisualizer.createTable(any())).thenReturn("MOCK TABLE");
                
                statsCommand.setVerbose(true);
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify operation was started with correct parameters
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("stats"), eq("READ"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals(true, params.get("verbose"));
                
                // Verify output contains timestamps (only shown in verbose mode)
                assertTrue(outContent.toString().contains("Statistics last updated"));
            }
        }
        
        @Test
        @DisplayName("Should record filter arguments in operation tracking")
        void shouldRecordFilterArgumentsInOperationTracking() {
            // Given
            try (var tableVisualizerMock = mockStatic(StatisticsVisualizer.class)) {
                tableVisualizerMock.when(() -> StatisticsVisualizer.createTable(any())).thenReturn("MOCK TABLE");
                
                statsCommand.setType("detail");
                statsCommand.setFilterArgs(new String[]{"completion"});
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify operation was started with correct parameters
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("stats"), eq("READ"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("detail", params.get("type"));
                assertEquals("completion", params.get("filter_args"));
                
                // Verify operation was completed with result data
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("detail", result.get("view"));
                assertEquals("completion", result.get("detail_type"));
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should track failed operations in MetadataService when statistics service throws exception")
        void shouldTrackFailedOperationsInMetadataServiceWhenStatisticsServiceThrowsException() {
            // Given
            when(mockStatsService.getSummaryStatistics()).thenThrow(new RuntimeException("Test exception"));
            
            // When
            int exitCode = statsCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify operation was started
            verify(mockMetadataService).startOperation(eq("stats"), eq("READ"), any());
            
            // Verify operation was marked as failed
            ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), exceptionCaptor.capture());
            
            Exception capturedException = exceptionCaptor.getValue();
            assertEquals("Test exception", capturedException.getMessage());
        }
        
        @Test
        @DisplayName("Should track failed operations in MetadataService for unknown statistic type")
        void shouldTrackFailedOperationsInMetadataServiceForUnknownStatisticType() {
            // Given
            statsCommand.setType("nonexistent_type");
            
            // When
            int exitCode = statsCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify operation was started
            verify(mockMetadataService).startOperation(eq("stats"), eq("READ"), any());
            
            // Verify operation was marked as failed
            ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), exceptionCaptor.capture());
            
            Exception capturedException = exceptionCaptor.getValue();
            assertTrue(capturedException.getMessage().contains("Unknown statistics type: nonexistent_type"));
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should generate JSON output with proper structure when JSON format is requested")
        void shouldGenerateJsonOutputWithProperStructureWhenJsonFormatIsRequested() {
            // Given
            statsCommand.setJsonOutput(true);
            
            // When
            int exitCode = statsCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Check JSON output structure
            String output = outContent.toString();
            assertTrue(output.contains("\"result\": \"success\""), "JSON output should contain success result");
            assertTrue(output.contains("\"type\": \"summary\""), "JSON output should contain type");
            assertTrue(output.contains("\"statistics\": ["), "JSON output should contain statistics array");
            assertTrue(output.contains("\"description\": \""), "JSON output should contain description field");
            assertTrue(output.contains("\"value\": "), "JSON output should contain value field");
            assertTrue(output.contains("\"unit\": \""), "JSON output should contain unit field");
            
            // Verify operation tracking
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any());
        }
        
        @Test
        @DisplayName("Should generate JSON error format when an error occurs and JSON output is requested")
        void shouldGenerateJsonErrorFormatWhenAnErrorOccursAndJsonOutputIsRequested() {
            // Given
            when(mockStatsService.getSummaryStatistics()).thenThrow(new RuntimeException("Test JSON error"));
            statsCommand.setJsonOutput(true);
            
            // When
            int exitCode = statsCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Check JSON error output structure
            String output = outContent.toString();
            assertTrue(output.contains("\"result\": \"error\""), "JSON output should contain error result");
            assertTrue(output.contains("\"message\": \"Test JSON error\""), "JSON output should contain error message");
            
            // Verify operation failure was tracked
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(Exception.class));
        }
    }
    
    @Nested
    @DisplayName("Different Statistics Types Tests")
    class DifferentStatisticsTypesTests {
        
        @Test
        @DisplayName("Should retrieve and display dashboard statistics")
        void shouldRetrieveAndDisplayDashboardStatistics() {
            // Given
            try (var dashboardMock = mockStatic(StatisticsVisualizer.class)) {
                dashboardMock.when(() -> StatisticsVisualizer.createDashboard(any())).thenReturn("MOCK DASHBOARD");
                
                statsCommand.setType("dashboard");
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify statistics service interaction
                verify(mockStatsService).getAllStatistics();
                
                // Verify dashboard visualizer was used
                dashboardMock.verify(() -> StatisticsVisualizer.createDashboard(any()));
                
                // Verify output contains dashboard
                assertTrue(outContent.toString().contains("MOCK DASHBOARD"));
                
                // Verify operation tracking
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("dashboard", result.get("view"));
            }
        }
        
        @Test
        @DisplayName("Should retrieve and display distribution statistics")
        void shouldRetrieveAndDisplayDistributionStatistics() {
            // Given
            try (var barChartMock = mockStatic(StatisticsVisualizer.class)) {
                barChartMock.when(() -> StatisticsVisualizer.createBarChart(any(), anyInt())).thenReturn("MOCK BAR CHART");
                
                statsCommand.setType("distribution");
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify statistics service interactions
                verify(mockStatsService).getStatistic(StatisticType.ITEMS_BY_TYPE);
                verify(mockStatsService).getStatistic(StatisticType.ITEMS_BY_STATE);
                verify(mockStatsService).getStatistic(StatisticType.ITEMS_BY_PRIORITY);
                verify(mockStatsService).getStatistic(StatisticType.ITEMS_BY_ASSIGNEE);
                
                // Verify bar chart visualizer was used multiple times
                barChartMock.verify(() -> StatisticsVisualizer.createBarChart(any(), anyInt()), times(4));
                
                // Verify output contains bar charts
                assertTrue(outContent.toString().contains("MOCK BAR CHART"));
                
                // Verify operation tracking
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals("distribution", result.get("view"));
            }
        }
        
        @Test
        @DisplayName("Should retrieve and display completion metrics for detail type")
        void shouldRetrieveAndDisplayCompletionMetricsForDetailType() {
            // Given
            try (var tableVisualizerMock = mockStatic(StatisticsVisualizer.class)) {
                tableVisualizerMock.when(() -> StatisticsVisualizer.createTable(any())).thenReturn("MOCK COMPLETION METRICS");
                
                statsCommand.setType("detail");
                statsCommand.setFilterArgs(new String[]{"completion"});
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify statistics service interactions
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.COMPLETION_RATE);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.AVG_COMPLETION_TIME);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.LEAD_TIME);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.CYCLE_TIME);
                
                // Verify table visualizer was used
                tableVisualizerMock.verify(() -> StatisticsVisualizer.createTable(any()));
                
                // Verify output contains completion metrics
                String output = outContent.toString();
                assertTrue(output.contains("COMPLETION METRICS"));
                assertTrue(output.contains("MOCK COMPLETION METRICS"));
            }
        }
        
        @Test
        @DisplayName("Should retrieve and display workflow metrics for detail type")
        void shouldRetrieveAndDisplayWorkflowMetricsForDetailType() {
            // Given
            try (var barChartMock = mockStatic(StatisticsVisualizer.class)) {
                barChartMock.when(() -> StatisticsVisualizer.createBarChart(any(), anyInt())).thenReturn("MOCK STATE CHART");
                barChartMock.when(() -> StatisticsVisualizer.createTable(any())).thenReturn("MOCK WORKFLOW METRICS");
                
                statsCommand.setType("detail");
                statsCommand.setFilterArgs(new String[]{"workflow"});
                
                // When
                int exitCode = statsCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify statistics service interactions
                verify(mockStatsService).getStatistic(StatisticType.ITEMS_BY_STATE);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.WORK_IN_PROGRESS);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.THROUGHPUT);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.BURNDOWN_RATE);
                
                // Verify visualizers were used
                barChartMock.verify(() -> StatisticsVisualizer.createBarChart(any(), anyInt()));
                barChartMock.verify(() -> StatisticsVisualizer.createTable(any()));
                
                // Verify output contains workflow metrics
                String output = outContent.toString();
                assertTrue(output.contains("WORKFLOW METRICS"));
                assertTrue(output.contains("MOCK STATE CHART"));
                assertTrue(output.contains("MOCK WORKFLOW METRICS"));
            }
        }
        
        @Test
        @DisplayName("Should retrieve and display help information")
        void shouldRetrieveAndDisplayHelpInformation() {
            // Given
            statsCommand.setType("help");
            
            // When
            int exitCode = statsCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output contains help information
            String output = outContent.toString();
            assertTrue(output.contains("Statistics Command Usage"));
            assertTrue(output.contains("Types:"));
            assertTrue(output.contains("Options:"));
            assertTrue(output.contains("Examples:"));
            
            // Verify operation tracking
            ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
            
            Map<String, Object> result = resultCaptor.getValue();
            assertEquals("help", result.get("view"));
        }
    }
    
    @Nested
    @DisplayName("Empty Results Handling Tests")
    class EmptyResultsHandlingTests {
        
        @Test
        @DisplayName("Should handle empty statistics gracefully")
        void shouldHandleEmptyStatisticsGracefully() {
            // Given
            when(mockStatsService.getSummaryStatistics()).thenReturn(new ArrayList<>());
            
            // When
            int exitCode = statsCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output shows no statistics available message
            String output = outContent.toString();
            assertTrue(output.contains("No statistics available"));
            assertTrue(output.contains("Run 'rin stats refresh' to update statistics"));
            
            // Verify operation was still tracked as complete
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any());
        }
    }
}