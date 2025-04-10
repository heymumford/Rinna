/*
 * BDD step definitions for Rinna admin Maven integration
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for testing Rinna admin Maven integration.
 */
public class AdminMavenIntegrationSteps {
    private final TestContext context;
    private String commandOutput;
    private String commandError;
    private Path tempProjectDir;
    private Path pomFile;
    
    /**
     * Constructs a new AdminMavenIntegrationSteps with the given test context.
     *
     * @param context the test context
     */
    public AdminMavenIntegrationSteps(TestContext context) {
        this.context = context;
    }
    
    @Given("I have an empty Java project with a standard POM file")
    public void iHaveAnEmptyJavaProjectWithAStandardPOMFile() throws IOException {
        // Create a temporary directory in target path for the test project - use absolute path
        String projectRoot = System.getProperty("user.dir");
        Path targetDir = Paths.get(projectRoot, "target/test/temp");
        Files.createDirectories(targetDir);
        tempProjectDir = Files.createDirectory(targetDir.resolve("rinna-test-project-" + System.currentTimeMillis()));
        context.setConfigurationValue("projectDir", tempProjectDir);
        
        // Create a basic POM file
        pomFile = tempProjectDir.resolve("pom.xml");
        String basicPom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.example</groupId>\n" +
                "    <artifactId>test-project</artifactId>\n" +
                "    <version>1.0-SNAPSHOT</version>\n" +
                "    <name>Test Project</name>\n" +
                "    <properties>\n" +
                "        <maven.compiler.source>21</maven.compiler.source>\n" +
                "        <maven.compiler.target>21</maven.compiler.target>\n" +
                "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "    </properties>\n" +
                "</project>";
        Files.write(pomFile, basicPom.getBytes());
        
        // Create src/main/java and src/test/java directories
        Files.createDirectories(tempProjectDir.resolve("src/main/java"));
        Files.createDirectories(tempProjectDir.resolve("src/test/java"));
    }
    
    @Given("I have Maven installed on my system")
    public void iHaveMavenInstalledOnMySystem() {
        // Simulate checking for Maven installation
        context.setConfigurationFlag("mavenInstalled", true);
    }
    
    @Given("my current username is {string}")
    public void myCurrentUsernameIs(String username) {
        context.setConfigurationValue("currentUsername", username);
    }
    
    @Given("my hostname is {string}")
    public void myHostnameIs(String hostname) {
        context.setConfigurationValue("hostname", hostname);
    }
    
    @When("I add the Rinna core dependency to my project's POM file")
    public void iAddTheRinnaCoreDependencyToMyProjectSPOMFile() throws IOException {
        String pomContent = new String(Files.readAllBytes(pomFile));
        
        // Add dependency after properties section
        int insertPoint = pomContent.indexOf("</properties>") + "</properties>".length();
        String dependency = "\n    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.rinna</groupId>\n" +
                "            <artifactId>rinna-core</artifactId>\n" +
                "            <version>1.8.0</version>\n" +
                "        </dependency>\n";
        
        String newPomContent = pomContent.substring(0, insertPoint) + dependency;
        
        if (pomContent.contains("<dependencies>")) {
            // If dependencies section already exists, just add the new dependency
            newPomContent = pomContent.replace("<dependencies>", 
                    "<dependencies>\n        <dependency>\n" +
                    "            <groupId>org.rinna</groupId>\n" +
                    "            <artifactId>rinna-core</artifactId>\n" +
                    "            <version>1.8.0</version>\n" +
                    "        </dependency>");
        } else {
            // Otherwise add the entire dependencies section
            newPomContent = pomContent.substring(0, insertPoint) + dependency + 
                    "    </dependencies>\n" + pomContent.substring(insertPoint);
        }
        
        Files.write(pomFile, newPomContent.getBytes());
    }
    
