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
package net.tourbook.ext.srtm;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Initialize preferences for the SRTM plugin
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(IPreferences.SRTM_USE_DEFAULT_DATA_FILEPATH, true);
		
		// set srtm default data path to the working directory
		store.setDefault(IPreferences.SRTM_DATA_FILEPATH, Platform.getInstanceLocation().getURL().getPath());

		// set srtm default color profiles
		store.setDefault(IPreferences.SRTM_COLORS_PROFILES,
				"-5000,0,25,168;" //$NON-NLS-1$
				+"0,14,76,255;" //$NON-NLS-1$
				+"10,255,255,255;" //$NON-NLS-1$
				+"20,204,255,204;" //$NON-NLS-1$
				+"40,144,239,129;" //$NON-NLS-1$
				+"80,104,225,172;" //$NON-NLS-1$
				+"160,0,102,0;" //$NON-NLS-1$
				+"320,255,204,153;" //$NON-NLS-1$
				+"500,204,153,0;" //$NON-NLS-1$
				+"640,153,51,0;" //$NON-NLS-1$
				+"1280,102,51,0;" //$NON-NLS-1$
				+"2560,51,51,51;" //$NON-NLS-1$
				+"5120,204,255,255;" //$NON-NLS-1$
				+"X" //$NON-NLS-1$
				+"-5000,0,25,168;" //$NON-NLS-1$
				+"0,14,76,255;" //$NON-NLS-1$
				+"20,179,244,129;" //$NON-NLS-1$
				+"40,144,239,129;" //$NON-NLS-1$
				+"80,104,225,172;" //$NON-NLS-1$
				+"160,113,207,57;" //$NON-NLS-1$
				+"320,255,255,0;" //$NON-NLS-1$
				+"480,255,153,0;" //$NON-NLS-1$
				+"500,153,0,0;" //$NON-NLS-1$
				+"520,255,0,51;" //$NON-NLS-1$
				+"640,255,204,204;" //$NON-NLS-1$
				+"1000,204,255,255;" //$NON-NLS-1$
				+"X" //$NON-NLS-1$
				+"-5000,0,25,168;" //$NON-NLS-1$
				+"0,14,76,255;" //$NON-NLS-1$
				+"250,204,255,204;" //$NON-NLS-1$
				+"500,102,255,102;" //$NON-NLS-1$
				+"1000,51,153,0;" //$NON-NLS-1$
				+"2000,102,51,0;" //$NON-NLS-1$
				+"3000,51,51,51;" //$NON-NLS-1$
				+"4000,204,255,255;" //$NON-NLS-1$
				+"8850,255,255,255;" //$NON-NLS-1$
				+"X" //$NON-NLS-1$
                );
		
		// set srtm actual color profile index
		store.setDefault(IPreferences.SRTM_COLORS_ACTUAL_PROFILE, 0);

		// set srtm default resolution 
		store.setDefault(IPreferences.SRTM_RESOLUTION, IPreferences.SRTM_RESOLUTION_FINE);                
	}
}
