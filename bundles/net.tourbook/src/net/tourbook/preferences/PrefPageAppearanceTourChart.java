/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import net.tourbook.common.UI;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
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

	public static final String		ID											= "net.tourbook.preferences.PrefPageChartGraphs";						//$NON-NLS-1$

	private static final String		STATE_PREF_PAGE_CHART_GRAPHS_SELECTED_TAB	= "PrefPage.ChartGraphs.SelectedTab";									//$NON-NLS-1$

// SET_FORMATTING_OFF
	
	private static final String	GRAPH_LABEL_ALTIMETER						= net.tourbook.common.Messages.Graph_Label_Altimeter;
	private static final String	GRAPH_LABEL_ALTITUDE						= net.tourbook.common.Messages.Graph_Label_Altitude;
	private static final String	GRAPH_LABEL_CADENCE							= net.tourbook.common.Messages.Graph_Label_Cadence;
	private static final String	GRAPH_LABEL_GEARS							= net.tourbook.common.Messages.Graph_Label_Gears;
	private static final String	GRAPH_LABEL_GRADIENT						= net.tourbook.common.Messages.Graph_Label_Gradient;
	private static final String	GRAPH_LABEL_HEARTBEAT						= net.tourbook.common.Messages.Graph_Label_Heartbeat;
	private static final String	GRAPH_LABEL_PACE							= net.tourbook.common.Messages.Graph_Label_Pace;
	private static final String	GRAPH_LABEL_POWER							= net.tourbook.common.Messages.Graph_Label_Power;
	private static final String	GRAPH_LABEL_SHOW_HR_ZONE_BACKGROUND_TOOLTIP	= net.tourbook.common.Messages.Graph_Label_ShowHrZoneBackground_Tooltip;
	private static final String	GRAPH_LABEL_SHOW_HR_ZONE_BACKGROUND			= net.tourbook.common.Messages.Graph_Label_ShowHrZoneBackground;
	private static final String	GRAPH_LABEL_SPEED							= net.tourbook.common.Messages.Graph_Label_Speed;
	private static final String	GRAPH_LABEL_TEMPERATURE						= net.tourbook.common.Messages.Graph_Label_Temperature;
	private static final String	GRAPH_LABEL_TOUR_COMPARE_RESULT				= net.tourbook.common.Messages.Graph_Label_Tour_Compare;
	
	private static final String	GRAPH_LABEL_RUN_DYN_STANCE_TIME				= net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTime;
	private static final String	GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCED	= net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTimeBalanced;
	private static final String	GRAPH_LABEL_RUN_DYN_STEP_LENGTH				= net.tourbook.common.Messages.Graph_Label_RunDyn_StepLength;
	private static final String	GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION	= net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalOscillation;
	private static final String	GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO			= net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalRatio;

