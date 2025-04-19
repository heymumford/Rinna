# Rinna Server Configuration Guide

This guide provides detailed information on setting up and configuring the Rinna server for administrators.

## Server Architecture

Rinna uses a combined architecture:
- Java back-end for core business logic and workflow management
- Go API server for high-performance RESTful services
- CLI client for command-line operations

## Automatic Server Management

By default, Rinna is configured to automatically:
1. Start the server when needed if no external server is configured
2. Use the local user's credentials
3. Shut down automatically when idle (configurable timeout)

## Manual Server Management

### Command Line Control

```bash
# Check server status
rin server status

# Start server
rin server start

# Stop server
rin server stop

# Restart server
rin server restart

# View server logs
rin server logs

# View detailed server information
rin server info
```

### Server Configuration

```bash
# Set server port
rin config server --port 9090

# Set server host (for binding)
rin config server --host 0.0.0.0

# Configure server memory
rin config server --memory 512m

# Set database location
rin config server --database /path/to/database

# Set server context path
rin config server --context-path /rinna

# Configure log level
rin config server --log-level INFO
```

## Environment Configuration

### Default Configuration

When first installed, the server uses these defaults:
- Username: admin
- Password: nimda
- User ID: Your local machine username
- Machine ID: Your computer's hostname
- Port: 9090
- Database: In-memory H2 (for development/testing)

### Production Configuration

For production environments, update the configuration:

```bash
# Set production mode
rin config env --mode production

# Configure external database
rin config database --type postgresql --host db.example.com --port 5432 --name rinna --user dbuser --password PASSWORD

# Enable HTTPS
rin config security --enable-https --cert-path /path/to/cert.pem --key-path /path/to/key.pem

# Configure authentication
rin config auth --type ldap --url ldap://ldap.example.com --bind-dn "cn=admin,dc=example,dc=com" --bind-password PASSWORD
```

## Advanced Configuration

### Scaling Options

```bash
# Set connection pool size
rin config server --pool-size 50

# Set thread pool size
rin config server --thread-pool 20

# Enable clustering
rin config server --cluster-enabled --cluster-nodes 3
```

### High Availability

```bash
# Enable failover
rin config server --failover-enabled --failover-host backup.example.com

# Configure master-slave replication
rin config server --replication-enabled --replication-mode master
```

### Security Hardening

```bash
# Enable strict security mode
rin config security --strict-mode

# Configure allowed hosts
rin config security --allowed-hosts 192.168.1.0/24,10.0.0.0/8

# Set secure cookie options
rin config security --secure-cookies

# Enable content security policy
rin config security --enable-csp
```

## Monitoring and Diagnostics

```bash
# Show system health
rin server health

# View active connections
rin server connections

# Monitor performance
rin server performance

# Generate diagnostic report
rin server diagnostics
```

## Implementation Example

Below is a step-by-step example for setting up a production Rinna server:

```bash
# Install Rinna from Maven
mvn clean package

# Configure server for production
rin config env --mode production

# Set up database
rin config database --type postgresql --host db.prod.internal --name rinna_prod --user rinna_app --password "securePassword123"

# Configure security
rin config security --enable-https --cert-path /etc/ssl/certs/rinna.pem --key-path /etc/ssl/private/rinna.key

# Set up LDAP authentication
rin config auth --type ldap --url ldap://ldap.internal --base-dn "ou=users,dc=example,dc=com"

# Configure server resources
rin config server --memory 2048m --pool-size 100 --thread-pool 50

# Enable monitoring
rin config monitoring --enable --metrics-port 9091

# Start server
rin server start

# Verify server is running properly
rin server status
rin server health
```

For more detailed information, refer to the [full Admin Guide](./admin-guide.md) and [Configuration Reference](./configuration-reference.md).