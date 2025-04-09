package org.rinna.domain.service.macro;

import org.rinna.domain.model.macro.MacroSchedule;
import org.rinna.domain.model.macro.ScheduledExecution;

import java.util.List;

/**
 * Service interface for scheduling macro executions.
 */
public interface SchedulerService {
    /**
     * Schedules a macro for execution according to the given schedule.
     *
     * @param macroId the macro ID
     * @param schedule the execution schedule
     */
    void scheduleMacro(String macroId, MacroSchedule schedule);
    
    /**
     * Cancels all scheduled executions for a macro.
     *
     * @param macroId the macro ID
     */
    void cancelScheduledMacro(String macroId);
    
    /**
     * Retrieves all scheduled executions.
     *
     * @return a list of scheduled executions
     */
    List<ScheduledExecution> getScheduledExecutions();
    
    /**
     * Starts the scheduler.
     */
    void start();
    
    /**
     * Stops the scheduler.
     */
    void stop();
    
    /**
     * Checks if the scheduler is running.
     *
     * @return true if the scheduler is running
     */
    boolean isRunning();
}