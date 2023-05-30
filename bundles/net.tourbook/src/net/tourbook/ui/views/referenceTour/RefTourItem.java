/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;

public class RefTourItem {

   private static final char NL = UI.NEW_LINE;

   /**
    * Entity ID of the reference tour {@link TourReference}
    */
   public long               refId;

   /**
    * Tour ID of the {@link TourData} which is referenced
    */
   public long               tourId;

   public String             label;

   public int                startIndex;
   public int                endIndex;

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof RefTourItem)) {
         return false;
      }
      final RefTourItem other = (RefTourItem) obj;
      if (refId != other.refId) {
         return false;
      }
      return true;
   }

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      result = prime * result + (int) (refId ^ (refId >>> 32));

      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "RefTourItem" + NL //                      //$NON-NLS-1$

            + "[" + NL //                                //$NON-NLS-1$

            + "refId       =" + refId + NL //            //$NON-NLS-1$
            + "tourId      =" + tourId + NL //           //$NON-NLS-1$
            + "label       =" + label + NL //            //$NON-NLS-1$
            + "startIndex  =" + startIndex + NL //       //$NON-NLS-1$
            + "endIndex    =" + endIndex + NL //         //$NON-NLS-1$

            + "]" + NL //                                //$NON-NLS-1$
      ;
   }
}
