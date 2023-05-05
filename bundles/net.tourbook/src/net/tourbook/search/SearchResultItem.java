/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import net.tourbook.common.UI;

public class SearchResultItem {

   private static final char NL = UI.NEW_LINE;

   long                      tourStartTime;

   int                       docSource;

   String                    markerId;
   String                    tourId;

   String                    title;
   String                    description;
   String                    locationStart;
   String                    locationEnd;
   String                    weather;

//   float   score;

   /**
    * Lucene doc id
    */
   int docId;

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "SearchResultItem [" + NL //                  //$NON-NLS-1$

            + "   tourId=" + tourId + NL //                 //$NON-NLS-1$
            + "   markerId=" + markerId + NL //             //$NON-NLS-1$
            + "   tourStartTime=" + tourStartTime + NL //   //$NON-NLS-1$
            + "   tourTitle=" + title + NL //               //$NON-NLS-1$
            + "   description=" + description + NL //       //$NON-NLS-1$

            + "]"; //                                       //$NON-NLS-1$
   }

}
