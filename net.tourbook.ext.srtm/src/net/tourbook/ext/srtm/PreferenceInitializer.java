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
//		store.setDefault(IPreferences.SRTM_COLORS_PROFILES,
//                "0,14,76,255;" //$NON-NLS-1$
//                +"100,198,235,197;" //$NON-NLS-1$
//                +"200,0,102,0;" //$NON-NLS-1$
//                +"350,255,204,153;" //$NON-NLS-1$
//                +"500,204,153,0;" //$NON-NLS-1$
//                +"750,153,51,0;" //$NON-NLS-1$
//                +"1000,102,51,0;" //$NON-NLS-1$
//                +"1500,92,67,64;" //$NON-NLS-1$
//                +"2000,204,255,255;" //$NON-NLS-1$
//                +"X" //$NON-NLS-1$
//                +"0,14,76,255;" //$NON-NLS-1$
//                +"100,179,244,129;" //$NON-NLS-1$
//                +"200,144,239,129;" //$NON-NLS-1$
//                +"300,104,225,172;" //$NON-NLS-1$
//                +"400,113,207,57;" //$NON-NLS-1$
//                +"500,255,255,0;" //$NON-NLS-1$
//                +"600,255,153,0;" //$NON-NLS-1$
//                +"700,153,0,0;" //$NON-NLS-1$
//                +"800,255,0,51;" //$NON-NLS-1$
//                +"900,255,204,204;" //$NON-NLS-1$
//                +"1000,204,255,255;" //$NON-NLS-1$
//                +"X" //$NON-NLS-1$
//                +"0,14,76,255;" //$NON-NLS-1$
//                +"500,166,219,156;" //$NON-NLS-1$
//                +"1000,51,153,0;" //$NON-NLS-1$
//                +"2000,102,51,0;" //$NON-NLS-1$
//                +"3000,51,51,51;" //$NON-NLS-1$
//                +"4000,204,255,255;" //$NON-NLS-1$
//                +"8850,255,255,255;" //$NON-NLS-1$
//                +"X" //$NON-NLS-1$
//                +"0,255,255,255;" //$NON-NLS-1$
//                +"1000,178,81,0;" //$NON-NLS-1$
//                +"2000,100,0,59;" //$NON-NLS-1$
//                +"3000,0,102,127;" //$NON-NLS-1$
//                +"X" //$NON-NLS-1$
//                +"0,0,0,255;" //$NON-NLS-1$
//                +"1000,127,0,215;" //$NON-NLS-1$
//                +"2000,255,0,0;" //$NON-NLS-1$
//                +"3000,195,103,0;" //$NON-NLS-1$
//                +"4000,190,193,0;" //$NON-NLS-1$
//                +"5000,122,190,0;" //$NON-NLS-1$
//                +"6000,20,141,0;" //$NON-NLS-1$
//                +"7000,105,231,202;" //$NON-NLS-1$
//                +"8000,255,255,255;" //$NON-NLS-1$
//                +"X" //$NON-NLS-1$
//                +"0,255,255,255;" //$NON-NLS-1$
//                +"100,92,43,0;" //$NON-NLS-1$
//                +"150,166,77,0;" //$NON-NLS-1$
//                +"200,106,148,0;" //$NON-NLS-1$
//                +"250,35,161,48;" //$NON-NLS-1$
//                +"300,54,134,255;" //$NON-NLS-1$
//                +"350,130,255,255;" //$NON-NLS-1$
//                +"X" //$NON-NLS-1$
//                );
		
		// set srtm actual color profile index
		store.setDefault(IPreferences.SRTM_COLORS_ACTUAL_PROFILE, 0);

		// set srtm default resolution 
		store.setDefault(IPreferences.SRTM_RESOLUTION, IPreferences.SRTM_RESOLUTION_FINE);                
	}
}
