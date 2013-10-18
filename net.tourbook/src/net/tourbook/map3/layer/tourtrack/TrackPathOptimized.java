/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.map3.layer.tourtrack;

import gnu.trove.list.array.TIntArrayList;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.terrain.Terrain;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import net.tourbook.map3.shape.MTMultiResolutionPath;

import org.eclipse.swt.graphics.RGB;

import com.sun.opengl.util.BufferUtil;

public class TrackPathOptimized extends MTMultiResolutionPath implements ITrackPath {

	private static final int	SKIP_COUNTER			= 30;

	private TourTrack			_tourTrack;
	private TourTrackConfig		_tourTrackConfig;

	private static final String	ARROWS_KEY				= TrackPathOptimized.class.getName() + ".DirectionArrows";

	/** Default arrow length, in meters. */
	public static final double	DEFAULT_ARROW_LENGTH	= 300;

	/** Default arrow angle. */
	public static final Angle	DEFAULT_ARROW_ANGLE		= Angle.fromDegrees(45.0);

	/** Default maximum screen size of the arrowheads, in pixels. */
	public static final double	DEFAULT_MAX_SCREEN_SIZE	= 30.0;

	/** The length, in meters, of the arrowhead, from tip to base. */
	protected double			arrowLength				= DEFAULT_ARROW_LENGTH;

	/** The angle of the arrowhead tip. */
	protected Angle				arrowAngle				= DEFAULT_ARROW_ANGLE;

	/** The maximum screen size, in pixels, of the direction arrowheads. */
	protected double			maxScreenSize			= DEFAULT_MAX_SCREEN_SIZE;
	private TIntArrayList		_polePositionsIndizes;

	protected static class MTPathData extends MultiResolutionPathData {

		protected FloatBuffer	renderedPath_Selected;

		protected int			vertexStride_Selected;
		protected int			vertexCount_Selected;

		public MTPathData(final DrawContext dc, final Path shape) {
			super(dc, shape);
		}

	}

	public TrackPathOptimized(final ArrayList<TourMap3Position> trackPositions) {
		super(trackPositions);
	}

	@Override
	protected FloatBuffer computeAbsolutePoints(final DrawContext dc,
												final List<Position> positions,
												final FloatBuffer path,
												final PathData pathData) {

		computeAbsolutePoints_Selected(dc, positions, (MTPathData) pathData);

		return super.computeAbsolutePoints(dc, positions, path, pathData);
	}

	private void computeAbsolutePoints_Selected(final DrawContext dc,
												final List<Position> positions,
												final MTPathData pathData) {

		final double poleEyeHeight = pathData.getEyeDistance() / 10.0;

		final int elemsPerPoint = 3;
		final int positionSize = positions.size();
		final int polePositionSize = (positionSize / SKIP_COUNTER) + 3;
		final int numPoints = polePositionSize * 2;

		final int bufferSize = elemsPerPoint * numPoints;
		FloatBuffer renderedPath = pathData.renderedPath_Selected;

		if (renderedPath == null || renderedPath.capacity() < bufferSize) {
			renderedPath = BufferUtil.newFloatBuffer(bufferSize);
		}
		renderedPath.clear();

		if (_polePositionsIndizes == null || _polePositionsIndizes.size() < polePositionSize) {
			_polePositionsIndizes = new TIntArrayList(polePositionSize);
		}
		_polePositionsIndizes.clear();

		final Globe globe = dc.getGlobe();
		final Vec4 referencePoint = pathData.getReferencePoint();

		double verticalExaggeration = 1;
		final boolean isVerticalExaggeration = dc.getVerticalExaggeration() != 1;
		if (isVerticalExaggeration) {
			verticalExaggeration = dc.getVerticalExaggeration();
		}
		final double poleHeight = poleEyeHeight;// * verticalExaggeration;

		for (int posIndex = 0; posIndex < positionSize; posIndex++) {

			if (posIndex % SKIP_COUNTER == 0 || posIndex == 0 || posIndex == positionSize - 1) {

				_polePositionsIndizes.add(posIndex);

				final Position geoPosition = positions.get(posIndex);
				final double altitude = verticalExaggeration * geoPosition.getAltitude();

				// create bottom vertex
				Vec4 pt = globe.computePointFromPosition(//
						geoPosition.getLatitude(),
						geoPosition.getLongitude(),
						altitude);

//				Vec4 pt = dc.computeTerrainPoint(//
//						geoPosition.getLatitude(),
//						geoPosition.getLongitude(),
//						0);

				renderedPath.put((float) (pt.x - referencePoint.x));
				renderedPath.put((float) (pt.y - referencePoint.y));
				renderedPath.put((float) (pt.z - referencePoint.z));

				// create top vertex
				pt = globe.computePointFromPosition(//
						geoPosition.getLatitude(),
						geoPosition.getLongitude(),
						altitude + poleHeight);

				renderedPath.put((float) (pt.x - referencePoint.x));
				renderedPath.put((float) (pt.y - referencePoint.y));
				renderedPath.put((float) (pt.z - referencePoint.z));
			}
		}

		renderedPath.flip(); // since the path is reused the limit might not be the same as the previous usage

		pathData.renderedPath_Selected = renderedPath;
		pathData.vertexStride_Selected = elemsPerPoint;
		pathData.vertexCount_Selected = renderedPath.limit() / (elemsPerPoint);
	}

