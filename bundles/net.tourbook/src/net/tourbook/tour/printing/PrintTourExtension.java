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
package net.tourbook.tour.printing;

import java.util.List;

import net.tourbook.data.TourData;

import org.eclipse.jface.resource.ImageDescriptor;

public abstract class PrintTourExtension {

   private String          _printId;
   private String          _visibleName;
   private ImageDescriptor _imageDescriptor;

   public ImageDescriptor getImageDescriptor() {
      return _imageDescriptor;
   }

   public String getPrintId() {
      return _printId;
   }

   public String getVisibleName() {
      return _visibleName;
   }

   /**
    * Prints the tours in the {@link TourData} list. If only one tour is printed, the values of
    * tourStartIndex and tourEndIndex is the range which points are printed, when the index is -1,
    * the whole tour is printed.
    *
    * @param tourDataList
    * @param tourStartIndex
    * @param tourEndIndex
    */
   public abstract void printTours(List<TourData> tourDataList, int tourStartIndex, int tourEndIndex);

   public void setImageDescriptor(final ImageDescriptor imageDescriptor) {
      _imageDescriptor = imageDescriptor;
   }

   public void setPrintId(final String printId) {
      _printId = printId;
   }

   public void setVisibleName(final String visibleName) {
      _visibleName = visibleName;
   }

   @Override
   public String toString() {

      return new StringBuilder().append("id: ")//$NON-NLS-1$
            .append(_printId)
            .append(" \t") //$NON-NLS-1$
            //
            .append("name: ") //$NON-NLS-1$
            .append(_visibleName)
            .append(" \t") //$NON-NLS-1$
            //
            .toString();
   }
}
