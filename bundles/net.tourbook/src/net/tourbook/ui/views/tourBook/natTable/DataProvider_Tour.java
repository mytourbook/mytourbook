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

public class DataProvider_Tour implements IRowDataProvider<TVITourBookTour> {

   private DataProvider _dataProvider;

   public DataProvider_Tour(final DataProvider natTable_DataProvider) {

      _dataProvider = natTable_DataProvider;
   }

   @Override
   public int getColumnCount() {
      return _dataProvider.numVisibleColumns;
   }

   @Override
   public Object getDataValue(final int columnIndex, final int rowIndex) {

      final TVITourBookTour tviTour = _dataProvider.getTour(rowIndex);

      if (tviTour == null) {

         // tour is not yet loaded
         return null;
      }

      final ColumnDefinition colDef = _dataProvider.allSortedColumns.get(columnIndex + 1);
      final NatTable_LabelProvider labelProvider = colDef.getNatTable_LabelProvider();

      if (labelProvider == null) {

         return "n/a";

      } else {

         return labelProvider.getValueText(tviTour);
      }
   }

   @Override
   public int getRowCount() {

      return _dataProvider.getNumberOfTours();
   }

   @Override
   public TVITourBookTour getRowObject(final int rowIndex) {

      return _dataProvider.getTour(rowIndex);
   }

   @Override
   public int indexOfRowObject(final TVITourBookTour rowObject) {

      // a lazy data provider cannot easily get the index by it's object

      return -1;
   }

   @Override
   public void setDataValue(final int columnIndex, final int rowIndex, final Object newValue) {

      // setting data within the table is not yet supported
   }

}
