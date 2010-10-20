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
package net.tourbook.preferences;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
 
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.InputFieldFloat;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePeople extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String			MODIFIER_SYMBOL			= "*";

	public static final String			ID						= "net.tourbook.preferences.PrefPagePeopleId";		//$NON-NLS-1$

	private final IPreferenceStore		_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();

//	private ModifyListener				_textFirstNameModifyListener;
//	private ModifyListener				_textLastNameModifyListener;
//	private ModifyListener				_textHeightModifyListener;
//	private ModifyListener				_textWeightModifyListener;
//	private ModifyListener				_comboDeviceModifyListener;
	private IPropertyChangeListener		_prefChangeListener;

	private ArrayList<TourPerson>		_people;

	private TourPerson					_currentPerson;
	private boolean						_isPersonModified;

	/**
	 * this device list has all the devices which are visible in the device combobox
	 */
	private ArrayList<ExternalDevice>	_deviceList;

	private boolean						_isPersonListModified	= false;
	private boolean						_isNewPerson			= false;

	/*
	 * UI controls
	 */
	private TableViewer					_peopleViewer;
	private Button						_btnAdd;

	private Composite					_containerPersonDetails;
	private Text						_txtFirstName;
	private Text						_txtLastName;
	private Text						_txtHeight;
	private Text						_txtWeight;
	private DateTime					_dtBirthDay;
	private Combo						_cboDevice;
	private DirectoryFieldEditor		_rawDataPathEditor;

	private Button						_btnUpdate;
	private Button						_btnCancel;

	/*
	 * none UI fields
	 */

	private PixelConverter				_pc;

	private ModifyListener				_defaultModifyListener;
	private SelectionListener			_defaultSelectionListener;

	private boolean						_isDisableModifyListener;

	{
		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {

				if (_isDisableModifyListener) {
					return;
				}

				_isPersonModified = true;
				validatePerson();
			}
		};
		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_isDisableModifyListener) {
					return;
				}

				_isPersonModified = true;
				validatePerson();
			}
		};
	}

	private class PeopleContentProvider implements IStructuredContentProvider {

		public PeopleContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {

			if (_people == null) {
				_people = PersonManager.getTourPeople();
			}

			return _people.toArray(new TourPerson[_people.size()]);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

//	private void addModifyListener() {
//
//		_txtFirstName.addModifyListener(_textFirstNameModifyListener);
//		_txtLastName.addModifyListener(_textLastNameModifyListener);
//		_txtHeight.addModifyListener(_textHeightModifyListener);
//		_txtWeight.addModifyListener(_textWeightModifyListener);
//
//		_cboDevice.addModifyListener(_comboDeviceModifyListener);
//
//		_rawDataPathEditor.setPropertyChangeListener(new org.eclipse.jface.util.IPropertyChangeListener() {
//			public void propertyChange(final PropertyChangeEvent event) {
//				if (_currentPerson != null) {
//
//					_isPersonModified = true;
//					_peopleViewer.update(_currentPerson, null);
//
//					validatePerson();
//				}
//			}
//		});
//
//	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_BIKE_LIST_IS_MODIFIED)) {

					// update person details
					updateUIPersonDetails();
				}
			}

		};
		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	protected Control createContents(final Composite parent) {

		initializeDialogUnits(parent);

		final Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_People_Title);

		final Composite container = createUI(parent);

		addPrefListener();

		_peopleViewer.setInput(this);

		// select first person
		_peopleViewer.getTable().setSelection(0);
		updateUIPersonDetails();
		enableControls(true);

		return container;
	}

	private Composite createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			createUI100PeopleViewerContainer(container);
			createUI200PersonDetailsContainer(container);

			// spacer
			new Label(parent, SWT.NONE);
			new Label(parent, SWT.NONE);
		}

		return container;
	}

	private Composite createUI100PeopleViewerContainer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI110PeopleViewer(container);
			createUI120PeopleViewerButtons(container);
		}

		return container;
	}

	private void createUI110PeopleViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults()//
				.hint(400, _pc.convertHeightInCharsToPixels(10))
				.grab(true, true)
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.BORDER);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		/*
		 * create viewer
		 */
		_peopleViewer = new TableViewer(table);
		_peopleViewer.setUseHashlookup(true);
		_peopleViewer.setContentProvider(new PeopleContentProvider());

		_peopleViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {

				final TourPerson person1 = (TourPerson) e1;
				final TourPerson person2 = (TourPerson) e2;

				// sort by last name
				int compareResult = person1.getLastName().compareTo(person2.getLastName());

				// sort by first name
				if (compareResult == 0) {
					compareResult = person1.getFirstName().compareTo(person2.getFirstName());
				}

				return compareResult;
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

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		/*
		 * column: modifier symbol
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(_isPersonModified ? MODIFIER_SYMBOL : UI.EMPTY_STRING);
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(3 * 4), false));

		/*
		 * column: first name
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_first_name);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourPerson tourPerson = (TourPerson) cell.getElement();
				cell.setText(tourPerson.getFirstName());
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
				final TourPerson tourPerson = (TourPerson) cell.getElement();
				cell.setText(tourPerson.getLastName());
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
				final String personDeviceId = tourPerson.getDeviceReaderId();

				if (personDeviceId != null) {
					for (final ExternalDevice device : _deviceList) {
						if (device != null && personDeviceId.equals(device.deviceId)) {
							cell.setText(device.visibleName);
							return;
						}
					}
				}

				cell.setText(UI.EMPTY_STRING);
			}
		});
		tableLayout.setColumnData(tc, new ColumnWeightData(3, convertWidthInCharsToPixels(3)));

		/*
		 * column: height
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_height);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourPerson tourPerson = (TourPerson) cell.getElement();
				cell.setText(Float.toString(tourPerson.getHeight()));
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(8 * 4), true));

		/*
		 * column: weight
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Pref_People_Column_weight);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourPerson tourPerson = (TourPerson) cell.getElement();
				cell.setText(Float.toString(tourPerson.getWeight()));
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(8 * 4), true));
	}

	private void createUI120PeopleViewerButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			// button: add
			_btnAdd = new Button(container, SWT.NONE);
			_btnAdd.setText(Messages.Pref_People_Action_add_person);
			setButtonLayoutData(_btnAdd);
			_btnAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAddPerson();
					// enableButtons();
				}
			});
		}
	}

	private void createUI200PersonDetailsContainer(final Composite parent) {

		// person data group
		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.Pref_People_Group_person);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			createUI202PersonDetails(group);
			createUI290DetailsButtons(group);
		}
	}

	private void createUI202PersonDetails(final Composite parent) {

		final int floatInputWidth = convertHorizontalDLUsToPixels(40);

		// person data group
		_containerPersonDetails = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerPersonDetails);
		GridLayoutFactory.fillDefaults().applyTo(_containerPersonDetails);
		{
			createUI210FieldFirstName(_containerPersonDetails);
			createUI215FieldLastName(_containerPersonDetails);
			createUI217FieldBirthday(_containerPersonDetails);
			createUI220FieldWeight(_containerPersonDetails, floatInputWidth);
			createUI225FieldHeight(_containerPersonDetails, floatInputWidth);
			createUI235FieldDevice(_containerPersonDetails);
			createUI240FieldDataPath(_containerPersonDetails);
		}
	}

	/**
	 * field: first name
	 * 
	 * @param parent
	 */
	private void createUI210FieldFirstName(final Composite parent) {

		// label: first name
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_first_name);

		// text: first name
		_txtFirstName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFirstName);
