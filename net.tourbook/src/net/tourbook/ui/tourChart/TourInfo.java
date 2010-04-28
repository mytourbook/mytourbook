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

import net.tourbook.Messages;
import net.tourbook.chart.ChartToolTip;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.Util;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

final class TourInfo extends ChartToolTip {

	private static final int	HOVER_AREA	= 2;

	/**
	 * Control for which the tour info is displayed
	 */
	private final Control		_infoControl;

	private final TourChartInfo	_tourChartInfo;

	private final Image			_imageTourInfo;
	private final Rectangle		_imageBounds;

	private int					_devImageX;
	private int					_devImageY;

	public class TourChartInfo extends TourInfoContent {

		public TourChartInfo(final Control control) {
			super(control);
		}

		@Override
		public Point getLocation(final Point tipSize, final Event event) {

			final int toolTipBorder = 7;

			final int devX = _devImageX - HOVER_AREA - tipSize.x;
			final int devY = _devImageY - HOVER_AREA - toolTipBorder;

			return _infoControl.toDisplay(devX, devY);
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
//					&& devMouseY <= devMouseY + _imageBounds.height + HOVER_AREA) {

				return _imageBounds;
			}

			return null;
		}
	}

	public TourInfo(final Control control) {

		_infoControl = control;

		_imageTourInfo = TourbookPlugin.getImageDescriptor(Messages.Image__TourInfo).createImage();
		_imageBounds = _imageTourInfo.getBounds();

		_tourChartInfo = new TourChartInfo(control);

		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	private void onDispose() {
		Util.disposeResource(_imageTourInfo);
	}

	@Override
	public void paint(final GC gc, final Rectangle rectangle) {

		_devImageX = HOVER_AREA;
//		_devImageY = rectangle.height - _imageBounds.height - HOVER_AREA;
		_devImageY = HOVER_AREA;

		gc.drawImage(_imageTourInfo, _devImageX, _devImageY);
	}

	public void setTourData(final TourData tourData) {
		_tourChartInfo.setTourData(tourData);
	}
}
