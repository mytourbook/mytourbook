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

import org.eclipse.swt.graphics.RGB;

public class ElevationColor {
	
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
		return PrefPageSRTMColors.getGrid();
	}
	
	public boolean isShadowState() {
		return PrefPageSRTMColors.isShadowState();
	}
	
	public int hashCode() {
        // Type of map is changed IFF one of colors, shadow state or grid is changed.
		String s = PrefPageSRTMColors.getRGBVertexListString() + isShadowState() + getGrid();
		return s.hashCode();
	}

}
