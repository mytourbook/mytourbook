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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.database.PersonManager;
import net.tourbook.importdata.DeviceManager;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.tour.TourManager;
import net.tourbook.training.DialogHRZones;
import net.tourbook.training.TrainingManager;
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
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PrefPagePeople extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID							= "net.tourbook.preferences.PrefPagePeopleId";			//$NON-NLS-1$

	private static final String			STATE_SELECTED_PERSON		= "selectedPersonId";									//$NON-NLS-1$
	private static final String			STATE_SELECTED_TAB_FOLDER	= "selectedTabFolder";									//$NON-NLS-1$

	private final IDialogSettings		_state						= TourbookPlugin.getDefault()//
																			.getDialogSettingsSection(ID);

	// REMOVED BIKES 30.4.2011

	private ArrayList<TourPerson>		_people;

	/**
	 * this device list has all the devices which are visible in the device combobox
	 */
	private ArrayList<ExternalDevice>	_deviceList;

	private final DateTimeFormatter		_dtFormatter				= DateTimeFormat.shortDate();
	private final NumberFormat			_nf1						= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf2						= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf2.setMinimumFractionDigits(2);
		_nf2.setMaximumFractionDigits(2);
	}
	private final boolean				_isOSX						= net.tourbook.util.UI.IS_OSX;

	private int							_spinnerWidth;

	private SelectionListener			_defaultSelectionListener;
	private ModifyListener				_defaultModifyListener;

	private boolean						_isFireModifyEvent			= false;
	private boolean						_isPersonModified			= false;
	private boolean						_isUpdateUI					= false;

	private TourPerson					_selectedPerson;
	private TourPerson					_newPerson;

	private org.joda.time.DateTime		_today						= new org.joda.time.DateTime().withTime(0, 0, 0, 0);
