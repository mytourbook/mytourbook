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
package net.tourbook.util;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public interface ITourToolTipProvider {

	/**
	 * * Creates the content area of the the tooltip.
	 * 
	 * @param event
	 *            the event that triggered the activation of the tooltip
	 * @param parent
	 *            the parent of the content area
	 * @return the content area created
	 */
	public Composite createToolTipContentArea(Event event, Composite parent);

//	/**
//	 * @param hoveredArea
//	 * @return Returns <code>true</code> when the tour tool tip provider supports the
//	 *         {@link #hoveredArea}.
//	 *         <p>
//	 *         The tour tool tip provider is using the {@link #hoveredArea} to get the content for
//	 *         the tool tip.
//	 */
//	public boolean isHoveredAreaSupported(Object hoveredArea);

	/**
	 * Paints the tool tip icon in the control
	 * 
	 * @param gc
	 * @param rectangle
	 *            Client area where the tool tip can be painted
	 */
	public void paint(GC gc, Rectangle clientArea);

	/**
	 * This method is called when the tool tip control is requesting the tool tip should be
	 * displayed.
	 * 
	 * @param point
	 *            Position for the mouse pointer
	 */
	public void show(Point point);

}
