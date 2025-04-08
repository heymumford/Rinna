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
        // This would format the object as text
        // In a real implementation, this would have proper formatting logic
        System.out.println(value);
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
}