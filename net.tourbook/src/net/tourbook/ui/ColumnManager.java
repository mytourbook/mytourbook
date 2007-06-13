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

	private final TableViewer			fTableViewer;

	/**
	 * contains the column definitions in the order of the table in <code>fTableViewer</code>
	 */
	private ArrayList<ColumnDefinition>	fColumns			= new ArrayList<ColumnDefinition>();

	private int							columnCreateIndex	= 0;

	public ColumnManager(final TableViewer tableViewer) {
		fTableViewer = tableViewer;
	}

	protected void addColumn(final ColumnDefinition colDef) {
		colDef.setCreateIndex(columnCreateIndex++);
		fColumns.add(colDef);
	}

	/**
	 * Creates a column in the table viewer
	 * 
	 * @param colDef
	 */
	private void createColumn(final ColumnDefinition colDef) {

		TableViewerColumn tvc;
		TableColumn tableColumn;

		tvc = new TableViewerColumn(fTableViewer, colDef.getStyle());

		final CellLabelProvider cellLabelProvider = colDef.getCellLabelProvider();
		if (cellLabelProvider != null) {
			tvc.setLabelProvider(cellLabelProvider);
		}

		tableColumn = tvc.getColumn();

		final String columnText = colDef.getColumnText();
		if (columnText != null) {
			tableColumn.setText(columnText);
		}

		final String columnToolTipText = colDef.getColumnToolTipText();
		if (columnToolTipText != null) {
			tableColumn.setToolTipText(columnToolTipText);
		}

		// all columns are created, but hidden columns width is 0
		if (colDef.isVisible()) {
			tableColumn.setWidth(colDef.getColumnWidth());
		} else {
			tableColumn.setWidth(0);
		}

		tableColumn.setResizable(colDef.isColumnResizable());
		tableColumn.setMoveable(colDef.isColumnMoveable());

		// keep reference to the column definition
		tableColumn.setData(colDef);
		colDef.setTableColumn(tableColumn);

		final SelectionAdapter columnSelectionListener = colDef.getColumnSelectionListener();
		if (columnSelectionListener != null) {
			tableColumn.addSelectionListener(columnSelectionListener);
		}

//		colDef.setVisible(true);
	}

	/**
	 * Create columns for all defined columns
	 */
	public void createColumns() {

		// create all columns in the table
		for (final ColumnDefinition colDef : fColumns) {
			createColumn(colDef);
		}
	}

//	/**
//	 * Create columns which contain the column id's
//	 * 
//	 * @param columnIds
//	 */
//	public void createColumns(final int[] columnIds) {
//
//		final int[] columnIdAndWidth = getColumnWidths();
//
//		// create new columns
//		for (final int columnId : columnIds) {
//			final ColumnDefinition colDef = getColumnDefinition(columnId);
//			if (colDef != null) {
//				createColumn(colDef);
//			}
//		}
//
//		setColumnWidth(columnIdAndWidth);
//	}

	/**
	 * @param columnId
	 *        column id
	 * @return Returns the column definition for the column id, or <code>null</code> when the
	 *         column for the column id is not available
	 */
	private ColumnDefinition getColumnDefinitionByColumnId(final int columnId) {
		for (final ColumnDefinition colDef : fColumns) {
			if (colDef.getColumnId() == columnId) {
				return colDef;
			}
		}
		return null;
	}

	/**
	 * @param orderIndex
	 *        column create id
	 * @return Returns the column definition for the column create index, or <code>null</code>
	 *         when the column is not available
	 */
	private ColumnDefinition getColumnDefinitionByCreateIndex(final int orderIndex) {
		for (final ColumnDefinition colDef : fColumns) {
			if (colDef.getCreateIndex() == orderIndex) {
				return colDef;
			}
		}
		return null;
	}

	/**
	 * @return Returns the column which are managed by the <code>ColumnManager</code>
	 */
	public ArrayList<ColumnDefinition> getColumns() {
		return fColumns;
	}

