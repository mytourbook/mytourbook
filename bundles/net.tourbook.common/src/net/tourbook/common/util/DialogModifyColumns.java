/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import net.tourbook.common.CommonActivator;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GlyphMetrics;
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
import org.eclipse.swt.widgets.Widget;

public class DialogModifyColumns extends TrayDialog {

	private ColumnManager				_columnManager;
	private CheckboxTableViewer			_columnViewer;

	private Button						_btnMoveUp;
	private Button						_btnMoveDown;
	private Button						_btnSelectAll;
	private Button						_btnDeselectAll;
	private Button						_btnDefault;

	private ArrayList<ColumnDefinition>	_allColumnsInDialog;
	private ArrayList<ColumnDefinition>	_allDefinedColumns;

	private PixelConverter				_pc;

	private long						_dndDragStartViewerLeft;
	private Object[]					_dndCheckedElements;

	public DialogModifyColumns(	final Shell parentShell,
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

		_pc = new PixelConverter(parent);

		Label label;

		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.ColumnModifyDialog_Label_info);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(label);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(container);
		{
			createUI10ColumnsViewer(container);
			createUI20Buttons(container);
			createUI30Hints(container);
		}
	}

	private void createUI10ColumnsViewer(final Composite parent) {

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
		TableColumn tc;

		// column: label
		tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.ColumnModifyDialog_column_column);
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
		tableLayout.setColumnData(tc, new ColumnWeightData(1, true));

		// column: unit
		tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.ColumnModifyDialog_column_unit);
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
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(13), true));

		// column: width
		tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.ColumnModifyDialog_column_width);
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
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(10), true));

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

		/*
		 * set drag adapter
		 */
		_columnViewer.addDragSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					public void dragFinished(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					public void dragSetData(final DragSourceEvent event) {
						// data are set in LocalSelectionTransfer
					}

					public void dragStart(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = _columnViewer.getSelection();

						_dndCheckedElements = _columnViewer.getCheckedElements();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dndDragStartViewerLeft = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_columnViewer) {

			private Widget	_dragOverItem;

			@Override
			public void dragOver(final DropTargetEvent dropEvent) {

				// keep table item
				_dragOverItem = dropEvent.item;

				super.dragOver(dropEvent);
			}

			@Override
			public boolean performDrop(final Object data) {

				if (data instanceof StructuredSelection) {
					final StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof ColumnDefinition) {

						final ColumnDefinition colDef = (ColumnDefinition) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table filterTable = _columnViewer.getTable();

						/*
						 * check if drag was startet from this item, remove the item before the new
						 * item is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dndDragStartViewerLeft) {
							_columnViewer.remove(colDef);
						}

						int filterIndex;

						if (_dragOverItem == null) {

							_columnViewer.add(colDef);
							filterIndex = filterTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							filterIndex = filterTable.indexOf((TableItem) _dragOverItem);
							if (filterIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								_columnViewer.insert(colDef, filterIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								_columnViewer.insert(colDef, ++filterIndex);
							}
						}

						// reselect filter item
						_columnViewer.setSelection(new StructuredSelection(colDef));

						// set focus to selection
						filterTable.setSelection(filterIndex);
						filterTable.setFocus();

						// recheck items
						_columnViewer.setCheckedElements(_dndCheckedElements);

						enableUpDownButtons();

						return true;
					}
				}

				return false;
			}

			@Override
			public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

				final LocalSelectionTransfer transferData = LocalSelectionTransfer.getTransfer();

				// check if dragged item is the target item
				final ISelection selection = transferData.getSelection();
				if (selection instanceof StructuredSelection) {
					final Object dragFilter = ((StructuredSelection) selection).getFirstElement();
					if (target == dragFilter) {
						return false;
					}
				}

				if (transferData.isSupportedType(transferType) == false) {
					return false;
				}

				// check if target is between two items
				if (getCurrentLocation() == LOCATION_ON) {
					return false;
				}

				return true;
			}

		};

		_columnViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createUI20Buttons(final Composite parent) {

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

	private void createUI30Hints(final Composite parent) {

		// use a bulleted list to display this info
		final StyleRange style = new StyleRange();
		style.metrics = new GlyphMetrics(0, 0, 10);
		final Bullet bullet = new Bullet(style);

		final String infoText = Messages.ColumnModifyDialog_Label_Hints;
		final int lineCount = Util.countCharacter(infoText, '\n');

		final StyledText styledText = new StyledText(parent, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(styledText);
		styledText.setText(infoText);
		styledText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		styledText.setLineBullet(1, lineCount, bullet);
		styledText.setLineWrapIndent(1, lineCount, 10);
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
		return CommonActivator.getState(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
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
