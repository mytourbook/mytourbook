/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.jhlabs.image;

import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.common.UI;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.shape.CubicBezierCurve;

/**
 * Curve x/y values 0...1
 */
public class CurveValues {

   private static final char NL = UI.NEW_LINE;

   /**
    * 0...1
    */
   public float[]            allValuesX;

   /**
    * 0...1
    */
   public float[]            allValuesY;

   public CurveValues() {

      allValuesX = new float[] { 0, 1 };
      allValuesY = new float[] { 0, 1 };
   }

   public CurveValues(final CurveValues curve) {

      allValuesX = curve.allValuesX.clone();
      allValuesY = curve.allValuesY.clone();
   }

   public int addKnot(final float kx, final float ky) {

      int pos = -1;
      final int numKnots = allValuesX.length;

      final float[] nx = new float[numKnots + 1];
      final float[] ny = new float[numKnots + 1];

      int j = 0;

      for (int i = 0; i < numKnots; i++) {

         if (pos == -1 && allValuesX[i] > kx) {

            pos = j;

            nx[j] = kx;
            ny[j] = ky;

            j++;
         }

         nx[j] = allValuesX[i];
         ny[j] = allValuesY[i];

         j++;
      }

      if (pos == -1) {

         pos = j;
         nx[j] = kx;
         ny[j] = ky;
      }

      allValuesX = nx;
      allValuesY = ny;

      return pos;
   }

   private int[] createLUT() {

      int[] table = null;

//      table = createLUT_NEW();
      table = createLUT_NEW_WithLine();
//      table = createLUT_OLD();

      return table;
   }

   /**
    * Creates 0...255 values from the 0...1 knot values
    *
    * @return
    */
   private int[] createLUT_NEW() {

      final ArrayList<Coordinate> coordinates = new ArrayList<>();

      // add an additional point to ensure that at least 3 points are set !!!
      coordinates.add(new Coordinate(0, 0));

      for (int valueIndex = 0; valueIndex < allValuesX.length; valueIndex++) {

         final float xValue = allValuesX[valueIndex];
         final float yValue = allValuesY[valueIndex];

         coordinates.add(new Coordinate(xValue, yValue));
      }

      final Coordinate[] coor = coordinates.toArray(new Coordinate[coordinates.size()]);

      final Geometry geom = new GeometryFactory().createLineString(coor);

      final double alpha = 1;
      final double skew = 0;

      final Geometry bezierCurve = CubicBezierCurve.bezierCurve(geom, alpha, skew);
      final Coordinate[] allBezierPointsRaw = bezierCurve.getCoordinates();

      // get all none NaN points
      final ArrayList<Coordinate> allBezierPoints = new ArrayList<>();
      for (final Coordinate coordinate : allBezierPointsRaw) {
         if (Double.isNaN(coordinate.x) == false) {
            allBezierPoints.add(coordinate);
         }
      }

      final int numPoints = allBezierPoints.size();
      final int numLUT = 256;

      final int[] lut = new int[numLUT];

      int pointIndex = 0;
      Coordinate bezPoint = allBezierPoints.get(0);
      Coordinate prevBezPoint = bezPoint;

      for (int lutX = 0; lutX < numLUT; lutX++) {

         while (pointIndex < numPoints) {

            final double bezPointX = bezPoint.x;
            final double bezPointY = bezPoint.y;
            final double prevBezPointX = prevBezPoint.x;
            final double prevBezPointY = prevBezPoint.y;

            final double diffX = (bezPointX - prevBezPointX) * 255;
            final double diffY = (bezPointY - prevBezPointY) * 255;

//            System.out.println(UI.timeStamp() + "%4d %5.2f  %5.2f".formatted(lutX, diffX, diffY));
//// TODO remove SYSTEM.OUT.PRINTLN

            final double nextBezPointIndex = bezPointX * 255;

            if (nextBezPointIndex >= lutX) {
               break;
            }

            pointIndex++;

            if (pointIndex < numPoints) {

               prevBezPoint = bezPoint;
               bezPoint = allBezierPoints.get(pointIndex);
            }
         }

         final int lutY = (int) (bezPoint.y * 255);

         lut[lutX] = ImageMath.clamp(lutY, 0, 255);

         if (lutX < 20) {
            System.out.println(UI.timeStamp() + " %4d  %4d ".formatted(lutX, lutY));
// TODO remove SYSTEM.OUT.PRINTLN
         }
      }

      System.out.println();
      System.out.println();
      System.out.println();
// TODO remove SYSTEM.OUT.PRINTLN

      return lut;
   }

