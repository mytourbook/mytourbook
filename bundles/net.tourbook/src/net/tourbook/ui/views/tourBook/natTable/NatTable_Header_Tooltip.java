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
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

public class NatTable_Header_Tooltip extends NatTableContentTooltip {

   private TourBookView _tourBookView;

   public NatTable_Header_Tooltip(final NatTable natTable, final TourBookView tourBookView) {

      super(natTable, ToolTip.NO_RECREATE, false);

      _tourBookView = tourBookView;

      setPopupDelay(0);
      setShift(new Point(10, 10));

      activate();

      this.natTable = natTable;
      this.tooltipRegions = new String[] { GridRegion.COLUMN_HEADER };
   }

   private String getSortDirectionText(final SortDirectionEnum sortDirection) {

      switch (sortDirection) {

      case ASC:
         return Messages.App_SortDirection_Ascending;

      case DESC:
         return Messages.App_SortDirection_Descending;

      case NONE:
      default:
         return Messages.App_SortDirection_None;

      }
   }

   /**
    * {@inheritDoc}
    * <p>
    * Evaluates the cell for which the tooltip should be rendered and checks
    * the display value. If the display value is empty <code>null</code> will
    * be returned which will result in not showing a tooltip.
    */
   @Override
   protected String getText(final Event event) {

      final ColumnDefinition colDef = _tourBookView.getNatTable_SelectedColumnDefinition(event);
      if (colDef != null) {

         // column found

         final String defaultToolTipText = colDef.getColumnHeaderToolTipText();

         final String hoveredColumnId = colDef.getColumnId();

         if (isHoveredColumnAlsoSorted(hoveredColumnId)) {

            // the hovered column is also sorted -> show different column tooltip

            final NatTable_SortModel sortModel = _tourBookView.getNatTableLayer_SortModel();
            final List<Integer> allSortedColumnIndexes = sortModel.getSortedColumnIndexes();
            final ArrayList<ColumnDefinition> allColumnDefs = _tourBookView.getNatTable_ColumnManager().getVisibleAndSortedColumns();

            final StringBuilder sb = new StringBuilder();

            for (int sortedColumnIndex = 0; sortedColumnIndex < allSortedColumnIndexes.size(); sortedColumnIndex++) {

               final int columnIndex = allSortedColumnIndexes.get(sortedColumnIndex);
               final SortDirectionEnum sortDirection = sortModel.getSortDirection(columnIndex);
               final String columnLabel = allColumnDefs.get(columnIndex).getColumnLabel();

               if (sortedColumnIndex > 0) {
                  sb.append(UI.NEW_LINE);
               }

               sb.append(columnLabel + UI.DASH_WITH_SPACE + getSortDirectionText(sortDirection));
            }

            final String sortColumnTooltip = String.format(Messages.Tour_Book_SortColumnTooltip, sb.toString());
            if (defaultToolTipText == null) {

               return sortColumnTooltip;

            } else {

               return defaultToolTipText + UI.NEW_LINE2 + sortColumnTooltip;
            }

         } else {

            return defaultToolTipText;
         }
      }

      return null;
   }

   private boolean isHoveredColumnAlsoSorted(final String hoveredColumnId) {

      final NatTable_SortModel sortModel = _tourBookView.getNatTableLayer_SortModel();
      final List<Integer> _allSortedColumnIndexes = sortModel.getSortedColumnIndexes();
      final ArrayList<ColumnDefinition> allColumnDefs = _tourBookView.getNatTable_ColumnManager().getVisibleAndSortedColumns();

      for (final Integer sortedIndex : _allSortedColumnIndexes) {

         final String sortedColumnId = allColumnDefs.get(sortedIndex).getColumnId();

         if (hoveredColumnId.equals(sortedColumnId)) {
            return true;
         }
      }

      return false;
   }
}
