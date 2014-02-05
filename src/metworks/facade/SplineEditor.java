/* 
 *                 __                       __            
 *   _____   _____/  |___  _  _____________|  | __  ______
 *  /     \_/ __ \   __\ \/ \/ /  _ \_  __ \  |/ / /  ___/
 * |  Y Y  \  ___/|  |  \     (  <_> )  | \/    <  \___ \ 
 * |__|_|  /\___  >__|   \/\_/ \____/|__|  |__|_ \/____  >
 *       \/     \/                              \/     \/ 
 *   _____                          .___      
 * _/ ____\____    ____ _____     __| _/____    Processing and
 * \   __\\__  \ _/ ___\\__  \   / __ |/ __ \   toxiclibs workshop
 *  |  |   / __ \\  \___ / __ \_/ /_/ \  ___/   at Metropolitan Works
 *  |__|  (____  /\___  >____  /\____ |\___  >  London, December 2011
 *             \/     \/     \/      \/    \/ 
 *
 * Copyright (c) 2011 Karsten Schmidt
 * 
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * http://creativecommons.org/licenses/LGPL/2.1/
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package metworks.facade;

import processing.core.PGraphics;
import toxi.geom.LineStrip2D;
import toxi.geom.Rect;
import toxi.geom.Spline2D;
import toxi.geom.Vec2D;
import toxi.processing.ToxiclibsSupport;

/**
 * Simple 2D spline editor to allow the user define a profile for the facade.
 * The editor supports selecting & repositioning existing control points.
 */
public class SplineEditor {

	/**
	 * Snap distance (in pixels) used for selecting points with mouse
	 */
	private static final float SNAP_DISTANCE = 10;

	/**
	 * Container for storing points and compute resulting curve
	 */
	private final Spline2D spline = new Spline2D();

	/**
	 * Screen space rectangle to define boundaries of the curve edit area
	 */
	private final Rect editBounds;

	/**
	 * Distance between grid lines (in pixels)
	 */
	private final int gridSize;

	/**
	 * Reference to the currently selected point (or null, if none is selected)
	 */
	private Vec2D selectedPoint;

	/**
	 * Creates a new instance of this class with the given screen bounds and
	 * grid size.
	 * 
	 * @param bounds
	 *            edit area rectangle
	 * @param grid
	 *            grid size
	 */
	public SplineEditor(Rect bounds, int grid) {
		this.editBounds = bounds;
		this.gridSize = grid;
		resetSpline();
	}

	/**
	 * Draws all elements related to this editor: semi-transparent background,
	 * grid and current curve
	 * 
	 * @param gfx
	 *            toxiclibs helper class for rendering
	 */
	public void draw(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		pg.noStroke();
		// draw semi-transparent bg
		pg.fill(0, 100);
		gfx.rect(editBounds);
		// draw grid lines
		pg.noFill();
		pg.stroke(255, 20);
		float left = editBounds.getLeft();
		float right = editBounds.getRight();
		float top = editBounds.getTop();
		float bottom = editBounds.getBottom();
		// horizontal
		for (float y = top; y < bottom; y += gridSize) {
			pg.line(left, y, right, y);
		}
		// vertical
		for (float x = left; x < right; x += gridSize) {
			pg.line(x, top, x, bottom);
		}
		// mark curve vertex positions
		pg.stroke(255);
		for (Vec2D p : spline.getPointList()) {
			gfx.circle(p, 2);
		}
		// draw current spline curve
		if (spline.getNumPoints() > 2) {
			LineStrip2D strip = spline.toLineStrip2D(20);
			gfx.lineStrip2D(strip);
		}
	}

	/**
	 * @return edit area rectangle
	 */
	public Rect getBounds() {
		return editBounds;
	}

	/**
	 * @return actual spline instance
	 */
	public Spline2D getSpline() {
		return spline;
	}

	/**
	 * @return true, if there's a control point currently selected
	 */
	public boolean hasSelectedPoint() {
		return selectedPoint != null;
	}

	/**
	 * Moves the currently selected control point (if any) to the new mouse
	 * position.
	 * 
	 * @param mousePos
	 * @return true, if point has been updated
	 */
	public boolean mouseDragged(Vec2D mousePos) {
		// do we actually have a selection?
		if (hasSelectedPoint()) {
			selectedPoint.set(mousePos.getConstrained(editBounds));
			return true;
		}
		return false;
	}

	/**
	 * Checks if the mouse position is on or near any of the curve control
	 * points and if so marks that point as selected. If no points match the
	 * position and the mouse position is inside the edit area, then a new
	 * control point is added to the curve and used as the current selection.
	 * 
	 * @param mousePos
	 * @return true, if a point has been selected (or added)
	 */
	public boolean mousePressed(Vec2D mousePos) {
		// check if mouse pos is inside edit area
		if (editBounds.containsPoint(mousePos)) {
			// attempt to match mouse pos to any of the existing control points
			for (Vec2D p : spline.pointList) {
				if (p.distanceTo(mousePos) < SNAP_DISTANCE) {
					selectedPoint = p;
					return true;
				}
			}
			// no point has been matched, so add a new one at the given position
			// and use as selection
			selectedPoint = mousePos.copy();
			spline.add(selectedPoint);
			return true;
		}
		return false;
	}

	/**
	 * Simply clears the reference to any currently selected point.
	 */
	public void mouseReleased() {
		selectedPoint = null;
	}

	/**
	 * Resets the spline curve to its default state.
	 */
	public void resetSpline() {
		// get middle of edit area
		Vec2D centroid = editBounds.getCentroid();
		// empty spline
		spline.getPointList().clear();
		// add three control points
		spline.add(centroid.sub(0, editBounds.height / 4));
		spline.add(centroid.sub(editBounds.width / 4, 0));
		spline.add(centroid.add(0, editBounds.height / 4));
	}
}
