# Test-Driven Development Practical Guide

This guide provides practical steps for applying Test-Driven Development (TDD) principles to the Rinna project's multi-language environment. TDD is particularly valuable in polyglot architectures to ensure consistent behavior across language boundaries.

## Table of Contents

1. [TDD Fundamentals](#tdd-fundamentals)
2. [TDD in a Multi-Language Environment](#tdd-in-a-multi-language-environment)
3. [Language-Specific TDD Approaches](#language-specific-tdd-approaches)
4. [Step-by-Step TDD Workflow](#step-by-step-tdd-workflow)
5. [Cross-Language TDD Patterns](#cross-language-tdd-patterns)
6. [Common Challenges and Solutions](#common-challenges-and-solutions)
7. [TDD Tools and Techniques](#tdd-tools-and-techniques)
8. [Case Studies](#case-studies)

## TDD Fundamentals

### The TDD Cycle

Test-Driven Development follows a simple cycle:

1. **Red**: Write a failing test that defines the expected behavior
2. **Green**: Write the minimal implementation to make the test pass
3. **Refactor**: Improve the code without changing its behavior

This cycle is repeated for each small increment of functionality.

### Benefits of TDD in Rinna

- **Clear Requirements**: Tests serve as executable specifications
- **Incremental Development**: Build functionality in small, manageable steps
- **Immediate Feedback**: Detect issues early in the development cycle
- **Maintainable Design**: TDD naturally leads to modular, loosely coupled designs
- **Cross-Language Consistency**: Ensures consistent behavior across language boundaries
- **Documentation**: Tests serve as living documentation of system behavior

## TDD in a Multi-Language Environment

In Rinna's polyglot architecture (Java, Go, Python), TDD requires special considerations:

### Contract-First Development

1. **Define Cross-Language Contracts First**:
   - JSON/API schemas
   - Command-line interfaces
   - File formats
   - Event structures

2. **Write Tests Against Contracts**:
   - Each language component tests against the same contract
   - Ensures consistent interpretation of requirements across languages

3. **Implement Each Language Component Separately**:
   - Follow the TDD cycle for each component
   - Ensure all components satisfy the contract tests

### Test Hierarchy in Multi-Language Systems

For features spanning multiple languages, follow this testing hierarchy:

1. **Unit Tests**: Language-specific behavior (Java, Go, Python separately)
2. **Contract Tests**: Interface conformance (each language against the contract)
3. **Integration Tests**: Cross-language communication (Java calling Go, etc.)
4. **End-to-End Tests**: Complete workflows across all language boundaries

## Language-Specific TDD Approaches

### Java TDD

```java
// Step 1: Write a failing test
@Test
void shouldCreateWorkItem() {
    // Arrange
    WorkItemCreateRequest request = new WorkItemCreateRequest("Test Item", WorkItemType.TASK);
    
    // Act
    WorkItem result = itemService.createWorkItem(request);
    
    // Assert
    assertNotNull(result.getId());
    assertEquals("Test Item", result.getTitle());
    assertEquals(WorkItemType.TASK, result.getType());
    assertEquals(WorkflowState.CREATED, result.getState());
}

// Step 2: Write minimal implementation to make it pass
public WorkItem createWorkItem(WorkItemCreateRequest request) {
    WorkItem item = new DefaultWorkItem();
    item.setId(generateId());
    item.setTitle(request.getTitle());
    item.setType(request.getType());
    item.setState(WorkflowState.CREATED);
    return item;
}

// Step 3: Refactor for clean code
public WorkItem createWorkItem(WorkItemCreateRequest request) {
    if (request == null) {
        throw new IllegalArgumentException("Request cannot be null");
    }
    
    WorkItem item = WorkItemFactory.create(request);
    item.setState(WorkflowState.CREATED);
    
    repository.save(item);
    return item;
}
```

**Java TDD Tools**:
- JUnit 5 for tests
- Mockito for mocking
- AssertJ for readable assertions
- JaCoCo for coverage

### Go TDD

```go
// Step 1: Write a failing test
func TestCreateWorkItem(t *testing.T) {
    // Arrange
    request := &model.WorkItemCreateRequest{
        Title: "Test Item",
        Type:  model.WorkItemTypeTask,
    }
    
    // Act
    item, err := service.CreateWorkItem(request)
    
    // Assert
    if err != nil {
        t.Fatalf("Expected no error, got %v", err)
    }
    
    if item.ID == 0 {
        t.Error("Expected ID to be set")
    }
    
    if item.Title != "Test Item" {
        t.Errorf("Expected title to be 'Test Item', got '%s'", item.Title)
    }
    
    if item.Type != model.WorkItemTypeTask {
        t.Errorf("Expected type to be task, got %v", item.Type)
    }
    
    if item.State != model.WorkflowStateCreated {
        t.Errorf("Expected state to be created, got %v", item.State)
    }
}

// Step 2: Write minimal implementation to make it pass
func (s *service) CreateWorkItem(request *model.WorkItemCreateRequest) (*model.WorkItem, error) {
    item := &model.WorkItem{
        ID:    s.generateID(),
        Title: request.Title,
        Type:  request.Type,
        State: model.WorkflowStateCreated,
    }
    
    return item, nil
}

// Step 3: Refactor for clean code
func (s *service) CreateWorkItem(request *model.WorkItemCreateRequest) (*model.WorkItem, error) {
    if request == nil {
        return nil, errors.New("request cannot be nil")
    }
    
    item, err := model.NewWorkItem(request)
    if err != nil {
        return nil, fmt.Errorf("failed to create work item: %w", err)
    }
    
    item.State = model.WorkflowStateCreated
    
    if err := s.repository.Save(item); err != nil {
        return nil, fmt.Errorf("failed to save work item: %w", err)
    }
    
    return item, nil
}
```

**Go TDD Tools**:
- Standard `testing` package
- `testify` for assertions
- Table-driven tests for multiple scenarios
- `gomock` for mocking

### Python TDD

```python
# Step 1: Write a failing test
def test_create_work_item():
    # Arrange
    request = WorkItemCreateRequest(title="Test Item", type=WorkItemType.TASK)
    
    # Act
    result = item_service.create_work_item(request)
    
    # Assert
    assert result.id is not None
    assert result.title == "Test Item"
    assert result.type == WorkItemType.TASK
    assert result.state == WorkflowState.CREATED

# Step 2: Write minimal implementation to make it pass
def create_work_item(request):
    item = WorkItem()
    item.id = generate_id()
    item.title = request.title
    item.type = request.type
    item.state = WorkflowState.CREATED
    return item

# Step 3: Refactor for clean code
def create_work_item(request):
    if request is None:
        raise ValueError("Request cannot be None")
    
    item = WorkItem.from_request(request)
    item.state = WorkflowState.CREATED
    
    repository.save(item)
    return item
```

**Python TDD Tools**:
- pytest for tests
- pytest-mock for mocking
- pytest-bdd for behavior-driven development
- pytest-cov for coverage

## Step-by-Step TDD Workflow

### 1. Plan the Feature

Before writing any code, define:
- The behavior you want to implement
- The interfaces between components
- The test scenarios to cover

Example planning for a cross-language feature:

```
Feature: Work Item Management API
- Java CLI will create work items
- Go API will store and manage work items
- Python utilities will generate reports on work items

Contract:
- Work item JSON schema
- REST API endpoints for CRUD operations
- Command-line interface for item creation

Test Scenarios:
1. Create work item via CLI
2. Retrieve work item via API
3. Update work item status
4. Generate work item report
```

### 2. Write Contract Tests

Start by writing tests against the contracts between components:

```java
// Java test for API contract
@Test
void shouldSerializeWorkItemToValidJson() {
    // Arrange
    WorkItem item = new DefaultWorkItem();
    item.setId(123L);
    item.setTitle("Test");
    item.setType(WorkItemType.TASK);
    
    // Act
    String json = objectMapper.writeValueAsString(item);
    
    // Assert
    JsonNode node = objectMapper.readTree(json);
    assertEquals(123, node.get("id").asLong());
    assertEquals("Test", node.get("title").asText());
    assertEquals("TASK", node.get("type").asText());
}
```

```go
// Go test for API contract
func TestDeserializeWorkItemFromJson(t *testing.T) {
    // Arrange
    json := `{"id":123,"title":"Test","type":"TASK"}`
    
    // Act
    var item model.WorkItem
    err := json.Unmarshal([]byte(json), &item)
    
    // Assert
    if err != nil {
        t.Fatalf("Failed to deserialize: %v", err)
    }
    
    if item.ID != 123 {
        t.Errorf("Expected ID 123, got %d", item.ID)
    }
    
    if item.Title != "Test" {
        t.Errorf("Expected title 'Test', got '%s'", item.Title)
    }
    
    if item.Type != model.WorkItemTypeTask {
        t.Errorf("Expected type TASK, got %v", item.Type)
    }
}
```

### 3. Implement Language-Specific Components

For each language component:

1. Write failing tests for the component
2. Implement the minimal code to make tests pass
3. Refactor the code for clarity and maintainability
4. Verify that contract tests still pass

### 4. Implement Cross-Language Integration

Once individual components work correctly:

1. Write integration tests for cross-language communication
2. Implement the integration code
3. Run end-to-end tests to verify complete workflows

Example Java-Go integration test:

```java
@Test
void shouldCreateWorkItemThroughApi() {
    // Arrange - Ensure Go API server is running
    ApiClient client = new ApiClient("http://localhost:8080");
    WorkItemCreateRequest request = new WorkItemCreateRequest("Integration Test", WorkItemType.TASK);
    
    // Act
    WorkItem created = client.createWorkItem(request);
    
    // Assert
    assertNotNull(created.getId());
    assertEquals("Integration Test", created.getTitle());
    
    // Verify retrievable
    WorkItem retrieved = client.getWorkItem(created.getId());
    assertEquals(created.getId(), retrieved.getId());
}
```

### 5. Implement End-to-End Workflow

Write BDD-style tests for complete workflows:

```gherkin
Feature: Work Item Management
  
  Scenario: Create and report on work item
    Given the API server is running
    When I create a work item via CLI with title "TDD Example"
    Then the work item should be created successfully
    And I can retrieve the work item via API
    When I generate a report for the work item
    Then the report should include the work item details
```

### 6. Refactor Across Languages

After the feature works end-to-end:

1. Identify opportunities for improvement in each language component
2. Refactor components while keeping tests passing
3. Verify cross-language integration still works

## Cross-Language TDD Patterns

### Contract-Test-First Pattern

1. Define the contract between language components
2. Write contract tests in all languages
3. Implement each component using TDD
4. Integrate components and verify against contract tests

Example for REST API contract:

```
# Swagger Contract (shared between languages)
paths:
  /api/workitems:
    post:
      summary: Create a work item
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/WorkItemCreateRequest'
      responses:
        '200':
          description: Work item created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/WorkItem'
```

### Mock-Server Pattern

1. Write API client in language A (e.g., Java)
2. Create mock server in language B (e.g., Go)
3. Test client against mock server
4. Implement real server using TDD
5. Replace mock server with real implementation

Example mock server in Go:

```go
func createMockServer() *httptest.Server {
    return httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        if r.URL.Path == "/api/workitems" && r.Method == "POST" {
            // Parse request body
            var req struct {
                Title string `json:"title"`
                Type  string `json:"type"`
            }
            json.NewDecoder(r.Body).Decode(&req)
            
            // Send mock response
            w.Header().Set("Content-Type", "application/json")
            json.NewEncoder(w).Encode(map[string]interface{}{
                "id":    123,
                "title": req.Title,
                "type":  req.Type,
                "state": "CREATED",
            })
        } else {
            w.WriteHeader(http.StatusNotFound)
        }
    }))
}
```

### Shared-Test-Data Pattern

1. Define test data sets in a language-neutral format (e.g., JSON)
2. Load test data in tests for all languages
3. Verify consistent behavior across languages
4. Use for regression testing of cross-language features

Example shared test data:

```json
{
  "testWorkItems": [
    {
      "id": 1,
      "title": "Implement login feature",
      "type": "FEATURE",
      "state": "IN_PROGRESS"
    },
    {
      "id": 2,
      "title": "Fix navigation bug",
      "type": "BUG",
      "state": "FOUND"
    }
  ]
}
```

## Common Challenges and Solutions

### Challenge: Different Testing Styles Across Languages

**Solution**:
- Define common testing principles (e.g., arrange-act-assert pattern)
- Create language-specific examples of these principles
- Use shared descriptive naming conventions

### Challenge: Mock Consistency

**Solution**:
- Create shared mock behavior specifications
- Implement language-specific mocks following these specifications
- Validate mock behavior consistency with integration tests

### Challenge: Test Data Management

**Solution**:
- Store test data in language-neutral formats (JSON, YAML)
- Create test data loaders for each language
- Ensure consistent interpretation of test data across languages

### Challenge: Different Library Capabilities

**Solution**:
- Abstract differences behind adapters
- Focus tests on behavior, not implementation details
- Use wrapper libraries to harmonize capabilities

## TDD Tools and Techniques

### Automated Test Runners

Each language has its own test runner:

```bash
# Java tests with Maven
mvn test

# Go tests
go test ./...

# Python tests with pytest
python -m pytest
```

Use the unified test runner for consistent execution:

```bash
# Run all tests
./bin/rin-test

# Run language-specific tests
./bin/rin-test --only=java unit
./bin/rin-test --only=go unit
./bin/rin-test --only=python unit
```

### Continuous Testing

Set up continuous testing to run tests on code changes:

```bash
# Watch and run tests on changes
./bin/rin-test --watch

# Run tests in parallel
./bin/rin-test --parallel
```

### Code Coverage

Monitor test coverage for all languages:

```bash
# Generate unified coverage report
./bin/polyglot-coverage.sh

# View coverage report
open target/coverage/index.html
```

## Case Studies

### Case Study 1: TDD for API Endpoints

This case study demonstrates using TDD to implement a new API endpoint across Java client and Go server components.

#### Step 1: Define the Contract

Create a Swagger specification for the new endpoint:

```yaml
/api/projects:
  post:
    summary: Create a new project
    requestBody:
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ProjectCreateRequest'
    responses:
      '200':
        description: Project created successfully
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Project'
```

#### Step 2: Write Java Client Tests

```java
@Test
void shouldCreateProject() {
    // Arrange
    ProjectCreateRequest request = new ProjectCreateRequest("Test Project", "TEST");
    
    // Act
    Project project = apiClient.createProject(request);
    
    // Assert
    assertNotNull(project.getId());
    assertEquals("Test Project", project.getName());
    assertEquals("TEST", project.getCode());
}
```

#### Step 3: Implement Java Client

```java
public Project createProject(ProjectCreateRequest request) {
    ResponseEntity<Project> response = restTemplate.postForEntity(
        baseUrl + "/api/projects",
        request,
        Project.class
    );
    
    if (!response.getStatusCode().is2xxSuccessful()) {
        throw new ApiException("Failed to create project: " + response.getStatusCode());
    }
    
    return response.getBody();
}
```

#### Step 4: Write Go Server Tests

```go
func TestCreateProject(t *testing.T) {
    // Arrange
    handler := NewProjectHandler(mockRepository)
    
    request := &model.ProjectCreateRequest{
        Name: "Test Project",
        Code: "TEST",
    }
    
    body, _ := json.Marshal(request)
    req := httptest.NewRequest("POST", "/api/projects", bytes.NewBuffer(body))
    req.Header.Set("Content-Type", "application/json")
    
    w := httptest.NewRecorder()
    
    // Act
    handler.CreateProject(w, req)
    
    // Assert
    resp := w.Result()
    if resp.StatusCode != http.StatusOK {
        t.Errorf("Expected status OK, got %v", resp.Status)
    }
    
    var project model.Project
    json.NewDecoder(resp.Body).Decode(&project)
    
    if project.ID == 0 {
        t.Error("Expected project ID to be set")
    }
    
    if project.Name != "Test Project" {
        t.Errorf("Expected name 'Test Project', got '%s'", project.Name)
    }
    
    if project.Code != "TEST" {
        t.Errorf("Expected code 'TEST', got '%s'", project.Code)
    }
}
```

#### Step 5: Implement Go Server Handler

```go
func (h *ProjectHandler) CreateProject(w http.ResponseWriter, r *http.Request) {
    var request model.ProjectCreateRequest
    if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    
    project := &model.Project{
        ID:   h.generateID(),
        Name: request.Name,
        Code: request.Code,
    }
    
    if err := h.repository.SaveProject(project); err != nil {
        http.Error(w, "Failed to save project", http.StatusInternalServerError)
        return
    }
    
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(project)
}
```

#### Step 6: Implement Integration Test

Create an integration test that uses the Java client to communicate with the Go server:

```java
@Test
void shouldCreateAndRetrieveProjectAcrossLanguages() {
    // Start Go API server
    Process apiProcess = startApiServer();
    try {
        // Create client
        ApiClient client = new ApiClient("http://localhost:8080");
        
        // Create project using Java client
        ProjectCreateRequest request = new ProjectCreateRequest("Integration Project", "INT");
        Project created = client.createProject(request);
        
        // Verify project was created
        assertNotNull(created.getId());
        assertEquals("Integration Project", created.getName());
        
        // Retrieve project using Java client
        Project retrieved = client.getProject(created.getId());
        
        // Verify retrieved project matches
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getName(), retrieved.getName());
        assertEquals(created.getCode(), retrieved.getCode());
        
    } finally {
        // Stop API server
        apiProcess.destroy();
    }
}
```

### Case Study 2: TDD for CLI Commands

This case study demonstrates using TDD to implement a new CLI command that interacts with the API.

#### Step 1: Define Command Interface

```
Command: rin project create
Description: Creates a new project
Arguments:
  --name: Project name (required)
  --code: Project code (required)
  --description: Project description (optional)
```

#### Step 2: Write CLI Command Tests

```java
@Test
void shouldCreateProjectViaCommand() {
    // Arrange
    ProjectCommand command = new ProjectCommand();
    String[] args = {"create", "--name=Test Project", "--code=TEST"};
    PrintStream originalOut = System.out;
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    
    // Mock API client
    ApiClient mockClient = mock(ApiClient.class);
    Project mockProject = new Project();
    mockProject.setId(123L);
    mockProject.setName("Test Project");
    mockProject.setCode("TEST");
    when(mockClient.createProject(any())).thenReturn(mockProject);
    command.setApiClient(mockClient);
    
    try {
        // Act
        int exitCode = command.execute(args);
        
        // Assert
        assertEquals(0, exitCode);
        assertTrue(outContent.toString().contains("Project created successfully"));
        assertTrue(outContent.toString().contains("ID: 123"));
        
        // Verify client called with correct request
        ArgumentCaptor<ProjectCreateRequest> requestCaptor = 
            ArgumentCaptor.forClass(ProjectCreateRequest.class);
        verify(mockClient).createProject(requestCaptor.capture());
        
        ProjectCreateRequest request = requestCaptor.getValue();
        assertEquals("Test Project", request.getName());
        assertEquals("TEST", request.getCode());
        
    } finally {
        System.setOut(originalOut);
    }
}
```

#### Step 3: Implement CLI Command

```java
public class ProjectCommand {
    private ApiClient apiClient;
    
    public ProjectCommand() {
        this.apiClient = new ApiClient();
    }
    
    // For testing
    void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    public int execute(String[] args) {
        if (args.length < 1) {
            System.err.println("No subcommand specified");
            return 1;
        }
        
        String subcommand = args[0];
        if ("create".equals(subcommand)) {
            return executeCreate(Arrays.copyOfRange(args, 1, args.length));
        }
        
        System.err.println("Unknown subcommand: " + subcommand);
        return 1;
    }
    
    private int executeCreate(String[] args) {
        String name = null;
        String code = null;
        String description = null;
        
        for (String arg : args) {
            if (arg.startsWith("--name=")) {
                name = arg.substring("--name=".length());
            } else if (arg.startsWith("--code=")) {
                code = arg.substring("--code=".length());
            } else if (arg.startsWith("--description=")) {
                description = arg.substring("--description=".length());
            }
        }
        
        if (name == null || code == null) {
            System.err.println("Missing required arguments");
            System.err.println("Usage: rin project create --name=<name> --code=<code> [--description=<desc>]");
            return 1;
        }
        
        try {
            ProjectCreateRequest request = new ProjectCreateRequest(name, code);
            request.setDescription(description);
            
            Project project = apiClient.createProject(request);
            
            System.out.println("Project created successfully:");
            System.out.println("ID: " + project.getId());
            System.out.println("Name: " + project.getName());
            System.out.println("Code: " + project.getCode());
            
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to create project: " + e.getMessage());
            return 1;
        }
    }
}
```

#### Step 4: Integration Test

Write a test that runs the actual CLI command and verifies it calls the API correctly:

```java
@Test
void shouldCreateProjectViaRealCommand() {
    // Set up mock API server
    MockServer server = new MockServer();
    server.addResponse("/api/projects", "POST", 200, 
        (request) -> {
            String body = request.getBody();
            JsonNode node = objectMapper.readTree(body);
            String name = node.get("name").asText();
            String code = node.get("code").asText();
            
            return String.format(
                "{\"id\":999,\"name\":\"%s\",\"code\":\"%s\"}",
                name, code
            );
        }
    );
    server.start();
    
    try {
        // Run CLI command
        Process process = new ProcessBuilder(
            "./bin/rin", "project", "create", 
            "--name=CLI Project", 
            "--code=CLI"
        ).start();
        
        // Get command output
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        // Wait for process to complete
        int exitCode = process.waitFor();
        
        // Verify command succeeded
        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("Project created successfully"));
        assertTrue(output.toString().contains("ID: 999"));
        assertTrue(output.toString().contains("Name: CLI Project"));
        assertTrue(output.toString().contains("Code: CLI"));
        
        // Verify request received by server
        List<String> requests = server.getRequests("/api/projects", "POST");
        assertEquals(1, requests.size());
        JsonNode requestBody = objectMapper.readTree(requests.get(0));
        assertEquals("CLI Project", requestBody.get("name").asText());
        assertEquals("CLI", requestBody.get("code").asText());
        
    } finally {
        server.stop();
    }
}
```

## Additional Resources

- [Test Automation Guide](TEST_AUTOMATION_GUIDE.md) - Complete guide to test automation
- [Test Templates](TEST_TEMPLATES.md) - Ready-to-use test templates
- [Test Compatibility Matrix](TEST_COMPATIBILITY_MATRIX.md) - Framework for cross-language testing
- [Test Troubleshooting Guide](TEST_TROUBLESHOOTING.md) - Solutions for common test issues