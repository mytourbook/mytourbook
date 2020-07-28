/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import com.jogamp.common.nio.Buffers;

import gnu.trove.list.array.TIntArrayList;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.terrain.Terrain;

import java.awt.Color;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL2;

import net.tourbook.common.color.ColorUtil;
import net.tourbook.map3.view.Map3View;

import org.eclipse.swt.graphics.RGB;

public class TrackPathOptimized extends MTMultiResolutionPath implements ITrackPath {

   private static final int    SKIP_COUNTER       = 30;

   private static final String ARROW_BORDER_KEY   = TrackPathOptimized.class.getName() + ".ArrowBorder";   //$NON-NLS-1$
   private static final String ARROW_POSITION_KEY = TrackPathOptimized.class.getName() + ".ArrowPosition"; //$NON-NLS-1$
   private static final String ARROW_SURFACE_KEY  = TrackPathOptimized.class.getName() + ".ArrowSurface";  //$NON-NLS-1$

   private TourTrack           _tourTrack;

   /** The length, in meters, of the arrowhead, from tip to base. */
   protected double            _arrowLength       = 30.0;

   /** The angle of the arrowhead tip. */
   protected Angle             _arrowAngle        = Angle.fromDegrees(30.0);

   private TIntArrayList       _arrowPositionIndizes;

   private int                 _arrowPositionVboId;
   private int                 _arrowSurfaceVboId;
   private int                 _arrowBorderVboId;

   private int                 _numberOfDirectionArrows;

   public TrackPathOptimized(final ArrayList<TourMap3Position> trackPositions) {
      super(trackPositions);
   }

   @Override
   protected FloatBuffer computeAbsolutePoints(final DrawContext dc,
                                               final List<Position> positions,
                                               final FloatBuffer path,
                                               final PathData pathData) {

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      if (config.isShowDirectionArrows) {

         computeArrowPositions(dc, positions, pathData);
         computeDirectionArrows(dc, pathData);
      }

      return computeAbsolutePoints_WithOffset(dc, positions, path, pathData);
   }

   /**
    * This is a copy from {@link Path#computeAbsolutePoints} with altitude adjustment.
    *
    * @param dc
    * @param positions
    * @param path
    * @param pathData
    * @return
    */
   private FloatBuffer computeAbsolutePoints_WithOffset(final DrawContext dc,
                                                        final List<Position> positions,
                                                        FloatBuffer path,
                                                        final PathData pathData) {

      final double eyeElevation = dc.getView().getEyePosition().getElevation();

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      final boolean isAbsoluteAltitudeMode = config.altitudeMode == WorldWind.ABSOLUTE;
      final boolean isAltitudeOffset = config.isAltitudeOffset;
      final boolean isOffsetModeAbsolute = config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_ABSOLUTE;
      final boolean isOffsetModeRelative = config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_RELATIVE;

      final int relativeOffset = config.altitudeOffsetDistanceRelative;

      double altitudeOffset = 0;

      if (isAbsoluteAltitudeMode && isAltitudeOffset) {

         if (isOffsetModeAbsolute) {

            altitudeOffset = config.altitudeOffsetDistanceAbsolute;

         } else if (isOffsetModeRelative && relativeOffset > 0) {

            altitudeOffset = eyeElevation / 100.0 * relativeOffset;
         }

         if (config.isAltitudeOffsetRandom) {

            /*
             * The random value should be kept that other objects, e.g. map chart slider is
             * correctly positioned.
             */
            altitudeOffset *= Math.random() + 0.1;
         }
      }

      altitudeOffset *= dc.getVerticalExaggeration();

      final List<Color> tessellatedColors = pathData.getTessellatedColors();

      final int numPoints = this.isExtrude() ? 2 * positions.size() : positions.size();
      final int elemsPerPoint = (tessellatedColors != null ? 7 : 3);
      final Iterator<Color> colorIter = (tessellatedColors != null ? tessellatedColors.iterator() : null);
      final float[] color = (tessellatedColors != null ? new float[4] : null);

      if (path == null || path.capacity() < elemsPerPoint * numPoints) {
         path = Buffers.newDirectFloatBuffer(elemsPerPoint * numPoints);
      }
      path.clear();

      final Globe globe = dc.getGlobe();
      final Vec4 referencePoint = pathData.getReferencePoint();
      final double ve = dc.getVerticalExaggeration();

      for (final Position pos : positions) {

         final double altitude = pos.getAltitude();

         final Vec4 pt = globe.computePointFromPosition(//
               pos.getLatitude(),
               pos.getLongitude(),
               ve * altitude + altitudeOffset);

         path.put((float) (pt.x - referencePoint.x));
         path.put((float) (pt.y - referencePoint.y));
         path.put((float) (pt.z - referencePoint.z));

         if (colorIter != null && colorIter.hasNext()) {
            colorIter.next().getRGBComponents(color);
            path.put(color);
         }

         if (this.isExtrude()) {
            this.appendTerrainPoint(dc, pos, color, path, pathData);
         }
      }

      pathData.setColorOffset(tessellatedColors != null ? 3 : 0);
      pathData.setVertexStride(elemsPerPoint);

      return path;
   }

