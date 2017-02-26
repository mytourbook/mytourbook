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
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ToolbarSlideout;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
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
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourFilter extends ToolbarSlideout {

//	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private static final String	FIELD_NO	= "fieldNo";			//$NON-NLS-1$

	private ModifyListener		_defaultModifyListener;
	private FocusListener		_keepOpenListener;

	private SelectionAdapter	_fieldSelectionListener_DateTime;

	private boolean				_isUpdateUI;

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
				setIsAnotherDialogOpened(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsAnotherDialogOpened(false);
			}
		};
	}

	private PixelConverter						_pc;

	private Action_Profile_Add					_actionProfile_Add;
//	private Action_CopyProfile					_actionProfile_Copy;
	private Action_Profile_Delete				_actionProfile_Delete;
	private Action_Property_Add					_actionProperty_Add;

	private TableViewer							_profileViewer;

	private final ArrayList<TourFilterProfile>	_filterProfiles	= TourFilterManager.getProfiles();
	private TourFilterProfile					_selectedProfile;

	/*
	 * UI controls
	 */
	private Composite							_filterOuterContainer;

	private ScrolledComposite					_filterScrolledContainer;

	private Label								_lblProfileName;

	private Text								_txtProfileName;

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

	public SlideoutTourFilter(final Control ownerControl, final ToolBar toolBar) {

		super(ownerControl, toolBar);
	}

	void action_ProfileAdd() {

		final TourFilterProfile filterProfile = new TourFilterProfile();

		// update model
		_filterProfiles.add(filterProfile);

		// update viewer
		_profileViewer.refresh();

		// select new profile
		selectProfile(filterProfile);

		_txtProfileName.setFocus();
	}

	void action_ProfileCopy() {

	}

	void action_ProfileDelete() {
		onProfile_DeleteSelected();
	}

	void action_PropertyAdd() {

		final TourFilterProperty filterProperty = new TourFilterProperty();

		// update model
		_selectedProfile.filterProperties.add(filterProperty);

		// update UI
		createUI_410_FilterProperties();
		updateUI_Properties();
	}

	void action_PropertyDelete(final TourFilterProperty filterProperty) {

		/*
		 * Confirm deletion
		 */
		boolean isDeleteProfile;
		setIsAnotherDialogOpened(true);
		{
			isDeleteProfile = MessageDialog.openConfirm(

					Display.getCurrent().getActiveShell(),
					Messages.Slideout_TourFilter_Confirm_DeleteProperty_Title,
					NLS.bind(
							Messages.Slideout_TourFilter_Confirm_DeleteProperty_Message,
							filterProperty.fieldConfig.name));
		}
		setIsAnotherDialogOpened(false);

		if (isDeleteProfile == false) {
			return;
		}

		// update model
		final ArrayList<TourFilterProperty> filterProperties = _selectedProfile.filterProperties;
		filterProperties.remove(filterProperty);

		// update UI
		createUI_410_FilterProperties();
		updateUI_Properties();
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

	@Override
	protected void beforeHideToolTip() {}

	private void createActions() {

		_actionProfile_Add = new Action_Profile_Add(this);
//		_actionProfile_Copy = new Action_CopyProfile(this);
		_actionProfile_Delete = new Action_Profile_Delete(this);

		_actionProperty_Add = new Action_Property_Add(this);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		/*
		 * Reset to a valid state when the slideout is opened again
		 */
		_selectedProfile = null;

		initUI(parent);
		createActions();

		final Composite ui = createUI(parent);

		// load viewer
		_profileViewer.setInput(new Object());

		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			createUI_100_Title(shellContainer);

			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
			{
				createUI_200_Profiles(container);

				final Composite filterContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(filterContainer);
				GridLayoutFactory.fillDefaults().numColumns(1).applyTo(filterContainer);
				{
					createUI_300_FilterInfo(filterContainer);
					createUI_400_FilterOuterContainer(filterContainer);
					createUI_500_FilterActions(filterContainer);
				}
			}
		}

		return shellContainer;
	}

	private void createUI_100_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_TourFilter_Label_Title);
		label.setFont(JFaceResources.getBannerFont());
	}

	private void createUI_200_Profiles(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
//				.grab(true, true)
				.hint(_pc.convertWidthInCharsToPixels(20), _pc.convertHeightInCharsToPixels(15))
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.spacing(0, 2)
				.applyTo(container);
		{
			createUI_210_ProfileViewer(container);

			{
				/*
				 * Toolbar: Profile actions
				 */
				final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

				final ToolBarManager tbm = new ToolBarManager(toolbar);

				tbm.add(_actionProfile_Add);
//				tbm.add(_actionProfile_Copy);
				tbm.add(_actionProfile_Delete);

				tbm.update(true);
			}
		}
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

		// !!! this prevents that the horizontal scrollbar is displayed !!!
		table.setHeaderVisible(false);

		_profileViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tc;

		// column: map provider
		tvc = new TableViewerColumn(_profileViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Slideout_TourFilter_Column_ProfileName);
//		tc.setResizable(false);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourFilterProfile profile = (TourFilterProfile) cell.getElement();

				cell.setText(profile.name);
			}
		});
		tableLayout.setColumnData(tc, new ColumnWeightData(1, false));

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
					onProfile_DeleteSelected();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});
	}

	private void createUI_300_FilterInfo(final Composite parent) {

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
//						.grab(true, false)
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
				.hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(10))
				.applyTo(_filterOuterContainer);
		{
//			createUI_410_FilterContent();
		}
	}

	/**
	 * Create the filter fields from the selected profile.
	 */
	private void createUI_410_FilterProperties() {

		// ensure that this is reseted
		setIsAnotherDialogOpened(false);

		if (isFilterDisposed()) {
			return;
		}

		Point scrollOrigin = null;

		// dispose previous content
		if (_filterScrolledContainer != null && !_filterScrolledContainer.isDisposed()) {

			// get current scroll position
			scrollOrigin = _filterScrolledContainer.getOrigin();

			_filterScrolledContainer.dispose();
		}

		if (_selectedProfile == null) {
			return;
		}

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

		final Composite parent = _filterOuterContainer;

		final Composite filterContainer = createUI_420_FilterScrolledContainer(parent);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(5)
				.applyTo(filterContainer);
//		filterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		filterContainer.setRedraw(false);
		{
			for (int propertyIndex = 0; propertyIndex < numProperties; propertyIndex++) {

				final TourFilterProperty filterProperty = filterProperties.get(propertyIndex);

				{
					/*
					 * checkbox: show vertical grid
					 */
					final Button chkIsFieldEnabled = new Button(filterContainer, SWT.CHECK);
					chkIsFieldEnabled.setData(filterProperty);

					chkIsFieldEnabled.setText(String.format("&%d", propertyIndex + 1));//$NON-NLS-1$
					chkIsFieldEnabled.setToolTipText(Messages.Slideout_TourFilter_Checkbox_IsPropertyEnabled_Tooltip);
					chkIsFieldEnabled.addSelectionListener(enabledListener);

					filterProperty.checkboxIsEnabled = chkIsFieldEnabled;
				}
				{
					/*
					 * Combo: Filter field
					 */
					final Combo comboFilterField = new Combo(filterContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
					comboFilterField.setData(filterProperty);
					comboFilterField.addFocusListener(_keepOpenListener);
					comboFilterField.addSelectionListener(fieldListener);

					// keep combo reference
					filterProperty.comboFieldName = comboFilterField;
				}
				{
					/*
					 * Combo: Field operator
					 */
					final Combo comboFieldOperator = new Combo(filterContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
					comboFieldOperator.setData(filterProperty);
					comboFieldOperator.addFocusListener(_keepOpenListener);
					comboFieldOperator.addSelectionListener(operatorListener);

					// keep combo reference
					filterProperty.comboFieldOperator = comboFieldOperator;
				}

				{
					/*
					 * Container: Field details
					 */
					final Composite fieldDetailOuterContainer = new Composite(filterContainer, SWT.NONE);
					GridDataFactory.fillDefaults().grab(true, false).applyTo(fieldDetailOuterContainer);
					GridLayoutFactory.fillDefaults().numColumns(1).applyTo(fieldDetailOuterContainer);

					filterProperty.fieldDetailOuterContainer = fieldDetailOuterContainer;
				}

				{
					/*
					 * Toolbar: Property actions
					 */
					createUI_418_PropertyActions(filterContainer, filterProperty, propertyIndex, numProperties);
				}
			}
		}
		filterContainer.setRedraw(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_filterScrolledContainer.setOrigin(scrollOrigin);
		}

		// set focus back to the slideout
//		_txtProfileName.setFocus();
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

	private Composite createUI_420_FilterScrolledContainer(final Composite parent) {

		// scrolled container
		_filterScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_filterScrolledContainer);
		_filterScrolledContainer.setExpandVertical(true);
		_filterScrolledContainer.setExpandHorizontal(true);

		// properties container
		final Composite filterContainer = new Composite(_filterScrolledContainer, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
//				.grab(true, true)
				.applyTo(filterContainer);

		_filterScrolledContainer.setContent(filterContainer);
		_filterScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_filterScrolledContainer.setMinSize(filterContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return filterContainer;
	}

	private void createUI_430_FieldDetail(final TourFilterProperty filterProperty) {

		// remove previous content
		filterProperty.disposeFieldInnerContainer();

		final Composite fieldOuterContainer = filterProperty.fieldDetailOuterContainer;

		fieldOuterContainer.setRedraw(false);
		{
			final Composite container = new Composite(fieldOuterContainer, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

			// default for number of columns is 1
			int numColumns = 1;

			{
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

						filterProperty.uiDateTime1 = createUI_Field_Date(container, filterProperty, 1);

						break;

					case TIME:

						filterProperty.uiDateTime1 = createUI_Field_Time(container, filterProperty, 1);

						break;

					case NUMBER:

						filterProperty.uiSpinner_Number1 = createUI_Field_Number(
								container,
								filterProperty,
								1,
								fieldConfig.minValue,
								fieldConfig.maxValue,
								fieldConfig.pageIncrement);

						break;

					case TEXT:

						break;

					case SEASON:

						numColumns = 2;

						filterProperty.uiSpinner_SeasonDay1 = createUI_Field_SeasonDay(container, filterProperty, 1);
						filterProperty.uiCombo_SeasonMonth1 = createUI_Field_SeasonMonth(container, filterProperty, 1);

						break;
					}

					break;

				case BETWEEN:
				case NOT_BETWEEN:

					switch (fieldType) {
					case DATE:

						numColumns = 3;

						filterProperty.uiDateTime1 = createUI_Field_Date(container, filterProperty, 1);
						createUI_Operator_And(container);
						filterProperty.uiDateTime2 = createUI_Field_Date(container, filterProperty, 2);

						break;

					case TIME:

						numColumns = 3;

						filterProperty.uiDateTime1 = createUI_Field_Time(container, filterProperty, 1);
						createUI_Operator_And(container);
						filterProperty.uiDateTime2 = createUI_Field_Time(container, filterProperty, 2);

						break;

					case NUMBER:

						break;

					case TEXT:

						break;

					case SEASON:

						numColumns = 5;

						filterProperty.uiSpinner_SeasonDay1 = createUI_Field_SeasonDay(container, filterProperty, 1);
						filterProperty.uiCombo_SeasonMonth1 = createUI_Field_SeasonMonth(container, filterProperty, 1);
						createUI_Operator_And(container);
						filterProperty.uiSpinner_SeasonDay2 = createUI_Field_SeasonDay(container, filterProperty, 2);
						filterProperty.uiCombo_SeasonMonth2 = createUI_Field_SeasonMonth(container, filterProperty, 2);

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
				}
			}

			GridLayoutFactory.fillDefaults().numColumns(numColumns).applyTo(container);
		}
		fieldOuterContainer.setRedraw(true);
		fieldOuterContainer.layout(true);
	}

	private void createUI_500_FilterActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			{
				/*
				 * Toolbar: Property actions
				 */
				final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

				final ToolBarManager tbm = new ToolBarManager(toolbar);

				tbm.add(_actionProperty_Add);

				tbm.update(true);
			}
		}
	}

	private DateTime createUI_Field_Date(	final Composite parent,
											final TourFilterProperty filterProperty,
											final int fieldNo) {

		final DateTime dtTourDate = new DateTime(parent, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);

		dtTourDate.setData(filterProperty);
		dtTourDate.setData(FIELD_NO, fieldNo);

		dtTourDate.addFocusListener(_keepOpenListener);
		dtTourDate.addSelectionListener(_fieldSelectionListener_DateTime);

		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(dtTourDate);

		return dtTourDate;
	}

	private Spinner createUI_Field_Number(	final Composite container,
											final TourFilterProperty filterProperty,
											final int fieldNo,
											final int minValue,
											final int maxValue,
											final int pageIncrement) {

		final Spinner spinner = new Spinner(container, SWT.BORDER);

		spinner.setMinimum(minValue);
		spinner.setMaximum(maxValue);
		spinner.setPageIncrement(pageIncrement);

		spinner.setData(filterProperty);
		spinner.setData(FIELD_NO, fieldNo);

		spinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
//				onField_Select_SeasonDay(event.widget);
			}
		});

		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
