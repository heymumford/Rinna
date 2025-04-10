/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.bdd;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.mockito.Mockito;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MockAuditService;
import org.rinna.cli.service.MockBackupService;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockComplianceService;
import org.rinna.cli.service.MockCriticalPathService;
import org.rinna.cli.service.MockDiagnosticsService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockMessageService;
import org.rinna.cli.service.MockMonitoringService;
import org.rinna.cli.service.MockNotificationService;
import org.rinna.cli.service.MockRecoveryService;
import org.rinna.cli.service.MockReportService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.MockSecurityService;
import org.rinna.cli.service.MockServerService;
import org.rinna.cli.service.MockStatisticsService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ProjectContext;
import org.rinna.cli.service.ServiceManager;

/**
 * Shared test context for BDD tests to transfer state between steps.
 */
public class TestContext {
    private static TestContext instance;
    
    // Capture console output
    private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    // Mock services
    private MockItemService mockItemService;
    private MockWorkflowService mockWorkflowService;
    private MockSearchService mockSearchService;
    private MockMessageService mockMessageService;
    private MockNotificationService mockNotificationService;
    private MockHistoryService mockHistoryService;
    private MockCommentService mockCommentService;
    private MockReportService mockReportService;
    private MockStatisticsService mockStatisticsService;
    private MockCriticalPathService mockCriticalPathService;
    private MockSecurityService mockSecurityService;
    private MockServerService mockServerService;
    private MockAuditService mockAuditService;
    private MockComplianceService mockComplianceService;
    private MockMonitoringService mockMonitoringService;
    private MockDiagnosticsService mockDiagnosticsService;
    private MockBackupService mockBackupService;
    private MockRecoveryService mockRecoveryService;
    private ConfigurationService mockConfigService;
    private ProjectContext mockProjectContext;
    private ServiceManager mockServiceManager;
    
    // Test state
    private Map<String, Object> testState = new HashMap<>();
    private String lastCommandOutput;
    private int lastCommandExitCode;
    
    private TestContext() {
        // Initialize mock services
        mockItemService = new MockItemService();
        mockWorkflowService = new MockWorkflowService();
        mockSearchService = new MockSearchService();
        mockMessageService = new MockMessageService();
        mockHistoryService = new MockHistoryService();
        mockCommentService = new MockCommentService();
        mockNotificationService = MockNotificationService.getInstance();
        mockReportService = Mockito.mock(MockReportService.class);
        mockStatisticsService = Mockito.mock(MockStatisticsService.class);
        mockCriticalPathService = Mockito.mock(MockCriticalPathService.class);
        mockSecurityService = new MockSecurityService();
        mockServerService = new MockServerService();
        mockAuditService = Mockito.mock(MockAuditService.class);
        mockComplianceService = Mockito.mock(MockComplianceService.class);
        mockMonitoringService = Mockito.mock(MockMonitoringService.class);
        mockDiagnosticsService = Mockito.mock(MockDiagnosticsService.class);
        mockBackupService = Mockito.mock(MockBackupService.class);
        mockRecoveryService = Mockito.mock(MockRecoveryService.class);
        mockConfigService = Mockito.mock(ConfigurationService.class);
        mockProjectContext = Mockito.mock(ProjectContext.class);
        mockServiceManager = Mockito.mock(ServiceManager.class);
        
        // Save original console streams
        originalOut = System.out;
        originalErr = System.err;
    }
    
    /**
     * Gets the singleton instance.
     */
    public static synchronized TestContext getInstance() {
        if (instance == null) {
            instance = new TestContext();
        }
        return instance;
    }
    
