/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.rinna.cli.messaging.MessageClient;

/**
 * Mock implementation of MessageClient for testing.
 */
public class MockMessageClient implements MessageClient {
    
    private String serverUrl;
    private String authToken;
    private boolean connected;
    
    /**
     * Constructs a new MockMessageClient.
     */
    public MockMessageClient() {
        this.serverUrl = "http://localhost:8080";
        this.authToken = null;
        this.connected = false;
    }
    
    @Override
    public String getServerUrl() {
        return serverUrl;
    }
    
    @Override
    public void setServerUrl(String url) {
        this.serverUrl = url;
    }
    
    @Override
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    @Override
    public String getAuthToken() {
        return authToken;
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public boolean connect() {
        connected = serverUrl != null && serverUrl.startsWith("http");
        return connected;
    }
    
    @Override
    public void disconnect() {
        connected = false;
    }
}