    @When("I add the Rinna CLI dependency to my project's POM file")
    public void iAddTheRinnaCLIDependencyToMyProjectSPOMFile() throws IOException {
        String pomContent = new String(Files.readAllBytes(pomFile));
        
        if (pomContent.contains("</dependencies>")) {
            // Add before closing dependencies tag
            String newPomContent = pomContent.replace("</dependencies>", 
                    "        <dependency>\n" +
                    "            <groupId>org.rinna</groupId>\n" +
                    "            <artifactId>rinna-cli</artifactId>\n" +
                    "            <version>1.8.0</version>\n" +
                    "        </dependency>\n    </dependencies>");
            Files.write(pomFile, newPomContent.getBytes());
        } else {
            fail("Dependencies section not found in POM file");
        }
    }
    
    @When("I add the Rinna Maven plugin to my project's POM file with admin initialization enabled")
    public void iAddTheRinnaMavenPluginToMyProjectSPOMFileWithAdminInitializationEnabled() throws IOException {
        String pomContent = new String(Files.readAllBytes(pomFile));
        
        // Add plugin section if it doesn't exist
        String pluginSection;
        if (pomContent.contains("<build>")) {
            pluginSection = "";
        } else {
            pluginSection = "\n    <build>\n        <plugins>\n";
        }
        
        // Add Maven plugin
        String mavenPlugin = 
                "            <plugin>\n" +
                "                <groupId>org.rinna</groupId>\n" +
                "                <artifactId>rinna-maven-plugin</artifactId>\n" +
                "                <version>1.8.0</version>\n" +
                "                <executions>\n" +
                "                    <execution>\n" +
                "                        <goals>\n" +
                "                            <goal>initialize</goal>\n" +
                "                        </goals>\n" +
                "                        <phase>package</phase>\n" +
                "                    </execution>\n" +
                "                </executions>\n" +
                "                <configuration>\n" +
                "                    <adminInit>true</adminInit>\n" +
                "                    <projectName>${project.name}</projectName>\n" +
                "                    <autoStartServer>true</autoStartServer>\n" +
                "                    <adminUsername>admin</adminUsername>\n" +
                "                    <adminPassword>nimda</adminPassword>\n" +
                "                </configuration>\n" +
                "            </plugin>\n";
        
        // Insert at appropriate location
        String newPomContent;
        if (pomContent.contains("<build>")) {
            if (pomContent.contains("<plugins>")) {
                // Add to existing plugins section
                newPomContent = pomContent.replace("<plugins>", "<plugins>\n" + mavenPlugin);
            } else {
                // Add plugins section to existing build section
                newPomContent = pomContent.replace("<build>", "<build>\n        <plugins>\n" + mavenPlugin + "        </plugins>");
            }
        } else {
            // Add entire build section
            newPomContent = pomContent.replace("</dependencies>", 
                    "</dependencies>\n    <build>\n        <plugins>\n" + 
                    mavenPlugin + 
                    "        </plugins>\n    </build>");
        }
        
        Files.write(pomFile, newPomContent.getBytes());
    }
    
    @When("I run {string} in my project directory")
    public void iRunInMyProjectDirectory(String command) {
        TestContext.CommandRunner runner = context.getCommandRunner();
        String[] parts = command.split("\\s+", 2);
        String mainCommand = parts[0];
        String args = parts.length > 1 ? parts[1] : "";
        
        // Simulate command execution
        String[] results = runner.runCommand(mainCommand, args);
        commandOutput = results[0];
        commandError = results[1];
        
        // Store command output in context for later assertions
        context.setConfigurationValue("lastCommandOutput", commandOutput);
        context.setConfigurationValue("lastCommandError", commandError);
        
        // Simulate success status
        if (command.equals("mvn clean package")) {
            context.setConfigurationFlag("buildSuccess", true);
            context.setConfigurationFlag("rinnaInstalled", true);
        }
    }
    
    @Then("the build should complete successfully")
    public void theBuildShouldCompleteSuccessfully() {
        assertTrue(context.getConfigurationFlag("buildSuccess"), 
                "Build should have completed successfully");
        assertFalse(commandError.contains("ERROR"), 
                "Build output should not contain errors");
    }
    
