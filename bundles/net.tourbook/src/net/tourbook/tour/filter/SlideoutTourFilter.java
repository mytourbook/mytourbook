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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ToolbarSlideout;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourFilter extends ToolbarSlideout {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionListener;
	private ModifyListener			_defaultModifyListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private FocusListener			_keepOpenListener;

	private boolean					_isUpdateUI;

	{
		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};

		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onProfile_Modify();
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
		createUI_410_FilterContent();
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
							TourFilterManager.getFilterFieldName(filterProperty.filterField)));
		}
		setIsAnotherDialogOpened(false);

		if (isDeleteProfile == false) {
			return;
		}

		// update model
		final ArrayList<TourFilterProperty> filterProperties = _selectedProfile.filterProperties;
		filterProperties.remove(filterProperty);

		// update UI
		createUI_410_FilterContent();
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
					createUI_400_Filter(filterContainer);
					createUI_800_FilterActions(filterContainer);
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
						.grab(true, false)
						.hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
						.applyTo(_txtProfileName);
			}
		}
	}

	private void createUI_400_Filter(final Composite parent) {

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
	private void createUI_410_FilterContent() {

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

		final Composite parent = _filterOuterContainer;
		final Display display = parent.getDisplay();

		final Composite filterContainer = createUI_420_FilterScrolledContainer(parent);

		/*
		 * Field listener
		 */
//		final MouseAdapter colorMouseListener = new MouseAdapter() {
//			@Override
//			public void mouseDown(final MouseEvent e) {
//				onFieldMouseDown(display, e);
//			}
//		};
//
//		// value listener
//		final SelectionListener valueSelectionListener = new SelectionAdapter() {
//			@Override
//			public void widgetSelected(final SelectionEvent event) {
//				onFieldSelectValue(event.widget);
//			}
//		};
//		final MouseWheelListener valueMouseWheelListener = new MouseWheelListener() {
//			@Override
//			public void mouseScrolled(final MouseEvent event) {
//				UI.adjustSpinnerValueOnMouseScroll(event);
//				onFieldSelectValue(event.widget);
//			}
//		};
//
//		final SelectionListener prontoListener = new SelectionAdapter() {
//			@Override
//			public void widgetSelected(final SelectionEvent event) {
//				onFieldSelectPronto(event.widget);
//			}
//		};

		final SelectionListener filterFieldSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onProperty_SelectField(e);
			}

		};

		filterContainer.setRedraw(false);
		{
			for (int propertyIndex = 0; propertyIndex < numProperties; propertyIndex++) {

				final TourFilterProperty filterProperty = filterProperties.get(propertyIndex);

				Combo comboFilterField;
				Composite propertyContainer;

				{
					/*
					 * Combo: Filter field
					 */
					comboFilterField = new Combo(filterContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
					comboFilterField.setData(filterProperty);
					comboFilterField.addFocusListener(_keepOpenListener);
					comboFilterField.addSelectionListener(filterFieldSelectionListener);

					// fill combo
					for (final TourFilterFieldConfig filterTemplate : TourFilterManager.FILTER_FIELD_CONFIG) {
						comboFilterField.add(filterTemplate.name);
					}

					// keep combo reference
					filterProperty.comboFilterField = comboFilterField;
				}
				{
					/*
					 * Container: Field properties
					 */
					propertyContainer = new Composite(filterContainer, SWT.NONE);
					GridDataFactory.fillDefaults().grab(true, false).applyTo(propertyContainer);
					GridLayoutFactory.fillDefaults().numColumns(1).applyTo(propertyContainer);
					{
						createUI_Property(propertyContainer, filterProperty);
					}
				}
				{
					/*
					 * Toolbar: Property actions
					 */
					final Action_Property_Delete actionProperty_Delete = new Action_Property_Delete(
							this,
							filterProperty);

					final ToolBar toolbar = new ToolBar(filterContainer, SWT.FLAT);
					final ToolBarManager tbm = new ToolBarManager(toolbar);

					tbm.add(actionProperty_Delete);

					tbm.update(true);
				}
			}
		}
		filterContainer.setRedraw(true);
//		filterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		_filterOuterContainer.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_filterScrolledContainer.setOrigin(scrollOrigin);
		}

		// set focus back to the slideout
		_txtProfileName.setFocus();
	}

	private Composite createUI_420_FilterScrolledContainer(final Composite parent) {

		// scrolled container
		_filterScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_filterScrolledContainer);
		_filterScrolledContainer.setExpandVertical(true);
		_filterScrolledContainer.setExpandHorizontal(true);

		// properties container
		final Composite filterContainer = new Composite(_filterScrolledContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(filterContainer);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(3)
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

	private void createUI_800_FilterActions(final Composite parent) {

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

	private void createUI_Property(final Composite parent, final TourFilterProperty filterProperty) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Combo: Field operator
				 */
				final Combo comboFieldOperator = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				comboFieldOperator.setData(filterProperty);
				comboFieldOperator.addFocusListener(_keepOpenListener);
				comboFieldOperator.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onProperty_SelectOperator(e);
					}
				});

				// fill combo
				for (final TourFilterFieldOperator filterOperator : TourFilterManager
						.getFieldOperators(filterProperty.filterField)) {

					comboFieldOperator.add(TourFilterManager.getFieldOperatorName(filterOperator));
				}

				// keep combo reference
				filterProperty.comboFieldOperator = comboFieldOperator;
			}
		}
	}

	private void enableControls() {

		final boolean isProfileSelected = _selectedProfile != null;

		_actionProfile_Delete.setEnabled(isProfileSelected);
		_actionProperty_Add.setEnabled(isProfileSelected);

		_lblProfileName.setEnabled(isProfileSelected);
		_txtProfileName.setEnabled(isProfileSelected);
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

	private void onChangeUI() {

		enableControls();
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

			createUI_410_FilterContent();
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

		// update model from previous profile
		if (_selectedProfile != null) {

			_selectedProfile.name = _txtProfileName.getText();
		}

		// get selected profile from viewer
		final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();
		final Object firstElement = selection.getFirstElement();
		if (firstElement != null) {
			_selectedProfile = (TourFilterProfile) firstElement;
		}

		// update model
		TourFilterManager.setSelectedProfile(_selectedProfile);

		// update UI
		if (_selectedProfile == null) {

			_txtProfileName.setText(UI.EMPTY_STRING);

		} else {

			_txtProfileName.setText(_selectedProfile.name);
		}

		createUI_410_FilterContent();
		updateUI_Properties();

		enableControls();
	}

	private void onProperty_SelectField(final SelectionEvent event) {

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tonProperty_SelectField"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		final TourFilterProperty filterProperty = (TourFilterProperty) event.widget.getData();

		final int selectedFilterConfigIndex = ((Combo) (event.widget)).getSelectionIndex();

		filterProperty.filterField = TourFilterManager.FILTER_FIELD_CONFIG[selectedFilterConfigIndex].filterField;

		createUI_410_FilterContent();
		updateUI_Properties();
	}

	private void onProperty_SelectOperator(final SelectionEvent event) {

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tonProperty_SelectOperator"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		final TourFilterProperty filterProperty = (TourFilterProperty) event.widget.getData();

		final int selectedIndex = ((Combo) (event.widget)).getSelectionIndex();

		filterProperty.fieldOperator = TourFilterManager.getFieldOperator(filterProperty.filterField, selectedIndex);

		createUI_410_FilterContent();
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

			final int filterFieldIndex = TourFilterManager.getFilterFieldIndex(filterProperty.filterField);
			final int fieldOperatorIndex = TourFilterManager.getFieldOperatorIndex(//
					filterProperty.filterField,
					filterProperty.fieldOperator);

			filterProperty.comboFilterField.select(filterFieldIndex);
			filterProperty.comboFieldOperator.select(fieldOperatorIndex);
		}

	}

}
