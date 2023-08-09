/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

public enum ChartType {

   BAR,

   DOT,

   HISTORY,

   HORIZONTAL_BAR,

   LINE,

   LINE_WITH_BARS,

   XY_SCATTER,

   /**
    * R-R intervals can contain multiple values (>100) in one time slice -> x-axis must be
    * calculated differently
    */
   VARIABLE_X_AXIS,

   /**
    *
    */
   VARIABLE_X_AXIS_WITH_2ND_LINE,

   /**
    * Show a symbol at the x/y position
    */
   SYMBOL,

}