//		_textFirstNameModifyListener = new ModifyListener() {
//			public void modifyText(final ModifyEvent e) {
//				if (_currentPerson != null) {
//					final String firstName = ((Text) (e.widget)).getText();
//					if (!firstName.equals(_currentPerson.getFirstName())) {
//
//						_isPersonModified = true;
//
//						_currentPerson.setFirstName(firstName);
//						_peopleViewer.update(_currentPerson, null);
//					}
//				}
//				validatePerson();
//			}
//		};
	}

	/**
	 * field: last name
	 * 
	 * @param parent
	 */
	private void createUI215FieldLastName(final Composite parent) {

		final Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_last_name);

		_txtLastName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtLastName);
		_txtLastName.addModifyListener(_defaultModifyListener);
//		_textLastNameModifyListener = new ModifyListener() {
//			public void modifyText(final ModifyEvent e) {
//				if (_currentPerson != null) {
//					final String lastName = ((Text) (e.widget)).getText();
//					if (!lastName.equals(_currentPerson.getLastName())) {
//
//						_isPersonModified = true;
//
//						_currentPerson.setLastName(lastName);
//						_peopleViewer.update(_currentPerson, null);
//					}
//				}
//			}
//		};
	}

	/**
	 * field: age
	 * 
	 * @param parent
	 */
	private void createUI217FieldBirthday(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_Birthday);

		_dtBirthDay = new DateTime(parent, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_dtBirthDay);
		_dtBirthDay.addSelectionListener(_defaultSelectionListener);

		// spacer
		new Label(parent, SWT.NONE);
	}

	/**
	 * field: weight
	 * 
	 * @param parent
	 */
	private void createUI220FieldWeight(final Composite parent, final int floatInputWidth) {

		final InputFieldFloat floatInput = new InputFieldFloat(
				parent,
				Messages.Pref_People_Label_weight,
				floatInputWidth);

		_txtWeight = floatInput.getTextField();

//		_textWeightModifyListener = new ModifyListener() {
//			public void modifyText(final ModifyEvent e) {
//				if (_currentPerson != null) {
//					final Text control = (Text) e.widget;
//					try {
//						final float value = Float.parseFloat(((Text) (e.widget)).getText());
//						if (value != _currentPerson.getWeight()) {
//							_currentPerson.setWeight(value);
//							_peopleViewer.update(_currentPerson, null);
//						}
//						UI.setDefaultColor(control);
//					} catch (final NumberFormatException e1) {
//						UI.setErrorColor(control);
//					}
//					_isPersonModified = true;
//					validatePerson();
//				}
//			}
//		};

		// filler
		new Label(parent, SWT.NONE);
	}

	/**
	 * field: height
	 * 
	 * @param parent
	 */
	private void createUI225FieldHeight(final Composite parent, final int floatInputWidth) {

		final InputFieldFloat floatInput = new InputFieldFloat(
				parent,
				Messages.Pref_People_Label_height,
				floatInputWidth);

		_txtHeight = floatInput.getTextField();

//		_textHeightModifyListener = new ModifyListener() {
//			public void modifyText(final ModifyEvent e) {
//				if (_currentPerson != null) {
//					final Text control = (Text) e.widget;
//					try {
//						final float value = Float.parseFloat(((Text) (e.widget)).getText());
//						if (value != _currentPerson.getHeight()) {
//							_currentPerson.setHeight(value);
//							_peopleViewer.update(_currentPerson, null);
//						}
//						UI.setDefaultColor(control);
//					} catch (final NumberFormatException e1) {
//						UI.setErrorColor(control);
//					}
//					_isPersonModified = true;
//					validatePerson();
//				}
//			}
//		};

		// filler
		new Label(parent, SWT.NONE);
	}

	/**
	 * field: device
	 * 
	 * @param parent
	 */
	private void createUI235FieldDevice(final Composite parent) {

		// label
		final Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_device);

		// combo: device
		_cboDevice = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboDevice.setVisibleItemCount(10);
		_cboDevice.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

