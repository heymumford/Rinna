/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.entity;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Configuration for document generation services.
 * This configuration holds settings for various document services including Docmosis.
 */
public class DocumentConfig {
    private final String docmosisLicenseKey;
    private final String docmosisSite;
    private final Path templatesPath;
    private final boolean preferDocmosis;
    
    /**
     * Creates a new DocumentConfig with the given settings.
     * 
     * @param docmosisLicenseKey the Docmosis license key (can be null or empty if not using Docmosis)
     * @param docmosisSite the Docmosis site identifier (can be null or empty if not using Docmosis)
     * @param templatesPath the path to document templates
     * @param preferDocmosis whether to prefer Docmosis over other document engines when available
     */
    public DocumentConfig(String docmosisLicenseKey, String docmosisSite, Path templatesPath, boolean preferDocmosis) {
        this.docmosisLicenseKey = docmosisLicenseKey;
        this.docmosisSite = docmosisSite;
        this.templatesPath = templatesPath;
        this.preferDocmosis = preferDocmosis;
    }
    
    /**
     * Returns the Docmosis license key.
     * 
     * @return an Optional containing the license key, or empty if not configured
     */
    public Optional<String> getDocmosisLicenseKey() {
        if (docmosisLicenseKey == null || docmosisLicenseKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(docmosisLicenseKey);
    }
    
    /**
     * Returns the Docmosis site identifier.
     * 
     * @return an Optional containing the site identifier, or empty if not configured
     */
    public Optional<String> getDocmosisSite() {
        if (docmosisSite == null || docmosisSite.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(docmosisSite);
    }
    
    /**
     * Returns the templates path.
     * 
     * @return the path to document templates
     */
    public Path getTemplatesPath() {
        return templatesPath;
    }
    
    /**
     * Returns whether to prefer Docmosis over other document engines when available.
     * 
     * @return true if Docmosis should be preferred, false otherwise
     */
    public boolean isPreferDocmosis() {
        return preferDocmosis;
    }
    
    /**
     * Returns whether Docmosis is configured with a license key.
     * 
     * @return true if Docmosis is configured, false otherwise
     */
    public boolean isDocmosisConfigured() {
        return getDocmosisLicenseKey().isPresent();
    }
    
    /**
     * Builder for creating DocumentConfig instances.
     */
    public static class Builder {
        private String docmosisLicenseKey;
        private String docmosisSite;
        private Path templatesPath = Path.of("templates");
        private boolean preferDocmosis = true;
        
        /**
         * Sets the Docmosis license key.
         * 
         * @param licenseKey the license key
         * @return this builder
         */
        public Builder docmosisLicenseKey(String licenseKey) {
            this.docmosisLicenseKey = licenseKey;
            return this;
        }
        
        /**
         * Sets the Docmosis site identifier.
         * 
         * @param site the site identifier
         * @return this builder
         */
        public Builder docmosisSite(String site) {
            this.docmosisSite = site;
            return this;
        }
        
        /**
         * Sets the templates path.
         * 
         * @param path the path to document templates
         * @return this builder
         */
        public Builder templatesPath(Path path) {
            this.templatesPath = path;
            return this;
        }
        
        /**
         * Sets whether to prefer Docmosis.
         * 
         * @param prefer true to prefer Docmosis, false otherwise
         * @return this builder
         */
        public Builder preferDocmosis(boolean prefer) {
            this.preferDocmosis = prefer;
            return this;
        }
        
        /**
         * Builds and returns a new DocumentConfig.
         * 
         * @return a new DocumentConfig
         */
        public DocumentConfig build() {
            return new DocumentConfig(docmosisLicenseKey, docmosisSite, templatesPath, preferDocmosis);
        }
    }
}