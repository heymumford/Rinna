/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.geom;

/**
 * Represents the size of a component in terms of width and height.
 */
public class Dimension {
    
    private final int width;
    private final int height;
    
    /**
     * Creates a new dimension with the specified width and height.
     * 
     * @param width the width
     * @param height the height
     */
    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    /**
     * Gets the width.
     * 
     * @return the width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the height.
     * 
     * @return the height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Creates a new dimension by adding the specified offsets to this dimension.
     * 
     * @param dw the width offset
     * @param dh the height offset
     * @return the new dimension
     */
    public Dimension expand(int dw, int dh) {
        return new Dimension(width + dw, height + dh);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Dimension other = (Dimension) obj;
        return width == other.width && height == other.height;
    }
    
    @Override
    public int hashCode() {
        return 31 * width + height;
    }
    
    @Override
    public String toString() {
        return "Dimension(" + width + "x" + height + ")";
    }
}