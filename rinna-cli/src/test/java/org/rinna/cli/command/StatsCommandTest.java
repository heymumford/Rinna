/*
 * StatsCommandTest for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.adapter.MockItemServiceAdapter;
import org.rinna.cli.adapter.StatisticItemAdapter;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.stats.StatisticType;
import org.rinna.cli.stats.StatisticValue;
import org.rinna.cli.stats.StatisticsService;
import org.rinna.cli.stats.StatisticsVisualizer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the StatsCommand class.
 * 
 * This test suite follows best practices:
 * 1. Command Type Tests - Testing different statistics types (summary, all, dashboard, etc.)
 * 2. Format Tests - Testing different output formats (table, json)
 * 3. Error Handling Tests - Testing error scenarios
 * 4. Help Tests - Testing help display functionality
 */
@DisplayName("StatsCommand Tests")
class StatsCommandTest {
    
    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.err;
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    
    private MockItemService mockItemService;
    private ServiceManager mockServiceManager;
    private StatisticsService mockStatsService;
    
    @BeforeEach
    void setUp() {
        // Redirect stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Create mocks
        mockItemService = mock(MockItemService.class);
        mockServiceManager = mock(ServiceManager.class);
        mockStatsService = mock(StatisticsService.class);
        
        // Setup mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout and stderr
        System.setOut(standardOut);
        System.setErr(standardErr);
    }
    
    /**
     * Creates a mock statistic value for testing.
     */
    private StatisticValue createMockStatistic(StatisticType type, double value, String description) {
        return new StatisticValue(type, value, "items", description);
    }
    
    /**
     * Creates a mock distribution statistic for testing.
     */
    private StatisticValue createMockDistribution(StatisticType type, String description) {
        Map<String, Double> distribution = new HashMap<>();
        distribution.put("TASK", 10.0);
        distribution.put("BUG", 5.0);
        distribution.put("FEATURE", 7.0);
        return StatisticValue.createDistribution(type, distribution, description);
    }
    
