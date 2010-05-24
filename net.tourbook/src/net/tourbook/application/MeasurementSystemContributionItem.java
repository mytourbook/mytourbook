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
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.CustomControlContribution;

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

	private static final String		ID						= "net.tourbook.measurementSelector";				//$NON-NLS-1$

	private static final boolean	IS_OSX					= "carbon".equals(SWT.getPlatform());				//$NON-NLS-1$

	private final IPreferenceStore	_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();

	private IPropertyChangeListener	_prefChangeListener;

	private boolean					_isFireSelectionEvent	= true;

	private Combo					_combo;

	public MeasurementSystemContributionItem() {
		this(ID);
	}

	protected MeasurementSystemContributionItem(final String id) {
		super(id);
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
					selectSystem();
					_isFireSelectionEvent = true;
				}
			}

		};
		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private Composite createComboBox(final Composite parent) {

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

				final int selectedIndex = _combo.getSelectionIndex();
				if (selectedIndex == -1) {
					return;
				}

				if (selectedIndex == 0) {

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

				// fire modify event
				_prefStore.setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
			}
		});

		// fill combo box
		_combo.add(Messages.App_measurement_metric); // metric system
		_combo.add(Messages.App_measurement_imperial); // imperial system

		// select previous value
		selectSystem();

		return _combo;
	}

	@Override
	protected Control createControl(final Composite parent) {

		Composite content;

		if (IS_OSX) {

			content = createComboBox(parent);

		} else {

			/*
			 * on win32 a few pixel above and below the combobox are drawn, wrapping it into a
			 * composite removes the pixels
			 */
			content = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(content);

			final Composite control = createComboBox(content);
			control.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
		}

		addPrefListener();

		return content;
	}

	private void selectSystem() {

		final String system = _prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE);

		if (system.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM)) {
			_combo.select(0);
		} else if (system.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI)) {
			_combo.select(1);
		} else {
			_combo.select(0);
		}
	}
}
