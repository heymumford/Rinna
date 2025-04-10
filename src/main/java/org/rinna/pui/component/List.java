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

import java.util.ArrayList;
import java.util.Collection;

import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.input.KeyHandler;
import org.rinna.pui.render.ComponentRenderer;
import org.rinna.pui.render.ListRenderer;
import org.rinna.pui.style.Style;

/**
 * A component that displays a list of items and allows selection.
 */
public class List<T> implements Component {
    
    /**
     * Interface for selection change event listeners.
     */
    public interface SelectionListener<T> {
        /**
         * Called when the selection in the list changes.
         * 
         * @param list the list where the selection changed
         * @param selected the newly selected item, or null if no item is selected
         */
        void onSelectionChange(List<T> list, T selected);
    }
    
    /**
     * Interface for converting list items to display strings.
     */
    public interface ItemRenderer<T> {
        /**
         * Converts an item to a display string.
         * 
         * @param item the item to convert
         * @return the display string
         */
        String getDisplayString(T item);
    }
    
    private String id;
    private java.util.List<T> items;
    private Point position;
    private Dimension size;
    private Style style;
    private Container parent;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused = false;
    private ListRenderer<T> renderer;
    private Object layoutConstraints;
    private int selectedIndex = -1;
    private int topIndex = 0;
    private int visibleItems;
    private ItemRenderer<T> itemRenderer;
    private java.util.List<SelectionListener<T>> selectionListeners;
    
    /**
     * Creates a new list with the specified ID and visible items.
     * 
     * @param id the list ID
     * @param visibleItems the number of visible items
     */
    public List(String id, int visibleItems) {
        this.id = id;
        this.items = new ArrayList<>();
        this.visibleItems = Math.max(1, visibleItems);
        this.renderer = new ListRenderer<>();
        this.selectionListeners = new ArrayList<>();
        this.itemRenderer = Object::toString;
        updateSize();
    }
    
    /**
     * Creates a new list with the specified visible items and a default ID.
     * 
     * @param visibleItems the number of visible items
     */
    public List(int visibleItems) {
        this("list-" + System.currentTimeMillis(), visibleItems);
    }
    
    /**
     * Creates a new list with a default ID and 10 visible items.
     */
    public List() {
        this(10);
    }
    
    /**
     * Gets all items in the list.
     * 
     * @return the items
     */
    public java.util.List<T> getItems() {
        return new ArrayList<>(items);
    }
    
    /**
     * Sets the items in the list.
     * 
     * @param items the new items
     */
    public void setItems(Collection<T> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        
        // Adjust selection and top index
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : this.items.size() - 1;
        }
        
        if (topIndex > 0 && topIndex >= this.items.size()) {
            topIndex = Math.max(0, this.items.size() - visibleItems);
        }
        
