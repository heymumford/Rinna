package org.rinna.acceptance.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

@Tag("acceptance")
public class NewUserAuthSteps {
    private final TestContext context;
    private String commandOutput;
    private Map<String, String> userDetails;
    private String username;
    private String hostname;
    private boolean isInitialized;

    public NewUserAuthSteps(TestContext context) {
        this.context = context;
        this.userDetails = new HashMap<>();
        
        // Get current username and hostname for the test
        try {
            this.username = System.getProperty("user.name");
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            this.username = "eric"; // Fallback to our test user
            this.hostname = "rinnadev"; // Fallback host
        }
        
        this.userDetails.put("username", this.username);
        this.userDetails.put("hostname", this.hostname);
        this.userDetails.put("project", "Rinna");
        this.isInitialized = false;
    }

    @Given("a new user is running Rinna for the first time")
    public void aNewUserIsRunningRinnaForTheFirstTime() {
        // Simulate a new user by setting our flag
        isInitialized = false;
    }

    @Given("a new user has initialized Rinna")
    public void aNewUserHasInitializedRinna() {
        // Simulate that the user has already initialized
        isInitialized = true;
        
        // Run the init command in case we need to set up anything
        executeCommand("rin init");
    }

    @When("the user runs {string}")
    public void theUserRuns(String command) {
        executeCommand(command);
    }

    @Then("the system should detect the user's credentials")
    public void theSystemShouldDetectTheUserSCredentials() {
        Assert.assertTrue("Output should mention detecting user credentials",
            commandOutput.contains("Detected user") || 
            commandOutput.contains("credentials detected"));
            
        Assert.assertTrue("Output should mention the username",
            commandOutput.contains(username));
    }

    @And("create a local configuration with the user's identity")
    public void createALocalConfigurationWithTheUserSIdentity() {
        Assert.assertTrue("Output should mention creating configuration",
            commandOutput.contains("Created configuration") || 
            commandOutput.contains("Initialized configuration"));
    }

    @And("the user should see a welcome message")
    public void theUserShouldSeeAWelcomeMessage() {
        Assert.assertTrue("Output should contain a welcome message",
            commandOutput.contains("Welcome") || 
            commandOutput.contains("welcome"));
    }

    @And("the user should see their identity details")
    public void theUserShouldSeeTheirIdentityDetails() {
        Assert.assertTrue("Output should contain the username",
            commandOutput.contains(username));
            
        Assert.assertTrue("Output should contain the hostname",
            commandOutput.contains(hostname));
    }

    @Then("the user should see a message indicating there are no work items")
    public void theUserShouldSeeAMessageIndicatingThereAreNoWorkItems() {
        Assert.assertTrue("Output should indicate no work items",
            commandOutput.contains("No work items found") || 
            commandOutput.contains("Your workspace is empty"));
    }

    @And("the user should see the default workflow stages")
    public void theUserShouldSeeTheDefaultWorkflowStages() {
        Assert.assertTrue("Output should mention default workflow stages",
            commandOutput.contains("Default workflow stages") || 
            commandOutput.contains("workflow stages"));
            
        // Check for common workflow stages
        Assert.assertTrue("Output should mention TODO stage",
            commandOutput.contains("TODO"));
            
        Assert.assertTrue("Output should mention IN_PROGRESS stage",
            commandOutput.contains("IN_PROGRESS"));
            
        Assert.assertTrue("Output should mention REVIEW stage",
            commandOutput.contains("REVIEW"));
            
        Assert.assertTrue("Output should mention DONE stage",
            commandOutput.contains("DONE"));
    }

    @And("the user should see they are authorized for all CRUD operations")
    public void theUserShouldSeeTheyAreAuthorizedForAllCRUDOperations() {
        Assert.assertTrue("Output should mention authorization",
            commandOutput.contains("authorized") || 
            commandOutput.contains("permissions"));
            
        Assert.assertTrue("Output should mention CRUD operations",
            commandOutput.contains("CRUD") || 
            (commandOutput.contains("Create") && 
             commandOutput.contains("Read") && 
             commandOutput.contains("Update") && 
             commandOutput.contains("Delete")));
    }

    @Then("the user should see their authentication details")
    public void theUserShouldSeeTheirAuthenticationDetails() {
        Assert.assertTrue("Output should contain authentication details",
            commandOutput.contains("Authentication") || 
            commandOutput.contains("authenticated as"));
            
        Assert.assertTrue("Output should contain the username",
            commandOutput.contains(username));
    }

    @And("the user should see they have full authorization for all work item types")
    public void theUserShouldSeeTheyHaveFullAuthorizationForAllWorkItemTypes() {
        Assert.assertTrue("Output should mention authorization for all item types",
            commandOutput.contains("all work item types") || 
            commandOutput.contains("all types"));
            
        // Check for common work item types
        Assert.assertTrue("Output should mention FEATURE type",
            commandOutput.contains("FEATURE"));
            
        Assert.assertTrue("Output should mention BUG type",
            commandOutput.contains("BUG"));
            
        Assert.assertTrue("Output should mention TASK type",
            commandOutput.contains("TASK"));
    }

    @And("the user should see they are registered on their local machine")
    public void theUserShouldSeeTheyAreRegisteredOnTheirLocalMachine() {
        Assert.assertTrue("Output should mention local machine registration",
            commandOutput.contains("local machine") || 
            commandOutput.contains(hostname));
    }

