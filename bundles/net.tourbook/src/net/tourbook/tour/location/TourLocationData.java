/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import java.io.Serializable;

import net.tourbook.common.UI;
import net.tourbook.data.TourLocation;

public class TourLocationData implements Serializable {

   private static final long serialVersionUID = 1L;

   private static final char NL               = UI.NEW_LINE;

   String                    downloadedData;

   long                      downloadTime;
   long                      waitingTime;

   public TourLocation       tourLocation;

   public TourLocationData() {}

   public TourLocationData(final String downloadedData, final long retrievalTime, final long waitingTime) {

      this.downloadedData = downloadedData;

      this.downloadTime = retrievalTime;
      this.waitingTime = waitingTime;
   }

   public TourLocationData(final TourLocation tourLocation) {

      this.tourLocation = tourLocation;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourLocationData" + NL //                          //$NON-NLS-1$

            + " downloadedData = " + downloadedData + NL //       //$NON-NLS-1$
            + " downloadTime   = " + downloadTime + NL //         //$NON-NLS-1$
            + " waitingTime    = " + waitingTime + NL //          //$NON-NLS-1$

            + " tourLocation   = " + NL + tourLocation + NL //    //$NON-NLS-1$

      ;
   }
}
