# Rinna Troubleshooting Guide

This comprehensive guide addresses common issues that users might encounter while using Rinna and provides step-by-step solutions for resolving them.

## Table of Contents

1. [CLI Issues](#cli-issues)
2. [Workflow Issues](#workflow-issues)
3. [API Integration Issues](#api-integration-issues)
4. [Migration Issues](#migration-issues)
5. [Build and Installation Issues](#build-and-installation-issues)
6. [Service and Connection Issues](#service-and-connection-issues)
7. [Cross-Language Integration Issues](#cross-language-integration-issues)
8. [Common Error Codes](#common-error-codes)
9. [Getting Support](#getting-support)

## CLI Issues

### Command Not Found

**Problem**: Running `rin-cli` command results in "command not found" error.

**Possible Causes**:
- CLI binary is not in your PATH
- CLI is not installed or built correctly
- Execute permissions are not set

**Solutions**:

1. **Check if CLI executable exists**:
   ```bash
   ls -la /path/to/rinna/bin/rin-cli
   ```

2. **Add to PATH**:
   ```bash
   export PATH=$PATH:/path/to/rinna/bin
   # Add to ~/.bashrc or ~/.zshrc for persistence
   ```

3. **Fix permissions**:
   ```bash
   chmod +x /path/to/rinna/bin/rin-cli
   ```

4. **Rebuild CLI**:
   ```bash
   cd /path/to/rinna
   mvn -pl rinna-cli clean package
   ```

### Invalid Authentication

**Problem**: CLI commands fail with authentication errors even with correct credentials.

**Possible Causes**:
- Expired token or session
- Configuration file issues
- Permission problems

**Solutions**:

1. **Log in again**:
   ```bash
   rin-cli login
   ```

2. **Check configuration file**:
   ```bash
   cat ~/.rinna/config.yaml
   ```

3. **Reset authentication**:
   ```bash
   rm ~/.rinna/auth.token
   rin-cli login
   ```

4. **Update credentials**:
   ```bash
   rin-cli config auth --update
   ```

### Output Formatting Problems

**Problem**: CLI output is malformed or not displaying correctly.

**Possible Causes**:
- Terminal compatibility issues
- Encoding problems
- JSON/YAML parsing errors

**Solutions**:

1. **Change output format**:
   ```bash
   rin-cli list --format=text   # For plain text
   rin-cli list --format=json   # For JSON
   ```

2. **Check terminal encoding**:
   ```bash
   echo $LANG                   # Should be UTF-8
   ```

3. **Use terse output mode**:
   ```bash
   rin-cli -t list              # Terse mode
   ```

4. **Check for special characters**:
   ```bash
   rin-cli list --format=json | jq .  # Pretty-print JSON
   ```

### Service Auto-start Issues

**Problem**: CLI commands hang or fail because services don't auto-start properly.

**Possible Causes**:
- Services failing to start
- Port conflicts
- Network issues

**Solutions**:

1. **Check service status**:
   ```bash
   rin-cli server status
   ```

2. **Start services manually**:
   ```bash
   rin-cli server start --verbose
   ```

3. **Disable auto-start temporarily**:
   ```bash
   rin-cli --no-auto-start list  # Run without auto-starting services
   ```

4. **Check for port conflicts**:
   ```bash
   lsof -i :8080                 # Check if port 8080 is in use
   ```

### Operation Tracking Errors

**Problem**: Operation tracking fails or returns incorrect data.

**Possible Causes**:
- Database corruption
- Permission issues
- Configuration problems

**Solutions**:

1. **Check operation history**:
   ```bash
   rin-cli admin operations list --limit 10
   ```

2. **Reset operation database**:
   ```bash
   rin-cli admin operations clean --confirm
   ```

3. **Verify MetadataService configuration**:
   ```bash
   rin-cli admin diagnostics check --service metadata
   ```

## Workflow Issues

### Invalid Transition Errors

**Problem**: Work item state transitions fail with "Invalid transition" error.

**Possible Causes**:
- Attempting disallowed workflow state transition
- Dependency constraints blocking the transition
- Validation rules failing

**Solutions**:

1. **Check available transitions**:
   ```bash
   rin-cli workflow transitions WI-123
   ```

2. **Check for blocking dependencies**:
   ```bash
   rin-cli dependencies WI-123 --blocking
   ```

3. **Check validation rules**:
   ```bash
   rin-cli workflow validate WI-123 --to-state=IN_PROGRESS --verbose
   ```

4. **Use emergency workflow for critical fixes**:
   ```bash
   rin-cli hotfix start WI-123  # For emergency fixes that bypass normal workflow
   ```

### Missing Work Items

**Problem**: Work items are not appearing in queries or lists.

**Possible Causes**:
- Filtering criteria excluding items
- Permission issues
- Archived or deleted items

**Solutions**:

1. **List all items without filters**:
   ```bash
   rin-cli list --all
   ```

2. **Check archives**:
   ```bash
   rin-cli list --archived
   ```

3. **Verify permissions**:
   ```bash
   rin-cli access check WI-123
   ```

4. **Search by ID directly**:
   ```bash
   rin-cli find --id WI-123
   ```

### Dependency Cycle Detected

**Problem**: Dependency changes fail with "Circular dependency detected" error.

**Possible Causes**:
- Creating a dependency that would form a cycle
- Complex dependency chains with cycles

**Solutions**:

1. **Check current dependencies**:
   ```bash
   rin-cli dependencies WI-123 --recursive
   ```

2. **Visualize dependency graph**:
   ```bash
   rin-cli dependencies --graph --output=deps.dot
   ```

3. **Analyze circular dependencies**:
   ```bash
   rin-cli check-circular --release RELEASE-456
   ```

4. **Change relationship type to break cycle**:
   ```bash
   rin-cli link change WI-789 DEPENDS_ON WI-123 RELATED_TO
   ```

### Database Locks or Contention

**Problem**: Operations fail with database lock or contention errors.

**Possible Causes**:
- Multiple simultaneous writes
- Long transactions
- Database corruption

**Solutions**:

1. **Check active operations**:
   ```bash
   rin-cli admin operations list --active
   ```

2. **Kill stuck operations**:
   ```bash
   rin-cli admin operations kill OP-12345
   ```

3. **Restart service layer**:
   ```bash
   rin-cli server restart
   ```

4. **Check database health**:
   ```bash
   rin-cli admin diagnostics run --target database
   ```

## API Integration Issues

### API Authentication Failures

**Problem**: API calls fail with authentication errors.

**Possible Causes**:
- Invalid or expired token
- Incorrect authentication headers
- Missing permissions

**Solutions**:

1. **Generate a new API token**:
   ```bash
   rin-cli api token create --name="My App" --expiry=30days
   ```

2. **Check token validity**:
   ```bash
   rin-cli api token validate --token=YOUR_TOKEN
   ```

3. **Revoke and create new token**:
   ```bash
   rin-cli api token revoke --token=YOUR_TOKEN
   rin-cli api token create --name="My App" --scope=read,write
   ```

4. **Verify API request headers**:
   ```
   Authorization: Bearer YOUR_TOKEN_HERE
   Content-Type: application/json
   ```

### Rate Limiting Issues

**Problem**: API calls return 429 Too Many Requests errors.

**Possible Causes**:
- Too many requests in short time
- Rate limit configuration too strict
- Inefficient client implementation

**Solutions**:

1. **Check current rate limits**:
   ```bash
   rin-cli api rate-limits show
   ```

2. **Implement exponential backoff**:
   ```python
   # Python example
   import time
   
   def api_call_with_backoff(max_retries=5):
       retries = 0
       while retries < max_retries:
           try:
               response = make_api_call()
               return response
           except RateLimitException:
               wait_time = 2 ** retries
               print(f"Rate limited. Waiting {wait_time} seconds...")
               time.sleep(wait_time)
               retries += 1
       raise Exception("Max retries exceeded")
   ```

3. **Request higher limits**:
   ```bash
   rin-cli api rate-limits adjust --limit=120 --window=60s
   ```

4. **Use bulk endpoints**:
   ```bash
   # Instead of multiple single-item API calls
   # Use bulk endpoints to reduce request count
   rin-cli api bulk-update --items="WI-123,WI-124,WI-125" --status=IN_PROGRESS
   ```

### Webhook Integration Problems

**Problem**: Webhooks are not being received or processed.

**Possible Causes**:
- Incorrect webhook URL
- Signature verification issues
- Network or firewall problems

**Solutions**:

1. **Test webhook delivery**:
   ```bash
   rin-cli api webhook test --url=https://your-server.com/webhook
   ```

2. **Verify webhook signature**:
   ```bash
   rin-cli api webhook verify --payload-file=sample.json --signature="sha256=HASH"
   ```

3. **Check webhook logs**:
   ```bash
   rin-cli admin logs --service api --filter webhook
   ```

4. **Update webhook configuration**:
   ```bash
   rin-cli api webhook update --id=WH-123 --url=https://new-url.com/webhook --secret=NEW_SECRET
   ```

### OAuth Integration Issues

**Problem**: OAuth flow fails with errors.

**Possible Causes**:
- Incorrect OAuth client configuration
- Redirect URI mismatch
- Expired client credentials

**Solutions**:

1. **Verify OAuth client configuration**:
   ```bash
   rin-cli api oauth client show
   ```

2. **Update client configuration**:
   ```bash
   rin-cli api oauth client update --redirect-uri=https://app.example.com/callback
   ```

3. **Test OAuth flow**:
   ```bash
   rin-cli api oauth test-flow
   ```

4. **Generate new client secret**:
   ```bash
   rin-cli api oauth client rotate-secret
   ```

## Migration Issues

### Import Failure

**Problem**: Migration import fails with errors.

**Possible Causes**:
- Invalid source data format
- Mapping configuration issues
- Unsupported fields or values

**Solutions**:

1. **Run with validation only**:
   ```bash
   rin-cli migrate import --validate-only --source-file=data.json
   ```

2. **Check mapping configuration**:
   ```bash
   rin-cli migrate mappings validate --file=mapping.json
   ```

3. **Run with detailed logging**:
   ```bash
   rin-cli migrate import --source-file=data.json --verbose
   ```

4. **Import in smaller batches**:
   ```bash
   rin-cli migrate import --source-file=data.json --batch-size=50
   ```

### Missing Data After Migration

**Problem**: Data is incomplete after migration.

**Possible Causes**:
- Incomplete source data
- Filtering criteria excluding items
- Failed transformations

**Solutions**:

1. **Verify migration results**:
   ```bash
   rin-cli migrate verify --source-file=data.json --report
   ```

2. **Generate gap analysis**:
   ```bash
   rin-cli migrate analyze-gaps --source-file=data.json
   ```

3. **Run targeted import**:
   ```bash
   rin-cli migrate import --source-file=data.json --items=ITEM-123,ITEM-124
   ```

4. **Update mappings and retry**:
   ```bash
   # Edit the mapping file to handle edge cases, then reimport
   rin-cli migrate import --source-file=data.json --mapping-file=updated-mapping.json
   ```

### Source System Connection Problems

**Problem**: Cannot connect to source system for migration.

**Possible Causes**:
- Authentication issues
- Network problems
- API limitations

**Solutions**:

1. **Test connection**:
   ```bash
   rin-cli migrate test-connection --source=jira --url=https://jira.example.com
   ```

2. **Update credentials**:
   ```bash
   rin-cli migrate config --source=jira --update-auth
   ```

3. **Use offline export**:
   ```bash
   # Export from source system manually, then import the file
   rin-cli migrate import --source-file=jira-export.json --source=jira
   ```

4. **Set up proxy if needed**:
   ```bash
   rin-cli migrate config --proxy=http://proxy.example.com:8080
   ```

### User Mapping Issues

**Problem**: User references aren't correctly mapped during migration.

**Possible Causes**:
- Missing user mapping configuration
- Users don't exist in target system
- Username format differences

**Solutions**:

1. **Generate user mapping template**:
   ```bash
   rin-cli migrate users extract --source-file=data.json
   ```

2. **Update user mappings**:
   ```bash
   rin-cli migrate users update --mapping-file=user-mapping.json
   ```

3. **Auto-create users**:
   ```bash
   rin-cli migrate users create --from-mapping
   ```

4. **Skip user validation**:
   ```bash
   rin-cli migrate import --source-file=data.json --skip-user-validation
   ```

## Build and Installation Issues

### Maven Build Failures

**Problem**: Maven build fails with errors.

**Possible Causes**:
- Dependency resolution issues
- Compilation errors
- Test failures

**Solutions**:

1. **Clean and rebuild**:
   ```bash
   mvn clean install -DskipTests
   ```

2. **Resolve dependency issues**:
   ```bash
   # Fix Maven repository caching issues
   ./bin/fix-maven-caching.sh
   ```

3. **Check version conflicts**:
   ```bash
   mvn dependency:tree
   ```

4. **Fix version inconsistencies**:
   ```bash
   ./bin/version-tools/version-sync.sh
   ```

### SQLite Database Issues

**Problem**: SQLite-related errors occur during startup or operation.

**Possible Causes**:
- Database file corruption
- Permission issues
- Missing SQLite libraries

**Solutions**:

1. **Check database integrity**:
   ```bash
   rin-cli admin diagnostics database --check-integrity
   ```

2. **Reset database (caution: data loss)**:
   ```bash
   rin-cli admin reset-database --confirm
   ```

3. **Verify SQLite installation**:
   ```bash
   sqlite3 --version
   ```

4. **Fix permissions**:
   ```bash
   chmod -R 755 ~/.rinna/data/
   ```

### Cross-Platform Issues

**Problem**: Application behaves differently across platforms (Linux, macOS, Windows).

**Possible Causes**:
- Path separators
- System dependencies
- Platform-specific code

**Solutions**:

1. **Use platform-agnostic paths**:
   ```bash
   rin-cli config --set path.separator=auto
   ```

2. **Check platform compatibility**:
   ```bash
   rin-cli diagnostics platform
   ```

3. **Use container deployment**:
   ```bash
   ./bin/run-in-container.sh
   ```

4. **Update platform-specific dependencies**:
   ```bash
   ./bin/update-platform-deps.sh
   ```

## Service and Connection Issues

### Service Start Failure

**Problem**: Services fail to start.

**Possible Causes**:
- Port conflicts
- Missing dependencies
- Configuration errors

**Solutions**:

1. **Check service status with details**:
   ```bash
   rin-cli server status --verbose
   ```

2. **View service logs**:
   ```bash
   rin-cli server logs
   ```

3. **Configure alternative ports**:
   ```bash
   rin-cli config set service.api.port=9090
   ```

4. **Restart with clean state**:
   ```bash
   rin-cli server restart --clean
   ```

### Database Connection Issues

**Problem**: Cannot connect to database.

**Possible Causes**:
- Database service not running
- Authentication failure
- Connection string issues

**Solutions**:

1. **Check database connection**:
   ```bash
   rin-cli admin diagnostics database --connection-test
   ```

2. **Reconfigure database connection**:
   ```bash
   rin-cli config database --interactive
   ```

3. **Reset database credentials**:
   ```bash
   rin-cli admin database reset-credentials
   ```

4. **Use in-memory database temporarily**:
   ```bash
   rin-cli --mem-db list
   ```

### Network Configuration Problems

**Problem**: Network-related errors with API or services.

**Possible Causes**:
- Firewall blocking connections
- Incorrect network settings
- DNS issues

**Solutions**:

1. **Test connectivity**:
   ```bash
   rin-cli admin diagnostics network
   ```

2. **Configure proxy settings**:
   ```bash
   rin-cli config set network.proxy=http://proxy.example.com:8080
   ```

3. **Use direct IP address**:
   ```bash
   rin-cli config set service.host=192.168.1.100
   ```

4. **Disable TLS verification (development only)**:
   ```bash
   rin-cli config set network.ssl.verify=false
   ```

## Cross-Language Integration Issues

### Java-Go Communication Problems

**Problem**: Java components cannot communicate with Go services.

**Possible Causes**:
- Data serialization incompatibilities
- Protocol mismatches
- Mismatched versions

**Solutions**:

1. **Check API compatibility**:
   ```bash
   rin-cli admin diagnostics cross-language --java-go
   ```

2. **Update protocol version**:
   ```bash
   rin-cli config set integration.protocol.version=2
   ```

3. **Restart all services**:
   ```bash
   rin-cli server restart --all
   ```

4. **Debug communication**:
   ```bash
   rin-cli server logs --filter "java-go"
   ```

### Python Integration Issues

**Problem**: Python scripts or components fail to integrate properly.

**Possible Causes**:
- Python environment issues
- Library compatibility
- Path or import problems

**Solutions**:

1. **Check Python environment**:
   ```bash
   rin-cli admin diagnostics python
   ```

2. **Activate dedicated Python environment**:
   ```bash
   source ./utils/activate-python.sh
   ```

3. **Update Python dependencies**:
   ```bash
   pip install -r requirements.txt --upgrade
   ```

4. **Fix Python path issues**:
   ```bash
   export PYTHONPATH=$PYTHONPATH:/path/to/rinna/python
   ```

## Common Error Codes

This section lists the most common error codes you might encounter and their solutions.

### General Error Codes (GEN-*)

| Error Code | Description | Solution |
|------------|-------------|----------|
| GEN-INIT-001 | Initialization failure | Check configuration file exists and is valid |
| GEN-AUTH-001 | Authentication failure | Verify credentials and token permissions |
| GEN-CONF-001 | Configuration error | Check configuration file syntax and values |
| GEN-FILE-001 | File not found or access denied | Verify file path and permissions |
| GEN-DB-001 | Database connection error | Check database service and credentials |
| GEN-NET-001 | Network connectivity issue | Verify network settings and firewall rules |

### CLI Error Codes (CLI-*)

| Error Code | Description | Solution |
|------------|-------------|----------|
| CLI-CMD-001 | Invalid command or syntax | Check command syntax and parameters |
| CLI-OPT-001 | Invalid option or parameter | Verify command options and formats |
| CLI-AUTH-001 | CLI authentication failure | Log in again with correct credentials |
| CLI-EXEC-001 | Command execution failed | Check command details and try again |
| CLI-SERV-001 | Service unavailable | Start required services with `server start` |

### Workflow Error Codes (WF-*)

| Error Code | Description | Solution |
|------------|-------------|----------|
| WF-TRANS-001 | Invalid state transition | Check allowed transitions with `workflow transitions` |
| WF-DEP-001 | Dependency constraint violation | Resolve blocking dependencies first |
| WF-CIRC-001 | Circular dependency detected | Modify relationships to break the cycle |
| WF-VAL-001 | Validation rule failure | Check validation requirements |
| WF-AUTH-001 | Not authorized for transition | Verify you have permission for this operation |

### API Error Codes (API-*)

| Error Code | Description | Solution |
|------------|-------------|----------|
| API-AUTH-001 | API authentication failure | Verify token and credentials |
| API-RATE-001 | Rate limit exceeded | Implement backoff or request higher limits |
| API-PARAM-001 | Invalid parameter | Check API request parameters |
| API-FORMAT-001 | Invalid request format | Verify request body format (JSON, etc.) |
| API-WH-001 | Webhook delivery failure | Check webhook URL and network connectivity |
| API-OAUTH-001 | OAuth flow error | Verify OAuth configuration |

### Migration Error Codes (MIG-*)

| Error Code | Description | Solution |
|------------|-------------|----------|
| MIG-SRC-001 | Source connection error | Verify source system credentials |
| MIG-MAP-001 | Mapping configuration error | Check mapping file syntax |
| MIG-DATA-001 | Invalid source data | Validate source data format |
| MIG-TRANS-001 | Data transformation error | Adjust mappings to handle the data |
| MIG-USER-001 | User mapping error | Update user mappings or create missing users |

### Build and Environment Codes (ENV-*)

| Error Code | Description | Solution |
|------------|-------------|----------|
| ENV-JAVA-001 | Java environment issue | Verify Java version and JAVA_HOME |
| ENV-GO-001 | Go environment issue | Check Go version and GOPATH |
| ENV-PYTHON-001 | Python environment issue | Verify Python version and virtualenv |
| ENV-DEP-001 | Dependency resolution failure | Update dependencies or fix conflicts |
| ENV-BUILD-001 | Build system error | Check build logs and try clean rebuild |

## Getting Support

If you can't resolve an issue using this guide, follow these steps for additional support:

1. **Generate a diagnostic report**:
   ```bash
   rin-cli admin diagnostics report --output=diagnostic-report.zip
   ```

2. **Check logs**:
   ```bash
   rin-cli admin logs --last=1h
   ```

3. **Contact Support**:
   - Submit diagnostic report and detailed issue description
   - Include steps to reproduce the problem
   - Note any recent changes to your environment

4. **Community Resources**:
   - Rinna GitHub issues for bugs: [GitHub Issues](https://github.com/example/rinna/issues)
   - Documentation: [Rinna Docs](https://docs.rinna.example.com)
   - Community forum: [Rinna Community](https://community.rinna.example.com)

5. **Common Support Requests**:
   - For API access issues: Contact api-support@rinna.example.com
   - For enterprise integration: Contact enterprise@rinna.example.com
   - For security concerns: Contact security@rinna.example.com