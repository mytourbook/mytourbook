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

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourLocation;

public class PartItem implements Comparable<Object> {

   public static final String ALLOWED_FIELDNAME_NAME         = "name";         //$NON-NLS-1$
   public static final String ALLOWED_FIELDNAME_DISPLAY_NAME = "display_name"; //$NON-NLS-1$

   private static final char  NL                             = UI.NEW_LINE;

   public LocationPartID      partID_Start;
   public LocationPartID      partID_End;

   public String              partLabel_Start;
   public String              partLabel_End;

   public String              locationLabel_Start            = UI.EMPTY_STRING;
   public String              locationLabel_End              = UI.EMPTY_STRING;

   public PartItem(final LocationPartID partID,
                   final String partLabel,

                   final String locationLabel,
                   final boolean isSetStartLocation) {

      if (isSetStartLocation) {

         this.partID_Start = partID;
         this.partLabel_Start = partLabel;

         this.locationLabel_Start = locationLabel;

      } else {

         this.partID_End = partID;
         this.partLabel_End = partLabel;

         this.locationLabel_End = locationLabel;
      }
   }

   public static Map<LocationPartID, PartItem> getAllPartItems(final TourLocation tourLocation, final boolean isSetStartLocation) {

      final Map<LocationPartID, PartItem> allPartItems = new LinkedHashMap<>();

      try {

         final Field[] allAddressFields = tourLocation.getClass().getFields();

         // loop: all fields in the retrieved address
         for (final Field field : allAddressFields) {

            String fieldName = field.getName();

            if (ALLOWED_FIELDNAME_NAME.equals(fieldName)) {

               // allow this field that it can be selected as part

               fieldName = LocationPartID.OSM_NAME.name();

            } else if (ALLOWED_FIELDNAME_DISPLAY_NAME.equals(fieldName)) {

               // allow this field that it can be selected as part

               fieldName = LocationPartID.OSM_DEFAULT_NAME.name();

            } else if (TourLocation.IGNORED_FIELDS.contains(fieldName)) {

               // skip field names which are not address parts
               continue;
            }

            final Object fieldValue = field.get(tourLocation);

            if (fieldValue instanceof final String stringValue) {

               // use only fields with a value
               if (stringValue.length() > 0) {

                  final LocationPartID partID = LocationPartID.valueOf(fieldName);
                  final String partLabel = TourLocationManager.ALL_LOCATION_PART_AND_LABEL.get(partID);

                  allPartItems.put(partID, new PartItem(partID, partLabel, stringValue, isSetStartLocation));
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {
         StatusUtil.showStatus(e);
      }

      return allPartItems;
   }

   @Override
   public int compareTo(final Object other) {

      /*
       * Set sorting
       */

      if (other instanceof final PartItem otherPartItem) {

         if (otherPartItem.locationLabel_Start != null) {

            return locationLabel_Start.compareTo(otherPartItem.locationLabel_Start);
         }

         if (otherPartItem.locationLabel_End != null) {

            return locationLabel_End.compareTo(otherPartItem.locationLabel_End);
         }
      }

      return 0;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "PartItem" + NL //                                              //$NON-NLS-1$

            + "   partID_Start         = " + partID_Start + NL //             //$NON-NLS-1$
            + "   partID_End           = " + partID_End + NL //               //$NON-NLS-1$

            + "   partLabel_Start      = " + partLabel_Start + NL //          //$NON-NLS-1$
            + "   partLabel_End        = " + partLabel_End + NL //            //$NON-NLS-1$

            + NL

            + "   locationLabel_Start  = " + locationLabel_Start + NL //      //$NON-NLS-1$
            + "   locationLabel_End    = " + locationLabel_End + NL //        //$NON-NLS-1$

            + NL;
   }
}
