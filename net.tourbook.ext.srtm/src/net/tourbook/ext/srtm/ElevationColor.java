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
/**
 * @author Alfred Barten
 */
package net.tourbook.ext.srtm;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.RGB;

public class ElevationColor {
	
	private final static IPreferenceStore iPreferenceStore = Activator.getDefault().getPreferenceStore();
	
	private final static double dimFactor = 0.7; // like in standard java Color.darker() 
	
	public ElevationColor() {
		PrefPageSRTMColors.initVertexLists();
	}
	
	public RGB getRGB(int elev) {
		return PrefPageSRTMColors.getRGB(elev);
	}

	public RGB getDarkerRGB(int elev) {
		RGB rgb = getRGB(elev);
		rgb.red   *= dimFactor;
		rgb.green *= dimFactor;
		rgb.blue  *= dimFactor;
		return rgb;
	}

	public int getGrid() {
		// elevation is used at every grid-th pixel in both directions; 
		// the other values are interpolated
		// i.e. it gives the resolution of the image!
		String srtmResolution = iPreferenceStore.getString(IPreferences.SRTM_RESOLUTION);
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_ROUGH))
			return 64;
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_ROUGH))
			return 16;
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_FINE))
			return 4;
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_FINE))
			return 1;		
		return 4;
	}
	
	public boolean isShadowState() {
		return PrefPageSRTMColors.isShadowState();
	}

}
