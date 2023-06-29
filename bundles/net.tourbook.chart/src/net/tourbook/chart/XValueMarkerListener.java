/*******************************************************************************
 * Copyright (C) 2007, 2023 Wolfgang Schramm and Contributors
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

public interface XValueMarkerListener {

   /**
    * @return Returns the value difference between the left and right border of the part marker
    */
   double getXValueDifference();

   /**
    * this method is called when the x-marker was move, the parameter contain the new value index
    *
    * @param movedPartMarkerStartValueIndex
    *           start index
    * @param movedPartMarkerEndValueIndex
    *           end index
    */
   void xValueMarkerIsMoved(int movedPartMarkerStartValueIndex, int movedPartMarkerEndValueIndex);
}