    /**
     * Sets up mock statistics service with common statistics.
     */
    private void setupMockStatistics() {
        List<StatisticValue> summaryStats = Arrays.asList(
            createMockStatistic(StatisticType.TOTAL_ITEMS, 22.0, "Total work items"),
            createMockStatistic(StatisticType.COMPLETION_RATE, 45.5, "Completion rate"),
            createMockStatistic(StatisticType.WORK_IN_PROGRESS, 5.0, "Work in progress"),
            createMockStatistic(StatisticType.OVERDUE_ITEMS, 2.0, "Overdue items"),
            createMockStatistic(StatisticType.THROUGHPUT, 3.2, "Throughput (items/day, last 7 days)")
        );
        
        List<StatisticValue> allStats = new ArrayList<>(summaryStats);
        allStats.add(createMockDistribution(StatisticType.ITEMS_BY_TYPE, "Work items by type"));
        allStats.add(createMockDistribution(StatisticType.ITEMS_BY_STATE, "Work items by state"));
        allStats.add(createMockDistribution(StatisticType.ITEMS_BY_PRIORITY, "Work items by priority"));
        allStats.add(createMockDistribution(StatisticType.ITEMS_BY_ASSIGNEE, "Work items by assignee"));
        allStats.add(createMockStatistic(StatisticType.AVG_COMPLETION_TIME, 4.5, "Average completion time"));
        
        when(mockStatsService.getSummaryStatistics()).thenReturn(summaryStats);
        when(mockStatsService.getAllStatistics()).thenReturn(allStats);
        when(mockStatsService.getStatistic(StatisticType.ITEMS_BY_TYPE)).thenReturn(createMockDistribution(StatisticType.ITEMS_BY_TYPE, "Work items by type"));
        when(mockStatsService.getStatistic(StatisticType.ITEMS_BY_STATE)).thenReturn(createMockDistribution(StatisticType.ITEMS_BY_STATE, "Work items by state"));
        when(mockStatsService.getStatistic(StatisticType.ITEMS_BY_PRIORITY)).thenReturn(createMockDistribution(StatisticType.ITEMS_BY_PRIORITY, "Work items by priority"));
        when(mockStatsService.getStatistic(StatisticType.ITEMS_BY_ASSIGNEE)).thenReturn(createMockDistribution(StatisticType.ITEMS_BY_ASSIGNEE, "Work items by assignee"));
        when(mockStatsService.getStatistic(StatisticType.AVG_COMPLETION_TIME)).thenReturn(createMockStatistic(StatisticType.AVG_COMPLETION_TIME, 4.5, "Average completion time"));
        when(mockStatsService.getStatistic(StatisticType.COMPLETION_RATE)).thenReturn(createMockStatistic(StatisticType.COMPLETION_RATE, 45.5, "Completion rate"));
        when(mockStatsService.getStatistic(StatisticType.LEAD_TIME)).thenReturn(createMockStatistic(StatisticType.LEAD_TIME, 6.3, "Lead time (creation to completion)"));
        when(mockStatsService.getStatistic(StatisticType.CYCLE_TIME)).thenReturn(createMockStatistic(StatisticType.CYCLE_TIME, 4.2, "Cycle time (in progress to completion)"));
        when(mockStatsService.getStatistic(StatisticType.THROUGHPUT)).thenReturn(createMockStatistic(StatisticType.THROUGHPUT, 3.2, "Throughput (items/day, last 7 days)"));
        when(mockStatsService.getStatistic(StatisticType.WORK_IN_PROGRESS)).thenReturn(createMockStatistic(StatisticType.WORK_IN_PROGRESS, 5.0, "Work in progress"));
        when(mockStatsService.getStatistic(StatisticType.BURNDOWN_RATE)).thenReturn(createMockStatistic(StatisticType.BURNDOWN_RATE, 2.8, "Burndown rate (items completed per day, last 14 days)"));
    }
    
    @Nested
    @DisplayName("Command Type Tests")
    class CommandTypeTests {
        
        @Test
        @DisplayName("Should display summary statistics by default")
        void shouldDisplaySummaryStatisticsByDefault() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                StatsCommand command = new StatsCommand();
                // Default type is "summary" - no need to set
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Statistics Summary"), "Output should contain statistics summary");
                assertTrue(output.contains("Total work items"), "Output should contain total work items");
                assertTrue(output.contains("Completion rate"), "Output should contain completion rate");
                assertTrue(output.contains("Work in progress"), "Output should contain work in progress");
                
