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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

public class TourLocationProfile implements Cloneable, Comparable<Object> {

   private static final char NL         = UI.NEW_LINE;

   private static int        _idCounter = 0;

   int                       profileId;

   /**
    * Profile name
    */
   String                    name       = Messages.Tour_Location_DefaultProfileName;

   /**
    * Zoom address detail
    *
    * 3 country
    * 5 state
    * 8 county
    * 10 city
    * 12 town / borough
    * 13 village / suburb
    * 14 neighbourhood
    * 15 any settlement
    * 16 major streets
    * 17 major and minor streets
    * 18 building
    */
   int                       zoomlevel  = TourLocationManager.DEFAULT_ZOOM_LEVEL_VALUE;

   List<LocationPartID>      allParts   = new ArrayList<>();

   public TourLocationProfile() {

      profileId = ++_idCounter;
   }

   public TourLocationProfile(final String name,
                              final int zoomlevel,
                              final LocationPartID... allParts) {

      profileId = ++_idCounter;

      this.name = name;
      this.zoomlevel = zoomlevel;

      this.allParts.addAll(Arrays.asList(allParts));
   }

   @Override
   protected TourLocationProfile clone() {

      TourLocationProfile clonedObject = null;

      try {

         clonedObject = (TourLocationProfile) super.clone();

         clonedObject.profileId = ++_idCounter;

         // create a unique name
         clonedObject.name = name + UI.SPACE + Integer.toString(clonedObject.profileId);

         // clone all parts
         clonedObject.allParts = new ArrayList<>();
         for (final LocationPartID part : allParts) {
            clonedObject.allParts.add(part);
         }

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return clonedObject;
   }

   @Override
   public int compareTo(final Object obj) {

      if (obj instanceof final TourLocationProfile otherProfile) {
         return name.compareTo(otherProfile.name);
      }

      return 0;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (getClass() != obj.getClass()) {
         return false;
      }

      final TourLocationProfile other = (TourLocationProfile) obj;

      if (profileId != other.profileId) {
         return false;
      }

      return true;
   }

   public String getName() {

      return name;
   }

   public int getZoomlevel() {
      return zoomlevel;
   }

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + profileId;

      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourLocationProfile" + NL //              //$NON-NLS-1$

            + " name      = " + name + NL //             //$NON-NLS-1$
            + " zoomlevel = " + zoomlevel + NL //        //$NON-NLS-1$
            + " profileId = " + profileId + NL //        //$NON-NLS-1$
            + " allParts  =" + allParts + NL //          //$NON-NLS-1$

            + NL;
   }

}
