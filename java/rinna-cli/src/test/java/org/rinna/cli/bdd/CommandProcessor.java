/*
 * Command processor for BDD tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.bdd;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.ServiceManager;

/**
 * Helper class for processing commands in BDD tests.
 */
public class CommandProcessor {
    private final TestContext testContext;
    
    /**
     * Constructor with TestContext.
     *
     * @param testContext the test context
     */
    public CommandProcessor(TestContext testContext) {
        this.testContext = testContext;
    }
    
    /**
     * Processes a command line string for testing.
     *
     * @param commandLine the full command line (e.g., "rin notify list")
     * @return the exit code
     */
    public int processCommand(String commandLine) {
        // Parse the command line
        List<String> args = parseCommandLine(commandLine);
        
        if (args.isEmpty()) {
            return 1;
        }
        
        // The first part should be "rin"
        if (!"rin".equals(args.get(0))) {
            System.err.println("Error: Commands should start with 'rin'");
            return 1;
        }
        
        // The second part is the command name
        if (args.size() < 2) {
            System.err.println("Error: No command specified");
            return 1;
        }
        
        String commandName = args.get(1);
        
        // Extract the remaining arguments
        String[] commandArgs = args.subList(2, args.size()).toArray(new String[0]);
        
        // Process the command
        return processCommand(commandName, commandArgs);
    }
    
