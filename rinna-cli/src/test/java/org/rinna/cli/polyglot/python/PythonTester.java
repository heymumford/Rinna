package org.rinna.cli.polyglot.python;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.rinna.cli.polyglot.framework.PolyglotTestHarness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test utility for interacting with Python components.
 * This class provides methods for:
 * <ul>
 *   <li>Running Python tests</li>
 *   <li>Executing Python scripts</li>
 *   <li>Generating test fixtures for Python testing</li>
 *   <li>Testing Python integration with Java components</li>
 * </ul>
 */
public class PythonTester {
    private static final Logger logger = LoggerFactory.getLogger(PythonTester.class);
    
    private final PolyglotTestHarness harness;
    
    /**
     * Create a new PythonTester instance.
     * 
     * @param harness The polyglot test harness to use
     */
    public PythonTester(PolyglotTestHarness harness) {
        this.harness = harness;
    }
    
    /**
     * Run a Python module as a script.
     * 
     * @param modulePath The Python module path (e.g., "rinna.util.version")
     * @param args Arguments to pass to the script
     * @return The script output
     * @throws IOException If script execution fails
     * @throws InterruptedException If the script process is interrupted
     */
    public String runModule(String modulePath, String... args) throws IOException, InterruptedException {
        String argsString = Arrays.stream(args).collect(Collectors.joining(" "));
        String command = "python -m " + modulePath + " " + argsString;
        
        return harness.executePythonCommand(command, null, 30);
    }
    
    /**
     * Run a Python script.
     * 
     * @param scriptPath Path to the script (relative to the Python directory)
     * @param args Arguments to pass to the script
     * @return The script output
     * @throws IOException If script execution fails
     * @throws InterruptedException If the script process is interrupted
     */
    public String runScript(String scriptPath, String... args) throws IOException, InterruptedException {
        String argsString = Arrays.stream(args).collect(Collectors.joining(" "));
        String command = "python " + scriptPath + " " + argsString;
        
        return harness.executePythonCommand(command, null, 30);
    }
    
    /**
     * Run Python tests in a specific module or directory.
     * 
     * @param testPath The test module or directory path (e.g., "tests.unit" or "tests/unit")
     * @param verbose Enable verbose output
     * @return The test output
     * @throws IOException If test execution fails
     * @throws InterruptedException If the test process is interrupted
     */
    public String runTests(String testPath, boolean verbose) throws IOException, InterruptedException {
        String command = "python -m pytest " + testPath + (verbose ? " -v" : "");
        return harness.executePythonCommand(command, null, 60);
    }
    
    /**
     * Run the C4 diagram tests.
     * 
     * @param verbose Enable verbose output
     * @return The test output
     * @throws IOException If test execution fails
     * @throws InterruptedException If the test process is interrupted
     */
    public String runC4DiagramTests(boolean verbose) throws IOException, InterruptedException {
        Path binDir = harness.getProjectRoot().resolve("bin");
        String command = "python -m unittest " + 
                (verbose ? "-v " : "") + 
                "bin/test_c4_diagrams.py";
        
        return harness.executePythonCommand(command, harness.getProjectRoot(), 30);
    }
    
    /**
     * Generate C4 diagrams using the Python generator.
     * 
     * @param diagramTypes List of diagram types to generate (e.g., "context", "container", "component", "code")
     * @param outputFormat Output format (e.g., "svg", "png")
     * @param async Generate diagrams asynchronously
     * @return The generator output
     * @throws IOException If generator execution fails
     * @throws InterruptedException If the generator process is interrupted
     */
    public String generateC4Diagrams(List<String> diagramTypes, String outputFormat, boolean async) 
            throws IOException, InterruptedException {
        
        Path binDir = harness.getProjectRoot().resolve("bin");
        String typesArg = diagramTypes.isEmpty() ? "all" : String.join(",", diagramTypes);
        
        StringBuilder command = new StringBuilder("./generate-diagrams.sh");
        command.append(" --type ").append(typesArg);
        
        if (outputFormat != null && !outputFormat.isEmpty()) {
            command.append(" --format ").append(outputFormat);
        }
        
        if (async) {
            command.append(" --async");
        }
        
        return harness.executePythonCommand(command.toString(), binDir, 60);
    }
    
    /**
     * Create a Python test fixture in the temporary directory.
     * 
     * @param fixtureName Name of the fixture
     * @param content Content of the fixture
     * @return Path to the created fixture
     * @throws IOException If fixture creation fails
     */
    public Path createTestFixture(String fixtureName, String content) throws IOException {
        Path fixturePath = harness.getTestTempDir().resolve(fixtureName);
        Files.writeString(fixturePath, content);
        return fixturePath;
    }
    
    /**
     * Copy a file to the temporary directory as a test fixture.
     * 
     * @param sourcePath Source file path
     * @param fixtureName Name to give the fixture
     * @return Path to the copied fixture
     * @throws IOException If fixture copy fails
     */
    public Path copyFileAsFixture(Path sourcePath, String fixtureName) throws IOException {
        Path fixturePath = harness.getTestTempDir().resolve(fixtureName);
        Files.copy(sourcePath, fixturePath, StandardCopyOption.REPLACE_EXISTING);
        return fixturePath;
    }
    
    /**
     * Create a Python test script in the temporary directory.
     * 
     * @param scriptName Name of the script
     * @param content Content of the script
     * @return Path to the created script
     * @throws IOException If script creation fails
     */
    public Path createTestScript(String scriptName, String content) throws IOException {
        Path scriptPath = harness.getTestTempDir().resolve(scriptName);
        Files.writeString(scriptPath, content);
        
        // Make the script executable
        File scriptFile = scriptPath.toFile();
        scriptFile.setExecutable(true);
        
        return scriptPath;
    }
    
    /**
     * Execute a Python test script created in the temporary directory.
     * 
     * @param scriptName Name of the script to execute
     * @param args Arguments to pass to the script
     * @return The script output
     * @throws IOException If script execution fails
     * @throws InterruptedException If the script process is interrupted
     */
    public String executeTestScript(String scriptName, String... args) throws IOException, InterruptedException {
        Path scriptPath = harness.getTestTempDir().resolve(scriptName);
        if (!Files.exists(scriptPath)) {
            throw new IOException("Test script does not exist: " + scriptPath);
        }
        
        String argsString = Arrays.stream(args).collect(Collectors.joining(" "));
        String command = "python " + scriptPath.toString() + " " + argsString;
        
        return harness.executePythonCommand(command, harness.getTestTempDir(), 30);
    }
}