/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import net.tourbook.util.StringToArrayConverter;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IMemento;

/**
 * Manages the columns for a tree/table-viewer
 * <p>
 * created: 2007-05-27 by Wolfgang Schramm
 */
public class ColumnManager {

	/**
	 * minimum column width when the column width is 0, there was an bug that this happened
	 */
	private static final int			MINIMUM_COLUMN_WIDTH			= 7;

	private static final String			MEMENTO_COLUMN_SORT_ORDER		= "column_sort_order";					//$NON-NLS-1$
	private static final String			MEMENTO_COLUMN_WIDTH			= "column_width";						//$NON-NLS-1$

	private ITourViewer					fTourViewer;

	/**
	 * column definitions for all columns which are defined for the viewer
	 */
	private ArrayList<ColumnDefinition>	fAllDefinedColumnDefinitions	= new ArrayList<ColumnDefinition>();

	/**
	 * contains the column definitions for the visible columns in the sort order of the table/tree
	 */
	private ArrayList<ColumnDefinition>	fVisibleColumnDefinitions;

	/**
	 * contains the column ids which are visible in the viewer
	 */
	private String[]					fVisibleColumnIds;

	/**
	 * contains a pair of column id and width for each column
	 */
	private String[]					fColumnIdsAndWidth;

	public ColumnManager(final ITourViewer viewerAdapter, final IMemento memento) {

		fTourViewer = viewerAdapter;

		restoreState(memento);
	}

	public void addColumn(final ColumnDefinition colDef) {
		fAllDefinedColumnDefinitions.add(colDef);
	}

	/**
	 * Removes all defined columns
	 */
	public void clearColumns() {
		fAllDefinedColumnDefinitions.clear();
	}

	/**
	 * Create columns for all defined columns
	 */
	public void createColumns() {

		setVisibleColumnDefinitions();

		final ColumnViewer viewer = fTourViewer.getViewer();

		if (viewer instanceof TableViewer) {

			// create all columns in the table

			for (final ColumnDefinition colDef : fVisibleColumnDefinitions) {
				createTableColumn((TableColumnDefinition) colDef, (TableViewer) viewer);
			}

		} else if (viewer instanceof TreeViewer) {

			// create all columns in the tree

			for (final ColumnDefinition colDef : fVisibleColumnDefinitions) {
				createTreeColumn((TreeColumnDefinition) colDef, (TreeViewer) viewer);
			}
		}

	}

