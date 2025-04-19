package org.rinna.cli.polyglot.java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.rinna.cli.polyglot.framework.PolyglotTestHarness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test utility for interacting with the Java CLI components.
 * This class provides methods for:
 * <ul>
 *   <li>Executing CLI commands directly via Java</li>
 *   <li>Executing CLI commands via shell scripts</li>
 *   <li>Capturing and analyzing command output</li>
 *   <li>Testing CLI interactions with other components</li>
 * </ul>
 */
public class JavaCliTester {
    private static final Logger logger = LoggerFactory.getLogger(JavaCliTester.class);
    
    private final PolyglotTestHarness harness;
    
    /**
     * Create a new JavaCliTester instance.
     * 
     * @param harness The polyglot test harness to use
     */
    public JavaCliTester(PolyglotTestHarness harness) {
        this.harness = harness;
    }
    
    /**
     * Execute a CLI command using the shell scripts.
     * 
     * @param command The command name (e.g., "add", "list", "view")
     * @param args Arguments to pass to the command
     * @return The command output
     * @throws IOException If command execution fails
     * @throws InterruptedException If the command process is interrupted
     */
    public String executeCliScript(String command, String... args) throws IOException, InterruptedException {
        Path binDir = harness.getProjectRoot().resolve("bin");
        String scriptPath = binDir.resolve("rin-" + command).toString();
        
        String argsString = Arrays.stream(args).collect(Collectors.joining(" "));
        String fullCommand = scriptPath + " " + argsString;
        
        return harness.executeCliCommand(fullCommand, 30);
    }
    
    /**
     * Execute the main Rinna CLI command using the shell script.
     * 
     * @param args Arguments to pass to the command
     * @return The command output
     * @throws IOException If command execution fails
     * @throws InterruptedException If the command process is interrupted
     */
    public String executeMainCliScript(String... args) throws IOException, InterruptedException {
        Path binDir = harness.getProjectRoot().resolve("bin");
        String scriptPath = binDir.resolve("rin").toString();
        
        String argsString = Arrays.stream(args).collect(Collectors.joining(" "));
        String fullCommand = scriptPath + " " + argsString;
        
        return harness.executeCliCommand(fullCommand, 30);
    }
    
    /**
     * Execute a CLI command directly via Java reflection.
     * This allows testing the Java command classes directly without using the shell scripts.
     * 
     * @param commandClass The fully qualified class name of the command
     * @param args Arguments to pass to the command
     * @return The command output and exit code
     */
    public CliResult executeJavaCommand(String commandClass, String... args) {
        logger.info("Executing Java command: {} with args: {}", commandClass, Arrays.toString(args));
        
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        
        PrintStream outPrintStream = new PrintStream(outStream);
        PrintStream errPrintStream = new PrintStream(errStream);
        
        try {
            // Redirect stdout and stderr
            System.setOut(outPrintStream);
            System.setErr(errPrintStream);
            
            // Load the command class
            Class<?> clazz = Class.forName(commandClass);
            Object command = clazz.getDeclaredConstructor().newInstance();
            
            // Set up command arguments if it has setter methods
            for (String arg : args) {
                if (arg.startsWith("--")) {
                    String[] parts = arg.substring(2).split("=", 2);
                    String propertyName = parts[0];
                    String value = parts.length > 1 ? parts[1] : "true";
                    
                    String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                    
                    try {
                        // Try to find and invoke the setter method
                        Method[] methods = clazz.getMethods();
                        for (Method method : methods) {
                            if (method.getName().equalsIgnoreCase(setterName) && method.getParameterCount() == 1) {
                                Class<?> paramType = method.getParameterTypes()[0];
                                Object convertedValue = convertValue(value, paramType);
                                method.invoke(command, convertedValue);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to set property '{}' on command class {}", propertyName, commandClass, e);
                    }
                }
            }
            
            // Execute the command
            Method callMethod = clazz.getMethod("call");
            Object result = callMethod.invoke(command);
            
            int exitCode = (result instanceof Integer) ? (Integer) result : 0;
            
            outPrintStream.flush();
            errPrintStream.flush();
            
            String output = outStream.toString();
            String error = errStream.toString();
            
            return new CliResult(exitCode, output, error);
        } catch (Exception e) {
            logger.error("Failed to execute Java command: {}", commandClass, e);
            return new CliResult(-1, "", e.getMessage());
        } finally {
            // Restore stdout and stderr
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
    
    /**
     * Build the CLI project.
     * 
     * @return The build output
     * @throws IOException If build fails
     * @throws InterruptedException If the build process is interrupted
     */
    public String buildCli() throws IOException, InterruptedException {
        String command = "mvn -pl rinna-cli clean install -DskipTests";
        return harness.executeCliCommand(command, 120);
    }
    
    /**
     * Run CLI tests directly.
     * 
     * @param testClass The test class to run (optional, runs all CLI tests if null)
     * @param testMethod The test method to run (optional, runs all methods if null)
     * @return The test output
     * @throws IOException If test execution fails
     * @throws InterruptedException If the test process is interrupted
     */
    public String runCliTests(String testClass, String testMethod) throws IOException, InterruptedException {
        StringBuilder command = new StringBuilder("mvn -pl rinna-cli test");
        
        if (testClass != null) {
            command.append(" -Dtest=").append(testClass);
            
            if (testMethod != null) {
                command.append("#").append(testMethod);
            }
        }
        
        return harness.executeCliCommand(command.toString(), 60);
    }
    
    /**
     * Create a test configuration file for CLI testing.
     * 
     * @param configFile The name of the configuration file
     * @param content The content of the configuration file
     * @return Path to the created configuration file
     * @throws IOException If file creation fails
     */
    public Path createTestConfig(String configFile, String content) throws IOException {
        Path configPath = harness.getTestTempDir().resolve(configFile);
        Files.writeString(configPath, content);
        return configPath;
    }
    
    // Helper class to represent CLI command execution result
    public static class CliResult {
        private final int exitCode;
        private final String output;
        private final String error;
        
        public CliResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
        
        public int getExitCode() {
            return exitCode;
        }
        
        public String getOutput() {
            return output;
        }
        
        public String getError() {
            return error;
        }
        
        public boolean isSuccess() {
            return exitCode == 0;
        }
        
        @Override
        public String toString() {
            return "CliResult{exitCode=" + exitCode + ", output='" + output + "', error='" + error + "'}";
        }
    }
    
    // Helper methods
    
    private Object convertValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        } else if (targetType.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) targetType;
            return Enum.valueOf(enumType.asSubclass(Enum.class), value);
        }
        
        throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
    }
}