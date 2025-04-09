package org.rinna.samples;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelloRinnaTest {
    @Test
    void testGetGreeting() {
        assertEquals("Hello, Rinna!", HelloRinna.getGreeting());
    }
}
