package org.rinna.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface representing a project in Rinna.
 */
public interface Project {
    /**
     * Get the project ID.
     *
     * @return the project ID
     */
    String getId();

    /**
     * Get the project name.
     *
     * @return the project name
     */
    String getName();

    /**
     * Get the project description.
     *
     * @return the project description
     */
    String getDescription();

    /**
     * Get the project key (abbreviation).
     *
     * @return the project key
     */
    String getKey();

    /**
     * Get the creation date of the project.
     *
     * @return the creation date
     */
    LocalDateTime getCreatedAt();

    /**
     * Get the last update date of the project.
     *
     * @return the last update date
     */
    LocalDateTime getUpdatedAt();

    /**
     * Get the list of work items associated with this project.
     *
     * @return the list of work items
     */
    List<WorkItem> getWorkItems();

    /**
     * Add a work item to the project.
     *
     * @param workItem the work item to add
     */
    void addWorkItem(WorkItem workItem);

    /**
     * Get the list of releases associated with this project.
     *
     * @return the list of releases
     */
    List<Release> getReleases();

    /**
     * Add a release to the project.
     *
     * @param release the release to add
     */
    void addRelease(Release release);
    
    /**
     * Check if the project is active.
     *
     * @return true if the project is active, false otherwise
     */
    boolean isActive();
}