//	private org.joda.time.DateTime		_today1970					= new org.joda.time.DateTime().getMillis();

	private PixelConverter				_pc;

	/*
	 * UI controls
	 */
	private Composite					_prefPageContainer;
	private TableViewer					_peopleViewer;

	private Button						_btnAdd;
	private Button						_btnUpdate;
	private Button						_btnCancel;

	private TabFolder					_tabFolderPerson;
	private Text						_txtFirstName;
	private Text						_txtLastName;
	private Combo						_cboSportComputer;
	private Spinner						_spinnerWeight;
	private Spinner						_spinnerHeight;
	private Spinner						_spinnerRestingHR;
	private Spinner						_spinnerMaxHR;

	private Combo						_cboHrMaxFormula;
	private DateTime					_dtBirthday;
	private Label						_lblAge;

	private Button						_rdoGenderMale;
	private Button						_rdoGenderFemale;

	private Text						_txtRawDataPath;
	private DirectoryFieldEditor		_rawDataPathEditor;

	private Composite					_hrZoneOuterContainer;
	private ScrolledComposite			_hrZoneScrolledContainer;

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return _people.toArray(new TourPerson[_people.size()]);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

	private void actionEditHrZones() {

		if (new DialogHRZones(getShell(), _selectedPerson).open() == Window.OK) {

			createUI72HrZoneScrolledContainer(_hrZoneOuterContainer);

			// update layout that hr zones are displayed
			final Point folderSize = _tabFolderPerson.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			final GridData gd = (GridData) _tabFolderPerson.getLayoutData();
			gd.minimumHeight = folderSize.y;

			_prefPageContainer.layout(true, true);

		}
	}

	@Override
	public void applyData(final Object data) {

		// this is called after the UI is created

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
				_btnAdd.setEnabled(false);
				_peopleViewer.getTable().setEnabled(false);

				// select first name
				_tabFolderPerson.setSelection(0);
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

		// update people viewer
		_people = PersonManager.getTourPeople();
		_peopleViewer.setInput(new Object());

		// reselect previous person and tabfolder
		restoreState();

		enableActions();

		return container;
	}

	private TourPerson createDefaultPerson() {

		final TourPerson newPerson = new TourPerson(Messages.App_Default_PersonFirstName, UI.EMPTY_STRING);

		newPerson.setHeight(1.77f);
		newPerson.setWeight(77.7f);
		newPerson.setBirthDay(new org.joda.time.DateTime(1977, 7, 7, 0, 0, 0, 0).getMillis());

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
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_prefPageContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_prefPageContainer);
		{

			final Label label = new Label(_prefPageContainer, SWT.WRAP);
			label.setText(Messages.Pref_People_Title);

			final Composite innerContainer = new Composite(_prefPageContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(innerContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(innerContainer);
			{
				createUI10PeopleViewer(innerContainer);
				createUI20Buttons(innerContainer);

				createUI30PersonFolder(innerContainer);
			}

			// placeholder
			new Label(_prefPageContainer, SWT.NONE);
		}

		return _prefPageContainer;
	}

	private void createUI10PeopleViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory
				.fillDefaults()
				.grab(true, false)
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

	private void createUI20Buttons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(false, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
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

			/*
			 * button: update
			 */
			_btnUpdate = new Button(container, SWT.NONE);
			_btnUpdate.setText(Messages.App_Action_Update);
			_btnUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onUpdatePerson();
				}
			});
			setButtonLayoutData(_btnUpdate);
			final GridData gd = (GridData) _btnUpdate.getLayoutData();
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

	private void createUI30PersonFolder(final Composite parent) {

		_tabFolderPerson = new TabFolder(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(_tabFolderPerson);
		{
			// tab: person
			final TabItem tabItemDetails = new TabItem(_tabFolderPerson, SWT.NONE);
			tabItemDetails.setText(Messages.Pref_People_Tab_Person);
			tabItemDetails.setControl(createUI32TabPerson(_tabFolderPerson));

			// tab: hr zone
			final TabItem tabItemHRZone = new TabItem(_tabFolderPerson, SWT.NONE);
			tabItemHRZone.setText(Messages.Pref_People_Tab_HRZone);
			final Control hrZone = createUI34TabHRZone(_tabFolderPerson);
			tabItemHRZone.setControl(hrZone);

			// tab: data transfer
			final TabItem tabItemDataTransfer = new TabItem(_tabFolderPerson, SWT.NONE);
			tabItemDataTransfer.setText(Messages.Pref_People_Tab_DataTransfer);
			final Control dataTransfer = createUI36TabDataTransfer(_tabFolderPerson);
			tabItemDataTransfer.setControl(dataTransfer);
		}
	}

	private Control createUI32TabPerson(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
		{
			createUI50FieldFirstName(container);
			createUI52FieldLastName(container);
			createUI54FieldWeight(container);
			createUI56FieldHeight(container);
		}

		container.layout(true, true);

		return container;
	}

	private Control createUI34TabHRZone(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{

			final Composite containerPerson = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerPerson);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerPerson);
			{
				createUI60FieldGender(containerPerson);
				createUI62FieldBirthday(containerPerson);
				createUI66RestingHR(containerPerson);
				createUI68MaxHR(containerPerson);
			}

			createUI70HRZone(container);
		}

		return container;
	}

	private Control createUI36TabDataTransfer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		{
			createUI90FieldSportComputer(container);
			createUI92FieldRawDataPath(container);
		}

		// set layout after the fields are created
		GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 10, 0).applyTo(container);

		/*
		 * set width for the text control that the pref dialog is not as wide as the full path
		 */
		final Text rawPathControl = _rawDataPathEditor.getTextControl(container);
		final GridData gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = 200;

		return container;
	}

	/**
	 * field: first name
	 */
	private void createUI50FieldFirstName(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_first_name);

		_txtFirstName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFirstName);
		_txtFirstName.addModifyListener(_defaultModifyListener);
	}

	/**
	 * field: last name
	 */
	private void createUI52FieldLastName(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_last_name);

		_txtLastName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtLastName);
		_txtLastName.addModifyListener(_defaultModifyListener);
	}

	/**
	 * field: weight
	 */
	private void createUI54FieldWeight(final Composite parent) {

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
	private void createUI56FieldHeight(final Composite parent) {

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
	 * field: gender
	 */
	private void createUI60FieldGender(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_Gender);

		// radio
		final Composite containerGender = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(containerGender);
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
	 * field: birthday
	 */
	private void createUI62FieldBirthday(final Composite parent) {

		/*
		 * date-time: birthday
		 */
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_Birthday);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{

			_dtBirthday = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_dtBirthday);
			_dtBirthday.addSelectionListener(_defaultSelectionListener);
			_dtBirthday.addKeyListener(new KeyListener() {

				@Override
				public void keyPressed(final KeyEvent e) {}

				@Override
				public void keyReleased(final KeyEvent e) {
					/*
					 * listener is necessary because the selection listener is not fired when the
					 * values are modified with mouse up/down
					 */
					updateUIAge();
				}
			});

			/*
			 * label: age
			 */
			_lblAge = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(_lblAge);
		}
	}

	/**
	 * field: resting hr
	 */
	private void createUI66RestingHR(final Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_RestingHR);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// spinner: weight
			_spinnerRestingHR = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.grab(false, false)
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_spinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerRestingHR);
			_spinnerRestingHR.setMinimum(10);
			_spinnerRestingHR.setMaximum(200);
			_spinnerRestingHR.addSelectionListener(_defaultSelectionListener);
			_spinnerRestingHR.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onModifyPerson();
				}
			});

			// label: unit
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Graph_Label_Heartbeat_unit);
		}
	}

	/**
	 * field: max hr
	 */
	private void createUI68MaxHR(final Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_MaxHR);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * spinner: weight
			 */
			_spinnerMaxHR = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.grab(false, false)
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_spinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerMaxHR);
			_spinnerMaxHR.setMinimum(10);
			_spinnerMaxHR.setMaximum(300);
			_spinnerMaxHR.addSelectionListener(_defaultSelectionListener);
			_spinnerMaxHR.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onModifyPerson();
				}
			});

			/*
			 * label: unit
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Graph_Label_Heartbeat_unit);

			/*
			 * combo: formula to compute hr max
			 */
			_cboHrMaxFormula = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.indent(5, 0)
					.hint(50, SWT.DEFAULT)
					.applyTo(_cboHrMaxFormula);
			_cboHrMaxFormula.setVisibleItemCount(20);
			_cboHrMaxFormula.addSelectionListener(_defaultSelectionListener);

			// fill combobox
			final String[] hrMaxFormulaNames = TrainingManager.HRMaxFormulaNames;
			for (final String formulaName : hrMaxFormulaNames) {
				_cboHrMaxFormula.add(formulaName);
			}
		}
	}

	private void createUI70HRZone(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * hr zone fields
			 */
			_hrZoneOuterContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_hrZoneOuterContainer);
			GridLayoutFactory.fillDefaults().applyTo(_hrZoneOuterContainer);

			createUI72HrZoneScrolledContainer(_hrZoneOuterContainer);

			/*
			 * button: edit hr zones
			 */
			final Button button = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(button);
			button.setText(Messages.Dialog_HRZone_Button_ModifyHrZones);
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					actionEditHrZones();
				}
			});
		}
	}

	private void createUI72HrZoneScrolledContainer(final Composite parent) {

		Point scrollBackup = null;

		// dispose previous ui
		if (_hrZoneScrolledContainer != null) {

			// get current scroll position
			scrollBackup = _hrZoneScrolledContainer.getOrigin();

			// dispose previous fields
			_hrZoneScrolledContainer.dispose();
		}

		// scrolled container
		_hrZoneScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_hrZoneScrolledContainer);

		final Composite hrZoneInnerContainer = createUI74HrZoneInnerContainer(_hrZoneScrolledContainer);

		_hrZoneScrolledContainer.setContent(hrZoneInnerContainer);
		_hrZoneScrolledContainer.setExpandVertical(true);
		_hrZoneScrolledContainer.setExpandHorizontal(true);
		_hrZoneScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_hrZoneScrolledContainer.setMinSize(hrZoneInnerContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		parent.layout(true, true);

		// set scroll position to previous position
		if (scrollBackup != null) {
			_hrZoneScrolledContainer.setOrigin(scrollBackup);
		}
	}

	private Composite createUI74HrZoneInnerContainer(final Composite parent) {

		// hr zone container
		final Composite hrZoneContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(hrZoneContainer);
//		hrZoneContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI76HrZoneHeader(hrZoneContainer);
			createUI78HrZoneFields(hrZoneContainer);
		}

		return hrZoneContainer;
	}

	private void createUI76HrZoneHeader(final Composite parent) {

		/*
		 * label: zone
		 */
		Label label = new Label(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.grab(true, false)
				.hint(250, SWT.DEFAULT)
				.align(SWT.FILL, SWT.BOTTOM)
				.applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_Zone);

		/*
		 * header label: min pulse
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BOTTOM).applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_Pulse);

		/*
		 * label: ...
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(2), SWT.DEFAULT).applyTo(label);
		label.setText(UI.EMPTY_STRING);

		/*
		 * header label: max pulse
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BOTTOM).applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_MaxPulse);

		/*
		 * label: %
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(UI.EMPTY_STRING);
	}

	private void createUI78HrZoneFields(final Composite parent) {

		if (_selectedPerson == null) {
			return;
		}

		final Set<TourPersonHRZone> setHrZones = _selectedPerson.getHrZones();
		final ArrayList<TourPersonHRZone> hrZones = new ArrayList<TourPersonHRZone>(setHrZones);

		Collections.sort(hrZones);

		final int hrZoneSize = hrZones.size();

		for (int zoneIndex = 0; zoneIndex < hrZoneSize; zoneIndex++) {

			final TourPersonHRZone hrZone = hrZones.get(zoneIndex);

			final String zoneName = hrZone.getZoneName();
			final int zoneMinValue = hrZone.getZoneMinValue();
			final int zoneMaxValue = hrZone.getZoneMaxValue();

			/*
			 * label: hr zone name
			 */
			Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(zoneName == null ? UI.EMPTY_STRING : zoneName);

			/*
			 * label: min pulse
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(zoneMinValue == Integer.MIN_VALUE ? UI.EMPTY_STRING : Integer.toString(zoneMinValue));

			/*
			 * label: ...
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(zoneMinValue == Integer.MIN_VALUE || zoneMaxValue == Integer.MAX_VALUE
					? UI.EMPTY_STRING
					: UI.SYMBOL_DASH);

			/*
			 * spinner: max pulse
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(zoneMaxValue == Integer.MAX_VALUE ? UI.EMPTY_STRING : Integer.toString(zoneMaxValue));

			/*
			 * label: %
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.SYMBOL_PERCENTAGE);
		}
	}

	/**
	 * field: sport computer
	 */
	private void createUI90FieldSportComputer(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_People_Label_device);

		// combo
		_cboSportComputer = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
//			GridDataFactory.fillDefaults().indent(0, 10).align(SWT.BEGINNING, SWT.FILL).applyTo(_cboSportComputer);
		_cboSportComputer.setVisibleItemCount(20);
		_cboSportComputer.addSelectionListener(_defaultSelectionListener);

		// spacer
		new Label(parent, SWT.NONE);
	}

	/**
	 * field: path to save raw tour data
	 */
	private void createUI92FieldRawDataPath(final Composite parent) {

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

		/*
		 * label: info
		 */
		final Label label = new Label(parent, SWT.WRAP);
		GridDataFactory
				.fillDefaults()
				.span(3, 1)
				.indent(0, 15)
				.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
				.applyTo(label);
		label.setText(Messages.Pref_People_Label_DataTransfer);
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
		final boolean isNewPerson = _newPerson != null;

		_btnAdd.setEnabled(!_isPersonModified && isValid);
		_peopleViewer.getTable().setEnabled(!_isPersonModified && isValid);

		_btnUpdate.setText(isNewPerson ? Messages.App_Action_Save : Messages.App_Action_Update);
		_btnUpdate.setEnabled(_isPersonModified && isValid);
		_btnCancel.setEnabled(_isPersonModified);

		final int selectedHrMaxFormulaKey = getSelectedHrMaxFormulaKey();

		_spinnerMaxHR.setEnabled(selectedHrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_MANUAL);

		updateUIOnModify(selectedHrMaxFormulaKey);
	}

	private void fireModifyEvent() {

		if (_isFireModifyEvent) {

			TourManager.getInstance().clearTourDataCache();

			// fire bike list modify event
			getPreferenceStore().setValue(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED, Math.random());

			_isFireModifyEvent = false;
		}
	}

	private long getBirthdayFromUI() {

		return new org.joda.time.DateTime(
				_dtBirthday.getYear(),
				_dtBirthday.getMonth() + 1,
				_dtBirthday.getDay(),
				0,
				0,
				0,
				0).getMillis();
	}

	/**
	 * @return Returns the key for the selected Hr max formula.
	 */
	private int getSelectedHrMaxFormulaKey() {

		int selectedIndex = _cboHrMaxFormula.getSelectionIndex();
		if (selectedIndex == -1) {
			selectedIndex = 0;
			_cboHrMaxFormula.select(0);
		}

		return TrainingManager.HRMaxFormulaKeys[selectedIndex];
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

		updateUIFromPerson(_selectedPerson);
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

			updateUIFromPerson(_selectedPerson);
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
		fireModifyEvent();

		return super.performCancel();
	}

	@Override
	public boolean performOk() {

		if (isPersonValid() == false) {
			return false;
		}

		savePerson(false);

		saveState();
		fireModifyEvent();

		return super.performOk();
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
					updateUIFromPerson(_selectedPerson);

					return;
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

		// selected tab folder
		final int selectedTab = _tabFolderPerson.getSelectionIndex();
		_state.put(STATE_SELECTED_TAB_FOLDER, selectedTab < 0 ? 0 : selectedTab);
	}

	/**
	 * select device in the combo box
	 */
	private void selectDevice(final TourPerson person) {

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

	/**
	 * hr max formula
	 */
	private void selectHrMaxFormula(final TourPerson person) {

		final int hrMaxFormula = person.getHrMaxFormula();
		int selectionIndex = -1;
		for (final int formulaKey : TrainingManager.HRMaxFormulaKeys) {
			if (formulaKey == hrMaxFormula) {
				selectionIndex = hrMaxFormula;
				break;
			}
		}
		if (selectionIndex == -1) {
			// set default value
			selectionIndex = 0;
		}
		_cboHrMaxFormula.select(selectionIndex);
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

		person.setBirthDay(getBirthdayFromUI());
		person.setWeight(_spinnerWeight.getSelection() / 10.0f);
		person.setHeight(_spinnerHeight.getSelection() / 100.0f);

		person.setGender(_rdoGenderMale.getSelection() ? 0 : 1);
		person.setRestPulse(_spinnerRestingHR.getSelection());

		person.setRawDataPath(_rawDataPathEditor.getStringValue());
		person.setDeviceReaderId(deviceId);

		// hr max formula
		final int hrMaxSelectionIndex = _cboHrMaxFormula.getSelectionIndex();
		person.setHrMaxFormula(TrainingManager.HRMaxFormulaKeys[hrMaxSelectionIndex]);
	}

	private int updateUIAge() {

		final Period age = new Period(getBirthdayFromUI(), _today.getMillis());

		final int ageYears = age.getYears();
		_lblAge.setText(UI.SPACE + ageYears + UI.SPACE2 + Messages.Pref_People_Label_Years);

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
			final org.joda.time.DateTime dtBirthday = new org.joda.time.DateTime(person.getBirthDay());
			final int gender = person.getGender();
			final int restPulse = person.getRestPulse();

			_txtFirstName.setText(person.getFirstName());
			_txtLastName.setText(person.getLastName());
			_dtBirthday.setDate(dtBirthday.getYear(), dtBirthday.getMonthOfYear() - 1, dtBirthday.getDayOfMonth());
			_spinnerWeight.setSelection((int) (person.getWeight() * 10));
			_spinnerHeight.setSelection((int) (person.getHeight() * 100));
			_rawDataPathEditor.setStringValue(person.getRawDataPath());
			_rdoGenderMale.setSelection(gender == 0);
			_rdoGenderFemale.setSelection(gender != 0);
			_spinnerRestingHR.setSelection(restPulse == 0 ? TourPerson.DEFAULT_REST_PULSE : restPulse);

			updateUIAge();

			selectHrMaxFormula(person);
			selectDevice(person);

//			// update layout that hr zones are displayed
//			_tabFolderPerson.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
//			GridData gd = (GridData) _tabFolderPerson.getLayoutData();
//
//			_prefPageContainer.layout(true, true);
		}
		_isUpdateUI = false;
	}

	private void updateUIOnModify(final int selectedHrMaxFormulaKey) {

		final int age = updateUIAge();

		if (selectedHrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_220_AGE) {

			// HRmax = 220 - age

			_spinnerMaxHR.setSelection(220 - age);

		} else if (selectedHrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_205_8) {

			// HRmax = 205.8 - (0.685 x age)

			_spinnerMaxHR.setSelection((int) (205.8 - (0.685 * age)));

		} else if (selectedHrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_206_9) {

			//  HRmax = 206.9 - (0.67 x age)

			_spinnerMaxHR.setSelection((int) (206.9 - (0.67 * age)));

		} else if (selectedHrMaxFormulaKey == TrainingManager.HR_MAX_FORMULA_191_5) {

			//  HRmax = 191.5 - (0.007 x age2)

			_spinnerMaxHR.setSelection((int) (191.5 - (0.007 * age * age)));
		}

	}
}
