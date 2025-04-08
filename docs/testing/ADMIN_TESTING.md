# Admin Testing Guide for Rinna

This document provides guidance on running, maintaining, and extending the admin functionality tests for the Rinna project.

## Overview

The admin tests validate administrative functionality including:
- User management
- Workflow configuration
- Work item type configuration
- Project management
- Maven integration
- Server auto-launch capabilities
- Audit logging and compliance reporting
- Security investigation and regulatory compliance
- System monitoring and diagnostics
- Performance reporting and alerts
- Backup configuration and automation
- Disaster recovery planning and testing

## Test Structure

Admin tests follow the BDD (Behavior-Driven Development) approach using Cucumber:
- **Feature files** (`*.feature`) define behavior in Gherkin syntax
- **Step definition classes** implement the behavior defined in feature files
- **Test runners** orchestrate test execution

### Key Components

1. **Feature Files** (in `src/test/resources/features/`):
   - `admin-maven-integration.feature`
   - `admin-project-management.feature`
   - `admin-server-autolaunch.feature`
   - `admin-user-management.feature`
   - `admin-workflow-configuration.feature`
   - `admin-workitem-configuration.feature`
   - `admin-audit-compliance.feature`
   - `admin-system-monitoring.feature`
   - `admin-backup-recovery.feature`

2. **Step Definition Classes** (in `src/test/java/org/rinna/bdd/`):
   - `AdminMavenIntegrationSteps.java`
   - `AdminProjectManagementSteps.java`
   - `AdminServerAutolaunchSteps.java`
   - `AdminUserManagementSteps.java`
   - `AdminWorkflowConfigurationSteps.java`
   - `AdminWorkItemConfigurationSteps.java`
   - `AdminAuditComplianceSteps.java`
   - `AdminSystemMonitoringSteps.java`
   - `AdminBackupRecoverySteps.java`

3. **Test Runners** (in `src/test/java/org/rinna/bdd/`):
   - `AdminConfigurationRunner.java` - For workflow, user, and work item configuration
   - `AdminIntegrationRunner.java` - For Maven integration and server auto-launch
   - `AdminProjectRunner.java` - For project management
   - `AdminAuditComplianceRunner.java` - For audit logging and compliance functionality
   - `AdminSystemMonitoringRunner.java` - For system monitoring and diagnostics
   - `AdminBackupRecoveryRunner.java` - For backup and disaster recovery
   - `AdminFeaturesRunner.java` - For all admin tests

## Running Admin Tests

### Using the Dedicated Script

The most convenient way to run admin tests is using the dedicated script:

```bash
# Run all admin tests
./bin/run-admin-tests.sh

# Run specific categories
./bin/run-admin-tests.sh --integration  # Maven integration & server auto-launch
./bin/run-admin-tests.sh --config       # Workflow, work item & user configuration
./bin/run-admin-tests.sh --project      # Project management
./bin/run-admin-tests.sh --audit        # Audit logging and reporting
./bin/run-admin-tests.sh --compliance   # Regulatory compliance features
./bin/run-admin-tests.sh --security     # Both audit and compliance tests
./bin/run-admin-tests.sh --monitoring   # System monitoring and diagnostics
./bin/run-admin-tests.sh --backup       # Backup and disaster recovery

# Run with additional options
./bin/run-admin-tests.sh --verbose      # Show detailed output
./bin/run-admin-tests.sh --parallel     # Run tests in parallel
./bin/run-admin-tests.sh --tag=@smoke   # Run tests with specific tag
```

### Using the rin-test Command

You can also use the standard `rin-test` command:

```bash
# Run all admin tests
./bin/rin-test admin

# Run with additional options
./bin/rin-test admin --verbose
./bin/rin-test admin --parallel
```

### Using Maven Directly

For more control, you can use Maven directly:

```bash
# Run all admin tests
mvn clean test -Dtest='org.rinna.bdd.Admin*Runner'

# Run specific admin test runner
mvn clean test -Dtest='org.rinna.bdd.AdminConfigurationRunner'
```

## Extending Admin Tests

### Adding New Feature Files

