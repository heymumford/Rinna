package org.rinna.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of the Project interface.
 */
public class DefaultProject implements Project {

    private final String id;
    private String name;
    private String description;
    private final String key;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
    private final List<WorkItem> workItems;
    private final List<Release> releases;

    /**
     * Create a new project with the specified details.
     *
     * @param id the project ID
     * @param name the project name
     * @param description the project description
     * @param key the project key (abbreviation)
     */
    public DefaultProject(String id, String name, String description, String key) {
        this.id = Objects.requireNonNull(id, "Project ID cannot be null");
        this.name = Objects.requireNonNull(name, "Project name cannot be null");
        this.description = description;
        this.key = Objects.requireNonNull(key, "Project key cannot be null");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
        this.workItems = new ArrayList<>();
        this.releases = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the project name.
     *
     * @param name the new project name
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Project name cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Set the project description.
     *
     * @param description the new project description
     */
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Set the active status of the project.
     *
     * @param active true to mark the project as active, false otherwise
     */
    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public List<WorkItem> getWorkItems() {
        return new ArrayList<>(workItems);
    }

    @Override
    public void addWorkItem(WorkItem workItem) {
        Objects.requireNonNull(workItem, "Work item cannot be null");
        workItems.add(workItem);
        updatedAt = LocalDateTime.now();
    }

    @Override
    public List<Release> getReleases() {
        return new ArrayList<>(releases);
    }

    @Override
    public void addRelease(Release release) {
        Objects.requireNonNull(release, "Release cannot be null");
        releases.add(release);
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultProject that = (DefaultProject) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DefaultProject{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", active=" + active +
                '}';
    }
}