	/**
	 * Creates a column in a table viewer
	 * 
	 * @param colDef
	 * @param tableViewer
	 */
	private void createTableColumn(final TableColumnDefinition colDef, final TableViewer tableViewer) {

		TableViewerColumn tvc;
		TableColumn tc;

		tvc = new TableViewerColumn(tableViewer, colDef.getColumnStyle());

		final CellLabelProvider cellLabelProvider = colDef.getCellLabelProvider();
		if (cellLabelProvider != null) {
			tvc.setLabelProvider(cellLabelProvider);
		}

		tvc.setEditingSupport(colDef.getEditingSupport());
		
		tc = tvc.getColumn();

		final String columnText = colDef.getColumnText();
		if (columnText != null) {
			tc.setText(columnText);
		}

		final String columnToolTipText = colDef.getColumnToolTipText();
		if (columnToolTipText != null) {
			tc.setToolTipText(columnToolTipText);
		}

		/*
		 * set column width
		 */
		int columnWidth = colDef.getColumnWidth();
		if (colDef.isColumnHidden()) {
			columnWidth = 0;
		} else {
			columnWidth = columnWidth < MINIMUM_COLUMN_WIDTH ? colDef.getDefaultColumnWidth() : columnWidth;
		}
		tc.setWidth(columnWidth);

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
	private void createTreeColumn(final TreeColumnDefinition colDef, final TreeViewer treeViewer) {

		TreeViewerColumn tvc;
		TreeColumn tc;

		tvc = new TreeViewerColumn(treeViewer, colDef.getColumnStyle());

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

		/*
		 * set column width
		 */
		int columnWidth = colDef.getColumnWidth();
		if (colDef.isColumnHidden()) {
			columnWidth = 0;
		} else {
			columnWidth = columnWidth < MINIMUM_COLUMN_WIDTH ? colDef.getDefaultColumnWidth() : columnWidth;
		}
		tc.setWidth(columnWidth);

		tc.setResizable(colDef.isColumnResizable());
		tc.setMoveable(colDef.isColumnMoveable());

		// keep reference to the column definition
		tc.setData(colDef);
		colDef.setTreeColumn(tc);

		final SelectionAdapter columnSelectionListener = colDef.getColumnSelectionListener();
		if (columnSelectionListener != null) {
			tc.addSelectionListener(columnSelectionListener);
		}
	}

	/**
	 * @param columnId
	 *            column id
	 * @return Returns the column definition for the column id, or <code>null</code> when the column
	 *         for the column id is not available
	 */
	private ColumnDefinition getColumnDefinitionByColumnId(final String columnId) {
		for (final ColumnDefinition colDef : fAllDefinedColumnDefinitions) {
			if (colDef.getColumnId().compareTo(columnId) == 0) {
				return colDef;
			}
		}
		return null;
	}

	/**
	 * @param orderIndex
	 *            column create id
	 * @return Returns the column definition for the column create index, or <code>null</code> when
	 *         the column is not available
	 */
	private ColumnDefinition getColumnDefinitionByCreateIndex(final int orderIndex) {
		for (final ColumnDefinition colDef : fVisibleColumnDefinitions) {
			if (colDef.getCreateIndex() == orderIndex) {
				return colDef;
			}
		}
		return null;
	}

	/**
	 * @return Returns the columns in the format: id/width ...
	 */
	private String[] getColumnIdAndWidthFromViewer() {

		final ArrayList<String> columnIdsAndWidth = new ArrayList<String>();

		final ColumnViewer columnViewer = fTourViewer.getViewer();

		if (columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}

			for (final TableColumn column : table.getColumns()) {

				final String columnId = ((TableColumnDefinition) column.getData()).getColumnId();
				final int columnWidth = column.getWidth();

				setColumnIdAndWidth(columnIdsAndWidth, columnId, columnWidth);
			}

		} else if (columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) columnViewer).getTree();
			if (tree.isDisposed()) {
				return null;
			}

