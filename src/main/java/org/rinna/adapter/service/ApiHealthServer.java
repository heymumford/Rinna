/*
 * API Server for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.rinna.Rinna;
import org.rinna.domain.model.DefaultWorkItem;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.usecase.ItemService;

import java.util.ArrayList;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * A simple HTTP server that provides health check endpoints and API endpoints.
 */
public class ApiHealthServer {
    private final HttpServer server;
    private final int port;
    private final ItemService itemService;
    
    /**
     * Creates a new API Server.
     *
     * @param port the port to listen on
     * @throws IOException if the server cannot be created
     */
    public ApiHealthServer(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Get the item service from Rinna
        this.itemService = Rinna.initialize().items();
        
        // Create a thread pool with a fixed number of threads
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // Register the health check handlers
        server.createContext("/health", new HealthHandler());
        server.createContext("/health/live", new LivenessHandler());
        server.createContext("/health/ready", new ReadinessHandler());
        
        // Register the API handlers
        server.createContext("/api/workitems", new WorkItemsHandler());
    }
    
    /**
     * Starts the server.
     */
    public void start() {
        server.start();
        System.out.println("Rinna API Server started on port " + port);
    }
    
    /**
     * Stops the server.
     */
    public void stop() {
        server.stop(0);
        System.out.println("Rinna API Server stopped");
    }
    
