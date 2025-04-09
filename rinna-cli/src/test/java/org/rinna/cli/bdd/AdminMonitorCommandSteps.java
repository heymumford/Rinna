package org.rinna.cli.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.rinna.cli.service.MonitoringService;
import org.rinna.cli.service.MockMonitoringService;
import org.rinna.cli.service.MockSecurityService;
import org.rinna.cli.service.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

/**
 * Step definitions for admin monitor command tests.
 */
public class AdminMonitorCommandSteps {
    
    private final TestContext context;
    private final CommandProcessor commandProcessor;
    private final MockSecurityService mockSecurityService;
    private final MockMonitoringService mockMonitoringService;
    
    /**
     * Constructor with test context.
     * 
     * @param context the test context
     */
    public AdminMonitorCommandSteps(TestContext context) {
        this.context = context;
        this.commandProcessor = context.getCommandProcessor();
        this.mockSecurityService = context.getMockSecurityService();
        this.mockMonitoringService = context.getMockMonitoringService();
    }
    
    @Given("a monitoring alert {string} exists")
    public void a_monitoring_alert_exists(String alertName) {
        // Set up mock to return true when checking if the alert exists
        when(mockMonitoringService.addAlert(
                eq(alertName), 
                anyString(), 
                anyString(), 
                anyList()))
            .thenReturn(true);
            
        // Mock the alert listing to include the test alert
        when(mockMonitoringService.listAlerts())
            .thenReturn("Monitoring Alerts\n" +
                       "================\n\n" +
                       "Alert           | Metric           | Threshold | Recipients\n" +
                       "----------------|------------------|-----------|----------\n" +
                       alertName + " | CPU Load         | 80%       | admin@example.com\n");
                       
        // Mock the remove alert method
        when(mockMonitoringService.removeAlert(alertName))
            .thenReturn(true);
    }
    
    @Given("I am logged in as an administrator")
    public void i_am_logged_in_as_an_administrator() {
        mockSecurityService.login("admin", "admin123");
        mockSecurityService.setAdminStatus(true);
    }
    
    @Given("I am logged in as a regular user")
    public void i_am_logged_in_as_a_regular_user() {
        mockSecurityService.login("user", "password");
        mockSecurityService.setAdminStatus(false);
    }
    
    // The remaining necessary step definitions are already defined in other step classes
    // like AdminAuditCommandSteps and AdminComplianceCommandSteps
}