/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.HrZoneContext;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.training.DialogHRZones;
import net.tourbook.training.TrainingManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePeople extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID							= "net.tourbook.preferences.PrefPagePeopleId";	//$NON-NLS-1$

	private static final String			STATE_SELECTED_PERSON		= "selectedPersonId";							//$NON-NLS-1$
	private static final String			STATE_SELECTED_TAB_FOLDER	= "selectedTabFolder";							//$NON-NLS-1$

	public static final int				HEART_BEAT_MIN				= 10;
	public static final int				HEART_BEAT_MAX				= 300;

	/**
	 * Id to indicate that the hr zones should be displayed for the active person when the pref
	 * dialog is opened
	 */
	public static final String			PREF_DATA_SELECT_HR_ZONES	= "SelectHrZones";								//$NON-NLS-1$

	private final IPreferenceStore		_prefStore					= TourbookPlugin.getDefault()//
																			.getPreferenceStore();
	private final IDialogSettings		_state						= TourbookPlugin.getDefault()//
																			.getDialogSettingsSection(ID);

	// REMOVED BIKES 30.4.2011

	private ArrayList<TourPerson>		_people;

	/**
	 * this device list has all the devices which are visible in the device combobox
	 */
	private ArrayList<ExternalDevice>	_deviceList;

	private final NumberFormat			_nf1						= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf2						= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf2.setMinimumFractionDigits(2);
		_nf2.setMaximumFractionDigits(2);
	}

	private final boolean				_isOSX						= net.tourbook.common.UI.IS_OSX;
	private final boolean				_isLinux					= net.tourbook.common.UI.IS_LINUX;

	private SelectionListener			_defaultSelectionListener;
	private ModifyListener				_defaultModifyListener;
	private MouseListener				_hrZoneMouseListener;
	private IPropertyChangeListener		_prefChangeListener;

	private boolean						_isFireModifyEvent			= false;
	private boolean						_isPersonModified			= false;
	private boolean						_isUpdateUI					= false;

	private HashMap<Long, TourPerson>	_peopleWithModifiedHrZones	= new HashMap<Long, TourPerson>();
	private boolean						_isHrZoneModified			= false;

	private TourPerson					_selectedPerson;
	private TourPerson					_newPerson;
	private Set<TourPersonHRZone>		_backupSelectedPersonHrZones;

	private ZonedDateTime				_today						= TimeTools.now();

	private Font						_fontItalic;
	private Color[]						_hrZoneColors;

	/**
	 * Is <code>true</code> when a tour in the tour editor is modified.
	 */
	private boolean						_isNoUI						= false;

	/*
	 * UI controls
	 */
	private Composite					_prefPageContainer;
	private TableViewer					_peopleViewer;

	private Button						_btnAddPerson;
	private Button						_btnSavePerson;
	private Button						_btnCancel;

	private TabFolder					_tabFolderPerson;
	private Text						_txtFirstName;
	private Text						_txtLastName;
	private Combo						_cboSportComputer;
	private Spinner						_spinnerWeight;
	private Spinner						_spinnerHeight;
	private Spinner						_spinnerRestingHR;
	private Spinner						_spinnerMaxHR;
	private Button						_rdoGenderMale;
	private Button						_rdoGenderFemale;

	private ScrolledComposite			_hrZoneScrolledContainer;
	private Button						_btnModifyHrZones;
	private Button						_btnComputeHrZonesForAllTours;
	private Combo						_cboTemplate;
	private Combo						_cboHrMaxFormula;
	private DateTime					_dtBirthday;
	private Label						_lblAgePerson;
	private Label						_lblAgeHr;

	private Text						_txtRawDataPath;
	private DirectoryFieldEditor		_rawDataPathEditor;

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			return _people.toArray(new TourPerson[_people.size()]);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.HR_ZONES_ARE_MODIFIED)) {

					onEditHrZonesIsOK(getCurrentPerson());
					performOK10();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	public void applyData(final Object data) {

		// this is called after the UI is created

		if (_isNoUI) {
			return;
		}

		if (data instanceof Boolean) {

			final Boolean isCreatePerson = (Boolean) data;
			if (isCreatePerson && _people.size() == 0) {

				// this is a request, to create a new person

				final TourPerson newPerson = createDefaultPerson();

				newPerson.persist();

				// update model
				_people.add(newPerson);

				// update state
				_isFireModifyEvent = true;
				_isPersonModified = false;

				// update ui viewer and person ui
				_peopleViewer.add(newPerson);
				_peopleViewer.setSelection(new StructuredSelection(newPerson));

				enableActions();

				// for the first person, disable Add.. button and people list that the user is not confused
				_btnAddPerson.setEnabled(false);
				_peopleViewer.getTable().setEnabled(false);

				// select first name
				_tabFolderPerson.setSelection(0);
				_txtFirstName.selectAll();
				_txtFirstName.setFocus();
			}

		} else if (data instanceof PrefPagePeopleData) {

			final PrefPagePeopleData prefPageData = (PrefPagePeopleData) data;

			if (prefPageData.prefDataSelectHrZones.equals(PREF_DATA_SELECT_HR_ZONES)) {

				// select hr zones for the given person

				if (prefPageData.person != null) {

					_peopleViewer.setSelection(new StructuredSelection(prefPageData.person));

					final Table table = _peopleViewer.getTable();

					// set focus to selected person
					table.setSelection(table.getSelectionIndex());
				}

				if (_tabFolderPerson != null) {
					_tabFolderPerson.setSelection(1);
				} else {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							_tabFolderPerson.setSelection(1);
						}
					});
				}
			}
		}
	}

	private Set<TourPersonHRZone> cloneHrZones(final ArrayList<TourPersonHRZone> hrZones) {

		final HashSet<TourPersonHRZone> hrZonesClone = new HashSet<TourPersonHRZone>();

		for (final TourPersonHRZone tourPersonHRZone : hrZones) {
			hrZonesClone.add(tourPersonHRZone.clone());
		}

		return hrZonesClone;
	}

	/**
	 * @param isCheckPeople
	 * @return Returns <code>true</code> when all tours has been updated and the update process was
	 *         not canceled.
	 */
	private boolean computeHrZonesForAllTours(final boolean isCheckPeople) {

		setErrorMessage(null);

		final int[] tourCounter = { 0 };
		final int[] tourCounterWithHrZones = { 0 };

		final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

			@Override
			public boolean computeTourValues(final TourData originalTourData) {

				tourCounter[0]++;

				if (isCheckPeople) {

					// check if hr zone was modified for the person which owns the tour
					final long personId = originalTourData.getTourPerson().getPersonId();
					if (_peopleWithModifiedHrZones.containsKey(personId) == false) {
						return false;
					}
				}

				/*
				 * algorithmus for avg pulse is changed in version 11.7 (break time is now ignored)
				 */
				originalTourData.computeAvg_Pulse();

				final int[] allHrZones = originalTourData.getHrZones();
				if (allHrZones == null) {
					return false;
				}

				// check if hr zones are computed
				for (final int hrZone : allHrZones) {
					if (hrZone != -1) {
						// hr zone is set
						tourCounterWithHrZones[0]++;
						return true;
					}
				}

				return false;
			}

			@Override
			public String getResultText() {

				return NLS.bind(Messages.Compute_HrZones_Job_ComputeAllTours_Result, //
						new Object[] { tourCounterWithHrZones[0] });
			}

			@Override
			public String getSubTaskText(final TourData savedTourData) {
				return NLS.bind(Messages.Compute_HrZones_Job_ComputeAllTours_SubTask, //
						new Object[] { tourCounterWithHrZones[0], tourCounter[0] });
			}
		};

		final boolean isCanceled = TourDatabase.computeValuesForAllTours(computeTourValueConfig, null);

		boolean returnValue = true;

		if (isCheckPeople && isCanceled) {

			setErrorMessage(Messages.Pref_People_Error_ComputeHrZonesForAllTours);

			MessageDialog.openInformation(
					getShell(),
					Messages.Compute_HrZones_Dialog_ComputeAllTours_Title,
					Messages.Pref_People_Dialog_ComputeHrZonesForAllToursIsCanceled_Message);

			returnValue = false;
		}

		TourManager.getInstance().removeAllToursFromCache();
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);

		_isFireModifyEvent = true;
		fireModifyEvent();

		_peopleWithModifiedHrZones.clear();

		return returnValue;
	}

	@Override
	protected Control createContents(final Composite parent) {

		// check: if a tour is modified in the tour editor
		if (TourManager.isTourEditorModified()) {

			_isNoUI = true;

			return createUI_01_NoUI(parent);
		}

		initUI(parent);

		final Composite container = createUI(parent);

		updateUIDeviceList();

		// update people viewer
		_people = PersonManager.getTourPeople();
		_peopleViewer.setInput(new Object());

		// reselect previous person and tabfolder
		restoreState();

		enableActions();
		addPrefListener();

		return container;
	}

	private TourPerson createDefaultPerson() {

		final TourPerson newPerson = new TourPerson(Messages.App_Default_PersonFirstName, UI.EMPTY_STRING);

		newPerson.setHeight(1.77f);
		newPerson.setWeight(77.7f);
		newPerson.setBirthDay(TourPerson.DEFAULT_BIRTHDAY.toInstant().toEpochMilli());

		newPerson.setGender(0);
		newPerson.setRestPulse(TourPerson.DEFAULT_REST_PULSE);

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

		_prefPageContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, true)
				.applyTo(_prefPageContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_prefPageContainer);
		{

			final Label label = new Label(_prefPageContainer, SWT.WRAP);
			label.setText(Messages.Pref_People_Title);

			final Composite innerContainer = new Composite(_prefPageContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(innerContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(innerContainer);
			{
				createUI_10_People_Viewer(innerContainer);
				createUI_20_People_Actions(innerContainer);

				createUI_30_Person_Folder(innerContainer);
			}

			// placeholder
			new Label(_prefPageContainer, SWT.NONE);
		}

		return _prefPageContainer;
	}

	private Control createUI_01_NoUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			final Label label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).hint(350, SWT.DEFAULT).applyTo(label);
			label.setText(Messages.Pref_App_Label_TourEditorIsModified);
		}

		return container;
	}

	private void createUI_10_People_Viewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults() //
				.grab(true, true)
				.hint(convertWidthInCharsToPixels(30), convertHeightInCharsToPixels(5))
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
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectPerson();
			}
		});

		_peopleViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				_tabFolderPerson.setSelection(0);
				_txtFirstName.setFocus();
				_txtFirstName.selectAll();
			}
		});

	}

	private void createUI_20_People_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(false, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: add
			 */
			_btnAddPerson = new Button(container, SWT.NONE);
			_btnAddPerson.setText(Messages.Pref_People_Action_add_person);
			setButtonLayoutData(_btnAddPerson);
			_btnAddPerson.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAddPerson();
				}
			});

			/*
			 * button: update
			 */
			_btnSavePerson = new Button(container, SWT.NONE);
			_btnSavePerson.setText(Messages.App_Action_Save);
			_btnSavePerson.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSavePerson();
				}
			});
			setButtonLayoutData(_btnSavePerson);
			final GridData gd = (GridData) _btnSavePerson.getLayoutData();
			gd.verticalAlignment = SWT.BOTTOM;
			gd.grabExcessVerticalSpace = true;

			/*
			 * button: cancel
			 */
			_btnCancel = new Button(container, SWT.NONE);
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

	private void createUI_30_Person_Folder(final Composite parent) {

		_tabFolderPerson = new TabFolder(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(_tabFolderPerson);
		{
			// tab: person
			final TabItem tabItemDetails = new TabItem(_tabFolderPerson, SWT.NONE);
			tabItemDetails.setText(Messages.Pref_People_Tab_Person);
			tabItemDetails.setControl(createUI_50_Tab_Person(_tabFolderPerson));

			// tab: hr zone
			final TabItem tabItemHRZone = new TabItem(_tabFolderPerson, SWT.NONE);
			tabItemHRZone.setText(Messages.Pref_People_Tab_HRZone);
			tabItemHRZone.setControl(createUI_60_Tab_HRZone(_tabFolderPerson));

			// tab: data transfer
			final TabItem tabItemDataTransfer = new TabItem(_tabFolderPerson, SWT.NONE);
			tabItemDataTransfer.setText(Messages.Pref_People_Tab_DataTransfer);
			tabItemDataTransfer.setControl(createUI_90_Tab_DataTransfer(_tabFolderPerson));
		}
	}

	private Control createUI_50_Tab_Person(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 7, 0).applyTo(container);
		{
			createUI_51_Field_FirstName(container);
			createUI_52_Field_LastName(container);

			createUI_53_Field_Birthday(container);
			createUI_54_Field_Gender(container);
			createUI_55_Field_Weight(container);
			createUI_56_Field_Height(container);
		}
		container.layout(true, true);

		return container;
	}

	/**
	 * field: first name
	 */
	private void createUI_51_Field_FirstName(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_first_name);

		_txtFirstName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(_txtFirstName);
		_txtFirstName.addModifyListener(_defaultModifyListener);
	}

	/**
	 * field: last name
	 */
	private void createUI_52_Field_LastName(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_last_name);

		_txtLastName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(_txtLastName);
		_txtLastName.addModifyListener(_defaultModifyListener);
	}

	/**
	 * field: birthday
	 */
	private void createUI_53_Field_Birthday(final Composite parent) {

		/*
		 * date-time: birthday
		 */
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_Birthday);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{

			_dtBirthday = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_dtBirthday);
			_dtBirthday.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					updateUIOnModifiedHrZones();
				}
			});
			_dtBirthday.addKeyListener(new KeyListener() {

				@Override
				public void keyPressed(final KeyEvent e) {}

				@Override
				public void keyReleased(final KeyEvent e) {
					/*
					 * key listener is necessary because the selection listener is not fired when
					 * the values are modified with mouse wheel up/down
					 */
//					onModifyHrZones();
				}
			});

			/*
			 * label: age
			 */
			_lblAgePerson = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblAgePerson);
		}
	}

	/**
	 * field: gender
	 */
	private void createUI_54_Field_Gender(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_Gender);

		// radio
		final Composite containerGender = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.applyTo(containerGender);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerGender);
		{
			_rdoGenderMale = new Button(containerGender, SWT.RADIO);
			_rdoGenderMale.setText(Messages.Pref_People_Label_GenderMale);
			_rdoGenderMale.addSelectionListener(_defaultSelectionListener);

			_rdoGenderFemale = new Button(containerGender, SWT.RADIO);
			_rdoGenderFemale.setText(Messages.Pref_People_Label_GenderFemale);
			_rdoGenderFemale.addSelectionListener(_defaultSelectionListener);
		}
	}

	/**
	 * field: weight
	 */
	private void createUI_55_Field_Weight(final Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_weight);

		final Composite containerWeight = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.applyTo(containerWeight);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerWeight);
		{
			// spinner: weight
			_spinnerWeight = new Spinner(containerWeight, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
//					.hint(_spinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerWeight);
			_spinnerWeight.setDigits(1);
			_spinnerWeight.setMinimum(0);
			_spinnerWeight.setMaximum(3000); // 300.0 kg
			_spinnerWeight.addSelectionListener(_defaultSelectionListener);
			_spinnerWeight.addMouseWheelListener(new MouseWheelListener() {
				@Override
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
	private void createUI_56_Field_Height(final Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_height);

		final Composite containerHeight = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.applyTo(containerHeight);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerHeight);
		{
			// spinner: height
			_spinnerHeight = new Spinner(containerHeight, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
//					.hint(_spinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerHeight);
			_spinnerHeight.setDigits(2);
			_spinnerHeight.setMinimum(0);
			_spinnerHeight.setMaximum(300); // 3.00 m
			_spinnerHeight.addSelectionListener(_defaultSelectionListener);
			_spinnerHeight.addMouseWheelListener(new MouseWheelListener() {
				@Override
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

	private Control createUI_60_Tab_HRZone(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults()//
				.numColumns(1)
				.extendedMargins(0, 0, 7, 0)
				.applyTo(container);
		{
			final Composite containerHr = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerHr);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(containerHr);
			{
//				createUI53FieldBirthday(containerHr);
				createUI_62_RestingHR(containerHr);
				createUI_64_MaxHR(containerHr);
			}

			final Group group = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
			GridLayoutFactory.swtDefaults().extendedMargins(0, 0, -5, 0).applyTo(group);
//			group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_70_HrZone(group);
				createUI_72_HrZone_Actions(group);
			}
		}

		return container;
	}

	/**
	 * field: resting hr
	 */
	private void createUI_62_RestingHR(final Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_RestingHR);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// spinner: weight
			_spinnerRestingHR = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
//					.hint(_spinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerRestingHR);
			_spinnerRestingHR.setMinimum(10);
			_spinnerRestingHR.setMaximum(200);
			_spinnerRestingHR.addSelectionListener(_defaultSelectionListener);
			_spinnerRestingHR.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onModifyPerson();
				}
			});

			// label: unit
			label = new Label(container, SWT.NONE);
			label.setText(net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit);
		}

		final Composite containerAge = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.FILL)
				.applyTo(containerAge);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerAge);
