package org.rinna.cli.service;

public class ServiceManager {
    private static ServiceManager instance;
    
    public static ServiceManager getInstance() {
        if (instance == null) {
            instance = new ServiceManager();
        }
        return instance;
    }
}