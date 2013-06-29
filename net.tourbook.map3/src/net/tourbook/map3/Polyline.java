/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package net.tourbook.map3;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.Restorable;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Cylinder;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.MeasurableLength;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.util.measure.LengthMeasurer;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

/**
 * @author tag
 * @version $Id: Polyline.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Polyline extends AVListImpl implements Renderable, OrderedRenderable, Movable, Restorable,
		MeasurableLength, ExtentHolder {

	public final static int					GREAT_CIRCLE		= WorldWind.GREAT_CIRCLE;
	public final static int					LINEAR				= WorldWind.LINEAR;
	public final static int					RHUMB_LINE			= WorldWind.RHUMB_LINE;
	public final static int					LOXODROME			= RHUMB_LINE;

	public final static int					ANTIALIAS_DONT_CARE	= WorldWind.ANTIALIAS_DONT_CARE;
	public final static int					ANTIALIAS_FASTEST	= WorldWind.ANTIALIAS_FASTEST;
	public final static int					ANTIALIAS_NICEST	= WorldWind.ANTIALIAS_NICEST;

	protected ArrayList<Position>			positions;
	protected Vec4							referenceCenterPoint;
	protected int							antiAliasHint		= GL.GL_FASTEST;
	protected Color							color				= Color.WHITE;
	protected double						lineWidth			= 1;
	protected boolean						filled				= false;								// makes it a polygon
	protected boolean						closed				= false;								// connect last point to first
	protected boolean						followTerrain		= false;
	protected double						offset				= 0;
	protected double						terrainConformance	= 10;
	protected int							pathType			= GREAT_CIRCLE;
	protected List<List<Vec4>>				currentSpans;
	protected short							stipplePattern		= (short) 0xAAAA;
	protected int							stippleFactor		= 0;
	protected int							numSubsegments		= 10;
	protected boolean						highlighted			= false;
	protected Color							highlightColor		= new Color(1f, 1f, 1f, 0.5f);
	protected Object						delegateOwner;
	protected LengthMeasurer				measurer			= new LengthMeasurer();
	protected long							geomGenTimeStamp	= -Long.MAX_VALUE;
	protected double						geomGenVE			= 1;
	protected double						eyeDistance;
	protected PickSupport					pickSupport			= new PickSupport();
	protected long							frameNumber			= -1;									// identifies frame used to calculate these values
	protected Layer							pickLayer;

	// Manage an extent for each globe the polyline's associated with.

	protected HashMap<Globe, ExtentInfo>	extents	= new HashMap<Globe, ExtentInfo>(2);	// usually only 1, but few at most

	protected static class ExtentInfo {
		// The extent depends on the state of the globe used to compute it, and the vertical exaggeration.
		protected Extent	extent;
		protected double	verticalExaggeration;
		protected Globe		globe;
		protected Object	globeStateKey;

		public ExtentInfo(final Extent extent, final DrawContext dc) {
			this.extent = extent;
			this.verticalExaggeration = dc.getVerticalExaggeration();
			this.globe = dc.getGlobe();
			this.globeStateKey = dc.getGlobe().getStateKey(dc);
		}

		protected boolean isValid(final DrawContext dc) {
			return this.verticalExaggeration == dc.getVerticalExaggeration()
					&& this.globe == dc.getGlobe()
					&& globeStateKey.equals(dc.getGlobe().getStateKey(dc));
		}
	}

	public Polyline() {
		this.setPositions(null);
		this.measurer.setFollowTerrain(this.followTerrain);
		this.measurer.setPathType(this.pathType);
	}

	public Polyline(final Iterable<? extends LatLon> positions, final double elevation) {
		this.setPositions(positions, elevation);
		this.measurer.setFollowTerrain(this.followTerrain);
		this.measurer.setPathType(this.pathType);
	}

	public Polyline(final Iterable<? extends Position> positions) {
		this.setPositions(positions);
		this.measurer.setFollowTerrain(this.followTerrain);
		this.measurer.setPathType(this.pathType);
	}

	protected static double[] computeElevationExtremes(final Iterable<? extends Position> positions) {
		final double[] extremes = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };
		for (final Position pos : positions) {
			if (extremes[0] > pos.getElevation())
			 {
				extremes[0] = pos.getElevation(); // min
			}
			if (extremes[1] < pos.getElevation())
			 {
				extremes[1] = pos.getElevation(); // max
			}
		}

		return extremes;
	}

	protected ArrayList<Vec4> addPointToSpan(final Vec4 p, ArrayList<Vec4> span) {
		if (span == null) {
			span = new ArrayList<Vec4>();
		}

		span.add(p.subtract3(this.referenceCenterPoint));

		return span;
	}

	protected void addSpan(final ArrayList<Vec4> span) {
		if (span != null && span.size() > 0) {
			this.currentSpans.add(span);
		}
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	protected ArrayList<Vec4> clipAndAdd(final DrawContext dc, final Vec4 ptA, final Vec4 ptB, ArrayList<Vec4> span) {
		// Line clipping appears to be useful only for long lines with few segments. It's costly otherwise.
		// TODO: Investigate trade-off of line clipping.
//        if (Line.clipToFrustum(ptA, ptB, dc.getView().getFrustumInModelCoordinates()) == null)
//        {
//            if (span != null)
//            {
//                this.addSpan(span);
//                span = null;
//            }
//            return span;
//        }

		if (span == null) {
			span = this.addPointToSpan(ptA, span);
		}

		return this.addPointToSpan(ptB, span);
	}

	protected Extent computeExtent(final DrawContext dc) {
		return this.computeExtent(dc.getGlobe(), dc.getVerticalExaggeration());
	}

	protected Extent computeExtent(final Globe globe, final double verticalExaggeration) {
		final Sector sector = Sector.boundingSector(this.getPositions());

		double[] minAndMaxElevations;
		if (this.isFollowTerrain()) {
			minAndMaxElevations = globe.getMinAndMaxElevations(sector);
		} else {
			minAndMaxElevations = computeElevationExtremes(this.getPositions());
		}
		minAndMaxElevations[0] += this.getOffset();
		minAndMaxElevations[1] += this.getOffset();

		return Sector.computeBoundingBox(
				globe,
				verticalExaggeration,
				sector,
				minAndMaxElevations[0],
				minAndMaxElevations[1]);
	}

	protected Vec4 computePoint(final DrawContext dc, final Position pos, final boolean applyOffset) {
		if (this.followTerrain) {
			final double height = !applyOffset ? 0 : this.offset;
			// computeTerrainPoint will apply vertical exaggeration
			return dc.computeTerrainPoint(pos.getLatitude(), pos.getLongitude(), height);
		} else {
			final double height = pos.getElevation() + (applyOffset ? this.offset : 0);
			return dc.getGlobe().computePointFromPosition(
					pos.getLatitude(),
					pos.getLongitude(),
					height * dc.getVerticalExaggeration());
		}
	}

	protected void computeReferenceCenter(final DrawContext dc) {
		// The reference position is null if this Polyline has no positions. In this case computing the Polyline's
		// Cartesian reference point is meaningless because the Polyline has no geographic location. Therefore we exit
		// without updating the reference point.
		final Position refPos = this.getReferencePosition();
		if (refPos == null) {
			return;
		}

		this.referenceCenterPoint = dc.computeTerrainPoint(refPos.getLatitude(), refPos.getLongitude(), this.offset);
	}

	protected double computeSegmentLength(final DrawContext dc, final Position posA, final Position posB) {
		final LatLon llA = new LatLon(posA.getLatitude(), posA.getLongitude());
		final LatLon llB = new LatLon(posB.getLatitude(), posB.getLongitude());

		final Angle ang = LatLon.greatCircleDistance(llA, llB);

		if (this.followTerrain) {
			return ang.radians * (dc.getGlobe().getRadius() + this.offset * dc.getVerticalExaggeration());
		} else {
			final double height = this.offset + 0.5 * (posA.getElevation() + posB.getElevation());
			return ang.radians * (dc.getGlobe().getRadius() + height * dc.getVerticalExaggeration());
		}
	}

	/**
	 * If the scene controller is rendering ordered renderables, this method draws this placemark's
	 * image as an ordered renderable. Otherwise the method determines whether this instance should
	 * be added to the ordered renderable list.
	 * <p/>
	 * The Cartesian and screen points of the placemark are computed during the first call per frame
	 * and re-used in subsequent calls of that frame.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void draw(final DrawContext dc) {
		if (dc.isOrderedRenderingMode()) {
			this.drawOrderedRenderable(dc);
			return;
		}

		// The rest of the code in this method determines whether to queue an ordered renderable for the polyline.

		if (this.positions.size() < 2) {
			return;
		}

		// vertices potentially computed every frame to follow terrain changes
		if (this.currentSpans == null
				|| (this.followTerrain && this.geomGenTimeStamp != dc.getFrameTimeStamp())
				|| this.geomGenVE != dc.getVerticalExaggeration()) {
			// Reference center must be computed prior to computing vertices.
			this.computeReferenceCenter(dc);
			this.eyeDistance = this.referenceCenterPoint.distanceTo3(dc.getView().getEyePoint());
			this.makeVertices(dc);
			this.geomGenTimeStamp = dc.getFrameTimeStamp();
			this.geomGenVE = dc.getVerticalExaggeration();
		}

		if (this.currentSpans == null || this.currentSpans.size() < 1) {
			return;
		}

		if (this.intersectsFrustum(dc)) {
			if (dc.isPickingMode()) {
				this.pickLayer = dc.getCurrentLayer();
			}

			dc.addOrderedRenderable(this); // add the ordered renderable
		}
	}

	protected void drawOrderedRenderable(final DrawContext dc) {
		final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		int attrBits = GL2.GL_HINT_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT;
		if (!dc.isPickingMode()) {
			if (this.color.getAlpha() != 255) {
				attrBits |= GL.GL_COLOR_BUFFER_BIT;
			}
		}

		gl.glPushAttrib(attrBits);
		dc.getView().pushReferenceCenter(dc, this.referenceCenterPoint);

		boolean projectionOffsetPushed = false; // keep track for error recovery

		try {
			if (!dc.isPickingMode()) {
				if (this.color.getAlpha() != 255) {
					gl.glEnable(GL.GL_BLEND);
					gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				}
				gl.glColor4ub(
						(byte) this.color.getRed(),
						(byte) this.color.getGreen(),
						(byte) this.color.getBlue(),
						(byte) this.color.getAlpha());
			} else {
				// We cannot depend on the layer to set a pick color for us because this Polyline is picked during ordered
				// rendering. Therefore we set the pick color ourselves.
				final Color pickColor = dc.getUniquePickColor();
				final Object userObject = this.getDelegateOwner() != null ? this.getDelegateOwner() : this;
				this.pickSupport.addPickableObject(pickColor.getRGB(), userObject, null);
				gl.glColor3ub((byte) pickColor.getRed(), (byte) pickColor.getGreen(), (byte) pickColor.getBlue());
			}

			if (this.stippleFactor > 0) {
				gl.glEnable(GL2.GL_LINE_STIPPLE);
				gl.glLineStipple(this.stippleFactor, this.stipplePattern);
			} else {
				gl.glDisable(GL2.GL_LINE_STIPPLE);
			}

			int hintAttr = GL2.GL_LINE_SMOOTH_HINT;
			if (this.filled) {
				hintAttr = GL2.GL_POLYGON_SMOOTH_HINT;
			}
			gl.glHint(hintAttr, this.antiAliasHint);

			int primType = GL2.GL_LINE_STRIP;
			if (this.filled) {
				primType = GL2.GL_POLYGON;
			}

			if (dc.isPickingMode()) {
				gl.glLineWidth((float) this.lineWidth + 8);
			} else {
				gl.glLineWidth((float) this.lineWidth);
			}

			if (this.followTerrain) {
				dc.pushProjectionOffest(0.99);
				projectionOffsetPushed = true;
			}

			if (this.currentSpans == null) {
				return;
			}

			for (final List<Vec4> span : this.currentSpans) {
				if (span == null) {
					continue;
				}

				// Since segments can very often be very short -- two vertices -- use explicit rendering. The
				// overhead of batched rendering, e.g., gl.glDrawArrays, is too high because it requires copying
				// the vertices into a DoubleBuffer, and DoubleBuffer creation and access performs relatively poorly.
				gl.glBegin(primType);
				for (final Vec4 p : span) {
					gl.glVertex3d(p.x, p.y, p.z);
				}
				gl.glEnd();
			}

			if (this.highlighted) {
				if (!dc.isPickingMode()) {
					if (this.highlightColor.getAlpha() != 255) {
						gl.glEnable(GL.GL_BLEND);
						gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
					}
					gl.glColor4ub(
							(byte) this.highlightColor.getRed(),
							(byte) this.highlightColor.getGreen(),
							(byte) this.highlightColor.getBlue(),
							(byte) this.highlightColor.getAlpha());

					gl.glLineWidth((float) this.lineWidth + 2);
					for (final List<Vec4> span : this.currentSpans) {
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
			}
		} finally {
			if (projectionOffsetPushed) {
				dc.popProjectionOffest();
			}

			gl.glPopAttrib();
			dc.getView().popReferenceCenter(dc);
		}
	}

	public int getAntiAliasHint() {
		return antiAliasHint;
	}

	public Color getColor() {
		return color;
	}

	/**
	 * Returns the delegate owner of this Polyline. If non-null, the returned object replaces the
	 * Polyline as the pickable object returned during picking. If null, the Polyline itself is the
	 * pickable object returned during picking.
	 * 
	 * @return the object used as the pickable object returned during picking, or null to indicate
	 *         that the Polyline is returned during picking.
	 */
	public Object getDelegateOwner() {
		return this.delegateOwner;
	}

	public double getDistanceFromEye() {
		return this.eyeDistance;
	}

	/**
	 * Returns this Polyline's enclosing volume as an {@link gov.nasa.worldwind.geom.Extent} in
	 * model coordinates, given a specified {@link gov.nasa.worldwind.render.DrawContext}. The
	 * returned Extent may be different than the Extent returned by calling
	 * {@link #getExtent(gov.nasa.worldwind.globes.Globe, double)} with the DrawContext's Globe and
	 * vertical exaggeration. Additionally, this may cache the computed extent and is therefore
	 * potentially faster than calling {@link #getExtent(gov.nasa.worldwind.globes.Globe, double)}.
	 * 
	 * @param dc
	 *            the current DrawContext.
	 * @return this Polyline's Extent in model coordinates.
	 * @throws IllegalArgumentException
	 *             if the DrawContext is null, or if the Globe held by the DrawContext is null.
	 */
	public Extent getExtent(final DrawContext dc) {
		if (dc == null) {
			final String message = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		if (dc.getGlobe() == null) {
			final String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		ExtentInfo extentInfo = this.extents.get(dc.getGlobe());
		if (extentInfo != null && extentInfo.isValid(dc)) {
			return extentInfo.extent;
		} else {
			extentInfo = new ExtentInfo(this.computeExtent(dc), dc);
			this.extents.put(dc.getGlobe(), extentInfo);
			return extentInfo.extent;
		}
	}

	/**
	 * Returns this Polyline's enclosing volume as an {@link gov.nasa.worldwind.geom.Extent} in
	 * model coordinates, given a specified {@link gov.nasa.worldwind.globes.Globe} and vertical
	 * exaggeration (see {@link gov.nasa.worldwind.SceneController#getVerticalExaggeration()}.
	 * 
	 * @param globe
	 *            the Globe this Polyline is related to.
	 * @param verticalExaggeration
	 *            the vertical exaggeration to apply.
	 * @return this Polyline's Extent in model coordinates.
	 * @throws IllegalArgumentException
	 *             if the Globe is null.
	 */
	public Extent getExtent(final Globe globe, final double verticalExaggeration) {
		if (globe == null) {
			final String message = Logging.getMessage("nullValue.GlobeIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		return this.computeExtent(globe, verticalExaggeration);
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
		final Iterator<ExtentInfo> infos = this.extents.values().iterator();
		return infos.hasNext() ? this.measurer.getLength(infos.next().globe) : 0;
	}

	public double getLength(final Globe globe) {
		// The length measurer will throw an exception and log the error if globe is null
		return this.measurer.getLength(globe);
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

	public String getPathTypeString() {
		return this.getPathType() == GREAT_CIRCLE ? AVKey.GREAT_CIRCLE : this.getPathType() == RHUMB_LINE
				? AVKey.RHUMB_LINE
				: AVKey.LINEAR;
	}

	public Iterable<Position> getPositions() {
		return this.positions;
	}

	public Position getReferencePosition() {
		if (this.positions.size() < 1) {
			return null;
		} else if (this.positions.size() < 3) {
			return this.positions.get(0);
		} else {
			return this.positions.get(this.positions.size() / 2);
		}
	}

	/**
	 * Returns an XML state document String describing the public attributes of this Polyline.
	 * 
	 * @return XML state document string describing this Polyline.
	 */
	public String getRestorableState() {
		final RestorableSupport rs = RestorableSupport.newRestorableSupport();
		// Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
		if (rs == null) {
			return null;
		}

		if (this.color != null) {
			final String encodedColor = RestorableSupport.encodeColor(this.color);
			if (encodedColor != null) {
				rs.addStateValueAsString("color", encodedColor);
			}
		}

		if (this.highlightColor != null) {
			final String encodedColor = RestorableSupport.encodeColor(this.highlightColor);
			if (encodedColor != null) {
				rs.addStateValueAsString("highlightColor", encodedColor);
			}
		}

		if (this.positions != null) {
			// Create the base "positions" state object.
			final RestorableSupport.StateObject positionsStateObj = rs.addStateObject("positions");
			if (positionsStateObj != null) {
				for (final Position p : this.positions) {
					// Save each position only if all parts (latitude, longitude, and elevation) can be
					// saved. We will not save a partial iconPosition (for example, just the elevation).
					if (p != null && p.getLatitude() != null && p.getLongitude() != null) {
						// Create a nested "position" element underneath the base "positions".
						final RestorableSupport.StateObject pStateObj = rs.addStateObject(positionsStateObj, "position");
						if (pStateObj != null) {
							rs.addStateValueAsDouble(pStateObj, "latitudeDegrees", p.getLatitude().degrees);
							rs.addStateValueAsDouble(pStateObj, "longitudeDegrees", p.getLongitude().degrees);
							rs.addStateValueAsDouble(pStateObj, "elevation", p.getElevation());
						}
					}
				}
			}
		}

		rs.addStateValueAsInteger("antiAliasHint", this.antiAliasHint);
		rs.addStateValueAsBoolean("filled", this.filled);
		rs.addStateValueAsBoolean("closed", this.closed);
		rs.addStateValueAsBoolean("highlighted", this.highlighted);
		rs.addStateValueAsInteger("pathType", this.pathType);
		rs.addStateValueAsBoolean("followTerrain", this.followTerrain);
		rs.addStateValueAsDouble("offset", this.offset);
		rs.addStateValueAsDouble("terrainConformance", this.terrainConformance);
		rs.addStateValueAsDouble("lineWidth", this.lineWidth);
		rs.addStateValueAsInteger("stipplePattern", this.stipplePattern);
		rs.addStateValueAsInteger("stippleFactor", this.stippleFactor);
		rs.addStateValueAsInteger("numSubsegments", this.numSubsegments);

		final RestorableSupport.StateObject so = rs.addStateObject(null, "avlist");
		for (final Map.Entry<String, Object> avp : this.getEntries()) {
			this.getRestorableStateForAVPair(avp.getKey(), avp.getValue() != null ? avp.getValue() : "", rs, so);
		}

		return rs.getStateAsXml();
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

	/**
	 * Indicates whether the shape is visible in the current view.
	 * 
	 * @param dc
	 *            the draw context.
	 * @return true if the shape is visible, otherwise false.
	 */
	protected boolean intersectsFrustum(final DrawContext dc) {
		final Extent extent = this.getExtent(dc);
		if (extent == null)
		 {
			return true; // don't know the visibility, shape hasn't been computed yet
		}

		if (dc.isPickingMode()) {
			return dc.getPickFrustums().intersectsAny(extent);
		}

		return dc.getView().getFrustumInModelCoordinates().intersects(extent);
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

	protected boolean isSegmentVisible(final DrawContext dc, final Position posA, final Position posB, final Vec4 ptA, final Vec4 ptB) {
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

		final Position posC = Position.interpolateRhumb(0.5, posA, posB);
		final Vec4 ptC = this.computePoint(dc, posC, true);
		if (f.contains(ptC)) {
			return true;
		}

		final double r = Line.distanceToSegment(ptA, ptB, ptC);
		final Cylinder cyl = new Cylinder(ptA, ptB, r == 0 ? 1 : r);
		return cyl.intersects(dc.getView().getFrustumInModelCoordinates());
	}

	protected ArrayList<Vec4> makeSegment(final DrawContext dc, final Position posA, final Position posB, Vec4 ptA, Vec4 ptB) {
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
				if (segmentAzimuth == null) {
					segmentAzimuth = LatLon.linearAzimuth(posA, posB);
					segmentDistance = LatLon.linearDistance(posA, posB);
				}
				final Angle distance = Angle.fromRadians(s * segmentDistance.radians);
				final LatLon latLon = LatLon.linearEndPosition(posA, segmentAzimuth, distance);
				pos = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
			} else if (this.pathType == RHUMB_LINE) // or LOXODROME (note that loxodrome is translated to RHUMB_LINE in setPathType)
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
		if (this.currentSpans == null) {
			this.currentSpans = new ArrayList<List<Vec4>>();
		} else {
			this.currentSpans.clear();
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
				this.addSpan(span);
			}

			posA = posB;
			ptA = ptB;
		}
	}

	public void move(final Position delta) {
		if (delta == null) {
			final String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		final Position refPos = this.getReferencePosition();

		// The reference position is null if this Polyline has no positions. In this case moving the Polyline by a
		// relative delta is meaningless because the Polyline has no geographic location. Therefore we fail softly by
		// exiting and doing nothing.
		if (refPos == null) {
			return;
		}

		this.moveTo(refPos.add(delta));
	}

	public void moveTo(final Position position) {
		if (position == null) {
			final String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.reset();
		this.extents.clear();

		final Position oldRef = this.getReferencePosition();

		// The reference position is null if this Polyline has no positions. In this case moving the Polyline to a new
		// reference position is meaningless because the Polyline has no geographic location. Therefore we fail softly
		// by exiting and doing nothing.
		if (oldRef == null) {
			return;
		}

		final double elevDelta = position.getElevation() - oldRef.getElevation();

		for (int i = 0; i < this.positions.size(); i++) {
			final Position pos = this.positions.get(i);

			final Angle distance = LatLon.greatCircleDistance(oldRef, pos);
			final Angle azimuth = LatLon.greatCircleAzimuth(oldRef, pos);
			final LatLon newLocation = LatLon.greatCircleEndPosition(position, azimuth, distance);
			final double newElev = pos.getElevation() + elevDelta;

			this.positions.set(i, new Position(newLocation, newElev));
		}
	}

	public void pick(final DrawContext dc, final Point pickPoint) {
		// This method is called only when ordered renderables are being drawn.
		// Arg checked within call to render.

		this.pickSupport.clearPickList();
		try {
			this.pickSupport.beginPicking(dc);
			this.render(dc);
		} finally {
			this.pickSupport.endPicking(dc);
			this.pickSupport.resolvePick(dc, pickPoint, this.pickLayer);
		}
	}

	public void render(final DrawContext dc) {
		// This render method is called three times during frame generation. It's first called as a {@link Renderable}
		// during <code>Renderable</code> picking. It's called again during normal rendering. And it's called a third
		// time as an OrderedRenderable. The first two calls determine whether to add the polyline to the ordered
		// renderable list during pick and render. The third call just draws the ordered renderable.
		if (dc == null) {
			final String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		if (dc.getSurfaceGeometry() == null) {
			return;
		}

		this.draw(dc);
	}

	private void reset() {
		if (this.currentSpans != null) {
			this.currentSpans.clear();
		}
		this.currentSpans = null;
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
						// Restore each position only if all parts are available.
						// We will not restore a partial position (for example, just the elevation).
						final Double latitudeState = restorableSupport.getStateValueAsDouble(pStateObj, "latitudeDegrees");
						final Double longitudeState = restorableSupport.getStateValueAsDouble(pStateObj, "longitudeDegrees");
						final Double elevationState = restorableSupport.getStateValueAsDouble(pStateObj, "elevation");
						if (latitudeState != null && longitudeState != null && elevationState != null) {
							newPositions.add(Position.fromDegrees(latitudeState, longitudeState, elevationState));
						}
					}
				}
			}

			// Even if there are no actual positions specified, we set positions as an empty list.
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

		final RestorableSupport.StateObject so = restorableSupport.getStateObject(null, "avlist");
		if (so != null) {
			final RestorableSupport.StateObject[] avpairs = restorableSupport.getAllStateObjects(so, "");
			if (avpairs != null) {
				for (final RestorableSupport.StateObject avp : avpairs) {
					if (avp != null) {
						this.setValue(avp.getName(), avp.getValue());
					}
				}
			}
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

	/**
	 * Specifies the delegate owner of this Polyline. If non-null, the delegate owner replaces the
	 * Polyline as the pickable object returned during picking. If null, the Polyline itself is the
	 * pickable object returned during picking.
	 * 
	 * @param owner
	 *            the object to use as the pickable object returned during picking, or null to
	 *            return the Polyline.
	 */
	public void setDelegateOwner(final Object owner) {
		this.delegateOwner = owner;
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
		this.extents.clear();
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
		this.extents.clear();
	}

	/**
	 * Sets the type of path to draw, one of {@link #GREAT_CIRCLE}, which draws each segment of the
	 * path as a great circle, {@link #LINEAR}, which determines the intermediate positions between
	 * segments by interpolating the segment endpoints, or {@link #RHUMB_LINE}, which draws each
	 * segment of the path as a line of constant heading.
	 * 
	 * @param pathType
	 *            the type of path to draw.
	 * @see <a href="{@docRoot}/overview-summary.html#path-types">Path Types</a>
	 */
	public void setPathType(final int pathType) {
		this.reset();
		this.pathType = pathType;
		this.measurer.setPathType(pathType);
	}

	/**
	 * Sets the type of path to draw, one of {@link AVKey#GREAT_CIRCLE}, which draws each segment of
	 * the path as a great circle, {@link AVKey#LINEAR}, which determines the intermediate positions
	 * between segments by interpolating the segment endpoints, or {@link AVKey#RHUMB_LINE}, which
	 * draws each segment of the path as a line of constant heading.
	 * 
	 * @param pathType
	 *            the type of path to draw.
	 * @see <a href="{@docRoot}/overview-summary.html#path-types">Path Types</a>
	 */
	public void setPathType(final String pathType) {
		if (pathType == null) {
			final String msg = Logging.getMessage("nullValue.PathTypeIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.setPathType(pathType.equals(AVKey.GREAT_CIRCLE) ? GREAT_CIRCLE : pathType.equals(AVKey.RHUMB_LINE)
				|| pathType.equals(AVKey.LOXODROME) ? RHUMB_LINE : LINEAR);
	}

	/**
	 * Sets the paths positions as latitude and longitude values at a constant altitude.
	 * 
	 * @param inPositions
	 *            the latitudes and longitudes of the positions.
	 * @param altitude
	 *            the elevation to assign each position.
	 */
	public void setPositions(final Iterable<? extends LatLon> inPositions, final double altitude) {
		this.reset();
		this.positions = new ArrayList<Position>();
		this.extents.clear();
		if (inPositions != null) {
			for (final LatLon position : inPositions) {
				this.positions.add(new Position(position, altitude));
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
		this.extents.clear();
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
