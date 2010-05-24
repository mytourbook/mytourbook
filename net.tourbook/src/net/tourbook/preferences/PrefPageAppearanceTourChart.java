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

	private HashMap<Integer, Graph>	fGraphMap;
	private ArrayList<Graph>		fGraphList;
	private ArrayList<Graph>		fViewerGraphs;

	/*
	 * UI controls
	 */
	private CheckboxTableViewer		_graphCheckboxList;

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

	private IntegerFieldEditor		_editGridVerticalDistance;
	private IntegerFieldEditor		_editGridHorizontalDistance;

	private Button					_rdoZoomFeatures;
	private Button					_rdoSliderFeatures;

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
				return graph.__graphLabel;
			}
		});

		checkboxList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				// keep the checked status
				final Graph item = (Graph) event.getElement();
				item.__isChecked = event.getChecked();

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
			final int graphId = graph.__graphId;
			for (final String prefId : prefVisibleIds) {
				if (graphId == Integer.parseInt(prefId)) {
					graph.__isChecked = true;
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
			final int graphId = Integer.parseInt(allGraphId);
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
		_graphCheckboxList = createGraphCheckBoxList(graphContainer);
		GridData gd = new GridData();
		gd.verticalSpan = 2;
		_graphCheckboxList.getTable().setLayoutData(gd);

		// button container
		final Composite buttonContainer = new Composite(graphContainer, SWT.NONE);
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		buttonContainer.setLayout(gl);

		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;

		// up button
		_btnUp = new Button(buttonContainer, SWT.NONE);
		_btnUp.setText(Messages.Pref_Graphs_Button_up);
		_btnUp.setLayoutData(gd);
		_btnUp.setEnabled(false);
		setButtonLayoutData(_btnUp);
		_btnUp.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(final SelectionEvent e) {}

			public void widgetSelected(final SelectionEvent e) {
				moveSelectionUp();
				enableUpDownButtons();
			}
		});

		// down button
		_btnDown = new Button(buttonContainer, SWT.NONE);
		_btnDown.setText(Messages.Pref_Graphs_Button_down);
		_btnDown.setLayoutData(gd);
		_btnDown.setEnabled(false);
		setButtonLayoutData(_btnDown);
		_btnDown.addSelectionListener(new SelectionListener() {
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
		_editGridVerticalDistance = new IntegerFieldEditor(
				ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE,
				Messages.Pref_Graphs_grid_vertical_distance,
				groupGrid);
		_editGridVerticalDistance.setPreferenceStore(getPreferenceStore());
		_editGridVerticalDistance.setPage(this);
		_editGridVerticalDistance.setValidRange(10, 100);
		_editGridVerticalDistance.load();
		UI.setFieldWidth(groupGrid, _editGridVerticalDistance, DEFAULT_FIELD_WIDTH);

		/*
		 * editor: grid horizontal distance
		 */
		_editGridHorizontalDistance = new IntegerFieldEditor(
				ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE,
				Messages.Pref_Graphs_grid_horizontal_distance,
				groupGrid);
		_editGridHorizontalDistance.setPreferenceStore(getPreferenceStore());
		_editGridHorizontalDistance.setPage(this);
		_editGridHorizontalDistance.setValidRange(20, 200);
		_editGridHorizontalDistance.load();
		UI.setFieldWidth(groupGrid, _editGridHorizontalDistance, DEFAULT_FIELD_WIDTH);

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
			_editPaceMinMaxCheckbox = new BooleanFieldEditor(
					ITourbookPreferences.GRAPH_PACE_MINMAX_IS_ENABLED,
					Messages.Pref_Graphs_Check_force_minmax_for_pace,
					groupMinValue);
			_editPaceMinMaxCheckbox.setPreferenceStore(prefStore);
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
			_editPaceMin.setPreferenceStore(prefStore);
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
			_editPaceMax.setPreferenceStore(prefStore);
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
			_editAltimeterMinCheckbox.setPreferenceStore(prefStore);
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
			_editAltimeterMinEditor.setPreferenceStore(prefStore);
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
			_editGradientMinCheckbox.setPreferenceStore(prefStore);
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
			_editGradientMinEditor.setPreferenceStore(prefStore);
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

	private void createUIMouseMode(final Composite container) {

		final Group group = new Group(container, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_mouse_mode);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);

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

		// initialize the radio button
		if (getPreferenceStore().getString(ITourbookPreferences.GRAPH_MOUSE_MODE).equals(Chart.MOUSE_MODE_SLIDER)) {
			_rdoSliderFeatures.setSelection(true);
		} else {
			_rdoZoomFeatures.setSelection(true);
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
		_rdoShowDistance = new Button(group, SWT.RADIO);
		_rdoShowDistance.setText(Messages.Pref_Graphs_Radio_show_distance);
		_rdoShowDistance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				enableActions();
			}
		});

		// radio: time
		_rdoShowTime = new Button(group, SWT.RADIO);
		_rdoShowTime.setText(Messages.Pref_Graphs_Radio_show_time);

		_chkShowStartTime = new Button(group, SWT.CHECK);
		_chkShowStartTime.setText(Messages.Pref_Graphs_Check_show_start_time);
		gd = new GridData();
		gd.horizontalIndent = UI.FORM_FIRST_COLUMN_INDENT;
		_chkShowStartTime.setLayoutData(gd);

		// initialize the radio button
		if (getPreferenceStore().getString(ITourbookPreferences.GRAPH_X_AXIS).equals(TourManager.X_AXIS_TIME)) {
			_rdoShowTime.setSelection(true);
		} else {
			_rdoShowDistance.setSelection(true);
		}

		// checkbox: starttime
		_chkShowStartTime.setSelection(getPreferenceStore().getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME));
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
		_chkZoomToSlider = new Button(groupZoomOptions, SWT.CHECK);
		_chkZoomToSlider.setText(Messages.Pref_Graphs_Check_autozoom);
		_chkZoomToSlider.setSelection(prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER));

		// checkbox: move sliders to border when zoomed
		_chkMoveSlidersWhenZoomed = new Button(groupZoomOptions, SWT.CHECK);
		_chkMoveSlidersWhenZoomed.setText(Messages.Pref_Graphs_move_sliders_when_zoomed);
		_chkMoveSlidersWhenZoomed.setSelection(prefStore
				.getBoolean(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED));
	}

	private void enableActions() {
		_chkShowStartTime.setEnabled(_rdoShowTime.getSelection());
	}

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownButtons() {

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
	protected void performDefaults() {

		_editGridHorizontalDistance.loadDefault();
		_editGridVerticalDistance.loadDefault();

		super.performDefaults();
	}

	/*
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {

		saveGraphs();

		final IPreferenceStore prefStore = getPreferenceStore();

		if (_rdoShowTime.getSelection()) {
			prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_TIME);
		} else {
			prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);
		}

		if (_rdoSliderFeatures.getSelection()) {
			prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_SLIDER);
		} else {
			prefStore.setValue(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_ZOOM);
		}

		prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME, //
				_chkShowStartTime.getSelection());

		prefStore.setValue(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER, //
				_chkZoomToSlider.getSelection());

		prefStore.setValue(
				ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED,
				_chkMoveSlidersWhenZoomed.getSelection());

		_editPaceMinMaxCheckbox.store();
		_editPaceMin.store();
		_editPaceMax.store();
		_editAltimeterMinCheckbox.store();
		_editAltimeterMinEditor.store();
		_editGradientMinCheckbox.store();
		_editGradientMinEditor.store();

		_editGridHorizontalDistance.store();
		_editGridVerticalDistance.store();

		return super.performOk();
	}

	/**
	 * get the graph id's from the preferences and check the graphs in the list
	 */
	private void saveGraphs() {

		final IPreferenceStore prefstore = getPreferenceStore();

		// convert the array with the graph objects into a string which is store
		// in the prefs
		final Object[] graphs = _graphCheckboxList.getCheckedElements();
		final String[] prefGraphsChecked = new String[graphs.length];
		for (int graphIndex = 0; graphIndex < graphs.length; graphIndex++) {
			final Graph graph = (Graph) graphs[graphIndex];
			prefGraphsChecked[graphIndex] = Integer.toString(graph.__graphId);
		}
		prefstore.setValue(
				ITourbookPreferences.GRAPH_VISIBLE,
				StringToArrayConverter.convertArrayToString(prefGraphsChecked));

		// convert the array of all table items into a string which is store in
		// the prefs
		final TableItem[] items = _graphCheckboxList.getTable().getItems();
		final String[] prefGraphs = new String[items.length];
		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			prefGraphs[itemIndex] = Integer.toString(((Graph) items[itemIndex].getData()).__graphId);
		}

		prefstore.setValue(ITourbookPreferences.GRAPH_ALL, StringToArrayConverter.convertArrayToString(prefGraphs));
	}

	/**
	 * check the fields in the tab if they are valid
	 */
	private void validateTab() {

		if (_graphCheckboxList.getCheckedElements().length == 0) {
			setErrorMessage(Messages.Pref_Graphs_Error_one_graph_must_be_selected);
			setValid(false);

		} else {
			setErrorMessage(null);
			setValid(true);
		}
	}

}
