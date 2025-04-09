/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.messaging;

import java.time.Instant;

/**
 * Represents a user's authenticated session.
 */
public class UserSession {
    private final String username;
    private final String authToken;
    private final Instant createdAt;
    private Instant lastAccessed;

    /**
     * Constructs a new UserSession.
     *
     * @param username  the username
     * @param authToken the authentication token
     */
    public UserSession(String username, String authToken) {
        this.username = username;
        this.authToken = authToken;
        this.createdAt = Instant.now();
        this.lastAccessed = Instant.now();
    }

    public String getUsername() {
        return username;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessed() {
        return lastAccessed;
    }

    /**
     * Updates the last accessed timestamp to the current time.
     */
    public void updateLastAccessed() {
        this.lastAccessed = Instant.now();
    }

    /**
     * Checks if the session is expired.
     *
     * @param maxAgeSeconds the maximum age of the session in seconds
     * @return true if the session is expired, false otherwise
     */
    public boolean isExpired(long maxAgeSeconds) {
        Instant expirationTime = lastAccessed.plusSeconds(maxAgeSeconds);
        return Instant.now().isAfter(expirationTime);
    }
}