/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.test;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Helper class for managing parallel test execution.
 * Provides utilities for test isolation and resource locking.
 */
public class ParallelExecutionManager {
    
    // Static locks for controlling access to shared resources
    private static final ReentrantLock CONSOLE_LOCK = new ReentrantLock();
    private static final ReentrantLock FILE_SYSTEM_LOCK = new ReentrantLock();
    private static final ReentrantLock SINGLETON_LOCK = new ReentrantLock();
    
    /**
     * Annotation for tests that should run in isolation.
     * This prevents parallel execution of annotated tests.
     */
    @Execution(ExecutionMode.SAME_THREAD)
    @ResourceLock("org.rinna.cli.test.isolation")
    public @interface IsolatedTest {
    }
    
    /**
     * Annotation for tests that require console access.
     * This ensures only one test at a time can capture console output.
     */
    @ResourceLock("org.rinna.cli.test.console")
    public @interface ConsoleAccess {
    }
    
    /**
     * Annotation for tests that interact with the file system.
     * This ensures file system operations don't conflict.
     */
    @ResourceLock("org.rinna.cli.test.filesystem")
    public @interface FileSystemAccess {
    }
    
    /**
     * Annotation for tests that manipulate singleton instances.
     * This ensures singleton operations don't conflict.
     */
    @ResourceLock("org.rinna.cli.test.singleton")
    public @interface SingletonAccess {
    }
    
    /**
     * Locks the console for exclusive access.
     * Use this in a try-with-resources block to ensure proper release.
     * 
     * @return The acquired lock
     */
    public static AutoCloseable lockConsole() {
        CONSOLE_LOCK.lock();
        return CONSOLE_LOCK::unlock;
    }
    
    /**
     * Locks the file system for exclusive access.
     * Use this in a try-with-resources block to ensure proper release.
     * 
     * @return The acquired lock
     */
    public static AutoCloseable lockFileSystem() {
        FILE_SYSTEM_LOCK.lock();
        return FILE_SYSTEM_LOCK::unlock;
    }
    
    /**
     * Locks singleton access for exclusive operations.
     * Use this in a try-with-resources block to ensure proper release.
     * 
     * @return The acquired lock
     */
    public static AutoCloseable lockSingleton() {
        SINGLETON_LOCK.lock();
        return SINGLETON_LOCK::unlock;
    }
    
    /**
     * JUnit extension to manage isolation and cleanup for parallel tests.
     * This handles exceptions and ensures resources are properly cleaned up.
     */
    @ExtendWith(ParallelExecutionManager.IsolationExtension.class)
    public @interface ManagedExecution {
    }
    
    /**
     * JUnit extension that manages test isolation and cleanup.
     */
    public static class IsolationExtension implements TestExecutionExceptionHandler {
        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            // Ensure locks are released if test fails
            CONSOLE_LOCK.unlock();
            FILE_SYSTEM_LOCK.unlock();
            SINGLETON_LOCK.unlock();
            
            throw throwable;
        }
    }
}