/*
 * Multi-language logging system for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Multi-language logging system that provides a unified logging interface
 * for Java, Go, Python, and Bash components of the Rinna system.
 * <p>
 * This class acts as a bridge between the different logging systems,
 * ensuring consistent log format, levels, and context fields across
 * all languages.
 */
public class MultiLanguageLogger {
    private static final String DEFAULT_LOG_DIR = System.getProperty("user.home") + "/.rinna/logs";
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final ConcurrentHashMap<String, MultiLanguageLogger> loggers = new ConcurrentHashMap<>();
    
    private final Logger logger;
    private final String name;
    private final Map<String, String> contextFields = new HashMap<>();
    
    /**
     * Private constructor to prevent direct instantiation.
     * Use {@link #getLogger(String)} instead.
     *
     * @param name the logger name
     */
    private MultiLanguageLogger(String name) {
        this.name = name;
        this.logger = LoggerFactory.getLogger(name);
    }
    
    /**
     * Initializes the multi-language logging system.
     * This method should be called once at application startup.
     */
    public static synchronized void initialize() {
        if (initialized.get()) {
            return;
        }
        
        // Ensure log directory exists
        String logDir = System.getenv("RINNA_LOG_DIR");
        if (logDir == null || logDir.isEmpty()) {
            logDir = DEFAULT_LOG_DIR;
        }
        
        Path logDirPath = Paths.get(logDir);
        try {
            Files.createDirectories(logDirPath);
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + logDirPath);
            e.printStackTrace();
        }
        
        // Set system property for logback
        System.setProperty("rinna.log.dir", logDir);
        
        // Set the LOG_DIR environment variable for child processes
        System.setProperty("RINNA_LOG_DIR", logDir);
        
        // Install JUL bridge to forward java.util.logging to SLF4J
        org.rinna.config.LoggingBridge.installJulBridge();
        
        initialized.set(true);
        
