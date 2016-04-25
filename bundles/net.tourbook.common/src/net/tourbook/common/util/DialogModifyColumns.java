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
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.ValueFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
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
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;

public class DialogModifyColumns extends TrayDialog {

	private static final String[]		IS_SORTER_PROPERTY	= new String[] { "DummyProperty" }; //$NON-NLS-1$

	private Action						_actionShowHideCategory;

	private ColumnManager				_columnManager;

	/** Model for the column viewer */
	private ArrayList<ColumnDefinition>	_columnViewerModel;

	private ArrayList<ColumnDefinition>	_allDefinedColumns;
	//
	private PixelConverter				_pc;
	//
	private long						_dndDragStartViewerLeft;
	private Object[]					_dndCheckedElements;
	//
	private ColumnProfile				_selectedProfile;
	private ArrayList<ColumnProfile>	_columnMgr_Profiles;

	private ArrayList<ColumnProfile>	_dialog_Profiles;
	//
	private boolean						_isInUpdate;
	//
	private boolean						_isShowCategory;
	private boolean						_isCategoryAvailable;
	private int							_categoryColumnWidth;
	//
	private ViewerComparator			_profileComparator	= new ProfileComparator();
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
	private Button						_btnProfile_Rename;
	private Button						_btnColumn_Sort;
	//
	private CheckboxTableViewer			_columnViewer;
	private CheckboxTableViewer			_profileViewer;
	private Composite					_uiContainer;

	private TableColumn					_categoryColumn;

	public class ActionCategoryColumn extends Action {

		public ActionCategoryColumn() {

			super(null, AS_CHECK_BOX);

			setImageDescriptor(CommonActivator.getImageDescriptor(Messages.Image__ColumnCategory));
			setToolTipText(Messages.ColumnModifyDialog_Button_ShowCategoryColumn_Tooltip);
		}

		@Override
		public void run() {
			action_ShowHideCategory();
		}
	}

	public class ProfileComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final ColumnProfile profile1 = (ColumnProfile) e1;
			final ColumnProfile profile2 = (ColumnProfile) e2;