   private void computeArrowPositions(final DrawContext dc, final List<Position> positions, final PathData pathData) {

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \tcomputeArrowPositions()");
//		// TODO remove SYSTEM.OUT.PRINTLN

      final int elemsPerPoint = 3;
      final int positionSize = positions.size();
      final int polePositionSize = (positionSize / SKIP_COUNTER) + 3;
      final int numPoints = polePositionSize * 1;

      final int bufferSize = elemsPerPoint * numPoints;
      FloatBuffer arrowPositions = (FloatBuffer) pathData.getValue(ARROW_POSITION_KEY);

      if (arrowPositions == null || arrowPositions.capacity() < bufferSize) {
         arrowPositions = Buffers.newDirectFloatBuffer(bufferSize);
      }
      pathData.setValue(ARROW_POSITION_KEY, arrowPositions);
      ((Buffer) arrowPositions).clear();
      //https://stackoverflow.com/questions/48693695/java-nio-buffer-not-loading-clear-method-on-runtime

      if (_arrowPositionIndizes == null || _arrowPositionIndizes.size() < polePositionSize) {
         _arrowPositionIndizes = new TIntArrayList(polePositionSize);
      }
      _arrowPositionIndizes.clear();

      final Globe globe = dc.getGlobe();
      final Vec4 referencePoint = pathData.getReferencePoint();

      // vertical exaggeration
      final double verticalExaggeration = dc.getVerticalExaggeration();

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      final int altitudeMode = config.altitudeMode;
      final double altitudeOffset = Map3View.getAltitudeOffset(dc.getView().getEyePosition());

      final double poleHeight = pathData.getEyeDistance() / (100.0 / config.directionArrowDistance * 1.5);
//		poleHeight *= verticalExaggeration;

      for (int posIndex = 0; posIndex < positionSize; posIndex++) {

         if (posIndex % SKIP_COUNTER == 0 || posIndex == 0 || posIndex == positionSize - 1) {

            _arrowPositionIndizes.add(posIndex);

            final Position geoPosition = positions.get(posIndex);
            final double trackAltitude = (geoPosition.getAltitude() + altitudeOffset) * verticalExaggeration;

            // create arrow position vertex
            Vec4 pt;
            if (altitudeMode == WorldWind.CLAMP_TO_GROUND) {

               pt = dc.computeTerrainPoint(geoPosition.getLatitude(), geoPosition.getLongitude(), 0 + poleHeight);

            } else if (altitudeMode == WorldWind.RELATIVE_TO_GROUND) {

               pt = dc.computeTerrainPoint(//
                     geoPosition.getLatitude(),
                     geoPosition.getLongitude(),
                     trackAltitude + poleHeight);

            } else { // WorldWind.ABSOLUTE

               pt = globe.computePointFromPosition(//
                     geoPosition.getLatitude(),
                     geoPosition.getLongitude(),
                     trackAltitude + poleHeight);

            }

            putVertexIntoBuffer(arrowPositions, pt, referencePoint);
         }
      }

      // since the buffer is reused the limit might not be the same as the previous usage
      ((Buffer) arrowPositions).flip();
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
         arrowSurfaceBuffer = Buffers.newDirectFloatBuffer(FLOATS_PER_ARROW_SURFACE * numPositions);
      }
      pathData.setValue(ARROW_SURFACE_KEY, arrowSurfaceBuffer);
      arrowSurfaceBuffer.clear();

