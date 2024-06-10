/*******************************************************************************
 * Copyright (C) 2020, 2024 Wolfgang Schramm and Contributors
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
import net.tourbook.common.util.ColumnManager;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

public class NatTable_Header_Tooltip extends NatTableContentTooltip {

   private static final String NL = UI.NEW_LINE1;

   private TourBookView        _tourBookView;

   public NatTable_Header_Tooltip(final NatTable natTable, final TourBookView tourBookView) {

// The 4th parameter is necessary otherwise the build process fails !!!
//
//[ERROR] Failed to execute goal org.eclipse.tycho:tycho-compiler-plugin:1.4.0:compile (default-compile) on project net.tourbook: Compilation failure: Compilation failure:
//[ERROR] C:\DAT\mytourbook-BUILD-autocreated\core\net.tourbook\src\net\tourbook\\ui\views\tourBook\natTable\NatTable_Header_Tooltip.java:[42]
//[ERROR]         super(natTable, ToolTip.NO_RECREATE, false);
//[ERROR]         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//[ERROR] The constructor NatTableContentTooltip(NatTable, int, boolean) is undefined

      super(natTable, UI.EMPTY_STRING);

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

      final boolean isShowDebugInfo = false;

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
                  sb.append(NL);
               }

               sb.append(columnLabel + UI.DASH_WITH_SPACE + getSortDirectionText(sortDirection));
            }

            if (isShowDebugInfo) {
               sb.append(natTableDebugInfo(event));
            }

            final String sortColumnTooltip = String.format(Messages.Tour_Book_SortColumnTooltip, sb.toString());
            if (defaultToolTipText == null) {

               return sortColumnTooltip;

            } else {

               return defaultToolTipText + UI.NEW_LINE2 + sortColumnTooltip;
            }

         } else {

            // hovered column is not sorted

            final ColumnManager columnMgr = _tourBookView.getNatTable_ColumnManager();
            final boolean isShowSortingInHeader = columnMgr.isShowColumnAnnotation_Sorting();

            final String debugInfo = isShowDebugInfo ? natTableDebugInfo(event) : UI.EMPTY_STRING;

            String sortInfo;

            if (colDef.canSortColumn()) {
               sortInfo = Messages.Column_SortInfo_CanSort;
            } else {
               sortInfo = Messages.Column_SortInfo_CanNotSort;
            }

            if (isShowSortingInHeader) {

               // the header is already showing the sorting info, do not show it again in the header tooltip

               if (defaultToolTipText == null) {

                  return sortInfo + debugInfo;

               } else {

                  return defaultToolTipText + debugInfo;
               }

            } else {

               // the header is not showing the sorting info

               if (defaultToolTipText == null) {

                  return sortInfo + debugInfo;

               } else {

                  return defaultToolTipText
                        + UI.NEW_LINE2 + sortInfo
                        + debugInfo;
               }
            }
         }
      }

      return null;
   }

   private boolean isHoveredColumnAlsoSorted(final String hoveredColumnId) {

      final NatTable_SortModel sortModel = _tourBookView.getNatTableLayer_SortModel();
      final List<Integer> allSortedColumnIndexes = sortModel.getSortedColumnIndexes();
      final ArrayList<ColumnDefinition> allColumnDefs = _tourBookView.getNatTable_ColumnManager().getVisibleAndSortedColumns();

      for (final Integer sortedIndex : allSortedColumnIndexes) {

         final String sortedColumnId = allColumnDefs.get(sortedIndex).getColumnId();

         if (hoveredColumnId.equals(sortedColumnId)) {
            return true;
         }
      }

      return false;
   }

   private String natTableDebugInfo(final Event event) {

      final int columnPosition = natTable.getColumnPositionByX(event.x);
      final int rowPosition = natTable.getRowPositionByY(event.y);

      final String msg = NL + NL

            + "NatTable DEBUG INFO" + NL //$NON-NLS-1$
            + "==============" + NL + NL //$NON-NLS-1$

            + "Display mode    " + natTable.getDisplayModeByPosition(columnPosition, rowPosition) + NL //$NON-NLS-1$

            + NL

            + "Config labels    " + natTable.getConfigLabelsByPosition(columnPosition, rowPosition) + NL //$NON-NLS-1$
            + "Data value        " + natTable.getDataValueByPosition(columnPosition, rowPosition) + NL //$NON-NLS-1$

            + NL

            + "Column position  " + columnPosition + NL //$NON-NLS-1$
            + "Column index      " + natTable.getColumnIndexByPosition(columnPosition) + NL //$NON-NLS-1$

            + NL

            + "Row position      " + rowPosition + NL //$NON-NLS-1$
            + "Row index           " + natTable.getRowIndexByPosition(rowPosition); //$NON-NLS-1$

      return msg;
   }
}
