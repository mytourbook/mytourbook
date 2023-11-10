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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

public class TourLocationProfile implements Cloneable {

   private static final char NL         = UI.NEW_LINE                              //
   ;

   private static int        _idCounter = 0;

   int                       profileId;

   /**
    * Profile name
    */
   String                    name       = Messages.Tour_Location_Default_ProfileName;

//   ArrayList<TourFilterProperty> filterProperties = new ArrayList<>();

   public TourLocationProfile() {

      profileId = ++_idCounter;
   }

   @Override
   protected TourLocationProfile clone() {

      TourLocationProfile clonedObject = null;

      try {

         clonedObject = (TourLocationProfile) super.clone();

         clonedObject.profileId = ++_idCounter;

         // create a unique name
         clonedObject.name = name + UI.SPACE + Integer.toString(clonedObject.profileId);

//         clonedObject.filterProperties = new ArrayList<>();
//
//         for (final TourFilterProperty tourFilterProperty : filterProperties) {
//            clonedObject.filterProperties.add(tourFilterProperty.clone());
//         }

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return clonedObject;
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

            + "TourFilterProfile" + NL //                //$NON-NLS-1$

            + " profileId = " + profileId + NL //        //$NON-NLS-1$
            + " name      = " + name + NL //             //$NON-NLS-1$

            + NL;
   }

}
