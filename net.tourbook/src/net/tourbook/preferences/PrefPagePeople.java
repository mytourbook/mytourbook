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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourBike;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.InputFieldFloat;
import net.tourbook.ui.UI;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePeople extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID						= "net.tourbook.preferences.PrefPagePeopleId";		//$NON-NLS-1$

	private static final int			COLUMN_IS_MODIFIED		= 0;
	private static final int			COLUMN_FIRSTNAME		= 1;
	private static final int			COLUMN_LASTNAME			= 2;
	private static final int			COLUMN_DEVICE			= 3;
	private static final int			COLUMN_HEIGHT			= 4;
	private static final int			COLUMN_WEIGHT			= 5;

	private final IPreferenceStore		_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();

	private ModifyListener				_textFirstNameModifyListener;
	private ModifyListener				_textLastNameModifyListener;
	private ModifyListener				_textHeightModifyListener;
	private ModifyListener				_textWeightModifyListener;
	private ModifyListener				_comboDeviceModifyListener;
	private ModifyListener				_comboBikeModifyListener;
	private IPropertyChangeListener		_prefChangeListener;

	private ArrayList<TourPerson>		_people;

	private TourPerson					_currentPerson;
	private boolean						_isPersonModified;
	private TourBike[]					_bikes;

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

	private Composite					_personDetailContainer;
	private Text						_txtFirstName;
	private Text						_txtLastName;
	private Text						_txtHeight;
	private Text						_txtWeight;
	private Combo						_cboDevice;
	private Combo						_cboBike;
	private DirectoryFieldEditor		_rawDataPathEditor;

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {

			if (_people == null) {
				_people = PersonManager.getTourPeople();
			}

			return _people.toArray(new TourPerson[_people.size()]);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private class ClientsLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		public String getColumnText(final Object obj, final int index) {

			final TourPerson tourPerson = ((TourPerson) obj);

			switch (index) {
			case COLUMN_IS_MODIFIED:
				return _isPersonModified ? "*" : UI.EMPTY_STRING; //$NON-NLS-1$

			case COLUMN_FIRSTNAME:
				return tourPerson.getFirstName();

			case COLUMN_LASTNAME:
				return tourPerson.getLastName();

			case COLUMN_DEVICE:

				final String deviceId = tourPerson.getDeviceReaderId();

				if (deviceId != null) {
					for (final ExternalDevice device : _deviceList) {
						if (device != null && deviceId.equals(device.deviceId)) {
							return device.visibleName;
						}
					}
				}
				break;

			case COLUMN_HEIGHT:
				return Float.toString(tourPerson.getHeight());

			case COLUMN_WEIGHT:
				return Float.toString(tourPerson.getWeight());
			}
			return UI.EMPTY_STRING;
		}
	}

	private void addModifyListener() {

		_txtFirstName.addModifyListener(_textFirstNameModifyListener);
		_txtLastName.addModifyListener(_textLastNameModifyListener);
		_txtHeight.addModifyListener(_textHeightModifyListener);
		_txtWeight.addModifyListener(_textWeightModifyListener);

		_cboDevice.addModifyListener(_comboDeviceModifyListener);
		_cboBike.addModifyListener(_comboBikeModifyListener);

		_rawDataPathEditor.setPropertyChangeListener(new org.eclipse.jface.util.IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				if (_currentPerson != null) {

					_isPersonModified = true;
					_peopleViewer.update(_currentPerson, null);

					validatePerson();
				}
			}
		});

	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_BIKE_LIST_IS_MODIFIED)) {

					// create new bike list
					_cboBike.removeAll();
					createBikeList();

					// update person details
					showSelectedPersonDetails();
				}
			}

		};
		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void createBikeList() {

		// create bike list
		_cboBike.add(DeviceManager.DEVICE_IS_NOT_SELECTED);

		final ArrayList<TourBike> bikes = TourDatabase.getTourBikes();

		if (bikes == null) {
			_bikes = new TourBike[0];
		} else {
			_bikes = bikes.toArray(new TourBike[bikes.size()]);
			for (final TourBike bike : _bikes) {
				_cboBike.add(bike.getName());
			}
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		initializeDialogUnits(parent);

		final Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.Pref_People_Title);

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createPeopleViewer(container);
		createPeopleViewerButtons(container);
		createPersonDetails(container);

		// enableButtons();
		addPrefListener();

		_peopleViewer.setInput(this);

		// select first person
		_peopleViewer.getTable().setSelection(0);
		showSelectedPersonDetails();
		enableButtons(true);

		return container;
	}

	private void createFieldBike() {
		Label lbl;
		/*
		 * field: bike
		 */
		lbl = new Label(_personDetailContainer, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_bike);
		_cboBike = new Combo(_personDetailContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboBike.setVisibleItemCount(10);
		_cboBike.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		_comboBikeModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentPerson != null) {
					int selectedIndex = _cboBike.getSelectionIndex();
					if (selectedIndex != -1) {
						final TourBike personTourBike = _currentPerson.getTourBike();
						if (selectedIndex == 0) {
							if (personTourBike == null) {
								// person had no bike this was not changed
								return;
							} else {
								// person had before a bike which is now
								// removed
								_currentPerson.setTourBike(null);
								_isPersonModified = true;
							}
							return;
						}

						// adjust to correct index in the bike array
						selectedIndex--;

						final TourBike selectedBike = _bikes[selectedIndex];

						if (personTourBike == null || (personTourBike.getBikeId() != selectedBike.getBikeId())) {

							_currentPerson.setTourBike(selectedBike);
							_isPersonModified = true;
						}

						if (_isPersonModified) {
							_peopleViewer.update(_currentPerson, null);
						}
					}
				}
			}
		};

		createBikeList();

		// filler
		new Label(_personDetailContainer, SWT.NONE);
	}

	/**
	 * field: device
	 */
	private void createFieldDevice() {

		final Label lbl = new Label(_personDetailContainer, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_device);

		_cboDevice = new Combo(_personDetailContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		_cboDevice.setVisibleItemCount(10);
		_cboDevice.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		_comboDeviceModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {

				if (_currentPerson != null) {

					final ExternalDevice device = getSelectedDevice();

					if (device == null && _currentPerson.getDeviceReaderId() == null) {
						return;
					}

					if (device == null
							|| (device.deviceId != null && !device.deviceId.equals(_currentPerson.getDeviceReaderId()))) {

						_currentPerson.setDeviceReaderId(device == null ? null : device.deviceId);

						_isPersonModified = true;

						_peopleViewer.update(_currentPerson, null);
					}
				}
				validatePerson();
			}
		};

		// spacer
		new Label(_personDetailContainer, SWT.NONE);

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
	 * field: first name
	 */
	private void createFieldFirstName() {

		final Label lbl = new Label(_personDetailContainer, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_first_name);

		_txtFirstName = new Text(_personDetailContainer, SWT.BORDER);
		_txtFirstName.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		_textFirstNameModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentPerson != null) {
					final String firstName = ((Text) (e.widget)).getText();
					if (!firstName.equals(_currentPerson.getFirstName())) {
						_isPersonModified = true;

						_currentPerson.setFirstName(firstName);
						_peopleViewer.update(_currentPerson, null);
					}
				}
				validatePerson();
			}
		};

		// spacer
		new Label(_personDetailContainer, SWT.NONE);
	}

	/**
	 * field: height
	 */
	private void createFieldHeight(final int floatInputWidth) {

		final InputFieldFloat floatInput = new InputFieldFloat(
				_personDetailContainer,
				Messages.Pref_People_Label_height,
				floatInputWidth);

		_txtHeight = floatInput.getTextField();

		_textHeightModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentPerson != null) {
					final Text control = (Text) e.widget;
					try {
						final float value = Float.parseFloat(((Text) (e.widget)).getText());
						if (value != _currentPerson.getHeight()) {
							_currentPerson.setHeight(value);
							_peopleViewer.update(_currentPerson, null);
						}
						UI.setDefaultColor(control);
					} catch (final NumberFormatException e1) {
						UI.setErrorColor(control);
					}
					_isPersonModified = true;
					validatePerson();
				}
			}
		};

		// filler
		new Label(_personDetailContainer, SWT.NONE);
	}

	/**
	 * field: last name
	 */
	private void createFieldLastName() {

		final Label lbl = new Label(_personDetailContainer, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_last_name);

		_txtLastName = new Text(_personDetailContainer, SWT.BORDER);
		final GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		_txtLastName.setLayoutData(gd);
		_textLastNameModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentPerson != null) {
					final String lastName = ((Text) (e.widget)).getText();
					if (!lastName.equals(_currentPerson.getLastName())) {
						_currentPerson.setLastName(lastName);
						_isPersonModified = true;

						_peopleViewer.update(_currentPerson, null);
					}
				}
			}
		};

		// filler
		new Label(_personDetailContainer, SWT.NONE);
	}

	/**
	 * field: weight
	 */
	private void createFieldWeight(final int floatInputWidth) {

		final InputFieldFloat floatInput = new InputFieldFloat(
				_personDetailContainer,
				Messages.Pref_People_Label_weight,
				floatInputWidth);

		_txtWeight = floatInput.getTextField();

		_textWeightModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				if (_currentPerson != null) {
					final Text control = (Text) e.widget;
					try {
						final float value = Float.parseFloat(((Text) (e.widget)).getText());
						if (value != _currentPerson.getWeight()) {
							_currentPerson.setWeight(value);
							_peopleViewer.update(_currentPerson, null);
						}
						UI.setDefaultColor(control);
					} catch (final NumberFormatException e1) {
						UI.setErrorColor(control);
					}
					_isPersonModified = true;
					validatePerson();
				}
			}
		};

		// filler
		new Label(_personDetailContainer, SWT.NONE);
	}

	private void createPeopleViewer(final Composite container) {

		final TableLayoutComposite layouter = new TableLayoutComposite(container, SWT.NONE);
		final GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = convertWidthInCharsToPixels(30);
		layouter.setLayoutData(gridData);

		final Table table = new Table(
				layouter,
				(SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tc;

		tc = new TableColumn(table, SWT.NONE);
		layouter.addColumnData(new ColumnPixelData(convertHorizontalDLUsToPixels(3 * 4), false));

		tc = new TableColumn(table, SWT.NONE);
		tc.setText(Messages.Pref_People_Column_first_name);
		layouter.addColumnData(new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

		tc = new TableColumn(table, SWT.NONE);
		tc.setText(Messages.Pref_People_Column_last_name);
		layouter.addColumnData(new ColumnWeightData(5, convertWidthInCharsToPixels(5)));

		tc = new TableColumn(table, SWT.NONE);
		tc.setText(Messages.Pref_People_Column_device);
		layouter.addColumnData(new ColumnWeightData(3, convertWidthInCharsToPixels(3)));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.Pref_People_Column_height);
		layouter.addColumnData(new ColumnPixelData(convertHorizontalDLUsToPixels(8 * 4), true));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.Pref_People_Column_weight);
		layouter.addColumnData(new ColumnPixelData(convertHorizontalDLUsToPixels(8 * 4), true));

		_peopleViewer = new TableViewer(table);

		_peopleViewer.setContentProvider(new ClientsContentProvider());
		_peopleViewer.setLabelProvider(new ClientsLabelProvider());

		_peopleViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {
				return ((TourPerson) e1).getLastName().compareTo(((TourPerson) e2).getLastName());
			}
		});

		_peopleViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {
				return ((TourPerson) e1).getLastName().compareTo(((TourPerson) e2).getLastName());
			}
		});

		_peopleViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				if (_isNewPerson) {
					_isNewPerson = false;
				} else {
					savePerson();
				}

				showSelectedPersonDetails();
			}
		});

		_peopleViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				_txtFirstName.setFocus();
				_txtFirstName.selectAll();
			}
		});
	}

	private void createPeopleViewerButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginRight = 0;
		container.setLayout(gridLayout);

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

		// button: delete

		/*
		 * "Delete" button is disabled because the tours don't display the info that the person was
		 * removed
		 */
		// fButtonDelete = new Button(container, SWT.NONE);
		// fButtonDelete.setText("&Delete");
		// GridData gd = setButtonLayoutData(fButtonDelete);
		// gd.verticalIndent = 10;
		// fButtonDelete.addSelectionListener(new SelectionAdapter() {
		// public void widgetSelected(SelectionEvent e) {
		// onDeletePerson();
		// // enableButtons();
		// }
		// });
	}

	private void createPersonDetails(final Composite parent) {

		GridLayout gl;
		GridData gd;
		final int floatInputWidth = convertHorizontalDLUsToPixels(40);

		// person data group
		final Group groupPersonInfo = new Group(parent, SWT.NONE);
		groupPersonInfo.setText(Messages.Pref_People_Group_person);
		groupPersonInfo.setLayout(new GridLayout(1, false));
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		groupPersonInfo.setLayoutData(gd);

		_personDetailContainer = new Composite(groupPersonInfo, SWT.NONE);
		gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		_personDetailContainer.setLayout(gl);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		_personDetailContainer.setLayoutData(gd);

		createFieldFirstName();
		createFieldLastName();
		createFieldWeight(floatInputWidth);
		createFieldHeight(floatInputWidth);
		createFieldDevice();
		createFieldBike();

		/**
		 * field: path to save raw tour data
		 */
		_rawDataPathEditor = new DirectoryFieldEditor(
				ITourbookPreferences.DUMMY_FIELD,
				Messages.Pref_People_Label_rawdata_path,
				_personDetailContainer);
		_rawDataPathEditor.setEmptyStringAllowed(true);

		// placeholder
		new Label(parent, SWT.NONE);
		new Label(parent, SWT.NONE);
	}

