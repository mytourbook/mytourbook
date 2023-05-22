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

import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;

import org.eclipse.jface.viewers.ISelection;

/**
 * Selection contains data for a compared tour from the {@link ReferenceTourView}
 */
public class SelectionReferenceTourView implements ISelection {

   private static final String    NL = UI.NEW_LINE;

   /**
    * Unique id for a reference tour in {@link TourReference} entity
    */
   private Long                   _refId;

   /**
    * unique id for {@link TourCompared} entity or <code>-1</code> when the compared tour is not
    * saved in the database
    */
   private Long                   _compTourId;

   private TVIRefTour_RefTourItem _catalogRefItem;
   private TVIRefTour_YearItem    _catalogYearItem;

   public SelectionReferenceTourView(final long refId) {

      _refId = refId;
      _catalogRefItem = ElevationCompareManager.createCatalogRefItem(refId);
   }

   public SelectionReferenceTourView(final TVIRefTour_RefTourItem refItem) {

      _refId = refItem.refId;
      _catalogRefItem = refItem;
   }

   public SelectionReferenceTourView(final TVIRefTour_YearItem yearItem) {

      _refId = yearItem.refId;
      _catalogYearItem = yearItem;
   }

   /**
    * @return Returns the tour Id of the {@link TourData} for the compared tour or
    *         <code>null</code> when it's not set
    */
   public Long getCompTourId() {
      return _compTourId;
   }

   public Long getRefId() {
      return _refId;
   }

   /**
    * @return Returns the item {@link TVIRefTour_RefTourItem} or <code>null</code> when it's not
    *         set
    */
   public TVIRefTour_RefTourItem getRefItem() {
      return _catalogRefItem;
   }

   /**
    * @return Returns the item {@link TVIRefTour_YearItem} or <code>null</code> when it's not set
    */
   public TVIRefTour_YearItem getYearItem() {
      return _catalogYearItem;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "SelectionReferenceTourView" + NL //$NON-NLS-1$

            + "[" + NL //$NON-NLS-1$

            + "_refId=" + _refId + NL //$NON-NLS-1$
            + "_compTourId=" + _compTourId + NL //$NON-NLS-1$
            + "_catalogRefItem=" + _catalogRefItem + NL //$NON-NLS-1$
            + "_catalogYearItem=" + _catalogYearItem + NL //$NON-NLS-1$

            + "]" + NL //$NON-NLS-1$

      ;
   }

}