//		containerAge.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * label: age
			 */
			label = new Label(containerAge, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.END, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.Pref_People_Label_Age);

			/*
			 * label: age years
			 */
			_lblAgeHr = new Label(containerAge, SWT.NONE);
			GridDataFactory.fillDefaults()//
//					.hint(convertHeightInCharsToPixels(4), SWT.DEFAULT)
					.grab(true, true)
					.align(SWT.END, SWT.CENTER)
					.applyTo(_lblAgeHr);
		}
	}

	/**
	 * field: max hr
	 */
	private void createUI_64_MaxHR(final Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_MaxHR);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * spinner: hr max
			 */
			_spinnerMaxHR = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
//					.hint(_spinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerMaxHR);
			_spinnerMaxHR.setMinimum(HEART_BEAT_MIN);
			_spinnerMaxHR.setMaximum(HEART_BEAT_MAX);
			_spinnerMaxHR.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					updateUIOnModifiedHrZones();
				}
			});
			_spinnerMaxHR.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					updateUIOnModifiedHrZones();
				}
			});

			/*
			 * label: unit
			 */
			label = new Label(container, SWT.NONE);
			label.setText(net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit);

			/*
			 * combo: formula to compute hr max
			 */
			_cboHrMaxFormula = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
			GridDataFactory.fillDefaults() //
					.grab(true, false)
					.indent(5, 0)
					.hint(50, SWT.DEFAULT)
					.applyTo(_cboHrMaxFormula);
			_cboHrMaxFormula.setVisibleItemCount(20);
			_cboHrMaxFormula.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					updateUIOnModifiedHrZones();
				}
			});

			// fill combobox
			final String[] hrMaxFormulaNames = TrainingManager.HRMaxFormulaNames;
			for (final String formulaName : hrMaxFormulaNames) {
				_cboHrMaxFormula.add(formulaName);
			}
		}
	}

	private void createUI_70_HrZone(final Composite parent) {

		final Composite hrZoneContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(hrZoneContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(hrZoneContainer);
//		hrZoneContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * container for hr zone fields
			 */

			// scrolled container
			_hrZoneScrolledContainer = new ScrolledComposite(hrZoneContainer, //
					SWT.V_SCROLL //
							| SWT.H_SCROLL
//					| SWT.BORDER
			);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.hint(SWT.DEFAULT, convertHeightInCharsToPixels(10))
					.applyTo(_hrZoneScrolledContainer);
//			_hrZoneScrolledContainer.setAlwaysShowScrollBars(true);
			_hrZoneScrolledContainer.setExpandVertical(true);
			_hrZoneScrolledContainer.setExpandHorizontal(true);

			_hrZoneScrolledContainer.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					final Control content = _hrZoneScrolledContainer.getContent();
					if (content != null) {
//						_hrZoneScrolledContainer.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					}
				}
			});
		}
	}

	private void createUI_72_HrZone_Actions(final Composite parent) {

		// compute size for the first combo item
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.HR_Zone_Template_Select);
		final Point comboSize = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		label.dispose();

		final int comboWidth = (int) (_isOSX || _isLinux ? comboSize.x * 1.3 : comboSize.x);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.indent(0, 20)
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: compute speed values for all tours
			 */
			_btnComputeHrZonesForAllTours = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.applyTo(_btnComputeHrZonesForAllTours);
			_btnComputeHrZonesForAllTours.setText(Messages.Pref_People_Button_HrZones_ComputeAllTours);
			_btnComputeHrZonesForAllTours.setToolTipText(Messages.Pref_People_Button_HrZones_ComputeAllTours_Tooltip);
			_btnComputeHrZonesForAllTours.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					if (MessageDialog.openConfirm(
							Display.getCurrent().getActiveShell(),
							Messages.Compute_HrZones_Dialog_ComputeAllTours_Title,
							Messages.Compute_HrZones_Dialog_ComputeAllTours_Title_Message)) {

						computeHrZonesForAllTours(false);
					}
				}
			});

			/*
			 * button: edit hr zones
			 */
			_btnModifyHrZones = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.END, SWT.FILL)
					.applyTo(_btnModifyHrZones);
			_btnModifyHrZones.setText(Messages.Dialog_HRZone_Button_EditHrZones);
			_btnModifyHrZones.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onEditHrZones();
				}
			});

			/*
			 * combo: formula to compute hr max
			 */
			_cboTemplate = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
			GridDataFactory.fillDefaults() //
					.hint(comboWidth, SWT.DEFAULT)
					.applyTo(_cboTemplate);
			_cboTemplate.setToolTipText(Messages.Pref_People_Label_HrZoneTemplate_Tooltip);
			_cboTemplate.setVisibleItemCount(20);
			_cboTemplate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onCreateHrZonesFromTemplate();
				}
			});

			// fill combobox
			for (final String hrZoneTemplate : TrainingManager.HR_ZONE_TEMPLATES) {
				_cboTemplate.add(hrZoneTemplate);
			}
			_cboTemplate.select(TrainingManager.HR_ZONE_TEMPLATE_00);
		}
	}

	private void createUI_80_HrZone_InnerContainer(	final int hrMaxFormulaKey,
													final int hrMaxPulse,
													final ZonedDateTime birthDay) {

		// get current scroll position
		final Point scrollBackup = _hrZoneScrolledContainer.getOrigin();

		// dispose previous ui
		final Control content = _hrZoneScrolledContainer.getContent();
		if (content != null) {
			content.dispose();
		}

		// hr zone container
		final Composite innerContainer = new Composite(_hrZoneScrolledContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.numColumns(10)
//				.extendedMargins(0, 5, 0, 0)
				.spacing(2, 2)
				.applyTo(innerContainer);
		innerContainer.addMouseListener(_hrZoneMouseListener);
//		innerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			final ArrayList<TourPersonHRZone> hrZones = getCurrentPerson().getHrZonesSorted();

			if (hrZones.size() == 0) {
				// hr zones are not available, show info
				createUI_81_HrZone_Info(innerContainer);
			} else {
				createUI_82_HrZone_Header(innerContainer);
				createUI_84_HrZone_Fields(innerContainer, hrMaxFormulaKey, hrMaxPulse, birthDay);
			}
		}

		_hrZoneScrolledContainer.setContent(innerContainer);

		// force the v-scrollbar to be displayed
		_hrZoneScrolledContainer.setMinSize(innerContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		// set scroll position to previous position
		if (scrollBackup != null) {
			_hrZoneScrolledContainer.setOrigin(scrollBackup);
		}
	}

	private void createUI_81_HrZone_Info(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Pref_People_Label_HrZoneInfo);
	}

	private void createUI_82_HrZone_Header(final Composite parent) {

		/*
		 * label: color
		 */
		Label label = new Label(parent, SWT.NONE);
		label.addMouseListener(_hrZoneMouseListener);

		/*
		 * label: zone
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false)
//				.hint(250, SWT.DEFAULT)
				.align(SWT.FILL, SWT.BOTTOM)
				.indent(5, 0)
				.applyTo(label);
		label.setFont(_fontItalic);
		label.setText(Messages.Dialog_HRZone_Label_Header_Zone);
		label.addMouseListener(_hrZoneMouseListener);

		/*
		 * label: min pulse
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BOTTOM).span(8, 1).applyTo(label);
		label.setFont(_fontItalic);
		label.setText(Messages.Dialog_HRZone_Label_Header_Pulse);
		label.addMouseListener(_hrZoneMouseListener);
//
//		/*
//		 * label: min pulse
//		 */
//		label = new Label(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BOTTOM).span(4, 1).applyTo(label);
//		label.setFont(_fontItalic);
//		label.setText(Messages.Dialog_HRZone_Label_Header_Pulse);
//		label.addMouseListener(_hrZoneMouseListener);
	}

	private void createUI_84_HrZone_Fields(	final Composite parent,
											final int hrMaxFormulaKey,
											final int hrMaxPulse,
											final ZonedDateTime birthDay) {

		final TourPerson currentPerson = getCurrentPerson();
		if (currentPerson == null) {
			return;
		}

		// get sorted hr zones
		final ArrayList<TourPersonHRZone> hrZones = new ArrayList<TourPersonHRZone>(currentPerson.getHrZonesSorted());
		final int hrZoneSize = hrZones.size();
		Collections.sort(hrZones);

		final HrZoneContext hrZoneMinMaxBpm = currentPerson.getHrZoneContext(
				hrMaxFormulaKey,
				hrMaxPulse,
				birthDay,
				_today);

		// init hr zone colors
		final Display display = parent.getDisplay();
		disposeHrZoneColors();
		_hrZoneColors = new Color[hrZoneSize];

		for (int zoneIndex = 0; zoneIndex < hrZoneSize; zoneIndex++) {

			final TourPersonHRZone hrZone = hrZones.get(zoneIndex);

			final int zoneMinValue = hrZone.getZoneMinValue();
			final int zoneMaxValue = hrZone.getZoneMaxValue();

			final Color hrZoneColor = _hrZoneColors[zoneIndex] = new Color(display, hrZone.getColor());

			/*
			 * label: color
			 */
			Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().hint(16, 16).applyTo(label);
			label.setText(UI.EMPTY_STRING);
			label.setBackground(hrZoneColor);
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: hr zone name
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.indent(5, 0)
					.applyTo(label);
			label.setText(hrZone.getNameLong());
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: min pulse %
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).indent(10, 0).applyTo(label);
			label.setText(zoneMinValue == Integer.MIN_VALUE ? UI.EMPTY_STRING : Integer.toString(zoneMinValue));
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: ...
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(label);
//			label.setText(zoneMaxValue == Integer.MAX_VALUE ? UI.EMPTY_STRING : UI.SYMBOL_DASH);
			label.setText(UI.SYMBOL_DASH);
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: max pulse %
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.FILL)
//					.indent(10, 0)
					.applyTo(label);
			label.setText(zoneMaxValue == Integer.MAX_VALUE //
					? Messages.App_Label_max
					: Integer.toString(zoneMaxValue));
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: %
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.SYMBOL_PERCENTAGE);
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: min pulse bpm
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).indent(15, 0).applyTo(label);
			label.setText(zoneMinValue == Integer.MIN_VALUE //
					? UI.EMPTY_STRING
					: Integer.toString((int) hrZoneMinMaxBpm.zoneMinBpm[zoneIndex]));
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: ...
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(label);
			label.setText(UI.SYMBOL_DASH);
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: max pulse bpm
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.FILL)
//					.indent(10, 0)
					.applyTo(label);
			label.setText(zoneMaxValue == Integer.MAX_VALUE //
					? Messages.App_Label_max
					: Integer.toString((int) hrZoneMinMaxBpm.zoneMaxBpm[zoneIndex]));
			label.addMouseListener(_hrZoneMouseListener);

			/*
			 * label: bpm
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit);
			label.addMouseListener(_hrZoneMouseListener);
		}
	}

	private Control createUI_90_Tab_DataTransfer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		{
			/*
			 * label: info
			 */
			final Label label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