   private int[] createLUT_NEW_WithLine() {

      final ArrayList<Coordinate> coordinates = new ArrayList<>();

      // add an additional point to ensure that at least 3 points are set !!!
      coordinates.add(new Coordinate(0, 0));

      for (int valueIndex = 0; valueIndex < allValuesX.length; valueIndex++) {

         final float xValue = allValuesX[valueIndex];
         final float yValue = allValuesY[valueIndex];

         coordinates.add(new Coordinate(xValue, yValue));
      }

      final Coordinate[] coor = coordinates.toArray(new Coordinate[coordinates.size()]);

      final Geometry geom = new GeometryFactory().createLineString(coor);

      final double alpha = 1;
      final double skew = 0;

      final Geometry bezierCurve = CubicBezierCurve.bezierCurve(geom, alpha, skew);
      final Coordinate[] allBezierPointsRaw = bezierCurve.getCoordinates();

      // get all none NaN points
      final ArrayList<Coordinate> allBezierPoints = new ArrayList<>();
      for (final Coordinate coordinate : allBezierPointsRaw) {
         if (Double.isNaN(coordinate.x) == false) {
            allBezierPoints.add(coordinate);
         }
      }

      final int numLUT = 256;
      final int[] lut = new int[numLUT];

      final int numPoints = allBezierPoints.size();

      if (numPoints > 0) {

         Coordinate bezPoint = allBezierPoints.get(0);

         int bezPointX0 = (int) (bezPoint.x * 255);
         int bezPointY0 = (int) (bezPoint.y * 255);

         int bezPointX1;
         int bezPointY1;

         for (int pointIndex = 1; pointIndex < numPoints; pointIndex++) {

            bezPointX1 = bezPointX0;
            bezPointY1 = bezPointY0;

            bezPoint = allBezierPoints.get(pointIndex);

            bezPointX0 = (int) (bezPoint.x * 255);
            bezPointY0 = (int) (bezPoint.y * 255);

            plotLine(lut, bezPointX0, bezPointY0, bezPointX1, bezPointY1);
         }
      }

      return lut;
   }

   /**
    * Creates 0...255 values from the 0...1 knot values
    *
    * @return
    */
   private int[] createLUT_OLD() {

      final int numKnots = allValuesX.length;

      final float[] allRenderKnotsX = new float[numKnots + 2];
      final float[] allRenderKnotsY = new float[numKnots + 2];

      System.arraycopy(allValuesX, 0, allRenderKnotsX, 1, numKnots);
      System.arraycopy(allValuesY, 0, allRenderKnotsY, 1, numKnots);

      allRenderKnotsX[0] = allRenderKnotsX[1];
      allRenderKnotsY[0] = allRenderKnotsY[1];
      allRenderKnotsX[numKnots + 1] = allRenderKnotsX[numKnots];
      allRenderKnotsY[numKnots + 1] = allRenderKnotsY[numKnots];

      final int[] lut = new int[256];

      // if first knot is > 0 fill the table with y position
      if (allRenderKnotsX[0] > 0) {

         final int renderStartX = (int) (allRenderKnotsX[0] * 255);
         final int renderStartY = (int) (allRenderKnotsY[0] * 255);

         for (int renderIndexX = 0; renderIndexX <= renderStartX; renderIndexX++) {
            lut[renderIndexX] = renderStartY;
         }
      }

      // if last knot is < 1 fill the table with y position
      if (allRenderKnotsX[numKnots] < 1) {

         final int renderEndX = (int) (allRenderKnotsX[numKnots] * 255);
         final int renderEndY = (int) (allRenderKnotsY[numKnots] * 255);

         for (int i = renderEndX; i <= 255; i++) {
            lut[i] = renderEndY;
         }
      }

      final int numRenderKnots = allRenderKnotsX.length;

      for (int renderIndex = 0; renderIndex < 2048; renderIndex++) {

         final float f = renderIndex / 2048.0f;

         int lutX = (int) (255 * ImageMath.splineClamped(f, numRenderKnots, allRenderKnotsX) + 0.5f);
         int lutY = (int) (255 * ImageMath.spline(f, numRenderKnots, allRenderKnotsY) + 0.5f);

         lutX = ImageMath.clamp(lutX, 0, 255);
         lutY = ImageMath.clamp(lutY, 0, 255);

         lut[lutX] = lutY;
      }

      return lut;
   }

