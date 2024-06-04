/*
 * Copyright (c) 2010, Martin Luboschik, Hilko Cords
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   - Neither the name of the University of Rostock (Germany) nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL MARTIN LUBOSCHIK OR HILKO CORDS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Please report any oportunity to speed up to
 * martin DOT luboschik AT uni-rostock DOT de
 *
 * Have fun with this labeling approach =)
 */
package particlelabeling;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;

import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.swt.graphics.GC;

/**
 * <p>
 * This class enables a very very fast labeling of several thausand point-features (2D-points)
 * within
 * some milliseconds (in general). All labels are placed without overlapping each other. Moreover,
 * non-label
 * objects may be kept free of labels by transferring them into an internally used data structure.
 * The implemented labeling
 * method is a greedy algorithm whose runtime depends on the complexity of labeling:
 * the less space is available for labels, the more time does it take to process all labels.
 * </p>
 * <p>
 * The following code gives an example of usage:<br>
 * <br>
 * <code>
 * BufferedImage        conflictMap = new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);<br>
 * ...//draw something to respect later<br><br>
 * Vector pfeatures   = new Vector();<br>
 * ...//generate the point-features to label and calculate their label-sizes<br><br>
 * PointFeatureLabeler  labeler = new PointFeatureLabeler();<br>
 *<br><br>
 * labeler.loadDataStadard(pfeatures,0,conflictMap.getWidth(),0,conflictMap.getHeight());<br>
 * labeler.respectMap(conflictMap);<br>
 * labeler.label_StandardPipelineAll();<br>
 * //all labeling is done now, the resulting label positions are stored within the point-features
 * </code><br>
 *
 * </p>
 * <p>
 * See "Particle-Based Labeling: Fast Point-Feature Labeling without Obscuring Other Visual
 * Features"
 * (M. Luboschik, H. Cords, H. Schumann) for details.
 * </p>
 * <p>
 *
 * The accompanying source or binary forms are published under the terms of the <b>New BSD
 * License</b>:<br>
 * <br>
 * Copyright (c) 2010, Martin Luboschik, Hilko Cords<br>
 * All rights reserved.
 * </p>
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <ul>
 * <li>Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <li>Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <li>Neither the name of the University of Rostock (Germany) nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * </ul>
 * </p>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL MARTIN LUBOSCHIK OR HILKO CORDS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </p>
 *
 * @author M. Luboschik
 *
 * @version 1.0
 *
 * @see PointFeature
 */
public class PointFeatureLabeler extends Object {

   private static double    _deg_to_rad            = Math.PI / 180.;

   private static int       _numSamplePoints  = 500;
   private static float[][] _spiral           = new float[_numSamplePoints][2];

   private static int       _angleSubDivision      = 4;
   private static float[]   _sin                   = new float[360 * _angleSubDivision];
   private static float[]   _cos                   = new float[360 * _angleSubDivision];

   private static float     _spiralRadius          = 100;
   private static float     _numWindings           = 20;

   static {

      for (int i = 1; i < _numSamplePoints; i++) {

         final float m = (float) i / (float) _numSamplePoints;

         _spiral[i][0] = -(float) Math.cos(2 * Math.PI * Math.sqrt(m) * _numWindings) * m * _spiralRadius;
         _spiral[i][1] = (float) Math.sin(2 * Math.PI * Math.sqrt(m) * _numWindings) * m * _spiralRadius;
      }

      for (int i = 0; i < 360 * _angleSubDivision; i++) {

         _sin[i] = (float) Math.sin(((double) i / (double) _angleSubDivision) * _deg_to_rad);
         _cos[i] = (float) Math.cos(((double) i / (double) _angleSubDivision) * _deg_to_rad);
      }
   }

   private ParticleStore            _particleStorage;
   private List<List<PointFeature>> _pointFeatures;

   //width, heigth and boundaries of the labeling area
   private int     laW, laH;
   private int     laL, laR, laT, laB;

   private float   minLabelW, minLabelH;
   private float   maxLabelW, maxLabelH;

   private int     _numPriorityLevels;

   private boolean _isDataLoaded = false;

   /*
    * ########################################################################
    * GLOBAL VARIABLES TO SPEED UP OFTEN CALLED METHODS EG.
    * RESPECTING 1000 CIRCLES, LINES ...
    * ## gl_XXX denotes a global variable
    * ## XXX denotes the corresponding method
    * ##### rL - respectLine
    * ##### rC - respectCircle
    * ##### rM - respectMap
    * ##### rB - respectBox
    * ##### rP - respectPoints
    * ########################################################################
    */

   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   //RESPECT LINE
   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   private int   gl_rL_i;
   private float gl_rL_x;
   private float gl_rL_y;
   private float gl_rL_dx;
   private float gl_rL_dy;
   private float gl_rL_length;
   private float gl_rL_delta;

   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   //RESPECT POINTS
   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   private int gl_rP_length;
   private int gl_rP_i;

   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   //RESPECT CIRCLE
   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   private float   gl_rC_newParticleX;
   private float   gl_rC_newParticleY;
   private float   gl_rC_deltaX;
   private float   gl_rC_deltaY;
   private float   gl_rC_secantLength;
   private float   gl_rC_radiusStepWidth;
   private float   gl_rC_angleDelta;
   private float   gl_rC_usedAngleDelta;
   private int     gl_rC_i;
   private int     gl_rC_sRadiusIndex;
   private int     gl_rC_eRadiusIndex;
   private double  gl_rC_angle;
   private double  gl_rC_startAngle;
   private double  gl_rC_stopAngle;
   private double  gl_rC_usedAngle;
   private boolean gl_rC_newRound;

   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   //RESPECT BOX
   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   private float gl_rB_newParticleX;
   private float gl_rB_newParticleY;

   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   //RESPECT MAP
   //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   private int gl_rM_x;
   private int gl_rM_y;
   private int gl_rM_maxX;
   private int gl_rM_maxY;
   private int gl_rM_color;

   /**
    * Generates a new PointFeatureLabeler.
    */
   public PointFeatureLabeler() {
      super();
   }

   /**
    * Converts a filled box into conflict particles. These particles are returned as their positions
    * in
    * a two dimensional array.
    *
    * @param x
    *           left coordinate of the box
    * @param y
    *           top coordinate of the box
    * @param w
    *           width of the box
    * @param h
    *           height of the box
    * @param hResolution
    *           the maximum horizontal distance of particles within the box
    * @param vResolution
    *           the maximum horizontal distance of particles within the box
    *
    * @return the conflict particles generated, with their x-position at [particle][0] and
    *         y-position at [particle][1]
    */
   public static float[][] sampleBox(final float x, final float y, final float w, final float h, final float hResolution, final float vResolution) {

      final FloatArrayList xs = new FloatArrayList();
      final FloatArrayList ys = new FloatArrayList();

      int i;

      float newParticleX;
      float newParticleY;
      final float r = x + w;
      final float b = y + h;

      newParticleX = x;

      while (newParticleX < r) {
         newParticleY = y;
         while (newParticleY < b) {
            xs.add(newParticleX);
            ys.add(newParticleY);
            newParticleY += vResolution;
         }
         newParticleX += hResolution;
      }

      newParticleX = r;
      newParticleY = y;

      while (newParticleY < b) {
         xs.add(newParticleX);
         ys.add(newParticleY);
         newParticleY += vResolution;
      }

      newParticleX = x;
      newParticleY = b;

      while (newParticleX < r) {
         xs.add(newParticleX);
         ys.add(newParticleY);
         newParticleX += hResolution;
      }

      xs.add(r);
      ys.add(b);

      final int length = xs.size();
      final float[][] output = new float[length][2];

      for (i = 0; i < length; i++) {
         output[i][0] = xs.get(i);
         output[i][1] = ys.get(i);
      }

      return output;

   }

