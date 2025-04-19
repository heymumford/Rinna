/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.command.AdminCommand;
import org.rinna.cli.command.impl.AdminAuditCommand;
import org.rinna.cli.command.impl.AdminBackupCommand;
import org.rinna.cli.command.impl.AdminComplianceCommand;
import org.rinna.cli.command.impl.AdminDiagnosticsCommand;
import org.rinna.cli.command.impl.AdminMonitorCommand;
import org.rinna.cli.command.impl.AdminRecoveryCommand;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.AuditService;
import org.rinna.cli.service.BackupService;
import org.rinna.cli.service.ComplianceService;
import org.rinna.cli.service.DiagnosticsService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MonitoringService;
import org.rinna.cli.service.RecoveryService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component test for the AdminCommand class, focusing on:
 * - Proper integration with MetadataService for hierarchical operation tracking
 * - Proper delegation to subcommands
 * - Authentication and authorization checks
 * - Error handling and output formatting
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCommand Component Tests")
public class AdminCommandComponentTest {

    private static final String OPERATION_ID = "op-12345";
    private static final String SUBCOMMAND_OP_ID = "subop-12345";

    @Mock
    private ServiceManager mockServiceManager;

    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private AuditService mockAuditService;
    
    @Mock
    private ComplianceService mockComplianceService;
    
    @Mock
    private MonitoringService mockMonitoringService;
    
    @Mock
    private DiagnosticsService mockDiagnosticsService;
    
    @Mock
    private BackupService mockBackupService;
    
    @Mock
    private RecoveryService mockRecoveryService;
    
    @Mock
    private SecurityManager mockSecurityManager;

    private AdminCommand adminCommand;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        // Configure mock service manager
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
        when(mockServiceManager.getComplianceService()).thenReturn(mockComplianceService);
        when(mockServiceManager.getMockMonitoringService()).thenReturn(mockMonitoringService);
        when(mockServiceManager.getDiagnosticsService()).thenReturn(mockDiagnosticsService);
        when(mockServiceManager.getBackupService()).thenReturn(mockBackupService);
        when(mockServiceManager.getMockRecoveryService()).thenReturn(mockRecoveryService);

        // Configure mock metadata service
        when(mockMetadataService.startOperation(eq("admin"), eq("ADMIN"), anyMap())).thenReturn(OPERATION_ID);
        when(mockMetadataService.trackOperation(anyString(), anyMap())).thenReturn(SUBCOMMAND_OP_ID);

        // Initialize the command with mocked services
        adminCommand = new AdminCommand(mockServiceManager);
        
