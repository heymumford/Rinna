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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.rinna.cli.model.WorkflowState;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.input.KeyHandler;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;

/**
 * A component that visualizes workflow states and transitions.
 * It displays the current state and available transitions, allowing
 * the user to navigate between states using keyboard shortcuts.
 */
public class WorkflowStateView extends Container {
    
    private WorkflowState currentState;
    private List<WorkflowState> availableTransitions = new ArrayList<>();
    private final Map<Character, WorkflowState> keyMappings = new HashMap<>();
    private final Map<WorkflowState, Point> statePositions = new HashMap<>();
    private Consumer<WorkflowState> transitionHandler;
    private Label statusLabel;
    private Map<WorkflowState, Label> stateLabels = new HashMap<>();
    
    /**
     * Creates a new workflow state view with a specific size.
     *
     * @param id The component ID
     * @param width The component width
     * @param height The component height
     */
    public WorkflowStateView(String id, int width, int height) {
        super(id);
        setSize(new Dimension(width, height));
        
        // Create a border around the component
        Style style = new Style()
            .setBorderStyle(BorderStyle.SINGLE);
        setStyle(style);
        
        // Initialize the component with a vertical box layout
        setLayout(new BoxLayout(BoxLayout.Orientation.VERTICAL, 1));
        
        // Create status label for messages
        statusLabel = new Label("Select a transition using the highlighted key");
        statusLabel.setAlignment(Label.Alignment.CENTER);
        addComponent(statusLabel);
        
        // Set up the standard workflow visualization layout
        setupWorkflowLayout();
    }
    
    /**
     * Sets the current state and available transitions.
     *
     * @param currentState The current workflow state
     * @param availableTransitions List of available transitions
     */
    public void setStateAndTransitions(WorkflowState currentState, List<WorkflowState> availableTransitions) {
        this.currentState = currentState;
        this.availableTransitions = availableTransitions;
        
        // Update visual state
        updateStateVisualization();
    }
    
    /**
     * Sets the handler to be called when a transition is selected.
     *
     * @param handler Consumer that handles the selected transition
     * @return This component for method chaining
     */
    public WorkflowStateView setTransitionHandler(Consumer<WorkflowState> handler) {
        this.transitionHandler = handler;
        return this;
    }
    
    /**
     * Sets up the standard workflow visualization layout.
     */
    private void setupWorkflowLayout() {
        // Create a container for the workflow diagram
        Container diagramContainer = new Container("workflow-diagram");
        diagramContainer.setSize(new Dimension(getWidth() - 2, getHeight() - 5));
        
        // Initialize the state positions
        initializeStatePositions(diagramContainer.getWidth(), diagramContainer.getHeight());
        
        // Create labels for all workflow states
        for (WorkflowState state : WorkflowState.values()) {
            Label stateLabel = new Label(formatStateLabel(state, ' '));
            
            // Set position based on the state map
            Point position = statePositions.get(state);
            if (position != null) {
                stateLabel.setPosition(position);
            }
            
            // Add to the diagram container and the state labels map
            diagramContainer.addComponent(stateLabel);
            stateLabels.put(state, stateLabel);
        }
        
        // Add the diagram container to this component
        addComponent(diagramContainer);
        
        // Add a legend explaining the colors
        Container legendContainer = new Container("legend");
        BoxLayout legendLayout = new BoxLayout(BoxLayout.Orientation.HORIZONTAL, 2);
        legendContainer.setLayout(legendLayout);
        
        Label currentStateLabel = new Label("■ Current State");
        Style currentStyle = new Style().setForeground(Color.GREEN).setBold(true);
        currentStateLabel.setStyle(currentStyle);
        
        Label availableLabel = new Label("■ Available Transitions");
        Style availableStyle = new Style().setForeground(Color.YELLOW).setBold(true);
        availableLabel.setStyle(availableStyle);
        
        Label otherLabel = new Label("■ Other States");
        Style otherStyle = new Style().setForeground(Color.GRAY);
        otherLabel.setStyle(otherStyle);
        
        legendContainer.addComponent(currentStateLabel);
        legendContainer.addComponent(availableLabel);
        legendContainer.addComponent(otherLabel);
        
        addComponent(legendContainer);
    }
    
