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

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;
import toxi.geom.AABB;
import toxi.geom.Line3D;
import toxi.geom.LineStrip2D;
import toxi.geom.LineStrip3D;
import toxi.geom.Polygon2D;
import toxi.geom.PolygonClipper2D;
import toxi.geom.Rect;
import toxi.geom.SutherlandHodgemanClipper;
import toxi.geom.Triangle2D;
import toxi.geom.Vec2D;
import toxi.geom.Vec3D;
import toxi.geom.mesh.LaplacianSmooth;
import toxi.geom.mesh.WETriangleMesh;
import toxi.geom.mesh2d.Voronoi;
import toxi.math.MathUtils;
import toxi.processing.ToxiclibsSupport;
import toxi.util.DateUtils;
import toxi.util.datatypes.FloatRange;
import toxi.volume.BoxBrush;
import toxi.volume.HashIsoSurface;
import toxi.volume.MeshLatticeBuilder;
import toxi.volume.VolumetricBrush;
import toxi.volume.VolumetricSpace;
import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlListener;
import controlP5.ControlP5;
import controlP5.RadioButton;
import controlP5.Slider;
import controlP5.Toggle;

public class FacadeApp extends PApplet {

	/**
	 * Main entry point for the application when running as application (not
	 * applet). We simply delegate to the parent Processing PApplet to handle
	 * all basic initialization tasks required.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] { "metworks.facade.FacadeApp" });
	}

	/**
	 * helper libraries for rendering 2d/3d geometry types
	 */
	private ToxiclibsSupport gfx;

	/**
	 * user interface library
	 */
	private ControlP5 gui;

	/**
	 * 3D arc ball navigation
	 */
	private ArcBall arcBall;

	/**
	 * particle system for managing the spatial distribution of points on the
	 * facade using physics
	 */
	private ParticleSystem particleSys;

	/**
	 * spline editor used to define facade profile
	 */
	private SplineEditor splineEditor;

	/**
	 * particles are connected into triangles and then clipped to a bounding
	 * rectangle. this list stores the clipped 2D version of these resulting
	 * shapes
	 */
	private List<Polygon2D> clippedPolies;

	/**
	 * since there's no direct 3D counterpart for Polygon2D, we're using
	 * LineStrip3D's to recreate the polygons in 3D space
	 */
	private List<LineStrip3D> splineShapes;

	/**
	 * this list is a duplicate of splineShapes with added surface displacement
	 * (user controllable)
	 */
	private List<LineStrip3D> displacedShapes;

	/**
	 * surface displacement instance applied to each vertex of every splineShape
	 * entry
	 */
	private DisplacementStrategy displacement;

	/**
	 * top-left corner of 2d facade layout on screen
	 */
	private Vec2D offset2d;

	/**
	 * 3D bounding box of all shapes in displacedShapes this box is needed to
	 * correctly compute the iso surface mesh. AABB = axis aligned bounding box.
	 */
	private AABB bounds3D;

	/**
	 * mesh instance to hold iso surface
	 */
	private WETriangleMesh mesh;

	/**
	 * volumetric grid resolution (mapped to GUI slider in 3D mesh mode)
	 */
	private int voxelRes = 128;

	/**
	 * draw mode selector/state: 0 = 2d, 1 = 3d outlines, 2 = 3d iso mesh
	 */
	private int drawMode;

	/**
	 * 3D zoom settings (1.0 = 100%)
	 */
	private float currZoom = 1.0f, targetZoom = currZoom;

	/**
	 * UI element: only shown in 3D display mode to manipulate surface
	 * displacement
	 */
	private Slider displaceSlider;

	/**
	 * UI element: only shown in 2D mode when attractor has been selected to
	 * manipulate its radius of influence
	 */
	private Slider radiusSlider;

	/**
	 * UI element: only shown in 3D mesh mode to control the resolution of the
	 * volumetric space used to produce the iso surface of the facade.
	 */
	private Slider voxelSlider;

	/**
	 * UI element: only shown in 3D mesh mode to trigger export of STL file
	 */
	private Button btExportSTL;

	/**
	 * UI element: only shown when spline editor is visible to reset curve
	 */
	private Button btResetSpline;

	/**
	 * UI element: always shown, used to switch between different display modes
	 */
	private RadioButton btDrawMode;

	/**
	 * trigger flag to indicate that particles should be connected to form
	 * shapes
	 */
	public boolean doComputeShapes;

