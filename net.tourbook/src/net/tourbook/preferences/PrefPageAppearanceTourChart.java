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

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceTourChart extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID											= "net.tourbook.preferences.PrefPageChartGraphs";	//$NON-NLS-1$

	private static final int		DEFAULT_FIELD_WIDTH							= 40;

	private static final String		STATE_PREF_PAGE_CHART_GRAPHS_SELECTED_TAB	= "PrefPage.ChartGraphs.SelectedTab";				//$NON-NLS-1$

	private final IPreferenceStore	_prefStore									= TourbookPlugin.getDefault()//
																						.getPreferenceStore();

	private HashMap<Integer, Graph>	_graphMap;
	private ArrayList<Graph>		_graphList;
	private ArrayList<Graph>		_viewerGraphs;

	private MouseWheelListener		_mouseWheelListener;

	/*
	 * UI controls
	 */
	private TabFolder				_tabFolder;
	private TabItem					_tabGrid;
	private TabItem					_tabVisibleGraphs;
	private TabItem					_tabMinimumValues;
	private TabItem					_tabZoom;

	private CheckboxTableViewer		_graphCheckboxList;
	private Spinner					_spinnerGraphTransparencyLine;
	private Spinner					_spinnerGraphTransparencyFilling;
	private Button					_chkGraphAntialiasing;
	private Button					_chkShowHrZoneBackground;

	private Button					_btnUp;
	private Button					_btnDown;

	private Button					_rdoShowDistance;
	private Button					_rdoShowTime;
	private Button					_chkShowStartTime;

	private Button					_chkZoomToSlider;
	private Button					_chkMoveSlidersWhenZoomed;

	private BooleanFieldEditor		_editPaceMinMaxCheckbox;
	private IntegerFieldEditor		_editPaceMin;
	private IntegerFieldEditor		_editPaceMax;
	private BooleanFieldEditor		_editAltimeterMinCheckbox;
	private IntegerFieldEditor		_editAltimeterMinEditor;
	private BooleanFieldEditor		_editGradientMinCheckbox;
	private IntegerFieldEditor		_editGradientMinEditor;

	private Spinner					_spinnerGridVerticalDistance;
	private Spinner					_spinnerGridHorizontalDistance;
	private Button					_chkShowVerticalGridLines;
	private Button					_chkShowHorizontalGridLines;

	private Button					_rdoZoomFeatures;
	private Button					_rdoSliderFeatures;

	private boolean					_isGridLineWarningDisplayed					= false;

	private static class Graph {

		int		__graphId;
		String	__graphLabel;
		boolean	__isChecked	= false;

		public Graph(final int graphId, final String graphLabel) {
			__graphId = graphId;
			__graphLabel = graphLabel;
		}
	};

	@Override
	protected Control createContents(final Composite parent) {

		initUI();

		final Control ui = createUI(parent);

		restoreState();

		validateInput();

		enableActions();
		enableUpDownActions();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_tabFolder = new TabFolder(parent, SWT.NONE);
		_tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		{
			_tabVisibleGraphs = new TabItem(_tabFolder, SWT.NONE);
			_tabVisibleGraphs.setText(Messages.Pref_Graphs_Tab_graph_defaults);
			_tabVisibleGraphs.setControl(createUI10TabGraphs(_tabFolder));

			_tabGrid = new TabItem(_tabFolder, SWT.NONE);
			_tabGrid.setText(Messages.Pref_Graphs_Tab_Grid);
			_tabGrid.setControl(createUI30TabGrid(_tabFolder));

			_tabMinimumValues = new TabItem(_tabFolder, SWT.NONE);
			_tabMinimumValues.setText(Messages.Pref_Graphs_Tab_default_values);
			_tabMinimumValues.setControl(createUI50TabMinMaxValues(_tabFolder));

			_tabZoom = new TabItem(_tabFolder, SWT.NONE);
			_tabZoom.setText(Messages.Pref_Graphs_Tab_zoom_options);
			_tabZoom.setControl(createUI80TabOptions(_tabFolder));
		}

		return _tabFolder;
	}

	private Control createUI10TabGraphs(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			createUI12DefaultGraphs(container);
		}

		return container;
	}

	private void createUI12DefaultGraphs(final Composite parent) {

		// group: default graphs
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Label_select_graph);
		group.setToolTipText(Messages.Pref_Graphs_Label_select_graph_tooltip);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);
		{
			/*
			 * label: select info
			 */
			final Label label = new Label(group, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
			label.setText(Messages.Pref_Graphs_Label_select_graph_tooltip);

			/*
			 * graph container
			 */
			final Composite graphContainer = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().indent(0, 10).applyTo(graphContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(graphContainer);
//			graphContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI13GraphCheckBoxList(graphContainer);
				createUI14GraphActions(graphContainer);
			}

		}

		createUI15GraphOptions(parent);
	}

	private void createUI13GraphCheckBoxList(final Composite parent) {

		_graphCheckboxList = CheckboxTableViewer.newCheckList(//
				parent,
				SWT.SINGLE | SWT.TOP | SWT.BORDER);

		_graphCheckboxList.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return _viewerGraphs.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_graphCheckboxList.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(final Object element) {
				final Graph graph = (Graph) element;
				return graph.__graphLabel;
			}
		});

		_graphCheckboxList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				// keep the checked status
				final Graph item = (Graph) event.getElement();
				item.__isChecked = event.getChecked();

				// select the checked item
				_graphCheckboxList.setSelection(new StructuredSelection(item));

				validateInput();
			}
		});

		_graphCheckboxList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownActions();
			}
		});

