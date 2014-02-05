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

import toxi.math.MathUtils;
import toxi.math.noise.SimplexNoise;

/**
 * Concrete implementation of a {@link DisplacementStrategy} using Ken Perlin's
 * Simplex noise as driver for spatial displacement.
 */
public class NoiseDisplacement implements DisplacementStrategy {

	public static final float NOISE_SCALE = 0.005f;

	private float displace;

	/**
	 * Uses the point's x & y position to compute a displacement amount using 2d
	 * Simplex noise.
	 * 
	 * @see metworks.facade.DisplacementStrategy#getDisplacementForPoint(metworks.facade.FacadePoint)
	 */
	@Override
	public float getDisplacementForPoint(FacadePoint p) {
		// compute noise value at the point's position scaled with the
		// NOISE_SCALE factor
		// in order to create more smooth value changes between points
		float amp = MathUtils.abs((float) SimplexNoise.noise(p.x * NOISE_SCALE,
				p.y * NOISE_SCALE) * displace);
		return amp;
	}

	@Override
	public float getDisplacementStrength() {
		return displace;
	}

	@Override
	public void setDisplacementStrength(float displace) {
		this.displace = displace;
	}

}
