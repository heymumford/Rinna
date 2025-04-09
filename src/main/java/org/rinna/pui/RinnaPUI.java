/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui;

import org.rinna.pui.component.Component;
import org.rinna.pui.component.Container;
import org.rinna.pui.component.Layout;
import org.rinna.pui.input.KeyHandler;
import org.rinna.pui.render.TerminalRenderer;
import org.rinna.pui.style.Theme;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main entry point for the Rinna Pragmatic User Interface (PUI).
 * This class is responsible for initializing the UI, handling input events,
 * and managing the rendering loop.
 */
public class RinnaPUI {
    
    private static RinnaPUI instance;
    
    private final TerminalRenderer renderer;
    private final KeyHandler keyHandler;
    private final Container rootContainer;
    private final Theme theme;
    private final ExecutorService renderThread;
    private final AtomicBoolean running;
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private RinnaPUI() {
        this.renderer = new TerminalRenderer();
        this.keyHandler = new KeyHandler();
        this.rootContainer = new Container();
        this.theme = Theme.createDefault();
        this.renderThread = Executors.newSingleThreadExecutor();
        this.running = new AtomicBoolean(false);
    }
    
    /**
     * Gets the singleton instance of RinnaPUI.
     * 
     * @return the singleton instance
     */
    public static synchronized RinnaPUI getInstance() {
        if (instance == null) {
            instance = new RinnaPUI();
        }
        return instance;
    }
    
    /**
     * Initializes the UI with the given layout.
     * 
     * @param layout the layout to use
     * @return this instance for method chaining
     */
    public RinnaPUI initialize(Layout layout) {
        rootContainer.setLayout(layout);
        return this;
    }
    
    /**
     * Adds a component to the root container.
     * 
     * @param component the component to add
     * @return this instance for method chaining
     */
    public RinnaPUI addComponent(Component component) {
        rootContainer.addComponent(component);
        return this;
    }
    
    /**
     * Sets the theme for the UI.
     * 
     * @param theme the theme to use
     * @return this instance for method chaining
     */
    public RinnaPUI setTheme(Theme theme) {
        this.theme.updateFrom(theme);
        return this;
    }
    
    /**
     * Starts the UI rendering loop and input handling.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void start() throws IOException {
        if (running.compareAndSet(false, true)) {
            renderer.initialize();
            keyHandler.initialize();
            
            renderThread.submit(this::renderLoop);
            
            // Start input handling on the current thread
            inputLoop();
        }
    }
    
    /**
     * Stops the UI and releases resources.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void stop() throws IOException {
        if (running.compareAndSet(true, false)) {
            renderer.cleanup();
            keyHandler.cleanup();
            renderThread.shutdown();
        }
    }
    
    /**
     * The main rendering loop that runs on a separate thread.
     */
    private void renderLoop() {
        try {
            while (running.get()) {
                renderer.clear();
                renderer.render(rootContainer, theme);
                renderer.refresh();
                
                Thread.sleep(1000 / 30); // ~30 FPS
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * The main input handling loop that runs on the calling thread.
     */
    private void inputLoop() {
        try {
            while (running.get()) {
                int key = keyHandler.readKey();
                if (key != -1) {
                    handleKey(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Handles a key press event.
     * 
     * @param key the key code
     */
    private void handleKey(int key) {
        // ESC key (27) to exit
        if (key == 27) {
            try {
                stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        // Delegate key handling to current focused component
        Component focused = rootContainer.getFocusedComponent();
        if (focused != null) {
            focused.handleKey(key);
        }
    }
}