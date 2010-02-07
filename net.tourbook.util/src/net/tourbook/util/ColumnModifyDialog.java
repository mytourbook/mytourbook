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
package net.tourbook.util;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class ColumnModifyDialog extends TrayDialog {

	private ColumnManager				_columnManager;
	private CheckboxTableViewer			_columnViewer;

	private Button						_btnMoveUp;
	private Button						_btnMoveDown;
	private Button						_btnSelectAll;
	private Button						_btnDeselectAll;
	private Button						_btnDefault;

	private ArrayList<ColumnDefinition>	_allColumnsInDialog;
	private ArrayList<ColumnDefinition>	_allDefinedColumns;

	public ColumnModifyDialog(	final Shell parentShell,
								final ColumnManager columnManager,
								final ArrayList<ColumnDefinition> dialogColumns,
								final ArrayList<ColumnDefinition> allDefaultColumns) {

		super(parentShell);

		_columnManager = columnManager;
		_allColumnsInDialog = dialogColumns;
		_allDefinedColumns = allDefaultColumns;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.ColumnModifyDialog_Dialog_title);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);
		final GridData gd = (GridData) dlgContainer.getLayoutData();
		gd.heightHint = 400;
		gd.widthHint = 400;

		createUI(dlgContainer);

		setupColumnsInViewer();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		Label label;

		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.ColumnModifyDialog_Label_info);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(label);

		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(dlgContainer);

		createUIColumnsViewer(dlgContainer);
		createUIButtons(dlgContainer);

		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.ColumnModifyDialog_Label_hint);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(label);
	}

	private void createUIButtons(final Composite parent) {

		final Composite btnContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(btnContainer);
		GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).applyTo(btnContainer);

		_btnMoveUp = new Button(btnContainer, SWT.NONE);
		_btnMoveUp.setText(Messages.ColumnModifyDialog_Button_move_up);
		_btnMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				moveSelectionUp();
				enableUpDownButtons();
			}
		});
		setButtonLayoutData(_btnMoveUp);

		_btnMoveDown = new Button(btnContainer, SWT.NONE);
		_btnMoveDown.setText(Messages.ColumnModifyDialog_Button_move_down);
		_btnMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				moveSelectionDown();
				enableUpDownButtons();
			}
		});
		setButtonLayoutData(_btnMoveDown);

		// spacer
		new Label(btnContainer, SWT.NONE);

		_btnSelectAll = new Button(btnContainer, SWT.NONE);
		_btnSelectAll.setText(Messages.ColumnModifyDialog_Button_select_all);
		_btnSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// update model
				for (final ColumnDefinition colDef : _allColumnsInDialog) {
					colDef.setIsCheckedInDialog(true);
				}

				// update viewer
				_columnViewer.setAllChecked(true);
			}
		});
		setButtonLayoutData(_btnSelectAll);

		_btnDeselectAll = new Button(btnContainer, SWT.NONE);
		_btnDeselectAll.setText(Messages.ColumnModifyDialog_Button_deselect_all);
		_btnDeselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// list with all columns which must be checked
				final ArrayList<ColumnDefinition> checkedElements = new ArrayList<ColumnDefinition>();

				// update model
				for (final ColumnDefinition colDef : _allColumnsInDialog) {
					if (colDef.canModifyVisibility() == false) {
						checkedElements.add(colDef);
						colDef.setIsCheckedInDialog(true);
					} else {
						colDef.setIsCheckedInDialog(false);
					}
				}

				// update viewer
				_columnViewer.setCheckedElements(checkedElements.toArray());
			}
		});
		setButtonLayoutData(_btnDeselectAll);

		// spacer
		new Label(btnContainer, SWT.NONE);

		_btnDefault = new Button(btnContainer, SWT.NONE);
		_btnDefault.setText(Messages.ColumnModifyDialog_Button_default);
		_btnDefault.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {

				/*
				 * copy all columns into the custom columns
				 */

				_allColumnsInDialog = new ArrayList<ColumnDefinition>();

				for (final ColumnDefinition definedColumnDef : _allDefinedColumns) {
					try {

						final ColumnDefinition columnDefinitionClone = (ColumnDefinition) definedColumnDef.clone();

						// visible columns in the viewer will be checked
						final boolean isDefaultColumn = definedColumnDef.isDefaultColumn();
						columnDefinitionClone.setIsCheckedInDialog(isDefaultColumn);
						columnDefinitionClone.setColumnWidth(definedColumnDef.getDefaultColumnWidth());

						_allColumnsInDialog.add(columnDefinitionClone);

					} catch (final CloneNotSupportedException e) {
						e.printStackTrace();
					}
				}

				setupColumnsInViewer();
			}
		});
		setButtonLayoutData(_btnDefault);

	}

	private void createUIColumnsViewer(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.CHECK | SWT.FULL_SELECTION | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		_columnViewer = new CheckboxTableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;

		// column: label
		tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.ColumnModifyDialog_column_column);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
				cell.setText(colDef.getColumnLabel());

				// paint columns in a different color which can't be hidden
				if (colDef.canModifyVisibility() == false) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
				}
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(1, true));

		// column: unit
		tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.ColumnModifyDialog_column_unit);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
				cell.setText(colDef.getColumnUnit());

				// paint columns in a different color which can't be hidden
				if (colDef.canModifyVisibility() == false) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
				}
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(13), true));

		// column: width
		tvc = new TableViewerColumn(_columnViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.ColumnModifyDialog_column_width);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
				cell.setText(Integer.toString(colDef.getColumnWidth()));

				// paint columns in a different color which can't be hidden
				if (colDef.canModifyVisibility() == false) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
				}
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(10), true));

		_columnViewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return _allColumnsInDialog.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_columnViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final Object firstElement = ((IStructuredSelection) _columnViewer.getSelection()).getFirstElement();
				if (firstElement != null) {

					// check/uncheck current item

					_columnViewer.setChecked(firstElement, !_columnViewer.getChecked(firstElement));
				}
			}
		});

		_columnViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				final ColumnDefinition colDef = (ColumnDefinition) event.getElement();

				if (colDef.canModifyVisibility()) {

					// keep the checked status
					colDef.setIsCheckedInDialog(event.getChecked());

					// select the checked item
					_columnViewer.setSelection(new StructuredSelection(colDef));

				} else {

					// column can't be unchecked
					_columnViewer.setChecked(colDef, true);
				}
			}
		});

		_columnViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownButtons();
			}
		});
	}

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownButtons() {

		final Table table = _columnViewer.getTable();
		final TableItem[] items = table.getSelection();

		final boolean isSelected = items != null && items.length > 0;

		boolean isUpEnabled = isSelected;
		boolean isDownEnabled = isSelected;

		if (isSelected) {

			final int indices[] = table.getSelectionIndices();
			final int max = table.getItemCount();

			isUpEnabled = indices[0] != 0;
			isDownEnabled = indices[indices.length - 1] < max - 1;
		}

		// disable movable when a column is not allowed to be moved
		for (final TableItem tableItem : items) {
			final ColumnDefinition colDef = (ColumnDefinition) tableItem.getData();

			if (colDef.isColumnMoveable() == false) {
				isUpEnabled = false;
				isDownEnabled = false;

				break;
			}
		}

		_btnMoveUp.setEnabled(isUpEnabled);
		_btnMoveDown.setEnabled(isDownEnabled);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return Activator.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	/**
	 * Moves an entry in the table to the given index.
	 */
	private void move(final TableItem item, final int index) {

		final ColumnDefinition colDef = (ColumnDefinition) item.getData();

		// remove existing item
		item.dispose();

		// create new item
		_columnViewer.insert(colDef, index);
		_columnViewer.setChecked(colDef, colDef.isCheckedInDialog());
	}

	/**
	 * Move the current selection in the build list down.
	 */
	private void moveSelectionDown() {
		final Table table = _columnViewer.getTable();
		final int indices[] = table.getSelectionIndices();
		if (indices.length < 1) {
			return;
		}
		final int newSelection[] = new int[indices.length];
		final int max = table.getItemCount() - 1;
		for (int i = indices.length - 1; i >= 0; i--) {
			final int index = indices[i];
			if (index < max) {
				move(table.getItem(index), index + 1);
				newSelection[i] = index + 1;
			}
		}
		table.setSelection(newSelection);
	}

	/**
	 * Move the current selection in the build list up.
	 */
	private void moveSelectionUp() {
		final Table table = _columnViewer.getTable();
		final int indices[] = table.getSelectionIndices();
		final int newSelection[] = new int[indices.length];
		for (int i = 0; i < indices.length; i++) {
			final int index = indices[i];
			if (index > 0) {
				move(table.getItem(index), index - 1);
				newSelection[i] = index - 1;
			}
		}
		table.setSelection(newSelection);
	}

	@Override
	protected void okPressed() {

		_columnManager.updateColumns(_columnViewer.getTable().getItems());

		super.okPressed();
	}

	private void setupColumnsInViewer() {

		// load columns into the viewer
		_columnViewer.setInput(new Object[0]);

		// check columns which are visible
		final ArrayList<ColumnDefinition> checkedColumns = new ArrayList<ColumnDefinition>();

		for (final ColumnDefinition colDef : _allColumnsInDialog) {
			if (colDef.isCheckedInDialog()) {
				checkedColumns.add(colDef);
			}
		}
		_columnViewer.setCheckedElements(checkedColumns.toArray());

		enableUpDownButtons();
	}

}
