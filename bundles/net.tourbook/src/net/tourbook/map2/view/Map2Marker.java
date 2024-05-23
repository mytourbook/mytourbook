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
import net.tourbook.map25.layer.marker.algorithm.distance.ClusterItem;

import org.oscim.core.GeoPoint;

/**
 * Immutable class describing a GeoPoint with a Title and a Description
 */
public class Map2Marker implements ClusterItem {

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
    * {@link #tourMarker} or {@link #tourLocation} is set
    */
   public TourMarker   tourMarker;

   /**
    * {@link #tourMarker} or {@link #tourLocation} is set
    */
   public TourLocation tourLocation;

   /**
    * @param tourMarker
    * @param geoPoint
    */
   public Map2Marker(final GeoPoint geoPoint) {

      this.geoPoint = geoPoint;
   }

   public String getFormattedLabel() {

      if (tourMarker != null) {
         
         // tour marker is wrapped

         return numDuplicates < 2
               ? formattedLabel
               : formattedLabel + " (" + numDuplicates + ")";

      } else {
         
         // tour location is wrapped

         if (numDuplicates_Start > 1 && numDuplicates_End > 1) {

            return formattedLabel + " (" + numDuplicates_Start + " / " + numDuplicates_End + ")";

         } else if (numDuplicates_Start > 1) {

            return formattedLabel + " (" + numDuplicates_Start + ")";

         } else if (numDuplicates_End > 1) {

            return formattedLabel + " (" + numDuplicates_End + ")";

         } else {

            return formattedLabel;
         }
      }
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

            + "Map2Marker" //$NON-NLS-1$

            + " geoPoint =" + geoPoint + ", " //$NON-NLS-1$ //$NON-NLS-2$

            + UI.NEW_LINE;
   }

}
