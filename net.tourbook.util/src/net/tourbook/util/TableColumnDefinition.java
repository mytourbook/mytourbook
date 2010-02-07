/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.util;

import org.eclipse.swt.widgets.TableColumn;

/**
 * A ColumnDefinition contains the data for creating a column in a TableViewer
 */
public class TableColumnDefinition extends ColumnDefinition {

	private TableColumn	fTableColumn;
	private int			fColumnIndex;

	/**
	 * @param columnManager
	 *            manager which managed the columns
	 * @param columnId
	 *            column id which must be unique within the table
	 * @param style
	 *            ui style
	 */
	public TableColumnDefinition(final ColumnManager columnManager, final String columnId, final int style) {

		_columnId = columnId;
		_style = style;

		columnManager.addColumn(this);
	}

	public TableColumn getTableColumn() {
		return fTableColumn;
	}

	public int getTableColumnIndex() {
		return fColumnIndex;
	}

	public void setTableColumn(final TableColumn tableColumn) {
		fTableColumn = tableColumn;
	}

	public void setTableColumnIndex(final int tcIndex) {
		fColumnIndex = tcIndex;
	}

	@Override
	public String toString() {
		return _columnId;
	}

}