    /**
     * Parses a command line string into a list of arguments.
     *
     * @param commandLine the command line to parse
     * @return the list of arguments
     */
    private List<String> parseCommandLine(String commandLine) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : commandLine.toCharArray()) {
            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (currentArg.length() > 0) {
                    args.add(currentArg.toString());
                    currentArg = new StringBuilder();
                }
            } else {
                currentArg.append(c);
            }
        }
        
        // Add the last argument if there is one
        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }
        
        return args;
    }
    
    /**
     * Processes a CLI command for testing.
     *
     * @param commandName the command name (e.g., "notify", "msg")
     * @param args the command arguments
     * @return the exit code
     */
    public int processCommand(String commandName, String[] args) {
        // Reset output capture
        testContext.resetCapturedOutput();
        
        // Create and execute the appropriate command
        int exitCode = 0;
        
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            // Mock ServiceManager to return our mock instance
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(testContext.getMockServiceManager());
            
            // Execute the appropriate command based on the command name
            switch (commandName) {
                case "notify":
                    exitCode = executeNotifyCommand(args);
                    break;
                case "msg":
                    exitCode = executeMsgCommand(args);
                    break;
                case "access":
                    exitCode = executeAccessCommand(args);
                    break;
                case "server":
                    exitCode = executeServerCommand(args);
                    break;
                case "login":
                    exitCode = executeLoginCommand(args);
                    break;
                case "logout":
                    exitCode = executeLogoutCommand(args);
                    break;
                case "admin":
                    exitCode = executeAdminCommand(args);
                    break;
                // Add other command types as needed
                default:
                    exitCode = 1; // Unknown command
                    System.err.println("Error: Unknown command: " + commandName);
                    break;
            }
        }
        
        // Save the exit code in the test context
        testContext.setLastCommandExitCode(exitCode);
        
        // Save the output in the test context
        testContext.setLastCommandOutput(testContext.getStandardOutput());
        
        return exitCode;
    }
    
    /**
     * Executes the notify command.
     *
     * @param args the command arguments
     * @return the exit code
     */
    private int executeNotifyCommand(String[] args) {
        try {
            // Find the notify command class
            Class<?> notifyCommandClass = Class.forName("org.rinna.cli.command.NotifyCommand");
            
            // Create an instance of the command
            Object notifyCommand = notifyCommandClass.getDeclaredConstructor().newInstance();
            
            // Process the arguments and set them on the command
            if (args.length > 0) {
                String action = args[0];
                
                // Set the action
                java.lang.reflect.Method setActionMethod = notifyCommandClass.getDeclaredMethod("setAction", String.class);
                setActionMethod.setAccessible(true);
                setActionMethod.invoke(notifyCommand, action);
                
                // Process additional arguments
                if ("read".equals(action) || "markread".equals(action)) {
                    // For read/markread commands, the second argument should be the notification ID
                    if (args.length > 1) {
                        try {
                            UUID notificationId = UUID.fromString(args[1]);
                            java.lang.reflect.Method setNotificationIdMethod = notifyCommandClass.getDeclaredMethod("setNotificationId", UUID.class);
                            setNotificationIdMethod.setAccessible(true);
                            setNotificationIdMethod.invoke(notifyCommand, notificationId);
                        } catch (IllegalArgumentException e) {
                            System.err.println("Invalid notification ID format");
                        }
                    }
                } else if ("clear".equals(action)) {
                    // For clear command, check for --days parameter
                    for (int i = 1; i < args.length; i++) {
                        String arg = args[i];
                        if (arg.startsWith("--days=")) {
                            try {
                                int days = Integer.parseInt(arg.substring("--days=".length()));
                                java.lang.reflect.Method setDaysMethod = notifyCommandClass.getDeclaredMethod("setDays", int.class);
                                setDaysMethod.setAccessible(true);
                                setDaysMethod.invoke(notifyCommand, days);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid days format");
                            }
                        }
                    }
                }
                
                // Check for common flags
                for (int i = 1; i < args.length; i++) {
                    String arg = args[i];
                    if ("--json".equals(arg)) {
                        java.lang.reflect.Method setJsonOutputMethod = notifyCommandClass.getDeclaredMethod("setJsonOutput", boolean.class);
                        setJsonOutputMethod.setAccessible(true);
                        setJsonOutputMethod.invoke(notifyCommand, true);
                    } else if ("--verbose".equals(arg)) {
                        java.lang.reflect.Method setVerboseMethod = notifyCommandClass.getDeclaredMethod("setVerbose", boolean.class);
                        setVerboseMethod.setAccessible(true);
                        setVerboseMethod.invoke(notifyCommand, true);
                    }
                }
            }
            
            // Call the command
            java.lang.reflect.Method callMethod = notifyCommandClass.getDeclaredMethod("call");
            callMethod.setAccessible(true);
            Object result = callMethod.invoke(notifyCommand);
            
            // Return the exit code
            if (result instanceof Integer) {
                return (Integer) result;
            } else {
                return 0; // Default to success if not an integer
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: NotifyCommand class not found");
            return 1;
        } catch (Exception e) {
            System.err.println("Error executing notify command: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    /**
     * Executes the msg command.
     *
     * @param args the command arguments
     * @return the exit code
     */
    private int executeMsgCommand(String[] args) {
        try {
            // Find the msg command class
            Class<?> msgCommandClass = Class.forName("org.rinna.cli.command.MsgCommand");
            
            // Create an instance of the command
            Object msgCommand = msgCommandClass.getDeclaredConstructor().newInstance();
            
            // Set the arguments using reflection
            java.lang.reflect.Method setArgsMethod = msgCommandClass.getDeclaredMethod("setArgs", String[].class);
            setArgsMethod.setAccessible(true);
            setArgsMethod.invoke(msgCommand, (Object) args);
            
            // Call the command
            java.lang.reflect.Method callMethod = msgCommandClass.getDeclaredMethod("call");
            callMethod.setAccessible(true);
            Object result = callMethod.invoke(msgCommand);
            
            // Return the exit code
            if (result instanceof Integer) {
                return (Integer) result;
            } else {
                return 0; // Default to success if not an integer
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: MsgCommand class not found");
            return 1;
        } catch (Exception e) {
            System.err.println("Error executing msg command: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    /**
     * Executes the access command.
     *
     * @param args the command arguments
     * @return the exit code
     */
    /**
     * Executes the server command.
     *
     * @param args the command arguments
     * @return the exit code
     */
    /**
     * Executes the login command.
     *
     * @param args the command arguments
     * @return the exit code
     */
    private int executeLoginCommand(String[] args) {
        try {
            // Find the login command class
            Class<?> loginCommandClass = Class.forName("org.rinna.cli.command.LoginCommand");
            
            // Create an instance of the command
            Object loginCommand = loginCommandClass.getDeclaredConstructor().newInstance();
            
            // Handle credentials based on arguments
            if (args.length >= 1) {
                // Set the username
                java.lang.reflect.Method setUsernameMethod = loginCommandClass.getDeclaredMethod("setUsername", String.class);
                setUsernameMethod.setAccessible(true);
                setUsernameMethod.invoke(loginCommand, args[0]);
                
                // Set the password if provided
                if (args.length >= 2) {
                    java.lang.reflect.Method setPasswordMethod = loginCommandClass.getDeclaredMethod("setPassword", String.class);
                    setPasswordMethod.setAccessible(true);
                    setPasswordMethod.invoke(loginCommand, args[1]);
                }
            }
            
            // Mock the SecurityManager for the LoginCommand
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(testContext.getMockSecurityService());
                
                // Call the command
                java.lang.reflect.Method callMethod = loginCommandClass.getDeclaredMethod("call");
                callMethod.setAccessible(true);
                Object result = callMethod.invoke(loginCommand);
                
                // Return the exit code
                if (result instanceof Integer) {
                    return (Integer) result;
                } else {
                    return 0; // Default to success if not an integer
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: LoginCommand class not found");
            return 1;
        } catch (Exception e) {
            System.err.println("Error executing login command: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    /**
     * Executes the logout command.
     *
     * @param args the command arguments
     * @return the exit code
     */
    private int executeLogoutCommand(String[] args) {
        try {
            // Find the logout command class
            Class<?> logoutCommandClass = Class.forName("org.rinna.cli.command.LogoutCommand");
            
            // Create an instance of the command
            Object logoutCommand = logoutCommandClass.getDeclaredConstructor().newInstance();
            
            // Mock the SecurityManager for the LogoutCommand
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(testContext.getMockSecurityService());
                
                // Call the command
                java.lang.reflect.Method callMethod = logoutCommandClass.getDeclaredMethod("call");
                callMethod.setAccessible(true);
                Object result = callMethod.invoke(logoutCommand);
                
                // Return the exit code
                if (result instanceof Integer) {
                    return (Integer) result;
                } else {
                    return 0; // Default to success if not an integer
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: LogoutCommand class not found");
            return 1;
        } catch (Exception e) {
            System.err.println("Error executing logout command: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    private int executeServerCommand(String[] args) {
        try {
            // Find the server command class
            Class<?> serverCommandClass = Class.forName("org.rinna.cli.command.ServerCommand");
            
            // Create an instance of the command
            Object serverCommand = serverCommandClass.getDeclaredConstructor().newInstance();
            
            // Set up parameters based on arguments
            if (args.length > 0) {
                // Set subcommand (first argument)
                java.lang.reflect.Method setSubcommandMethod = serverCommandClass.getDeclaredMethod("setSubcommand", String.class);
                setSubcommandMethod.setAccessible(true);
                setSubcommandMethod.invoke(serverCommand, args[0]);
                
                // Determine if we have a service name (second argument) or flags
                if (args.length > 1) {
                    String secondArg = args[1];
                    
                    // Handle flags and service name
                    if (secondArg.startsWith("--")) {
                        handleServerCommandFlags(serverCommandClass, serverCommand, secondArg);
                    } else {
                        // It's likely a service name
                        java.lang.reflect.Method setServiceNameMethod = serverCommandClass.getDeclaredMethod("setServiceName", String.class);
                        setServiceNameMethod.setAccessible(true);
                        setServiceNameMethod.invoke(serverCommand, secondArg);
                        
                        // Check for additional arguments (flags or config path)
                        for (int i = 2; i < args.length; i++) {
                            String arg = args[i];
                            if (arg.startsWith("--")) {
                                handleServerCommandFlags(serverCommandClass, serverCommand, arg);
                            } else if (args[0].equals("config")) {
                                // For config subcommand, third argument is config path
                                java.lang.reflect.Method setConfigPathMethod = serverCommandClass.getDeclaredMethod("setConfigPath", String.class);
                                setConfigPathMethod.setAccessible(true);
                                setConfigPathMethod.invoke(serverCommand, arg);
                            }
                        }
                    }
                }
            }
            
            // Call the command
            java.lang.reflect.Method callMethod = serverCommandClass.getDeclaredMethod("call");
            callMethod.setAccessible(true);
            Object result = callMethod.invoke(serverCommand);
            
            // Return the exit code
            if (result instanceof Integer) {
                return (Integer) result;
            } else {
                return 0; // Default to success if not an integer
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: ServerCommand class not found");
            return 1;
        } catch (Exception e) {
            System.err.println("Error executing server command: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    /**
     * Helper method to handle server command flags.
     */
    private void handleServerCommandFlags(Class<?> serverCommandClass, Object serverCommand, String flag) throws Exception {
        if ("--json".equals(flag)) {
            java.lang.reflect.Method setJsonOutputMethod = serverCommandClass.getDeclaredMethod("setJsonOutput", boolean.class);
            setJsonOutputMethod.setAccessible(true);
            setJsonOutputMethod.invoke(serverCommand, true);
        } else if ("--verbose".equals(flag)) {
            java.lang.reflect.Method setVerboseMethod = serverCommandClass.getDeclaredMethod("setVerbose", boolean.class);
            setVerboseMethod.setAccessible(true);
            setVerboseMethod.invoke(serverCommand, true);
        }
    }
    
    private int executeAccessCommand(String[] args) {
        try {
            // Find the user access command class
            Class<?> accessCommandClass = Class.forName("org.rinna.cli.command.UserAccessCommand");
            
            // Create an instance of the command
            Object accessCommand = accessCommandClass.getDeclaredConstructor().newInstance();
            
            // Parse and set the action and options
            if (args.length > 0) {
                // Set the action using reflection
                java.lang.reflect.Method setActionMethod = accessCommandClass.getDeclaredMethod("setAction", String.class);
                setActionMethod.setAccessible(true);
                setActionMethod.invoke(accessCommand, args[0]);
                
                // Process options (--user, --permission, --area)
                for (int i = 1; i < args.length; i++) {
                    String arg = args[i];
                    if (arg.startsWith("--user=")) {
                        String username = arg.substring("--user=".length());
                        java.lang.reflect.Method setUsernameMethod = accessCommandClass.getDeclaredMethod("setUsername", String.class);
                        setUsernameMethod.setAccessible(true);
                        setUsernameMethod.invoke(accessCommand, username);
                    } else if (arg.startsWith("--permission=")) {
                        String permission = arg.substring("--permission=".length());
                        java.lang.reflect.Method setPermissionMethod = accessCommandClass.getDeclaredMethod("setPermission", String.class);
                        setPermissionMethod.setAccessible(true);
                        setPermissionMethod.invoke(accessCommand, permission);
                    } else if (arg.startsWith("--area=")) {
                        String area = arg.substring("--area=".length());
                        java.lang.reflect.Method setAreaMethod = accessCommandClass.getDeclaredMethod("setArea", String.class);
                        setAreaMethod.setAccessible(true);
                        setAreaMethod.invoke(accessCommand, area);
                    }
                }
            }
            
            // Mock the SecurityManager
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                // Create a mocked SecurityManager instance that delegates to our MockSecurityService
                SecurityManager mockSecurityManager = Mockito.mock(SecurityManager.class);
                
                // Configure the mock behavior to delegate to our MockSecurityService
                Mockito.when(mockSecurityManager.isAuthenticated()).thenAnswer(invocation -> 
                    testContext.getMockSecurityService().isAuthenticated());
                
                Mockito.when(mockSecurityManager.isAdmin()).thenAnswer(invocation -> 
                    testContext.getMockSecurityService().isAdmin());
                
                Mockito.when(mockSecurityManager.getCurrentUser()).thenAnswer(invocation -> 
                    testContext.getMockSecurityService().getCurrentUser());
                
                Mockito.when(mockSecurityManager.grantPermission(Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> {
                    String username = invocation.getArgument(0, String.class);
                    String permission = invocation.getArgument(1, String.class);
                    return testContext.getMockSecurityService().grantPermission(username, permission);
                });
                
                Mockito.when(mockSecurityManager.revokePermission(Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> {
                    String username = invocation.getArgument(0, String.class);
                    String permission = invocation.getArgument(1, String.class);
                    return testContext.getMockSecurityService().revokePermission(username, permission);
                });
                
                Mockito.when(mockSecurityManager.grantAdminAccess(Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> {
                    String username = invocation.getArgument(0, String.class);
                    String area = invocation.getArgument(1, String.class);
                    return testContext.getMockSecurityService().grantAdminAccess(username, area);
                });
                
                Mockito.when(mockSecurityManager.revokeAdminAccess(Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> {
                    String username = invocation.getArgument(0, String.class);
                    String area = invocation.getArgument(1, String.class);
                    return testContext.getMockSecurityService().revokeAdminAccess(username, area);
                });
                
                Mockito.when(mockSecurityManager.promoteToAdmin(Mockito.anyString())).thenAnswer(invocation -> {
                    String username = invocation.getArgument(0, String.class);
                    return testContext.getMockSecurityService().promoteToAdmin(username);
                });
                
                // Return our mocked instance when the getInstance() is called
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                // Call the command
                java.lang.reflect.Method callMethod = accessCommandClass.getDeclaredMethod("call");
                callMethod.setAccessible(true);
                Object result = callMethod.invoke(accessCommand);
                
                // Return the exit code
                if (result instanceof Integer) {
                    return (Integer) result;
                } else {
                    return 0; // Default to success if not an integer
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: UserAccessCommand class not found");
            return 1;
        } catch (Exception e) {
            System.err.println("Error executing access command: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    /**
     * Executes the admin command.
     *
     * @param args the command arguments
     * @return the exit code
     */
    private int executeAdminCommand(String[] args) {
        try {
            // Find the admin command class
            Class<?> adminCommandClass = Class.forName("org.rinna.cli.command.AdminCommand");
            
            // Create an instance of the command
            Object adminCommand = adminCommandClass.getDeclaredConstructor().newInstance();
            
            // Process arguments
            if (args.length > 0) {
                // First argument is the subcommand
                java.lang.reflect.Method setSubcommandMethod = adminCommandClass.getDeclaredMethod("setSubcommand", String.class);
                setSubcommandMethod.setAccessible(true);
                setSubcommandMethod.invoke(adminCommand, args[0]);
                
                // If there are more arguments, pass them as args array
                if (args.length > 1) {
                    String[] subcommandArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, subcommandArgs, 0, args.length - 1);
                    
                    java.lang.reflect.Method setArgsMethod = adminCommandClass.getDeclaredMethod("setArgs", String[].class);
                    setArgsMethod.setAccessible(true);
                    setArgsMethod.invoke(adminCommand, (Object) subcommandArgs);
                }
                
                // Check for flags
                for (String arg : args) {
                    if ("--json".equals(arg)) {
                        java.lang.reflect.Method setJsonOutputMethod = adminCommandClass.getDeclaredMethod("setJsonOutput", boolean.class);
                        setJsonOutputMethod.setAccessible(true);
                        setJsonOutputMethod.invoke(adminCommand, true);
                    } else if ("--verbose".equals(arg)) {
                        java.lang.reflect.Method setVerboseMethod = adminCommandClass.getDeclaredMethod("setVerbose", boolean.class);
                        setVerboseMethod.setAccessible(true);
                        setVerboseMethod.invoke(adminCommand, true);
                    }
                }
            }
            
            // Mock the SecurityManager
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                // Create a mocked SecurityManager instance that delegates to our MockSecurityService
                SecurityManager mockSecurityManager = Mockito.mock(SecurityManager.class);
                
                // Configure the mock behavior to delegate to our MockSecurityService
                Mockito.when(mockSecurityManager.isAuthenticated()).thenAnswer(invocation -> 
                    testContext.getMockSecurityService().isAuthenticated());
                
                Mockito.when(mockSecurityManager.isAdmin()).thenAnswer(invocation -> 
                    testContext.getMockSecurityService().isAdmin());
                
                Mockito.when(mockSecurityManager.getCurrentUser()).thenAnswer(invocation -> 
                    testContext.getMockSecurityService().getCurrentUser());
                
                Mockito.when(mockSecurityManager.hasAdminAccess(Mockito.anyString())).thenAnswer(invocation -> {
                    String area = invocation.getArgument(0, String.class);
                    return testContext.getMockSecurityService().hasAdminAccess(area);
                });
                
                // Return our mocked instance when the getInstance() is called
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                
                // Call the command
                java.lang.reflect.Method callMethod = adminCommandClass.getDeclaredMethod("call");
                callMethod.setAccessible(true);
                Object result = callMethod.invoke(adminCommand);
                
                // Return the exit code
                if (result instanceof Integer) {
                    return (Integer) result;
                } else {
                    return 0; // Default to success if not an integer
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: AdminCommand class not found");
            return 1;
        } catch (Exception e) {
            System.err.println("Error executing admin command: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    /**
     * Execute a command with input.
     * 
     * @param commandLine the command line
     * @param input the input to provide to the command
     * @return the exit code
     */
    public int executeWithInput(String commandLine, String input) {
        // Save the original System.in and replace it with our input
        java.io.InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        try {
            return processCommand(commandLine);
        } finally {
            // Restore the original System.in
            System.setIn(originalIn);
        }
    }
}