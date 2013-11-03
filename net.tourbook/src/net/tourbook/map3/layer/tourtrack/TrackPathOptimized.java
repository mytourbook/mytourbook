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

import net.tourbook.common.color.ColorUtil;
import net.tourbook.map3.shape.MTMultiResolutionPath;

import org.eclipse.swt.graphics.RGB;

import com.sun.opengl.util.BufferUtil;

public class TrackPathOptimized extends MTMultiResolutionPath implements ITrackPath {

	private static final int	SKIP_COUNTER		= 30;

	private TourTrack			_tourTrack;
	private TourTrackConfig		_tourTrackConfig;

	private static final String	ARROW_BORDER_KEY	= TrackPathOptimized.class.getName() + ".ArrowBorder";
	private static final String	ARROW_POSITION_KEY	= TrackPathOptimized.class.getName() + ".ArrowPosition";
	private static final String	ARROW_SURFACE_KEY	= TrackPathOptimized.class.getName() + ".ArrowSurface";

	/** The length, in meters, of the arrowhead, from tip to base. */
	protected double			_arrowLength		= 30.0;

	/** The angle of the arrowhead tip. */
	protected Angle				_arrowAngle			= Angle.fromDegrees(30.0);

	private TIntArrayList		_arrowPositionIndizes;

	private int					_arrowPositionVboId;
	private int					_arrowSurfaceVboId;
	private int					_arrowBorderVboId;

	private int					_numberOfDirectionArrows;

	public TrackPathOptimized(final ArrayList<TourMap3Position> trackPositions) {
		super(trackPositions);
	}

	@Override
	protected FloatBuffer computeAbsolutePoints(final DrawContext dc,
												final List<Position> positions,
												final FloatBuffer path,
												final PathData pathData) {

		if (_tourTrackConfig.isShowDirectionArrows) {

			computeArrowPositions(dc, positions, pathData);
			computeDirectionArrows(dc, pathData);
		}

		return super.computeAbsolutePoints(dc, positions, path, pathData);
	}

	private void computeArrowPositions(final DrawContext dc, final List<Position> positions, final PathData pathData) {

		final int elemsPerPoint = 3;
		final int positionSize = positions.size();
		final int polePositionSize = (positionSize / SKIP_COUNTER) + 3;
		final int numPoints = polePositionSize * 1;

		final int bufferSize = elemsPerPoint * numPoints;
		FloatBuffer arrowPositions = (FloatBuffer) pathData.getValue(ARROW_POSITION_KEY);

		if (arrowPositions == null || arrowPositions.capacity() < bufferSize) {
			arrowPositions = BufferUtil.newFloatBuffer(bufferSize);
		}
		pathData.setValue(ARROW_POSITION_KEY, arrowPositions);
		arrowPositions.clear();

		if (_arrowPositionIndizes == null || _arrowPositionIndizes.size() < polePositionSize) {
			_arrowPositionIndizes = new TIntArrayList(polePositionSize);
		}
		_arrowPositionIndizes.clear();

		final Globe globe = dc.getGlobe();
		final Vec4 referencePoint = pathData.getReferencePoint();

		double verticalExaggeration = 1;
		final boolean isVerticalExaggeration = dc.getVerticalExaggeration() != 1;
		if (isVerticalExaggeration) {
			verticalExaggeration = dc.getVerticalExaggeration();
		}

		final double poleHeight = pathData.getEyeDistance() / (100.0 / _tourTrackConfig.directionArrowDistance * 1.5);

		for (int posIndex = 0; posIndex < positionSize; posIndex++) {

			if (posIndex % SKIP_COUNTER == 0 || posIndex == 0 || posIndex == positionSize - 1) {

				_arrowPositionIndizes.add(posIndex);

				final Position geoPosition = positions.get(posIndex);
				final double altitude = geoPosition.getAltitude() * verticalExaggeration;

				// create arrow position vertex
				final Vec4 pt = globe.computePointFromPosition(//
						geoPosition.getLatitude(),
						geoPosition.getLongitude(),
						altitude + poleHeight);

				putVertexIntoBuffer(arrowPositions, pt, referencePoint);
			}
		}

		// since the buffer is reused the limit might not be the same as the previous usage
		arrowPositions.flip();
	}

