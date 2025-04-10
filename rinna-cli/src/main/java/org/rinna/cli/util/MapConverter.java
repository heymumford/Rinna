/*
 * Map converter utility for Rinna CLI.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting between different map types.
 * This is particularly useful for command implementations that need to
 * interact with both the CLI-specific services and domain services that
 * may have different map parameter types.
 */
public final class MapConverter {
    
    // Private constructor to prevent instantiation
    private MapConverter() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Converts a Map with Object values to a Map with String values.
     * Any null values will be converted to "null" strings.
     * Non-string objects will be converted using toString().
     *
     * @param input The input map with Object values
     * @return A new map with the same keys but String values
     */
    public static Map<String, String> toStringMap(Map<String, Object> input) {
        if (input == null) {
            return new HashMap<>();
        }
        
        Map<String, String> result = new HashMap<>(input.size());
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue().toString() : "null";
            result.put(entry.getKey(), value);
        }
        return result;
    }
    
    /**
     * Converts a Map with String values to a Map with Object values.
     * This is primarily for compatibility with APIs that expect Object values.
     *
     * @param input The input map with String values
     * @return A new map with the same keys but Object values
     */
    public static Map<String, Object> toObjectMap(Map<String, String> input) {
        if (input == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>(input.size());
        result.putAll(input);
        return result;
    }
}