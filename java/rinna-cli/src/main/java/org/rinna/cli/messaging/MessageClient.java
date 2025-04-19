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

/**
 * Client interface for interacting with the messaging API.
 */
public interface MessageClient {
    
    /**
     * Gets the server URL.
     *
     * @return the server URL
     */
    String getServerUrl();
    
    /**
     * Sets the server URL.
     *
     * @param url the server URL
     */
    void setServerUrl(String url);
    
    /**
     * Sets the authentication token for API requests.
     *
     * @param token the authentication token
     */
    void setAuthToken(String token);
    
    /**
     * Gets the authentication token.
     *
     * @return the authentication token
     */
    String getAuthToken();
    
    /**
     * Checks if the client is connected to the server.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();
    
    /**
     * Connects to the messaging server.
     *
     * @return true if connection successful, false otherwise
     */
    boolean connect();
    
    /**
     * Disconnects from the messaging server.
     */
    void disconnect();
}