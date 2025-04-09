package org.rinna.cli;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * A standalone test runner for CLI tests.
 */
public class RinnaCliTestRunner {

    public static void main(String[] args) {
        // Define the test classes to run
        List<Class<?>> testClasses = Arrays.asList(
                org.rinna.cli.command.ListCommandTest.class,
                org.rinna.cli.command.AddCommandTest.class,
                org.rinna.cli.acceptance.WorkflowAcceptanceTest.class
        );

        // Create selectors for each test class
        DiscoverySelector[] selectors = testClasses.stream()
                .map(DiscoverySelectors::selectClass)
                .toArray(DiscoverySelector[]::new);

        // Create a test plan
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors)
                .build();

        // Create a launcher and a listener
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        // Register the listener and run the tests
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        // Print out the summary
        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out, true));

        // Exit with success if all tests passed
        if (summary.getTestsFailedCount() > 0) {
            System.exit(1);
        }
    }
}