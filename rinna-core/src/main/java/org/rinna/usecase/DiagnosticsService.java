package org.rinna.usecase;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service for system diagnostics.
 */
public interface DiagnosticsService {
    /**
     * Run a diagnostic test.
     *
     * @param testName the name of the diagnostic test to run
     * @return the result of the diagnostic test
     */
    DiagnosticResult runDiagnostic(String testName);
    
    /**
     * Run all available diagnostic tests.
     *
     * @return list of diagnostic results
     */
    List<DiagnosticResult> runAllDiagnostics();
    
    /**
     * Schedule a diagnostic test to run at a specified interval.
     *
     * @param testName the name of the diagnostic test to schedule
     * @param interval the interval to run the test at (e.g., "hourly", "daily", "weekly")
     * @return the ID of the scheduled diagnostic
     */
    String scheduleDiagnostic(String testName, String interval);
    
    /**
     * Get the status of a scheduled diagnostic.
     *
     * @param scheduleId the ID of the scheduled diagnostic
     * @return the status of the scheduled diagnostic
     */
    ScheduleStatus getScheduleStatus(String scheduleId);
    
    /**
     * Cancel a scheduled diagnostic.
     *
     * @param scheduleId the ID of the scheduled diagnostic
     * @return true if the scheduled diagnostic was cancelled, false otherwise
     */
    boolean cancelScheduledDiagnostic(String scheduleId);
    
    /**
     * Get a list of available diagnostic tests.
     *
     * @return list of available diagnostic tests
     */
    List<String> getAvailableTests();
    
    /**
     * Get diagnostic history.
     *
     * @param startDate the start date for the history
     * @param endDate the end date for the history
     * @return list of diagnostic results in the date range
     */
    List<DiagnosticResult> getDiagnosticHistory(Date startDate, Date endDate);
    
    /**
     * Diagnostic result.
     */
    interface DiagnosticResult {
        /**
         * Get the name of the diagnostic test.
         *
         * @return the test name
         */
        String getTestName();
        
        /**
         * Get the status of the diagnostic test.
         *
         * @return the test status
         */
        DiagnosticStatus getStatus();
        
        /**
         * Get the message from the diagnostic test.
         *
         * @return the test message
         */
        String getMessage();
        
        /**
         * Get the timestamp of the diagnostic test.
         *
         * @return the test timestamp
         */
        Date getTimestamp();
        
        /**
         * Get detailed metrics from the diagnostic test.
         *
         * @return map of metric name to value
         */
        Map<String, Object> getMetrics();
    }
    
    /**
     * Diagnostic status.
     */
    enum DiagnosticStatus {
        PASS,
        WARNING,
        FAIL,
        ERROR
    }
    
    /**
     * Schedule status.
     */
    enum ScheduleStatus {
        ACTIVE,
        PAUSED,
        CANCELLED
    }
}