//		_comboDeviceModifyListener = new ModifyListener() {
//			public void modifyText(final ModifyEvent e) {
//
//				if (_currentPerson != null) {
//
//					final ExternalDevice device = getSelectedDevice();
//
//					if (device == null && _currentPerson.getDeviceReaderId() == null) {
//						return;
//					}
//
//					if (device == null
//							|| (device.deviceId != null && !device.deviceId.equals(_currentPerson.getDeviceReaderId()))) {
//
//						_isPersonModified = true;
//
//						_currentPerson.setDeviceReaderId(device == null ? null : device.deviceId);
//
//						_peopleViewer.update(_currentPerson, null);
//					}
//				}
//				validatePerson();
//			}
//		};

		// spacer
		new Label(parent, SWT.NONE);

		// create device list
		_deviceList = new ArrayList<ExternalDevice>();

		// add special device
		_deviceList.add(null);

		// add all devices which can read from a device
		final List<ExternalDevice> deviceList = DeviceManager.getExternalDeviceList();
		for (final ExternalDevice device : deviceList) {
			_deviceList.add(device);
		}

		// add all devices to the combobox
		for (final ExternalDevice device : _deviceList) {
			if (device == null) {
				_cboDevice.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
			} else {
				_cboDevice.add(device.visibleName);
			}
		}
	}

	/**
	 * field: path to save raw tour data
	 * 
	 * @param parent
	 */
	private void createUI240FieldDataPath(final Composite parent) {
		_rawDataPathEditor = new DirectoryFieldEditor(
				ITourbookPreferences.DUMMY_FIELD,
				Messages.Pref_People_Label_rawdata_path,
				parent);
		_rawDataPathEditor.setEmptyStringAllowed(true);
	}

	private void createUI290DetailsButtons(final Group parent) {

		final Composite btnContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(10, 0).applyTo(btnContainer);
		GridLayoutFactory.fillDefaults().applyTo(btnContainer);
		{
			// button: update
			_btnUpdate = new Button(btnContainer, SWT.NONE);
			_btnUpdate.setText(Messages.App_Action_Update);
			_btnUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onUpdateDetail();
				}
			});
			setButtonLayoutData(_btnUpdate);

			// button: cancel
			_btnCancel = new Button(btnContainer, SWT.NONE);
			_btnCancel.setText(Messages.App_Action_Cancel);
			_btnCancel.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onCancelDetail();
				}
			});
			setButtonLayoutData(_btnCancel);
		}
	}

	@Override
	public void dispose() {

		if (_prefChangeListener != null) {
			_prefStore.removePropertyChangeListener(_prefChangeListener);
		}

		super.dispose();
	}

	private void enableControls(final boolean isPersonValid) {

		final boolean isPersonSelected = _currentPerson != null;

		_btnAdd.setEnabled(isPersonSelected == false || (isPersonSelected && _isPersonModified == false));

		_btnCancel.setEnabled(_isPersonModified);
		_btnUpdate.setEnabled(_isPersonModified && isPersonValid);

		_peopleViewer.getTable().setEnabled(_isPersonModified == false);

	}

	private void firePersonListModifyEvent() {

		if (_isPersonListModified) {

			TourManager.getInstance().clearTourDataCache();

			// fire bike list modify event
			getPreferenceStore().setValue(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED, Math.random());

			_isPersonListModified = false;
		}
	}

	/**
	 * @return Returns default date which is 1.1.1800
	 */
	private org.joda.time.DateTime getDefaultBirthday() {
		return new org.joda.time.DateTime(1800, 1, 1, 0, 0, 0, 0);
	}

	private ExternalDevice getSelectedDevice() {

		final int selectedIndex = _cboDevice.getSelectionIndex();

		if (selectedIndex == -1 || selectedIndex == 0) {
			return null;
		}

		return _deviceList.get(selectedIndex);
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * Check if the default date is set, first day in the date widget is 14.9.1752 but the date is
	 * checked against 1.1.1800
	 * 
	 * @return
	 */
	private boolean isDefaultBirthday() {

		if (_dtBirthDay.getYear() == 1800 && _dtBirthDay.getMonth() == 0 && _dtBirthDay.getDay() == 1) {
			return true;
		}

		return false;
	}

	private boolean isFloatValid(final String floatText) {
		try {
			Float.parseFloat(floatText);
			return true;

		} catch (final NumberFormatException e) {
			setErrorMessage(Messages.Pref_People_Error_invalid_number);
			return false;
		}
	}

	@Override
	public boolean okToLeave() {

		if (validatePerson() == false) {
			return false;
		}

		savePerson();
		firePersonListModifyEvent();

		return super.okToLeave();
	}

	private void onAddPerson() {

		savePerson();

		_currentPerson = new TourPerson();

		_currentPerson.setLastName(UI.EMPTY_STRING);
		_currentPerson.setFirstName(UI.EMPTY_STRING);
		_currentPerson.setHeight(1.77f);
		_currentPerson.setWeight(80f);

		_people.add(_currentPerson);

		_isPersonModified = true;
		_isPersonListModified = true;

		// update ui viewer
		_peopleViewer.add(_currentPerson);
		_isNewPerson = true;
		_peopleViewer.setSelection(new StructuredSelection(_currentPerson));
		validatePerson();

		// edit first name
		_txtFirstName.selectAll();
		_txtFirstName.setFocus();
	}

	private void onCancelDetail() {
		// TODO Auto-generated method stub

	}

	private void onSelectPerson() {

		if (_isNewPerson) {
			_isNewPerson = false;
		} else {
			savePerson();
		}

		updateUIPersonDetails();
	}

	private void onUpdateDetail() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performCancel() {
		firePersonListModifyEvent();
		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();

		final org.joda.time.DateTime dtBirthDay = getDefaultBirthday();

		_dtBirthDay.setDate(dtBirthDay.getYear(), dtBirthDay.getMonthOfYear() - 1, dtBirthDay.getDayOfMonth());
	}

	@Override
	public boolean performOk() {

		if (validatePerson() == false) {
			return false;
		}

		savePerson();
		firePersonListModifyEvent();

		return super.performOk();
	}

//	private void removeModifyListener() {
//
//		_txtFirstName.removeModifyListener(_textFirstNameModifyListener);
//		_txtLastName.removeModifyListener(_textLastNameModifyListener);
//		_txtHeight.removeModifyListener(_textHeightModifyListener);
//		_txtWeight.removeModifyListener(_textWeightModifyListener);
//
//		_cboDevice.addModifyListener(_comboDeviceModifyListener);
//
//		_rawDataPathEditor.setPropertyChangeListener(null);
//	}

	/**
	 * save current person when it was modified
	 */
	private void savePerson() {

		if (_currentPerson != null && _isPersonModified && validatePerson()) {

			final long jodaDate = new org.joda.time.DateTime(
					_dtBirthDay.getYear(),
					_dtBirthDay.getMonth() + 1,
					_dtBirthDay.getDay(),
					0,
					0,
					0,
					0).getMillis();

			_currentPerson.setBirthday(new Date(jodaDate));
			_currentPerson.setRawDataPath(_rawDataPathEditor.getStringValue());

			_currentPerson.persist();

			// update modify flag before the viewer is updated
			_isPersonModified = false;
			_isPersonListModified = true;

			_peopleViewer.update(_currentPerson, null);

			_isPersonModified = false;
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

			// when the device id was not found, select "no selection" entry
			if (deviceIndex == 0) {
				_cboDevice.select(0);
			}
		}
	}

	/**
	 * update person data fields from the selected person in the viewer
	 */
	private void updateUIPersonDetails() {

		final IStructuredSelection selection = (IStructuredSelection) _peopleViewer.getSelection();

		final Object item = selection.getFirstElement();
		boolean isEnabled = true;
		_isDisableModifyListener = true;
		{
			if (item instanceof TourPerson) {

				final TourPerson person = (TourPerson) item;
				_currentPerson = person;

				final Date birthDay = person.getBirthday();
				org.joda.time.DateTime jodaBirthDay;
				if (birthDay == null) {
					jodaBirthDay = getDefaultBirthday();
				} else {
					jodaBirthDay = new org.joda.time.DateTime(birthDay);
				}

				_txtFirstName.setText(person.getFirstName());
				_txtLastName.setText(person.getLastName());
				_txtHeight.setText(Float.toString(person.getHeight()));
				_txtWeight.setText(Float.toString(person.getWeight()));

				_dtBirthDay.setDate(//
						jodaBirthDay.getYear(),
						jodaBirthDay.getMonthOfYear() - 1,
						jodaBirthDay.getDayOfMonth());

				selectDevice(person);

				_rawDataPathEditor.setStringValue(person.getRawDataPath());

			} else {

				isEnabled = false;
				_currentPerson = null;

				_txtFirstName.setText(UI.EMPTY_STRING);
				_txtLastName.setText(UI.EMPTY_STRING);
				_txtHeight.setText(UI.EMPTY_STRING);
				_txtWeight.setText(UI.EMPTY_STRING);

				_cboDevice.select(0);

				_rawDataPathEditor.setStringValue(null);
			}
		}
		_isDisableModifyListener = false;

		_txtFirstName.setEnabled(isEnabled);
		_txtLastName.setEnabled(isEnabled);
		_txtHeight.setEnabled(isEnabled);
		_txtWeight.setEnabled(isEnabled);

		_cboDevice.setEnabled(isEnabled);
		_rawDataPathEditor.setEnabled(isEnabled, _containerPersonDetails);
	}

	/**
	 * Validates person fields
	 * 
	 * @return Returns <code>true</code> when person data are valid, otherwise <code>false</code>
	 */
	private boolean validatePerson() {

		boolean isPersonValid = true;

		while (true) {

			if (_currentPerson == null) {
				break;
			}

			if (_txtFirstName.getText().trim().equals(UI.EMPTY_STRING)) {

				isPersonValid = false;
				_txtFirstName.setFocus();

				setErrorMessage(Messages.Pref_People_Error_first_name_is_required);
				break;
			}

			if (!_rawDataPathEditor.getStringValue().trim().equals(UI.EMPTY_STRING) && !_rawDataPathEditor.isValid()) {

				isPersonValid = false;
				_rawDataPathEditor.setFocus();

				setErrorMessage(Messages.Pref_People_Error_path_is_invalid);
				break;
			}

			if (isDefaultBirthday()) {

				isPersonValid = false;
				_dtBirthDay.setFocus();

				setErrorMessage(Messages.Pref_People_Error_Birthday_IsInvalid);
				break;
			}

			if (isFloatValid(_txtHeight.getText()) == false) {//
				isPersonValid = false;
				break;
			}

			if (isFloatValid(_txtWeight.getText()) == false) {//
				isPersonValid = false;
				break;
			}

			isPersonValid = true;

			break;
		}

		enableControls(isPersonValid);

		if (isPersonValid) {
			setErrorMessage(null);
		}

		return isPersonValid;
	}

}
