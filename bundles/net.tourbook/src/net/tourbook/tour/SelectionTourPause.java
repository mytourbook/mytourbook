/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.tour;

import net.tourbook.data.TourData;

import org.eclipse.jface.viewers.ISelection;

/**
 * This selection is fired when tour pauses are selected.
 */
public class SelectionTourPause implements ISelection {

   private TourData _tourData;
   private int      _serieIndex;

   public SelectionTourPause(final TourData tourData, final int serieIndex) {

      _tourData = tourData;
      _serieIndex = serieIndex;
   }

   public int getSerieIndex() {
      return _serieIndex;
   }

   public TourData getTourData() {
      return _tourData;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public String toString() {
      return "SelectionTourPause [" //$NON-NLS-1$
            + ("_serieIndex=" + _serieIndex) //$NON-NLS-1$
            + "]"; //$NON-NLS-1$
   }

}
