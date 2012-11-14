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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.common.PointLong;
import net.tourbook.photo.PhotoToolTipUI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

/**
 * Photo tooltip for a tour chart.
 * 
 * @author Wolfgang Schramm, created 1.8.2012
 */
public class ChartPhotoToolTip extends PhotoToolTipUI {

	private ArrayList<ChartPhoto>	_hoveredPhotos	= new ArrayList<ChartPhoto>();

	private int						_devXGridCenterX;

//	/**
//	 * This counter is incremented when 0 photos can be displayed with the hovered mouse. Counter is
//	 * not incremented when subsequent different number of photos can be displayed until 0 photos
//	 * can be displayed. This counter is used to not hide photo tooltip when number of photos
//	 * changes.
//	 */
//	private int						_hideCounter;

	/*
	 * UI controls
	 */
	private TourChart				_tourChart;

	public ChartPhotoToolTip(final TourChart tourChart) {

		super(tourChart);

		_tourChart = tourChart;
	}

	Shell getShell() {
		return getToolTipShell();
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int chartHeight = _tourChart.getSize().y;
//		final int chartMarginTop = _tourChart.getMarginTop();
		final Point chartDisplay = _tourChart.toDisplay(0, 0);

		/*
		 * use grid center that the tooltip is NOT jumping when the graph is autoscrolling, the
		 * right border is sometimes still jumping :-(
		 */
		final int itemPosX = _tourChart.getLeftAxisWidth() + _devXGridCenterX;
//		final int itemPosY = _devYHoveredPhoto;

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;
		final int tipWidth2 = tipWidth / 2;

		// center tooltip horizontally
		final int ttPosX = itemPosX - tipWidth2;

		final int ttChartLocation = super.getTooltipLocation();

		int ttPosY;
		if (ttChartLocation == 1) {

			// set vertical position at the top of the upper most graph
			ttPosY = -tipHeight - 0;//+ chartMarginTop;
		} else {

			// set vertical position at the bottom
			ttPosY = chartHeight;
		}

		final Point ttLocation = _tourChart.toDisplay(ttPosX, ttPosY);

		if (ttLocation.y < 0) {
			ttLocation.y = chartDisplay.y + chartHeight;
		}

		return ttLocation;
	}

	@Override
	protected void restoreState(final IDialogSettings state) {
		super.restoreState(state);
	}

	@Override
	protected void saveState(final IDialogSettings state) {
		super.saveState(state);
	}

	public void showChartPhotoToolTip(	final ChartLayerPhoto photoLayer,
										final PointLong devHoveredValue,
										final int devXMouseMove,
										final int devYMouseMove) {

		final ArrayList<PhotoGroup> photoGroups = photoLayer.getPhotoPositions();

		if (photoGroups == null) {
			// photo positions are not initialized
			return;
		}

		_hoveredPhotos.clear();

		final ArrayList<ChartPhoto> chartPhotos = photoLayer.getChartPhotos();

		final int hoveredXPos = devXMouseMove;

		for (final PhotoGroup photoGroup : photoGroups) {

			if (hoveredXPos >= photoGroup.hGridStart && hoveredXPos <= photoGroup.hGridEnd) {

				// photo is within current hovered area

				final ArrayList<Integer> photoPosition = photoGroup.photoIndex;

				for (final int photoIndex : photoPosition) {

					final ChartPhoto chartPhoto = chartPhotos.get(photoIndex);

					_hoveredPhotos.add(chartPhoto);
				}

				// set tooltip position
				_devXGridCenterX = photoGroup.hGridStart + (ChartLayerPhoto.GROUP_HORIZONTAL_WIDTH / 2);
			}
		}

		if (_hoveredPhotos.size() == 0) {

			hide();

		} else {

			showPhotoToolTip(_hoveredPhotos);
		}
	}

}
