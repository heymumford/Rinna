/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import org.rinna.domain.model.Project;
import org.rinna.domain.model.Release;
import org.rinna.domain.model.WorkItem;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Service for generating documents from Rinna data.
 * 
 * <p>This service provides document generation capabilities for various entities in the system,
 * allowing export of data in different formats for reporting, sharing, and archiving purposes.</p>
 * 
 * <p>The document service uses templates to generate formatted documents based on Rinna data models.
 * It supports multiple output formats and template types, which can be selected based on the
 * specific document requirements.</p>
 * 
 * <p>Implementations of this service may use different document generation engines, such as
 * Docmosis, Apache POI, or custom template engines. The interface abstracts away these
 * implementation details, providing a consistent API for document generation.</p>
 * 
 * @author Eric C. Mumford
 * @since 1.0
 */
public interface DocumentService {

    /**
     * Document format options for generated output.
     * 
     * <p>Defines the supported file formats for document generation:</p>
     * <ul>
     *   <li>{@code PDF} - Portable Document Format, suitable for distribution and printing</li>
     *   <li>{@code DOCX} - Microsoft Word format, suitable for further editing</li>
     *   <li>{@code HTML} - Web page format, suitable for online viewing</li>
     * </ul>
     */
    enum Format {
        /** Portable Document Format */
        PDF,
        
        /** Microsoft Word Document Format */
        DOCX,
        
        /** HTML Web Page Format */
        HTML
    }
    
    /**
     * Template type options for document generation.
     * 
     * <p>Defines the different types of templates available for document generation:</p>
     * <ul>
     *   <li>{@code PROJECT_SUMMARY} - Summary of project details and metrics</li>
     *   <li>{@code RELEASE_NOTES} - Release notes detailing changes in a release</li>
     *   <li>{@code WORKITEM_DETAILS} - Detailed information about a work item</li>
     *   <li>{@code STATUS_REPORT} - Project or team status report</li>
     *   <li>{@code CUSTOM} - Custom template specified by the user</li>
     * </ul>
     */
    enum TemplateType {
        /** Project summary template */
        PROJECT_SUMMARY,
        
        /** Release notes template */
        RELEASE_NOTES,
        
        /** Work item details template */
        WORKITEM_DETAILS,
        
        /** Status report template */
        STATUS_REPORT,
        
        /** Custom template */
        CUSTOM
    }
    
    /**
     * Generates a document for a work item.
     * 
     * <p>This method creates a document containing details about a single work item.
     * It uses the specified template type and generates the output in the requested format.</p>
     * 
     * <p>The document typically includes work item attributes such as title, description,
     * status, assignee, and other relevant information. The exact content depends on the
     * template type selected.</p>
     *
     * @param workItem the work item to generate a document for
     * @param format the output format (PDF, DOCX, or HTML)
     * @param templateType the template type to use
     * @param output the output stream to write the document to
     * @throws IllegalArgumentException if any parameter is null or invalid
     * @throws java.io.IOException if an I/O error occurs during document generation
     */
    void generateWorkItemDocument(WorkItem workItem, Format format, TemplateType templateType, OutputStream output);
    
    /**
     * Generates a document for a project.
     * 
     * <p>This method creates a document containing information about a project.
     * It uses the specified template type and generates the output in the requested format.</p>
     * 
     * <p>The document typically includes project metadata, summary statistics, workload
     * distribution, and progress information. The PROJECT_SUMMARY template type is
     * commonly used for this method.</p>
     *
     * @param project the project to generate a document for
     * @param format the output format (PDF, DOCX, or HTML)
     * @param templateType the template type to use
     * @param output the output stream to write the document to
     * @throws IllegalArgumentException if any parameter is null or invalid
     * @throws java.io.IOException if an I/O error occurs during document generation
     */
    void generateProjectDocument(Project project, Format format, TemplateType templateType, OutputStream output);
    
    /**
     * Generates a document for a release.
     * 
     * <p>This method creates a document containing information about a release.
     * It uses the specified template type and generates the output in the requested format.</p>
     * 
     * <p>The document typically includes release notes, new features, bug fixes, and other
     * changes included in the release. The RELEASE_NOTES template type is most appropriate
     * for this method.</p>
     *
     * @param release the release to generate a document for
     * @param format the output format (PDF, DOCX, or HTML)
     * @param templateType the template type to use
     * @param output the output stream to write the document to
     * @throws IllegalArgumentException if any parameter is null or invalid
     * @throws java.io.IOException if an I/O error occurs during document generation
     */
    void generateReleaseDocument(Release release, Format format, TemplateType templateType, OutputStream output);
    
    /**
     * Generates a document for a list of work items.
     * 
     * <p>This method creates a document containing information about multiple work items.
     * It uses the specified template type and generates the output in the requested format.</p>
     * 
     * <p>This is useful for generating reports such as sprint backlogs, status reports, or
     * filtered lists of work items. The STATUS_REPORT template type is often used with
     * this method.</p>
     *
     * @param workItems the work items to generate a document for
     * @param format the output format (PDF, DOCX, or HTML)
     * @param templateType the template type to use
     * @param output the output stream to write the document to
     * @throws IllegalArgumentException if any parameter is null or invalid
     * @throws java.io.IOException if an I/O error occurs during document generation
     */
    void generateWorkItemsDocument(List<WorkItem> workItems, Format format, TemplateType templateType, OutputStream output);
    
    /**
     * Generates a custom document using a template and data.
     * 
     * <p>This method creates a document using a custom template and a map of data.
     * It provides maximum flexibility for document generation by allowing the caller
     * to specify both the template and the data to populate it with.</p>
     * 
     * <p>The templatePath should point to a template file in a format understood by
     * the underlying document generation engine. The data map contains key-value pairs
     * that will be used to populate placeholders in the template.</p>
     * 
     * @param templatePath the path to the template file
     * @param data the data to populate the template with
     * @param format the output format (PDF, DOCX, or HTML)
     * @param output the output stream to write the document to
     * @throws IllegalArgumentException if any parameter is null or invalid
     * @throws java.io.IOException if an I/O error occurs during document generation
     * @throws java.io.FileNotFoundException if the template file cannot be found
     */
    void generateCustomDocument(String templatePath, Map<String, Object> data, Format format, OutputStream output);
    
    /**
     * Returns whether this document service is available.
     * 
     * <p>Some document service implementations may require external dependencies,
     * licenses, or configurations to be available. This method checks whether
     * the service is properly configured and ready to generate documents.</p>
     * 
     * <p>Callers should check this method before attempting to generate documents,
     * and provide appropriate fallback behavior if the service is not available.</p>
     *
     * @return true if the service is available and ready to generate documents, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Returns the name of this document service implementation.
     * 
     * <p>This method returns a descriptive name for the document service implementation.
     * It can be used for logging, monitoring, or displaying information about the
     * document generation engine being used.</p>
     *
     * @return the service name (e.g., "Docmosis", "Apache POI", etc.)
     */
    String getServiceName();
}