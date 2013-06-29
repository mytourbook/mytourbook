/**
 * Source: <a href="http://forum.worldwindcentral.com/showthread.php?t=22210">http://forum.worldwindcentral.com/showthread.php?t=22210</a>
 * 
 * Date: 28.6.2013<p>
 * 
 * package gov.nasa.worldwind.render;
 */
package net.tourbook.map3;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.Restorable;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Cylinder;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Quaternion;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.util.measure.LengthMeasurer;

import java.awt.Color;
import java.util.ArrayList;

import javax.media.opengl.GL;

public class PolylineWithDisplayList implements Renderable, Movable, Restorable {

	public final static int		GREAT_CIRCLE			= 0;
	public final static int		LINEAR					= 1;
	public final static int		RHUMB_LINE				= 2;
	public final static int		LOXODROME				= RHUMB_LINE;

	public final static int		ANTIALIAS_DONT_CARE		= GL.GL_DONT_CARE;
	public final static int		ANTIALIAS_FASTEST		= GL.GL_FASTEST;
	public final static int		ANTIALIAS_NICEST		= GL.GL_NICEST;

	private ArrayList<Position>	positions;
	private Vec4				referenceCenterPoint;
	private Position			referenceCenterPosition	= Position.ZERO;
	private int					antiAliasHint			= GL.GL_FASTEST;
	private Color				color					= Color.WHITE;
	private double				lineWidth				= 1;
	private boolean				filled					= false;						// makes it a polygon
	private boolean				closed					= false;						// connect last point to first
	private boolean				followTerrain			= false;
	private double				offset					= 0;
	private double				terrainConformance		= 10;
	private int					pathType				= GREAT_CIRCLE;
	private short				stipplePattern			= (short) 0xAAAA;
	private int					stippleFactor			= 0;
	private Globe				globe;
	private int					numSubsegments			= 10;
	private boolean				highlighted				= false;
	private Color				highlightColor			= new Color(1f, 1f, 1f, 0.5f);
	private LengthMeasurer		measurer				= new LengthMeasurer();

	protected boolean			reset					= true;
	protected int				displayListIndex		= -1;
	protected int				displayListPickingIndex	= -1;

	private long				geomGenFrameTime		= -Long.MAX_VALUE;

	public PolylineWithDisplayList() {
		this.setPositions(null);
		this.measurer.setFollowTerrain(this.followTerrain);
		this.measurer.setPathType(this.pathType);
	}

	public PolylineWithDisplayList(final Iterable<? extends LatLon> positions, final double elevation) {
		this.setPositions(positions, elevation);
		this.measurer.setFollowTerrain(this.followTerrain);
		this.measurer.setPathType(this.pathType);
	}

	public PolylineWithDisplayList(final Iterable<? extends Position> positions) {
		this.setPositions(positions);
		this.measurer.setFollowTerrain(this.followTerrain);
		this.measurer.setPathType(this.pathType);
	}

	private ArrayList<Vec4> addPointToSpan(final Vec4 p, ArrayList<Vec4> span) {
		if (span == null) {
			span = new ArrayList<Vec4>();
		}

		span.add(p.subtract3(this.referenceCenterPoint));

		return span;
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	private ArrayList<Vec4> clipAndAdd(final DrawContext dc, final Vec4 ptA, final Vec4 ptB, ArrayList<Vec4> span) {
		// Line clipping appears to be useful only for long lines with few
		// segments. It's costly otherwise.
		// TODO: Investigate trade-off of line clipping.
		// if (Line.clipToFrustum(ptA, ptB,
		// dc.getView().getFrustumInModelCoordinates()) == null)
		// {
		// if (span != null)
		// {
		// this.addSpan(span);
		// span = null;
		// }
		// return span;
		// }

		if (span == null) {
			span = this.addPointToSpan(ptA, span);
		}

		return this.addPointToSpan(ptB, span);
	}

	private Vec4 computePoint(final DrawContext dc, final Position pos, final boolean applyOffset) {
		if (this.followTerrain) {
			final double height = !applyOffset ? 0 : this.offset;
			return this.computeTerrainPoint(dc, pos.getLatitude(), pos.getLongitude(), height);
		} else {
			final double height = pos.getElevation() + (applyOffset ? this.offset : 0);
			return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(), height);
		}
	}