	private void computeDirectionArrows(final DrawContext dc, final PathData pathData) {

		final int numPositions = _arrowPositionIndizes.size();
		if (numPositions < 2) {
			return;
		}

		/*
		 * Arrow surface
		 */
		final int FLOATS_PER_ARROW_SURFACE = 9; // 3 points * 3 coordinates per point
		FloatBuffer arrowSurfaceBuffer = (FloatBuffer) pathData.getValue(ARROW_SURFACE_KEY);
		if (arrowSurfaceBuffer == null || arrowSurfaceBuffer.capacity() < numPositions * FLOATS_PER_ARROW_SURFACE) {
			arrowSurfaceBuffer = BufferUtil.newFloatBuffer(FLOATS_PER_ARROW_SURFACE * numPositions);
		}
		pathData.setValue(ARROW_SURFACE_KEY, arrowSurfaceBuffer);
		arrowSurfaceBuffer.clear();

		/*
		 * Arrow border
		 */
		final int FLOATS_PER_ARROW_BORDER = 18; // 6 points * 3 coordinates per point
		FloatBuffer arrowBorderBuffer = (FloatBuffer) pathData.getValue(ARROW_BORDER_KEY);
		if (arrowBorderBuffer == null || arrowBorderBuffer.capacity() < numPositions * FLOATS_PER_ARROW_BORDER) {
			arrowBorderBuffer = BufferUtil.newFloatBuffer(FLOATS_PER_ARROW_BORDER * numPositions);
		}
		pathData.setValue(ARROW_BORDER_KEY, arrowBorderBuffer);
		arrowBorderBuffer.clear();

		final Terrain terrain = dc.getTerrain();

		final FloatBuffer arrowPositions = (FloatBuffer) pathData.getValue(ARROW_POSITION_KEY);
		arrowPositions.rewind();

		final List<Position> tessellatedPositions = pathData.getTessellatedPositions();

		int currentPositionIndex = _arrowPositionIndizes.get(0);
		final Position poleA = tessellatedPositions.get(currentPositionIndex);
		Vec4 polePtA = computePoint(terrain, poleA);

		final Vec4 referencePoint = pathData.getReferencePoint();

		for (int poleIndex = 1; poleIndex < _arrowPositionIndizes.size(); poleIndex++) {

			final int nextPositionIndex = _arrowPositionIndizes.get(poleIndex);

			final Position poleB = tessellatedPositions.get(nextPositionIndex);

			final Vec4 polePtB = computePoint(terrain, poleB);

			final int poleIndexOffset = (poleIndex - 1) * 3;

			final Vec4 arrowPole = new Vec4(
					arrowPositions.get(poleIndexOffset + 0) + referencePoint.x,
					arrowPositions.get(poleIndexOffset + 1) + referencePoint.y,
					arrowPositions.get(poleIndexOffset + 2) + referencePoint.z);

			computeDirectionArrows_ArrowheadGeometry(
					dc,
					polePtA,
					polePtB,
					arrowPole,
					arrowSurfaceBuffer,
					arrowBorderBuffer,
					pathData);

			currentPositionIndex = nextPositionIndex;
			polePtA = polePtB;
		}

		// hide unused arrows
		arrowSurfaceBuffer.flip();
		arrowBorderBuffer.flip();

		_numberOfDirectionArrows = arrowSurfaceBuffer.limit() / 3;
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
	 * @param arrowSurfaceBuffer
	 *            buffer in which to place computed points
	 * @param arrowBorderBuffer
	 * @param pathData
	 *            the current globe-specific path data.
	 */
	private void computeDirectionArrows_ArrowheadGeometry(	final DrawContext dc,
															final Vec4 polePtA,
															final Vec4 polePtB,
															final Vec4 arrowPole,
															final FloatBuffer arrowSurfaceBuffer,
															final FloatBuffer arrowBorderBuffer,
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

		double arrowLength = _arrowLength;
		double arrowBase = arrowLength * _arrowAngle.tanHalfAngle();

		arrowLength = _tourTrackConfig.directionArrowSize * pixelSize;
		arrowBase = arrowLength * _arrowAngle.tanHalfAngle();

		// Don't draw an arrowhead if the path segment is smaller than the arrow
		if (poleDistance <= arrowLength) {
			return;
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

		final Vec4 referencePoint = pathData.getReferencePoint();

		// Add geometry to the buffer
		putVertexIntoBuffer(arrowSurfaceBuffer, vertex1, referencePoint);
		putVertexIntoBuffer(arrowSurfaceBuffer, vertex2, referencePoint);
		putVertexIntoBuffer(arrowSurfaceBuffer, midPoint, referencePoint);

		// add border to the buffer
		putVertexIntoBuffer(arrowBorderBuffer, vertex1, referencePoint);
		putVertexIntoBuffer(arrowBorderBuffer, vertex2, referencePoint);

		putVertexIntoBuffer(arrowBorderBuffer, vertex2, referencePoint);
		putVertexIntoBuffer(arrowBorderBuffer, midPoint, referencePoint);

		putVertexIntoBuffer(arrowBorderBuffer, midPoint, referencePoint);
		putVertexIntoBuffer(arrowBorderBuffer, vertex1, referencePoint);
	}

	@Override
	protected void doDrawInteriorVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		/*
		 * Interior vertex color is painted in drawInteriorVertexColorVBO(...) to solve z fighting
		 * problems.
		 */
		if (isShowInteriorVertexColor(dc, pathData) == false) {

			// draw with solid color

			final Color solidColor = getSolidColor_Interior();

			if (!dc.isPickingMode() && solidColor != null) {

				// set solid color

				dc.getGL().glColor4ub(
						(byte) solidColor.getRed(),
						(byte) solidColor.getGreen(),
						(byte) solidColor.getBlue(),
						(byte) solidColor.getAlpha());
			}

			super.doDrawInteriorVBO(dc, vboIds, pathData);
		}

	}

	@Override
	protected void doDrawOutlineVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		final boolean isPickingMode = dc.isPickingMode();

		final boolean isTourSelected = _tourTrack.isSelected();
		final boolean isTourHovered = _tourTrack.isHovered();

		final GL gl = dc.getGL();

		try {

			final int vertexStride = pathData.getVertexStride();
			final int vertexCount = pathData.getVertexCount();
			final boolean hasExtrusionPoints = pathData.isHasExtrusionPoints();

			final int stride = hasExtrusionPoints ? 2 * vertexStride : vertexStride;
			final int count = hasExtrusionPoints ? vertexCount / 2 : vertexCount;

			final boolean isShowTrackValueColor = isShowTrackValueColor_Outline();

			final boolean useVertexColors = !isPickingMode
					&& pathData.getTessellatedColors() != null
					&& isShowTrackValueColor;

			if (!isPickingMode && !isShowTrackValueColor) {

				RGB rgb;

				if (isTourHovered && isTourSelected) {
					rgb = _tourTrackConfig.outlineColor_HovSel;
				} else if (isTourHovered) {
					rgb = _tourTrackConfig.outlineColor_Hovered;
				} else if (isTourSelected) {
					rgb = _tourTrackConfig.outlineColor_Selected;
				} else {
					rgb = _tourTrackConfig.outlineColor;
				}

				gl.glColor4ub((byte) rgb.red, (byte) rgb.green, (byte) rgb.blue, (byte) 0xbf);
			}

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

			if (hasExtrusionPoints && isDrawVerticals()) {
				drawVerticalOutlineVBO(dc, vboIds, pathData);
			}

			if (isShowPositions()) {
				drawPointsVBO(dc, vboIds, pathData);
			}

			if (isShowInteriorVertexColor(dc, pathData)) {
				// Interior with vertex color must be painted here otherwise there is a z depth problem !!!
				drawInteriorVertexColorVBO(dc, vboIds, pathData);
			}

			if (!isPickingMode && _tourTrackConfig.isShowDirectionArrows) {
				drawDirectionArrows(dc, vboIds, pathData);
			}

		} finally {
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
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
	 * @param vboIds
	 * @param pathData
	 *            the current globe-specific path data.
	 */
	private void drawDirectionArrows(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		if (_numberOfDirectionArrows < 1) {
			return;
		}

		RGB rgb = null;
		final boolean isTourSelected = _tourTrack.isSelected();
		final boolean isTourHovered = _tourTrack.isHovered();

		if (isTourHovered && isTourSelected) {
			rgb = _tourTrackConfig.outlineColor_HovSel;
		} else if (isTourHovered) {
			rgb = _tourTrackConfig.outlineColor_Hovered;
		} else if (isTourSelected) {
			rgb = _tourTrackConfig.outlineColor_Selected;
		}

		if (rgb == null) {
			return;
		}

		final GL gl = dc.getGL();
		boolean isProjectionOffsetPushed = false; // keep track for error recovery

		try {

			if (isSurfacePath()) {

				// Pull the arrow triangles forward just a bit to ensure they show over the terrain.
				dc.pushProjectionOffest(SURFACE_PATH_DEPTH_OFFSET);
				gl.glDepthMask(false);
				isProjectionOffsetPushed = true;
			}

			final int opacity = 0xff;

			// draw triangle
			gl.glColor4ub((byte) rgb.red, (byte) rgb.green, (byte) rgb.blue, (byte) opacity);

			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[_arrowSurfaceVboId]);

			// Convert stride from number of elements to number of bytes.
			gl.glVertexPointer(3, GL.GL_FLOAT, 4 * 3, 0);
			gl.glDrawArrays(GL.GL_TRIANGLES, 0, _numberOfDirectionArrows);

			// draw border
			final Color borderColor = ColorUtil.getContrastColor(rgb.red, rgb.green, rgb.blue);
			gl.glLineWidth(2.0f);
			gl.glColor4ub(
					(byte) borderColor.getRed(),
					(byte) borderColor.getGreen(),
					(byte) borderColor.getBlue(),
					(byte) opacity);

			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[_arrowBorderVboId]);

			// Convert stride from number of elements to number of bytes.
			gl.glVertexPointer(3, GL.GL_FLOAT, 4 * 3, 0);
			gl.glDrawArrays(GL.GL_LINES, 0, _numberOfDirectionArrows * 2);

		} finally {

			if (isProjectionOffsetPushed) {
				dc.popProjectionOffest();
				gl.glDepthMask(true);
			}
		}
	}