	private void computeDirectionArrows(final DrawContext dc, final MTPathData pathData) {

		final int numPositions = _polePositionsIndizes.size();
		if (numPositions < 2) {
			return;
		}

		final int FLOATS_PER_ARROWHEAD = 9; // 3 points * 3 coordinates per point
		FloatBuffer arrowBuffer = (FloatBuffer) pathData.getValue(ARROWS_KEY);
		if (arrowBuffer == null || arrowBuffer.capacity() < numPositions * FLOATS_PER_ARROWHEAD) {
			arrowBuffer = BufferUtil.newFloatBuffer(FLOATS_PER_ARROWHEAD * numPositions);
		}

		pathData.setValue(ARROWS_KEY, arrowBuffer);
		arrowBuffer.clear();

		final Terrain terrain = dc.getTerrain();

		final double arrowBase = arrowLength * arrowAngle.tanHalfAngle();

		final FloatBuffer renderedPath = pathData.renderedPath_Selected;
		renderedPath.rewind();

		final List<Position> tessellatedPositions = pathData.getTessellatedPositions();

		int currentPositionIndex = _polePositionsIndizes.get(0);
		final Position poleA = tessellatedPositions.get(currentPositionIndex);
		Vec4 polePtA = computePoint(terrain, poleA);

		final Vec4 referencePoint = pathData.getReferencePoint();

		for (int poleIndex = 1; poleIndex < _polePositionsIndizes.size(); poleIndex++) {

			final int positionIndex = _polePositionsIndizes.get(poleIndex);

			/*
			 * Find the position of this pole and the next pole. Divide by 2 to convert an index in
			 * the renderedPath buffer to a index in the tessellatedPositions list.
			 */
			final int nextPositionIndex = positionIndex;

			final Position poleB = tessellatedPositions.get(nextPositionIndex);

			final Vec4 polePtB = computePoint(terrain, poleB);

			final int poleIndexOffset = poleIndex * 6;

			final Vec4 arrowPole = new Vec4(
					renderedPath.get(poleIndexOffset + 3) + referencePoint.x,
					renderedPath.get(poleIndexOffset + 4) + referencePoint.y,
					renderedPath.get(poleIndexOffset + 5) + referencePoint.z);

			computeDirectionArrows_ArrowheadGeometry(
					dc,
					polePtA,
					polePtB,
					arrowPole,
					polePtB,
					arrowLength,
					arrowBase,
					arrowBuffer,
					pathData);

			currentPositionIndex = nextPositionIndex;
			polePtA = polePtB;
		}

		// hide unused arrows
		arrowBuffer.flip();
	}

