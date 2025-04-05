/*
 * Configuration for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.config;

import org.rinna.adapter.persistence.InMemoryItemRepository;
import org.rinna.adapter.persistence.InMemoryMetadataRepository;
import org.rinna.adapter.persistence.InMemoryQueueRepository;
import org.rinna.adapter.persistence.InMemoryReleaseRepository;
import org.rinna.adapter.service.DefaultDocumentService;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.adapter.service.DefaultQueueService;
import org.rinna.adapter.service.DefaultReleaseService;
import org.rinna.adapter.service.DefaultWorkflowService;
import org.rinna.adapter.service.DocmosisDocumentService;
import org.rinna.adapter.service.DocumentServiceFactory;
import org.rinna.domain.entity.DocumentConfig;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.repository.MetadataRepository;
import org.rinna.domain.repository.QueueRepository;
import org.rinna.domain.repository.ReleaseRepository;
import org.rinna.domain.usecase.DocumentService;
import org.rinna.domain.usecase.ItemService;
import org.rinna.domain.usecase.QueueService;
import org.rinna.domain.usecase.ReleaseService;
import org.rinna.domain.usecase.WorkflowService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration class for wiring together Rinna components.
 * This class follows the Dependency Injection pattern to provide properly
 * initialized instances of all components.
 */
public class RinnaConfig {
    private static final Logger LOGGER = Logger.getLogger(RinnaConfig.class.getName());
    
    private static final String DEFAULT_CONFIG_FILE = "docmosis.properties";
    private static final String DOCMOSIS_LICENSE_KEY_PROP = "docmosis.license.key";
    private static final String DOCMOSIS_SITE_PROP = "docmosis.site";
    private static final String DOCMOSIS_TEMPLATES_PATH_PROP = "docmosis.templates.path";
    private static final String DOCMOSIS_PREFERRED_PROP = "docmosis.preferred";
    
    private final Properties properties;
    private final DocumentConfig documentConfig;
    
    private ItemRepository itemRepository;
    private ReleaseRepository releaseRepository;
    private QueueRepository queueRepository;
    private MetadataRepository metadataRepository;
    private ItemService itemService;
    private WorkflowService workflowService;
    private ReleaseService releaseService;
    private QueueService queueService;
    private DocumentService documentService;
    
    /**
     * Initializes the configuration with default components.
     */
    public RinnaConfig() {
        // Load document configuration
        this.properties = loadProperties(DEFAULT_CONFIG_FILE);
        this.documentConfig = createDocumentConfig();
        
        // Initialize repositories
        this.itemRepository = new InMemoryItemRepository();
        this.releaseRepository = new InMemoryReleaseRepository();
        this.queueRepository = new InMemoryQueueRepository();
        this.metadataRepository = new InMemoryMetadataRepository();
        
        // Initialize services
        this.itemService = new DefaultItemService(itemRepository);
        this.workflowService = new DefaultWorkflowService(itemRepository);
        this.releaseService = new DefaultReleaseService(releaseRepository, itemService);
        this.queueService = new DefaultQueueService(queueRepository, itemService, metadataRepository);
        this.documentService = DocumentServiceFactory.createDocumentService(documentConfig);
    }
    
    /**
     * Returns the item repository.
     * 
     * @return the item repository
     */
    public ItemRepository getItemRepository() {
        return itemRepository;
    }
    
    /**
     * Returns the item service.
     * 
     * @return the item service
     */
    public ItemService getItemService() {
        return itemService;
    }
    
    /**
     * Returns the workflow service.
     * 
     * @return the workflow service
     */
    public WorkflowService getWorkflowService() {
        return workflowService;
    }
    
    /**
     * Sets a custom item repository.
     * This will automatically update the services to use the new repository.
     * 
     * @param itemRepository the item repository
     * @return this configuration
     */
    public RinnaConfig setItemRepository(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
        this.itemService = new DefaultItemService(itemRepository);
        this.workflowService = new DefaultWorkflowService(itemRepository);
        this.releaseService = new DefaultReleaseService(releaseRepository, itemService);
        return this;
    }
    
    /**
     * Sets a custom item service.
     * 
     * @param itemService the item service
     * @return this configuration
     */
    public RinnaConfig setItemService(ItemService itemService) {
        this.itemService = itemService;
        return this;
    }
    
