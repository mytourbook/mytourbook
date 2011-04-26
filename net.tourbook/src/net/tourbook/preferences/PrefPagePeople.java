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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PrefPagePeople extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID						= "net.tourbook.preferences.PrefPagePeopleId";	//$NON-NLS-1$

	private static final String			STATE_SELECTED_PERSON	= "selectedPersonId";							//$NON-NLS-1$

//	private final IPreferenceStore		_prefStore				= TourbookPlugin.getDefault()//
//																		.getPreferenceStore();
	private final IDialogSettings		_state					= TourbookPlugin.getDefault()//
																		.getDialogSettingsSection(ID);

//	private IPropertyChangeListener		_prefChangeListener;
//	private TourBike[]					_bikes;

	private ArrayList<TourPerson>		_people;

	/**
	 * this device list has all the devices which are visible in the device combobox
	 */
	private ArrayList<ExternalDevice>	_deviceList;

	private final DateTimeFormatter		_dtFormatter			= DateTimeFormat.shortDate();

	private final NumberFormat			_nf1					= NumberFormat.getNumberInstance();

	private final NumberFormat			_nf2					= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf2.setMinimumFractionDigits(2);
		_nf2.setMaximumFractionDigits(2);
	}
	private final boolean				_isOSX					= net.tourbook.util.UI.IS_OSX;

	private int							_spinnerWidth;

	private SelectionListener			_defaultSelectionListener;
	private ModifyListener				_defaultModifyListener;

	private boolean						_isPersonListModified	= false;
	private boolean						_isPersonModified		= false;
	private boolean						_isUpdateUI				= false;

	private TourPerson					_selectedPerson;
	private TourPerson					_newPerson;

	/*
	 * UI controls
	 */
	private PixelConverter				_pc;
	private TableViewer					_peopleViewer;

	private Button						_btnAdd;

	private Text						_txtFirstName;
	private Text						_txtLastName;
	private Combo						_cboDevice;
	private Spinner						_spinnerWeight;
	private Spinner						_spinnerHeight;
	private Composite					_containerRawPath;
	private DirectoryFieldEditor		_rawDataPathEditor;

	private Button						_btnUpdate;
	private Button						_btnCancel;

	private DateTime					_dtBirthday;

	private Text						_txtRawDataPath;

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return _people.toArray(new TourPerson[_people.size()]);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

//	private void addPrefListener() {
//
//		_prefChangeListener = new IPropertyChangeListener() {
//			@Override
//			public void propertyChange(final PropertyChangeEvent event) {
//
//				final String property = event.getProperty();
//
//				if (property.equals(ITourbookPreferences.TOUR_BIKE_LIST_IS_MODIFIED)) {
//
//					// create new bike list
////					_cboBike.removeAll();
////					updateUIBikeList();
//
//					// update person details
////					updateUIPersonDetails();
//				}
//			}
//
//		};
//		// register the listener
//		_prefStore.addPropertyChangeListener(_prefChangeListener);
//	}

	@Override
	public void applyData(final Object data) {

		// this is called after the UI is created

		if (data instanceof Boolean) {
			final Boolean isCreatePerson = (Boolean) data;
			if (isCreatePerson && _people.size() == 0) {

				// it's requested to create a new person

				final TourPerson newPerson = createDefaultPerson();

				newPerson.persist();

				// update model
				_people.add(newPerson);

				// update state
				_isPersonListModified = true;
				_isPersonModified = false;

				// update ui viewer and person ui
				_peopleViewer.add(newPerson);
				_peopleViewer.setSelection(new StructuredSelection(newPerson));

				enableActions();

				// for the first person, disable Add.. button and people list that the user is not confused
				_btnAdd.setEnabled(false);
				_peopleViewer.getTable().setEnabled(false);

				// select first name
				_txtFirstName.selectAll();
				_txtFirstName.setFocus();
			}
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		initUI(parent);

		final Composite container = createUI(parent);

		updateUIDeviceList();

//		updateUIBikeList();
//		addPrefListener();

		// update people viewer
		_people = PersonManager.getTourPeople();
		_peopleViewer.setInput(new Object());

		// reselect previous person
		restoreState();

		enableActions();

		return container;
	}

	private TourPerson createDefaultPerson() {

		final TourPerson newPerson = new TourPerson(Messages.App_Default_PersonFirstName, UI.EMPTY_STRING);

		newPerson.setHeight(1.77f);
		newPerson.setWeight(77.7f);
		newPerson.setBirthDay(new org.joda.time.DateTime(1977, 7, 7, 0, 0, 0, 0).getMillis());

		return newPerson;
	}

	private void createDeviceList() {

		// create device list
		_deviceList = new ArrayList<ExternalDevice>();

		// add special device
		_deviceList.add(null);

		// add all devices which can read from a device
		final List<ExternalDevice> deviceList = DeviceManager.getExternalDeviceList();
		for (final ExternalDevice device : deviceList) {
			_deviceList.add(device);
		}
	}

	private Composite createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_People_Title);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI10PeopleViewer(container);
			createUI20PeopleViewerButtons(container);

			createUI30PersonDetails(container);
		}

		// placeholder