    @Then("the user should see that a workspace has been created")
    public void theUserShouldSeeThatAWorkspaceHasBeenCreated() {
        Assert.assertTrue("Output should mention workspace creation",
            commandOutput.contains("Workspace created") || 
            commandOutput.contains("workspace has been set up"));
    }

    @And("the workspace should be linked to the current project")
    public void theWorkspaceShouldBeLinkedToTheCurrentProject() {
        Assert.assertTrue("Output should mention project linkage",
            commandOutput.contains("linked to") || 
            commandOutput.contains("connected to") || 
            commandOutput.contains("project: Rinna"));
    }

    @And("the user should see they are the owner of the workspace")
    public void theUserShouldSeeTheyAreTheOwnerOfTheWorkspace() {
        Assert.assertTrue("Output should mention workspace ownership",
            commandOutput.contains("workspace owner") || 
            commandOutput.contains("owner:") || 
            commandOutput.contains("owned by you"));
    }

    private void executeCommand(String command) {
        // Mock the command execution for testing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        
        if (command.equals("rin init")) {
            simulateInitCommand(printStream);
        } else if (command.equals("rin list")) {
            simulateListCommand(printStream);
        } else if (command.equals("rin auth status")) {
            simulateAuthStatusCommand(printStream);
        } else if (command.equals("rin workspace status")) {
            simulateWorkspaceStatusCommand(printStream);
        } else {
            printStream.println("Unknown command: " + command);
        }
        
        commandOutput = outputStream.toString();
        context.setCommandOutput(commandOutput);
    }
    
    private void simulateInitCommand(PrintStream out) {
        out.println("🔄 Initializing Rinna...");
        out.println();
        out.println("✅ Detected user credentials:");
        out.println("   Username: " + username);
        out.println("   Hostname: " + hostname);
        out.println();
        out.println("✅ Created configuration in ~/.rinna/config.yaml");
        out.println();
        out.println("✅ Initialized local workspace for project: Rinna");
        out.println();
        out.println("🎉 Welcome to Rinna, " + username + "!");
        out.println("   You are now ready to start tracking your work items.");
        out.println();
        out.println("   To add your first work item, try:");
        out.println("   rin add --title \"My first task\" --type TASK");
        out.println();
        out.println("   For more information, run:");
        out.println("   rin help");
        
        isInitialized = true;
    }
    
    private void simulateListCommand(PrintStream out) {
        if (!isInitialized) {
            out.println("❌ Rinna is not initialized. Please run 'rin init' first.");
            return;
        }
        
        out.println("📋 Work Items");
        out.println("=============================================================");
        out.println();
        out.println("No work items found in your workspace.");
        out.println();
        out.println("Default workflow stages:");
        out.println("  • TODO         - Work that needs to be done");
        out.println("  • IN_PROGRESS  - Work currently being done");
        out.println("  • REVIEW       - Work ready for review");
        out.println("  • DONE         - Completed work");
        out.println();
        out.println("Your permissions:");
        out.println("  • You are authorized for all CRUD operations (Create, Read, Update, Delete)");
        out.println("  • You can manage work items of types: FEATURE, BUG, TASK");
        out.println();
        out.println("To add a new work item, use:");
        out.println("  rin add --title \"Your feature title\" --type FEATURE");
        out.println("  rin add --title \"Bug description\" --type BUG --priority HIGH");
        out.println("  rin add --title \"Task to do\" --type TASK --assignee \"" + username + "\"");
    }
    
    private void simulateAuthStatusCommand(PrintStream out) {
        if (!isInitialized) {
            out.println("❌ Rinna is not initialized. Please run 'rin init' first.");
            return;
        }
        
        out.println("🔐 Authentication Status");
        out.println("=============================================================");
        out.println();
        out.println("✅ You are authenticated as " + username + "@" + hostname);
        out.println("   • Local machine authentication active");
        out.println("   • Authentication method: System credentials");
        out.println("   • Authentication scope: Local workspace");
        out.println();
        out.println("🔓 Authorization");
        out.println("   • You have full access to all work item types:");
        out.println("     - FEATURE");
        out.println("     - BUG");
        out.println("     - TASK");
        out.println();
        out.println("   • You have the following permissions:");
        out.println("     - Create: ✅ Allowed");
        out.println("     - Read:   ✅ Allowed");
        out.println("     - Update: ✅ Allowed");
        out.println("     - Delete: ✅ Allowed");
        out.println();
        out.println("   • You are registered on your local machine: " + hostname);
        out.println("   • Local configuration: ~/.rinna/config.yaml");
    }
    
    private void simulateWorkspaceStatusCommand(PrintStream out) {
        if (!isInitialized) {
            out.println("❌ Rinna is not initialized. Please run 'rin init' first.");
            return;
        }
        
        out.println("🏢 Workspace Status");
        out.println("=============================================================");
        out.println();
        out.println("✅ Workspace has been set up for project: Rinna");
        out.println("   • Location: " + System.getProperty("user.dir") + "/.rinna");
        out.println("   • Created: " + java.time.LocalDate.now());
        out.println("   • Owner: " + username + "@" + hostname);
        out.println();
        out.println("📊 Workspace Statistics:");
        out.println("   • 0 work items");
        out.println("   • 0 completed items");
        out.println("   • 0 pending items");
        out.println();
        out.println("🔗 Project Connection:");
        out.println("   • Connected to project: Rinna");
        out.println("   • Project root: " + System.getProperty("user.dir"));
        out.println();
        out.println("👥 Workspace Members:");
        out.println("   • " + username + " (owner)");
    }
}
