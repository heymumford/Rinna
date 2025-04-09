/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.component;

import org.rinna.pui.component.BoxLayout.Orientation;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.input.KeyHandler;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A container that implements Miller columns for hierarchical data navigation.
 * Miller columns display a hierarchy as a series of adjacent columns, with each column
 * showing the children of the currently selected item in the previous column.
 * This is useful for navigating complex hierarchical structures like work item relationships.
 *
 * @param <T> The type of data displayed in the columns
 */
public class MillerColumnsContainer<T> extends Container {
    
    private final List<Container> columns = new ArrayList<>();
    private final List<List<T>> columnData = new ArrayList<>();
    private final List<Integer> selectedIndices = new ArrayList<>();
    private final Map<Container, Integer> columnWidths = new HashMap<>();
    
    private final int maxColumns;
    private final int columnWidth;
    private final int columnHeight;
    private int activeColumn = 0;
    
    private Function<T, String> itemRenderer;
    private Function<T, List<T>> childrenProvider;
    private Function<T, String> detailProvider;
    
    /**
     * Creates a new MillerColumnsContainer with the specified number of columns.
     *
     * @param id The component ID
     * @param maxColumns The maximum number of columns to display
     * @param columnWidth The width of each column
     * @param columnHeight The height of each column
     */
    public MillerColumnsContainer(String id, int maxColumns, int columnWidth, int columnHeight) {
        super(id);
        this.maxColumns = maxColumns;
        this.columnWidth = columnWidth;
        this.columnHeight = columnHeight;
        
        // Create a horizontal box layout
        BoxLayout layout = new BoxLayout(Orientation.HORIZONTAL, 0);
        setLayout(layout);
        
        // Set the initial size based on the maximum columns
        setSize(new Dimension(columnWidth * maxColumns, columnHeight));
    }
    
    /**
     * Sets the function used to render items as strings in the lists.
     *
     * @param itemRenderer The function that converts items to strings
     * @return this MillerColumnsContainer for method chaining
     */
    public MillerColumnsContainer<T> setItemRenderer(Function<T, String> itemRenderer) {
        this.itemRenderer = itemRenderer;
        
        // Update existing lists
        for (int i = 0; i < columns.size(); i++) {
            Container column = columns.get(i);
            List listComponent = getListFromColumn(column);
            if (listComponent != null) {
                listComponent.setItemRenderer(itemRenderer);
            }
        }
        
        return this;
    }
    
    /**
     * Sets the function used to provide children for a given item.
     *
     * @param childrenProvider The function that returns children for an item
     * @return this MillerColumnsContainer for method chaining
     */
    public MillerColumnsContainer<T> setChildrenProvider(Function<T, List<T>> childrenProvider) {
        this.childrenProvider = childrenProvider;
        return this;
    }
    
    /**
     * Sets the function used to provide detailed information for an item.
     *
     * @param detailProvider The function that returns details for an item
     * @return this MillerColumnsContainer for method chaining
     */
    public MillerColumnsContainer<T> setDetailProvider(Function<T, String> detailProvider) {
        this.detailProvider = detailProvider;
        return this;
    }
    
    /**
     * Sets the root items for the first column.
     *
     * @param rootItems The list of root items
     */
    public void setRootItems(List<T> rootItems) {
        // Clear existing columns
        clear();
        
        // Add the first column with root items
        addColumn(rootItems);
        
        // If we have a selection in the first column, add the second column
        if (selectedIndices.size() > 0 && selectedIndices.get(0) >= 0 && selectedIndices.get(0) < rootItems.size()) {
            T selectedItem = rootItems.get(selectedIndices.get(0));
            updateNextColumn(selectedItem, 0);
        }
    }
    
    /**
     * Clears all columns and data.
     */
    public void clear() {
        columns.forEach(this::removeComponent);
        columns.clear();
        columnData.clear();
        selectedIndices.clear();
        columnWidths.clear();
        activeColumn = 0;
    }
    
