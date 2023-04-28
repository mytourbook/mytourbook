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

import net.tourbook.data.TourType;

public class FitSportMapper {

   public static Sport mapTourTypeToSport(final TourType tourType) {

      Sport sport = Sport.GENERIC;

      if (tourType == null) {
         return sport;
      }

      final String tourTypeName = tourType.getName();

      switch (tourTypeName.toLowerCase().trim()) {

      case "cycling": //$NON-NLS-1$
      case "trainer": //$NON-NLS-1$
         sport = Sport.CYCLING;
         break;

      case "hiking": //$NON-NLS-1$
         sport = Sport.HIKING;
         break;

      case "running": //$NON-NLS-1$
      case "trail": //$NON-NLS-1$
         sport = Sport.RUNNING;
         break;

      case "snowshoeing": //$NON-NLS-1$
         sport = Sport.SNOWSHOEING;
         break;

      case "skating": //$NON-NLS-1$
      case "cross-country": //$NON-NLS-1$
      case "skate-skiing": //$NON-NLS-1$

         sport = Sport.CROSS_COUNTRY_SKIING;
         break;

      case "walking": //$NON-NLS-1$
         sport = Sport.WALKING;
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

      final String tourTypeName = tourType.getName();

      switch (tourTypeName.toLowerCase().trim()) {

      case "skating": //$NON-NLS-1$
         subSport = SubSport.SKATE_SKIING;
         break;

      case "trail": //$NON-NLS-1$
         subSport = SubSport.TRAIL;
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
