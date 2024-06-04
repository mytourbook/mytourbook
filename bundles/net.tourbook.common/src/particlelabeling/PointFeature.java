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

/**
 * <p>
 * This class encapsulates all information, that are necessary for the particle-based labeling
 * approach. Moreover, the labeling results (final position of the labeling box) and the labeling
 * state are stored within the point-feature.
 * </p>
 * <p>
 * As labeling speed is most important, internal variables are made public to avoid 'slow' Getter-
 * and Setter-methods. Hence, those variables should not be manipulated or read during labeling, due
 * to changes caused by the labeling.
 * </p>
 * <p>
 * Note that labelwidth and -height of all point-features have to be set before labling starts,
 * since the labling depends on the minimum labelwidh and -height - their values have to be bigger
 * than 0 !!!
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
public class PointFeature {

   /**
    * ID of the particle stored in {@link ParticleStore}, representing this point-feature.
    */
   public int      particleId = -1;

   /**
    * Width of the corresponding label box. Should be set to values >0 before labeling, as
    * zero widths or heights are blocking the labeling.
    */
   public float    labelBoxW;

   /**
    * Height of the corresponding label box. Should be set to values >0 before labeling, as
    * zero widths or heights are blocking the labeling.
    */
   public float    labelBoxH;

   /**
    * Left coordinate of the finally placed corresponding label box. Is set during labeling by
    * {@link PointFeatureLabeler}.
    */
   public float    labelBoxL;

   /**
    * Bottom coordinate of the finally placed corresponding label box. Is set during labeling by
    * {@link PointFeatureLabeler}.
    */
   public float    labelBoxB;

   /**
    * Top coordinate of the finally placed corresponding label box. Is set during labeling by
    * {@link PointFeatureLabeler}.
    */
   public float    labelBoxT;

   /**
    * Right coordinate of the finally placed corresponding label box. Is set during labeling by
    * {@link PointFeatureLabeler}.
    */
   public float    labelBoxR;

   /**
    * Width of a box enclosing this point-feature if the point-feature is not of "zero"-size.
    * Is considered during labeling by {@link PointFeatureLabeler} to calculate candidate label
    * positions.
    */
   public float    rectangularExtendW;

   /**
    * Height of a box enclosing this point-feature if the point-feature is not of "zero"-size.
    * Is considered during labeling by {@link PointFeatureLabeler} to calculate candidate label
    * positions.
    */
   public float    rectangularExtendH;

   /**
    * Radius of a circle enclosing this point-feature if the point-feature is not of "zero"-size.
    * Is (currently not) considered during labeling by {@link PointFeatureLabeler} to calculate
    * candidate label positions.
    */
   public float    radialExtendRadius;

   /**
    * Label of this point-feature.
    */
   public String   label;

   /**
    * Green light of the point-feature to be labeled by {@link PointFeatureLabeler}. For that it has
    * to be inside the labeling area defined in {@link PointFeatureLabeler}.
    */
   public boolean  isEnabledForLabeling;

   /**
    * Tells if the point-feature is successfully labeled by {@link PointFeatureLabeler}.
    */
   public boolean  isLabeled;

   /**
    * Tells if the point-feature is labeled by the first pipeline step of the
    * {@link PointFeatureLabeler}.
    */
   public boolean  isLabeled_greedy1_4;

   /**
    * Tells if the point-feature is labeled by the second pipeline step of the
    * {@link PointFeatureLabeler}.
    */
   public boolean  isLabeled_greedy5_8;

   /**
    * Tells if the point-feature is labeled by the third pipeline step of the
    * {@link PointFeatureLabeler}.
    */
   public boolean  isLabeled_greedySlider;

   /**
    * Tells if the point-feature is labeled by the fourth pipeline step of the
    * {@link PointFeatureLabeler}.
    */
   public boolean  isLabeled_greedySpiral;

   private float[] pos;
   private int     id;

   /**
    * Custom data to store anything
    */
   public Object   data;

   /**
    * Generates a new point-feature, defined by its label, id and position. The label-box dimensions
    * and
    * the graphical rectangular extend are 0. The label-box dimension have to be set separately
    * before labeling!
    *
    * @param label
    *           the label of the point-feature, can be null.
    * @param id
    *           the id of the point-feature to be used in applications, not used during labeling.
    * @param xPos
    *           the x-coordinate of this point-feature.
    * @param yPos
    *           the y-coordinate of this point-feature.
    *
    * @see #PointFeature(java.lang.String, int, float, float, float, float, float, float)
    */
   public PointFeature(final String label,
                       final int id,

                       final float xPos,
                       final float yPos) {

      this(label,
            id,

            xPos,
            yPos,

            0,
            0,
            0,
            0);
   }

   /**
    * Generates a new point-feature, defined by its label, id, position and label-dimensions. The
    * graphical rectangular extend is 0.
    *
    * @param label
    *           the label of the point-feature, can be null.
    * @param id
    *           the id of the point-feature to be used in applications, not used during labeling.
    * @param xPos
    *           the x-coordinate of this point-feature.
    * @param yPos
    *           the y-coordinate of this point-feature.
    * @param labelwidth
    *           the width of the corresponding labelbox to be placed, should be > 0.
    * @param labelheight
    *           the height of the corresponding labelbox to be placed, should be > 0.
    *
    * @see #PointFeature(java.lang.String, int, float, float, float, float, float, float)
    */
   public PointFeature(final String label,
                       final int id,
                       final float xPos,
                       final float yPos,
                       final float labelwidth,
                       final float labelheight) {

      this(label,
            id,

            xPos,
            yPos,

            labelwidth,
            labelheight,

            0,
            0);
   }

   /**
    * Generates a new point-feature, defined by its label, id, position, label-dimensions and
    * graphical rectangular extend.
    *
    * @param label
    *           the label of the point-feature, can be null.
    * @param id
    *           the id of the point-feature to be used in applications, not used during labeling.
    * @param xPos
    *           the x-coordinate of this point-feature.
    * @param yPos
    *           the y-coordinate of this point-feature.
    * @param labelwidth
    *           the width of the corresponding labelbox to be placed, should be > 0.
    * @param labelheight
    *           the height of the corresponding labelbox to be placed, should be > 0.
    * @param hExtend
    *           the horizontal graphical extend of this point-feature, can be 0.
    * @param vExtend
    *           the vertical graphical extend of this point-feature, can be 0.
    */
   public PointFeature(final String label,
                       final int id,

                       final float xPos,
                       final float yPos,

                       final float labelwidth,
                       final float labelheight,

                       final float hExtend,
                       final float vExtend) {

      this.label = label;
      this.id = id;

      this.pos = new float[] { xPos, yPos };

      this.labelBoxW = labelwidth;
      this.labelBoxH = labelheight;

      this.rectangularExtendW = hExtend;
      this.rectangularExtendH = vExtend;

      this.radialExtendRadius = Math.max(vExtend, hExtend);
   }

   /**
    * Returns the id of this point-feature.
    *
    * @return the id.
    */
   public int getID() {
      return this.id;
   }

   /**
    * Returns the x-coordinate of this point-feature.
    *
    * @return the x-coordinate.
    */
   public float getX() {
      return this.pos[0];
   }

   /**
    * Returns the y-coordinate of this point-feature.
    *
    * @return the y-coordinate.
    */
   public float getY() {
      return this.pos[1];
   }

}
