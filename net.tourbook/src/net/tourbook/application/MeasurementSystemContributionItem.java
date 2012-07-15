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
package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.CustomControlContribution;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class MeasurementSystemContributionItem extends CustomControlContribution {

	private static final String				ID						= "net.tourbook.measurementSelector";				//$NON-NLS-1$

	private final static IPreferenceStore	_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();

	private IPropertyChangeListener			_prefChangeListener;

	private boolean							_isFireSelectionEvent	= true;

	private Combo							_combo;

	public MeasurementSystemContributionItem() {
		this(ID);
	}

	protected MeasurementSystemContributionItem(final String id) {
		super(id);
	}

	public static void selectSystemFromPrefStore(final Combo combo) {

		final String system = _prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE);

		if (system.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM)) {
			combo.select(0);
		} else if (system.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI)) {
			combo.select(1);
		} else {
			combo.select(0);
		}
	}

	/**
	 * Sets measurement system in the pref store, updates {@link UI#UNIT_VALUE_ALTITUDE}... var's
	 * and fires modify event {@link ITourbookPreferences#MEASUREMENT_SYSTEM}
	 * 
	 * @param systemIndex
	 *            0...metric, 1...imperial
	 */
	public static void selectSystemInPrefStore(final int systemIndex) {

		if (systemIndex == 0) {

			// set metric system

			_prefStore.putValue(
					ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE,
					ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM);

			_prefStore.putValue(
					ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE,
					ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_M);

			_prefStore.putValue(
					ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE,
					ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE_C);

		} else {

			// set imperial system

			_prefStore.putValue(
					ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE,
					ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI);

			_prefStore.putValue(
					ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE,
					ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_FOOT);

			_prefStore.putValue(
					ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE,
					ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPTERATURE_F);
		}

		UI.updateUnits();

		// fire modify event
		_prefStore.setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
	}

	/**
	 * listen for changes in the person list
	 */
	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					_isFireSelectionEvent = false;
					{
						selectSystemFromPrefStore(_combo);
					}
					_isFireSelectionEvent = true;
				}
			}

		};
		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	protected Control createControl(final Composite parent) {

		final Composite ui = createUI(parent);

		addPrefListener();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		if (net.tourbook.common.UI.IS_OSX) {

			return createUI10ComboBox(parent);

		} else {

			/*
			 * on win32 a few pixel above and below the combobox are drawn, wrapping it into a
			 * composite removes the pixels
			 */
			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(container);
			{
				final Composite control = createUI10ComboBox(container);
				control.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
			}

			return container;
		}
	}

	private Composite createUI10ComboBox(final Composite parent) {

		_combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		_combo.setToolTipText(Messages.App_measurement_tooltip);

		_combo.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				_prefStore.removePropertyChangeListener(_prefChangeListener);
			}
		});

		_combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_isFireSelectionEvent == false) {
					return;
				}

				onSelectSystem();
			}
		});

		// fill combo box
		_combo.add(Messages.App_measurement_metric); // metric system
		_combo.add(Messages.App_measurement_imperial); // imperial system

		// select previous value
		selectSystemFromPrefStore(_combo);

		return _combo;
	}

	private void onSelectSystem() {

		final int selectedIndex = _combo.getSelectionIndex();

		if (selectedIndex == -1) {
			return;
		}

		selectSystemInPrefStore(selectedIndex);
	}
}