	/**
	 * Compute the geometry of a direction arrow between two points.
	 * 
	 * @param dc
	 *            current draw context
	 * @param polePtA
	 *            the first pole position. This is one of the application defined path positions.
	 * @param polePtB
	 *            second pole position
	 * @param ptA
	 *            first position of the tessellated path midway between the poles
	 * @param ptB
	 *            second position of the tessellated path midway between the poles
	 * @param arrowLength
	 *            length of the arrowhead, in meters. The arrow head may not be rendered at full
	 *            size, because is it may not exceed {@link #maxScreenSize} pixels in length.
	 * @param arrowBase
	 *            length of the arrowhead base
	 * @param buffer
	 *            buffer in which to place computed points
	 * @param pathData
	 *            the current globe-specific path data.
	 */
	private void computeDirectionArrows_ArrowheadGeometry(	final DrawContext dc,
															final Vec4 polePtA,
															final Vec4 polePtB,
															final Vec4 arrowPole,
															final Vec4 ptB,
															double arrowLength,
															double arrowBase,
															final FloatBuffer buffer,
															final PathData pathData) {
		/*
		 * Build a triangle to represent the arrowhead. The triangle is built from two vectors, one
		 * parallel to the segment, and one perpendicular to it. The plane of the arrowhead will be
		 * parallel to the surface.
		 */
		final double poleDistance = polePtA.distanceTo3(polePtB);

		// Compute parallel component
		Vec4 parallel = polePtA.subtract3(polePtB);

		final Vec4 surfaceNormal = dc.getGlobe().computeSurfaceNormalAtPoint(polePtB);

		// Compute perpendicular component
		Vec4 perpendicular = surfaceNormal.cross3(parallel);

		// Compute midpoint of segment
//		Vec4 midPoint = polePtA.add3(polePtB).divide3(2.0);
		Vec4 midPoint = arrowPole;

		/*
		 * Compute the size of the arrowhead in pixels to ensure that the arrow does not exceed the
		 * maximum screen size.
		 */
		final View view = dc.getView();
		final double midpointDistance = view.getEyePoint().distanceTo3(midPoint);
		final double pixelSize = view.computePixelSizeAtDistance(midpointDistance);
		if (arrowLength / pixelSize > this.maxScreenSize) {
			arrowLength = this.maxScreenSize * pixelSize;
			arrowBase = arrowLength * arrowAngle.tanHalfAngle();
		}

		// Don't draw an arrowhead if the path segment is smaller than the arrow
		if (poleDistance <= arrowLength) {
//			return;
		}

		perpendicular = perpendicular.normalize3().multiply3(arrowBase);
		parallel = parallel.normalize3().multiply3(arrowLength);

		/*
		 * If the distance between the poles is greater than the arrow length, center the arrow on
		 * the midpoint. Otherwise position the tip of the arrow at the midpoint. On short segments
		 * it looks weird if the tip of the arrow does not fall on the path, but on longer segments
		 * it looks better to center the arrow on the segment.
		 */
		if (poleDistance > arrowLength) {
			midPoint = midPoint.subtract3(parallel.divide3(2.0));
		}

		// Compute geometry of direction arrow
		final Vec4 vertex1 = midPoint.add3(parallel).add3(perpendicular);
		final Vec4 vertex2 = midPoint.add3(parallel).add3(perpendicular.multiply3(-1.0));

		// Add geometry to the buffer
		final Vec4 referencePoint = pathData.getReferencePoint();
		buffer.put((float) (vertex1.x - referencePoint.x));
		buffer.put((float) (vertex1.y - referencePoint.y));
		buffer.put((float) (vertex1.z - referencePoint.z));

		buffer.put((float) (vertex2.x - referencePoint.x));
		buffer.put((float) (vertex2.y - referencePoint.y));
		buffer.put((float) (vertex2.z - referencePoint.z));

		buffer.put((float) (midPoint.x - referencePoint.x));
		buffer.put((float) (midPoint.y - referencePoint.y));
		buffer.put((float) (midPoint.z - referencePoint.z));
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Overridden to return a new instance of MultiResolutionPathData.
	 */
	@Override
	protected AbstractShapeData createCacheEntry(final DrawContext dc) {
		return new MTPathData(dc, this);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Overridden to also draw direction arrows.
	 * 
	 * @param dc
	 *            Current draw context.
	 */
	@Override
	protected void doDrawOutline(final DrawContext dc) {

		super.doDrawOutline(dc);

		// this must be drawn AFTER the super.doDrawOutline() because it is using the pole positions !!!
		if (dc.isPickingMode() == false) {

			final PathData pathData = getCurrentPathData();

			computeDirectionArrows(dc, (MTPathData) pathData);
			drawDirectionArrows(dc, pathData);
		}
	}

	@Override
	protected void doDrawOutlineVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		final boolean isPickingMode = dc.isPickingMode();

		final GL gl = dc.getGL();

		try {

			final int vertexStride = pathData.getVertexStride();
			final int vertexCount = pathData.getVertexCount();
			final boolean hasExtrusionPoints = pathData.isHasExtrusionPoints();

			final int stride = hasExtrusionPoints ? 2 * vertexStride : vertexStride;
			final int count = hasExtrusionPoints ? vertexCount / 2 : vertexCount;

			final boolean useVertexColors = !isPickingMode && pathData.getTessellatedColors() != null;

			// Convert stride from number of elements to number of bytes.
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[0]);
			gl.glVertexPointer(3, GL.GL_FLOAT, 4 * stride, 0);

			// Apply this path's per-position colors if we're in normal rendering mode (not picking) and this path's
			// positionColors is non-null.
			if (useVertexColors) {

				// Convert stride and offset from number of elements to number of bytes.
				gl.glEnableClientState(GL.GL_COLOR_ARRAY);
				gl.glColorPointer(4, GL.GL_FLOAT, 4 * stride, 4 * pathData.getColorOffset());
			}

			gl.glDrawArrays(GL.GL_LINE_STRIP, 0, count);

			if (useVertexColors) {
				gl.glDisableClientState(GL.GL_COLOR_ARRAY);
			}

			if (hasExtrusionPoints && this.isDrawVerticals()) {
				drawVerticalOutlineVBO(dc, vboIds, pathData);
			}

			if (isShowPositions()) {
				drawPointsVBO(dc, vboIds, pathData);
			}

			final boolean isTourSelected = _tourTrack.isSelected();
			final boolean isTourHovered = _tourTrack.isHovered();

			if (isPickingMode == false && isTourSelected) {
				drawColoredWallVBO(dc, vboIds, pathData);
			}

			if (isPickingMode == false && (isTourSelected || isTourHovered)) {

				RGB rgb = null;

				if (isTourHovered && isTourSelected) {
					rgb = _tourTrackConfig.outlineColorHovSel;
				} else if (isTourHovered) {
					rgb = _tourTrackConfig.outlineColorHovered;
				} else {
					rgb = _tourTrackConfig.outlineColorSelected;
				}

				gl.glColor4ub((byte) rgb.red, (byte) rgb.green, (byte) rgb.blue, (byte) 0xbf);

				gl.glLineWidth(2);

				drawSelectedPositionsVBO(dc, vboIds, (MTPathData) pathData);
			}

		} finally {
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
	}

	private void drawColoredWallVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		final GL gl = dc.getGL();

		final int vertexStride = pathData.getVertexStride();

		final int stride = vertexStride;
		final boolean useVertexColors = !dc.isPickingMode() && pathData.getTessellatedColors() != null;

		// Convert stride from number of elements to number of bytes.
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[0]);
		gl.glVertexPointer(3, GL.GL_FLOAT, 4 * stride, 0);

