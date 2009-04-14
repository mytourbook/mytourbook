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

import de.byteholder.geoclipse.map.TileFactoryInfo;

public class ElevationColor {

	private final static double		dimFactor	= 0.7;	// like in standard java Color.darker() 

	private static TileFactoryInfo	fTileFactoryInfo;

	public static TileFactoryInfo getTileFactoryInfo() {
		return fTileFactoryInfo;
	}

	public ElevationColor(final TileFactoryInfo tileFactoryInfo) {
		PrefPageSRTMColors.initVertexLists();
		fTileFactoryInfo = tileFactoryInfo;
	}

	public RGB getDarkerRGB(final int elev) {
		final RGB rgb = getRGB(elev);
		rgb.red *= dimFactor;
		rgb.green *= dimFactor;
		rgb.blue *= dimFactor;
		return rgb;
	}
 
	public int getResolution() {
		return PrefPageSRTMColors.getResolutionValue();
	}

	public RGB getRGB(final int elev) {
		return PrefPageSRTMColors.getRGB(elev);
	}

	@Override
	public int hashCode() {
		// Type of map is changed IFF one of colors, shadow state or grid is changed.
		return PrefPageSRTMColors.getProfileKeyHashCode();
	}

	public boolean isShadowState() {
		return PrefPageSRTMColors.isShadowState();
	}

}
