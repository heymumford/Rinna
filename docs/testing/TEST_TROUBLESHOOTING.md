# Test Troubleshooting Guide

This guide provides solutions for common test automation issues encountered in the Rinna project. Use this reference when you encounter test failures or other testing challenges.

## Table of Contents

1. [Understanding Test Failures](#understanding-test-failures)
2. [Common Issues and Solutions](#common-issues-and-solutions)
3. [Language-Specific Troubleshooting](#language-specific-troubleshooting)
4. [CI/CD Pipeline Issues](#cicd-pipeline-issues)
5. [Cross-Language Integration Issues](#cross-language-integration-issues)
6. [Performance Issues](#performance-issues)
7. [Test Environment Problems](#test-environment-problems)
8. [Debugging Techniques](#debugging-techniques)

## Understanding Test Failures

### Reading Test Failure Output

When a test fails, focus on these key parts of the error output:

1. **Test name**: Identifies which test failed
2. **Assertion message**: Explains what expectation was not met
3. **Expected vs. actual values**: Shows the difference between expected and observed behavior
4. **Stack trace**: Indicates where in the code the failure occurred

Example:
```
org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:149)
Expected :TRIAGED
Actual   :FOUND
```

### Categorizing Test Failures

Categorize failures to identify the appropriate solution:

1. **Functional failures**: Test logic is correct, but application code is wrong
2. **Expectation failures**: Test expectations don't match actual requirements
3. **Environment failures**: Test environment is not properly configured
4. **Timing failures**: Race conditions or async issues
5. **Data failures**: Test data is incorrect or missing
6. **Infrastructure failures**: CI pipeline, networking, or system issues

## Common Issues and Solutions

### Flaky Tests

**Symptoms**:
- Tests that sometimes pass and sometimes fail
- Tests that fail only in CI but pass locally
- Tests that fail only under certain timing conditions

**Solutions**:

1. **Add Explicit Waits**:
   ```java
   // Instead of fixed sleep
   Thread.sleep(2000); // Unreliable
   
   // Use explicit wait with timeout
   WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
   wait.until(ExpectedConditions.elementToBeClickable(element));
   ```

2. **Isolate Test State**:
   ```java
   @BeforeEach
   void cleanTestData() {
       // Reset test database or state
       repository.deleteAll();
   }
   ```

3. **Use Test Retries** (for unavoidable flakiness):
   ```java
   // JUnit 5 RetryExtension
   @RetryTest(3)
   void flakyNetworkTest() {
       // Test that may have transient failures
   }
   ```

### Test Dependency Order

**Symptoms**:
- Tests pass when run individually but fail when run as a group
- Tests fail when run in a specific order

**Solutions**:

1. **Ensure Test Independence**:
   ```java
   @BeforeEach
   void setUp() {
       // Create fresh test data for each test
       testData = new TestData();
   }
   
   @AfterEach
   void tearDown() {
       // Clean up any resources or test data
       testData.cleanup();
   }
   ```

2. **Use Test-Specific Resources**:
   ```java
   // Instead of shared resource
   private static final String TEST_FILE = "shared.txt"; // Bad: shared between tests
   
   // Use test-specific resources
   @Test
   void testFileProcessing() {
       String testFile = "test-" + UUID.randomUUID() + ".txt"; // Good: unique per test
       // Use testFile
   }
   ```

### Database Issues

**Symptoms**:
- Tests fail with database connection errors
- Constraint violations during tests
- Database state bleeds between tests

**Solutions**:

1. **Use In-Memory Databases for Testing**:
   ```java
   // Test configuration
   @Bean
   public DataSource dataSource() {
       return new EmbeddedDatabaseBuilder()
           .setType(EmbeddedDatabaseType.H2)
           .build();
   }
   ```

2. **Use Transactions for Test Isolation**:
   ```java
   @Transactional
   @Test
   void databaseTest() {
       // Test that modifies database
       // Changes will be rolled back after test
   }
   ```

3. **Run Test Suite with Clean Database**:
   ```java
   @BeforeAll
   static void initDb() {
       // Initialize clean database schema
       runMigrations();
   }
   ```

### Mocking Issues

**Symptoms**:
- Unexpected invocations of real methods
- `NullPointerException` in mocked objects
- Mock verification failures

**Solutions**:

1. **Verify Mock Setup**:
   ```java
   // Verify mock was initialized properly
   @BeforeEach
   void verifyMocks() {
       assertNotNull(mockService);
       verify(mockService, never()).anyMethod(); // No interactions yet
   }
   ```

2. **Use Spy Carefully**:
   ```java
   // Instead of partial spy that may call real methods
   Service spy = spy(realService);
   
   // Better: Use stub to control behavior fully
   Service stub = mock(Service.class);
   when(stub.method()).thenReturn(expectedValue);
   ```

3. **Check Mock Argument Matchers**:
   ```java
   // Correct: All arguments use matchers
   verify(mockService).process(any(), eq("exact string"), anyInt());
   
   // Incorrect: Mixing matchers and raw values
   verify(mockService).process(any(), "exact string", 5); // Will fail
   ```

## Language-Specific Troubleshooting

### Java Test Issues

1. **JUnit 5 Extension Problems**:
   
   **Issue**: Extensions not running or throwing unexpected errors
   
   **Solution**: 
   ```java
   // Ensure extension is properly registered
   @ExtendWith(MockitoExtension.class)
   public class ServiceTest {
       // Test code
   }
   
   // Or use the automatic extension detection
   // META-INF/services/org.junit.jupiter.api.extension.Extension
   ```

2. **Mockito Stubbing Issues**:
   
   **Issue**: Stubbed methods not returning expected values
   
   **Solution**:
   ```java
   // Check for argument mismatches
   // This won't work if called with different arguments
   when(mockService.getData("test")).thenReturn(result);
   
   // More flexible solution
   when(mockService.getData(any())).thenReturn(result);
   
   // For method chaining, stub all parts of the chain
   when(mockObj.getX().getY().getZ()).thenThrow(new RuntimeException()); // Won't work if getX() returns null
   
   // Instead, set up the full chain
   X x = mock(X.class);
   Y y = mock(Y.class);
   when(mockObj.getX()).thenReturn(x);
   when(x.getY()).thenReturn(y);
   when(y.getZ()).thenThrow(new RuntimeException());
   ```

3. **Spring Test Context Issues**:
   
   **Issue**: Slow tests due to context reloading
   
   **Solution**:
   ```java
   // Use same context configuration across test classes
   @ContextConfiguration(classes = TestConfig.class)
   
   // Or use context caching directives
   @DirtiesContext(classMode = ClassMode.AFTER_CLASS)
   ```

### Go Test Issues

1. **Table-Driven Test Problems**:
   
   **Issue**: Difficult to identify which test case failed
   
   **Solution**:
   ```go
   // Add the test case index and name to error messages
   for i, tc := range testCases {
       t.Run(tc.name, func(t *testing.T) {
           result := Function(tc.input)
           if result != tc.expected {
               t.Errorf("Case %d (%s): got %v, want %v", 
                   i, tc.name, result, tc.expected)
           }
       })
   }
   ```

2. **Concurrent Test Issues**:
   
   **Issue**: Race conditions in concurrent tests
   
   **Solution**:
   ```go
   // Use t.Parallel() with care
   // Ensure test state is isolated
   t.Run("concurrent test", func(t *testing.T) {
       t.Parallel()
       // Use local variables instead of shared state
       localVar := setup()
       // Test using localVar
   })
   
   // Run race detector
   // go test -race ./...
   ```

3. **Testing HTTP Handlers**:
   
   **Issue**: Complex setup for HTTP handler tests
   
   **Solution**:
   ```go
   // Use httptest package for simpler testing
   func TestHandler(t *testing.T) {
       // Create a request
       req := httptest.NewRequest("GET", "/api/items", nil)
       rec := httptest.NewRecorder()
       
       // Call the handler directly
       handler := NewHandler()
       handler.ServeHTTP(rec, req)
       
       // Check the response
       if rec.Code != http.StatusOK {
           t.Errorf("Expected status OK, got %v", rec.Code)
       }
   }
   ```

### Python Test Issues

1. **pytest Fixture Scope Issues**:
   
   **Issue**: Fixtures with incorrect scope causing performance or state problems
   
   **Solution**:
   ```python
   # Use appropriate fixture scope
   
   # For expensive setup shared across tests
   @pytest.fixture(scope="session")
   def database_connection():
       conn = create_connection()
       yield conn
       conn.close()
   
   # For isolated state per test
   @pytest.fixture(scope="function")
   def test_data():
       data = create_test_data()
       yield data
       delete_test_data(data)
   ```

2. **Mocking Issues with pytest-mock**:
   
   **Issue**: Patches not applying correctly
   
   **Solution**:
   ```python
   # Be specific about what is being patched
   
   # Instead of this:
   @mock.patch('module.Class.method')  # May not work if imported elsewhere
   
   # Be more specific:
   @mock.patch('fully.qualified.module.Class.method')
   
   # Or use the mocker fixture for local patching
   def test_function(mocker):
       mock_method = mocker.patch.object(Class, 'method')
       mock_method.return_value = 'mocked result'
   ```

3. **Path and Import Issues**:
   
   **Issue**: Tests can't import modules correctly
   
   **Solution**:
   ```python
   # Create or update conftest.py to modify the Python path
   import sys
   import os
   
   # Add project root to path
   sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
   
   # Or use pytest's configuration
   # pytest.ini
   # [pytest]
   # pythonpath = . src
   ```

## CI/CD Pipeline Issues

### GitHub Actions Failures

1. **Environment Differences**:
   
   **Issue**: Tests pass locally but fail in CI
   
   **Solution**:
   ```yaml
   # Make CI environment more like local
   jobs:
     test:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v2
         
         # Use the same Java version
         - uses: actions/setup-java@v2
           with:
             java-version: '21'
             distribution: 'adopt'
         
         # Set environment variables
         - name: Set environment
           run: |
             echo "ENVIRONMENT=test" >> $GITHUB_ENV
             echo "DEBUG=true" >> $GITHUB_ENV
         
         # Run tests with diagnostic output
         - name: Run tests
           run: ./bin/rin-test --verbose
   ```

2. **Test Discovery Issues**:
   
   **Issue**: CI doesn't find or run the expected tests
   
   **Solution**:
   ```yaml
   # Be explicit about test command
   - name: Run tests
     run: |
       # Print test discovery information
       ./bin/test-discovery.sh
       
       # Run tests with explicit pattern
       ./bin/rin-test
   ```

3. **Resource Constraints**:
   
   **Issue**: Tests timeout or fail due to resource limits in CI
   
   **Solution**:
   ```yaml
   # Adjust resource allocation or test parameters
   - name: Run resource-intensive tests
     run: |
       # Set JVM memory limits
       export JAVA_OPTS="-Xmx2g -Xms512m"
       
       # Run performance tests with reduced load
       ./bin/rin-test performance --scale=0.5
       
       # Or skip certain tests in CI
       ./bin/rin-test --exclude=resource-intensive
   ```

### Build System Issues

1. **Maven Test Execution Problems**:
   
   **Issue**: Maven doesn't run the expected tests
   
   **Solution**:
   ```bash
   # Verify Maven configuration
   mvn help:effective-pom
   
   # Run with test debugging
   mvn test -X
   
   # Check Surefire configuration
   mvn help:effective-pom | grep -A 20 surefire-plugin
   
   # Try more explicit test specification
   mvn -Dtest=org.rinna.unit.** test
   ```

2. **Go Test Discovery Issues**:
   
   **Issue**: Go doesn't find or run tests
   
   **Solution**:
   ```bash
   # Check test files naming
   find . -name "*_test.go"
   
   # Run with verbose flag
   go test -v ./...
   
   # Specify test directly
   go test -v ./path/to/package -run TestName
   ```

## Cross-Language Integration Issues

### Java-Go Communication Problems

1. **API Endpoint Mismatch**:
   
   **Issue**: Java client can't connect to Go API server
   
   **Solution**:
   ```java
   // Add logging to debug request/response
   RestTemplate restTemplate = new RestTemplate();
   restTemplate.getInterceptors().add(new LoggingInterceptor());
   
   // Verify endpoint URLs match
   System.out.println("API URL: " + apiUrl);
   
   // Check for protocol/port issues
   // Ensure Go API is running on expected port
   ```

2. **Data Serialization Issues**:
   
   **Issue**: JSON mapping errors between Java and Go
   
   **Solution**:
   ```java
   // Add debug logging for request/response bodies
   String requestBody = objectMapper.writeValueAsString(request);
   System.out.println("Request: " + requestBody);
   
   // Ensure field names match between Java models and Go structs
   // Java: @JsonProperty("work_item_id")
   // Go: `json:"work_item_id"`
   ```

### CLI Integration Issues

1. **Command Execution Problems**:
   
   **Issue**: CLI commands don't work in tests
   
   **Solution**:
   ```java
   // Capture and log process output
   ProcessBuilder pb = new ProcessBuilder("./bin/rin", "command");
   pb.redirectErrorStream(true);
   Process process = pb.start();
   
   try (BufferedReader reader = new BufferedReader(
           new InputStreamReader(process.getInputStream()))) {
       String line;
       while ((line = reader.readLine()) != null) {
           System.out.println(line);
       }
   }
   
   int exitCode = process.waitFor();
   System.out.println("Exit code: " + exitCode);
   ```

2. **Path and Permission Issues**:
   
   **Issue**: CLI scripts not found or not executable
   
   **Solution**:
   ```java
   // Check and fix executable permissions
   @BeforeAll
   static void ensureExecutable() {
       Path cliPath = Paths.get("./bin/rin");
       if (!Files.isExecutable(cliPath)) {
           Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
           Files.setPosixFilePermissions(cliPath, perms);
       }
   }
   ```

## Performance Issues

### Slow Test Execution

1. **Identify Slow Tests**:
   
   ```bash
   # Enable JUnit timing output
   mvn test -Dsurefire.reportFormat=brief
   
   # Or use custom timing extension
   @ExtendWith(TimingExtension.class)
   public class SlowTest {
       // Tests will be logged with execution time
   }
   ```

2. **Parallelizing Tests**:
   
   ```java
   // JUnit 5 parallel execution
   // junit-platform.properties
   junit.jupiter.execution.parallel.enabled = true
   junit.jupiter.execution.parallel.mode.default = concurrent
   
   // Maven parallel test execution
   // pom.xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-surefire-plugin</artifactId>
       <configuration>
           <parallel>classes</parallel>
           <threadCount>4</threadCount>
       </configuration>
   </plugin>
   ```

3. **Resource Cleanup**:
   
   ```java
   // Ensure proper resource cleanup
   @Test
   void testWithResources() {
       try (Connection conn = createConnection()) {
           // Test using connection
       } // Connection automatically closed
   }
   ```

### Memory Issues

1. **Memory Leaks in Tests**:
   
   ```java
   // Run with Java Flight Recorder to detect memory issues
   // java -XX:+FlightRecorder -XX:StartFlightRecording=filename=recording.jfr test.MainTest
   
   // Add explicit cleanup
   @AfterEach
   void cleanupResources() {
       // Explicitly clear collections
       largeCollection.clear();
       
       // Close resources
       if (resource != null) {
           resource.close();
       }
   }
   ```

2. **Heap Settings for Tests**:
   
   ```bash
   # Set JVM memory parameters
   export MAVEN_OPTS="-Xmx2g -XX:+HeapDumpOnOutOfMemoryError"
   mvn test
   
   # Or for a specific test run
   java -Xmx2g -XX:+HeapDumpOnOutOfMemoryError -cp ... org.junit.runner.JUnitCore TestClass
   ```

## Test Environment Problems

### Environment Setup Issues

1. **Missing Dependencies**:
   
   ```bash
   # Check Java version
   java -version
   
   # Check Go version
   go version
   
   # Check Python version
   python --version
   
   # Run environment setup script
   ./bin/rin-setup-unified --verbose
   ```

2. **Configuration Issues**:
   
   ```bash
   # Verify test config exists and is correct
   cat src/test/resources/application-test.properties
   
   # Create test config if missing
   cp src/main/resources/application.properties src/test/resources/application-test.properties
   # Edit to set test-specific values
   ```

3. **File Path Issues**:
   
   ```java
   // Use absolute paths in tests
   Path resourcePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources");
   
   // Or load test resources from classpath
   InputStream resourceStream = getClass().getResourceAsStream("/test-data.json");
   ```

### Docker Issues in Tests

1. **Container Startup Problems**:
   
   ```java
   // Add container wait logic
   @Container
   static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
       .withStartupTimeout(Duration.ofSeconds(60));
   
   // Wait for container to be ready
   @BeforeAll
   static void waitForContainer() {
       Awaitility.await()
           .atMost(60, TimeUnit.SECONDS)
           .until(() -> {
               try {
                   mysql.createConnection("");
                   return true;
               } catch (Exception e) {
                   return false;
               }
           });
   }
   ```

2. **Container Cleanup Issues**:
   
   ```java
   // Ensure containers are stopped after tests
   @AfterAll
   static void stopContainers() {
       DockerClientFactory.instance().client().stopContainerCmd(containerId).exec();
   }
   ```

## Debugging Techniques

### Java Test Debugging

1. **Enable Logging**:
   
   ```java
   // Configure test logging
   // src/test/resources/logback-test.xml
   <configuration>
     <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
       <encoder>
         <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
       </encoder>
     </appender>
     
     <logger name="org.rinna" level="DEBUG"/>
     
     <root level="INFO">
       <appender-ref ref="STDOUT" />
     </root>
   </configuration>
   ```

2. **Remote Debugging**:
   
   ```bash
   # Run Maven with debugging enabled
   mvn -Dmaven.surefire.debug test
   
   # Then connect with remote debugger to port 5005
   ```

3. **Test-Specific Debugging**:
   
   ```java
   @Test
   void problematicTest() {
       System.out.println("Debug: Starting test");
       
       // Add debugging checkpoints
       var result = methodUnderTest();
       System.out.println("Debug: Result = " + result);
       
       // Track object state
       System.out.println("Debug: Object state = " + object);
       
       // Or use logging framework
       logger.debug("Method returned: {}", result);
   }
   ```

### Go Test Debugging

1. **Verbose Test Output**:
   
   ```bash
   # Run tests with verbose flag
   go test -v ./...
   
   # For even more detail
   go test -v -x ./...
   ```

2. **Function-Level Tracing**:
   
   ```go
   func TestProblem(t *testing.T) {
       t.Logf("Starting test with input: %v", input)
       
       result := functionUnderTest(input)
       t.Logf("Function returned: %v", result)
       
       if result != expected {
           t.Errorf("Expected %v, got %v", expected, result)
       }
   }
   ```

3. **Debugger Integration**:
   
   ```bash
   # Using Delve debugger
   dlv test ./path/to/package -- -test.run TestName
   ```

### Python Test Debugging

1. **pytest Verbose Mode**:
   
   ```bash
   # Run with verbose output
   python -m pytest -v
   
   # Show print statements
   python -m pytest -v --capture=no
   ```

2. **Interactive Debugging**:
   
   ```python
   def test_problem():
       # Add breakpoint
       import pdb; pdb.set_trace()
       
       # Or for Python 3.7+
       breakpoint()
       
       result = function_under_test()
       assert result == expected
   ```

3. **pytest-xdist Debugging**:
   
   ```bash
   # Run problematic test in isolation
   python -m pytest path/to/test.py::test_name -v
   
   # Or use pdb with xdist
   python -m pytest --no-xdist --pdb
   ```

## Advanced Troubleshooting

### Creating a Minimal Reproduction

When you encounter a complex test failure, create a minimal reproduction:

1. Start with a failing test
2. Remove parts unrelated to the failure
3. Simplify test inputs and environment
4. Document precise steps to reproduce

Example:
```java
// Original complex test
@Test
void complexTest() {
    // Setup database
    // Create multiple entities
    // Perform complex operations
    // Assert results
}

// Simplified reproduction
@Test
void simplifiedReproduction() {
    // Only create the minimal necessary entities
    Entity e = new Entity("test");
    
    // Only perform the operation that fails
    service.process(e);
    
    // Only check the failing assertion
    assertEquals(ExpectedState.DONE, e.getState());
}
```

### Creating Custom Test Rules/Extensions

For recurring test problems, create custom solutions:

1. **JUnit 5 Extension for Retrying Flaky Tests**:
   
   ```java
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface RetryTest {
       int value() default 3;
   }
   
   public class RetryExtension implements TestTemplateInvocationContextProvider {
       // Implementation details
   }
   ```

2. **Custom Resource Management Extension**:
   
   ```java
   public class ResourceCleanupExtension implements AfterEachCallback, AfterAllCallback {
       private static final List<AutoCloseable> resources = new ArrayList<>();
       
       public static <T extends AutoCloseable> T register(T resource) {
           resources.add(resource);
           return resource;
       }
       
       @Override
       public void afterEach(ExtensionContext context) {
           // Clean up resources created during test
       }
       
       @Override
       public void afterAll(ExtensionContext context) {
           // Clean up resources created for the class
       }
   }
   ```

### Creating Test Failure Reports

When reporting test failures:

1. Provide test name and exception details
2. Include environment information (JDK/Go/Python version, OS)
3. List steps to reproduce
4. Attach relevant logs and test output
5. Describe expected vs. actual behavior

Example template:
```
Test Failure Report

Test: org.rinna.test.WorkflowTest.shouldTransitionWorkItem
Date/Time: 2025-04-08 14:30 UTC

Environment:
- Java: OpenJDK 21.0.1
- OS: Ubuntu 22.04
- Branch: feature/workflow-enhancements

Error Message:
Expected workflow state to be 'TRIAGED' but was 'FOUND'

Stack Trace:
[Include relevant portion of stack trace]

Steps to Reproduce:
1. Run specific test: mvn test -Dtest=WorkflowTest#shouldTransitionWorkItem
2. Or reproduce with: ./bin/rin-test path:org/rinna/test/WorkflowTest.java

Expected Behavior:
Work item should transition from FOUND to TRIAGED state when processed.

Actual Behavior:
Work item remains in FOUND state after processing.

Logs/Output:
[Include relevant logs]

Potential Causes:
- Authorization check may be failing
- Workflow rules may have changed
- Database state may be inconsistent
```

## Additional Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito FAQ](https://github.com/mockito/mockito/wiki/FAQ)
- [Go Testing Tips](https://golang.org/doc/test-procedure)
- [pytest Documentation](https://docs.pytest.org/)