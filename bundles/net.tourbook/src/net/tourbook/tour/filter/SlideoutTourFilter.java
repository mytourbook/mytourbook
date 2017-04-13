/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter;

import java.text.DateFormatSymbols;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.form.SashLeftFixedForm;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.search.SearchView;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourFilter extends AdvancedSlideout {

// SET_FORMATTING_OFF

	private static final String		HIDE_ALLOWED_DATE_TIME	= "DateTime";						//$NON-NLS-1$

	static final String				FIELD_NO				= "fieldNo";						//$NON-NLS-1$

	private static final String		STATE_IS_LIVE_UPDATE	= "STATE_IS_LIVE_UPDATE";			//$NON-NLS-1$
	private static final String		STATE_SASH_WIDTH		= "STATE_SASH_WIDTH";				//$NON-NLS-1$

// SET_FORMATTING_ON

	private final IPreferenceStore	_prefStore				= TourbookPlugin.getPrefStore();
	private final IDialogSettings	_state;

	private ModifyListener			_defaultModifyListener;
	private FocusListener			_keepOpenListener;
	private IPropertyChangeListener	_prefChangeListener;

	private SelectionAdapter		_fieldSelectionListener_DateTime;

	private boolean					_isUpdateUI;

	{
		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onProfile_Modify();
			}
		};

		_fieldSelectionListener_DateTime = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onField_Select_DateTime(event);
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsKeepOpenInternally(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsKeepOpenInternally(false);
			}
		};
	}

	private PixelConverter						_pc;

	private TableViewer							_profileViewer;

	private final ArrayList<TourFilterProfile>	_filterProfiles			= TourFilterManager.getProfiles();
	private TourFilterProfile					_selectedProfile;

	private boolean								_isLiveUpdate;

	/**
	 * Contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width.
	 */
	private final ArrayList<Control>			_firstColumnControls	= new ArrayList<Control>();

	private ToolItem							_tourFilterItem;

	/*
	 * UI controls
	 */
	private Composite							_filterScrolled_Content;
	private Composite							_filterOuterContainer;
	private Composite							_containerFilter;
	private Composite							_containerProfiles;

	private ScrolledComposite					_filterScrolled_Container;

	private Button								_btnActivateAll_No;
	private Button								_btnActivateAll_Yes;
	private Button								_btnAddProperty;
	private Button								_btnApply;
	private Button								_btnCopyProfile;
	private Button								_btnDeleteProfile;
	private Button								_chkLiveUpdate;

	private Label								_lblProfileName;

	private Text								_txtProfileName;

	private SashLeftFixedForm					_sashForm;

	private ToolBar								_ownerToolbar;

	private class FilterProfileComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			if (e1 == null || e2 == null) {
				return 0;
			}

			final TourFilterProfile profile1 = (TourFilterProfile) e1;
			final TourFilterProfile profile2 = (TourFilterProfile) e2;

			return profile1.name.compareTo(profile2.name);
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {

			// force resorting when a name is renamed
			return true;
		}
	}

	private class FilterProfileProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _filterProfiles.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public SlideoutTourFilter(	final Control ownerControl,
								final ToolItem toolItem,
								final IDialogSettings state) {

		super(ownerControl, state, new int[] { 900, 200, 900, 200 });

		_state = state;
		_ownerToolbar = (ToolBar) ownerControl;
		_tourFilterItem = toolItem;

		setShellFadeOutDelaySteps(30);
		setDraggerText(Messages.Slideout_TourFilter_Label_Title);
	}

	void action_PropertyDelete(final TourFilterProperty filterProperty) {

		/*
		 * Confirm deletion
		 */
		boolean isDeleteProfile;
		setIsKeepOpenInternally(true);
		{
			isDeleteProfile = MessageDialog.openConfirm(

					Display.getCurrent().getActiveShell(),
					Messages.Slideout_TourFilter_Confirm_DeleteProperty_Title,
					NLS.bind(
							Messages.Slideout_TourFilter_Confirm_DeleteProperty_Message,
							filterProperty.fieldConfig.name));
		}
		setIsKeepOpenInternally(false);

		if (isDeleteProfile == false) {
			return;
		}

		// update model
		final ArrayList<TourFilterProperty> filterProperties = _selectedProfile.filterProperties;
		filterProperties.remove(filterProperty);

		// update UI
		createUI_410_FilterProperties();
		updateUI_Properties();

		// update number of properties
		_profileViewer.update(_selectedProfile, null);

		fireModifyEvent();
	}

	void action_PropertyMoveDown(final TourFilterProperty filterProperty) {

		final ArrayList<TourFilterProperty> filterProperties = _selectedProfile.filterProperties;
		final int selectedIndex = filterProperties.indexOf(filterProperty);

		Collections.swap(filterProperties, selectedIndex, selectedIndex + 1);

		createUI_410_FilterProperties();
		updateUI_Properties();
	}

	void action_PropertyMoveUp(final TourFilterProperty filterProperty) {

		final ArrayList<TourFilterProperty> filterProperties = _selectedProfile.filterProperties;
		final int selectedIndex = filterProperties.indexOf(filterProperty);

		Collections.swap(filterProperties, selectedIndex, selectedIndex - 1);

		createUI_410_FilterProperties();
		updateUI_Properties();
	}

	private void addPrefListener(final Composite parent) {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {
					updateUI_Properties();
				}
			}
		};

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDisposeSlideout();
			}
		});

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	protected boolean canCloseShell(final Shell[] openedShells) {

		/*
		 * Linux creates a shell in DateTime widget which prevents to close the slideout, accept
		 * this "shell".
		 */

		for (final Shell shell : openedShells) {

//			Util.dumpChildren(shell, 1);

			for (final Control child : shell.getChildren()) {

				final String controlText = child.toString();

//				System.out.println(this.getClass().getName() + "\tcontrolText:" + controlText);

				if (controlText.startsWith(HIDE_ALLOWED_DATE_TIME) == false) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		/*
		 * Reset to a valid state when the slideout is opened again
		 */
		_selectedProfile = null;

		initUI(parent);

		createUI(parent);

		addPrefListener(parent);

		// load viewer
		_profileViewer.setInput(new Object());

		restoreState();
		enableControls();
	}

	private void createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(container);
			GridLayoutFactory.swtDefaults().applyTo(container);
			{
				// left part
				_containerProfiles = createUI_200_Profiles(container);

				// sash
				final Sash sash = new Sash(container, SWT.VERTICAL);
				{
					UI.addSashColorHandler(sash);

					// save sash width
					sash.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseUp(final MouseEvent e) {
							_state.put(STATE_SASH_WIDTH, _containerProfiles.getSize().x);
						}
					});
				}

				// right part
				_containerFilter = createUI_300_Filter(container);

				_sashForm = new SashLeftFixedForm(//
						container,
						_containerProfiles,
						sash,
						_containerFilter,
						30);
			}
		}
	}

	private Composite createUI_200_Profiles(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.extendedMargins(0, 3, 0, 0)
				.applyTo(container);
		{
			{
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText(Messages.Slideout_TourFilter_Label_Profiles);
			}

			createUI_210_ProfileViewer(container);
			createUI_220_ProfileActions(container);
		}

		return container;
	}

	private void createUI_210_ProfileViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

		table.setLayout(new TableLayout());

		// !!! this prevents that the horizontal scrollbar is displayed, but is not always working :-(
		table.setHeaderVisible(false);