//		final Table table = _graphCheckboxList.getTable();
//		table.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
	}

	private void createUI14GraphActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			/*
			 * button: up
			 */
			_btnUp = new Button(container, SWT.NONE);
			setButtonLayoutData(_btnUp);
			_btnUp.setText(Messages.Pref_Graphs_Button_up);
			_btnUp.setEnabled(false);
			_btnUp.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(final SelectionEvent e) {}

				public void widgetSelected(final SelectionEvent e) {
					moveSelectionUp();
					enableUpDownActions();
				}
			});

			/*
			 * button: down
			 */
			_btnDown = new Button(container, SWT.NONE);
			setButtonLayoutData(_btnDown);
			_btnDown.setText(Messages.Pref_Graphs_Button_down);
			_btnDown.setEnabled(false);
			_btnDown.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(final SelectionEvent e) {}

				public void widgetSelected(final SelectionEvent e) {
					moveSelectionDown();
					enableUpDownActions();
				}
			});
		}
	}

	private void createUI15GraphOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * label: graph filling transparency
			 */
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.Pref_Graphs_Label_GraphTransparencyLine);
			label.setToolTipText(Messages.Pref_Graphs_Label_GraphTransparencyLine_Tooltip);

			/*
			 * spinner: graph filling transparence
			 */
			_spinnerGraphTransparencyLine = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerGraphTransparencyLine);
			_spinnerGraphTransparencyLine.setMinimum(0x10);
			_spinnerGraphTransparencyLine.setMaximum(0xff);
			_spinnerGraphTransparencyLine.addMouseWheelListener(_mouseWheelListener);

			/*
			 * label: graph filling transparency
			 */
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.Pref_Graphs_Label_GraphTransparency);
			label.setToolTipText(Messages.Pref_Graphs_Label_GraphTransparency_Tooltip);

			/*
			 * spinner: graph filling transparence
			 */
			_spinnerGraphTransparencyFilling = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerGraphTransparencyFilling);
			_spinnerGraphTransparencyFilling.setMinimum(0x00);
			_spinnerGraphTransparencyFilling.setMaximum(0xff);
			_spinnerGraphTransparencyFilling.addMouseWheelListener(_mouseWheelListener);

			/*
			 * checkbox: graph antialiasing
			 */
			_chkGraphAntialiasing = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkGraphAntialiasing);
			_chkGraphAntialiasing.setText(Messages.Pref_Graphs_Checkbox_GraphAntialiasing);
			_chkGraphAntialiasing.setToolTipText(Messages.Pref_Graphs_Checkbox_GraphAntialiasing_Tooltip);

			/*
			 * checkbox: HR zones
			 */
			_chkShowHrZoneBackground = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowHrZoneBackground);
			_chkShowHrZoneBackground.setText(Messages.Graph_Label_ShowHrZoneBackground);
			_chkShowHrZoneBackground.setToolTipText(Messages.Graph_Label_ShowHrZoneBackground_Tooltip);
		}
	}

	/**
	 * tab: grid/units
	 * 
	 * @return
	 */
	private Control createUI30TabGrid(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			createUI32Grid(container);
			createUI34XAxisUnits(container);
		}

		return container;
	}

	private void createUI32Grid(final Composite parent) {

		final SelectionAdapter gridLineListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectGridLine();
			}
		};

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_Grid);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			/*
			 * label: grid distance
			 */
			Label label = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.applyTo(label);
			label.setText(Messages.Pref_Graphs_grid_distance);
			{
				/*
				 * label: horizontal grid
				 */
				label = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.indent(16, 0)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(Messages.Pref_Graphs_grid_horizontal_distance);

				/*
				 * spinner: horizontal grid
				 */
				_spinnerGridHorizontalDistance = new Spinner(group, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerGridHorizontalDistance);
				_spinnerGridHorizontalDistance.setMinimum(10);
				_spinnerGridHorizontalDistance.setMaximum(200);
				_spinnerGridHorizontalDistance.addMouseWheelListener(_mouseWheelListener);

				/*
				 * label: vertical grid
				 */
				label = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.indent(16, 0)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(Messages.Pref_Graphs_grid_vertical_distance);

				/*
				 * spinner: vertical grid
				 */
				_spinnerGridVerticalDistance = new Spinner(group, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerGridVerticalDistance);
				_spinnerGridVerticalDistance.setMinimum(10);
				_spinnerGridVerticalDistance.setMaximum(200);
				_spinnerGridVerticalDistance.addMouseWheelListener(_mouseWheelListener);
			}

			/*
			 * checkbox: show horizontal grid
			 */
			_chkShowHorizontalGridLines = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.indent(0, 15)
					.span(2, 1)
					.applyTo(_chkShowHorizontalGridLines);
			_chkShowHorizontalGridLines.setText(Messages.Pref_Graphs_Checkbox_ShowHorizontalGrid);
			_chkShowHorizontalGridLines.addSelectionListener(gridLineListener);

			/*
			 * checkbox: show vertical grid
			 */
			_chkShowVerticalGridLines = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowVerticalGridLines);
			_chkShowVerticalGridLines.setText(Messages.Pref_Graphs_Checkbox_ShowVerticalGrid);
			_chkShowVerticalGridLines.addSelectionListener(gridLineListener);

		}
	}

	private void createUI34XAxisUnits(final Composite container) {

		// group: units for the x-axis
		final Group group = new Group(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.Pref_Graphs_Group_units_for_xaxis);
		GridLayoutFactory.swtDefaults().applyTo(group);
		{
			/*
			 * radio: distance
			 */
			_rdoShowDistance = new Button(group, SWT.RADIO);
			_rdoShowDistance.setText(Messages.Pref_Graphs_Radio_show_distance);
			_rdoShowDistance.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					enableActions();
				}
			});

			/*
			 * radio: time
			 */
			_rdoShowTime = new Button(group, SWT.RADIO);
			_rdoShowTime.setText(Messages.Pref_Graphs_Radio_show_time);

			_chkShowStartTime = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(_chkShowStartTime);
			_chkShowStartTime.setText(Messages.Pref_Graphs_Check_show_start_time);
		}
	}

	private Control createUI50TabMinMaxValues(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			createUI52MinMaxValue(container);
		}

		return container;
	}

	private void createUI52MinMaxValue(final Composite container) {

		GridData gd;
		Label label;

		// the editor container removes all margins
		final Group groupMinValue = new Group(container, SWT.NONE);
		groupMinValue.setText(Messages.Pref_Graphs_force_minimum_value);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupMinValue);
		{
			/*
			 * checkbox: pace min/max value
			 */
			_editPaceMinMaxCheckbox = new BooleanFieldEditor(
					ITourbookPreferences.GRAPH_PACE_MINMAX_IS_ENABLED,
					Messages.Pref_Graphs_Check_force_minmax_for_pace,
					groupMinValue);
			_editPaceMinMaxCheckbox.setPreferenceStore(_prefStore);
			_editPaceMinMaxCheckbox.setPage(this);
			_editPaceMinMaxCheckbox.load();
			_editPaceMinMaxCheckbox.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					final boolean isChecked = (Boolean) event.getNewValue();
					_editPaceMin.setEnabled(isChecked, groupMinValue);
					_editPaceMax.setEnabled(isChecked, groupMinValue);
				}
			});

			// paceholder
			new Label(groupMinValue, SWT.NONE);
			new Label(groupMinValue, SWT.NONE);

			/*
			 * editor: pace min value
			 */
			_editPaceMin = new IntegerFieldEditor(
					ITourbookPreferences.GRAPH_PACE_MIN_VALUE,
					Messages.Pref_Graphs_Text_min_value,
					groupMinValue);
			_editPaceMin.setPreferenceStore(_prefStore);
			_editPaceMin.setPage(this);
			_editPaceMin.setTextLimit(4);
			_editPaceMin.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
			_editPaceMin.load();
			UI.setFieldWidth(groupMinValue, _editPaceMin, DEFAULT_FIELD_WIDTH);
			gd = new GridData();
			gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
			_editPaceMin.getLabelControl(groupMinValue).setLayoutData(gd);

			_editPaceMin.setEnabled(_editPaceMinMaxCheckbox.getBooleanValue(), groupMinValue);

			// label: minutes
			label = new Label(groupMinValue, SWT.NONE);
			label.setText(Messages.app_unit_minutes);

			/*
			 * editor: pace max value
			 */
			_editPaceMax = new IntegerFieldEditor(
					ITourbookPreferences.GRAPH_PACE_MAX_VALUE,
					Messages.Pref_Graphs_Text_max_value,
					groupMinValue);
			_editPaceMax.setPreferenceStore(_prefStore);
			_editPaceMax.setPage(this);
			_editPaceMax.setTextLimit(4);
			_editPaceMax.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
			_editPaceMax.load();
			UI.setFieldWidth(groupMinValue, _editPaceMax, DEFAULT_FIELD_WIDTH);
			gd = new GridData();
			gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
			_editPaceMax.getLabelControl(groupMinValue).setLayoutData(gd);

			_editPaceMax.setEnabled(_editPaceMinMaxCheckbox.getBooleanValue(), groupMinValue);

			// label: minutes
			label = new Label(groupMinValue, SWT.NONE);
			label.setText(Messages.app_unit_minutes);

			/*
			 * checkbox: altimeter min value
			 */
			_editAltimeterMinCheckbox = new BooleanFieldEditor(
					ITourbookPreferences.GRAPH_ALTIMETER_MIN_IS_ENABLED,
					Messages.Pref_Graphs_Check_force_minimum_for_altimeter,
					groupMinValue);
			_editAltimeterMinCheckbox.setPreferenceStore(_prefStore);
			_editAltimeterMinCheckbox.setPage(this);
			_editAltimeterMinCheckbox.load();
			_editAltimeterMinCheckbox.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					final boolean isChecked = (Boolean) event.getNewValue();
					_editAltimeterMinEditor.setEnabled(isChecked, groupMinValue);
				}
			});

			// paceholder
			new Label(groupMinValue, SWT.NONE);
			new Label(groupMinValue, SWT.NONE);

			/*
			 * editor: altimeter min value
			 */
			_editAltimeterMinEditor = new IntegerFieldEditor(
					ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE,
					Messages.Pref_Graphs_Text_min_value,
					groupMinValue);
			_editAltimeterMinEditor.setPreferenceStore(_prefStore);
			_editAltimeterMinEditor.setPage(this);
			_editAltimeterMinEditor.setTextLimit(4);
			_editAltimeterMinEditor.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
			_editAltimeterMinEditor.load();
			UI.setFieldWidth(groupMinValue, _editAltimeterMinEditor, DEFAULT_FIELD_WIDTH);
			gd = new GridData();
			gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
			_editAltimeterMinEditor.getLabelControl(groupMinValue).setLayoutData(gd);

			_editAltimeterMinEditor.setEnabled(_editAltimeterMinCheckbox.getBooleanValue(), groupMinValue);

			// paceholder
			new Label(groupMinValue, SWT.NONE);

			/*
			 * checkbox: gradient min value
			 */
			_editGradientMinCheckbox = new BooleanFieldEditor(
					ITourbookPreferences.GRAPH_GRADIENT_MIN_IS_ENABLED,
					Messages.Pref_Graphs_Check_force_minimum_for_gradient,
					groupMinValue);
			_editGradientMinCheckbox.setPreferenceStore(_prefStore);
			_editGradientMinCheckbox.setPage(this);
			_editGradientMinCheckbox.load();
			_editGradientMinCheckbox.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					final boolean isChecked = (Boolean) event.getNewValue();
					_editGradientMinEditor.setEnabled(isChecked, groupMinValue);
				}
			});

			// add placeholder
			new Label(groupMinValue, SWT.NONE);
			new Label(groupMinValue, SWT.NONE);

			/*
			 * editor: gradient min value
			 */
			_editGradientMinEditor = new IntegerFieldEditor(
					ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
					Messages.Pref_Graphs_Text_min_value,
					groupMinValue);
			_editGradientMinEditor.setPreferenceStore(_prefStore);
			_editGradientMinEditor.setPage(this);
			_editGradientMinEditor.setTextLimit(4);
			_editGradientMinEditor.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
			_editGradientMinEditor.load();
			UI.setFieldWidth(groupMinValue, _editGradientMinEditor, DEFAULT_FIELD_WIDTH);
			gd = new GridData();
			gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
			_editGradientMinEditor.getLabelControl(groupMinValue).setLayoutData(gd);
			_editGradientMinEditor.setEnabled(_editGradientMinCheckbox.getBooleanValue(), groupMinValue);

			// add placeholder
			new Label(groupMinValue, SWT.NONE);
		}

		GridLayoutFactory.swtDefaults().margins(5, 5).numColumns(3).applyTo(groupMinValue);
	}

	private Control createUI80TabOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			createUI82MouseMode(container);
			createUI84ZoomOptions(container);
		}

		return container;
	}

	private void createUI82MouseMode(final Composite container) {

		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_mouse_mode);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);
		{
			// radio: zoom features
			_rdoZoomFeatures = new Button(group, SWT.RADIO);
			_rdoZoomFeatures.setText(Messages.Pref_Graphs_Radio_mouse_mode_zoom);
			_rdoZoomFeatures.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					enableActions();
				}
			});

			// radio: slider features
			_rdoSliderFeatures = new Button(group, SWT.RADIO);
			_rdoSliderFeatures.setText(Messages.Pref_Graphs_Radio_mouse_mode_slider);
		}
	}

	/**
	 * group: zoom options
	 */
	private void createUI84ZoomOptions(final Composite container) {

		final Group groupZoomOptions = new Group(container, SWT.NONE);
		groupZoomOptions.setText(Messages.Pref_Graphs_Group_zoom_options);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupZoomOptions);
		GridLayoutFactory.swtDefaults().applyTo(groupZoomOptions);
		{
			/*
			 * checkbox: auto zoom to moved slider
			 */
			_chkZoomToSlider = new Button(groupZoomOptions, SWT.CHECK);
			_chkZoomToSlider.setText(Messages.Pref_Graphs_Check_autozoom);

			/*
			 * checkbox: move sliders to border when zoomed
			 */
			_chkMoveSlidersWhenZoomed = new Button(groupZoomOptions, SWT.CHECK);
			_chkMoveSlidersWhenZoomed.setText(Messages.Pref_Graphs_move_sliders_when_zoomed);
		}
	}

	private void enableActions() {
		_chkShowStartTime.setEnabled(_rdoShowTime.getSelection());
	}

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownActions() {

		final Table table = _graphCheckboxList.getTable();
		final TableItem[] items = table.getSelection();
		final boolean validSelection = items != null && items.length > 0;
		boolean enableUp = validSelection;
		boolean enableDown = validSelection;

		if (validSelection) {
			final int indices[] = table.getSelectionIndices();
			final int max = table.getItemCount();
			enableUp = indices[0] != 0;
			enableDown = indices[indices.length - 1] < max - 1;
		}

		_btnUp.setEnabled(enableUp);
		_btnDown.setEnabled(enableDown);
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUI() {

		_mouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};

		// create a map and list with all available graphs

		final Graph graphAltitude = new Graph(TourManager.GRAPH_ALTITUDE, Messages.Graph_Label_Altitude);
		final Graph graphSpeed = new Graph(TourManager.GRAPH_SPEED, Messages.Graph_Label_Speed);
		final Graph graphPace = new Graph(TourManager.GRAPH_PACE, Messages.Graph_Label_Pace);
		final Graph graphPower = new Graph(TourManager.GRAPH_POWER, Messages.Graph_Label_Power);
		final Graph graphPulse = new Graph(TourManager.GRAPH_PULSE, Messages.Graph_Label_Heartbeat);
		final Graph graphTemperature = new Graph(TourManager.GRAPH_TEMPERATURE, Messages.Graph_Label_Temperature);
		final Graph graphCadence = new Graph(TourManager.GRAPH_CADENCE, Messages.Graph_Label_Cadence);
		final Graph graphAltimeter = new Graph(TourManager.GRAPH_ALTIMETER, Messages.Graph_Label_Altimeter);
		final Graph graphGradient = new Graph(TourManager.GRAPH_GRADIENT, Messages.Graph_Label_Gradient);

		_graphMap = new HashMap<Integer, Graph>();
		_graphMap.put(TourManager.GRAPH_ALTITUDE, graphAltitude);
		_graphMap.put(TourManager.GRAPH_SPEED, graphSpeed);
		_graphMap.put(TourManager.GRAPH_PACE, graphPace);
		_graphMap.put(TourManager.GRAPH_POWER, graphPower);
		_graphMap.put(TourManager.GRAPH_PULSE, graphPulse);
		_graphMap.put(TourManager.GRAPH_TEMPERATURE, graphTemperature);
		_graphMap.put(TourManager.GRAPH_CADENCE, graphCadence);
		_graphMap.put(TourManager.GRAPH_ALTIMETER, graphAltimeter);
		_graphMap.put(TourManager.GRAPH_GRADIENT, graphGradient);

		_graphList = new ArrayList<Graph>();
		_graphList.add(graphAltitude);
		_graphList.add(graphSpeed);
		_graphList.add(graphPace);
		_graphList.add(graphPower);
		_graphList.add(graphPulse);
		_graphList.add(graphTemperature);
		_graphList.add(graphCadence);
		_graphList.add(graphAltimeter);
		_graphList.add(graphGradient);
	}

	/**
	 * Moves an entry in the table to the given index.
	 */
	private void move(final TableItem item, final int index) {

		this.setValid(true);
		final Graph graph = (Graph) item.getData();
		item.dispose();

		_graphCheckboxList.insert(graph, index);
		_graphCheckboxList.setChecked(graph, graph.__isChecked);
	}

	/**
	 * Move the current selection in the build list down.
	 */
	private void moveSelectionDown() {

		final Table table = _graphCheckboxList.getTable();
		final int indices[] = table.getSelectionIndices();
		if (indices.length < 1) {
			return;
		}

		final int newSelection[] = new int[indices.length];
		final int max = table.getItemCount() - 1;

		for (int i = indices.length - 1; i >= 0; i--) {
			final int index = indices[i];
			if (index < max) {
				move(table.getItem(index), index + 1);
				newSelection[i] = index + 1;
			}
		}
		table.setSelection(newSelection);
	}

	/**
	 * Move the current selection in the build list up.
	 */
	private void moveSelectionUp() {

		final Table table = _graphCheckboxList.getTable();
		final int indices[] = table.getSelectionIndices();
		final int newSelection[] = new int[indices.length];

		for (int i = 0; i < indices.length; i++) {
			final int index = indices[i];
			if (index > 0) {
				move(table.getItem(index), index - 1);
				newSelection[i] = index - 1;
			}
		}
		table.setSelection(newSelection);
	}

	@Override
	public boolean okToLeave() {
		saveUIState();
		return super.okToLeave();
	}

	private void onSelectGridLine() {

		// show performance warning

		if (_isGridLineWarningDisplayed) {
			return;
		}

		// don't show warning when both are hidden
		if (!_chkShowHorizontalGridLines.getSelection() && !_chkShowVerticalGridLines.getSelection()) {
			return;
		}

		_isGridLineWarningDisplayed = true;

		MessageDialog.openWarning(
				getShell(),
				Messages.Pref_Graphs_Dialog_GridLine_Warning_Title,
				Messages.Pref_Graphs_Dialog_GridLine_Warning_Message);
	}

	@Override
	public boolean performCancel() {
		saveUIState();
		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		/*
		 * perform defaults for the currently selected tab
		 */

		final int selectedTabIndex = _tabFolder.getSelectionIndex();
		if (selectedTabIndex != -1) {

			final TabItem selectedTabItem = _tabFolder.getItem(selectedTabIndex);

			if (selectedTabItem.equals(_tabVisibleGraphs)) {

				_chkShowHrZoneBackground.setSelection(//
						_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE));

				_spinnerGraphTransparencyLine.setSelection(//
						_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE));
				_spinnerGraphTransparencyFilling.setSelection(//
						_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING));

				_chkGraphAntialiasing.setSelection(//
						_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_ANTIALIASING));

			} else if (selectedTabItem.equals(_tabGrid)) {

				_spinnerGridHorizontalDistance.setSelection(//
						_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE));
				_spinnerGridVerticalDistance.setSelection(//
						_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

				_chkShowHorizontalGridLines.setSelection(//
						_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES));
				_chkShowVerticalGridLines.setSelection(//
						_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));
			}
		}

		super.performDefaults();
	}

	/*
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreGraphs() {

		/*
		 * create a list with all available graphs
		 */
		final String[] prefAllGraphIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(ITourbookPreferences.GRAPH_ALL));

		_viewerGraphs = new ArrayList<Graph>();

		// put all graphs in the viewer which are defined in the prefs
		for (final String graphIdKey : prefAllGraphIds) {

			final int graphId = Integer.parseInt(graphIdKey);

			if (_graphMap.containsKey(graphId)) {
				_viewerGraphs.add(_graphMap.get(graphId));
			}
		}

		// make sure that all available graphs are in the viewer
		for (final Graph graph : _graphList) {
			if (!_viewerGraphs.contains(graph)) {
				_viewerGraphs.add(graph);
			}
		}

		_graphCheckboxList.setInput(this);

		final String[] prefVisibleIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(ITourbookPreferences.GRAPH_VISIBLE));

		// check all graphs which are defined in the prefs
		final ArrayList<Graph> checkedGraphs = new ArrayList<Graph>();
		for (final Graph graph : _viewerGraphs) {
			final int graphId = graph.__graphId;
			for (final String prefId : prefVisibleIds) {
				if (graphId == Integer.parseInt(prefId)) {
					graph.__isChecked = true;
					checkedGraphs.add(graph);
				}
			}
		}

		_graphCheckboxList.setCheckedElements(checkedGraphs.toArray());
	}

	private void restoreState() {

		_spinnerGraphTransparencyLine.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE));
		_spinnerGraphTransparencyFilling.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING));
		_chkGraphAntialiasing.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_ANTIALIASING));

		_chkShowHrZoneBackground.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE));

		_spinnerGridHorizontalDistance.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE));

		_spinnerGridVerticalDistance.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		_chkShowHorizontalGridLines.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES));
		_chkShowVerticalGridLines.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		_chkZoomToSlider.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER));
		_chkMoveSlidersWhenZoomed.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED));

		if (_prefStore.getString(ITourbookPreferences.GRAPH_MOUSE_MODE).equals(Chart.MOUSE_MODE_SLIDER)) {
			_rdoSliderFeatures.setSelection(true);
		} else {
			_rdoZoomFeatures.setSelection(true);
		}

		if (_prefStore.getString(ITourbookPreferences.GRAPH_X_AXIS).equals(TourManager.X_AXIS_TIME)) {
			_rdoShowTime.setSelection(true);
		} else {
			_rdoShowDistance.setSelection(true);
		}

		// checkbox: starttime
		_chkShowStartTime.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME));

		/*
		 * folder
		 */
		_tabFolder.setSelection(_prefStore.getInt(STATE_PREF_PAGE_CHART_GRAPHS_SELECTED_TAB));

		restoreGraphs();
	}

	/**
	 * get the graph id's from the preferences and check the graphs in the list
	 */
	private void saveGraphs() {

		// convert the array with the graph objects into a string which is store
		// in the prefs
		final Object[] graphs = _graphCheckboxList.getCheckedElements();
		final String[] prefGraphsChecked = new String[graphs.length];
		for (int graphIndex = 0; graphIndex < graphs.length; graphIndex++) {
			final Graph graph = (Graph) graphs[graphIndex];
			prefGraphsChecked[graphIndex] = Integer.toString(graph.__graphId);
		}
		_prefStore.setValue(
				ITourbookPreferences.GRAPH_VISIBLE,
				StringToArrayConverter.convertArrayToString(prefGraphsChecked));

		// convert the array of all table items into a string which is store in
		// the prefs
		final TableItem[] items = _graphCheckboxList.getTable().getItems();
		final String[] prefGraphs = new String[items.length];
		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			prefGraphs[itemIndex] = Integer.toString(((Graph) items[itemIndex].getData()).__graphId);
		}

		_prefStore.setValue(ITourbookPreferences.GRAPH_ALL, StringToArrayConverter.convertArrayToString(prefGraphs));
	}

	private void saveState() {

		_prefStore.setValue(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE, //
				_spinnerGraphTransparencyLine.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING, //
				_spinnerGraphTransparencyFilling.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_ANTIALIASING,//
				_chkGraphAntialiasing.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE,//
				_chkShowHrZoneBackground.getSelection());

		if (_rdoShowTime.getSelection()) {
			_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_TIME);
		} else {
			_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);
		}

		if (_rdoSliderFeatures.getSelection()) {
			_prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_SLIDER);
		} else {
			_prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_ZOOM);
		}

		_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME, //
				_chkShowStartTime.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER, //
				_chkZoomToSlider.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED, //
				_chkMoveSlidersWhenZoomed.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE, //
				_spinnerGridHorizontalDistance.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE, //
				_spinnerGridVerticalDistance.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES, //
				_chkShowHorizontalGridLines.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES, //
				_chkShowVerticalGridLines.getSelection());

		_editPaceMinMaxCheckbox.store();
		_editPaceMin.store();
		_editPaceMax.store();
		_editAltimeterMinCheckbox.store();
		_editAltimeterMinEditor.store();
		_editGradientMinCheckbox.store();
		_editGradientMinEditor.store();

		saveGraphs();
	}

	private void saveUIState() {

		if (_tabFolder == null || _tabFolder.isDisposed()) {
			return;
		}

		_prefStore.setValue(STATE_PREF_PAGE_CHART_GRAPHS_SELECTED_TAB, _tabFolder.getSelectionIndex());
	}

	/**
	 * check the fields if they are valid
	 */
	private void validateInput() {

		if (_graphCheckboxList.getCheckedElements().length == 0) {
			setErrorMessage(Messages.Pref_Graphs_Error_one_graph_must_be_selected);
			setValid(false);

		} else {
			setErrorMessage(null);
			setValid(true);
		}
	}

}