1. Create a new feature file in `src/test/resources/features/` with the `@admin` tag
2. Write scenarios using Given/When/Then format
3. Include descriptive scenario names and appropriate tags

Example:
```gherkin
@admin @permissions
Feature: Admin Permission Management
  As an administrator
  I want to manage user permissions
  So that I can control access to system functionality

  Scenario: Create a new role with specific permissions
    Given I am logged in as an administrator
    When I create a new role "Developer" with permissions:
      | Permission      | Value |
      | ViewWorkItems   | true  |
      | EditWorkItems   | true  |
      | DeleteWorkItems | false |
    Then the role "Developer" should be created successfully
    And the role should have the specified permissions
```

### Implementing Step Definitions

1. Create a new step definition class in `src/test/java/org/rinna/bdd/`
2. Implement methods for each step with appropriate annotations
3. Use the shared `TestContext` to maintain state between steps

Example:
```java
package org.rinna.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.datatable.DataTable;

import java.util.List;
import java.util.Map;

public class AdminPermissionManagementSteps {
    private final TestContext context;
    
    public AdminPermissionManagementSteps(TestContext context) {
        this.context = context;
    }
    
    @When("I create a new role {string} with permissions:")
    public void createRoleWithPermissions(String roleName, DataTable permissionsTable) {
        List<Map<String, String>> permissions = permissionsTable.asMaps();
        // Implementation code to create a role with permissions
        
        // Store in context for verification in subsequent steps
        context.put("currentRole", roleName);
        context.put("currentPermissions", permissions);
    }
    
    @Then("the role {string} should be created successfully")
    public void verifyRoleCreated(String roleName) {
        // Verification code
    }
    
    @Then("the role should have the specified permissions")
    public void verifyRolePermissions() {
        // Verification code using context.get("currentPermissions")
    }
}
```

### Creating a New Test Runner

To create a specialized test runner for a new category of admin tests:

1. Create a new runner class in `src/test/java/org/rinna/bdd/`
2. Configure it with appropriate annotations
3. Update `run-admin-tests.sh` to include the new category

Example:
```java
package org.rinna.bdd;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
        value = "pretty, json:target/cucumber-reports/admin-permissions-report.json, " +
                "html:target/cucumber-reports/admin-permissions-report.html")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.bdd")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@admin and @permissions")
public class AdminPermissionsRunner {
    // This class is intentionally empty.
    // Its purpose is to be a holder for JUnit Platform Cucumber Suite annotations.
}
```

## Best Practices

1. **Use Descriptive Names**:
   - Feature files should clearly describe the functionality
   - Step definition methods should be descriptive and readable

2. **Proper Tagging**:
   - Always include the `@admin` tag for admin-related features
   - Add specific tags like `@user`, `@workflow`, `@project` for categorization
   - Use tags like `@smoke` or `@critical` for test prioritization

3. **State Management**:
   - Use `TestContext` for sharing state between step definition classes
   - Clear context before each scenario to avoid leakage

4. **Reuse Steps**:
   - Create common step methods in base classes for reuse
   - Use parameterized steps for similar behaviors

5. **Data-Driven Tests**:
   - Use Scenario Outlines for testing multiple data variations
   - Use DataTables for complex input data

6. **Effective Assertions**:
   - Make meaningful assertions that validate business rules
   - Include descriptive error messages in assertions

7. **Clean Up**:
   - Use `@After` hooks to clean up resources
   - Restore the system to its original state after tests

## Troubleshooting

### Common Issues

1. **Tests not finding feature files**:
   - Ensure feature files are in the correct location
   - Check `@SelectClasspathResource` paths in runners

2. **Step definitions not matching**:
   - Check for typos or inconsistencies between feature file and step definitions
   - Verify regex patterns in annotations

3. **Context sharing issues**:
   - Ensure `TestContext` is properly injected into step classes
   - Verify objects are correctly stored and retrieved

4. **Test runners not executing**:
   - Check Maven surefire configuration
   - Verify class naming matches test patterns

### Getting Help

If you encounter issues with admin tests:
- Check the test execution logs
- Review the Cucumber reports in `target/cucumber-reports/`
- Consult the project documentation for more specific troubleshooting