	private void computeReferenceCenter(final DrawContext dc) {
		if (this.positions.size() < 1) {
			return;
		}

		if (this.positions.size() < 3) {
			this.referenceCenterPosition = this.positions.get(0);
		} else {
			this.referenceCenterPosition = this.positions.get(this.positions.size() / 2);
		}

		this.referenceCenterPoint = this.computeTerrainPoint(
				dc,
				this.referenceCenterPosition.getLatitude(),
				this.referenceCenterPosition.getLongitude(),
				this.offset);
	}

	private double computeSegmentLength(final DrawContext dc, final Position posA, final Position posB) {
		final LatLon llA = new LatLon(posA.getLatitude(), posA.getLongitude());
		final LatLon llB = new LatLon(posB.getLatitude(), posB.getLongitude());

		final Angle ang = LatLon.greatCircleDistance(llA, llB);

		if (this.followTerrain) {
			return ang.radians * (dc.getGlobe().getRadius() + this.offset);
		} else {
			final double height = this.offset + 0.5 * (posA.getElevation() + posB.getElevation());
			return ang.radians * (dc.getGlobe().getRadius() + height);
		}
	}

	private Vec4 computeTerrainPoint(final DrawContext dc, final Angle lat, final Angle lon, final double offset) {
		Vec4 p = dc.getSurfaceGeometry().getSurfacePoint(lat, lon, offset);

		if (p == null) {
			p = dc.getGlobe().computePointFromPosition(
					lat,
					lon,
					offset + dc.getGlobe().getElevation(lat, lon) * dc.getVerticalExaggeration());
		}

		return p;
	}

	public int getAntiAliasHint() {
		return antiAliasHint;
	}

	public Color getColor() {
		return color;
	}

	public Color getHighlightColor() {
		return this.highlightColor;
	}

	/**
	 * Returns the length of the line as drawn. If the path follows the terrain, the length returned
	 * is the distance one would travel if on the surface. If the path does not follow the terrain,
	 * the length returned is the distance along the full length of the path at the path's
	 * elevations and current path type.
	 * 
	 * @return the path's length in meters.
	 */
	public double getLength() {
		return this.measurer.getLength(this.globe);
	}

	public double getLineWidth() {
		return lineWidth;
	}

	public LengthMeasurer getMeasurer() {
		return this.measurer;
	}

	public int getNumSubsegments() {
		return numSubsegments;
	}

	public double getOffset() {
		return offset;
	}

	public int getPathType() {
		return pathType;
	}

	public Iterable<Position> getPositions() {
		return this.positions;
	}

	public Position getReferencePosition() {
		return this.referenceCenterPosition;
	}

