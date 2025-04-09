/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.impl.*;
import org.rinna.cli.domain.service.*;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AdminCommand class.
 * 
 * This test suite follows best practices:
 * 1. Command Delegation Tests - Verifying proper delegation to subcommands
 * 2. Authentication Tests - Testing authentication and authorization checks
 * 3. Error Handling Tests - Testing error handling scenarios
 * 4. Help Tests - Testing help display functionality
 */
@DisplayName("AdminCommand Tests")
class AdminCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private SecurityManager mockSecurityManager;
    private MetadataService mockMetadataService;
    
    private AdminAuditCommand mockAuditCommand;
    private AdminComplianceCommand mockComplianceCommand;
    private AdminMonitorCommand mockMonitorCommand;
    private AdminDiagnosticsCommand mockDiagnosticsCommand;
    private AdminBackupCommand mockBackupCommand;
    private AdminRecoveryCommand mockRecoveryCommand;
    
    // Operation tracking
    private static final String MOCK_OPERATION_ID = "mock-operation-id";
    private static final String MOCK_SUBCOMMAND_OPERATION_ID = "mock-subcommand-operation-id";
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock service manager and security manager
        mockServiceManager = mock(ServiceManager.class);
        mockSecurityManager = mock(SecurityManager.class);
        mockMetadataService = mock(MetadataService.class);
        
        // Configure the service manager to return the mock metadata service
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up default responses for the metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap())).thenReturn(MOCK_OPERATION_ID);
        when(mockMetadataService.trackOperation(anyString(), anyMap())).thenReturn(MOCK_SUBCOMMAND_OPERATION_ID);
        
        // Create mock subcommands
        mockAuditCommand = mock(AdminAuditCommand.class);
        mockComplianceCommand = mock(AdminComplianceCommand.class);
        mockMonitorCommand = mock(AdminMonitorCommand.class);
        mockDiagnosticsCommand = mock(AdminDiagnosticsCommand.class);
        mockBackupCommand = mock(AdminBackupCommand.class);
        mockRecoveryCommand = mock(AdminRecoveryCommand.class);
        
        // Set up default responses for the security manager
        when(mockSecurityManager.isAuthenticated()).thenReturn(true);
        when(mockSecurityManager.isAdmin()).thenReturn(true);
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset output capture
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    /**
     * Tests for command delegation to subcommands.
     */
    @Nested
    @DisplayName("Command Delegation Tests")
    class CommandDelegationTests {
        
        @Test
        @DisplayName("Should delegate to audit command when subcommand is 'audit'")
        void shouldDelegateToAuditCommandWhenSubcommandIsAudit() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Create test instance with mocked constructor
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                command.setArgs(new String[]{"list"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockAuditCommand).setOperation("list");
                verify(mockAuditCommand).call();
            }
        }
        
        @Test
        @DisplayName("Should delegate to compliance command when subcommand is 'compliance'")
        void shouldDelegateToComplianceCommandWhenSubcommandIsCompliance() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminComplianceCommand> complianceCommandMock = Mockito.mockStatic(AdminComplianceCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                ComplianceService mockComplianceService = mock(ComplianceService.class);
                when(mockServiceManager.getComplianceService()).thenReturn(mockComplianceService);
                
                // Create test instance with mocked constructor
                complianceCommandMock.when(() -> new AdminComplianceCommand(mockServiceManager)).thenReturn(mockComplianceCommand);
                when(mockComplianceCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("compliance");
                command.setArgs(new String[]{"report", "financial"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockComplianceCommand).setOperation("report");
                verify(mockComplianceCommand).setArgs(new String[]{"financial"});
                verify(mockComplianceCommand).call();
            }
        }
        
        @Test
        @DisplayName("Should delegate to monitor command when subcommand is 'monitor'")
        void shouldDelegateToMonitorCommandWhenSubcommandIsMonitor() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminMonitorCommand> monitorCommandMock = Mockito.mockStatic(AdminMonitorCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                MockMonitoringService mockMonitoringService = mock(MockMonitoringService.class);
                when(mockServiceManager.getMonitoringService()).thenReturn(mockMonitoringService);
                
                // Create test instance with mocked constructor
                monitorCommandMock.when(() -> new AdminMonitorCommand(mockServiceManager)).thenReturn(mockMonitorCommand);
                when(mockMonitorCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("monitor");
                command.setArgs(new String[]{"dashboard"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockMonitorCommand).setOperation("dashboard");
                verify(mockMonitorCommand).call();
            }
        }
        
        @Test
        @DisplayName("Should delegate to diagnostics command when subcommand is 'diagnostics'")
        void shouldDelegateToDiagnosticsCommandWhenSubcommandIsDiagnostics() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminDiagnosticsCommand> diagnosticsCommandMock = Mockito.mockStatic(AdminDiagnosticsCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                DiagnosticsService mockDiagnosticsService = mock(DiagnosticsService.class);
                when(mockServiceManager.getDiagnosticsService()).thenReturn(mockDiagnosticsService);
                
                // Create test instance with mocked constructor
                diagnosticsCommandMock.when(() -> new AdminDiagnosticsCommand(mockServiceManager)).thenReturn(mockDiagnosticsCommand);
                when(mockDiagnosticsCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("diagnostics");
                command.setArgs(new String[]{"run"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockDiagnosticsCommand).setOperation("run");
                verify(mockDiagnosticsCommand).call();
            }
        }
        
        @Test
        @DisplayName("Should delegate to backup command when subcommand is 'backup'")
        void shouldDelegateToBackupCommandWhenSubcommandIsBackup() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminBackupCommand> backupCommandMock = Mockito.mockStatic(AdminBackupCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                BackupService mockBackupService = mock(BackupService.class);
                when(mockServiceManager.getBackupService()).thenReturn(mockBackupService);
                
                // Create test instance with mocked constructor
                backupCommandMock.when(() -> new AdminBackupCommand(mockServiceManager)).thenReturn(mockBackupCommand);
                when(mockBackupCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("backup");
                command.setArgs(new String[]{"start", "--type=full"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockBackupCommand).setOperation("start");
                verify(mockBackupCommand).setArgs(new String[]{"--type=full"});
                verify(mockBackupCommand).call();
            }
        }
        
        @Test
        @DisplayName("Should delegate to recovery command when subcommand is 'recovery'")
        void shouldDelegateToRecoveryCommandWhenSubcommandIsRecovery() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminRecoveryCommand> recoveryCommandMock = Mockito.mockStatic(AdminRecoveryCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                MockRecoveryService mockRecoveryService = mock(MockRecoveryService.class);
                when(mockServiceManager.getRecoveryService()).thenReturn(mockRecoveryService);
                
                // Create test instance with mocked constructor
                recoveryCommandMock.when(() -> new AdminRecoveryCommand(mockServiceManager)).thenReturn(mockRecoveryCommand);
                when(mockRecoveryCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("recovery");
                command.setArgs(new String[]{"status"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockRecoveryCommand).setOperation("status");
                verify(mockRecoveryCommand).call();
            }
        }
        
        @Test
        @DisplayName("Should pass JSON and verbose flags to subcommands")
        void shouldPassJsonAndVerboseFlagsToSubcommands() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Create test instance with mocked constructor
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                command.setArgs(new String[]{"list"});
                command.setJsonOutput(true);
                command.setVerbose(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockAuditCommand).setJsonOutput(true);
                verify(mockAuditCommand).setVerbose(true);
            }
        }
    }
    
    /**
     * Tests for authentication and authorization.
     */
    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {
        
        @Test
        @DisplayName("Should fail when user is not authenticated")
        void shouldFailWhenUserIsNotAuthenticated() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Authentication required"));
                assertTrue(error.contains("Use 'rin login' to authenticate"));
            }
        }
        
        @Test
        @DisplayName("Should show JSON error when not authenticated with JSON output")
        void shouldShowJsonErrorWhenNotAuthenticatedWithJsonOutput() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""));
                assertTrue(output.contains("\"message\": \"Authentication required\""));
                assertTrue(output.contains("\"action\": \"Use 'rin login' to authenticate\""));
            }
        }
        
        @Test
        @DisplayName("Should fail when user is authenticated but not an admin")
        void shouldFailWhenUserIsAuthenticatedButNotAdmin() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                when(mockSecurityManager.hasAdminAccess("audit")).thenReturn(false);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("You do not have administrative privileges for the 'audit' area"));
            }
        }
        
        @Test
        @DisplayName("Should succeed when user has area-specific admin access")
        void shouldSucceedWhenUserHasAreaSpecificAdminAccess() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Setup user permissions
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                when(mockSecurityManager.hasAdminAccess("audit")).thenReturn(true);
                
                // Create test instance with mocked constructor
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                verify(mockAuditCommand).call();
            }
        }
    }
    
    /**
     * Tests for error handling.
     */
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should show error when no subcommand is provided")
        void shouldShowErrorWhenNoSubcommandIsProvided() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                // No subcommand provided
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Usage: rin admin <command> [options]"));
            }
        }
        
        @Test
        @DisplayName("Should show error for unknown subcommand")
        void shouldShowErrorForUnknownSubcommand() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("unknown");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Unknown admin command: unknown"));
            }
        }
        
        @Test
        @DisplayName("Should handle exception thrown by subcommand")
        void shouldHandleExceptionThrownBySubcommand() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Create test instance with mocked constructor that throws exception
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenThrow(new RuntimeException("Audit command failed"));
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Audit command failed"));
            }
        }
        
        @Test
        @DisplayName("Should handle subcommand exception with JSON output")
        void shouldHandleSubcommandExceptionWithJsonOutput() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Create test instance with mocked constructor that throws exception
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenThrow(new RuntimeException("Audit command failed"));
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""));
                assertTrue(output.contains("\"message\": \"Audit command failed\""));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace with verbose flag")
        void shouldShowStackTraceWithVerboseFlag() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Create test instance with mocked constructor that throws exception
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenThrow(new RuntimeException("Audit command failed"));
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                command.setVerbose(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Audit command failed"));
                assertTrue(error.contains("java.lang.RuntimeException"));
                assertTrue(error.contains("at org.rinna.cli.command.AdminCommandTest"));
            }
        }
        
        @Test
        @DisplayName("Should escape quotes in JSON error message")
        void shouldEscapeQuotesInJsonErrorMessage() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Create test instance with mocked constructor that throws exception with quotes
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenThrow(new RuntimeException("Audit \"command\" failed"));
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"message\": \"Audit \\\"command\\\" failed\""));
            }
        }
    }
    
    /**
     * Tests for help display.
     */
    @Nested
    @DisplayName("Help Tests")
    class HelpTests {
        
        @Test
        @DisplayName("Should display help when subcommand is 'help'")
        void shouldDisplayHelpWhenSubcommandIsHelp() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("help");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("Usage: rin admin <command> [options]")),
                    () -> assertTrue(output.contains("audit")),
                    () -> assertTrue(output.contains("compliance")),
                    () -> assertTrue(output.contains("monitor")),
                    () -> assertTrue(output.contains("diagnostics")),
                    () -> assertTrue(output.contains("backup")),
                    () -> assertTrue(output.contains("recovery"))
                );
            }
        }
        
        @Test
        @DisplayName("Should display help in JSON format when JSON output is enabled")
        void shouldDisplayHelpInJsonFormatWhenJsonOutputIsEnabled() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("help");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"result\": \"success\"")),
                    () -> assertTrue(output.contains("\"command\": \"admin\"")),
                    () -> assertTrue(output.contains("\"usage\": \"rin admin <command> [options]\"")),
                    () -> assertTrue(output.contains("\"name\": \"audit\"")),
                    () -> assertTrue(output.contains("\"name\": \"compliance\"")),
                    () -> assertTrue(output.contains("\"name\": \"monitor\"")),
                    () -> assertTrue(output.contains("\"name\": \"diagnostics\"")),
                    () -> assertTrue(output.contains("\"name\": \"backup\"")),
                    () -> assertTrue(output.contains("\"name\": \"recovery\""))
                );
            }
        }
    }
    
    /**
     * Tests for operation tracking with MetadataService.
     */
    @Nested
    @DisplayName("Operation Tracking Tests")
    class OperationTrackingTests {
        
        @Test
        @DisplayName("Should track main admin operation")
        void shouldTrackMainAdminOperation() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Create test instance with mocked constructor
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                command.setArgs(new String[]{"list"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                
                // Verify operation tracking for main command
                ArgumentCaptor<String> commandNameCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> operationTypeCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                
                verify(mockMetadataService).startOperation(
                    commandNameCaptor.capture(), 
                    operationTypeCaptor.capture(), 
                    paramsCaptor.capture()
                );
                
                assertEquals("admin", commandNameCaptor.getValue());
                assertEquals("ADMIN", operationTypeCaptor.getValue());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("audit", params.get("subcommand"));
                assertEquals(1, params.get("argsCount"));
                assertEquals("list", params.get("operation"));
                
                // Verify operation completion
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> resultData = resultCaptor.getValue();
                assertEquals(true, resultData.get("success"));
                assertEquals("audit", resultData.get("subcommand"));
            }
        }
        
        @Test
        @DisplayName("Should track subcommand operations")
        void shouldTrackSubcommandOperations() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminComplianceCommand> complianceCommandMock = Mockito.mockStatic(AdminComplianceCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                ComplianceService mockComplianceService = mock(ComplianceService.class);
                when(mockServiceManager.getComplianceService()).thenReturn(mockComplianceService);
                
                // Create test instance with mocked constructor
                complianceCommandMock.when(() -> new AdminComplianceCommand(mockServiceManager)).thenReturn(mockComplianceCommand);
                when(mockComplianceCommand.call()).thenReturn(0);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("compliance");
                command.setArgs(new String[]{"report", "financial"});
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                
                // Verify subcommand operation tracking
                ArgumentCaptor<String> trackCommandCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<Map<String, Object>> trackParamsCaptor = ArgumentCaptor.forClass(Map.class);
                
                verify(mockMetadataService).trackOperation(
                    trackCommandCaptor.capture(),
                    trackParamsCaptor.capture()
                );
                
                assertEquals("admin-compliance", trackCommandCaptor.getValue());
                
                Map<String, Object> trackParams = trackParamsCaptor.getValue();
                assertEquals("admin", trackParams.get("command"));
                assertEquals("compliance", trackParams.get("subcommand"));
                assertEquals("report", trackParams.get("operation"));
                assertEquals("[financial]", trackParams.get("args"));
                
                // Verify operation completion
                verify(mockMetadataService).completeOperation(eq(MOCK_SUBCOMMAND_OPERATION_ID), any(Map.class));
            }
        }
        
        @Test
        @DisplayName("Should track operation failure when authentication fails")
        void shouldTrackOperationFailureWhenAuthenticationFails() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                
                // Verify operation failure tracking
                ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), exceptionCaptor.capture());
                
                Throwable exception = exceptionCaptor.getValue();
                assertTrue(exception instanceof SecurityException);
                assertTrue(exception.getMessage().contains("Authentication required"));
            }
        }
        
        @Test
        @DisplayName("Should track operation failure when authorization fails")
        void shouldTrackOperationFailureWhenAuthorizationFails() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockSecurityManager.isAuthenticated()).thenReturn(true);
                when(mockSecurityManager.isAdmin()).thenReturn(false);
                when(mockSecurityManager.hasAdminAccess("audit")).thenReturn(false);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                
                // Verify operation failure tracking
                ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), exceptionCaptor.capture());
                
                Throwable exception = exceptionCaptor.getValue();
                assertTrue(exception instanceof SecurityException);
                assertTrue(exception.getMessage().contains("You do not have administrative privileges"));
            }
        }
        
        @Test
        @DisplayName("Should track operation failure when subcommand fails")
        void shouldTrackOperationFailureWhenSubcommandFails() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<AdminAuditCommand> auditCommandMock = Mockito.mockStatic(AdminAuditCommand.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup mock services
                AuditService mockAuditService = mock(AuditService.class);
                when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
                
                // Create test instance with mocked constructor that throws exception
                RuntimeException testException = new RuntimeException("Audit command failed");
                auditCommandMock.when(() -> new AdminAuditCommand(mockServiceManager)).thenReturn(mockAuditCommand);
                when(mockAuditCommand.call()).thenThrow(testException);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("audit");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), same(testException));
            }
        }
        
        @Test
        @DisplayName("Should track help operation")
        void shouldTrackHelpOperation() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("help");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                
                // Verify operation tracking for help command
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> resultData = resultCaptor.getValue();
                assertEquals("admin", resultData.get("command"));
                assertEquals("help", resultData.get("action"));
            }
        }
        
        @Test
        @DisplayName("Should track operation failure for unknown subcommand")
        void shouldTrackOperationFailureForUnknownSubcommand() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                AdminCommand command = new AdminCommand(mockServiceManager);
                command.setSubcommand("unknown");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                
                // Verify operation failure tracking
                ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), exceptionCaptor.capture());
                
                Throwable exception = exceptionCaptor.getValue();
                assertTrue(exception instanceof IllegalArgumentException);
                assertEquals("Unknown admin command: unknown", exception.getMessage());
            }
        }
    }
}