		// Apply this path's per-position colors if we're in normal rendering mode (not picking) and this path's
		// positionColors is non-null.
		if (useVertexColors) {

			// Convert stride and offset from number of elements to number of bytes.
			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL.GL_FLOAT, 4 * stride, 4 * pathData.getColorOffset());
		}

		gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, pathData.getVertexCount());

		if (useVertexColors) {
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
		}
	}

	/**
	 * Draws this DirectedPath's direction arrows. Called from
	 * {@link #doDrawOutline(gov.nasa.worldwind.render.DrawContext)} before drawing the Path's
	 * actual outline.
	 * <p/>
	 * If this Path is entirely located on the terrain, this applies an offset to the arrow's depth
	 * values to to ensure they shows over the terrain. This does not apply a depth offset in any
	 * other case to avoid incorrectly drawing the arrows over objects they should be behind,
	 * including the terrain. In addition to applying a depth offset, this disables writing to the
	 * depth buffer to avoid causing subsequently drawn ordered renderables to incorrectly fail the
	 * depth test. Since the arrows are located on the terrain, the terrain already provides the
	 * necessary depth values and we can be certain that other ordered renderables should appear on
	 * top of them.
	 * 
	 * @param dc
	 *            Current draw context.
	 * @param pathData
	 *            the current globe-specific path data.
	 */
	private void drawDirectionArrows(final DrawContext dc, final PathData pathData) {

		RGB rgb = null;
		final boolean isTourSelected = _tourTrack.isSelected();
		final boolean isTourHovered = _tourTrack.isHovered();

		if (isTourHovered && isTourSelected) {
			rgb = _tourTrackConfig.outlineColorHovSel;
		} else if (isTourHovered) {
			rgb = _tourTrackConfig.outlineColorHovered;
		} else if (isTourSelected) {
			rgb = _tourTrackConfig.outlineColorSelected;
		}

		if (rgb == null) {
			return;
		}

		final GL gl = dc.getGL();
		boolean projectionOffsetPushed = false; // keep track for error recovery

		try {

			if (this.isSurfacePath()) {

				// Pull the arrow triangles forward just a bit to ensure they show over the terrain.
				dc.pushProjectionOffest(SURFACE_PATH_DEPTH_OFFSET);
				gl.glDepthMask(false);
				projectionOffsetPushed = true;
			}

			gl.glColor4ub((byte) rgb.red, (byte) rgb.green, (byte) rgb.blue, (byte) 0xbf);

			gl.glLineWidth(2);

			final FloatBuffer directionArrows = (FloatBuffer) pathData.getValue(ARROWS_KEY);
			gl.glVertexPointer(3, GL.GL_FLOAT, 0, directionArrows.rewind());
			gl.glDrawArrays(GL.GL_TRIANGLES, 0, directionArrows.limit() / 3);

		} finally {

			if (projectionOffsetPushed) {
				dc.popProjectionOffest();
				gl.glDepthMask(true);
			}
		}
	}

	private void drawSelectedPositionsVBO(final DrawContext dc, final int[] vboIds, final MTPathData pathData) {

		final FloatBuffer selectedPositions = pathData.renderedPath_Selected;
		if (selectedPositions == null || selectedPositions.limit() < 1) {
			return;
		}

		final int vboId = vboIds[vboIds.length - 1];

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \tdraw vboId:" + vboId);
//		// TODO remove SYSTEM.OUT.PRINTLN

		final GL gl = dc.getGL();

		// Convert stride from number of elements to number of bytes.
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
		gl.glVertexPointer(3, GL.GL_FLOAT, 4 * pathData.vertexStride_Selected, 0);
//		gl.glDrawArrays(GL.GL_LINES, 0, Math.min(2, pathData.vertexCount_Selected));
		gl.glDrawArrays(GL.GL_LINES, 0, pathData.vertexCount_Selected);

//		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
//		gl.glLineWidth(1);
	}

	@Override
	protected void fillVBO(final DrawContext dc) {

		final MTPathData pathData = (MTPathData) getCurrentPathData();

		int numIds = isShowPositions() ? //
				3
				: pathData.isHasExtrusionPoints() && isDrawVerticals() ? //
						2
						: 1;

		// show selected path positions
		numIds++;

		int[] vboIds = (int[]) dc.getGpuResourceCache().get(pathData.getVboCacheKey());
		if (vboIds != null && vboIds.length != numIds) {
			this.clearCachedVbos(dc);
			vboIds = null;
		}

		final GL gl = dc.getGL();

		final int vSize = pathData.getRenderedPath().limit() * 4;
		int iSize = pathData.isHasExtrusionPoints() && isDrawVerticals() ? //
				pathData.getTessellatedPositions().size() * 2 * 4
				: 0;

		if (isShowPositions()) {
			iSize += pathData.getTessellatedPositions().size();
		}

		// show selected path positions
		iSize += pathData.renderedPath_Selected.limit() * 4;

		if (vboIds == null) {

			vboIds = new int[numIds];

			gl.glGenBuffers(vboIds.length, vboIds, 0);

			dc.getGpuResourceCache().put(//
					pathData.getVboCacheKey(),
					vboIds,
					GpuResourceCache.VBO_BUFFERS,
					vSize + iSize);
		}

		try {

			final FloatBuffer vb = pathData.getRenderedPath();
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[0]);
			gl.glBufferData(GL.GL_ARRAY_BUFFER, vb.limit() * 4, vb.rewind(), GL.GL_STATIC_DRAW);

			if (pathData.isHasExtrusionPoints() && isDrawVerticals()) {
				final IntBuffer ib = pathData.getPolePositions();
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboIds[1]);
				gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, ib.limit() * 4, ib.rewind(), GL.GL_STATIC_DRAW);
			}

			if (isShowPositions()) {
				final IntBuffer ib = pathData.getPositionPoints();
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboIds[2]);
				gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, ib.limit() * 4, ib.rewind(), GL.GL_STATIC_DRAW);
			}

			// create selected path position vbo
			final FloatBuffer fb = pathData.renderedPath_Selected;
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[numIds - 1]);
			gl.glBufferData(GL.GL_ARRAY_BUFFER, fb.limit() * 4, fb.rewind(), GL.GL_STATIC_DRAW);