//					.indent(0, 15)
					.hint(net.tourbook.common.UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.applyTo(label);
			label.setText(Messages.Pref_People_Label_DataTransfer);

			createUI_92_Field_SportComputer(container);
			createUI_94_Field_RawDataPath(container);
		}

		// set layout after the fields are created
		GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 7, 0).applyTo(container);

		/*
		 * set width for the text control that the pref dialog is not as wide as the full path
		 */
		final Text rawPathControl = _rawDataPathEditor.getTextControl(container);
		final GridData gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = 200;

		return container;
	}

	/**
	 * field: sport computer
	 */
	private void createUI_92_Field_SportComputer(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(0, 15).align(SWT.FILL, SWT.CENTER).applyTo(label);
		label.setText(Messages.Pref_People_Label_device);

		// combo
		_cboSportComputer = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridDataFactory.swtDefaults().indent(0, 15).applyTo(_cboSportComputer);
		_cboSportComputer.setVisibleItemCount(20);
		_cboSportComputer.addSelectionListener(_defaultSelectionListener);

		// spacer
		new Label(parent, SWT.NONE);
	}

	/**
	 * field: path to save raw tour data
	 */
	private void createUI_94_Field_RawDataPath(final Composite parent) {

		/*
		 * editor: raw data path
		 */
		_rawDataPathEditor = new DirectoryFieldEditor(
				ITourbookPreferences.DUMMY_FIELD,
				Messages.Pref_People_Label_DefaultDataTransferFilePath,
				parent);
		_rawDataPathEditor.setEmptyStringAllowed(true);
		_rawDataPathEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

		final Label lblPath = _rawDataPathEditor.getLabelControl(parent);
		lblPath.setToolTipText(Messages.Pref_People_Label_DefaultDataTransferFilePath_Tooltip);

		_txtRawDataPath = _rawDataPathEditor.getTextControl(parent);
		_txtRawDataPath.addModifyListener(_defaultModifyListener);
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
					cell.setText(TimeTools.getZonedDateTime(birthDayValue).format(TimeTools.Formatter_Date_S));
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

		disposeHrZoneColors();

		if (_prefChangeListener != null) {
			_prefStore.removePropertyChangeListener(_prefChangeListener);
		}

		if (_isNoUI) {
			super.dispose();
			return;
		}

		super.dispose();
	}

	private void disposeHrZoneColors() {

		if (_hrZoneColors == null) {
			return;
		}

		for (final Color hrZoneColor : _hrZoneColors) {
			hrZoneColor.dispose();
		}
	}

	private void enableActions() {

		final boolean isValid = isPersonValid();

		boolean isHrZoneAvailable = false;
		final TourPerson currentPerson = getCurrentPerson();

		if (currentPerson != null) {
			isHrZoneAvailable = currentPerson.getHrZonesSorted().size() > 0;
		}

		_btnAddPerson.setEnabled(!_isPersonModified && isValid);
		_peopleViewer.getTable().setEnabled(!_isPersonModified && isValid);

		_btnSavePerson.setEnabled(_isPersonModified && isValid);
		_btnCancel.setEnabled(_isPersonModified);

		_spinnerMaxHR.setEnabled(getSelectedHrMaxFormulaKey() == TrainingManager.HR_MAX_NOT_COMPUTED);

		_btnModifyHrZones.setEnabled(isHrZoneAvailable);
//		_btnComputeHrZonesForAllTours.setEnabled(_isPersonModified && isValid);
		_btnComputeHrZonesForAllTours.setEnabled(_isPersonModified == false);
	}

	private void fireModifyEvent() {

		if (_isFireModifyEvent) {

			TourManager.getInstance().clearTourDataCache();

			// fire event that person is modified
			getPreferenceStore().setValue(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED, Math.random());

			_isFireModifyEvent = false;
		}
	}

	private ZonedDateTime getBirthdayFromUI() {

		return ZonedDateTime.of(
				_dtBirthday.getYear(),
				_dtBirthday.getMonth() + 1,
				_dtBirthday.getDay(),
				0,
				0,
				0,
				0,
				TimeTools.getDefaultTimeZone());
	}

	/**
	 * @return Returns person which is currently displayed, one person is at least available
	 *         therefor this should never return <code>null</code> but it can be <code>null</code>
	 *         when the application is started the first time and people are not yet created.
	 */
	private TourPerson getCurrentPerson() {

		final boolean isNewPerson = _newPerson != null;
		return isNewPerson ? _newPerson : _selectedPerson;
	}

	/**
	 * @return Returns the key for the selected HR max formula in the combo box.
	 */
	private int getSelectedHrMaxFormulaKey() {

		int selectedIndex = _cboHrMaxFormula.getSelectionIndex();

		if (selectedIndex == -1) {
			selectedIndex = 0;
			_cboHrMaxFormula.select(0);
		}

		return TrainingManager.HRMaxFormulaKeys[selectedIndex];
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
		noDefaultAndApplyButton();
	}

	private void initUI(final Composite parent) {

		initializeDialogUnits(parent);

		_fontItalic = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);

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

		_hrZoneMouseListener = new MouseListener() {

			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				onEditHrZones();
			}

			@Override
			public void mouseDown(final MouseEvent e) {}

			@Override
			public void mouseUp(final MouseEvent e) {}
		};

		createDeviceList();
	}

	/**
	 * @return Returns <code>true</code> when person is valid, otherwise <code>false</code>.
	 */
	private boolean isPersonValid() {

		if (_txtFirstName.getText().trim().equals(UI.EMPTY_STRING)) {

			setErrorMessage(Messages.Pref_People_Error_first_name_is_required);

			// don't set focus because another field could be edited
//			_txtFirstName.setFocus();

			return false;

		} else {

			final String transferPath = _rawDataPathEditor.getStringValue().trim();

			if (!transferPath.equals(UI.EMPTY_STRING) && Util.isDirectory(transferPath) == false) {

				setErrorMessage(Messages.Pref_People_Error_path_is_invalid);

				return false;
			}
		}

		setErrorMessage(null);

		return true;
	}

	@Override
	public boolean okToLeave() {

		if (_isNoUI) {
			super.okToLeave();
			return true;
		}

		if (isPersonValid() == false) {
			return false;
		}

		saveState();
		savePerson(true, true);

		// enable action because the user can go back to this pref page
		enableActions();

		if (updateToursWithModifiedHrZones() == false) {
			return false;
		}

		return super.okToLeave();
	}

	private void onAddPerson() {

		_newPerson = createDefaultPerson();
		_isPersonModified = true;

		updateUIFromPerson(_newPerson);
		enableActions();

		// edit first name
		_tabFolderPerson.setSelection(0);
		_txtFirstName.selectAll();
		_txtFirstName.setFocus();
	}

	private void onCancelPerson() {

		_newPerson = null;
		_isPersonModified = false;

		if (_backupSelectedPersonHrZones != null) {
			_selectedPerson.setHrZones(_backupSelectedPersonHrZones);
		}

		updateUIFromPerson(_selectedPerson);
		enableActions();

		_peopleViewer.getTable().setFocus();
	}

	private void onCreateHrZonesFromTemplate() {

		final int selectedTemplate = _cboTemplate.getSelectionIndex();

		// reselect first item
		_cboTemplate.select(TrainingManager.HR_ZONE_TEMPLATE_00);

		if (selectedTemplate == TrainingManager.HR_ZONE_TEMPLATE_00) {
			// just ignore it
			return;
		}

		final TourPerson person = getCurrentPerson();
		final ArrayList<TourPersonHRZone> hrZones = person.getHrZonesSorted();

		// check if hr zones are already available
//		if (hrZones != null && hrZones.size() > 0) {
//
//			// hr zones are availabe
//			if (MessageDialog.openQuestion(
//					getShell(),
//					Messages.Pref_People_Dialog_ReplaceHrZones_Title,
//					Messages.Pref_People_Dialog_ReplaceHrZones_Message) == false) {
//				return;
//			}
//		}

		if (_backupSelectedPersonHrZones == null) {
			_backupSelectedPersonHrZones = cloneHrZones(hrZones);
		}

		person.setHrZones(TrainingManager.createHrZones(person, selectedTemplate));

		onEditHrZonesIsOK(person);
	}

	private void onEditHrZones() {

		/*
		 * valication must be checked because the dialog can save the person and therefor the person
		 * must be valid
		 */
		if (isPersonValid() == false) {
			return;
		}

		final TourPerson person = getCurrentPerson();

		if (_backupSelectedPersonHrZones == null) {
			_backupSelectedPersonHrZones = cloneHrZones(person.getHrZonesSorted());
		}

		if (new DialogHRZones(getShell(), person).open() == Window.OK) {
			onEditHrZonesIsOK(person);
		}
	}

	private void onEditHrZonesIsOK(final TourPerson person) {

		_isHrZoneModified = true;

		final int hrMaxFormulaKey = person.getHrMaxFormula();
		final int maxPulse = person.getMaxPulse();

		createUI_80_HrZone_InnerContainer(hrMaxFormulaKey, maxPulse, getBirthdayFromUI());
		onModifyPerson();
	}

	/**
	 * set person modified and enable actions accordingly
	 */
	private void onModifyPerson() {

		if (_isUpdateUI) {
			return;
		}

		_isPersonModified = true;

		enableActions();
	}

	private void onSavePerson() {

		if (isPersonValid() == false) {
			return;
		}

		savePerson(false, false);
		enableActions();

		_peopleViewer.getTable().setFocus();
	}

	private void onSelectPerson() {

		final IStructuredSelection selection = (IStructuredSelection) _peopleViewer.getSelection();
		final TourPerson person = (TourPerson) selection.getFirstElement();

		if (person != null) {

			_selectedPerson = person;

			_backupSelectedPersonHrZones = null;

			updateUIFromPerson(_selectedPerson);

		} else {
			// irgnore, this can happen when a refresh() of the table viewer is done
		}

		enableActions();
	}

	@Override
	public boolean performCancel() {

		if (_isNoUI) {
			super.performCancel();
			return true;
		}

		if (_isPersonModified && _newPerson == null) {

			// existing person is modified, reset hr zones

			if (_backupSelectedPersonHrZones != null) {
				_selectedPerson.setHrZones(_backupSelectedPersonHrZones);
			}
		}

		saveState();
		fireModifyEvent();

		if (updateToursWithModifiedHrZones() == false) {
			return false;
		}

		return super.performCancel();
	}

	@Override
	public boolean performOk() {

		if (_isNoUI) {
			super.performOk();
			return true;
		}

		if (performOK10() == false) {
			return false;
		}

		return super.performOk();
	}

	private boolean performOK10() {

		if (isPersonValid() == false) {
			return false;
		}

		savePerson(false, false);

		/*
		 * update UI because it's possible that other dialog boxes are displayed before the pref
		 * dialog is closed
		 */
		enableActions();

		saveState();
		fireModifyEvent();

		if (updateToursWithModifiedHrZones() == false) {
			return false;
		}

		return true;
	}

	private void restoreState() {

		/*
		 * selected person
		 */
		final long personId = Util.getStateLong(_state, STATE_SELECTED_PERSON, -1);
		StructuredSelection personSelection = null;
		if (personId != -1) {

			for (final TourPerson person : _people) {
				if (person.getPersonId() == personId) {
					personSelection = new StructuredSelection(person);
					break;
				}
			}
		}
		if (personSelection == null && _people.size() > 0) {

			/*
			 * previous person could not be reselected, select first person, a person MUST always be
			 * available since version 11.7
			 */

			personSelection = new StructuredSelection(_peopleViewer.getTable().getItem(0).getData());
		}

		if (personSelection != null) {
			_peopleViewer.setSelection(personSelection);
		}

		// reselected tab folder
		_tabFolderPerson.setSelection(Util.getStateInt(_state, STATE_SELECTED_TAB_FOLDER, 0));
	}

	/**
	 * @param isAskToSave
	 * @param isRevert
	 * @return Returns <code>false</code> when person is not saved, modifications will be reverted.
	 */
	private boolean savePerson(final boolean isAskToSave, final boolean isRevert) {

		final boolean isNewPerson = _newPerson != null;
		final TourPerson person = getCurrentPerson();
		_newPerson = null;

		if (_isPersonModified) {

			if (isAskToSave) {

				if (MessageDialog.openQuestion(
						Display.getCurrent().getActiveShell(),
						Messages.Pref_People_Dialog_SaveModifiedPerson_Title,
						NLS.bind(Messages.Pref_People_Dialog_SaveModifiedPerson_Message,

						// use name from the ui because it could be modified
								_txtFirstName.getText())) == false) {

					// revert person

					if (isRevert) {

						// update state
						_isPersonModified = false;

						// update ui from the previous selected person
						if (_backupSelectedPersonHrZones != null) {
							_selectedPerson.setHrZones(_backupSelectedPersonHrZones);
						}
						updateUIFromPerson(_selectedPerson);
					}

					return false;
				}
			}

			updatePersonFromUI(person);
			person.persist();

			// .persist() updates the people list, the model, retrieve updated people list
			_people = PersonManager.getTourPeople();

			// update state
			_isFireModifyEvent = true;
			_isPersonModified = false;

			// update ui
			if (isNewPerson) {
//				_people.add(person);
				_peopleViewer.add(person);

			} else {
				// !!! refreshing a person do not resort the table when sorting has changed !!!
				_peopleViewer.refresh();
			}

			if (_isHrZoneModified) {
				// keep person which hr zone was modified
				_peopleWithModifiedHrZones.put(person.getPersonId(), person);
			}

			_isHrZoneModified = false;

			// select updated/new person
			_peopleViewer.setSelection(new StructuredSelection(person), true);
		}

		return true;
	}

	private void saveState() {

		// selected person
		final Object firstElement = ((IStructuredSelection) _peopleViewer.getSelection()).getFirstElement();
		if (firstElement instanceof TourPerson) {
			_state.put(STATE_SELECTED_PERSON, ((TourPerson) firstElement).getPersonId());
		}

		// selected tab folder
		final int selectedTab = _tabFolderPerson.getSelectionIndex();
		_state.put(STATE_SELECTED_TAB_FOLDER, selectedTab < 0 ? 0 : selectedTab);
	}

	private void updatePersonFromUI(final TourPerson person) {

		String deviceId = null;
		final int selectedIndex = _cboSportComputer.getSelectionIndex();
		if (selectedIndex > 0) {
			deviceId = _deviceList.get(selectedIndex).deviceId;
		}

		/*
		 * update person
		 */
		person.setFirstName(_txtFirstName.getText());
		person.setLastName(_txtLastName.getText());

		person.setBirthDay(getBirthdayFromUI().toInstant().toEpochMilli());
		person.setWeight(_spinnerWeight.getSelection() / 10.0f);
		person.setHeight(_spinnerHeight.getSelection() / 100.0f);

		person.setGender(_rdoGenderMale.getSelection() ? 0 : 1);
		person.setRestPulse(_spinnerRestingHR.getSelection());

		person.setRawDataPath(_rawDataPathEditor.getStringValue());
		person.setDeviceReaderId(deviceId);

		// hr max formula
		final int hrMaxSelectionIndex = _cboHrMaxFormula.getSelectionIndex();
		person.setHrMaxFormula(TrainingManager.HRMaxFormulaKeys[hrMaxSelectionIndex]);
		person.setMaxPulse(_spinnerMaxHR.getSelection());
	}

	/**
	 * @return Return <code>true</code> when HR zones are not modified or when HR zones has been
	 *         updated. Returns <code>false</code> when tour data update has been canceled.
	 */
	private boolean updateToursWithModifiedHrZones() {

		setErrorMessage(null);

		if (_peopleWithModifiedHrZones.size() == 0) {
			return true;
		}

		if (MessageDialog.openQuestion(
				getShell(),
				Messages.Compute_HrZones_Dialog_ComputeAllTours_Title,
				Messages.Pref_People_Dialog_ComputeHrZonesForAllTours_Message)) {

			return computeHrZonesForAllTours(true);
		}

		// user has canceled and the user is informed that hr zones can be inconsistent
		return true;
	}

	/**
	 * @return Returns age from the birthday in the UI
	 */
	private int updateUIAge() {

		final Period age = Period.between(getBirthdayFromUI().toLocalDate(), _today.toLocalDate());

		final int ageYears = age.getYears();
		final String ageText = UI.SPACE + Integer.toString(ageYears) + UI.SPACE2 + Messages.Pref_People_Label_Years;

		_lblAgePerson.setText(ageText);
		_lblAgeHr.setText(ageText);

		return ageYears;
	}

	private void updateUIDeviceList() {

		// add all devices to the combobox
		for (final ExternalDevice device : _deviceList) {
			if (device == null) {
				_cboSportComputer.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
			} else {
				_cboSportComputer.add(device.visibleName);
			}
		}
	}

	private void updateUIFromPerson(final TourPerson person) {

		_isUpdateUI = true;
		{
			final ZonedDateTime dtBirthday = person.getBirthDayWithDefault();
			final int gender = person.getGender();
			final int restPulse = person.getRestPulse();

			_txtFirstName.setText(person.getFirstName());
			_txtLastName.setText(person.getLastName());
			_dtBirthday.setDate(dtBirthday.getYear(), dtBirthday.getMonthValue() - 1, dtBirthday.getDayOfMonth());
			_spinnerWeight.setSelection((int) (person.getWeight() * 10));
			_spinnerHeight.setSelection((int) (person.getHeight() * 100));
			_rawDataPathEditor.setStringValue(person.getRawDataPath());
			_rdoGenderMale.setSelection(gender == 0);
			_rdoGenderFemale.setSelection(gender != 0);
			_spinnerRestingHR.setSelection(restPulse == 0 ? TourPerson.DEFAULT_REST_PULSE : restPulse);

			final int hrMaxFormulaKey = person.getHrMaxFormula();
			final int maxPulse = person.getMaxPulse();

			updateUIHrMax(hrMaxFormulaKey, maxPulse);
			updateUISportComputer(person);

			createUI_80_HrZone_InnerContainer(hrMaxFormulaKey, maxPulse, dtBirthday);
		}
		_isUpdateUI = false;
	}

	/**
	 * hr max formula
	 * 
	 * @param hrMaxFormulaKey
	 * @param maxPulse
	 */
	private void updateUIHrMax(final int hrMaxFormulaKey, final int maxPulse) {

		final int age = updateUIAge();
		final int hrMax = TourPerson.getHrMax(hrMaxFormulaKey, maxPulse, age);

		if (hrMaxFormulaKey == TrainingManager.HR_MAX_NOT_COMPUTED) {

			// hr max is not computed

			_cboHrMaxFormula.select(TrainingManager.HRMaxFormulaKeys.length - 1);
			_spinnerMaxHR.setSelection(maxPulse);

		} else {

			int comboIndex = -1;
			for (final int formulaKey : TrainingManager.HRMaxFormulaKeys) {
				if (formulaKey == hrMaxFormulaKey) {
					comboIndex = hrMaxFormulaKey;
					break;
				}
			}

			if (comboIndex == -1) {
				// set default value
				comboIndex = 0;
			}

			_cboHrMaxFormula.select(comboIndex);
			_spinnerMaxHR.setSelection(hrMax);
		}
	}

	/**
	 * This method is called when variables are modified which influence the HR zones
	 */
	private void updateUIOnModifiedHrZones() {

		final int selectedHrMaxFormulaKey = getSelectedHrMaxFormulaKey();
		final int maxPulse = _spinnerMaxHR.getSelection();

		updateUIHrMax(selectedHrMaxFormulaKey, maxPulse);

		getCurrentPerson().resetHrZones();
		_isHrZoneModified = true;

		// update modified bpm in hr zones
		createUI_80_HrZone_InnerContainer(selectedHrMaxFormulaKey, maxPulse, getBirthdayFromUI());

		onModifyPerson();
	}

	/**
	 * select device in the combo box
	 */
	private void updateUISportComputer(final TourPerson person) {

		final String deviceId = person.getDeviceReaderId();

		if (deviceId == null) {
			_cboSportComputer.select(0);
		} else {

			int deviceIndex = 0;

			for (final ExternalDevice device : _deviceList) {

				if (device != null) {
					if (deviceId.equals(device.deviceId)) {
						_cboSportComputer.select(deviceIndex);
						break;
					}
				}

				deviceIndex++;
			}

			// when the device id was not found, select "<no selection>" entry
			if (deviceIndex == 0) {
				_cboSportComputer.select(0);
			}
		}
	}
}
