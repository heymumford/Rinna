/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import org.rinna.domain.model.Project;
import org.rinna.domain.model.Release;
import org.rinna.domain.model.WorkItem;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Service for generating documents from Rinna data.
 * This service provides document generation capabilities for various entities in the system.
 */
public interface DocumentService {

    /**
     * Document format options.
     */
    enum Format {
        PDF, DOCX, HTML
    }
    
    /**
     * Template type options.
     */
    enum TemplateType {
        PROJECT_SUMMARY,
        RELEASE_NOTES,
        WORKITEM_DETAILS,
        STATUS_REPORT,
        CUSTOM
    }
    
    /**
     * Generates a document for a work item.
     *
     * @param workItem the work item to generate a document for
     * @param format the output format
     * @param templateType the template type to use
     * @param output the output stream to write the document to
     */
    void generateWorkItemDocument(WorkItem workItem, Format format, TemplateType templateType, OutputStream output);
    
    /**
     * Generates a document for a project.
     *
     * @param project the project to generate a document for
     * @param format the output format
     * @param templateType the template type to use
     * @param output the output stream to write the document to
     */
    void generateProjectDocument(Project project, Format format, TemplateType templateType, OutputStream output);
    
    /**
     * Generates a document for a release.
     *
     * @param release the release to generate a document for
     * @param format the output format
     * @param templateType the template type to use
     * @param output the output stream to write the document to
     */
    void generateReleaseDocument(Release release, Format format, TemplateType templateType, OutputStream output);
    
    /**
     * Generates a document for a list of work items.
     *
     * @param workItems the work items to generate a document for
     * @param format the output format
     * @param templateType the template type to use
     * @param output the output stream to write the document to
     */
    void generateWorkItemsDocument(List<WorkItem> workItems, Format format, TemplateType templateType, OutputStream output);
    
    /**
     * Generates a custom document using a template and data.
     * 
     * @param templatePath the path to the template file
     * @param data the data to populate the template with
     * @param format the output format
     * @param output the output stream to write the document to
     */
    void generateCustomDocument(String templatePath, Map<String, Object> data, Format format, OutputStream output);
    
    /**
     * Returns whether this document service is available.
     * Some document services may require external dependencies or licenses.
     *
     * @return true if the service is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Returns the name of this document service.
     *
     * @return the service name
     */
    String getServiceName();
}