//	/**
//	 * Delete person from the the database
//	 *
//	 * @param person
//	 * @return
//	 */
//	private boolean deletePerson(final TourPerson person) {
//
//		if (removePersonFromTourData(person)) {
//			if (deletePersonFromDb(person)) {
//				return true;
//			}
//		}
//
//		return false;
//	}

//	private boolean deletePersonFromDb(final TourPerson person) {
//
//		boolean returnResult = false;
//
//		final EntityManager em = TourDatabase.getInstance().getEntityManager();
//		final EntityTransaction ts = em.getTransaction();
//
//		try {
//			final TourPerson entity = em.find(TourPerson.class, person.getPersonId());
//
//			if (entity != null) {
//				ts.begin();
//				em.remove(entity);
//				ts.commit();
//			}
//
//		} catch (final Exception e) {
//			e.printStackTrace();
//		} finally {
//			if (ts.isActive()) {
//				ts.rollback();
//			} else {
//				returnResult = true;
//			}
//			em.close();
//		}
//
//		return returnResult;
//	}

	@Override
	public void dispose() {

		if (_prefChangeListener != null) {
			_prefStore.removePropertyChangeListener(_prefChangeListener);
		}

		super.dispose();
	}

	private void enableButtons(final boolean isPersonValid) {

//		IStructuredSelection selection = (IStructuredSelection) fPeopleViewer.getSelection();

//		boolean isPersonSelected = !selection.isEmpty();

		_btnAdd.setEnabled(_currentPerson == null || (_currentPerson != null && isPersonValid));

		// fButtonDelete.setEnabled(isPersonSelected);
	}

	private void firePersonListModifyEvent() {

		if (_isPersonListModified) {

			TourManager.getInstance().clearTourDataCache();

			// fire bike list modify event
			getPreferenceStore().setValue(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED, Math.random());

			_isPersonListModified = false;
		}
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

//	private void onDeletePerson() {
//
//		final IStructuredSelection selection = (IStructuredSelection) _peopleViewer.getSelection();
//		if (selection.isEmpty()) {
//			return;
//		}
//
//		// ask for the reference tour name
//		final String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };
//
//		final MessageDialog dialog = new MessageDialog(
//				getShell(),
//				Messages.Pref_People_Dlg_del_person_title,
//				null,
//				Messages.Pref_People_Dlg_del_person_message,
//				MessageDialog.QUESTION,
//				buttons,
//				1);
//
//		if (dialog.open() != Window.OK) {
//			return;
//		}
//
//		BusyIndicator.showWhile(null, new Runnable() {
//			@SuppressWarnings("unchecked")
//			public void run() {
//
//				final Table table = _peopleViewer.getTable();
//				final int lastIndex = table.getSelectionIndex();
//
//				for (final Iterator<TourPerson> iter = selection.iterator(); iter.hasNext();) {
//					final TourPerson person = iter.next();
//
//					deletePerson(person);
//
//					// remove from data model
//					_people.remove(person);
//				}
//
//				// remove from ui
//				_peopleViewer.remove(selection.toArray());
//
//				// select next person
//				if (lastIndex >= _people.size()) {
//					table.setSelection(_people.size() - 1);
//				} else {
//					table.setSelection(lastIndex);
//				}
//
//				_currentPerson = null;
//				_isPersonModified = false;
//				_isPersonListModified = true;
//
//				showSelectedPersonDetails();
//			}
//		});
//	}

	@Override
	public boolean performCancel() {
		firePersonListModifyEvent();
		return super.performCancel();
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

	private void removeModifyListener() {

		_txtFirstName.removeModifyListener(_textFirstNameModifyListener);
		_txtLastName.removeModifyListener(_textLastNameModifyListener);
		_txtHeight.removeModifyListener(_textHeightModifyListener);
		_txtWeight.removeModifyListener(_textWeightModifyListener);

		_cboDevice.addModifyListener(_comboDeviceModifyListener);
		_cboBike.addModifyListener(_comboBikeModifyListener);

		_rawDataPathEditor.setPropertyChangeListener(null);
	}

//	private boolean removePersonFromTourData(final TourPerson person) {
//
//		boolean returnResult = false;
//
//		final EntityManager em = TourDatabase.getInstance().getEntityManager();
//
//		if (em != null) {
//
//			final Query query = em.createQuery(//
//					"SELECT tourData" //$NON-NLS-1$
//							+ (" FROM TourData as tourData") //$NON-NLS-1$
//							+ (" WHERE tourData.tourPerson.personId=" + person.getPersonId())); //$NON-NLS-1$
//
//			final ArrayList<TourData> tourDataList = (ArrayList<TourData>) query.getResultList();
//
//			if (tourDataList.size() > 0) {
//
//				final EntityTransaction ts = em.getTransaction();
//
//				try {
//
//					ts.begin();
//
//					// remove person from all saved tour data for this person
//					for (final TourData tourData : tourDataList) {
//						tourData.setTourPerson(null);
//						em.merge(tourData);
//					}
//
//					ts.commit();
//
//				} catch (final Exception e) {
//					e.printStackTrace();
//				} finally {
//					if (ts.isActive()) {
//						ts.rollback();
//					}
//				}
//			}
//
//			returnResult = true;
//			em.close();
//		}
//
//		return returnResult;
//	}

	/**
	 * save current person when it was modified
	 */
	private void savePerson() {

		if (_currentPerson != null && _isPersonModified && validatePerson()) {

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
	 * select bike in the combo box
	 */
	private void selectBike(final TourPerson person) {

		// select default value
		int bikeIndex = 0;
		final TourBike personBike = person.getTourBike();

		if (personBike == null || _bikes == null) {
			_cboBike.select(0);
		} else {
			boolean isBikeFound = false;
			for (final TourBike bike : _bikes) {
				if (personBike.getBikeId() == bike.getBikeId()) {
					_cboBike.select(bikeIndex + 1);
					isBikeFound = true;
					break;
				}
				bikeIndex++;
			}

			// when the bike id was not found, select "no selection" entry
			if (!isBikeFound) {
				_cboBike.select(0);
			}
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
	private void showSelectedPersonDetails() {

		final IStructuredSelection selection = (IStructuredSelection) _peopleViewer.getSelection();

		final Object item = selection.getFirstElement();
		boolean isEnabled = true;

		if (item instanceof TourPerson) {

			final TourPerson person = (TourPerson) item;
			_currentPerson = person;

			removeModifyListener();

			_txtFirstName.setText(person.getFirstName());
			_txtLastName.setText(person.getLastName());
			_txtHeight.setText(Float.toString(person.getHeight()));
			_txtWeight.setText(Float.toString(person.getWeight()));

			selectDevice(person);
			selectBike(person);

			_rawDataPathEditor.setStringValue(person.getRawDataPath());

			addModifyListener();

		} else {

			isEnabled = false;
			_currentPerson = null;

			_txtFirstName.setText(UI.EMPTY_STRING);
			_txtLastName.setText(UI.EMPTY_STRING);
			_txtHeight.setText(UI.EMPTY_STRING);
			_txtWeight.setText(UI.EMPTY_STRING);

			_cboDevice.select(0);
			_cboBike.select(0);

			_rawDataPathEditor.setStringValue(null);
		}

		_txtFirstName.setEnabled(isEnabled);
		_txtLastName.setEnabled(isEnabled);
		_txtHeight.setEnabled(isEnabled);
		_txtWeight.setEnabled(isEnabled);

		_cboBike.setEnabled(isEnabled);
		_cboDevice.setEnabled(isEnabled);
		_rawDataPathEditor.setEnabled(isEnabled, _personDetailContainer);
	}

	private boolean validatePerson() {

		boolean isValid = false;

		if (_currentPerson == null) {

			isValid = true;

		} else if (_txtFirstName.getText().trim().equals(UI.EMPTY_STRING)) {

			setErrorMessage(Messages.Pref_People_Error_first_name_is_required);

		} else if (!_rawDataPathEditor.getStringValue().trim().equals(UI.EMPTY_STRING) && !_rawDataPathEditor.isValid()) {

			setErrorMessage(Messages.Pref_People_Error_path_is_invalid);

		} else {
			try {
				Float.parseFloat(_txtHeight.getText());
				Float.parseFloat(_txtWeight.getText());
				isValid = true;
			} catch (final NumberFormatException e) {
				setErrorMessage(Messages.Pref_People_Error_invalid_number);
			}
		}

		enableButtons(isValid);

		if (isValid) {
			_peopleViewer.getTable().setEnabled(true);
			setErrorMessage(null);
			return true;
		} else {

			_peopleViewer.getTable().setEnabled(false);
			return false;
		}
	}

}
