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

import net.tourbook.common.util.ColumnManager;

import org.eclipse.nebula.widgets.nattable.data.IDataProvider;

public class DataProvider_ColumnHeader implements IDataProvider {

   private NatTable_DataLoader  _dataProvider;
   private ColumnManager _columnManager;

   public DataProvider_ColumnHeader(final NatTable_DataLoader natTable_DataProvider, final ColumnManager columnManager) {

      _dataProvider = natTable_DataProvider;
      _columnManager = columnManager;
   }

   @Override
   public int getColumnCount() {
      return _dataProvider.getNumberOfVisibleColumns();
   }

   @Override
   public Object getDataValue(final int columnIndex, final int rowIndex) {

      if (columnIndex < 0 || columnIndex >= _dataProvider.getNumberOfVisibleColumns()) {
         return null;
      }

      return _dataProvider.allSortedColumns.get(columnIndex).getColumnHeaderText(_columnManager);
   }

   @Override
   public int getRowCount() {
      return 1;
   }

   @Override
   public void setDataValue(final int columnIndex, final int rowIndex, final Object newValue) {
      throw new UnsupportedOperationException();
   }

}
