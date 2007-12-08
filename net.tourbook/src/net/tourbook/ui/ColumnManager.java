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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Manages the columns for a tree/table-viewer
 * <p>
 * created: 2007-05-27 by Wolfgang Schramm
 */
public class ColumnManager {

	private IAdaptable					fViewerAdapter;

	/**
	 * contains the column definitions in the sort order of the table/tree
	 */
	private ArrayList<ColumnDefinition>	fColumns			= new ArrayList<ColumnDefinition>();

	private int							columnCreateIndex	= 0;

	public ColumnManager(IAdaptable viewerAdapter) {
		fViewerAdapter = viewerAdapter;
	}

	protected void addColumn(final ColumnDefinition colDef) {
		colDef.setCreateIndex(columnCreateIndex++);
		fColumns.add(colDef);
	}

	/**
	 * Create columns for all defined columns
	 */
	public void createColumns() {

		Object adapter = fViewerAdapter.getAdapter(ColumnViewer.class);

		if (adapter instanceof TableViewer) {

			// create all columns in the table

			for (final ColumnDefinition colDef : fColumns) {
				createTableColumn((TableColumnDefinition) colDef, (TableViewer) adapter);
			}

		} else if (adapter instanceof TreeViewer) {

			// create all columns in the tree

			for (final ColumnDefinition colDef : fColumns) {
				createTreeColumn((TreeColumnDefinition) colDef, (TreeViewer) adapter);
			}
		}

		setAllColumnsVisible();
	}

	/**
	 * Creates a column in a table viewer
	 * 
	 * @param colDef
	 * @param tableViewer
	 */
	private void createTableColumn(final TableColumnDefinition colDef, TableViewer tableViewer) {

		TableViewerColumn tvc;
		TableColumn tc;

		tvc = new TableViewerColumn(tableViewer, colDef.getStyle());

		final CellLabelProvider cellLabelProvider = colDef.getCellLabelProvider();
		if (cellLabelProvider != null) {
			tvc.setLabelProvider(cellLabelProvider);
		}

		tc = tvc.getColumn();

		final String columnText = colDef.getColumnText();
		if (columnText != null) {
			tc.setText(columnText);
		}

		final String columnToolTipText = colDef.getColumnToolTipText();
		if (columnToolTipText != null) {
			tc.setToolTipText(columnToolTipText);
		}

		// all columns are created, but hidden columns width is 0
		if (colDef.isVisible()) {
			tc.setWidth(colDef.getColumnWidth());
		} else {
			tc.setWidth(0);
		}

		tc.setResizable(colDef.isColumnResizable());
		tc.setMoveable(colDef.isColumnMoveable());

		// keep reference to the column definition
		tc.setData(colDef);
		colDef.setTableColumn(tc);

		final SelectionAdapter columnSelectionListener = colDef.getColumnSelectionListener();
		if (columnSelectionListener != null) {
			tc.addSelectionListener(columnSelectionListener);
		}
	}

	/**
	 * Creates a column in a tree viewer
	 * 
	 * @param colDef
	 * @param treeViewer
	 */
	private void createTreeColumn(final TreeColumnDefinition colDef, TreeViewer treeViewer) {

		TreeViewerColumn tvc;
		TreeColumn tc;

		tvc = new TreeViewerColumn(treeViewer, colDef.getStyle());

		final CellLabelProvider cellLabelProvider = colDef.getCellLabelProvider();
		if (cellLabelProvider != null) {
			tvc.setLabelProvider(cellLabelProvider);
		}

		tc = tvc.getColumn();

		final String columnText = colDef.getColumnText();
		if (columnText != null) {
			tc.setText(columnText);
		}

		final String columnToolTipText = colDef.getColumnToolTipText();
		if (columnToolTipText != null) {
			tc.setToolTipText(columnToolTipText);
		}

		// all columns are created, but hidden columns width is 0
		if (colDef.isVisible()) {
			tc.setWidth(colDef.getColumnWidth());
		} else {
			tc.setWidth(0);
		}

		tc.setResizable(colDef.isColumnResizable());
		tc.setMoveable(colDef.isColumnMoveable());

		// keep reference to the column definition
		tc.setData(colDef);
		colDef.setTreeColumn(tc);

		final SelectionAdapter columnSelectionListener = colDef.getColumnSelectionListener();
		if (columnSelectionListener != null) {
			tc.addSelectionListener(columnSelectionListener);
		}

//		System.out.println("create column: " + tc);
	}

