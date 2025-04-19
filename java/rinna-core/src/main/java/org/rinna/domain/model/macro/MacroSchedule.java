package org.rinna.domain.model.macro;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Defines a schedule for time-based macro execution.
 */
public class MacroSchedule {
    private ScheduleType type;           // Type of schedule
    private LocalDateTime startDateTime;  // When to start (for ONE_TIME)
    private LocalTime timeOfDay;          // Time of day (for DAILY, WEEKLY, MONTHLY)
    private Set<DayOfWeek> daysOfWeek;    // Days of week (for WEEKLY)
    private Set<Integer> daysOfMonth;     // Days of month (for MONTHLY)
    private ZoneId timeZone;              // Time zone
    private Integer interval;             // Interval (e.g., every 2 hours)
    private LocalDateTime endDateTime;    // When to end (optional)
    private Integer maxExecutions;        // Maximum number of executions (optional)

    /**
     * Enum representing the different types of schedules.
     */
    public enum ScheduleType {
        ONE_TIME,    // Execute once at a specific time
        HOURLY,      // Execute every X hours
        DAILY,       // Execute every day at a specific time
        WEEKLY,      // Execute on specific days of the week
        MONTHLY      // Execute on specific days of the month
    }

    /**
     * Default constructor.
     */
    public MacroSchedule() {
        this.daysOfWeek = EnumSet.noneOf(DayOfWeek.class);
        this.daysOfMonth = new HashSet<>();
        this.timeZone = ZoneId.systemDefault();
    }

    /**
     * Constructor for a one-time schedule.
     *
     * @param startDateTime when to execute
     */
    public MacroSchedule(LocalDateTime startDateTime) {
        this();
        this.type = ScheduleType.ONE_TIME;
        this.startDateTime = startDateTime;
    }

    /**
     * Constructor for a daily schedule.
     *
     * @param timeOfDay time of day to execute
     */
    public MacroSchedule(LocalTime timeOfDay) {
        this();
        this.type = ScheduleType.DAILY;
        this.timeOfDay = timeOfDay;
    }

    /**
     * Constructor for a weekly schedule.
     *
     * @param timeOfDay time of day to execute
     * @param daysOfWeek days of week to execute
     */
    public MacroSchedule(LocalTime timeOfDay, Set<DayOfWeek> daysOfWeek) {
        this();
        this.type = ScheduleType.WEEKLY;
        this.timeOfDay = timeOfDay;
        if (daysOfWeek != null) {
            this.daysOfWeek.addAll(daysOfWeek);
        }
    }

    public ScheduleType getType() {
        return type;
    }