      /*
       * Arrow border
       */
      final int FLOATS_PER_ARROW_BORDER = 18; // 6 points * 3 coordinates per point
      FloatBuffer arrowBorderBuffer = (FloatBuffer) pathData.getValue(ARROW_BORDER_KEY);
      if (arrowBorderBuffer == null || arrowBorderBuffer.capacity() < numPositions * FLOATS_PER_ARROW_BORDER) {
         arrowBorderBuffer = Buffers.newDirectFloatBuffer(FLOATS_PER_ARROW_BORDER * numPositions);
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

         computeDirectionArrows_ArrowHeadGeometry(
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
      ((Buffer) arrowSurfaceBuffer).flip();
      ((Buffer) arrowBorderBuffer).flip();

      _numberOfDirectionArrows = arrowSurfaceBuffer.limit() / 3;
   }

   /**
    * Compute the geometry of a direction arrow between two points.
    *
    * @param dc
    *           current draw context
    * @param polePtA
    *           the first pole position. This is one of the application defined path positions.
    * @param polePtB
    *           second pole position
    * @param ptA
    *           first position of the tessellated path midway between the poles
    * @param ptB
    *           second position of the tessellated path midway between the poles
    * @param arrowSurfaceBuffer
    *           buffer in which to place computed points
    * @param arrowBorderBuffer
    * @param pathData
    *           the current globe-specific path data.
    */
   private void computeDirectionArrows_ArrowHeadGeometry(final DrawContext dc,
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

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      double arrowLength = _arrowLength;
      double arrowBase = arrowLength * _arrowAngle.tanHalfAngle();

      arrowLength = config.directionArrowSize * pixelSize;
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
   protected FloatBuffer computePointsRelativeToTerrain(final DrawContext dc,
                                                        final List<Position> positions,
                                                        final Double altitude,
                                                        final FloatBuffer path,
                                                        final PathData pathData) {

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      if (config.isShowDirectionArrows) {

         computeArrowPositions(dc, positions, pathData);
         computeDirectionArrows(dc, pathData);
      }

      return super.computePointsRelativeToTerrain(dc, positions, altitude, path, pathData);
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

            dc.getGL().getGL2().glColor4ub(
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

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      final boolean isPickingMode = dc.isPickingMode();

      final boolean isTourSelected = _tourTrack.isSelected();
      final boolean isTourHovered = _tourTrack.isHovered();

      final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

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
            double alpha;

            if (isTourHovered && isTourSelected) {
               rgb = config.outlineColor_HovSel;
               alpha = config.outlineOpacity_HovSel;
            } else if (isTourHovered) {
               rgb = config.outlineColor_Hovered;
               alpha = config.outlineOpacity_Hovered;
            } else if (isTourSelected) {
               rgb = config.outlineColor_Selected;
               alpha = config.outlineOpacity_Selected;
            } else {
               rgb = config.outlineColor;
               alpha = config.outlineOpacity;

               // enforce better visibility
//					alpha = 1;
            }

            gl.glColor4ub((byte) rgb.red, (byte) rgb.green, (byte) rgb.blue, (byte) (alpha * 0xff));
         }

         // Convert stride from number of elements to number of bytes.
         gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds[0]);
         gl.glVertexPointer(3, GL2.GL_FLOAT, 4 * stride, 0);

         // Apply this path's per-position colors if we're in normal rendering mode (not picking) and this path's
         // positionColors is non-null.
         if (useVertexColors) {

            // Convert stride and offset from number of elements to number of bytes.
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
            gl.glColorPointer(4, GL2.GL_FLOAT, 4 * stride, 4 * pathData.getColorOffset());
         }

         gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, count);

         if (useVertexColors) {
            gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
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

         if (!isPickingMode && config.isShowDirectionArrows) {
            drawDirectionArrows(dc, vboIds, pathData);
         }

      } finally {
         gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
         gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
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
    *           Current draw context.
    * @param vboIds
    * @param pathData
    *           the current globe-specific path data.
    */
   private void drawDirectionArrows(final DrawContext dc, final int[] vboIds, final PathData pathData) {

      if (_numberOfDirectionArrows < 1) {
         return;
      }

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();
      RGB rgb = null;
      final boolean isTourSelected = _tourTrack.isSelected();
      final boolean isTourHovered = _tourTrack.isHovered();

      if (isTourHovered && isTourSelected) {
         rgb = config.outlineColor_HovSel;
      } else if (isTourHovered) {
         rgb = config.outlineColor_Hovered;
      } else if (isTourSelected) {
         rgb = config.outlineColor_Selected;
      }

      if (rgb == null) {
         return;
      }

      final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

      boolean isProjectionOffsetPushed = false; // keep track for error recovery

      try {

         if (isSurfacePath(dc)) {

            // Pull the arrow triangles forward just a bit to ensure they show over the terrain.
            dc.pushProjectionOffest(SURFACE_PATH_DEPTH_OFFSET);
            gl.glDepthMask(false);
            isProjectionOffsetPushed = true;
         }

         final int opacity = 0xff;

         // draw triangle
         gl.glColor4ub((byte) rgb.red, (byte) rgb.green, (byte) rgb.blue, (byte) opacity);

         gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds[_arrowSurfaceVboId]);

         // Convert stride from number of elements to number of bytes.
         gl.glVertexPointer(3, GL2.GL_FLOAT, 4 * 3, 0);
         gl.glDrawArrays(GL2.GL_TRIANGLES, 0, _numberOfDirectionArrows);

         // draw border
         final Color borderColor = ColorUtil.getContrastColorAWT(rgb.red, rgb.green, rgb.blue, opacity);
         gl.glLineWidth(2.0f);
         gl.glColor4ub(
               (byte) borderColor.getRed(),
               (byte) borderColor.getGreen(),
               (byte) borderColor.getBlue(),
               (byte) opacity);

         gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds[_arrowBorderVboId]);

         // Convert stride from number of elements to number of bytes.
         gl.glVertexPointer(3, GL2.GL_FLOAT, 4 * 3, 0);
         gl.glDrawArrays(GL2.GL_LINES, 0, _numberOfDirectionArrows * 2);

      } finally {

         if (isProjectionOffsetPushed) {
            dc.popProjectionOffest();
            gl.glDepthMask(true);
         }
      }
   }

