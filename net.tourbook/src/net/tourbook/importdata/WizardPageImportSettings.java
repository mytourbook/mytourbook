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
package net.tourbook.importdata;

import gnu.io.CommPortIdentifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;

import net.tourbook.Messages;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class WizardPageImportSettings extends WizardPage {

	// dialog settings
	private static final String			COMBO_DEVICE_ID		= "combo.device-id";		//$NON-NLS-1$
	private static final String			COMBO_PERSON_ID		= "combo.person-id";		//$NON-NLS-1$
	private static final String			COMBO_SERIAL_PORT	= "combo.serial-port";		//$NON-NLS-1$
	private static final String			TEXT_RAW_DATA_PATH	= "text.auto-save-path";	//$NON-NLS-1$

	private boolean						_isPortListAvailable;

	private ArrayList<TourPerson>		_people;
	private ArrayList<ExternalDevice>	_deviceList;
	private ArrayList<String>			_portList;

//	private boolean						_isDeviceAvailable;

	/*
	 * UI controls
	 */
	private Combo						_cboDevice;
	Combo								_cboPorts;
	private Combo						_cboPerson;
	DirectoryFieldEditor				_pathEditor;
	private Composite					_container;

	protected WizardPageImportSettings(final String pageName) {
		super(pageName);
		setTitle(Messages.Import_Wizard_Dlg_title);
		setMessage(Messages.Import_Wizard_Dlg_message);
	}

	public void createControl(final Composite parent) {

		initializeDialogUnits(parent);

		_container = new Composite(parent, SWT.NONE);
		_container.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		_container.setLayout(new GridLayout(3, false));

		createFieldPerson();
		createFieldDevice();
		createFieldRawDataPath();
		createFieldSerialPort();

		restoreDialogSettings();

		enableControls();
		validatePage();

		// control must be set, otherwise nothing is displayed
		setControl(_container);
	}

	/**
	 * create field: device
	 */
	private void createFieldDevice() {

		GridData gd;

		final Label label = new Label(_container, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_device);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.verticalIndent = 10;
		label.setLayoutData(gd);

		_cboDevice = new Combo(_container, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboDevice.setVisibleItemCount(10);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.verticalIndent = 10;
		_cboDevice.setLayoutData(gd);

		_cboDevice.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				validatePage();
			}
		});

		// create device list
		_deviceList = new ArrayList<ExternalDevice>();
		_deviceList.addAll(DeviceManager.getExternalDeviceList());

		_cboDevice.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
		for (final ExternalDevice device : _deviceList) {
			// populate the device listbox
			_cboDevice.add(device.visibleName);
		}

		// filler
		new Label(_container, SWT.NONE);
	}

	/**
	 * create field: person
	 */
	private void createFieldPerson() {
		Label label;
		label = new Label(_container, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_use_settings);

		_cboPerson = new Combo(_container, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboPerson.setVisibleItemCount(10);
		_cboPerson.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		_cboPerson.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				int personIndex = _cboPerson.getSelectionIndex();

				if (personIndex == 0) {
					// person is not selected
//					_isDeviceAvailable = false;
					System.setProperty(WizardImportData.SYSPROPERTY_IMPORT_PERSON, "MyTourbook"); //$NON-NLS-1$
				} else {
					final TourPerson person = _people.get(--personIndex);
					selectPersonDevice(person);
					_pathEditor.setStringValue(person.getRawDataPath());
					System.setProperty(WizardImportData.SYSPROPERTY_IMPORT_PERSON, person.getName());
				}
				validatePage();
			}
		});

		_cboPerson.add(Messages.Import_Wizard_Control_combo_person_default_settings);

		// add people to list
		_people = PersonManager.getTourPeople();
		for (final TourPerson person : _people) {
			_cboPerson.add(person.getName());
		}

		// filler
		new Label(_container, SWT.NONE);
	}

	/**
	 * create field: raw data path
	 */
	private void createFieldRawDataPath() {

		/*
		 * path to save raw tour data
		 */
		_pathEditor = new DirectoryFieldEditor(
				ITourbookPreferences.DUMMY_FIELD,
				Messages.Import_Wizard_Label_auto_save_path,
				_container);

		_pathEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				validatePage();
			}
		});
	}

	/**
	 * create field: serial port
	 */
	@SuppressWarnings("unchecked")
	private void createFieldSerialPort() {

		GridData gd;

		final Label label = new Label(_container, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_serial_port);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.verticalIndent = 10;
		label.setLayoutData(gd);

		_cboPorts = new Combo(_container, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboPorts.setVisibleItemCount(10);
		gd = new GridData(SWT.NONE, SWT.NONE, false, false);
		gd.horizontalSpan = 2;
		gd.verticalIndent = 10;
		_cboPorts.setLayoutData(gd);
		_cboPorts.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				validatePage();
			}
		});

		_portList = new ArrayList<String>();

		// read serial ports from the system
		final Enumeration<CommPortIdentifier> ports = CommPortIdentifier.getPortIdentifiers();
		_isPortListAvailable = ports.hasMoreElements();

		if (_isPortListAvailable) {
			// ports are available
			while (ports.hasMoreElements()) {
				final CommPortIdentifier port = ports.nextElement();
				if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					final String portName = port.getName();
					_portList.add(portName);
					_cboPorts.add(portName);
				}
			}

		} else {
			// no ports are available
			_cboPorts.add(Messages.Import_Wizard_Control_combo_ports_not_available);
			_cboPorts.select(0);
			_cboPorts.setEnabled(false);
		}
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

	void persistDialogSettings() {

		final IDialogSettings settings = getDialogSettings();

		// save person id
		if (!_cboPerson.isDisposed()) {

			int personIndex = _cboPerson.getSelectionIndex();
			if (personIndex <= 0) {
				settings.put(COMBO_PERSON_ID, -1);
			} else {
				final Long personId = _people.get(--personIndex).getPersonId();
				settings.put(COMBO_PERSON_ID, personId);
			}
		}

		// save device id
		if (!_cboDevice.isDisposed()) {

			int deviceIndex = _cboDevice.getSelectionIndex();
			if (deviceIndex <= 0) {
				settings.put(COMBO_DEVICE_ID, -1);
			} else {
				final String deviceId = _deviceList.get(--deviceIndex).deviceId;
				settings.put(COMBO_DEVICE_ID, deviceId);
			}
		}

		// save port
		if (!_cboPorts.isDisposed()) {

			if (_isPortListAvailable && _cboPorts.getSelectionIndex() != -1) {
				settings.put(COMBO_SERIAL_PORT, _cboPorts.getItem(_cboPorts.getSelectionIndex()));
			}
		}

		// save auto save settings
		final String pathValue = _pathEditor.getStringValue();
		if (pathValue != null) {
			// this case happened that the path is null, bug in 10.3
			settings.put(TEXT_RAW_DATA_PATH, pathValue);
		}

	}

	private void restoreDialogSettings() {

		final IDialogSettings settings = getDialogSettings();

		// restore person
		_cboPerson.select(0);
		Long savedPersonId;
		try {
			savedPersonId = settings.getLong(COMBO_PERSON_ID);
			if (savedPersonId != null) {
				int personIndex = 1;
				for (final TourPerson person : _people) {

					if (person.getPersonId() == savedPersonId) {
						_cboPerson.select(personIndex);
						System.setProperty(WizardImportData.SYSPROPERTY_IMPORT_PERSON, person.getName());
						break;
					}
					personIndex++;
				}
			}
		} catch (final NumberFormatException e) {
			// irgnore
		}

		// restore device
		final String savedDeviceId = settings.get(COMBO_DEVICE_ID);
		_cboDevice.select(0);
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

		// restore port
		if (_isPortListAvailable) {
			// select the port which is stored in the settings
			final String savedPort = settings.get(COMBO_SERIAL_PORT);

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

		// restore raw data path
		_pathEditor.setStringValue(settings.get(TEXT_RAW_DATA_PATH));

	}

	/**
	 * select device in the combo box
	 */
	private void selectPersonDevice(final TourPerson person) {

		int deviceIndex = 0;
		int selectIndex = 0;
		final String deviceId = person.getDeviceReaderId();

		if (deviceId == null) {
			_cboDevice.select(0);
		} else {
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
//				_isDeviceAvailable = false;
			} else {
				_cboDevice.select(selectIndex);
//				_isDeviceAvailable = true;
			}
		}
	}

	boolean validatePage() {

		// validate device
		if (_cboDevice.getSelectionIndex() <= 0) {
			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_select_a_device);
			_cboDevice.setFocus();
			return false;
		}

		// validate path
		if (validatePath() == false) {
			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_path_is_invalid);

			_pathEditor.getTextControl(_container).setFocus();
			return false;
		}

		// validate ports
		if (!_isPortListAvailable || _cboPorts.getSelectionIndex() == -1) {
			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_com_port_is_required);

			_cboPorts.setFocus();
			return false;
		}

		setPageComplete(true);
		setErrorMessage(null);
		return true;
	}

	private boolean validatePath() {

		String fileName = _pathEditor.getTextControl(_container).getText();
		fileName = fileName.trim();
		if (fileName.length() == 0) {
			return false;
		}
		final File file = new File(fileName);
		return file.isDirectory();
	}
}
