package net.tourbook.ui.views;

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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.part.ViewPart;

public class TourChartPropertyView extends ViewPart {

	public static final String	ID	= "net.tourbook.views.TourChartPropertyView";	//$NON-NLS-1$

	private Spinner				fSpinnerTimeslice;

	private Button				fRadioSpeedLineChart;

	private Button				fRadioSpeedBarChart;

	private Button				fChkUseCustomSettings;

	private void createLayout(Composite parent) {

//		GridData gd;
		Label label;

		Composite container = new Composite(parent, SWT.NONE);

		GridLayout gl = new GridLayout(2, false);
		container.setLayout(gl);

		/*
		 * chart type
		 */
		label = new Label(container, SWT.NONE);
		label.setText("Chart Type:");

		// group: units for the x-axis
		Composite groupChartType = new Composite(container, SWT.NONE);
		groupChartType.setLayout(new GridLayout(2, false));

		{
			// radio: line chart
			fRadioSpeedLineChart = new Button(groupChartType, SWT.RADIO);
			fRadioSpeedLineChart.setText("Line Chart");
			fRadioSpeedLineChart.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					onChangeProperty();
				}
			});

			// radio: bar chart
			fRadioSpeedBarChart = new Button(groupChartType, SWT.RADIO);
			fRadioSpeedBarChart.setText("Bar Chart");
			fRadioSpeedBarChart.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					onChangeProperty();
				}
			});
		}

		// check: use custom settings
		fChkUseCustomSettings = new Button(container, SWT.CHECK);
		fChkUseCustomSettings.setText("Use custom setting for speed:");
		fChkUseCustomSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				onChangeProperty();
			}
		});
		label = new Label(container, SWT.NONE);

		/*
		 * time slice
		 */
		label = new Label(container, SWT.NONE);
		label.setText("Timeslices for speed:");

		fSpinnerTimeslice = new Spinner(container, SWT.HORIZONTAL | SWT.BORDER);
		fSpinnerTimeslice.setMaximum(1000);

		fSpinnerTimeslice.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onChangeProperty();
			}
		});

		fSpinnerTimeslice.addMouseWheelListener(new MouseWheelListener() {

			public void mouseScrolled(MouseEvent event) {

				// accelerate the change with Ctrl + Shift key
				int accelerator = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
				accelerator *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

				Spinner spinner = (Spinner) event.widget;
				final int newValue = ((event.count > 0 ? 1 : -1) * accelerator);

				spinner.setSelection(spinner.getSelection() + newValue);

				onChangeProperty();
			}
		});
	}

	@Override
	public void createPartControl(Composite parent) {

		createLayout(parent);
		restoreSettings();
	}

	private void restoreSettings() {

		IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// get values from pref store

		// use custom settings
		boolean isCustom = store.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_USE_CUSTOM);
		fChkUseCustomSettings.setSelection(isCustom);

		// time slice
		fSpinnerTimeslice.setSelection(store.getInt(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE));

		// speed chart type
		int speedChartType = store.getInt(ITourbookPreferences.GRAPH_PROPERTY_SPEED_CHARTTYPE);
		if (speedChartType == 0 || speedChartType == ChartDataModel.CHART_TYPE_LINE) {
			fRadioSpeedLineChart.setSelection(true);
		} else {
			fRadioSpeedBarChart.setSelection(true);
		}
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {

		enableControls();

		IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// set new values in the pref store

		// use custom settings
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_USE_CUSTOM,
				fChkUseCustomSettings.getSelection());

		// time slice
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE,
				fSpinnerTimeslice.getSelection());

		// speed chart type
		final int speedChartType = fRadioSpeedLineChart.getSelection()
				? ChartDataModel.CHART_TYPE_LINE
				: ChartDataModel.CHART_TYPE_BAR;
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_SPEED_CHARTTYPE, speedChartType);

		TourManager.getInstance().firePropertyChange(TourManager.TOURCHART_PROPERTY_IS_MODIFIED);
	}

	private void enableControls() {
		boolean useCustomSettings = fChkUseCustomSettings.getSelection();
		fSpinnerTimeslice.setEnabled(useCustomSettings);
	}

	@Override
	public void setFocus() {

	}

}
