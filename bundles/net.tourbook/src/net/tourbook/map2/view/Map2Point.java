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

import net.tourbook.common.UI;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourMarker;
import net.tourbook.map.location.LocationType;
import net.tourbook.map25.layer.marker.algorithm.distance.ClusterItem;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.oscim.core.GeoPoint;

/**
 * Immutable class describing a GeoPoint with a Title and a Description
 */
public class Map2Point implements ClusterItem {

   private static int  _uniqueID;

   public GeoPoint     geoPoint;

   /**
    * Geo position in device pixel for the marker position
    */
   public int          geoPointDevX;
   public int          geoPointDevY;

   /**
    * Long labels are wrapped
    */
   private String      formattedLabel;

   public int          numDuplicates;

   public int          numDuplicates_Start;
   public int          numDuplicates_End;

   /**
    * A {@link #tourMarker} or a {@link #tourLocation} is set
    */
   public TourMarker   tourMarker;

   /**
    * A {@link #tourMarker} or a {@link #tourLocation} is set
    */
   public TourLocation tourLocation;

   /**
    * Type of the tour location {@link #tourLocation}
    */
   public LocationType locationType;

   public Color        boundingBox_Color;

   public Rectangle    boundingBox;
   public Rectangle    boundingBox_Resized;

   /**
    * Unique ID
    */
   public String       ID;

   @SuppressWarnings("unused")
   private Map2Point() {}

   /**
    * @param tourMarker
    * @param geoPoint
    */
   public Map2Point(final GeoPoint geoPoint) {

      this.geoPoint = geoPoint;

      ID = String.valueOf(_uniqueID++);
   }

   public Color getFillColor() {

      final Map2Config markerConfig = Map2ConfigManager.getActiveConfig();

      final boolean isTourMarker = tourMarker != null;

      final Color fillColor = isTourMarker

            ? markerConfig.markerFill_Color
            : locationType.equals(LocationType.Common)

                  ? markerConfig.commonLocationFill_Color
                  : markerConfig.tourLocationFill_Color;

      return fillColor;
   }

   public Color getFillColor_Hovered() {

      final Map2Config markerConfig = Map2ConfigManager.getActiveConfig();

      final boolean isTourMarker = tourMarker != null;

      final Color fillColor = isTourMarker

            ? markerConfig.markerFill_Hovered_Color

            : locationType.equals(LocationType.Common)

                  ? markerConfig.commonLocationFill_Hovered_Color
                  : markerConfig.tourLocationFill_Hovered_Color;

      return fillColor;
   }

   public String getFormattedLabel() {

      if (tourMarker != null) {

         // a tour marker is displayed

         return numDuplicates < 2
               ? formattedLabel
               : formattedLabel + " (" + numDuplicates + ")";

      } else {

         // a tour location is displayed

         if (numDuplicates_Start > 1 || numDuplicates_End > 1) {

            return formattedLabel + " (" + numDuplicates_Start + "/" + numDuplicates_End + ")";

         } else if (numDuplicates_Start > 1) {

            return formattedLabel + " (" + numDuplicates_Start + "s)";

         } else if (numDuplicates_End > 1) {

            return formattedLabel + " (" + numDuplicates_End + "e)";

         } else {

            return formattedLabel;
         }
      }
   }

   public Color getOutlineColor() {

      final Map2Config markerConfig = Map2ConfigManager.getActiveConfig();

      final boolean isTourMarker = tourMarker != null;

      final Color outlineColor = isTourMarker

            ? markerConfig.markerOutline_Color
            : locationType.equals(LocationType.Common)

                  ? markerConfig.commonLocationOutline_Color
                  : markerConfig.tourLocationOutline_Color;

      return outlineColor;
   }

   public Color getOutlineColor_Hovered() {

      final Map2Config markerConfig = Map2ConfigManager.getActiveConfig();

      final boolean isTourMarker = tourMarker != null;

      final Color outlineColor = isTourMarker

            ? markerConfig.markerOutline_Hovered_Color
            : locationType.equals(LocationType.Common)

                  ? markerConfig.commonLocationOutline_Hovered_Color
                  : markerConfig.tourLocationOutline_Hovered_Color;

      return outlineColor;
   }

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
