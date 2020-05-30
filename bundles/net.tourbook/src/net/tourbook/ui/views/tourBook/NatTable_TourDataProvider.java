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
package net.tourbook.ui.views.tourBook;

import net.tourbook.common.util.ColumnManager;

import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;

public class NatTable_TourDataProvider implements IRowDataProvider<TVITourBookTour> {

   private ColumnManager _columnManager;

   public NatTable_TourDataProvider(final ColumnManager columnManager) {

      _columnManager = columnManager;
   }

   @Override
   public int getColumnCount() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public Object getDataValue(final int columnIndex, final int rowIndex) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public int getRowCount() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public TVITourBookTour getRowObject(final int rowIndex) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public int indexOfRowObject(final TVITourBookTour rowObject) {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public void setDataValue(final int columnIndex, final int rowIndex, final Object newValue) {
      // TODO Auto-generated method stub

   }

}