   public int findKnotPos(final float kx) {

      final int numKnots = allValuesX.length;

      for (int i = 0; i < numKnots; i++) {

         if (allValuesX[i] > kx) {
            return i;
         }
      }

      return numKnots;
   }

   public int[] makeTable() {

      return createLUT();
   }

   /**
    * Original source: https://zingl.github.io/bresenham.html
    *
    * @param lut
    * @param bezPoint0
    * @param bezPoint1
    */
   private void plotLine(final int[] line, int x0, int y0, final int x1, final int y1) {

      final int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
      final int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;

      int err = dx + dy, e2; /* error value e_xy */

      final int numPixel = line.length;

      for (;;) {

         if (x0 < 0 || x0 >= numPixel) {
            break;
         }

         line[x0] = y0;

         if (x0 == x1 && y0 == y1) {
            break;
         }

         e2 = 2 * err;

         /* e_xy + e_x > 0 */
         if (e2 >= dy) {
            err += dy;
            x0 += sx;
         }

         /* e_xy + e_y < 0 */
         if (e2 <= dx) {
            err += dx;
            y0 += sy;
         }
      }
   }

   public void removeKnot(final int deleteIndex) {

      final int numKnots = allValuesX.length;

      if (numKnots <= 2) {
         return;
      }

      final float[] newValuesX = new float[numKnots - 1];
      final float[] newValuesY = new float[numKnots - 1];

      int oldIndex = 0;

      for (int newIndex = 0; newIndex < numKnots - 1; newIndex++) {

         if (newIndex == deleteIndex) {
            oldIndex++;
         }

         newValuesX[newIndex] = allValuesX[oldIndex];
         newValuesY[newIndex] = allValuesY[oldIndex];

         oldIndex++;
      }

      allValuesX = newValuesX;
      allValuesY = newValuesY;
   }

   @SuppressWarnings("unused")
   private void sortKnots() {

//        int numKnots = x.length;
//        for (int i = 1; i < numKnots - 1; i++) {
//            for (int j = 1; j < i; j++) {
//                if (x[i] < x[j]) {
//                    float t = x[i];
//                    x[i] = x[j];
//                    x[j] = t;
//                    t = y[i];
//                    y[i] = y[j];
//                    y[j] = t;
//                }
//            }
//        }
   }

   @Override
   public String toString() {

      final int maxLen = 5;

      return UI.EMPTY_STRING

            + "CurveValues" + NL //$NON-NLS-1$

            + " allValuesX = " + (allValuesX != null //$NON-NLS-1$
                  ? Arrays.toString(Arrays.copyOf(allValuesX, Math.min(allValuesX.length, maxLen)))
                  : null) + NL

            + " allValuesY = " + (allValuesY != null //$NON-NLS-1$
                  ? Arrays.toString(Arrays.copyOf(allValuesY, Math.min(allValuesY.length, maxLen)))
                  : null)

      ;
   }
}
