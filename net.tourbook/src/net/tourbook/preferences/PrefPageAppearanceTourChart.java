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
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.StringToArrayConverter;
 
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceTourChart extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int		DEFAULT_FIELD_WIDTH	= 40;

	private CheckboxTableViewer		fGraphCheckboxList;

	private HashMap<Integer, Graph>	fGraphMap;
	private ArrayList<Graph>		fGraphList;
	private ArrayList<Graph>		fViewerGraphs;

	private Button					fBtnUp;
	private Button					fBtnDown;

	private Button					fRdoShowDistance;
	private Button					fRdoShowTime;
	private Button					fChkShowStartTime;

	private Button					fChkZoomToSlider;
	private Button					fChkMoveSlidersWhenZoomed;

	private BooleanFieldEditor		fEditPaceMinMaxCheckbox;
	private IntegerFieldEditor		fEditPaceMin;
	private IntegerFieldEditor		fEditPaceMax;
	private BooleanFieldEditor		fEditAltimeterMinCheckbox;
	private IntegerFieldEditor		fEditAltimeterMinEditor;
	private BooleanFieldEditor		fEditGradientMinCheckbox;
	private IntegerFieldEditor		fEditGradientMinEditor;

	private IntegerFieldEditor		fEditGridVerticalDistance;
	private IntegerFieldEditor		fEditGridHorizontalDistance;

	private Button					fRdoZoomFeatures;
	private Button					fRdoSliderFeatures;

	private class Graph {

		int		graphId;
		String	graphLabel;
		boolean	isChecked	= false;

		public Graph(final int graphId, final String graphLabel) {
			this.graphId = graphId;
			this.graphLabel = graphLabel;
		}
	};

	@Override
	protected Control createContents(final Composite parent) {

		initializeGraphs();

		final TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final TabItem tabVisibleGraphs = new TabItem(tabFolder, SWT.NONE);
		tabVisibleGraphs.setText(Messages.Pref_Graphs_Tab_graph_defaults);
		tabVisibleGraphs.setControl(createUITabGraphs(tabFolder));

		final TabItem tabMinimumValues = new TabItem(tabFolder, SWT.NONE);
		tabMinimumValues.setText(Messages.Pref_Graphs_Tab_default_values);
		tabMinimumValues.setControl(createUITabMinMaxValues(tabFolder));

		final TabItem tabZoom = new TabItem(tabFolder, SWT.NONE);
		tabZoom.setText(Messages.Pref_Graphs_Tab_zoom_options);
		tabZoom.setControl(createUITabOptions(tabFolder));

		enableActions();

		return tabFolder;
	}

	private CheckboxTableViewer createGraphCheckBoxList(final Composite parent) {

		final CheckboxTableViewer checkboxList = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE
				| SWT.TOP
				| SWT.BORDER);

		checkboxList.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return fViewerGraphs.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		checkboxList.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(final Object element) {
				final Graph graph = (Graph) element;
				return graph.graphLabel;
			}
		});

		checkboxList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				// keep the checked status
				final Graph item = (Graph) event.getElement();
				item.isChecked = event.getChecked();

				// select the checked item
				checkboxList.setSelection(new StructuredSelection(item));

				validateTab();
			}
		});

		checkboxList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownButtons();
			}
		});

		// first create the input, then set it
		createGraphList();
		checkboxList.setInput(this);

		final String[] prefVisibleIds = StringToArrayConverter.convertStringToArray(getPreferenceStore().getString(
				ITourbookPreferences.GRAPH_VISIBLE));

		// check all graphs which are defined in the prefs
		final ArrayList<Graph> checkedGraphs = new ArrayList<Graph>();
		for (final Graph graph : fViewerGraphs) {
			final int graphId = graph.graphId;
			for (final String prefId : prefVisibleIds) {
				if (graphId == Integer.valueOf(prefId)) {
					graph.isChecked = true;
					checkedGraphs.add(graph);
				}
			}
		}

		checkboxList.setCheckedElements(checkedGraphs.toArray());

		return checkboxList;
	}

	/**
	 * create a list with all available graphs
	 */
	private void createGraphList() {

		final String[] allGraphIds = StringToArrayConverter.convertStringToArray(//
				getPreferenceStore().getString(ITourbookPreferences.GRAPH_ALL));

		fViewerGraphs = new ArrayList<Graph>();

		// put all graphs in the viewer which are defined in the prefs
		for (final String allGraphId : allGraphIds) {
			final int graphId = Integer.valueOf(allGraphId);
			if (fGraphMap.containsKey(graphId)) {
				fViewerGraphs.add(fGraphMap.get(graphId));
			}
		}

		// make sure that all available graphs are in the viewer
		for (final Graph graph : fGraphList) {
			if (!fViewerGraphs.contains(graph)) {
				fViewerGraphs.add(graph);
			}
		}

	}

	private void createUIGraphs(final Composite parent) {

		// group: units for the x-axis
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Label_select_graph);
		group.setToolTipText(Messages.Pref_Graphs_Label_select_graph_tooltip);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);

		/*
		 * graph container
		 */
		final Composite graphContainer = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(graphContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(graphContainer);

		// graph list
		fGraphCheckboxList = createGraphCheckBoxList(graphContainer);
		GridData gd = new GridData();
		gd.verticalSpan = 2;
		fGraphCheckboxList.getTable().setLayoutData(gd);

		// button container
		final Composite buttonContainer = new Composite(graphContainer, SWT.NONE);
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		buttonContainer.setLayout(gl);

		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;

		// up button
		fBtnUp = new Button(buttonContainer, SWT.NONE);
		fBtnUp.setText(Messages.Pref_Graphs_Button_up);
		fBtnUp.setLayoutData(gd);
		fBtnUp.setEnabled(false);
		setButtonLayoutData(fBtnUp);
		fBtnUp.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(final SelectionEvent e) {}

			public void widgetSelected(final SelectionEvent e) {
				moveSelectionUp();
				enableUpDownButtons();
			}
		});

		// down button
		fBtnDown = new Button(buttonContainer, SWT.NONE);
		fBtnDown.setText(Messages.Pref_Graphs_Button_down);
		fBtnDown.setLayoutData(gd);
		fBtnDown.setEnabled(false);
		setButtonLayoutData(fBtnDown);
		fBtnDown.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(final SelectionEvent e) {}

			public void widgetSelected(final SelectionEvent e) {
				moveSelectionDown();
				enableUpDownButtons();
			}
		});

		validateTab();
		enableUpDownButtons();
	}

	/**
	 * group: grid
	 */
	private void createUIGrid(final Composite container) {

		final Group groupGrid = new Group(container, SWT.NONE);
		groupGrid.setText(Messages.Pref_Graphs_grid_distance);
		GridDataFactory.fillDefaults().indent(0, 0).applyTo(groupGrid);

		/*
		 * editor: grid vertical distance
		 */
		fEditGridVerticalDistance = new IntegerFieldEditor(
				ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE,
				Messages.Pref_Graphs_grid_vertical_distance,
				groupGrid);
		fEditGridVerticalDistance.setPreferenceStore(getPreferenceStore());
		fEditGridVerticalDistance.setPage(this);
		fEditGridVerticalDistance.setValidRange(10, 100);
		fEditGridVerticalDistance.load();
		UI.setFieldWidth(groupGrid, fEditGridVerticalDistance, DEFAULT_FIELD_WIDTH);

		/*
		 * editor: grid horizontal distance
		 */
		fEditGridHorizontalDistance = new IntegerFieldEditor(
				ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE,
				Messages.Pref_Graphs_grid_horizontal_distance,
				groupGrid);
		fEditGridHorizontalDistance.setPreferenceStore(getPreferenceStore());
		fEditGridHorizontalDistance.setPage(this);
		fEditGridHorizontalDistance.setValidRange(20, 200);
		fEditGridHorizontalDistance.load();
		UI.setFieldWidth(groupGrid, fEditGridHorizontalDistance, DEFAULT_FIELD_WIDTH);

		GridLayoutFactory.swtDefaults()//
				.margins(5, 5)
				.numColumns(2)
				.applyTo(groupGrid);
	}

	private void createUIMinMaxValue(final Composite container) {

		final IPreferenceStore prefStore = getPreferenceStore();
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
			fEditPaceMinMaxCheckbox = new BooleanFieldEditor(
					ITourbookPreferences.GRAPH_PACE_MINMAX_IS_ENABLED,
					Messages.Pref_Graphs_Check_force_minmax_for_pace,
					groupMinValue);
			fEditPaceMinMaxCheckbox.setPreferenceStore(prefStore);
			fEditPaceMinMaxCheckbox.setPage(this);
			fEditPaceMinMaxCheckbox.load();
			fEditPaceMinMaxCheckbox.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					final boolean isChecked = (Boolean) event.getNewValue();
					fEditPaceMin.setEnabled(isChecked, groupMinValue);
					fEditPaceMax.setEnabled(isChecked, groupMinValue);
				}
			});

			// paceholder
			new Label(groupMinValue, SWT.NONE);
			new Label(groupMinValue, SWT.NONE);

			/*
			 * editor: pace min value
			 */
			fEditPaceMin = new IntegerFieldEditor(
					ITourbookPreferences.GRAPH_PACE_MIN_VALUE,
					Messages.Pref_Graphs_Text_min_value,
					groupMinValue);
			fEditPaceMin.setPreferenceStore(prefStore);
			fEditPaceMin.setPage(this);
			fEditPaceMin.setTextLimit(4);
			fEditPaceMin.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
			fEditPaceMin.load();
			UI.setFieldWidth(groupMinValue, fEditPaceMin, DEFAULT_FIELD_WIDTH);
			gd = new GridData();
			gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
			fEditPaceMin.getLabelControl(groupMinValue).setLayoutData(gd);

			fEditPaceMin.setEnabled(fEditPaceMinMaxCheckbox.getBooleanValue(), groupMinValue);

			// label: minutes
			label = new Label(groupMinValue, SWT.NONE);
			label.setText(Messages.app_unit_minutes);

			/*
			 * editor: pace max value
			 */
			fEditPaceMax = new IntegerFieldEditor(
					ITourbookPreferences.GRAPH_PACE_MAX_VALUE,
					Messages.Pref_Graphs_Text_max_value,
					groupMinValue);
			fEditPaceMax.setPreferenceStore(prefStore);
			fEditPaceMax.setPage(this);
			fEditPaceMax.setTextLimit(4);
			fEditPaceMax.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
			fEditPaceMax.load();
			UI.setFieldWidth(groupMinValue, fEditPaceMax, DEFAULT_FIELD_WIDTH);
			gd = new GridData();
			gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
			fEditPaceMax.getLabelControl(groupMinValue).setLayoutData(gd);

			fEditPaceMax.setEnabled(fEditPaceMinMaxCheckbox.getBooleanValue(), groupMinValue);

			// label: minutes
			label = new Label(groupMinValue, SWT.NONE);
			label.setText(Messages.app_unit_minutes);

			/*
			 * checkbox: altimeter min value
			 */
			fEditAltimeterMinCheckbox = new BooleanFieldEditor(
					ITourbookPreferences.GRAPH_ALTIMETER_MIN_IS_ENABLED,
					Messages.Pref_Graphs_Check_force_minimum_for_altimeter,
					groupMinValue);
			fEditAltimeterMinCheckbox.setPreferenceStore(prefStore);
			fEditAltimeterMinCheckbox.setPage(this);
			fEditAltimeterMinCheckbox.load();
			fEditAltimeterMinCheckbox.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					final boolean isChecked = (Boolean) event.getNewValue();
					fEditAltimeterMinEditor.setEnabled(isChecked, groupMinValue);
				}
			});

			// paceholder
			new Label(groupMinValue, SWT.NONE);
			new Label(groupMinValue, SWT.NONE);

			/*
			 * editor: altimeter min value
			 */
			fEditAltimeterMinEditor = new IntegerFieldEditor(
					ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE,
					Messages.Pref_Graphs_Text_min_value,
					groupMinValue);
			fEditAltimeterMinEditor.setPreferenceStore(prefStore);
			fEditAltimeterMinEditor.setPage(this);
			fEditAltimeterMinEditor.setTextLimit(4);
			fEditAltimeterMinEditor.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
			fEditAltimeterMinEditor.load();
			UI.setFieldWidth(groupMinValue, fEditAltimeterMinEditor, DEFAULT_FIELD_WIDTH);
			gd = new GridData();
			gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
			fEditAltimeterMinEditor.getLabelControl(groupMinValue).setLayoutData(gd);

			fEditAltimeterMinEditor.setEnabled(fEditAltimeterMinCheckbox.getBooleanValue(), groupMinValue);

			// paceholder
			new Label(groupMinValue, SWT.NONE);

			/*
			 * checkbox: gradient min value
			 */
			fEditGradientMinCheckbox = new BooleanFieldEditor(
					ITourbookPreferences.GRAPH_GRADIENT_MIN_IS_ENABLED,
					Messages.Pref_Graphs_Check_force_minimum_for_gradient,
					groupMinValue);
			fEditGradientMinCheckbox.setPreferenceStore(prefStore);
			fEditGradientMinCheckbox.setPage(this);
			fEditGradientMinCheckbox.load();
			fEditGradientMinCheckbox.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					final boolean isChecked = (Boolean) event.getNewValue();
					fEditGradientMinEditor.setEnabled(isChecked, groupMinValue);
				}
			});

			// add placeholder
			new Label(groupMinValue, SWT.NONE);
			new Label(groupMinValue, SWT.NONE);

			/*
			 * editor: gradient min value
			 */
			fEditGradientMinEditor = new IntegerFieldEditor(
					ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
					Messages.Pref_Graphs_Text_min_value,
					groupMinValue);
			fEditGradientMinEditor.setPreferenceStore(prefStore);
			fEditGradientMinEditor.setPage(this);
			fEditGradientMinEditor.setTextLimit(4);
			fEditGradientMinEditor.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
			fEditGradientMinEditor.load();
			UI.setFieldWidth(groupMinValue, fEditGradientMinEditor, DEFAULT_FIELD_WIDTH);
			gd = new GridData();
			gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
			fEditGradientMinEditor.getLabelControl(groupMinValue).setLayoutData(gd);
			fEditGradientMinEditor.setEnabled(fEditGradientMinCheckbox.getBooleanValue(), groupMinValue);

			// add placeholder
			new Label(groupMinValue, SWT.NONE);
		}

		GridLayoutFactory.swtDefaults().margins(5, 5).numColumns(3).applyTo(groupMinValue);
	}

	private void createUIMouseMode(final Composite container) {

		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_mouse_mode);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);

		// radio: zoom features
		fRdoZoomFeatures = new Button(group, SWT.RADIO);
		fRdoZoomFeatures.setText(Messages.Pref_Graphs_Radio_mouse_mode_zoom);
		fRdoZoomFeatures.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				enableActions();
			}
		});

		// radio: slider features
		fRdoSliderFeatures = new Button(group, SWT.RADIO);
		fRdoSliderFeatures.setText(Messages.Pref_Graphs_Radio_mouse_mode_slider);

		// initialize the radio button
		if (getPreferenceStore().getString(ITourbookPreferences.GRAPH_MOUSE_MODE).equals(Chart.MOUSE_MODE_SLIDER)) {
			fRdoSliderFeatures.setSelection(true);
		} else {
			fRdoZoomFeatures.setSelection(true);
		}
	}

	private Control createUITabGraphs(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);

		createUIGraphs(container);
		createUIXAxisUnits(container);
		createUIGrid(container);

		return container;
	}

	private Control createUITabMinMaxValues(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);

		createUIMinMaxValue(container);

		return container;
	}

	private Control createUITabOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(container);

		createUIMouseMode(container);
		createUIZoomOptions(container);

		return container;
	}

	private void createUIXAxisUnits(final Composite container) {

		GridData gd;

		// group: units for the x-axis
		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_units_for_xaxis);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);

		// radio: distance
		fRdoShowDistance = new Button(group, SWT.RADIO);
		fRdoShowDistance.setText(Messages.Pref_Graphs_Radio_show_distance);
		fRdoShowDistance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				enableActions();
			}
		});

		// radio: time
		fRdoShowTime = new Button(group, SWT.RADIO);
		fRdoShowTime.setText(Messages.Pref_Graphs_Radio_show_time);

		fChkShowStartTime = new Button(group, SWT.CHECK);
		fChkShowStartTime.setText(Messages.Pref_Graphs_Check_show_start_time);
		gd = new GridData();
		gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
		fChkShowStartTime.setLayoutData(gd);

		// initialize the radio button
		if (getPreferenceStore().getString(ITourbookPreferences.GRAPH_X_AXIS).equals(TourManager.X_AXIS_TIME)) {
			fRdoShowTime.setSelection(true);
		} else {
			fRdoShowDistance.setSelection(true);
		}

		// checkbox: starttime
		fChkShowStartTime.setSelection(getPreferenceStore().getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME));
	}

	/**
	 * group: zoom options
	 */
	private void createUIZoomOptions(final Composite container) {

		final IPreferenceStore prefStore = getPreferenceStore();

		final Group groupZoomOptions = new Group(container, SWT.NONE);
		groupZoomOptions.setText(Messages.Pref_Graphs_Group_zoom_options);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupZoomOptions);
		GridLayoutFactory.swtDefaults().applyTo(groupZoomOptions);

		// checkbox: auto zoom to moved slider
		fChkZoomToSlider = new Button(groupZoomOptions, SWT.CHECK);
		fChkZoomToSlider.setText(Messages.Pref_Graphs_Check_autozoom);
		fChkZoomToSlider.setSelection(prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER));

		// checkbox: move sliders to border when zoomed
		fChkMoveSlidersWhenZoomed = new Button(groupZoomOptions, SWT.CHECK);
		fChkMoveSlidersWhenZoomed.setText(Messages.Pref_Graphs_move_sliders_when_zoomed);
		fChkMoveSlidersWhenZoomed.setSelection(prefStore
				.getBoolean(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED));
	}

	private void enableActions() {
		fChkShowStartTime.setEnabled(fRdoShowTime.getSelection());
	}

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownButtons() {

		final Table table = fGraphCheckboxList.getTable();
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
		fBtnUp.setEnabled(enableUp);
		fBtnDown.setEnabled(enableDown);
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(final IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	private void initializeGraphs() {
		// create a map with all available graphs
		fGraphMap = new HashMap<Integer, Graph>();
		fGraphMap.put(TourManager.GRAPH_ALTITUDE, new Graph(TourManager.GRAPH_ALTITUDE, Messages.Graph_Label_Altitude));
		fGraphMap.put(TourManager.GRAPH_SPEED, new Graph(TourManager.GRAPH_SPEED, Messages.Graph_Label_Speed));
		fGraphMap.put(TourManager.GRAPH_PACE, new Graph(TourManager.GRAPH_PACE, Messages.Graph_Label_Pace));
		fGraphMap.put(TourManager.GRAPH_POWER, new Graph(TourManager.GRAPH_POWER, Messages.Graph_Label_Power));
		fGraphMap.put(TourManager.GRAPH_PULSE, new Graph(TourManager.GRAPH_PULSE, Messages.Graph_Label_Heartbeat));

		fGraphMap.put(TourManager.GRAPH_TEMPERATURE, //
				new Graph(TourManager.GRAPH_TEMPERATURE, Messages.Graph_Label_Temperature));

		fGraphMap.put(TourManager.GRAPH_CADENCE, //
				new Graph(TourManager.GRAPH_CADENCE, Messages.Graph_Label_Cadence));

		fGraphMap.put(TourManager.GRAPH_ALTIMETER, //
				new Graph(TourManager.GRAPH_ALTIMETER, Messages.Graph_Label_Altimeter));

		fGraphMap.put(TourManager.GRAPH_GRADIENT, //
				new Graph(TourManager.GRAPH_GRADIENT, Messages.Graph_Label_Gradient));

		// create a list with all available graphs
		fGraphList = new ArrayList<Graph>();
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_ALTITUDE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_SPEED));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_PACE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_POWER));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_PULSE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_TEMPERATURE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_CADENCE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_ALTIMETER));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_GRADIENT));
	}

	/**
	 * Moves an entry in the table to the given index.
	 */
	private void move(final TableItem item, final int index) {
		this.setValid(true);
		final Graph graph = (Graph) item.getData();
		item.dispose();
		fGraphCheckboxList.insert(graph, index);
		fGraphCheckboxList.setChecked(graph, graph.isChecked);
	}

	/**
	 * Move the current selection in the build list down.
	 */
	private void moveSelectionDown() {
		final Table table = fGraphCheckboxList.getTable();
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
		final Table table = fGraphCheckboxList.getTable();
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
	protected void performDefaults() {

		fEditGridHorizontalDistance.loadDefault();
		fEditGridVerticalDistance.loadDefault();

		super.performDefaults();
	}

	/*
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {

		saveGraphs();

		final IPreferenceStore prefStore = getPreferenceStore();

		if (fRdoShowTime.getSelection()) {
			prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_TIME);
		} else {
			prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);
		}

		if (fRdoSliderFeatures.getSelection()) {
			prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_SLIDER);
		} else {
			prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_ZOOM);
		}

		prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME, //
				fChkShowStartTime.getSelection());

		prefStore.setValue(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER, //
				fChkZoomToSlider.getSelection());

		prefStore.setValue(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED, fChkMoveSlidersWhenZoomed
				.getSelection());

		fEditPaceMinMaxCheckbox.store();
		fEditPaceMin.store();
		fEditPaceMax.store();
		fEditAltimeterMinCheckbox.store();
		fEditAltimeterMinEditor.store();
		fEditGradientMinCheckbox.store();
		fEditGradientMinEditor.store();

		fEditGridHorizontalDistance.store();
		fEditGridVerticalDistance.store();

		return super.performOk();
	}

	/**
	 * get the graph id's from the preferences and check the graphs in the list
	 */
	private void saveGraphs() {

		final IPreferenceStore prefstore = getPreferenceStore();

		// convert the array with the graph objects into a string which is store
		// in the prefs
		final Object[] graphs = fGraphCheckboxList.getCheckedElements();
		final String[] prefGraphsChecked = new String[graphs.length];
		for (int graphIndex = 0; graphIndex < graphs.length; graphIndex++) {
			final Graph graph = (Graph) graphs[graphIndex];
			prefGraphsChecked[graphIndex] = Integer.toString(graph.graphId);
		}
		prefstore.setValue(ITourbookPreferences.GRAPH_VISIBLE, StringToArrayConverter
				.convertArrayToString(prefGraphsChecked));

		// convert the array of all table items into a string which is store in
		// the prefs
		final TableItem[] items = fGraphCheckboxList.getTable().getItems();
		final String[] prefGraphs = new String[items.length];
		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			prefGraphs[itemIndex] = Integer.toString(((Graph) items[itemIndex].getData()).graphId);
		}

		prefstore.setValue(ITourbookPreferences.GRAPH_ALL, StringToArrayConverter.convertArrayToString(prefGraphs));
	}

	/**
	 * check the fields in the tab if they are valid
	 */
	private void validateTab() {

		if (fGraphCheckboxList.getCheckedElements().length == 0) {
			setErrorMessage(Messages.Pref_Graphs_Error_one_graph_must_be_selected);
			setValid(false);

		} else {
			setErrorMessage(null);
			setValid(true);
		}
	}

}
