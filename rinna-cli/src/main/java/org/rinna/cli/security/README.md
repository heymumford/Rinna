# Rinna CLI Security System

This package contains the security system for the Rinna CLI, providing authentication and authorization capabilities for the Rinna workflow management system.

## Components

### SecurityManager

The `SecurityManager` class serves as the central access point for all security functionality. It provides a simplified interface to authenticate users, check permissions, and manage administrative access.

Key methods:
- `login(username, password)` - Authenticate a user
- `logout()` - End the current session
- `isAuthenticated()` - Check if a user is currently logged in
- `isAdmin()` - Check if the current user has administrator privileges
- `hasPermission(permission)` - Check if the user has a specific permission
- `hasAdminAccess(area)` - Check if the user has admin access to a specific area

### AuthenticationService

The `AuthenticationService` handles user authentication, including login, logout, and session management.

Key features:
- Token-based authentication
- User session management
- Admin role detection

### AuthorizationService

The `AuthorizationService` handles permission checking and access control.

Key features:
- Permission-based access control
- Area-specific administrative access
- Centralized permission management

## Security System Design

The security system follows these design principles:

1. **Separation of Concerns**: Authentication and authorization are handled by separate components.
2. **Centralized Security Management**: The SecurityManager provides a unified interface.
3. **Persistence**: Security credentials are stored securely in configuration files.
4. **Least Privilege**: Users only get the permissions they need.
5. **Configuration-based**: Security settings are easily configurable.

## CLI Commands

The security system is accessed through the following CLI commands:

```bash
# Authentication
rin login [username]           # Login as a user (prompts for password)
rin logout                     # End the current session

# User access management
rin access help                                      # Show available actions
rin access grant-permission --user=X --permission=Y  # Grant a permission
rin access revoke-permission --user=X --permission=Y # Revoke a permission
rin access grant-admin --user=X --area=Y             # Grant admin access to an area
rin access revoke-admin --user=X --area=Y            # Revoke admin access
rin access promote --user=X                          # Promote to full admin
```

## Integration with Rinna CLI

The security system is integrated with the Rinna CLI through command handlers in the `RinnaCli` class:

- `handleLoginCommand(subargs)` - Handles the "login" command
- `handleLogoutCommand(subargs)` - Handles the "logout" command
- `handleAccessCommand(subargs)` - Handles the "access" command

## Future Enhancements

Potential enhancements to the security system include:

1. Adding support for multi-factor authentication
2. Implementing more sophisticated password hashing
3. Adding token expiration and automatic renewal
4. Supporting role-based access control
5. Adding a comprehensive audit log
6. Supporting multiple authentication providers