//			System.out.println(UI.timeStampNano()
//					+ " ["
//					+ getClass().getSimpleName()
//					+ "] fillVBO()"
//					+ (" \tvboId: " + vboIds[numIds - 1])
//					+ ("\tsize: " + fb.limit() * 4)
//					+ ("\tfb.limit(): " + fb.limit())
//					+ "\n"
//			//
//					);
//			// TODO remove SYSTEM.OUT.PRINTLN

		} finally {

			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
	}

	@Override
	protected Color getColor(final Position pos, final Integer ordinal) {
		return _tourTrack.getColor(pos, ordinal);
	}

	@Override
	public Path getPath() {
		return this;
	}

	@Override
	public PositionColors getPathPositionColors() {
		return positionColors;
	}

	@Override
	public TourTrack getTourTrack() {
		return _tourTrack;
	}

	@Override
	public void setPicked(final boolean isHovered, final Integer pickPositionIndex) {

		_tourTrack.setHovered(isHovered, pickPositionIndex);

//		System.out.println(UI.timeStampNano()
//				+ " ["
//				+ getClass().getSimpleName()
//				+ "]  "
//				+ _tourTrack
//				+ "\tisHovered="
//				+ isHovered
//				+ "\tisSelected="
//				+ _tourTrack.isSelected());
//		// TODO remove SYSTEM.OUT.PRINTLN

//		if (isHovered == false && _tourTrack.isSelected() == false) {
//
//			/*
//			 * This hack prevents a tess color NPE, it took me many days to understand 3D drawing
//			 * and find this "solution".
//			 */
//			getCurrentPathData().setTessellatedPositions(null);
//
////			System.out.println(UI.timeStampNano()
////					+ " ["
////					+ getClass().getSimpleName()
////					+ "] "
////					+ _tourTrack
////					+ "\tset tess pos = null");
////			// TODO remove SYSTEM.OUT.PRINTLN
//		}
//
//		// after picking, ensure that the positions colors are set again
//		getCurrentPathData().setExpired(true);
	}

