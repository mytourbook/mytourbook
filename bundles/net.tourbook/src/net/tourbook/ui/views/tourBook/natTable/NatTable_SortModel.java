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

import java.util.Comparator;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.sort.ISortModel;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;

public class NatTable_SortModel implements ISortModel {

   public NatTable_SortModel(final ConfigRegistry configRegistry, final DataLayer columnHeader_DataLayer) {
      // TODO Auto-generated constructor stub
   }

   @Override
   public void clear() {

   }

   @Override
   public Comparator<?> getColumnComparator(final int columnIndex) {

      return null;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public List<Comparator> getComparatorsForColumnIndex(final int columnIndex) {

      return null;
   }

   @Override
   public SortDirectionEnum getSortDirection(final int columnIndex) {

      return null;
   }

   @Override
   public List<Integer> getSortedColumnIndexes() {

      return null;
   }

   @Override
   public int getSortOrder(final int columnIndex) {

      return 0;
   }

   @Override
   public boolean isColumnIndexSorted(final int columnIndex) {

      return false;
   }

   @Override
   public void sort(final int columnIndex, final SortDirectionEnum sortDirection, final boolean accumulate) {

   }

}