//		new Label(parent, SWT.NONE);
		new Label(parent, SWT.NONE);

		return container;
	}

	private void createUI10PeopleViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory
				.fillDefaults()
				.grab(true, true)
				.hint(convertWidthInCharsToPixels(30), SWT.DEFAULT)
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(
				layoutContainer,
				(SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI));

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		_peopleViewer = new TableViewer(table);
		defineAllColumns(tableLayout);

		_peopleViewer.setUseHashlookup(true);
		_peopleViewer.setContentProvider(new ClientsContentProvider());

		_peopleViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {

				// compare by last + first name

				final TourPerson p1 = (TourPerson) e1;
				final TourPerson p2 = (TourPerson) e2;

				final int compareLastName = p1.getLastName().compareTo(p2.getLastName());

				if (compareLastName != 0) {
					return compareLastName;
				}

				return p1.getFirstName().compareTo(p2.getFirstName());
			}
		});

		_peopleViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectPerson();
			}
		});

		_peopleViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				_txtFirstName.setFocus();
				_txtFirstName.selectAll();
			}
		});

	}

	private void createUI20PeopleViewerButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * button: add
			 */
			_btnAdd = new Button(container, SWT.NONE);
			_btnAdd.setText(Messages.Pref_People_Action_add_person);
			setButtonLayoutData(_btnAdd);
			_btnAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAddPerson();
				}
			});
		}
	}

	private void createUI30PersonDetails(final Composite parent) {

		/*
		 * group: person
		 */
		final Group groupPerson = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(groupPerson);
		groupPerson.setText(Messages.Pref_People_Group_person);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).applyTo(groupPerson);
//		_groupPerson.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI50PersonFields(groupPerson);
			createUI70PersonDetailsAction(groupPerson);
		}

		/*
		 * group: device/data transfer
		 */
		final Group groupDevice = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(groupDevice);
		groupDevice.setText(Messages.Pref_People_Group_Device);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(groupDevice);
