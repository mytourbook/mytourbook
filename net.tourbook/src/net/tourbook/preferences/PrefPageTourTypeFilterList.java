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
package net.tourbook.preferences;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;
import net.tourbook.ui.UI;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
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
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTourTypeFilterList extends PreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore		_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private IPropertyChangeListener		_prefChangeListener;

	private long						_dragStartViewerLeft;

	private boolean						_isModified;

	private ArrayList<TourType>			_tourTypes;
	private ArrayList<TourTypeFilter>	_filterList;

	private TourTypeFilter				_activeFilter;

	/*
	 * UI controls
	 */
	private TableViewer					_filterViewer;

	private CheckboxTableViewer			_tourTypeViewer;

	private Button						_btnNew;
	private Button						_btnRename;
	private Button						_btnRemove;
	private Button						_btnUp;
	private Button						_btnDown;

	private Button						_chkTourTypeContextMenu;

	public PrefPageTourTypeFilterList() {}

	public PrefPageTourTypeFilterList(final String title) {
		super(title);
	}

	public PrefPageTourTypeFilterList(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				if (event.getProperty().equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
					updateViewers();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite ui = createUI(parent);

		restoreState();

		addPrefListener();

		updateViewers();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_root_title);
		label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			createUI10FilterViewer(container);
			createUI20TourTypeViewer(container);
			createUI30Buttons(container);
		}

		// hint to use drag & drop
		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_dnd_hint);
		label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		/*
		 * show tour type context menu on mouse over
		 */
		_chkTourTypeContextMenu = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().indent(0, 10).applyTo(_chkTourTypeContextMenu);
		_chkTourTypeContextMenu.setText(Messages.Pref_Appearance_ShowTourTypeContextMenu);
		_chkTourTypeContextMenu.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				_isModified = true;
			}
		});

		// spacer
		new Label(parent, SWT.WRAP);

		return container;
	}

	private void createUI10FilterViewer(final Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(200, SWT.DEFAULT).applyTo(layouter);

		final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		TableViewerColumn tvc;

		_filterViewer = new TableViewer(table);

		// column: name + image
		tvc = new TableViewerColumn(_filterViewer, SWT.NONE);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourTypeFilter filter = ((TourTypeFilter) cell.getElement());
				final int filterType = filter.getFilterType();

				String filterName = null;
				Image filterImage = null;

				// set filter name/image
				switch (filterType) {
				case TourTypeFilter.FILTER_TYPE_DB:
					final TourType tourType = filter.getTourType();
					filterName = tourType.getName();
					filterImage = UI.getInstance().getTourTypeImage(tourType.getTypeId());
					break;

				case TourTypeFilter.FILTER_TYPE_SYSTEM:
					filterName = filter.getSystemFilterName();
					filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER_SYSTEM);
					break;

				case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
					filterName = filter.getTourTypeSet().getName();
					filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER);
					break;

				default:
					break;
				}

				cell.setText(filterName);
				cell.setImage(filterImage);
			}
		});
		layouter.addColumnData(new ColumnWeightData(1));

		_filterViewer.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return _filterList.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_filterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectFilter();
			}
		});

		_filterViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				onRenameFilterSet();
			}
		});

		/*
		 * set drag adapter
		 */
		_filterViewer.addDragSupport(
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
						final ISelection selection = _filterViewer.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dragStartViewerLeft = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_filterViewer) {

			private Widget	_tableItem;

			@Override
			public void dragOver(final DropTargetEvent dropEvent) {

				// keep table item
				_tableItem = dropEvent.item;

				super.dragOver(dropEvent);
			}

			@Override
			public boolean performDrop(final Object data) {

				if (data instanceof StructuredSelection) {
					final StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof TourTypeFilter) {

						final TourTypeFilter filterItem = (TourTypeFilter) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table filterTable = _filterViewer.getTable();

						/*
						 * check if drag was startet from this filter, remove the filter item before
						 * the new filter is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStartViewerLeft) {
							_filterViewer.remove(filterItem);
						}

						int filterIndex;

						if (_tableItem == null) {

							_filterViewer.add(filterItem);
							filterIndex = filterTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							filterIndex = filterTable.indexOf((TableItem) _tableItem);
							if (filterIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								_filterViewer.insert(filterItem, filterIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								_filterViewer.insert(filterItem, ++filterIndex);
							}
						}

						// reselect filter item
						_filterViewer.setSelection(new StructuredSelection(filterItem));

						// set focus to selection
						filterTable.setSelection(filterIndex);
						filterTable.setFocus();

						_isModified = true;

						return true;
					}
				}

				return false;
			}

			@Override
			public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

				final ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				if (selection instanceof StructuredSelection) {
					final Object dragFilter = ((StructuredSelection) selection).getFirstElement();
					if (target == dragFilter) {
						return false;
					}
				}

				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) == false) {
					return false;
				}

				return true;
			}

		};

		_filterViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createUI20TourTypeViewer(final Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(200, SWT.DEFAULT).applyTo(layouter);

		final Table table = new Table(
				layouter,
				(SWT.CHECK | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));

		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		_tourTypeViewer = new CheckboxTableViewer(table);

		TableViewerColumn tvc;

		// column: name
		tvc = new TableViewerColumn(_tourTypeViewer, SWT.NONE);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourType tourType = ((TourType) cell.getElement());
				cell.setText(tourType.getName());
				cell.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
			}
		});
		layouter.addColumnData(new ColumnWeightData(1));

		_tourTypeViewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return _tourTypes.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_tourTypeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				_isModified = true;
			}
		});

		_tourTypeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectTourType();
			}
		});

		_tourTypeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				/*
				 * invert check state
				 */
				final TourType tourType = (TourType) ((StructuredSelection) _tourTypeViewer.getSelection())
						.getFirstElement();

				final boolean isChecked = _tourTypeViewer.getChecked(tourType);

				_tourTypeViewer.setChecked(tourType, !isChecked);

//				getSelectedTourTypes();
			}
		});
	}

	private void createUI30Buttons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(container);
		{
			// button: new
			_btnNew = new Button(container, SWT.NONE);
			_btnNew.setText(Messages.Pref_TourTypeFilter_button_new);
			setButtonLayoutData(_btnNew);
			_btnNew.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onNewFilterSet();
				}
			});

			// button: rename
			_btnRename = new Button(container, SWT.NONE);
			_btnRename.setText(Messages.Pref_TourTypeFilter_button_rename);
			setButtonLayoutData(_btnRename);
			_btnRename.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onRenameFilterSet();
				}
			});

			// button: delete
			_btnRemove = new Button(container, SWT.NONE);
			_btnRemove.setText(Messages.Pref_TourTypeFilter_button_remove);
			setButtonLayoutData(_btnRemove);
			_btnRemove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onDeleteFilterSet();
				}
			});

			// spacer
			new Label(container, SWT.NONE);

			// button: up
			_btnUp = new Button(container, SWT.NONE);
			_btnUp.setText(Messages.PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_up);
			setButtonLayoutData(_btnUp);
			_btnUp.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onMoveUp();
				}
			});

			// button: down
			_btnDown = new Button(container, SWT.NONE);
			_btnDown.setText(Messages.PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_down);
			setButtonLayoutData(_btnDown);
			_btnDown.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onMoveDown();
				}
			});
		}
	}

	@Override
	public void dispose() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableButtons() {

		final IStructuredSelection selection = (IStructuredSelection) _filterViewer.getSelection();

		final TourTypeFilter filterItem = (TourTypeFilter) selection.getFirstElement();
		final Table filterTable = _filterViewer.getTable();

		_btnUp.setEnabled(filterItem != null && filterTable.getSelectionIndex() > 0);

		_btnDown.setEnabled(filterItem != null && filterTable.getSelectionIndex() < filterTable.getItemCount() - 1);

		_btnRename.setEnabled(filterItem != null
				&& filterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET);

		_btnRemove.setEnabled(filterItem != null
				&& filterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET);
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	public boolean isValid() {

		saveState();

		return true;
	}

	private void onDeleteFilterSet() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _filterViewer.getSelection())
				.getFirstElement();

		if (filterItem == null || filterItem.getFilterType() != TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {
			return;
		}

		final Table filterTable = _filterViewer.getTable();
		final int selectionIndex = filterTable.getSelectionIndex();

		_filterViewer.remove(filterItem);

		// select next filter item
		final int nextIndex = Math.min(filterTable.getItemCount() - 1, selectionIndex);
		_filterViewer.setSelection(new StructuredSelection(_filterViewer.getElementAt(nextIndex)));

		_isModified = true;
	}

	private void onMoveDown() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _filterViewer.getSelection())
				.getFirstElement();

		if (filterItem == null) {
			return;
		}

		final Table filterTable = _filterViewer.getTable();
		final int selectionIndex = filterTable.getSelectionIndex();

		if (selectionIndex < filterTable.getItemCount() - 1) {

			_filterViewer.remove(filterItem);
			_filterViewer.insert(filterItem, selectionIndex + 1);

			// reselect moved item
			_filterViewer.setSelection(new StructuredSelection(filterItem));

			if (filterTable.getSelectionIndex() == filterTable.getItemCount() - 1) {
				_btnUp.setFocus();
			} else {
				_btnDown.setFocus();
			}

			_isModified = true;
		}
	}

	private void onMoveUp() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _filterViewer.getSelection())
				.getFirstElement();

		if (filterItem == null) {
			return;
		}

		final Table filterTable = _filterViewer.getTable();

		final int selectionIndex = filterTable.getSelectionIndex();
		if (selectionIndex > 0) {
			_filterViewer.remove(filterItem);
			_filterViewer.insert(filterItem, selectionIndex - 1);

			// reselect moved item
			_filterViewer.setSelection(new StructuredSelection(filterItem));

			if (filterTable.getSelectionIndex() == 0) {
				_btnDown.setFocus();
			} else {
				_btnUp.setFocus();
			}

			_isModified = true;
		}
	}

	private void onNewFilterSet() {

		final InputDialog inputDialog = new InputDialog(
				getShell(),
				Messages.Pref_TourTypeFilter_dlg_new_title,
				Messages.Pref_TourTypeFilter_dlg_new_message,
				UI.EMPTY_STRING,
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// create new filterset
		final TourTypeFilterSet filterSet = new TourTypeFilterSet();
		filterSet.setName(inputDialog.getValue().trim());

		final TourTypeFilter tourTypeFilter = new TourTypeFilter(filterSet);

		// update model and viewer
		_filterViewer.add(tourTypeFilter);
		_filterList.add(tourTypeFilter);

		// select new set
		_filterViewer.setSelection(new StructuredSelection(tourTypeFilter), true);

		_tourTypeViewer.getTable().setFocus();

		_isModified = true;
	}

	private void onRenameFilterSet() {

		final TourTypeFilter filter = (TourTypeFilter) ((StructuredSelection) _filterViewer.getSelection())
				.getFirstElement();

		final InputDialog inputDialog = new InputDialog(
				getShell(),
				Messages.Pref_TourTypeFilter_dlg_rename_title,
				Messages.Pref_TourTypeFilter_dlg_rename_message,
				filter.getFilterName(),
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// update model
		filter.setName(inputDialog.getValue().trim());

		// update viewer
		_filterViewer.update(filter, null);

		_isModified = true;
	}

	private void onSelectFilter() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((StructuredSelection) _filterViewer.getSelection())
				.getFirstElement();

		if (filterItem == null) {
			return;
		}

		_activeFilter = filterItem;

		final int filterType = filterItem.getFilterType();

		Object[] tourTypes;
		switch (filterType) {
		case TourTypeFilter.FILTER_TYPE_SYSTEM:
			final int systemFilter = filterItem.getSystemFilterId();
			_tourTypeViewer.setAllChecked(systemFilter == TourTypeFilter.SYSTEM_FILTER_ID_ALL);
			_tourTypeViewer.getTable().setEnabled(false);

			break;

		case TourTypeFilter.FILTER_TYPE_DB:
			final TourType tourType = filterItem.getTourType();
			_tourTypeViewer.setCheckedElements(new Object[] { tourType });
			_tourTypeViewer.getTable().setEnabled(false);
			break;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
			_tourTypeViewer.getTable().setEnabled(true);
			tourTypes = filterItem.getTourTypeSet().getTourTypes();
			if (tourTypes == null) {
				_tourTypeViewer.setAllChecked(false);
			} else {
				_tourTypeViewer.setCheckedElements(tourTypes);
			}
			break;

		default:
			break;
		}

		enableButtons();
	}

	private void onSelectTourType() {

		if (_activeFilter == null) {
			return;
		}

		// set tour types for current filter set
		if (_activeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {
			_activeFilter.getTourTypeSet().setTourTypes(_tourTypeViewer.getCheckedElements());
		}
	}

	@Override
	protected void performDefaults() {

		_isModified = true;

		_chkTourTypeContextMenu.setSelection(_prefStore
				.getDefaultBoolean(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU));

		super.performDefaults();

		// this do not work, I have no idea why, but with the apply button it works :-(
//		fireModificationEvent();
	}

	@Override
	public boolean performOk() {

		saveState();

		return true;
	}

	private void restoreState() {

		_chkTourTypeContextMenu.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU));

	}

	private void saveState() {

		if (_isModified) {

			_isModified = false;

			TourTypeFilterManager.writeXMLFilterFile(_filterViewer);

			_prefStore.setValue(
					ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU,
					_chkTourTypeContextMenu.getSelection());

			// fire modify event
			_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
		}
	}

	private void updateViewers() {

		_filterList = TourTypeFilterManager.readTourTypeFilters();
		_tourTypes = TourDatabase.getAllTourTypes();

		// show contents in the viewers
		_filterViewer.setInput(new Object());
		_tourTypeViewer.setInput(new Object());

		enableButtons();
	}

}
