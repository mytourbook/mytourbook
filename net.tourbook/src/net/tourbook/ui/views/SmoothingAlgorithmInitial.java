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
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;

class SmoothingAlgorithmInitial implements ISmoothingAlgorithm {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isUpdateUI;

	/*
	 * UI controls
	 */
	private FormToolkit				_tk;
	private Spinner					_spinnerSpeedMinTime;

	SmoothingAlgorithmInitial() {}

	@Override
	public Composite createUI(final Composite parent, final boolean isShowDescription) {

		initUI(parent);

		final Composite container = createUI10(parent, isShowDescription);

		restoreState();

		return container;
	}

	private Composite createUI10(final Composite parent, final boolean isShowDescription) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.applyTo(container);
		{
			// label: title
			Label label = _tk.createLabel(container, Messages.Compute_TourValueSpeed_Title);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(label);

			// label: min alti diff
			_tk.createLabel(container, Messages.compute_tourValueSpeed_label_speedTimeSlice);

			// combo: min altitude
			_spinnerSpeedMinTime = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.applyTo(_spinnerSpeedMinTime);
			_spinnerSpeedMinTime.setMaximum(1000);
			_spinnerSpeedMinTime.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isUpdateUI) {
						return;
					}
					onModifySpeed(true);
				}
			});
			_spinnerSpeedMinTime.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onModifySpeed(true);
				}
			});

			// label: unit
			_tk.createLabel(container, Messages.app_unit_seconds);

			if (isShowDescription) {

				// label: description
				label = _tk.createLabel(container, Messages.compute_tourValueSpeed_label_description, SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.span(3, 1)
						.indent(0, 10)
						.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
						.grab(true, false)
						.applyTo(label);

				// hint bullets
				UI.createBullets(container, //
						Messages.compute_tourValueSpeed_label_description_Hints,
						1,
						3,
						UI.DEFAULT_DESCRIPTION_WIDTH,
						null);
			}
		}

		return container;
	}

	@Override
	public void dispose() {

		_tk.dispose();
	}

	private void initUI(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());
	}

	/**
	 * Property has changed, fire an change event
	 * 
	 * @param isFireModifications
	 */
	private void onModifySpeed(final boolean isFireModifications) {

		// set new values in the pref store
		saveState();

		if (isFireModifications) {

			// force all tours to recompute the speed
			TourManager.getInstance().clearTourDataCache();

			// fire unique event for all changes
			TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED, null);
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

		onModifySpeed(isFireModifications);
	}

	private void restoreState() {

		_isUpdateUI = true;
		{
			_spinnerSpeedMinTime.setSelection(_prefStore.getInt(//
					ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE));
		}
		_isUpdateUI = false;
	}

	private void saveState() {

		// spinner: compute value time slice
		_prefStore.setValue(
				ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE,
				_spinnerSpeedMinTime.getSelection());
	}

	@Override
	public void updateUIFromPrefStore() {
		restoreState();
	}

}
