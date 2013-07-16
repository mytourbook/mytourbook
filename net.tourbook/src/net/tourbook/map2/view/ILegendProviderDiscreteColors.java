/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

package net.tourbook.map2.view;

import net.tourbook.common.color.ILegendProvider;
import net.tourbook.data.TourData;

public interface ILegendProviderDiscreteColors extends ILegendProvider {

	/**
	 * @param tourData
	 *            Tour which is currently painted, can be <code>null</code>.
	 * @param valueIndex
	 *            in the data serie
	 * @param isDrawLine
	 *            Is <code>true</code> when a line is painted. This requires that the painted color
	 *            is adjusted.
	 * @return Returns the RGB value for a graph value.
	 */
	abstract int getColorValue(TourData tourData, int valueIndex, boolean isDrawLine);

}
