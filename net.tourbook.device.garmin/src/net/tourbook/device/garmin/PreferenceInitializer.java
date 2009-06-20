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
package net.tourbook.device.garmin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {}

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(IPreferences.IS_IMPORT_INTO_DESCRIPTION_FIELD, true);
		store.setDefault(IPreferences.IS_IMPORT_INTO_TITLE_FIELD, false);

		store.setDefault(IPreferences.IS_TITLE_IMPORT_ALL, false);
		store.setDefault(IPreferences.NUMBER_OF_TITLE_CHARACTERS, 100);

	}

}