			for (final TreeColumn column : tree.getColumns()) {

				final String columnId = ((TreeColumnDefinition) column.getData()).getColumnId();
				final int columnWidth = column.getWidth();

				setColumnIdAndWidth(columnIdsAndWidth, columnId, columnWidth);
			}
		}

		return columnIdsAndWidth.toArray(new String[columnIdsAndWidth.size()]);
	}

	/**
	 * Read the column order from a table and set {@link ColumnManager#fColumns}
	 */
	private String[] getColumnIdsFromViewer() {

		final ArrayList<String> orderedColumnIds = new ArrayList<String>();

		int[] columnOrder = null;
		final ColumnViewer columnViewer = fTourViewer.getViewer();

		if (columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}
			columnOrder = table.getColumnOrder();

		} else if (columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) columnViewer).getTree();
			if (tree.isDisposed()) {
				return null;
			}
			columnOrder = tree.getColumnOrder();
		}

		if (columnOrder == null) {
			return null;
		}

		// create columns in the correct sort order
		for (final int createIndex : columnOrder) {

			final ColumnDefinition colDef = getColumnDefinitionByCreateIndex(createIndex);

			if (colDef != null) {
				orderedColumnIds.add(colDef.getColumnId());
			}
		}

		return orderedColumnIds.toArray(new String[orderedColumnIds.size()]);
	}

	private int getColumnWidth(final String columnWidthId) {

		for (int columnIndex = 0; columnIndex < fColumnIdsAndWidth.length; columnIndex++) {
			final String columnId = fColumnIdsAndWidth[columnIndex];

			if (columnWidthId.equals(columnId)) {
				try {
					return Integer.parseInt(fColumnIdsAndWidth[++columnIndex]);
				} catch (final Exception e) {
					// ignore format exception
				}
			}

			// skip width, advance to next id
			columnIndex++;
		}

		return 0;
	}

	/**
	 * Read the order/width for the columns, this is necessary because the user can have rearranged
	 * the columns and/or resized the columns with the mouse
	 * 
	 * @return Returns all columns which are displayed in the {@link ColumnModifyDialog}
	 */
	private ArrayList<ColumnDefinition> getDialogColumns() {

		final ArrayList<ColumnDefinition> allColumnsClone = new ArrayList<ColumnDefinition>();

		try {
			for (final ColumnDefinition definedColDef : fAllDefinedColumnDefinitions) {
				allColumnsClone.add((ColumnDefinition) definedColDef.clone());
			}
		} catch (final CloneNotSupportedException e) {
			e.printStackTrace();
		}

		final ArrayList<ColumnDefinition> allDialogColumns = new ArrayList<ColumnDefinition>();

		final ColumnViewer columnViewer = fTourViewer.getViewer();
		int[] columnOrder = null;

		/*
		 * get column order from viewer
		 */
		if (columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}
			columnOrder = table.getColumnOrder();

		} else if (columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) columnViewer).getTree();
			if (tree.isDisposed()) {
				return null;
			}
			columnOrder = tree.getColumnOrder();
		}

		/*
		 * add columns in the sort order of the modify dialog
		 */
		for (final int createIndex : columnOrder) {

			final ColumnDefinition colDef = getColumnDefinitionByCreateIndex(createIndex);
			if (colDef != null) {

				// check all visible columns in the dialog
				colDef.setIsCheckedInDialog(true);

				// set column width
				colDef.setColumnWidth(getColumnWidth(colDef.fColumnId));

				// keep the column
				allDialogColumns.add(colDef);

				allColumnsClone.remove(colDef);
			}
		}

		/*
		 * add columns which are defined but not visible
		 */
		for (final ColumnDefinition colDef : allColumnsClone) {

			// uncheck hidden columns
			colDef.setIsCheckedInDialog(false);

			// set column default width
			colDef.setColumnWidth(colDef.getDefaultColumnWidth());

			allDialogColumns.add(colDef);
		}

		return allDialogColumns;
	}

	public void openColumnDialog() {

		// get the sorting order and column width from the viewer
		fVisibleColumnIds = getColumnIdsFromViewer();
		fColumnIdsAndWidth = getColumnIdAndWidthFromViewer();

		(new ColumnModifyDialog(Display.getCurrent().getActiveShell(),
				this,
				getDialogColumns(),
				fAllDefinedColumnDefinitions)).open();
	}

	/**
	 * Restore the column order and width from a memento
	 * 
	 * @param memento
	 */
	private void restoreState(final IMemento memento) {

		if (memento == null) {
			return;
		}

		// restore table columns sort order
		final String mementoColumnSortOrderIds = memento.getString(MEMENTO_COLUMN_SORT_ORDER);
		if (mementoColumnSortOrderIds != null) {
			fVisibleColumnIds = StringToArrayConverter.convertStringToArray(mementoColumnSortOrderIds);
		}

		// restore column width
		final String mementoColumnWidth = memento.getString(MEMENTO_COLUMN_WIDTH);
		if (mementoColumnWidth != null) {
			fColumnIdsAndWidth = StringToArrayConverter.convertStringToArray(mementoColumnWidth);
		}
	}

	/**
	 * Save the column order and width into a memento
	 * 
	 * @param memento
	 */
	public void saveState(final IMemento memento) {

		if (memento == null) {
			return;
		}

		// save column sort order
		fVisibleColumnIds = getColumnIdsFromViewer();
		if (fVisibleColumnIds != null) {
			memento.putString(MEMENTO_COLUMN_SORT_ORDER, StringToArrayConverter.convertArrayToString(fVisibleColumnIds));
		}

		// save columns width and keep it for internal use
		fColumnIdsAndWidth = getColumnIdAndWidthFromViewer();
		if (fColumnIdsAndWidth != null) {
			memento.putString(MEMENTO_COLUMN_WIDTH, StringToArrayConverter.convertArrayToString(fColumnIdsAndWidth));
		}
	}

	private void setColumnIdAndWidth(final ArrayList<String> columnIdsAndWidth, final String columnId, int columnWidth) {

		final ColumnDefinition colDef = getColumnDefinitionByColumnId(columnId);
		if (colDef.isColumnHidden()) {

			// column is hidden

			columnWidth = 0;

		} else {

			// column is visible

			if (columnWidth == 0) {
				columnWidth = MINIMUM_COLUMN_WIDTH;
			}
		}

		columnIdsAndWidth.add(columnId);
		columnIdsAndWidth.add(Integer.toString(columnWidth));
	}

	/**
	 * Set the columns in {@link #fVisibleColumnDefinitions} to the order of the
	 * <code>tableItems</code> in the {@link ColumnModifyDialog}
	 * 
	 * @param tableItems
	 */
	private void setColumnIdsFromModifyDialog(final TableItem[] tableItems) {

		final ArrayList<String> visibleColumnIds = new ArrayList<String>();
		final ArrayList<String> columnIdsAndWidth = new ArrayList<String>();

		// recreate columns in the correct sort order
		for (final TableItem tableItem : tableItems) {

			if (tableItem.getChecked()) {

				// data in the table item contains the input items for the viewer
				final ColumnDefinition colDef = (ColumnDefinition) tableItem.getData();

				// set the visible columns 
				visibleColumnIds.add(colDef.getColumnId());

				// set column id and width
				columnIdsAndWidth.add(colDef.getColumnId());
				columnIdsAndWidth.add(Integer.toString(colDef.getColumnWidth()));
			}
		}

		fVisibleColumnIds = visibleColumnIds.toArray(new String[visibleColumnIds.size()]);
		fColumnIdsAndWidth = columnIdsAndWidth.toArray(new String[columnIdsAndWidth.size()]);
	}