    /**
     * Initializes the positions for each workflow state in the visualization.
     *
     * @param width The container width
     * @param height The container height
     */
    private void initializeStatePositions(int width, int height) {
        // Define positions for standard workflow states
        // Using a horizontal flow with multiple rows
        int startX = 2;
        int startY = 2;
        int stateWidth = 16;
        int stateHeight = 3;
        int horizontalGap = 2;
        int verticalGap = 2;
        int statesPerRow = 4;
        
        // Position the main workflow states in a pipeline
        statePositions.put(WorkflowState.CREATED, new Point(startX, startY));
        statePositions.put(WorkflowState.READY, new Point(startX + stateWidth + horizontalGap, startY));
        statePositions.put(WorkflowState.IN_PROGRESS, new Point(startX + (stateWidth + horizontalGap) * 2, startY));
        statePositions.put(WorkflowState.REVIEW, new Point(startX + (stateWidth + horizontalGap) * 3, startY));
        
        // Second row
        statePositions.put(WorkflowState.TESTING, new Point(startX, startY + stateHeight + verticalGap));
        statePositions.put(WorkflowState.DONE, new Point(startX + stateWidth + horizontalGap, startY + stateHeight + verticalGap));
        statePositions.put(WorkflowState.BLOCKED, new Point(startX + (stateWidth + horizontalGap) * 2, startY + stateHeight + verticalGap));
        
        // Bug workflow states
        statePositions.put(WorkflowState.FOUND, new Point(startX, startY + (stateHeight + verticalGap) * 2));
        statePositions.put(WorkflowState.TRIAGED, new Point(startX + stateWidth + horizontalGap, startY + (stateHeight + verticalGap) * 2));
        statePositions.put(WorkflowState.TO_DO, new Point(startX + (stateWidth + horizontalGap) * 2, startY + (stateHeight + verticalGap) * 2));
        statePositions.put(WorkflowState.IN_TEST, new Point(startX + (stateWidth + horizontalGap) * 3, startY + (stateHeight + verticalGap) * 2));
    }
    
    /**
     * Updates the visual representation of the workflow states.
     */
    private void updateStateVisualization() {
        // Reset key mappings
        keyMappings.clear();
        
        // Assign keyboard shortcuts to available transitions
        char key = '1';
        for (WorkflowState state : availableTransitions) {
            keyMappings.put(key, state);
            key++;
        }
        
        // Update state labels with colors and key shortcuts
        for (WorkflowState state : WorkflowState.values()) {
            Label stateLabel = stateLabels.get(state);
            if (stateLabel != null) {
                // Check if this is the current state
                if (state == currentState) {
                    // Current state - green and bold
                    Style currentStyle = new Style().setForeground(Color.GREEN).setBold(true);
                    stateLabel.setStyle(currentStyle);
                    stateLabel.setText(formatStateLabel(state, ' '));
                } 
                // Check if this is an available transition
                else if (availableTransitions.contains(state)) {
                    // Available transition - yellow with key shortcut
                    char shortcutKey = getKeyForState(state);
                    Style availableStyle = new Style().setForeground(Color.YELLOW).setBold(true);
                    stateLabel.setStyle(availableStyle);
                    stateLabel.setText(formatStateLabel(state, shortcutKey));
                } 
                // Other states
                else {
                    // Other state - gray
                    Style otherStyle = new Style().setForeground(Color.GRAY);
                    stateLabel.setStyle(otherStyle);
                    stateLabel.setText(formatStateLabel(state, ' '));
                }
            }
        }
    }
    
    /**
     * Gets the key assigned to a specific state.
     *
     * @param state The workflow state
     * @return The key assigned to the state, or space if none
     */
    private char getKeyForState(WorkflowState state) {
        for (Map.Entry<Character, WorkflowState> entry : keyMappings.entrySet()) {
            if (entry.getValue() == state) {
                return entry.getKey();
            }
        }
        return ' ';
    }
    
    /**
     * Formats a label for a workflow state, optionally including a key shortcut.
     *
     * @param state The workflow state
     * @param key The key shortcut (or space if none)
     * @return The formatted label
     */
    private String formatStateLabel(WorkflowState state, char key) {
        if (key == ' ') {
            return state.toString();
        } else {
            return String.format("%c: %s", key, state.toString());
        }
    }
    
    /**
     * Handles key events for workflow transitions.
     *
     * @param keyCode The key code
     * @param keyChar The key character
     * @param modifiers Key modifiers
     * @return true if the key was handled, false otherwise
     */
    @Override
    public boolean handleKeyEvent(int keyCode, char keyChar, int modifiers) {
        // Check if the key corresponds to an available transition
        if (keyMappings.containsKey(keyChar)) {
            WorkflowState targetState = keyMappings.get(keyChar);
            
            // Call the transition handler if set
            if (transitionHandler != null) {
                transitionHandler.accept(targetState);
                return true;
            }
        }
        
        // Handle ESC key to cancel
        if (keyCode == KeyHandler.KEY_ESCAPE) {
            statusLabel.setText("Transition cancelled");
            return true;
        }
        
        return super.handleKeyEvent(keyCode, keyChar, modifiers);
    }
    
    /**
     * Sets a status message in the status label.
     *
     * @param message The message to display
     */
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }
}