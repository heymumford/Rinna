/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.model;

/**
 * Domain enum for priority levels - used to minimize dependency issues
 * during CLI module migration. This is a temporary class that matches
 * the core domain Priority enum.
 */
public enum DomainPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}