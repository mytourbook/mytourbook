/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * A ColumnDefinition contains the data for creating a column in a {@link TreeViewer}
 */
public class TreeColumnDefinition extends ColumnDefinition {

   private TreeColumn _treeColumn;

   /**
    * @param columnManager
    *           Manager which managed the columns.
    * @param columnId
    *           Column id which must be unique within the table.
    * @param style
    *           UI style.
    */
   public TreeColumnDefinition(final ColumnManager columnManager, final String columnId, final int style) {

      super(columnId, style);

      columnManager.addColumn(this);
   }

   public TreeColumn getTreeColumn() {
      return _treeColumn;
   }

   public void setTreeColumn(final TreeColumn tableColumn) {
      _treeColumn = tableColumn;
   }

}