//		table.setHeaderVisible(true);

		_profileViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		{
			// Column: Profile name

			tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_TourFilter_Column_ProfileName);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final TourFilterProfile profile = (TourFilterProfile) cell.getElement();

					cell.setText(profile.name);
				}
			});
			tableLayout.setColumnData(tc, new ColumnWeightData(1, false));
		}

		{
			// Column: Number of properties

			tvc = new TableViewerColumn(_profileViewer, SWT.TRAIL);
			tc = tvc.getColumn();
			tc.setText(Messages.Slideout_TourFilter_Column_Properties);
			tc.setToolTipText(Messages.Slideout_TourFilter_Column_Properties_Tooltip);
			tvc.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {

					final TourFilterProfile profile = (TourFilterProfile) cell.getElement();

					cell.setText(Integer.toString(profile.filterProperties.size()));
				}
			});
			tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(6), false));
		}

		/*
		 * create table viewer
		 */
		_profileViewer.setContentProvider(new FilterProfileProvider());
		_profileViewer.setComparator(new FilterProfileComparator());

		_profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onProfile_Select();
			}
		});

		_profileViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {

				// set focus to  profile name
				_txtProfileName.setFocus();
				_txtProfileName.selectAll();
			}
		});

		_profileViewer.getTable().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {

				if (e.keyCode == SWT.DEL) {
					onProfile_Delete();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});
	}

	private void createUI_220_ProfileActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			{
				/*
				 * Button: New
				 */
				final Button button = new Button(container, SWT.PUSH);
				button.setText(Messages.Slideout_TourFilter_Action_AddProfile);
				button.setToolTipText(Messages.Slideout_TourFilter_Action_AddProfile_Tooltip);
				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Add();
					}
				});

				// set button default width
				UI.setButtonLayoutData(button);
			}
			{
				/*
				 * Button: Copy
				 */
				_btnCopyProfile = new Button(container, SWT.PUSH);
				_btnCopyProfile.setText(Messages.Slideout_TourFilter_Action_CopyProfile);
				_btnCopyProfile.setToolTipText(Messages.Slideout_TourFilter_Action_CopyProfile_Tooltip);
				_btnCopyProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Copy();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnCopyProfile);
			}
			{
				/*
				 * Button: Delete
				 */
				_btnDeleteProfile = new Button(container, SWT.PUSH);
				_btnDeleteProfile.setText(Messages.Slideout_TourFilter_Action_DeleteProfile);
				_btnDeleteProfile.setToolTipText(Messages.Slideout_TourFilter_Action_DeleteProfile_Tooltip);
				_btnDeleteProfile.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProfile_Delete();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnDeleteProfile);
			}
		}
	}

	private Composite createUI_300_Filter(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.extendedMargins(3, 0, 0, 0)
				.applyTo(container);
		{
			createUI_310_FilterName(container);
			createUI_400_FilterOuterContainer(container);
			createUI_500_FilterActions(container);
		}

		return container;
	}

	private void createUI_310_FilterName(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				// Label: Profile name
				_lblProfileName = new Label(container, SWT.NONE);
				_lblProfileName.setText(Messages.Slideout_TourFilter_Label_ProfileName);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblProfileName);
			}
			{
				// Text: Profile name
				_txtProfileName = new Text(container, SWT.BORDER);
				_txtProfileName.addModifyListener(_defaultModifyListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT)
						.applyTo(_txtProfileName);
			}
		}
	}

	private void createUI_400_FilterOuterContainer(final Composite parent) {

		_filterOuterContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_filterOuterContainer);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(2))
				.applyTo(_filterOuterContainer);
	}

	/**
	 * Create the filter fields from the selected profile.
	 */
	private void createUI_410_FilterProperties() {

		if (isFilterDisposed()) {
			return;
		}

		Point scrollOrigin = null;

		// dispose previous content
		if (_filterScrolled_Container != null && !_filterScrolled_Container.isDisposed()) {

			// get current scroll position
			scrollOrigin = _filterScrolled_Container.getOrigin();

			_filterScrolled_Container.dispose();
		}

		if (_selectedProfile == null) {
			return;
		}

		/*
		 * Setup first columns
		 */
		_firstColumnControls.clear();
		_firstColumnControls.add(_lblProfileName);

		final ArrayList<TourFilterProperty> filterProperties = _selectedProfile.filterProperties;
		final int numProperties = filterProperties.size();

		if (numProperties == 0) {
			return;
		}

		/*
		 * Field listener
		 */
		final SelectionListener fieldListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onProperty_SelectField(e.widget);
			}

		};
		final SelectionAdapter operatorListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onProperty_SelectOperator(e.widget);
			}
		};

		final SelectionAdapter enabledListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onProperty_SelectEnabled(e.widget);
			}
		};

		{
			// scrolled container
			_filterScrolled_Container = new ScrolledComposite(_filterOuterContainer, SWT.H_SCROLL | SWT.V_SCROLL);
			_filterScrolled_Container.setExpandVertical(true);
			_filterScrolled_Container.setExpandHorizontal(true);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_filterScrolled_Container);
		}

		{
			// properties container
			_filterScrolled_Content = new Composite(_filterScrolled_Container, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					//				.grab(true, true)
					.applyTo(_filterScrolled_Content);
			_filterScrolled_Container.setContent(_filterScrolled_Content);
			_filterScrolled_Container.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					onResizeFilterContent();
				}
			});

			GridLayoutFactory
					.fillDefaults()//
					.numColumns(5)
					.applyTo(_filterScrolled_Content);
		}

