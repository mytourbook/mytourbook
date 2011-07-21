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
package net.tourbook.importdata;

import gnu.io.CommPortIdentifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class DataTransferWizardPage extends WizardPage {

	private static final String			STATE_DEVICE_ID				= "combo.device-id";		//$NON-NLS-1$
	private static final String			STATE_PERSON_ID				= "combo.person-id";		//$NON-NLS-1$
	private static final String			STATE_SERIAL_PORT			= "combo.serial-port";		//$NON-NLS-1$
	private static final String			STATE_TEXT_RAW_DATA_PATH	= "text.auto-save-path";	//$NON-NLS-1$

	private boolean						_isPortListAvailable;

	private ArrayList<TourPerson>		_people;
	private ArrayList<ExternalDevice>	_deviceList;
	private ArrayList<String>			_portList;

	/*
	 * UI controls
	 */
	private Composite					_container;
	private Combo						_cboDevice;
	Combo								_cboPorts;
	private Combo						_cboPerson;
	DirectoryFieldEditor				_pathEditor;

	protected DataTransferWizardPage(final String pageName) {
		super(pageName);
		setTitle(Messages.Import_Wizard_Dlg_title);
		setMessage(Messages.Import_Wizard_Dlg_message);
	}

	public void createControl(final Composite parent) {

		initializeDialogUnits(parent);

		createUI(parent);
		updateUI();

		restoreState();

		enableControls();
		isPageValid();

		// control must be set, otherwise nothing is displayed
		setControl(_container);
	}

	private void createUI(final Composite parent) {

		_container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(_container);
		{
			createUI10Person(_container);
			createUI12Device(_container);
			createUI16SerialPort(_container);
			createUI14RawDataPath(_container);
		}

		/*
		 * layout must be set/reset after the directory field editor is set, because it is adjusting
		 * the layout and layout data
		 */
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_container);

		GridData gd = (GridData) _cboPerson.getLayoutData();
		gd.grabExcessHorizontalSpace = false;
		gd.horizontalAlignment = SWT.BEGINNING;

		gd = (GridData) _cboDevice.getLayoutData();
		gd.grabExcessHorizontalSpace = false;
		gd.horizontalAlignment = SWT.BEGINNING;

		gd = (GridData) _cboPorts.getLayoutData();
		gd.grabExcessHorizontalSpace = false;
		gd.horizontalAlignment = SWT.BEGINNING;
	}

	/**
	 * create field: person
	 * 
	 * @param parent
	 */
	private void createUI10Person(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_use_settings);

		_cboPerson = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().applyTo(_cboPerson);
		_cboPerson.setVisibleItemCount(20);

		_cboPerson.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectPerson();
			}
		});

		// filler
		new Label(parent, SWT.NONE);
	}

	/**
	 * create field: device
	 * 
	 * @param parent
	 */
	private void createUI12Device(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_device);

		_cboDevice = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().applyTo(_cboDevice);
		_cboDevice.setVisibleItemCount(20);

		_cboDevice.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				isPageValid();
			}
		});

		// filler
		new Label(parent, SWT.NONE);
	}

	/**
	 * create field: raw data path
	 * 
	 * @param parent
	 */
	private void createUI14RawDataPath(final Composite parent) {

		/*
		 * path to save raw tour data
		 */
		_pathEditor = new DirectoryFieldEditor(
				ITourbookPreferences.DUMMY_FIELD,
				Messages.Import_Wizard_Label_auto_save_path,
				parent);

		_pathEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				isPageValid();
			}
		});
	}

	/**
	 * create field: serial port
	 * 
	 * @param parent
	 */
	private void createUI16SerialPort(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_serial_port);

		_cboPorts = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().applyTo(_cboPorts);
		_cboPorts.setVisibleItemCount(20);
		_cboPorts.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				isPageValid();
			}
		});

		// filler
		new Label(parent, SWT.NONE);
	}

	private void enableControls() {

		_cboDevice.setEnabled(true);
		_pathEditor.setEnabled(true, _container);
	}

	/**
	 * @return Return the device which is selected in the device list or <code>null</code> when no
	 *         device is selected
	 */
	public ExternalDevice getSelectedDevice() {

		int deviceIndex = _cboDevice.getSelectionIndex();
		if (deviceIndex <= 0) {
			return null;
		} else {
			return _deviceList.get(--deviceIndex);
		}
	}

	boolean isPageValid() {

		// validate device
		if (_cboDevice.getSelectionIndex() <= 0) {

			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_select_a_device);
			_cboDevice.setFocus();
			return false;
		}

		// validate ports
		if (!_isPortListAvailable || _cboPorts.getSelectionIndex() == -1) {

			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_com_port_is_required);

			_cboPorts.setFocus();
			return false;
		}

		// validate path
		if (isPathValid() == false) {

			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_path_is_invalid);

			_pathEditor.getTextControl(_container).setFocus();
			return false;
		}

		setPageComplete(true);
		setErrorMessage(null);

		return true;
	}

	private boolean isPathValid() {

		final String fileName = _pathEditor.getTextControl(_container).getText().trim();
		if (fileName.length() == 0) {
			return false;
		}

		return new File(fileName).isDirectory();
	}

	private void onSelectPerson() {

		int personIndex = _cboPerson.getSelectionIndex();
		if (personIndex == 0) {

			// a person is not selected
			System.setProperty(DataTransferWizard.SYSPROPERTY_IMPORT_PERSON, "MyTourbook"); //$NON-NLS-1$

		} else {

			final TourPerson person = _people.get(--personIndex);

			selectDevice(person);
			_pathEditor.setStringValue(person.getRawDataPath());

			System.setProperty(DataTransferWizard.SYSPROPERTY_IMPORT_PERSON, person.getName());
		}

		isPageValid();
	}

	private void restoreState() {

		final IDialogSettings state = getDialogSettings();

		/*
		 * person
		 */
		_cboPerson.select(0);

		final long savedPersonId = Util.getStateLong(state, STATE_PERSON_ID, -1);
		if (savedPersonId != -1) {

			int personIndex = 1;
			for (final TourPerson person : _people) {

				if (person.getPersonId() == savedPersonId) {
					_cboPerson.select(personIndex);
					System.setProperty(DataTransferWizard.SYSPROPERTY_IMPORT_PERSON, person.getName());
					break;
				}
				personIndex++;
			}
		}

		/*
		 * device
		 */
		_cboDevice.select(0);

		final String savedDeviceId = state.get(STATE_DEVICE_ID);
		if (savedDeviceId != null) {
			int deviceIndex = 1;
			for (final ExternalDevice device : _deviceList) {

				if (device.deviceId.equals(savedDeviceId)) {
					_cboDevice.select(deviceIndex);
					break;
				}
				deviceIndex++;
			}
		}

		/*
		 * port
		 */
		if (_isPortListAvailable) {

			// select the port which is stored in the settings

			final String savedPort = state.get(STATE_SERIAL_PORT);
			if (savedPort != null) {

				int portIndex = 0;
				for (final String port : _portList) {
					if (port.equalsIgnoreCase(savedPort)) {
						_cboPorts.select(portIndex);
						break;
					}
					portIndex++;
				}
			}
		}

		/*
		 * raw data path
		 */
		_pathEditor.setStringValue(state.get(STATE_TEXT_RAW_DATA_PATH));

	}

	void saveState() {

		if (_cboPerson.isDisposed()) {
			return;
		}

		final IDialogSettings state = getDialogSettings();

		/*
		 * person id
		 */
		int personIndex = _cboPerson.getSelectionIndex();
		if (personIndex <= 0) {
			state.put(STATE_PERSON_ID, -1);
		} else {
			final Long personId = _people.get(--personIndex).getPersonId();
			state.put(STATE_PERSON_ID, personId);
		}

		/*
		 * device id
		 */
		int deviceIndex = _cboDevice.getSelectionIndex();
		if (deviceIndex <= 0) {
			state.put(STATE_DEVICE_ID, -1);
		} else {
			final String deviceId = _deviceList.get(--deviceIndex).deviceId;
			state.put(STATE_DEVICE_ID, deviceId);
		}

		/*
		 * port
		 */
		if (_isPortListAvailable && _cboPorts.getSelectionIndex() != -1) {
			state.put(STATE_SERIAL_PORT, _cboPorts.getItem(_cboPorts.getSelectionIndex()));
		}

		// save auto save settings
		final String pathValue = _pathEditor.getStringValue();
		if (pathValue != null) {
			// this case happened that the path is null, bug in 10.3
			state.put(STATE_TEXT_RAW_DATA_PATH, pathValue);
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
			int selectIndex = 0;

			for (final ExternalDevice device : _deviceList) {
				if (deviceId.equals(device.deviceId)) {

					// skip first entry
					deviceIndex++;

					selectIndex = deviceIndex;
					break;
				}
				deviceIndex++;
			}

			/*
			 * when the device was not found or when the data can't be read from the device, select
			 * the "<Not Selected>" entry
			 */
			if (selectIndex == 0) {
				_cboDevice.select(0);
			} else {
				_cboDevice.select(selectIndex);
			}
		}
	}

	private void updateUI() {

		/*
		 * people
		 */
		_cboPerson.add(Messages.Import_Wizard_Control_combo_person_default_settings);

		// add people to list
		_people = PersonManager.getTourPeople();
		for (final TourPerson person : _people) {
			_cboPerson.add(person.getName());
		}

		/*
		 * device
		 */
		_deviceList = new ArrayList<ExternalDevice>();
		_deviceList.addAll(DeviceManager.getExternalDeviceList());

		// set 1st item
		_cboDevice.add(DeviceManager.DEVICE_IS_NOT_SELECTED);

		// populate the device listbox
		for (final ExternalDevice device : _deviceList) {
			_cboDevice.add(device.visibleName);
		}

		/*
		 * file path
		 */

		/**
		 * set pref store because when not set, a NPE can be raised
		 * 
		 * <pre>
		 * 
		 * Caused by: java.lang.NullPointerException
		 *         at org.eclipse.jface.preference.StringFieldEditor.getStringValue(StringFieldEditor.java:305)
		 *         at net.tourbook.importdata.DataTransferWizardPage.persistDialogSettings(DataTransferWizardPage.java:305)
		 * </pre>
		 */
		_pathEditor.setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
		_pathEditor.setPreferenceName("dummy");//$NON-NLS-1$

		/*
		 * ports
		 */
		_portList = new ArrayList<String>();

		// read serial ports from the system
		@SuppressWarnings("unchecked")
		final Enumeration<CommPortIdentifier> ports = CommPortIdentifier.getPortIdentifiers();
		_isPortListAvailable = ports.hasMoreElements();

		System.out.println("Available ports:"); //$NON-NLS-1$

		if (_isPortListAvailable) {
			// ports are available
			while (ports.hasMoreElements()) {

				final CommPortIdentifier port = ports.nextElement();
				final String portName = port.getName();

				if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {

					_portList.add(portName);
					_cboPorts.add(portName);

					System.out.println("\tserial port:\t" + portName); //$NON-NLS-1$

				} else {
					System.out.println("\tother port:\t" + portName); //$NON-NLS-1$
				}
			}

		} else {

			// ports are not available
			_cboPorts.add(Messages.Import_Wizard_Control_combo_ports_not_available);
			_cboPorts.select(0);
			_cboPorts.setEnabled(false);

			System.out.println("\tserial ports are not available"); //$NON-NLS-1$
		}
	}
}
