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
package net.tourbook.preferences;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourTypeContributionItem;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;
import net.tourbook.ui.UI;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
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
import org.eclipse.swt.layout.GridLayout;
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

	private TableViewer					fFilterViewer;
	private CheckboxTableViewer			fTourTypeViewer;

	private long						fDragStartViewerLeft;

	private Button						fBtnNew;
	private Button						fBtnRename;
	private Button						fBtnRemove;
	private Button						fBtnUp;
	private Button						fBtnDown;

	private boolean						fIsModified;

	private ArrayList<TourType>			fTourTypes;
	private ArrayList<TourTypeFilter>	fFilterList;

	private TourTypeFilter				fActiveFilter;
	private IPropertyChangeListener		fPrefChangeListener;

	public PrefPageTourTypeFilterList() {}

	public PrefPageTourTypeFilterList(final String title) {
		super(title);
	}

	public PrefPageTourTypeFilterList(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				if (event.getProperty().equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
					updateViewers();
				}
			}
		};

		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void createButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);

		// button: new
		fBtnNew = new Button(container, SWT.NONE);
		fBtnNew.setText(Messages.Pref_TourTypeFilter_button_new);
		setButtonLayoutData(fBtnNew);
		fBtnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onNewFilterSet();
			}
		});

		// button: rename
		fBtnRename = new Button(container, SWT.NONE);
		fBtnRename.setText(Messages.Pref_TourTypeFilter_button_rename);
		setButtonLayoutData(fBtnRename);
		fBtnRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onRenameFilterSet();
			}
		});

		// button: delete
		fBtnRemove = new Button(container, SWT.NONE);
		fBtnRemove.setText(Messages.Pref_TourTypeFilter_button_remove);
		setButtonLayoutData(fBtnRemove);
		fBtnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onDeleteFilterSet();
			}
		});

		// spacer
		new Label(container, SWT.NONE);

		// button: up
		fBtnUp = new Button(container, SWT.NONE);
		fBtnUp.setText(Messages.PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_up);
		setButtonLayoutData(fBtnUp);
		fBtnUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onMoveUp();
			}
		});

		// button: down
		fBtnDown = new Button(container, SWT.NONE);
		fBtnDown.setText(Messages.PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_down);
		setButtonLayoutData(fBtnDown);
		fBtnDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onMoveDown();
			}
		});

	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite viewerContainer = createUI(parent);

		addPrefListener();

		updateViewers();

		return viewerContainer;
	}

	private void createFilterViewer(final Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 20;
		layouter.setLayoutData(gd);

		final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		TableViewerColumn tvc;

		fFilterViewer = new TableViewer(table);

		// column: name + image
		tvc = new TableViewerColumn(fFilterViewer, SWT.NONE);
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

		fFilterViewer.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return fFilterList.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		fFilterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectFilter();
			}
		});

		fFilterViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				onRenameFilterSet();
			}
		});

		// set drag adapter
		fFilterViewer.addDragSupport(DND.DROP_MOVE,
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
						final ISelection selection = fFilterViewer.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(fDragStartViewerLeft = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		// set drop adapter
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(fFilterViewer) {

			private Widget	fTableItem;

			@Override
			public void dragOver(final DropTargetEvent event) {

				// keep table item
				fTableItem = event.item;

				super.dragOver(event);
			}

			@Override
			public boolean performDrop(final Object data) {

				if (data instanceof StructuredSelection) {
					final StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof TourTypeFilter) {

						final TourTypeFilter filterItem = (TourTypeFilter) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table filterTable = fFilterViewer.getTable();

						/*
						 * check if drag was startet from this filter, remove the filter item before
						 * the new filter is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == fDragStartViewerLeft) {
							fFilterViewer.remove(filterItem);
						}

						int filterIndex;

						if (fTableItem == null) {

							fFilterViewer.add(filterItem);
							filterIndex = filterTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							filterIndex = filterTable.indexOf((TableItem) fTableItem);
							if (filterIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								fFilterViewer.insert(filterItem, filterIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								fFilterViewer.insert(filterItem, ++filterIndex);
							}
						}

						// reselect filter item
						fFilterViewer.setSelection(new StructuredSelection(filterItem));

						// set focus to selection
						filterTable.setSelection(filterIndex);
						filterTable.setFocus();

						fIsModified = true;

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

		fFilterViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createTourTypeViewer(final Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 20;
		layouter.setLayoutData(gd);

		final Table table = new Table(layouter,
				(SWT.CHECK | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));

		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		fTourTypeViewer = new CheckboxTableViewer(table);

		TableViewerColumn tvc;

		// column: name
		tvc = new TableViewerColumn(fTourTypeViewer, SWT.NONE);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourType tourType = ((TourType) cell.getElement());
				cell.setText(tourType.getName());
				cell.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
			}
		});
		layouter.addColumnData(new ColumnWeightData(1));

		fTourTypeViewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return fTourTypes.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		fTourTypeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				fIsModified = true;
			}
		});

		fTourTypeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectTourType();
			}
		});

		fTourTypeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				/*
				 * invert check state
				 */
				final TourType tourType = (TourType) ((StructuredSelection) fTourTypeViewer.getSelection()).getFirstElement();

				final boolean isChecked = fTourTypeViewer.getChecked(tourType);

				fTourTypeViewer.setChecked(tourType, !isChecked);

//				getSelectedTourTypes();
			}
		});
	}

	private Composite createUI(final Composite parent) {

		Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_root_title);
		label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

		// container
		final Composite viewerContainer = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		viewerContainer.setLayout(gl);
		viewerContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createFilterViewer(viewerContainer);
		createTourTypeViewer(viewerContainer);
		createButtons(viewerContainer);

		// hint to use drag & drop
		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_dnd_hint);
		label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		// spacer
		new Label(parent, SWT.WRAP);
		return viewerContainer;
	}

	@Override
	public void dispose() {

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	private void enableButtons() {

		final IStructuredSelection selection = (IStructuredSelection) fFilterViewer.getSelection();

		final TourTypeFilter filterItem = (TourTypeFilter) selection.getFirstElement();
		final Table filterTable = fFilterViewer.getTable();

		fBtnUp.setEnabled(filterItem != null && filterTable.getSelectionIndex() > 0);

		fBtnDown.setEnabled(filterItem != null && filterTable.getSelectionIndex() < filterTable.getItemCount() - 1);

		fBtnRename.setEnabled(filterItem != null
				&& filterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET);

		fBtnRemove.setEnabled(filterItem != null
				&& filterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET);
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean isValid() {

		saveFilterList();

		return true;
	}

	private void onDeleteFilterSet() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) fFilterViewer.getSelection()).getFirstElement();

		if (filterItem == null || filterItem.getFilterType() != TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {
			return;
		}

		final Table filterTable = fFilterViewer.getTable();
		final int selectionIndex = filterTable.getSelectionIndex();

		fFilterViewer.remove(filterItem);

		// select next filter item
		final int nextIndex = Math.min(filterTable.getItemCount() - 1, selectionIndex);
		fFilterViewer.setSelection(new StructuredSelection(fFilterViewer.getElementAt(nextIndex)));
	}

	private void onMoveDown() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) fFilterViewer.getSelection()).getFirstElement();

		if (filterItem == null) {
			return;
		}

		final Table filterTable = fFilterViewer.getTable();
		final int selectionIndex = filterTable.getSelectionIndex();

		if (selectionIndex < filterTable.getItemCount() - 1) {

			fFilterViewer.remove(filterItem);
			fFilterViewer.insert(filterItem, selectionIndex + 1);

			// reselect moved item
			fFilterViewer.setSelection(new StructuredSelection(filterItem));

			if (filterTable.getSelectionIndex() == filterTable.getItemCount() - 1) {
				fBtnUp.setFocus();
			} else {
				fBtnDown.setFocus();
			}

			fIsModified = true;
		}
	}

	private void onMoveUp() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) fFilterViewer.getSelection()).getFirstElement();

		if (filterItem == null) {
			return;
		}

		final Table filterTable = fFilterViewer.getTable();

		final int selectionIndex = filterTable.getSelectionIndex();
		if (selectionIndex > 0) {
			fFilterViewer.remove(filterItem);
			fFilterViewer.insert(filterItem, selectionIndex - 1);

			// reselect moved item
			fFilterViewer.setSelection(new StructuredSelection(filterItem));

			if (filterTable.getSelectionIndex() == 0) {
				fBtnDown.setFocus();
			} else {
				fBtnUp.setFocus();
			}

			fIsModified = true;
		}
	}

	private void onNewFilterSet() {

		final InputDialog inputDialog = new InputDialog(getShell(),
				Messages.Pref_TourTypeFilter_dlg_new_title,
				Messages.Pref_TourTypeFilter_dlg_new_message,
				"", //$NON-NLS-1$
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
		fFilterViewer.add(tourTypeFilter);
		fFilterList.add(tourTypeFilter);

		// select new set
		fFilterViewer.setSelection(new StructuredSelection(tourTypeFilter), true);

		fTourTypeViewer.getTable().setFocus();

		fIsModified = true;
	}

	private void onRenameFilterSet() {

		final TourTypeFilter filter = (TourTypeFilter) ((StructuredSelection) fFilterViewer.getSelection()).getFirstElement();

		final InputDialog inputDialog = new InputDialog(getShell(),
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
		fFilterViewer.update(filter, null);

		fIsModified = true;
	}

	private void onSelectFilter() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((StructuredSelection) fFilterViewer.getSelection()).getFirstElement();

		if (filterItem == null) {
			return;
		}

		fActiveFilter = filterItem;

		final int filterType = filterItem.getFilterType();

		Object[] tourTypes;
		switch (filterType) {
		case TourTypeFilter.FILTER_TYPE_SYSTEM:
			final int systemFilter = filterItem.getSystemFilterId();
			fTourTypeViewer.setAllChecked(systemFilter == TourTypeFilter.SYSTEM_FILTER_ID_ALL);
			fTourTypeViewer.getTable().setEnabled(false);

			break;

		case TourTypeFilter.FILTER_TYPE_DB:
			final TourType tourType = filterItem.getTourType();
			fTourTypeViewer.setCheckedElements(new Object[] { tourType });
			fTourTypeViewer.getTable().setEnabled(false);
			break;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
			fTourTypeViewer.getTable().setEnabled(true);
			tourTypes = filterItem.getTourTypeSet().getTourTypes();
			if (tourTypes == null) {
				fTourTypeViewer.setAllChecked(false);
			} else {
				fTourTypeViewer.setCheckedElements(tourTypes);
			}
			break;

		default:
			break;
		}

		enableButtons();
	}

	private void onSelectTourType() {

		if (fActiveFilter == null) {
			return;
		}

		// set tour types for current filter set
		if (fActiveFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {
			fActiveFilter.getTourTypeSet().setTourTypes(fTourTypeViewer.getCheckedElements());
		}
	}

	@Override
	public boolean performOk() {

		saveFilterList();

		return true;
	}

	private void saveFilterList() {

		if (fIsModified) {

			fIsModified = false;

			TourTypeContributionItem.writeXMLFilterFile(fFilterViewer);

			// fire modify event
			getPreferenceStore().setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
		}
	}

	private void updateViewers() {

		fFilterList = TourTypeContributionItem.getTourTypeFilters();
		fTourTypes = TourDatabase.getAllTourTypes();

		// show contents in the viewers
		fFilterViewer.setInput(new Object());
		fTourTypeViewer.setInput(new Object());

		enableButtons();
	}

}