// SET_FORMATTING_ON

	private final IPreferenceStore	_prefStore									= TourbookPlugin.getPrefStore();

	private HashMap<Integer, Graph>	_graphMap;
	private ArrayList<Graph>		_graphList;
	private ArrayList<Graph>		_viewerGraphs;

	private IPropertyChangeListener	_defaultChangePropertyListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private SelectionAdapter		_defaultSelectionListener;

	/*
	 * UI controls
	 */
	private TabFolder				_tabFolder;
	private TabItem					_tab1_Graphs;
	private TabItem					_tab2_Grid;
	private TabItem					_tab3_Options;

	private CheckboxTableViewer		_graphCheckboxList;

	private Button					_btnDown;
	private Button					_btnUp;

	private Button					_chkGraphAntialiasing;
	private Button					_chkLiveUpdate;

	private Button					_chkMoveSlidersWhenZoomed;
	private Button					_chkSegmentAlternateColor;
	private Button					_chkShowGrid_HorizontalLines;
	private Button					_chkShowHrZoneBackground;
	private Button					_chkShowStartTime;
	private Button					_chkShowGrid_VerticalLines;
	private Button					_chkZoomToSlider;

	private Label					_lblGridHorizontal;
	private Label					_lblGridHorizontal_Unit;
	private Label					_lblGridVertical;
	private Label					_lblGridVertical_Unit;

	private Button					_rdoMouseModeSlider;
	private Button					_rdoMouseModeZoom;
	private Button					_rdoShowDistance;
	private Button					_rdoShowTime;

	private Spinner					_spinnerGraphTransparencyLine;
	private Spinner					_spinnerGraphTransparencyFilling;
	private Spinner					_spinnerGridHorizontalDistance;
	private Spinner					_spinnerGridVerticalDistance;

	private ColorSelector			_colorSegmentAlternateColor;

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

		initUI(parent);

		final Control ui = createUI(parent);

		restoreState();

		validateInput();

		enableActions();
		enableUpDownActions();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			_tabFolder = new TabFolder(container, SWT.NONE);
			_tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			{
				_tab1_Graphs = new TabItem(_tabFolder, SWT.NONE);
				_tab1_Graphs.setText(Messages.Pref_Graphs_Tab_graph_defaults);
				_tab1_Graphs.setControl(createUI_10_Tab_1_Graphs(_tabFolder));

				_tab2_Grid = new TabItem(_tabFolder, SWT.NONE);
				_tab2_Grid.setText(Messages.Pref_Graphs_Tab_Grid);
				_tab2_Grid.setControl(createUI_70_Tab_2_Grid(_tabFolder));

				_tab3_Options = new TabItem(_tabFolder, SWT.NONE);
				_tab3_Options.setText(Messages.Pref_Graphs_Tab_zoom_options);
				_tab3_Options.setControl(createUI_80_Tab_3_Options(_tabFolder));
			}

			createUI_99_LiveUpdate(container);
		}

		return container;
	}

	private Control createUI_10_Tab_1_Graphs(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			createUI_12_DefaultGraphs(container);
		}

		return container;
	}

	private void createUI_12_DefaultGraphs(final Composite parent) {

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
				createUI_13_GraphCheckBoxList(graphContainer);
				createUI_14_GraphActions(graphContainer);
			}

		}

		createUI_15_GraphOptions(parent);
	}

	private void createUI_13_GraphCheckBoxList(final Composite parent) {

		_graphCheckboxList = CheckboxTableViewer.newCheckList(//
				parent,
				SWT.SINGLE | SWT.TOP /* | SWT.BORDER */);
//		_graphCheckboxList.getTable().setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		_graphCheckboxList.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {}

			@Override
			public Object[] getElements(final Object inputElement) {
				return _viewerGraphs.toArray();
			}

			@Override
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
			@Override
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
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownActions();
				doLiveUpdate();
			}
		});

