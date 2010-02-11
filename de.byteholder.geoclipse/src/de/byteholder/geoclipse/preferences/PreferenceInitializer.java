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
 
package de.byteholder.geoclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.byteholder.geoclipse.Activator;

/**
 * Initialize preferences for the mapping plugin
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE, false);
		store.setDefault(IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION, true);

		store.setDefault(IMappingPreferences.OFFLINE_CACHE_PERIOD_OF_VALIDITY, 7);
		store.setDefault(IMappingPreferences.OFFLINE_CACHE_MAX_SIZE, 100);

		store.setDefault(IMappingPreferences.SHOW_MAP_TILE_INFO, true);

		store.setDefault(IMappingPreferences.THEME_FONT_LOGGING, "1|Lucida Console|9.0|0|"); //$NON-NLS-1$
	}
}
