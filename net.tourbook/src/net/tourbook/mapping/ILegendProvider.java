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
package net.tourbook.mapping;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

public interface ILegendProvider {

	/**
	 * @param legendValue
	 * @param device
	 *            Device for which the color is created
	 * @return Returns a color for the legend value, this {@link Color} must be disposed
	 */
	abstract int getColorValue(int legendValue);

	abstract LegendColor getLegendColor();

	abstract LegendConfig getLegendConfig();

	abstract int getTourColorId();

	/**
	 * Set the colors for the legend, the values will not be changed
	 * 
	 * @param newLegendColor
	 */
	abstract void setLegendColorColors(LegendColor newLegendColor);

	abstract void setLegendColorValues(Rectangle legendBounds, int minValue, int maxValue, String unit_label_altitude);

	abstract void setLegendColorValues(Rectangle legendBounds, int[] dataSerie, String unitLabel);
}