//		final Table table = _graphCheckboxList.getTable();
//		table.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
	}

	private void createUI_14_GraphActions(final Composite parent) {

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
				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					moveSelectionUp();
					enableUpDownActions();
					doLiveUpdate();
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
				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					moveSelectionDown();
					enableUpDownActions();
					doLiveUpdate();
				}
			});
		}
	}

	private void createUI_15_GraphOptions(final Composite parent) {

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
			_spinnerGraphTransparencyLine.addMouseWheelListener(_defaultMouseWheelListener);
			_spinnerGraphTransparencyLine.addSelectionListener(_defaultSelectionListener);

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
			_spinnerGraphTransparencyFilling.addMouseWheelListener(_defaultMouseWheelListener);
			_spinnerGraphTransparencyFilling.addSelectionListener(_defaultSelectionListener);

			/*
			 * checkbox: graph antialiasing
			 */
			_chkGraphAntialiasing = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkGraphAntialiasing);
			_chkGraphAntialiasing.setText(Messages.Pref_Graphs_Checkbox_GraphAntialiasing);
			_chkGraphAntialiasing.setToolTipText(Messages.Pref_Graphs_Checkbox_GraphAntialiasing_Tooltip);
			_chkGraphAntialiasing.addSelectionListener(_defaultSelectionListener);

			/*
			 * Checkbox: Segments with alternate colors
			 */
			_chkSegmentAlternateColor = new Button(container, SWT.CHECK);
			_chkSegmentAlternateColor.setText(Messages.Pref_Graphs_Checkbox_SegmentAlternateColor);
			_chkSegmentAlternateColor.setToolTipText(Messages.Pref_Graphs_Checkbox_SegmentAlternateColor_Tooltip);
			_chkSegmentAlternateColor.addSelectionListener(_defaultSelectionListener);

			// Color: Segment alternate color
			_colorSegmentAlternateColor = new ColorSelector(container);
			_colorSegmentAlternateColor.addListener(_defaultChangePropertyListener);

			/*
			 * checkbox: HR zones
			 */
			_chkShowHrZoneBackground = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowHrZoneBackground);
			_chkShowHrZoneBackground.setText(GRAPH_LABEL_SHOW_HR_ZONE_BACKGROUND);
			_chkShowHrZoneBackground.setToolTipText(GRAPH_LABEL_SHOW_HR_ZONE_BACKGROUND_TOOLTIP);
			_chkShowHrZoneBackground.addSelectionListener(_defaultSelectionListener);
		}
	}

	/**
	 * tab: grid/units
	 * 
	 * @return
	 */
	private Control createUI_70_Tab_2_Grid(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			createUI_72_Grid(container);
			createUI_74_XAxisUnits(container);
		}

		return container;
	}

	private void createUI_72_Grid(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_Grid);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				/*
				 * label: grid distance
				 */
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_Graphs_grid_distance);
				GridDataFactory.fillDefaults()//
						.span(3, 1)
						.applyTo(label);
			}

			{
				/*
				 * label: horizontal grid
				 */
				_lblGridHorizontal = new Label(group, SWT.NONE);
				_lblGridHorizontal.setText(Messages.Pref_Graphs_grid_horizontal_distance);
				GridDataFactory.fillDefaults()//
//						.indent(16, 0)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblGridHorizontal);

				/*
				 * spinner: horizontal grid
				 */
				_spinnerGridHorizontalDistance = new Spinner(group, SWT.BORDER);
				_spinnerGridHorizontalDistance.setMinimum(10);
				_spinnerGridHorizontalDistance.setMaximum(1000);
				_spinnerGridHorizontalDistance.addMouseWheelListener(_defaultMouseWheelListener);
				_spinnerGridHorizontalDistance.addSelectionListener(_defaultSelectionListener);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerGridHorizontalDistance);

				/*
				 * Label: px
				 */
				_lblGridHorizontal_Unit = new Label(group, SWT.NONE);
				_lblGridHorizontal_Unit.setText(Messages.App_Unit_Px);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblGridHorizontal_Unit);
			}

			{
				/*
				 * label: vertical grid
				 */
				_lblGridVertical = new Label(group, SWT.NONE);
				_lblGridVertical.setText(Messages.Pref_Graphs_grid_vertical_distance);
				GridDataFactory.fillDefaults()//
//						.indent(16, 0)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblGridVertical);

				/*
				 * spinner: vertical grid
				 */
				_spinnerGridVerticalDistance = new Spinner(group, SWT.BORDER);
				_spinnerGridVerticalDistance.setMinimum(10);
				_spinnerGridVerticalDistance.setMaximum(1000);
				_spinnerGridVerticalDistance.addMouseWheelListener(_defaultMouseWheelListener);
				_spinnerGridVerticalDistance.addSelectionListener(_defaultSelectionListener);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerGridVerticalDistance);
				/*
				 * Label: px
				 */
				_lblGridVertical_Unit = new Label(group, SWT.NONE);
				_lblGridVertical_Unit.setText(Messages.App_Unit_Px);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblGridVertical_Unit);
			}
			{
				/*
				 * checkbox: show horizontal grid
				 */
				_chkShowGrid_HorizontalLines = new Button(group, SWT.CHECK);
				_chkShowGrid_HorizontalLines.setText(Messages.Pref_Graphs_Checkbox_ShowHorizontalGrid);
				_chkShowGrid_HorizontalLines.setToolTipText(Messages.Pref_Graphs_Dialog_GridLine_Warning_Message);
				_chkShowGrid_HorizontalLines.addSelectionListener(_defaultSelectionListener);
				GridDataFactory.fillDefaults()//
//						.indent(0, 15)
						.span(3, 1)
						.applyTo(_chkShowGrid_HorizontalLines);
			}
			{
				/*
				 * checkbox: show vertical grid
				 */
				_chkShowGrid_VerticalLines = new Button(group, SWT.CHECK);
				_chkShowGrid_VerticalLines.setText(Messages.Pref_Graphs_Checkbox_ShowVerticalGrid);
				_chkShowGrid_VerticalLines.setToolTipText(Messages.Pref_Graphs_Dialog_GridLine_Warning_Message);
				_chkShowGrid_VerticalLines.addSelectionListener(_defaultSelectionListener);
				GridDataFactory.fillDefaults()//
						.span(3, 1)
						.applyTo(_chkShowGrid_VerticalLines);
			}
		}
	}

	private void createUI_74_XAxisUnits(final Composite container) {

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
			_rdoShowDistance.addSelectionListener(_defaultSelectionListener);

			/*
			 * radio: time
			 */
			_rdoShowTime = new Button(group, SWT.RADIO);
			_rdoShowTime.setText(Messages.Pref_Graphs_Radio_show_time);
			_rdoShowTime.addSelectionListener(_defaultSelectionListener);

			// checkbox: start time
			_chkShowStartTime = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(_chkShowStartTime);
			_chkShowStartTime.setText(Messages.Pref_Graphs_Check_show_start_time);
			_chkShowStartTime.addSelectionListener(_defaultSelectionListener);
		}
	}

	private Control createUI_80_Tab_3_Options(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			createUI_82_MouseMode(container);
			createUI_84_ZoomOptions(container);
		}

		return container;
	}

	private void createUI_82_MouseMode(final Composite container) {

		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_mouse_mode);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);
		{
			// radio: zoom features
			_rdoMouseModeZoom = new Button(group, SWT.RADIO);
			_rdoMouseModeZoom.setText(Messages.Pref_Graphs_Radio_mouse_mode_zoom);
			_rdoMouseModeZoom.addSelectionListener(_defaultSelectionListener);

			// radio: slider features
			_rdoMouseModeSlider = new Button(group, SWT.RADIO);
			_rdoMouseModeSlider.setText(Messages.Pref_Graphs_Radio_mouse_mode_slider);
			_rdoMouseModeSlider.addSelectionListener(_defaultSelectionListener);
		}
	}

	/**
	 * group: zoom options
	 */
	private void createUI_84_ZoomOptions(final Composite container) {

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
			_chkZoomToSlider.addSelectionListener(_defaultSelectionListener);

			/*
			 * checkbox: move sliders to border when zoomed
			 */
			_chkMoveSlidersWhenZoomed = new Button(groupZoomOptions, SWT.CHECK);
			_chkMoveSlidersWhenZoomed.setText(Messages.Pref_Graphs_move_sliders_when_zoomed);
			_chkMoveSlidersWhenZoomed.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void createUI_99_LiveUpdate(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * Checkbox: live update
			 */
			_chkLiveUpdate = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_chkLiveUpdate);
			_chkLiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
			_chkLiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
			_chkLiveUpdate.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void doLiveUpdate() {

		if (_chkLiveUpdate.getSelection()) {
			performApply();
		}
	}

	private void enableActions() {

		_chkShowStartTime.setEnabled(_rdoShowTime.getSelection());
	}

	private void enableControls() {

		_colorSegmentAlternateColor.setEnabled(_chkSegmentAlternateColor.getSelection());
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
	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUI(final Composite parent) {

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onSelection();
			}
		};

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelection();
			}
		};

		_defaultChangePropertyListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onSelection();
			}
		};

