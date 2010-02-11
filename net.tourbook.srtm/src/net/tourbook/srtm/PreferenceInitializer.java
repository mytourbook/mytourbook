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
package net.tourbook.srtm;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
 
/**
 * Initialize preferences for the SRTM plugin
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();

		prefStore.setDefault(IPreferences.SRTM_USE_DEFAULT_DATA_FILEPATH, true);

		// set srtm default data path to the working directory
		prefStore.setDefault(IPreferences.SRTM_DATA_FILEPATH, Platform.getInstanceLocation().getURL().getPath());

		// set srtm actual color profile index
		prefStore.setDefault(IPreferences.SRTM_COLORS_SELECTED_PROFILE_ID, 0);
		prefStore.setDefault(IPreferences.SRTM_COLORS_SELECTED_PROFILE_KEY, 0);

		// apply profile when it's selected in the profile list
		prefStore.setDefault(IPreferences.SRTM_APPLY_WHEN_PROFILE_IS_SELECTED, false);

		// srtm3 server
		prefStore.setDefault(IPreferences.STATE_IS_SRTM3_FTP, false);
		prefStore.setDefault(IPreferences.STATE_SRTM3_FTP_URL, "ftp://e0srp01u.ecs.nasa.gov"); //$NON-NLS-1$
		prefStore.setDefault(IPreferences.STATE_SRTM3_HTTP_URL, "http://dds.cr.usgs.gov/srtm/version2_1"); //$NON-NLS-1$
	}
}
