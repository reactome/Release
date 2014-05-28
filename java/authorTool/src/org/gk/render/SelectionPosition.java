/*
 * Created on May 29, 2008
 *
 */
package org.gk.render;

/**
 * This enum is used to indicate what position in a node is selected.
 * @author wgm
 *
 */
public enum SelectionPosition {
    // north east corner
    NORTH_EAST,
    // south east corner
    SOUTH_EAST,
    // south west corner
    SOUTH_WEST,
    // north west corner
    NORTH_WEST,
    // inside north east corner
    IN_NORTH_EAST,
    // inside south east corner
    IN_SOUTH_EAST,
    // inside south west corner
    IN_SOUTH_WEST,
    // inside north west corner
    IN_NORTH_WEST,
    // nothing is selected
    NONE,
    // the whole node is selected
    NODE,
    // The text label is selected
    TEXT,
    // A contained child Node (e.g. complex component) is selected
    CHILD_NODE,
    // A RenderableFeature is selected
    FEATURE,
    // A RenderableState is selected
    STATE
}