	/**
	 * Returns an XML state document String describing the public attributes of this Polyline.
	 * 
	 * @return XML state document string describing this Polyline.
	 */
	public String getRestorableState() {
		final RestorableSupport restorableSupport = RestorableSupport.newRestorableSupport();
		// Creating a new RestorableSupport failed. RestorableSupport logged the
		// problem, so just return null.
		if (restorableSupport == null) {
			return null;
		}

		if (this.color != null) {
			final String encodedColor = RestorableSupport.encodeColor(this.color);
			if (encodedColor != null) {
				restorableSupport.addStateValueAsString("color", encodedColor);
			}
		}

		if (this.highlightColor != null) {
			final String encodedColor = RestorableSupport.encodeColor(this.highlightColor);
			if (encodedColor != null) {
				restorableSupport.addStateValueAsString("highlightColor", encodedColor);
			}
		}

		if (this.positions != null) {
			// Create the base "positions" state object.
			final RestorableSupport.StateObject positionsStateObj = restorableSupport.addStateObject("positions");
			if (positionsStateObj != null) {
				for (final Position p : this.positions) {
					// Save each position only if all parts (latitude,
					// longitude, and elevation) can be
					// saved. We will not save a partial iconPosition (for
					// example, just the elevation).
					if (p != null && p.getLatitude() != null && p.getLongitude() != null) {
						// Create a nested "position" element underneath the
						// base "positions".
						final RestorableSupport.StateObject pStateObj = restorableSupport.addStateObject(
								positionsStateObj,
								"position");
						if (pStateObj != null) {
							restorableSupport.addStateValueAsDouble(
									pStateObj,
									"latitudeDegrees",
									p.getLatitude().degrees);
							restorableSupport.addStateValueAsDouble(
									pStateObj,
									"longitudeDegrees",
									p.getLongitude().degrees);
							restorableSupport.addStateValueAsDouble(pStateObj, "elevation", p.getElevation());
						}
					}
				}
			}
		}

		restorableSupport.addStateValueAsInteger("antiAliasHint", this.antiAliasHint);
		restorableSupport.addStateValueAsBoolean("filled", this.filled);
		restorableSupport.addStateValueAsBoolean("closed", this.closed);
		restorableSupport.addStateValueAsBoolean("highlighted", this.highlighted);
		restorableSupport.addStateValueAsInteger("pathType", this.pathType);
		restorableSupport.addStateValueAsBoolean("followTerrain", this.followTerrain);
		restorableSupport.addStateValueAsDouble("offset", this.offset);
		restorableSupport.addStateValueAsDouble("terrainConformance", this.terrainConformance);
		restorableSupport.addStateValueAsDouble("lineWidth", this.lineWidth);
		restorableSupport.addStateValueAsInteger("stipplePattern", this.stipplePattern);
		restorableSupport.addStateValueAsInteger("stippleFactor", this.stippleFactor);
		restorableSupport.addStateValueAsInteger("numSubsegments", this.numSubsegments);

		return restorableSupport.getStateAsXml();
	}

	public int getStippleFactor() {
		return stippleFactor;
	}

	public short getStipplePattern() {
		return stipplePattern;
	}

