/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for formatting command output in various formats.
 */
public class OutputFormatter {

    private final boolean jsonOutput;
    
    /**
     * Creates a new OutputFormatter.
     * 
     * @param jsonOutput true to output in JSON format, false for text format
     */
    public OutputFormatter(boolean jsonOutput) {
        this.jsonOutput = jsonOutput;
    }
    
    /**
     * Outputs an object in the configured format.
     * 
     * @param name the name of the object
     * @param value the object to output
     */
    public void outputObject(String name, Object value) {
        if (jsonOutput) {
            outputJson(name, value);
        } else {
            outputText(name, value);
        }
    }
    
    /**
     * Outputs an object as JSON.
     * 
     * @param name the name of the object
     * @param value the object to output
     */
    private void outputJson(String name, Object value) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(name, value);
        
        // Very simple JSON output - in a real implementation, use a proper JSON library
        System.out.println(convertToJson(wrapper));
    }
    
    /**
     * Outputs an object as text.
     * 
     * @param name the name of the object
     * @param value the object to output
     */
    private void outputText(String name, Object value) {
        if (value == null) {
            System.out.println(name + ": null");
            return;
        }
        
        if (value instanceof Map) {
            System.out.println(name + ":");
            Map<?, ?> map = (Map<?, ?>)value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + formatValue(entry.getValue()));
            }
        } else if (value instanceof Collection) {
            System.out.println(name + ":");
            Collection<?> collection = (Collection<?>)value;
            int index = 1;
            for (Object item : collection) {
                System.out.println("  " + index + ". " + formatValue(item));
                index++;
            }
        } else if (value instanceof org.rinna.cli.model.WorkItem) {
            System.out.println(name + ":");
            org.rinna.cli.model.WorkItem workItem = (org.rinna.cli.model.WorkItem)value;
            formatWorkItem(workItem, false);
        } else {
            System.out.println(name + ": " + formatValue(value));
        }
    }
    
    /**
     * Formats a value for text output.
     * 
     * @param value the value to format
     * @return the formatted value as a string
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof org.rinna.cli.model.WorkItem) {
            org.rinna.cli.model.WorkItem workItem = (org.rinna.cli.model.WorkItem)value;
            return workItem.getId() + " - " + workItem.getTitle();
        }
        
        return value.toString();
    }
    
    /**
     * Formats a work item as text.
     * 
     * @param workItem the work item to format
     * @param verbose whether to include detailed information
     */
    private void formatWorkItem(org.rinna.cli.model.WorkItem workItem, boolean verbose) {
        System.out.println("  ID: " + workItem.getId());
        System.out.println("  Title: " + workItem.getTitle());
        System.out.println("  Status: " + (workItem.getStatus() != null ? workItem.getStatus() : "Not set"));
        System.out.println("  Type: " + (workItem.getType() != null ? workItem.getType() : "Not set"));
        System.out.println("  Priority: " + (workItem.getPriority() != null ? workItem.getPriority() : "Not set"));
        
        if (workItem.getAssignee() != null) {
            System.out.println("  Assignee: " + workItem.getAssignee());
        }
        
        if (verbose) {
            System.out.println("  Description: " + workItem.getDescription());
            
            if (workItem.getReporter() != null) {
                System.out.println("  Reporter: " + workItem.getReporter());
            }
            
            if (workItem.getCreated() != null) {
                System.out.println("  Created: " + workItem.getCreated());
            }
            
            if (workItem.getUpdated() != null) {
                System.out.println("  Updated: " + workItem.getUpdated());
            }
            
            if (workItem.getProject() != null) {
                System.out.println("  Project: " + workItem.getProject());
            }
            
            if (workItem.getDueDate() != null) {
                System.out.println("  Due Date: " + workItem.getDueDate());
            }
            
            if (workItem.getVersion() != null) {
                System.out.println("  Version: " + workItem.getVersion());
            }
        }
    }
    
    /**
     * Converts a Map to JSON format.
     * 
     * @param map the map to convert
     * @return a JSON string representation of the map
     */
    public static String toJson(Map<String, Object> map) {
        return convertToJsonStatic(map);
    }
    
    /**
     * Converts a Map to JSON format with verbosity option.
     * 
     * @param map the map to convert
     * @param verbose whether to include detailed information (ignored for maps)
     * @return a JSON string representation of the map
     */
    public static String toJson(Map<String, Object> map, boolean verbose) {
        return convertToJsonStatic(map);
    }
    
    /**
     * Converts a WorkItem to JSON format.
     * 
     * @param workItem the work item to convert
     * @param verbose whether to include detailed information
     * @return a JSON string representation of the work item
     */
    public static String toJson(org.rinna.cli.model.WorkItem workItem, boolean verbose) {
        Map<String, Object> map = new HashMap<>();
        
        // Add basic fields
        map.put("id", workItem.getId());
        map.put("title", workItem.getTitle());
        map.put("status", workItem.getStatus() != null ? workItem.getStatus().toString() : null);
        map.put("type", workItem.getType() != null ? workItem.getType().toString() : null);
        map.put("priority", workItem.getPriority() != null ? workItem.getPriority().toString() : null);
        map.put("assignee", workItem.getAssignee());
        
        // Add detailed fields if verbose mode is enabled
        if (verbose) {
            map.put("description", workItem.getDescription());
            map.put("reporter", workItem.getReporter());
            map.put("created", workItem.getCreated() != null ? workItem.getCreated().toString() : null);
            map.put("updated", workItem.getUpdated() != null ? workItem.getUpdated().toString() : null);
            
            // Add optional fields if they are available
            if (workItem.getProject() != null) {
                map.put("project", workItem.getProject());
            }
            
            if (workItem.getDueDate() != null) {
                map.put("dueDate", workItem.getDueDate().toString());
            }
            
            if (workItem.getVersion() != null) {
                map.put("version", workItem.getVersion());
            }
        }
        
        return convertToJsonStatic(map);
    }
    
    /**
     * Simple method to convert objects to JSON.
     * In a real implementation, use a proper JSON library like Jackson.
     * 
     * @param object the object to convert
     * @return a JSON string
     */
    private String convertToJson(Object object) {
        return convertToJsonStatic(object);
    }
    
    /**
     * Static version of convertToJson for use by static methods.
     * 
     * @param object the object to convert
     * @return a JSON string
     */
    private static String convertToJsonStatic(Object object) {
        if (object == null) {
            return "null";
        }
        
        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>)object;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(convertToJsonStatic(entry.getValue()));
            }
            
            sb.append("}");
            return sb.toString();
        } else if (object instanceof Collection) {
            Collection<?> collection = (Collection<?>)object;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            
            for (Object item : collection) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                
                sb.append(convertToJsonStatic(item));
            }
            
            sb.append("]");
            return sb.toString();
        } else if (object instanceof String) {
            return "\"" + escapeJsonStatic((String)object) + "\"";
        } else if (object instanceof Number || object instanceof Boolean) {
            return object.toString();
        } else {
            // For complex objects, attempt a simple toString conversion
            // In a real implementation, use a proper JSON serialization library
            return "\"" + escapeJsonStatic(object.toString()) + "\"";
        }
    }
    
    /**
     * Escapes special characters in a JSON string.
     * 
     * @param input the string to escape
     * @return the escaped string
     */
    private String escapeJson(String input) {
        return escapeJsonStatic(input);
    }
    
    /**
     * Static version of escapeJson for use by static methods.
     * 
     * @param input the string to escape
     * @return the escaped string
     */
    private static String escapeJsonStatic(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // Check for control characters
                    if (c < 32) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int j = 0; j < 4 - hex.length(); j++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Formats a message as a JSON object with result status, message, and optional data.
     * This is a useful utility for creating consistent JSON responses for CLI commands.
     *
     * @param result the result status ("success", "error", "warning", etc.)
     * @param message the message to include
     * @param data additional data to include in the response (can be null)
     * @return a JSON-formatted string
     */
    public static String formatJsonMessage(String result, String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("message", message);
        
        if (data != null && !data.isEmpty()) {
            // Add all data fields to the response
            response.putAll(data);
        }
        
        return convertToJsonStatic(response);
    }
}