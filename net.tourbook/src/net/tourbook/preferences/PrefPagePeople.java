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
import net.tourbook.data.TourBike;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePeople extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID						= "net.tourbook.preferences.PrefPagePeopleId";	//$NON-NLS-1$

	private static final String			STATE_SELECTED_PERSON	= "selectedPersonId";							//$NON-NLS-1$

	private final IPreferenceStore		_prefStore				= TourbookPlugin.getDefault()//
																		.getPreferenceStore();
	private final IDialogSettings		_state					= TourbookPlugin.getDefault()//
																		.getDialogSettingsSection(ID);

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
	/*
	 * UI controls
	 */
	private TableViewer					_peopleViewer;

	private Button						_btnAdd;
	private Group						_groupPerson;

	private Composite					_personFieldContainer;
	private Text						_txtFirstName;
	private Text						_txtLastName;
	private Combo						_cboDevice;
	private Combo						_cboBike;
	private DirectoryFieldEditor		_rawDataPathEditor;
	private Button						_btnUpdate;
	private Button						_btnCancel;
	private Spinner						_spinnerWeight;
	private PixelConverter				_pc;

	private Spinner						_spinnerHeight;

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return _people.toArray(new TourPerson[_people.size()]);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

//	public PrefPagePeople() {
//	}

	private void addModifyListener() {

//		_txtFirstName.addModifyListener(_textFirstNameModifyListener);
//		_txtLastName.addModifyListener(_textLastNameModifyListener);
//		_txtHeight.addModifyListener(_textHeightModifyListener);
//		_txtWeight.addModifyListener(_textWeightModifyListener);
//
//		_cboDevice.addModifyListener(_comboDeviceModifyListener);
//		_cboBike.addModifyListener(_comboBikeModifyListener);

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
					updateUIBikeList();

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
		_pc = new PixelConverter(parent);
		_spinnerWidth = _pc.convertWidthInCharsToPixels(_isOSX ? 10 : 5);

		createDeviceList();

		final Composite container = createUI(parent);

		// enableButtons();
		addPrefListener();

		_people = PersonManager.getTourPeople();
		_peopleViewer.setInput(this);

		// reselect previous person
		restoreState();

		return container;
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

	/**
	 * field: first name
	 * 
	 * @param parent
	 */
	private void createUI10054FieldFirstName(final Composite parent) {

//		_textFirstNameModifyListener = new ModifyListener() {
//			public void modifyText(final ModifyEvent e) {
//				if (_currentPerson != null) {
//					final String firstName = ((Text) (e.widget)).getText();
//					if (!firstName.equals(_currentPerson.getFirstName())) {
//						_isPersonModified = true;
//
//						_currentPerson.setFirstName(firstName);
//						_peopleViewer.update(_currentPerson, null);
//					}
//				}
//				validatePerson();
//			}
//		};
//
//		_textLastNameModifyListener = new ModifyListener() {
//			public void modifyText(final ModifyEvent e) {
//				if (_currentPerson != null) {
//					final String lastName = ((Text) (e.widget)).getText();
//					if (!lastName.equals(_currentPerson.getLastName())) {
//						_currentPerson.setLastName(lastName);
//						_isPersonModified = true;
//
//						_peopleViewer.update(_currentPerson, null);
//					}
//				}
//			}
//		};
//
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
//
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
// ???	table.setLayout(new TableLayout());

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		_peopleViewer = new TableViewer(table);
		_peopleViewer.setUseHashlookup(true);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		/*
		 * column: is modified
		 */
		tvc = new TableViewerColumn(_peopleViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(_isPersonModified ? "*" : UI.EMPTY_STRING);//$NON-NLS-1$
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
		tableLayout.setColumnData(tc, new ColumnWeightData(3, convertWidthInCharsToPixels(3)));

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
		tableLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(8 * 4), true));

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
				cell.setText(_nf2.format(weight));
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(8 * 4), true));

		_peopleViewer.setContentProvider(new ClientsContentProvider());

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

				updateUIPersonDetails();
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
					// enableButtons();
				}
			});

			// button: delete

			/*
			 * "Delete" button is disabled because the tours don't display the info that the person
			 * was removed
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
	}

	private void createUI30PersonDetails(final Composite parent) {

		/*
		 * group_ person data
		 */
		_groupPerson = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_groupPerson);
		_groupPerson.setText(Messages.Pref_People_Group_person);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_groupPerson);
