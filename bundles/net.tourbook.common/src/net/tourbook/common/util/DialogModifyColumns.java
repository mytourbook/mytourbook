/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import net.tourbook.common.Messages;
import net.tourbook.common.UI;

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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DialogModifyColumns extends TrayDialog {

	private ColumnManager				_columnManager;

	/** Model for the column viewer */
	private ArrayList<ColumnDefinition>	_allDialogColumns;
	private ArrayList<ColumnDefinition>	_allDefinedColumns;

	private PixelConverter				_pc;

	private long						_dndDragStartViewerLeft;
	private Object[]					_dndCheckedElements;

	private ArrayList<ColumnProfile>	_columnMgr_Profiles;
	private ColumnProfile				_dialog_SelectedProfile;
	private ArrayList<ColumnProfile>	_dialog_Profiles;

	private boolean						_isInProfileUpdate;
	private boolean						_isUserTyped;
	private int							_profileIndex_Modified	= -2;
	private int							_profileIndex_Selected;

	private final DateTimeFormatter		_dtFormatter			= DateTimeFormat.forStyle("SM");	//$NON-NLS-1$

	/*
	 * UI controls
	 */
	private Button						_btnColumn_MoveUp;
	private Button						_btnColumn_MoveDown;
	private Button						_btnColumn_SelectAll;
	private Button						_btnColumn_DeselectAll;
	private Button						_btnColumn_Default;
	private Button						_btnProfile_New;
	private Button						_btnProfile_Remove;

	private CheckboxTableViewer			_columnViewer;

	private Combo						_comboProfiles;

	public DialogModifyColumns(	final Shell parentShell,
								final ColumnManager columnManager,
								final ArrayList<ColumnDefinition> allRearrangedColumns,
								final ArrayList<ColumnDefinition> allDefinedColumns,
								final ColumnProfile columnMgr_ActiveProfile,
								final ArrayList<ColumnProfile> columnMgr_Profiles) {

		super(parentShell);

		_columnManager = columnManager;
		_allDialogColumns = allRearrangedColumns;
		_allDefinedColumns = allDefinedColumns;

		_columnMgr_Profiles = columnMgr_Profiles;

		// use cloned profiles in the dialog
		_dialog_Profiles = new ArrayList<>();
		for (final ColumnProfile columnMgr_Profile : columnMgr_Profiles) {

			final ColumnProfile clonedProfile = columnMgr_Profile.clone();

			_dialog_Profiles.add(clonedProfile);

			// set active profile
			if (columnMgr_Profile == columnMgr_ActiveProfile) {
				_dialog_SelectedProfile = clonedProfile;
			}
		}

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.ColumnModifyDialog_Dialog_title);
	}

	/**
	 * Create all columns in default order and selection.
	 */
	private void createDefaultColumns() {

		_allDialogColumns = new ArrayList<ColumnDefinition>();

		for (final ColumnDefinition definedColDef : _allDefinedColumns) {
			try {

				final ColumnDefinition columnDefinitionClone = (ColumnDefinition) definedColDef.clone();

				// visible columns in the viewer will be checked
				final boolean isDefaultColumn = definedColDef.isDefaultColumn();
				columnDefinitionClone.setIsCheckedInDialog(isDefaultColumn);
				columnDefinitionClone.setColumnWidth(definedColDef.getDefaultColumnWidth());

				_allDialogColumns.add(columnDefinitionClone);

			} catch (final CloneNotSupportedException e) {
				StatusUtil.log(e);
			}
		}
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		// set default width
		final GridData gd = (GridData) dlgContainer.getLayoutData();
		gd.heightHint = 400;
		gd.widthHint = 400;

		createUI(dlgContainer);

		setupColumnsInViewer();

		restoreState();

		enableProfileActions();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(container);
		{
			createUI_10_Profile(container);
			createUI_12_ProfileActions(container);

			createUI_70_ColumnsHeader(container);
			createUI_72_ColumnsViewer(container);
			createUI_74_ColumnActions(container);
			createUI_76_Hints(container);
		}
	}

	private void createUI_10_Profile(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * Label: Profile
			 */
			{
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.ColumnModifyDialog_Label_Profile);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
			}

			/*
			 * Combo: Profiles
			 */
			{
				_comboProfiles = new Combo(container, SWT.SINGLE | SWT.BORDER);
				_comboProfiles.setVisibleItemCount(20);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_comboProfiles);

				/*
				 * Combo text editing solved with this solution:
				 * http://stackoverflow.com/questions/12410636
				 * /swt-differentiating-between-selection-and-typing-in-a-combo
				 */
				_comboProfiles.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(final VerifyEvent e) {

						_isUserTyped = (e.keyCode != 0);
					}
				});

				_comboProfiles.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(final ModifyEvent e) {

						if (_isInProfileUpdate) {
							return;
						}

						final Combo combo = (Combo) e.widget;
						final int selectionIndex = combo.getSelectionIndex();

						if (_isUserTyped || _profileIndex_Modified == selectionIndex || selectionIndex == -1) {

							onProfile_Edit();
						}

						_profileIndex_Modified = selectionIndex;
					}
				});

				_comboProfiles.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {

						if (_isInProfileUpdate) {
							return;
						}

						onProfile_Select(e);
					}
				});
			}
		}
	}

	private void createUI_12_ProfileActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).extendedMargins(5, 0, 0, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * Button: New
			 */
			{
				_btnProfile_New = new Button(container, SWT.NONE);
				_btnProfile_New.setText(Messages.App_Action_New);
				_btnProfile_New.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Add();
					}
				});
				setButtonLayoutData(_btnProfile_New);
			}

			/*
			 * Button: Remove
			 */
			{
				_btnProfile_Remove = new Button(container, SWT.NONE);
				_btnProfile_Remove.setText(Messages.App_Action_Remove_NoConfirm);
				_btnProfile_Remove.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Remove();
					}
				});
				setButtonLayoutData(_btnProfile_Remove);
			}
		}
	}

	private void createUI_70_ColumnsHeader(final Composite container) {

		final Label label = new Label(container, SWT.WRAP);
		label.setText(Messages.ColumnModifyDialog_Label_info);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
//				.indent(0, 20)
				.applyTo(label);
	}

	private void createUI_72_ColumnsViewer(final Composite parent) {

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

			@Override
			public void dispose() {}

			@Override
			public Object[] getElements(final Object inputElement) {
				return _allDialogColumns.toArray();
			}

			@Override
			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_columnViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object firstElement = ((IStructuredSelection) _columnViewer.getSelection()).getFirstElement();
				if (firstElement != null) {

					// check/uncheck current item

					_columnViewer.setChecked(firstElement, !_columnViewer.getChecked(firstElement));
				}
			}
		});

		_columnViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
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

				// save columns
				saveCurrentProfile_Columns();
			}
		});

		_columnViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownActions();
			}
		});

		/*
		 * set drag adapter
		 */
		_columnViewer.addDragSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					@Override
					public void dragFinished(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					@Override
					public void dragSetData(final DragSourceEvent event) {
						// data are set in LocalSelectionTransfer
					}

					@Override
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

						enableUpDownActions();

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

	private void createUI_74_ColumnActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).applyTo(container);
		{
			_btnColumn_MoveUp = new Button(container, SWT.NONE);
			_btnColumn_MoveUp.setText(Messages.ColumnModifyDialog_Button_move_up);
			_btnColumn_MoveUp.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					moveSelectionUp();
					enableUpDownActions();
				}
			});
			setButtonLayoutData(_btnColumn_MoveUp);

			_btnColumn_MoveDown = new Button(container, SWT.NONE);
			_btnColumn_MoveDown.setText(Messages.ColumnModifyDialog_Button_move_down);
			_btnColumn_MoveDown.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					moveSelectionDown();
					enableUpDownActions();
				}
			});
			setButtonLayoutData(_btnColumn_MoveDown);

			// spacer
			new Label(container, SWT.NONE);

			_btnColumn_SelectAll = new Button(container, SWT.NONE);
			_btnColumn_SelectAll.setText(Messages.ColumnModifyDialog_Button_select_all);
			_btnColumn_SelectAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					// update model
					for (final ColumnDefinition colDef : _allDialogColumns) {
						colDef.setIsCheckedInDialog(true);
					}

					// update viewer
					_columnViewer.setAllChecked(true);
				}
			});
			setButtonLayoutData(_btnColumn_SelectAll);

			_btnColumn_DeselectAll = new Button(container, SWT.NONE);
			_btnColumn_DeselectAll.setText(Messages.ColumnModifyDialog_Button_deselect_all);
			_btnColumn_DeselectAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					// list with all columns which must be checked
					final ArrayList<ColumnDefinition> checkedElements = new ArrayList<ColumnDefinition>();

					// update model
					for (final ColumnDefinition colDef : _allDialogColumns) {
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
			setButtonLayoutData(_btnColumn_DeselectAll);

			// spacer
			new Label(container, SWT.NONE);

			_btnColumn_Default = new Button(container, SWT.NONE);
			_btnColumn_Default.setText(Messages.ColumnModifyDialog_Button_default);
			_btnColumn_Default.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {

					/*
					 * copy all defined columns into the dialog columns
					 */

					createDefaultColumns();

					setupColumnsInViewer();
				}
			});
			setButtonLayoutData(_btnColumn_Default);
		}
	}

	private void createUI_76_Hints(final Composite parent) {

		// use a bulleted list to display this info
		final StyleRange style = new StyleRange();
		style.metrics = new GlyphMetrics(0, 0, 10);
		final Bullet bullet = new Bullet(style);

		final String infoText = Messages.ColumnModifyDialog_Label_Hints;
		final int lineCount = Util.countCharacter(infoText, '\n');

		final StyledText styledText = new StyledText(parent, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		styledText.setText(infoText);
		styledText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		styledText.setLineBullet(1, lineCount, bullet);
		styledText.setLineWrapIndent(1, lineCount, 10);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(styledText);
	}

	private void enableProfileActions() {

		final int profilesSize = _dialog_Profiles.size();

		_btnProfile_Remove.setEnabled(profilesSize > 1);
	}

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownActions() {

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

		_btnColumn_MoveUp.setEnabled(isUpEnabled);
		_btnColumn_MoveDown.setEnabled(isDownEnabled);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return CommonActivator.getState(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	private ColumnProfile getSelectedProfile() {

		if (_profileIndex_Selected == -1 || _profileIndex_Selected >= _dialog_Profiles.size()) {

			// nothing is selected, index is out of range
			return null;
		}

		// set active profile
		final ColumnProfile selectedProfile = _dialog_Profiles.get(_profileIndex_Selected);

		return selectedProfile;
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

		// replace column mgr profiles
		_columnMgr_Profiles.clear();
		_columnMgr_Profiles.addAll(_dialog_Profiles);

		_columnManager.updateColumns(_dialog_SelectedProfile, _columnViewer.getTable().getItems());

		super.okPressed();
	}

	private void onProfile_Add() {

		_isInProfileUpdate = true;

		// save profile name + column
		saveCurrentProfile_Name();
		saveCurrentProfile_Columns();

		/*
		 * Create new profile
		 */

		// use default columns for a new profile
		createDefaultColumns();

		final ColumnProfile newProfile = new ColumnProfile();

		// set profile name
		newProfile.name = Messages.Column_Profile_Name_New + UI.SPACE + _dtFormatter.print(new DateTime());

		_dialog_Profiles.add(0, newProfile);
		_dialog_SelectedProfile = newProfile;

		/*
		 * Update UI
		 */
		_comboProfiles.add(newProfile.name, 0);
		_comboProfiles.select(0);

		setupColumnsInViewer();

		enableProfileActions();

		_isInProfileUpdate = false;
	}

	private void onProfile_Edit() {

		final ColumnProfile selectedProfile = getSelectedProfile();

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
				+ ("\tonProfile_Edit - selectedProfile: " + selectedProfile));
		// TODO remove SYSTEM.OUT.PRINTLN

		if (selectedProfile == null) {
			return;
		}

		_isInProfileUpdate = true;

		final int selectedIndex = _profileIndex_Selected;
		final String modifiedProfileName = _comboProfiles.getText();

		// save modified name
		saveCurrentProfile_Name();

//		_comboProfiles.getDisplay().asyncExec(new Runnable() {
//
//			@Override
//			public void run() {
//
//				if (_comboProfiles.isDisposed()) {
//					return;
//				}
//
		// replace combo item
		_comboProfiles.add(modifiedProfileName, selectedIndex);
		_comboProfiles.remove(selectedIndex + 1);

		_isInProfileUpdate = false;
//			}
//		});
	}

	private void onProfile_Remove() {

		_isInProfileUpdate = true;

		final int selectedIndex = _profileIndex_Selected;

		// remove from model
		_dialog_Profiles.remove(selectedIndex);

		// remove from UI
		_comboProfiles.remove(selectedIndex);

		/*
		 * Select profile at the same position
		 */

		final int profilesSize = _dialog_Profiles.size();
		int newIndex = selectedIndex;
		if (newIndex >= profilesSize) {
			newIndex = profilesSize - 1;
		}

		// update model
		_dialog_SelectedProfile = _dialog_Profiles.get(newIndex);

		// update UI
		_profileIndex_Selected = newIndex;
		_comboProfiles.select(newIndex);

		enableProfileActions();

		_isInProfileUpdate = false;
	}

	private void onProfile_Select(final SelectionEvent selectionEvent) {

		_profileIndex_Selected = _comboProfiles.getSelectionIndex();

		if (_isInProfileUpdate) {
			return;
		}

		final ColumnProfile selectedProfile = getSelectedProfile();
		if (selectedProfile == null) {
			return;
		}

		_dialog_SelectedProfile = selectedProfile;

		/*
		 * Update column viewer from the active profile
		 */
		_columnManager.setVisibleColumnDefinitions(selectedProfile);

		_allDialogColumns = selectedProfile.visibleColumnDefinitions;

		setupColumnsInViewer();
	}

	private void restoreState() {

		_isInProfileUpdate = true;

		int activeProfileIndex = 0;

		// fill profile combo
		for (int profileIndex = 0; profileIndex < _dialog_Profiles.size(); profileIndex++) {

			final ColumnProfile columnProfile = _dialog_Profiles.get(profileIndex);

			_comboProfiles.add(columnProfile.name);

			if (columnProfile == _dialog_SelectedProfile) {
				activeProfileIndex = profileIndex;
			}
		}

		// select active profile
		_profileIndex_Selected = activeProfileIndex;
		_comboProfiles.select(activeProfileIndex);
		_profileIndex_Selected = activeProfileIndex;

		_isInProfileUpdate = false;
	}

	private void saveCurrentProfile_Columns() {

		// save columns
		_columnManager.setVisibleColumnIds_FromModifyDialog(_dialog_SelectedProfile, _columnViewer
				.getTable()
				.getItems());
	}

	private void saveCurrentProfile_Name() {

		final String comboText = _comboProfiles.getText();

		// save name
		_dialog_SelectedProfile.name = comboText;
	}

	private void setupColumnsInViewer() {

		// load columns into the viewer
		_columnViewer.setInput(new Object[0]);

		// check columns
		final ArrayList<ColumnDefinition> checkedColumns = new ArrayList<ColumnDefinition>();

		for (final ColumnDefinition colDef : _allDialogColumns) {
			if (colDef.isCheckedInDialog()) {
				checkedColumns.add(colDef);
			}
		}
		_columnViewer.setCheckedElements(checkedColumns.toArray());

		enableUpDownActions();
	}

}
