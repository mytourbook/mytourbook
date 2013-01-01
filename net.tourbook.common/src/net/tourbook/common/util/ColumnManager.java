/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.common.UI;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
	private static final int					MINIMUM_COLUMN_WIDTH			= 7;

	private static final String					MEMENTO_COLUMN_SORT_ORDER		= "column_sort_order";					//$NON-NLS-1$
	private static final String					MEMENTO_COLUMN_WIDTH			= "column_width";						//$NON-NLS-1$

	/**
	 * column definitions for all columns which are defined for the viewer
	 */
	private final ArrayList<ColumnDefinition>	_allDefinedColumnDefinitions	= new ArrayList<ColumnDefinition>();

	/**
	 * contains the column definitions for the visible columns in the sort order of the table/tree
	 */
	private ArrayList<ColumnDefinition>			_visibleColumnDefinitions;

	/**
	 * contains the column ids which are visible in the viewer
	 */
	private String[]							_visibleColumnIds;

	/**
	 * contains a pair of column id and width for each column
	 */
	private String[]							_columnIdsAndWidth;

	private AbstractColumnLayout				_columnLayout;

	private final ITourViewer					_tourViewer;

	/**
	 * viewer which is managed by this {@link ColumnManager}
	 */
	private ColumnViewer						_columnViewer;

	/**
	 * Context menu listener
	 */
	private Listener							_menuDetectListener;

	public ColumnManager(final ITourViewer tourViewer, final IDialogSettings viewState) {

		_tourViewer = tourViewer;

		restoreState(viewState);
	}

	public void addColumn(final ColumnDefinition colDef) {
		_allDefinedColumnDefinitions.add(colDef);
	}

	/**
	 * Removes all defined columns
	 */
	public void clearColumns() {
		_allDefinedColumnDefinitions.clear();
	}

	/**
	 * Creates the columns in the tree/table for all defined columns
	 * 
	 * @param columnViewer
	 */
	public void createColumns(final ColumnViewer columnViewer) {

		_columnViewer = columnViewer;

		setVisibleColumnDefinitions();

		if (columnViewer instanceof TableViewer) {

			// create all columns in the table

			for (final ColumnDefinition colDef : _visibleColumnDefinitions) {
				createTableColumn((TableColumnDefinition) colDef, (TableViewer) columnViewer);
			}

		} else if (columnViewer instanceof TreeViewer) {

			// create all columns in the tree

			for (final ColumnDefinition colDef : _visibleColumnDefinitions) {
				createTreeColumn((TreeColumnDefinition) colDef, (TreeViewer) columnViewer);
			}
		}
	}

	/**
	 * set context menu depending on the position of the mouse
	 * 
	 * @param table
	 * @param tableContextMenu
	 *            can be <code>null</code>
	 */
	public void createHeaderContextMenu(final Table table, final Menu tableContextMenu) {

		// remove old listener
		if (_menuDetectListener != null) {
			table.removeListener(SWT.MenuDetect, _menuDetectListener);
		}

		final Menu headerContextMenu = createHeaderContextMenuInternal(table, tableContextMenu);

		// add the context menu to the table
		_menuDetectListener = new Listener() {
			public void handleEvent(final Event event) {

				final Decorations shell = table.getShell();
				final Display display = shell.getDisplay();
				final Point pt = display.map(null, table, new Point(event.x, event.y));
				final Rectangle clientArea = table.getClientArea();

				final boolean isTableHeaderHit = clientArea.y <= pt.y
						&& pt.y < (clientArea.y + table.getHeaderHeight());

				final Menu contextMenu = isTableHeaderHit ? headerContextMenu : tableContextMenu;

				table.setMenu(contextMenu);
			}
		};

		table.addListener(SWT.MenuDetect, _menuDetectListener);
	}

	/**
	 * set context menu depending on the position of the mouse
	 * 
	 * @param tree
	 * @param treeContextMenu
	 *            can be <code>null</code>
	 */
	public void createHeaderContextMenu(final Tree tree, final Menu treeContextMenu) {

		final Menu headerContextMenu = createHeaderContextMenuInternal(tree, treeContextMenu);

		// add the context menu to the table viewer
		tree.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(final Event event) {

				final Decorations shell = tree.getShell();
				final Display display = shell.getDisplay();
				final Point pt = display.map(null, tree, new Point(event.x, event.y));
				final Rectangle clientArea = tree.getClientArea();

				final boolean header = clientArea.y <= pt.y && pt.y < (clientArea.y + tree.getHeaderHeight());

				tree.setMenu(header ? headerContextMenu : treeContextMenu);
			}
		});
	}

	/**
	 * Create header context menu which has the action to modify columns
	 * 
	 * @param composite
	 * @param compositeContextMenu
	 * @return
	 */
	private Menu createHeaderContextMenuInternal(final Composite composite, final Menu compositeContextMenu) {

		final Decorations shell = composite.getShell();
		final Menu headerContextMenu = new Menu(shell, SWT.POP_UP);

		/*
		 * Size All Columns to Fit
		 */
		MenuItem itemName = new MenuItem(headerContextMenu, SWT.PUSH);
		itemName.setText(Messages.Action_App_SizeAllColumnsToFit);
		itemName.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				onFitAllColumnSize();
			}
		});

		/*
		 * Customize &Columns...
		 */
		itemName = new MenuItem(headerContextMenu, SWT.PUSH);
		itemName.setText(Messages.Action_App_ConfigureColumns);
		itemName.setImage(UI.IMAGE_REGISTRY.get(UI.IMAGE_CONFIGURE_COLUMNS));
		itemName.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				openColumnDialog();
			}
		});

		/*
		 * IMPORTANT: Dispose the menus (only the current menu, when menu is set with setMenu() it
		 * will be disposed automatically)
		 */
		composite.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(final Event event) {
				headerContextMenu.dispose();
				if (compositeContextMenu != null) {
					compositeContextMenu.dispose();
				}
			}
		});

		return headerContextMenu;
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
		if (_columnLayout == null) {

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
				_columnLayout.setColumnData(tc, columnPixelData);
			} else {
				_columnLayout.setColumnData(tc, columnLayoutData);
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
		for (final ColumnDefinition colDef : _allDefinedColumnDefinitions) {
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
		for (final ColumnDefinition colDef : _visibleColumnDefinitions) {
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

		if (_columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) _columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}

			for (final TableColumn column : table.getColumns()) {

				final String columnId = ((ColumnDefinition) column.getData()).getColumnId();
				final int columnWidth = column.getWidth();

				setColumnIdAndWidth(columnIdsAndWidth, columnId, columnWidth);
			}

		} else if (_columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) _columnViewer).getTree();
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

		if (_columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) _columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}
			columnOrder = table.getColumnOrder();

		} else if (_columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) _columnViewer).getTree();
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

		for (int columnIndex = 0; columnIndex < _columnIdsAndWidth.length; columnIndex++) {
			final String columnId = _columnIdsAndWidth[columnIndex];

			if (columnWidthId.equals(columnId)) {
				try {
					return Integer.parseInt(_columnIdsAndWidth[++columnIndex]);
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
	 * @return Returns all columns which are displayed in the {@link DialogModifyColumns}
	 */
	private ArrayList<ColumnDefinition> getDialogColumns() {

		final ArrayList<ColumnDefinition> allColumnsClone = new ArrayList<ColumnDefinition>();

		try {
			for (final ColumnDefinition definedColDef : _allDefinedColumnDefinitions) {
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
		if (_columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) _columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}
			columnOrder = table.getColumnOrder();

		} else if (_columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) _columnViewer).getTree();
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
				colDef.setColumnWidth(getColumnWidth(colDef._columnId));

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

	private void onFitAllColumnSize() {

		// larger tables/trees needs more time to resize

		BusyIndicator.showWhile(_columnViewer.getControl().getDisplay(), new Runnable() {
			public void run() {

				boolean isColumn0Visible = true;

				if (_tourViewer instanceof ITourViewer2) {
					isColumn0Visible = ((ITourViewer2) _tourViewer).isColumn0Visible(_columnViewer);
				}

				if (_columnViewer instanceof TableViewer) {

					final Table table = ((TableViewer) _columnViewer).getTable();
					if (table.isDisposed()) {
						return;
					}

					table.setRedraw(false);
					{
						final TableColumn[] allColumns = table.getColumns();

						for (int columnIndex = 0; columnIndex < allColumns.length; columnIndex++) {
							final TableColumn tableColumn = allColumns[columnIndex];
							if (columnIndex == 0) {

								if (isColumn0Visible) {
									tableColumn.pack();
								} else {
									tableColumn.setWidth(0);
								}
							} else {
								tableColumn.pack();
							}
						}
					}
					table.setRedraw(true);

				} else if (_columnViewer instanceof TreeViewer) {

					final Tree tree = ((TreeViewer) _columnViewer).getTree();
					if (tree.isDisposed()) {
						return;
					}

					tree.setRedraw(false);
					{
						final TreeColumn[] allColumns = tree.getColumns();
						for (final TreeColumn tableColumn : allColumns) {
							tableColumn.pack();
						}
					}
					tree.setRedraw(true);
				}
			}
		});
	}

	public void openColumnDialog() {

		// get the sorting order and column width from the viewer
		_visibleColumnIds = getColumnIdsFromViewer();
		_columnIdsAndWidth = getColumnIdAndWidthFromViewer();

		(new DialogModifyColumns(
				Display.getCurrent().getActiveShell(),
				this,
				getDialogColumns(),
				_allDefinedColumnDefinitions)).open();
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
			_visibleColumnIds = StringToArrayConverter.convertStringToArray(mementoColumnSortOrderIds);
		}

		// restore column width
		final String mementoColumnWidth = settings.get(MEMENTO_COLUMN_WIDTH);
		if (mementoColumnWidth != null) {
			_columnIdsAndWidth = StringToArrayConverter.convertStringToArray(mementoColumnWidth);
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
		_visibleColumnIds = getColumnIdsFromViewer();
		if (_visibleColumnIds != null) {
			settings.put(MEMENTO_COLUMN_SORT_ORDER, StringToArrayConverter.convertArrayToString(_visibleColumnIds));
		}

		// save columns width and keep it for internal use
		_columnIdsAndWidth = getColumnIdAndWidthFromViewer();
		if (_columnIdsAndWidth != null) {
			settings.put(MEMENTO_COLUMN_WIDTH, StringToArrayConverter.convertArrayToString(_columnIdsAndWidth));
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
	 * Set the columns in {@link #_visibleColumnDefinitions} to the order of the
	 * <code>tableItems</code> in the {@link DialogModifyColumns}
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

		_visibleColumnIds = visibleColumnIds.toArray(new String[visibleColumnIds.size()]);
		_columnIdsAndWidth = columnIdsAndWidth.toArray(new String[columnIdsAndWidth.size()]);
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
		_columnLayout = columnLayout;
	}

	/**
	 * Set the visible column definitions from the visible ids
	 */
	private void setVisibleColumnDefinitions() {

		_visibleColumnDefinitions = new ArrayList<ColumnDefinition>();

		if (_visibleColumnIds != null) {

			// create columns with the correct sort order

			int createIndex = 0;

			for (final String columnId : _visibleColumnIds) {

				final ColumnDefinition colDef = getColumnDefinitionByColumnId(columnId);
				if (colDef != null) {

					colDef.setCreateIndex(createIndex++);

					_visibleColumnDefinitions.add(colDef);
				}
			}
		}

		if (_columnIdsAndWidth != null) {

			// set the width for all columns

			for (int dataIdx = 0; dataIdx < _columnIdsAndWidth.length; dataIdx++) {

				final String columnId = _columnIdsAndWidth[dataIdx++];
				final int columnWidth = Integer.valueOf(_columnIdsAndWidth[dataIdx]);

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
		if ((_visibleColumnDefinitions.size() == 0) && (_allDefinedColumnDefinitions.size() > 0)) {

			final ArrayList<String> columnIds = new ArrayList<String>();
			int createIndex = 0;

			for (final ColumnDefinition columnDef : _allDefinedColumnDefinitions) {
				if (columnDef.isDefaultColumn()) {

					columnDef.setCreateIndex(createIndex++);

					_visibleColumnDefinitions.add(columnDef);
					columnIds.add(columnDef.getColumnId());
				}
			}

			_visibleColumnIds = columnIds.toArray(new String[columnIds.size()]);
		}

		/*
		 * when no default columns are set, use the first column
		 */
		if ((_visibleColumnDefinitions.size() == 0) && (_allDefinedColumnDefinitions.size() > 0)) {

			final ColumnDefinition firstColumn = _allDefinedColumnDefinitions.get(0);
			firstColumn.setCreateIndex(0);

			_visibleColumnDefinitions.add(firstColumn);

			_visibleColumnIds = new String[1];
			_visibleColumnIds[0] = firstColumn.getColumnId();
		}
	}

	/**
	 * Update the viewer with the columns from the {@link DialogModifyColumns}
	 * 
	 * @param tableItems
	 *            table item in the {@link DialogModifyColumns}
	 */
	void updateColumns(final TableItem[] tableItems) {

		setColumnIdsFromModifyDialog(tableItems);

		_columnViewer = _tourViewer.recreateViewer(_columnViewer);
	}
}
