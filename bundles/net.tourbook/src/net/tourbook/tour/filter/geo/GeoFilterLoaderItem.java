/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter.geo;

import de.byteholder.geoclipse.map.GridBoxItem;

import org.eclipse.swt.graphics.Point;

/**
 * Contains all data for a tour comparison
 */
public class GeoFilterLoaderItem {

   long               executorId;

   /**
    * When <code>true</code> then the loading/comparing of tours in this loader is canceled.
    */
   boolean            isCanceled;

   /**
    * /** Time in ms to calculate sql data
    */
   long               sqlRunningTime;

   /**
    *
    */
   boolean            isReselectedInUI;

   public Point       topLeftE2;
   public Point       bottomRightE2;

   public GridBoxItem mapGridBoxItem;

   @SuppressWarnings("unused")
   private GeoFilterLoaderItem() {}

   public GeoFilterLoaderItem(final long executorId) {

      this.executorId = executorId;
   }

   @Override
   public String toString() {
      return "GeoPartItem [" //$NON-NLS-1$
            + "executorId=" + executorId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            //				+ "geoParts=" + Arrays.toString(geoParts) + ", "
            //				+ "latPartSerie5=" + Arrays.toString(latPartSerie5) + ", "
            //				+ "lonPartSerie5=" + Arrays.toString(lonPartSerie5) + ", tourIds=" + Arrays.toString(tourIds) + ", "
            //				+ "isUseAppFilter=" + isUseAppFilter + ", "
            //				+ "sqlRunningTime=" + sqlRunningTime
            + "]"; //$NON-NLS-1$
   }

}
