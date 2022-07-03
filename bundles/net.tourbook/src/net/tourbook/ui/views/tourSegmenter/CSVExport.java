/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.ui.views.tourSegmenter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class CSVExport {

   private static char         SEPARATOR = UI.TAB;

   private static final String NL        = UI.SYSTEM_NEW_LINE;

   private Table               _segmentViewerTable;

   /**
    * Write the current segment view into a CSV file
    *
    * @param segmentViewerTable
    * @param selectedFilePath
    */
   public CSVExport(final Table segmentViewerTable, final String selectedFilePath) {

      _segmentViewerTable = segmentViewerTable;

      csvExport_CurrentView(selectedFilePath);
   }

   private void csvExport_CurrentView(final String selectedFilePath) {

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(selectedFilePath)))) {

         final int[] columnOrder = _segmentViewerTable.getColumnOrder();

         //Writing the headers
         for (final int columnIndex : columnOrder) {

            final String tableColumnText = _segmentViewerTable.getColumn(columnIndex).getText();

            writer.write(tableColumnText + SEPARATOR);
         }
         writer.write(NL);

         //Writing the row data
         for (int itemIndex = 0; itemIndex < _segmentViewerTable.getItemCount(); itemIndex++) {

            final TableItem tableItem = _segmentViewerTable.getItem(itemIndex);

            for (final int columnIndex : columnOrder) {
               writer.write(tableItem.getText(columnIndex) + SEPARATOR);
            }
            writer.write(NL);
         }
      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      }
   }
}
