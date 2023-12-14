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

import org.eclipse.jface.action.ContributionItem;

public interface ITourLocationConsumer {

   /**
    * Close other slideouts, ensure that only one slideout is open
    *
    * @param contribItem_DownloadTourLocation
    */
   public void closeOtherSlideouts(ContributionItem requestForOpeningContribItem);

   /**
    * Is called when a new profile is set with {@link TourLocationManager#setDefaultProfile()}
    */
   public void defaultProfileIsUpdated();

   /**
    * Retrieve tour end location and sets the location value
    */
   public void downloadAndSetTourEndLocation();

   /**
    * Retrieve tour start location and sets the location value
    */
   public void downloadAndSetTourStartLocation();

   /**
    * Set the tour end location value
    *
    * @param endLocation
    */
   public void setTourEndLocation(String endLocationLabel);

   /**
    * Set the tour start location value
    *
    * @param startLocation
    */
   public void setTourStartLocation(String startLocationLabel);
}
