/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.part.ViewPart;

public class TourChartPropertyView extends ViewPart implements IComputeTourValues {

	public static final String	ID	= "net.tourbook.views.TourChartPropertyView";	//$NON-NLS-1$

	private Spinner				fSpinnerComputeValues;
	private Spinner				fSpinnerClipValues;

	private Button				fRadioLineChartType;
	private Button				fRadioBarChartType;

	private Button				fChkUseCustomComputeSettings;
	private Button				fChkUseCustomClipSettings;

	private Button				fChkUseCustomPaceClipping;
	private Spinner				fSpinnerPaceClipping;

	private void adjustSpinnerValueOnMouseScroll(final MouseEvent event) {

		// accelerate with Ctrl + Shift key
		int accelerator = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
		accelerator *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

		final Spinner spinner = (Spinner) event.widget;
		final int newValue = ((event.count > 0 ? 1 : -1) * accelerator);

		spinner.setSelection(spinner.getSelection() + newValue);
	}

	public boolean computeTourValues(final TourData tourData) {

		tourData.computeComputedValues();

		return true;
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
		restoreSettings();
	}

	private void createUI(final Composite parent) {

		GridLayout gl;
		GridData gd;
		Label label;

		final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledContainer.setExpandVertical(true);
		scrolledContainer.setExpandHorizontal(true);

		final Composite container = new Composite(scrolledContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);

		final Composite containerChartType = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 0).applyTo(containerChartType);
		{
			/*
			 * chart type
			 */
			label = new Label(containerChartType, SWT.NONE);
			label.setText(Messages.TourChart_Property_label_chart_type);

			// group: 
			final Composite groupChartType = new Composite(containerChartType, SWT.NONE);
			gl = new GridLayout(2, false);
			gl.marginTop = 0;
			gl.marginWidth = 0;
			groupChartType.setLayout(gl);

			{
				// radio: line chart
				fRadioLineChartType = new Button(groupChartType, SWT.RADIO);
				fRadioLineChartType.setText(Messages.TourChart_Property_chart_type_line);
				fRadioLineChartType.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						onChangeProperty();
					}
				});

				// radio: bar chart
				fRadioBarChartType = new Button(groupChartType, SWT.RADIO);
				fRadioBarChartType.setText(Messages.TourChart_Property_chart_type_bar);
				fRadioBarChartType.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						onChangeProperty();
					}
				});
			}
		}

		final Composite containerCustomizeValues = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 0).spacing(0, 0).applyTo(containerCustomizeValues);

		// check: use custom settings to compute values
		fChkUseCustomComputeSettings = new Button(containerCustomizeValues, SWT.CHECK);
		fChkUseCustomComputeSettings.setText(Messages.TourChart_Property_check_customize_value_computing);
		fChkUseCustomComputeSettings.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false, 2, 1));
		fChkUseCustomComputeSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onChangeProperty();
			}
		});

		/*
		 * computed value time slice
		 */
		label = new Label(containerCustomizeValues, SWT.NONE);
		label.setText(Messages.TourChart_Property_label_time_slices);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);

		fSpinnerComputeValues = new Spinner(containerCustomizeValues, SWT.HORIZONTAL | SWT.BORDER);
		fSpinnerComputeValues.setMaximum(1000);

		fSpinnerComputeValues.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeProperty();
			}
		});

		fSpinnerComputeValues.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		});

		/*
		 * value clipping
		 */
		fChkUseCustomClipSettings = new Button(containerCustomizeValues, SWT.CHECK);
		fChkUseCustomClipSettings.setText(Messages.TourChart_Property_check_customize_value_clipping);

		gd = new GridData(SWT.NONE, SWT.NONE, false, false, 2, 1);
		gd.verticalIndent = 5;
		fChkUseCustomClipSettings.setLayoutData(gd);

		fChkUseCustomClipSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onChangeProperty();
			}
		});

		/*
		 * time slice to clip values
		 */
		label = new Label(containerCustomizeValues, SWT.NONE);
		label.setText(Messages.TourChart_Property_label_time_slices);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);

		fSpinnerClipValues = new Spinner(containerCustomizeValues, SWT.HORIZONTAL | SWT.BORDER);
		fSpinnerClipValues.setMaximum(1000);

		fSpinnerClipValues.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeProperty();
			}
		});

		fSpinnerClipValues.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		});

		/*
		 * pace clipping
		 */
		fChkUseCustomPaceClipping = new Button(containerCustomizeValues, SWT.CHECK);
		fChkUseCustomPaceClipping.setText(Messages.TourChart_Property_check_customize_pace_clipping);

		gd = new GridData(SWT.NONE, SWT.NONE, false, false, 2, 1);
		gd.verticalIndent = 5;
		fChkUseCustomPaceClipping.setLayoutData(gd);

		fChkUseCustomPaceClipping.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onChangeProperty();
			}
		});

		/*
		 * time slice to clip pace
		 */
		label = new Label(containerCustomizeValues, SWT.NONE);
		label.setText(Messages.TourChart_Property_label_pace_speed);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);

		fSpinnerPaceClipping = new Spinner(containerCustomizeValues, SWT.HORIZONTAL | SWT.BORDER);
		fSpinnerPaceClipping.setMaximum(1000);

		fSpinnerPaceClipping.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeProperty();
			}
		});

		fSpinnerPaceClipping.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		});

		/*
		 * button: update computed values
		 */
		final Button btnComputValues = new Button(container, SWT.NONE);
		btnComputValues.setText(Messages.TourChart_Property_button_compute_values);
		btnComputValues.setToolTipText(Messages.TourChart_Property_button_compute_values_tooltip);
		btnComputValues.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (MessageDialog.openConfirm(
						Display.getCurrent().getActiveShell(),
						Messages.TourChart_Property_dlg_compute_values_title,
						Messages.TourChart_Property_dlg_compute_values_message)) {

					TourDatabase.computeComputedValuesForAllTours(TourChartPropertyView.this);

					TourManager.getInstance().removeAllToursFromCache();
					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourChartPropertyView.this);

					// fire unique event for all changes
					TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED, null);
				}
			}
		});

		/*
		 * setup scrolled container
		 */
		scrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				scrolledContainer.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		scrolledContainer.setContent(container);
	}

	private void enableControls() {

		fSpinnerComputeValues.setEnabled(fChkUseCustomComputeSettings.getSelection());
		fSpinnerClipValues.setEnabled(fChkUseCustomClipSettings.getSelection());
		fSpinnerPaceClipping.setEnabled(fChkUseCustomPaceClipping.getSelection());
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {

		enableControls();

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// set new values in the pref store

		// checkbox: use custom settings to compute values
		store.setValue(
				ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING,
				fChkUseCustomComputeSettings.getSelection());

		// spinner: compute value time slice
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE, fSpinnerComputeValues.getSelection());

		// checkbox: clip values
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING, fChkUseCustomClipSettings.getSelection());

		// spinner: clip value time slice
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE, fSpinnerClipValues.getSelection());

		// checkbox: pace clipping
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING, fChkUseCustomPaceClipping.getSelection());

		// spinner: pace clipping value in 0.1 km/h-mph
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE, fSpinnerPaceClipping.getSelection());

		// radio: chart type
		final int speedChartType = fRadioLineChartType.getSelection()
				? ChartDataModel.CHART_TYPE_LINE
				: ChartDataModel.CHART_TYPE_BAR;
		store.setValue(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE, speedChartType);

		TourManager.getInstance().removeAllToursFromCache();
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourChartPropertyView.this);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED, null);
	}

	private void restoreSettings() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// get values from pref store

		fChkUseCustomComputeSettings.setSelection(store.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING));
		fSpinnerComputeValues.setSelection(store.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE));

		fChkUseCustomClipSettings.setSelection(store.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING));
		fSpinnerClipValues.setSelection(store.getInt(ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE));

		fChkUseCustomPaceClipping.setSelection(store.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING));
		fSpinnerPaceClipping.setSelection(store.getInt(ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE));

		// chart type
		final int speedChartType = store.getInt(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);
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
