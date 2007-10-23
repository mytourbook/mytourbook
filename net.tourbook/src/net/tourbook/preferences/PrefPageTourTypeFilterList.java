package net.tourbook.preferences;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.util.StringToArrayConverter;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
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
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTourTypeFilterList extends PreferencePage implements IWorkbenchPreferencePage {

	private TableViewer					fFilterViewerLeft;
	private TableViewer					fFilterViewerRight;

	private long						fDragStartViewerLeft;
	private long						fDragStartViewerRight;
	private long						fRemovedFilterTime	= -1;

	private Button						fBtnMoveRight;
	private Button						fBtnMoveLeft;
	private Button						fBtnUp;
	private Button						fBtnDown;
	private Button						fBtnMoveLeftAll;
	private Button						fBtnMoveRightAll;

	private ArrayList<TourTypeFilter>	fFilterLeft;
	private ArrayList<TourTypeFilter>	fFilterRight;

	private boolean						fIsModified;

	private class TourTypeLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int index) {
			return element == null ? "" : ((TourTypeFilter) element).getFilterName(); //$NON-NLS-1$
		}
	}

	public PrefPageTourTypeFilterList() {}

	public PrefPageTourTypeFilterList(String title) {
		super(title);
	}

	public PrefPageTourTypeFilterList(String title, ImageDescriptor image) {
		super(title, image);
	}

	private void createButtons(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);

		// button: <
		fBtnMoveLeft = new Button(container, SWT.NONE);
		fBtnMoveLeft.setText("<");
		setButtonLayoutData(fBtnMoveLeft);
		fBtnMoveLeft.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onMoveLeft();
				enableButtons();
				fBtnMoveLeft.setFocus();
			}
		});

		// button: >
		fBtnMoveRight = new Button(container, SWT.NONE);
		fBtnMoveRight.setText(">");
		setButtonLayoutData(fBtnMoveRight);
		fBtnMoveRight.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onMoveRight();
				enableButtons();
				fBtnMoveRight.setFocus();
			}
		});

		// button: <<
		fBtnMoveLeftAll = new Button(container, SWT.NONE);
		fBtnMoveLeftAll.setText("<<");
		setButtonLayoutData(fBtnMoveLeftAll);
		fBtnMoveLeftAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onMoveLeftAll();
				enableButtons();
				fBtnMoveRightAll.setFocus();
			}
		});

		// button: >>
		fBtnMoveRightAll = new Button(container, SWT.NONE);
		fBtnMoveRightAll.setText(">>");
		setButtonLayoutData(fBtnMoveRightAll);
		fBtnMoveRightAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onMoveRightAll();
				enableButtons();
				fBtnMoveLeftAll.setFocus();
			}
		});

		// spacer
		new Label(container, SWT.NONE);

		// button: up
		fBtnUp = new Button(container, SWT.NONE);
		fBtnUp.setText("&Up");
		setButtonLayoutData(fBtnUp);
		fBtnUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onMoveUp();
			}
		});

		// button: down
		fBtnDown = new Button(container, SWT.NONE);
		fBtnDown.setText("&Down");
		setButtonLayoutData(fBtnDown);
		fBtnDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onMoveDown();
			}
		});

	}

	@Override
	protected Control createContents(Composite parent) {

		Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_root_title);
		label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

		// container
		Composite viewerContainer = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		viewerContainer.setLayout(gl);
		viewerContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createFilterViewerLeft(viewerContainer);
		createButtons(viewerContainer);
		createFilterViewerRight(viewerContainer);

		// hint for drag & drop
		label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_TourTypes_dnd_hint);
		label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 3, 1));

		// spacer
		label = new Label(parent, SWT.WRAP);

		createFilterList();

		// show tour types
		fFilterViewerLeft.setInput(new Object());
		fFilterViewerRight.setInput(new Object());

		enableButtons();

		return viewerContainer;
	}

	private void createFilterList() {

		TourTypeFilter filterAllTours = new TourTypeFilter(TourTypeFilter.FILTER_TYPE_SYSTEM,
				TourTypeFilter.SYSTEM_FILTER_ID_ALL,
				Messages.App_Tour_type_item_all_types);

		TourTypeFilter filterNoTours = new TourTypeFilter(TourTypeFilter.FILTER_TYPE_SYSTEM,
				TourTypeFilter.SYSTEM_FILTER_ID_NOT_DEFINED,
				Messages.App_Tour_type_item_not_defined);

		ArrayList<TourTypeFilter> filterDbTourTypes = new ArrayList<TourTypeFilter>();
		ArrayList<TourTypeFilter> dbFilterLeft = new ArrayList<TourTypeFilter>();

		ArrayList<TourType> dbTourTypes = TourDatabase.getTourTypes();
		for (TourType tourType : dbTourTypes) {
			filterDbTourTypes.add(new TourTypeFilter(TourTypeFilter.FILTER_TYPE_DB, tourType));
		}

		fFilterLeft = new ArrayList<TourTypeFilter>();
		fFilterRight = new ArrayList<TourTypeFilter>();

		IPreferenceStore prefStore = getPreferenceStore();

		boolean useDefaultList = true;

		if (prefStore.contains(ITourbookPreferences.TOUR_TYPE_FILTER_LIST)) {

			// get filter list from pref store

			String filterListString = prefStore.getString(ITourbookPreferences.TOUR_TYPE_FILTER_LIST);
			String[] filterList = StringToArrayConverter.convertStringToArray(filterListString);

			try {

				for (int filterIndex = 0; filterIndex < filterList.length; filterIndex++) {

					final int filterParam1 = Integer.parseInt(filterList[filterIndex]);
					final long filterParam2 = Long.parseLong(filterList[++filterIndex]);

					switch (filterParam1) {
					case TourTypeFilter.FILTER_TYPE_SYSTEM:

						// add system filters

						switch ((int) filterParam2) {
						case TourTypeFilter.SYSTEM_FILTER_ID_ALL:
							fFilterLeft.add(filterAllTours);
							break;

						case TourTypeFilter.SYSTEM_FILTER_ID_NOT_DEFINED:
							fFilterLeft.add(filterNoTours);
							break;

						default:
							break;
						}

						break;

					case TourTypeFilter.FILTER_TYPE_DB:

						for (TourTypeFilter filterItem : filterDbTourTypes) {
							if (filterParam2 == filterItem.getTourType().getTypeId()) {
								fFilterLeft.add(filterItem);
								dbFilterLeft.add(filterItem);
								break;
							}
						}

						break;

					default:
						break;
					}
				}

				useDefaultList = false;

				/*
				 * when the system filters are not set, something is worng, create default list
				 */
				if (fFilterLeft.contains(filterAllTours) == false
						|| fFilterLeft.contains(filterNoTours) == false) {

					useDefaultList = true;

				} else {

					/*
					 * add all filters to the right site which are not set on the left site
					 */
					fFilterRight.addAll(filterDbTourTypes);
					fFilterRight.removeAll(dbFilterLeft);
				}

			} catch (NumberFormatException e) {}
		}

		if (useDefaultList) {

			// add all available filters

			fFilterLeft.clear();

			fFilterLeft.add(filterAllTours);
			fFilterLeft.add(filterNoTours);
			fFilterLeft.addAll(filterDbTourTypes);
		}
	}

	private void createFilterViewerLeft(Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 30;
		layouter.setLayoutData(gd);

		final Table table = new Table(layouter,
				(SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		new TableColumn(table, SWT.NONE);
		layouter.addColumnData(new ColumnWeightData(1));

		// Create list viewer	
		fFilterViewerLeft = new TableViewer(table);

		fFilterViewerLeft.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(Object inputElement) {
				return fFilterLeft.toArray();
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		});

		fFilterViewerLeft.setLabelProvider(new TourTypeLabelProvider());

		fFilterViewerLeft.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				enableButtons();
			}
		});

		fFilterViewerLeft.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				onMoveRight();
				enableButtons();
			}
		});

		// set drag adapter
		fFilterViewerLeft.addDragSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					public void dragFinished(DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						if (event.detail == DND.DROP_MOVE
								&& transfer.getSelectionSetTime() != fRemovedFilterTime) {

							// drag was removed from the left viewer
							removeFilterItem(fFilterViewerLeft, transfer);
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					public void dragSetData(DragSourceEvent event) {
					// data are set in LocalSelectionTransfer
					}

					public void dragStart(DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = fFilterViewerLeft.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(fDragStartViewerLeft = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		// set drop adapter
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(fFilterViewerLeft) {

			private Widget	fLeftTableItem;

			@Override
			public boolean performDrop(Object data) {

				if (data instanceof StructuredSelection) {
					StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof TourTypeFilter) {

						TourTypeFilter filterItem = (TourTypeFilter) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table filterTable = fFilterViewerLeft.getTable();

						/*
						 * check if drag was startet from this filter, remove the filter item before
						 * the new filter is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == fDragStartViewerLeft) {
							fFilterViewerLeft.remove(filterItem);
							fRemovedFilterTime = fDragStartViewerLeft;
						}

						int filterIndex;

						if (fLeftTableItem == null) {

							fFilterViewerLeft.add(filterItem);
							filterIndex = filterTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							filterIndex = filterTable.indexOf((TableItem) fLeftTableItem);
							if (filterIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								fFilterViewerLeft.insert(filterItem, filterIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								fFilterViewerLeft.insert(filterItem, ++filterIndex);
							}
						}

						// reselect filter item
						fFilterViewerLeft.setSelection(new StructuredSelection(filterItem));

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
			public boolean validateDrop(Object target, int operation, TransferData transferType) {

				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) == false) {
					return false;
				}

				return true;
			}

			@Override
			public void dragOver(DropTargetEvent event) {

				// keep table item
				fLeftTableItem = event.item;

				super.dragOver(event);
			}

		};

		fFilterViewerLeft.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createFilterViewerRight(Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 30;
		layouter.setLayoutData(gd);

		final Table table = new Table(layouter,
				(SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		new TableColumn(table, SWT.NONE);
		layouter.addColumnData(new ColumnWeightData(1));

		// Create list viewer	
		fFilterViewerRight = new TableViewer(table);

		fFilterViewerRight.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(Object inputElement) {
				return fFilterRight.toArray();
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		});

		fFilterViewerRight.setLabelProvider(new TourTypeLabelProvider());

		fFilterViewerRight.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				enableButtons();
			}
		});

		fFilterViewerRight.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				onMoveLeft();
				enableButtons();
			}
		});

		fFilterViewerRight.addDragSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					public void dragFinished(DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						if (event.detail == DND.DROP_MOVE) {
							removeFilterItem(fFilterViewerRight, transfer);
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					public void dragSetData(DragSourceEvent event) {
					// data are set in LocalSelectionTransfer
					}

					public void dragStart(DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = fFilterViewerRight.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(fDragStartViewerRight = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		// set drop adapter
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(fFilterViewerRight) {

			@Override
			public boolean performDrop(Object data) {

				if (data instanceof StructuredSelection) {
					StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof TourTypeFilter) {

						TourTypeFilter filterItem = (TourTypeFilter) selection.getFirstElement();

						// add filter item
						fFilterViewerRight.add(filterItem);

						// set focus to new filter item
						fFilterViewerRight.setSelection(new StructuredSelection(filterItem));
						final Table filterTable = fFilterViewerRight.getTable();
						filterTable.setSelection(filterTable.getItemCount() - 1);
						filterTable.setFocus();

						fIsModified = true;

						return true;
					}
				}

				return false;
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {

				final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

				/*
				 * system filters cannot be droped
				 */
				ISelection selection = transfer.getSelection();
				if (selection instanceof StructuredSelection) {
					Object item = ((StructuredSelection) selection).getFirstElement();
					if (item instanceof TourTypeFilter) {
						TourTypeFilter filterItem = (TourTypeFilter) item;
						if (filterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM) {
							return false;
						}
					}

				}

				if (transfer.isSupportedType(transferType) == false) {
					return false;
				}

				/*
				 * prevent dnd within the right viewer
				 */
				if (transfer.getSelectionSetTime() == fDragStartViewerRight) {
					return false;
				}

				return true;
			}
		};

		viewerDropAdapter.setSelectionFeedbackEnabled(false);
		viewerDropAdapter.setFeedbackEnabled(false);

		fFilterViewerRight.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void enableButtons() {

		final IStructuredSelection selectionLeftViewer = (IStructuredSelection) fFilterViewerLeft.getSelection();
		final IStructuredSelection selectionRightViewer = (IStructuredSelection) fFilterViewerRight.getSelection();

		final TourTypeFilter leftFilterItem = (TourTypeFilter) selectionLeftViewer.getFirstElement();
		final Table leftFilterTable = fFilterViewerLeft.getTable();

		fBtnMoveLeft.setEnabled(selectionRightViewer.getFirstElement() != null);

		if (leftFilterItem == null) {
			fBtnMoveRight.setEnabled(false);
		} else {
			if (leftFilterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM) {
				fBtnMoveRight.setEnabled(false);
			} else {
				fBtnMoveRight.setEnabled(true);
			}
		}

		/*
		 * check if in the left filter list is a non system filter
		 */
		boolean isMoveRightAll = false;
		for (int indexList = 0; indexList < leftFilterTable.getItemCount(); indexList++) {
			TourTypeFilter filterItem = (TourTypeFilter) fFilterViewerLeft.getElementAt(indexList);
			if (filterItem.getFilterType() != TourTypeFilter.FILTER_TYPE_SYSTEM) {
				isMoveRightAll = true;
				break;
			}
		}
		fBtnMoveRightAll.setEnabled(isMoveRightAll);
		fBtnMoveLeftAll.setEnabled(fFilterViewerRight.getTable().getItemCount() > 0);

		fBtnUp.setEnabled(leftFilterItem != null && leftFilterTable.getSelectionIndex() > 0);

		fBtnDown.setEnabled(leftFilterItem != null
				&& leftFilterTable.getSelectionIndex() < leftFilterTable.getItemCount() - 1);
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean isValid() {

		savePreferences();

		return super.isValid();
	}

	private void onMoveDown() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) fFilterViewerLeft.getSelection()).getFirstElement();

		if (filterItem == null) {
			return;
		}

		final Table filterTable = fFilterViewerLeft.getTable();
		final int selectionIndex = filterTable.getSelectionIndex();

		if (selectionIndex < filterTable.getItemCount() - 1) {

			fFilterViewerLeft.remove(filterItem);
			fFilterViewerLeft.insert(filterItem, selectionIndex + 1);

			// reselect moved item
			fFilterViewerLeft.setSelection(new StructuredSelection(filterItem));

			if (filterTable.getSelectionIndex() == filterTable.getItemCount() - 1) {
				fBtnUp.setFocus();
			} else {
				fBtnDown.setFocus();
			}

			fIsModified = true;
		}
	}

	private void onMoveLeft() {

		IStructuredSelection selectionInViewer = (IStructuredSelection) fFilterViewerRight.getSelection();
		TourTypeFilter filterItem = (TourTypeFilter) selectionInViewer.getFirstElement();

		if (filterItem == null) {
			return;
		}

		final Table tableList = fFilterViewerRight.getTable();
		int lastSelectedIndex = tableList.getSelectionIndex();

		fFilterViewerRight.remove(filterItem);
		fFilterViewerLeft.add(filterItem);

		// select filter item at the same position
		tableList.select(Math.min(lastSelectedIndex, tableList.getItemCount() - 1));

		fIsModified = true;
	}

	private void onMoveLeftAll() {

		Table tableRight = fFilterViewerRight.getTable();
		ArrayList<TourTypeFilter> removedFilters = new ArrayList<TourTypeFilter>();

		for (int listIndex = 0; listIndex < tableRight.getItemCount(); listIndex++) {
			final TourTypeFilter filterItem = (TourTypeFilter) fFilterViewerRight.getElementAt(listIndex);
			fFilterViewerLeft.add(filterItem);
			removedFilters.add(filterItem);
		}

		fFilterViewerRight.remove(removedFilters.toArray());

		fIsModified = true;
	}

	private void onMoveRight() {

		IStructuredSelection selectionOutViewer = (IStructuredSelection) fFilterViewerLeft.getSelection();

		TourTypeFilter filterItem = (TourTypeFilter) selectionOutViewer.getFirstElement();

		if (filterItem == null || filterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM) {
			return;
		}

		final Table leftTable = fFilterViewerLeft.getTable();
		final int lastSelectedIndex = leftTable.getSelectionIndex();

		fFilterViewerLeft.remove(filterItem);
		fFilterViewerRight.add(filterItem);

		// select filter item at the same position
		leftTable.select(Math.min(lastSelectedIndex, leftTable.getItemCount() - 1));

		fIsModified = true;
	}

	private void onMoveRightAll() {

		Table listTable = fFilterViewerLeft.getTable();
		ArrayList<TourTypeFilter> removedFilters = new ArrayList<TourTypeFilter>();

		for (int listIndex = 0; listIndex < listTable.getItemCount(); listIndex++) {
			final TourTypeFilter filterItem = (TourTypeFilter) fFilterViewerLeft.getElementAt(listIndex);

			// check for system filters, they cannot be removed
			if (filterItem.getFilterType() != TourTypeFilter.FILTER_TYPE_SYSTEM) {

				fFilterViewerRight.add(filterItem);
				removedFilters.add(filterItem);
			}
		}

		fFilterViewerLeft.remove(removedFilters.toArray());

		fIsModified = true;
	}

	private void onMoveUp() {

		final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) fFilterViewerLeft.getSelection()).getFirstElement();

		if (filterItem == null) {
			return;
		}

		Table filterTable = fFilterViewerLeft.getTable();

		final int selectionIndex = filterTable.getSelectionIndex();
		if (selectionIndex > 0) {
			fFilterViewerLeft.remove(filterItem);
			fFilterViewerLeft.insert(filterItem, selectionIndex - 1);

			// reselect moved item
			fFilterViewerLeft.setSelection(new StructuredSelection(filterItem));

			if (filterTable.getSelectionIndex() == 0) {
				fBtnDown.setFocus();
			} else {
				fBtnUp.setFocus();
			}

			fIsModified = true;
		}
	}

	@Override
	public boolean performOk() {

		savePreferences();

		return super.performOk();
	}

	private void savePreferences() {

		if (fIsModified) {

			fIsModified = false;

			Table listTable = fFilterViewerLeft.getTable();
			ArrayList<String> filterList = new ArrayList<String>();

			for (int listIndex = 0; listIndex < listTable.getItemCount(); listIndex++) {

				final TourTypeFilter filterItem = (TourTypeFilter) fFilterViewerLeft.getElementAt(listIndex);
				final int filterType = filterItem.getFilterType();

				filterList.add(Integer.toString(filterType)); // 1. parameter

				switch (filterType) {
				case TourTypeFilter.FILTER_TYPE_SYSTEM:
					filterList.add(Integer.toString(filterItem.getSystemFilterId())); // 2. parameter
					break;

				case TourTypeFilter.FILTER_TYPE_DB:
					filterList.add(Long.toString(filterItem.getTourType().getTypeId())); // 2. parameter
					break;

				default:
					break;
				}
			}

			IPreferenceStore prefStore = getPreferenceStore();
			String filterListString = StringToArrayConverter.convertArrayToString(filterList.toArray(new String[filterList.size()]));

			prefStore.setValue(ITourbookPreferences.TOUR_TYPE_FILTER_LIST, filterListString);
		}
	}

	private void removeFilterItem(AbstractTableViewer viewer, final LocalSelectionTransfer transfer) {
		ISelection selection = transfer.getSelection();
		if (selection instanceof StructuredSelection) {

			Object item = ((StructuredSelection) selection).getFirstElement();
			if (item instanceof TourTypeFilter) {

				TourTypeFilter filterItem = (TourTypeFilter) item;
				viewer.remove(filterItem);
			}
		}
	}
}
