/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

import net.tourbook.common.UI;
import net.tourbook.data.TourLocation;
import net.tourbook.map.location.LocationType;

/**
 * Wrapper for a {@link TourLocation} with additional data
 */
public class TourLocationExtended {

   private static final char NL = UI.NEW_LINE;

   public TourLocation       tourLocation;

   public LocationType       locationType;

   @SuppressWarnings("unused")
   private TourLocationExtended() {}

   public TourLocationExtended(final TourLocation tourLocation, final LocationType locationType) {

      this.tourLocation = tourLocation;
      this.locationType = locationType;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourLocationExtended" + NL //                      //$NON-NLS-1$

            + " tourLocation   = " + NL + tourLocation + NL //    //$NON-NLS-1$
            + " locationType   = " + NL + locationType + NL //    //$NON-NLS-1$
      ;
   }
}
