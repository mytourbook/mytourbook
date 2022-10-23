/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.renderer;

import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;
import net.tourbook.map25.layer.tourtrack.TourTrack_Layer;

import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.LineStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Original class org.oscim.renderer.bucket.LineBucket but with many modifications
 */
public class TourTrack_Bucket {

   static final Logger        log              = LoggerFactory.getLogger(TourTrack_Bucket.class);

   /**
    * Scale factor mapping extrusion vector to short values
    */
   public static final float  DIR_SCALE        = 2048;

   /**
    * Maximal resolution
    */
   private static final float MIN_DIST         = 1 / 8f;

   /**
    * Not quite right.. need to go back so that additional
    * bevel vertices are at least MIN_DIST apart
    */
   private static final float MIN_BEVEL        = MIN_DIST * 4;

   /**
    * Mask for packing last two bits of extrusion vector with texture
    * coordinates, .... 1111 1100
    */
   private static final int   DIR_MASK         = 0xFFFFFFFC;

   public LineStyle           lineStyle;
   public int                 lineColorMode;

   boolean                    isCapRounded;
   float                      heightOffset;

   private float              _minimumDistance = MIN_DIST;
   private float              _minimumBevel    = MIN_BEVEL;

   private int                _tMin            = Integer.MIN_VALUE, _tMax = Integer.MAX_VALUE;

   /**
    * Number of vertices for this layer.
    */
   int                        numTrackVertices;

   TourTrack_VertexData       trackVertexData;

   /**
    * Contains the vertices for direction arrows multiplied with {@link MapRenderer#COORD_SCALE}:
    * <p>
    *
    * <pre>
    * x1    y2    z1    arrowPart
    * x2    y2    z2    arrowPart
    * ...
    * </pre>
    */
   ShortArrayList             directionArrow_Vertices;

   /**
    * Barycentric coordinates are simply (1, 0, 0), (0, 1, 0) and (0, 0, 1) for the three
    * triangle vertices (the order does not really matter, which makes packing into triangle strips
    * potentially easier).
    * Source:
    * https://stackoverflow.com/questions/137629/how-do-you-render-primitives-as-wireframes-in-opengl#answer-33004265
    */
   ShortArrayList             directionArrow_ColorCoords;

   /**
    * X/Y positions
    *
    * <pre>
    * posX1    posY2
    * posX2    posY2
    * ...
    * </pre>
    */
   ShortArrayList             animatedPositions;

   public TourTrack_Bucket() {

      trackVertexData = new TourTrack_VertexData();

      animatedPositions = new ShortArrayList();

      directionArrow_Vertices = new ShortArrayList();
      directionArrow_ColorCoords = new ShortArrayList();
   }

   /**
    * This is called from {@link TourTrack_Layer#_simpleWorker}
    *
    * @param pixelPoints
    *           -2048 ... 2048
    * @param numPoints
    * @param isCapClosed
    * @param pixelPointColors
    *           One {@link #pixelPointColors} has two {@link #pixelPoints}
    */
   public void addLine(final float[] pixelPoints,
                       final int numPoints,
                       final boolean isCapClosed,
                       final int[] pixelPointColors) {

      if (numPoints >= 4) {
         addLine(pixelPoints, null, numPoints, isCapClosed, pixelPointColors);
      }
   }

