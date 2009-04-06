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

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.AbstractColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.events.ControlListener;
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

	/**
	 * minimum column width, when the column width is 0, there was a bug that this happened
	 */
	private static final int			MINIMUM_COLUMN_WIDTH			= 7;

	private static final String			MEMENTO_COLUMN_SORT_ORDER		= "column_sort_order";					//$NON-NLS-1$
	private static final String			MEMENTO_COLUMN_WIDTH			= "column_width";						//$NON-NLS-1$

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

	private AbstractColumnLayout		fColumnLayout;

	private ITourViewer					fTourViewer;

	/**
	 * viewer which is managed by this {@link ColumnManager}
	 */
	private ColumnViewer				fColumnViewer;

	public ColumnManager(final ITourViewer tourViewer, final IDialogSettings viewState) {

		fTourViewer = tourViewer;

		restoreState(viewState);
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
	 * Creates the columns in the tree/table for all defined columns
	 * 
	 * @param columnViewer
	 */
	public void createColumns(final ColumnViewer columnViewer) {

		fColumnViewer = columnViewer;

		setVisibleColumnDefinitions();

		if (columnViewer instanceof TableViewer) {

			// create all columns in the table

			for (final ColumnDefinition colDef : fVisibleColumnDefinitions) {
				createTableColumn((TableColumnDefinition) colDef, (TableViewer) columnViewer);
			}

		} else if (columnViewer instanceof TreeViewer) {

			// create all columns in the tree

			for (final ColumnDefinition colDef : fVisibleColumnDefinitions) {
				createTreeColumn((TreeColumnDefinition) colDef, (TreeViewer) columnViewer);
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

		// get column widget
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
		if (fColumnLayout == null) {

			// set the column width with pixels

			tc.setWidth(getColumnWidth(colDef));

		} else {

			// use the column layout to set the width of the columns

			final ColumnLayoutData columnLayoutData = colDef.getColumnWeightData();

			if (columnLayoutData == null) {
				try {
					throw new Exception("ColumnWeightData is not set for the column: " + colDef); //$NON-NLS-1$
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

			if (columnLayoutData instanceof ColumnPixelData) {
				final ColumnPixelData columnPixelData = (ColumnPixelData) columnLayoutData;

				// overwrite the width
				columnPixelData.width = getColumnWidth(colDef);
				fColumnLayout.setColumnData(tc, columnPixelData);
			} else {
				fColumnLayout.setColumnData(tc, columnLayoutData);
			}
		}

		tc.setResizable(colDef.isColumnResizable());
		tc.setMoveable(colDef.isColumnMoveable());

		// keep reference to the column definition
		tc.setData(colDef);

		// keep tc ref
		colDef.setTableColumn(tc);

		// keep create index
		final int tcIndex = tableViewer.getTable().getColumnCount();
		colDef.setTableColumnIndex(tcIndex);

		// add selection listener
		final SelectionAdapter columnSelectionListener = colDef.getColumnSelectionListener();
		if (columnSelectionListener != null) {
			tc.addSelectionListener(columnSelectionListener);
		}

		// add resize/move listener
		final ControlListener columnControlListener = colDef.getColumnControlListener();
		if (columnControlListener != null) {
			tc.addControlListener(columnControlListener);
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

		if (fColumnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) fColumnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}

			for (final TableColumn column : table.getColumns()) {

				final String columnId = ((ColumnDefinition) column.getData()).getColumnId();
				final int columnWidth = column.getWidth();

				setColumnIdAndWidth(columnIdsAndWidth, columnId, columnWidth);
			}

		} else if (fColumnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) fColumnViewer).getTree();
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

		if (fColumnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) fColumnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}
			columnOrder = table.getColumnOrder();

		} else if (fColumnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) fColumnViewer).getTree();
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

	private int getColumnWidth(final TableColumnDefinition colDef) {

		int columnWidth = colDef.getColumnWidth();

		if (colDef.isColumnHidden()) {
			columnWidth = 0;
		} else {
			columnWidth = columnWidth < MINIMUM_COLUMN_WIDTH ? colDef.getDefaultColumnWidth() : columnWidth;
		}

		return columnWidth;
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

		int[] columnOrder = null;

		/*
		 * get column order from viewer
		 */
		if (fColumnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) fColumnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}
			columnOrder = table.getColumnOrder();

		} else if (fColumnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) fColumnViewer).getTree();
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
	 * @param settings
	 */
	private void restoreState(final IDialogSettings settings) {

		if (settings == null) {
			return;
		}

		// restore table columns sort order
		final String mementoColumnSortOrderIds = settings.get(MEMENTO_COLUMN_SORT_ORDER);
		if (mementoColumnSortOrderIds != null) {
			fVisibleColumnIds = StringToArrayConverter.convertStringToArray(mementoColumnSortOrderIds);
		}

		// restore column width
		final String mementoColumnWidth = settings.get(MEMENTO_COLUMN_WIDTH);
		if (mementoColumnWidth != null) {
			fColumnIdsAndWidth = StringToArrayConverter.convertStringToArray(mementoColumnWidth);
		}
	}

	/**
	 * Save the column order and width into a memento
	 * 
	 * @param settings
	 */
	public void saveState(final IDialogSettings settings) {

		if (settings == null) {
			return;
		}

		// save column sort order
		fVisibleColumnIds = getColumnIdsFromViewer();
		if (fVisibleColumnIds != null) {
			settings.put(MEMENTO_COLUMN_SORT_ORDER, StringToArrayConverter.convertArrayToString(fVisibleColumnIds));
		}

		// save columns width and keep it for internal use
		fColumnIdsAndWidth = getColumnIdAndWidthFromViewer();
		if (fColumnIdsAndWidth != null) {
			settings.put(MEMENTO_COLUMN_WIDTH, StringToArrayConverter.convertArrayToString(fColumnIdsAndWidth));
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
				/*
				 * there is somewhere an error that the column width is 0,
				 */
				columnWidth = colDef.getDefaultColumnWidth();
				columnWidth = Math.max(MINIMUM_COLUMN_WIDTH, columnWidth);
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

	/**
	 * Sets the column layout for the viewer which is managed by the {@link ColumnManager}.
	 * <p>
	 * When the columnLayout is set, all columns must have a {@link ColumnWeightData}, otherwise it
	 * will fail
	 * 
	 * @param columnLayout
	 */
	public void setColumnLayout(final AbstractColumnLayout columnLayout) {
		fColumnLayout = columnLayout;
	}

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

		fColumnViewer = fTourViewer.recreateViewer(fColumnViewer);
	}

}
