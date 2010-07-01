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

import java.util.ArrayList;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;

public class TourToolTip extends ToolTip {

	public static final int					SHELL_MARGIN			= 5;

	private ArrayList<ITourToolTipProvider>	_tourToolTipProvider	= new ArrayList<ITourToolTipProvider>();

	private final ListenerList				_hideListeners			= new ListenerList(ListenerList.IDENTITY);

	private Control							_toolTipControl;

	private HoveredAreaContext				_hoveredContext;

	public TourToolTip(final Control control) {

		super(control, NO_RECREATE, false);

		_toolTipControl = control;
	}

	/**
	 * Add a tool tip hide listener which is called when the tool tip was hidden.
	 * <p>
	 * This is helpfull to hide the hovered area when the mouse is not within the tool tip control
	 * but leaves the tool tip window.
	 * 
	 * @param hideListener
	 */
	public void addHideListener(final IToolTipHideListener hideListener) {
		_hideListeners.add(hideListener);
	}

	public void addToolTipProvider(final ITourToolTipProvider tourToolTipProvider) {

		_tourToolTipProvider.add(tourToolTipProvider);

		// tell the provider the tool tip parent
		tourToolTipProvider.setTourToolTip(this);
	}

	@Override
	protected void afterHideToolTip(final Event event) {

		for (final ITourToolTipProvider tttProvider : _tourToolTipProvider) {
			tttProvider.afterHideToolTip();
		}

		final Object[] listeners = _hideListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((IToolTipHideListener) listeners[i]).afterHideToolTip(event);
		}

		_hoveredContext = null;
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		if (_hoveredContext == null) {
			return null;
		}

		return _hoveredContext.tourToolTipProvider.createToolTipContentArea(event, parent);
	}

	/**
	 * @param tipSize
	 * @param ttTopLeft
	 *            Top/left location for the hovered area relativ to the display
	 * @return
	 */
	private Point fixupDisplayBounds(final Point tipSize, final Point ttTopLeft) {

		Rectangle displayBounds;
		final Monitor[] allMonitors = _toolTipControl.getDisplay().getMonitors();

		if (allMonitors.length > 1) {

			// By default present in the monitor of the control
			displayBounds = _toolTipControl.getMonitor().getBounds();
			final Point topLeft2 = new Point(ttTopLeft.x, ttTopLeft.y);

			// Search on which monitor the event occurred
			Rectangle monitorBounds;
			for (final Monitor monitor : allMonitors) {
				monitorBounds = monitor.getBounds();
				if (monitorBounds.contains(topLeft2)) {
					displayBounds = monitorBounds;
					break;
				}
			}

		} else {
			displayBounds = _toolTipControl.getDisplay().getBounds();
		}

		final Point bottomRight = new Point(ttTopLeft.x + tipSize.x, ttTopLeft.y + tipSize.y);

//		System.out.println();
//		System.out.println();
//		System.out.println("displayBounds\t" + displayBounds);
//		System.out.println("bottomRight\t" + bottomRight);
//		System.out.println("ttTopLeft\t" + ttTopLeft);
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (!(displayBounds.contains(ttTopLeft) && displayBounds.contains(bottomRight))) {

			if (bottomRight.x > displayBounds.x + displayBounds.width) {
//				System.out.println("1");
				ttTopLeft.x -= bottomRight.x - (displayBounds.x + displayBounds.width);
			}

			if (bottomRight.y > displayBounds.y + displayBounds.height) {
//				System.out.println("2");
				ttTopLeft.y -= bottomRight.y - (displayBounds.y + displayBounds.height);
			}

			if (ttTopLeft.x < displayBounds.x) {
//				System.out.println("3");
//original		topLeft.x = displayBounds.x;

				ttTopLeft.x += displayBounds.x + _hoveredContext.hoveredWidth + tipSize.x;
			}

			if (ttTopLeft.y < displayBounds.y) {
//				System.out.println("4");
//original			topLeft.y = displayBounds.y;
				ttTopLeft.y = displayBounds.y;
			}
		}

//		System.out.println("ttTopLeft\t" + ttTopLeft);

		return ttTopLeft;
	}

	@Override
	public Point getLocation(final Point tipSize, final Event event) {

		if (_hoveredContext == null) {
			return null;
		}

		final int devX = _hoveredContext.hoveredTopLeftX - tipSize.x;
		final int devY = _hoveredContext.hoveredTopLeftY;

		final Point toolTipDisplayLocation = _toolTipControl.toDisplay(devX, devY);

		return fixupDisplayBounds(tipSize, toolTipDisplayLocation);
	}

	@Override
	protected Object getToolTipArea(final Event event) {

		return _hoveredContext;
	}

	/**
	 * @return Returns the tool tip provider which are attached to the tour tool tip.
	 */
	public ArrayList<ITourToolTipProvider> getToolTipProvider() {
		return _tourToolTipProvider;
	}

	/**
	 * Hide the hovered area in the tooltip
	 */
	public void hideHoveredArea() {
		_hoveredContext = null;
	}

	/**
	 * @return Returns <code>true</code> when at least one {@link ITourToolTipProvider} is available
	 */
	public boolean isActive() {
		return _tourToolTipProvider.size() > 0;
	}

	/**
	 * Paints the tool tip icons into the control
	 * 
	 * @param gc
	 * @param clientArea
	 */
	public void paint(final GC gc, final Rectangle clientArea) {

		for (final ITourToolTipProvider tttProvider : _tourToolTipProvider) {
			tttProvider.paint(gc, clientArea);
		}
	}

	public void removeToolTipProvider(final ITourToolTipProvider tourToolTipProvider) {

		_tourToolTipProvider.remove(tourToolTipProvider);

		tourToolTipProvider.setTourToolTip(null);
	}

	public void setHoveredContext(final HoveredAreaContext hoveredAreaContext) {
		_hoveredContext = hoveredAreaContext;
	}

	/**
	 * Hide existing tooltip and display a new tooltip for another hovered area
	 */
	public void update() {

		hide();

		if (_hoveredContext == null) {
			return;
		}

		show(new Point(_hoveredContext.hoveredTopLeftX, _hoveredContext.hoveredTopLeftY));
	}

}
