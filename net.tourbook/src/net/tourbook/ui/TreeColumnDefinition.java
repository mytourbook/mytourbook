/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import org.eclipse.swt.widgets.TreeColumn;

/**
 * A ColumnDefinition contains the data for creating a column in a TableViewer
 */
public class TreeColumnDefinition extends ColumnDefinition {

	private TreeColumn	fTreeColumn;

	/**
	 * @param columnManager
	 *        manager which managed the columns
	 * @param columnId
	 *        column id which must be unique within the tree
	 * @param style
	 *        ui style
	 */
	public TreeColumnDefinition(ColumnManager columnManager, String columnId, int style) {
		columnManager.addColumn(this);
		fColumnId = columnId;
		fStyle = style;
	}

	public void setTreeColumn(TreeColumn tableColumn) {
		fTreeColumn = tableColumn;
	}

	public TreeColumn getTreeColumn() {
		return fTreeColumn;
	}

}
