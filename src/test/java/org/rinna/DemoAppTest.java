/*
 * Component of the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for the DemoApp class.
 */
public class DemoAppTest {
    
    @Test
    void testGetGreeting() {
        // When
        String result = DemoApp.getGreeting();
        
        // Then
        assertEquals("Hello, Rinna!", result);
    }
}