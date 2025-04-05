/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.entity.DocumentConfig;
import org.rinna.domain.entity.Project;
import org.rinna.domain.entity.Release;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.usecase.DocumentService;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Document service implementation that uses Docmosis for document generation.
 * This implementation is conditional and will only be active if the Docmosis libraries are available
 * and a valid license key is provided.
 */
public class DocmosisDocumentService implements DocumentService {
    private static final Logger LOGGER = Logger.getLogger(DocmosisDocumentService.class.getName());
    
    // Use lazy initialization pattern to avoid class loading errors if Docmosis isn't available
    private static class DocmosisHolder {
        // These will cause ClassNotFoundError if Docmosis is not available,
        // but only when the class is loaded (not when DocmosisDocumentService is instantiated)
        private static final boolean DOCMOSIS_AVAILABLE;
        
        static {
            boolean available = false;
            try {
                // Check if Docmosis classes are available
                Class.forName("com.docmosis.SystemManager");
                Class.forName("com.docmosis.template.TemplateStore");
                Class.forName("com.docmosis.renderer.DocumentRenderer");
                available = true;
                LOGGER.info("Docmosis library detected on classpath");
            } catch (ClassNotFoundException e) {
                LOGGER.warning("Docmosis library not available: " + e.getMessage());
                available = false;
            }
            DOCMOSIS_AVAILABLE = available;
        }
    }
    
    private final DocumentConfig config;
    private final Object systemManager; // Object instead of SystemManager to avoid direct dependency
    private boolean initialized = false;
    
    /**
     * Creates a new DocmosisDocumentService with the given configuration.
     * 
     * @param config the document configuration
     */
    public DocmosisDocumentService(DocumentConfig config) {
        this.config = config;
        this.systemManager = initializeDocmosis();
    }
    
    private Object initializeDocmosis() {
        if (!isAvailable()) {
            LOGGER.warning("Docmosis is not available, initialization skipped");
            return null;
        }
        
        if (!config.isDocmosisConfigured()) {
            LOGGER.warning("Docmosis license key not configured, initialization skipped");
            return null;
        }
        
        try {
            // Use reflection to avoid direct dependency
            Class<?> systemManagerClass = Class.forName("com.docmosis.SystemManager");
            Object manager = systemManagerClass.getDeclaredMethod("getSystemManager").invoke(null);
            
            // Set the license key
            String licenseKey = config.getDocmosisLicenseKey().orElseThrow();
            systemManagerClass.getDeclaredMethod("setLicenseKey", String.class).invoke(manager, licenseKey);
            
            // Initialize the system
            systemManagerClass.getDeclaredMethod("initialize").invoke(manager);
            
            // Register templates
            Class<?> templateStoreClass = Class.forName("com.docmosis.template.TemplateStore");
            Object templateStore = systemManagerClass.getDeclaredMethod("getTemplateStore").invoke(manager);
            
            Path templatesPath = config.getTemplatesPath();
            templateStoreClass.getDeclaredMethod("load", String.class).invoke(
                    templateStore, templatesPath.toAbsolutePath().toString());
            
            initialized = true;
            LOGGER.info("Docmosis initialized successfully with templates from: " + templatesPath);
            return manager;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Docmosis", e);
            return null;
        }
    }
    
    /**
     * Converts the service format enum to Docmosis output format string.
     */
    private String getDocmosisFormat(Format format) {
        return switch (format) {
            case PDF -> "PDF";
            case DOCX -> "DOCX";
            case HTML -> "HTML";
        };
    }
    
    /**
     * Gets the template name for a given entity and template type.
     */
    private String getTemplateName(TemplateType templateType) {
        return switch (templateType) {
            case PROJECT_SUMMARY -> "project_summary.docx";
            case RELEASE_NOTES -> "release_notes.docx";
            case WORKITEM_DETAILS -> "workitem_details.docx";
            case STATUS_REPORT -> "status_report.docx";
            case CUSTOM -> "custom.docx";
        };
    }
    