   /**
    * Converts a filled circle into conflict particles. These particles are returned as their
    * positions in
    * a two dimensional array.
    *
    * @param centerX
    *           x-coordinate of center point
    * @param centerY
    *           y-coordinate of center point
    * @param circleRadius
    *           radius of the circle
    * @param resolution
    *           the maximum distance of particles within the circle
    *
    * @return the conflict particles generated, with their x-position at [particle][0] and
    *         y-position at [particle][1]
    */
   public static float[][] sampleCircle(final float centerX, final float centerY, final float circleRadius, final float resolution) {

      final FloatArrayList newParticleXs = new FloatArrayList();
      final FloatArrayList newParticleYs = new FloatArrayList();

      int i;
      int length;

      float newParticleX, newParticleY;
      float deltaX, deltaY;
      float secantLength;

      final float radiusStepWidth = resolution;
      float angleDelta = 90f;
      float usedAngleDelta = angleDelta;
      int sRadiusIndex = 0;
      final int eRadiusIndex = (int) Math.floor(circleRadius / radiusStepWidth);

      double angle;
      double usedAngle;
      double startAngle = 0;
      double stopAngle = 360;

      boolean newRound = true;

      //adding the center-particle
      newParticleXs.add(centerX);
      newParticleYs.add(centerY);

      //adding the border-particles
      //angle delta (0.5d) could be calculated to hit each pixel only once !!!
      for (angle = 0; angle < 360; angle += 0.5d) {
         newParticleXs.add(centerX + _cos[(int) (angle * _angleSubDivision)] * circleRadius);
         newParticleYs.add(centerY + _sin[(int) (angle * _angleSubDivision)] * circleRadius);
      }

      //filling the circle
      while (sRadiusIndex < eRadiusIndex) {

         //length of an secant depends on angleDelta
         secantLength = 2 * sRadiusIndex * radiusStepWidth * _sin[(int) (angleDelta / 2 * _angleSubDivision)];

         //if secant larger than min labeldimension, subdivide angleDelta
         while (secantLength > radiusStepWidth) {
            angleDelta /= 2;
            secantLength = 2 * sRadiusIndex * radiusStepWidth * _sin[(int) (angleDelta / 2 * _angleSubDivision)];
            newRound = true;
         }

         //method sends beams from center to border to avoid multiple calc. of sin and cos
         //needs only additions

         //if new beams necessary (due to subdivision of deltaAngle)
         if (newRound) {

            //rotating the starting beam and calculating the angle delta to avoid
            //generating particles at same positions
            usedAngleDelta = angleDelta;

            if (sRadiusIndex != 0) {
               if (sRadiusIndex == 1) {
                  usedAngleDelta = angleDelta + angleDelta;
               }
               startAngle += angleDelta;
               stopAngle = 360 + startAngle;
            }

            //iterating the beams...
            for (angle = startAngle; angle < stopAngle; angle += usedAngleDelta) {

               usedAngle = angle % 360;

               //getting cos and sin only once per beam
               deltaX = _cos[(int) (usedAngle * _angleSubDivision)] * radiusStepWidth;
               deltaY = _sin[(int) (usedAngle * _angleSubDivision)] * radiusStepWidth;

               //placing the first particle of beam
               newParticleX = centerX + sRadiusIndex * deltaX;
               newParticleY = centerY + sRadiusIndex * deltaY;
               newParticleXs.add(newParticleX);
               newParticleYs.add(newParticleY);

               //iterating the beam from center to border
               for (i = sRadiusIndex; i < eRadiusIndex; i++) {
                  newParticleX += deltaX;
                  newParticleY += deltaY;
                  newParticleXs.add(newParticleX);
                  newParticleYs.add(newParticleY);
               }
            }
            newRound = false;
         }
         sRadiusIndex++;
      }

      length = newParticleXs.size();

      final float[][] output = new float[length][2];

      for (i = 0; i < length; i++) {
         output[i][0] = newParticleXs.get(i);
         output[i][1] = newParticleYs.get(i);
      }

      return output;
   }

   /**
    * Converts a line into conflict particles. See
    * {@link #sampleLine(float x1, float y1, float x2, float y2, float resolution)}
    * with <code>resolution</code> = 1.
    *
    * @param x1
    *           x-coordinate of point 1
    * @param y1
    *           y-coordinate of point 1
    * @param x2
    *           x-coordinate of point 2
    * @param y2
    *           y-coordinate of point 2
    *
    * @see #sampleLine(float, float, float, float, float)
    *
    * @return the conflict particles generated, with their x-position at [particle][0] and
    *         y-position at [particle][1]
    */
   public static float[][] sampleLine(final float x1, final float y1, final float x2, final float y2) {
      return sampleLine(x1, y1, x2, y2, 1f);
   }

   /**
    * Converts a line into conflict particles. Every <code>resolution<sup>th</sup></code>
    * pixel along the line generates a conflict particle.
    * These particles are returned as their positions in
    * a two dimensional array.
    *
    * @param x1
    *           x-coordinate of point 1
    * @param y1
    *           y-coordinate of point 1
    * @param x2
    *           x-coordinate of point 2
    * @param y2
    *           y-coordinate of point 2
    * @param resolution
    *           the maximum distance of particles along the line
    *
    * @return the conflict particles generated, with their x-position at [particle][0] and
    *         y-position at [particle][1]
    */
   public static float[][] sampleLine(final float x1, final float y1, final float x2, final float y2, final float resolution) {

      final FloatArrayList xs = new FloatArrayList();
      final FloatArrayList ys = new FloatArrayList();

      float dx = x2 - x1;
      float dy = y2 - y1;
      final float l = (float) Math.ceil(Math.sqrt(dx * dx + dy * dy));
      final float delta = (1f / l) * resolution;

      dx *= delta;
      dy *= delta;

      float x = x1;
      float y = y1;

      int i = 0;

      while (i++ < l) {
         xs.add(x);
         ys.add(y);
         x += dx;
         y += dy;
      }

      final int length = xs.size();
      final float[][] output = new float[length][2];

      for (i = 0; i < length; i++) {
         output[i][0] = xs.get(i);
         output[i][1] = ys.get(i);
      }

      return output;

   }

   /**
    * Converts a map that is given by a <code>BufferedImage</code> into conflict particles. See
    * {@link #sampleMap(BufferedImage map, Color emptySpaceColor, int resolution)}
    * with <code>emptySpaceColor = Color.white</code> and <code>resolution</code> = 1.
    *
    * @param map
    *           the map to be converted given as an <code>BufferedImage</code>
    *
    * @return the conflict particles generated, with their x-position at [particle][0] and
    *         y-position at [particle][1]
    *
    * @see #respectMap(java.awt.image.BufferedImage, java.awt.Color, int)
    */
   public static float[][] sampleMap(final BufferedImage map) {
      return sampleMap(map, Color.white, 1);
   }

   /**
    * Converts a map that is given by a <code>BufferedImage</code> into conflict particles. See
    * {@link #sampleMap(BufferedImage map, Color emptySpaceColor, int resolution)}
    * with <code>resolution</code> = 1.
    *
    * @param map
    *           the map to be converted given as an <code>BufferedImage</code>
    * @param emptySpaceColor
    *           the color that defines the empty space
    *
    * @return the conflict particles generated, with their x-position at [particle][0] and
    *         y-position at [particle][1]
    *
    * @see #respectMap(java.awt.image.BufferedImage, java.awt.Color, int)
    */
   public static float[][] sampleMap(final BufferedImage map, final Color emptySpaceColor) {
      return sampleMap(map, emptySpaceColor, 1);
   }

   /**
    * Converts a map that is given by a <code>BufferedImage</code> into conflict
    * particles. Every <code>resolution<sup>th</sup></code> pixel (horizontal and
    * vertical) that is not equal to <code>emptySpaceColor</code> generates
    * a conflict particle. These particles are returned as their positions in
    * a two dimensional array.
    *
    * @param map
    *           the map to be converted given as a <code>BufferedImage</code>
    * @param emptySpaceColor
    *           the color that defines the empty space
    * @param resolution
    *           the maximum distance of particles within the map
    *
    * @return the conflict particles generated, with their x-position at [particle][0] and
    *         y-position at [particle][1]
    */
   public static float[][] sampleMap(final BufferedImage map, final Color emptySpaceColor, final int resolution) {

      int i, x, y;
      final int maxX = map.getWidth();
      final int maxY = map.getHeight();
      final int color = emptySpaceColor.getRGB();

      final IntArrayList xs = new IntArrayList();
      final IntArrayList ys = new IntArrayList();

      for (y = 0; y < maxY; y += resolution) {
         for (x = 0; x < maxX; x += resolution) {
            if (map.getRGB(x, y) != color) {
               xs.add(x);
               ys.add(y);
            }
         }
      }

      final int length = xs.size();
      final float[][] output = new float[length][2];

      for (i = 0; i < length; i++) {
         output[i][0] = xs.get(i);
         output[i][1] = ys.get(i);
      }

      return output;

   }

   /**
    * Sets up the parameters of the internally used spiral to find distant label positions.
    * See "Particle-Based Labeling: Fast Point-Feature Labeling without Obscuring Other Visual
    * Features"
    * (M. Luboschik, H. Cords, H. Schumann) for details. Standard parameters are
    * <ul>
    * <li>numSamplePoints = 500
    * <li>radius = 150
    * <li>numWindings = 20
    * <li>spiralDirection = -1
    * </ul>
    *
    * @param numSamplePoints
    *           the number of sample points along the spiral
    * @param radius
    *           the radius of the spiral
    * @param numWindings
    *           the number of windings till the maximum radius
    * @param spiralDirection
    *           the direction of the spiral (-1 or 1)
    */
   public static void setSpiralParameters(final int numSamplePoints,
                                          final float radius,
                                          final float numWindings,
                                          final float spiralDirection) {

      _numSamplePoints = numSamplePoints;
      _spiral = new float[numSamplePoints][2];

      float rotationDirection;

      if (spiralDirection >= 0) {
         rotationDirection = 1f;
      } else {
         rotationDirection = -1f;
      }

// SET_FORMATTING_OFF

      for (int i = 1; i < _numSamplePoints; i++) {

         final float m = (float) i / (float) _numSamplePoints;

         _spiral[i][0] = rotationDirection * (float) Math.cos(2 * Math.PI * Math.sqrt(m) * numWindings) * m * radius;
         _spiral[i][1] =                     (float) Math.sin(2 * Math.PI * Math.sqrt(m) * numWindings) * m * radius;
      }

// SET_FORMATTING_ON
   }

