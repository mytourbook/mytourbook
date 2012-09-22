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
package net.tourbook.chart;

import org.eclipse.swt.graphics.GC;

public interface CustomOverlay {

	/**
	 * Draws into the chart overlay, this event can happen at each mouse move event.
	 * 
	 * @param gcOverlay
	 */
	public boolean draw(GC gcOverlay);

//	/**
//	 * @return Returns <code>true</code> when the overlay should be hidden, e.g. when the mouse has
//	 *         exited the chart.
//	 */
//	public boolean isHideOverlay();

	/**
	 * @param devXMouse
	 *            Mouse horizontal position or {@link Integer#MIN_VALUE} when mouse position is not
	 *            available.
	 * @param devYMouse
	 *            Mouse vertical position or {@link Integer#MIN_VALUE} when mouse position is not
	 *            available.
	 * @return Returns <code>true</code> when the custom overlay must be painted.
	 */
	public boolean onMouseMove(int devXMouse, int devYMouse);
}