	/**
	 * @param columnId
	 *        column id
	 * @return Returns the column definition for the column id, or <code>null</code> when the
	 *         column for the column id is not available
	 */
	private ColumnDefinition getColumnDefinitionByColumnId(final String columnId) {
		for (final ColumnDefinition colDef : fColumns) {
			if (colDef.getColumnId().compareTo(columnId) == 0) {
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
	 * @return Returns the columns in the format: id/width ...
	 */
	public String[] getColumnIdAndWidth() {

		ArrayList<String> columnIds = new ArrayList<String>();

		Object adapter = fViewerAdapter.getAdapter(ColumnViewer.class);

		if (adapter instanceof TableViewer) {

			final Table table = ((TableViewer) adapter).getTable();
			if (table.isDisposed()) {
				return null;
			}

			for (TableColumn column : table.getColumns()) {
				columnIds.add(((TableColumnDefinition) column.getData()).getColumnId());
				columnIds.add(Integer.toString(column.getWidth()));
			}

		} else if (adapter instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) adapter).getTree();
			if (tree.isDisposed()) {
				return null;
			}

			for (TreeColumn column : tree.getColumns()) {
				columnIds.add(((TreeColumnDefinition) column.getData()).getColumnId());
				columnIds.add(Integer.toString(column.getWidth()));
			}
		}

		return columnIds.toArray(new String[columnIds.size()]);
	}

	/**
	 * @return Returns all column id's
	 */
	public String[] getColumnIds() {

		readColumnsFromTable();

		ArrayList<String> columnIds = new ArrayList<String>();

		for (ColumnDefinition colDef : fColumns) {
			columnIds.add(colDef.getColumnId());
		}

		return columnIds.toArray(new String[columnIds.size()]);
	}

	/**
	 * @return Returns the columns which are managed by the <code>ColumnManager</code>
	 */
	public ArrayList<ColumnDefinition> getColumns() {
		return fColumns;
	}

	public void openColumnDialog() {

		/*
		 * read the order/width for the columns, this is necessary because the user can have
		 * rearranged the columns and/or resized the columns with the mouse
		 */
		readColumnsFromTable();

		final int returnValue = (new ColumnModifyDialog(Display.getCurrent().getActiveShell(), this)).open();

		if (returnValue == Window.OK) {

			for (final ColumnDefinition colDef : fColumns) {

				// copy the visibility status from the dialog into the column definition
				final boolean isVisible = colDef.isVisibleInDialog();
				colDef.setVisible(isVisible);

				// show/hide column in the table
				if (colDef instanceof TableColumnDefinition) {
					final TableColumn tableColumn = ((TableColumnDefinition) colDef).getTableColumn();
					if (isVisible) {
						tableColumn.setWidth(colDef.getColumnWidth());
						tableColumn.setResizable(colDef.isColumnResizable());
					} else {
						tableColumn.setWidth(0);
						tableColumn.setResizable(false);
					}
				} else if (colDef instanceof TreeColumnDefinition) {
					final TreeColumn treeColumn = ((TreeColumnDefinition) colDef).getTreeColumn();
					if (isVisible) {
						treeColumn.setWidth(colDef.getColumnWidth());
						treeColumn.setResizable(colDef.isColumnResizable());
					} else {
						treeColumn.setWidth(0);
						treeColumn.setResizable(false);
					}
				}

			}
			orderColumnsInTable();
		}
	}

	/**
	 * Orders the columns in order of the column id's
	 * 
	 * @param columnIds
	 */
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	public void orderColumns(final String[] columnIds) {

		final ArrayList<ColumnDefinition> orderedColumns = new ArrayList<ColumnDefinition>();
		final ArrayList<ColumnDefinition> deletedColumns = (ArrayList<ColumnDefinition>) fColumns.clone();

		// create columns in the correct sort order
		for (final String columnId : columnIds) {
			final ColumnDefinition colDef = getColumnDefinitionByColumnId(columnId);
			if (colDef != null) {
				orderedColumns.add(colDef);
				deletedColumns.remove(colDef);
			}
		}

		// add all columns which are not sorted but are available
		for (final ColumnDefinition colDef : deletedColumns) {
			orderedColumns.add(colDef);
		}

		// set new column order
		fColumns = orderedColumns;

		orderColumnsInTable();
	}

	/**
	 * Order the columns in the order of the <code>tableItems</code>
	 * 
	 * @param tableItems
	 */
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	void orderColumns(final TableItem[] tableItems) {

		final ArrayList<ColumnDefinition> sortedColumns = new ArrayList<ColumnDefinition>();
		final ArrayList<ColumnDefinition> deletedColumns = (ArrayList<ColumnDefinition>) fColumns.clone();

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

		Object adapter = fViewerAdapter.getAdapter(ColumnViewer.class);

		if (adapter instanceof TableViewer) {
			((TableViewer) adapter).getTable().setColumnOrder(columnOrder);
		} else if (adapter instanceof TreeViewer) {
			((TreeViewer) adapter).getTree().setColumnOrder(columnOrder);
		}

	}

	/**
	 * Read the column order from a table and set {@link ColumnManager#fColumns}
	 */
	private void readColumnsFromTable() {

		final ArrayList<ColumnDefinition> orderedColumns = new ArrayList<ColumnDefinition>();

		int[] columnOrder = null;

		Object adapter = fViewerAdapter.getAdapter(ColumnViewer.class);

		if (adapter instanceof TableViewer) {
			columnOrder = ((TableViewer) adapter).getTable().getColumnOrder();
		} else if (adapter instanceof TreeViewer) {
			columnOrder = ((TreeViewer) adapter).getTree().getColumnOrder();
		}

		// create columns in the correct sort order
		for (final int createIndex : columnOrder) {

			final ColumnDefinition colDef = getColumnDefinitionByCreateIndex(createIndex);

			if (colDef != null) {

				// set column order
				orderedColumns.add(colDef);

				// set width
				if (colDef instanceof TableColumnDefinition) {
					final int columnWidth = ((TableColumnDefinition) colDef).getTableColumn()
							.getWidth();
					if (columnWidth > 0) {
						colDef.setWidth(columnWidth);
					}

				} else if (colDef instanceof TreeColumnDefinition) {

					final TreeColumn treeColumn = ((TreeColumnDefinition) colDef).getTreeColumn();

//					System.out.println("read column: " + treeColumn);
					final int columnWidth = treeColumn.getWidth();

					if (columnWidth > 0) {
						colDef.setWidth(columnWidth);
					}
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

			if (colDef instanceof TableColumnDefinition) {
				((TableColumnDefinition) colDef).getTableColumn().setWidth(colDef.getColumnWidth());
			} else if (colDef instanceof TreeColumnDefinition) {
				((TreeColumnDefinition) colDef).getTreeColumn().setWidth(colDef.getColumnWidth());
			}
		}
	}

	/**
	 * Sets the width for columns
	 * 
	 * @param columnIdAndWidth
	 *        contains the data pair: column id/column width,...
	 */
	public void setColumnWidth(final String[] columnIdAndWidth) {

		for (int dataIdx = 0; dataIdx < columnIdAndWidth.length; dataIdx++) {

			final String columnId = columnIdAndWidth[dataIdx++];
			final int columnWidth = Integer.valueOf(columnIdAndWidth[dataIdx]);

			final ColumnDefinition colDef = getColumnDefinitionByColumnId(columnId);
			if (colDef != null) {

				if (colDef instanceof TableColumnDefinition) {
					((TableColumnDefinition) colDef).getTableColumn().setWidth(columnWidth);
				} else if (colDef instanceof TreeColumnDefinition) {
					((TreeColumnDefinition) colDef).getTreeColumn().setWidth(columnWidth);
				}

				colDef.setVisible(columnWidth != 0);
			}
		}
	}

//	Object adapter = fviewerAdapter.getAdapter(ColumnViewer.class);
//
//	if (adapter instanceof TableViewer) {
//
//
//	} else if (adapter instanceof TreeViewer) {
//
//	}
}