			return profile1.name.compareTo(profile2.name);
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {
			// force resorting when a name is renamed
			return true;
		}
	}

	public DialogModifyColumns(	final Shell parentShell,
								final ColumnManager columnManager,
								final ArrayList<ColumnDefinition> allRearrangedColumns,
								final ArrayList<ColumnDefinition> allDefinedColumns,
								final ColumnProfile columnMgr_ActiveProfile,
								final ArrayList<ColumnProfile> columnMgr_Profiles) {

		super(parentShell);

		_columnManager = columnManager;
		_columnViewerModel = allRearrangedColumns;
		_allDefinedColumns = allDefinedColumns;

		_columnMgr_Profiles = columnMgr_Profiles;

		// use cloned profiles in the dialog
		_dialog_Profiles = new ArrayList<>();
		for (final ColumnProfile columnMgr_Profile : columnMgr_Profiles) {

			final ColumnProfile clonedProfile = columnMgr_Profile.clone();

			_dialog_Profiles.add(clonedProfile);

			// set active profile
			if (columnMgr_Profile == columnMgr_ActiveProfile) {
				_selectedProfile = clonedProfile;
			}
		}
		if (_selectedProfile == null) {
			_selectedProfile = _dialog_Profiles.get(0);
		}

		sortDialogProfiles();

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	private void action_ShowHideCategory() {

		// toggle column
		_isShowCategory = !_isShowCategory;

		_categoryColumn.setWidth(_isShowCategory ? _categoryColumnWidth : 0);
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.ColumnModifyDialog_Dialog_title);
	}

	private void createActions() {

		_actionShowHideCategory = new ActionCategoryColumn();
	}

	/**
	 * Create model for the column viewer from a {@link ColumnProfile}.
	 * 
	 * @param columnProfile
	 * @return Returns ALL columns, first the visible then the hidden columns.
	 */
	private ArrayList<ColumnDefinition> createColumnViewerModel(final ColumnProfile columnProfile) {

		// Set column definitions in the ColumnProfile from the visible id's.
		_columnManager.setVisibleColDefs(columnProfile);

		final ArrayList<ColumnDefinition> modelColumns = new ArrayList<ColumnDefinition>();

		try {

			final ArrayList<ColumnDefinition> allClonedColDef = new ArrayList<ColumnDefinition>();

			/*
			 * Clone original column definitions
			 */
			for (final ColumnDefinition definedColDef : _allDefinedColumns) {
				allClonedColDef.add((ColumnDefinition) definedColDef.clone());
			}

			/*
			 * Add visible columns
			 */
			for (final ColumnDefinition colDef : columnProfile.visibleColumnDefinitions) {

				final ColumnDefinition modelColDef = (ColumnDefinition) colDef.clone();

				modelColDef.setIsCheckedInDialog(true);

				modelColDef.setColumnFormat(colDef.getValueFormat());
				modelColDef.setColumnWidth(colDef.getColumnWidth());

				modelColumns.add(modelColDef);

				allClonedColDef.remove(colDef);
			}

			/*
			 * Add not visible columns
			 */
			for (final ColumnDefinition colDef : allClonedColDef) {

				colDef.setIsCheckedInDialog(false);

				// set default values
				colDef.setColumnWidth(colDef.getDefaultColumnWidth());
				colDef.setColumnFormat(colDef.getDefaultValueFormat());

				modelColumns.add(colDef);
			}

			/*
			 * Set create index, otherwise save/restore do not work!!!
			 */
			int createIndex = 0;
			for (final ColumnDefinition colDef : modelColumns) {
				colDef.setCreateIndex(createIndex++);
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return modelColumns;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		// set default width
		final GridData gd = (GridData) dlgContainer.getLayoutData();
		gd.widthHint = 600;
		gd.heightHint = 800;

		restoreState_BeforeUI();

		createActions();
		createUI(dlgContainer);

		setupColumnsInViewer();

		restoreState();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_uiContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_uiContainer);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(_uiContainer);
		{
			createUI_10_Profile(_uiContainer);
			createUI_12_ProfileViewer(_uiContainer);
			createUI_14_ProfileActions(_uiContainer);

			createUI_70_ColumnsHeader(_uiContainer);
			createUI_72_ColumnsViewer(_uiContainer);
			createUI_74_ColumnActions(_uiContainer);
			createUI_76_Hints(_uiContainer);
		}
	}

	private void createUI_10_Profile(final Composite parent) {

		/*
		 * Label: Profile
		 */
		{
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.ColumnModifyDialog_Label_Profile);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.span(2, 1)
					.applyTo(label);
		}

	}

	private void createUI_12_ProfileViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(7))
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		final Table table = new Table(layoutContainer, //
				SWT.CHECK //
						| SWT.SINGLE
//						| SWT.H_SCROLL
//						| SWT.V_SCROLL
						| SWT.BORDER
						| SWT.FULL_SELECTION);

		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		_profileViewer = new CheckboxTableViewer(table);

		_profileViewer.setUseHashlookup(true);
		_profileViewer.setComparator(_profileComparator);

		_profileViewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public void dispose() {}

			@Override
			public Object[] getElements(final Object inputElement) {
				return _dialog_Profiles.toArray();
			}

			@Override
			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_profileViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {
				onProfileViewer_CheckStateChanged(event);
			}
		});

		_profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onProfileViewer_Select(event);
			}
		});

		_profileViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				onProfile_Rename();
			}
		});

		/*
		 * Create single column
		 */
		TableViewerColumn tvc;

		// column: name
		tvc = new TableViewerColumn(_profileViewer, SWT.NONE);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final ColumnProfile profile = ((ColumnProfile) cell.getElement());

				cell.setText(profile.name);
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(1));
	}

	private void createUI_14_ProfileActions(final Composite parent) {

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
				_btnProfile_New.setText(Messages.App_Action_New_WithConfirm);
				_btnProfile_New.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Add();
					}
				});
				setButtonLayoutData(_btnProfile_New);

			}

			/*
			 * Button: Rename
			 */
			{
				_btnProfile_Rename = new Button(container, SWT.NONE);
				_btnProfile_Rename.setText(Messages.App_Action_Rename_WithConfirm);
				_btnProfile_Rename.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Rename();
					}
				});
				setButtonLayoutData(_btnProfile_Rename);

			}

			// spacer
			new Label(container, SWT.NONE);

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

	private void createUI_70_ColumnsHeader(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(0, 20)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			/*
			 * Action: Show/hide category
			 */
			if (_isCategoryAvailable) {

				final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

				final ToolBarManager tbm = new ToolBarManager(toolbar);

				tbm.add(_actionShowHideCategory);

				tbm.update(true);
			}

			/*
			 * Label: Column
			 */
			{
				final Label label = new Label(container, SWT.WRAP);
				label.setText(Messages.ColumnModifyDialog_Label_Column);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.END)
						.applyTo(label);
			}
		}

		// 2nd column
		new Label(parent, SWT.NONE);
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
		table.setLinesVisible(false);

		_columnViewer = new CheckboxTableViewer(table);

		defineAllColumns(tableLayout);
		reorderColumns(table);

		_columnViewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public void dispose() {}

			@Override
			public Object[] getElements(final Object inputElement) {
				return _columnViewerModel.toArray();
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
				saveState_CurrentProfileColumns();
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
			/*
			 * Button: Move Up
			 */
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
			}

			/*
			 * Button: Move Down
			 */
			{
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
			}

			// spacer
			new Label(container, SWT.NONE);

			/*
			 * Button: Select All
			 */
			{
				_btnColumn_SelectAll = new Button(container, SWT.NONE);
				_btnColumn_SelectAll.setText(Messages.ColumnModifyDialog_Button_select_all);
				_btnColumn_SelectAll.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {

						// update model
						for (final ColumnDefinition colDef : _columnViewerModel) {
							colDef.setIsCheckedInDialog(true);
						}

						// update viewer
						_columnViewer.setAllChecked(true);
					}
				});
				setButtonLayoutData(_btnColumn_SelectAll);
			}

			/*
			 * Button: Deselect All
			 */
			{
				_btnColumn_DeselectAll = new Button(container, SWT.NONE);
				_btnColumn_DeselectAll.setText(Messages.ColumnModifyDialog_Button_deselect_all);
				_btnColumn_DeselectAll.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {

						// list with all columns which must be checked
						final ArrayList<ColumnDefinition> checkedElements = new ArrayList<ColumnDefinition>();

						// update model
						for (final ColumnDefinition colDef : _columnViewerModel) {
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
			}

			// spacer
			new Label(container, SWT.NONE);

			/*
			 * Button: Default
			 */
			{
				_btnColumn_Default = new Button(container, SWT.NONE);
				_btnColumn_Default.setText(Messages.ColumnModifyDialog_Button_default);
				_btnColumn_Default.setToolTipText(Messages.ColumnModifyDialog_Button_Default_Tooltip);
				_btnColumn_Default.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {

						/*
						 * copy all defined columns into the dialog columns
						 */

						_columnViewerModel = getDefaultColumns(true);

						setupColumnsInViewer();
					}
				});
				setButtonLayoutData(_btnColumn_Default);
			}

			/*
			 * Button: Sort
			 */
			{
				_btnColumn_Sort = new Button(container, SWT.NONE);
				_btnColumn_Sort.setText(Messages.ColumnModifyDialog_Button_Sort);
				_btnColumn_Sort.setToolTipText(Messages.ColumnModifyDialog_Button_Sort_Tooltip);
				_btnColumn_Sort.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {

						/*
						 * copy all defined columns into the dialog columns
						 */

						_columnViewerModel = getDefaultColumns(false);

						setupColumnsInViewer();
					}
				});
				setButtonLayoutData(_btnColumn_Sort);
			}
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

	private void defineAllColumns(final TableColumnLayout tableLayout) {

		defineColumn_ColumnName(tableLayout);
		defineColumn_Unit(tableLayout);
		defineColumn_Format(tableLayout);
		defineColumn_Width(tableLayout);

		/**
		 * This column CANNOT be the first column because it would contain the checkbox, but with
		 * the reorder feature this column is set as first column :-)
		 */
		defineColumn_Category(tableLayout);
	}

	/**
	 * Column: Category
	 */
	private void defineColumn_Category(final TableColumnLayout tableLayout) {

		if (_isCategoryAvailable) {

			final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

			final TableColumn tc = tvc.getColumn();
			tc.setMoveable(true);
			tc.setText(Messages.ColumnModifyDialog_Column_Category);

			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
					cell.setText(colDef.getColumnCategory());

					// paint columns in a different color which can't be hidden
					if (colDef.canModifyVisibility() == false) {
						cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
					}
				}
			});
			_categoryColumn = tc;

			_categoryColumnWidth = _pc.convertWidthInCharsToPixels(20);
			int categoryColumnWidth;
			if (_columnManager.isShowCategory()) {
				categoryColumnWidth = _categoryColumnWidth;
			} else {
				// hide column
				categoryColumnWidth = 0;
			}

			tableLayout.setColumnData(tc, new ColumnPixelData(categoryColumnWidth, true));
		}
	}

	/**
	 * Column: Label
	 */
	private void defineColumn_ColumnName(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();
		tc.setMoveable(true);
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
		tableLayout.setColumnData(tc, new ColumnWeightData(30, true));
	}

	/**
	 * Column: Format
	 */
	private void defineColumn_Format(final TableColumnLayout tableLayout) {
		
		final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);
		
		final TableColumn tc = tvc.getColumn();
		tc.setMoveable(true);
		tc.setText(Messages.ColumnModifyDialog_Column_Format);
		
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				
				final ColumnDefinition colDef = (ColumnDefinition) cell.getElement();
				final ValueFormat valueFormat = colDef.getValueFormat();
				
				if (valueFormat == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					final String valueFormatterText = ColumnManager.getValueFormatterName(valueFormat);

					cell.setText(valueFormatterText);
				}

				// paint columns in a different color which can't be hidden
				if (colDef.canModifyVisibility() == false) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
				}
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(14), true));
	}

	/**
	 * Column: Unit
	 */
	private void defineColumn_Unit(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();
		tc.setText(Messages.ColumnModifyDialog_column_unit);
		tc.setMoveable(true);

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
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(14), true));
	}

	/**
	 * Column: Width
	 */
	private void defineColumn_Width(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_columnViewer, SWT.TRAIL);

		final TableColumn tc = tvc.getColumn();
		tc.setMoveable(true);
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
	}

	private void enableProfileActions() {

		final int numProfiles = _dialog_Profiles.size();

		_btnProfile_Remove.setEnabled(numProfiles > 1);
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

	/**
	 * Create all columns in default order and selection.
	 * 
	 * @param isSetDefaultProperties
	 * @return Returns all {@link ColumnDefinition}s in default order/selection.
	 */
	private ArrayList<ColumnDefinition> getDefaultColumns(final boolean isSetDefaultProperties) {

		final ArrayList<ColumnDefinition> allDialogColumns = new ArrayList<ColumnDefinition>();

		for (final ColumnDefinition definedColDef : _allDefinedColumns) {
			try {

				// clone column
				final ColumnDefinition colDefClone = (ColumnDefinition) definedColDef.clone();

				if (isSetDefaultProperties) {

					// visible columns in the viewer will be checked

					colDefClone.setIsCheckedInDialog(definedColDef.isDefaultColumn());

					colDefClone.setColumnWidth(definedColDef.getDefaultColumnWidth());
					colDefClone.setColumnFormat(definedColDef.getDefaultValueFormat());

				} else {

					// set properties from the current settings

					final String definedColumnId = definedColDef.getColumnId();

					for (final ColumnDefinition currentColDef : _columnViewerModel) {

						if (currentColDef.getColumnId().equals(definedColumnId)) {

							colDefClone.setIsCheckedInDialog(currentColDef.isCheckedInDialog());

							colDefClone.setColumnWidth(currentColDef.getColumnWidth());
							colDefClone.setColumnFormat(currentColDef.getColumnFormat());

							break;
						}
					}
				}

				allDialogColumns.add(colDefClone);

			} catch (final CloneNotSupportedException e) {
				StatusUtil.log(e);
			}
		}

		return allDialogColumns;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		final IDialogSettings state = CommonActivator.getState(getClass().getName() + "_DialogBounds");//$NON-NLS-1$
//		final IDialogSettings state = null;

		return state;
	}

	private ColumnProfile getSelectedProfile() {

		final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();

		final ColumnProfile selectedProfile = (ColumnProfile) selection.getFirstElement();

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

		saveState();

		super.okPressed();
	}

	private void onProfile_Add() {

		final InputDialog inputDialog = new InputDialog(
				getShell(),
				Messages.ColumnModifyDialog_Dialog_Profile_Title,
				Messages.ColumnModifyDialog_Dialog_ProfileNew_Message,
				UI.EMPTY_STRING,
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// save current profile columns
		saveState_CurrentProfileColumns();

		/*
		 * Create new profile
		 */

		// create default columns for a new profile
		_columnViewerModel = getDefaultColumns(true);

		final ColumnProfile newProfile = new ColumnProfile();

		// set profile name
		newProfile.name = inputDialog.getValue().trim();

		// update model
		_dialog_Profiles.add(newProfile);
		_selectedProfile = newProfile;

		// update UI
		_profileViewer.add(newProfile);

		_profileViewer.setCheckedElements(new ColumnProfile[] { newProfile });
		_profileViewer.setSelection(new StructuredSelection(newProfile), true);

		// force that horizontal scrollbar is NOT visible
		_uiContainer.layout(true, true);

		enableProfileActions();

		_profileViewer.getTable().setFocus();
	}

	private void onProfile_Remove() {

		_isInUpdate = true;

		final Table profileTable = _profileViewer.getTable();

		final int selectedIndex = profileTable.getSelectionIndex();

		final ColumnProfile selectedProfile = getSelectedProfile();

		// update UI
		_dialog_Profiles.remove(selectedProfile);

		// update model
		_profileViewer.remove(selectedProfile);

		/*
		 * Select profile at the same position
		 */
		final int profilesSize = _dialog_Profiles.size();
		int newIndex = selectedIndex;
		if (newIndex >= profilesSize) {
			newIndex = profilesSize - 1;
		}

		int nextIndex = 0;
		final TableItem nextItem = profileTable.getItem(newIndex);
		final ColumnProfile nextProfile = (ColumnProfile) nextItem.getData();

		for (int profileIndex = 0; profileIndex < _dialog_Profiles.size(); profileIndex++) {

			final ColumnProfile profile = _dialog_Profiles.get(profileIndex);

			if (profile.getID() == nextProfile.getID()) {
				nextIndex = profileIndex;
				break;
			}
		}
		_selectedProfile = _dialog_Profiles.get(nextIndex);

		// update UI
		_profileViewer.setCheckedElements(new ColumnProfile[] { _selectedProfile });
		_profileViewer.setSelection(new StructuredSelection(_selectedProfile), true);

		enableProfileActions();

		profileTable.setFocus();

		_isInUpdate = false;
	}

	private void onProfile_Rename() {

		final ColumnProfile selectedProfile = getSelectedProfile();

		final InputDialog inputDialog = new InputDialog(
				getShell(),
				Messages.ColumnModifyDialog_Dialog_Profile_Title,
				Messages.ColumnModifyDialog_Dialog_ProfileRename_Message,
				selectedProfile.name,
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			// canceled
			return;
		}

		// get name
		final String modifiedProfileName = inputDialog.getValue().trim();

		// update model
		selectedProfile.name = modifiedProfileName;

		_profileViewer.update(selectedProfile, IS_SORTER_PROPERTY);

		// focus can have changed when resorted, set focus to the selected item
		int selectedIndex = 0;
		final Table table = _profileViewer.getTable();
		final TableItem[] items = table.getItems();
		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {

			final TableItem tableItem = items[itemIndex];

			if (tableItem.getData() == selectedProfile) {
				selectedIndex = itemIndex;
			}
		}
		table.setSelection(selectedIndex);
		table.showSelection();

		_profileViewer.getTable().setFocus();
	}

	private void onProfileViewer_CheckStateChanged(final CheckStateChangedEvent event) {

		final boolean isChecked = event.getChecked();
		final ColumnProfile checkedProfile = (ColumnProfile) event.getElement();

		if (isChecked) {

			// uncheck others

			_profileViewer.setCheckedElements(new ColumnProfile[] { checkedProfile });

		} else {

			// ensure one is checked

			_profileViewer.setCheckedElements(new ColumnProfile[] { checkedProfile });
		}

		_profileViewer.setSelection(new StructuredSelection(checkedProfile));
	}

	private void onProfileViewer_Select(final SelectionChangedEvent event) {

		if (_isInUpdate) {
			return;
		}

		final ColumnProfile selectedProfile = getSelectedProfile();

		if (selectedProfile == _selectedProfile) {
			// no new selection, this occures when another profile is checked
			return;
		}

		// keep previous selected columns
		saveState_CurrentProfileColumns();

		_selectedProfile = selectedProfile;

		// check the selected profile
		_profileViewer.setCheckedElements(new ColumnProfile[] { selectedProfile });

		/*
		 * Update column viewer from the selected profile
		 */
		_columnViewerModel = createColumnViewerModel(selectedProfile);

		setupColumnsInViewer();

		enableProfileActions();
	}

	/**
	 * Reorder columns, set category column to the first but the checkbox keeps with the column name
	 * column.
	 */
	private void reorderColumns(final Table table) {

		if (_isCategoryAvailable) {

			final int[] oldColumnOrder = table.getColumnOrder();
			final int numColumns = oldColumnOrder.length;
			final int[] newColumnOrder = new int[numColumns];

			// set last column to the first
			newColumnOrder[0] = oldColumnOrder[numColumns - 1];

			for (int columnIndex = 1; columnIndex < numColumns; columnIndex++) {
				newColumnOrder[columnIndex] = oldColumnOrder[columnIndex - 1];
			}

			table.setColumnOrder(newColumnOrder);
		}
	}

	private void restoreState() {

		/*
		 * Show/hide category
		 */
		_isShowCategory = _columnManager.isShowCategory();
		_actionShowHideCategory.setChecked(_isShowCategory);

		// load viewer
		_profileViewer.setInput(new Object());

		// select active profile
		_profileViewer.setSelection(new StructuredSelection(_selectedProfile), true);
		_profileViewer.setCheckedElements(new ColumnProfile[] { _selectedProfile });

		enableProfileActions();
	}

	private void restoreState_BeforeUI() {

		_isCategoryAvailable = _columnManager.isCategoryAvailable();
	}

	private void saveState() {

		saveState_CurrentProfileColumns();

		// replace column mgr profiles
		_columnMgr_Profiles.clear();
		_columnMgr_Profiles.addAll(_dialog_Profiles);

		_columnManager.setIsShowCategory(_isShowCategory);

		_columnManager.updateColumns(//
				_selectedProfile,
				_columnViewer.getTable().getItems());
	}

	/**
	 * Set {@link ColumnProfile#visibleColumnIds} from the current column viewer.
	 */
	private void saveState_CurrentProfileColumns() {

		// save columns
		_columnManager.setVisibleColumnIds_FromModifyDialog(//
				_selectedProfile,
				_columnViewer.getTable().getItems());
	}

	private void setupColumnsInViewer() {

		// load columns into the viewer
		_columnViewer.setInput(new Object[0]);

		// check columns
		final ArrayList<ColumnDefinition> checkedColumns = new ArrayList<ColumnDefinition>();

		for (final ColumnDefinition colDef : _columnViewerModel) {
			if (colDef.isCheckedInDialog()) {
				checkedColumns.add(colDef);
			}
		}
		_columnViewer.setCheckedElements(checkedColumns.toArray());

		enableUpDownActions();
	}

	private void sortDialogProfiles() {

		Collections.sort(_dialog_Profiles, new Comparator<ColumnProfile>() {
			@Override
			public int compare(final ColumnProfile colProfile1, final ColumnProfile colProfile2) {
				return colProfile1.name.compareTo(colProfile2.name);
			}
		});
	}

}
