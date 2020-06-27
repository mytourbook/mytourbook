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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;

import org.eclipse.nebula.widgets.nattable.sort.ISortModel;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;

/**
 * Part of the cource code is copied from
 * {@linkplain org.eclipse.nebula.widgets.nattable.examples._500_Layers._509_SortHeaderLayerExample.PersonWithAddressSortModel}
 */
public class NatTable_SortModel implements ISortModel {

   /**
    * Array that contains the sort direction for every column. Needed to
    * access the current sort state of a column.
    */
   private SortDirectionEnum[] _sortDirections;

   /**
    * Array that contains the sorted flags for every column. Needed to
    * access the current sort state of a column.
    */
   private boolean[]           _sorted;

   /**
    * As this implementation only supports single column sorting, this
    * property contains the the column index of the column that is
    * currently used for sorting. Initial value = -1 for no sort column
    */
   private int                 _currentSortColumn    = -1;

   /**
    * As this implementation only supports single column sorting, this
    * property contains the current sort direction of the column that is
    * currently used for sorting.
    */
   private SortDirectionEnum   _currentSortDirection = SortDirectionEnum.ASC;

   private ColumnManager       _columnManager;

   private NatTable_DataLoader _dataLoader;

   public NatTable_SortModel(final ColumnManager columnManager, final NatTable_DataLoader dataLoader) {

      _columnManager = columnManager;
      _dataLoader = dataLoader;

      final ArrayList<ColumnDefinition> allVisibleColums = _columnManager.getVisibleAndSortedColumns();
      final int numVisibleColums = allVisibleColums.size();

      _sortDirections = new SortDirectionEnum[numVisibleColums];
      Arrays.fill(_sortDirections, SortDirectionEnum.NONE);

      _sorted = new boolean[numVisibleColums];
      Arrays.fill(_sorted, false);
   }

   @Override
   public void clear() {

      Arrays.fill(_sortDirections, SortDirectionEnum.NONE);
      Arrays.fill(_sorted, false);

      _currentSortColumn = -1;
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
      return _sortDirections[columnIndex];
   }

   /**
    * As this is a simple implementation of an {@link ISortModel} and we
    * don't support multiple column sorting, this list returns either a
    * list with one entry for the current sort column or an empty list.
    */
   @Override
   public List<Integer> getSortedColumnIndexes() {
      final List<Integer> indexes = new ArrayList<>();
      if (_currentSortColumn > -1) {
         indexes.add(Integer.valueOf(_currentSortColumn));
      }
      return indexes;
   }

   /**
    * @return 0 as we currently don't support multiple column sorting.
    */
   @Override
   public int getSortOrder(final int columnIndex) {
      return 0;
   }

   /**
    * @return TRUE if the column with the given index is sorted at the
    *         moment.
    */
   @Override
   public boolean isColumnIndexSorted(final int columnIndex) {
      return _sorted[columnIndex];
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

      if (!isColumnIndexSorted(columnIndex)) {
         clear();
      }

      SortDirectionEnum sortDirectionAdjusted = sortDirection;

      if (sortDirection.equals(SortDirectionEnum.NONE)) {

         // we don't support NONE as user action
         sortDirectionAdjusted = SortDirectionEnum.ASC;
      }

      _sortDirections[columnIndex] = sortDirectionAdjusted;
      _sorted[columnIndex] = sortDirectionAdjusted.equals(SortDirectionEnum.NONE) ? false : true;

      _currentSortColumn = columnIndex;
      _currentSortDirection = sortDirectionAdjusted;

      final ArrayList<ColumnDefinition> allColumns = _columnManager.getVisibleAndSortedColumns();
      final String sortColumnId = allColumns.get(columnIndex).getColumnId();

      // setup the data loader with the new sorting field/direction
      _dataLoader.setupSortColumn(sortColumnId, sortDirectionAdjusted);
   }

}
