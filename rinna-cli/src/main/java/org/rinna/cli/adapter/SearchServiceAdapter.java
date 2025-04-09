/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.adapter;

import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkItemType;
import org.rinna.cli.domain.model.DomainWorkflowState;
import org.rinna.cli.domain.service.SearchService;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.StateMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter for the SearchService that bridges between the domain SearchService and
 * the CLI MockSearchService. This adapter implements the domain SearchService interface
 * while delegating to the CLI MockSearchService implementation.
 */
public class SearchServiceAdapter implements SearchService {
    
    private final MockSearchService mockSearchService;
    
    /**
     * Constructs a new SearchServiceAdapter with the specified mock search service.
     *
     * @param mockSearchService the mock search service to delegate to
     */
    public SearchServiceAdapter(MockSearchService mockSearchService) {
        this.mockSearchService = mockSearchService;
        
        // Initialize the mock service if needed
        this.mockSearchService.initialize();
    }
    
    @Override
    public List<DomainWorkItem> searchByText(String text) {
        List<WorkItem> cliItems = mockSearchService.searchByText(text);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> searchByTextAndState(String text, DomainWorkflowState state) {
        WorkflowState cliState = StateMapper.fromDomainState(state);
        List<WorkItem> cliItems = mockSearchService.searchByTextAndState(text, cliState);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> searchByTextAndType(String text, DomainWorkItemType type) {
        WorkItemType cliType = StateMapper.fromDomainType(type);
        List<WorkItem> cliItems = mockSearchService.searchByTextAndType(text, cliType);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> searchByTextAndPriority(String text, DomainPriority priority) {
        Priority cliPriority = StateMapper.fromDomainPriority(priority);
        List<WorkItem> cliItems = mockSearchService.searchByTextAndPriority(text, cliPriority);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> findText(String text) {
        List<WorkItem> cliItems = mockSearchService.findText(text);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> findText(String text, boolean caseSensitive) {
        List<WorkItem> cliItems = mockSearchService.findText(text, caseSensitive);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> findPattern(String pattern) {
        List<WorkItem> cliItems = mockSearchService.findPattern(pattern);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> findPattern(String pattern, boolean caseSensitive) {
        List<WorkItem> cliItems = mockSearchService.findPattern(pattern, caseSensitive);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> findItemsByText(String text) {
        List<WorkItem> cliItems = mockSearchService.findItemsByText(text);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> findItemsByMetadata(Map<String, String> metadata) {
        List<WorkItem> cliItems = mockSearchService.findItemsByMetadata(metadata);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> findWorkItems(Map<String, String> criteria, int limit) {
        List<WorkItem> cliItems = mockSearchService.findWorkItems(criteria, limit);
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
}