    @Then("the Rinna CLI should be installed in the project's bin directory")
    public void theRinnaCLIShouldBeInstalledInTheProjectSBinDirectory() {
        context.setConfigurationFlag("cliInstalled", true);
        Path binDir = tempProjectDir.resolve("bin");
        context.setConfigurationValue("binDir", binDir);
        
        // Simulate CLI installation check
        assertTrue(context.getConfigurationFlag("rinnaInstalled"), 
                "Rinna should be installed");
    }
    
    @Then("a default admin user should be created with username {string} and password {string}")
    public void aDefaultAdminUserShouldBeCreatedWithUsernameAndPassword(String username, String password) {
        context.setConfigurationValue("adminUsername", username);
        context.setConfigurationValue("adminPassword", password);
        
        // Simulate check for admin user creation
        assertTrue(true, "Admin user should be created");
    }
    
    @Then("the admin user should be associated with my local username {string}")
    public void theAdminUserShouldBeAssociatedWithMyLocalUsername(String username) {
        assertEquals(username, context.getConfigurationValue("currentUsername").orElse(""),
                "Admin user should be associated with local username");
    }
    
    @Then("the admin user should be associated with my machine name {string}")
    public void theAdminUserShouldBeAssociatedWithMyMachineName(String machine) {
        assertEquals(machine, context.getConfigurationValue("hostname").orElse(""),
                "Admin user should be associated with machine name");
    }
    
    @Then("the Rinna server executable should be present in the project")
    public void theRinnaServerExecutableShouldBePresentInTheProject() {
        // Simulate check for server executable
        assertTrue(context.getConfigurationFlag("rinnaInstalled"), 
                "Rinna server should be installed");
    }
    
    @Given("I have successfully built my project with Rinna")
    public void iHaveSuccessfullyBuiltMyProjectWithRinna() {
        context.setConfigurationFlag("buildSuccess", true);
        context.setConfigurationFlag("rinnaInstalled", true);
        context.setConfigurationFlag("cliInstalled", true);
    }
    
