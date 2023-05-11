/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard and Contributors
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
package net.tourbook.export.fit;

import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;

import net.tourbook.common.UI;
import net.tourbook.data.TourType;

public class FitSportMapper {

   private FitSportMapper() {}

   public static Sport mapTourTypeToSport(final TourType tourType) {

      Sport sport = Sport.GENERIC;

      if (tourType == null) {
         return sport;
      }

      final String tourTypeName = tourType.getName().trim();

      try {

         sport = Sport.valueOf(tourTypeName.toUpperCase().replace(UI.SPACE, UI.SYMBOL_UNDERSCORE.charAt(0)));
         return sport;

      } catch (IllegalArgumentException | NullPointerException e) {
         //ignore
      }

      switch (tourTypeName.toLowerCase()) {

      case "trainer": //$NON-NLS-1$
         sport = Sport.CYCLING;
         break;

      case "trail": //$NON-NLS-1$
         sport = Sport.RUNNING;
         break;

      case "snow-shoeing": //$NON-NLS-1$
         sport = Sport.SNOWSHOEING;
         break;

      case "skating": //$NON-NLS-1$
      case "cross-country": //$NON-NLS-1$
      case "skate-skiing": //$NON-NLS-1$

         sport = Sport.CROSS_COUNTRY_SKIING;
         break;

      default:
         sport = Sport.GENERIC;
         break;
      }

      return sport;
   }

   public static SubSport mapTourTypeToSubSport(final TourType tourType) {

      SubSport subSport = SubSport.GENERIC;

      if (tourType == null) {
         return subSport;
      }

      final String tourTypeName = tourType.getName().trim();

      try {

         subSport = SubSport.valueOf(tourTypeName.toUpperCase().replace(UI.SPACE, UI.SYMBOL_UNDERSCORE.charAt(0)));
         return subSport;

      } catch (IllegalArgumentException | NullPointerException e) {
         //ignore
      }

      switch (tourTypeName.toLowerCase()) {

      case "skating": //$NON-NLS-1$
         subSport = SubSport.SKATE_SKIING;
         break;

      case "trainer": //$NON-NLS-1$
         subSport = SubSport.VIRTUAL_ACTIVITY;
         break;

      default:
         subSport = SubSport.GENERIC;
         break;
      }

      return subSport;
   }
}
