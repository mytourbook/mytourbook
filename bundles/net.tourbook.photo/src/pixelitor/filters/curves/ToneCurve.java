/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.curves;

import com.jhlabs.image.CurveValues;
import com.jhlabs.image.ImageMath;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import pixelitor.filters.levels.Channel;

/**
 * Represents a single tone curve for a color channel,
 * allowing points to be added, moved, or deleted.
 *
 * Point coordinates are defined from 0.0 to 1.0 on the x and y axes.
 * Any point that comes from user coordinates must be first
 * normalized to the curve coordinates space.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurve {

   // Maximum allowed number of control points on the curve
   private static final int MAX_CONTROL_POINTS = 16;

   // Radius for visual representation and hit detection
   private static final int   KNOT_RADIUS_PIXELS    = 6;
   private static final float KNOT_RADIUS           = 0.04F;
   private static final float KNOT_DETECTION_RADIUS = 0.08F;

   // Stroke styles for drawing the curve and points
   private static final BasicStroke CURVE_STROKE    = new BasicStroke(1);
   private static final BasicStroke POINT_STROKE    = new BasicStroke(2);

   public final CurveValues         curveValues     = new CurveValues();

   private final Channel            channel;

   private int                      width           = 255;
   private int                      height          = 255;

   private int[]                    curvePlotData;

   private boolean                  _isCurveUpdated = true;
   private boolean                  active          = false;

   public ToneCurve(final Channel channel) {

      this.channel = channel;
   }

   /**
    * Clamps a point to within the [0,1] bounds for the curve.
    */
   private static void clampToBoundary(final Point2D.Float p) {

      p.x = ImageMath.clamp01(p.x);
      p.y = ImageMath.clamp01(p.y);
   }

   /**
    * Checks if two points are close enough based on defined detection radius.
    */
   private static boolean isClose(final Point2D p, final Point2D q) {

      return Math.abs(p.getX() - q.getX()) < KNOT_DETECTION_RADIUS
            && Math.abs(p.getY() - q.getY()) < KNOT_DETECTION_RADIUS;
   }

   private static boolean isOver(final Point2D.Float p, final Point2D.Float q) {

      if (Math.abs(p.x - q.x) < KNOT_RADIUS) {
         return Math.abs(p.y - q.y) < KNOT_RADIUS;
      }
      return false;
   }

   public static boolean isOverChart(final Point2D.Float p) {

      return p.x >= 0 && p.x <= 1 && p.y >= 0 && p.y <= 1;
   }

   /**
    * Adds a new, normalized point (knot) to the curve at the specified position.
    * If allowReplace is true, replaces nearby points instead of adding.
    * Returns the index of the added or replaced knot, or -1 if not added.
    */
   public int addKnot(final Point2D.Float p, final boolean allowReplace) {

      // clamp to boundaries [0,1]
      clampToBoundary(p);

      final int lastIndex = curveValues.allValuesX.length - 1;
      final int index = curveValues.findKnotPos(p.x);

      // Prevent adding knots at the edges
      if (index <= 0 || index > lastIndex) {
         return -1;
      }

      // If allowReplace is true, replace a nearby knot if is's too close
      if (allowReplace) {
         final int prevIndex = index - 1;
         if (isClose(p, new Point2D.Float(curveValues.allValuesX[prevIndex], curveValues.allValuesY[prevIndex]))) {
            setKnotPosition(prevIndex, p);
            return prevIndex;
         } else if (isClose(p, new Point2D.Float(curveValues.allValuesX[index], curveValues.allValuesY[index]))) {
            setKnotPosition(index, p);
            return index;
         }
      }

      if (curveValues.allValuesX.length >= MAX_CONTROL_POINTS) {
         return -1; // can't add because the limit is reached
      }

      _isCurveUpdated = true;
      return curveValues.addKnot(p.x, p.y); // adds the new knot
   }


   /**
    * Deletes a knot at the given index if within bounds.
    */
   public void deleteKnot(final int index) {

      if (index < 0 || index > curveValues.allValuesX.length - 1) {
         return;
      }

      if (curveValues.allValuesX.length <= 2) {
         return;
      }

      _isCurveUpdated = true;
      curveValues.removeKnot(index);
   }

   /**
    * Draws the tone curve and, if active, the knots on the curve.
    */
   public void draw(final Graphics2D g, final boolean darkTheme) {
      drawCurve(g, darkTheme);
      if (active) {
         drawKnots(g, darkTheme);
      }
   }

   /**
    * Draws the curve based on precomputed curve plot data.
    */
   private void drawCurve(final Graphics2D g, final boolean darkTheme) {

      updateCurvePlotData();
      final Path2D path = new Path2D.Float();
      path.moveTo(0, ((float) curvePlotData[0] / 255) * height);
      for (int i = 0; i < curvePlotData.length; i++) {
         final float x = ((float) i / 255) * width;
         final float y = ((float) curvePlotData[i] / 255) * height;
         path.lineTo(x, y);
      }

      g.setColor(channel.getDrawColor(active, darkTheme));
      g.setStroke(CURVE_STROKE);
      g.draw(path);
   }

   /**
    * Draws the knots on the curve if the curve is active,
    * highlighting control points.
    */
   private void drawKnots(final Graphics2D g, final boolean darkTheme) {

      g.setColor(darkTheme ? Color.WHITE : Color.BLACK);
      g.setStroke(POINT_STROKE);
      final int knotDiameter = 2 * KNOT_RADIUS_PIXELS;
      for (int i = 0; i < curveValues.allValuesX.length; i++) {
         g.drawOval(
               (int) (curveValues.allValuesX[i] * width) - KNOT_RADIUS_PIXELS,
               (int) (curveValues.allValuesY[i] * height) - KNOT_RADIUS_PIXELS,
               knotDiameter,
               knotDiameter);
      }
   }

   public int getKnotIndexAt(final Point2D.Float p) {

      for (int i = 0; i < curveValues.allValuesX.length; i++) {
         if (isOver(p, new Point2D.Float(curveValues.allValuesX[i], curveValues.allValuesY[i]))) {
            return i;
         }
      }

      return -1;
   }

   /**
    * Checks if the given point can be positioned at the given index.
    *
    * @param index
    *           the knot index
    * @param point
    *           the normalized point to check
    *
    * @return true if the point is within the draggable range, false otherwise
    */
   public boolean isDraggedIn(final int index, final Point2D.Float point) {

      if (index <= 0 || index > curveValues.allValuesX.length - 1) {
         return false;
      }

      return point.x < curveValues.allValuesX[index] && point.x > curveValues.allValuesX[index - 1];
   }

   /**
    * Checks if a point is within the draggable range of the specified knot index.
    *
    * @param index
    *           the knot index
    * @param point
    *           the point to check
    *
    * @return true if the point is out of range, false otherwise
    */
   public boolean isDraggedOutOfRange(final int index, final Point2D.Float point) {

      if (index <= 0 || index >= curveValues.allValuesX.length - 1) {
         return false;
      }

      return point.x > curveValues.allValuesX[index + 1] + 0.02f || point.x < curveValues.allValuesX[index - 1] - 0.02f;
   }

   public boolean isOverKnot(final int index) {

      final var p = new Point2D.Float(curveValues.allValuesX[index], curveValues.allValuesY[index]);
      for (int i = 0; i < curveValues.allValuesX.length; i++) {
         if (i != index && isOver(p, new Point2D.Float(curveValues.allValuesX[i], curveValues.allValuesY[i]))) {
            return true;
         }
      }

      return false;
   }

   public boolean isOverKnot(final Point2D.Float p) {
      return getKnotIndexAt(p) >= 0;
   }

   /**
    * Resets the curve to a straight diagonal line from (0,0) to (1,1).
    */
   public void reset() {

      curveValues.allValuesX = new float[] { 0, 1 };
      curveValues.allValuesY = new float[] { 0, 1 };

      _isCurveUpdated = true;
   }

   public void setActive(final boolean active) {
      this.active = active;
   }

   /**
    * Sets the position of an existing knot at the given index.
    *
    * @param index
    *           the knot index
    * @param point
    *           the new position for the knot, normalized to [0,1] bounds
    */
   public void setKnotPosition(final int index, final Point2D.Float point) {

      final int lastIndex = curveValues.allValuesX.length - 1;

      if (index < 0 || index > lastIndex) {
         return;
      }

      // check prev/next index - knots can't change their index
      if (index > 0 && point.x < curveValues.allValuesX[index - 1]) {
         point.x = curveValues.allValuesX[index - 1];
      } else if (index < lastIndex && point.x > curveValues.allValuesX[index + 1]) {
         point.x = curveValues.allValuesX[index + 1];
      }

      curveValues.allValuesX[index] = ImageMath.clamp01(point.x);
      curveValues.allValuesY[index] = ImageMath.clamp01(point.y);

      _isCurveUpdated = true;
   }

   public void setSize(final int width, final int height) {

      this.width = width;
      this.height = height;
   }

   /**
    * Restores the curve state from a previously saved string representation.
    */
   public void setStateFrom(final String savedValue) {

      final String[] xyPairs = savedValue.split("#"); //$NON-NLS-1$
      final int numPoints = xyPairs.length;
      curveValues.allValuesX = new float[numPoints];
      curveValues.allValuesY = new float[numPoints];
      for (int i = 0; i < numPoints; i++) {
         final String pair = xyPairs[i];
         final int commaIndex = pair.indexOf(',');
         final String pairX = pair.substring(0, commaIndex);
         final String pairY = pair.substring(commaIndex + 1);
         curveValues.allValuesX[i] = Float.parseFloat(pairX);
         curveValues.allValuesY[i] = Float.parseFloat(pairY);
      }
      _isCurveUpdated = true;
   }

   /**
    * Converts the curve data to a saveable string format.
    */
   public String toSaveString() {

      final int numPoints = curveValues.allValuesX.length;
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < numPoints; i++) {
         sb.append(curveValues.allValuesX[i]);
         sb.append(","); //$NON-NLS-1$
         sb.append(curveValues.allValuesY[i]);
         if (i != numPoints - 1) {
            sb.append("#"); //$NON-NLS-1$
         }
      }
      return sb.toString();
   }

   /**
    * Precomputes the curve data for rendering,
    * if changes were made since the last render.
    */
   private void updateCurvePlotData() {

      if (_isCurveUpdated) {
         _isCurveUpdated = false;
         curvePlotData = curveValues.makeTable();
      }
   }
}