//	/**
//	 * @return Returns the columnId/columnWidth pair for all columns in the table
//	 */
//	private int[] getColumnWidths() {
//
//		final Table table = fTableViewer.getTable();
//		final TableColumn[] columns = table.getColumns();
//
//		final int[] columnIdAndWidth = new int[columns.length * 2];
//		int columIdx = 0;
//
//		for (final TableColumn column : columns) {
//
//			final ColumnDefinition colDef = (ColumnDefinition) column.getData();
//
//			columnIdAndWidth[columIdx++] = colDef.getColumnId();
//			columnIdAndWidth[columIdx++] = column.getWidth();
//		}
//
//		return columnIdAndWidth;
//	}

	public TableViewer getTableViewer() {
		return fTableViewer;
	}

	public void openColumnDialog() {

		/*
		 * read the order/width for the columns, this is necessary because the user can have
		 * rearranged the columns or resize the columns with the mouse
		 */
		readColumnsFromTable(fTableViewer.getTable());

		final int returnValue = (new ColumnModifyDialog(Display.getCurrent().getActiveShell(), this))
				.open();

		if (returnValue == Window.OK) {

			for (final ColumnDefinition colDef : fColumns) {

				// copy the visibility status from the dialog into the column definition
				final boolean isVisible = colDef.isVisibleInDialog();
				colDef.setVisible(isVisible);

				// show/hide column in the table
				final TableColumn tableColumn = colDef.getTableColumn();
				if (isVisible) {
					tableColumn.setWidth(colDef.getColumnWidth());
					tableColumn.setResizable(colDef.isColumnResizable());
				} else {
					tableColumn.setWidth(0);
					tableColumn.setResizable(false);
				}
			}
			orderColumnsInTable();
		}
	}

	@SuppressWarnings("unchecked")
	public void orderColumns(final int[] columnIds) {

		final ArrayList<ColumnDefinition> orderedColumns = new ArrayList<ColumnDefinition>();
		final ArrayList<ColumnDefinition> deletedColumns = (ArrayList<ColumnDefinition>) fColumns
				.clone();

		// create columns in the correct sort order
		for (final int columnId : columnIds) {
			final ColumnDefinition colDef = getColumnDefinitionByColumnId(columnId);
			if (colDef != null) {
				orderedColumns.add(colDef);
				deletedColumns.remove(colDef);
			}
		}

		// add all columns which are not sorted but available
		for (final ColumnDefinition colDef : deletedColumns) {
			orderedColumns.add(colDef);
		}

		// set new column order
		fColumns = orderedColumns;

		orderColumnsInTable();
	}

	/**
	 * Sort the column definition list in the order of the <code>tableItems</code>
	 * 
	 * @param tableItems
	 */
	@SuppressWarnings("unchecked")
	void orderColumns(final TableItem[] tableItems) {

		final ArrayList<ColumnDefinition> sortedColumns = new ArrayList<ColumnDefinition>();
		final ArrayList<ColumnDefinition> deletedColumns = (ArrayList<ColumnDefinition>) fColumns
				.clone();

		// secreate columns in the correct sort order
		for (final TableItem tableItem : tableItems) {

			final ColumnDefinition colDef = (ColumnDefinition) tableItem.getData();

			sortedColumns.add(colDef);
			deletedColumns.remove(colDef);
		}

		// add all columns which are not sorted but are available
		for (final ColumnDefinition colDef : deletedColumns) {
			sortedColumns.add(colDef);
		}

		// set new column order
		fColumns = sortedColumns;

		orderColumnsInTable();
	}

	/**
	 * order the columns in the table by the order of the columns in <code>fColumns</code>
	 */
	private void orderColumnsInTable() {

		final int[] columnOrder = new int[fColumns.size()];
		int columnIdx = 0;

		for (ColumnDefinition colDef : fColumns) {
			columnOrder[columnIdx++] = colDef.getCreateIndex();
		}
		fTableViewer.getTable().setColumnOrder(columnOrder);
	}

	/**
	 * Read the column order from a table and set <code>fColumn</code> according to this order
	 * 
	 * @param table
	 */
	private void readColumnsFromTable(Table table) {

		final ArrayList<ColumnDefinition> orderedColumns = new ArrayList<ColumnDefinition>();

		// create columns in the correct sort order
		for (final int createIndex : fTableViewer.getTable().getColumnOrder()) {

			final ColumnDefinition colDef = getColumnDefinitionByCreateIndex(createIndex);

			if (colDef != null) {

				// set column order
				orderedColumns.add(colDef);

				// set width
				final int columnWidth = colDef.getTableColumn().getWidth();
				if (columnWidth > 0) {
					colDef.setWidth(columnWidth);
				}
			}
		}

		// set new column order
		fColumns = orderedColumns;
	}

	/**
	 * shows all columns in the table
	 */
	public void setAllColumnsVisible() {
		for (ColumnDefinition colDef : fColumns) {
			colDef.setVisible(true);
			colDef.getTableColumn().setWidth(colDef.getColumnWidth());
		}
	}

	/**
	 * Sets the width for columns
	 * 
	 * @param columnIdAndWidth
	 *        contains the data pair: column id/column width,...
	 */
	public void setColumnWidth(final int[] columnIdAndWidth) {

		for (int dataIdx = 0; dataIdx < columnIdAndWidth.length; dataIdx++) {

			final int columnId = columnIdAndWidth[dataIdx++];
			final int columnWidth = columnIdAndWidth[dataIdx];

			final ColumnDefinition colDef = getColumnDefinitionByColumnId(columnId);
			if (colDef != null) {
				colDef.getTableColumn().setWidth(columnWidth);

				if (columnWidth != 0) {
					colDef.setVisible(true);
				}
			}
		}
	}

}