                verify(mockStatsService).getSummaryStatistics();
            }
        }
        
        @Test
        @DisplayName("Should display all statistics when type is 'all'")
        void shouldDisplayAllStatisticsWhenTypeIsAll() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                StatsCommand command = new StatsCommand();
                command.setType("all");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Statistics Summary"), "Output should contain statistics summary");
                assertTrue(output.contains("Total work items"), "Output should contain total work items");
                assertTrue(output.contains("Completion rate"), "Output should contain completion rate");
                assertTrue(output.contains("Work items by type"), "Output should contain work items by type");
                assertTrue(output.contains("Work items by state"), "Output should contain work items by state");
                
                verify(mockStatsService).getAllStatistics();
            }
        }
        
        @Test
        @DisplayName("Should display dashboard when type is 'dashboard'")
        void shouldDisplayDashboardWhenTypeIsDashboard() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class);
                 MockedStatic<StatisticsVisualizer> visualizerMock = Mockito.mockStatic(StatisticsVisualizer.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                // Mock the dashboard creation
                visualizerMock.when(() -> StatisticsVisualizer.createDashboard(any()))
                    .thenReturn("MOCK DASHBOARD VISUALIZATION");
                
                StatsCommand command = new StatsCommand();
                command.setType("dashboard");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("MOCK DASHBOARD VISUALIZATION"), "Output should contain dashboard visualization");
                
                verify(mockStatsService).getAllStatistics();
                visualizerMock.verify(() -> StatisticsVisualizer.createDashboard(any()));
            }
        }
        
        @Test
        @DisplayName("Should display distribution charts when type is 'distribution'")
        void shouldDisplayDistributionChartsWhenTypeIsDistribution() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class);
                 MockedStatic<StatisticsVisualizer> visualizerMock = Mockito.mockStatic(StatisticsVisualizer.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                // Mock the chart creation
                visualizerMock.when(() -> StatisticsVisualizer.createBarChart(any(), anyInt()))
                    .thenReturn("MOCK BAR CHART");
                
                StatsCommand command = new StatsCommand();
                command.setType("distribution");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("MOCK BAR CHART"), "Output should contain bar charts");
                
                verify(mockStatsService, times(4)).getStatistic(any(StatisticType.class));
                visualizerMock.verify(() -> StatisticsVisualizer.createBarChart(any(), anyInt()), times(4));
            }
        }
        
        @Test
        @DisplayName("Should display detail about completion metrics when type is 'detail' and args are 'completion'")
        void shouldDisplayDetailAboutCompletionMetricsWhenTypeIsDetailAndArgsAreCompletion() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class);
                 MockedStatic<StatisticsVisualizer> visualizerMock = Mockito.mockStatic(StatisticsVisualizer.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                // Mock the table creation
                visualizerMock.when(() -> StatisticsVisualizer.createTable(any()))
                    .thenReturn("MOCK COMPLETION METRICS TABLE");
                
                StatsCommand command = new StatsCommand();
                command.setType("detail");
                command.setFilterArgs(new String[]{"completion"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("COMPLETION METRICS"), "Output should contain completion metrics title");
                assertTrue(output.contains("MOCK COMPLETION METRICS TABLE"), "Output should contain completion metrics table");
                
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.COMPLETION_RATE);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.ITEMS_COMPLETED);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.AVG_COMPLETION_TIME);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.LEAD_TIME);
                verify(mockStatsService, atLeastOnce()).getStatistic(StatisticType.CYCLE_TIME);
            }
        }
        
        @Test
        @DisplayName("Should display specific statistic when type is a valid statistic type")
        void shouldDisplaySpecificStatisticWhenTypeIsValidStatisticType() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                StatsCommand command = new StatsCommand();
                command.setType("completion_rate");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("COMPLETION_RATE"), "Output should contain the statistic type");
                assertTrue(output.contains("Completion rate"), "Output should contain the statistic description");
                assertTrue(output.contains("45.50"), "Output should contain the statistic value");
                
                verify(mockStatsService).getStatistic(StatisticType.COMPLETION_RATE);
            }
        }
        
        @Test
        @DisplayName("Should refresh statistics when type is 'refresh'")
        void shouldRefreshStatisticsWhenTypeIsRefresh() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                StatsCommand command = new StatsCommand();
                command.setType("refresh");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Statistics refreshed"), "Output should confirm statistics were refreshed");
                
                verify(mockStatsService).refreshStatistics();
            }
        }
        
        @Test
        @DisplayName("Should display help when type is 'help'")
        void shouldDisplayHelpWhenTypeIsHelp() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                StatsCommand command = new StatsCommand();
                command.setType("help");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Statistics Command Usage"), "Output should contain usage information");
                assertTrue(output.contains("summary"), "Output should list the summary type");
                assertTrue(output.contains("dashboard"), "Output should list the dashboard type");
                assertTrue(output.contains("all"), "Output should list the all type");
                assertTrue(output.contains("distribution"), "Output should list the distribution type");
                assertTrue(output.contains("detail"), "Output should list the detail type");
                assertTrue(output.contains("refresh"), "Output should list the refresh type");
                assertTrue(output.contains("help"), "Output should list the help type");
            }
        }
    }
    
    @Nested
    @DisplayName("Format Tests")
    class FormatTests {
        
        @Test
        @DisplayName("Should format output as table by default")
        void shouldFormatOutputAsTableByDefault() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class);
                 MockedStatic<StatisticsVisualizer> visualizerMock = Mockito.mockStatic(StatisticsVisualizer.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                // Mock the table creation
                visualizerMock.when(() -> StatisticsVisualizer.createTable(any()))
                    .thenReturn("MOCK TABLE FORMAT");
                
                StatsCommand command = new StatsCommand();
                // Default format is "table" - no need to set
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("MOCK TABLE FORMAT"), "Output should contain table format");
                
                visualizerMock.verify(() -> StatisticsVisualizer.createTable(any()));
            }
        }
        
        @Test
        @DisplayName("Should format output as JSON when jsonOutput is true")
        void shouldFormatOutputAsJsonWhenJsonOutputIsTrue() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                StatsCommand command = new StatsCommand();
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""), "Output should contain JSON success result");
                assertTrue(output.contains("\"type\": \"summary\""), "Output should contain summary type in JSON");
                assertTrue(output.contains("\"statistics\": ["), "Output should contain statistics array in JSON");
                assertTrue(output.contains("\"value\": "), "Output should contain value field in JSON");
                
                verify(mockStatsService).getSummaryStatistics();
            }
        }
        
        @Test
        @DisplayName("Should include additional details when verbose is true")
        void shouldIncludeAdditionalDetailsWhenVerboseIsTrue() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class);
                 MockedStatic<StatisticsVisualizer> visualizerMock = Mockito.mockStatic(StatisticsVisualizer.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                // Mock the table creation
                visualizerMock.when(() -> StatisticsVisualizer.createTable(any()))
                    .thenReturn("MOCK TABLE FORMAT");
                
                StatsCommand command = new StatsCommand();
                command.setVerbose(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("MOCK TABLE FORMAT"), "Output should contain table format");
                assertTrue(output.contains("Statistics last updated"), "Output should contain last updated timestamp");
                
                visualizerMock.verify(() -> StatisticsVisualizer.createTable(any()));
            }
        }
        
        @Test
        @DisplayName("Should limit output to specified number of items when limit is set")
        void shouldLimitOutputToSpecifiedNumberOfItemsWhenLimitIsSet() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class);
                 MockedStatic<StatisticsVisualizer> visualizerMock = Mockito.mockStatic(StatisticsVisualizer.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                setupMockStatistics();
                
                // Mock the bar chart creation
                visualizerMock.when(() -> StatisticsVisualizer.createBarChart(any(), eq(3)))
                    .thenReturn("MOCK LIMITED BAR CHART");
                
                StatsCommand command = new StatsCommand();
                command.setType("distribution");
                command.setLimit(3);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("MOCK LIMITED BAR CHART"), "Output should contain limited bar charts");
                
                verify(mockStatsService, times(4)).getStatistic(any(StatisticType.class));
                visualizerMock.verify(() -> StatisticsVisualizer.createBarChart(any(), eq(3)), times(4));
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle unknown statistic type gracefully")
        void shouldHandleUnknownStatisticTypeGracefully() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                StatsCommand command = new StatsCommand();
                command.setType("nonexistent_type");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = errorCaptor.toString();
                assertTrue(output.contains("Error: Unknown statistics type: nonexistent_type"), 
                    "Error output should indicate unknown type");
                
                verify(mockStatsService, never()).getSummaryStatistics();
                verify(mockStatsService, never()).getAllStatistics();
            }
        }
        
        @Test
        @DisplayName("Should handle exceptions gracefully")
        void shouldHandleExceptionsGracefully() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                // Make the service throw an exception
                doThrow(new RuntimeException("Service error")).when(mockStatsService).getSummaryStatistics();
                
                StatsCommand command = new StatsCommand();
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = errorCaptor.toString();
                assertTrue(output.contains("Error: Service error"), "Error output should contain service error message");
                
                verify(mockStatsService).getSummaryStatistics();
            }
        }
        
        @Test
        @DisplayName("Should handle JSON output for errors")
        void shouldHandleJsonOutputForErrors() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                // Make the service throw an exception
                doThrow(new RuntimeException("Service error")).when(mockStatsService).getSummaryStatistics();
                
                StatsCommand command = new StatsCommand();
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("{ \"result\": \"error\", \"message\": \"Service error\" }"), 
                    "Output should contain JSON error message");
                
                verify(mockStatsService).getSummaryStatistics();
            }
        }
        
        @Test
        @DisplayName("Should handle missing detail type")
        void shouldHandleMissingDetailType() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                StatsCommand command = new StatsCommand();
                command.setType("detail");
                // No filter args provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result); // The command still returns success
                String output = errorCaptor.toString();
                assertTrue(output.contains("Error: Detail type required"), 
                    "Error output should indicate detail type is required");
                assertTrue(output.contains("Available types:"), 
                    "Error output should list available detail types");
                
                verify(mockStatsService, never()).getStatistic(any(StatisticType.class));
            }
        }
        
        @Test
        @DisplayName("Should handle unknown detail type")
        void shouldHandleUnknownDetailType() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                StatsCommand command = new StatsCommand();
                command.setType("detail");
                command.setFilterArgs(new String[]{"nonexistent_detail"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result); // The command still returns success
                String output = errorCaptor.toString();
                assertTrue(output.contains("Error: Unknown detail type: nonexistent_detail"), 
                    "Error output should indicate unknown detail type");
                assertTrue(output.contains("Available types:"), 
                    "Error output should list available detail types");
                
                verify(mockStatsService, never()).getStatistic(any(StatisticType.class));
            }
        }
        
        @Test
        @DisplayName("Should handle no statistics available gracefully")
        void shouldHandleNoStatisticsAvailableGracefully() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                // Return empty statistics
                when(mockStatsService.getSummaryStatistics()).thenReturn(new ArrayList<>());
                
                StatsCommand command = new StatsCommand();
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("No statistics available"), 
                    "Output should indicate no statistics are available");
                assertTrue(output.contains("Run 'rin stats refresh' to update statistics"), 
                    "Output should suggest refreshing statistics");
                
                verify(mockStatsService).getSummaryStatistics();
            }
        }
    }
    
    @Nested
    @DisplayName("Help Tests")
    class HelpTests {
        
        @Test
        @DisplayName("Should display text help information")
        void shouldDisplayTextHelpInformation() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                StatsCommand command = new StatsCommand();
                command.setType("help");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Statistics Command Usage:"), "Output should contain usage heading");
                assertTrue(output.contains("rin stats [type] [options]"), "Output should contain command syntax");
                assertTrue(output.contains("Types:"), "Output should list available types");
                assertTrue(output.contains("Options:"), "Output should list available options");
                assertTrue(output.contains("Examples:"), "Output should provide examples");
            }
        }
        
        @Test
        @DisplayName("Should display JSON help information when JSON output is enabled")
        void shouldDisplayJsonHelpInformationWhenJsonOutputIsEnabled() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<StatisticsService> statsServiceMock = Mockito.mockStatic(StatisticsService.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                statsServiceMock.when(StatisticsService::getInstance).thenReturn(mockStatsService);
                
                StatsCommand command = new StatsCommand();
                command.setType("help");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""), "Output should contain JSON success result");
                assertTrue(output.contains("\"command\": \"stats\""), "Output should contain command name in JSON");
                assertTrue(output.contains("\"usage\": \"rin stats [type] [options]\""), "Output should contain usage in JSON");
                assertTrue(output.contains("\"types\": ["), "Output should contain types array in JSON");
                assertTrue(output.contains("\"options\": ["), "Output should contain options array in JSON");
                assertTrue(output.contains("\"examples\": ["), "Output should contain examples array in JSON");
            }
        }
    }
}