	/**
	 * switch used in conjunction with {@link #doComputeShapes} to indicate the
	 * shapes created should be voronoi cells (if true) or delaunay triangles.
	 */
	public boolean doUseVoronoi;

	/**
	 * interaction flag to indicate that Shift key is currently pressed
	 */
	public boolean isShiftDown;

	/**
	 * Computes 2D Voronoi or Delaunay triangulation from the current particle
	 * positions. The resulting shapes are then clipped to the particle system's
	 * bounding rect and stored in clippedPolies list.
	 */
	public void computeClippedShapes() {
		// Computing the voronoi also requires the Delaunay triangulation
		Voronoi voronoi = new Voronoi();
		for (Vec2D p : particleSys.getPhysics().particles) {
			voronoi.addPoint(p);
		}
		// also add points along the bounding rect edges
		List<Vec2D> boundingPoints = new LineStrip2D(particleSys.getBounds()
				.toPolygon2D().scale(1.05f).vertices).getDecimatedVertices(50);
		for (Vec2D p : boundingPoints) {
			voronoi.addPoint(p);
		}

		clippedPolies = new ArrayList<Polygon2D>();
		// setup a polygon clipper to constrain polygons to the bounding
		// rectangle of the particle system
		PolygonClipper2D clipper = new SutherlandHodgemanClipper(
				particleSys.getBounds());
		if (doUseVoronoi) {
			for (Polygon2D p : voronoi.getRegions()) {
				p = clipper.clipPolygon(p);
				// only accept polygon if it still has at least 3 vertices
				if (p.getNumVertices() >= 3) {
					clippedPolies.add(p);
				}
			}
		} else {
			for (Triangle2D t : voronoi.getTriangles()) {
				Polygon2D p = clipper.clipPolygon(t.toPolygon2D());
				// only accept polygon if it still has at least 3 vertices
				if (p.getNumVertices() >= 3) {
					clippedPolies.add(p);
				}
			}
		}
	}

	/**
	 * Duplicates the contents of splineShapes list and applies surface
	 * displacement to each vertex. During that process it also updates the 3D
	 * bounding box enclosing all displaced vertices.
	 */
	public void computeDisplacedShapes() {
		displacedShapes = new ArrayList<LineStrip3D>();
		// define an initially empty bounding box
		bounds3D = new AABB();
		// iterate over all shapes
		for (LineStrip3D shape : splineShapes) {
			// create empty container for the shape's displaced clone
			LineStrip3D displacedShape = new LineStrip3D();
			// iterate over all vertices in the current shape
			for (Vec3D v : shape) {
				// we actually store FacadePoint instances, but need to cast
				// them manually (and FacadePoint class inherits from Vec3D)
				FacadePoint p = (FacadePoint) v;
				// use the DisplacementStrategy to compute the displacement
				// amount for the current vertex/point
				float amp = displacement.getDisplacementForPoint(p);
				// now get the actual displaced point
				Vec3D displaced = p.getDisplaced(amp);
				// and add it to the new vertex list
				displacedShape.add(displaced);
				// update bounding box
				bounds3D.growToContainPoint(displaced);
			}
			// add displaced shape to list
			displacedShapes.add(displacedShape);
		}
	}

