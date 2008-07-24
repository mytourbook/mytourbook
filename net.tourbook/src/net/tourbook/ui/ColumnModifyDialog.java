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

import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class ColumnModifyDialog extends TrayDialog {

	private ColumnManager				fColumnManager;
	private CheckboxTableViewer			fColumnViewer;

	private Button						fBtnMoveUp;
	private Button						fBtnMoveDown;
	private Button						fBtnSelectAll;
	private Button						fBtnDeselectAll;

	private ArrayList<ColumnDefinition>	fAllColumns;

	public ColumnModifyDialog(	final Shell parentShell,
								final ColumnManager columnManager,
								final ArrayList<ColumnDefinition> allDialogColumns) {

		super(parentShell);

		fColumnManager = columnManager;
		fAllColumns = allDialogColumns;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.ColumnModifyDialog_Dialog_title);
	}

	private void createButtons(final Composite parent) {

		final Composite btnContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(btnContainer);
		GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).applyTo(btnContainer);
//		btnContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		fBtnMoveUp = new Button(btnContainer, SWT.NONE);
		fBtnMoveUp.setText(Messages.ColumnModifyDialog_Button_move_up);
		fBtnMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				moveSelectionUp();
				enableUpDownButtons();
			}
		});
		setButtonLayoutData(fBtnMoveUp);

		fBtnMoveDown = new Button(btnContainer, SWT.NONE);
		fBtnMoveDown.setText(Messages.ColumnModifyDialog_Button_move_down);
		fBtnMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				moveSelectionDown();
				enableUpDownButtons();
			}
		});
		setButtonLayoutData(fBtnMoveDown);

		// spacer
		new Label(btnContainer, SWT.NONE);

		fBtnSelectAll = new Button(btnContainer, SWT.NONE);
		fBtnSelectAll.setText(Messages.ColumnModifyDialog_Button_select_all);
		fBtnSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// update model
				for (final ColumnDefinition colDef : fAllColumns) {
					colDef.setIsVisibleInDialog(true);
				}

				// update viewer
				fColumnViewer.setAllChecked(true);
			}
		});
		setButtonLayoutData(fBtnSelectAll);

		fBtnDeselectAll = new Button(btnContainer, SWT.NONE);
		fBtnDeselectAll.setText(Messages.ColumnModifyDialog_Button_deselect_all);
		fBtnDeselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// list with all columns which must be checked
				final ArrayList<ColumnDefinition> checkedElements = new ArrayList<ColumnDefinition>();

				// update model
				for (final ColumnDefinition colDef : fAllColumns) {
					if (colDef.canModifyVisibility() == false) {
						checkedElements.add(colDef);
						colDef.setIsVisibleInDialog(true);
					} else {
						colDef.setIsVisibleInDialog(false);
					}
				}

				// update viewer
				fColumnViewer.setCheckedElements(checkedElements.toArray());
			}
		});
		setButtonLayoutData(fBtnDeselectAll);
	}

	private void createColumnsViewer(final Composite parent) {

		final TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableLayouter);
//		tableLayouter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/*
		 * create table
		 */
		final Table table = new Table(tableLayouter, SWT.CHECK | SWT.FULL_SELECTION | SWT.BORDER);
		fColumnViewer = new CheckboxTableViewer(table);

		TableViewerColumn tvc;

		tvc = new TableViewerColumn(fColumnViewer, SWT.LEAD);
		tableLayouter.addColumnData(new ColumnWeightData(1, true));

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

		fColumnViewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return fAllColumns.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		fColumnViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				final ColumnDefinition colDef = (ColumnDefinition) event.getElement();

				if (colDef.canModifyVisibility()) {

					// keep the checked status
					colDef.setIsVisibleInDialog(event.getChecked());

					// select the checked item
					fColumnViewer.setSelection(new StructuredSelection(colDef));

				} else {

					// column can't be unchecked
					fColumnViewer.setChecked(colDef, true);
				}
			}
		});

		fColumnViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownButtons();
			}
		});
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgAreaContainer);

		// load columns into the viewer
		fColumnViewer.setInput(this);

		// check columns which are visible
		final ArrayList<ColumnDefinition> visibleColumns = new ArrayList<ColumnDefinition>();
		for (final ColumnDefinition colDef : fAllColumns) {
			if (colDef.isVisibleInDialog()) {
				visibleColumns.add(colDef);
			}
		}
		fColumnViewer.setCheckedElements(visibleColumns.toArray());

		enableUpDownButtons();

		return dlgAreaContainer;
	}

	private void createUI(final Composite parent) {

		Label label;

		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.ColumnModifyDialog_Label_info);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(label);

		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(dlgContainer);
//		dlgContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createColumnsViewer(dlgContainer);
		createButtons(dlgContainer);

		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.ColumnModifyDialog_Label_hint);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(label);
	}

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownButtons() {

		final Table table = fColumnViewer.getTable();
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

		fBtnMoveUp.setEnabled(isUpEnabled);
		fBtnMoveDown.setEnabled(isDownEnabled);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	/**
	 * Moves an entry in the table to the given index.
	 */
	private void move(final TableItem item, final int index) {

		final ColumnDefinition colDef = (ColumnDefinition) item.getData();

		// remove existing item
		item.dispose();

		// create new item
		fColumnViewer.insert(colDef, index);
		fColumnViewer.setChecked(colDef, colDef.isVisibleInDialog());
	}

	/**
	 * Move the current selection in the build list down.
	 */
	private void moveSelectionDown() {
		final Table table = fColumnViewer.getTable();
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
		final Table table = fColumnViewer.getTable();
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

		fColumnManager.updateColumns(fColumnViewer.getTable().getItems());

		super.okPressed();
	}

}
