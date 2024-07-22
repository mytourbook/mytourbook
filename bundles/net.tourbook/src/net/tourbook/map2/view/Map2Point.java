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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.map.TourPause;

import java.awt.Color;
import java.awt.Rectangle;

import net.tourbook.common.UI;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourMarker;
import net.tourbook.map.location.LocationType;
import net.tourbook.map25.layer.marker.algorithm.distance.ClusterItem;
import net.tourbook.photo.Photo;

import org.oscim.core.GeoPoint;

/**
 *
 */
public class Map2Point implements ClusterItem {

   private static int                        _uniqueID;

   /**
    * Unique ID
    */
   public String                             ID;

   /**
    * Depending on this type, other fields are set
    */
   public MapPointType                       pointType;

   public GeoPoint                           geoPoint;

   /**
    * Geo position in device pixel for the map point position
    */
   public int                                geoPointDevX;
   public int                                geoPointDevY;

   /**
    * Long labels are wrapped
    */
   private String                            formattedLabel;

   public int                                numDuplicates;

   public int                                numDuplicates_Start;
   public int                                numDuplicates_End;

   public TourLocation                       tourLocation;
   public TourMarker                         tourMarker;
   public TourPause                          tourPause;
   public Photo                              photo;

   /**
    * Type of the tour location {@link #tourLocation}
    */
   public LocationType                       locationType;

   public Color                              boundingBox_ColorAWT;

   public Rectangle                          boundingBoxAWT;
   public Rectangle                          boundingBox_ResizedAWT;
   public org.eclipse.swt.graphics.Rectangle boundingBoxSWT;
   public org.eclipse.swt.graphics.Rectangle boundingBox_ResizedSWT;

   @SuppressWarnings("unused")
   private Map2Point() {}

   /**
    * @param tourMarker
    * @param geoPoint
    */
   public Map2Point(final MapPointType pointType, final GeoPoint geoPoint) {

      this.pointType = pointType;
      this.geoPoint = geoPoint;

      ID = String.valueOf(_uniqueID++);
   }

   public Color getFillColorAWT() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

// SET_FORMATTING_OFF

      switch (pointType) {

      case COMMON_LOCATION:   return mapConfig.commonLocationFill_ColorAWT;

      case TOUR_LOCATION:

         if (numDuplicates_Start > 0 && numDuplicates_End == 0) {

            return mapConfig.tourLocation_StartFill_ColorAWT;

         } else if (numDuplicates_End > 0 && numDuplicates_Start == 0) {

            return mapConfig.tourLocation_EndFill_ColorAWT;

         } else {

            return mapConfig.tourLocationFill_ColorAWT;
         }

      case TOUR_PAUSE:        return mapConfig.tourPauseFill_ColorAWT;

      case TOUR_PHOTO:        return mapConfig.photoFill_ColorAWT;

      default:
      case TOUR_MARKER:       return mapConfig.tourMarkerFill_ColorAWT;

      }

// SET_FORMATTING_ON
   }

   public org.eclipse.swt.graphics.Color getFillColorSWT() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

// SET_FORMATTING_OFF

      switch (pointType) {

      case COMMON_LOCATION:   return mapConfig.commonLocationFill_ColorSWT;

      case TOUR_LOCATION:

         if (numDuplicates_Start > 0 && numDuplicates_End == 0) {

            return mapConfig.tourLocation_StartFill_ColorSWT;

         } else if (numDuplicates_End > 0 && numDuplicates_Start == 0) {

            return mapConfig.tourLocation_EndFill_ColorSWT;

         } else {

            return mapConfig.tourLocationFill_ColorSWT;
         }

      case TOUR_PAUSE:        return mapConfig.tourPauseFill_ColorSWT;

      case TOUR_PHOTO:        return mapConfig.photoFill_ColorSWT;

      default:
      case TOUR_MARKER:       return mapConfig.tourMarkerFill_ColorSWT;

      }

// SET_FORMATTING_ON
   }

   public String getFormattedLabel() {

      if (tourMarker != null) {

         // a tour marker is displayed

         return numDuplicates < 2
               ? formattedLabel
               : formattedLabel + UI.SPACE + UI.SYMBOL_BRACKET_LEFT + numDuplicates + UI.SYMBOL_BRACKET_RIGHT;

      } else {

         // a tour location is displayed

         if (numDuplicates_Start == 0 || numDuplicates_End == 0) {

            return formattedLabel;

         } else {

            return formattedLabel + UI.SPACE + UI.SYMBOL_BRACKET_LEFT + numDuplicates_Start + UI.SLASH + numDuplicates_End + UI.SYMBOL_BRACKET_RIGHT;
         }
      }
   }

   public Color getOutlineColorAWT() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

// SET_FORMATTING_OFF

      switch (pointType) {

      case COMMON_LOCATION:   return mapConfig.commonLocationOutline_ColorAWT;

      case TOUR_LOCATION:

         if (numDuplicates_Start > 0 && numDuplicates_End == 0) {

            return mapConfig.tourLocation_StartOutline_ColorAWT;

         } else if (numDuplicates_End > 0 && numDuplicates_Start == 0) {

            return mapConfig.tourLocation_EndOutline_ColorAWT;

         } else {

            return mapConfig.tourLocationOutline_ColorAWT;
         }

      case TOUR_PAUSE:        return mapConfig.tourPauseOutline_ColorAWT;

      case TOUR_PHOTO:        return mapConfig.photoOutline_ColorAWT;

      default:
      case TOUR_MARKER:       return mapConfig.tourMarkerOutline_ColorAWT;

      }

// SET_FORMATTING_ON
   }

   public org.eclipse.swt.graphics.Color getOutlineColorSWT() {

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

// SET_FORMATTING_OFF

      switch (pointType) {

      case COMMON_LOCATION:   return mapConfig.commonLocationOutline_ColorSWT;

      case TOUR_LOCATION:

         if (numDuplicates_Start > 0 && numDuplicates_End == 0) {

            return mapConfig.tourLocation_StartOutline_ColorSWT;

         } else if (numDuplicates_End > 0 && numDuplicates_Start == 0) {

            return mapConfig.tourLocation_EndOutline_ColorSWT;

         } else {

            return mapConfig.tourLocationOutline_ColorSWT;
         }

      case TOUR_PAUSE:        return mapConfig.tourPauseOutline_ColorSWT;

      case TOUR_PHOTO:        return mapConfig.photoOutline_ColorSWT;

      default:
      case TOUR_MARKER:       return mapConfig.tourMarkerOutline_ColorSWT;

      }

// SET_FORMATTING_ON
   }

   /**
    * This is needed for the tour marker clustering
    */
   @Override
   public GeoPoint getPosition() {
      return geoPoint;
   }

   public void setFormattedLabel(final String formattedLabel) {

      this.formattedLabel = formattedLabel;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "Map2Point" //$NON-NLS-1$

            + " ID=" + ID + ", " //$NON-NLS-1$ //$NON-NLS-2$
//          + " geoPoint =" + geoPoint + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + " label=" + formattedLabel + ", " //$NON-NLS-1$ //$NON-NLS-2$

            + UI.NEW_LINE;
   }

}