//		_groupPerson.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI50PersonFields(_groupPerson);
			createUI60PersonDetailsAction(_groupPerson);
		}
	}

	private void createUI50PersonFields(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * field: first name
			 */
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Pref_People_Label_first_name);

			_txtFirstName = new Text(container, SWT.BORDER);

			/*
			 * field: last name
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Pref_People_Label_last_name);

			_txtLastName = new Text(container, SWT.BORDER);

			/*
			 * field: weight
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Pref_People_Label_weight);

			final Composite containerWeight = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerWeight);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerWeight);
			{
				// spinner: weight
				_spinnerWeight = new Spinner(containerWeight, SWT.BORDER);
				_spinnerWeight.setDigits(1);
				_spinnerWeight.setMinimum(0);
				_spinnerWeight.setMaximum(3000); // 300.0 kg
				_spinnerWeight.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangePerson();
					}
				});
				_spinnerWeight.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangePerson();
					}
				});

				// label: unit
				label = new Label(containerWeight, SWT.NONE);
				label.setText(UI.UNIT_WEIGHT_KG);
			}

			// 3rd column filler
			new Label(container, SWT.NONE);

			/*
			 * field: height
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Pref_People_Label_height);

			final Composite containerHeight = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerHeight);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerHeight);
			{
				// spinner: height
				_spinnerHeight = new Spinner(containerHeight, SWT.BORDER);
				_spinnerHeight.setDigits(2);
				_spinnerHeight.setMinimum(0);
				_spinnerHeight.setMaximum(300); // 3.00 m
				_spinnerHeight.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangePerson();
					}
				});
				_spinnerHeight.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangePerson();
					}
				});

				// label: unit
				label = new Label(containerHeight, SWT.NONE);
				label.setText(UI.UNIT_METER);
			}

			// filler
			new Label(container, SWT.NONE);

			/*
			 * field: path to save raw tour data
			 */
			_rawDataPathEditor = new DirectoryFieldEditor(
					ITourbookPreferences.DUMMY_FIELD,
					Messages.Pref_People_Label_rawdata_path,
					container);
			_rawDataPathEditor.setEmptyStringAllowed(true);

			createUI52FieldDevice(container);
			createUI54FieldBike(container);
		}

		// layout must be set, AFTER the fields are created
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFirstName);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtLastName);
		GridDataFactory.fillDefaults() //
				.grab(false, false)
				.align(SWT.BEGINNING, SWT.FILL)
				.hint(_spinnerWidth, SWT.DEFAULT)
				.applyTo(_spinnerHeight);
		GridDataFactory.fillDefaults()//
				.grab(false, false)
				.align(SWT.BEGINNING, SWT.FILL)
				.hint(_spinnerWidth, SWT.DEFAULT)
				.applyTo(_spinnerWeight);

		container.layout(true, true);
		_personFieldContainer = container;
	}

	/**
	 * field: device
	 * 
	 * @param parent
	 */
	private void createUI52FieldDevice(final Composite parent) {

		final Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.Pref_People_Label_device);

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
//						_currentPerson.setDeviceReaderId(device == null ? null : device.deviceId);
//
//						_isPersonModified = true;
//
//						_peopleViewer.update(_currentPerson, null);
//					}
//				}
//				validatePerson();
//			}
//		};

		// spacer
		new Label(parent, SWT.NONE);

		// add all devices to the combobox
		for (final ExternalDevice device : _deviceList) {
			if (device == null) {
				_cboDevice.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
			} else {
				_cboDevice.add(device.visibleName);
			}
		}
	}

	private void createUI54FieldBike(final Composite parent) {

		/*
		 * field: bike
		 */
		// label
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_bike);

		// combo
		_cboBike = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().applyTo(_cboBike);
		_cboBike.setVisibleItemCount(10);