//				onField_Select_SeasonDay(event.widget);
			}
		});

		return spinner;
	}

	private Spinner createUI_Field_SeasonDay(	final Composite container,
												final TourFilterProperty filterProperty,
												final int fieldNo) {

		final Spinner spinnerDay = new Spinner(container, SWT.BORDER);

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
		} else {
			// select today
			spinnerDay.setSelection(MonthDay.now().getDayOfMonth());
		}

		return spinnerDay;
	}

	private Combo createUI_Field_SeasonMonth(	final Composite container,
												final TourFilterProperty filterProperty,
												final int fieldNo) {

		final Combo comboMonth = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);

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
			comboMonth.add(month);
		}

		// ensure that this field is not empty
		if (fieldNo == 1) {
			// select year start
			comboMonth.select(0);
		} else {
			// select today
			comboMonth.select(MonthDay.now().getMonthValue() - 1);
		}

		return comboMonth;
	}

	private DateTime createUI_Field_Time(	final Composite parent,
											final TourFilterProperty filterProperty,
											final int fieldNo) {

		final DateTime dtTourTime = new DateTime(parent, SWT.TIME | SWT.SHORT | SWT.BORDER);

		dtTourTime.setData(filterProperty);
		dtTourTime.setData(FIELD_NO, fieldNo);

		dtTourTime.addFocusListener(_keepOpenListener);
		dtTourTime.addSelectionListener(_fieldSelectionListener_DateTime);

		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(dtTourTime);

		return dtTourTime;
	}

	private void createUI_Operator_And(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Tour_Filter_Operator_And);
	}

	private void enableControls() {

		final boolean isProfileSelected = _selectedProfile != null;

		_actionProfile_Delete.setEnabled(isProfileSelected);
		_actionProperty_Add.setEnabled(isProfileSelected);

		_lblProfileName.setEnabled(isProfileSelected);
		_txtProfileName.setEnabled(isProfileSelected);

		// enable property UI controls
		if (isProfileSelected) {

			for (final TourFilterProperty property : _selectedProfile.filterProperties) {

				final boolean isEnabled = property.isEnabled;

				property.comboFieldName.setEnabled(isEnabled);
				property.comboFieldOperator.setEnabled(isEnabled);

				UI.setEnabledForAllChildren(property.fieldDetailOuterContainer, isEnabled);
			}
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

	/**
	 * Ensure that the selected month has also a valid day, {@link MonthDay} shows valid values.
	 * 
	 * @param filterProperty
	 * @param fieldNo
	 * @param selectedMonth
	 * @return
	 */
	private int getValidSeasonDay(final TourFilterProperty filterProperty, final int fieldNo, final int selectedMonth) {

		final MonthDay oldField = fieldNo == 1 //
				? filterProperty.monthDay1
				: filterProperty.monthDay2;

		int oldDay = oldField.getDayOfMonth();

		final int monthMaxDays = getMonthMaxDays(selectedMonth);

		final Spinner spinnerDay = fieldNo == 1 //
				? filterProperty.uiSpinner_SeasonDay1
				: filterProperty.uiSpinner_SeasonDay2;

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

	@Override
	protected boolean isCenterHorizontal() {
		return true;
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
	}

	private void onProfile_DeleteSelected() {

		if (_selectedProfile == null) {
			// ignore
			return;
		}

		/*
		 * Confirm deletion
		 */
		boolean isDeleteProfile;
		setIsAnotherDialogOpened(true);
		{
			isDeleteProfile = MessageDialog.openConfirm(
					Display.getCurrent().getActiveShell(),
					Messages.Slideout_TourFilter_Confirm_DeleteProfile_Title,
					NLS.bind(Messages.Slideout_TourFilter_Confirm_DeleteProfile_Message, _selectedProfile.name));
		}
		setIsAnotherDialogOpened(false);

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
			enableControls();

		} else {

			selectProfile((TourFilterProfile) nextSelectedProfile);
		}
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
		}

		createUI_410_FilterProperties();
		updateUI_Properties();
	}

	private void onProperty_SelectEnabled(final Widget widget) {

		final TourFilterProperty filterProperty = (TourFilterProperty) widget.getData();

		final boolean isFieldEnabled = ((Button) (widget)).getSelection();

		filterProperty.isEnabled = isFieldEnabled;

		enableControls();
	}

	private void onProperty_SelectField(final Widget widget) {

		final TourFilterProperty filterProperty = (TourFilterProperty) widget.getData();

		final int selectedFilterConfigIndex = ((Combo) (widget)).getSelectionIndex();

		filterProperty.fieldConfig = TourFilterManager.FILTER_FIELD_CONFIG[selectedFilterConfigIndex];

		updateUI_Properties();
	}

	private void onProperty_SelectOperator(final Widget widget) {

		final TourFilterProperty filterProperty = (TourFilterProperty) widget.getData();

		final int selectedIndex = ((Combo) (widget)).getSelectionIndex();

		filterProperty.fieldOperator = TourFilterManager.getFieldOperator(//
				filterProperty.fieldConfig.fieldId,
				selectedIndex);

		updateUI_Properties();
	}

	private void restoreState() {

		// get previous selected profile
		TourFilterProfile selectedProfile = TourFilterManager.getSelectedProfile();

		if (selectedProfile == null) {

			// select first profile

			selectedProfile = (TourFilterProfile) _profileViewer.getElementAt(0);
		}

		if (selectedProfile != null) {
			selectProfile(selectedProfile);
		}
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
			for (final TourFilterFieldConfig filterTemplate : TourFilterManager.FILTER_FIELD_CONFIG) {
				comboFilterField.add(filterTemplate.name);
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

			filterProperty.checkboxIsEnabled.setSelection(filterProperty.isEnabled);
		}

		_filterOuterContainer.layout(true);

		enableControls();

		final Shell shell = _filterOuterContainer.getShell();
		shell.pack(true);
	}

}
