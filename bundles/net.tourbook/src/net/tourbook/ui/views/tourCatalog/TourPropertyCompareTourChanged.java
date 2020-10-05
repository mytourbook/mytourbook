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
package net.tourbook.ui.views.tourCatalog;

public class TourPropertyCompareTourChanged {

   long    compareId;

   int     startIndex;
   int     endIndex;

   float   avgPulse;
   float   speed;
   int     tourDeviceTime_Elapsed;

   boolean isDataSaved;

   Object  comparedTourItem;

   public TourPropertyCompareTourChanged(final long compareId,
                                         final int startIndex,
                                         final int endIndex,
                                         final boolean isDataSaved,
                                         final Object comparedTourItem) {

      this.compareId = compareId;

      this.startIndex = startIndex;
      this.endIndex = endIndex;

      this.isDataSaved = isDataSaved;

      this.comparedTourItem = comparedTourItem;
   }

}