//		_comboBikeModifyListener = new ModifyListener() {
//			public void modifyText(final ModifyEvent e) {
//				if (_currentPerson != null) {
//					int selectedIndex = _cboBike.getSelectionIndex();
//					if (selectedIndex != -1) {
//						final TourBike personTourBike = _currentPerson.getTourBike();
//						if (selectedIndex == 0) {
//							if (personTourBike == null) {
//								// person had no bike this was not changed
//								return;
//							} else {
//								// person had before a bike which is now
//								// removed
//								_currentPerson.setTourBike(null);
//								_isPersonModified = true;
//							}
//							return;
//						}
//
//						// adjust to correct index in the bike array
//						selectedIndex--;
//
//						final TourBike selectedBike = _bikes[selectedIndex];
//
//						if (personTourBike == null || (personTourBike.getBikeId() != selectedBike.getBikeId())) {
//
//							_currentPerson.setTourBike(selectedBike);
//							_isPersonModified = true;
//						}
//
//						if (_isPersonModified) {
//							_peopleViewer.update(_currentPerson, null);
//						}
//					}
//				}
//			}
//		};

		// filler
		new Label(parent, SWT.NONE);

		updateUIBikeList();
	}

	private void createUI60PersonDetailsAction(final Composite parent) {

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

	@Override
	public void dispose() {

		if (_prefChangeListener != null) {
			_prefStore.removePropertyChangeListener(_prefChangeListener);
		}

		super.dispose();
	}

	private void enableActions(final boolean isPersonValid) {

		_btnAdd.setEnabled(_currentPerson == null || (_currentPerson != null && isPersonValid));
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
		noDefaultAndApplyButton();
	}

	@Override
	public boolean okToLeave() {

		saveState();

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

//	public boolean okToLeave() {
//		if (validatePerson() == false) {
//			return false;
//		}
//		savePerson();
//		firePersonListModifyEvent();
//
//		return super.okToLeave();
//	}

	private void onCancelPerson() {
		// TODO Auto-generated method stub

	}

	private void onChangePerson() {
		// TODO Auto-generated method stub

	}

	private void onUpdatePerson() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performCancel() {
		saveState();
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

//		_txtFirstName.removeModifyListener(_textFirstNameModifyListener);
//		_txtLastName.removeModifyListener(_textLastNameModifyListener);
//		_txtHeight.removeModifyListener(_textHeightModifyListener);
//		_txtWeight.removeModifyListener(_textWeightModifyListener);
//
//		_cboDevice.addModifyListener(_comboDeviceModifyListener);
//		_cboBike.addModifyListener(_comboBikeModifyListener);

		_rawDataPathEditor.setPropertyChangeListener(null);
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

	private void restoreState() {

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

	private void saveState() {

		final Object firstElement = ((IStructuredSelection) _peopleViewer.getSelection()).getFirstElement();
		if (firstElement instanceof TourPerson) {
			_state.put(STATE_SELECTED_PERSON, ((TourPerson) firstElement).getPersonId());
		}

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

	private void updateUIBikeList() {

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

	/**
	 * update person data fields from the selected person in the viewer
	 */
	private void updateUIPersonDetails() {

		final IStructuredSelection selection = (IStructuredSelection) _peopleViewer.getSelection();

		final Object item = selection.getFirstElement();
		boolean isEnabled = true;

		if (item instanceof TourPerson) {

			final TourPerson person = (TourPerson) item;
			_currentPerson = person;

			removeModifyListener();

			_txtFirstName.setText(person.getFirstName());
			_txtLastName.setText(person.getLastName());
			_spinnerWeight.setSelection((int) (person.getWeight() * 10));
			_spinnerHeight.setSelection((int) (person.getHeight() * 100));

			selectDevice(person);
			selectBike(person);

			_rawDataPathEditor.setStringValue(person.getRawDataPath());

			addModifyListener();

		} else {

			isEnabled = false;
			_currentPerson = null;

			_txtFirstName.setText(UI.EMPTY_STRING);
			_txtLastName.setText(UI.EMPTY_STRING);
			_spinnerHeight.setSelection(0);
			_spinnerWeight.setSelection(0);

			_cboDevice.select(0);
			_cboBike.select(0);

			_rawDataPathEditor.setStringValue(null);
		}

		_txtFirstName.setEnabled(isEnabled);
		_txtLastName.setEnabled(isEnabled);
		_spinnerHeight.setEnabled(isEnabled);
		_spinnerWeight.setEnabled(isEnabled);

		_cboBike.setEnabled(isEnabled);
		_cboDevice.setEnabled(isEnabled);
		_rawDataPathEditor.setEnabled(isEnabled, _personFieldContainer);
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
			isValid = true;
		}

		enableActions(isValid);

		if (isValid) {
			_peopleViewer.getTable().setEnabled(true);
			setErrorMessage(null);
			return true;
		} else {

			_peopleViewer.getTable().setEnabled(false);
			return false;
		}
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

}
