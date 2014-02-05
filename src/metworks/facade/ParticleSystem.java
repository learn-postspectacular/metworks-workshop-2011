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

import java.util.ArrayList;
import java.util.List;

import processing.core.PGraphics;
import toxi.geom.Circle;
import toxi.geom.Rect;
import toxi.geom.Vec2D;
import toxi.math.ScaleMap;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.physics2d.behaviors.ParticleBehavior2D;
import toxi.processing.ToxiclibsSupport;

/**
 * Physics based 2D constrained particle system with dedicated attractors and
 * general repulsion between particles. The system is the main design driver for
 * the patterning of the facade. The attractors are used to define loose
 * clusters of particles within the field and so use the higher spatial density
 * to be reflected as areas of lower light transmission in the resulting facade
 * model.
 * 
 * Apart from managing & updating the physical entities and simulation, this
 * class also provides all functionality to select & interactively manipulate
 * attractors and force parameters.
 */
public class ParticleSystem {

	/**
	 * Physics simulation instance
	 */
	private final VerletPhysics2D physics;

	/**
	 * List of user added attractors
	 */
	private final List<AttractionBehavior2D> attractors;

	/**
	 * World bounds for the simulation. No particle can leave this rect.
	 */
	private final Rect bounds;

	/**
	 * Reference to the currently selected attractor (if any, else null)
	 */
	private AttractionBehavior2D selectedAttractor;

	/**
	 * Helper variable for handling mouse dragging. Stores offset between click
	 * position and actual position of selected attractor.
	 */
	private Vec2D clickOffset;

	/**
	 * Initial separation distance between particles
	 */
	private float separation = 20;

	/**
	 * Creates a new instance with the given world bounds for the physics
	 * simulation.
	 * 
	 * @param width
	 * @param height
	 */
	public ParticleSystem(int width, int height) {
		physics = new VerletPhysics2D();
		physics.setDrag(0.03f);
		attractors = new ArrayList<AttractionBehavior2D>();
		bounds = new Rect(0, 0, width, height);
		physics.setWorldBounds(bounds);
	}

	/**
	 * Adds a new attractor at a random position within the allowed bounds.
	 * Furthermore, for each new attractor there're a number of particles
	 * created too within the radius of influence of the new attractor. The
	 * number of particles added is proportional to the overall size of the
	 * attractor.
	 */
	public void addAttractor() {
		// define a scale map to relate a range of possible radii with a range
		// of possible numbers of particles to add
		ScaleMap radiusParticleMap = new ScaleMap(50, 200, 10, 60);
		// pick a random value from the input range (possible radius: 50 - 200)
		float radius = (float) radiusParticleMap.getInputRange().pickRandom();
		// create new positive attractor at random position and 50% of full
		// force
		AttractionBehavior2D a = new AttractionBehavior2D(
				bounds.getRandomPoint(), radius, 0.5f);
		// add to simulation
		physics.addBehavior(a);
		// add to list of attractors (used for selecting etc.)
		attractors.add(a);
		// now get a proportional number of particles to add for the picked
		// radius
		int numP = (int) radiusParticleMap.getMappedValueFor(radius);
		// define a circle matching the attractor position and radius of
		// influence
		Circle attractorCircle = new Circle(a.getAttractor(), radius);
		for (int i = 0; i < numP; i++) {
			// create each particle within the circle
			Vec2D pos = attractorCircle.getRandomPoint();
			addParticle(pos);
		}
		// mark new attractor as selection
		selectAttractor(a);
	}

	/**
	 * Adds a single particle incl. repulsive force field around itself.
	 * 
	 * @param pos
	 *            particle position
	 */
	private void addParticle(Vec2D pos) {
		VerletParticle2D p = new VerletParticle2D(pos);
		physics.addParticle(p);
		physics.addBehavior(new AttractionBehavior2D(p, separation, -1.2f));
	}

	/**
	 * Adds the given number of particles to the system.
	 * 
	 * @param num
	 *            particles to add
	 */
	public void addParticles(int num) {
		for (int i = 0; i < num; i++) {
			addParticle(bounds.getRandomPoint());
		}
	}

	/**
	 * Removes all attractors and particles from the system
	 */
	public void clear() {
		deselectAttractor();
		physics.clear();
		attractors.clear();
	}

	/**
	 * Clears the current attractor selection
	 */
	public void deselectAttractor() {
		selectedAttractor = null;
	}

	/**
	 * Draws all particles and attractors, handles highlighting of currently
	 * selected attractor.
	 * 
	 * @param gfx
	 */
	public void draw(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		pg.noFill();
		pg.stroke(255);
		gfx.rect(bounds);
		for (AttractionBehavior2D a : attractors) {
			if (selectedAttractor == a) {
				pg.stroke(255, 0, 255);
			} else {
				pg.stroke(255, 0, 0);
			}
			gfx.circle(a.getAttractor(), a.getRadius());
		}
		pg.stroke(255, 255, 0);
		for (VerletParticle2D p : physics.particles) {
			gfx.circle(p, 2);
		}
	}

	/**
	 * @return world bounds of simulation
	 */
	public Rect getBounds() {
		return bounds;
	}

	/**
	 * @return drag force of physics engine
	 */
	public float getDrag() {
		return physics.getDrag();
	}

	/**
	 * @return physics engine
	 */
	public VerletPhysics2D getPhysics() {
		return physics;
	}

	/**
	 * @return selected attractor
	 */
	public AttractionBehavior2D getSelectedAttractor() {
		return selectedAttractor;
	}

	/**
	 * @return separation distance between particles
	 */
	public float getSeparation() {
		return separation;
	}

	/**
	 * @return true, if selection is not null.
	 */
	public boolean hasSelectedAttractor() {
		return selectedAttractor != null;
	}

	public void moveSelectedAttractor(Vec2D mousePos) {
		if (selectedAttractor != null) {
			selectedAttractor.getAttractor().set(mousePos.sub(clickOffset));
		}
	}

	/**
	 * Marks the given attractor as selection.
	 * 
	 * @param a
	 */
	private void selectAttractor(AttractionBehavior2D a) {
		selectedAttractor = a;
	}

	/**
	 * Attempts to match an attractor based on proximity to mouse positon (or
	 * actually containment, i.e. is mouse position inside radius of
	 * influence?). If successful, the matched attractor is marked as currently
	 * selected.
	 * 
	 * @param mousePos
	 */
	public void selectAttractorNearPosition(Vec2D mousePos) {
		selectedAttractor = null;
		for (AttractionBehavior2D a : attractors) {
			Circle c = new Circle(a.getAttractor(), a.getRadius());
			if (c.containsPoint(mousePos)) {
				selectAttractor(a);
				clickOffset = mousePos.sub(c);
			}
		}
	}

	/**
	 * Updates the drag in the underlying physics engine.
	 * 
	 * @param newDrag
	 */
	public void setDrag(float newDrag) {
		physics.setDrag(newDrag);
	}

	/**
	 * Applies the given separation distance to all existing
	 * {@link AttractionBehavior2D}s, but NOT if they are in the list of
	 * "actual" attractors. Hence only the repulsion radius between "standard"
	 * particles will be updated.
	 * 
	 * @param s
	 *            new separation distance
	 */
	public void setSeparation(float s) {
		separation = s;
		for (ParticleBehavior2D p : physics.behaviors) {
			if (!attractors.contains(p)) {
				AttractionBehavior2D a = (AttractionBehavior2D) p;
				a.setRadius(separation);
			}
		}
	}

	/**
	 * Updates the underlying physics system.
	 */
	public void update() {
		physics.update();
	}
}
