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

import toxi.geom.Quaternion;
import toxi.geom.ReadonlyVec3D;
import toxi.geom.Vec2D;
import toxi.geom.Vec3D;

/**
 * Arcball/trackball navigation controller to naturally rotate 3D object or
 * camera orientation using 2D mouse gestures. Based on code by Simon Greenwold
 * & Tom Carden. Karsten Schmidt simplified, removed Processing dependency &
 * adapted to use toxiclibs core classes.
 * 
 * In order to update view orientation you'll need to call
 * {@link #mousePressed(Vec2D)}, {@link #mouseDragged(Vec2D)} and
 * {@link #mouseReleased()} methods with the current mouse positions.
 * 
 * To apply the view, the class provides the {@link #getAxisAngle()} method
 * which returns a float array of the current rotation axis and angle.
 * 
 * @author Simon Greenwold
 * @author Tom Carden
 * @author Karsten Schmidt
 */
public class ArcBall {

	private final Vec2D center;
	private Vec3D downPos, dragPos;
	private Quaternion currOrientation, downOrientation, dragOrientation;
	private final ReadonlyVec3D[] axisSet;
	private final float radius;
	private int constrainedAxisID;

	private boolean isPressed;

	public ArcBall(float cx, float cy, float radius) {
		this.center = new Vec2D(cx, cy);
		this.radius = radius;

		downPos = new Vec3D();
		dragPos = new Vec3D();

		reset();

		axisSet = new ReadonlyVec3D[] { Vec3D.X_AXIS, Vec3D.Y_AXIS,
				Vec3D.Z_AXIS };
		constrainedAxisID = -1;
	}

	public Vec3D constrainVector(Vec3D v, ReadonlyVec3D axis) {
		Vec3D res = v.sub(axis.scale(axis.dot(v)));
		return res.normalize();
	}

	public float[] getAxisAngle() {
		if (isPressed) {
			currOrientation = dragOrientation.multiply(downOrientation);
		}
		return currOrientation.toAxisAngle();
	}

	/**
	 * @return the constrainedAxisID
	 */
	public int getConstrainedAxisID() {
		return constrainedAxisID;
	}

	public Vec3D mapPointOnSphere(Vec2D pos) {
		Vec2D p = pos.sub(center).scaleSelf(1.0f / radius);
		Vec3D v = p.to3DXY();
		float mag = p.magSquared();
		if (mag > 1.0f) {
			v.normalize();
		} else {
			v.z = (float) Math.sqrt(1.0f - mag);
		}
		return (constrainedAxisID == -1) ? v : constrainVector(v,
				axisSet[constrainedAxisID]);
	}

	public void mouseDragged(Vec2D mousePos) {
		dragPos = mapPointOnSphere(mousePos);
		dragOrientation.set(downPos.dot(dragPos), downPos.cross(dragPos));
	}

	public void mousePressed(Vec2D mousePos) {
		isPressed = true;
		downPos = mapPointOnSphere(mousePos);
		downOrientation.set(currOrientation);
		dragOrientation.identity();
	}

	public void mouseReleased() {
		isPressed = false;
	}

	public void reset() {
		currOrientation = new Quaternion();
		downOrientation = new Quaternion();
		dragOrientation = new Quaternion();
	}

	/**
	 * @param constrainedAxisID
	 *            the constrainedAxisID to set
	 */
	public void setConstrainedAxisID(int constrainedAxisID) {
		if (constrainedAxisID >= 0 && constrainedAxisID < axisSet.length) {
			this.constrainedAxisID = constrainedAxisID;
		}
	}
}
