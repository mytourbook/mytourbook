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

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Path;

import java.awt.Color;
import java.util.ArrayList;

import javax.media.opengl.GL;

import net.tourbook.map3.shape.MTMultiResolutionPath;

public class TrackPathOptimized extends MTMultiResolutionPath implements ITrackPath {

	private TourTrack	_tourTrack;

	public TrackPathOptimized(final ArrayList<TourMap3Position> trackPositions) {
		super(trackPositions);
	}

	@Override
	protected void doDrawInteriorVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		final GL gl = dc.getGL();

//		if (_tourTrack.isSelected() || _tourTrack.isHovered()) {
//
//			try {
//
//				final int stride = pathData.getVertexStride();
//				final boolean useVertexColors = !dc.isPickingMode() && pathData.getTessellatedColors() != null;
//
//				// Convert stride from number of elements to number of bytes.
//				gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[0]);
//				gl.glVertexPointer(3, GL.GL_FLOAT, 4 * stride, 0);
//
//				// Apply this path's per-position colors if we're in normal rendering mode (not picking) and this path's
//				// positionColors is non-null.
//				if (useVertexColors) {
//					// Convert stride and offset from number of elements to number of bytes.
//					gl.glEnableClientState(GL.GL_COLOR_ARRAY);
//					gl.glColorPointer(4, GL.GL_FLOAT, 4 * stride, 4 * pathData.getColorOffset());
//				}
//
//				gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, pathData.getVertexCount());
//
//				if (useVertexColors) {
//					gl.glDisableClientState(GL.GL_COLOR_ARRAY);
//				}
//
//			} finally {
//
//				gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
//				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
//			}
//
//		} else {
//
//			// Convert stride from number of elements to number of bytes.
//			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[0]);
//
//			gl.glVertexPointer(3, GL.GL_FLOAT, 4 * pathData.getVertexStride(), 0);
//			gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, pathData.getVertexCount());
//
//			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
//		}

			// Convert stride from number of elements to number of bytes.
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[0]);

			gl.glVertexPointer(3, GL.GL_FLOAT, 4 * pathData.getVertexStride(), 0);
			gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, pathData.getVertexCount());

			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	}

	@Override
	protected void doDrawOutlineVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		final GL gl = dc.getGL();

		try {

			final int vertexStride = pathData.getVertexStride();
			final int vertexCount = pathData.getVertexCount();
			final boolean hasExtrusionPoints = pathData.isHasExtrusionPoints();

			final int stride = hasExtrusionPoints ? 2 * vertexStride : vertexStride;
			final int count = hasExtrusionPoints ? vertexCount / 2 : vertexCount;

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

			gl.glDrawArrays(GL.GL_LINE_STRIP, 0, count);

//			if (_tourTrack.isSelected() || _tourTrack.isHovered()) {
				drawColoredWallVBO(dc, vboIds, pathData);
//			}

			if (useVertexColors) {
				gl.glDisableClientState(GL.GL_COLOR_ARRAY);
			}

			if (hasExtrusionPoints && this.isDrawVerticals()) {
				drawVerticalOutlineVBO(dc, vboIds, pathData);
			}

			if (isShowPositions()) {
				drawPointsVBO(dc, vboIds, pathData);
			}

		} finally {
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
	}

	private void drawColoredWallVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		final GL gl = dc.getGL();

		try {

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

		} finally {

//			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
//			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
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
	public void setTourTrack(final TourTrack tourTrack) {
		_tourTrack = tourTrack;
	}
}
