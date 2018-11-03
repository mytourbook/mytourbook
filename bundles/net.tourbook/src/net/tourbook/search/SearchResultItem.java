/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

public class SearchResultItem {

   int    docSource;

   String markerId;
   String tourId;

   String title;
   String description;
   long   tourStartTime;

//	float	score;

   /**
    * Lucene doc id.
    */
   int docId;

   @Override
   public String toString() {
      return "SearchResultItem [" //$NON-NLS-1$
            + ("tourId=" + tourId + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("markerId=" + markerId + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("tourStartTime=" + tourStartTime + ", ") //$NON-NLS-1$ //$NON-NLS-2$
//				+ ("markerLabel=" + markerLabel + ", ")
            + ("tourTitle=" + title + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("description=" + description + ", ") //$NON-NLS-1$ //$NON-NLS-2$
//				+ ("score=" + score)
            + "]"; //$NON-NLS-1$
   }

}