   /**
    * @param pixelPoints
    *           -2048 ... 2048
    * @param index
    * @param numPoints
    * @param isCapClosed
    * @param pixelPointColors
    */
   private void addLine(final float[] pixelPoints,
                        final int[] index,
                        final int numPoints,
                        final boolean isCapClosed,
                        final int[] pixelPointColors) {

      // test minimum distance
//      _minimumDistance = testValue * 2.0f;

      boolean isCapRounded = false;
      boolean isCapSquared = false;

      if (lineStyle.cap == Cap.ROUND) {
         isCapRounded = true;
      } else if (lineStyle.cap == Cap.SQUARE) {
         isCapSquared = true;
      }

      /*
       * Note: just a hack to save some vertices, when there are
       * more than 200 lines per type. Fixme: make optional!
       */
      if (isCapRounded && index != null) {
         int cnt = 0;
         for (int i = 0, n = index.length; i < n; i++, cnt++) {
            if (index[i] < 0) {
               break;
            }
            if (cnt > 400) {
               isCapRounded = false;
               break;
            }
         }
      }
      this.isCapRounded = isCapRounded;

      int numIndices;
      int numLinePoints = 0;

      if (index == null) {
         numIndices = 1;
         if (numPoints > 0) {
            numLinePoints = numPoints;
         } else {
            numLinePoints = pixelPoints.length;
         }
      } else {
         numIndices = index.length;
      }

      for (int indexIndex = 0, pos = 0; indexIndex < numIndices; indexIndex++) {

         if (index != null) {
            numLinePoints = index[indexIndex];
         }

         /* check end-marker in indices */
         if (numLinePoints < 0) {
            break;
         }

         final int startIndex = pos;
         pos += numLinePoints;

         /* need at least two points */
         if (numLinePoints < 4) {
            continue;
         }

         /* start and end point are equal */
         if (numLinePoints == 4 &&
               pixelPoints[startIndex] == pixelPoints[startIndex + 2] &&
               pixelPoints[startIndex + 1] == pixelPoints[startIndex + 3]) {

            continue;
         }

         /* avoid simple 180 degree angles */
         if (numLinePoints == 6 &&
               pixelPoints[startIndex] == pixelPoints[startIndex + 4] &&
               pixelPoints[startIndex + 1] == pixelPoints[startIndex + 5]) {

            numLinePoints -= 2;
         }

         addLine_ToVertices(

               pixelPoints,
               pixelPointColors,
               startIndex,
               numLinePoints,
               isCapRounded,
               isCapSquared,
               isCapClosed);
      }
   }

