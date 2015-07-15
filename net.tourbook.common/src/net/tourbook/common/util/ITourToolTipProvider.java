/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * Tour tooltip provider which can display tour data in a tooltip.
 */
public interface ITourToolTipProvider extends IToolTipProvider {

	/**
	 * This method is called after the tool tip is hidden. This method can be used to cleanup
	 * resources.
	 */
	public void afterHideToolTip();

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

	/**
	 * Paints the tool tip icon in the control
	 * 
	 * @param gc
	 * @param rectangle
	 *            Client area where the tool tip can be painted
	 */
	public void paint(GC gc, Rectangle clientArea);

	/**
	 * Set location where the mouse is hovering the client area.
	 * 
	 * @param x
	 * @param y
	 * @return Returns <code>true</code> when the mouse is hovering a hovered location.
	 */
	public boolean setHoveredLocation(int x, int y);

	/**
	 * Sets the {@link TourToolTip} into the tour tool tip provider. When set to <code>null</code>
	 * the tool tip provider is detached from the tour tool tip control.
	 * 
	 * @param tourToolTip
	 *            can be <code>null</code>
	 */
	public void setTourToolTip(TourToolTip tourToolTip);

	/**
	 * This method is called when the tool tip control is requesting the tool tip should be
	 * displayed.
	 * 
	 * @param point
	 *            Position for the mouse pointer
	 */
	public void show(Point point);

}
