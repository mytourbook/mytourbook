/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
package net.tourbook.common.util;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;

/**
 * Column: This column is invisible and is a HACK
 *
 * In order for the first visible column to be right aligned, an empty invisible
 * column must be created as the first column is ALWAYS left aligned.
 */
public class ColumnDefinitionFor1stVisibleAlignmentColumn extends TableColumnDefinition {

   public static final String COLUMN_ID = "1stVisibleColumnAlignment"; //$NON-NLS-1$

   /**
    * @param columnManager
    *           Manager which managed the columns.
    */
   public ColumnDefinitionFor1stVisibleAlignmentColumn(final ColumnManager columnManager) {

      super(columnManager, COLUMN_ID, SWT.TRAIL);

      setDefaultColumnWidth(0);

      setColumnHeaderText(UI.EMPTY_STRING);
      setColumnLabel(Messages.FirstColumn_AlignmentHack_Label);
      setColumnHeaderToolTipText(Messages.FirstColumn_AlignmentHack_Label_Tooltip);

      setColumnCategory(Messages.ColumnFactory_Category_Quirky);

      setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(UI.EMPTY_STRING);
         }
      });
   }
}
