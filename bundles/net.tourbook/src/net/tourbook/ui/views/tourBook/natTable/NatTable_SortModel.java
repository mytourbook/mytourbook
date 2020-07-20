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

   private ArrayList<Integer>           _allSortedColumnIndexes = new ArrayList<>();
   private ArrayList<Integer>           _allSortOrders          = new ArrayList<>();
   private ArrayList<SortDirectionEnum> _allSortDirections      = new ArrayList<>();

   private ColumnManager                _columnManager;

   private NatTable_DataLoader          _dataLoader;

   public NatTable_SortModel(final ColumnManager columnManager, final NatTable_DataLoader dataLoader) {

      _columnManager = columnManager;
      _dataLoader = dataLoader;
   }

   @Override
   public void clear() {

      _allSortedColumnIndexes.clear();
      _allSortOrders.clear();
      _allSortDirections.clear();
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

      if (_allSortedColumnIndexes.contains(columnIndex)) {
         return this._allSortDirections.get(this._allSortOrders.indexOf(columnIndex));
      }

      return SortDirectionEnum.NONE;
   }

   @Override
   public List<Integer> getSortedColumnIndexes() {
      return _allSortedColumnIndexes;
   }

   @Override
   public int getSortOrder(final int columnIndex) {

      if (_allSortedColumnIndexes.contains(columnIndex)) {
         return _allSortOrders.indexOf(columnIndex);
      }

      return -1;
   }

   @Override
   public boolean isColumnIndexSorted(final int columnIndex) {
      return _allSortedColumnIndexes.contains(columnIndex);
   }

   public void setupSortColumns(final String[] allSortColumnIds, final ArrayList<SortDirectionEnum> allSortDirections) {

      clear();

      for (int columnIdIndex = 0; columnIdIndex < allSortColumnIds.length; columnIdIndex++) {

         final String columnId = allSortColumnIds[columnIdIndex];
         final SortDirectionEnum sortDirection = allSortDirections.get(columnIdIndex);

         final ArrayList<ColumnDefinition> allVisibleColums = _columnManager.getVisibleAndSortedColumns();

         for (int columnIndex = 0; columnIndex < allVisibleColums.size(); columnIndex++) {

            final ColumnDefinition colDef = allVisibleColums.get(columnIndex);
            if (columnId.equals(colDef.getColumnId())) {

               _allSortedColumnIndexes.add(columnIndex);
               _allSortOrders.add(columnIndex);
               _allSortDirections.add(sortDirection);

               break;
            }
         }
      }

      _dataLoader.setupSortColumns(allSortColumnIds, allSortDirections);
   }

   @Override
   public void sort(final int columnIndex, final SortDirectionEnum sortDirection, final boolean isAccumulate) {

      if (isAccumulate == false) {

         // don't accumulate -> start a new sort

         clear();

         if (sortDirection.equals(SortDirectionEnum.NONE)) {

            // do not sort anything, this occures when the sort direction is toggled

         } else {

            // start a new sorting

            _allSortedColumnIndexes.add(columnIndex);
            _allSortOrders.add(columnIndex);
            _allSortDirections.add(sortDirection);
         }

      } else {

         // accumulate colum sortings

         // check if an already sorting column is clicked again
         if (isColumnIndexSorted(columnIndex) == false) {

            // a new column is selected

            _allSortedColumnIndexes.add(columnIndex);
            _allSortOrders.add(columnIndex);
            _allSortDirections.add(sortDirection);

         } else {

            // a click on an already sorted column is done -> revert sorting

            final int orderIndex = _allSortOrders.indexOf(columnIndex);
            final SortDirectionEnum newSortDirection = _allSortDirections.get(orderIndex).getNextSortDirection();

            _allSortDirections.set(orderIndex, newSortDirection);
         }
      }

      // convert column index into column id
      final ArrayList<ColumnDefinition> allColumnDefs = _columnManager.getVisibleAndSortedColumns();
      final ArrayList<String> allSortedColumnIds = new ArrayList<>();
      for (final Integer sortedIndex : _allSortedColumnIndexes) {
         allSortedColumnIds.add(allColumnDefs.get(sortedIndex).getColumnId());
      }

      // setup the data loader with the new sorting field/direction
      final String[] allSortedColumnIdsArray = allSortedColumnIds.toArray(new String[allSortedColumnIds.size()]);

      _dataLoader.setupSortColumns(allSortedColumnIdsArray, _allSortDirections);
   }

}
