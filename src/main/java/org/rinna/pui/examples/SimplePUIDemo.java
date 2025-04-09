/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.examples;

import org.rinna.pui.RinnaPUI;
import org.rinna.pui.component.BoxLayout;
import org.rinna.pui.component.Container;
import org.rinna.pui.component.BoxLayout.Orientation;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.BorderStyle;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

import java.io.IOException;

/**
 * Simple demo of the Rinna Pragmatic User Interface (PUI).
 */
public class SimplePUIDemo {
    
    /**
     * Entry point for the demo.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            // Create the main container
            Container mainContainer = new Container("main");
            mainContainer.setPosition(new Point(0, 0));
            mainContainer.setSize(new Dimension(80, 24));
            
            // Create the header container
            Container headerContainer = new Container("header");
            headerContainer.setSize(new Dimension(80, 3));
            
            Style headerStyle = new Style()
                .setBackground(Color.BLUE)
                .setForeground(Color.WHITE)
                .setBold(true);
            headerContainer.setStyle(headerStyle);
            
            // Create the body container
            Container bodyContainer = new Container("body");
            bodyContainer.setSize(new Dimension(80, 18));
            
            Style bodyStyle = new Style()
                .setBorderStyle(BorderStyle.SINGLE);
            bodyContainer.setStyle(bodyStyle);
            
            // Create the footer container
            Container footerContainer = new Container("footer");
            footerContainer.setSize(new Dimension(80, 3));
            
            Style footerStyle = new Style()
                .setBackground(Color.BLUE)
                .setForeground(Color.WHITE);
            footerContainer.setStyle(footerStyle);
            
            // Set up the main container layout
            BoxLayout mainLayout = new BoxLayout(Orientation.VERTICAL, 0);
            mainContainer.setLayout(mainLayout);
            
            // Add the containers to the main container
            mainContainer.addComponent(headerContainer);
            mainContainer.addComponent(bodyContainer);
            mainContainer.addComponent(footerContainer);
            
            // Create a custom theme
            Theme theme = Theme.createDefault();
            
            // Initialize the UI
            RinnaPUI pui = RinnaPUI.getInstance();
            pui.initialize(mainLayout)
               .addComponent(mainContainer)
               .setTheme(theme);
            
            // Start the UI
            pui.start();
            
            // This will block until the UI is closed by the user (e.g. by pressing ESC)
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}