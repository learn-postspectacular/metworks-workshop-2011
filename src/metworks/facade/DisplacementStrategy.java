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

/**
 * Interface to displace a single {@link FacadePoint} based on some custom logic
 * (e.g. using the coordinates and surface normal as inputs). See
 * {@link NoiseDisplacement} for a concrete example implementation of this
 * interface.
 */
public interface DisplacementStrategy {

	/**
	 * Applies displacement to single {@link FacadePoint}. This function should
	 * NOT manipulate to original point given, but create a new Vec3D with the
	 * displaced position, e.g. by using {@link FacadePoint#getDisplaced(float)}
	 * .
	 * 
	 * @param p
	 *            original point
	 * @return absolute displaced position
	 */
	float getDisplacementForPoint(FacadePoint p);

	/**
	 * @return displacement amount
	 */
	float getDisplacementStrength();

	/**
	 * Sets the displacement amount
	 * 
	 * @param displace
	 */
	void setDisplacementStrength(float displace);
}
