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
package net.tourbook.ui;

import java.util.ArrayList;

import net.tourbook.util.ITourToolTipProvider;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;

import de.byteholder.geoclipse.map.HoveredAreaContext;
import de.byteholder.geoclipse.mapprovider.MP;

public class TourToolTip extends ToolTip {

	public static final int					SHELL_MARGIN			= 5;

	private ArrayList<ITourToolTipProvider>	_tourToolTipProvider	= new ArrayList<ITourToolTipProvider>();

//	private ITourToolTipProvider			_hoveredTTTProvider;

	private Control							_toolTipControl;

	private HoveredAreaContext				_hoveredContext;

//	private Object							_hoveredArea;
//	private int								_hoveredTopLeftX;
//	private int								_hoveredTopLeftY;
//	private int								_hoveredWidth;
//	private int								_hoveredHeight;

	public TourToolTip(final Control control) {

		super(control, NO_RECREATE, false);

		_toolTipControl = control;
	}

	public void addToolTipProvider(final ITourToolTipProvider tourToolTipProvider) {
		_tourToolTipProvider.add(tourToolTipProvider);
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

	public HoveredAreaContext getHoveredContext(final int mouseMovePositionX, final int mouseMovePositionY) {

		for (final ITourToolTipProvider tttProvider : _tourToolTipProvider) {

			if (tttProvider instanceof IInfoToolTipProvider) {

				final IInfoToolTipProvider mapTTProvider = (IInfoToolTipProvider) tttProvider;

				final HoveredAreaContext hoveredContext = mapTTProvider.getHoveredContext(
						mouseMovePositionX,
						mouseMovePositionY);

				if (hoveredContext != null) {
					return _hoveredContext = hoveredContext;
				}
			}
		}

		return _hoveredContext = null;
	}

	public HoveredAreaContext getHoveredContext(	final int mouseMovePositionX,
												final int mouseMovePositionY,
												final Rectangle worldPixelTopLeftViewport,
												final MP mp,
												final int mapZoomLevel,
												final int tilePixelSize,
												final boolean isTourPaintMethodEnhanced) {

		for (final ITourToolTipProvider tttProvider : _tourToolTipProvider) {

			if (tttProvider instanceof IMapToolTipProvider) {

				final IMapToolTipProvider mapTTProvider = (IMapToolTipProvider) tttProvider;

				final HoveredAreaContext hoveredContext = mapTTProvider.getHoveredContext(
						mouseMovePositionX,
						mouseMovePositionY,
						worldPixelTopLeftViewport,
						mp,
						mapZoomLevel,
						tilePixelSize,
						isTourPaintMethodEnhanced);

				if (hoveredContext != null) {
					return _hoveredContext = hoveredContext;
				}
			}
		}

		return _hoveredContext = null;
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

//		System.out.println("getToolTipArea()\t" + _hoveredArea);
//		// TODO remove SYSTEM.OUT.PRINTLN

		return _hoveredContext;
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
	 * Paints the tool tip icon in the control
	 * 
	 * @param gc
	 * @param clientArea
	 */
	public void paint(final GC gc, final Rectangle clientArea) {

		for (final ITourToolTipProvider tttProvider : _tourToolTipProvider) {
			tttProvider.paint(gc, clientArea);
		}
	}

//	public void setHoveredArea(	final Object hoveredArea,
//								final int devX,
//								final int devY,
//								final int width,
//								final int height) {
//
//		_hoveredArea = hoveredArea;
//		_hoveredTopLeftX = devX;
//		_hoveredTopLeftY = devY;
//		_hoveredWidth = width;
//		_hoveredHeight = height;
//
//		/*
//		 * get tool tip provider which supports the hovered area
//		 */
//		for (final ITourToolTipProvider ttProvider : _tourToolTipProvider) {
//			if (ttProvider.isHoveredAreaSupported(hoveredArea)) {
//				_hoveredTTTProvider = ttProvider;
//				return;
//			}
//		}
//
//		_hoveredTTTProvider = null;
//	}

	public void removeToolTipProvider(final ITourToolTipProvider tourToolTipProvider) {
		_tourToolTipProvider.remove(tourToolTipProvider);
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