        // Notify selection change
        notifySelectionChange();
    }
    
    /**
     * Adds an item to the list.
     * 
     * @param item the item to add
     */
    public void addItem(T item) {
        if (item != null) {
            items.add(item);
        }
    }
    
    /**
     * Removes an item from the list.
     * 
     * @param item the item to remove
     * @return true if the item was removed, false otherwise
     */
    public boolean removeItem(T item) {
        int index = items.indexOf(item);
        if (index >= 0) {
            items.remove(index);
            
            // Adjust selection and top index
            if (index == selectedIndex) {
                selectedIndex = -1;
                notifySelectionChange();
            } else if (index < selectedIndex) {
                selectedIndex--;
            }
            
            if (index < topIndex) {
                topIndex = Math.max(0, topIndex - 1);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the currently selected item.
     * 
     * @return the selected item, or null if no item is selected
     */
    public T getSelectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }
    
    /**
     * Sets the selected item.
     * 
     * @param item the item to select
     * @return true if the item was found and selected, false otherwise
     */
    public boolean setSelectedItem(T item) {
        int index = items.indexOf(item);
        if (index >= 0) {
            setSelectedIndex(index);
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the selected index.
     * 
     * @return the selected index, or -1 if no item is selected
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Sets the selected index.
     * 
     * @param index the index to select
     */
    public void setSelectedIndex(int index) {
        if (index < -1 || index >= items.size()) {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }
        
        if (index != selectedIndex) {
            selectedIndex = index;
            
            // Ensure the selected item is visible
            if (selectedIndex >= 0) {
                if (selectedIndex < topIndex) {
                    topIndex = selectedIndex;
                } else if (selectedIndex >= topIndex + visibleItems) {
                    topIndex = selectedIndex - visibleItems + 1;
                }
            }
            
            notifySelectionChange();
        }
    }
    
    /**
     * Gets the top index (first visible item).
     * 
     * @return the top index
     */
    public int getTopIndex() {
        return topIndex;
    }
    
    /**
     * Sets the top index (first visible item).
     * 
     * @param index the new top index
     */
    public void setTopIndex(int index) {
        if (index < 0 || (index > 0 && index >= items.size())) {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }
        
        topIndex = index;
    }
    
    /**
     * Gets the number of visible items.
     * 
     * @return the number of visible items
     */
    public int getVisibleItems() {
        return visibleItems;
    }
    
    /**
     * Sets the number of visible items.
     * 
     * @param visibleItems the new number of visible items
     */
    public void setVisibleItems(int visibleItems) {
        this.visibleItems = Math.max(1, visibleItems);
        updateSize();
    }
    
    /**
     * Gets the item renderer.
     * 
     * @return the item renderer
     */
    public ItemRenderer<T> getItemRenderer() {
        return itemRenderer;
    }
    
    /**
     * Sets the item renderer.
     * 
     * @param itemRenderer the new item renderer
     */
    public void setItemRenderer(ItemRenderer<T> itemRenderer) {
        this.itemRenderer = itemRenderer != null ? itemRenderer : Object::toString;
    }
    
    /**
     * Adds a selection listener to this list.
     * 
     * @param listener the listener to add
     */
    public void addSelectionListener(SelectionListener<T> listener) {
        selectionListeners.add(listener);
    }
    
    /**
     * Removes a selection listener from this list.
     * 
     * @param listener the listener to remove
     * @return true if the listener was removed, false otherwise
     */
    public boolean removeSelectionListener(SelectionListener<T> listener) {
        return selectionListeners.remove(listener);
    }
    
    /**
     * Notifies all selection listeners of a selection change.
     */
    private void notifySelectionChange() {
        T selectedItem = getSelectedItem();
        for (SelectionListener<T> listener : selectionListeners) {
            listener.onSelectionChange(this, selectedItem);
        }
    }
    
    /**
     * Updates the size of the list based on the number of visible items.
     */
    private void updateSize() {
        setSize(new Dimension(20, visibleItems + 2)); // +2 for the border
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public Point getPosition() {
        return position;
    }
    
    @Override
    public void setPosition(Point position) {
        this.position = position;
    }
    
    @Override
    public Dimension getSize() {
        return size;
    }
    
    @Override
    public void setSize(Dimension size) {
        this.size = size;
    }
    
    @Override
    public Style getStyle() {
        return style;
    }
    
    @Override
    public void setStyle(Style style) {
        this.style = style;
    }
    
    @Override
    public Container getParent() {
        return parent;
    }
    
    @Override
    public void setParent(Container parent) {
        this.parent = parent;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isFocused() {
        return focused;
    }
    
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    
    @Override
    public boolean handleKey(int key) {
        if (!isEnabled() || !isFocused() || items.isEmpty()) {
            return false;
        }
        
        switch (key) {
            case KeyHandler.KEY_UP:
                // Select previous item
                if (selectedIndex > 0) {
                    setSelectedIndex(selectedIndex - 1);
                    return true;
                }
                break;
                
            case KeyHandler.KEY_DOWN:
                // Select next item
                if (selectedIndex < items.size() - 1) {
                    setSelectedIndex(selectedIndex + 1);
                    return true;
                }
                break;
                
            case KeyHandler.KEY_PGUP:
                // Select item a page up
                if (selectedIndex > 0) {
                    int newIndex = Math.max(0, selectedIndex - visibleItems);
                    setSelectedIndex(newIndex);
                    return true;
                }
                break;
                
            case KeyHandler.KEY_PGDN:
                // Select item a page down
                if (selectedIndex < items.size() - 1) {
                    int newIndex = Math.min(items.size() - 1, selectedIndex + visibleItems);
                    setSelectedIndex(newIndex);
                    return true;
                }
                break;
                
            case KeyHandler.KEY_HOME:
                // Select first item
                if (items.size() > 0 && selectedIndex != 0) {
                    setSelectedIndex(0);
                    return true;
                }
                break;
                
            case KeyHandler.KEY_END:
                // Select last item
                if (items.size() > 0 && selectedIndex != items.size() - 1) {
                    setSelectedIndex(items.size() - 1);
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    @Override
    public ComponentRenderer getRenderer() {
        return renderer;
    }
    
    @Override
    public void update(long deltaMs) {
        // Nothing to update
    }
    
    @Override
    public Object getLayoutConstraints() {
        return layoutConstraints;
    }
    
    @Override
    public void setLayoutConstraints(Object constraints) {
        this.layoutConstraints = constraints;
    }
}