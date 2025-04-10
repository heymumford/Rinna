/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.integration.api;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.rinna.base.IntegrationTest;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cross-language integration tests for WorkItem operations between Java and Go components.
 * These tests verify that Java and Go components can properly interact with shared WorkItem data.
 */
@Tag("integration")
@Tag("polyglot")
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("Cross-Language WorkItem Integration Tests")
public class CrossLanguageWorkItemTest extends IntegrationTest {
    
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private String apiUrl;
    
    private String testItemId;
    
    @BeforeAll
    void setUp() {
        // Initialize HTTP client for API requests
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        
        // Initialize JSON mapper
        objectMapper = new ObjectMapper();
        
        // Get API port from environment or use default
        String apiPort = System.getenv("RINNA_TEST_API_PORT");
        if (apiPort == null || apiPort.isEmpty()) {
            apiPort = "8085"; // Default test port
        }
        
        apiUrl = "http://localhost:" + apiPort + "/api";
        
        // Verify API is available
        try {
            HttpRequest healthRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/health"))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(healthRequest, BodyHandlers.ofString());
            
            assertEquals(200, response.statusCode(), "API server should be available");
        } catch (Exception e) {
            fail("API server not available: " + e.getMessage());
        }
    }
    
    @AfterAll
    void tearDown() {
        // Clean up test work item if it was created
        if (testItemId != null) {
            try {
                HttpRequest deleteRequest = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "/workitems/" + testItemId))
                        .DELETE()
                        .build();
                
                httpClient.send(deleteRequest, BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("Failed to clean up test work item: " + e.getMessage());
            }
        }
    }
    
    @Test
    @DisplayName("Should create WorkItem in Java and retrieve it via Go API")
    void shouldCreateWorkItemInJavaAndRetrieveItViaGoApi() throws IOException, InterruptedException {
        // Create a work item using Java domain model
        WorkItemCreateRequest createRequest = new WorkItemCreateRequest();
        createRequest.setTitle("Cross-language test from Java");
        createRequest.setType(WorkItemType.TASK);
        createRequest.setPriority(Priority.HIGH);
        createRequest.setDescription("Testing Java to Go communication");
        
        // Serialize to JSON
        String requestBody = objectMapper.writeValueAsString(createRequest);
        
        // Send request to Go API
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/workitems"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> createResponse = httpClient.send(createRequest, BodyHandlers.ofString());
        
        // Verify response
        assertEquals(201, createResponse.statusCode(), "Should return 201 Created status");
        
        // Parse response to get ID
        Map<String, Object> responseMap = objectMapper.readValue(createResponse.body(), Map.class);
        testItemId = (String) responseMap.get("id");
        
        assertNotNull(testItemId, "Response should contain work item ID");
        
        // Now retrieve the item via the Go API
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/workitems/" + testItemId))
                .GET()
                .build();
        
        HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
        
        // Verify the retrieved item
        assertEquals(200, getResponse.statusCode(), "Should return 200 OK status");
        
        Map<String, Object> itemMap = objectMapper.readValue(getResponse.body(), Map.class);
        assertEquals(testItemId, itemMap.get("id"), "ID should match");
        assertEquals("Cross-language test from Java", itemMap.get("title"), "Title should match");
        assertEquals("TASK", itemMap.get("type"), "Type should match");
        assertEquals("HIGH", itemMap.get("priority"), "Priority should match");
        assertEquals("Testing Java to Go communication", itemMap.get("description"), "Description should match");
    }
    
    @Test
    @DisplayName("Should update WorkItem via Go API and verify changes in Java model")
    void shouldUpdateWorkItemViaGoApiAndVerifyChangesInJavaModel() throws IOException, InterruptedException {
        // Skip if no test item was created
        if (testItemId == null) {
            fail("Test item not available - previous test may have failed");
        }
        
        // Update the work item via the Go API
        Map<String, Object> updateData = Map.of(
            "state", "IN_PROGRESS",
            "description", "Updated by Go API test"
        );
        
        String updateBody = objectMapper.writeValueAsString(updateData);
        
        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/workitems/" + testItemId))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                .build();
        
        HttpResponse<String> updateResponse = httpClient.send(updateRequest, BodyHandlers.ofString());
        
        // Verify update was successful
        assertEquals(200, updateResponse.statusCode(), "Should return 200 OK status");
        
        // Retrieve the updated item
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/workitems/" + testItemId))
                .GET()
                .build();
        
        HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
        
        // Parse into Java domain model
        WorkItem workItem = objectMapper.readValue(getResponse.body(), WorkItem.class);
        
        // Verify Java model reflects the updates made via Go API
        assertEquals(testItemId, workItem.getId(), "ID should match");
        assertEquals("Cross-language test from Java", workItem.getTitle(), "Title should be unchanged");
        assertEquals(WorkflowState.IN_PROGRESS, workItem.getState(), "State should be updated to IN_PROGRESS");
        assertEquals("Updated by Go API test", workItem.getDescription(), "Description should be updated");
    }
    
    @Test
    @DisplayName("Should handle error responses from Go API correctly in Java")
    void shouldHandleErrorResponsesFromGoApiCorrectlyInJava() throws IOException, InterruptedException {
        // Attempt to get a non-existent work item
        String nonExistentId = "non-existent-id";
        
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/workitems/" + nonExistentId))
                .GET()
                .build();
        
        HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
        
        // Verify error response
        assertEquals(404, getResponse.statusCode(), "Should return 404 Not Found status");
        
        // Parse error response
        Map<String, Object> errorMap = objectMapper.readValue(getResponse.body(), Map.class);
        assertTrue(errorMap.containsKey("error"), "Error response should contain 'error' field");
        
        // Attempt to create an invalid work item (missing required fields)
        Map<String, Object> invalidItem = Map.of(
            "type", "INVALID_TYPE"
        );
        
        String invalidBody = objectMapper.writeValueAsString(invalidItem);
        
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/workitems"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidBody))
                .build();
        
        HttpResponse<String> createResponse = httpClient.send(createRequest, BodyHandlers.ofString());
        
        // Verify error response
        assertTrue(createResponse.statusCode() >= 400, "Should return error status code");
        
        // Parse error response
        errorMap = objectMapper.readValue(createResponse.body(), Map.class);
        assertTrue(errorMap.containsKey("error"), "Error response should contain 'error' field");
    }
}