//	/**
//	 * Computes a model-coordinate path from a list of positions, using the altitudes in the
//	 * specified positions. Adds extrusion points -- those on the ground -- when the path is
//	 * extruded and the specified single altitude is not 0.
//	 *
//	 * @param dc
//	 *            the current draw context.
//	 * @param positions
//	 *            the positions to create a path for.
//	 * @param path
//	 *            a buffer in which to store the computed points. May be null. The buffer is not
//	 *            used if it is null or tool small for the required number of points. A new buffer
//	 *            is created in that case and returned by this method. This method modifies the
//	 *            buffer,s position and limit fields.
//	 * @param pathData
//	 *            the current globe-specific path data.
//	 * @return the buffer in which to place the computed points.
//	 */
//	@Override
//	protected FloatBuffer computeAbsolutePoints(final DrawContext dc,
//												final List<Position> positions,
//												FloatBuffer path,
//												final PathData pathData) {
//
//		final double eyeDistance = pathData.getEyeDistance();
//		final double highlightPole = eyeDistance / 30.0;
//		final int skipCounter = 15;
//
////		System.out.println(UI.timeStampNano()
////				+ " ["
////				+ getClass().getSimpleName()
////				+ "] \tpositions.size() "
////				+ positions.size()
////				+ "\teye: "
////				+ eyeDistance);
////		// TODO remove SYSTEM.OUT.PRINTLN
//
//		final List<Color> tessellatedColors = pathData.getTessellatedColors();
//
//		final int numPoints = this.isExtrude() ? 2 * positions.size() : positions.size();
//		final int elemsPerPoint = (tessellatedColors != null ? 7 : 3);
//
//		final Iterator<Color> colorIter = (tessellatedColors != null //
//				? tessellatedColors.iterator()
//				: null);
//
//		final float[] color = (tessellatedColors != null ? new float[4] : null);
//
//		if (path == null || path.capacity() < elemsPerPoint * numPoints) {
//			path = BufferUtil.newFloatBuffer(elemsPerPoint * numPoints);
//		}
//
//		path.clear();
//
//
//		final Globe globe = dc.getGlobe();
//		final Vec4 referencePoint = pathData.getReferencePoint();
//
//		if (dc.getVerticalExaggeration() != 1) {
//
//			final double ve = dc.getVerticalExaggeration();
//
//			for (final Position pos : positions) {
//
//				final Vec4 pt = globe.computePointFromPosition(
//						pos.getLatitude(),
//						pos.getLongitude(),
//						ve * (pos.getAltitude()));
//
//				path.put((float) (pt.x - referencePoint.x));
//				path.put((float) (pt.y - referencePoint.y));
//				path.put((float) (pt.z - referencePoint.z));
//
//				if (colorIter != null && colorIter.hasNext()) {
//					colorIter.next().getRGBComponents(color);
//					path.put(color);
//				}
//
//				if (this.isExtrude()) {
//					this.appendTerrainPoint(dc, pos, color, path, pathData);
//				}
//			}
//		} else {
//
//			for (int posIndex = 0; posIndex < positions.size(); posIndex++) {
//
//				final Position pos = positions.get(posIndex);
//
//				Vec4 pt;
//
//				if (posIndex % skipCounter == 0) {
//
//					pt = globe.computePointFromPosition(//
//							pos.getLatitude(),
//							pos.getLongitude(),
//							pos.getAltitude() + highlightPole);
//				} else {
//
//					pt = globe.computePointFromPosition(pos);
//				}
//
//				path.put((float) (pt.x - referencePoint.x));
//				path.put((float) (pt.y - referencePoint.y));
//				path.put((float) (pt.z - referencePoint.z));
//
//				if (colorIter != null && colorIter.hasNext()) {
//					colorIter.next().getRGBComponents(color);
//					path.put(color);
//				}
//
//				if (this.isExtrude()) {
//					this.appendTerrainPoint(dc, pos, color, path, pathData);
//				}
//			}
//		}
//
//		pathData.setColorOffset((tessellatedColors != null ? 3 : 0));
//		pathData.setVertexStride(elemsPerPoint);
//
//		return path;
//	}

	@Override
	public void setTourTrack(final TourTrack tourTrack, final TourTrackConfig tourTrackConfig) {

		_tourTrack = tourTrack;
		_tourTrackConfig = tourTrackConfig;
	}
}
