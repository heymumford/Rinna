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

import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.input.KeyHandler;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A text box that provides auto-completion suggestions as the user types.
 * The suggestions are displayed in a dropdown list below the text box.
 */
public class AutoCompleteTextBox extends Container {
    
    private TextBox textBox;
    private List<String> suggestions = new ArrayList<>();
    private List<String> filteredSuggestions = new ArrayList<>();
    private Container suggestionsContainer;
    private List<String> suggestionList;
    private boolean showingSuggestions = false;
    private int maxSuggestions = 8;
    private int minCharacters = 1;
    private Function<String, List<String>> suggestionProvider;
    private Consumer<String> selectionHandler;
    private Comparator<String> suggestionComparator;
    private int selectedSuggestionIndex = -1;
    
    /**
     * Creates a new auto-complete text box with the specified ID and width.
     *
     * @param id The component ID
     * @param width The width of the text box
     */
    public AutoCompleteTextBox(String id, int width) {
        super(id);
        
        // Create the text box
        textBox = new TextBox(width);
        
        // Set up text changed listener to show/update suggestions
        textBox.addTextChangedListener(this::handleTextChanged);
        
        // Create the suggestions container (initially hidden)
        suggestionsContainer = new Container("suggestions");
        Style suggestionsStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE)
            .setBackground(Color.BLUE)
            .setForeground(Color.WHITE);
        suggestionsContainer.setStyle(suggestionsStyle);
        
        // Calculate the size of the component
        int height = 1; // Text box height
        setSize(new Dimension(width + 2, height)); // +2 for potential border
        
        // Set up layout
        BoxLayout layout = new BoxLayout(BoxLayout.Orientation.VERTICAL, 0);
        setLayout(layout);
        
        // Add the text box to the container
        addComponent(textBox);
        