   private void drawInteriorVertexColorVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

      final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

      final int vertexStride = pathData.getVertexStride();

      // Convert stride from number of elements to number of bytes.
      gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds[0]);
      {
         gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
         {
            gl.glVertexPointer(3, GL2.GL_FLOAT, 4 * vertexStride, 0);
            gl.glColorPointer(4, GL2.GL_FLOAT, 4 * vertexStride, 4 * pathData.getColorOffset());

            gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, pathData.getVertexCount());
         }
         gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
      }
      gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
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
    *           the current draw context.
    * @param vboIds
    *           the ids of this shapes buffers.
    * @param pathData
    *           the current globe-specific path data.
    */
   @Override
   protected void drawPointsVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      if (config.isShowTrackPosition == false) {
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

      final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

      // Convert stride from number of elements to number of bytes.
      gl.glVertexPointer(3, GL2.GL_FLOAT, 4 * pathData.getVertexStride(), 0);

      if (dc.isPickingMode()) {

         gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
         gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
         gl.glColorPointer(3, GL2.GL_UNSIGNED_BYTE, 0, pickPositionColors);

      } else if (useVertexColors) {
         // Apply this path's per-position colors if we're in normal rendering mode (not picking) and this path's
         // positionColors is non-null. Convert the stride and offset from number of elements to number of bytes.
         gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
         gl.glColorPointer(4, GL2.GL_FLOAT, 4 * pathData.getVertexStride(), 4 * pathData.getColorOffset());
      }

      prepareToDrawPoints(dc);

      gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, vboIds[2]);
      gl.glDrawElements(GL2.GL_POINTS, posPoints.limit(), GL2.GL_UNSIGNED_INT, 0);

      // Restore the previous GL point state.
      gl.glPointSize(1f);
      gl.glDisable(GL2.GL_POINT_SMOOTH);

      // Restore the previous GL color array state.
      if (dc.isPickingMode() || useVertexColors) {
         gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
      }
   }

   @Override
   protected void fillVBO(final DrawContext dc) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tfillVBO " + dc.getFrameTimeStamp()));
