/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
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

	private static final String		ID					= "net.tourbook.measurementSelector";	//$NON-NLS-1$

	static TourbookPlugin			plugin				= TourbookPlugin.getDefault();

	private Combo					fCombo;

	private IPropertyChangeListener	fPrefChangeListener;

	private boolean					fFireSelectionEvent	= true;

	private static final boolean	osx					= "carbon".equals(SWT.getPlatform());	//$NON-NLS-1$

	public MeasurementSystemContributionItem() {
		this(ID);
	}

	protected MeasurementSystemContributionItem(String id) {
		super(id);
	}

	/**
	 * listen for changes in the person list
	 */
	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {
					fFireSelectionEvent = false;
					selectSystem();
					fFireSelectionEvent = true;
				}
			}

		};
		// register the listener
		plugin.getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	@Override
	protected Control createControl(Composite parent) {

		Composite content;

		if (osx) {

			content = createComboBox(parent);

		} else {

			/*
			 * on win32 a few pixel above and below the combobox are drawn, wrapping it into a
			 * composite removes the pixels
			 */
			content = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(content);

			Composite control = createComboBox(content);
			control.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
		}

		addPrefListener();

		return content;
	}

	private Composite createComboBox(Composite parent) {

		final IPreferenceStore prefStore = plugin.getPreferenceStore();

		fCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		fCombo.setToolTipText(Messages.App_measurement_tooltip);

		fCombo.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				plugin.getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
			}
		});

		fCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				if (fFireSelectionEvent == false) {
					return;
				}

				int selectedIndex = fCombo.getSelectionIndex();
				if (selectedIndex == -1) {
					return;
				}

				if (selectedIndex == 0) {

					// set metric system

					prefStore.putValue(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE,
							ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM);

					prefStore.putValue(ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE,
							ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_M);

					prefStore.putValue(ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE,
							ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE_C);

				} else {

					// set imperial system

					prefStore.putValue(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE,
							ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI);

					prefStore.putValue(ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE,
							ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_FOOT);

					prefStore.putValue(ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE,
							ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPTERATURE_F);
				}

				// fire modify event
				prefStore.setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
			}
		});

		// fill combo box
		fCombo.add(UI.UNIT_DISTANCE_KM); // metric system
		fCombo.add(UI.UNIT_DISTANCE_MI); // imperial system

		// select previous value
		selectSystem();

		return fCombo;
	}

	private void selectSystem() {

		String system = plugin.getPreferenceStore()
				.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE);

		if (system.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM)) {
			fCombo.select(0);
		} else if (system.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI)) {
			fCombo.select(1);
		} else {
			fCombo.select(0);
		}
	}
}
