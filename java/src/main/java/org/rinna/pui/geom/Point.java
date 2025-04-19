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
 * Represents a point in 2D space with x and y coordinates.
 */
public class Point {
    
    private final int x;
    private final int y;
    
    /**
     * Creates a new point at the specified coordinates.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Gets the x coordinate.
     * 
     * @return the x coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Gets the y coordinate.
     * 
     * @return the y coordinate
     */
    public int getY() {
        return y;
    }
    
    /**
     * Creates a new point by adding the specified offsets to this point.
     * 
     * @param dx the x offset
     * @param dy the y offset
     * @return the new point
     */
    public Point translate(int dx, int dy) {
        return new Point(x + dx, y + dy);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Point other = (Point) obj;
        return x == other.x && y == other.y;
    }
    
    @Override
    public int hashCode() {
        return 31 * x + y;
    }
    
    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }
}