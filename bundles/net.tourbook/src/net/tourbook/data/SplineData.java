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
package net.tourbook.data;

public class SplineData {

   public double[]  relativePositionX;
   public double[]  relativePositionY;

   public double[]  graphXValues;
   public double[]  graphYValues;

   public double[]  graphXMinValues;
   public double[]  graphXMaxValues;

   /**
    * Serie index for the spline point in the data serie (distance or time)
    */
   public int[]     serieIndex;

   public boolean[] isPointMovable;

   public SplineData() {}
}