//	/**

	/**
	 * Set the visible column definitions from the visible ids
	 */
	private void setVisibleColumnDefinitions() {

		fVisibleColumnDefinitions = new ArrayList<ColumnDefinition>();

		if (fVisibleColumnIds != null) {

			// create columns with the correct sort order

			int createIndex = 0;

			for (final String columnId : fVisibleColumnIds) {

				final ColumnDefinition colDef = getColumnDefinitionByColumnId(columnId);
				if (colDef != null) {

					colDef.setCreateIndex(createIndex++);

					fVisibleColumnDefinitions.add(colDef);
				}
			}
		}

		if (fColumnIdsAndWidth != null) {

			// set the width for all columns

			for (int dataIdx = 0; dataIdx < fColumnIdsAndWidth.length; dataIdx++) {

				final String columnId = fColumnIdsAndWidth[dataIdx++];
				final int columnWidth = Integer.valueOf(fColumnIdsAndWidth[dataIdx]);

				final ColumnDefinition colDef = getColumnDefinitionByColumnId(columnId);
				if (colDef != null) {
					colDef.setColumnWidth(columnWidth);
				}
			}
		}

		/*
		 * when no columns are visible (which is the first time), show only the default columns
		 * because every column reduces performance
		 */
		if (fVisibleColumnDefinitions.size() == 0 && fAllDefinedColumnDefinitions.size() > 0) {

			final ArrayList<String> columnIds = new ArrayList<String>();
			int createIndex = 0;

			for (final ColumnDefinition columnDef : fAllDefinedColumnDefinitions) {
				if (columnDef.isDefaultColumn()) {

					columnDef.setCreateIndex(createIndex++);

					fVisibleColumnDefinitions.add(columnDef);
					columnIds.add(columnDef.getColumnId());
				}
			}

			fVisibleColumnIds = columnIds.toArray(new String[columnIds.size()]);
		}

		/*
		 * when no default columns are set, use the first column
		 */
		if (fVisibleColumnDefinitions.size() == 0 && fAllDefinedColumnDefinitions.size() > 0) {

			final ColumnDefinition firstColumn = fAllDefinedColumnDefinitions.get(0);
			firstColumn.setCreateIndex(0);

			fVisibleColumnDefinitions.add(firstColumn);

			fVisibleColumnIds = new String[1];
			fVisibleColumnIds[0] = firstColumn.getColumnId();
		}
	}

	/**
	 * Update the viewer with the columns from the {@link ColumnModifyDialog}
	 * 
	 * @param tableItems
	 *            table item in the {@link ColumnModifyDialog}
	 */
	void updateColumns(final TableItem[] tableItems) {

		setColumnIdsFromModifyDialog(tableItems);

		fTourViewer.recreateViewer();
	}

}