	public double getTerrainConformance() {
		return terrainConformance;
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isFilled() {
		return filled;
	}

	public boolean isFollowTerrain() {
		return followTerrain;
	}

	public boolean isHighlighted() {
		return highlighted;
	}

	private boolean isSegmentVisible(	final DrawContext dc,
										final Position posA,
										final Position posB,
										final Vec4 ptA,
										final Vec4 ptB) {
		final Frustum f = dc.getView().getFrustumInModelCoordinates();

		if (f.contains(ptA)) {
			return true;
		}

		if (f.contains(ptB)) {
			return true;
		}

		if (ptA.equals(ptB)) {
			return false;
		}

		final Position posC = Position.interpolate(0.5, posA, posB);
		final Vec4 ptC = this.computePoint(dc, posC, true);
		if (f.contains(ptC)) {
			return true;
		}

		// TODO: Find a more efficient bounding geometry for this frustum
		// intersection test.
		final double r = Line.distanceToSegment(ptA, ptB, ptC);
		final Cylinder cyl = new Cylinder(ptA, ptB, r == 0 ? 1 : r);
		return cyl.intersects(dc.getView().getFrustumInModelCoordinates());
	}

	private ArrayList<Vec4> makeSegment(final DrawContext dc,
										final Position posA,
										final Position posB,
										Vec4 ptA,
										Vec4 ptB) {
		ArrayList<Vec4> span = null;

		final double arcLength = this.computeSegmentLength(dc, posA, posB);
		if (arcLength <= 0) // points differing only in altitude
		{
			span = this.addPointToSpan(ptA, span);
			if (!ptA.equals(ptB)) {
				span = this.addPointToSpan(ptB, span);
			}
			return span;
		}
		// Variables for great circle and rhumb computation.
		Angle segmentAzimuth = null;
		Angle segmentDistance = null;

		for (double s = 0, p = 0; s < 1;) {
			if (this.followTerrain) {
				p += this.terrainConformance
						* dc.getView().computePixelSizeAtDistance(ptA.distanceTo3(dc.getView().getEyePoint()));
			} else {
				p += arcLength / this.numSubsegments;
			}

			s = p / arcLength;

			Position pos;
			if (s >= 1) {
				pos = posB;
			} else if (this.pathType == LINEAR) {
				pos = Position.interpolate(s, posA, posB);
			} else if (this.pathType == RHUMB_LINE) // or LOXODROME
			{
				if (segmentAzimuth == null) {
					segmentAzimuth = LatLon.rhumbAzimuth(posA, posB);
					segmentDistance = LatLon.rhumbDistance(posA, posB);
				}
				final Angle distance = Angle.fromRadians(s * segmentDistance.radians);
				final LatLon latLon = LatLon.rhumbEndPosition(posA, segmentAzimuth, distance);
				pos = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
			} else // GREAT_CIRCLE
			{
				if (segmentAzimuth == null) {
					segmentAzimuth = LatLon.greatCircleAzimuth(posA, posB);
					segmentDistance = LatLon.greatCircleDistance(posA, posB);
				}
				final Angle distance = Angle.fromRadians(s * segmentDistance.radians);
				final LatLon latLon = LatLon.greatCircleEndPosition(posA, segmentAzimuth, distance);
				pos = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
			}

			ptB = this.computePoint(dc, pos, true);
			span = this.clipAndAdd(dc, ptA, ptB, span);

			ptA = ptB;
		}

		return span;
	}

	protected void makeVertices(final DrawContext dc) {
		reset = false;

		ArrayList<ArrayList<Vec4>> currentSpans = null;

		if (currentSpans == null) {
			currentSpans = new ArrayList<ArrayList<Vec4>>();
		} else {
			currentSpans.clear();
		}

		if (this.positions.size() < 1) {
			return;
		}

		Position posA = this.positions.get(0);
		Vec4 ptA = this.computePoint(dc, posA, true);
		for (int i = 1; i <= this.positions.size(); i++) {
			Position posB;
			if (i < this.positions.size()) {
				posB = this.positions.get(i);
			} else if (this.closed) {
				posB = this.positions.get(0);
			} else {
				break;
			}

			final Vec4 ptB = this.computePoint(dc, posB, true);

			if (this.followTerrain && !this.isSegmentVisible(dc, posA, posB, ptA, ptB)) {
				posA = posB;
				ptA = ptB;
				continue;
			}

			ArrayList<Vec4> span;
			span = this.makeSegment(dc, posA, posB, ptA, ptB);

			if (span != null) {
				if (span != null && span.size() > 0) {
					currentSpans.add(span);
				}
			}

			posA = posB;
			ptA = ptB;
		}

		// Fill doesn't work in 3D world
		setFilled(false);
		this.globe = dc.getGlobe();

		final GL gl = dc.getGL();

		// Create display list
		if (displayListIndex != -1) {
			gl.glDeleteLists(displayListIndex, 1);
			gl.glDeleteLists(displayListPickingIndex, 1);
		}

		displayListIndex = gl.glGenLists(1);

		gl.glNewList(displayListIndex, GL.GL_COMPILE);

		int attrBits = GL.GL_HINT_BIT | GL.GL_CURRENT_BIT | GL.GL_LINE_BIT;
		if (this.color.getAlpha() != 255) {
			attrBits |= GL.GL_COLOR_BUFFER_BIT;
		}

		gl.glPushAttrib(attrBits);

		try {
			if (this.color.getAlpha() != 255) {
				gl.glEnable(GL.GL_BLEND);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			}
			dc.getGL().glColor4ub(
					(byte) this.color.getRed(),
					(byte) this.color.getGreen(),
					(byte) this.color.getBlue(),
					(byte) this.color.getAlpha());

			if (this.stippleFactor > 0) {
				gl.glEnable(GL.GL_LINE_STIPPLE);
				gl.glLineStipple(this.stippleFactor, this.stipplePattern);
			} else {
				gl.glDisable(GL.GL_LINE_STIPPLE);
			}

			int hintAttr = GL.GL_LINE_SMOOTH_HINT;
			if (this.filled) {
				hintAttr = GL.GL_POLYGON_SMOOTH_HINT;
			}
			gl.glHint(hintAttr, this.antiAliasHint);

			int primType = GL.GL_LINE_STRIP;
			if (this.filled) {
				primType = GL.GL_POLYGON;
			}

			gl.glLineWidth((float) this.lineWidth);

			if (this.followTerrain) {
				this.pushOffest(dc);
			}

			for (final ArrayList<Vec4> span : currentSpans) {
				if (span == null) {
					continue;
				}

				// Since segements can very often be very short -- two vertices
				// -- use explicit rendering. The
				// overhead of batched rendering, e.g., gl.glDrawArrays, is too
				// high because it requires copying
				// the vertices into a DoubleBuffer, and DoubleBuffer creation
				// and access performs relatively poorly.
				gl.glBegin(primType);
				for (final Vec4 p : span) {
					gl.glVertex3d(p.x, p.y, p.z);
				}
				gl.glEnd();
			}

			if (this.highlighted) {
				if (this.highlightColor.getAlpha() != 255) {
					gl.glEnable(GL.GL_BLEND);
					gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				}
				dc.getGL().glColor4ub(
						(byte) this.highlightColor.getRed(),
						(byte) this.highlightColor.getGreen(),
						(byte) this.highlightColor.getBlue(),
						(byte) this.highlightColor.getAlpha());

				gl.glLineWidth((float) this.lineWidth + 2);
				for (final ArrayList<Vec4> span : currentSpans) {
					if (span == null) {
						continue;
					}

					gl.glBegin(primType);
					for (final Vec4 p : span) {
						gl.glVertex3d(p.x, p.y, p.z);
					}
					gl.glEnd();
				}
			}

			if (this.followTerrain) {
				this.popOffest(dc);
			}
		} finally {
			gl.glPopAttrib();
			gl.glEndList();
		}

		displayListPickingIndex = gl.glGenLists(1);

		gl.glNewList(displayListPickingIndex, GL.GL_COMPILE);
		gl.glPushAttrib(GL.GL_HINT_BIT | GL.GL_CURRENT_BIT | GL.GL_LINE_BIT);

		try {

			if (this.stippleFactor > 0) {
				gl.glEnable(GL.GL_LINE_STIPPLE);
				gl.glLineStipple(this.stippleFactor, this.stipplePattern);
			} else {
				gl.glDisable(GL.GL_LINE_STIPPLE);
			}

			int hintAttr = GL.GL_LINE_SMOOTH_HINT;
			if (this.filled) {
				hintAttr = GL.GL_POLYGON_SMOOTH_HINT;
			}
			gl.glHint(hintAttr, this.antiAliasHint);

			int primType = GL.GL_LINE_STRIP;
			if (this.filled) {
				primType = GL.GL_POLYGON;
			}

			gl.glLineWidth((float) this.lineWidth + 8);

			if (this.followTerrain) {
				this.pushOffest(dc);
			}

			for (final ArrayList<Vec4> span : currentSpans) {
				if (span == null) {
					continue;
				}

				// Since segements can very often be very short -- two vertices
				// -- use explicit rendering. The
				// overhead of batched rendering, e.g., gl.glDrawArrays, is too
				// high because it requires copying
				// the vertices into a DoubleBuffer, and DoubleBuffer creation
				// and access performs relatively poorly.
				gl.glBegin(primType);
				for (final Vec4 p : span) {
					gl.glVertex3d(p.x, p.y, p.z);
				}
				gl.glEnd();
			}

			if (this.followTerrain) {
				this.popOffest(dc);
			}
		} finally {
			gl.glPopAttrib();
			gl.glEndList();
		}
	}

	public void move(final Position delta) {
		if (delta == null) {
			final String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.moveTo(this.getReferencePosition().add(delta));
	}

	public void moveTo(final Position position) {
		if (position == null) {
			final String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.reset();

		if (this.positions.size() < 1) {
			return;
		}

		final Vec4 origRef = this.referenceCenterPoint;
		final Vec4 newRef = this.globe.computePointFromPosition(position);
		final Angle distance = LatLon.greatCircleDistance(this.referenceCenterPosition, position);
		final Vec4 axis = origRef.cross3(newRef).normalize3();
		final Quaternion q = Quaternion.fromAxisAngle(distance, axis);

		for (int i = 0; i < this.positions.size(); i++) {
			Position pos = this.positions.get(i);
			Vec4 p = this.globe.computePointFromPosition(pos);
			p = p.transformBy3(q);
			pos = this.globe.computePositionFromPoint(p);
			this.positions.set(i, pos);
		}
	}

	private void popOffest(final DrawContext dc) {
		final GL gl = dc.getGL();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	private void pushOffest(final DrawContext dc) {
		// Modify the projection transform to shift the depth values slightly
		// toward the camera in order to
		// ensure the lines are selected during depth buffering.
		final GL gl = dc.getGL();

		final float[] pm = new float[16];
		gl.glGetFloatv(GL.GL_PROJECTION_MATRIX, pm, 0);
		pm[10] *= 0.99; // TODO: See Lengyel 2 ed. Section 9.1.2 to compute
		// optimal/minimal offset

		gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadMatrixf(pm, 0);
	}

	public void render(final DrawContext dc) {
		if (dc == null) {
			final String message = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.logger().severe(message);
			throw new IllegalStateException(message);
		}

		this.globe = dc.getGlobe();

		if (this.positions.size() < 2) {
			return;
		}

		// vertices potentially computed every frame to follow terrain changes
		if (reset == true || (this.followTerrain && this.geomGenFrameTime != dc.getFrameTimeStamp())) {
			// Reference center must be computed prior to computing vertices.
			this.computeReferenceCenter(dc);
			this.makeVertices(dc);
			this.geomGenFrameTime = dc.getFrameTimeStamp();
		}

		dc.getView().pushReferenceCenter(dc, this.referenceCenterPoint);

		if (dc.isPickingMode()) {
			dc.getGL().glCallList(displayListPickingIndex);
		} else {
			dc.getGL().glCallList(displayListIndex);
		}

		dc.getView().popReferenceCenter(dc);
	}

	private void reset() {
		reset = true;
//		if (this.currentSpans != null)
//			this.currentSpans.clear();
//		this.currentSpans = null;
	}

	/**
	 * Restores publicly settable attribute values found in the specified XML state document String.
	 * The document specified by <code>stateInXml</code> must be a well formed XML document String,
	 * or this will throw an IllegalArgumentException. Unknown structures in <code>stateInXml</code>
	 * are benign, because they will simply be ignored.
	 * 
	 * @param stateInXml
	 *            an XML document String describing a Polyline.
	 * @throws IllegalArgumentException
	 *             If <code>stateInXml</code> is null, or if <code>stateInXml</code> is not a well
	 *             formed XML document String.
	 */
	public void restoreState(final String stateInXml) {
		if (stateInXml == null) {
			final String message = Logging.getMessage("nullValue.StringIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		RestorableSupport restorableSupport;
		try {
			restorableSupport = RestorableSupport.parse(stateInXml);
		} catch (final Exception e) {
			// Parsing the document specified by stateInXml failed.
			final String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message, e);
		}

		String colorState = restorableSupport.getStateValueAsString("color");
		if (colorState != null) {
			final Color color = RestorableSupport.decodeColor(colorState);
			if (color != null) {
				setColor(color);
			}
		}

		colorState = restorableSupport.getStateValueAsString("highlightColor");
		if (colorState != null) {
			final Color color = RestorableSupport.decodeColor(colorState);
			if (color != null) {
				setHighlightColor(color);
			}
		}

		// Get the base "positions" state object.
		final RestorableSupport.StateObject positionsStateObj = restorableSupport.getStateObject("positions");
		if (positionsStateObj != null) {
			final ArrayList<Position> newPositions = new ArrayList<Position>();
			// Get the nested "position" states beneath the base "positions".
			final RestorableSupport.StateObject[] positionStateArray = restorableSupport.getAllStateObjects(
					positionsStateObj,
					"position");
			if (positionStateArray != null && positionStateArray.length != 0) {
				for (final RestorableSupport.StateObject pStateObj : positionStateArray) {
					if (pStateObj != null) {
						// Restore each position only if all parts are
						// available.
						// We will not restore a partial position (for example,
						// just the elevation).
						final Double latitudeState = restorableSupport.getStateValueAsDouble(
								pStateObj,
								"latitudeDegrees");
						final Double longitudeState = restorableSupport.getStateValueAsDouble(
								pStateObj,
								"longitudeDegrees");
						final Double elevationState = restorableSupport.getStateValueAsDouble(pStateObj, "elevation");
						if (latitudeState != null && longitudeState != null && elevationState != null) {
							newPositions.add(Position.fromDegrees(latitudeState, longitudeState, elevationState));
						}
					}
				}
			}

			// Even if there are no actual positions specified, we set positions
			// as an empty list.
			// An empty set of positions is still a valid state.
			setPositions(newPositions);
		}

		final Integer antiAliasHintState = restorableSupport.getStateValueAsInteger("antiAliasHint");
		if (antiAliasHintState != null) {
			setAntiAliasHint(antiAliasHintState);
		}

		final Boolean isFilledState = restorableSupport.getStateValueAsBoolean("filled");
		if (isFilledState != null) {
			setFilled(isFilledState);
		}

		final Boolean isClosedState = restorableSupport.getStateValueAsBoolean("closed");
		if (isClosedState != null) {
			setClosed(isClosedState);
		}

		final Boolean isHighlightedState = restorableSupport.getStateValueAsBoolean("highlighted");
		if (isHighlightedState != null) {
			setHighlighted(isHighlightedState);
		}

		final Integer pathTypeState = restorableSupport.getStateValueAsInteger("pathType");
		if (pathTypeState != null) {
			setPathType(pathTypeState);
		}

		final Boolean isFollowTerrainState = restorableSupport.getStateValueAsBoolean("followTerrain");
		if (isFollowTerrainState != null) {
			setFollowTerrain(isFollowTerrainState);
		}

		final Double offsetState = restorableSupport.getStateValueAsDouble("offset");
		if (offsetState != null) {
			setOffset(offsetState);
		}

		final Double terrainConformanceState = restorableSupport.getStateValueAsDouble("terrainConformance");
		if (terrainConformanceState != null) {
			setTerrainConformance(terrainConformanceState);
		}

		final Double lineWidthState = restorableSupport.getStateValueAsDouble("lineWidth");
		if (lineWidthState != null) {
			setLineWidth(lineWidthState);
		}

		final Integer stipplePatternState = restorableSupport.getStateValueAsInteger("stipplePattern");
		if (stipplePatternState != null) {
			setStipplePattern(stipplePatternState.shortValue());
		}

		final Integer stippleFactorState = restorableSupport.getStateValueAsInteger("stippleFactor");
		if (stippleFactorState != null) {
			setStippleFactor(stippleFactorState);
		}

		final Integer numSubsegmentsState = restorableSupport.getStateValueAsInteger("numSubsegments");
		if (numSubsegmentsState != null) {
			setNumSubsegments(numSubsegmentsState);
		}
	}

	public void setAntiAliasHint(final int hint) {
		if (!(hint == ANTIALIAS_DONT_CARE || hint == ANTIALIAS_FASTEST || hint == ANTIALIAS_NICEST)) {
			final String msg = Logging.getMessage("generic.InvalidHint");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.antiAliasHint = hint;
	}

	public void setClosed(final boolean closed) {
		this.closed = closed;
	}

	public void setColor(final Color color) {
		if (color == null) {
			final String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.color = color;
	}

	public void setFilled(final boolean filled) {
		this.filled = filled;
	}

	/**
	 * Indicates whether the path should follow the terrain's surface. If the value is
	 * <code>true</code>, the elevation values in this path's positions are ignored and the path is
	 * drawn on the terrain surface. Otherwise the path is drawn according to the elevations given
	 * in the path's positions. If following the terrain, the path may also have an offset. See
	 * {@link #setOffset(double)};
	 * 
	 * @param followTerrain
	 *            <code>true</code> to follow the terrain, otherwise <code>false</code>.
	 */
	public void setFollowTerrain(final boolean followTerrain) {
		this.reset();
		this.followTerrain = followTerrain;
		this.measurer.setFollowTerrain(followTerrain);
	}

	public void setHighlightColor(final Color highlightColor) {
		if (highlightColor == null) {
			final String message = Logging.getMessage("nullValue.ColorIsNull");
			Logging.logger().severe(message);
			throw new IllegalStateException(message);
		}

		this.highlightColor = highlightColor;
	}

	public void setHighlighted(final boolean highlighted) {
		this.highlighted = highlighted;
	}

	public void setLineWidth(final double lineWidth) {
		this.lineWidth = lineWidth;
	}

	/**
	 * Specifies the number of intermediate segments to draw for each segment between positions. The
	 * end points of the intermediate segments are calculated according to the current path type and
	 * follow-terrain setting.
	 * 
	 * @param numSubsegments
	 *            the number of intermediate subsegments.
	 */
	public void setNumSubsegments(final int numSubsegments) {
		this.reset();
		this.numSubsegments = numSubsegments;
	}

	/**
	 * Specifies an offset, in meters, to add to the path points when the path's follow-terrain
	 * attribute is true. See {@link #setFollowTerrain(boolean)}.
	 * 
	 * @param offset
	 *            the path pffset in meters.
	 */
	public void setOffset(final double offset) {
		this.reset();
		this.offset = offset;
	}

	/**
	 * Sets the type of path to draw, one of {@link #GREAT_CIRCLE}, which draws each segment of the
	 * path as a great circle, {@link #LINEAR}, which determines the intermediate positions between
	 * segments by interpolating the segment endpoints, or {@link #RHUMB_LINE}, which draws each
	 * segment of the path as a line of constant heading.
	 * 
	 * @param pathType
	 *            the type of path to draw.
	 */
	public void setPathType(final int pathType) {
		this.reset();
		this.pathType = pathType;
		this.measurer.setPathType(pathType);
	}

	/**
	 * Sets the paths positions as latitude and longitude values at a constant altitude.
	 * 
	 * @param inPositions
	 *            the latitudes and longitudes of the positions.
	 * @param elevation
	 *            the elevation to assign each position.
	 */
	public void setPositions(final Iterable<? extends LatLon> inPositions, final double elevation) {
		this.reset();
		this.positions = new ArrayList<Position>();
		if (inPositions != null) {
			for (final LatLon position : inPositions) {
				this.positions.add(new Position(position, elevation));
			}
			this.measurer.setPositions(this.positions);
		}

		if (this.filled && this.positions.size() < 3) {
			final String msg = Logging.getMessage("generic.InsufficientPositions");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}
	}

	/**
	 * Specifies the path's positions.
	 * 
	 * @param inPositions
	 *            the path positions.
	 */
	public void setPositions(final Iterable<? extends Position> inPositions) {
		this.reset();
		this.positions = new ArrayList<Position>();
		if (inPositions != null) {
			for (final Position position : inPositions) {
				this.positions.add(position);
			}
			this.measurer.setPositions(this.positions);
		}

		if ((this.filled && this.positions.size() < 3)) {
			final String msg = Logging.getMessage("generic.InsufficientPositions");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}
	}

	/**
	 * Sets the stipple factor for specifying line types other than solid. See the OpenGL
	 * specification or programming guides for a description of this parameter. Stipple is also
	 * affected by the path's stipple pattern, {@link #setStipplePattern(short)}.
	 * 
	 * @param stippleFactor
	 *            the stipple factor.
	 */
	public void setStippleFactor(final int stippleFactor) {
		this.stippleFactor = stippleFactor;
	}

	/**
	 * Sets the stipple pattern for specifying line types other than solid. See the OpenGL
	 * specification or programming guides for a description of this parameter. Stipple is also
	 * affected by the path's stipple factor, {@link #setStippleFactor(int)}.
	 * 
	 * @param stipplePattern
	 *            the stipple pattern.
	 */
	public void setStipplePattern(final short stipplePattern) {
		this.stipplePattern = stipplePattern;
	}

	/**
	 * Specifies the precision to which the path follows the terrain when the follow-terrain
	 * attribute is true. The conformance value indicates the approximate length of each sub-segment
	 * of the path as it's drawn, in pixels. Lower values specify higher precision, but at the cost
	 * of performance.
	 * 
	 * @param terrainConformance
	 *            the path conformance in pixels.
	 */
	public void setTerrainConformance(final double terrainConformance) {
		this.terrainConformance = terrainConformance;
	}

}