	/**
	 * Maps all shapes in the {@link #clippedPolies} list onto the 3D profile of
	 * the current spline. Uses the relative X coordinate of the 2D shape
	 * vertices as metric to a related position on the curve and then constructs
	 * the surface in the XY plane.
	 */
	public void computePointsOnSpline() {
		splineShapes = new ArrayList<LineStrip3D>();
		// get a fairly highres & uniformly sampled list of point on the curve
		List<Vec2D> strip = splineEditor.getSpline().toLineStrip2D(20)
				.getDecimatedVertices(1);
		// take a note of their number
		int stripSize = strip.size() - 1;
		// get the strip's centroid
		Vec2D centroid = Rect.getBoundingRect(strip).getCentroid();
		// iterate over all shapes
		for (Polygon2D p : clippedPolies) {
			LineStrip3D s = new LineStrip3D();
			// ..over all vertices in the current shape
			for (Vec2D v : p) {
				// compute relative (normalized) 2D position
				// e.g. if a point was in the top left corner of the particle
				// system it's relPos = {0.0, 0.0}
				// point in bottom-right corner would have relPos = {1.0, 1.0}
				Vec2D relPos = v.scale(particleSys.getBounds().getDimensions()
						.getReciprocal());
				// use relative X position to map to point on curve
				int stripIndex = (int) (relPos.x * stripSize);
				// get point on curve
				Vec2D pointOnCurve = strip.get(stripIndex);
				// get prev point on curve
				Vec2D prevPointOnCurve = strip.get(max(stripIndex - 1, 0));
				// compute tangent
				Vec2D tangent = pointOnCurve.sub(prevPointOnCurve)
						.perpendicular().normalize();
				// transfer 2D tangent into 3D XZ plane
				// the swizzling of coordinates is needed here because the
				// spline itself has a generally vertical orientation (whereas
				// the particle system is more horizontal)
				Vec3D normal = new Vec3D(tangent.y, 0, tangent.x);
				// center 2D curve point around 0,0
				Vec2D t = pointOnCurve.sub(centroid);
				// construct a FacadePoint from to bundle the various metrics
				Vec3D t3d = new FacadePoint(t.y, v.y
						- particleSys.getBounds().height / 2, t.x, normal,
						relPos);
				// add to 3D shape
				s.add(t3d);
			}
			// (re)add first point to close shape
			s.add(s.get(0));
			// add entire shape to list
			splineShapes.add(s);
		}
	}

	@Override
	public void draw() {
		background(100);
		// enable 3D depth testing
		// also see: http://processing.org/reference/hint_.html
		hint(ENABLE_DEPTH_TEST);
		// update zoom factor through linear interpolation, basically:
		// currZoom = currZoom + (targetZoom - currZoom) * 0.15
		currZoom = lerp(currZoom, targetZoom, 0.15f);

		// update physics simulation and particle system state
		particleSys.update();

		// display system based on current user setting
		switch (drawMode) {
		case 0:
			drawShapes2D();
			break;
		case 1:
			drawOutlineShapes3D();
			break;
		case 2:
			drawMesh3D();
			break;
		}

		// then draw 2D facade curve editor without depth testing
		// also see: http://processing.org/reference/hint_.html
		hint(DISABLE_DEPTH_TEST);
		if (drawMode < 2) {
			splineEditor.draw(gfx);
		}
	}

	/**
	 * Applies the current arcball view rotation and draws the iso surface mesh.
	 * The original coordinate system is first saved and then restored again
	 * afterwards. Checks if mesh != null.
	 */
	private void drawMesh3D() {
		if (mesh != null) {
			// backup current coordinate system
			pushMatrix();
			// turn on default lights
			lights();
			// switch to 3D coord system
			translate(width / 2, height / 2, 0);
			// get current view orientation from arc ball controller
			float[] aa = arcBall.getAxisAngle();
			// apply orientation by rotating around the given axis
			rotate(aa[0], aa[1], aa[2], aa[3]);
			// apply zoom factor
			scale(currZoom);
			noStroke();
			fill(255);
			// draw iso surface mesh
			gfx.mesh(mesh);
			noLights();
			// restore previous coordinate system
			popMatrix();
		}
	}

	/**
	 * Applies the current arcball view rotation and draws the contents of
	 * displacedShapes as outlines (the original 2d shapes mapped on spline
	 * profile and applied surface deformation).
	 */
	private void drawOutlineShapes3D() {
		// backup current coordinate system
		pushMatrix();
		// switch to 3D coord system
		translate(width / 2, height / 2, 0);
		// get current view orientation from arc ball controller
		float[] aa = arcBall.getAxisAngle();
		// apply orientation by rotating around the given axis
		rotate(aa[0], aa[1], aa[2], aa[3]);
		// apply zoom factor
		scale(currZoom);
		// draw major axes of coordinate system
		gfx.origin(300);
		// draw all 3d shapes as outlines
		stroke(255);
		for (LineStrip3D s : displacedShapes) {
			gfx.lineStrip3D(s);
		}
		// restore previous coordinate system
		popMatrix();
	}

	/**
	 * Draw 2D particle system and resulting shapes (if enabled by user).
	 */
	private void drawShapes2D() {
		// backup current coordinate system
		pushMatrix();
		// temporarily move origin such that particle system will be centred on
		// screen. offset2d was calculated in initParticleSystem()...
		translate(offset2d.x, offset2d.y);
		// first draw particle system
		particleSys.draw(gfx);
		// check if user enabled shapes (delaunay/voronoi) and if so, show
		// them...
		if (doComputeShapes) {
			computeClippedShapes();
			if (clippedPolies != null) {
				stroke(0, 255, 255);
				for (Polygon2D p : clippedPolies) {
					gfx.polygon2D(p);
				}
			}
		}
		// restore previous coordinate system
		popMatrix();
	}

