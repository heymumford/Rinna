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
 * Test for the Demo class.
 */
public class DemoTest {
    
    @Test
    void testGetMessage() {
        // Given
        String message = "Test Message";
        Demo demo = new Demo(message);
        
        // When
        String result = demo.getMessage();
        
        // Then
        assertEquals(message, result);
    }
}