        // Get root logger and log initialization
        Logger rootLogger = LoggerFactory.getLogger(MultiLanguageLogger.class);
        rootLogger.info("Multi-language logging system initialized");
    }
    
    /**
     * Gets a logger with the specified name.
     *
     * @param name the logger name
     * @return a MultiLanguageLogger instance
     */
    public static MultiLanguageLogger getLogger(String name) {
        if (!initialized.get()) {
            initialize();
        }
        
        return loggers.computeIfAbsent(name, MultiLanguageLogger::new);
    }
    
    /**
     * Gets a logger with the specified class.
     *
     * @param clazz the class
     * @return a MultiLanguageLogger instance
     */
    public static MultiLanguageLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }
    
    /**
     * Creates a new logger with the specified context field.
     *
     * @param key the field key
     * @param value the field value
     * @return a new logger with the field added
     */
    public MultiLanguageLogger withField(String key, String value) {
        MultiLanguageLogger newLogger = new MultiLanguageLogger(this.name);
        newLogger.contextFields.putAll(this.contextFields);
        newLogger.contextFields.put(key, value);
        return newLogger;
    }
    
    /**
     * Creates a new logger with the specified context fields.
     *
     * @param fields the fields to add
     * @return a new logger with the fields added
     */
    public MultiLanguageLogger withFields(Map<String, String> fields) {
        MultiLanguageLogger newLogger = new MultiLanguageLogger(this.name);
        newLogger.contextFields.putAll(this.contextFields);
        newLogger.contextFields.putAll(fields);
        return newLogger;
    }
    
    /**
     * Logs a message at trace level.
     *
     * @param message the message to log
     */
    public void trace(String message) {
        log(() -> logger.trace(message));
    }
    
    /**
     * Logs a message at debug level.
     *
     * @param message the message to log
     */
    public void debug(String message) {
        log(() -> logger.debug(message));
    }
    
    /**
     * Logs a message at info level.
     *
     * @param message the message to log
     */
    public void info(String message) {
        log(() -> logger.info(message));
    }
    
    /**
     * Logs a message at warn level.
     *
     * @param message the message to log
     */
    public void warn(String message) {
        log(() -> logger.warn(message));
    }
    
    /**
     * Logs a message at error level.
     *
     * @param message the message to log
     */
    public void error(String message) {
        log(() -> logger.error(message));
    }
    
    /**
     * Logs a message at error level with an exception.
     *
     * @param message the message to log
     * @param t the exception
     */
    public void error(String message, Throwable t) {
        log(() -> logger.error(message, t));
    }
    
    /**
     * Logs a message at trace level with a format and arguments.
     *
     * @param format the message format
     * @param args the arguments
     */
    public void trace(String format, Object... args) {
        log(() -> logger.trace(format, args));
    }
    
    /**
     * Logs a message at debug level with a format and arguments.
     *
     * @param format the message format
     * @param args the arguments
     */
    public void debug(String format, Object... args) {
        log(() -> logger.debug(format, args));
    }
    
    /**
     * Logs a message at info level with a format and arguments.
     *
     * @param format the message format
     * @param args the arguments
     */
    public void info(String format, Object... args) {
        log(() -> logger.info(format, args));
    }
    
    /**
     * Logs a message at warn level with a format and arguments.
     *
     * @param format the message format
     * @param args the arguments
     */
    public void warn(String format, Object... args) {
        log(() -> logger.warn(format, args));
    }
    
    /**
     * Logs a message at error level with a format and arguments.
     *
     * @param format the message format
     * @param args the arguments
     */
    public void error(String format, Object... args) {
        log(() -> logger.error(format, args));
    }
    
    /**
     * Executes a Python logging command with the same context and level.
     *
     * @param level the log level (trace, debug, info, warn, error)
     * @param message the message to log
     */
    public void logPython(String level, String message) {
        // Get project root directory
        String projectRoot = System.getProperty("user.dir");
        String pythonScript = "bin/log_python.py";
        File scriptFile = new File(projectRoot, pythonScript);
        
        if (!scriptFile.exists()) {
            logger.warn("Python logging script not found: {}", scriptFile.getAbsolutePath());
            return;
        }
        
        // Build command with context fields
        StringBuilder command = new StringBuilder();
        command.append("python3 ").append(scriptFile.getAbsolutePath());
        command.append(" --level ").append(level);
        command.append(" --name ").append(name);
        
        for (Map.Entry<String, String> field : contextFields.entrySet()) {
            command.append(" --field ").append(field.getKey()).append("=").append(field.getValue());
        }
        
        command.append(" --message \"").append(message.replace("\"", "\\\"")).append("\"");
        
        try {
            Process process = Runtime.getRuntime().exec(command.toString());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Python logging failed with exit code: {}", exitCode);
                
                // Log error output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.warn("Python logger error: {}", line);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to execute Python logging: {}", e.getMessage());
        }
    }
    
    /**
     * Executes a Bash logging command with the same context and level.
     *
     * @param level the log level (trace, debug, info, warn, error)
     * @param message the message to log
     */
    public void logBash(String level, String message) {
        // Get project root directory
        String projectRoot = System.getProperty("user.dir");
        String bashScript = "bin/log_bash.sh";
        File scriptFile = new File(projectRoot, bashScript);
        
        if (!scriptFile.exists()) {
            logger.warn("Bash logging script not found: {}", scriptFile.getAbsolutePath());
            return;
        }
        
        // Build command with context fields
        StringBuilder command = new StringBuilder();
        command.append("bash ").append(scriptFile.getAbsolutePath());
        command.append(" --level ").append(level);
        command.append(" --name ").append(name);
        
        for (Map.Entry<String, String> field : contextFields.entrySet()) {
            command.append(" --field \"").append(field.getKey()).append("=").append(field.getValue()).append("\"");
        }
        
        command.append(" --message \"").append(message.replace("\"", "\\\"")).append("\"");
        
        try {
            Process process = Runtime.getRuntime().exec(command.toString());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Bash logging failed with exit code: {}", exitCode);
                
                // Log error output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.warn("Bash logger error: {}", line);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to execute Bash logging: {}", e.getMessage());
        }
    }
    
    /**
     * Executes a Go logging command with the same context and level.
     *
     * @param level the log level (trace, debug, info, warn, error)
     * @param message the message to log
     */
    public void logGo(String level, String message) {
        // Get project root directory
        String projectRoot = System.getProperty("user.dir");
        String goExecutable = "bin/rinna-logger";
        File executableFile = new File(projectRoot, goExecutable);
        
        if (!executableFile.exists()) {
            logger.warn("Go logging executable not found: {}", executableFile.getAbsolutePath());
            return;
        }
        
        // Build command with context fields
        StringBuilder command = new StringBuilder();
        command.append(executableFile.getAbsolutePath());
        command.append(" --level ").append(level);
        command.append(" --name ").append(name);
        
        for (Map.Entry<String, String> field : contextFields.entrySet()) {
            command.append(" --field \"").append(field.getKey()).append("=").append(field.getValue()).append("\"");
        }
        
        command.append(" --message \"").append(message.replace("\"", "\\\"")).append("\"");
        
        try {
            Process process = Runtime.getRuntime().exec(command.toString());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Go logging failed with exit code: {}", exitCode);
                
                // Log error output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.warn("Go logger error: {}", line);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to execute Go logging: {}", e.getMessage());
        }
    }
    
    /**
     * Logs a message using the specified logging function,
     * adding context fields from MDC.
     *
     * @param loggingFunction the function to execute for logging
     */
    private void log(Runnable loggingFunction) {
        // Push context fields to MDC
        if (!contextFields.isEmpty()) {
            for (Map.Entry<String, String> field : contextFields.entrySet()) {
                MDC.put(field.getKey(), field.getValue());
            }
        }
        
        try {
            // Execute the logging function
            loggingFunction.run();
        } finally {
            // Clean up MDC
            if (!contextFields.isEmpty()) {
                for (String key : contextFields.keySet()) {
                    MDC.remove(key);
                }
            }
        }
    }
    
    /**
     * Sets the log level for the root logger.
     *
     * @param level the log level
     */
    public static void setRootLogLevel(String level) {
        // Can't set log level programmatically in SLF4J
        // This is normally configured via logback.xml
        // Instead, set the environment variable
        System.setProperty("RINNA_LOG_LEVEL", level);
    }
}