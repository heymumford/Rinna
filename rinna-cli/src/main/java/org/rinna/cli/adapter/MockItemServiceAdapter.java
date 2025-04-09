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

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.MockItemService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter for the MockItemService to provide ItemService compatible operations.
 */
public class MockItemServiceAdapter {
    private final MockItemService mockItemService;
    
    /**
     * Creates a new adapter for a MockItemService.
     *
     * @param mockItemService the mock item service to adapt
     */
    public MockItemServiceAdapter(MockItemService mockItemService) {
        this.mockItemService = mockItemService;
    }
    
    /**
     * Gets all work items adapted for statistics use.
     *
     * @return the list of adapted work items
     */
    public List<StatisticItemAdapter> getAllItems() {
        return mockItemService.getAllItems().stream()
            .map(StatisticItemAdapter::new)
            .collect(Collectors.toList());
    }
}