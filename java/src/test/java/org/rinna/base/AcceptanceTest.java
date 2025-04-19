package org.rinna.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for acceptance tests.
 * 
 * Acceptance tests:
 * - Verify system meets business requirements
 * - End-to-end workflows
 * - Usually use the full system
 */
@Tag("acceptance")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AcceptanceTest extends BaseTest {
}
