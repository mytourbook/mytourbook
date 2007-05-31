/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.StringToArrayConverter;

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

public class PrefPageChartGraphs extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int		COLUMN_INDENT		= 20;

	private static final int		DEFAULT_FIELD_WIDTH	= 40;

	private CheckboxTableViewer		fGraphCheckboxList;

	private HashMap<Integer, Graph>	fGraphMap;
	private ArrayList<Graph>		fGraphList;
	private ArrayList<Graph>		fViewerGraphs;

	private Button					btnUp;
	private Button					btnDown;

	private Button					radioShowDistance;
	private Button					radioShowTime;
	private Button					checkShowStartTime;

	private Button					checkScrollZoomedChart;
	private Button					checkZoomToSlider;

	private BooleanFieldEditor		altimeterMinCheckbox;
	private IntegerFieldEditor		altimeterMinEditor;
	private BooleanFieldEditor		gradientMinCheckbox;
	private IntegerFieldEditor		gradientMinEditor;

	private class Graph {

		int		graphId;
		String	graphLabel;
		boolean	isChecked	= false;

		public Graph(int graphId, String graphLabel) {
			this.graphId = graphId;
			this.graphLabel = graphLabel;
		}
	};

	/*
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	protected Control createContents(Composite parent) {

		// create a map with all available graphs
		fGraphMap = new HashMap<Integer, Graph>();
		fGraphMap.put(TourManager.GRAPH_ALTITUDE, new Graph(
				TourManager.GRAPH_ALTITUDE,
				Messages.Graph_Label_Altitude));
		fGraphMap.put(TourManager.GRAPH_SPEED, new Graph(
				TourManager.GRAPH_SPEED,
				Messages.Graph_Label_Speed));
		fGraphMap.put(TourManager.GRAPH_PULSE, new Graph(
				TourManager.GRAPH_PULSE,
				Messages.Graph_Label_Heartbeat));
		fGraphMap.put(TourManager.GRAPH_TEMPERATURE, new Graph(
				TourManager.GRAPH_TEMPERATURE,
				Messages.Graph_Label_Temperature));
		fGraphMap.put(TourManager.GRAPH_CADENCE, new Graph(
				TourManager.GRAPH_CADENCE,
				Messages.Graph_Label_Cadence));
		fGraphMap.put(TourManager.GRAPH_ALTIMETER, new Graph(
				TourManager.GRAPH_ALTIMETER,
				Messages.Graph_Label_Altimeter));
		fGraphMap.put(TourManager.GRAPH_GRADIENT, new Graph(
				TourManager.GRAPH_GRADIENT,
				Messages.Graph_Label_Gradiend));

		// create a list with all available graphs
		fGraphList = new ArrayList<Graph>();
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_ALTITUDE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_SPEED));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_PULSE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_TEMPERATURE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_CADENCE));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_ALTIMETER));
		fGraphList.add(fGraphMap.get(TourManager.GRAPH_GRADIENT));

		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TabItem tabVisibleGraphs = new TabItem(tabFolder, SWT.NONE);
		tabVisibleGraphs.setText(Messages.Pref_Graphs_Tab_graph_defaults);
		tabVisibleGraphs.setControl(createTabGraphs(tabFolder));

		TabItem tabAltimeter = new TabItem(tabFolder, SWT.NONE);
		tabAltimeter.setText(Messages.Pref_Graphs_Tab_default_values);
		tabAltimeter.setControl(createTabDefaultsValues(tabFolder));

		return tabFolder;
	}

	private Control createTabGraphs(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		Label label = new Label(container, SWT.WRAP);
		label.setText(Messages.Pref_Graphs_Label_select_graph);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		// graph list
		fGraphCheckboxList = createGraphCheckBoxList(container);
		gd = new GridData();
		gd.verticalSpan = 2;
		fGraphCheckboxList.getTable().setLayoutData(gd);

		// button container
		Composite buttonContainer = new Composite(container, SWT.NONE);
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		buttonContainer.setLayout(gl);

		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;

		// up button
		btnUp = new Button(buttonContainer, SWT.NONE);
		btnUp.setText(Messages.Pref_Graphs_Button_up);
		btnUp.setLayoutData(gd);
		btnUp.setEnabled(false);
		btnUp.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				moveSelectionUp();
				enableUpDownButtons();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		// down button
		btnDown = new Button(buttonContainer, SWT.NONE);
		btnDown.setText(Messages.Pref_Graphs_Button_down);
		btnDown.setLayoutData(gd);
		btnDown.setEnabled(false);
		btnDown.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				moveSelectionDown();
				enableUpDownButtons();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		validateTab();
		enableUpDownButtons();

		// group: units for the x-axis
		Group group = new Group(container, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_units_for_xaxis);
		gd = new GridData();
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);
		group.setLayout(new GridLayout(1, false));

		// radio: distance
		radioShowDistance = new Button(group, SWT.RADIO);
		radioShowDistance.setText(Messages.Pref_Graphs_Radio_show_distance);
		radioShowDistance.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				enableTabGraphControls();
			}
		});

		// radio: time
		radioShowTime = new Button(group, SWT.RADIO);
		radioShowTime.setText(Messages.Pref_Graphs_Radio_show_time);

		checkShowStartTime = new Button(group, SWT.CHECK);
		checkShowStartTime.setText(Messages.Pref_Graphs_Check_show_start_time);
		gd = new GridData();
		gd.horizontalIndent = COLUMN_INDENT;
		checkShowStartTime.setLayoutData(gd);

		// initialize the radio button
		if (getPreferenceStore().getString(ITourbookPreferences.GRAPH_X_AXIS).equals(
				TourManager.X_AXIS_TIME)) {
			radioShowTime.setSelection(true);
		} else {
			radioShowDistance.setSelection(true);
		}

		// checkbox: starttime
		checkShowStartTime.setSelection(getPreferenceStore().getBoolean(
				ITourbookPreferences.GRAPH_X_AXIS_STARTTIME));

		// group: zoom options
		Group groupZoomOptions = new Group(container, SWT.NONE);
		groupZoomOptions.setText(Messages.Pref_Graphs_Group_zoom_options);
		gd = new GridData();
		gd.horizontalSpan = 2;
		groupZoomOptions.setLayoutData(gd);
		groupZoomOptions.setLayout(new GridLayout(1, false));

		// checkbox: scroll zoomed chart
		checkScrollZoomedChart = new Button(groupZoomOptions, SWT.CHECK);
		checkScrollZoomedChart.setText(Messages.Pref_Graphs_Check_scroll_zoomed_chart);
		checkScrollZoomedChart.setSelection(getPreferenceStore().getBoolean(
				ITourbookPreferences.GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH));
		checkScrollZoomedChart.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (checkScrollZoomedChart.getSelection()) {
					checkZoomToSlider.setSelection(false);
				}
			}
		});

		// checkbox: auto zoom to moved slider
		checkZoomToSlider = new Button(groupZoomOptions, SWT.CHECK);
		checkZoomToSlider.setText(Messages.Pref_Graphs_Check_autozoom);
		checkZoomToSlider.setSelection(getPreferenceStore().getBoolean(
				ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER));
		checkZoomToSlider.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (checkZoomToSlider.getSelection()) {
					checkScrollZoomedChart.setSelection(false);
				}
			}
		});

		// enable/disable the controls
		enableTabGraphControls();

		return container;
	}

	private Control createTabDefaultsValues(Composite parent) {

		GridData gd;

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		// the editor container removes all margins
		final Composite editorContainer = new Composite(container, SWT.NONE);
		editorContainer.setLayout(new GridLayout());
		editorContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/*
		 * checkbox: altimeter min value
		 */
		altimeterMinCheckbox = new BooleanFieldEditor(
				ITourbookPreferences.GRAPH_ALTIMETER_MIN_ENABLED,
				Messages.Pref_Graphs_Check_force_minimum_for_altimeter,
				editorContainer);
		altimeterMinCheckbox.setPreferenceStore(getPreferenceStore());
		altimeterMinCheckbox.setPage(this);
		altimeterMinCheckbox.load();
		altimeterMinCheckbox.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				boolean isChecked = (Boolean) event.getNewValue();
				altimeterMinEditor.setEnabled(isChecked, editorContainer);
			}
		});

		// add placeholder
		Label label = new Label(editorContainer, SWT.NONE);
		label.setText(""); //$NON-NLS-1$

		/*
		 * editor: altimeter min value
		 */
		altimeterMinEditor = new IntegerFieldEditor(
				ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE,
				Messages.Pref_Graphs_Text_min_value,
				editorContainer);
		altimeterMinEditor.setPreferenceStore(getPreferenceStore());
		altimeterMinEditor.setPage(this);
		altimeterMinEditor.setTextLimit(4);
		altimeterMinEditor.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
		altimeterMinEditor.load();
		UI.setFieldWidth(editorContainer, altimeterMinEditor, DEFAULT_FIELD_WIDTH);
		gd = new GridData();
		gd.horizontalIndent = COLUMN_INDENT;
		altimeterMinEditor.getLabelControl(editorContainer).setLayoutData(gd);

		altimeterMinEditor.setEnabled(altimeterMinCheckbox.getBooleanValue(), editorContainer);

		/*
		 * checkbox: gradient min value
		 */
		gradientMinCheckbox = new BooleanFieldEditor(
				ITourbookPreferences.GRAPH_GRADIENT_MIN_ENABLED,
				Messages.Pref_Graphs_Check_force_minimum_for_gradient,
				editorContainer);
		gradientMinCheckbox.setPreferenceStore(getPreferenceStore());
		gradientMinCheckbox.setPage(this);
		gradientMinCheckbox.load();
		gradientMinCheckbox.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				boolean isChecked = (Boolean) event.getNewValue();
				gradientMinEditor.setEnabled(isChecked, editorContainer);
			}
		});

		// add placeholder
		label = new Label(editorContainer, SWT.NONE);
		label.setText(""); //$NON-NLS-1$

		/*
		 * editor: gradient min value
		 */
		gradientMinEditor = new IntegerFieldEditor(
				ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
				Messages.Pref_Graphs_Text_min_value,
				editorContainer);
		gradientMinEditor.setPreferenceStore(getPreferenceStore());
		gradientMinEditor.setPage(this);
		gradientMinEditor.setTextLimit(4);
		gradientMinEditor.setErrorMessage(Messages.Pref_Graphs_Error_value_must_be_integer);
		gradientMinEditor.load();
		UI.setFieldWidth(editorContainer, gradientMinEditor, DEFAULT_FIELD_WIDTH);
		gd = new GridData();
		gd.horizontalIndent = COLUMN_INDENT;
		gradientMinEditor.getLabelControl(editorContainer).setLayoutData(gd);

		gradientMinEditor.setEnabled(gradientMinCheckbox.getBooleanValue(), editorContainer);

		return container;
	}

	private void enableTabGraphControls() {
		checkShowStartTime.setEnabled(radioShowTime.getSelection());
	}

	private CheckboxTableViewer createGraphCheckBoxList(Composite parent) {

		final CheckboxTableViewer checkboxList = CheckboxTableViewer.newCheckList(
				parent,
				SWT.SINGLE | SWT.TOP | SWT.BORDER);

		checkboxList.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return fViewerGraphs.toArray();
			}

			public void dispose() {}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		});

		checkboxList.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				Graph graph = (Graph) element;
				return graph.graphLabel;
			}
		});

		checkboxList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {

				// keep the checked status
				final Graph item = (Graph) event.getElement();
				item.isChecked = event.getChecked();

				// select the checked item
				checkboxList.setSelection(new StructuredSelection(item));

				validateTab();
			}
		});

		checkboxList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				enableUpDownButtons();
			}
		});

		// first create the input, then set it
		createGraphList();
		checkboxList.setInput(this);

		String[] prefVisibleIds = StringToArrayConverter.convertStringToArray(getPreferenceStore()
				.getString(ITourbookPreferences.GRAPH_VISIBLE));

		// check all graphs which are defined in the prefs
		ArrayList<Graph> checkedGraphs = new ArrayList<Graph>();
		for (Graph graph : fViewerGraphs) {
			final int graphId = graph.graphId;
			for (String prefId : prefVisibleIds) {
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
	 * create a list with the available graphs
	 */
	private void createGraphList() {

		String[] prefAllIds = StringToArrayConverter.convertStringToArray(getPreferenceStore()
				.getString(ITourbookPreferences.GRAPH_ALL));

		fViewerGraphs = new ArrayList<Graph>();

		// put all graphs in the viewer which are defined in the prefs
		for (int graphIndex = 0; graphIndex < prefAllIds.length; graphIndex++) {
			final int graphId = Integer.valueOf(prefAllIds[graphIndex]);
			if (fGraphMap.containsKey(graphId)) {
				fViewerGraphs.add(fGraphMap.get(graphId));
			}
		}

		// make sure that all available graphs are in the viewer
		for (Graph graph : fGraphList) {
			if (!fViewerGraphs.contains(graph)) {
				fViewerGraphs.add(graph);
			}
		}

	}

	/*
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	public boolean performOk() {

		saveGraphs();

		IPreferenceStore prefStore = getPreferenceStore();

		if (radioShowTime.getSelection()) {
			prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_TIME);
		} else {
			prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);
		}

		prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME, checkShowStartTime
				.getSelection());

		prefStore.setValue(
				ITourbookPreferences.GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH,
				checkScrollZoomedChart.getSelection());

		prefStore.setValue(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER, checkZoomToSlider
				.getSelection());

		altimeterMinCheckbox.store();
		altimeterMinEditor.store();
		gradientMinCheckbox.store();
		gradientMinEditor.store();

		return super.performOk();
	}

	/**
	 * get the graph id's from the preferences and check the graphs in the list
	 */
	private void saveGraphs() {

		// convert the array with the graph objects into a string which is store
		// in the prefs
		Object[] graphs = fGraphCheckboxList.getCheckedElements();
		String[] prefGraphsChecked = new String[graphs.length];
		for (int graphIndex = 0; graphIndex < graphs.length; graphIndex++) {
			Graph graph = (Graph) graphs[graphIndex];
			prefGraphsChecked[graphIndex] = Integer.toString(graph.graphId);
		}
		getPreferenceStore().setValue(
				ITourbookPreferences.GRAPH_VISIBLE,
				StringToArrayConverter.convertArrayToString(prefGraphsChecked));

		// convert the array of all table items into a string which is store in
		// the prefs
		TableItem[] items = fGraphCheckboxList.getTable().getItems();
		String[] prefGraphs = new String[items.length];
		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			prefGraphs[itemIndex] = Integer.toString(((Graph) items[itemIndex].getData()).graphId);
		}

		getPreferenceStore().setValue(
				ITourbookPreferences.GRAPH_ALL,
				StringToArrayConverter.convertArrayToString(prefGraphs));
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

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownButtons() {

		Table table = fGraphCheckboxList.getTable();
		TableItem[] items = table.getSelection();
		boolean validSelection = items != null && items.length > 0;
		boolean enableUp = validSelection;
		boolean enableDown = validSelection;

		if (validSelection) {
			int indices[] = table.getSelectionIndices();
			int max = table.getItemCount();
			enableUp = indices[0] != 0;
			enableDown = indices[indices.length - 1] < max - 1;
		}
		btnUp.setEnabled(enableUp);
		btnDown.setEnabled(enableDown);
	}

	/**
	 * Move the current selection in the build list down.
	 */
	private void moveSelectionDown() {
		Table table = fGraphCheckboxList.getTable();
		int indices[] = table.getSelectionIndices();
		if (indices.length < 1) {
			return;
		}
		int newSelection[] = new int[indices.length];
		int max = table.getItemCount() - 1;
		for (int i = indices.length - 1; i >= 0; i--) {
			int index = indices[i];
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
		Table table = fGraphCheckboxList.getTable();
		int indices[] = table.getSelectionIndices();
		int newSelection[] = new int[indices.length];
		for (int i = 0; i < indices.length; i++) {
			int index = indices[i];
			if (index > 0) {
				move(table.getItem(index), index - 1);
				newSelection[i] = index - 1;
			}
		}
		table.setSelection(newSelection);
	}

	/**
	 * Moves an entry in the table to the given index.
	 */
	private void move(TableItem item, int index) {
		this.setValid(true);
		final Graph graph = (Graph) item.getData();
		item.dispose();
		fGraphCheckboxList.insert(graph, index);
		fGraphCheckboxList.setChecked(graph, graph.isChecked);
	}

}