    /**
     * Handler for the /health endpoint.
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
                return;
            }
            
            String response = String.format("""
                {
                  "status": "ok",
                  "timestamp": "%s",
                  "version": "1.2.4",
                  "javaVersion": "%s",
                  "memory": {
                    "total": %d,
                    "free": %d,
                    "max": %d
                  },
                  "services": {
                    "core": {
                      "status": "ok",
                      "timestamp": "%s"
                    }
                  }
                }
                """,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                System.getProperty("java.version"),
                Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().maxMemory(),
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Handler for the /health/live endpoint.
     */
    static class LivenessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
                return;
            }
            
            String response = String.format("""
                {
                  "status": "ok",
                  "timestamp": "%s"
                }
                """,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Handler for the /health/ready endpoint.
     */
    static class ReadinessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
                return;
            }
            
            String response = String.format("""
                {
                  "status": "ok",
                  "timestamp": "%s",
                  "services": {
                    "core": {
                      "status": "ok",
                      "timestamp": "%s"
                    }
                  }
                }
                """,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Handler for the /api/workitems endpoint.
     */
    class WorkItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Set common headers
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            
            // Route based on method and path
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            
            try {
                if (path.equals("/api/workitems")) {
                    if (method.equals("GET")) {
                        handleListWorkItems(exchange);
                    } else if (method.equals("POST")) {
                        handleCreateWorkItem(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                } else if (path.startsWith("/api/workitems/")) {
                    String id = path.substring("/api/workitems/".length());
                    if (path.endsWith("/transitions")) {
                        if (method.equals("POST")) {
                            handleTransitionWorkItem(exchange, id.substring(0, id.length() - "/transitions".length()));
                        } else {
                            sendMethodNotAllowed(exchange);
                        }
                    } else {
                        if (method.equals("GET")) {
                            handleGetWorkItem(exchange, id);
                        } else if (method.equals("PUT")) {
                            handleUpdateWorkItem(exchange, id);
                        } else {
                            sendMethodNotAllowed(exchange);
                        }
                    }
                } else {
                    sendNotFound(exchange);
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendInternalServerError(exchange, e.getMessage());
            }
        }
        
        /**
         * Handles the GET /api/workitems request.
         */
        private void handleListWorkItems(HttpExchange exchange) throws IOException {
            // Parse query parameters
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);
            
            String status = params.get("status");
            int page = parseInt(params.get("page"), 1);
            int pageSize = parseInt(params.get("pageSize"), 10);
            String assignee = params.get("assignee");
            String projectId = params.get("project");
            String priority = params.get("priority");
            
            // Get all work items and apply filters
            List<WorkItem> items;
            if (status != null && !status.isEmpty()) {
                // Get items filtered by status if provided
                items = itemService.findByStatus(status);
            } else {
                // Get all items
                items = itemService.findAll();
            }
            
            // Apply additional filters if provided
            if (assignee != null && !assignee.isEmpty()) {
                items = items.stream()
                    .filter(item -> assignee.equals(item.assignee()))
                    .collect(Collectors.toList());
            }
            
            if (projectId != null && !projectId.isEmpty()) {
                items = items.stream()
                    .filter(item -> projectId.equals(item.project()))
                    .collect(Collectors.toList());
            }
            
            if (priority != null && !priority.isEmpty()) {
                try {
                    Priority priorityEnum = Priority.valueOf(priority.toUpperCase());
                    items = items.stream()
                        .filter(item -> priorityEnum.equals(item.priority()))
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    // Invalid priority, ignore this filter
                }
            }
            
            // Calculate pagination
            int totalCount = items.size();
            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalCount);
            
            // Create sublist for pagination (if valid indices)
            List<WorkItem> pagedItems = startIndex < totalCount ? 
                items.subList(startIndex, endIndex) : new ArrayList<>();
                
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("items", pagedItems);
            response.put("totalCount", totalCount);
            response.put("page", page);
            response.put("pageSize", pageSize);
            
            // Build the JSON response manually
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\n")
                .append("  \"items\": [");
            
            boolean first = true;
            for (WorkItem item : items) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                first = false;
                
                jsonBuilder.append("\n    {\n")
                    .append("      \"id\": \"").append(item.getId()).append("\",\n")
                    .append("      \"title\": \"").append(item.getTitle()).append("\",\n")
                    .append("      \"type\": \"").append(item.getType()).append("\",\n")
                    .append("      \"priority\": \"").append(item.getPriority()).append("\",\n")
                    .append("      \"status\": \"").append(item.getStatus()).append("\"\n")
                    .append("    }");
            }
            
            jsonBuilder.append("\n  ],\n")
                .append("  \"totalCount\": ").append(items.size()).append(",\n")
                .append("  \"page\": ").append(page).append(",\n")
                .append("  \"pageSize\": ").append(pageSize).append("\n")
                .append("}");
                
            String responseJson = jsonBuilder.toString();
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseJson.getBytes());
            }
        }
        
        /**
         * Handles the POST /api/workitems request.
         */
        private void handleCreateWorkItem(HttpExchange exchange) throws IOException {
            // Parse request body
            String requestBody = readRequestBody(exchange);
            Map<String, String> requestData = parseJsonToMap(requestBody);
            
            // Extract required fields
            String title = requestData.get("title");
            String description = requestData.get("description");
            String typeStr = requestData.get("type");
            String priorityStr = requestData.get("priority");
            
            // Validate
            if (title == null || title.isEmpty()) {
                sendBadRequest(exchange, "Title is required");
                return;
            }
            
            // Default to CHORE type if not specified
            WorkItemType type = WorkItemType.CHORE;
            if (typeStr != null && !typeStr.isEmpty()) {
                try {
                    type = WorkItemType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    sendBadRequest(exchange, "Invalid type: " + typeStr);
                    return;
                }
            }
            
            // Default to MEDIUM priority if not specified
            Priority priority = Priority.MEDIUM;
            if (priorityStr != null && !priorityStr.isEmpty()) {
                try {
                    priority = Priority.valueOf(priorityStr);
                } catch (IllegalArgumentException e) {
                    sendBadRequest(exchange, "Invalid priority: " + priorityStr);
                    return;
                }
            }
            
            // Create the work item request using the builder pattern
            WorkItemCreateRequest createRequest = new WorkItemCreateRequest.Builder()
                .title(title)
                .description(description != null ? description : "")
                .type(type)
                .priority(priority)
                .build();
            
            // Save the work item
            WorkItem workItem = itemService.create(createRequest);
            
            // Build response JSON manually
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\n")
                .append("  \"id\": \"").append(workItem.getId()).append("\",\n")
                .append("  \"title\": \"").append(workItem.getTitle()).append("\",\n")
                .append("  \"type\": \"").append(workItem.getType()).append("\",\n")
                .append("  \"priority\": \"").append(workItem.getPriority()).append("\",\n")
                .append("  \"status\": \"").append(workItem.getStatus()).append("\"\n")
                .append("}");
                
            String responseJson = jsonBuilder.toString();
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseJson.getBytes());
            }
        }
        
        /**
         * Handles the GET /api/workitems/{id} request.
         */
        private void handleGetWorkItem(HttpExchange exchange, String id) throws IOException {
            // Find the work item
            try {
                UUID uuid = UUID.fromString(id);
                Optional<WorkItem> workItemOpt = itemService.findById(uuid);
                
                if (workItemOpt.isEmpty()) {
                    sendNotFound(exchange);
                    return;
                }
                
                WorkItem workItem = workItemOpt.get();
            
                // Send response
                // Build response JSON manually
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\n")
                    .append("  \"id\": \"").append(workItem.getId()).append("\",\n")
                    .append("  \"title\": \"").append(workItem.getTitle()).append("\",\n")
                    .append("  \"type\": \"").append(workItem.getType()).append("\",\n")
                    .append("  \"priority\": \"").append(workItem.getPriority()).append("\",\n")
                    .append("  \"status\": \"").append(workItem.getStatus()).append("\"\n")
                    .append("}");
                    
                String responseJson = jsonBuilder.toString();
                exchange.sendResponseHeaders(200, responseJson.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes());
                }
            } catch (IllegalArgumentException e) {
                sendBadRequest(exchange, "Invalid ID format");
                return;
            }
        }
        
        /**
         * Handles the PUT /api/workitems/{id} request.
         */
        private void handleUpdateWorkItem(HttpExchange exchange, String id) throws IOException {
            // Find the work item
            try {
                UUID uuid = UUID.fromString(id);
                Optional<WorkItem> workItemOpt = itemService.findById(uuid);
                
                if (workItemOpt.isEmpty()) {
                    sendNotFound(exchange);
                    return;
                }
                
                WorkItem workItem = workItemOpt.get();
                
                // Parse request body
                String requestBody = readRequestBody(exchange);
                Map<String, String> requestData = parseJsonToMap(requestBody);
                
                // For now, only update the assignee which is the one operation supported by the interface
                if (requestData.containsKey("assignee")) {
                    String assignee = requestData.get("assignee");
                    workItem = itemService.updateAssignee(UUID.fromString(id), assignee);
                } else {
                    // Send a bad request since we don't support other update operations in this MVP
                    sendBadRequest(exchange, "Only assignee updates are supported in this version");
                    return;
                }
                
                // Send response
                // Build response JSON manually
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\n")
                    .append("  \"id\": \"").append(workItem.getId()).append("\",\n")
                    .append("  \"title\": \"").append(workItem.getTitle()).append("\",\n")
                    .append("  \"type\": \"").append(workItem.getType()).append("\",\n")
                    .append("  \"priority\": \"").append(workItem.getPriority()).append("\",\n")
                    .append("  \"status\": \"").append(workItem.getStatus()).append("\"\n")
                    .append("}");
                    
                String responseJson = jsonBuilder.toString();
                exchange.sendResponseHeaders(200, responseJson.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes());
                }
            } catch (IllegalArgumentException e) {
                sendBadRequest(exchange, "Invalid ID format");
                return;
            }
        }
        
        /**
         * Handles the POST /api/workitems/{id}/transitions request.
         */
        private void handleTransitionWorkItem(HttpExchange exchange, String id) throws IOException {
            // Find the work item
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
                Optional<WorkItem> workItemOpt = itemService.findById(uuid);
                
                if (workItemOpt.isEmpty()) {
                    sendNotFound(exchange);
                    return;
                }
            
            // Parse request body
            String requestBody = readRequestBody(exchange);
            Map<String, String> requestData = parseJsonToMap(requestBody);
            
                // Extract the target state
                String toStateStr = requestData.get("toState");
                if (toStateStr == null || toStateStr.isEmpty()) {
                    sendBadRequest(exchange, "ToState is required");
                    return;
                }
                
                // Convert to workflow state
                WorkflowState toState;
                try {
                    toState = WorkflowState.valueOf(toStateStr);
                } catch (IllegalArgumentException e) {
                    sendBadRequest(exchange, "Invalid state: " + toStateStr);
                    return;
                }
                
                // Transition the work item
                try {
                    // Use the workflow service to transition the work item
                    WorkItem transitionedItem = Rinna.initialize().workflow().transition(
                        uuid,
                        toState
                    );
                    
                    // Build response JSON manually
                    StringBuilder jsonBuilder = new StringBuilder();
                    jsonBuilder.append("{\n")
                        .append("  \"id\": \"").append(transitionedItem.getId()).append("\",\n")
                        .append("  \"title\": \"").append(transitionedItem.getTitle()).append("\",\n")
                        .append("  \"type\": \"").append(transitionedItem.getType()).append("\",\n")
                        .append("  \"priority\": \"").append(transitionedItem.getPriority()).append("\",\n")
                        .append("  \"status\": \"").append(transitionedItem.getStatus()).append("\"\n")
                        .append("}");
                        
                    String responseJson = jsonBuilder.toString();
                    exchange.sendResponseHeaders(200, responseJson.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseJson.getBytes());
                    }
                } catch (Exception e) {
                    sendBadRequest(exchange, "Invalid transition: " + e.getMessage());
                }
            } catch (IllegalArgumentException e) {
                sendBadRequest(exchange, "Invalid ID format");
                return;
            }
        }
        
        /**
         * Reads the request body as a string.
         */
        private String readRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                return br.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
        
        /**
         * Parses a JSON string into a Map of key-value pairs.
         * This is a very basic implementation that handles only simple JSON objects.
         */
        private Map<String, String> parseJsonToMap(String json) {
            Map<String, String> result = new HashMap<>();
            
            // Strip the { } brackets
            json = json.trim();
            if (json.startsWith("{")) {
                json = json.substring(1);
            }
            if (json.endsWith("}")) {
                json = json.substring(0, json.length() - 1);
            }
            
            // Split by commas, but not commas in quotes
            boolean inQuotes = false;
            StringBuilder sb = new StringBuilder();
            List<String> pairs = new ArrayList<>();
            
            for (char c : json.toCharArray()) {
                if (c == '"') {
                    inQuotes = !inQuotes;
                    sb.append(c);
                } else if (c == ',' && !inQuotes) {
                    pairs.add(sb.toString().trim());
                    sb = new StringBuilder();
                } else {
                    sb.append(c);
                }
            }
            
            // Add the last pair
            if (sb.length() > 0) {
                pairs.add(sb.toString().trim());
            }
            
            // Process each pair
            for (String pair : pairs) {
                int colonIndex = pair.indexOf(':');
                if (colonIndex > 0) {
                    String key = pair.substring(0, colonIndex).trim();
                    String value = pair.substring(colonIndex + 1).trim();
                    
                    // Remove quotes from key
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    
                    // Remove quotes from value
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    result.put(key, value);
                }
            }
            
            return result;
        }
        
        /**
         * Parses query parameters.
         */
        private Map<String, String> parseQueryParams(String query) {
            if (query == null || query.isEmpty()) {
                return Collections.emptyMap();
            }
            
            Map<String, String> params = new HashMap<>();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    params.put(pair.substring(0, idx), pair.substring(idx + 1));
                }
            }
            
            return params;
        }
        
        /**
         * Parses an integer, returning a default value if parsing fails.
         */
        private int parseInt(String value, int defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        /**
         * Sends a 400 Bad Request response.
         */
        private void sendBadRequest(HttpExchange exchange, String message) throws IOException {
            String response = String.format("""
                {
                  "error": "%s"
                }
                """, message);
            
            exchange.sendResponseHeaders(400, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
        
        /**
         * Sends a 404 Not Found response.
         */
        private void sendNotFound(HttpExchange exchange) throws IOException {
            String response = """
                {
                  "error": "Resource not found"
                }
                """;
            
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
        
        /**
         * Sends a 405 Method Not Allowed response.
         */
        private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
            String response = """
                {
                  "error": "Method not allowed"
                }
                """;
            
            exchange.sendResponseHeaders(405, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
        
        /**
         * Sends a 500 Internal Server Error response.
         */
        private void sendInternalServerError(HttpExchange exchange, String message) throws IOException {
            String response = String.format("""
                {
                  "error": "Internal server error",
                  "message": "%s"
                }
                """, message != null ? message : "Unknown error");
            
            exchange.sendResponseHeaders(500, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Main method to run the server standalone.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Default port is 8081
            int port = 8081;
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[0]);
                    System.exit(1);
                }
            }
            
            ApiHealthServer server = new ApiHealthServer(port);
            server.start();
            
            // Add a shutdown hook to stop the server gracefully
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            
            System.out.println("Press Ctrl+C to stop the server");
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}