    @Override
    public void generateWorkItemDocument(WorkItem workItem, Format format, TemplateType templateType, OutputStream output) {
        if (!isAvailable() || !initialized) {
            throw new IllegalStateException("Docmosis document service is not available");
        }
        
        try {
            // Prepare data for the template
            Map<String, Object> data = new HashMap<>();
            data.put("workItem", workItem);
            data.put("itemId", workItem.getId().toString());
            data.put("title", workItem.getTitle());
            data.put("description", workItem.getDescription());
            data.put("type", workItem.getType().name());
            data.put("status", workItem.getStatus().name());
            data.put("priority", workItem.getPriority().name());
            data.put("assignee", workItem.getAssignee());
            data.put("createdAt", workItem.getCreatedAt().toString());
            data.put("updatedAt", workItem.getUpdatedAt().toString());
            
            renderDocument(getTemplateName(templateType), data, format, output);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate work item document", e);
            throw new RuntimeException("Document generation failed", e);
        }
    }
    
    @Override
    public void generateProjectDocument(Project project, Format format, TemplateType templateType, OutputStream output) {
        if (!isAvailable() || !initialized) {
            throw new IllegalStateException("Docmosis document service is not available");
        }
        
        try {
            // Prepare data for the template
            Map<String, Object> data = new HashMap<>();
            data.put("project", project);
            data.put("projectId", project.getId().toString());
            data.put("key", project.getKey());
            data.put("name", project.getName());
            data.put("description", project.getDescription());
            data.put("createdAt", project.getCreatedAt().toString());
            data.put("updatedAt", project.getUpdatedAt().toString());
            data.put("active", project.isActive());
            
            renderDocument(getTemplateName(templateType), data, format, output);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate project document", e);
            throw new RuntimeException("Document generation failed", e);
        }
    }
    
    @Override
    public void generateReleaseDocument(Release release, Format format, TemplateType templateType, OutputStream output) {
        if (!isAvailable() || !initialized) {
            throw new IllegalStateException("Docmosis document service is not available");
        }
        
        try {
            // Prepare data for the template
            Map<String, Object> data = new HashMap<>();
            data.put("release", release);
            
            renderDocument(getTemplateName(templateType), data, format, output);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate release document", e);
            throw new RuntimeException("Document generation failed", e);
        }
    }
    
    @Override
    public void generateWorkItemsDocument(List<WorkItem> workItems, Format format, TemplateType templateType, OutputStream output) {
        if (!isAvailable() || !initialized) {
            throw new IllegalStateException("Docmosis document service is not available");
        }
        
        try {
            // Prepare data for the template
            Map<String, Object> data = new HashMap<>();
            data.put("workItems", workItems);
            data.put("itemCount", workItems.size());
            
            renderDocument(getTemplateName(templateType), data, format, output);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate work items document", e);
            throw new RuntimeException("Document generation failed", e);
        }
    }
    
    @Override
    public void generateCustomDocument(String templatePath, Map<String, Object> data, Format format, OutputStream output) {
        if (!isAvailable() || !initialized) {
            throw new IllegalStateException("Docmosis document service is not available");
        }
        
        try {
            renderDocument(templatePath, data, format, output);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate custom document", e);
            throw new RuntimeException("Document generation failed", e);
        }
    }
    
    /**
     * Renders a document using Docmosis.
     */
    private void renderDocument(String templateName, Map<String, Object> data, Format format, OutputStream output) 
            throws Exception {
        // Use reflection to avoid direct dependency
        Class<?> rendererClass = Class.forName("com.docmosis.renderer.DocumentRenderer");
        Object renderer = Class.forName("com.docmosis.SystemManager")
                .getDeclaredMethod("getRenderer")
                .invoke(systemManager);
        
        // Create render request
        Class<?> requestClass = Class.forName("com.docmosis.renderer.RenderRequest");
        Object request = requestClass.getDeclaredConstructor().newInstance();
        
        // Set template, data, format and output
        requestClass.getDeclaredMethod("setTemplateName", String.class)
                .invoke(request, templateName);
        
        requestClass.getDeclaredMethod("setData", Map.class)
                .invoke(request, data);
        
        requestClass.getDeclaredMethod("setOutputStreamDestination", OutputStream.class)
                .invoke(request, output);
        
        requestClass.getDeclaredMethod("setOutputFormat", String.class)
                .invoke(request, getDocmosisFormat(format));
        
        // Render the document
        rendererClass.getDeclaredMethod("render", requestClass)
                .invoke(renderer, request);
    }
    
    @Override
    public boolean isAvailable() {
        return DocmosisHolder.DOCMOSIS_AVAILABLE && config.isDocmosisConfigured();
    }
    
    @Override
    public String getServiceName() {
        return "Docmosis Document Service";
    }
}