   public static void setSpiralRadius(final float spiralRadius) {

      if (spiralRadius == _spiralRadius) {

         // radius is not modified

         return;
      }

      _spiralRadius = spiralRadius;
      _numWindings = spiralRadius / 2;
      _numSamplePoints = (int) (spiralRadius * 3);

      _spiral = new float[_numSamplePoints][2];

      for (int i = 1; i < _numSamplePoints; i++) {

         final float m = (float) i / (float) _numSamplePoints;

         _spiral[i][0] = -(float) Math.cos(2 * Math.PI * Math.sqrt(m) * _numWindings) * m * _spiralRadius;
         _spiral[i][1] = (float) Math.sin(2 * Math.PI * Math.sqrt(m) * _numWindings) * m * _spiralRadius;
      }
   }

   /**
    * Draws the internally used particles at the given graphic context for debugging purposes. The
    * particles are drawn with blue pixels.
    *
    * @param gc
    *           the graphic context to be drawn at.
    */
   public void drawParticles(final GC gc) {

      if (_isDataLoaded == false) {
         return;
      }

      gc.setBackground(UI.SYS_COLOR_GREEN);

      final int maxX = _particleStorage.getColumns();
      final int maxY = _particleStorage.getRows();

      for (int i = 0; i < maxX; i++) {
         for (int j = 0; j < maxY; j++) {

            final float[][] part = _particleStorage.getParticlesOfCell(i, j);

            for (final float[] element : part) {

               gc.fillRectangle(

                     (int) (element[1]),
                     (int) (element[2]),

                     1,
                     1);
            }
         }
      }
   }

   /**
    * Draws the internally used spiral at the given graphic context for debugging purposes. The
    * spiral is drawn with red pixels.
    *
    * @param gc
    *           the graphic context to be draw at.
    * @param x
    *           the x coordinate of the center of the spiral to be drawn.
    * @param y
    *           the y coordinate of the center of the spiral to be drawn.
    */
   public void drawSpiral(final GC gc, final int x, final int y) {

      gc.setBackground(UI.SYS_COLOR_WHITE);

      for (int i = 0; i < _numSamplePoints; i++) {

         gc.fillRectangle(

               (int) (x + _spiral[i][0]),
               (int) (y + _spiral[i][1]),

               1,
               1);
      }
   }

   /**
    * Finds minimum and maximum dimensions of the given pointfeatures.
    *
    * @param pointfeatures
    *           The point-features in which to find the minimum and
    *           maxium label dimensions.
    */
   private void findMinMaxLabelDimensions(final List<List<PointFeature>> pfs) {

      float lw;
      float lh;

      this.maxLabelW = Float.MIN_VALUE;
      this.maxLabelH = Float.MIN_VALUE;
      this.minLabelW = Float.MAX_VALUE;
      this.minLabelH = Float.MAX_VALUE;

      for (final List<PointFeature> priorityLevel : pfs) {
         for (final PointFeature p : priorityLevel) {

            lw = p.labelBoxW;
            lh = p.labelBoxH;

            this.maxLabelW = Math.max(this.maxLabelW, lw);
            this.minLabelW = Math.min(this.minLabelW, lw);
            this.maxLabelH = Math.max(this.maxLabelH, lh);
            this.minLabelH = Math.min(this.minLabelH, lh);
         }
      }
   }