        // Set up default suggestion comparator (alphabetical)
        suggestionComparator = String::compareToIgnoreCase;
    }
    
    /**
     * Sets the available suggestions.
     *
     * @param suggestions The suggestions to display
     * @return This component for method chaining
     */
    public AutoCompleteTextBox setSuggestions(List<String> suggestions) {
        if (suggestions != null) {
            this.suggestions = new ArrayList<>(suggestions);
        } else {
            this.suggestions = new ArrayList<>();
        }
        return this;
    }
    
    /**
     * Sets the provider function for dynamic suggestions based on input.
     *
     * @param provider A function that takes the current text and returns matching suggestions
     * @return This component for method chaining
     */
    public AutoCompleteTextBox setSuggestionProvider(Function<String, List<String>> provider) {
        this.suggestionProvider = provider;
        return this;
    }
    
    /**
     * Sets the handler for when a suggestion is selected.
     *
     * @param handler The handler to call when a suggestion is selected
     * @return This component for method chaining
     */
    public AutoCompleteTextBox setSelectionHandler(Consumer<String> handler) {
        this.selectionHandler = handler;
        return this;
    }
    
    /**
     * Sets the maximum number of suggestions to display.
     *
     * @param maxSuggestions The maximum number of suggestions
     * @return This component for method chaining
     */
    public AutoCompleteTextBox setMaxSuggestions(int maxSuggestions) {
        this.maxSuggestions = Math.max(1, maxSuggestions);
        return this;
    }
    
    /**
     * Sets the minimum number of characters required before showing suggestions.
     *
     * @param minCharacters The minimum number of characters
     * @return This component for method chaining
     */
    public AutoCompleteTextBox setMinCharacters(int minCharacters) {
        this.minCharacters = Math.max(0, minCharacters);
        return this;
    }
    
    /**
     * Sets the comparator for sorting suggestions.
     *
     * @param comparator The comparator to use
     * @return This component for method chaining
     */
    public AutoCompleteTextBox setSuggestionComparator(Comparator<String> comparator) {
        this.suggestionComparator = comparator;
        return this;
    }
    
    /**
     * Gets the current text in the text box.
     *
     * @return The current text
     */
    public String getText() {
        return textBox.getText();
    }
    
    /**
     * Sets the text in the text box.
     *
     * @param text The text to set
     */
    public void setText(String text) {
        textBox.setText(text);
    }
    
    /**
     * Adds a text changed listener.
     *
     * @param listener The listener to add
     * @return This component for method chaining
     */
    public AutoCompleteTextBox addTextChangedListener(Consumer<String> listener) {
        textBox.addTextChangedListener(listener);
        return this;
    }
    
    /**
     * Handles changes to the text box content.
     *
     * @param text The current text
     */
    private void handleTextChanged(String text) {
        if (text.length() >= minCharacters) {
            updateSuggestions(text);
        } else {
            hideSuggestions();
        }
    }
    
    /**
     * Updates the suggestions based on the current text.
     *
     * @param text The current text
     */
    private void updateSuggestions(String text) {
        // Get suggestions either from provider or static list
        if (suggestionProvider != null) {
            filteredSuggestions = suggestionProvider.apply(text);
        } else {
            // Filter the static suggestion list
            filteredSuggestions = filterSuggestions(text);
        }
        
        // Sort the suggestions
        if (suggestionComparator != null) {
            filteredSuggestions.sort(suggestionComparator);
        }
        
        // Limit the number of suggestions
        if (filteredSuggestions.size() > maxSuggestions) {
            filteredSuggestions = filteredSuggestions.subList(0, maxSuggestions);
        }
        
        // Display suggestions if there are any
        if (!filteredSuggestions.isEmpty()) {
            showSuggestions();
        } else {
            hideSuggestions();
        }
    }
    
    /**
     * Filters the suggestions based on the current text.
     *
     * @param text The current text
     * @return The filtered suggestions
     */
    private List<String> filterSuggestions(String text) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        
        String lowerText = text.toLowerCase();
        return suggestions.stream()
            .filter(s -> s.toLowerCase().contains(lowerText))
            .collect(Collectors.toList());
    }
    
    /**
     * Shows the suggestions dropdown.
     */
    private void showSuggestions() {
        // Remove current container if it exists
        if (getComponentCount() > 1) {
            removeComponent(getComponent(1));
        }
        
        // Reset selection
        selectedSuggestionIndex = -1;
        
        // Create and add the suggestions
        suggestionsContainer = new Container("suggestions");
        suggestionsContainer.setSize(new Dimension(textBox.getWidth(), Math.min(filteredSuggestions.size(), maxSuggestions)));
        
        Style suggestionsStyle = new Style()
            .setBorderStyle(BorderStyle.SINGLE)
            .setBackground(Color.BLACK);
        suggestionsContainer.setStyle(suggestionsStyle);
        
        // Set up the layout
        BoxLayout suggestionLayout = new BoxLayout(BoxLayout.Orientation.VERTICAL, 0);
        suggestionsContainer.setLayout(suggestionLayout);
        
        // Add the suggestions as labels
        for (int i = 0; i < filteredSuggestions.size(); i++) {
            String suggestion = filteredSuggestions.get(i);
            Label suggestionLabel = new Label(suggestion);
            suggestionsContainer.addComponent(suggestionLabel);
        }
        
        // Add the container
        addComponent(suggestionsContainer);
        
        // Update component size to include suggestions
        setSize(new Dimension(getWidth(), 1 + suggestionsContainer.getHeight()));
        
        showingSuggestions = true;
    }
    
    /**
     * Hides the suggestions dropdown.
     */
    private void hideSuggestions() {
        if (getComponentCount() > 1) {
            removeComponent(getComponent(1));
            
            // Reset component size
            setSize(new Dimension(getWidth(), 1));
        }
        
        showingSuggestions = false;
        selectedSuggestionIndex = -1;
    }
    
    /**
     * Handles key events for navigation and selection.
     *
     * @param keyCode The key code
     * @param keyChar The key character
     * @param modifiers Key modifiers
     * @return true if the key was handled, false otherwise
     */
    @Override
    public boolean handleKeyEvent(int keyCode, char keyChar, int modifiers) {
        // Let the text box handle the event first
        if (textBox.handleKeyEvent(keyCode, keyChar, modifiers)) {
            return true;
        }
        
        // Handle navigation in suggestions
        if (showingSuggestions && !filteredSuggestions.isEmpty()) {
            if (keyCode == KeyHandler.KEY_DOWN) {
                // Navigate down
                selectedSuggestionIndex = (selectedSuggestionIndex + 1) % filteredSuggestions.size();
                highlightSelectedSuggestion();
                return true;
            } else if (keyCode == KeyHandler.KEY_UP) {
                // Navigate up
                selectedSuggestionIndex = (selectedSuggestionIndex - 1 + filteredSuggestions.size()) % filteredSuggestions.size();
                highlightSelectedSuggestion();
                return true;
            } else if (keyCode == KeyHandler.KEY_ENTER) {
                // Select the highlighted suggestion
                if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < filteredSuggestions.size()) {
                    selectSuggestion(selectedSuggestionIndex);
                    return true;
                }
            } else if (keyCode == KeyHandler.KEY_ESCAPE) {
                // Hide suggestions
                hideSuggestions();
                return true;
            }
        }
        
        return super.handleKeyEvent(keyCode, keyChar, modifiers);
    }
    
    /**
     * Highlights the currently selected suggestion.
     */
    private void highlightSelectedSuggestion() {
        for (int i = 0; i < suggestionsContainer.getComponentCount(); i++) {
            Component comp = suggestionsContainer.getComponent(i);
            if (comp instanceof Label) {
                Label label = (Label) comp;
                if (i == selectedSuggestionIndex) {
                    // Highlight
                    Style highlightStyle = new Style()
                        .setBackground(Color.BLUE)
                        .setForeground(Color.WHITE)
                        .setBold(true);
                    label.setStyle(highlightStyle);
                } else {
                    // Normal
                    Style normalStyle = new Style()
                        .setBackground(Color.BLACK)
                        .setForeground(Color.WHITE);
                    label.setStyle(normalStyle);
                }
            }
        }
    }
    
    /**
     * Selects a suggestion by index.
     *
     * @param index The index of the suggestion to select
     */
    private void selectSuggestion(int index) {
        if (index >= 0 && index < filteredSuggestions.size()) {
            String suggestion = filteredSuggestions.get(index);
            
            // Set the text box text
            textBox.setText(suggestion);
            
            // Hide suggestions
            hideSuggestions();
            
            // Call selection handler if set
            if (selectionHandler != null) {
                selectionHandler.accept(suggestion);
            }
        }
    }
}