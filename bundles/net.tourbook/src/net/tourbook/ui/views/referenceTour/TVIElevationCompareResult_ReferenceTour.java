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

import de.byteholder.geoclipse.map.UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import net.tourbook.common.util.TreeViewerItem;

/**
 * Contains tree viewer items (TVI) for reference tours
 */
public class TVIElevationCompareResult_ReferenceTour extends TVIElevationCompareResult_Item {

   private static Comparator<? super TreeViewerItem> _compareResultComparator;

   static {

      _compareResultComparator = new Comparator<TreeViewerItem>() {

         @Override
         public int compare(final TreeViewerItem e1, final TreeViewerItem e2) {

            final TVIElevationCompareResult_ComparedTour result1 = (TVIElevationCompareResult_ComparedTour) e1;
            final TVIElevationCompareResult_ComparedTour result2 = (TVIElevationCompareResult_ComparedTour) e2;

            return (int) (result1.minAltitudeDiff - result2.minAltitudeDiff);
         }
      };
   }

   String                                    label;

   long                                      tourId;

   RefTourItem                               refTourItem;
   Object[]                                  sortedAndFilteredCompareResults;

   /**
    * Keeps the tourId's for all compared tours which have already been stored in the db
    */
   private HashMap<Long, StoredComparedTour> _storedComparedTours;

   public TVIElevationCompareResult_ReferenceTour(final TVIElevationCompareResult_RootItem parentItem,
                                                  final String label,
                                                  final RefTourItem refTourItem,
                                                  final long tourId) {

      this.setParentItem(parentItem);

      this.label = label;
      this.refTourItem = refTourItem;
      this.tourId = tourId;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TVIElevationCompareResult_ReferenceTour)) {
         return false;
      }
      final TVIElevationCompareResult_ReferenceTour other = (TVIElevationCompareResult_ReferenceTour) obj;
      if (refTourItem == null) {
         if (other.refTourItem != null) {
            return false;
         }
      } else if (!refTourItem.equals(other.refTourItem)) {
         return false;
      }
      return true;
   }

   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      final long refId = refTourItem.refId;

      if (_storedComparedTours != null) {
         _storedComparedTours.clear();
      }

      _storedComparedTours = ElevationCompareManager.getComparedToursFromDb(refId);

      final TVIElevationCompareResult_ComparedTour[] comparedTours = ElevationCompareManager.getComparedTours();

      // create children for one reference tour
      for (final TVIElevationCompareResult_ComparedTour compTour : comparedTours) {

         if (compTour.refTour.refId == refId) {

            // compared tour belongs to the reference tour

            // keep the ref tour as the parent
            compTour.setParentItem(this);

            /*
             * Set the status if the compared tour is already stored in the database and set the
             * id for the compared tour
             */
            final Long comparedTourId = compTour.getTourId();
            final boolean isStoredForRefTour = _storedComparedTours.containsKey(comparedTourId);

            if (isStoredForRefTour) {

               final StoredComparedTour storedComparedTour = _storedComparedTours.get(comparedTourId);

// SET_FORMATTING_OFF

               compTour.compareId         = storedComparedTour.comparedId;
               compTour.savedStartIndex   = storedComparedTour.startIndex;
               compTour.savedEndIndex     = storedComparedTour.endIndex;
               compTour.savedElapsedTime  = storedComparedTour.tourElapsedTime;

               compTour.savedSpeed        = storedComparedTour.tourSpeed;
               compTour.savedPace         = storedComparedTour.tourPace;

// SET_FORMATTING_ON

            } else {

               compTour.compareId = -1;
            }

            children.add(compTour);
         }
      }

      /*
       * Sort children that the next/prev navigation is working correctly.
       */
      Collections.sort(children, _compareResultComparator);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((refTourItem == null) ? 0 : refTourItem.hashCode());
      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVIElevationCompareResult_ReferenceTour" + NL //   //$NON-NLS-1$

            + "[" + NL //                                         //$NON-NLS-1$

            + "   label       =" + label + NL //                  //$NON-NLS-1$
            + "   tourId      =" + tourId + NL //                 //$NON-NLS-1$

            + "   refTourItem =" + refTourItem + NL //            //$NON-NLS-1$

            + "]" + NL //                                         //$NON-NLS-1$

      ;
   }

}