        // Redirect stdout and stderr for output validation
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should integrate with MetadataService for operation tracking")
        void shouldIntegrateWithMetadataServiceForOperationTracking() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                adminCommand.setSubcommand("help");
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("admin"), eq("ADMIN"), any(Map.class));
                
                // Verify operation completion
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any(Map.class));
            }
        }
        
        @Test
        @DisplayName("Should pass operation parameters to MetadataService")
        void shouldPassOperationParametersToMetadataService() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                adminCommand.setSubcommand("audit");
                adminCommand.setArgs(new String[]{"list", "--format=json", "--limit=10"});
                adminCommand.setFormat("json");
                adminCommand.setVerbose(true);
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify operation parameters
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("admin"), eq("ADMIN"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("audit", params.get("subcommand"));
                assertEquals(3, params.get("argsCount"));
                assertEquals("list", params.get("operation"));
                assertEquals("json", params.get("format"));
                assertEquals(true, params.get("verbose"));
            }
        }
        
        @Test
        @DisplayName("Should create subcommand operations with hierarchical tracking")
        void shouldCreateSubcommandOperationsWithHierarchicalTracking() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                // Mock the AdminAuditCommand
                AdminAuditCommand mockAuditCommand = mock(AdminAuditCommand.class);
                when(mockAuditCommand.call()).thenReturn(0);
                
                try (MockedStatic<AdminAuditCommand> mockedAuditCommand = Mockito.mockStatic(AdminAuditCommand.class)) {
                    mockedAuditCommand.when(() -> new AdminAuditCommand(any())).thenReturn(mockAuditCommand);
                    
                    adminCommand.setSubcommand("audit");
                    adminCommand.setArgs(new String[]{"list"});
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    
                    // Verify subcommand operation tracking
                    ArgumentCaptor<Map<String, Object>> trackingCaptor = ArgumentCaptor.forClass(Map.class);
                    verify(mockMetadataService).trackOperation(eq("admin-audit"), trackingCaptor.capture());
                    
                    Map<String, Object> trackingData = trackingCaptor.getValue();
                    assertEquals("admin", trackingData.get("command"));
                    assertEquals("audit", trackingData.get("subcommand"));
                    assertEquals("list", trackingData.get("operation"));
                    
                    // Verify subcommand operation completion
                    verify(mockMetadataService).completeOperation(eq(SUBCOMMAND_OP_ID), any(Map.class));
                    
                    // Verify main operation completion
                    verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any(Map.class));
                    
                    // Verify audit command was called
                    verify(mockAuditCommand).setOperation("list");
                    verify(mockAuditCommand).call();
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Authentication and Authorization Tests")
    class AuthenticationAndAuthorizationTests {
        
        @Test
        @DisplayName("Should check if user is authenticated")
        void shouldCheckIfUserIsAuthenticated() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                mockedOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
                    .thenReturn("{\"result\":\"error\",\"message\":\"Authentication required\"}");
                
                adminCommand.setSubcommand("audit");
                adminCommand.setFormat("json");
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify authentication check
                verify(mockSecurityManager).isAuthenticated();
                
                // Verify operation failure tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(SecurityException.class));
                
                // Check JSON error output
                String output = outputStream.toString();
                assertTrue(output.contains("\"result\":\"error\""));
                assertTrue(output.contains("\"message\":\"Authentication required\""));
            }
        }
        
        @Test
        @DisplayName("Should check if user has admin privileges")
        void shouldCheckIfUserHasAdminPrivileges() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                when(mockSecurityManager.hasAdminAccess(anyString())).thenReturn(false);
                
                adminCommand.setSubcommand("audit");
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify admin privilege check
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager).hasAdminAccess("audit");
                
                // Verify operation failure tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(SecurityException.class));
                
                // Check error output
                String error = errorStream.toString();
                assertTrue(error.contains("You do not have administrative privileges"));
            }
        }
        
        @Test
        @DisplayName("Should accept full admin users")
        void shouldAcceptFullAdminUsers() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                adminCommand.setSubcommand("help");
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify admin privilege check
                verify(mockSecurityManager).isAdmin();
                verify(mockSecurityManager, never()).hasAdminAccess(anyString());
            }
        }
        
        @Test
        @DisplayName("Should accept area-specific admin users")
        void shouldAcceptAreaSpecificAdminUsers() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                when(mockSecurityManager.hasAdminAccess("audit")).thenReturn(true);
                
                // Mock the AdminAuditCommand
                AdminAuditCommand mockAuditCommand = mock(AdminAuditCommand.class);
                when(mockAuditCommand.call()).thenReturn(0);
                
                try (MockedStatic<AdminAuditCommand> mockedAuditCommand = Mockito.mockStatic(AdminAuditCommand.class)) {
                    mockedAuditCommand.when(() -> new AdminAuditCommand(any())).thenReturn(mockAuditCommand);
                    
                    adminCommand.setSubcommand("audit");
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    
                    // Verify admin privilege check
                    verify(mockSecurityManager).isAdmin();
                    verify(mockSecurityManager).hasAdminAccess("audit");
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Subcommand Delegation Tests")
    class SubcommandDelegationTests {
        
        @Test
        @DisplayName("Should delegate to AdminAuditCommand")
        void shouldDelegateToAdminAuditCommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                // Mock the AdminAuditCommand
                AdminAuditCommand mockAuditCommand = mock(AdminAuditCommand.class);
                when(mockAuditCommand.call()).thenReturn(0);
                
                try (MockedStatic<AdminAuditCommand> mockedAuditCommand = Mockito.mockStatic(AdminAuditCommand.class)) {
                    mockedAuditCommand.when(() -> new AdminAuditCommand(any())).thenReturn(mockAuditCommand);
                    
                    adminCommand.setSubcommand("audit");
                    adminCommand.setArgs(new String[]{"list", "--limit=10"});
                    adminCommand.setFormat("json");
                    adminCommand.setVerbose(true);
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    
                    // Verify audit command was correctly configured
                    verify(mockAuditCommand).setOperation("list");
                    verify(mockAuditCommand).setArgs(eq(new String[]{"--limit=10"}));
                    verify(mockAuditCommand).setJsonOutput(true);
                    verify(mockAuditCommand).setVerbose(true);
                    verify(mockAuditCommand).call();
                }
            }
        }
        
        @Test
        @DisplayName("Should delegate to AdminComplianceCommand")
        void shouldDelegateToAdminComplianceCommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                // Mock the AdminComplianceCommand
                AdminComplianceCommand mockComplianceCommand = mock(AdminComplianceCommand.class);
                when(mockComplianceCommand.call()).thenReturn(0);
                
                try (MockedStatic<AdminComplianceCommand> mockedComplianceCommand = Mockito.mockStatic(AdminComplianceCommand.class)) {
                    mockedComplianceCommand.when(() -> new AdminComplianceCommand(any())).thenReturn(mockComplianceCommand);
                    
                    adminCommand.setSubcommand("compliance");
                    adminCommand.setArgs(new String[]{"report"});
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    
                    // Verify compliance command was correctly configured
                    verify(mockComplianceCommand).setOperation("report");
                    verify(mockComplianceCommand).call();
                }
            }
        }
        
        @Test
        @DisplayName("Should delegate to AdminMonitorCommand")
        void shouldDelegateToAdminMonitorCommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                // Mock the AdminMonitorCommand
                AdminMonitorCommand mockMonitorCommand = mock(AdminMonitorCommand.class);
                when(mockMonitorCommand.call()).thenReturn(0);
                
                try (MockedStatic<AdminMonitorCommand> mockedMonitorCommand = Mockito.mockStatic(AdminMonitorCommand.class)) {
                    mockedMonitorCommand.when(() -> new AdminMonitorCommand(any())).thenReturn(mockMonitorCommand);
                    
                    adminCommand.setSubcommand("monitor");
                    adminCommand.setArgs(new String[]{"dashboard"});
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    
                    // Verify monitor command was correctly configured
                    verify(mockMonitorCommand).setOperation("dashboard");
                    verify(mockMonitorCommand).call();
                }
            }
        }
        
        @Test
        @DisplayName("Should delegate to AdminDiagnosticsCommand")
        void shouldDelegateToAdminDiagnosticsCommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                // Mock the AdminDiagnosticsCommand
                AdminDiagnosticsCommand mockDiagnosticsCommand = mock(AdminDiagnosticsCommand.class);
                when(mockDiagnosticsCommand.call()).thenReturn(0);
                
                try (MockedStatic<AdminDiagnosticsCommand> mockedDiagnosticsCommand = Mockito.mockStatic(AdminDiagnosticsCommand.class)) {
                    mockedDiagnosticsCommand.when(() -> new AdminDiagnosticsCommand(any())).thenReturn(mockDiagnosticsCommand);
                    
                    adminCommand.setSubcommand("diagnostics");
                    adminCommand.setArgs(new String[]{"run"});
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    
                    // Verify diagnostics command was correctly configured
                    verify(mockDiagnosticsCommand).setOperation("run");
                    verify(mockDiagnosticsCommand).call();
                }
            }
        }
        
        @Test
        @DisplayName("Should delegate to AdminBackupCommand")
        void shouldDelegateToAdminBackupCommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                // Mock the AdminBackupCommand
                AdminBackupCommand mockBackupCommand = mock(AdminBackupCommand.class);
                when(mockBackupCommand.call()).thenReturn(0);
                
                try (MockedStatic<AdminBackupCommand> mockedBackupCommand = Mockito.mockStatic(AdminBackupCommand.class)) {
                    mockedBackupCommand.when(() -> new AdminBackupCommand(any())).thenReturn(mockBackupCommand);
                    
                    adminCommand.setSubcommand("backup");
                    adminCommand.setArgs(new String[]{"start"});
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    
                    // Verify backup command was correctly configured
                    verify(mockBackupCommand).setOperation("start");
                    verify(mockBackupCommand).call();
                }
            }
        }
        
        @Test
        @DisplayName("Should delegate to AdminRecoveryCommand")
        void shouldDelegateToAdminRecoveryCommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                // Mock the AdminRecoveryCommand
                AdminRecoveryCommand mockRecoveryCommand = mock(AdminRecoveryCommand.class);
                when(mockRecoveryCommand.call()).thenReturn(0);
                
                try (MockedStatic<AdminRecoveryCommand> mockedRecoveryCommand = Mockito.mockStatic(AdminRecoveryCommand.class)) {
                    mockedRecoveryCommand.when(() -> new AdminRecoveryCommand(any())).thenReturn(mockRecoveryCommand);
                    
                    adminCommand.setSubcommand("recovery");
                    adminCommand.setArgs(new String[]{"plan"});
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(0, exitCode);
                    
                    // Verify recovery command was correctly configured
                    verify(mockRecoveryCommand).setOperation("plan");
                    verify(mockRecoveryCommand).call();
                }
            }
        }
        
        @Test
        @DisplayName("Should handle help subcommand")
        void shouldHandleHelpSubcommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                adminCommand.setSubcommand("help");
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output contains help text
                String output = outputStream.toString();
                assertTrue(output.contains("Usage: rin admin"));
                assertTrue(output.contains("Administrative Commands:"));
                
                // Verify operation completion
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), argThat(map -> 
                    map.containsKey("command") && map.containsKey("action")));
            }
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should output help in text format by default")
        void shouldOutputHelpInTextFormatByDefault() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                adminCommand.setSubcommand("help");
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify text output
                String output = outputStream.toString();
                assertTrue(output.contains("Usage: rin admin <command> [options]"));
                assertTrue(output.contains("Administrative Commands:"));
                assertTrue(output.contains("audit       - Audit log management"));
                assertTrue(output.contains("compliance  - Regulatory compliance"));
            }
        }
        
        @Test
        @DisplayName("Should output help in JSON format when specified")
        void shouldOutputHelpInJsonFormatWhenSpecified() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                mockedOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
                    .thenReturn("{\"result\":\"success\",\"command\":\"admin\",\"commands\":[]}");
                
                adminCommand.setSubcommand("help");
                adminCommand.setFormat("json");
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output
                String output = outputStream.toString();
                assertTrue(output.contains("\"result\":\"success\""));
                assertTrue(output.contains("\"command\":\"admin\""));
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle unknown subcommand")
        void shouldHandleUnknownSubcommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                adminCommand.setSubcommand("invalid");
                
                // When
                int exitCode = adminCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Unknown admin command: invalid"));
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle exception in subcommand")
        void shouldHandleExceptionInSubcommand() {
            // Given
            try (MockedStatic<SecurityManager> mockedSecurityManager = Mockito.mockStatic(SecurityManager.class)) {
                mockedSecurityManager.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(true);
                
                // Mock the AdminAuditCommand to throw an exception
                AdminAuditCommand mockAuditCommand = mock(AdminAuditCommand.class);
                when(mockAuditCommand.call()).thenThrow(new RuntimeException("Subcommand failed"));
                
                try (MockedStatic<AdminAuditCommand> mockedAuditCommand = Mockito.mockStatic(AdminAuditCommand.class)) {
                    mockedAuditCommand.when(() -> new AdminAuditCommand(any())).thenReturn(mockAuditCommand);
                    
                    adminCommand.setSubcommand("audit");
                    
                    // When
                    int exitCode = adminCommand.call();
                    
                    // Then
                    assertEquals(1, exitCode);
                    
                    // Verify error message
                    String error = errorStream.toString();
                    assertTrue(error.contains("Subcommand failed"));
                    
                    // Verify operation failure was tracked
                    verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
                }
            }
        }
    }
}