//		// TODO remove SYSTEM.OUT.PRINTLN

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();
      final PathData pathData = getCurrentPathData();

      int numberOfIds = isShowPositions() ? //
            3 : pathData.isHasExtrusionPoints() && isDrawVerticals() ? //
                  2 : 1;

      // show arrow keys
      if (config.isShowDirectionArrows) {
         numberOfIds += 3;
      }

      int[] vboIds = (int[]) dc.getGpuResourceCache().get(pathData.getVboCacheKey());
      if (vboIds != null && vboIds.length != numberOfIds) {
         clearCachedVbos(dc);
         vboIds = null;
      }

      final GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

      final int vSize = pathData.getRenderedPath().limit() * 4;

      int iSize = pathData.isHasExtrusionPoints() && isDrawVerticals() ? //
            pathData.getTessellatedPositions().size() * 2 * 4 : 0;

      if (isShowPositions()) {
         iSize += pathData.getTessellatedPositions().size();
      }

      if (config.isShowDirectionArrows) {
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
         gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds[0]);
         gl.glBufferData(GL2.GL_ARRAY_BUFFER, vb.limit() * 4, vb.rewind(), GL2.GL_STATIC_DRAW);

         if (pathData.isHasExtrusionPoints() && isDrawVerticals()) {
            final IntBuffer ib = pathData.getPolePositions();
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, vboIds[1]);
            gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, ib.limit() * 4, ib.rewind(), GL2.GL_STATIC_DRAW);
         }

         if (isShowPositions()) {
            final IntBuffer ib = pathData.getPositionPoints();
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, vboIds[2]);
            gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, ib.limit() * 4, ib.rewind(), GL2.GL_STATIC_DRAW);
         }

         if (config.isShowDirectionArrows) {

            int vboIndex = numberOfIds - 3;

            // create vbo for direction arrows
            _arrowPositionVboId = vboIndex++;
            FloatBuffer fb = (FloatBuffer) pathData.getValue(ARROW_POSITION_KEY);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds[_arrowPositionVboId]);
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, fb.limit() * 4, fb.rewind(), GL2.GL_STATIC_DRAW);

            _arrowSurfaceVboId = vboIndex++;
            fb = (FloatBuffer) pathData.getValue(ARROW_SURFACE_KEY);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds[_arrowSurfaceVboId]);
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, fb.limit() * 4, fb.rewind(), GL2.GL_STATIC_DRAW);

            _arrowBorderVboId = vboIndex++;
            fb = (FloatBuffer) pathData.getValue(ARROW_BORDER_KEY);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds[_arrowBorderVboId]);
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, fb.limit() * 4, fb.rewind(), GL2.GL_STATIC_DRAW);
         }

      } finally {

         gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
         gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
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

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      final boolean isTourSelected = _tourTrack.isSelected();
      final boolean isTourHovered = _tourTrack.isHovered();

      RGB solidColor = null;
      float interiorOpacity = 0;

      if (isTourHovered && isTourSelected) {

         if (config.interiorColorMode_HovSel == TourTrackConfig.COLOR_MODE_SOLID_COLOR) {

            solidColor = config.interiorColor_HovSel;
            interiorOpacity = (float) (config.interiorOpacity_HovSel);
         }

      } else if (isTourHovered) {

         if (config.interiorColorMode_Hovered == TourTrackConfig.COLOR_MODE_SOLID_COLOR) {

            solidColor = config.interiorColor_Hovered;
            interiorOpacity = (float) (config.interiorOpacity_Hovered);
         }

      } else if (isTourSelected) {

         if (config.interiorColorMode_Selected == TourTrackConfig.COLOR_MODE_SOLID_COLOR) {
            solidColor = config.interiorColor_Selected;
            interiorOpacity = (float) (config.interiorOpacity_Selected);
         }

      } else {

         if (config.interiorColorMode == TourTrackConfig.COLOR_MODE_SOLID_COLOR) {
            solidColor = config.interiorColor;
            interiorOpacity = (float) (config.interiorOpacity);
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

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      boolean isShowTrackValue;

      final boolean isTourSelected = _tourTrack.isSelected();
      final boolean isTourHovered = _tourTrack.isHovered();

      if (isTourHovered && isTourSelected) {

         isShowTrackValue = config.outlineColorMode_HovSel == TourTrackConfig.COLOR_MODE_TRACK_VALUE;

      } else if (isTourHovered) {

         isShowTrackValue = config.outlineColorMode_Hovered == TourTrackConfig.COLOR_MODE_TRACK_VALUE;

      } else if (isTourSelected) {

         isShowTrackValue = config.outlineColorMode_Selected == TourTrackConfig.COLOR_MODE_TRACK_VALUE;

      } else {

         isShowTrackValue = config.outlineColorMode == TourTrackConfig.COLOR_MODE_TRACK_VALUE;
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
   public void setTourTrack(final TourTrack tourTrack) {
      _tourTrack = tourTrack;
   }

   @Override
   protected boolean shouldUseVBOs(final DrawContext dc) {

//		draw with VA is not yet implemented !!!
//		return this.getCurrentPathData().getTessellatedPositions().size() > VBO_THRESHOLD && super.shouldUseVBOs(dc);

      return true;
   }
}
