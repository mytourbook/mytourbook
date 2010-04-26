/**
 * 
 */
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

	private static final int		HOVER_AREA	= 2;

	/**
	 * Control for which the tour info is displayed
	 */
	private final Control			_infoControl;

	private final TourChartToolTip	_tourChartToolTip;

	private final Image				_imageTourInfo;
	private final Rectangle			_imageBounds;

	private int						_devImageX;
	private int						_devImageY;

	public class TourChartToolTip extends TourToolTip {

		public TourChartToolTip(final Control control) {
			super(control);
		}

		@Override
		public Point getLocation(final Point tipSize, final Event event) {

//			final int devX = _devImageX + _imageBounds.width + HOVER_AREA;
//			final int devY = _devImageY - HOVER_AREA;

			final int devX = _devImageX - HOVER_AREA - tipSize.x;
			final int devY = _devImageY - HOVER_AREA;

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
					&& devMouseY <= devMouseY + _imageBounds.height + HOVER_AREA) {

				return _imageBounds;
			}

			return null;
		}
	}

	public TourInfo(final Control control) {

		_infoControl = control;

		_imageTourInfo = TourbookPlugin.getImageDescriptor(Messages.Image__TourInfo).createImage();
		_imageBounds = _imageTourInfo.getBounds();

		_tourChartToolTip = new TourChartToolTip(control);

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
		_devImageY = rectangle.height - _imageBounds.height - HOVER_AREA;

		gc.drawImage(_imageTourInfo, _devImageX, _devImageY);
	}

	public void setTourData(final TourData tourData) {
		_tourChartToolTip.setTourData(tourData);
	}
}