//		_groupDevice.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI82RawDataPath(groupDevice);
		}

	}

	private void createUI50PersonFields(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI52FieldFirstName(container);
			createUI54FieldLastName(container);
			createUI56FieldBirthday(container);
			createUI58FieldWeight(container);
			createUI60FieldHeight(container);

			/*
			 * bike is disabled because it is currently not used, 21.04.2011
			 */
//			createUI64FieldBike(container);
		}

		container.layout(true, true);
	}

	/**
	 * field: first name
	 */
	private void createUI52FieldFirstName(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_first_name);

		_txtFirstName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFirstName);
		_txtFirstName.addModifyListener(_defaultModifyListener);
	}

	/**
	 * field: last name
	 */
	private void createUI54FieldLastName(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_last_name);

		_txtLastName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtLastName);
		_txtLastName.addModifyListener(_defaultModifyListener);
	}

	private void createUI56FieldBirthday(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_Birthday);

		_dtBirthday = new DateTime(parent, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).span(2, 1).applyTo(_dtBirthday);
		_dtBirthday.addSelectionListener(_defaultSelectionListener);
	}

	/**
	 * field: weight
	 */
	private void createUI58FieldWeight(final Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_weight);

		final Composite containerWeight = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(containerWeight);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerWeight);
		{
			// spinner: weight
			_spinnerWeight = new Spinner(containerWeight, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.grab(false, false)
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_spinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerWeight);
			_spinnerWeight.setDigits(1);
			_spinnerWeight.setMinimum(0);
			_spinnerWeight.setMaximum(3000); // 300.0 kg
			_spinnerWeight.addSelectionListener(_defaultSelectionListener);
			_spinnerWeight.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onModifyPerson();
				}
			});

			// label: unit
			label = new Label(containerWeight, SWT.NONE);
			label.setText(UI.UNIT_WEIGHT_KG);
		}

		// 3rd column filler
		new Label(parent, SWT.NONE);
	}

	/**
	 * field: height
	 */
	private void createUI60FieldHeight(final Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_height);

		final Composite containerHeight = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(containerHeight);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerHeight);
		{
			// spinner: height
			_spinnerHeight = new Spinner(containerHeight, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(false, false)
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_spinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerHeight);
			_spinnerHeight.setDigits(2);
			_spinnerHeight.setMinimum(0);
			_spinnerHeight.setMaximum(300); // 3.00 m
			_spinnerHeight.addSelectionListener(_defaultSelectionListener);
			_spinnerHeight.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onModifyPerson();
				}
			});

			// label: unit
			label = new Label(containerHeight, SWT.NONE);
			label.setText(UI.UNIT_METER);
		}

		// filler
		new Label(parent, SWT.NONE);
	}

	/**
	 * Actions: Update / Cancel
	 */
	private void createUI70PersonDetailsAction(final Composite parent) {

		final Composite btnContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(0, 0).applyTo(btnContainer);
		GridLayoutFactory.swtDefaults().applyTo(btnContainer);
		{
			// button: update
			_btnUpdate = new Button(btnContainer, SWT.NONE);
			_btnUpdate.setText(Messages.App_Action_Update);
			_btnUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onUpdatePerson();
				}
			});
			setButtonLayoutData(_btnUpdate);

			// button: cancel
			_btnCancel = new Button(btnContainer, SWT.NONE);
			_btnCancel.setText(Messages.App_Action_Cancel);
			_btnCancel.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onCancelPerson();
				}
			});
			setButtonLayoutData(_btnCancel);
		}
	}

//	/**
//	 * field: bike
//	 */
//	private void createUI64FieldBike(final Composite parent) {
//
//		// label
//		final Label label = new Label(parent, SWT.NONE);
//		label.setText(Messages.Pref_People_Label_bike);
//
//		// combo
//		_cboBike = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
//		_cboBike.setVisibleItemCount(20);
//		_cboBike.addSelectionListener(_defaultSelectionListener);
//
//		// filler
//		new Label(parent, SWT.NONE);
//	}

	private void createUI82RawDataPath(final Composite parent) {

		_containerRawPath = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_containerRawPath);