// SET_FORMATTING_OFF
		
		// create a map and list with all available graphs
		final Graph graph_Altitude 						= new Graph(TourManager.GRAPH_ALTITUDE, 		GRAPH_LABEL_ALTITUDE);
		final Graph graph_Speed 						= new Graph(TourManager.GRAPH_SPEED, 			GRAPH_LABEL_SPEED);
		final Graph graph_Pace 							= new Graph(TourManager.GRAPH_PACE, 			GRAPH_LABEL_PACE);
		final Graph graph_Power 						= new Graph(TourManager.GRAPH_POWER, 			GRAPH_LABEL_POWER);
		final Graph graph_Pulse 						= new Graph(TourManager.GRAPH_PULSE, 			GRAPH_LABEL_HEARTBEAT);
		final Graph graph_Temperature 					= new Graph(TourManager.GRAPH_TEMPERATURE, 		GRAPH_LABEL_TEMPERATURE);
		final Graph graph_Cadence 						= new Graph(TourManager.GRAPH_CADENCE, 			GRAPH_LABEL_CADENCE);
		final Graph graph_Gears 						= new Graph(TourManager.GRAPH_GEARS, 			GRAPH_LABEL_GEARS);
		final Graph graph_Altimeter 					= new Graph(TourManager.GRAPH_ALTIMETER, 		GRAPH_LABEL_ALTIMETER);
		final Graph graph_Gradient 						= new Graph(TourManager.GRAPH_GRADIENT, 		GRAPH_LABEL_GRADIENT);
		
		final Graph graph_RunDyn_StanceTime				= new Graph(TourManager.GRAPH_RUN_DYN_STANCE_TIME, 			GRAPH_LABEL_RUN_DYN_STANCE_TIME);
		final Graph graph_RunDyn_StanceTimeBalance 		= new Graph(TourManager.GRAPH_RUN_DYN_STANCE_TIME_BALANCED, GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCED);
		final Graph graph_RunDyn_StepLength			 	= new Graph(TourManager.GRAPH_RUN_DYN_STEP_LENGTH, 			GRAPH_LABEL_RUN_DYN_STEP_LENGTH);
		final Graph graph_RunDyn_VerticalOscillation 	= new Graph(TourManager.GRAPH_RUN_DYN_VERTICAL_OSCILLATION, GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION);
		final Graph graph_RunDyn_VerticalRatio		 	= new Graph(TourManager.GRAPH_RUN_DYN_VERTICAL_RATIO, 		GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO);

		final Graph graph_TourCompareResult 			= new Graph(TourManager.GRAPH_TOUR_COMPARE, 	GRAPH_LABEL_TOUR_COMPARE_RESULT);

		_graphMap = new HashMap<Integer, Graph>();
		
		_graphMap.put(TourManager.GRAPH_ALTITUDE,						graph_Altitude);
		_graphMap.put(TourManager.GRAPH_SPEED, 							graph_Speed);
		_graphMap.put(TourManager.GRAPH_PACE, 							graph_Pace);
		_graphMap.put(TourManager.GRAPH_POWER, 							graph_Power);
		_graphMap.put(TourManager.GRAPH_PULSE, 							graph_Pulse);
		_graphMap.put(TourManager.GRAPH_TEMPERATURE, 	 	 	 	 	graph_Temperature);
		_graphMap.put(TourManager.GRAPH_CADENCE, 						graph_Cadence);
		_graphMap.put(TourManager.GRAPH_GEARS, 							graph_Gears);
		_graphMap.put(TourManager.GRAPH_ALTIMETER, 						graph_Altimeter);
		_graphMap.put(TourManager.GRAPH_GRADIENT, 		graph_Gradient);
		
		_graphMap.put(TourManager.GRAPH_RUN_DYN_STANCE_TIME, 			graph_RunDyn_StanceTime);
		_graphMap.put(TourManager.GRAPH_RUN_DYN_STANCE_TIME_BALANCED,	graph_RunDyn_StanceTimeBalance);
		_graphMap.put(TourManager.GRAPH_RUN_DYN_STEP_LENGTH, 			graph_RunDyn_StepLength);
		_graphMap.put(TourManager.GRAPH_RUN_DYN_VERTICAL_OSCILLATION, 	graph_RunDyn_VerticalOscillation);
		_graphMap.put(TourManager.GRAPH_RUN_DYN_VERTICAL_RATIO, 		graph_RunDyn_VerticalRatio);
		
		_graphMap.put(TourManager.GRAPH_TOUR_COMPARE,					graph_TourCompareResult);
		
