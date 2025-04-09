package org.rinna.cli.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.rinna.cli.service.DiagnosticsService;
import org.rinna.cli.service.MockDiagnosticsService;
import org.rinna.cli.service.MockSecurityService;
import org.rinna.cli.service.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step definitions for admin diagnostics command tests.
 */
public class AdminDiagnosticsCommandSteps {
    
    private final TestContext context;
    private final CommandProcessor commandProcessor;
    private final MockSecurityService mockSecurityService;
    private final MockDiagnosticsService mockDiagnosticsService;
    
    /**
     * Constructor with test context.
     * 
     * @param context the test context
     */
    public AdminDiagnosticsCommandSteps(TestContext context) {
        this.context = context;
        this.commandProcessor = context.getCommandProcessor();
        this.mockSecurityService = context.getMockSecurityService();
        this.mockDiagnosticsService = context.getMockDiagnosticsService();
    }
    
    @Given("a system warning with ID {string} exists")
    public void a_system_warning_with_id_exists(String warningId) {
        // Create a map with warning details
        Map<String, String> warningDetails = new HashMap<>();
        warningDetails.put("type", "MemoryLeak");
        warningDetails.put("timestamp", "2025-04-08 10:15:22");
        warningDetails.put("severity", "High");
        warningDetails.put("description", "Possible memory leak detected in API server component");
        
        // Mock the getWarningDetails method to return the warning details for this ID
        when(mockDiagnosticsService.getWarningDetails(warningId))
            .thenReturn(warningDetails);
        
        // Mock the available actions for this warning
        when(mockDiagnosticsService.getAvailableWarningActions(warningId))
            .thenReturn(Arrays.asList(
                "Restart API server component",
                "Increase memory allocation",
                "Analyze memory usage patterns"
            ));
        
        // Mock the performWarningAction method to succeed for this warning
        when(mockDiagnosticsService.performWarningAction(eq(warningId), anyString()))
            .thenReturn(true);
    }
    
    // The remaining necessary step definitions are already defined in other step classes
    // like AdminAuditCommandSteps, AdminComplianceCommandSteps, and AdminMonitorCommandSteps
}