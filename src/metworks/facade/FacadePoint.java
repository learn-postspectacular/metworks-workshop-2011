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

import toxi.geom.ReadonlyVec2D;
import toxi.geom.ReadonlyVec3D;
import toxi.geom.Vec3D;

/**
 * A simple extension to the standard Vec3D class to pair position with surface
 * normal information. This normal vector is used to apply a displacement
 * operation based on current settings in the user interface.
 */
public class FacadePoint extends Vec3D {

	/**
	 * Immutable surface normal vector
	 */
	private final ReadonlyVec3D normal;
	private final ReadonlyVec2D relPos;

	/**
	 * Creates a new instance
	 * 
	 * @param x
	 *            x coord of point
	 * @param y
	 *            y coord of point
	 * @param z
	 *            z coord of point
	 * @param normal
	 *            surface normal (direction unit vector) at the given XYZ
	 * @param relPos
	 *            relative position of this point in the 2D space of the
	 *            particle system
	 */
	public FacadePoint(float x, float y, float z, ReadonlyVec3D normal,
			ReadonlyVec2D relPos) {
		super(x, y, z);
		this.normal = normal;
		this.relPos = relPos;
	}

	/**
	 * Computes a new position on the ray going from this point in the direction
	 * of the point's normal at the distance of the given a displacement amount.
	 * Does not modify original point.
	 * 
	 * @param displace
	 * @return displaced position
	 */
	public Vec3D getDisplaced(float displace) {
		return add(normal.getNormalizedTo(displace));
	}

	public ReadonlyVec3D getNormal() {
		return normal;
	}

	public ReadonlyVec2D getRelPos() {
		return relPos;
	}
}