// SET_FORMATTING_ON

		_graphList = new ArrayList<Graph>();

		_graphList.add(graph_Altitude);
		_graphList.add(graph_Speed);
		_graphList.add(graph_Pace);
		_graphList.add(graph_Power);
		_graphList.add(graph_Pulse);
		_graphList.add(graph_Temperature);
		_graphList.add(graph_Cadence);
		_graphList.add(graph_Gears);
		_graphList.add(graph_Altimeter);
		_graphList.add(graph_Gradient);

		_graphList.add(graph_RunDyn_StanceTime);
		_graphList.add(graph_RunDyn_StanceTimeBalance);
		_graphList.add(graph_RunDyn_StepLength);
		_graphList.add(graph_RunDyn_VerticalOscillation);
		_graphList.add(graph_RunDyn_VerticalRatio);

		_graphList.add(graph_TourCompareResult);
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

		saveState_UI();

		return super.okToLeave();
	}

	private void onSelection() {

		/*
		 * perform defaults for the currently selected tab
		 */
		final TabItem selectedTab = _tabFolder.getItem(_tabFolder.getSelectionIndex());

		if (selectedTab == _tab1_Graphs) {

		} else if (selectedTab == _tab2_Grid) {

		} else if (selectedTab == _tab3_Options) {

		}

		enableActions();
		enableControls();

		doLiveUpdate();
	}

	@Override
	public boolean performCancel() {

		saveState_UI();

		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		/*
		 * perform defaults for the currently selected tab
		 */
		final TabItem selectedTab = _tabFolder.getItem(_tabFolder.getSelectionIndex());

		if (selectedTab == _tab1_Graphs) {

			_chkGraphAntialiasing.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_ANTIALIASING));
			_chkShowHrZoneBackground.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE));

			_spinnerGraphTransparencyFilling.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING));
			_spinnerGraphTransparencyLine.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE));

			// segment alternate color
			_chkSegmentAlternateColor.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR));
			_colorSegmentAlternateColor.setColorValue(//
					PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR));

		} else if (selectedTab == _tab2_Grid) {

			_spinnerGridHorizontalDistance.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE));
			_spinnerGridVerticalDistance.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE));

			_chkShowGrid_HorizontalLines.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES));
			_chkShowGrid_VerticalLines.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES));
		}

		// live update
		_chkLiveUpdate.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE));

		super.performDefaults();

		onSelection();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		restoreState_Tab_1_Graphs();
		restoreState_Tab_2_Grid();
		restoreState_Tab_3_Options();

		// selected tab
		_tabFolder.setSelection(_prefStore.getInt(STATE_PREF_PAGE_CHART_GRAPHS_SELECTED_TAB));

		// live update
		_chkLiveUpdate.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE));

		enableControls();
	}

	private void restoreState_Tab_1_Graphs() {

		_chkGraphAntialiasing.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_ANTIALIASING));
		_chkShowHrZoneBackground.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE));

		_spinnerGraphTransparencyFilling.setSelection(_prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING));
		_spinnerGraphTransparencyLine.setSelection(_prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE));

		// segment alternate color
		_chkSegmentAlternateColor.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR));
		_colorSegmentAlternateColor.setColorValue(//
				PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR));

		restoreState_Tab_1_Graphs_Graphs();
	}

	private void restoreState_Tab_1_Graphs_Graphs() {

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

	private void restoreState_Tab_2_Grid() {

		/*
		 * Grid
		 */
		_spinnerGridHorizontalDistance.setSelection(_prefStore.getInt(ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE));
		_spinnerGridVerticalDistance.setSelection(_prefStore.getInt(ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE));

		_chkShowGrid_HorizontalLines.setSelection(_prefStore.getBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES));
		_chkShowGrid_VerticalLines.setSelection(_prefStore.getBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		/*
		 * Units
		 */
		if (_prefStore.getString(ITourbookPreferences.GRAPH_X_AXIS).equals(TourManager.X_AXIS_TIME)) {
			_rdoShowTime.setSelection(true);
		} else {
			_rdoShowDistance.setSelection(true);
		}

		_chkShowStartTime.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME));
	}

	private void restoreState_Tab_3_Options() {

		// mouse wheel mode
		_chkZoomToSlider.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER));
		_chkMoveSlidersWhenZoomed.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED));

		// zoom options
		if (_prefStore.getString(ITourbookPreferences.GRAPH_MOUSE_MODE).equals(Chart.MOUSE_MODE_SLIDER)) {
			_rdoMouseModeSlider.setSelection(true);
		} else {
			_rdoMouseModeZoom.setSelection(true);
		}
	}

	private void saveState() {

		saveState_Tab_1_Graphs();
		saveState_Tab_2_Grid();
		saveState_Tab_3_Options();

		// live update
		_prefStore.setValue(ITourbookPreferences.GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE, _chkLiveUpdate.getSelection());
	}

	private void saveState_Tab_1_Graphs() {

		_prefStore.setValue(ITourbookPreferences.GRAPH_ANTIALIASING, _chkGraphAntialiasing.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE, _chkShowHrZoneBackground.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING, _spinnerGraphTransparencyFilling.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE, _spinnerGraphTransparencyLine.getSelection());

		// segment alternate color
		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR, _chkSegmentAlternateColor.getSelection());
		PreferenceConverter.setValue(_prefStore,
				ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR,
				_colorSegmentAlternateColor.getColorValue());

		saveState_Tab_1_Graphs_Graphs();
	}

	/**
	 * get the graph id's from the preferences and check the graphs in the list
	 */
	private void saveState_Tab_1_Graphs_Graphs() {

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

	private void saveState_Tab_2_Grid() {

		_prefStore.setValue(ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE, _spinnerGridHorizontalDistance.getSelection());
		_prefStore.setValue(ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE, _spinnerGridVerticalDistance.getSelection());

		_prefStore.setValue(ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES, _chkShowGrid_HorizontalLines.getSelection());
		_prefStore.setValue(ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES, _chkShowGrid_VerticalLines.getSelection());

		if (_rdoShowTime.getSelection()) {
			_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_TIME);
		} else {
			_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);
		}

		_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME, _chkShowStartTime.getSelection());
	}

	private void saveState_Tab_3_Options() {

		// mouse wheel mode
		if (_rdoMouseModeSlider.getSelection()) {
			_prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_SLIDER);
		} else {
			_prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_ZOOM);
		}

		// zoom options
		_prefStore.setValue(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER, _chkZoomToSlider.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED, _chkMoveSlidersWhenZoomed.getSelection());
	}

	private void saveState_UI() {

		if (_tabFolder == null) {
			// this happened
			return;
		}

		// keep selected tab
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
