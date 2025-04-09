# Test-Driven Development Guide for Rinna CLI

This guide provides instructions and best practices for test-driven development (TDD) in the Rinna CLI module.

## TDD Approach

The Rinna CLI module follows a complete test-driven development approach with these principles:

1. **Write Tests First**: Always write tests before implementing features
2. **Red-Green-Refactor**: Make tests fail, make them pass, then improve the code
3. **Clean Architecture**: Keep testing aligned with clean architecture principles
4. **Multiple Test Levels**: Use different types of tests for different aspects of the system

## Test Types and Locations

The CLI module uses these test types, organized in specific locations for clarity:

### 1. Unit Tests

- **Purpose**: Test individual classes and methods in isolation
- **Location**: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/unit/`
- **Naming**: `*Test.java`
- **Framework**: JUnit 5
- **Mocking**: Mockito
- **Examples**: `ModelMapperTest.java`

### 2. Command Tests

- **Purpose**: Test CLI commands with mocked dependencies
- **Location**: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/command/`
- **Naming**: `*CommandTest.java`
- **Framework**: JUnit 5
- **Mocking**: Mockito
- **Examples**: `GrepCommandTest.java`, `ListCommandTest.java`

### 3. Component Tests

- **Purpose**: Test interactions between multiple units
- **Location**: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/component/`
- **Naming**: `*ComponentTest.java`
- **Framework**: JUnit 5
- **Examples**: `CommandExecutionComponentTest.java`

### 4. BDD Tests

- **Purpose**: Test user behavior and scenarios
- **Feature Files**: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/features/`
- **Step Definitions**: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/bdd/`
- **Runner**: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/bdd/CucumberRunner.java`
- **Framework**: Cucumber with JUnit 5
- **Examples**: `grep-command.feature`, `GrepCommandSteps.java`

## TDD Workflow

Follow this workflow when implementing new features:

### Step 1: Write Feature Files

Start by writing Cucumber feature files that describe the behavior from a user's perspective:

```gherkin
# /home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/features/my-command.feature
@bdd @myfeature
Feature: My Command Functionality
  As a user of the Rinna CLI
  I want to use the my-command
  So that I can perform a specific task

  Scenario: Basic usage of my-command
    When I run the my-command with option "foo"
    Then the command should execute successfully
    And the output should contain "Expected result"
```

### Step 2: Write Step Definitions

Implement the step definitions to translate the feature file into code:

```java
// /home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/bdd/MyCommandSteps.java
package org.rinna.cli.bdd;

import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.rinna.cli.command.MyCommand;

public class MyCommandSteps {
    
    private final TestContext testContext = TestContext.getInstance();
    
    @When("I run the my-command with option {string}")
    public void iRunMyCommandWithOption(String option) {
        MyCommand command = new MyCommand();
        command.setOption(option);
        
        testContext.resetCapturedOutput();
        int exitCode = command.call();
        
        testContext.setLastCommandOutput(testContext.getStandardOutput());
        testContext.setLastCommandExitCode(exitCode);
    }
    
    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        assertEquals(0, testContext.getLastCommandExitCode(), 
                "Command should have executed successfully with exit code 0");
    }
    
    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        String output = testContext.getLastCommandOutput();
        assertTrue(output.contains(expectedText), 
                "Output should contain: " + expectedText);
    }
}
```

### Step 3: Create Command Unit Tests

Write unit tests for the command class:

```java
// /home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/command/MyCommandTest.java
package org.rinna.cli.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("My Command Tests")
public class MyCommandTest {
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private MyCommand command;
    private ServiceManager mockServiceManager;
    
    @BeforeEach
    void setUp() {
        // Redirect console output
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize command and mocks
        command = new MyCommand();
        mockServiceManager = mock(ServiceManager.class);
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Nested
    @DisplayName("Basic Command Tests")
    class BasicCommandTests {
        
        @Test
        @DisplayName("Should execute successfully with valid option")
        void shouldExecuteSuccessfullyWithValidOption() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                command.setOption("foo");
                int result = command.call();
                
                assertEquals(0, result, "Command should return 0 for success");
                assertTrue(outContent.toString().contains("Expected result"), 
                           "Output should contain expected result");
            }
        }
        
        @Test
        @DisplayName("Should fail with invalid option")
        void shouldFailWithInvalidOption() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                command.setOption("invalid");
                int result = command.call();
                
                assertEquals(1, result, "Command should return 1 for failure");
                assertTrue(errContent.toString().contains("Invalid option"), 
                           "Error output should explain the issue");
            }
        }
    }
}
```

### Step 4: Implement the Command

Now implement the actual command with the minimal code needed to pass the tests:

```java
// /home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/main/java/org/rinna/cli/command/MyCommand.java
package org.rinna.cli.command;

import org.rinna.cli.service.ServiceManager;
import java.util.concurrent.Callable;

public class MyCommand implements Callable<Integer> {
    
    private String option;
    private final ServiceManager serviceManager = ServiceManager.getInstance();
    
    public void setOption(String option) {
        this.option = option;
    }
    
    @Override
    public Integer call() {
        try {
            if (option == null || !option.equals("foo")) {
                System.err.println("Invalid option: " + option);
                return 1;
            }
            
            System.out.println("Expected result");
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
```

### Step 5: Run Tests and Refactor

Run the tests to ensure they pass, then refactor as needed:

```bash
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-cli
mvn test -Dtest=MyCommandTest
mvn test -P bdd-only -Dcucumber.filter.tags="@myfeature"
```

## Testing Best Practices

### 1. Command Structure

Each command test should follow this structure:

```java
@Nested
@DisplayName("Help Documentation Tests")
class HelpDocumentationTests {
    // Tests for help documentation and usage
}

@Nested
@DisplayName("Positive Test Cases")
class PositiveTestCases {
    // Tests for normal, expected behavior
}

@Nested
@DisplayName("Negative Test Cases")
class NegativeTestCases {
    // Tests for error conditions and invalid inputs
}

@Nested
@DisplayName("Contract Tests")
class ContractTests {
    // Tests for interactions with dependencies
}

@Nested
@DisplayName("Integration Tests")
class IntegrationTests {
    // Tests with real or realistic dependencies
}
```

### 2. BDD Style

For BDD tests, use these best practices:

- One feature file per command or feature
- Clear Given-When-Then structure
- Meaningful tags for organization
- Shared test context for state
- Comprehensive step definitions

### 3. Clean Architecture

Ensure tests respect Clean Architecture principles:

- Domain logic tests should not depend on UI or infrastructure
- Adapter tests should focus on interface with external systems
- Use mocks for dependencies outside the current layer
- Maintain separation of concerns in tests

### 4. File Path Standards

Always use absolute paths in documentation and configuration:

- `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/pom.xml`
- `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/features/`
- `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/bdd/`

### 5. Mock Services

Use tracking mock services to verify interactions:

```java
private static class TrackingService extends MockService {
    private final List<String> methodCalls = new ArrayList<>();
    
    @Override
    public void someMethod(String parameter) {
        methodCalls.add(parameter);
        super.someMethod(parameter);
    }
    
    public List<String> getMethodCalls() {
        return methodCalls;
    }
}
```

## Test Configuration

The testing infrastructure is configured through these files:

1. Maven POM: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/pom.xml`
2. JUnit Properties: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/junit-platform.properties`
3. Cucumber Properties: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/cucumber.properties`

Key configuration settings include:

- Cucumber tags for test selection
- Parallel execution settings
- Report generation
- Plugin configuration

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Cucumber Documentation](https://cucumber.io/docs/cucumber/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Clean Architecture in Java](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)