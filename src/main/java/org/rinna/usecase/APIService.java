/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import org.rinna.domain.APIToken;
import org.rinna.domain.Project;
import org.rinna.domain.WorkItem;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for API-related operations.
 */
public interface APIService {
    
    /**
     * Authenticates an API token and returns the associated project.
     *
     * @param token the token to authenticate
     * @return the associated project if the token is valid
     * @throws AuthenticationException if the token is invalid
     */
    Project authenticate(String token) throws AuthenticationException;
    
    /**
     * Creates a work item from a JSON payload.
     *
     * @param project the project to create the work item in
     * @param payload the JSON payload as a map
     * @return the created work item
     * @throws ValidationException if the payload is invalid
     */
    WorkItem createWorkItemFromPayload(Project project, Map<String, Object> payload) throws ValidationException;
    
    /**
     * Creates an API token for a project.
     *
     * @param projectId the project ID
     * @param description the token description
     * @param validDays the number of days the token should be valid
     * @return the created token
     */
    APIToken createToken(UUID projectId, String description, int validDays);
    
    /**
     * Revokes an API token.
     *
     * @param token the token to revoke
     */
    void revokeToken(String token);
    
    /**
     * Validates a webhook request.
     *
     * @param projectKey the project key
     * @param source the webhook source
     * @param signature the webhook signature
     * @param payload the webhook payload
     * @return the validated project
     * @throws AuthenticationException if the signature is invalid
     */
    Project validateWebhook(String projectKey, String source, String signature, String payload) throws AuthenticationException;
    
    /**
     * Creates a work item from a GitHub pull request webhook.
     *
     * @param project the project to create the work item in
     * @param payload the webhook payload as a map
     * @return the created work item
     */
    WorkItem createWorkItemFromGitHubPullRequest(Project project, Map<String, Object> payload);
    
    /**
     * Creates a work item from a GitHub workflow run webhook.
     *
     * @param project the project to create the work item in
     * @param payload the webhook payload as a map
     * @return the created work item
     */
    WorkItem createWorkItemFromGitHubWorkflowRun(Project project, Map<String, Object> payload);
    
    /**
     * Exception thrown when authentication fails.
     */
    class AuthenticationException extends Exception {
        private final int statusCode;
        
        /**
         * Creates a new AuthenticationException.
         *
         * @param message the error message
         * @param statusCode the HTTP status code
         */
        public AuthenticationException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        /**
         * Returns the HTTP status code.
         *
         * @return the status code
         */
        public int getStatusCode() {
            return statusCode;
        }
    }
    
    /**
     * Exception thrown when validation fails.
     */
    class ValidationException extends Exception {
        private final int statusCode;
        
        /**
         * Creates a new ValidationException.
         *
         * @param message the error message
         * @param statusCode the HTTP status code
         */
        public ValidationException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        /**
         * Returns the HTTP status code.
         *
         * @return the status code
         */
        public int getStatusCode() {
            return statusCode;
        }
    }
}