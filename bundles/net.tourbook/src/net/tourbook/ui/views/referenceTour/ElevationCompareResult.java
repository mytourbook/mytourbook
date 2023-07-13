/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;

public class ElevationCompareResult {

   /**
    * Id for a {@link TourCompared}
    */
   public long compareId;

   /**
    * Elevation comparison is done with this {@link TourData}
    */
   public long tourId;

   /**
    * Elevation comparison is done against this {@link TourReference}
    */
   public long refTourId;

   @SuppressWarnings("unused")
   private ElevationCompareResult() {}

   public ElevationCompareResult(final long compareId, final long tourId, final long refTourId) {

      this.compareId = compareId;
      this.tourId = tourId;
      this.refTourId = refTourId;
   }

}
