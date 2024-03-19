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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.data.TourLocation;
import net.tourbook.map.location.SlideoutMapLocation;

import org.eclipse.ui.PlatformUI;

/**
 * Manage MapLocation's
 */
public class MapLocationManager {

   private static final char               NL                        = UI.NEW_LINE;

   private static final List<TourLocation> _allMapLocations          = new ArrayList<>();

   private static SlideoutMapLocation      _mapLocationSlideout;

   private static int                      _locationRequestZoomlevel = TourLocationManager.DEFAULT_ZOOM_LEVEL_VALUE;

   public static void addLocation(final TourLocation tourLocation) {

      // update model
      _allMapLocations.add(tourLocation);

      // update UI
      if (_mapLocationSlideout != null) {

         _mapLocationSlideout.open(false);

         // delay to be sure that the slideout is opened
         PlatformUI.getWorkbench().getDisplay().asyncExec(() -> _mapLocationSlideout.updateUI(tourLocation));
      }
   }

   public static int getLocationRequestZoomlevel() {
      return _locationRequestZoomlevel;
   }

   public static List<TourLocation> getMapLocations() {
      return _allMapLocations;
   }

   public static void setLocationRequestZoomlevel(final int locationRequestZoomlevel) {

      _locationRequestZoomlevel = locationRequestZoomlevel;
   }

   public static void setMapLocationSlideout(final SlideoutMapLocation mapLocationSlideout) {

      _mapLocationSlideout = mapLocationSlideout;
   }
}
