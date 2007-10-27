package net.tourbook.preferences;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.Messages;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.TourTypeFilterSet;
import net.tourbook.ui.UI;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PrefPageTourTypeFilterSet extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String				TAG_TOUR_TYPE_ID		= "id";			//$NON-NLS-1$
	private static final String				TAG_FILTER_SET_NAME		= "name";			//$NON-NLS-1$

	private static final String				MEMENTO_ROOT_FILTERSETS	= "filtersets";	//$NON-NLS-1$
	private static final String				MEMENTO_CHILD_FILTERSET	= "filterset";		//$NON-NLS-1$
	private static final String				MEMENTO_CHILD_TOURTYPE	= "tourtype";		//$NON-NLS-1$

	private static final String				MEMENTO_FILTERSET_FILE	= "filtersets.xml"; //$NON-NLS-1$

	private TableViewer						fFilterSetViewer;
	private CheckboxTableViewer				fTourTypeViewer;

	private Button							fBtnNew;
	private Button							fBtnRemove;
	private Button							fBtnRename;

	private ArrayList<TourType>				fTourTypes;
	private ArrayList<TourTypeFilterSet>	fTourTypeFilterSets;

	private boolean							fIsModified				= false;

	private TourTypeFilterSet				fActiveFilterSet;

	private IPropertyChangeListener			fPrefChangeListener;

	public PrefPageTourTypeFilterSet() {}

	public PrefPageTourTypeFilterSet(String title) {
		super(title);
	}

	public PrefPageTourTypeFilterSet(String title, ImageDescriptor image) {
		super(title, image);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();
				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
					updateViewers();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);
	}

	private void createButtons(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
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
			public void widgetSelected(SelectionEvent e) {
				onNewFilterSet();
			}
		});

		// button: rename
		fBtnRename = new Button(container, SWT.NONE);
		fBtnRename.setText(Messages.Pref_TourTypeFilter_button_rename);
		setButtonLayoutData(fBtnRename);
		fBtnRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onRenameFilterSet();
			}
		});

		// button: delete
		fBtnRemove = new Button(container, SWT.NONE);
		fBtnRemove.setText(Messages.Pref_TourTypeFilter_button_remove);
		GridData gd = setButtonLayoutData(fBtnRemove);
		gd.verticalIndent = 10;
		fBtnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onDeleteFilterSet();
			}
		});

	}

	@Override
	protected Control createContents(Composite parent) {

		Composite container = createUI(parent);

		updateViewers();

		addPrefListener();

		return container;
	}

	private void createFilterSetViewer(Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 20;
		layouter.setLayoutData(gd);

		final Table table = new Table(layouter,
				(SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		fFilterSetViewer = new TableViewer(table);

		TableViewerColumn tvc;

//		// column: tour type
//		tvc = new TableViewerColumn(fFilterSetViewer, SWT.NONE);
//		tvc.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(ViewerCell cell) {
//				cell.setImage(UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER));
//			}
//		});
//		layouter.addColumnData(new ColumnPixelData(26));

		// column: name
		tvc = new TableViewerColumn(fFilterSetViewer, SWT.NONE);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setImage(UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER));
				cell.setText(((TourTypeFilterSet) cell.getElement()).getName());
			}
		});
		layouter.addColumnData(new ColumnWeightData(100));

		fFilterSetViewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {}

			public Object[] getElements(Object inputElement) {
				return fTourTypeFilterSets.toArray();
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		});

		/*
		 * sort filter set by name
		 */
		fFilterSetViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object o1, Object o2) {
				final String filter1 = ((TourTypeFilterSet) o1).getName();
				final String filter2 = ((TourTypeFilterSet) o2).getName();
				return filter1.compareTo(filter2);
			}
		});

		fFilterSetViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				onSelectFilterSet();
				enableControls();
			}
		});
	}

	private void createTourTypeViewer(Composite parent) {

		final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 20;
		layouter.setLayoutData(gd);

		final Table table = new Table(layouter, (SWT.CHECK
				| SWT.SINGLE
				| SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER | SWT.FULL_SELECTION));

		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		fTourTypeViewer = new CheckboxTableViewer(table);

		TableViewerColumn tvc;

//		// column: tour type
//		tvc = new TableViewerColumn(fTourTypeViewer, SWT.NONE);
//		tvc.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(ViewerCell cell) {
//				final TourType tourType = (TourType) cell.getElement();
//				if (tourType != null) {
//					cell.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
//				}
//			}
//		});
//		layouter.addColumnData(new ColumnPixelData(40));

		// column: name
		tvc = new TableViewerColumn(fTourTypeViewer, SWT.NONE);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				final TourType tourType = ((TourType) cell.getElement());
				cell.setText(tourType.getName());
				cell.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
			}
		});
		layouter.addColumnData(new ColumnWeightData(1));

		fTourTypeViewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {}

			public Object[] getElements(Object inputElement) {
				return fTourTypes.toArray();
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		});

		fTourTypeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				fIsModified = true;
			}
		});

		fTourTypeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				getSelectedTourTypes();
			}
		});

		fTourTypeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {

				/*
				 * invert check state
				 */
				TourType tourType = (TourType) ((StructuredSelection) fTourTypeViewer.getSelection()).getFirstElement();

				boolean isChecked = fTourTypeViewer.getChecked(tourType);

				fTourTypeViewer.setChecked(tourType, !isChecked);

				getSelectedTourTypes();
			}
		});
	}

	private Composite createUI(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		{
			GridLayout gl = new GridLayout(3, false);
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			container.setLayout(gl);
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}

		Label label = new Label(container, SWT.WRAP);
		{
			label.setText(Messages.Pref_TourTypeFilter_title);
			GridData gd = new GridData();
			gd.horizontalSpan = 3;
			label.setLayoutData(gd);
		}

		label = new Label(container, SWT.WRAP);
		label.setText(Messages.Pref_TourTypeFilter_filter_sets);

		label = new Label(container, SWT.WRAP);
		label.setText(Messages.Pref_TourTypeFilter_tour_types);

		new Label(container, SWT.WRAP);

		createFilterSetViewer(container);
		createTourTypeViewer(container);
		createButtons(container);

		return container;
	}

	private void createViewerContent() {

		fTourTypes = TourDatabase.getTourTypes();

		fTourTypeFilterSets = readFilterSets();
	}

	@Override
	public void dispose() {
		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.removePropertyChangeListener(fPrefChangeListener);
		super.dispose();
	}

	private void enableControls() {

		final boolean isFilterSelected = ((StructuredSelection) fFilterSetViewer.getSelection()).getFirstElement() != null;

		fBtnRemove.setEnabled(isFilterSelected);
		fBtnRename.setEnabled(isFilterSelected);
		fTourTypeViewer.getTable().setEnabled(isFilterSelected);
	}

	private void getSelectedTourTypes() {
		fActiveFilterSet.setTourTypes(fTourTypeViewer.getCheckedElements());
	}

	private XMLMemento getXMLMementoRoot() {
		Document document;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element element = document.createElement(MEMENTO_ROOT_FILTERSETS);
			element.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			document.appendChild(element);

			return new XMLMemento(document, element);

		} catch (ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean okToLeave() {

		saveFilterSet();
		return super.okToLeave();
	}

	private void onDeleteFilterSet() {

		TourTypeFilterSet filterSet = (TourTypeFilterSet) ((StructuredSelection) fFilterSetViewer.getSelection()).getFirstElement();

		final Table filterSetTable = fFilterSetViewer.getTable();
		int selectedIndex = filterSetTable.getSelectionIndex();

		fTourTypeFilterSets.remove(filterSet);
		fFilterSetViewer.remove(filterSet);

		// select next filter set
		final int nextIndex = Math.min(selectedIndex, filterSetTable.getItemCount() - 1);
		filterSet = (TourTypeFilterSet) fFilterSetViewer.getElementAt(nextIndex);
		if (filterSet == null) {
			// all filters are removed, remove check state from last filter set
			fTourTypeViewer.setAllChecked(false);
		} else {
			fFilterSetViewer.setSelection(new StructuredSelection(filterSet));
		}

		enableControls();
	}

	private void onNewFilterSet() {

		InputDialog inputDialog = new InputDialog(getShell(),
				Messages.Pref_TourTypeFilter_dlg_new_title,
				Messages.Pref_TourTypeFilter_dlg_new_message,
				"", //$NON-NLS-1$
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// create new filterset
		TourTypeFilterSet filterSet = new TourTypeFilterSet();
		filterSet.setName(inputDialog.getValue().trim());

		// update model and viewer
		fTourTypeFilterSets.add(filterSet);
		fFilterSetViewer.add(filterSet);

		// select new set
		fFilterSetViewer.setSelection(new StructuredSelection(filterSet), true);

		fTourTypeViewer.getTable().setFocus();

		fIsModified = true;
	}

	private void onRenameFilterSet() {

		TourTypeFilterSet filterSet = (TourTypeFilterSet) ((StructuredSelection) fFilterSetViewer.getSelection()).getFirstElement();

		InputDialog inputDialog = new InputDialog(getShell(),
				Messages.Pref_TourTypeFilter_dlg_rename_title,
				Messages.Pref_TourTypeFilter_dlg_rename_message,
				filterSet.getName(),
				null);

		inputDialog.open();

		if (inputDialog.getReturnCode() != Window.OK) {
			return;
		}

		// update model
		filterSet.setName(inputDialog.getValue().trim());

		// update viewer
		fFilterSetViewer.update(filterSet, null);

		fIsModified = true;
	}

	private void onSelectFilterSet() {

		TourTypeFilterSet filterSetItem = (TourTypeFilterSet) ((StructuredSelection) fFilterSetViewer.getSelection()).getFirstElement();

		if (filterSetItem == null) {
			return;
		}

		fActiveFilterSet = filterSetItem;

		// enable all tour types for the filter set
		final Object[] tourTypes = filterSetItem.getTourTypes();
		if (tourTypes == null || tourTypes.length == 0) {
			fTourTypeViewer.setAllChecked(false);
		} else {
			fTourTypeViewer.setCheckedElements(tourTypes);
		}
	}

	@Override
	public boolean performOk() {

		saveFilterSet();

		return super.performOk();
	}

	private ArrayList<TourTypeFilterSet> readFilterSets() {

		ArrayList<TourTypeFilterSet> filterSets = new ArrayList<TourTypeFilterSet>();

		IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		String filename = stateLocation.append(MEMENTO_FILTERSET_FILE).toFile().getAbsolutePath();

		InputStreamReader reader = null;
		TourType[] tourTypes = fTourTypes.toArray(new TourType[fTourTypes.size()]);

		try {
			reader = new InputStreamReader(new FileInputStream(filename), "UTF-8"); //$NON-NLS-1$
			XMLMemento filterSetsMemento = XMLMemento.createReadRoot(reader);

			// read all filter sets
			IMemento[] mementoFilterSets = filterSetsMemento.getChildren(MEMENTO_CHILD_FILTERSET);
			for (IMemento mementoFilterSet : mementoFilterSets) {

				TourTypeFilterSet filterSet = new TourTypeFilterSet();
				filterSets.add(filterSet);

				filterSet.setName(mementoFilterSet.getString(TAG_FILTER_SET_NAME));

				IMemento[] mementoTourTypes = mementoFilterSet.getChildren(MEMENTO_CHILD_TOURTYPE);

				// get all tour types for the filter set
				ArrayList<TourType> filterSetTourTypes = new ArrayList<TourType>();
				for (IMemento mementoTourType : mementoTourTypes) {

					// get tour type from available tour types
					try {
						long tourTypeId = Long.parseLong(mementoTourType.getString(TAG_TOUR_TYPE_ID));

						for (TourType tourType : tourTypes) {
							if (tourType.getTypeId() == tourTypeId) {
								// tour type was found
								filterSetTourTypes.add(tourType);
								break;
							}
						}
					} catch (NumberFormatException e) {
						// ignore
					}
				}

				filterSet.setTourTypes(filterSetTourTypes.toArray());
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}

		return filterSets;
	}

	private void saveFilterSet() {

		if (fIsModified) {

			fIsModified = false;

			writeFilterSets();

			// fire modify event
			getPreferenceStore().setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED,
					Math.random());
		}
	}

	private void updateViewers() {

		createViewerContent();

		// show tour types
		fFilterSetViewer.setInput(new Object());
		fTourTypeViewer.setInput(new Object());

		enableControls();

		// select first filter set
		if (fTourTypeFilterSets.size() > 0) {
			fFilterSetViewer.setSelection(new StructuredSelection(fFilterSetViewer.getElementAt(0)));
		}
	}

	/**
	 * write the filter sets into an xml file
	 */
	private void writeFilterSets() {

		BufferedWriter writer = null;

		try {

			IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
			File file = stateLocation.append(MEMENTO_FILTERSET_FILE).toFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			XMLMemento xmlMemento = getXMLMementoRoot();

			for (TourTypeFilterSet filterSet : fTourTypeFilterSets) {

				IMemento filterSetMemento = xmlMemento.createChild(MEMENTO_CHILD_FILTERSET);

				filterSetMemento.putString(TAG_FILTER_SET_NAME, filterSet.getName());

				final Object[] tourTypes = filterSet.getTourTypes();
				if (tourTypes != null) {
					for (Object item : tourTypes) {

						TourType tourType = (TourType) item;
						IMemento tourTypeMemento = filterSetMemento.createChild(MEMENTO_CHILD_TOURTYPE);
						{
							tourTypeMemento.putString(TAG_TOUR_TYPE_ID,
									Long.toString(tourType.getTypeId()));
						}
					}
				}
			}

			xmlMemento.save(writer);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