//		_filterScrolled_Content.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));

		_filterScrolled_Content.setRedraw(false);
		{
			for (int propertyIndex = 0; propertyIndex < numProperties; propertyIndex++) {

				final TourFilterProperty filterProperty = filterProperties.get(propertyIndex);

				{
					/*
					 * Checkbox: Is filter enabled
					 */
					final Button chkIsFieldEnabled = new Button(_filterScrolled_Content, SWT.CHECK);
					chkIsFieldEnabled.setData(filterProperty);

					chkIsFieldEnabled.setText(String.format("&%d", propertyIndex + 1));//$NON-NLS-1$
					chkIsFieldEnabled.setToolTipText(Messages.Slideout_TourFilter_Checkbox_IsPropertyEnabled_Tooltip);
					chkIsFieldEnabled.addSelectionListener(enabledListener);

					filterProperty.checkboxIsPropertyEnabled = chkIsFieldEnabled;

					_firstColumnControls.add(chkIsFieldEnabled);
				}
				{
					/*
					 * Combo: Filter field
					 */
					final Combo comboFilterField = new Combo(_filterScrolled_Content, SWT.DROP_DOWN | SWT.READ_ONLY);
					comboFilterField.setData(filterProperty);
					comboFilterField.setVisibleItemCount(30);
					comboFilterField.addFocusListener(_keepOpenListener);
					comboFilterField.addSelectionListener(fieldListener);

					// keep combo reference
					filterProperty.comboFieldName = comboFilterField;
				}
				{
					/*
					 * Combo: Field operator
					 */
					final Combo comboFieldOperator = new Combo(_filterScrolled_Content, SWT.DROP_DOWN | SWT.READ_ONLY);
					comboFieldOperator.setData(filterProperty);
					comboFieldOperator.setVisibleItemCount(30);
					comboFieldOperator.addFocusListener(_keepOpenListener);
					comboFieldOperator.addSelectionListener(operatorListener);

					// keep combo reference
					filterProperty.comboFieldOperator = comboFieldOperator;
				}

				{
					/*
					 * Container: Field details
					 */
					final Composite fieldContainer = new Composite(_filterScrolled_Content, SWT.NONE);
					GridDataFactory
							.fillDefaults()//
//							.grab(true, false)
							.applyTo(fieldContainer);
					GridLayoutFactory.fillDefaults().numColumns(1).applyTo(fieldContainer);
//					fieldDetailOuterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

					filterProperty.fieldDetailContainer = fieldContainer;
				}

				{
					/*
					 * Toolbar: Property actions
					 */
					createUI_418_PropertyActions(_filterScrolled_Content, filterProperty, propertyIndex, numProperties);
				}
			}
		}
		_filterScrolled_Content.setRedraw(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_filterScrolled_Container.setOrigin(scrollOrigin);
		}
	}

	private void createUI_418_PropertyActions(	final Composite filterContainer,
												final TourFilterProperty filterProperty,
												final int propertyIndex,
												final int numProperties) {
		boolean isMoveUp = false;
		boolean isMoveDown = false;

		if (numProperties == 1) {

			// no up or down

		} else if (propertyIndex == 0) {

			// this is the first property

			isMoveDown = true;

		} else if (propertyIndex == numProperties - 1) {

			// this is the last property

			isMoveUp = true;

		} else {

			isMoveUp = true;
			isMoveDown = true;
		}

		final Action_Property_Delete actionDeleteProperty = new Action_Property_Delete(this, filterProperty);

		final ToolBar toolbar = new ToolBar(filterContainer, SWT.FLAT);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.CENTER)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		if (isMoveUp) {
			tbm.add(new Action_Property_MoveUp(this, filterProperty));
		}

		if (isMoveDown) {
			tbm.add(new Action_Property_MoveDown(this, filterProperty));
		}

		tbm.add(actionDeleteProperty);

		tbm.update(true);
	}

	private void createUI_430_FieldDetail(final TourFilterProperty filterProp) {

		// remove previous content
		filterProp.disposeFieldInnerContainer();

		final Composite fieldContainer = filterProp.fieldDetailContainer;
		GridDataFactory
				.fillDefaults()//
//				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(fieldContainer);
//		fieldContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// default for number of columns is 0
		int numColumns = 0;

		final TourFilterFieldConfig fldConfig = filterProp.fieldConfig;
		final TourFilterFieldType fieldType = fldConfig.fieldType;

		switch (filterProp.fieldOperator) {
		case GREATER_THAN:
		case GREATER_THAN_OR_EQUAL:
		case LESS_THAN:
		case LESS_THAN_OR_EQUAL:
		case EQUALS:
		case NOT_EQUALS:

			switch (fieldType) {
			case DATE:
				numColumns += createUI_Field_Date(fieldContainer, filterProp, 1);
				break;

			case TIME:
				numColumns += createUI_Field_Time(fieldContainer, filterProp, 1);
				break;

			case DURATION:
				numColumns += createUI_Field_Duration(fieldContainer, filterProp, 1, true);
				break;

			case NUMBER_INTEGER:
				numColumns += createUI_Field_Number_Integer(fieldContainer, filterProp, fldConfig, 1, true);
				break;

			case NUMBER_FLOAT:
				numColumns += createUI_Field_Number_Float(fieldContainer, filterProp, fldConfig, 1, true);
				break;
			}

			break;

		case BETWEEN:
		case NOT_BETWEEN:

			switch (fieldType) {
			case DATE:
				numColumns += createUI_Field_Date(fieldContainer, filterProp, 1);
				numColumns += createUI_Operator_And(fieldContainer);
				numColumns += createUI_Field_Date(fieldContainer, filterProp, 2);
				break;

			case TIME:
				numColumns += createUI_Field_Time(fieldContainer, filterProp, 1);
				numColumns += createUI_Operator_And(fieldContainer);
				numColumns += createUI_Field_Time(fieldContainer, filterProp, 2);
				break;

			case DURATION:
				numColumns += createUI_Field_Duration(fieldContainer, filterProp, 1, false);
				numColumns += createUI_Operator_And(fieldContainer);
				numColumns += createUI_Field_Duration(fieldContainer, filterProp, 2, true);
				break;

			case NUMBER_INTEGER:
				numColumns += createUI_Field_Number_Integer(fieldContainer, filterProp, fldConfig, 1, false);
				numColumns += createUI_Operator_And(fieldContainer);
				numColumns += createUI_Field_Number_Integer(fieldContainer, filterProp, fldConfig, 2, true);
				break;

			case NUMBER_FLOAT:
				numColumns += createUI_Field_Number_Float(fieldContainer, filterProp, fldConfig, 1, false);
				numColumns += createUI_Operator_And(fieldContainer);
				numColumns += createUI_Field_Number_Float(fieldContainer, filterProp, fldConfig, 2, true);
				break;

			case SEASON:
				numColumns += createUI_Field_SeasonDay(fieldContainer, filterProp, 1);
				numColumns += createUI_Field_SeasonMonth(fieldContainer, filterProp, 1);
				numColumns += createUI_Operator_And(fieldContainer);
				numColumns += createUI_Field_SeasonDay(fieldContainer, filterProp, 2);
				numColumns += createUI_Field_SeasonMonth(fieldContainer, filterProp, 2);
				break;
			}

			break;

		case STARTS_WITH:
			break;
		case ENDS_WITH:
			break;

		case INCLUDE_ANY:
			break;
		case EXCLUDE_ALL:
			break;

		case IS_EMPTY:
		case IS_NOT_EMPTY:

			switch (fieldType) {
			case TEXT:
				numColumns += createUI_Field_Text(fieldContainer);
				break;
			}

			break;

		case LIKE:
			break;
		case NOT_LIKE:
			break;

		case SEASON_UNTIL_TODAY_FROM_YEAR_START:
		case SEASON_TODAY_UNTIL_YEAR_END:
		case SEASON_CURRENT_DAY:
		case SEASON_CURRENT_MONTH:
			// no additional controls
			break;

		case SEASON_MONTH:
			numColumns += createUI_Field_SeasonMonth(fieldContainer, filterProp, 1);
			break;

		case SEASON_UNTIL_TODAY_FROM_DATE:
		case SEASON_TODAY_UNTIL_DATE:
			numColumns += createUI_Field_SeasonDay(fieldContainer, filterProp, 1);
			numColumns += createUI_Field_SeasonMonth(fieldContainer, filterProp, 1);
			break;
		}

		final GridLayout gl = (GridLayout) fieldContainer.getLayout();
		gl.numColumns = numColumns;

		fieldContainer.layout(true);
	}

	private void createUI_500_FilterActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
		{
			{
				/*
				 * Button: Add Property
				 */
				_btnAddProperty = new Button(container, SWT.PUSH);
				_btnAddProperty.setText(Messages.Slideout_TourFilter_Action_AddProperty);
				_btnAddProperty.setToolTipText(Messages.Slideout_TourFilter_Action_AddProperty_Tooltip);

				_btnAddProperty.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProperty_Add();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnAddProperty);
			}
			{
				/*
				 * Button: Activate All
				 */
				_btnActivateAll_Yes = new Button(container, SWT.PUSH);
				_btnActivateAll_Yes.setText(Messages.Slideout_TourFilter_Action_ActivateAll);
				_btnActivateAll_Yes.setToolTipText(Messages.Slideout_TourFilter_Action_ActivateAll_Tooltip);

				_btnActivateAll_Yes.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProperty_ActivateAll(true);
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnActivateAll_Yes);
			}
			{
				/*
				 * Button: Deactivate All
				 */
				_btnActivateAll_No = new Button(container, SWT.PUSH);
				_btnActivateAll_No.setText(Messages.Slideout_TourFilter_Action_DeactivateAll);
				_btnActivateAll_No.setToolTipText(Messages.Slideout_TourFilter_Action_DeactivateAll_Tooltip);

				_btnActivateAll_No.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProperty_ActivateAll(false);
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnActivateAll_No);
			}
			{
				/*
				 * Checkbox: live update
				 */
				_chkLiveUpdate = new Button(container, SWT.CHECK);
				_chkLiveUpdate.setText(Messages.Slideout_TourFilter_Checkbox_IsLiveUpdate);
				_chkLiveUpdate.setToolTipText(Messages.Slideout_TourFilter_Checkbox_IsLiveUpdate_Tooltip);
				_chkLiveUpdate.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						doLiveUpdate();
					}
				});

				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.CENTER)
						.applyTo(_chkLiveUpdate);
			}
			{
				/*
				 * Button: Apply
				 */
				_btnApply = new Button(container, SWT.PUSH);
				_btnApply.setText(Messages.Slideout_TourFilter_Action_Apply);
				_btnApply.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						doApply();
					}
				});

				// set button default width
				UI.setButtonLayoutData(_btnApply);
			}
		}
	}

	private int createUI_Field_Date(final Composite parent,
									final TourFilterProperty filterProperty,
									final int fieldNo) {

		final DateTime dtTourDate = new DateTime(parent, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);

		dtTourDate.setData(filterProperty);
		dtTourDate.setData(FIELD_NO, fieldNo);

		dtTourDate.addFocusListener(_keepOpenListener);
		dtTourDate.addSelectionListener(_fieldSelectionListener_DateTime);

		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(dtTourDate);

		if (fieldNo == 1) {
			filterProperty.uiDateTime1 = dtTourDate;
		} else {
			filterProperty.uiDateTime2 = dtTourDate;
		}

		return 1;
	}

	/**
	 * @param parent
	 * @param filterProperty
	 * @param fieldNo
	 * @param isShowUnit
	 * @return
	 */
	private int createUI_Field_Duration(final Composite parent,
										final TourFilterProperty filterProperty,
										final int fieldNo,
										final boolean isShowUnit) {

		final TimeDuration duration = new TimeDuration(parent, isShowUnit);

		duration.setMaxHours(999);
		duration.setData(filterProperty);
		duration.setData(SlideoutTourFilter.FIELD_NO, fieldNo);

		duration.setTimeListener(new TimeDurationListener() {
			@Override
			public void timeSelected(final int time) {
				onField_Select_Duration(duration, time);
			}
		});

		if (fieldNo == 1) {
			filterProperty.uiDuration1 = duration;
		} else {
			filterProperty.uiDuration2 = duration;
		}

		return 1;
	}

	private int createUI_Field_Number_Float(final Composite parent,
											final TourFilterProperty filterProperty,
											final TourFilterFieldConfig fieldConfig,
											final int fieldNo,
											final boolean isShowUnitLabel) {

		int numColumns = 0;

		{
			/*
			 * Spinner: float number
			 */
			numColumns++;

			final Spinner spinner = new Spinner(parent, SWT.BORDER);

			spinner.setDigits(fieldConfig.numDigits);
			spinner.setMinimum(fieldConfig.minValue);
			spinner.setMaximum(fieldConfig.maxValue);
			spinner.setPageIncrement(fieldConfig.pageIncrement);

			spinner.setData(filterProperty);
			spinner.setData(FIELD_NO, fieldNo);

			spinner.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onField_Select_Number_Float(event.widget);
				}
			});

			spinner.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					onField_Select_Number_Float(event.widget);
				}
			});

			if (fieldNo == 1) {
				filterProperty.uiSpinner_Number1 = spinner;
			} else {
				filterProperty.uiSpinner_Number2 = spinner;
			}
		}
		{
			/*
			 * Label: Unit
			 */
			if (isShowUnitLabel && fieldConfig.unitLabel != null) {

				// show label

				numColumns++;

				final Label label = new Label(parent, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
				label.setText(fieldConfig.unitLabel);
			}
		}

		return numColumns;
	}

	/**
	 * @param parent
	 * @param filterProperty
	 * @param fieldConfig
	 * @param fieldNo
	 * @param isShowUnitLabel
	 * @return
	 */
	private int createUI_Field_Number_Integer(	final Composite parent,
												final TourFilterProperty filterProperty,
												final TourFilterFieldConfig fieldConfig,
												final int fieldNo,
												final boolean isShowUnitLabel) {

		int numColumns = 0;

		{
			/*
			 * Spinner: integer number
			 */
			numColumns++;

			final Spinner spinner = new Spinner(parent, SWT.BORDER);

			spinner.setMinimum(fieldConfig.minValue);
			spinner.setMaximum(fieldConfig.maxValue);
			spinner.setPageIncrement(fieldConfig.pageIncrement);

			spinner.setData(filterProperty);
			spinner.setData(FIELD_NO, fieldNo);

			spinner.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onField_Select_Number_Integer(event.widget);
				}
			});

			spinner.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					onField_Select_Number_Integer(event.widget);
				}
			});

			if (fieldNo == 1) {
				filterProperty.uiSpinner_Number1 = spinner;
			} else {
				filterProperty.uiSpinner_Number2 = spinner;
			}
		}
		{
			/*
			 * Label: Unit
			 */
			if (isShowUnitLabel && fieldConfig.unitLabel != null) {

				// show label
				numColumns++;

				final Label label = new Label(parent, SWT.NONE);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(label);

				label.setText(fieldConfig.unitLabel);
			}
		}

		return numColumns;
	}

	private int createUI_Field_SeasonDay(	final Composite parent,
											final TourFilterProperty filterProperty,
											final int fieldNo) {

		final Spinner spinnerDay = new Spinner(parent, SWT.BORDER);

		spinnerDay.setMinimum(1);
		spinnerDay.setMaximum(31);
		spinnerDay.setPageIncrement(5);

		spinnerDay.setData(filterProperty);
		spinnerDay.setData(FIELD_NO, fieldNo);

		spinnerDay.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onField_Select_SeasonDay(event.widget);
			}
		});

		spinnerDay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onField_Select_SeasonDay(event.widget);
			}
		});

		// ensure that this field is not empty
		if (fieldNo == 1) {

			// select year start
			spinnerDay.setSelection(1);

			filterProperty.uiSpinner_SeasonDay1 = spinnerDay;

		} else {

			// select today
			spinnerDay.setSelection(MonthDay.now().getDayOfMonth());

			filterProperty.uiSpinner_SeasonDay2 = spinnerDay;
		}

		return 1;
	}

	private int createUI_Field_SeasonMonth(	final Composite parent,
											final TourFilterProperty filterProperty,
											final int fieldNo) {

		final Combo comboMonth = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);

		comboMonth.setData(filterProperty);
		comboMonth.setData(FIELD_NO, fieldNo);

		comboMonth.addFocusListener(_keepOpenListener);

		comboMonth.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onField_Select_SeasonMonth(event.widget);
			}
		});

		comboMonth.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onField_Select_SeasonMonth(event.widget);
			}
		});

		for (final String month : DateFormatSymbols.getInstance().getMonths()) {

			// 13 months are returned !!!
			if (month.length() == 0) {
				continue;
			}

			comboMonth.add(month);
		}

		// ensure that this field is not empty
		if (fieldNo == 1) {

			// select year start
			comboMonth.select(0);

			filterProperty.uiCombo_SeasonMonth1 = comboMonth;

		} else {

			// select today
			comboMonth.select(MonthDay.now().getMonthValue() - 1);

			filterProperty.uiCombo_SeasonMonth2 = comboMonth;
		}

		return 1;
	}

	private int createUI_Field_Text(final Composite parent) {

		{
			/*
			 * Link: Fulltext search hint
			 */
			final Link link = new Link(parent, SWT.NONE);
			link.setText(Messages.Slideout_TourFilter_Link_TextSearchHint);
			link.setToolTipText(Messages.Slideout_TourFilter_Link_TextSearchHint_Tooltip);
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					Util.showView(SearchView.ID, true);
				}
			});
		}

		return 1;
	}

	private int createUI_Field_Time(final Composite parent,
									final TourFilterProperty filterProperty,
									final int fieldNo) {

		final DateTime dtTourTime = new DateTime(parent, SWT.TIME | SWT.SHORT | SWT.BORDER);

		dtTourTime.setData(filterProperty);
		dtTourTime.setData(FIELD_NO, fieldNo);

		dtTourTime.addFocusListener(_keepOpenListener);
		dtTourTime.addSelectionListener(_fieldSelectionListener_DateTime);

		GridDataFactory
				.fillDefaults()//
				.align(SWT.END, SWT.CENTER)
				.applyTo(dtTourTime);

		if (fieldNo == 1) {
			filterProperty.uiDateTime1 = dtTourTime;
		} else {
			filterProperty.uiDateTime2 = dtTourTime;
		}

		return 1;
	}

	private int createUI_Operator_And(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Tour_Filter_Operator_And);

		return 1;
	}

	private void doApply() {

		TourFilterManager.fireTourFilterModifyEvent();
	}

	private void doLiveUpdate() {

		_isLiveUpdate = _chkLiveUpdate.getSelection();

		_state.put(STATE_IS_LIVE_UPDATE, _isLiveUpdate);

		enableControls();

		fireModifyEvent();
	}

	private void enableControls() {

		final boolean isProfileSelected = _selectedProfile != null;
		boolean isPropertyAvailable = false;

		if (isProfileSelected) {
			isPropertyAvailable = _selectedProfile.filterProperties.size() > 0;
		}

		_btnActivateAll_No.setEnabled(isProfileSelected && isPropertyAvailable);
		_btnActivateAll_Yes.setEnabled(isProfileSelected && isPropertyAvailable);
		_btnAddProperty.setEnabled(isProfileSelected);
		_btnApply.setEnabled(_isLiveUpdate == false);
		_btnCopyProfile.setEnabled(isProfileSelected);
		_btnDeleteProfile.setEnabled(isProfileSelected);

		_lblProfileName.setEnabled(isProfileSelected);
		_txtProfileName.setEnabled(isProfileSelected);

		// enable property UI controls
		if (isProfileSelected) {

			for (final TourFilterProperty property : _selectedProfile.filterProperties) {

				final boolean isEnabled = property.isEnabled;

				property.comboFieldName.setEnabled(isEnabled);
				property.comboFieldOperator.setEnabled(isEnabled);

				UI.setEnabledForAllChildren(property.fieldDetailContainer, isEnabled);
			}
		}
	}

	private void fireModifyEvent() {

		if (_isLiveUpdate) {
			TourFilterManager.fireTourFilterModifyEvent();
		}
	}

	private int getMonthMaxDays(final int month) {

		switch (month) {
		case 4:
		case 6:
		case 9:
		case 11:
			return 30;

		case 2:
			return 29;

		default:
			return 31;
		}
	}

	@Override
	public Point getToolTipLocation(final Point slideoutSize) {

		// get tooltip position
		final Rectangle itemBounds = _tourFilterItem.getBounds();
		final Point itemDisplayPosition = _ownerToolbar.toDisplay(itemBounds.x, itemBounds.y);

		final int slideoutWidth = slideoutSize.x;
		final int slideoutHeight = slideoutSize.y;

		final int itemWidth = itemBounds.width;
		final int itemHeight = itemBounds.height;

		// center horizontally
		int devX = itemDisplayPosition.x;
		devX += itemWidth / 2 - slideoutWidth / 2;

		int devY = itemDisplayPosition.y + itemHeight;

		final Rectangle displayBounds = Display.getCurrent().getBounds();

		if (isShowAboveToolItem()) {

			devY = itemDisplayPosition.y - slideoutHeight;

			if (devY < 0) {

				// slideout will hide the tool item, show it below

				devY = itemDisplayPosition.y + itemHeight;
			}

		} else if (devY + slideoutHeight > displayBounds.height) {

			// slideout is below bottom, show it above the action button

			devY = itemDisplayPosition.y - slideoutHeight;
		}

		return new Point(devX, devY);
	}

	/**
	 * Ensure that the selected month has also a valid day, {@link MonthDay} shows valid values.
	 * 
	 * @param filterProperty
	 * @param fieldNo
	 * @param selectedMonth
	 * @return
	 */
	private int getValidSeasonDay(final TourFilterProperty filterProperty, final int fieldNo, final int selectedMonth) {

		final Spinner spinnerDay = fieldNo == 1 //
				? filterProperty.uiSpinner_SeasonDay1
				: filterProperty.uiSpinner_SeasonDay2;

		// Spinner day can be null when only the month UI is displayed but not the day UI
		if (spinnerDay == null) {
			return 1;
		}

		final MonthDay oldField = fieldNo == 1 //
				? filterProperty.monthDay1
				: filterProperty.monthDay2;

		int oldDay = oldField.getDayOfMonth();

		final int monthMaxDays = getMonthMaxDays(selectedMonth);

		if (oldDay > monthMaxDays) {

			oldDay = monthMaxDays;

			// update day UI

			spinnerDay.setSelection(monthMaxDays);
		}

		spinnerDay.setMaximum(monthMaxDays);

		return oldDay;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	private boolean isFilterDisposed() {

		if (_filterOuterContainer != null && _filterOuterContainer.isDisposed()) {

			/*
			 * This can happen when a sub dialog was closed and the mouse is outside of the slideout
			 * -> this is closing the slideout
			 */
			return true;
		}

		return false;
	}

	private void onDisposeSlideout() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);
	}

	private void onField_Select_DateTime(final SelectionEvent event) {

		final DateTime dateTime = (DateTime) (event.widget);

		final TourFilterProperty filterProperty = (TourFilterProperty) dateTime.getData();
		final int fieldNo = (int) dateTime.getData(FIELD_NO);

		final LocalDateTime localDateTime = LocalDateTime.of(
				dateTime.getYear(),
				dateTime.getMonth() + 1,
				dateTime.getDay(),
				dateTime.getHours(),
				dateTime.getMinutes());

		if (fieldNo == 1) {
			filterProperty.dateTime1 = localDateTime;
		} else {
			filterProperty.dateTime2 = localDateTime;
		}

		fireModifyEvent();
	}

	private void onField_Select_Duration(final TimeDuration duration, final int durationTime) {

		final TourFilterProperty filterProperty = (TourFilterProperty) duration.getData();
		final int fieldNo = (int) duration.getData(FIELD_NO);

		if (fieldNo == 1) {
			filterProperty.intValue1 = durationTime;
		} else {
			filterProperty.intValue2 = durationTime;
		}

		fireModifyEvent();
	}

	private void onField_Select_Number_Float(final Widget widget) {

		final Spinner spinner = (Spinner) widget;

		final TourFilterProperty filterProperty = (TourFilterProperty) spinner.getData();
		final int fieldNo = (int) spinner.getData(FIELD_NO);

		final double selectedValue = spinner.getSelection();
		final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;

		// remove spinner digits
		double fieldValue = selectedValue / Math.pow(10, fieldConfig.numDigits);

		if (fieldConfig.fieldValueConverter != null) {
			fieldValue = fieldConfig.fieldValueConverter.convert_UI_To_Model(fieldValue);
		}

		if (fieldNo == 1) {
			filterProperty.doubleValue1 = fieldValue;
		} else {
			filterProperty.doubleValue2 = fieldValue;
		}

		fireModifyEvent();
	}

	private void onField_Select_Number_Integer(final Widget widget) {

		final Spinner spinner = (Spinner) widget;

		final TourFilterProperty filterProperty = (TourFilterProperty) spinner.getData();
		final int fieldNo = (int) spinner.getData(FIELD_NO);

		final int selectedValue = spinner.getSelection();
		final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;

		double fieldValue = selectedValue;

		if (fieldConfig.fieldValueConverter != null) {
			fieldValue = fieldConfig.fieldValueConverter.convert_UI_To_Model(fieldValue);
		}

		if (fieldNo == 1) {
			filterProperty.intValue1 = (int) (fieldValue + 0.5);
		} else {
			filterProperty.intValue2 = (int) (fieldValue + 0.5);
		}

		fireModifyEvent();
	}

	private void onField_Select_SeasonDay(final Widget widget) {

		final Spinner spinnerDay = (Spinner) widget;

		final TourFilterProperty filterProperty = (TourFilterProperty) spinnerDay.getData();
		final int fieldNo = (int) spinnerDay.getData(FIELD_NO);

		final int selectedDay = spinnerDay.getSelection();

		final MonthDay oldField = fieldNo == 1 //
				? filterProperty.monthDay1
				: filterProperty.monthDay2;
		final int oldMonth = oldField.getMonthValue();

		final MonthDay monthDay = MonthDay.of(oldMonth, selectedDay);

		if (fieldNo == 1) {
			filterProperty.monthDay1 = monthDay;
		} else {
			filterProperty.monthDay2 = monthDay;
		}

		fireModifyEvent();
	}

	private void onField_Select_SeasonMonth(final Widget widget) {

		final Combo comboMonth = (Combo) widget;

		final TourFilterProperty filterProperty = (TourFilterProperty) comboMonth.getData();
		final int fieldNo = (int) comboMonth.getData(FIELD_NO);

		final int selectedMonth = comboMonth.getSelectionIndex() + 1;

		final int oldDay = getValidSeasonDay(filterProperty, fieldNo, selectedMonth);

		final MonthDay monthDay = MonthDay.of(selectedMonth, oldDay);

		if (fieldNo == 1) {
			filterProperty.monthDay1 = monthDay;
		} else {
			filterProperty.monthDay2 = monthDay;
		}

		fireModifyEvent();
	}

	@Override
	protected void onFocus() {

		if (_selectedProfile != null
				&& _selectedProfile.name != null
				&& _selectedProfile.name.equals(Messages.Tour_Filter_Default_ProfileName)) {

			// default profile is selected, make is easy to rename it

			_txtProfileName.selectAll();
			_txtProfileName.setFocus();

		} else {

			_profileViewer.getTable().setFocus();
		}
	}

	private void onProfile_Add() {

		final TourFilterProfile filterProfile = new TourFilterProfile();

		// update model
		_filterProfiles.add(filterProfile);

		// update viewer
		_profileViewer.refresh();

		// select new profile
		selectProfile(filterProfile);

		_txtProfileName.setFocus();
	}

	private void onProfile_Copy() {

		if (_selectedProfile == null) {
			// ignore
			return;
		}

		final TourFilterProfile filterProfile = _selectedProfile.clone();

		// update model
		_filterProfiles.add(filterProfile);

		// update viewer
		_profileViewer.refresh();

		// select new profile
		selectProfile(filterProfile);

		_txtProfileName.setFocus();
	}

	private void onProfile_Delete() {

		if (_selectedProfile == null) {
			// ignore
			return;
		}

		/*
		 * Confirm deletion
		 */
		boolean isDeleteProfile;
		setIsKeepOpenInternally(true);
		{
			isDeleteProfile = MessageDialog.openConfirm(
					Display.getCurrent().getActiveShell(),
					Messages.Slideout_TourFilter_Confirm_DeleteProfile_Title,
					NLS.bind(Messages.Slideout_TourFilter_Confirm_DeleteProfile_Message, _selectedProfile.name));
		}
		setIsKeepOpenInternally(false);

		if (isDeleteProfile == false) {
			return;
		}

		// keep currently selected position
		final int lastIndex = _profileViewer.getTable().getSelectionIndex();

		// update model
		_filterProfiles.remove(_selectedProfile);
		TourFilterManager.setSelectedProfile(null);

		// update UI
		_profileViewer.remove(_selectedProfile);

		/*
		 * Select another filter at the same position
		 */
		final int numFilters = _filterProfiles.size();
		final int nextFilterIndex = Math.min(numFilters - 1, lastIndex);

		final Object nextSelectedProfile = _profileViewer.getElementAt(nextFilterIndex);
		if (nextSelectedProfile == null) {

			_selectedProfile = null;

			createUI_410_FilterProperties();
			updateUI_Properties();

		} else {

			selectProfile((TourFilterProfile) nextSelectedProfile);
		}

		enableControls();

		// set focus back to the viewer
		_profileViewer.getTable().setFocus();
	}

	private void onProfile_Modify() {

		if (_selectedProfile == null) {
			return;
		}

		final String profileName = _txtProfileName.getText();

		_selectedProfile.name = profileName;

		_profileViewer.refresh();
	}

	private void onProfile_Select() {

		TourFilterProfile selectedProfile = null;

		// get selected profile from viewer
		final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
		final Object firstElement = selection.getFirstElement();
		if (firstElement != null) {
			selectedProfile = (TourFilterProfile) firstElement;
		}

		if (_selectedProfile != null && _selectedProfile == selectedProfile) {
			// a new profile is not selected
			return;
		}

		_selectedProfile = selectedProfile;

		// update model
		TourFilterManager.setSelectedProfile(_selectedProfile);

		// update UI
		if (_selectedProfile == null) {

			_txtProfileName.setText(UI.EMPTY_STRING);

		} else {

			_txtProfileName.setText(_selectedProfile.name);

			if (_selectedProfile.name.equals(Messages.Tour_Filter_Default_ProfileName)) {

				// a default profile is selected, make is easy to rename it

				_txtProfileName.selectAll();
				_txtProfileName.setFocus();
			}
		}

		createUI_410_FilterProperties();
		updateUI_Properties();

		fireModifyEvent();
	}

	/**
	 * Activate/deactivate all properties for the selected profile.
	 * 
	 * @param isActivate
	 */
	private void onProperty_ActivateAll(final boolean isActivate) {

		if (_selectedProfile == null) {
			return;
		}

		for (final TourFilterProperty property : _selectedProfile.filterProperties) {
			property.isEnabled = isActivate;
		}

		updateUI_Properties();
		fireModifyEvent();
	}

	private void onProperty_Add() {

		final TourFilterProperty filterProperty = new TourFilterProperty();
		final ArrayList<TourFilterProperty> filterProperties = _selectedProfile.filterProperties;

		// update model
		filterProperties.add(filterProperty);

		// update UI
		createUI_410_FilterProperties();
		updateUI_Properties();

		// update number of properties
		_profileViewer.update(_selectedProfile, null);

		// set focus to the last created property
		final TourFilterProperty lastProperty = filterProperties.get(filterProperties.size() - 1);
		lastProperty.checkboxIsPropertyEnabled.setFocus();

		fireModifyEvent();
	}

	private void onProperty_SelectEnabled(final Widget widget) {

		final TourFilterProperty filterProperty = (TourFilterProperty) widget.getData();

		final boolean isFieldEnabled = ((Button) (widget)).getSelection();

		filterProperty.isEnabled = isFieldEnabled;

		enableControls();

		fireModifyEvent();
	}

	private void onProperty_SelectField(final Widget widget) {

		final Combo comboFilterField = (Combo) (widget);

		// get selected field config
		final int selectedFilterConfigIndex = comboFilterField.getSelectionIndex();
		final TourFilterFieldConfig selectedFieldConfig =
				TourFilterManager.FILTER_FIELD_CONFIG[selectedFilterConfigIndex];

		if (selectedFieldConfig.fieldType == TourFilterFieldType.CATEGORY) {

			// a category is selected, select default field in category

			final int defaultIndex = TourFilterManager.getFilterFieldIndex(selectedFieldConfig.categoryDefaultFieldId);

			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {

					// select default in UI
					comboFilterField.select(defaultIndex);

					// get default config
					final TourFilterFieldConfig newSelectedFieldConfig =
							TourFilterManager.FILTER_FIELD_CONFIG[defaultIndex];

					onProperty_SelectField_Update(widget, newSelectedFieldConfig);
				}
			});

		} else {

			onProperty_SelectField_Update(widget, selectedFieldConfig);
		}
	}

	private void onProperty_SelectField_Update(final Widget widget, final TourFilterFieldConfig selectedFieldConfig) {

		// setup filter property with a new config
		final TourFilterProperty filterProperty = (TourFilterProperty) widget.getData();
		filterProperty.fieldConfig = selectedFieldConfig;

		// select default operator
		filterProperty.fieldOperator = selectedFieldConfig.getDefaultFieldOperator();

		updateUI_Properties();

		fireModifyEvent();
	}

	private void onProperty_SelectOperator(final Widget widget) {

		final TourFilterProperty filterProperty = (TourFilterProperty) widget.getData();

		final int selectedIndex = ((Combo) (widget)).getSelectionIndex();

		filterProperty.fieldOperator = TourFilterManager.getFieldOperator(//
				filterProperty.fieldConfig.fieldId,
				selectedIndex);

		updateUI_Properties();

		fireModifyEvent();
	}

	private void onResizeFilterContent() {

		if (_filterScrolled_Content == null || _filterScrolled_Content.isDisposed()) {
			return;
		}

		final Point contentSize = _filterScrolled_Content.computeSize(SWT.DEFAULT, SWT.DEFAULT);

		_filterScrolled_Container.setMinSize(contentSize);
	}

	private void restoreState() {

		/*
		 * Get previous selected profile
		 */
		TourFilterProfile selectedProfile = TourFilterManager.getSelectedProfile();

		if (selectedProfile == null) {

			// select first profile

			selectedProfile = (TourFilterProfile) _profileViewer.getElementAt(0);
		}

		if (selectedProfile != null) {
			selectProfile(selectedProfile);
		}

		/*
		 * Other states
		 */
		_isLiveUpdate = Util.getStateBoolean(_state, STATE_IS_LIVE_UPDATE, false);
		_chkLiveUpdate.setSelection(_isLiveUpdate);

		// restore width for the profile list
		final int leftPartWidth = Util.getStateInt(_state, STATE_SASH_WIDTH, _pc.convertWidthInCharsToPixels(50));
		_sashForm.setViewerWidth(leftPartWidth);
	}

	private void selectProfile(final TourFilterProfile selectedProfile) {

		_profileViewer.setSelection(new StructuredSelection(selectedProfile));

		final Table table = _profileViewer.getTable();
		table.setSelection(table.getSelectionIndices());
	}

	private void updateUI_Properties() {

		if (_selectedProfile == null || isFilterDisposed()) {
			return;
		}

		_filterOuterContainer.setRedraw(false);
		{
			final ArrayList<TourFilterProperty> filterProperties = _selectedProfile.filterProperties;

			for (int properyIndex = 0; properyIndex < filterProperties.size(); properyIndex++) {

				final TourFilterProperty filterProperty = filterProperties.get(properyIndex);
				final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
				final TourFilterFieldOperator fieldOperator = filterProperty.fieldOperator;

				final TourFilterFieldId filterField = fieldConfig.fieldId;

				final Combo comboFilterField = filterProperty.comboFieldName;
				final Combo comboFieldOperator = filterProperty.comboFieldOperator;

				createUI_430_FieldDetail(filterProperty);

				// fill field combo with all available fields
				comboFilterField.removeAll();
				for (final TourFilterFieldConfig fieldConfigTemplate : TourFilterManager.FILTER_FIELD_CONFIG) {
					comboFilterField.add(fieldConfigTemplate.name);
				}

				// fill operator combo
				comboFieldOperator.removeAll();
				final TourFilterFieldOperator[] fieldOperators = TourFilterManager.getFieldOperators(filterField);
				for (final TourFilterFieldOperator filterOperator : fieldOperators) {
					comboFieldOperator.add(TourFilterManager.getFieldOperatorName(filterOperator));
				}

				final int fieldIndex = TourFilterManager.getFilterFieldIndex(filterField);
				final int operatorIndex = TourFilterManager.getFieldOperatorIndex(filterField, fieldOperator);

				comboFilterField.select(fieldIndex);
				comboFieldOperator.select(operatorIndex);

				filterProperty.checkboxIsPropertyEnabled.setSelection(filterProperty.isEnabled);

				updateUI_PropertyDetail(filterProperty);
			}

			// it took a while to manage the scrollbar when the content has changed
			_filterOuterContainer.layout(true, true);
			_containerFilter.layout(true, true);
			UI.setEqualizeColumWidths(_firstColumnControls, 0);
			_containerFilter.layout(true, true);

			onResizeFilterContent();
		}
		_filterOuterContainer.setRedraw(true);

		enableControls();

//		Util.dumpTabList(_shellContainer, 0);
	}

	private void updateUI_PropertyDetail(final TourFilterProperty filterProperty) {

		final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
		final TourFilterFieldType fieldType = fieldConfig.fieldType;

		switch (filterProperty.fieldOperator) {
		case GREATER_THAN:
		case GREATER_THAN_OR_EQUAL:
		case LESS_THAN:
		case LESS_THAN_OR_EQUAL:
		case EQUALS:
		case NOT_EQUALS:

			switch (fieldType) {
			case DATE:
				updateUI_PropertyDetail_Date(filterProperty, 1);
				break;

			case TIME:
				updateUI_PropertyDetail_Time(filterProperty, 1);
				break;

			case DURATION:
				updateUI_PropertyDetail_Duration(filterProperty, 1);
				break;

			case NUMBER_INTEGER:
				updateUI_PropertyDetail_Number_Integer(filterProperty, 1);
				break;

			case NUMBER_FLOAT:
				updateUI_PropertyDetail_Number_Float(filterProperty, 1);
				break;

			case TEXT:
				break;

			case SEASON:
				break;
			}

			break;

		case BETWEEN:
		case NOT_BETWEEN:

			switch (fieldType) {
			case DATE:
				updateUI_PropertyDetail_Date(filterProperty, 1);
				updateUI_PropertyDetail_Date(filterProperty, 2);
				break;

			case TIME:
				updateUI_PropertyDetail_Time(filterProperty, 1);
				updateUI_PropertyDetail_Time(filterProperty, 2);
				break;

			case DURATION:
				updateUI_PropertyDetail_Duration(filterProperty, 1);
				updateUI_PropertyDetail_Duration(filterProperty, 2);
				break;

			case NUMBER_INTEGER:
				updateUI_PropertyDetail_Number_Integer(filterProperty, 1);
				updateUI_PropertyDetail_Number_Integer(filterProperty, 2);
				break;

			case NUMBER_FLOAT:
				updateUI_PropertyDetail_Number_Float(filterProperty, 1);
				updateUI_PropertyDetail_Number_Float(filterProperty, 2);
				break;

			case TEXT:
				break;

			case SEASON:
				updateUI_PropertyDetail_Season(filterProperty, 1);
				updateUI_PropertyDetail_Season(filterProperty, 2);
				break;
			}

			break;

		case STARTS_WITH:
			break;
		case ENDS_WITH:
			break;

		case INCLUDE_ANY:
			break;
		case EXCLUDE_ALL:
			break;

		case IS_EMPTY:
			break;
		case IS_NOT_EMPTY:
			break;

		case LIKE:
			break;
		case NOT_LIKE:
			break;

		case SEASON_UNTIL_TODAY_FROM_YEAR_START:
		case SEASON_TODAY_UNTIL_YEAR_END:
		case SEASON_CURRENT_DAY:
		case SEASON_CURRENT_MONTH:
			// no additional controls
			break;

		case SEASON_MONTH:
			updateUI_PropertyDetail_Season_Month(filterProperty);
			break;

		case SEASON_UNTIL_TODAY_FROM_DATE:
		case SEASON_TODAY_UNTIL_DATE:
			updateUI_PropertyDetail_Season(filterProperty, 1);
			break;
		}
	}

	private void updateUI_PropertyDetail_Date(final TourFilterProperty filterProperty, final int fieldNo) {

		DateTime uiDateTime;
		LocalDateTime dateTime;

		if (fieldNo == 1) {

			uiDateTime = filterProperty.uiDateTime1;
			dateTime = filterProperty.dateTime1;

		} else {

			uiDateTime = filterProperty.uiDateTime2;
			dateTime = filterProperty.dateTime2;
		}

		uiDateTime.setYear(dateTime.getYear());
		uiDateTime.setMonth(dateTime.getMonthValue() - 1);
		uiDateTime.setDay(dateTime.getDayOfMonth());
	}

	private void updateUI_PropertyDetail_Duration(final TourFilterProperty filterProperty, final int fieldNo) {

		if (fieldNo == 1) {

			filterProperty.uiDuration1.setTime(filterProperty.intValue1);

		} else {

			filterProperty.uiDuration2.setTime(filterProperty.intValue2);
		}
	}

	private void updateUI_PropertyDetail_Number_Float(final TourFilterProperty filterProperty, final int fieldNo) {

		final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
		final FieldValueConverter fieldValueProvider = fieldConfig.fieldValueConverter;
		final int valueDigits = fieldConfig.numDigits;

		Spinner spinner;
		double modelValue;

		if (fieldNo == 1) {
			modelValue = filterProperty.doubleValue1;
			spinner = filterProperty.uiSpinner_Number1;
		} else {
			modelValue = filterProperty.doubleValue2;
			spinner = filterProperty.uiSpinner_Number2;
		}

		double uiValue = modelValue;

		if (fieldValueProvider != null) {
			uiValue = fieldValueProvider.convert_Model_To_UI(modelValue);
		}

		// add spinner digits
		final int spinnerValue = (int) (uiValue * Math.pow(10, valueDigits));

		spinner.setSelection(spinnerValue);

		spinner.setDigits(valueDigits);
		spinner.setMinimum(fieldConfig.minValue);
		spinner.setMaximum(fieldConfig.maxValue);
		spinner.setPageIncrement(fieldConfig.pageIncrement);
	}

	private void updateUI_PropertyDetail_Number_Integer(final TourFilterProperty filterProperty, final int fieldNo) {

		final TourFilterFieldConfig fieldConfig = filterProperty.fieldConfig;
		final FieldValueConverter fieldValueProvider = fieldConfig.fieldValueConverter;

		Spinner spinner;
		double modelValue;

		if (fieldNo == 1) {
			modelValue = filterProperty.intValue1;
			spinner = filterProperty.uiSpinner_Number1;
		} else {
			modelValue = filterProperty.intValue2;
			spinner = filterProperty.uiSpinner_Number2;
		}

		double uiValue = modelValue;

		if (fieldValueProvider != null) {
			uiValue = fieldValueProvider.convert_Model_To_UI(modelValue);
		}

		spinner.setSelection((int) (uiValue + 0.5));

		spinner.setMinimum(fieldConfig.minValue);
		spinner.setMaximum(fieldConfig.maxValue);
		spinner.setPageIncrement(fieldConfig.pageIncrement);
	}

	private void updateUI_PropertyDetail_Season(final TourFilterProperty filterProperty, final int fieldNo) {

		MonthDay monthDay;

		Combo uiComboMonth;
		Spinner uiSpinnerDay;

		if (fieldNo == 1) {

			monthDay = filterProperty.monthDay1;

			uiComboMonth = filterProperty.uiCombo_SeasonMonth1;
			uiSpinnerDay = filterProperty.uiSpinner_SeasonDay1;

		} else {

			monthDay = filterProperty.monthDay2;

			uiComboMonth = filterProperty.uiCombo_SeasonMonth2;
			uiSpinnerDay = filterProperty.uiSpinner_SeasonDay2;
		}

		uiSpinnerDay.setSelection(monthDay.getDayOfMonth());
		uiComboMonth.select(monthDay.getMonthValue() - 1);
	}

	private void updateUI_PropertyDetail_Season_Month(final TourFilterProperty filterProperty) {

		MonthDay monthDay;
		Combo uiComboMonth;

		monthDay = filterProperty.monthDay1;
		uiComboMonth = filterProperty.uiCombo_SeasonMonth1;

		uiComboMonth.select(monthDay.getMonthValue() - 1);
	}

	private void updateUI_PropertyDetail_Time(final TourFilterProperty filterProperty, final int fieldNo) {

		DateTime uiDateTime;
		LocalDateTime dateTime;

		if (fieldNo == 1) {

			uiDateTime = filterProperty.uiDateTime1;
			dateTime = filterProperty.dateTime1;

		} else {

			uiDateTime = filterProperty.uiDateTime2;
			dateTime = filterProperty.dateTime2;
		}

		uiDateTime.setHours(dateTime.getHour());
		uiDateTime.setMinutes(dateTime.getMinute());
	}

}