    /**
     * Redirects console output for capturing.
     */
    public void redirectConsoleOutput() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }
    
    /**
     * Restores original console output.
     */
    public void restoreConsoleOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Gets the captured standard output.
     */
    public String getStandardOutput() {
        return outContent.toString();
    }
    
    /**
     * Gets the captured error output.
     */
    public String getErrorOutput() {
        return errContent.toString();
    }
    
    /**
     * Resets the captured output.
     */
    public void resetCapturedOutput() {
        outContent.reset();
        errContent.reset();
    }
    
    /**
     * Stores a value in the test state.
     */
    public void storeState(String key, Object value) {
        testState.put(key, value);
    }
    
    /**
     * Retrieves a value from the test state.
     */
    @SuppressWarnings("unchecked")
    public <T> T getState(String key) {
        return (T) testState.get(key);
    }
    
    /**
     * Clears all test state.
     */
    public void clearState() {
        testState.clear();
    }
    
    /**
     * Gets the mock item service.
     */
    public MockItemService getMockItemService() {
        return mockItemService;
    }
    
    /**
     * Gets the mock workflow service.
     */
    public MockWorkflowService getMockWorkflowService() {
        return mockWorkflowService;
    }
    
    /**
     * Gets the mock search service.
     */
    public MockSearchService getMockSearchService() {
        return mockSearchService;
    }
    
    /**
     * Gets the mock message service.
     */
    public MockMessageService getMockMessageService() {
        return mockMessageService;
    }
    
    /**
     * Gets the mock notification service.
     */
    public MockNotificationService getMockNotificationService() {
        return mockNotificationService;
    }
    
    /**
     * Gets the mock history service.
     */
    public MockHistoryService getMockHistoryService() {
        return mockHistoryService;
    }
    
    /**
     * Gets the mock comment service.
     */
    public MockCommentService getMockCommentService() {
        return mockCommentService;
    }
    
    /**
     * Gets the mock report service.
     */
    public MockReportService getMockReportService() {
        return mockReportService;
    }
    
    /**
     * Gets the mock statistics service.
     */
    public MockStatisticsService getMockStatisticsService() {
        return mockStatisticsService;
    }
    
    /**
     * Gets the mock critical path service.
     */
    public MockCriticalPathService getMockCriticalPathService() {
        return mockCriticalPathService;
    }
    
    /**
     * Gets the mock security service.
     */
    public MockSecurityService getMockSecurityService() {
        return mockSecurityService;
    }
    
    /**
     * Gets the mock server service.
     */
    public MockServerService getMockServerService() {
        return mockServerService;
    }
    
    /**
     * Gets the mock audit service.
     */
    public MockAuditService getMockAuditService() {
        return mockAuditService;
    }
    
    /**
     * Gets the mock compliance service.
     */
    public MockComplianceService getMockComplianceService() {
        return mockComplianceService;
    }
    
    /**
     * Gets the mock monitoring service.
     */
    public MockMonitoringService getMockMonitoringService() {
        return mockMonitoringService;
    }
    
    /**
     * Gets the mock diagnostics service.
     */
    public MockDiagnosticsService getMockDiagnosticsService() {
        return mockDiagnosticsService;
    }
    
    /**
     * Gets the mock backup service.
     */
    public MockBackupService getMockBackupService() {
        return mockBackupService;
    }
    
    /**
     * Gets the mock recovery service.
     */
    public MockRecoveryService getMockRecoveryService() {
        return mockRecoveryService;
    }
    
    /**
     * Gets the mock configuration service.
     */
    public ConfigurationService getMockConfigService() {
        return mockConfigService;
    }
    
    /**
     * Gets the mock project context.
     */
    public ProjectContext getMockProjectContext() {
        return mockProjectContext;
    }
    
    /**
     * Gets the mock service manager.
     */
    public ServiceManager getMockServiceManager() {
        return mockServiceManager;
    }
    
    /**
     * Sets up mock services in the ServiceManager.
     */
    public void setupMockServices() {
        ServiceManager serviceManager = ServiceManager.getInstance();
        
        // Use reflection to set the mock services
        try {
            java.lang.reflect.Field itemServiceField = serviceManager.getClass().getDeclaredField("itemService");
            itemServiceField.setAccessible(true);
            itemServiceField.set(serviceManager, mockItemService);
            
            java.lang.reflect.Field workflowServiceField = serviceManager.getClass().getDeclaredField("workflowService");
            workflowServiceField.setAccessible(true);
            workflowServiceField.set(serviceManager, mockWorkflowService);
            
            java.lang.reflect.Field searchServiceField = serviceManager.getClass().getDeclaredField("searchService");
            searchServiceField.setAccessible(true);
            searchServiceField.set(serviceManager, mockSearchService);
            
            java.lang.reflect.Field messageServiceField = serviceManager.getClass().getDeclaredField("messageService");
            messageServiceField.setAccessible(true);
            messageServiceField.set(serviceManager, mockMessageService);
            
            java.lang.reflect.Field notificationServiceField = serviceManager.getClass().getDeclaredField("notificationService");
            notificationServiceField.setAccessible(true);
            notificationServiceField.set(serviceManager, mockNotificationService);
            
            java.lang.reflect.Field historyServiceField = serviceManager.getClass().getDeclaredField("historyService");
            historyServiceField.setAccessible(true);
            historyServiceField.set(serviceManager, mockHistoryService);
            
            java.lang.reflect.Field commentServiceField = serviceManager.getClass().getDeclaredField("commentService");
            commentServiceField.setAccessible(true);
            commentServiceField.set(serviceManager, mockCommentService);
            
            java.lang.reflect.Field reportServiceField = serviceManager.getClass().getDeclaredField("reportService");
            reportServiceField.setAccessible(true);
            reportServiceField.set(serviceManager, mockReportService);
            
            java.lang.reflect.Field statisticsServiceField = serviceManager.getClass().getDeclaredField("statisticsService");
            statisticsServiceField.setAccessible(true);
            statisticsServiceField.set(serviceManager, mockStatisticsService);
            
            java.lang.reflect.Field criticalPathServiceField = serviceManager.getClass().getDeclaredField("criticalPathService");
            criticalPathServiceField.setAccessible(true);
            criticalPathServiceField.set(serviceManager, mockCriticalPathService);
            
            java.lang.reflect.Field securityServiceField = serviceManager.getClass().getDeclaredField("securityService");
            securityServiceField.setAccessible(true);
            securityServiceField.set(serviceManager, mockSecurityService);
            
            java.lang.reflect.Field serverServiceField = serviceManager.getClass().getDeclaredField("serverService");
            serverServiceField.setAccessible(true);
            serverServiceField.set(serviceManager, mockServerService);
            
            java.lang.reflect.Field auditServiceField = serviceManager.getClass().getDeclaredField("auditService");
            auditServiceField.setAccessible(true);
            auditServiceField.set(serviceManager, mockAuditService);
            
            java.lang.reflect.Field complianceServiceField = serviceManager.getClass().getDeclaredField("complianceService");
            complianceServiceField.setAccessible(true);
            complianceServiceField.set(serviceManager, mockComplianceService);
            
            java.lang.reflect.Field monitoringServiceField = serviceManager.getClass().getDeclaredField("monitoringService");
            monitoringServiceField.setAccessible(true);
            monitoringServiceField.set(serviceManager, mockMonitoringService);
            
            java.lang.reflect.Field diagnosticsServiceField = serviceManager.getClass().getDeclaredField("diagnosticsService");
            diagnosticsServiceField.setAccessible(true);
            diagnosticsServiceField.set(serviceManager, mockDiagnosticsService);
            
            java.lang.reflect.Field backupServiceField = serviceManager.getClass().getDeclaredField("backupService");
            backupServiceField.setAccessible(true);
            backupServiceField.set(serviceManager, mockBackupService);
            
            java.lang.reflect.Field recoveryServiceField = serviceManager.getClass().getDeclaredField("recoveryService");
            recoveryServiceField.setAccessible(true);
            recoveryServiceField.set(serviceManager, mockRecoveryService);
            
            // No need to mock methods directly, we've already set the field values
            
            java.lang.reflect.Field configServiceField = serviceManager.getClass().getDeclaredField("configService");
            configServiceField.setAccessible(true);
            configServiceField.set(serviceManager, mockConfigService);
            
            java.lang.reflect.Field projectContextField = serviceManager.getClass().getDeclaredField("projectContext");
            projectContextField.setAccessible(true);
            projectContextField.set(serviceManager, mockProjectContext);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up mock services", e);
        }
    }
    
    /**
     * Gets the output content stream.
     */
    public ByteArrayOutputStream getOutContent() {
        return outContent;
    }
    
    /**
     * Gets the error content stream.
     */
    public ByteArrayOutputStream getErrContent() {
        return errContent;
    }
    
    /**
     * Gets a command processor for executing commands in tests.
     */
    public CommandProcessor getCommandProcessor() {
        return new CommandProcessor(this);
    }
    
    /**
     * Sets the last command output.
     */
    public void setLastCommandOutput(String output) {
        this.lastCommandOutput = output;
    }
    
    /**
     * Gets the last command output.
     */
    public String getLastCommandOutput() {
        return lastCommandOutput;
    }
    
    /**
     * Sets the last command exit code.
     */
    public void setLastCommandExitCode(int exitCode) {
        this.lastCommandExitCode = exitCode;
    }
    
    /**
     * Gets the last command exit code.
     */
    public int getLastCommandExitCode() {
        return lastCommandExitCode;
    }
    
    /**
     * Gets the output content of the last command execution.
     */
    public String getOutputContent() {
        return getStandardOutput();
    }
    
    /**
     * Sets the exit code for the current command.
     */
    public void setExitCode(int exitCode) {
        this.lastCommandExitCode = exitCode;
    }
}