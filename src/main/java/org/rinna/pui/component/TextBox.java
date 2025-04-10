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
import java.util.List;

import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.input.KeyHandler;
import org.rinna.pui.render.ComponentRenderer;
import org.rinna.pui.render.TextBoxRenderer;
import org.rinna.pui.style.Style;

/**
 * A text box component for text input.
 */
public class TextBox implements Component {
    
    /**
     * Interface for text change event listeners.
     */
    public interface TextChangeListener {
        /**
         * Called when the text in the text box changes.
         * 
         * @param textBox the text box that was changed
         * @param oldText the old text
         * @param newText the new text
         */
        void onTextChange(TextBox textBox, String oldText, String newText);
    }
    
    private String id;
    private StringBuilder text;
    private int caretPosition;
    private int width;
    private Point position;
    private Dimension size;
    private Style style;
    private Container parent;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused = false;
    private TextBoxRenderer renderer;
    private Object layoutConstraints;
    private List<TextChangeListener> textChangeListeners;
    private String placeholder;
    private boolean password;
    private char passwordChar = '*';
    
    /**
     * Creates a new text box with the specified ID and width.
     * 
     * @param id the text box ID
     * @param width the width of the text box
     */
    public TextBox(String id, int width) {
        this.id = id;
        this.text = new StringBuilder();
        this.caretPosition = 0;
        this.width = width;
        this.renderer = new TextBoxRenderer();
        this.size = new Dimension(width, 1);
        this.textChangeListeners = new ArrayList<>();
    }
    
    /**
     * Creates a new text box with the specified width and a default ID.
     * 
     * @param width the width of the text box
     */
    public TextBox(int width) {
        this("textbox-" + System.currentTimeMillis(), width);
    }
    
    /**
     * Gets the text in the text box.
     * 
     * @return the text
     */
    public String getText() {
        return text.toString();
    }
    
    /**
     * Sets the text in the text box.
     * 
     * @param text the new text
     */
    public void setText(String text) {
        String oldText = this.text.toString();
        this.text = new StringBuilder(text != null ? text : "");
        this.caretPosition = this.text.length();
        
        // Notify listeners of the text change
        for (TextChangeListener listener : textChangeListeners) {
            listener.onTextChange(this, oldText, this.text.toString());
        }
    }
    
    /**
     * Gets the caret position in the text box.
     * 
     * @return the caret position
     */
    public int getCaretPosition() {
        return caretPosition;
    }
    
    /**
     * Sets the caret position in the text box.
     * 
     * @param position the new caret position
     */
    public void setCaretPosition(int position) {
        this.caretPosition = Math.max(0, Math.min(text.length(), position));
    }
    
    /**
     * Gets the placeholder text for the text box.
     * 
     * @return the placeholder text
     */
    public String getPlaceholder() {
        return placeholder;
    }
    
    /**
     * Sets the placeholder text for the text box.
     * The placeholder is displayed when the text box is empty.
     * 
     * @param placeholder the new placeholder text
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
    
    /**
     * Checks if the text box is in password mode.
     * 
     * @return true if the text box is in password mode, false otherwise
     */
    public boolean isPassword() {
        return password;
    }
    
    /**
     * Sets whether the text box is in password mode.
     * In password mode, the text is displayed as password characters.
     * 
     * @param password whether the text box is in password mode
     */
    public void setPassword(boolean password) {
        this.password = password;
    }
    
    /**
     * Gets the password character used to mask the text in password mode.
     * 
     * @return the password character
     */
    public char getPasswordChar() {
        return passwordChar;
    }
    
    /**
     * Sets the password character used to mask the text in password mode.
     * 
     * @param passwordChar the new password character
     */
    public void setPasswordChar(char passwordChar) {
        this.passwordChar = passwordChar;
    }
    
    /**
     * Adds a text change listener to this text box.
     * 
     * @param listener the listener to add
     */
    public void addTextChangeListener(TextChangeListener listener) {
        textChangeListeners.add(listener);
    }
    
    /**
     * Removes a text change listener from this text box.
     * 
     * @param listener the listener to remove
     * @return true if the listener was removed, false otherwise
     */
    public boolean removeTextChangeListener(TextChangeListener listener) {
        return textChangeListeners.remove(listener);
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
        this.width = size.getWidth();
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
        if (!isEnabled() || !isFocused()) {
            return false;
        }
        
        String oldText = text.toString();
        boolean textChanged = false;
        
        switch (key) {
            case KeyHandler.KEY_LEFT:
                // Move caret left
                if (caretPosition > 0) {
                    caretPosition--;
                }
                break;
                
            case KeyHandler.KEY_RIGHT:
                // Move caret right
                if (caretPosition < text.length()) {
                    caretPosition++;
                }
                break;
                
            case KeyHandler.KEY_HOME:
                // Move caret to beginning
                caretPosition = 0;
                break;
                
            case KeyHandler.KEY_END:
                // Move caret to end
                caretPosition = text.length();
                break;
                
            case KeyHandler.KEY_BACKSPACE:
                // Delete character before caret
                if (caretPosition > 0) {
                    text.deleteCharAt(caretPosition - 1);
                    caretPosition--;
                    textChanged = true;
                }
                break;
                
            case KeyHandler.KEY_ENTER:
                // In a real application, this might trigger a form submission
                return false;
                
            default:
                // Add character at caret position
                if (key >= 32 && key <= 126) { // Printable ASCII characters
                    text.insert(caretPosition, (char) key);
                    caretPosition++;
                    textChanged = true;
                }
                break;
        }
        
        // Notify listeners of text change
        if (textChanged) {
            for (TextChangeListener listener : textChangeListeners) {
                listener.onTextChange(this, oldText, text.toString());
            }
        }
        
        return true;
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
    
    /**
     * Gets the width of the text box.
     * 
     * @return the width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Sets the width of the text box.
     * 
     * @param width the new width
     */
    public void setWidth(int width) {
        this.width = width;
        this.size = new Dimension(width, 1);
    }
}