	/**
	 * Initializes the 3D arcball controller used for updating the 3D view and
	 * allows for natural interactive manipulation of the view orientation. This
	 * controller is only used when the 3D drawing modes are active in this app.
	 * Also, to not interfere with the GUI controller interactions, in this app
	 * the user will need to click & drag with the right mouse button to change
	 * the 3D view orientation.
	 */
	private void initArcball() {
		arcBall = new ArcBall(width / 2, height / 2, MathUtils.min(width,
				height) / 2);
	}

	/**
	 * Initializes the surface displacement strategy used to manipulate
	 * {@link FacadePoint}s in 3D. Currently the only option is:
	 * {@link NoiseDisplacement}
	 */
	private void initDisplacement() {
		displacement = new NoiseDisplacement();
	}

	/**
	 * Called from {@link #setup()}. Initializes all user interface elements.
	 * Makes heavy use of event listeners to dynamically switch between
	 * rendering modes, update data structures (shapes, meshes), but also
	 * dynamically tweak parts of the GUI itself. E.g. some sliders & buttons
	 * are only visible in certain draw modes.
	 */
	private void initGUI() {
		gui = new ControlP5(this);

		Button btClearParticles = gui.addButton("clearParticles", 0, 20, 20,
				100, 20);
		btClearParticles.setLabel("clear particles");
		btClearParticles.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				particleSys.clear();
				radiusSlider.hide();
			}
		});

		Button btAddAttractor = gui.addButton("addAttractor", 0, 20, 50, 100,
				20);
		btAddAttractor.setLabel("add attractor");
		btAddAttractor.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				particleSys.addAttractor();
			}
		});

		Button btAddParticles = gui.addButton("addParticles", 0, 20, 80, 100,
				20);
		btAddParticles.setLabel("add particles");
		btAddParticles.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				particleSys.addParticles(50);
			}
		});

		Toggle btDelaunay = gui.addToggle("doComputeShapes", 20, 110, 20, 20);
		btDelaunay.setLabel("shapes on/off");
		Toggle btVoronoi = gui.addToggle("doUseVoronoi", 140, 110, 20, 20);
		btVoronoi.setLabel("voronoi on/off");

		ControlListener drawModeUpdater = new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				drawMode = e.controller().id();
				if (drawMode > 0) {
					computeClippedShapes();
					computePointsOnSpline();
					computeDisplacedShapes();
					particleSys.deselectAttractor();
					radiusSlider.hide();
					if (drawMode == 1) {
						displaceSlider.show();
						btResetSpline.show();
						btExportSTL.hide();
						voxelSlider.hide();
					} else if (drawMode == 2) {
						voxelizeStructure();
						voxelSlider.show();
						btExportSTL.show();
						btResetSpline.hide();
						displaceSlider.hide();
					}
				} else {
					displaceSlider.hide();
					voxelSlider.hide();
					btExportSTL.hide();
					btResetSpline.hide();
				}
			}
		};
		btDrawMode = gui.addRadioButton("setDrawMode", 20, 180);
		Toggle r = btDrawMode.addItem("2d", 0);
		r.setId(0);
		btDrawMode.activate(0);
		r.addListener(drawModeUpdater);
		r = btDrawMode.addItem("3d wireframe", 1);
		r.setId(1);
		r.addListener(drawModeUpdater);
		r = btDrawMode.addItem("3d mesh", 2);
		r.setId(2);
		r.addListener(drawModeUpdater);

		gui.addSlider("setDrag", 0.0f, 0.1f, particleSys.getDrag(), 220, 20,
				100, 20).setLabel("drag");

		Slider btSeparation = gui.addSlider("setSeparation", 0.0f, 50,
				particleSys.getSeparation(), 220, 50, 100, 20);
		btSeparation.setLabel("separation");
		btSeparation.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				particleSys.setSeparation(e.controller().value());
			}
		});

		radiusSlider = gui.addSlider("setRadius", 50, 200, 420, 20, 100, 20);
		radiusSlider.setLabel("attrator radius");
		radiusSlider.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				particleSys.getSelectedAttractor().setRadius(
						e.controller().value());
			}
		});
		radiusSlider.hide();

		displaceSlider = gui.addSlider("setDisplace", 0, 100, 420, 20, 100, 20);
		displaceSlider.setLabel("surface displacement");
		displaceSlider.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				displacement.setDisplacementStrength(e.controller().value());
				computeDisplacedShapes();
			}
		});
		displaceSlider.hide();

		voxelSlider = gui.addSlider("voxelRes", 32, 192, 420, 20, 100, 20);
		voxelSlider.setLabel("voxel resolution");
		voxelSlider.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				voxelRes = (int) e.controller().value();
				voxelizeStructure();
			}
		});
		voxelSlider.hide();

		btExportSTL = gui.addButton("exportSTL", 0, 420, 50, 100, 20);
		btExportSTL.setLabel("export STL");
		btExportSTL.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent e) {
				mesh.saveAsSTL(sketchPath("facade-" + DateUtils.timeStamp()
						+ ".stl"));
			}
		});
		btExportSTL.hide();

		btResetSpline = gui.addButton("resetSpline", 0, (int) splineEditor
				.getBounds().getLeft() + 20, 20, 100, 20);
		btResetSpline.setLabel("Reset spline");
		btResetSpline.addListener(new ControlListener() {
			@Override
			public void controlEvent(ControlEvent arg0) {
				splineEditor.resetSpline();
			}
		});

		gui.addTextlabel("helpText0",
				"Select & drag attractors with RIGHT mouse button", 20,
				height - 60);
		gui.addTextlabel("helpText1",
				"Hold down SHIFT and drag mouse to rotate 3D view", 20,
				height - 48);
		gui.addTextlabel("helpText2", "Use mouse wheel to zoom in/out", 20,
				height - 36);
	}

	/**
	 * Initializes the particle system physics simulation and configures it to
	 * span 50% of the screen width/height. Also computes the 2D screen offset
	 * of the top-left corner in order to display the system centred on screen.
	 */
	private void initParticleSystem() {
		particleSys = new ParticleSystem(width / 2, height / 2);
		offset2d = new Vec2D(width, height).sub(
				particleSys.getBounds().getDimensions()).scale(0.5f);
	}

	/**
	 * Initializes the 2D spline profile editor in the right hand side of the
	 * window.
	 */
	private void initSplineEditor() {
		splineEditor = new SplineEditor(new Rect(width * 0.8f, 0, width * 0.2f,
				height), 20);
	}

	/**
	 * Attaches a listener for mouse wheel events to manipulate the
	 * {@link #targetZoom} variable used for 3D mode.
	 */
	private void initZoom() {
		// since our app extends PApplet, which extends the standard Java applet
		// it can receive mouse wheel events
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				targetZoom = MathUtils.clip(targetZoom - e.getWheelRotation()
						* 0.01f, 0.1f, 4f);
			}
		});
	}

	/**
	 * Processing event handler hook for reacting to key presses. Here we are
	 * only interested if Shift has been pressed.
	 * 
	 * <a href="http://processing.org/reference/keyCode.html">Processing
	 * reference</a>
	 */
	@Override
	public void keyPressed() {
		if (key == CODED && keyCode == SHIFT) {
			isShiftDown = true;
		}
	}

	@Override
	public void keyReleased() {
		if (key == CODED && keyCode == SHIFT) {
			isShiftDown = false;
		}
	}

	/**
	 * Handler for mouse drag events: First check if we're in 2D mode and right
	 * button is pressed. if so try to update selected attractor (if any). If
	 * Shift is pressed and we're in a 3D mode, update arc ball view
	 * orientation. In all other case check if spline editor can handle the drag
	 * event.
	 */
	@Override
	public void mouseDragged() {
		Vec2D mousePos = new Vec2D(mouseX, mouseY);
		if (mouseButton == RIGHT) {
			if (drawMode == 0) {
				mousePos.subSelf(offset2d);
				particleSys.moveSelectedAttractor(mousePos);
			}
		} else if (isShiftDown) {
			if (drawMode > 0) {
				arcBall.mouseDragged(mousePos);
			}
		} else if (splineEditor.mouseDragged(mousePos)) {
			if (drawMode > 0) {
				computePointsOnSpline();
				computeDisplacedShapes();
			}
		}
	}

	/**
	 * Handler for mouse press events: First check if we're in 2D mode and right
	 * button is pressed. if so try to update selected attractor (if any). If
	 * Shift is pressed and we're in a 3D mode, update arc ball view
	 * orientation. In all other case check if spline editor can handle the drag
	 * event.
	 */
	@Override
	public void mousePressed() {
		Vec2D mousePos = new Vec2D(mouseX, mouseY);
		if (splineEditor.mousePressed(mousePos)) {
			if (drawMode == 1) {
				computePointsOnSpline();
				computeDisplacedShapes();
			}
		} else {
			if (mouseButton == RIGHT) {
				mousePos.subSelf(offset2d);
				particleSys.selectAttractorNearPosition(mousePos);
				if (particleSys.hasSelectedAttractor()) {
					radiusSlider.setValue(particleSys.getSelectedAttractor()
							.getRadius());
					radiusSlider.show();
				} else {
					radiusSlider.hide();
				}
			} else if (isShiftDown) {
				arcBall.mousePressed(mousePos);
			}
		}
	}

	/**
	 * Notifies spline editor and arc ball controller about mouse button
	 * released.
	 */
	@Override
	public void mouseReleased() {
		splineEditor.mouseReleased();
		arcBall.mouseReleased();
	}

	/**
	 * Updates the drag setting/force damping in the particle system's physics
	 * instance.
	 * 
	 * @param newDrag
	 */
	public void setDrag(float newDrag) {
		particleSys.setDrag(newDrag);
	}

	@Override
	public void setup() {
		size(1280, 720, OPENGL);
		// we want to draw circles by specifying their radius (default is
		// diameter)
		ellipseMode(RADIUS);
		// associate toxiclibs render helper with this app
		gfx = new ToxiclibsSupport(this);

		initParticleSystem();
		initSplineEditor();
		initDisplacement();
		initArcball();
		initGUI();
		initZoom();
	}

	/**
	 * Voxelizes the current contents of the 3D displaced shapes list and
	 * constructs an iso surface mesh from the voxel structure.
	 */
	public void voxelizeStructure() {
		// create empty container for iso surface mesh
		mesh = new WETriangleMesh();
		// get the extent of the 3d bounding box enclosing
		// all displaced facade points
		Vec3D extent = bounds3D.getExtent();
		// figure out which axis is the longest/largest
		float maxAxis = max(extent.x, extent.y, extent.z);
		// scale voxel resolution per axis in relation to major axis
		int resX = (int) (extent.x / maxAxis * voxelRes);
		int resY = (int) (extent.y / maxAxis * voxelRes);
		int resZ = (int) (extent.z / maxAxis * voxelRes);
		// create a new mesh lattice builder utility configured
		// to match the current physical size of the facade and voxel resolution
		MeshLatticeBuilder builder = new MeshLatticeBuilder(extent.scale(2),
				resX, resY, resZ, new FloatRange(1, 1));
		// use a slightly enlarged bounding box as range for input coordinates
		// it needs to be slightly larger to avoid clipping/thinning of the
		// voxel structure
		// at the sides of the volume
		builder.setInputBounds(new AABB(bounds3D, extent.scale(1.1f)));
		// ask the builder for the underlying volumetric/voxel space data
		// structure
		VolumetricSpace volume = builder.getVolume();
		// create a volumetric brush associated with this volume and using a
		// small brush size
		VolumetricBrush brush = new BoxBrush(volume, 0.33f);
		// set the brush mode so that lower density values don't overwrite
		// existing higher ones
		brush.setMode(VolumetricBrush.MODE_PEAK);
		// now iterate over all shapes and segments within each shape
		for (LineStrip3D shape : displacedShapes) {
			for (Line3D segment : shape.getSegments()) {
				// use the builder class to represent the current line segment
				// as voxels by sweeping the brush along the line at the given
				// step distance (1 unit)
				builder.createLattice(brush, segment, 1);
			}
		}
		// finally ensure the volume will be water tight
		volume.closeSides();
		// create an iso surface for the volume and threshold value
		// and turn it into a triangle mesh
		new HashIsoSurface(volume).computeSurfaceMesh(mesh, 0.66f);
		// center the mesh around the world origin (0,0,0)
		mesh.center(new Vec3D(0, 0, 0));
		// apply 2 iterations of the laplacian smooth filter to average
		// neighboring mesh vertices and so reduce voxel aliasing
		new LaplacianSmooth().filter(mesh, 2);
	}
}