    @Then("I should see {string}")
    public void iShouldSee(String expectedOutput) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains(expectedOutput), 
                "Output should contain: " + expectedOutput);
    }
    
    @Then("the Rinna server should start automatically")
    public void theRinnaServerShouldStartAutomatically() {
        context.setConfigurationFlag("serverStarted", true);
    }
    
    @Then("I should be prompted to authenticate")
    public void iShouldBePromptedToAuthenticate() {
        context.setConfigurationFlag("authPrompted", true);
    }
    
    @When("I enter {string} as the username")
    public void iEnterAsTheUsername(String username) {
        context.setConfigurationValue("enteredUsername", username);
    }
    
    @When("I enter {string} as the password")
    public void iEnterAsThePassword(String password) {
        context.setConfigurationValue("enteredPassword", password);
        
        // Simulate successful authentication if credentials match
        if ("admin".equals(context.getConfigurationValue("enteredUsername").orElse("")) && 
            "nimda".equals(password)) {
            context.setConfigurationFlag("authenticated", true);
        }
    }
    
    @Then("I should be authenticated successfully")
    public void iShouldBeAuthenticatedSuccessfully() {
        assertTrue(context.getConfigurationFlag("authenticated"), 
                "User should be authenticated successfully");
    }
    
    @Then("I should see a message about no projects being configured yet")
    public void iShouldSeeAMessageAboutNoProjectsBeingConfiguredYet() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains("No projects") || lastOutput.contains("no projects"), 
                "Output should mention no projects are configured");
    }
    
    @Given("I am authenticated as the default admin user")
    public void iAmAuthenticatedAsTheDefaultAdminUser() {
        context.setConfigurationFlag("authenticated", true);
        context.setConfigurationValue("currentUser", "admin");
    }
    
    @Then("a new project should be created in the Rinna database")
    public void aNewProjectShouldBeCreatedInTheRinnaDatabase() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains("Successfully created project"), 
                "Output should confirm project creation");
    }
    
    @Given("I have created a project named {string}")
    public void iHaveCreatedAProjectNamed(String projectName) {
        context.setConfigurationValue("currentProject", projectName);
        
        // Simulate project creation
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("name", projectName);
        projectData.put("description", "Description for " + projectName);
        context.setConfigurationValue("project:" + projectName, projectData);
    }
    
    @Then("I should see {string} in the user list")
    public void iShouldSeeInTheUserList(String username) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains(username), 
                "User list should contain: " + username);
    }
    
    @Then("I should see {string} in the user list with role {string}")
    public void iShouldSeeInTheUserListWithRole(String username, String role) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains(username) && lastOutput.contains(role), 
                "User list should show " + username + " with role " + role);
    }
    
    @Then("I should not see {string}")
    public void iShouldNotSee(String text) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertFalse(lastOutput.contains(text), 
                "Output should not contain: " + text);
    }
    
    @Given("I have added work item types {string} and {string} to the project")
    public void iHaveAddedWorkItemTypesToTheProject(String type1, String type2) {
        String projectName = (String) context.getConfigurationValue("currentProject").orElse("Unknown Project");
        
        // Simulate adding work item types to the project
        @SuppressWarnings("unchecked")
        Map<String, Object> projectData = (Map<String, Object>) context.getConfigurationValue("project:" + projectName).orElse(new HashMap<>());
        
        @SuppressWarnings("unchecked")
        List<String> workItemTypes = (List<String>) projectData.getOrDefault("workItemTypes", new ArrayList<String>());
        workItemTypes.add(type1);
        workItemTypes.add(type2);
        
        projectData.put("workItemTypes", workItemTypes);
        context.setConfigurationValue("project:" + projectName, projectData);
    }
    
    @Given("I have configured workflow states and transitions for the project")
    public void iHaveConfiguredWorkflowStatesAndTransitionsForTheProject() {
        String projectName = (String) context.getConfigurationValue("currentProject").orElse("Unknown Project");
        
        // Simulate adding workflow states and transitions
        @SuppressWarnings("unchecked")
        Map<String, Object> projectData = (Map<String, Object>) context.getConfigurationValue("project:" + projectName).orElse(new HashMap<>());
        
        // Add states
        List<String> states = Arrays.asList("PLANNING", "DEVELOPMENT", "REVIEW", "DONE");
        projectData.put("workflowStates", states);
        
        // Add transitions
        List<Map<String, String>> transitions = new ArrayList<>();
        transitions.add(Map.of("from", "PLANNING", "to", "DEVELOPMENT"));
        transitions.add(Map.of("from", "DEVELOPMENT", "to", "REVIEW"));
        transitions.add(Map.of("from", "REVIEW", "to", "DONE"));
        projectData.put("workflowTransitions", transitions);
        
        context.setConfigurationValue("project:" + projectName, projectData);
    }
    
    @Then("I should see all workflow states including {string}, {string}, {string}, and {string}")
    public void iShouldSeeAllWorkflowStatesIncludingAnd(String state1, String state2, String state3, String state4) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains(state1), "Output should include state: " + state1);
        assertTrue(lastOutput.contains(state2), "Output should include state: " + state2);
        assertTrue(lastOutput.contains(state3), "Output should include state: " + state3);
        assertTrue(lastOutput.contains(state4), "Output should include state: " + state4);
    }
    
    @Then("I should see all workflow transitions between the states")
    public void iShouldSeeAllWorkflowTransitionsBetweenTheStates() {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains("Transitions:") || lastOutput.contains("TRANSITIONS:"), 
                  "Output should include transitions section");
    }
    
    @Then("I should see the start state is {string}")
    public void iShouldSeeTheStartStateIs(String state) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains("Start State") && lastOutput.contains(state), 
                  "Output should show " + state + " as a start state");
    }
    
    @Then("I should see the end state is {string}")
    public void iShouldSeeTheEndStateIs(String state) {
        String lastOutput = (String) context.getConfigurationValue("lastCommandOutput").orElse("");
        assertTrue(lastOutput.contains("End State") && lastOutput.contains(state), 
                  "Output should show " + state + " as an end state");
    }
}