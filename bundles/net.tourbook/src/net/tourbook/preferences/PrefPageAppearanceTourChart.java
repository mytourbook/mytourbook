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

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.common.util.Util;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
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

/**
 * @author IBM_ADMIN
 */
public class PrefPageAppearanceTourChart extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID											= "net.tourbook.preferences.PrefPageChartGraphs";							//$NON-NLS-1$

	private static final String		GRAPH_LABEL_ALTIMETER						= net.tourbook.common.Messages.Graph_Label_Altimeter;
	private static final String		GRAPH_LABEL_ALTITUDE						= net.tourbook.common.Messages.Graph_Label_Altitude;
	private static final String		GRAPH_LABEL_CADENCE							= net.tourbook.common.Messages.Graph_Label_Cadence;
	private static final String		GRAPH_LABEL_CADENCE_UNIT					= net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
	private static final String		GRAPH_LABEL_GEARS							= net.tourbook.common.Messages.Graph_Label_Gears;
	private static final String		GRAPH_LABEL_GRADIENT						= net.tourbook.common.Messages.Graph_Label_Gradient;
	private static final String		GRAPH_LABEL_HEARTBEAT						= net.tourbook.common.Messages.Graph_Label_Heartbeat;
	private static final String		GRAPH_LABEL_HEARTBEAT_UNIT					= net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
	private static final String		GRAPH_LABEL_PACE							= net.tourbook.common.Messages.Graph_Label_Pace;
	private static final String		GRAPH_LABEL_POWER							= net.tourbook.common.Messages.Graph_Label_Power;
	private static final String		GRAPH_LABEL_POWER_UNIT						= net.tourbook.common.Messages.Graph_Label_Power_Unit;
	private static final String		GRAPH_LABEL_SHOW_HR_ZONE_BACKGROUND_TOOLTIP	= net.tourbook.common.Messages.Graph_Label_ShowHrZoneBackground_Tooltip;
	private static final String		GRAPH_LABEL_SHOW_HR_ZONE_BACKGROUND			= net.tourbook.common.Messages.Graph_Label_ShowHrZoneBackground;
	private static final String		GRAPH_LABEL_SPEED							= net.tourbook.common.Messages.Graph_Label_Speed;
	private static final String		GRAPH_LABEL_TEMPERATURE						= net.tourbook.common.Messages.Graph_Label_Temperature;

	private static final String		STATE_PREF_PAGE_CHART_GRAPHS_SELECTED_TAB	= "PrefPage.ChartGraphs.SelectedTab";										//$NON-NLS-1$

	private static final int		ALTIMETER_MIN								= -10000;
	private static final int		ALTIMETER_MAX								= 10000;
	private static final int		ALTITUDE_MIN								= -1000;
	private static final int		ALTITUDE_MAX								= 10000;
	private static final int		CADENCE_MAX									= 300;
	private static final int		GRADIENT_MIN								= -100;
	private static final int		GRADIENT_MAX								= 100;
	private static final int		PACE_MAX									= 100;
	private static final int		POWER_MAX									= 1000000;
	private static final int		SPEED_MAX									= 1000;
	private static final int		TEMPERATURE_MIN								= -100;
	private static final int		TEMPERATURE_MAX								= 100;

	private final IPreferenceStore	_prefStore									= TourbookPlugin.getPrefStore();

	private HashMap<Integer, Graph>	_graphMap;
	private ArrayList<Graph>		_graphList;
	private ArrayList<Graph>		_viewerGraphs;

	private IPropertyChangeListener	_defaultChangePropertyListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private SelectionAdapter		_defaultSelectionListener;

	private PixelConverter			_pc;
	private int						_columnSpacing;

	/*
	 * UI controls
	 */
	private TabFolder				_tabFolder;
	private TabItem					_tab1_Graphs;
	private TabItem					_tab2_MinMax;
	private TabItem					_tab3_Grid;
	private TabItem					_tab4_Options;

	private CheckboxTableViewer		_graphCheckboxList;

	private Button					_btnDown;
	private Button					_btnUp;

	private Button					_chkEnableMinMax;
	private Button					_chkGraphAntialiasing;
	private Button					_chkLiveUpdate;
	private Button					_chkMin_Altimeter;
	private Button					_chkMax_Altimeter;
	private Button					_chkMin_Altitude;
	private Button					_chkMax_Altitude;
	private Button					_chkMin_Cadence;
	private Button					_chkMax_Cadence;
	private Button					_chkMin_Gradient;
	private Button					_chkMax_Gradient;
	private Button					_chkMin_Pace;
	private Button					_chkMax_Pace;
	private Button					_chkMin_Power;
	private Button					_chkMax_Power;
	private Button					_chkMin_Pulse;
	private Button					_chkMax_Pulse;
	private Button					_chkMax_Speed;
	private Button					_chkMin_Speed;
	private Button					_chkMin_Temperature;
	private Button					_chkMax_Temperature;
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
	private Label					_lblMaxValue;
	private Label					_lblMinValue;
	private Label					_lblMinMax_Altimeter;
	private Label					_lblMinMax_AltimeterUnit;
	private Label					_lblMinMax_Altitude;
	private Label					_lblMinMax_AltitudeUnit;
	private Label					_lblMinMax_Cadence;
	private Label					_lblMinMax_CadenceUnit;
	private Label					_lblMinMax_Gradient;
	private Label					_lblMinMax_GradientUnit;
	private Label					_lblMinMax_Pulse;
	private Label					_lblMinMax_PulseUnit;
	private Label					_lblMinMax_Pace;
	private Label					_lblMinMax_PaceUnit;
	private Label					_lblMinMax_Power;
	private Label					_lblMinMax_PowerUnit;
	private Label					_lblMinMax_Speed;
	private Label					_lblMinMax_SpeedUnit;
	private Label					_lblMinMax_Temperature;
	private Label					_lblMinMax_TemperatureUnit;

	private Button					_rdoMouseModeSlider;
	private Button					_rdoMouseModeZoom;
	private Button					_rdoShowDistance;
	private Button					_rdoShowTime;

	private Spinner					_spinnerGraphTransparencyLine;
	private Spinner					_spinnerGraphTransparencyFilling;
	private Spinner					_spinnerGridHorizontalDistance;
	private Spinner					_spinnerGridVerticalDistance;
	private Spinner					_spinnerMin_Altimeter;
	private Spinner					_spinnerMax_Altimeter;
	private Spinner					_spinnerMin_Altitude;
	private Spinner					_spinnerMax_Altitude;
	private Spinner					_spinnerMin_Cadence;
	private Spinner					_spinnerMax_Cadence;
	private Spinner					_spinnerMin_Gradient;
	private Spinner					_spinnerMax_Gradient;
	private Spinner					_spinnerMin_Pace;
	private Spinner					_spinnerMax_Pace;
	private Spinner					_spinnerMin_Power;
	private Spinner					_spinnerMax_Power;
	private Spinner					_spinnerMin_Pulse;
	private Spinner					_spinnerMax_Pulse;
	private Spinner					_spinnerMin_Speed;
	private Spinner					_spinnerMax_Speed;
	private Spinner					_spinnerMin_Temperature;
	private Spinner					_spinnerMax_Temperature;

	private ColorSelector			_colorSegmentAlternateColor;

	private Image					_imageAltimeter;
	private Image					_imageAltitude;
	private Image					_imageCadence;
	private Image					_imageGradient;
	private Image					_imagePace;
	private Image					_imagePower;
	private Image					_imagePulse;
	private Image					_imageSpeed;
	private Image					_imageTemperature;

	private Image					_imageAltimeterDisabled;
	private Image					_imageAltitudeDisabled;
	private Image					_imageCadenceDisabled;
	private Image					_imageGradientDisabled;
	private Image					_imagePaceDisabled;
	private Image					_imagePowerDisabled;
	private Image					_imagePulseDisabled;
	private Image					_imageSpeedDisabled;
	private Image					_imageTemperatureDisabled;

	private CLabel					_iconAltitude;
	private CLabel					_iconAltimeter;
	private CLabel					_iconGradient;
	private CLabel					_iconPulse;
	private CLabel					_iconSpeed;
	private CLabel					_iconPace;
	private CLabel					_iconCadence;
	private CLabel					_iconPower;
	private CLabel					_iconTemperature;

	{

		_imageAltimeter = TourbookPlugin.getImageDescriptor(Messages.Image__graph_altimeter).createImage();
		_imageAltitude = TourbookPlugin.getImageDescriptor(Messages.Image__graph_altitude).createImage();
		_imageCadence = TourbookPlugin.getImageDescriptor(Messages.Image__graph_cadence).createImage();
		_imageGradient = TourbookPlugin.getImageDescriptor(Messages.Image__graph_gradient).createImage();
		_imagePace = TourbookPlugin.getImageDescriptor(Messages.Image__graph_pace).createImage();
		_imagePower = TourbookPlugin.getImageDescriptor(Messages.Image__graph_power).createImage();
		_imagePulse = TourbookPlugin.getImageDescriptor(Messages.Image__graph_heartbeat).createImage();
		_imageSpeed = TourbookPlugin.getImageDescriptor(Messages.Image__graph_speed).createImage();
		_imageTemperature = TourbookPlugin.getImageDescriptor(Messages.Image__graph_temperature).createImage();

		_imageAltimeterDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_altimeter_disabled)//
				.createImage();
		_imageAltitudeDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_altitude_disabled)//
				.createImage();
		_imageCadenceDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_cadence_disabled)//
				.createImage();
		_imageGradientDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_gradient_disabled)//
				.createImage();
		_imagePaceDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_pace_disabled)//
				.createImage();
		_imagePowerDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_power_disabled)//
				.createImage();
		_imagePulseDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_heartbeat_disabled)//
				.createImage();
		_imageSpeedDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_speed_disabled)//
				.createImage();
		_imageTemperatureDisabled = TourbookPlugin.getImageDescriptor(Messages.Image__graph_temperature_disabled)//
				.createImage();
	}

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

				_tab2_MinMax = new TabItem(_tabFolder, SWT.NONE);
				_tab2_MinMax.setText(Messages.Pref_Graphs_Tab_default_values);
				_tab2_MinMax.setControl(createUI_50_Tab_2_MinMax(_tabFolder));

				_tab3_Grid = new TabItem(_tabFolder, SWT.NONE);
				_tab3_Grid.setText(Messages.Pref_Graphs_Tab_Grid);
				_tab3_Grid.setControl(createUI_70_Tab_3_Grid(_tabFolder));

				_tab4_Options = new TabItem(_tabFolder, SWT.NONE);
				_tab4_Options.setText(Messages.Pref_Graphs_Tab_zoom_options);
				_tab4_Options.setControl(createUI_80_Tab_4_Options(_tabFolder));
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

	private Control createUI_50_Tab_2_MinMax(final Composite parent) {

		_columnSpacing = _pc.convertWidthInCharsToPixels(4);

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			final Group group = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
			GridLayoutFactory.swtDefaults()//
					.numColumns(7)
					.spacing(_pc.convertHorizontalDLUsToPixels(4), _pc.convertVerticalDLUsToPixels(4))
					.applyTo(group);
			group.setText(Messages.Pref_Graphs_force_minimum_value);
//			group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_52_MinMax_Enable(group);
				createUI_54_MinMax_Header(group);

				createUI_61_MinMax_Altitude(group);
				createUI_64_MinMax_Pulse(group);
				createUI_65_MinMax_Speed(group);
				createUI_66_MinMax_Pace(group);
				createUI_68_MinMax_Power(group);
				createUI_69_MinMax_Temperature(group);
				createUI_63_MinMax_Gradient(group);
				createUI_62_MinMax_Altimeter(group);
				createUI_67_MinMax_Cadence(group);
			}
		}

		return container;
	}

	private void createUI_52_MinMax_Enable(final Group parent) {

		// ckeckbox: enable min/max
		_chkEnableMinMax = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults()//
				.span(7, 1)
				.applyTo(_chkEnableMinMax);
		_chkEnableMinMax.setText(Messages.Pref_Graphs_Checkbox_EnableMinMaxValues);
		_chkEnableMinMax.addSelectionListener(_defaultSelectionListener);
	}

	private void createUI_54_MinMax_Header(final Group parent) {

		// label: spacer
		new Label(parent, SWT.NONE);
		new Label(parent, SWT.NONE);

		// label: min value
		_lblMinValue = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.indent(_columnSpacing, 0)
				.applyTo(_lblMinValue);
		_lblMinValue.setText(Messages.Pref_Graphs_Label_MinValue);

		// label: max value
		_lblMaxValue = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.indent(_columnSpacing, 0)
				.applyTo(_lblMaxValue);
		_lblMaxValue.setText(Messages.Pref_Graphs_Label_MaxValue);

		// label: spacer
		new Label(parent, SWT.NONE);
	}

	private void createUI_61_MinMax_Altitude(final Group parent) {

		_iconAltitude = createUI_Icon(parent, _imageAltitude);

		_lblMinMax_Altitude = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Altitude);

		_chkMin_Altitude = createUI_Checkbox(parent);
		_spinnerMin_Altitude = createUI_Spinner(parent, //
				ALTITUDE_MIN,
				ALTITUDE_MAX);

		_chkMax_Altitude = createUI_Checkbox(parent);
		_spinnerMax_Altitude = createUI_Spinner(parent, //
				ALTITUDE_MIN,
				ALTITUDE_MAX);

		_lblMinMax_AltitudeUnit = createUI_Label(parent, UI.UNIT_LABEL_ALTITUDE);
	}

	private void createUI_62_MinMax_Altimeter(final Group parent) {

		_iconAltimeter = createUI_Icon(parent, _imageAltimeter);

		_lblMinMax_Altimeter = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceAltimeterValue);

		_chkMin_Altimeter = createUI_Checkbox(parent);
		_spinnerMin_Altimeter = createUI_Spinner(parent, //
				ALTIMETER_MIN,
				ALTIMETER_MAX);

		_chkMax_Altimeter = createUI_Checkbox(parent);
		_spinnerMax_Altimeter = createUI_Spinner(parent, //
				ALTIMETER_MIN,
				ALTIMETER_MAX);

		_lblMinMax_AltimeterUnit = createUI_Label(parent, UI.UNIT_LABEL_ALTIMETER);
	}

	private void createUI_63_MinMax_Gradient(final Group parent) {

		_iconGradient = createUI_Icon(parent, _imageGradient);

		_lblMinMax_Gradient = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceGradientValue);

		_chkMin_Gradient = createUI_Checkbox(parent);
		_spinnerMin_Gradient = createUI_Spinner(parent, //
				GRADIENT_MIN,
				GRADIENT_MAX);

		_chkMax_Gradient = createUI_Checkbox(parent);
		_spinnerMax_Gradient = createUI_Spinner(parent, //
				GRADIENT_MIN,
				GRADIENT_MAX);

		_lblMinMax_GradientUnit = createUI_Label(parent, UI.SYMBOL_PERCENTAGE);
	}

	private void createUI_64_MinMax_Pulse(final Group parent) {

		_iconPulse = createUI_Icon(parent, _imagePulse);

		_lblMinMax_Pulse = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForcePulseValue);

		_chkMin_Pulse = createUI_Checkbox(parent);
		_spinnerMin_Pulse = createUI_Spinner(parent, //
				PrefPagePeople.HEART_BEAT_MIN,
				PrefPagePeople.HEART_BEAT_MAX);

		_chkMax_Pulse = createUI_Checkbox(parent);
		_spinnerMax_Pulse = createUI_Spinner(parent, //
				PrefPagePeople.HEART_BEAT_MIN,
				PrefPagePeople.HEART_BEAT_MAX);

		_lblMinMax_PulseUnit = createUI_Label(parent, GRAPH_LABEL_HEARTBEAT_UNIT);
	}

	private void createUI_65_MinMax_Speed(final Group parent) {

		_iconSpeed = createUI_Icon(parent, _imageSpeed);

		_lblMinMax_Speed = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Speed);

		_chkMin_Speed = createUI_Checkbox(parent);
		_spinnerMin_Speed = createUI_Spinner(parent, 0, SPEED_MAX);

		_chkMax_Speed = createUI_Checkbox(parent);
		_spinnerMax_Speed = createUI_Spinner(parent, 0, SPEED_MAX);

		_lblMinMax_SpeedUnit = createUI_Label(parent, UI.UNIT_LABEL_SPEED);
	}

	private void createUI_66_MinMax_Pace(final Group parent) {

		_iconPace = createUI_Icon(parent, _imagePace);

		_lblMinMax_Pace = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForcePaceValue);

		_chkMin_Pace = createUI_Checkbox(parent);
		_spinnerMin_Pace = createUI_Spinner(parent, 0, PACE_MAX);

		_chkMax_Pace = createUI_Checkbox(parent);
		_spinnerMax_Pace = createUI_Spinner(parent, 0, PACE_MAX);

		_lblMinMax_PaceUnit = createUI_Label(parent, Messages.app_unit_minutes);
	}

	private void createUI_67_MinMax_Cadence(final Group parent) {

		_iconCadence = createUI_Icon(parent, _imageCadence);

		_lblMinMax_Cadence = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Cadence);

		_chkMin_Cadence = createUI_Checkbox(parent);
		_spinnerMin_Cadence = createUI_Spinner(parent, 0, CADENCE_MAX);

		_chkMax_Cadence = createUI_Checkbox(parent);
		_spinnerMax_Cadence = createUI_Spinner(parent, 0, CADENCE_MAX);

		_lblMinMax_CadenceUnit = createUI_Label(parent, GRAPH_LABEL_CADENCE_UNIT);
	}

	private void createUI_68_MinMax_Power(final Group parent) {

		_iconPower = createUI_Icon(parent, _imagePower);

		_lblMinMax_Power = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Power);

		_chkMin_Power = createUI_Checkbox(parent);
		_spinnerMin_Power = createUI_Spinner(parent, 0, POWER_MAX);

		_chkMax_Power = createUI_Checkbox(parent);
		_spinnerMax_Power = createUI_Spinner(parent, 0, POWER_MAX);

		_lblMinMax_PowerUnit = createUI_Label(parent, GRAPH_LABEL_POWER_UNIT);
	}

	private void createUI_69_MinMax_Temperature(final Group parent) {

		_iconTemperature = createUI_Icon(parent, _imageTemperature);

		_lblMinMax_Temperature = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Temperature);

		_chkMin_Temperature = createUI_Checkbox(parent);
		_spinnerMin_Temperature = createUI_Spinner(parent, //
				TEMPERATURE_MIN,
				TEMPERATURE_MAX);

		_chkMax_Temperature = createUI_Checkbox(parent);
		_spinnerMax_Temperature = createUI_Spinner(parent, //
				TEMPERATURE_MIN,
				TEMPERATURE_MAX);

		_lblMinMax_TemperatureUnit = createUI_Label(parent, UI.UNIT_LABEL_TEMPERATURE);
	}

	/**
	 * tab: grid/units
	 * 
	 * @return
	 */
	private Control createUI_70_Tab_3_Grid(final Composite parent) {

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

	private Control createUI_80_Tab_4_Options(final Composite parent) {

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

	private Button createUI_Checkbox(final Group parent) {

		// ckeckbox
		final Button checkbox = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults()//
				.indent(_columnSpacing, 0)
				.applyTo(checkbox);
		checkbox.addSelectionListener(_defaultSelectionListener);

		return checkbox;
	}

	private CLabel createUI_Icon(final Group parent, final Image image) {

		final CLabel icon = new CLabel(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(16, 0).applyTo(icon);
		icon.setImage(image);

		return icon;
	}

	private Label createUI_Label(final Group parent, final String text) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		label.setText(text);

		return label;
	}

	private Spinner createUI_Spinner(final Group parent, final int minValue, final int maxValue) {

		final Spinner spinner = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.align(SWT.END, SWT.FILL)
				.applyTo(spinner);

		spinner.setMinimum(minValue);
		spinner.setMaximum(maxValue);

		spinner.addMouseWheelListener(_defaultMouseWheelListener);
		spinner.addSelectionListener(_defaultSelectionListener);

		return spinner;
	}

	@Override
	public void dispose() {

		Util.disposeResource(_imageAltimeter);
		Util.disposeResource(_imageAltitude);
		Util.disposeResource(_imageCadence);
		Util.disposeResource(_imageGradient);
		Util.disposeResource(_imagePace);
		Util.disposeResource(_imagePower);
		Util.disposeResource(_imagePulse);
		Util.disposeResource(_imageSpeed);
		Util.disposeResource(_imageTemperature);

		Util.disposeResource(_imageAltimeterDisabled);
		Util.disposeResource(_imageAltitudeDisabled);
		Util.disposeResource(_imageCadenceDisabled);
		Util.disposeResource(_imageGradientDisabled);
		Util.disposeResource(_imagePaceDisabled);
		Util.disposeResource(_imagePowerDisabled);
		Util.disposeResource(_imagePulseDisabled);
		Util.disposeResource(_imageSpeedDisabled);
		Util.disposeResource(_imageTemperatureDisabled);

		super.dispose();
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

		final boolean isMinMaxEnabled = _chkEnableMinMax.getSelection();

		_lblMinValue.setEnabled(isMinMaxEnabled);
		_lblMaxValue.setEnabled(isMinMaxEnabled);

		_lblMinMax_Altimeter.setEnabled(isMinMaxEnabled);
		_lblMinMax_AltimeterUnit.setEnabled(isMinMaxEnabled);
		_lblMinMax_Altitude.setEnabled(isMinMaxEnabled);
		_lblMinMax_AltitudeUnit.setEnabled(isMinMaxEnabled);
		_lblMinMax_Cadence.setEnabled(isMinMaxEnabled);
		_lblMinMax_CadenceUnit.setEnabled(isMinMaxEnabled);
		_lblMinMax_Gradient.setEnabled(isMinMaxEnabled);
		_lblMinMax_GradientUnit.setEnabled(isMinMaxEnabled);
		_lblMinMax_Pace.setEnabled(isMinMaxEnabled);
		_lblMinMax_PaceUnit.setEnabled(isMinMaxEnabled);
		_lblMinMax_Power.setEnabled(isMinMaxEnabled);
		_lblMinMax_PowerUnit.setEnabled(isMinMaxEnabled);
		_lblMinMax_Pulse.setEnabled(isMinMaxEnabled);
		_lblMinMax_PulseUnit.setEnabled(isMinMaxEnabled);
		_lblMinMax_Speed.setEnabled(isMinMaxEnabled);
		_lblMinMax_SpeedUnit.setEnabled(isMinMaxEnabled);
		_lblMinMax_Temperature.setEnabled(isMinMaxEnabled);
		_lblMinMax_TemperatureUnit.setEnabled(isMinMaxEnabled);

		_chkMin_Altimeter.setEnabled(isMinMaxEnabled);
		_chkMax_Altimeter.setEnabled(isMinMaxEnabled);
		_chkMin_Altitude.setEnabled(isMinMaxEnabled);
		_chkMax_Altitude.setEnabled(isMinMaxEnabled);
		_chkMin_Cadence.setEnabled(isMinMaxEnabled);
		_chkMax_Cadence.setEnabled(isMinMaxEnabled);
		_chkMin_Gradient.setEnabled(isMinMaxEnabled);
		_chkMax_Gradient.setEnabled(isMinMaxEnabled);
		_chkMin_Pace.setEnabled(isMinMaxEnabled);
		_chkMax_Pace.setEnabled(isMinMaxEnabled);
		_chkMin_Power.setEnabled(isMinMaxEnabled);
		_chkMax_Power.setEnabled(isMinMaxEnabled);
		_chkMin_Pulse.setEnabled(isMinMaxEnabled);
		_chkMax_Pulse.setEnabled(isMinMaxEnabled);
		_chkMin_Speed.setEnabled(isMinMaxEnabled);
		_chkMax_Speed.setEnabled(isMinMaxEnabled);
		_chkMin_Temperature.setEnabled(isMinMaxEnabled);
		_chkMax_Temperature.setEnabled(isMinMaxEnabled);

		_iconAltimeter.setImage(isMinMaxEnabled ? _imageAltimeter : _imageAltimeterDisabled);
		_iconAltitude.setImage(isMinMaxEnabled ? _imageAltitude : _imageAltitudeDisabled);
		_iconCadence.setImage(isMinMaxEnabled ? _imageCadence : _imageCadenceDisabled);
		_iconGradient.setImage(isMinMaxEnabled ? _imageGradient : _imageGradientDisabled);
		_iconPace.setImage(isMinMaxEnabled ? _imagePace : _imagePaceDisabled);
		_iconPower.setImage(isMinMaxEnabled ? _imagePower : _imagePowerDisabled);
		_iconPulse.setImage(isMinMaxEnabled ? _imagePulse : _imagePulseDisabled);
		_iconSpeed.setImage(isMinMaxEnabled ? _imageSpeed : _imageSpeedDisabled);
		_iconTemperature.setImage(isMinMaxEnabled ? _imageTemperature : _imageTemperatureDisabled);

		_spinnerMin_Altimeter.setEnabled(isMinMaxEnabled && _chkMin_Altimeter.getSelection());
		_spinnerMax_Altimeter.setEnabled(isMinMaxEnabled && _chkMax_Altimeter.getSelection());
		_spinnerMin_Altitude.setEnabled(isMinMaxEnabled && _chkMin_Altitude.getSelection());
		_spinnerMax_Altitude.setEnabled(isMinMaxEnabled && _chkMax_Altitude.getSelection());
		_spinnerMin_Cadence.setEnabled(isMinMaxEnabled && _chkMin_Cadence.getSelection());
		_spinnerMax_Cadence.setEnabled(isMinMaxEnabled && _chkMax_Cadence.getSelection());
		_spinnerMin_Gradient.setEnabled(isMinMaxEnabled && _chkMin_Gradient.getSelection());
		_spinnerMax_Gradient.setEnabled(isMinMaxEnabled && _chkMax_Gradient.getSelection());
		_spinnerMin_Pace.setEnabled(isMinMaxEnabled && _chkMin_Pace.getSelection());
		_spinnerMax_Pace.setEnabled(isMinMaxEnabled && _chkMax_Pace.getSelection());
		_spinnerMin_Power.setEnabled(isMinMaxEnabled && _chkMin_Power.getSelection());
		_spinnerMax_Power.setEnabled(isMinMaxEnabled && _chkMax_Power.getSelection());
		_spinnerMin_Pulse.setEnabled(isMinMaxEnabled && _chkMin_Pulse.getSelection());
		_spinnerMax_Pulse.setEnabled(isMinMaxEnabled && _chkMax_Pulse.getSelection());
		_spinnerMin_Speed.setEnabled(isMinMaxEnabled && _chkMin_Speed.getSelection());
		_spinnerMax_Speed.setEnabled(isMinMaxEnabled && _chkMax_Speed.getSelection());
		_spinnerMin_Temperature.setEnabled(isMinMaxEnabled && _chkMin_Temperature.getSelection());
		_spinnerMax_Temperature.setEnabled(isMinMaxEnabled && _chkMax_Temperature.getSelection());

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

		_pc = new PixelConverter(parent);

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

		// create a map and list with all available graphs
		final Graph graphAltitude = new Graph(TourManager.GRAPH_ALTITUDE, GRAPH_LABEL_ALTITUDE);
		final Graph graphSpeed = new Graph(TourManager.GRAPH_SPEED, GRAPH_LABEL_SPEED);
		final Graph graphPace = new Graph(TourManager.GRAPH_PACE, GRAPH_LABEL_PACE);
		final Graph graphPower = new Graph(TourManager.GRAPH_POWER, GRAPH_LABEL_POWER);
		final Graph graphPulse = new Graph(TourManager.GRAPH_PULSE, GRAPH_LABEL_HEARTBEAT);
		final Graph graphTemperature = new Graph(TourManager.GRAPH_TEMPERATURE, GRAPH_LABEL_TEMPERATURE);
		final Graph graphCadence = new Graph(TourManager.GRAPH_CADENCE, GRAPH_LABEL_CADENCE);
		final Graph graphGears = new Graph(TourManager.GRAPH_GEARS, GRAPH_LABEL_GEARS);
		final Graph graphAltimeter = new Graph(TourManager.GRAPH_ALTIMETER, GRAPH_LABEL_ALTIMETER);
		final Graph graphGradient = new Graph(TourManager.GRAPH_GRADIENT, GRAPH_LABEL_GRADIENT);

		_graphMap = new HashMap<Integer, Graph>();
		_graphMap.put(TourManager.GRAPH_ALTITUDE, graphAltitude);
		_graphMap.put(TourManager.GRAPH_SPEED, graphSpeed);
		_graphMap.put(TourManager.GRAPH_PACE, graphPace);
		_graphMap.put(TourManager.GRAPH_POWER, graphPower);
		_graphMap.put(TourManager.GRAPH_PULSE, graphPulse);
		_graphMap.put(TourManager.GRAPH_TEMPERATURE, graphTemperature);
		_graphMap.put(TourManager.GRAPH_CADENCE, graphCadence);
		_graphMap.put(TourManager.GRAPH_GEARS, graphGears);
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
		_graphList.add(graphGears);
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

		saveState_UI();

		return super.okToLeave();
	}

	private void onSelection() {

		/*
		 * perform defaults for the currently selected tab
		 */
		final TabItem selectedTab = _tabFolder.getItem(_tabFolder.getSelectionIndex());

		if (selectedTab == _tab1_Graphs) {

		} else if (selectedTab == _tab3_Grid) {

		} else if (selectedTab == _tab2_MinMax) {

			validateMinMax();

		} else if (selectedTab == _tab4_Options) {

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

			_chkGraphAntialiasing.setSelection(//
					_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_ANTIALIASING));

			_chkShowHrZoneBackground.setSelection(//
					_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE));

			_spinnerGraphTransparencyFilling.setSelection(//
					_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING));

			_spinnerGraphTransparencyLine.setSelection(//
					_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE));

			// segment alternate color
			_chkSegmentAlternateColor.setSelection(//
					_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR));
			_colorSegmentAlternateColor.setColorValue(//
					PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR));

		} else if (selectedTab == _tab3_Grid) {

			_spinnerGridHorizontalDistance.setSelection(//
					_prefStore.getDefaultInt(ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE));
			_spinnerGridVerticalDistance.setSelection(//
					_prefStore.getDefaultInt(ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE));

			_chkShowGrid_HorizontalLines.setSelection(//
					_prefStore.getDefaultBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES));
			_chkShowGrid_VerticalLines.setSelection(//
					_prefStore.getDefaultBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		} else if (selectedTab == _tab2_MinMax) {

			_chkEnableMinMax.setSelection(//
					_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED));

			// min/max altitude
			prefRestoreDefault(_chkMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE);

			// min/max pulse
			prefRestoreDefault(_chkMin_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Pulse, ITourbookPreferences.GRAPH_PULSE_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Pulse, ITourbookPreferences.GRAPH_PULSE_MAX_VALUE);

			// min/max speed
			prefRestoreDefault(_chkMin_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Speed, ITourbookPreferences.GRAPH_SPEED_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Speed, ITourbookPreferences.GRAPH_SPEED_MAX_VALUE);

			// min/max pace
			prefRestoreDefault(_chkMin_Pace, ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Pace, ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);

			// min/max cadence
			prefRestoreDefault(_chkMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE);

			// min/max gradient
			prefRestoreDefault(_chkMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE);

			// min/max altimeter
			prefRestoreDefault(_chkMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);

			// min/max power
			prefRestoreDefault(_chkMin_Power, ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Power, ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Power, ITourbookPreferences.GRAPH_POWER_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Power, ITourbookPreferences.GRAPH_POWER_MAX_VALUE);

			// min/max temperature
			prefRestoreDefault(_chkMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED);
			prefRestoreDefault(_chkMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED);
			prefRestoreDefault(_spinnerMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE);
			prefRestoreDefault(_spinnerMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE);
		}

		// live update
		_chkLiveUpdate.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE));

		super.performDefaults();

		onSelection();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void prefRestoreDefault(final Button button, final String prefName) {
		button.setSelection(_prefStore.getDefaultBoolean(prefName));
	}

	private void prefRestoreDefault(final Spinner spinner, final String prefName) {
		spinner.setSelection(_prefStore.getDefaultInt(prefName));
	}

	private void prefRestoreValue(final Button button, final String prefName) {
		button.setSelection(_prefStore.getBoolean(prefName));
	}

	private void prefRestoreValue(final Spinner spinner, final String prefName) {
		spinner.setSelection(_prefStore.getInt(prefName));
	}

	private void prefSaveValue(final Button button, final String prefName) {
		_prefStore.setValue(prefName, button.getSelection());
	}

	private void prefSaveValue(final Spinner spinner, final String prefName) {
		_prefStore.setValue(prefName, spinner.getSelection());
	}

	private void restoreState() {

		restoreState_Tab_1_Graphs();
		restoreState_Tab_2_MinMax();
		restoreState_Tab_3_Grid();
		restoreState_Tab_4_Options();

		// selected tab
		_tabFolder.setSelection(_prefStore.getInt(STATE_PREF_PAGE_CHART_GRAPHS_SELECTED_TAB));

		// live update
		_chkLiveUpdate.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE));

		enableControls();
	}

	private void restoreState_Tab_1_Graphs() {

		_chkGraphAntialiasing.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_ANTIALIASING));

		_chkShowHrZoneBackground.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE));

		_spinnerGraphTransparencyFilling.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING));

		_spinnerGraphTransparencyLine.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE));

		// segment alternate color
		_chkSegmentAlternateColor.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR));
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

	private void restoreState_Tab_2_MinMax() {

		_chkEnableMinMax.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED));

		// min/max altitude
		prefRestoreValue(_chkMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE);

		// min/max pulse
		prefRestoreValue(_chkMin_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Pulse, ITourbookPreferences.GRAPH_PULSE_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Pulse, ITourbookPreferences.GRAPH_PULSE_MAX_VALUE);

		// min/max speed
		prefRestoreValue(_chkMin_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Speed, ITourbookPreferences.GRAPH_SPEED_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Speed, ITourbookPreferences.GRAPH_SPEED_MAX_VALUE);

		// min/max pace
		prefRestoreValue(_chkMin_Pace, ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Pace, ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);

		// min/max cadence
		prefRestoreValue(_chkMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE);

		// min/max gradient
		prefRestoreValue(_chkMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE);

		// min/max altimeter
		prefRestoreValue(_chkMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);

		// min/max power
		prefRestoreValue(_chkMin_Power, ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Power, ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Power, ITourbookPreferences.GRAPH_POWER_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Power, ITourbookPreferences.GRAPH_POWER_MAX_VALUE);

		// min/max temperature
		prefRestoreValue(_chkMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE);
	}

	private void restoreState_Tab_3_Grid() {

		/*
		 * Grid
		 */
		_spinnerGridHorizontalDistance.setSelection(//
				_prefStore.getInt(ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE));
		_spinnerGridVerticalDistance.setSelection(//
				_prefStore.getInt(ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE));

		_chkShowGrid_HorizontalLines.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES));
		_chkShowGrid_VerticalLines.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES));

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

	private void restoreState_Tab_4_Options() {

		// mouse wheel mode
		_chkZoomToSlider.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER));
		_chkMoveSlidersWhenZoomed.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED));

		// zoom options
		if (_prefStore.getString(ITourbookPreferences.GRAPH_MOUSE_MODE).equals(Chart.MOUSE_MODE_SLIDER)) {
			_rdoMouseModeSlider.setSelection(true);
		} else {
			_rdoMouseModeZoom.setSelection(true);
		}
	}

	private void saveState() {

		saveState_Tab_1_Graphs();
		saveState_Tab_2_MinMax();
		saveState_Tab_3_Grid();
		saveState_Tab_4_Options();

		// live update
		_prefStore.setValue(
				ITourbookPreferences.GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE,
				_chkLiveUpdate.getSelection());
	}

	private void saveState_Tab_1_Graphs() {

		_prefStore.setValue(ITourbookPreferences.GRAPH_ANTIALIASING,//
				_chkGraphAntialiasing.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE,//
				_chkShowHrZoneBackground.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING, //
				_spinnerGraphTransparencyFilling.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE, //
				_spinnerGraphTransparencyLine.getSelection());

		// segment alternate color
		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR,//
				_chkSegmentAlternateColor.getSelection());
		PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR,//
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

	private void saveState_Tab_2_MinMax() {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED, _chkEnableMinMax.getSelection());

		// min/max altitude
		prefSaveValue(_chkMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE);
		prefSaveValue(_spinnerMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE);

		// min/max pulse
		prefSaveValue(_chkMin_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Pulse, ITourbookPreferences.GRAPH_PULSE_MIN_VALUE);
		prefSaveValue(_spinnerMax_Pulse, ITourbookPreferences.GRAPH_PULSE_MAX_VALUE);

		// min/max speed
		prefSaveValue(_chkMin_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Speed, ITourbookPreferences.GRAPH_SPEED_MIN_VALUE);
		prefSaveValue(_spinnerMax_Speed, ITourbookPreferences.GRAPH_SPEED_MAX_VALUE);

		// min/max pace
		prefSaveValue(_chkMin_Pace, ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Pace, ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
		prefSaveValue(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);

		// min/max cadence
		prefSaveValue(_chkMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE);
		prefSaveValue(_spinnerMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE);

		// min/max gradient
		prefSaveValue(_chkMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE);
		prefSaveValue(_spinnerMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE);

		// min/max altimeter
		prefSaveValue(_chkMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);
		prefSaveValue(_spinnerMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);

		// min/max power
		prefSaveValue(_chkMin_Power, ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Power, ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Power, ITourbookPreferences.GRAPH_POWER_MIN_VALUE);
		prefSaveValue(_spinnerMax_Power, ITourbookPreferences.GRAPH_POWER_MAX_VALUE);

		// min/max temperature
		prefSaveValue(_chkMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE);
		prefSaveValue(_spinnerMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE);
	}

	private void saveState_Tab_3_Grid() {

		_prefStore.setValue(ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE, //
				_spinnerGridHorizontalDistance.getSelection());
		_prefStore.setValue(ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE, //
				_spinnerGridVerticalDistance.getSelection());

		_prefStore.setValue(ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES, //
				_chkShowGrid_HorizontalLines.getSelection());
		_prefStore.setValue(ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES, //
				_chkShowGrid_VerticalLines.getSelection());

		if (_rdoShowTime.getSelection()) {
			_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_TIME);
		} else {
			_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);
		}

		_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME, //
				_chkShowStartTime.getSelection());
	}

	private void saveState_Tab_4_Options() {

		// mouse wheel mode
		if (_rdoMouseModeSlider.getSelection()) {
			_prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_SLIDER);
		} else {
			_prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_ZOOM);
		}

		// zoom options
		_prefStore.setValue(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER, //
				_chkZoomToSlider.getSelection());

		_prefStore.setValue(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED, //
				_chkMoveSlidersWhenZoomed.getSelection());
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

	private void validateMinMax() {

		if (_chkEnableMinMax.getSelection() == false) {

			// min/max is disabled
			return;
		}

		if (_chkMin_Altimeter.getSelection()) {

			final int min = _spinnerMin_Altimeter.getSelection();
			final int max = _spinnerMax_Altimeter.getSelection();

			if (min >= max) {

				if (max == ALTIMETER_MAX) {
					_spinnerMin_Altimeter.setSelection(max - 1);
				} else {
					_spinnerMax_Altimeter.setSelection(min + 1);
				}
			}
		}

		if (_chkMin_Gradient.getSelection()) {

			final int min = _spinnerMin_Gradient.getSelection();
			final int max = _spinnerMax_Gradient.getSelection();

			if (min >= max) {

				if (max == GRADIENT_MAX) {
					_spinnerMin_Gradient.setSelection(max - 1);
				} else {
					_spinnerMax_Gradient.setSelection(min + 1);
				}
			}
		}

		if (_chkMin_Pace.getSelection()) {

			final int min = _spinnerMin_Pace.getSelection();
			final int max = _spinnerMax_Pace.getSelection();

			if (min >= max) {

				if (max == PACE_MAX) {
					_spinnerMin_Pace.setSelection(max - 1);
				} else {
					_spinnerMax_Pace.setSelection(min + 1);
				}
			}
		}

		if (_chkMin_Pulse.getSelection()) {

			final int min = _spinnerMin_Pulse.getSelection();
			final int max = _spinnerMax_Pulse.getSelection();

			if (min >= max) {

				if (max == PrefPagePeople.HEART_BEAT_MAX) {
					_spinnerMin_Pulse.setSelection(max - 1);
				} else {
					_spinnerMax_Pulse.setSelection(min + 1);
				}
			}
		}
	}

}
