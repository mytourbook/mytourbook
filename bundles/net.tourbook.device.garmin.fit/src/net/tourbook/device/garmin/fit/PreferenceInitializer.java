/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {}

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(IPreferences.FIT_TEMPERATURE_ADJUSTMENT, 0.0f);

		store.setDefault(IPreferences.FIT_IS_IGNORE_LAST_MARKER, false);
		store.setDefault(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES, 5);

		store.setDefault(IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE, false);
		store.setDefault(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION, 365 * 24 * 60 * 60);
	}

}
