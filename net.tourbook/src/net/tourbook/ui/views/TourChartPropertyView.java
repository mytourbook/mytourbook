package net.tourbook.ui.views;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.part.ViewPart;

public class TourChartPropertyView extends ViewPart {

	public static final String	ID	= "net.tourbook.views.TourChartPropertyView";	//$NON-NLS-1$

	private Spinner				fSpinnerComputeValues;
	private Spinner				fSpinnerClipValues;

	private Button				fRadioLineChartType;
	private Button				fRadioBarChartType;

	private Button				fChkUseCustomComputeSettings;
	private Button				fChkUseCustomClipSettings;

	private void adjustSpinnerValueOnMouseScroll(MouseEvent event) {

		// accelerate with Ctrl + Shift key
		int accelerator = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
		accelerator *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

		Spinner spinner = (Spinner) event.widget;
		final int newValue = ((event.count > 0 ? 1 : -1) * accelerator);

		spinner.setSelection(spinner.getSelection() + newValue);
	}

	private void createLayout(Composite parent) {

		GridData gd;
		Label label;

		Composite container = new Composite(parent, SWT.NONE);

		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 0;
		container.setLayout(gl);

		/*
		 * chart type
		 */
		label = new Label(container, SWT.NONE);
		label.setText(Messages.TourChartProperty_label_chart_type);

		// group: units for the x-axis
		Composite groupChartType = new Composite(container, SWT.NONE);
		gl = new GridLayout(2, false);
		gl.marginTop = 0;
		gl.marginWidth = 0;
		groupChartType.setLayout(gl);

		{
			// radio: line chart
			fRadioLineChartType = new Button(groupChartType, SWT.RADIO);
			fRadioLineChartType.setText(Messages.TourChartProperty_chart_type_line);
			fRadioLineChartType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					onChangeProperty();
				}
			});

			// radio: bar chart
			fRadioBarChartType = new Button(groupChartType, SWT.RADIO);
			fRadioBarChartType.setText(Messages.TourChartProperty_chart_type_bar);
			fRadioBarChartType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					onChangeProperty();
				}
			});
		}

		// check: use custom settings to compute values
		fChkUseCustomComputeSettings = new Button(container, SWT.CHECK);
		fChkUseCustomComputeSettings.setText(Messages.TourChartProperty_check_customize_value_computing);
		fChkUseCustomComputeSettings.setLayoutData(new GridData(SWT.NONE,
				SWT.NONE,
				false,
				false,
				2,
				1));
		fChkUseCustomComputeSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				onChangeProperty();
			}
		});

		/*
		 * computed value time slice
		 */
		label = new Label(container, SWT.NONE);
		label.setText(Messages.TourChartProperty_label_time_slices);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);

		fSpinnerComputeValues = new Spinner(container, SWT.HORIZONTAL | SWT.BORDER);
		fSpinnerComputeValues.setMaximum(1000);

		fSpinnerComputeValues.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onChangeProperty();
			}
		});

		fSpinnerComputeValues.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(MouseEvent event) {
				adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		});

		/*
		 * value clipping
		 */
		fChkUseCustomClipSettings = new Button(container, SWT.CHECK);
		fChkUseCustomClipSettings.setText(Messages.TourChartProperty_check_customize_value_clipping);
		fChkUseCustomClipSettings.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false, 2, 1));
		fChkUseCustomClipSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				onChangeProperty();
			}
		});

		/*
		 * time slice to clip values
		 */
		label = new Label(container, SWT.NONE);
		label.setText(Messages.TourChartProperty_label_time_slices);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);

		fSpinnerClipValues = new Spinner(container, SWT.HORIZONTAL | SWT.BORDER);
		fSpinnerClipValues.setMaximum(1000);

		fSpinnerClipValues.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onChangeProperty();
			}
		});

		fSpinnerClipValues.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(MouseEvent event) {
				adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		});
	}

	@Override
	public void createPartControl(Composite parent) {

		createLayout(parent);
		restoreSettings();
	}

	private void enableControls() {

		fSpinnerComputeValues.setEnabled(fChkUseCustomComputeSettings.getSelection());
		fSpinnerClipValues.setEnabled(fChkUseCustomClipSettings.getSelection());
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {

		enableControls();

		IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// set new values in the pref store

		// checkbox: use custom settings to compute values
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_IS_COMPUTE_VALUE,
				fChkUseCustomComputeSettings.getSelection());

		// spinner: compute value time slice
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_COMPUTE_VALUE,
				fSpinnerComputeValues.getSelection());

		// checkbox: clip values
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_IS_CLIP_VALUE,
				fChkUseCustomClipSettings.getSelection());

		// spinner: clip value time slice
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_CLIP_VALUE,
				fSpinnerClipValues.getSelection());

		// radioo: chart type
		final int speedChartType = fRadioLineChartType.getSelection()
				? ChartDataModel.CHART_TYPE_LINE
				: ChartDataModel.CHART_TYPE_BAR;
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE, speedChartType);

		// fire modify event
		TourManager.getInstance().firePropertyChange(TourManager.TOUR_PROPERTY_CHART_IS_MODIFIED,
				null);
	}

	private void restoreSettings() {

		IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// get values from pref store

		fChkUseCustomComputeSettings.setSelection(store.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_COMPUTE_VALUE));
		fSpinnerComputeValues.setSelection(store.getInt(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_COMPUTE_VALUE));

		fChkUseCustomClipSettings.setSelection(store.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_CLIP_VALUE));
		fSpinnerClipValues.setSelection(store.getInt(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_CLIP_VALUE));

		// chart type
		int speedChartType = store.getInt(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);
		if (speedChartType == 0 || speedChartType == ChartDataModel.CHART_TYPE_LINE) {
			fRadioLineChartType.setSelection(true);
		} else {
			fRadioBarChartType.setSelection(true);
		}

		enableControls();
	}

	@Override
	public void setFocus() {}

}
