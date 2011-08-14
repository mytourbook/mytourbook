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
package net.tourbook.ui.views;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.part.ViewPart;

public class TourChartPropertyView extends ViewPart {

	public static final String		ID			= "net.tourbook.views.TourChartPropertyView";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault()//
														.getPreferenceStore();

	private Button					_rdoLineChartType;
	private Button					_rdoBarChartType;

	private Button					_chkIsCustomClipSettings;
	private Spinner					_spinnerClipValues;

	private Button					_chkIsCustomPaceClipping;
	private Spinner					_spinnerPaceClipping;

	private SelectionAdapter		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;

	@Override
	public void createPartControl(final Composite parent) {

		initUI();
		createUI(parent);
		restoreStore();
	}

	private void createUI(final Composite parent) {

		final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledContainer.setExpandVertical(true);
		scrolledContainer.setExpandHorizontal(true);

		final Composite container = new Composite(scrolledContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
//				.spacing(0, 0)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI10ChartType(container);
			createUI30Clipping(container);
		}

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

	private void createUI10ChartType(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);
		{
			/*
			 * chart type
			 */
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.TourChart_Property_label_chart_type);

			final Composite group = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(group);
			{
				// radio: line chart
				_rdoLineChartType = new Button(group, SWT.RADIO);
				_rdoLineChartType.setText(Messages.TourChart_Property_chart_type_line);
				_rdoLineChartType.addSelectionListener(_defaultSelectionListener);

				// radio: bar chart
				_rdoBarChartType = new Button(group, SWT.RADIO);
				_rdoBarChartType.setText(Messages.TourChart_Property_chart_type_bar);
				_rdoBarChartType.addSelectionListener(_defaultSelectionListener);
			}
		}
	}

	private void createUI30Clipping(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 0).spacing(5, 0).applyTo(container);
		{
			/*
			 * is value clipping
			 */
			_chkIsCustomClipSettings = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.applyTo(_chkIsCustomClipSettings);
			_chkIsCustomClipSettings.setText(Messages.TourChart_Property_check_customize_value_clipping);
			_chkIsCustomClipSettings.addSelectionListener(_defaultSelectionListener);

			/*
			 * time slice to clip values
			 */
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().indent(16, 0).align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.TourChart_Property_label_time_slices);

			_spinnerClipValues = new Spinner(container, SWT.HORIZONTAL | SWT.BORDER);
			_spinnerClipValues.setMaximum(1000);
			_spinnerClipValues.addSelectionListener(_defaultSelectionListener);
			_spinnerClipValues.addMouseWheelListener(_defaultMouseWheelListener);

			/*
			 * is pace clipping
			 */
			_chkIsCustomPaceClipping = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.applyTo(_chkIsCustomPaceClipping);
			_chkIsCustomPaceClipping.setText(Messages.TourChart_Property_check_customize_pace_clipping);
			_chkIsCustomPaceClipping.addSelectionListener(_defaultSelectionListener);

			/*
			 * time slice to clip pace
			 */
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.indent(16, 0)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.TourChart_Property_label_pace_speed);

			_spinnerPaceClipping = new Spinner(container, SWT.HORIZONTAL | SWT.BORDER);
			_spinnerPaceClipping.setMaximum(1000);
			_spinnerPaceClipping.addSelectionListener(_defaultSelectionListener);
			_spinnerPaceClipping.addMouseWheelListener(_defaultMouseWheelListener);
		}
	}

	private void enableControls() {

		_spinnerClipValues.setEnabled(_chkIsCustomClipSettings.getSelection());
		_spinnerPaceClipping.setEnabled(_chkIsCustomPaceClipping.getSelection());
	}

	private void initUI() {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onChangeProperty();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		};
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {

		enableControls();

		saveStore();

		TourManager.getInstance().removeAllToursFromCache();
//		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourChartPropertyView.this);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED, null);
	}

	private void restoreStore() {

		/*
		 * chart type
		 */
		final int speedChartType = _prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);
		if (speedChartType == 0 || speedChartType == ChartDataModel.CHART_TYPE_LINE) {
			_rdoLineChartType.setSelection(true);
		} else {
			_rdoBarChartType.setSelection(true);
		}

		/*
		 * clipping
		 */
		_chkIsCustomClipSettings.setSelection(_prefStore.getBoolean(//
				ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING));
		_spinnerClipValues.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE));

		_chkIsCustomPaceClipping.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING));
		_spinnerPaceClipping.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE));

		enableControls();
	}

	/**
	 * Update new values in the pref store
	 */
	private void saveStore() {

		/*
		 * chart type
		 */
		final int speedChartType = _rdoLineChartType.getSelection()
				? ChartDataModel.CHART_TYPE_LINE
				: ChartDataModel.CHART_TYPE_BAR;
		_prefStore.setValue(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE, speedChartType);

		/*
		 * clip time slices
		 */
		_prefStore.setValue(
				ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING,
				_chkIsCustomClipSettings.getSelection());
		_prefStore.setValue(
				ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE,
				_spinnerClipValues.getSelection());

		/*
		 * pace clipping value in 0.1 km/h-mph
		 */
		_prefStore.setValue(
				ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING,
				_chkIsCustomPaceClipping.getSelection());

		_prefStore.setValue(
				ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE,
				_spinnerPaceClipping.getSelection());
	}

	@Override
	public void setFocus() {
		_rdoLineChartType.setFocus();
	}

}