    /**
     * Adds a new column with the provided items.
     *
     * @param items The items to display in the column
     * @return The index of the new column
     */
    private int addColumn(List<T> items) {
        // Create a new column container
        Container column = new Container("column-" + columns.size());
        column.setSize(new Dimension(columnWidth, columnHeight));
        
        // Add border style to the column
        Style columnStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        column.setStyle(columnStyle);
        
        // Create vertical layout for the column
        BoxLayout columnLayout = new BoxLayout(Orientation.VERTICAL, 0);
        column.setLayout(columnLayout);
        
        // Create a label for the column header
        Label headerLabel = new Label(getColumnHeader(columns.size()));
        Style headerStyle = new Style()
            .setBold(true);
        headerLabel.setStyle(headerStyle);
        
        // Create a list for the items
        List<T> list = new List<>("list-" + columns.size(), columnHeight - 2);
        list.setItems(items);
        if (itemRenderer != null) {
            list.setItemRenderer(itemRenderer);
        }
        
        // Add selection listener to update the next column when an item is selected
        final int columnIndex = columns.size();
        list.addSelectionListener((l, selectedItem) -> {
            if (selectedItem != null) {
                // Update selected indices
                while (selectedIndices.size() <= columnIndex) {
                    selectedIndices.add(-1);
                }
                selectedIndices.set(columnIndex, list.getSelectedIndex());
                
                // Update the next column
                updateNextColumn(selectedItem, columnIndex);
            }
        });
        
        // Add components to the column
        column.addComponent(headerLabel);
        column.addComponent(list);
        
        // Add the column to the container
        addComponent(column);
        columns.add(column);
        columnData.add(items);
        columnWidths.put(column, columnWidth);
        
        // Select the first item by default
        if (!items.isEmpty()) {
            list.setSelectedIndex(0);
            selectedIndices.add(0);
        } else {
            selectedIndices.add(-1);
        }
        
        return columns.size() - 1;
    }
    
    /**
     * Updates the next column based on the selected item in the current column.
     *
     * @param selectedItem The selected item
     * @param columnIndex The index of the column containing the selected item
     */
    private void updateNextColumn(T selectedItem, int columnIndex) {
        // Get children of the selected item
        List<T> children = childrenProvider != null ? childrenProvider.apply(selectedItem) : new ArrayList<>();
        
        // If this is the last column or we have no children, add a details column
        if (columnIndex == maxColumns - 2 || children.isEmpty()) {
            updateDetailsColumn(selectedItem, columnIndex + 1);
            return;
        }
        
        // Remove all columns after the current one
        while (columns.size() > columnIndex + 1) {
            int lastIndex = columns.size() - 1;
            removeComponent(columns.get(lastIndex));
            columns.remove(lastIndex);
            columnData.remove(lastIndex);
            if (selectedIndices.size() > lastIndex) {
                selectedIndices.remove(lastIndex);
            }
        }
        
        // Add the new column with children
        if (columnIndex + 1 < columns.size()) {
            // Update existing column
            List list = getListFromColumn(columns.get(columnIndex + 1));
            if (list != null) {
                list.setItems(children);
                if (!children.isEmpty()) {
                    list.setSelectedIndex(0);
                    if (selectedIndices.size() > columnIndex + 1) {
                        selectedIndices.set(columnIndex + 1, 0);
                    } else {
                        selectedIndices.add(0);
                    }
                }
                columnData.set(columnIndex + 1, children);
            }
        } else {
            // Add new column
            addColumn(children);
        }
    }
    
    /**
     * Updates the details column with information about the selected item.
     *
     * @param selectedItem The selected item
     * @param columnIndex The index of the details column
     */
    private void updateDetailsColumn(T selectedItem, int columnIndex) {
        // Remove all columns after the current one
        while (columns.size() > columnIndex) {
            int lastIndex = columns.size() - 1;
            removeComponent(columns.get(lastIndex));
            columns.remove(lastIndex);
            columnData.remove(lastIndex);
            if (selectedIndices.size() > lastIndex) {
                selectedIndices.remove(lastIndex);
            }
        }
        
        // Create details column
        Container detailsColumn = new Container("details-column");
        detailsColumn.setSize(new Dimension(columnWidth, columnHeight));
        
        // Add border style to the column
        Style columnStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        detailsColumn.setStyle(columnStyle);
        
        // Create vertical layout for the column
        BoxLayout columnLayout = new BoxLayout(Orientation.VERTICAL, 0);
        detailsColumn.setLayout(columnLayout);
        
        // Create a label for the column header
        Label headerLabel = new Label("Details");
        Style headerStyle = new Style()
            .setBold(true);
        headerLabel.setStyle(headerStyle);
        
        // Create a label for the details
        String detailsText = detailProvider != null ? detailProvider.apply(selectedItem) : selectedItem.toString();
        Label detailsLabel = new Label(detailsText);
        detailsLabel.setWordWrap(true);
        
        // Add components to the column
        detailsColumn.addComponent(headerLabel);
        detailsColumn.addComponent(detailsLabel);
        
        // Add the column to the container
        addComponent(detailsColumn);
        columns.add(detailsColumn);
        columnData.add(null); // No data for details column
        columnWidths.put(detailsColumn, columnWidth);
    }
    