   /**
    * @param pixelPoints
    *           -2048 ... 2048
    * @param pixelPointColors
    * @param startIndex
    * @param numLinePoints
    * @param isRounded
    * @param isSquared
    * @param isClosed
    */
   private void addLine_ToVertices(final float[] pixelPoints,
                                   final int[] pixelPointColors,
                                   final int startIndex,
                                   final int numLinePoints,
                                   final boolean isRounded,
                                   final boolean isSquared,
                                   final boolean isClosed) {

      float ux, uy;
      float unit1X, unit1Y;
      float unit2X, unit2Y;
      float curX, curY;
      float nextX, nextY;
      double unitDistance;
      int pixelColor;

      final TourTrack_VertexData vertices = trackVertexData;

      /*
       * amount of vertices used
       * + 2 for drawing triangle-strip
       * + 4 for round caps
       * + 2 for closing polygons
       */
      numTrackVertices += numLinePoints
            + (isRounded ? 6 : 2)
            + (isClosed ? 2 : 0);

      int pointIndex = startIndex;
      int pointIndexColor = startIndex / 2;

      curX = pixelPoints[pointIndex++];
      curY = pixelPoints[pointIndex++];
      pixelColor = pixelPointColors[pointIndexColor++];

      nextX = pixelPoints[pointIndex++];
      nextY = pixelPoints[pointIndex++];
      pixelColor = pixelPointColors[pointIndexColor++];

      // unit vector to next node
      unit1X = nextX - curX;
      unit1Y = nextY - curY;
      unitDistance = (float) Math.sqrt(unit1X * unit1X + unit1Y * unit1Y);
      unit1X /= unitDistance;
      unit1Y /= unitDistance;

      // perpendicular on the first segment
      ux = -unit1Y;
      uy = unit1X;

      int ddx, ddy;

      // vertex point coordinate
      short ox = (short) (curX * COORD_SCALE);
      short oy = (short) (curY * COORD_SCALE);

      // vertex extrusion vector, last two bit encode texture coord
      short dx, dy;

      // when the endpoint is outside the tile region omit round caps
      boolean isOutside = (curX < _tMin || curX > _tMax || curY < _tMin || curY > _tMax);

      if (isRounded && isOutside == false) {

         ddx = (int) ((ux - unit1X) * DIR_SCALE);
         ddy = (int) ((uy - unit1Y) * DIR_SCALE);
         dx = (short) (0 | ddx & DIR_MASK);
         dy = (short) (2 | ddy & DIR_MASK);

         vertices.add(ox, oy, dx, dy, pixelColor);
         vertices.add(ox, oy, dx, dy, pixelColor);

         ddx = (int) (-(ux + unit1X) * DIR_SCALE);
         ddy = (int) (-(uy + unit1Y) * DIR_SCALE);

         vertices.add(ox, oy, (short) (2 | ddx & DIR_MASK), (short) (2 | ddy & DIR_MASK), pixelColor);

         // start of line
         ddx = (int) (ux * DIR_SCALE);
         ddy = (int) (uy * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);
         vertices.add(ox, oy, (short) (2 | -ddx & DIR_MASK), (short) (1 | -ddy & DIR_MASK), pixelColor);

      } else {

         /*
          * Outside means line is probably clipped
          * Todo: should align ending with tile boundary
          * for now, just extend the line a little
          */

         float tx = unit1X;
         float ty = unit1Y;

         if (isRounded == false && isSquared == false) {

            tx = 0;
            ty = 0;

         } else if (isRounded) {

            tx *= 0.5;
            ty *= 0.5;
         }

         if (isRounded) {
            numTrackVertices -= 2;
         }

         // add first vertex twice
         ddx = (int) ((ux - tx) * DIR_SCALE);
         ddy = (int) ((uy - ty) * DIR_SCALE);
         dx = (short) (0 | ddx & DIR_MASK);
         dy = (short) (1 | ddy & DIR_MASK);

         vertices.add(ox, oy, dx, dy, pixelColor);
         vertices.add(ox, oy, dx, dy, pixelColor);

         ddx = (int) (-(ux + tx) * DIR_SCALE);
         ddy = (int) (-(uy + ty) * DIR_SCALE);

         vertices.add(ox, oy, (short) (2 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);
      }

      curX = nextX;
      curY = nextY;

      // unit vector pointing back to previous node
      unit1X *= -1;
      unit1Y *= -1;

      for (final int endIndex = startIndex + numLinePoints;;) {

         if (pointIndex < endIndex) {

            nextX = pixelPoints[pointIndex++];
            nextY = pixelPoints[pointIndex++];

            pixelColor = pixelPointColors[pointIndexColor++];

         } else if (isClosed && pointIndex < endIndex + 2) {

            // close the loop -> the next point is back to the startpoint
            // (Original comment) add startpoint == endpoint

            nextX = pixelPoints[startIndex];
            nextY = pixelPoints[startIndex + 1];

            pointIndex += 2;
            pixelColor = pixelPointColors[pointIndexColor++];

         } else {

            break;
         }

         // unit vector pointing forward to next node
         unit2X = nextX - curX;
         unit2Y = nextY - curY;
         unitDistance = Math.sqrt(unit2X * unit2X + unit2Y * unit2Y);

         // skip too short segments
         if (unitDistance < _minimumDistance) {

            numTrackVertices -= 2;

            continue;
         }

         unit2X /= unitDistance;
         unit2Y /= unitDistance;

         final double dotp = unit2X * unit1X + unit2Y * unit1Y;

         //log.debug("acos " + dotp);
         if (dotp > 0.65) {

            // add bevel join to avoid miter going to infinity
            numTrackVertices += 2;

            // dotp = FastMath.clamp(dotp, -1, 1);
            // double cos = Math.acos(dotp);
            // log.debug("cos " + Math.toDegrees(cos));
            // log.debug("back " + (mMinDist * 2 / Math.sin(cos + Math.PI / 2)));

            float px, py;
            if (dotp > 0.999) {

               // 360 degree angle, set points aside
               ux = unit1X + unit2X;
               uy = unit1Y + unit2Y;

               unitDistance = unit2X * uy - unit2Y * ux;

               if (unitDistance < 0.1 && unitDistance > -0.1) {

                  // almost straight
                  ux = -unit2Y;
                  uy = unit2X;

               } else {

                  ux /= unitDistance;
                  uy /= unitDistance;
               }

               //log.debug("aside " + a + " " + ux + " " + uy);
               px = curX - ux * _minimumBevel;
               py = curY - uy * _minimumBevel;
               curX = curX + ux * _minimumBevel;
               curY = curY + uy * _minimumBevel;

            } else {

               //log.debug("back");

               // go back by min dist
               px = curX + unit1X * _minimumBevel;
               py = curY + unit1Y * _minimumBevel;

               // go forward by min dist
               curX = curX + unit2X * _minimumBevel;
               curY = curY + unit2Y * _minimumBevel;
            }

            // unit vector pointing forward to next node
            unit2X = curX - px;
            unit2Y = curY - py;
            unitDistance = Math.sqrt(unit2X * unit2X + unit2Y * unit2Y);
            unit2X /= unitDistance;
            unit2Y /= unitDistance;

            addVertex(vertices, px, py, unit1X, unit1Y, unit2X, unit2Y, pixelColor);

            // flip unit vector to point back
            unit1X = -unit2X;
            unit1Y = -unit2Y;

            // unit vector pointing forward to next node
            unit2X = nextX - curX;
            unit2Y = nextY - curY;
            unitDistance = Math.sqrt(unit2X * unit2X + unit2Y * unit2Y);
            unit2X /= unitDistance;
            unit2Y /= unitDistance;
         }

         addVertex(vertices, curX, curY, unit1X, unit1Y, unit2X, unit2Y, pixelColor);

         curX = nextX;
         curY = nextY;

         // flip vector to point back
         unit1X = -unit2X;
         unit1Y = -unit2Y;
      }

      ux = unit1Y;
      uy = -unit1X;

      isOutside = curX < _tMin || curX > _tMax || curY < _tMin || curY > _tMax;

      ox = (short) (curX * COORD_SCALE);
      oy = (short) (curY * COORD_SCALE);

      if (isRounded && isOutside == false) {

         // inside

         ddx = (int) (ux * DIR_SCALE);
         ddy = (int) (uy * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);
         vertices.add(ox, oy, (short) (2 | -ddx & DIR_MASK), (short) (1 | -ddy & DIR_MASK), pixelColor);

         // for rounded line edges
         ddx = (int) ((ux - unit1X) * DIR_SCALE);
         ddy = (int) ((uy - unit1Y) * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (0 | ddy & DIR_MASK), pixelColor);

         // last vertex
         ddx = (int) (-(ux + unit1X) * DIR_SCALE);
         ddy = (int) (-(uy + unit1Y) * DIR_SCALE);
         dx = (short) (2 | ddx & DIR_MASK);
         dy = (short) (0 | ddy & DIR_MASK);

      } else {

         if (isRounded == false && isSquared == false) {

            unit1X = 0;
            unit1Y = 0;

         } else if (isRounded) {

            unit1X *= 0.5;
            unit1Y *= 0.5;
         }

         if (isRounded) {
            numTrackVertices -= 2;
         }

         ddx = (int) ((ux - unit1X) * DIR_SCALE);
         ddy = (int) ((uy - unit1Y) * DIR_SCALE);

         vertices.add(ox, oy, (short) (0 | ddx & DIR_MASK), (short) (1 | ddy & DIR_MASK), pixelColor);

         // last vertex
         ddx = (int) (-(ux + unit1X) * DIR_SCALE);
         ddy = (int) (-(uy + unit1Y) * DIR_SCALE);
         dx = (short) (2 | ddx & DIR_MASK);
         dy = (short) (1 | ddy & DIR_MASK);
      }

      // add last vertex twice
      vertices.add(ox, oy, dx, dy, pixelColor);
      vertices.add(ox, oy, dx, dy, pixelColor);
   }

   /**
    * Adds 2 vertices
    *
    * @param vertexData
    * @param x
    * @param y
    * @param unitNextX
    * @param unitNextY
    * @param unitPrevX
    * @param unitPrevY
    * @param pixelColor
    */
   private void addVertex(final TourTrack_VertexData vertexData,
                          final float x,
                          final float y,
                          final float unitNextX,
                          final float unitNextY,
                          final float unitPrevX,
                          final float unitPrevY,
                          final int pixelColor) {

      float ux = unitNextX + unitPrevX;
      float uy = unitNextY + unitPrevY;

      // vPrev times perpendicular of sum(unitNext, unitPrev)
      final double a = uy * unitPrevX - ux * unitPrevY;

      if (a < 0.01 && a > -0.01) {
         ux = -unitPrevY;
         uy = unitPrevX;
      } else {
         ux /= a;
         uy /= a;
      }

      final short ox = (short) (x * COORD_SCALE);
      final short oy = (short) (y * COORD_SCALE);

      final int ddx = (int) (ux * DIR_SCALE);
      final int ddy = (int) (uy * DIR_SCALE);

// SET_FORMATTING_OFF

      vertexData.add(ox, oy, (short) (0 |  ddx & DIR_MASK), (short) (1 |  ddy & DIR_MASK), pixelColor);
      vertexData.add(ox, oy, (short) (2 | -ddx & DIR_MASK), (short) (1 | -ddy & DIR_MASK), pixelColor);

// SET_FORMATTING_ON
   }

   /**
    * Clear all resources.
    */
   protected void clear() {

      numTrackVertices = 0;
   }

   /**
    * Creates direction arrow vertices from it's x/y position
    *
    * @param allDirectionArrowPixelList
    *           Contains the x/y pixel positions for the direction arrows
    */
   public void createArrowVertices(final FloatArrayList allDirectionArrowPixelList) {

      // create new list to not update currently used list, otherwise a bound exception can occure !!!
      animatedPositions.clear();

      directionArrow_Vertices.clear();
      directionArrow_ColorCoords.clear();

      // at least 2 positions are needed
      if (allDirectionArrowPixelList.size() < 4) {
         return;
      }

      final float[] allDirectionArrowPixel = allDirectionArrowPixelList.toArray();

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      if (trackConfig.arrow_IsAnimate) {

         createArrowVertices_200_Animated(allDirectionArrowPixel);

      } else {

         createArrowVertices_100_NotAnimated(allDirectionArrowPixel);
      }
   }

   /**
    * Arrows are not animated, draw static arrows
    *
    * @param allDirectionArrowPixel
    */
   private void createArrowVertices_100_NotAnimated(final float[] allDirectionArrowPixel) {

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

// SET_FORMATTING_OFF

      final float  configArrowScale          = trackConfig.arrow_Scale / 10f;
      final int    configArrowLength         = (int) (configArrowScale * trackConfig.arrow_Length);
      final int    configArrowLengthCenter   = (int) (configArrowScale * trackConfig.arrow_LengthCenter);
      final int    configArrowWidth          = (int) (configArrowScale * trackConfig.arrow_Width);
      final int    configArrowHeight         = (int) (configArrowScale * trackConfig.arrow_Height);

      final short  arrowZ                    = (short) trackConfig.arrow_VerticalOffset;
      final short  finTopZ                   = (short) (arrowZ + configArrowHeight);
      final short  finBottomZ                = (short) (arrowZ - configArrowHeight);

      final double arrowLength               = configArrowLength;
      final double arrowLengthMiddle         = configArrowLengthCenter;
      final double arrowHalfWidth            = configArrowWidth / 2; // half arrow width

// SET_FORMATTING_ON

      int pixelIndex = 0;

      float p1X = allDirectionArrowPixel[pixelIndex++];
      float p1Y = allDirectionArrowPixel[pixelIndex++];

      for (; pixelIndex < allDirectionArrowPixel.length;) {

         final float p2X = allDirectionArrowPixel[pixelIndex++];
         final float p2Y = allDirectionArrowPixel[pixelIndex++];

         // get unit (direction) vector: unit = (P2-P1)/|P2-P1|
         final float p21DiffX = p2X - p1X;
         final float p21DiffY = p2Y - p1Y;
         final double p12Distance = Math.sqrt(p21DiffX * p21DiffX + p21DiffY * p21DiffY); // distance between P1 and P2
         final double p12UnitX = p21DiffX / p12Distance;
         final double p12UnitY = p21DiffY / p12Distance;

         // get perpendicular vector for the arrow head
         final double unitPerpendX = p12UnitY;
         final double unitPerpendY = -p12UnitX;

         // point on line between P1 and P2
         final double pOnLineX = p2X - (arrowLength * p12UnitX);
         final double pOnLineY = p2Y - (arrowLength * p12UnitY);
         final double pBackX = p2X - (arrowLengthMiddle * p12UnitX);
         final double pBackY = p2Y - (arrowLengthMiddle * p12UnitY);

         final double vFinX = unitPerpendX * arrowHalfWidth;
         final double vFinY = unitPerpendY * arrowHalfWidth;

         // set arrow points which are left/right of the line
         final float pLeftX = (float) (pOnLineX + vFinX);
         final float pLeftY = (float) (pOnLineY + vFinY);

         final float pRightX = (float) (pOnLineX - vFinX);
         final float pRightY = (float) (pOnLineY - vFinY);

         /**
          * <code>
          *
          * WING
          *
          *                 Pleft
          *                     #---\
          *                      -   ---\
          *                     . -      ---\
          *                        -         ---\
          *                     .   -            ---\
          *                          -               ---\
          *                     .     -                  ---\
          *                            -                     ---\
          *    P1  #------------#-------#------------------------# P2
          *                 PonLine    - Pback               ---/
          *                     .     -                  ---/
          *                          -               ---/
          *                     .   -            ---/
          *                        -         ---/
          *                     . -      ---/
          *                      -   ---/
          *                     #---/
          *                 Pright
          *
          * FIN
          *
          *                 finTopZ
          *                     #---\
          *                          ---\
          *                     .        ---\
          *                                  ---\
          *                     .                ---\
          *                                          ---\
          *                     .                        ---\
          *                                                  ---\
          *    P1  #------------#--------------------------------# P2
          *                 PonLine                          ---/
          *                     .                        ---/
          *                                          ---/
          *                     .                ---/
          *                                  ---/
          *                     .        ---/
          *                          ---/
          *                     #---/
          *                 finBottomZ
          * </code>
          */

         /*
          * Set arrow positions
          */

// SET_FORMATTING_OFF

         final short p2X_scaled        = (short) (p2X       * COORD_SCALE);
         final short p2Y_scaled        = (short) (p2Y       * COORD_SCALE);
         final short pLeftX_scaled     = (short) (pLeftX    * COORD_SCALE);
         final short pLeftY_scaled     = (short) (pLeftY    * COORD_SCALE);
         final short pRight_Xscaled    = (short) (pRightX   * COORD_SCALE);
         final short pRightY_scaled    = (short) (pRightY   * COORD_SCALE);
         final short pBackX_scaled     = (short) (pBackX    * COORD_SCALE);
         final short pBackY_scaled     = (short) (pBackY    * COORD_SCALE);
         final short pOnLineX_scaled   = (short) (pOnLineX  * COORD_SCALE);
         final short pOnLineY_scaled   = (short) (pOnLineY  * COORD_SCALE);

         final short arrowPart_Wing    = 0;
         final short arrowPart_Fin     = 1;

         final int arrowIndex = (pixelIndex - 2) / 2;

         /**
          * !!! VERY IMPORTANT !!! <p>
          *
          * THE ORDER, TO FIX Z-FIGHTING
          */

         switch (trackConfig.arrow_Design) {

         case WINGS_WITH_MIDDLE_FIN:

            createArrowVertices_120_Wings(      p2X_scaled,       p2Y_scaled,
                                                pLeftX_scaled,    pLeftY_scaled,
                                                pRight_Xscaled,   pRightY_scaled,
                                                pBackX_scaled,    pBackY_scaled,
                                                arrowZ,
                                                arrowPart_Wing,
                                                arrowIndex);

            createArrowVertices_130_MiddleFin(  p2X_scaled,       p2Y_scaled,
                                                pBackX_scaled,    pBackY_scaled,
                                                pOnLineX_scaled,  pOnLineY_scaled,
                                                arrowZ,
                                                finTopZ,
                                                arrowPart_Fin,
                                                arrowIndex);
            break;

         case WINGS_WITH_OUTER_FINS:

            createArrowVertices_120_Wings(      p2X_scaled,       p2Y_scaled,
                                                pLeftX_scaled,    pLeftY_scaled,
                                                pRight_Xscaled,   pRightY_scaled,
                                                pBackX_scaled,    pBackY_scaled,
                                                arrowZ,
                                                arrowPart_Wing,
                                                arrowIndex);

            createArrowVertices_140_OuterFins(  p2X_scaled,       p2Y_scaled,
                                                pLeftX_scaled,    pLeftY_scaled,
                                                pRight_Xscaled,   pRightY_scaled,
                                                arrowZ,
                                                finBottomZ,
                                                arrowPart_Fin,
                                                arrowIndex);
            break;

         case MIDDLE_FIN:

            createArrowVertices_130_MiddleFin(  p2X_scaled,       p2Y_scaled,
                                                pBackX_scaled,    pBackY_scaled,
                                                pOnLineX_scaled,  pOnLineY_scaled,
                                                arrowZ,
                                                finTopZ,
                                                arrowPart_Fin,
                                                arrowIndex);
            break;

         case OUTER_FINS:

            createArrowVertices_140_OuterFins(  p2X_scaled,       p2Y_scaled,
                                                pLeftX_scaled,    pLeftY_scaled,
                                                pRight_Xscaled,   pRightY_scaled,
                                                arrowZ,
                                                finBottomZ,
                                                arrowPart_Fin,
                                                arrowIndex);
            break;

         case WINGS:
         default:
            createArrowVertices_120_Wings(      p2X_scaled,       p2Y_scaled,
                                                pLeftX_scaled,    pLeftY_scaled,
                                                pRight_Xscaled,   pRightY_scaled,
                                                pBackX_scaled,    pBackY_scaled,
                                                arrowZ,
                                                arrowPart_Wing,
                                                arrowIndex);
            break;
         }

// SET_FORMATTING_ON

         // setup next position
         p1X = p2X;
         p1Y = p2Y;
      }
   }

   private void createArrowVertices_120_Wings(final short p2X,
                                              final short p2Y,
                                              final short pLeftX,
                                              final short pLeftY,
                                              final short pRight,
                                              final short pRightY,
                                              final short pBackX,
                                              final short pBackY,
                                              final short arrowZ,
                                              final short arrowPart_Wing,
                                              final int arrowIndex) {
// SET_FORMATTING_OFF

      /*
       * When the head and tail have the same z-value then the overlapping part is flickering
       */
      final short arrowHeadZ = (short) (arrowZ+1);

      directionArrow_Vertices.addAll(

            // wing: left
            p2X,        p2Y,     arrowHeadZ, arrowPart_Wing,
            pBackX,     pBackY,  arrowZ,     arrowPart_Wing,
            pLeftX,     pLeftY,  arrowZ,     arrowPart_Wing,

            // wing: right
            p2X,        p2Y,     arrowHeadZ, arrowPart_Wing,
            pBackX,     pBackY,  arrowZ,     arrowPart_Wing,
            pRight,     pRightY, arrowZ,     arrowPart_Wing);

      directionArrow_ColorCoords.addAll(

            (short) 1, (short) 0, (short) 0,
            (short) 0, (short) 1, (short) 1,
            (short) 0, (short) 0, (short) 1,

            (short) 1, (short) 0, (short) 0,
            (short) 0, (short) 1, (short) 1,
            (short) 0, (short) 0, (short) 1);

// SET_FORMATTING_ON
   }

   private void createArrowVertices_130_MiddleFin(final short p2X,
                                                  final short p2Y,
                                                  final short pBackX,
                                                  final short pBackY,
                                                  final short pOnLineX,
                                                  final short pOnLineY,
                                                  final short arrowZ,
                                                  final short finTopZ,
                                                  final short arrowPart_Fin,
                                                  final int arrowIndex) {
// SET_FORMATTING_OFF

      // fin: middle
      directionArrow_Vertices.addAll(

            p2X,        p2Y,        arrowZ,  arrowPart_Fin,
            pOnLineX,   pOnLineY,   finTopZ, arrowPart_Fin,
            pBackX,     pBackY,     arrowZ,  arrowPart_Fin);

      directionArrow_ColorCoords.addAll(

            (short) 1, (short) 1, (short) 0,
            (short) 0, (short) 1, (short) 0,
            (short) 0, (short) 0, (short) 1);

// SET_FORMATTING_ON
   }

   private void createArrowVertices_140_OuterFins(final short p2X,
                                                  final short p2Y,
                                                  final short pLeftX,
                                                  final short pLeftY,
                                                  final short pRightX,
                                                  final short pRightY,
                                                  final short arrowZ,
                                                  final short finBottomZ,
                                                  final short arrowPart_Fin,
                                                  final int arrowIndex) {
// SET_FORMATTING_OFF

      directionArrow_Vertices.addAll(

            // fin: left
            p2X,        p2Y,        arrowZ,     arrowPart_Fin,
            pLeftX,     pLeftY,     arrowZ,     arrowPart_Fin,
            pLeftX,     pLeftY,     finBottomZ, arrowPart_Fin,

            // fin: right
            p2X,        p2Y,        arrowZ,     arrowPart_Fin,
            pRightX,    pRightY,    arrowZ,     arrowPart_Fin,
            pRightX,    pRightY,    finBottomZ, arrowPart_Fin);

      directionArrow_ColorCoords.addAll(

            (short) 1, (short) 0, (short) 0,
            (short) 0, (short) 1, (short) 0,
            (short) 0, (short) 0, (short) 1,

            (short) 0, (short) 1, (short) 0,
            (short) 1, (short) 0, (short) 0,
            (short) 0, (short) 0, (short) 1);

// SET_FORMATTING_ON
   }

   private void createArrowVertices_200_Animated(final float[] allDirectionArrowPixel) {

      for (int pixelIndex = 0; pixelIndex < allDirectionArrowPixel.length;) {

         final float p2X = allDirectionArrowPixel[pixelIndex++];
         final float p2Y = allDirectionArrowPixel[pixelIndex++];

         final short p2X_scaled = (short) (p2X * COORD_SCALE);
         final short p2Y_scaled = (short) (p2Y * COORD_SCALE);

         animatedPositions.addAll(p2X_scaled, p2Y_scaled);
      }
   }

}
