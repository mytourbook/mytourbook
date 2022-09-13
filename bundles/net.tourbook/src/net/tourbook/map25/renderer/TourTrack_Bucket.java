/**
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2021 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map25.renderer;

import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

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
 * Note:
 * <p>
 * Coordinates must be in range +/- (Short.MAX_VALUE / COORD_SCALE) if using GL.SHORT
 * <p>
 * The maximum resolution for coordinates is 0.25 as points will be converted
 * to fixed point values.
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

   private float              _minimumDistance = MIN_DIST;
   private float              _minimumBevel    = MIN_BEVEL;

   boolean                    _isCapRounded;
   float                      _heightOffset;

   private int                _tMin            = Integer.MIN_VALUE, _tMax = Integer.MAX_VALUE;

   ////////////////////////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////////////////////////////////////////////////////////
   ////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Number of vertices for this layer.
    */
   protected int                        numVertices;

   /**
    * Temporary list of vertex data.
    */
   protected final TourTrack_VertexData vertexItems;

   /**
    * Contains the vertices for direction arrows multiplied with {@link MapRenderer#COORD_SCALE}:
    * <p>
    *
    * <pre>
    * x1    y2    z1
    * x2    y2    z2
    *
    * ...
    * </pre>
    */
   protected ShortArrayList             directionArrow_XYZPositions;

   /**
    * Barycentric coordinates are simply (1, 0, 0), (0, 1, 0) and (0, 0, 1) for the three
    * triangle vertices (the order does not really matter, which makes packing into triangle strips
    * potentially easier).
    * Source:
    * https://stackoverflow.com/questions/137629/how-do-you-render-primitives-as-wireframes-in-opengl#answer-33004265
    */
   protected ShortArrayList             colorCoords;

   public TourTrack_Bucket() {

      vertexItems = new TourTrack_VertexData();

      directionArrow_XYZPositions = new ShortArrayList();
      colorCoords = new ShortArrayList();
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
       * more than 200 lines per type. FIXME make optional!
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
      _isCapRounded = isCapRounded;

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

               vertexItems,

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
    * @param vertices
    * @param pixelPoints
    *           -2048 ... 2048
    * @param pixelPointColors
    * @param startIndex
    * @param numLinePoints
    * @param isRounded
    * @param isSquared
    * @param isClosed
    */
   private void addLine_ToVertices(final TourTrack_VertexData vertices,
                                   final float[] pixelPoints,
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

      /*
       * amount of vertices used
       * + 2 for drawing triangle-strip
       * + 4 for round caps
       * + 2 for closing polygons
       */
      numVertices += numLinePoints
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
          * TODO should align ending with tile boundary
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
            numVertices -= 2;
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

            numVertices -= 2;

            continue;
         }

         unit2X /= unitDistance;
         unit2Y /= unitDistance;

         final double dotp = unit2X * unit1X + unit2Y * unit1Y;

         //log.debug("acos " + dotp);
         if (dotp > 0.65) {

            // add bevel join to avoid miter going to infinity
            numVertices += 2;

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
            numVertices -= 2;
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

      vertexItems.dispose();

      numVertices = 0;
   }

   private void createArrow_MiddleFin(final short p2X,
                                      final short p2Y,
                                      final short pBackX,
                                      final short pBackY,
                                      final short pOnLineX,
                                      final short pOnLineY,
                                      final short arrowZ,
                                      final short finTopZ,
                                      final short arrowPart_Fin) {
// SET_FORMATTING_OFF

      // fin: middle
      directionArrow_XYZPositions.addAll(

            p2X,        p2Y,        arrowZ,  arrowPart_Fin,
            pOnLineX,   pOnLineY,   finTopZ, arrowPart_Fin,
            pBackX,     pBackY,     arrowZ,  arrowPart_Fin);

      colorCoords.addAll(

            (short) 1, (short) 1, (short) 0,
            (short) 0, (short) 1, (short) 0,
            (short) 0, (short) 0, (short) 1);

// SET_FORMATTING_ON
   }

   private void createArrow_OuterFins(final short p2X,
                                      final short p2Y,
                                      final short pLeftX,
                                      final short pLeftY,
                                      final short pRightX,
                                      final short pRightY,
                                      final short arrowZ,
                                      final short finBottomZ,
                                      final short arrowPart_Fin) {
// SET_FORMATTING_OFF

      directionArrow_XYZPositions.addAll(

            // fin: left
            p2X,        p2Y,        arrowZ,     arrowPart_Fin,
            pLeftX,     pLeftY,     arrowZ,     arrowPart_Fin,
            pLeftX,     pLeftY,     finBottomZ, arrowPart_Fin,

            // fin: right
            p2X,        p2Y,        arrowZ,     arrowPart_Fin,
            pRightX,    pRightY,    arrowZ,     arrowPart_Fin,
            pRightX,    pRightY,    finBottomZ, arrowPart_Fin);

      colorCoords.addAll(

            (short) 1, (short) 0, (short) 0,
            (short) 0, (short) 1, (short) 0,
            (short) 0, (short) 0, (short) 1,

            (short) 0, (short) 1, (short) 0,
            (short) 1, (short) 0, (short) 0,
            (short) 0, (short) 0, (short) 1);

// SET_FORMATTING_ON
   }

   private void createArrow_Wings(final short p2X,
                                  final short p2Y,
                                  final short pLeftX,
                                  final short pLeftY,
                                  final short pRight,
                                  final short pRightY,
                                  final short pBackX,
                                  final short pBackY,
                                  final short arrowZ,
                                  final short arrowPart_Wing) {
// SET_FORMATTING_OFF

      /*
       * When the head and tail have the same z-value then the overlapping part is flickering
       */
      final short arrowHeadZ = (short) (arrowZ+1);

      directionArrow_XYZPositions.addAll(

            // wing: left
            p2X,        p2Y,     arrowHeadZ, arrowPart_Wing,
            pBackX,     pBackY,  arrowZ,     arrowPart_Wing,
            pLeftX,     pLeftY,  arrowZ,     arrowPart_Wing,

            // wing: right
            p2X,        p2Y,     arrowHeadZ, arrowPart_Wing,
            pBackX,     pBackY,  arrowZ,     arrowPart_Wing,
            pRight,     pRightY, arrowZ,     arrowPart_Wing);

      colorCoords.addAll(

            (short) 1, (short) 0, (short) 0,
            (short) 0, (short) 1, (short) 1,
            (short) 0, (short) 0, (short) 1,

            (short) 1, (short) 0, (short) 0,
            (short) 0, (short) 1, (short) 1,
            (short) 0, (short) 0, (short) 1);

// SET_FORMATTING_ON
   }

   /**
    * @param allDirectionArrowPixel_Raw
    *           Contains the x/y pixel positions for the direction arrows
    */
   public void createDirectionArrowVertices(final FloatArrayList allDirectionArrowPixel_Raw) {

      directionArrow_XYZPositions.clear();
      colorCoords.clear();

      // at least 2 positions are needed
      if (allDirectionArrowPixel_Raw.size() < 4) {
         return;
      }

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      final float[] allDirectionArrowPixel = allDirectionArrowPixel_Raw.toArray();

// SET_FORMATTING_OFF

      final float configArrowScale           = trackConfig.arrow_Scale / 10f;
      final int   configArrowLength          = (int) (configArrowScale * trackConfig.arrow_Length);
      final int   configArrowLengthCenter    = (int) (configArrowScale * trackConfig.arrow_LengthCenter);
      final int   configArrowWidth           = (int) (configArrowScale * trackConfig.arrow_Width);
      final int   configArrowHeight          = (int) (configArrowScale * trackConfig.arrow_Height);

      final short arrowZ      = (short) trackConfig.arrow_VerticalOffset;
      final short finTopZ     = (short) (arrowZ + configArrowHeight);
      final short finBottomZ  = (short) (arrowZ - configArrowHeight);

// SET_FORMATTING_ON

      int pixelIndex = 0;

      float p1X = allDirectionArrowPixel[pixelIndex++];
      float p1Y = allDirectionArrowPixel[pixelIndex++];

      for (; pixelIndex < allDirectionArrowPixel.length;) {

         final float p2X = allDirectionArrowPixel[pixelIndex++];
         final float p2Y = allDirectionArrowPixel[pixelIndex++];

         // get direction/unit vector: dir = (P2-P1)/|P2-P1|
         final float diffX = p2X - p1X;
         final float diffY = p2Y - p1Y;

         // distance between P1 and P2
         final double p12Distance = Math.sqrt(diffX * diffX + diffY * diffY);

         final double p12UnitX = diffX / p12Distance;
         final double p12UnitY = diffY / p12Distance;

         // get perpendicular vector for the arrow head
         final double unitPerpendX = p12UnitY;
         final double unitPerpendY = -p12UnitX;

         final double arrowLength = Math.min(configArrowLength, p12Distance);
         final double arrowLengthMiddle = Math.min(configArrowLengthCenter, p12Distance);
         final double arrowWidth2 = configArrowWidth / 2; // half arrow width

         // point on line between P1 and P2
         final double pOnLineX = p2X - (arrowLength * p12UnitX);
         final double pOnLineY = p2Y - (arrowLength * p12UnitY);
         final double pBackX = p2X - (arrowLengthMiddle * p12UnitX);
         final double pBackY = p2Y - (arrowLengthMiddle * p12UnitY);

         final double vFinX = unitPerpendX * arrowWidth2;
         final double vFinY = unitPerpendY * arrowWidth2;

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
          *                 ZfinTop
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
          *                 ZfinBottom
          * </code>
          */

         /*
          * Set arrow positions
          */

// SET_FORMATTING_OFF

         final short arrowPart_Wing    = 0;
         final short arrowPart_Fin     = 1;

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


         /**
          * !!! VERY IMPORTANT !!! <p>
          *
          * THE ORDER, TO FIX Z-FIGHTING
          */

         switch (trackConfig.arrow_Design) {

         case WINGS_WITH_MIDDLE_FIN:

            createArrow_Wings(      p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    arrowZ,
                                    arrowPart_Wing);

            createArrow_MiddleFin(  p2X_scaled,       p2Y_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    pOnLineX_scaled,  pOnLineY_scaled,
                                    arrowZ,
                                    finTopZ,
                                    arrowPart_Fin);

            break;

         case WINGS_WITH_OUTER_FINS:

            createArrow_Wings(      p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    arrowZ,
                                    arrowPart_Wing);

            createArrow_OuterFins(  p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    arrowZ,
                                    finBottomZ,
                                    arrowPart_Fin);

            break;

         case MIDDLE_FIN:

            createArrow_MiddleFin(  p2X_scaled,       p2Y_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    pOnLineX_scaled,  pOnLineY_scaled,
                                    arrowZ,
                                    finTopZ,
                                    arrowPart_Fin);
            break;

         case OUTER_FINS:

            createArrow_OuterFins(  p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    arrowZ,
                                    finBottomZ,
                                    arrowPart_Fin);

            break;

         case WINGS:
         default:
            createArrow_Wings(      p2X_scaled,       p2Y_scaled,
                                    pLeftX_scaled,    pLeftY_scaled,
                                    pRight_Xscaled,   pRightY_scaled,
                                    pBackX_scaled,    pBackY_scaled,
                                    arrowZ,
                                    arrowPart_Wing);
            break;
         }

// SET_FORMATTING_ON

         // setup next position
         p1X = p2X;
         p1Y = p2Y;
      }
   }

   protected void fillVerticesBuffer(final ShortBuffer vboBuffer, final ByteBuffer colorBuffer) {

      vertexItems.fillVerticesBuffer(vboBuffer, colorBuffer);
   }

}
