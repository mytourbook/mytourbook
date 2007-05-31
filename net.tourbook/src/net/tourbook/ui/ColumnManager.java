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

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Manages the columns for a table viewer
 * <p>
 * created: 2007-05-27 by Wolfgang Schramm
 */
public class ColumnManager {

	private TableViewer					fTableViewer;

	private ArrayList<ColumnDefinition>	fColumns	= new ArrayList<ColumnDefinition>();

	public ColumnManager(TableViewer tableViewer) {
		fTableViewer = tableViewer;
	}

	protected void addColumn(ColumnDefinition columnDefinition) {
		fColumns.add(columnDefinition);
	}

	/**
	 * Creates a column in the table viewer
	 * 
	 * @param colDef
	 */
	private void createColumn(ColumnDefinition colDef) {

		TableViewerColumn tvc;
		TableColumn column;

		tvc = new TableViewerColumn(fTableViewer, colDef.getStyle());

		final CellLabelProvider cellLabelProvider = colDef.getCellLabelProvider();
		if (cellLabelProvider != null) {
			tvc.setLabelProvider(cellLabelProvider);
		}

		column = tvc.getColumn();

		final String columnText = colDef.getColumnText();
		if (columnText != null) {
			column.setText(columnText);
		}

		final String columnToolTipText = colDef.getColumnToolTipText();
		if (columnToolTipText != null) {
			column.setToolTipText(columnToolTipText);
		}

		column.setWidth(colDef.getColumnWidth());
		column.setResizable(colDef.isColumnResizable());

		// keep reference to the column definition
		column.setData(colDef);
		colDef.setTableColumn(column);

		final SelectionAdapter columnSelectionListener = colDef.getColumnSelectionListener();
		if (columnSelectionListener != null) {
			column.addSelectionListener(columnSelectionListener);
		}

		colDef.setVisible(true);
	}

	/**
	 * Create columns for all defined columns
	 */
	public void createColumns() {

		removeAllColumns();

		// add all columns to the table viewer
		for (ColumnDefinition colDef : fColumns) {
			if (colDef.isVisible()) {
				createColumn(colDef);
			}
		}
	}

	/**
	 * Create columns which contain the column id's
	 * 
	 * @param columnIds
	 */
	public void createColumns(int[] columnIds) {

		removeAllColumns();

		for (int columnId : columnIds) {

			ColumnDefinition colDef = getColumnDefinition(columnId);
			if (colDef != null) {
				createColumn(colDef);
			}
		}
	}

	/**
	 * @param columnId
	 *        column id
	 * @return Returns the column definition for the column id, or <code>null</code> when the
	 *         column for the column id is not available
	 */
	private ColumnDefinition getColumnDefinition(int columnId) {
		for (ColumnDefinition colDef : fColumns) {
			if (colDef.getColumnId() == columnId) {
				return colDef;
			}
		}
		return null;
	}

	public ArrayList<ColumnDefinition> getColumns() {
		return fColumns;
	}

	public TableViewer getTableViewer() {
		return fTableViewer;
	}

	public void openColumnDialog() {

		int returnValue = (new ColumnModifyDialog(Display.getCurrent().getActiveShell(), this))
				.open();

		if (returnValue == Window.OK) {

			// copy the visibility status from the dialog into the columns
			for (ColumnDefinition colDef : fColumns) {
				final boolean isVisibleInDialog = colDef.isVisibleInDialog();
				colDef.setVisible(isVisibleInDialog);
			}
			createColumns();

			fTableViewer.refresh();
		}
	}

	/**
	 * remove all columns from the table control
	 */
	private void removeAllColumns() {

		// remove the columns from the end to the beginning, to reduce flickering
		final Table table = fTableViewer.getTable();

		for (int columnIdx = table.getColumnCount() - 1; columnIdx >= 0; columnIdx--) {
			table.getColumn(columnIdx).dispose();
		}
	}

	public void setColumnWidth(int[] columnIdAndWidth) {

		for (int columnIdx = 0; columnIdx < columnIdAndWidth.length; columnIdx++) {

			int columnId = columnIdAndWidth[columnIdx++];
			int columnWidth = columnIdAndWidth[columnIdx];

			ColumnDefinition colDef = getColumnDefinition(columnId);
			if (colDef != null) {
				colDef.getTableColumn().setWidth(columnWidth);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void sortColumns(int[] columnIds) {

		ArrayList<ColumnDefinition> sortedColumns = new ArrayList<ColumnDefinition>();
		ArrayList<ColumnDefinition> deletedColumns = (ArrayList<ColumnDefinition>) fColumns.clone();

		// create columns in the correct sort order
		for (int columnId : columnIds) {
			ColumnDefinition colDef = getColumnDefinition(columnId);
			if (colDef != null) {
				sortedColumns.add(colDef);
				deletedColumns.remove(colDef);
			}
		}

		// add all columns which are not sorted but available
		for (ColumnDefinition colDef : deletedColumns) {
			sortedColumns.add(colDef);
		}

		// set new columns
		fColumns = sortedColumns;
	}

	@SuppressWarnings("unchecked")
	public void sortColumns(TableItem[] tableItems) {

		ArrayList<ColumnDefinition> sortedColumns = new ArrayList<ColumnDefinition>();
		ArrayList<ColumnDefinition> deletedColumns = (ArrayList<ColumnDefinition>) fColumns.clone();

		// create columns in the correct sort order
		for (TableItem tableItem : tableItems) {

			ColumnDefinition colDef = (ColumnDefinition) tableItem.getData();

			sortedColumns.add(colDef);
			deletedColumns.remove(colDef);
		}

		// add all columns which are not sorted but are available
		for (ColumnDefinition colDef : deletedColumns) {
			sortedColumns.add(colDef);
		}

		// set new columns
		fColumns = sortedColumns;
	}

}
