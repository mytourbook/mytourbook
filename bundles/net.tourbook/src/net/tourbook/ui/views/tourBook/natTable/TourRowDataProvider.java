/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook.natTable;

import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.NatTable_LabelProvider;
import net.tourbook.ui.views.tourBook.TVITourBookTour;

import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;

public class TourRowDataProvider implements IRowDataProvider<TVITourBookTour> {

   private NatTable_DataLoader _dataLoader;

   private TVITourBookTour     _dummyTourItem = new TVITourBookTour(null, null);

   public TourRowDataProvider(final NatTable_DataLoader natTable_DataProvider) {

      _dataLoader = natTable_DataProvider;
   }

   @Override
   public int getColumnCount() {
      return _dataLoader.getNumberOfVisibleColumns();
   }

   @Override
   public Object getDataValue(final int columnIndex, final int rowIndex) {

      final TVITourBookTour tviTour = _dataLoader.getTour(rowIndex);

      if (tviTour == null) {

         // tour is not yet loaded
         return null;
      }

      final ColumnDefinition colDef = _dataLoader.allSortedColumns.get(columnIndex);
      final NatTable_LabelProvider labelProvider = colDef.getNatTable_LabelProvider();

      if (labelProvider == null) {

         return "A LabelProvider is not defined"; //$NON-NLS-1$

      } else {

         return labelProvider.getValueText(tviTour);
      }
   }

   @Override
   public int getRowCount() {

      return _dataLoader.getNumberOfTours();
   }

   @Override
   public TVITourBookTour getRowObject(final int rowIndex) {

      final TVITourBookTour tourItem = _dataLoader.getTour(rowIndex);

      /**
       * Very important !
       * <p>
       * When <code>null</code> is returned then reselecting of tours which are not in the viewport
       * fails. It took me several days to debug this issue finally.
       */
      if (tourItem == null) {

         final long tourId = _dataLoader.getTourId(rowIndex);

         _dummyTourItem.tourId = tourId;

         return _dummyTourItem;
      }

      return tourItem;
   }

   @Override
   public int indexOfRowObject(final TVITourBookTour tourItem) {

      // a lazy data provider cannot easily get the index by it's object

      return _dataLoader.getFetchedTourIndex(tourItem.tourId);
   }

   @Override
   public void setDataValue(final int columnIndex, final int rowIndex, final Object newValue) {

      // setting data within the table is not yet supported
   }

}