   /**
    * Labels the loaded point-features of priority <code>prio</code> in a greedy manner according to
    * the positions
    * of the 4-position label model. Priority 0 denotes the highest priority.
    *
    * @param prio
    *           the priority level to be labeled
    *
    * @return the number of labels that were successfully placed
    */
   public int label_greedy1_4(final int prio) {

      if (_isDataLoaded == false) {
         return 0;
      }

      final List<PointFeature> pointFeaturesToLabel = _pointFeatures.get(prio);

      if (pointFeaturesToLabel != null) {

         float[][] conflictParticles;

         boolean isAllowed = true;

         float l = 0f;
         float r = 0f;
         float t = 0f;
         float b = 0f;

         float lw = 0f;
         float lh = 0f;
         float px = 0f;
         float py = 0f;

         float hGapDueToExtend = 0f;
         float vGapDueToExtend = 0f;

         int numLabels = 0;
         int numConflictingParticles = 0;
         int i = 0;
         int particleID = 0;

         float[] testParticle;
         float testParticleX;
         float testParticleY;

         for (final PointFeature pf : pointFeaturesToLabel) {

            if (pf.isEnabledForLabeling && pf.isLabeled == false) {

               lw = pf.labelBoxW;
               lh = pf.labelBoxH;
               px = pf.getX();
               py = pf.getY();

               particleID = pf.particleId;

               //Gap between label and center of point feature
               hGapDueToExtend = (pf.rectangularExtendW / 2f) + 0.1f;
               vGapDueToExtend = (pf.rectangularExtendH / 2f) + 0.1f;

               //right above the point feature
               l = px + hGapDueToExtend;
               r = l + lw;
               b = py - vGapDueToExtend;
               t = b - lh;

               //getting the particles to test with
               conflictParticles = _particleStorage.getInvolvedParticles(

                     px - hGapDueToExtend - lw,
                     t,
                     r,
                     py + vGapDueToExtend + lh);

               numConflictingParticles = conflictParticles.length;

               if ((r > this.laR) || (t < this.laT)) {

                  //labelarea outside viewport?
                  isAllowed = false;

               } else {

                  isAllowed = true;

                  i = 0;

                  //testing with all conflict particles except
                  //the currently labeled as it may be adjacent (GAP = 0)
                  //with the current label area. this would prohibit the
                  //current label position.

                  while (isAllowed && i < numConflictingParticles) {

                     testParticle = conflictParticles[i++];
                     testParticleX = testParticle[1];
                     testParticleY = testParticle[2];

                     if ((testParticle[0] != particleID) &&
                           (testParticleX >= l) &&
                           (testParticleX <= r) &&
                           (testParticleY <= b) &&
                           (testParticleY >= t)) {

                        isAllowed = false;
                     }
                  }
               }

               if (isAllowed) {

                  //labelposition found

                  numLabels++;

                  pf.isLabeled = true;
                  pf.isLabeled_greedy1_4 = true;

                  pf.labelBoxL = l;
                  pf.labelBoxR = r;
                  pf.labelBoxT = t;
                  pf.labelBoxB = b;

                  //generating new particles to prevent future occlusion

                  if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                     //different labelsizes
                     respectBoxIntern(l, r, t, b);

                  } else {

                     //equal labelsizes
                     _particleStorage.addParticle(l, b);
                     _particleStorage.addParticle(l, t);
                     _particleStorage.addParticle(r, t);
                     _particleStorage.addParticle(r, b);
                  }

               } else {

                  // left above

                  r = px - hGapDueToExtend;
                  l = r - lw;

                  if ((l < this.laL) || (t < this.laT)) {

                     //labelarea outside viewport?
                     isAllowed = false;

                  } else {

                     isAllowed = true;

                     i = 0;

                     //testing with all conflict particles except
                     //the currently labeled as it may be adjacent (GAP = 0)
                     //with the current label area. this would prohibit the
                     //current label position.

                     while (isAllowed && (i < numConflictingParticles)) {

                        testParticle = conflictParticles[i++];

                        testParticleX = testParticle[1];
                        testParticleY = testParticle[2];

                        if ((testParticle[0] != particleID) &&
                              (testParticleX >= l) &&
                              (testParticleX <= r) &&
                              (testParticleY <= b) &&
                              (testParticleY >= t)) {

                           isAllowed = false;
                        }
                     }
                  }

                  if (isAllowed) {

                     //labelposition found

                     numLabels++;

                     pf.isLabeled = true;
                     pf.isLabeled_greedy1_4 = true;

                     pf.labelBoxL = l;
                     pf.labelBoxR = r;
                     pf.labelBoxT = t;
                     pf.labelBoxB = b;

                     //generating new particles to prevent future occlusion

                     if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                        //different labelsizes
                        respectBoxIntern(l, r, t, b);

                     } else {

                        //equal labelsizes
                        _particleStorage.addParticle(l, b);
                        _particleStorage.addParticle(l, t);
                        _particleStorage.addParticle(r, t);
                        _particleStorage.addParticle(r, b);
                     }

                  } else {

                     //left below

                     t = py + vGapDueToExtend;
                     b = t + lh;

                     if ((l < this.laL) || (b > this.laB)) {

                        //labelarea outside viewport?
                        isAllowed = false;

                     } else {

                        isAllowed = true;

                        i = 0;

                        //testing with all conflict particles except
                        //the currently labeled as it may be adjacent (GAP = 0)
                        //with the current label area. this would prohibit the
                        //current label position.

                        while ((isAllowed) && (i < numConflictingParticles)) {
                           testParticle = conflictParticles[i++];
                           testParticleX = testParticle[1];
                           testParticleY = testParticle[2];
                           if ((testParticle[0] != particleID) &&
                                 (testParticleX >= l) &&
                                 (testParticleX <= r) &&
                                 (testParticleY <= b) &&
                                 (testParticleY >= t)) {
                              isAllowed = false;
                           }
                        }
                     }

                     if (isAllowed) {

                        //labelposition found

                        numLabels++;

                        pf.isLabeled = true;
                        pf.isLabeled_greedy1_4 = true;

                        pf.labelBoxL = l;
                        pf.labelBoxR = r;
                        pf.labelBoxT = t;
                        pf.labelBoxB = b;

                        //generating new particles to prevent future occlusion

                        if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                           //different labelsizes
                           respectBoxIntern(l, r, t, b);

                        } else {

                           //equal labelsizes
                           _particleStorage.addParticle(l, b);
                           _particleStorage.addParticle(l, t);
                           _particleStorage.addParticle(r, t);
                           _particleStorage.addParticle(r, b);
                        }

                     } else {

                        //right below

                        l = px + hGapDueToExtend;
                        r = l + lw;

                        if ((r > this.laR) || (b > this.laB)) {

                           //labelarea outside viewport?
                           isAllowed = false;

                        } else {

                           isAllowed = true;

                           i = 0;

                           //testing with all conflict particles except
                           //the currently labeled as it may be adjacent (GAP = 0)
                           //with the current label area. this would prohibit the
                           //current label position.

                           while ((isAllowed) && (i < numConflictingParticles)) {
                              testParticle = conflictParticles[i++];
                              testParticleX = testParticle[1];
                              testParticleY = testParticle[2];
                              if ((testParticle[0] != particleID) &&
                                    (testParticleX >= l) &&
                                    (testParticleX <= r) &&
                                    (testParticleY <= b) &&
                                    (testParticleY >= t)) {
                                 isAllowed = false;
                              }
                           }
                        }

                        if (isAllowed) {

                           //labelposition found

                           numLabels++;

                           pf.isLabeled = true;
                           pf.isLabeled_greedy1_4 = true;

                           pf.labelBoxL = l;
                           pf.labelBoxR = r;
                           pf.labelBoxT = t;
                           pf.labelBoxB = b;

                           //generating new particles to prevent future occlusion

                           if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                              //different labelsizes
                              respectBoxIntern(l, r, t, b);

                           } else {

                              //equal labelsizes
                              _particleStorage.addParticle(l, b);
                              _particleStorage.addParticle(l, t);
                              _particleStorage.addParticle(r, t);
                              _particleStorage.addParticle(r, b);
                           }
                        }
                     }
                  }
               }
            }
         }

         return numLabels;
      }

      return 0;
   }

   /**
    * Labels the loaded point-features of priority <code>prio</code> in a greedy manner according to
    * the positions
    * 5 to 8 of the 8-position label model. Priority 0 denotes the highest priority.
    *
    * @param prio
    *           the priority level to be labeled
    *
    * @return the number of labels that were successfully placed
    */
   public int label_greedy5_8(final int prio) {

      if (_isDataLoaded) {

         final List<PointFeature> pointFeaturesToLabel = _pointFeatures.get(prio);

         if (pointFeaturesToLabel != null) {

            float[][] conflictParticles;

            boolean allowed = true;

            float px = 0f;
            float py = 0f;
            float lw = 0f;
            float lh = 0f;
            float lw2 = 0f;
            float lh2 = 0f;

            float hGapDueToExtend = 0f;
            float vGapDueToExtend = 0f;

            float l = 0f;
            float r = 0f;
            float t = 0f;
            float b = 0f;

            int size = 0;
            int i = 0;
            int particleID = 0;
            int labelCount = 0;

            float[] testParticle;
            float testParticleX;
            float testParticleY;

            for (final PointFeature pf : pointFeaturesToLabel) {

               if (pf.isEnabledForLabeling && !pf.isLabeled) {

                  px = pf.getX();
                  py = pf.getY();
                  lw = pf.labelBoxW;
                  lh = pf.labelBoxH;

                  particleID = pf.particleId;

                  hGapDueToExtend = (pf.rectangularExtendW / 2f) + 0.1f;
                  vGapDueToExtend = (pf.rectangularExtendH / 2f) + 0.1f;

                  lw2 = lw / 2f;
                  lh2 = lh / 2f;

                  // right center

                  l = px + hGapDueToExtend;
                  r = l + lw;
                  t = py - lh2;
                  b = py + lh2;

                  conflictParticles = _particleStorage.getInvolvedParticles(
                        px - hGapDueToExtend - lw,
                        py - vGapDueToExtend - lh,
                        r,
                        py + vGapDueToExtend + lh);

                  size = conflictParticles.length;

                  if ((r > this.laR) || (b > this.laB) || t < this.laT) {

                     allowed = false;

                  } else {

                     allowed = true;

                     i = 0;

                     while ((allowed) && (i < size)) {
                        testParticle = conflictParticles[i++];
                        testParticleX = testParticle[1];
                        testParticleY = testParticle[2];
                        if ((testParticle[0] != particleID) &&
                              (testParticleX >= l) &&
                              (testParticleX <= r) &&
                              (testParticleY <= b) &&
                              (testParticleY >= t)) {
                           allowed = false;
                        }
                     }
                  }

                  if (allowed) {

                     labelCount++;

                     pf.isLabeled = true;
                     pf.isLabeled_greedy5_8 = true;

                     pf.labelBoxL = l;
                     pf.labelBoxR = r;
                     pf.labelBoxT = t;
                     pf.labelBoxB = b;

                     if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                        //different labelsizes
                        respectBoxIntern(l, r, t, b);

                     } else {

                        //equal labelsizes
                        _particleStorage.addParticle(l, b);
                        _particleStorage.addParticle(l, t);
                        _particleStorage.addParticle(r, t);
                        _particleStorage.addParticle(r, b);
                     }

                  } else {

                     // above center

                     l = px - lw2;
                     r = px + lw2;
                     b = py - vGapDueToExtend;
                     t = b - lh;

                     if ((r > this.laR) || (l < this.laL) || t < this.laT) {

                        allowed = false;

                     } else {

                        allowed = true;

                        i = 0;

                        while ((allowed) && (i < size)) {
                           testParticle = conflictParticles[i++];
                           testParticleX = testParticle[1];
                           testParticleY = testParticle[2];
                           if ((testParticle[0] != particleID) &&
                                 (testParticleX >= l) &&
                                 (testParticleX <= r) &&
                                 (testParticleY <= b) &&
                                 (testParticleY >= t)) {
                              allowed = false;
                           }
                        }
                     }

                     if (allowed) {

                        labelCount++;

                        pf.isLabeled = true;
                        pf.isLabeled_greedy5_8 = true;

                        pf.labelBoxL = l;
                        pf.labelBoxR = r;
                        pf.labelBoxT = t;
                        pf.labelBoxB = b;

                        if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                           //different labelsizes
                           respectBoxIntern(l, r, t, b);

                        } else {

                           //equal labelsizes
                           _particleStorage.addParticle(l, b);
                           _particleStorage.addParticle(l, t);
                           _particleStorage.addParticle(r, t);
                           _particleStorage.addParticle(r, b);
                        }

                     } else {

                        // left center

                        r = px - hGapDueToExtend;
                        l = r - lw;
                        t = py - lh2;
                        b = py + lh2;

                        if ((b > this.laB) || (l < this.laL) || t < this.laT) {

                           allowed = false;

                        } else {

                           allowed = true;

                           i = 0;

                           while ((allowed) && (i < size)) {

                              testParticle = conflictParticles[i++];
                              testParticleX = testParticle[1];
                              testParticleY = testParticle[2];

                              if ((testParticle[0] != particleID) &&
                                    (testParticleX >= l) &&
                                    (testParticleX <= r) &&
                                    (testParticleY <= b) &&
                                    (testParticleY >= t)) {

                                 allowed = false;
                              }
                           }
                        }

                        if (allowed) {

                           labelCount++;

                           pf.isLabeled = true;
                           pf.isLabeled_greedy5_8 = true;

                           pf.labelBoxL = l;
                           pf.labelBoxR = r;
                           pf.labelBoxT = t;
                           pf.labelBoxB = b;

                           if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                              //different labelsizes
                              respectBoxIntern(l, r, t, b);

                           } else {

                              //equal labelsizes
                              _particleStorage.addParticle(l, b);
                              _particleStorage.addParticle(l, t);
                              _particleStorage.addParticle(r, t);
                              _particleStorage.addParticle(r, b);
                           }

                        } else {

                           // below center

                           l = px - lw2;
                           r = px + lw2;
                           t = py + vGapDueToExtend;
                           b = t + lh;

                           if ((b > this.laB) || (l < this.laL) || r > this.laR) {

                              allowed = false;

                           } else {

                              allowed = true;

                              i = 0;

                              while ((allowed) && (i < size)) {
                                 testParticle = conflictParticles[i++];
                                 testParticleX = testParticle[1];
                                 testParticleY = testParticle[2];
                                 if ((testParticle[0] != particleID) &&
                                       (testParticleX >= l) &&
                                       (testParticleX <= r) &&
                                       (testParticleY <= b) &&
                                       (testParticleY >= t)) {
                                    allowed = false;
                                 }
                              }
                           }

                           if (allowed) {
                              labelCount++;

                              pf.isLabeled = true;
                              pf.isLabeled_greedy5_8 = true;

                              pf.labelBoxL = l;
                              pf.labelBoxR = r;
                              pf.labelBoxT = t;
                              pf.labelBoxB = b;

                              if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                                 //different labelsizes
                                 respectBoxIntern(l, r, t, b);

                              } else {

                                 //equal labelsizes
                                 _particleStorage.addParticle(l, b);
                                 _particleStorage.addParticle(l, t);
                                 _particleStorage.addParticle(r, t);
                                 _particleStorage.addParticle(r, b);
                              }
                           }
                        }
                     }
                  }
               }
            }
            return (labelCount);
         }
      }
      return 0;
   }

   /**
    * Labels the loaded point-features of priority <code>prio</code> in a greedy manner according to
    * the positions
    * of the slider model. Priority 0 denotes the highest priority.
    *
    * @param prio
    *           the priority level to be labeled
    *
    * @return the number of labels that were successfully placed
    */
   public int label_greedySlider(final int prio) {

      if (_isDataLoaded) {

         final List<PointFeature> pointFeaturesToLabel = _pointFeatures.get(prio);

         if (pointFeaturesToLabel != null) {

            float[][] conflictParticles;

            boolean allowed = true;
            boolean found = false;

            float l = 0f;
            float r = 0f;
            float t = 0f;
            float b = 0f;

            float lw = 0f;
            float lh = 0f;

            float px = 0f;
            float py = 0f;

            float lStop, rStop, tStop, bStop = 0f;

            float hGapDueToExtend = 0f;
            float vGapDueToExtend = 0f;

            int size = 0;
            int i = 0;
            int particleID = 0;
            int labelCount = 0;

            float[] testParticle;
            float testParticleX;
            float testParticleY;

            for (final PointFeature pf : pointFeaturesToLabel) {

               if (pf.isEnabledForLabeling && !pf.isLabeled) {

                  found = false;

                  lw = pf.labelBoxW;
                  lh = pf.labelBoxH;

                  px = pf.getX();
                  py = pf.getY();

                  hGapDueToExtend = (pf.rectangularExtendW / 2f) + 0.1f;
                  vGapDueToExtend = (pf.rectangularExtendH / 2f) + 0.1f;

                  particleID = pf.particleId;

                  lStop = px - hGapDueToExtend - lw;
                  bStop = py + vGapDueToExtend + lh;
                  rStop = px + hGapDueToExtend + lw;
                  tStop = py - vGapDueToExtend - lh;

                  conflictParticles = _particleStorage.getInvolvedParticles(lStop, tStop, rStop, bStop);
                  size = conflictParticles.length;

                  // right above

                  r = rStop;
                  l = rStop - lw;
                  t = tStop;
                  b = tStop + lh;

                  if (t >= this.laT) {

                     //sliding to left

                     while ((l - lStop >= 0.01f) && (!found)) {

                        if ((r > this.laR) || (l < this.laL)) {

                           allowed = false;

                        } else {

                           allowed = true;

                           i = 0;

                           while ((allowed) && (i < size)) {
                              testParticle = conflictParticles[i++];
                              testParticleX = testParticle[1];
                              testParticleY = testParticle[2];
                              if ((testParticle[0] != particleID) &&
                                    (testParticleX >= l) &&
                                    (testParticleX <= r) &&
                                    (testParticleY <= b) &&
                                    (testParticleY >= t)) {
                                 allowed = false;
                              }
                           }
                        }

                        if (allowed) {

                           labelCount++;

                           pf.isLabeled = true;
                           pf.isLabeled_greedySlider = true;

                           pf.labelBoxL = l;
                           pf.labelBoxR = r;
                           pf.labelBoxT = t;
                           pf.labelBoxB = b;

                           if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                              //different labelsizes
                              respectBoxIntern(l, r, t, b);

                           } else {

                              //equal labelsizes
                              _particleStorage.addParticle(l, b);
                              _particleStorage.addParticle(l, t);
                              _particleStorage.addParticle(r, t);
                              _particleStorage.addParticle(r, b);
                           }

                           found = true;

                        } else {

                           //sliding
                           l -= 1f;
                           r -= 1f;

                        }
                     }
                  }

                  if (!found) {

                     //left above

                     l = lStop;
                     r = l + lw;

                     if (l >= this.laL) {

                        //sliding down

                        while ((bStop - b >= 0.01f) && (!found)) {

                           if ((t < this.laT) || (b > this.laB)) {

                              allowed = false;

                           } else {

                              allowed = true;

                              i = 0;

                              while ((allowed) && (i < size)) {
                                 testParticle = conflictParticles[i++];
                                 testParticleX = testParticle[1];
                                 testParticleY = testParticle[2];
                                 if ((testParticle[0] != particleID) &&
                                       (testParticleX >= l) &&
                                       (testParticleX <= r) &&
                                       (testParticleY <= b) &&
                                       (testParticleY >= t)) {
                                    allowed = false;
                                 }
                              }
                           }

                           if (allowed) {

                              labelCount++;

                              pf.isLabeled = true;
                              pf.isLabeled_greedySlider = true;

                              pf.labelBoxL = l;
                              pf.labelBoxR = r;
                              pf.labelBoxT = t;
                              pf.labelBoxB = b;

                              if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                                 //different labelsizes
                                 respectBoxIntern(l, r, t, b);

                              } else {

                                 //equal labelsizes
                                 _particleStorage.addParticle(l, b);
                                 _particleStorage.addParticle(l, t);
                                 _particleStorage.addParticle(r, t);
                                 _particleStorage.addParticle(r, b);
                              }

                              found = true;

                           } else {

                              //sliding
                              t += 1f;
                              b += 1f;

                           }
                        }
                     }
                  }

                  if (!found) {

                     //left below

                     b = bStop;
                     t = bStop - lh;

                     if (b <= this.laB) {

                        //sliding right

                        while ((rStop - r >= 0.01f) && (!found)) {

                           if ((l < this.laL) || (r > this.laR)) {

                              allowed = false;

                           } else {

                              allowed = true;
                              i = 0;

                              while ((allowed) && (i < size)) {
                                 testParticle = conflictParticles[i++];
                                 testParticleX = testParticle[1];
                                 testParticleY = testParticle[2];
                                 if ((testParticle[0] != particleID) &&
                                       (testParticleX >= l) &&
                                       (testParticleX <= r) &&
                                       (testParticleY <= b) &&
                                       (testParticleY >= t)) {
                                    allowed = false;
                                 }
                              }
                           }

                           if (allowed) {

                              labelCount++;

                              pf.isLabeled = true;
                              pf.isLabeled_greedySlider = true;

                              pf.labelBoxL = l;
                              pf.labelBoxR = r;
                              pf.labelBoxT = t;
                              pf.labelBoxB = b;

                              if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                                 //different labelsizes
                                 respectBoxIntern(l, r, t, b);

                              } else {

                                 //equal labelsizes
                                 _particleStorage.addParticle(l, b);
                                 _particleStorage.addParticle(l, t);
                                 _particleStorage.addParticle(r, t);
                                 _particleStorage.addParticle(r, b);
                              }

                              found = true;

                           } else {

                              //sliding
                              r += 1f;
                              l += 1f;
                           }
                        }
                     }
                  }

                  if (!found) {

                     //right below

                     r = rStop;
                     l = r - lw;

                     if (r <= this.laR) {

                        //sliding up
                        while ((t - tStop >= 0.01f) && (!found)) {

                           if ((t < this.laT) || (b > this.laB)) {

                              allowed = false;

                           } else {

                              allowed = true;

                              i = 0;

                              while ((allowed) && (i < size)) {
                                 testParticle = conflictParticles[i++];
                                 testParticleX = testParticle[1];
                                 testParticleY = testParticle[2];
                                 if ((testParticle[0] != particleID) &&
                                       (testParticleX >= l) &&
                                       (testParticleX <= r) &&
                                       (testParticleY <= b) &&
                                       (testParticleY >= t)) {
                                    allowed = false;
                                 }
                              }
                           }

                           if (allowed) {

                              labelCount++;

                              pf.isLabeled = true;
                              pf.isLabeled_greedySlider = true;

                              pf.labelBoxL = l;
                              pf.labelBoxR = r;
                              pf.labelBoxT = t;
                              pf.labelBoxB = b;

                              if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                                 //different labelsizes
                                 respectBoxIntern(l, r, t, b);

                              } else {

                                 //equal labelsizes
                                 _particleStorage.addParticle(l, b);
                                 _particleStorage.addParticle(l, t);
                                 _particleStorage.addParticle(r, t);
                                 _particleStorage.addParticle(r, b);
                              }

                              found = true;

                           } else {
                              //sliding
                              t -= 1f;
                              b -= 1f;
                           }
                        }
                     }
                  }
               }
            }
            return (labelCount);
         }
      }
      return 0;
   }

   /**
    * Labels the loaded point-features of priority <code>prio</code> in a greedy manner according to
    * the positions
    * along a spiral around the point feature. Priority 0 denotes the highest priority.
    *
    * @param prio
    *           the priority level to be labeled
    *
    * @return the number of labels that were successfully placed
    */
   public int label_greedySpiral(final int prio) {

      if (_isDataLoaded) {

         final List<PointFeature> pointFeaturesToLabel = _pointFeatures.get(prio);

         if (pointFeaturesToLabel != null) {

            float[][] conflictParticles;

            boolean allowed = true;
            boolean found = false;

            float l = 0f;
            float r = 0f;
            float t = 0f;
            float b = 0f;

            float lw = 0f;
            float lh = 0f;

            float px = 0f;
            float py = 0f;

            float hGapDueToExtend = 0f;
            float vGapDueToExtend = 0f;

            int spiralPos = 0;
            int size = 0;
            int i = 0;

            int labelCount = 0;

            float[] testParticle;
            float testParticleX;
            float testParticleY;

            float labelX = 0f;
            float labelY = 0f;
            float boxL, boxR, boxT, boxB = 0f;

            for (final PointFeature pf : pointFeaturesToLabel) {

               if (pf.isEnabledForLabeling && !pf.isLabeled) {

                  found = false;

                  px = pf.getX();
                  py = pf.getY();
                  lw = pf.labelBoxW;
                  lh = pf.labelBoxH;

                  hGapDueToExtend = pf.rectangularExtendW / 2f;
                  vGapDueToExtend = pf.rectangularExtendH / 2f;

                  boxL = px - hGapDueToExtend;
                  boxR = px + hGapDueToExtend;
                  boxT = py - vGapDueToExtend;
                  boxB = py + vGapDueToExtend;

                  spiralPos = 0;

                  //walking the spiral
                  while ((spiralPos < _numSamplePoints) && (!found)) {

                     labelX = px + _spiral[spiralPos][0];
                     labelY = py + _spiral[spiralPos][1];

                     l = labelX;
                     r = labelX + lw;
                     t = labelY - lh;
                     b = labelY;

                     allowed = true;

                     if ((l < this.laL) ||
                           (r > this.laR) ||
                           (t < this.laT) ||
                           (b > this.laB)) {

                        //labelarea inside viewport?
                        allowed = false;

                     }

                     if (allowed) {

                        //labelarea intersecting with rectangular extend of point feature?

                        if (l >= boxL && l <= boxR) {
                           // left in box
                           if (t >= boxT && t <= boxB) {
                              // top in box
                              allowed = false;
                           } else if (b >= boxT && b <= boxB) {
                              // bottom in box
                              allowed = false;
                           }
                        }

                        if (r >= boxL && r <= boxR) {
                           // right in box
                           if (t >= boxT && t <= boxB) {
                              // top in box
                              allowed = false;
                           } else if (b >= boxT && b <= boxB) {
                              // bottom in box
                              allowed = false;
                           }
                        }
                     }

                     if (allowed) {

                        conflictParticles = _particleStorage.getInvolvedParticles(l, t, r, b);
                        size = conflictParticles.length;
                        i = 0;

                        //testing with all conflict particles INCLUSIVE
                        //the currently labeled as it should not be adjacent
                        //with the current label area. otherwise, the current labelparticle
                        //could apear inside!!! the tested label area

                        while ((allowed) && (i < size)) {
                           testParticle = conflictParticles[i++];
                           testParticleX = testParticle[1];
                           testParticleY = testParticle[2];
                           if ((testParticleX >= l) &&
                                 (testParticleX <= r) &&
                                 (testParticleY <= b) &&
                                 (testParticleY >= t)) {
                              allowed = false;
                           }
                        }
                     }

                     if (allowed) {

                        labelCount++;

                        pf.isLabeled = true;
                        pf.isLabeled_greedySpiral = true;

                        pf.labelBoxL = l;
                        pf.labelBoxR = r;
                        pf.labelBoxT = t;
                        pf.labelBoxB = b;

                        if ((lh > this.minLabelH) || (lw > this.minLabelW)) {

                           //different labelsizes
                           respectBoxIntern(l, r, t, b);

                        } else {

                           //equal labelsizes
                           _particleStorage.addParticle(l, b);
                           _particleStorage.addParticle(l, t);
                           _particleStorage.addParticle(r, t);
                           _particleStorage.addParticle(r, b);
                        }

                        found = true;

                     } else {

                        spiralPos++;

                     }
                  }
               }
            }
            return labelCount;
         }
      }
      return 0;
   }

   /**
    * Labels all loaded point-features in a greedy manner of priority
    * <code>prio</code> by excecuting the standard labeling pipeline.
    * Excecution is equal to the sequence<br>
    * <ol>
    * <li>{@link #label_greedy1_4(int)}
    * <li>{@link #label_greedy5_8(int)}
    * <li>{@link #label_greedySlider(int)}
    * <li>{@link #label_greedySpiral(int)}
    * </ol>
    * for the selected priority level. Returns the number of overall successfully labeled point
    * features.
    *
    * @param prio
    *           the priority level to be labeled
    *
    * @return the number of labels that were successfully placed
    */
   public int label_StandardPipeline(final int prio) {

      //the single methods
      //label_greedy1_4
      //label_greedy5_8
      //label_greedySlider
      //label_greedySpiral
      //done one after the other

      if (!_isDataLoaded) {
         return 0;
      }

      return 0

            + label_greedy1_4(prio)
            + label_greedy5_8(prio)
            + label_greedySlider(prio)
            + label_greedySpiral(prio)

      ;

   }

   /**
    * Labels all loaded point-features in a greedy manner of priority
    * <code>prio</code> by excecuting the standard labeling pipeline without distant labeling.
    * Excecution is equal to the sequence<br>
    * <ol>
    * <li>{@link #label_greedy1_4(int)}
    * <li>{@link #label_greedy5_8(int)}
    * <li>{@link #label_greedySlider(int)}
    * </ol>
    * for the selected priority level. Returns the number of overall successfully labeled point
    * features.
    *
    * @param prio
    *           the priority level to be labeled
    *
    * @return the number of labels that were successfully placed
    */
   public int label_StandardPipelineAdjacent(final int prio) {

      //the single methods
      //label_greedy1_4
      //label_greedy5_8
      //label_greedySlider
      //done one after the other

      if (!_isDataLoaded) {
         return 0;
      }

      return label_greedy1_4(prio)
            + label_greedy5_8(prio)
            + label_greedySlider(prio);

   }

   /**
    * Labels all loaded point-features in a greedy manner according to their priority.
    * Excecution is equal to the sequence<br>
    * <ol>
    * <li>{@link #label_greedy1_4(int)}
    * <li>{@link #label_greedy5_8(int)}
    * <li>{@link #label_greedySlider(int)}
    * </ol>
    * for each priority level separately. Returns the number of overall successfully labeled point
    * features.
    *
    * @return the number of labels that were successfully placed
    */
   public int label_StandardPipelineAdjacentAll() {

      int labels = 0;

      for (int i = 0; i < _numPriorityLevels; i++) {
         labels += label_StandardPipelineAdjacent(i);
      }
      return labels;
   }

   /**
    * Labels all loaded point-features in a greedy manner according to their priority.
    * Excecution is equal to the sequence<br>
    * <ol>
    * <li>{@link #label_greedy1_4(int)}
    * <li>{@link #label_greedy5_8(int)}
    * <li>{@link #label_greedySlider(int)}
    * <li>{@link #label_greedySpiral(int)}
    * </ol>
    * for each priority level separately. Returns the number of overall successfully labeled point
    * features.
    *
    * @return the number of labels that were successfully placed
    */
   public int label_StandardPipelineAll() {

      int labels = 0;

      for (int i = 0; i < _numPriorityLevels; i++) {
         labels += label_StandardPipeline(i);
      }

      return labels;
   }

   /**
    * Sets up the labeler by providing the point-features to label and
    * the area bounds, the labels are to be placed in. Labels are given in
    * a vector of vectors, where the first vector holds the most important
    * labels etc. Labeling is done in order of entrys within each vector.
    *
    * @param pointfeatures
    *           the point-features to be labeled. If pointfeatures is
    *           null, no labeling happens at all.
    * @param labelAreaL
    *           the left boundary of the labeling area
    * @param labelAreaR
    *           the right boundary of the labeling area
    * @param labelAreaT
    *           the top boundary of the labeling area
    * @param labelAreaB
    *           the bottom boundary of the labeling area
    *
    * @see #loadDataPriority(java.util.Vector, int, int, int, int, float, float, float, float)
    * @see #loadDataStandard(java.util.Vector, int, int, int, int)
    * @see #loadDataStandard(java.util.Vector, int, int, int, int, float, float, float, float)
    */
   public void loadDataPriority(final List<List<PointFeature>> pointfeatures,

                                final int labelAreaL,
                                final int labelAreaR,
                                final int labelAreaT,
                                final int labelAreaB) {

      loadDataPriority(

            pointfeatures,

            labelAreaL,
            labelAreaR,
            labelAreaT,
            labelAreaB,

            0,
            0,
            0,
            0);
   }

   /**
    * Sets up the labeler by providing the point-features to label and
    * the area bounds, the labels are to be placed in. Labels are given in
    * a vector of vectors, where the first vector holds the most important
    * labels etc. Labeling is done in order of entrys within each vector.
    * The minimum and maximum label dimensions are given and are not computed within initialization.
    *
    * @param pointfeatures
    *           the point-features to be labeled. If pointfeatures is
    *           null, no labeling happens at all.
    * @param labelAreaL
    *           the left boundary of the labeling area
    * @param labelAreaR
    *           the right boundary of the labeling area
    * @param labelAreaT
    *           the top boundary of the labeling area
    * @param labelAreaB
    *           the bottom boundary of the labeling area
    * @param minLabelWidth
    *           the global minimum label width
    * @param minLabelHeight
    *           the global minimum label height
    * @param maxLabelWidth
    *           the global maximum label width
    * @param maxLabelHeight
    *           the global maximum label height
    *
    * @see #loadDataPriority(java.util.Vector, int, int, int, int)
    * @see #loadDataStandard(java.util.Vector, int, int, int, int)
    * @see #loadDataStandard(java.util.Vector, int, int, int, int, float, float, float, float)
    */
   public void loadDataPriority(final List<List<PointFeature>> pointfeatures,

                                final int labelAreaL,
                                final int labelAreaR,
                                final int labelAreaT,
                                final int labelAreaB,

                                final float minLabelWidth,
                                final float minLabelHeight,
                                final float maxLabelWidth,
                                final float maxLabelHeight) {

      _isDataLoaded = false;

      if (pointfeatures == null) {
         return;
      }

      _pointFeatures = pointfeatures;

      if (minLabelWidth <= 0 || minLabelHeight <= 0 || maxLabelWidth <= 0 || maxLabelHeight <= 0) {

         findMinMaxLabelDimensions(pointfeatures);

      } else {

         this.maxLabelW = Math.max(maxLabelWidth, minLabelWidth);
         this.minLabelW = Math.min(maxLabelWidth, minLabelWidth);
         this.maxLabelH = Math.max(maxLabelHeight, minLabelHeight);
         this.minLabelH = Math.min(maxLabelHeight, minLabelHeight);
      }

      if (maxLabelW > 0 &&
            maxLabelH > 0 &&
            minLabelW > 0 &&
            minLabelH > 0) {

         final int labelAreaWidth = Math.abs(labelAreaR - labelAreaL);
         final int labelAreaHeight = Math.abs(labelAreaB - labelAreaT);

         if ((labelAreaWidth != laW) || (labelAreaHeight != laH) || (labelAreaL != laL) || (labelAreaT != laT) || (_particleStorage == null)) {

            this.laW = labelAreaWidth;
            this.laH = labelAreaHeight;
            this.laL = Math.min(labelAreaL, labelAreaR);
            this.laR = this.laL + this.laW;
            this.laT = Math.min(labelAreaT, labelAreaB);
            this.laB = this.laT + this.laH;

            _particleStorage = new ParticleStore(

                  this.laW,
                  this.laH,

                  (int) this.maxLabelW,
                  (int) this.maxLabelH);

         } else {

            _particleStorage.reset();

         }

         _numPriorityLevels = _pointFeatures.size();

         float px, py;

         for (final List<PointFeature> priorityLevel : _pointFeatures) {
            for (final PointFeature p : priorityLevel) {

               px = p.getX();
               py = p.getY();

               if (px >= this.laL && px <= this.laR && py >= this.laT && py <= this.laB) {

                  p.isEnabledForLabeling = true;

                  //Generating a corresponding particle and keeping its id
                  p.particleId = _particleStorage.addParticle(px, py);

               } else {

                  p.isEnabledForLabeling = false;
               }
            }
         }

         // Results till now:
         // PointFeatures in 2DIM "array"
         // Corresponding particles saved to grid
         // Link between pointfeatures and particles set

         _isDataLoaded = true;
      }
   }

   /**
    * Sets up the labeler by providing the point-features to label and
    * the area bounds, the labels are to be placed in. Labels are given in
    * a single vector. Labeling is done in order of entrys within this vector.
    *
    * @param pointfeatures
    *           the point-features to be labeled. If pointfeatures is
    *           null, no labeling happens at all.
    * @param labelAreaL
    *           the left boundary of the labeling area
    * @param labelAreaR
    *           the right boundary of the labeling area
    * @param labelAreaT
    *           the top boundary of the labeling area
    * @param labelAreaB
    *           the bottom boundary of the labeling area
    *
    * @see #loadDataPriority(java.util.Vector, int, int, int, int)
    * @see #loadDataPriority(java.util.Vector, int, int, int, int, float, float, float, float)
    * @see #loadDataStandard(java.util.Vector, int, int, int, int, float, float, float, float)
    */
   public void loadDataStandard(final List<PointFeature> allPointfeatures,
                                final int labelAreaL,
                                final int labelAreaR,
                                final int labelAreaT,
                                final int labelAreaB) {

      if (allPointfeatures != null) {

         final List<List<PointFeature>> nLabels = new ArrayList<>();

         nLabels.add(allPointfeatures);

         loadDataPriority(nLabels, labelAreaL, labelAreaR, labelAreaT, labelAreaB, 0, 0, 0, 0);
      }
   }

   /**
    * Sets up the labeler by providing the point-features to label and
    * the area bounds, the labels are to be placed in. Labels are given in
    * a single vector. Labeling is done in order of entrys within this vector.
    * The minimum and maximum label dimensions are given and are not computed within initialization.
    *
    * @param pointfeatures
    *           the point-features to be labeled. If pointfeatures is
    *           null, no labeling happens at all.
    * @param labelAreaL
    *           the left boundary of the labeling area
    * @param labelAreaR
    *           the right boundary of the labeling area
    * @param labelAreaT
    *           the top boundary of the labeling area
    * @param labelAreaB
    *           the bottom boundary of the labeling area
    * @param minLabelWidth
    *           the global minimum label width
    * @param minLabelHeight
    *           the global minimum label height
    * @param maxLabelWidth
    *           the global maximum label width
    * @param maxLabelHeight
    *           the global maximum label height
    *
    * @see #loadDataPriority(java.util.Vector, int, int, int, int)
    * @see #loadDataPriority(java.util.Vector, int, int, int, int, float, float, float, float)
    * @see #loadDataStandard(java.util.Vector, int, int, int, int)
    */
   public void loadDataStandard(final List<PointFeature> pointfeatures,

                                final int labelAreaL,
                                final int labelAreaR,
                                final int labelAreaT,
                                final int labelAreaB,

                                final float minLabelWidth,
                                final float minLabelHeight,
                                final float maxLabelWidth,
                                final float maxLabelHeight) {

      if (pointfeatures != null) {

         final List<List<PointFeature>> nLabels = new ArrayList<>();

         nLabels.add(pointfeatures);

         loadDataPriority(

               nLabels,

               labelAreaL,
               labelAreaR,
               labelAreaT,
               labelAreaB,

               minLabelWidth,
               minLabelHeight,
               maxLabelWidth,
               maxLabelHeight);
      }

   }

   /**
    * Converts a filled box into conflict particles. These are
    * regarded during labeling.
    *
    * @param x
    *           left coordinate of the box
    * @param y
    *           top coordinate of the box
    * @param w
    *           width of the box
    * @param h
    *           height of the box
    */
   public void respectBox(final float x, final float y, final float w, final float h) {

      if (_isDataLoaded) {

         respectBoxIntern(x, x + w, y, y + h);
      }
   }

   /**
    * Converts a filled box into conflict particles. These are
    * regarded during labeling.
    *
    * @param l
    *           left coordinate of the box
    * @param r
    *           right coordinate of the box
    * @param t
    *           top coordinate of the box
    * @param b
    *           bottom coordinate of the box
    */
   private void respectBoxIntern(final float l, final float r, final float t, final float b) {

      // DATA HAS TO BE LOADED !!! -> ELSE NULL_POINTER EXCEPTION
      // CHECK BEFORE CALLING !

      gl_rB_newParticleX = l;

      while (gl_rB_newParticleX < r) {
         gl_rB_newParticleY = t;
         while (gl_rB_newParticleY < b) {
            _particleStorage.addParticle(gl_rB_newParticleX, gl_rB_newParticleY);
            gl_rB_newParticleY += this.minLabelH;
         }
         gl_rB_newParticleX += this.minLabelW;
      }

      gl_rB_newParticleX = r;
      gl_rB_newParticleY = t;

      while (gl_rB_newParticleY < b) {
         _particleStorage.addParticle(gl_rB_newParticleX, gl_rB_newParticleY);
         gl_rB_newParticleY += this.minLabelH;
      }

      gl_rB_newParticleX = l;
      gl_rB_newParticleY = b;

      while (gl_rB_newParticleX < r) {
         _particleStorage.addParticle(gl_rB_newParticleX, gl_rB_newParticleY);
         gl_rB_newParticleX += this.minLabelW;
      }

      _particleStorage.addParticle(r, b);

   }

   /**
    * Converts a filled circle into conflict particles. These are
    * regarded during labeling.
    *
    * @param centerX
    *           x-coordinate of center point
    * @param centerY
    *           y-coordinate of center point
    * @param circleRadius
    *           radius of the circle
    */
   public void respectCircle(final float centerX, final float centerY, final float circleRadius) {

      if (_isDataLoaded == false) {
         return;
      }

      gl_rC_newParticleX = 0;
      gl_rC_newParticleY = 0;
      gl_rC_radiusStepWidth = Math.min(minLabelH, minLabelW);
      gl_rC_angleDelta = 90f;
      gl_rC_usedAngleDelta = gl_rC_angleDelta;
      gl_rC_sRadiusIndex = 0;
      gl_rC_eRadiusIndex = (int) Math.floor(circleRadius / gl_rC_radiusStepWidth);
      gl_rC_startAngle = 0;
      gl_rC_stopAngle = 360;
      gl_rC_newRound = true;

      //adding the center-particle
      _particleStorage.addParticle(centerX, centerY);

      //adding the border-particles
      //angle delta (0.5d) could be calculated to hit each pixel only once !!!
      for (gl_rC_angle = 0; gl_rC_angle < 360; gl_rC_angle += 0.5d) {

         _particleStorage.addParticle(
               centerX + _cos[(int) (gl_rC_angle * _angleSubDivision)] * circleRadius,
               centerY + _sin[(int) (gl_rC_angle * _angleSubDivision)] * circleRadius);
      }

      //Filling the circle
      while (gl_rC_sRadiusIndex < gl_rC_eRadiusIndex) {

         //Length of an secant depends on angleDelta
         gl_rC_secantLength = 2 * gl_rC_sRadiusIndex * gl_rC_radiusStepWidth * _sin[(int) (gl_rC_angleDelta / 2 * _angleSubDivision)];

         //if secant larger than min labeldimension, reduce angleDelta
         while (gl_rC_secantLength > gl_rC_radiusStepWidth) {
            gl_rC_angleDelta /= 2;
            gl_rC_secantLength = 2 * gl_rC_sRadiusIndex * gl_rC_radiusStepWidth * _sin[(int) (gl_rC_angleDelta / 2 * _angleSubDivision)];
            gl_rC_newRound = true;
         }

         //method sends beams from center to border to avoid multiple calc. of sin and cos
         //needs only additions

         //if new beams necessary (due to subdivision of deltaAngle)
         if (gl_rC_newRound) {

            //rotating the starting beam and calculating the angle delta to avoid
            //generating particles at same positions
            gl_rC_usedAngleDelta = gl_rC_angleDelta;

            if (gl_rC_sRadiusIndex != 0) {
               if (gl_rC_sRadiusIndex == 1) {
                  gl_rC_usedAngleDelta = gl_rC_angleDelta + gl_rC_angleDelta;
               }
               gl_rC_startAngle += gl_rC_angleDelta;
               gl_rC_stopAngle = 360 + gl_rC_startAngle;
            }

            //iterating the beams...
            for (gl_rC_angle = gl_rC_startAngle; gl_rC_angle < gl_rC_stopAngle; gl_rC_angle += gl_rC_usedAngleDelta) {

               gl_rC_usedAngle = gl_rC_angle % 360;

               //getting cos and sin only once per beam
               gl_rC_deltaX = _cos[(int) (gl_rC_usedAngle * _angleSubDivision)] * gl_rC_radiusStepWidth;
               gl_rC_deltaY = _sin[(int) (gl_rC_usedAngle * _angleSubDivision)] * gl_rC_radiusStepWidth;

               //placing the first particle of beam
               gl_rC_newParticleX = centerX + gl_rC_sRadiusIndex * gl_rC_deltaX;
               gl_rC_newParticleY = centerY + gl_rC_sRadiusIndex * gl_rC_deltaY;
               _particleStorage.addParticle(gl_rC_newParticleX, gl_rC_newParticleY);

               //iterating the beam from center to border
               for (gl_rC_i = gl_rC_sRadiusIndex; gl_rC_i < gl_rC_eRadiusIndex; gl_rC_i++) {

                  gl_rC_newParticleX += gl_rC_deltaX;
                  gl_rC_newParticleY += gl_rC_deltaY;

                  _particleStorage.addParticle(gl_rC_newParticleX, gl_rC_newParticleY);
               }
            }
            gl_rC_newRound = false;
         }
         gl_rC_sRadiusIndex++;
      }
   }

   /**
    * Converts a line into conflict particles. Every
    * pixel along the line generates a conflict particle that is
    * regarded during labeling.
    *
    * @param x1
    *           x-coordinate of point 1
    * @param y1
    *           y-coordinate of point 1
    * @param x2
    *           x-coordinate of point 2
    * @param y2
    *           y-coordinate of point 2
    *
    * @see #respectLine(float, float, float, float, float)
    */
   public void respectLine(final float x1, final float y1, final float x2, final float y2) {
      respectLine(x1, y1, x2, y2, 1f);
   }

   /**
    * Converts a line into conflict particles. Every resolution<sup>th</sup>
    * pixel along the line generates a conflict particle that is
    * regarded during labeling.
    *
    * @param x1
    *           x-coordinate of point 1
    * @param y1
    *           y-coordinate of point 1
    * @param x2
    *           x-coordinate of point 2
    * @param y2
    *           y-coordinate of point 2
    * @param resolution
    *           the maximum distance of particles along the line
    *
    * @see #respectLine(float, float, float, float)
    */
   public void respectLine(final float x1, final float y1, final float x2, final float y2, final float resolution) {
      if (_isDataLoaded) {
         gl_rL_i = 0;
         gl_rL_x = x1;
         gl_rL_y = y1;
         gl_rL_dx = x2 - x1;
         gl_rL_dy = y2 - y1;
         gl_rL_length = (float) Math.ceil(Math.sqrt(gl_rL_dx * gl_rL_dx + gl_rL_dy * gl_rL_dy));
         gl_rL_delta = (1f / gl_rL_length) * Math.max(resolution, 0.0001f);

         gl_rL_dx *= gl_rL_delta;
         gl_rL_dy *= gl_rL_delta;

         while (gl_rL_i++ < gl_rL_length) {
            _particleStorage.addParticle(gl_rL_x, gl_rL_y);
            gl_rL_x += gl_rL_dx;
            gl_rL_y += gl_rL_dy;
         }
      }
   }

   /**
    * Converts a map that is given by a <code>BufferedImage</code> into conflict particles. See
    * {@link #respectMap(BufferedImage map, Color emptySpaceColor, int resolution)}
    * with <code>emptySpaceColor = Color.white</code> and <code>resolution</code> = 1.
    *
    * @param map
    *           the map to be converted given as an <code>BufferedImage</code>
    *
    * @see #respectMap(java.awt.image.BufferedImage, java.awt.Color, int)
    */
   public void respectMap(final BufferedImage map) {

      respectMap(map, Color.white, 1);
   }

   /**
    * Converts a map that is given by a <code>BufferedImage</code> into conflict particles. See
    * {@link #respectMap(BufferedImage map, Color emptySpaceColor, int resolution)}
    * with <code>resolution</code> = 1.
    *
    * @param map
    *           the map to be converted given as a <code>BufferedImage</code>
    * @param emptySpaceColor
    *           the color that defines the empty space
    *
    * @see #respectMap(java.awt.image.BufferedImage, java.awt.Color, int)
    */
   public void respectMap(final BufferedImage map, final Color emptySpaceColor) {

      respectMap(map, emptySpaceColor, 1);
   }

   /**
    * Converts a map that is given by a <code>BufferedImage</code> into conflict particles. Every
    * resolution<sup>th</sup>
    * pixel (horizontal and vertical) that is not equal to emptySpaceColor generates a conflict
    * particle that is
    * regarded during labeling.
    *
    * @param map
    *           the map to be converted given as a <code>BufferedImage</code>
    * @param emptySpaceColor
    *           the color that defines the empty space
    * @param resolution
    *           the maximum distance of particles within the map
    */
   public void respectMap(final BufferedImage map, final Color emptySpaceColor, final int resolution) {

      if (_isDataLoaded) {

         gl_rM_maxX = Math.min(this.laW, map.getWidth());
         gl_rM_maxY = Math.min(this.laH, map.getHeight());

         gl_rM_color = emptySpaceColor.getRGB();

         for (gl_rM_y = 0; gl_rM_y < gl_rM_maxY; gl_rM_y += resolution) {
            for (gl_rM_x = 0; gl_rM_x < gl_rM_maxX; gl_rM_x += resolution) {

               if (map.getRGB(gl_rM_x, gl_rM_y) != gl_rM_color) {

                  _particleStorage.addParticle(gl_rM_x, gl_rM_y);
               }
            }
         }
      }
   }

   /**
    * Converts a list of 2D-points into conflict particles. These are
    * regarded during labeling.
    *
    * @param pts
    *           a two dimensional array, containing the x-coordinate at [pt][0] and the y-coordinate
    *           at [pt][1]
    */
   public void respectPoints(final float[][] pts) {

      gl_rP_length = pts.length;

      for (gl_rP_i = 0; gl_rP_i < gl_rP_length; gl_rP_i++) {

         _particleStorage.addParticle(pts[gl_rP_i][0], pts[gl_rP_i][1]);
      }
   }

}