    /**
     * Gets the list component from a column.
     *
     * @param column The column container
     * @return The list component or null if not found
     */
    private List<T> getListFromColumn(Container column) {
        for (Component component : column.getComponents()) {
            if (component instanceof List) {
                return (List<T>) component;
            }
        }
        return null;
    }
    
    /**
     * Gets the header text for a column.
     *
     * @param columnIndex The index of the column
     * @return The header text
     */
    private String getColumnHeader(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Items";
            case 1:
                return "Related";
            case 2:
                return "Children";
            case 3:
                return "Details";
            default:
                return "Level " + (columnIndex + 1);
        }
    }
    
    /**
     * Handles key events for navigation between columns.
     *
     * @param keyCode The key code
     * @param keyChar The character
     * @param modifiers Key modifiers
     * @return true if the key was handled, false otherwise
     */
    @Override
    public boolean handleKeyEvent(int keyCode, char keyChar, int modifiers) {
        // Handle tab key to switch between columns
        if (keyCode == KeyHandler.KEY_TAB) {
            if ((modifiers & KeyHandler.SHIFT_MASK) != 0) {
                // Shift+Tab = previous column
                if (activeColumn > 0) {
                    activeColumn--;
                    focusColumn(activeColumn);
                    return true;
                }
            } else {
                // Tab = next column
                if (activeColumn < columns.size() - 1) {
                    activeColumn++;
                    focusColumn(activeColumn);
                    return true;
                }
            }
        }
        
        // Pass other keys to the active column
        if (activeColumn >= 0 && activeColumn < columns.size()) {
            return columns.get(activeColumn).handleKeyEvent(keyCode, keyChar, modifiers);
        }
        
        return super.handleKeyEvent(keyCode, keyChar, modifiers);
    }
    
    /**
     * Sets focus to a specific column.
     *
     * @param columnIndex The index of the column to focus
     */
    private void focusColumn(int columnIndex) {
        if (columnIndex >= 0 && columnIndex < columns.size()) {
            Container column = columns.get(columnIndex);
            
            // Find the list in the column and focus it
            List<T> list = getListFromColumn(column);
            if (list != null) {
                requestFocus();
                list.requestFocus();
            } else {
                // If no list (e.g., in details column), just focus the column
                column.requestFocus();
            }
        }
    }
    
    /**
     * Gets currently selected items across all columns.
     *
     * @return List of selected items, one per column
     */
    public List<T> getSelectedItems() {
        List<T> selectedItems = new ArrayList<>();
        
        for (int i = 0; i < columnData.size(); i++) {
            if (i < selectedIndices.size() && selectedIndices.get(i) >= 0) {
                List<T> data = columnData.get(i);
                if (data != null && selectedIndices.get(i) < data.size()) {
                    selectedItems.add(data.get(selectedIndices.get(i)));
                }
            }
        }
        
        return selectedItems;
    }
    
    /**
     * Gets the item selected in the first column (root item).
     *
     * @return The selected root item or null if none
     */
    public T getSelectedRootItem() {
        if (!columnData.isEmpty() && !selectedIndices.isEmpty() && selectedIndices.get(0) >= 0) {
            List<T> rootItems = columnData.get(0);
            if (rootItems != null && selectedIndices.get(0) < rootItems.size()) {
                return rootItems.get(selectedIndices.get(0));
            }
        }
        return null;
    }
    
    /**
     * Gets the item selected in the last active column.
     *
     * @return The last selected item or null if none
     */
    public T getSelectedItem() {
        List<T> selectedItems = getSelectedItems();
        return selectedItems.isEmpty() ? null : selectedItems.get(selectedItems.size() - 1);
    }
}