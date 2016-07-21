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
package net.tourbook.ui.views;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.form.FormTools;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;

class SmoothingUI_Initial implements ISmoothingAlgorithm {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isUpdateUI;

	private SelectionAdapter		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;

	/*
	 * UI controls
	 */
	private FormToolkit				_tk;

	private Button					_chkIsCustomClipSettings;
	private Button					_chkIsCustomPaceClipping;

	private Label					_lblTimeClipping;
	private Label					_lblPaceClipping;

	private Spinner					_spinnerClipValues;
	private Spinner					_spinnerPaceClipping;
	private Spinner					_spinnerSpeedMinTime;

	SmoothingUI_Initial() {}

	@Override
	public Composite createUI(	final SmoothingUI smoothingUI,
								final Composite parent,
								final FormToolkit tk,
								final boolean isShowDescription,
								final boolean isShowAdditionalActions) {

		_tk = tk;

		createUI_0_init(parent);

		final Composite container = createUI_10(parent, isShowDescription);

		restoreState();

		return container;
	}

	private void createUI_0_init(final Composite parent) {

		if (_tk == null) {
			_tk = new FormToolkit(parent.getDisplay());
		}

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				if (_isUpdateUI) {
					return;
				}
				onChangeProperty();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		};
	}

	private Composite createUI_10(final Composite parent, final boolean isShowDescription) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.applyTo(container);
		{
			createUI_20_GPSSpeed(container);
			createUI_30_Clipping(container);

			if (isShowDescription) {
				createUI_50_Description(container);
			}
		}

		return container;
	}

	private void createUI_20_GPSSpeed(final Composite container) {

		{
			// Label: Speed for GPS devices
			final Label label = _tk.createLabel(container, Messages.Compute_TourValueSpeed_Title);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
		}

		{
			/*
			 * Speed &Time Slice:
			 */

			// Label
			_tk.createLabel(container, Messages.compute_tourValueSpeed_label_speedTimeSlice);

			final Composite valueContainer = _tk.createComposite(container);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(valueContainer);
			{
				// Combo
				_spinnerSpeedMinTime = new Spinner(valueContainer, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerSpeedMinTime);
				_spinnerSpeedMinTime.setMaximum(1000);
				_spinnerSpeedMinTime.addSelectionListener(_defaultSelectionListener);
				_spinnerSpeedMinTime.addMouseWheelListener(_defaultMouseWheelListener);

				// label: unit
				_tk.createLabel(valueContainer, Messages.app_unit_seconds);
			}
		}
	}

	/**
	 * All controlsa are disabled, this is only for info, changes have to be done in the
	 * "Chart Properties" view.
	 * 
	 * @param parent
	 */
	private void createUI_30_Clipping(final Composite parent) {

		{
			/*
			 * is value clipping
			 */
			_chkIsCustomClipSettings = _tk.createButton(
					parent,
					Messages.TourChart_Property_check_customize_value_clipping,
					SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(0, 20)
					.applyTo(_chkIsCustomClipSettings);
			_chkIsCustomClipSettings.addSelectionListener(_defaultSelectionListener);

			/*
			 * time slice to clip values
			 */
			_lblTimeClipping = _tk.createLabel(parent, Messages.TourChart_Property_label_time_slices);
			GridDataFactory.fillDefaults()//
					.indent(16, 0)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblTimeClipping);

			_spinnerClipValues = new Spinner(parent, SWT.HORIZONTAL | SWT.BORDER);
			_spinnerClipValues.setMaximum(100);
			_spinnerClipValues.addSelectionListener(_defaultSelectionListener);
			_spinnerClipValues.addMouseWheelListener(_defaultMouseWheelListener);
		}

		{
			/*
			 * is pace clipping
			 */
			_chkIsCustomPaceClipping = _tk.createButton(
					parent,
					Messages.TourChart_Property_check_customize_pace_clipping,
					SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.applyTo(_chkIsCustomPaceClipping);
			_chkIsCustomPaceClipping.addSelectionListener(_defaultSelectionListener);

			/*
			 * time slice to clip pace
			 */
			_lblPaceClipping = _tk.createLabel(parent, Messages.TourChart_Property_label_pace_speed);
			GridDataFactory.fillDefaults()//
					.indent(16, 0)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblPaceClipping);

			_spinnerPaceClipping = new Spinner(parent, SWT.HORIZONTAL | SWT.BORDER);
			_spinnerPaceClipping.setMaximum(100);
			_spinnerPaceClipping.addSelectionListener(_defaultSelectionListener);
			_spinnerPaceClipping.addMouseWheelListener(_defaultMouseWheelListener);
		}
	}

	private void createUI_50_Description(final Composite container) {
		{

			// label: The computation of the speed (and pace) value for one ...
			final Label label = _tk.createLabel(container, Messages.compute_tourValueSpeed_label_description, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(0, 10)
					.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);

			// hint bullets
			FormTools.createBullets(container, //
					Messages.compute_tourValueSpeed_label_description_Hints,
					1,
					3,
					UI.DEFAULT_DESCRIPTION_WIDTH,
					null);
		}
	}

	@Override
	public void dispose() {}

	private void enableControls() {

		final boolean isTimeClipping = _chkIsCustomClipSettings.getSelection();
		final boolean isPaceClipping = _chkIsCustomPaceClipping.getSelection();

		_lblPaceClipping.setEnabled(isPaceClipping);
		_lblTimeClipping.setEnabled(isTimeClipping);

		_spinnerClipValues.setEnabled(isTimeClipping);
		_spinnerPaceClipping.setEnabled(isPaceClipping);
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {
		onChangeProperty(true);
	}

	/**
	 * Property has changed, fire an change event
	 * 
	 * @param isFireModifications
	 */
	private void onChangeProperty(final boolean isFireModifications) {

		enableControls();

		// set new values in the pref store
		saveState();

		if (isFireModifications) {

			// force all tours to recompute the speed
			TourManager.getInstance().clearTourDataCache();

			// fire unique event for all changes
			TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
		}
	}

	@Override
	public void performDefaults(final boolean isFireModifications) {

		_isUpdateUI = true;
		{
			_spinnerSpeedMinTime.setSelection(_prefStore.getDefaultInt(//
					ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE));
		}
		_isUpdateUI = false;

		onChangeProperty(isFireModifications);
	}

	private void restoreState() {

		_isUpdateUI = true;
		{
			_spinnerSpeedMinTime.setSelection(_prefStore.getInt(//
					ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE));

			/*
			 * clipping
			 */
			_chkIsCustomClipSettings.setSelection(_prefStore
					.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING));
			_spinnerClipValues.setSelection(//
					_prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE));

			_chkIsCustomPaceClipping.setSelection(//
					_prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING));
			_spinnerPaceClipping.setSelection(//
					_prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE));
		}
		_isUpdateUI = false;

		enableControls();
	}

	private void saveState() {

		/*
		 * GPS time slice
		 */
		_prefStore.setValue(
				ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE,
				_spinnerSpeedMinTime.getSelection());

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
	public void updateUIFromPrefStore() {
		restoreState();
	}

}