    /**
     * Sets a custom workflow service.
     * 
     * @param workflowService the workflow service
     * @return this configuration
     */
    public RinnaConfig setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
        return this;
    }
    
    /**
     * Returns the release repository.
     * 
     * @return the release repository
     */
    public ReleaseRepository getReleaseRepository() {
        return releaseRepository;
    }
    
    /**
     * Sets a custom release repository.
     * This will automatically update the releaseService to use the new repository.
     * 
     * @param releaseRepository the release repository
     * @return this configuration
     */
    public RinnaConfig setReleaseRepository(ReleaseRepository releaseRepository) {
        this.releaseRepository = releaseRepository;
        this.releaseService = new DefaultReleaseService(releaseRepository, itemService);
        return this;
    }
    
    /**
     * Returns the release service.
     * 
     * @return the release service
     */
    public ReleaseService getReleaseService() {
        return releaseService;
    }
    
    /**
     * Sets a custom release service.
     * 
     * @param releaseService the release service
     * @return this configuration
     */
    public RinnaConfig setReleaseService(ReleaseService releaseService) {
        this.releaseService = releaseService;
        return this;
    }
    
    /**
     * Returns the queue repository.
     * 
     * @return the queue repository
     */
    public QueueRepository getQueueRepository() {
        return queueRepository;
    }
    
    /**
     * Sets a custom queue repository.
     * This will automatically update the queueService to use the new repository.
     * 
     * @param queueRepository the queue repository
     * @return this configuration
     */
    public RinnaConfig setQueueRepository(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
        this.queueService = new DefaultQueueService(queueRepository, itemService, metadataRepository);
        return this;
    }
    
    /**
     * Returns the metadata repository.
     * 
     * @return the metadata repository
     */
    public MetadataRepository getMetadataRepository() {
        return metadataRepository;
    }
    
    /**
     * Sets a custom metadata repository.
     * This will automatically update the queueService to use the new repository.
     * 
     * @param metadataRepository the metadata repository
     * @return this configuration
     */
    public RinnaConfig setMetadataRepository(MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
        this.queueService = new DefaultQueueService(queueRepository, itemService, metadataRepository);
        return this;
    }
    
    /**
     * Returns the queue service.
     * 
     * @return the queue service
     */
    public QueueService getQueueService() {
        return queueService;
    }
    
    /**
     * Sets a custom queue service.
     * 
     * @param queueService the queue service
     * @return this configuration
     */
    public RinnaConfig setQueueService(QueueService queueService) {
        this.queueService = queueService;
        return this;
    }
    
    /**
     * Returns the document service.
     * 
     * @return the document service
     */
    public DocumentService getDocumentService() {
        return documentService;
    }
    
    /**
     * Sets a custom document service.
     * 
     * @param documentService the document service
     * @return this configuration
     */
    public RinnaConfig setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
        return this;
    }
    
    /**
     * Returns the document configuration.
     * 
     * @return the document configuration
     */
    public DocumentConfig getDocumentConfig() {
        return documentConfig;
    }
    
    /**
     * Gets a property value.
     * 
     * @param key the property key
     * @return the property value, or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Gets a property value with a default.
     * 
     * @param key the property key
     * @param defaultValue the default value if the property is not found
     * @return the property value, or the default if not found
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Sets a configuration property.
     * 
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
     * Updates the Docmosis license key.
     * This will reinitialize the document service.
     * 
     * @param licenseKey the Docmosis license key
     * @return this configuration
     */
    public RinnaConfig setDocmosisLicenseKey(String licenseKey) {
        properties.setProperty(DOCMOSIS_LICENSE_KEY_PROP, licenseKey);
        DocumentConfig newConfig = createDocumentConfig();
        this.documentService = DocumentServiceFactory.createDocumentService(newConfig);
        return this;
    }
    
    /**
     * Loads properties from the given file.
     */
    private Properties loadProperties(String configFile) {
        Properties props = new Properties();
        
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input != null) {
                props.load(input);
                LOGGER.info("Loaded configuration from " + configFile);
            } else {
                LOGGER.warning("Configuration file not found: " + configFile);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load configuration", e);
        }
        
        return props;
    }
    
    /**
     * Creates a document configuration from the properties.
     */
    private DocumentConfig createDocumentConfig() {
        String licenseKey = properties.getProperty(DOCMOSIS_LICENSE_KEY_PROP, "");
        String site = properties.getProperty(DOCMOSIS_SITE_PROP, "");
        String templatesPathStr = properties.getProperty(DOCMOSIS_TEMPLATES_PATH_PROP, "templates");
        boolean preferDocmosis = Boolean.parseBoolean(properties.getProperty(DOCMOSIS_PREFERRED_PROP, "true"));
        
        Path templatesPath = Paths.get(templatesPathStr);
        
        return new DocumentConfig.Builder()
                .docmosisLicenseKey(licenseKey)
                .docmosisSite(site)
                .templatesPath(templatesPath)
                .preferDocmosis(preferDocmosis)
                .build();
    }
}