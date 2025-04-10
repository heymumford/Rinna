package org.rinna.domain.service.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


/**
 * Service interface for generating management dashboards and reports related to work items,
 * cognitive load, and team performance within the Ryorin-do framework.
 */
public interface DashboardService {
    
    /**
     * Generates a snapshot of the current state of all work items and their distribution.
     * 
     * @return A comprehensive dashboard data object
     */
    DashboardData generateDashboard();
    
    /**
     * Generates a dashboard focused on cognitive load across organizational units.
     * 
     * @return A cognitive load dashboard data object
     */
    CognitiveLoadDashboardData generateCognitiveLoadDashboard();
    
    /**
     * Generates a dashboard showing the CYNEFIN domain distribution across work items.
     * 
     * @return A map of CYNEFIN domains to their counts and percentages
     */
    DomainDistributionData generateCynefinDistribution();
    
    /**
     * Generates a dashboard showing the work paradigm distribution across work items.
     * 
     * @return A map of work paradigms to their counts and percentages
     */
    ParadigmDistributionData generateWorkParadigmDistribution();
    
    /**
     * Generates a trend analysis of cognitive load over time.
     * 
     * @param startDate The start date for the trend analysis
     * @param endDate The end date for the trend analysis
     * @param interval The interval for data points (e.g., "daily", "weekly")
     * @return A trend analysis data object
     */
    TrendAnalysisData generateCognitiveLoadTrend(Instant startDate, Instant endDate, String interval);
    
    /**
     * Generates a report of potential cognitive overload situations.
     * 
     * @return A list of overload risk data objects
     */
    List<OverloadRiskData> identifyOverloadRisks();
    
    /**
     * Generates recommendations for work item reassignments to optimize cognitive load balance.
     * 
     * @return A list of reassignment recommendation data objects
     */
    List<ReassignmentRecommendation> generateReassignmentRecommendations();
    
    /**
     * Generates a dashboard for a specific organizational unit.
     * 
     * @param unitId The ID of the organizational unit
     * @return A unit-specific dashboard data object
     */
    UnitDashboardData generateUnitDashboard(UUID unitId);
    
    /**
     * Generates a historical comparison of estimated vs. actual cognitive load.
     * 
     * @param startDate The start date for the comparison
     * @param endDate The end date for the comparison
     * @return A comparison data object
     */
    EstimationAccuracyData generateEstimationAccuracyReport(Instant startDate, Instant endDate);
}