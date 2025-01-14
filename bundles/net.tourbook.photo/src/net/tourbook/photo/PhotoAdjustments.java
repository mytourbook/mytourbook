/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

/**
 * Adjustments for a photo, e.g. cropping, tonality...
 */
public class PhotoAdjustments {

   public boolean   isPhotoCropped;

   /**
    * Relative position 0...1 of the crop area top left x position
    */
   public float     cropAreaX1;
   public float     cropAreaY1;

   public float     cropAreaX2;
   public float     cropAreaY2;

   public boolean   isSetTonality;

   public CurveType curveType         = CurveType.THREE_POINTS;

   public int       threePoint_Dark   = 0;
   public int       threePoint_Bright = 255;

   /**
    * Middle is the relative position between dark and bright, it is between 0...100
    */
   public float     threePoint_Middle = 50.0f;

}