//		_containerRawPath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * field: device
			 */

			// label
			final Label label = new Label(_containerRawPath, SWT.NONE);
			label.setText(Messages.Pref_People_Label_device);

			// combo
			_cboDevice = new Combo(_containerRawPath, SWT.READ_ONLY | SWT.DROP_DOWN);
			_cboDevice.setVisibleItemCount(20);
			_cboDevice.addSelectionListener(_defaultSelectionListener);

			// spacer
			new Label(_containerRawPath, SWT.NONE);

			/*
			 * field: path to save raw tour data
			 */
			_rawDataPathEditor = new DirectoryFieldEditor(
					ITourbookPreferences.DUMMY_FIELD,
					Messages.Pref_People_Label_DefaultDataTransferFilePath,
					_containerRawPath);
			_rawDataPathEditor.setEmptyStringAllowed(true);
			_rawDataPathEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

			final Label lblPath = _rawDataPathEditor.getLabelControl(_containerRawPath);
			lblPath.setToolTipText(Messages.Pref_People_Label_DefaultDataTransferFilePath_Tooltip);

			_txtRawDataPath = _rawDataPathEditor.getTextControl(_containerRawPath);
			_txtRawDataPath.addModifyListener(_defaultModifyListener);
		}

		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_containerRawPath);

		/*
		 * set width for the text control that the pref dialog is not as wide as the full path
		 */
		final Text rawPathControl = _rawDataPathEditor.getTextControl(_containerRawPath);
		final GridData gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = 200;
	}

	private void defineAllColumns(final TableColumnLayout tableLayout) {

		TableViewerColumn tvc;
		TableColumn tc;

		/*
		 * column: first name
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_first_name);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(((TourPerson) cell.getElement()).getFirstName());
			}
		});
		tableLayout.setColumnData(tc, new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

		/*
		 * column: last name
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_last_name);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(((TourPerson) cell.getElement()).getLastName());
			}
		});
		tableLayout.setColumnData(tc, new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

		/*
		 * column: birth day
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_Birthday);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final long birthDayValue = ((TourPerson) cell.getElement()).getBirthDay();

				if (birthDayValue == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_dtFormatter.print(birthDayValue));
				}
			}
		});
		tableLayout.setColumnData(tc, new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

		/*
		 * column: device
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_device);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourPerson tourPerson = (TourPerson) cell.getElement();
				final String deviceId = tourPerson.getDeviceReaderId();

				if (deviceId != null) {
					for (final ExternalDevice device : _deviceList) {
						if (device != null && deviceId.equals(device.deviceId)) {
							cell.setText(device.visibleName);
							return;
						}
					}
				}

				cell.setText(UI.EMPTY_STRING);
			}
		});
		tableLayout.setColumnData(tc, new ColumnWeightData(4, convertWidthInCharsToPixels(4)));

		/*
		 * column: height
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_height);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final float height = ((TourPerson) cell.getElement()).getHeight();
				cell.setText(_nf2.format(height));
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(6 * 4), true));

		/*
		 * column: weight
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_weight);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final float weight = ((TourPerson) cell.getElement()).getWeight();
				cell.setText(_nf1.format(weight));
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(7 * 4), true));
	}

	@Override
	public void dispose() {

//		if (_prefChangeListener != null) {
//			_prefStore.removePropertyChangeListener(_prefChangeListener);
//		}

		super.dispose();
	}

	private void enableActions() {

		final boolean isValid = isPersonValid();

		_btnAdd.setEnabled(!_isPersonModified && isValid);
		_peopleViewer.getTable().setEnabled(!_isPersonModified && isValid);

		_btnUpdate.setEnabled(_isPersonModified && isValid);
		_btnCancel.setEnabled(_isPersonModified);

	}

	private void firePersonListModifyEvent() {

		if (_isPersonListModified) {

			TourManager.getInstance().clearTourDataCache();

			// fire bike list modify event
			getPreferenceStore().setValue(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED, Math.random());

			_isPersonListModified = false;
		}
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
		noDefaultAndApplyButton();
	}

	private void initUI(final Composite parent) {

		initializeDialogUnits(parent);

		_pc = new PixelConverter(parent);
		_spinnerWidth = _pc.convertWidthInCharsToPixels(_isOSX ? 10 : 5);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyPerson();
			}
		};

		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				onModifyPerson();
			}
		};
		createDeviceList();
	}

	/**
	 * @return Returns <code>true</code> when person is valid, otherwise <code>false</code>.
	 */
	private boolean isPersonValid() {

		if (_txtFirstName.getText().trim().equals(UI.EMPTY_STRING)) {

			setErrorMessage(Messages.Pref_People_Error_first_name_is_required);

			_txtFirstName.setFocus();

			return false;

		} else {

			final String transferPath = _rawDataPathEditor.getStringValue().trim();

			if (!transferPath.equals(UI.EMPTY_STRING) && Util.isDirectory(transferPath) == false) {

				setErrorMessage(Messages.Pref_People_Error_path_is_invalid);

				_txtRawDataPath.selectAll();

				return false;
			}
		}

		setErrorMessage(null);

		return true;
	}

	@Override
	public boolean okToLeave() {

		if (isPersonValid() == false) {
			return false;
		}

		saveState();
		savePerson(true);

		// enable action because the user can go back to this pref page
		enableActions();

		return super.okToLeave();
	}

	private void onAddPerson() {

		_newPerson = createDefaultPerson();
		_isPersonModified = true;

		updateUIPerson(_newPerson);
		enableActions();

		// edit first name
		_txtFirstName.selectAll();
		_txtFirstName.setFocus();
	}

	private void onCancelPerson() {

		_newPerson = null;
		_isPersonModified = false;

		updateUIPerson(_selectedPerson);
		enableActions();

		_peopleViewer.getTable().setFocus();
	}

	/**
	 * set person modified and enable actions accordingly
	 */
	private void onModifyPerson() {

		if (_isUpdateUI) {
			return;
		}

		if (_isPersonModified == false) {
			_isPersonModified = true;
		}

		enableActions();
	}

	private void onSelectPerson() {

		final IStructuredSelection selection = (IStructuredSelection) _peopleViewer.getSelection();
		final TourPerson person = (TourPerson) selection.getFirstElement();

		if (person != null) {

			_selectedPerson = person;

			updateUIPerson(_selectedPerson);
		} else {
			// irgnore, this can happen when a refresh() of the table viewer is done
		}
	}

	private void onUpdatePerson() {

		if (isPersonValid() == false) {
			return;
		}

		savePerson(false);
		enableActions();
	}

	@Override
	public boolean performCancel() {

		saveState();
		firePersonListModifyEvent();

		return super.performCancel();
	}

	@Override
	public boolean performOk() {

		if (isPersonValid() == false) {
			return false;
		}

		savePerson(false);

		saveState();
		firePersonListModifyEvent();

		return super.performOk();
	}

	private void restoreState() {

		/*
		 * selected person
		 */
		final long personId = Util.getStateLong(_state, STATE_SELECTED_PERSON, -1);
		if (personId != -1) {

			for (final TourPerson person : _people) {
				if (person.getPersonId() == personId) {
					_peopleViewer.setSelection(new StructuredSelection(person));
					return;
				}
			}
		}

		// previous person could not be reselected, select first person
		if (_people.size() > 0) {

			final TableItem tblItem = _peopleViewer.getTable().getItem(0);

			_peopleViewer.setSelection(new StructuredSelection(tblItem.getData()));
		}
	}

	private void savePerson(final boolean isAskToSave) {

		final boolean isNewPerson = _newPerson != null;
		final TourPerson person = isNewPerson ? _newPerson : _selectedPerson;
		_newPerson = null;

		if (_isPersonModified) {

			if (isAskToSave) {

				if (MessageDialog.openQuestion(
						Display.getCurrent().getActiveShell(),
						Messages.Pref_People_Dialog_SaveModifiedPerson_Title,
						NLS.bind(Messages.Pref_People_Dialog_SaveModifiedPerson_Message,

						// use name from the ui because it could be modified
								_txtFirstName.getText())) == false) {

					// update state
					_isPersonModified = false;

					// update ui from the previous selected person
					updateUIPerson(_selectedPerson);

					return;
				}
			}

			updatePersonFromUI(person);
			person.persist();

			// .persist() updates the people list
			_people = PersonManager.getTourPeople();

			// update state
			_isPersonListModified = true;
			_isPersonModified = false;

			// update model/ui
			if (isNewPerson) {
				_people.add(person);
				_peopleViewer.add(person);
			} else {
				// !!! refreshing a person do not resort the table when sorting has changed !!!
				_peopleViewer.refresh();
			}

			// select updated/new person
			_peopleViewer.setSelection(new StructuredSelection(person), true);
		}
	}

	private void saveState() {

		// selected person
		final Object firstElement = ((IStructuredSelection) _peopleViewer.getSelection()).getFirstElement();
		if (firstElement instanceof TourPerson) {
			_state.put(STATE_SELECTED_PERSON, ((TourPerson) firstElement).getPersonId());
		}
	}

	/**
	 * select device in the combo box
	 */
	private void selectDevice(final TourPerson person) {

		final String deviceId = person.getDeviceReaderId();

		if (deviceId == null) {
			_cboDevice.select(0);
		} else {

			int deviceIndex = 0;

			for (final ExternalDevice device : _deviceList) {

				if (device != null) {
					if (deviceId.equals(device.deviceId)) {
						_cboDevice.select(deviceIndex);
						break;
					}
				}

				deviceIndex++;
			}

			// when the device id was not found, select "<no selection>" entry
			if (deviceIndex == 0) {
				_cboDevice.select(0);
			}
		}
	}

	//	/**
//	 * select bike in the combo box
//	 */
//	private void selectBike(final TourPerson person) {
//
//		// select default value
//		int bikeIndex = 0;
//		final TourBike personBike = person.getTourBike();
//
//		if (personBike == null || _bikes == null) {
//			_cboBike.select(0);
//		} else {
//			boolean isBikeFound = false;
//			for (final TourBike bike : _bikes) {
//				if (personBike.getBikeId() == bike.getBikeId()) {
//					_cboBike.select(bikeIndex + 1);
//					isBikeFound = true;
//					break;
//				}
//				bikeIndex++;
//			}
//
//			// when the bike id was not found, select "<no selection>" entry
//			if (!isBikeFound) {
//				_cboBike.select(0);
//			}
//		}
//	}
//
//	private void updateUIBikeList() {
//
//		// create bike list
//		_cboBike.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
//
//		final ArrayList<TourBike> bikes = TourDatabase.getTourBikes();
//
//		if (bikes == null) {
//			_bikes = new TourBike[0];
//		} else {
//			_bikes = bikes.toArray(new TourBike[bikes.size()]);
//			for (final TourBike bike : _bikes) {
//				_cboBike.add(bike.getName());
//			}
//		}
//	}

	private void updatePersonFromUI(final TourPerson person) {

		final long birthDay = new org.joda.time.DateTime(
				_dtBirthday.getYear(),
				_dtBirthday.getMonth() + 1,
				_dtBirthday.getDay(),
				0,
				0,
				0,
				0).getMillis();

		String deviceId = null;
		final int selectedIndex = _cboDevice.getSelectionIndex();
		if (selectedIndex > 0) {
			deviceId = _deviceList.get(selectedIndex).deviceId;
		}

		/*
		 * update person
		 */
		person.setFirstName(_txtFirstName.getText());
		person.setLastName(_txtLastName.getText());

		person.setBirthDay(birthDay);
		person.setWeight(_spinnerWeight.getSelection() / 10.0f);
		person.setHeight(_spinnerHeight.getSelection() / 100.0f);

		person.setRawDataPath(_rawDataPathEditor.getStringValue());
		person.setDeviceReaderId(deviceId);
	}

	private void updateUIDeviceList() {
		// add all devices to the combobox
		for (final ExternalDevice device : _deviceList) {
			if (device == null) {
				_cboDevice.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
			} else {
				_cboDevice.add(device.visibleName);
			}
		}
	}

	private void updateUIPerson(final TourPerson person) {

		_isUpdateUI = true;
		{
			final org.joda.time.DateTime dtBirthday = new org.joda.time.DateTime(person.getBirthDay());

			_txtFirstName.setText(person.getFirstName());
			_txtLastName.setText(person.getLastName());
			_dtBirthday.setDate(dtBirthday.getYear(), dtBirthday.getMonthOfYear() - 1, dtBirthday.getDayOfMonth());
			_spinnerWeight.setSelection((int) (person.getWeight() * 10));
			_spinnerHeight.setSelection((int) (person.getHeight() * 100));
			_rawDataPathEditor.setStringValue(person.getRawDataPath());

			selectDevice(person);
		}
		_isUpdateUI = false;
	}

}
