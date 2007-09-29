/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import net.tourbook.database.TourDatabase;
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

	Combo								fComboDevice;
	Combo								fComboPorts;

	private boolean						fIsPortListAvailable;

	private ArrayList<TourPerson>		fPeopleList;
	private ArrayList<TourbookDevice>	fDeviceList;
	private ArrayList<TourbookDevice>	fDeviceListWithDeviceImport;

	private ArrayList<String>			fPortList;
	private Combo						fComboPerson;
	DirectoryFieldEditor				fPathEditor;
	private Composite					fGroupContainer;
	protected boolean					fIsDeviceAvailable;

	protected WizardPageImportSettings(final String pageName) {
		super(pageName);
		setTitle(Messages.Import_Wizard_Dlg_title);
		setMessage(Messages.Import_Wizard_Dlg_message);
	}

	public void createControl(final Composite parent) {

		initializeDialogUnits(parent);

		fGroupContainer = new Composite(parent, SWT.NONE);
		fGroupContainer.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fGroupContainer.setLayout(new GridLayout(3, false));

		createFieldPerson();
		createFieldDevice();
		createFieldRawDataPath();
		createFieldSerialPort();

		restoreDialogSettings();

		enableControls();
		validatePage();

		// control must be set, otherwise nothing is displayed
		setControl(fGroupContainer);
	}

	/**
	 * create field: device
	 */
	private void createFieldDevice() {

		GridData gd;

		Label label = new Label(fGroupContainer, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_device);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.verticalIndent = 10;
		label.setLayoutData(gd);

		fComboDevice = new Combo(fGroupContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboDevice.setVisibleItemCount(10);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.verticalIndent = 10;
		fComboDevice.setLayoutData(gd);

		fComboDevice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				validatePage();
			}
		});

		// create device list
		fDeviceList = new ArrayList<TourbookDevice>();
		fDeviceList.addAll(DeviceManager.getDeviceList());

		fDeviceListWithDeviceImport = new ArrayList<TourbookDevice>();

		fComboDevice.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
		for (final TourbookDevice device : fDeviceList) {
			if (device.canReadFromDevice) {

				// populate the device listbox
				fComboDevice.add(device.visibleName);

				// keep the device from where the data can be read
				fDeviceListWithDeviceImport.add(device);
			}
		}

		// filler
		new Label(fGroupContainer, SWT.NONE);
	}

	/**
	 * create field: person
	 */
	private void createFieldPerson() {
		Label label;
		label = new Label(fGroupContainer, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_use_settings);

		fComboPerson = new Combo(fGroupContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboPerson.setVisibleItemCount(10);
		fComboPerson.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		fComboPerson.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int personIndex = fComboPerson.getSelectionIndex();

				if (personIndex == 0) {
					// person is not selected
					fIsDeviceAvailable = false;
				} else {
					final TourPerson person = fPeopleList.get(--personIndex);
					selectPersonDevice(person);
					fPathEditor.setStringValue(person.getRawDataPath());
				}
				validatePage();
			}
		});

		fComboPerson.add(Messages.Import_Wizard_Control_combo_person_default_settings);

		// add people to list
		fPeopleList = TourDatabase.getTourPeople();
		for (final TourPerson person : fPeopleList) {
			String lastName = person.getLastName();
			lastName = lastName.equals("") ? "" : " " + lastName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fComboPerson.add(person.getFirstName() + lastName);
		}

		// filler
		label = new Label(fGroupContainer, SWT.NONE);
	}

	/**
	 * create field: raw data path
	 */
	private void createFieldRawDataPath() {

		/*
		 * path to save raw tour data
		 */
		fPathEditor = new DirectoryFieldEditor(
				ITourbookPreferences.DUMMY_FIELD,
				Messages.Import_Wizard_Label_auto_save_path,
				fGroupContainer);

		fPathEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				validatePage();
			}
		});
	}

	/**
	 * create field: serial port
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	private void createFieldSerialPort() {

		GridData gd;

		final Label label = new Label(fGroupContainer, SWT.NONE);
		label.setText(Messages.Import_Wizard_Label_serial_port);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.verticalIndent = 10;
		label.setLayoutData(gd);

		fComboPorts = new Combo(fGroupContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboPorts.setVisibleItemCount(10);
		gd = new GridData(SWT.NONE, SWT.NONE, false, false);
		gd.horizontalSpan = 2;
		gd.verticalIndent = 10;
		fComboPorts.setLayoutData(gd);
		fComboPorts.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				validatePage();
			}
		});

		fPortList = new ArrayList<String>();

		// read serial ports from the system
		final Enumeration<CommPortIdentifier> ports = CommPortIdentifier.getPortIdentifiers();
		fIsPortListAvailable = ports.hasMoreElements();

		if (fIsPortListAvailable) {
			// ports are available
			while (ports.hasMoreElements()) {
				final CommPortIdentifier port = ports.nextElement();
				if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					final String portName = port.getName();
					fPortList.add(portName);
					fComboPorts.add(portName);
				}
			}

		} else {
			// no ports are available
			fComboPorts.add(Messages.Import_Wizard_Control_combo_ports_not_available);
			fComboPorts.select(0);
			fComboPorts.setEnabled(false);
		}
	}

	private void enableControls() {

		fComboDevice.setEnabled(true);
		fPathEditor.setEnabled(true, fGroupContainer);
	}

	/**
	 * @return Return the device which is selected in the device list or <code>null</code> when no
	 *         device is selected
	 */
	public TourbookDevice getSelectedDevice() {

		int deviceIndex = fComboDevice.getSelectionIndex();
		if (deviceIndex <= 0) {
			return null;
		} else {
			return fDeviceListWithDeviceImport.get(--deviceIndex);
		}
	}

	void persistDialogSettings() {

		final IDialogSettings settings = getDialogSettings();

		// save person id
		int personIndex = fComboPerson.getSelectionIndex();
		if (personIndex <= 0) {
			settings.put(COMBO_PERSON_ID, -1);
		} else {
			final Long personId = fPeopleList.get(--personIndex).getPersonId();
			settings.put(COMBO_PERSON_ID, personId);
		}

		// save device id
		int deviceIndex = fComboDevice.getSelectionIndex();
		if (deviceIndex <= 0) {
			settings.put(COMBO_DEVICE_ID, -1);
		} else {
			final String deviceId = fDeviceList.get(--deviceIndex).deviceId;
			settings.put(COMBO_DEVICE_ID, deviceId);
		}

		// save port
		if (fIsPortListAvailable && fComboPorts.getSelectionIndex() != -1) {
			settings.put(COMBO_SERIAL_PORT, fComboPorts.getItem(fComboPorts.getSelectionIndex()));
		}

		// save auto save settings
		settings.put(TEXT_RAW_DATA_PATH, fPathEditor.getStringValue());

	}

	private void restoreDialogSettings() {

		final IDialogSettings settings = getDialogSettings();

		// restore person
		fComboPerson.select(0);
		Long savedPersonId;
		try {
			savedPersonId = settings.getLong(COMBO_PERSON_ID);
			if (savedPersonId != null) {
				int personIndex = 1;
				for (final TourPerson person : fPeopleList) {

					if (person.getPersonId() == savedPersonId) {
						fComboPerson.select(personIndex);
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
		fComboDevice.select(0);
		if (savedDeviceId != null) {
			int deviceIndex = 1;
			for (final TourbookDevice device : fDeviceList) {

				if (device.deviceId.equals(savedDeviceId)) {
					fComboDevice.select(deviceIndex);
					break;
				}
				deviceIndex++;
			}
		}

		// restore port
		if (fIsPortListAvailable) {
			// select the port which is stored in the settings
			final String savedPort = settings.get(COMBO_SERIAL_PORT);

			if (savedPort != null) {
				int portIndex = 0;
				for (final String port : fPortList) {
					if (port.equalsIgnoreCase(savedPort)) {
						fComboPorts.select(portIndex);
						break;
					}
					portIndex++;
				}
			}
		}

		// restore raw data path
		fPathEditor.setStringValue(settings.get(TEXT_RAW_DATA_PATH));

	}

	/**
	 * select device in the combo box
	 */
	private void selectPersonDevice(final TourPerson person) {

		int deviceIndex = 0;
		int selectIndex = 0;
		final String deviceId = person.getDeviceReaderId();

		if (deviceId == null) {
			fComboDevice.select(0);
		} else {
			for (final TourbookDevice device : fDeviceListWithDeviceImport) {
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
				fComboDevice.select(0);
				fIsDeviceAvailable = false;
			} else {
				fComboDevice.select(selectIndex);
				fIsDeviceAvailable = true;
			}
		}
	}

	boolean validatePage() {

		// validate device
		if (fComboDevice.getSelectionIndex() <= 0) {
			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_select_a_device);
			fComboDevice.setFocus();
			return false;
		}

		// validate path
		if (validatePath() == false) {
			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_path_is_invalid);
			
			fPathEditor.getTextControl(fGroupContainer).setFocus();
			return false;
		}

		// validate ports
		if (!fIsPortListAvailable || fComboPorts.getSelectionIndex() == -1) {
			setPageComplete(false);
			setErrorMessage(Messages.Import_Wizard_Error_com_port_is_required);
			
			fComboPorts.setFocus();
			return false;
		}
		
		setPageComplete(true);
		setErrorMessage(null);
		return true;
	}

	private boolean validatePath() {

		String fileName = fPathEditor.getTextControl(fGroupContainer).getText();
		fileName = fileName.trim();
		if (fileName.length() == 0) {
			return false;
		}
		File file = new File(fileName);
		return file.isDirectory();
	}
}