    public void setType(ScheduleType type) {
        this.type = type;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalTime getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(LocalTime timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek != null ? 
                          EnumSet.copyOf(daysOfWeek) : 
                          EnumSet.noneOf(DayOfWeek.class);
    }

    public void addDayOfWeek(DayOfWeek dayOfWeek) {
        if (dayOfWeek != null) {
            this.daysOfWeek.add(dayOfWeek);
        }
    }

    public Set<Integer> getDaysOfMonth() {
        return daysOfMonth;
    }

    public void setDaysOfMonth(Set<Integer> daysOfMonth) {
        this.daysOfMonth = daysOfMonth != null ? new HashSet<>(daysOfMonth) : new HashSet<>();
    }

    public void addDayOfMonth(Integer dayOfMonth) {
        if (dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31) {
            this.daysOfMonth.add(dayOfMonth);
        }
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone = timeZone != null ? timeZone : ZoneId.systemDefault();
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Integer getMaxExecutions() {
        return maxExecutions;
    }

    public void setMaxExecutions(Integer maxExecutions) {
        this.maxExecutions = maxExecutions;
    }

    /**
     * Calculates the next execution time after the specified time.
     *
     * @param after the time to calculate from
     * @return the next execution time, or null if no more executions
     */
    public LocalDateTime calculateNextExecution(LocalDateTime after) {
        if (after == null) {
            after = LocalDateTime.now();
        }

        // Check end date
        if (endDateTime != null && after.isAfter(endDateTime)) {
            return null;
        }

        ZonedDateTime zonedAfter = after.atZone(timeZone);
        ZonedDateTime nextExecution = null;

        switch (type) {
            case ONE_TIME:
                if (startDateTime != null && !after.isAfter(startDateTime)) {
                    nextExecution = startDateTime.atZone(timeZone);
                }
                break;
            case HOURLY:
                if (interval == null || interval <= 0) {
                    interval = 1;
                }
                nextExecution = zonedAfter.plusHours(interval).withMinute(0).withSecond(0).withNano(0);
                break;
            case DAILY:
                if (timeOfDay == null) {
                    timeOfDay = LocalTime.of(0, 0);
                }
                nextExecution = zonedAfter.toLocalDate().plusDays(1).atTime(timeOfDay).atZone(timeZone);
                if (!nextExecution.isAfter(zonedAfter)) {
                    nextExecution = nextExecution.plusDays(1);
                }
                break;
            case WEEKLY:
                if (timeOfDay == null) {
                    timeOfDay = LocalTime.of(0, 0);
                }
                if (daysOfWeek.isEmpty()) {
                    daysOfWeek.add(DayOfWeek.MONDAY);
                }
                
                // Find next day of week
                ZonedDateTime candidateDate = zonedAfter;
                for (int i = 0; i < 7; i++) {
                    candidateDate = candidateDate.plusDays(1);
                    if (daysOfWeek.contains(candidateDate.getDayOfWeek())) {
                        nextExecution = candidateDate.with(timeOfDay);
                        break;
                    }
                }
                break;
            case MONTHLY:
                if (timeOfDay == null) {
                    timeOfDay = LocalTime.of(0, 0);
                }
                if (daysOfMonth.isEmpty()) {
                    daysOfMonth.add(1);  // First day of month
                }
                
                // Find next day of month
                ZonedDateTime currentDate = zonedAfter;
                boolean found = false;
                
                // Try current month first
                for (Integer day : daysOfMonth) {
                    ZonedDateTime candidate = currentDate.withDayOfMonth(day).with(timeOfDay);
                    if (candidate.isAfter(zonedAfter)) {
                        nextExecution = candidate;
                        found = true;
                        break;
                    }
                }
                
                // If not found, try next month
                if (!found) {
                    currentDate = zonedAfter.plusMonths(1).withDayOfMonth(1);
                    for (Integer day : daysOfMonth) {
                        if (day <= currentDate.toLocalDate().lengthOfMonth()) {
                            nextExecution = currentDate.withDayOfMonth(day).with(timeOfDay);
                            break;
                        }
                    }
                }
                break;
        }

        return nextExecution != null ? nextExecution.toLocalDateTime() : null;
    }

    /**
     * Creates a one-time schedule for immediate execution.
     *
     * @return a new MacroSchedule instance
     */
    public static MacroSchedule immediateExecution() {
        return new MacroSchedule(LocalDateTime.now());
    }

    /**
     * Creates a daily schedule.
     *
     * @param hour hour of day (0-23)
     * @param minute minute of hour (0-59)
     * @return a new MacroSchedule instance
     */
    public static MacroSchedule daily(int hour, int minute) {
        return new MacroSchedule(LocalTime.of(hour, minute));
    }

    /**
     * Creates a weekly schedule for Mondays.
     *
     * @param hour hour of day (0-23)
     * @param minute minute of hour (0-59)
     * @return a new MacroSchedule instance
     */
    public static MacroSchedule weeklyOnMonday(int hour, int minute) {
        MacroSchedule schedule = new MacroSchedule(LocalTime.of(hour, minute));
        schedule.setType(ScheduleType.WEEKLY);
        schedule.addDayOfWeek(DayOfWeek.MONDAY);
        return schedule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroSchedule that = (MacroSchedule) o;
        return type == that.type &&
                Objects.equals(startDateTime, that.startDateTime) &&
                Objects.equals(timeOfDay, that.timeOfDay) &&
                Objects.equals(daysOfWeek, that.daysOfWeek) &&
                Objects.equals(daysOfMonth, that.daysOfMonth) &&
                Objects.equals(timeZone, that.timeZone) &&
                Objects.equals(interval, that.interval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, startDateTime, timeOfDay, daysOfWeek, daysOfMonth, timeZone, interval);
    }

    @Override
    public String toString() {
        switch (type) {
            case ONE_TIME:
                return "One time at " + startDateTime;
            case HOURLY:
                return "Every " + interval + " hour(s)";
            case DAILY:
                return "Daily at " + timeOfDay;
            case WEEKLY:
                return "Weekly on " + daysOfWeek + " at " + timeOfDay;
            case MONTHLY:
                return "Monthly on day(s) " + daysOfMonth + " at " + timeOfDay;
            default:
                return "Unknown schedule type";
        }
    }
}