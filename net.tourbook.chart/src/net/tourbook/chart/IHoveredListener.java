/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
 * @author Wolfgang Schramm Created: 31.7.2012
 */
package net.tourbook.chart;

import net.tourbook.common.PointLong;

public interface IHoveredListener {

	/**
	 * Hide tooltip because chart has been modified (zoomed in/out)
	 */
	void hideTooltip();

	/**
	 * Event is fired when mouse is moved over a line graph value point.
	 * 
	 * @param hoveredValueIndex
	 * @param devHoveredValue
	 * @param devYMouseMove
	 * @param devXMouseMove
	 */
	void hoveredValue(int hoveredValueIndex, PointLong devHoveredValue, int devXMouseMove, int devYMouseMove);

}