	private void drawInteriorVertexColorVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		final GL gl = dc.getGL();

		final int vertexStride = pathData.getVertexStride();

		// Convert stride from number of elements to number of bytes.
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[0]);
		{
			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			{
				gl.glVertexPointer(3, GL.GL_FLOAT, 4 * vertexStride, 0);
				gl.glColorPointer(4, GL.GL_FLOAT, 4 * vertexStride, 4 * pathData.getColorOffset());

				gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, pathData.getVertexCount());
			}
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
		}
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	}

	/**
	 * Draws points at this path's specified positions.
	 * <p/>
	 * Note: when the draw context is in picking mode, this binds the current GL_ARRAY_BUFFER to 0
	 * after using the currently bound GL_ARRAY_BUFFER to specify the vertex pointer. This does not
	 * restore GL_ARRAY_BUFFER to the its previous state. If the caller intends to use that buffer
	 * after this method returns, the caller must bind the buffer again.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param vboIds
	 *            the ids of this shapes buffers.
	 * @param pathData
	 *            the current globe-specific path data.
	 */
	@Override
	protected void drawPointsVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		if (_tourTrackConfig.isShowTrackPosition == false) {
			return;
		}

		final double d = getDistanceMetric(dc, pathData);
		if (d > getShowPositionsThreshold()) {
			return;
		}

		final IntBuffer posPoints = pathData.getPositionPoints();
		if (posPoints == null || posPoints.limit() < 1) {
			return;
		}

		final boolean useVertexColors = pathData.getTessellatedColors() != null && isShowTrackValueColor_Outline();

		final GL gl = dc.getGL();

		// Convert stride from number of elements to number of bytes.
		gl.glVertexPointer(3, GL.GL_FLOAT, 4 * pathData.getVertexStride(), 0);

		if (dc.isPickingMode()) {

			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
			gl.glColorPointer(3, GL.GL_UNSIGNED_BYTE, 0, pickPositionColors);

		} else if (useVertexColors) {
			// Apply this path's per-position colors if we're in normal rendering mode (not picking) and this path's
			// positionColors is non-null. Convert the stride and offset from number of elements to number of bytes.
			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL.GL_FLOAT, 4 * pathData.getVertexStride(), 4 * pathData.getColorOffset());
		}

		prepareToDrawPoints(dc);

		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboIds[2]);
		gl.glDrawElements(GL.GL_POINTS, posPoints.limit(), GL.GL_UNSIGNED_INT, 0);

		// Restore the previous GL point state.
		gl.glPointSize(1f);
		gl.glDisable(GL.GL_POINT_SMOOTH);

		// Restore the previous GL color array state.
		if (dc.isPickingMode() || useVertexColors) {
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
		}
	}

	@Override
	protected void fillVBO(final DrawContext dc) {

		final PathData pathData = getCurrentPathData();

		int numberOfIds = isShowPositions() ? //
				3
				: pathData.isHasExtrusionPoints() && isDrawVerticals() ? //
						2
						: 1;

		// show arrow keys
		if (_tourTrackConfig.isShowDirectionArrows) {
			numberOfIds += 3;
		}

		int[] vboIds = (int[]) dc.getGpuResourceCache().get(pathData.getVboCacheKey());
		if (vboIds != null && vboIds.length != numberOfIds) {
			clearCachedVbos(dc);
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

		if (_tourTrackConfig.isShowDirectionArrows) {
			iSize += ((FloatBuffer) pathData.getValue(ARROW_POSITION_KEY)).limit() * 4;
			iSize += ((FloatBuffer) pathData.getValue(ARROW_SURFACE_KEY)).limit() * 4;
			iSize += ((FloatBuffer) pathData.getValue(ARROW_BORDER_KEY)).limit() * 4;
		}

		if (vboIds == null) {

			vboIds = new int[numberOfIds];

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

			if (_tourTrackConfig.isShowDirectionArrows) {

				int vboIndex = numberOfIds - 3;

				// create vbo for direction arrows
				_arrowPositionVboId = vboIndex++;
				FloatBuffer fb = (FloatBuffer) pathData.getValue(ARROW_POSITION_KEY);
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[_arrowPositionVboId]);
				gl.glBufferData(GL.GL_ARRAY_BUFFER, fb.limit() * 4, fb.rewind(), GL.GL_STATIC_DRAW);

				_arrowSurfaceVboId = vboIndex++;
				fb = (FloatBuffer) pathData.getValue(ARROW_SURFACE_KEY);
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[_arrowSurfaceVboId]);
				gl.glBufferData(GL.GL_ARRAY_BUFFER, fb.limit() * 4, fb.rewind(), GL.GL_STATIC_DRAW);

				_arrowBorderVboId = vboIndex++;
				fb = (FloatBuffer) pathData.getValue(ARROW_BORDER_KEY);
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[_arrowBorderVboId]);
				gl.glBufferData(GL.GL_ARRAY_BUFFER, fb.limit() * 4, fb.rewind(), GL.GL_STATIC_DRAW);
			}

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

	/**
	 * @return Returns <code>null</code> when vertex color should be use for painting otherwise it
	 *         returns the solid color.
	 */
	private Color getSolidColor_Interior() {

		final boolean isTourSelected = _tourTrack.isSelected();
		final boolean isTourHovered = _tourTrack.isHovered();

		RGB solidColor = null;
		float interiorOpacity = 0;

		if (isTourHovered && isTourSelected) {

			if (_tourTrackConfig.interiorColorMode_HovSel == TourTrackConfig.COLOR_MODE_SOLID_COLOR) {

				solidColor = _tourTrackConfig.interiorColor_HovSel;
				interiorOpacity = (float) (_tourTrackConfig.interiorOpacity_HovSel);
			}

		} else if (isTourHovered) {

			if (_tourTrackConfig.interiorColorMode_Hovered == TourTrackConfig.COLOR_MODE_SOLID_COLOR) {

				solidColor = _tourTrackConfig.interiorColor_Hovered;
				interiorOpacity = (float) (_tourTrackConfig.interiorOpacity_Hovered);
			}

		} else if (isTourSelected) {

			if (_tourTrackConfig.interiorColorMode_Selected == TourTrackConfig.COLOR_MODE_SOLID_COLOR) {
				solidColor = _tourTrackConfig.interiorColor_Selected;
				interiorOpacity = (float) (_tourTrackConfig.interiorOpacity_Selected);
			}

		} else {

			if (_tourTrackConfig.interiorColorMode == TourTrackConfig.COLOR_MODE_SOLID_COLOR) {
				solidColor = _tourTrackConfig.interiorColor;
				interiorOpacity = (float) (_tourTrackConfig.interiorOpacity);
			}
		}

		if (solidColor == null) {
			return null;
		}

		return new Color(//
				solidColor.red / 255.0f,
				solidColor.green / 255.0f,
				solidColor.blue / 255.0f,
				interiorOpacity);
	}

	@Override
	public TourTrack getTourTrack() {
		return _tourTrack;
	}

	private boolean isShowInteriorVertexColor(final DrawContext dc, final PathData pathData) {

		final boolean useVertexColors = !dc.isPickingMode()
				&& pathData.getTessellatedColors() != null
				&& getSolidColor_Interior() == null;

		return useVertexColors;
	}

	/**
	 * @return Returns <code>true</code> when the track value color should be used to paint a track
	 *         or curtain.
	 */
	private boolean isShowTrackValueColor_Outline() {

		boolean isShowTrackValue;

		final boolean isTourSelected = _tourTrack.isSelected();
		final boolean isTourHovered = _tourTrack.isHovered();

		if (isTourHovered && isTourSelected) {

			isShowTrackValue = _tourTrackConfig.outlineColorMode_HovSel == TourTrackConfig.COLOR_MODE_TRACK_VALUE;

		} else if (isTourHovered) {

			isShowTrackValue = _tourTrackConfig.outlineColorMode_Hovered == TourTrackConfig.COLOR_MODE_TRACK_VALUE;

		} else if (isTourSelected) {

			isShowTrackValue = _tourTrackConfig.outlineColorMode_Selected == TourTrackConfig.COLOR_MODE_TRACK_VALUE;

		} else {

			isShowTrackValue = _tourTrackConfig.outlineColorMode == TourTrackConfig.COLOR_MODE_TRACK_VALUE;
		}

		return isShowTrackValue;
	}

	private void putVertexIntoBuffer(final FloatBuffer buffer, final Vec4 vertex, final Vec4 referencePoint) {

		buffer.put((float) (vertex.x - referencePoint.x));
		buffer.put((float) (vertex.y - referencePoint.y));
		buffer.put((float) (vertex.z - referencePoint.z));
	}

	@Override
	public void setExpired() {

		final AbstractShapeData pathData = getCurrentData();

		// it was null when implementing and testing
		if (pathData != null) {
			pathData.setExpired(true);
		}
	}

	@Override
	public void setPicked(final boolean isHovered, final Integer pickPositionIndex) {

		_tourTrack.setHovered(isHovered, pickPositionIndex);
	}

	@Override
	public void setTourTrack(final TourTrack tourTrack, final TourTrackConfig tourTrackConfig) {

		_tourTrack = tourTrack;
		_tourTrackConfig = tourTrackConfig;
	}

	@Override
	protected boolean shouldUseVBOs(final DrawContext dc) {

//		draw with VA is not yet implemented !!!
//		return this.getCurrentPathData().getTessellatedPositions().size() > VBO_THRESHOLD && super.shouldUseVBOs(dc);

		return true;
	}
}
