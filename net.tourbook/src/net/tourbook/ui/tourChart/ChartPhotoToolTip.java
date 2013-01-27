/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoSelection;
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

	private int			_devXGridCenterX;

	/*
	 * UI controls
	 */
	private TourChart	_tourChart;

	public ChartPhotoToolTip(final TourChart tourChart, final IDialogSettings state) {

		super(tourChart, state);

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
	protected void onSelectPhoto(final PhotoSelection photoSelection) {
		PhotoManager.firePhotoEvent(null, PhotoEventId.PHOTO_SELECTION, photoSelection);
	}

	@Override
	protected void restoreState() {
		super.restoreState();
	}

	@Override
	protected void saveState() {
		super.saveState();
	}

	public void showChartPhotoToolTip(	final ChartLayerPhoto photoLayer,
										final long eventTime,
										final PointLong devHoveredValue,
										final int devXMouseMove,
										final int devYMouseMove) {

		final ArrayList<ChartPhoto> hoveredPhotos = photoLayer.getHoveredPhotos(//
				eventTime,
				devXMouseMove,
				devYMouseMove);

		if (hoveredPhotos.size() == 0) {

			hide();

		} else {

			final PhotoPaintGroup photoGroup = photoLayer.getHoveredGroup();
			final PhotoCategory hoveredCategory = photoLayer
					.getHoveredCategory(eventTime, devXMouseMove, devYMouseMove);

			// set tooltip position
			_devXGridCenterX = photoGroup.hGridStart + (ChartLayerPhoto.GROUP_HORIZONTAL_WIDTH / 2);

			final boolean isLinkPhotoDisplayed = hoveredCategory.photoType == ChartPhotoType.LINK;

			showPhotoToolTip(hoveredPhotos, isLinkPhotoDisplayed);
		}
	}

}
