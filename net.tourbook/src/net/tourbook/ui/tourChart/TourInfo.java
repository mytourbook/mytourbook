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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.ITourToolTip;
import net.tourbook.util.Util;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;

public class TourInfo implements ITourToolTip {

	private static final int			HOVER_AREA		= 2;

	/**
	 * Control for which the tour info is displayed
	 */
	protected final Control				_infoControl;

	protected final TourInfoToolTip		_tourInfoToolTip;

	private final ArrayList<TourData>	_tourDataList	= new ArrayList<TourData>();

	private final Image					_imageTourInfo;
	private final Rectangle				_imageBounds;

	private int							_devImageX;
	private int							_devImageY;

	private boolean						_isVisible		= true;

	protected class TourInfoToolTip extends TourToolTip {

		public TourInfoToolTip(final Control control) {
			super(control);
		}

		private Point fixupDisplayBounds(final Point tipSize, final Point topLeft) {

			Rectangle displayBounds;
			final Monitor[] allMonitors = _infoControl.getDisplay().getMonitors();

			if (allMonitors.length > 1) {

				// By default present in the monitor of the control
				displayBounds = _infoControl.getMonitor().getBounds();
				final Point topLeft2 = new Point(topLeft.x, topLeft.y);

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
				displayBounds = _infoControl.getDisplay().getBounds();
			}

			final Point bottomRight = new Point(topLeft.x + tipSize.x, topLeft.y + tipSize.y);

//			System.out.println();
//			System.out.println();
//			System.out.println("displayBounds\t" + displayBounds);
//			System.out.println("bottomRight\t" + bottomRight);
//			System.out.println("topLeft\t\t" + topLeft);
//			// TODO remove SYSTEM.OUT.PRINTLN

			if (!(displayBounds.contains(topLeft) && displayBounds.contains(bottomRight))) {

				if (bottomRight.x > displayBounds.x + displayBounds.width) {
//					System.out.println("1");
					topLeft.x -= bottomRight.x - (displayBounds.x + displayBounds.width);
				}

				if (bottomRight.y > displayBounds.y + displayBounds.height) {
//					System.out.println("2");
					topLeft.y -= bottomRight.y - (displayBounds.y + displayBounds.height);
				}

				if (topLeft.x < displayBounds.x) {
//					System.out.println("3");
// original			topLeft.x = displayBounds.x;
					topLeft.x += _devImageX + _imageBounds.width + HOVER_AREA + tipSize.x;
				}

				if (topLeft.y < displayBounds.y) {
//					System.out.println("4");
// original			topLeft.y = displayBounds.y;
					topLeft.y = displayBounds.y;
				}
			}

//			System.out.println("topLeft\t\t" + topLeft);

			return topLeft;
		}

		@Override
		public Point getLocation(final Point tipSize, final Event event) {

			final int toolTipBorder = 7;

			final int devX = _devImageX - HOVER_AREA - tipSize.x;
			final int devY = _devImageY - HOVER_AREA - toolTipBorder;

			final Point location = _infoControl.toDisplay(devX, devY);

			return fixupDisplayBounds(tipSize, location);
		}

		@Override
		protected Object getToolTipArea(final Event event) {

			final int devMouseX = event.x;
			final int devMouseY = event.y;

			// display the tool tip when the mouse hovers the info image
			if (devMouseX >= _devImageX - HOVER_AREA
					&& devMouseX <= _devImageX + _imageBounds.width + HOVER_AREA
					&& devMouseY >= _devImageY - HOVER_AREA
					&& devMouseY <= _devImageY + _imageBounds.height + HOVER_AREA) {

				return _imageBounds;
			}

			return null;
		}

		@Override
		protected boolean shouldCreateToolTip(final Event event) {

			if (_isVisible == false) {
				return false;
			}

			return super.shouldCreateToolTip(event);
		}

	}

	public TourInfo(final Control control) {

		_infoControl = control;

		_imageTourInfo = TourbookPlugin.getImageDescriptor(Messages.Image__TourInfo).createImage();
		_imageBounds = _imageTourInfo.getBounds();

		_tourInfoToolTip = new TourInfoToolTip(control);

		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	private void onDispose() {
		Util.disposeResource(_imageTourInfo);
		_tourDataList.clear();
	}

	@Override
	public void paint(final GC gc, final Rectangle rectangle) {

		if (_isVisible == false) {
			return;
		}

		_devImageX = HOVER_AREA;
		_devImageY = HOVER_AREA;

		gc.drawImage(_imageTourInfo, _devImageX, _devImageY);
	}

	public void setTourData(final TourData tourData) {

		if (tourData == null) {
			_tourDataList.clear();
			return;
		}

		// populate list
		_tourDataList.clear();
		_tourDataList.add(tourData);

		_tourInfoToolTip.setTourData(_tourDataList);
	}

	public void setTourDataList(final ArrayList<TourData> tourDataList) {

		if (tourDataList == null || tourDataList.size() == 0) {
			_tourDataList.clear();
			return;
		}

		_tourInfoToolTip.setTourData(tourDataList);
	}

	public void setTourId(final long tourId) {
		_tourInfoToolTip.setTourId(tourId);
	}

	public void setVisible(final boolean isVisible) {
		_isVisible = isVisible;
	}

	@Override
	public void show(final Point point) {

	}
}
