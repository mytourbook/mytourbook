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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;

import org.eclipse.nebula.widgets.nattable.sort.ISortModel;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;

/**
 * Part of the source code is copied from
 * {@linkplain org.eclipse.nebula.widgets.nattable.examples._500_Layers._509_SortHeaderLayerExample.PersonWithAddressSortModel}
 */
public class NatTable_SortModel implements ISortModel {

   private List<Integer>           _sortedColumnIndexes;
   private List<Integer>           _sortOrder;
   private List<SortDirectionEnum> _sortDirection;

   private ColumnManager           _columnManager;

   private NatTable_DataLoader     _dataLoader;

   public NatTable_SortModel(final ColumnManager columnManager, final NatTable_DataLoader dataLoader) {

      _columnManager = columnManager;
      _dataLoader = dataLoader;
   }

   @Override
   public void clear() {

      _sortedColumnIndexes.clear();
      _sortOrder.clear();
      _sortDirection.clear();
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

   /**
    * @return the direction in which the column with the given index is
    *         currently sorted
    */
   @Override
   public SortDirectionEnum getSortDirection(final int columnIndex) {

      if (_sortedColumnIndexes.contains(columnIndex)) {
         return this._sortDirection.get(this._sortOrder.indexOf(columnIndex));
      }

      return SortDirectionEnum.NONE;
   }

   @Override
   public List<Integer> getSortedColumnIndexes() {
      return _sortedColumnIndexes;
   }

   @Override
   public int getSortOrder(final int columnIndex) {

      if (_sortedColumnIndexes.contains(columnIndex)) {
         return _sortOrder.indexOf(columnIndex);
      }

      return -1;
   }

   @Override
   public boolean isColumnIndexSorted(final int columnIndex) {
      return _sortedColumnIndexes.contains(columnIndex);
   }

   public void setupSortColumn(final String sortColumnId, final Enum<SortDirectionEnum> sortDirection) {

      final ArrayList<ColumnDefinition> allVisibleColums = _columnManager.getVisibleAndSortedColumns();

      for (int columnIndex = 0; columnIndex < allVisibleColums.size(); columnIndex++) {
         final ColumnDefinition colDef = allVisibleColums.get(columnIndex);
         if (sortColumnId.equals(colDef.getColumnId())) {

            // setup sort fields, this will not start the sorting
            sort(columnIndex, (SortDirectionEnum) sortDirection, false);

            return;
         }
      }
   }

   @Override
   public void sort(final int columnIndex, final SortDirectionEnum sortDirection, final boolean isAccumulate) {

      if (isAccumulate == false) {
         clear();
      }

      _sortedColumnIndexes.add(columnIndex);
      _sortOrder.add(columnIndex);
      _sortDirection.add(sortDirection);

      if (!isColumnIndexSorted(columnIndex)) {
         clear();
      }

      SortDirectionEnum sortDirectionAdjusted = sortDirection;

      if (sortDirection.equals(SortDirectionEnum.NONE)) {

         // we don't support NONE as user action
         sortDirectionAdjusted = SortDirectionEnum.ASC;
      }

      // convert column index into column id
      final ArrayList<ColumnDefinition> allColumns = _columnManager.getVisibleAndSortedColumns();

      final ArrayList<String> allSortedColumnIds = new ArrayList<>();

      for (final Integer sortedIndex : _sortedColumnIndexes) {
         allSortedColumnIds.add(allColumns.get(sortedIndex).getColumnId());
      }

      // setup the data loader with the new sorting field/direction
      _dataLoader.setupSortColumn(allSortedColumnIds, _sortDirection);
   }

}
