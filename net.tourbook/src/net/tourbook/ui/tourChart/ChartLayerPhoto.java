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
/**
 * @author Wolfgang Schramm Created: 23.7.2012
 */
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayerPhoto implements IChartLayer {

	private static final int		GROUP_HORIZONTAL_WIDTH	= 40;

	private ArrayList<PhotoGroup>	_photoGroups			= new ArrayList<PhotoGroup>();

	private ArrayList<ChartPhoto>	_chartPhotos;

	public ChartLayerPhoto(final ArrayList<ChartPhoto> chartPhotos) {
		_chartPhotos = chartPhotos;
	}

	private void createGroupedPhotos(	final Point[] photoPositions,
										final int devGraphImageOffset,
										final GraphDrawingData graphDrawingData) {

		_photoGroups.clear();

		final ChartDrawingData chartDrawingData = graphDrawingData.getChartDrawingData();
		final int devVisibleChartWidth = chartDrawingData.devDevVisibleChartWidth;
		final int devVirtualGraphWidth = graphDrawingData.devVirtualGraphWidth;
		final double zoomRatio = (double) devVirtualGraphWidth / devVisibleChartWidth;

		// adjust group with to zoom ratia
		double groupWidth = GROUP_HORIZONTAL_WIDTH * zoomRatio;

		// ensure a group is not too large
		while (groupWidth > GROUP_HORIZONTAL_WIDTH * 2) {
			groupWidth /= 2;
		}

		/*
		 * ensure the groups always starts at the same position, otherwise a group can contain
		 * different number of photo when graph is zoomed in and horizontally moved
		 */
		double groupHGrid = -devGraphImageOffset % groupWidth;

		PhotoGroup groupPhoto = null;

		for (int positionIndex = 0; positionIndex < photoPositions.length; positionIndex++) {

			final Point photoPosition = photoPositions[positionIndex];

			if (photoPosition == null) {
				// photo is not in the graph viewport
				continue;
			}

			final int photoPosX = photoPosition.x;

			if (photoPosX <= groupHGrid) {

				// current photo is in the current group

				if (groupPhoto == null) {
					groupPhoto = new PhotoGroup();
					_photoGroups.add(groupPhoto);
				}

				// keep photo index within the group
				groupPhoto.addPhoto(positionIndex);

			} else {

				// current photo is in the next group

				// advance to the next group
				groupHGrid += groupWidth;

				// check if photo is within the next group
				while (true) {

					if (photoPosX <= groupHGrid) {
						break;
					}

					groupHGrid += groupWidth;
				}

				// create next group
				groupPhoto = new PhotoGroup();
				_photoGroups.add(groupPhoto);

				groupPhoto.hGridStart = (int) (groupHGrid - groupWidth + 1);
				groupPhoto.hGridEnd = (int) groupHGrid;

				// keep photo index within the group
				groupPhoto.addPhoto(positionIndex);
			}
		}

		/*
		 * set position for the group average positions
		 */
		for (final PhotoGroup photoGroup : _photoGroups) {

			final Point firstPosition = photoPositions[photoGroup.photoIndex.get(0)];

			int minX = firstPosition.x;
			int minY = firstPosition.y;
			int maxX = minX;
			int maxY = minY;

			for (final int photoIndex : photoGroup.photoIndex) {

				final Point photoPosition = photoPositions[photoIndex];

				final int photoPosX = photoPosition.x;
				final int photoPosY = photoPosition.y;

				if (photoPosX < minX) {
					minX = photoPosX;
				} else if (photoPosX > maxX) {
					maxX = photoPosX;
				}

				if (photoPosY < minY) {
					minY = photoPosY;
				} else if (photoPosY > maxY) {
					maxY = photoPosY;
				}
			}

			final int posX = minX + (maxX - minX) / 2;
			final int posY = minY + (maxY - minY) / 2;

			photoGroup.groupCenterPosition = new Point(posX, posY);
		}
	}

	/**
	 * Draw photos into the current graph.
	 */
	public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart) {

		final Display display = Display.getCurrent();

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final int devGraphImageOffset = chart.getXXDevViewPortLeftBorder();
		final int devGraphHeight = drawingData.devGraphHeight;
		final int devGraphWidth = drawingData.devVirtualGraphWidth;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float[] yValues = drawingData.getYData().getHighValues()[0];
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		final int lineWidth = 2;
		int photoIndex = 0;

		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));

		/*
		 * get all photo positions within the graph viewport (clientarea)
		 */
		final Point[] photoPositions = new Point[_chartPhotos.size()];

		for (final ChartPhoto chartPhoto : _chartPhotos) {

			final float yValue = yValues[chartPhoto.serieIndex];
			final int devYGraph = (int) ((yValue - graphYBottom) * scaleY);

			final int devXValue = (int) (chartPhoto.xValue * scaleX) - devGraphImageOffset;
			final int devYValue = devYBottom - devYGraph;

			final int photoIconWidth = 4;
			final int photoIconWidth2 = photoIconWidth / 2;

			final int devXPhoto = devXValue;
			int devYPhoto = devYValue - photoIconWidth - 2;

			// check if photo is visible
			if (devXPhoto + photoIconWidth2 < 0 || devXPhoto - photoIconWidth2 > devGraphWidth) {

				photoPositions[photoIndex++] = null;

				continue;
			}

			// don't draw the photo marker before the chart
			final int devXImageOffset = chart.getXXDevViewPortLeftBorder();
			if (devXImageOffset == 0 && devXPhoto < photoIconWidth) {
//				devXPhoto = photoIconWidth - lineWidth;
//				gc.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
			}

			// don't draw the photo marker after the chart
			if (devXPhoto + photoIconWidth > devGraphWidth) {
//				devXPhoto = devGraphWidth - photoIconWidth + lineWidth;
//				gc.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
			}

			// force photo marker to be not below the bottom
			if (devYPhoto + photoIconWidth > devYBottom) {
				devYPhoto = devYBottom - photoIconWidth;
//				gc.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
			}

			// force photo marker to be not above the top
			if (devYPhoto < devYTop) {
				devYPhoto = devYTop + lineWidth;
//				gc.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
			}

			// keep photo position which is used when tooltip is displayed
			photoPositions[photoIndex++] = new Point(devXPhoto, devYPhoto);

			// debug: draw photo at original position
			gc.fillRectangle(//
					devXPhoto - photoIconWidth2 / 2,
					devYPhoto,
					photoIconWidth,
					photoIconWidth);
		}

		// convert all photo positions into grouped photo positions
		createGroupedPhotos(photoPositions, devGraphImageOffset, drawingData);

		gc.setClipping(0, devYTop, devGraphWidth, devGraphHeight);
		gc.setAntialias(SWT.ON);

		for (final PhotoGroup photoGroup : _photoGroups) {

			final int numberOfPhotos = photoGroup.photoIndex.size();

			final String groupText = Integer.toString(numberOfPhotos);
			final Point textSize = gc.textExtent(groupText);
			final int textWidth = textSize.x;
			final int textHeight = textSize.y;

			final int posY = devYTop;

			final int groupTextWidth = textWidth + 4;
			final int groupTextHeight = textHeight - 2;
			int groupX = photoGroup.groupCenterPosition.x - (groupTextWidth / 2);
			final int groupY = posY + lineWidth;

			/*
			 * ensure that group text do no overlap another group
			 */
			if (groupX + groupTextWidth >= photoGroup.hGridEnd) {
				groupX = photoGroup.hGridEnd - groupTextWidth - 2;
			} else if (groupX <= photoGroup.hGridStart) {
				groupX = photoGroup.hGridStart + 2;
			}

			final int textX = groupX + 3;
			final int textY = posY - 1 + lineWidth;

			/*
			 * keep painted positions which is used when group is hovered and painted with another
			 * color
			 */
			photoGroup.paintedGroupDevX = groupX;
			photoGroup.paintedGroupDevY = groupY;
			photoGroup.paintedTextDevX = textX;
			photoGroup.paintedTextDevY = textY;
			photoGroup.paintedGroupTextWidth = groupTextWidth;
			photoGroup.paintedGroupTextHeight = groupTextHeight;
			photoGroup.paintedGroupText = groupText;

			gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
			gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));

			drawGroup(gc, photoGroup);

//			// debug: draw grid
//			gc.setLineWidth(1);
//			gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
//			gc.drawLine(photoGroup.hGridStart, devYTop, photoGroup.hGridStart, devYBottom);
//
//			gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
//			gc.drawLine(photoGroup.hGridEnd, devYTop, photoGroup.hGridEnd, devYBottom);
		}

		gc.setClipping((Rectangle) null);
		gc.setLineWidth(1);
	}

	void drawGroup(final GC gc, final PhotoGroup photoGroup) {

		// draw group
		gc.fillRoundRectangle(
				photoGroup.paintedGroupDevX,
				photoGroup.paintedGroupDevY,
				photoGroup.paintedGroupTextWidth,
				photoGroup.paintedGroupTextHeight,
				6,
				6);

		// draw text
		gc.drawText(photoGroup.paintedGroupText, photoGroup.paintedTextDevX, photoGroup.paintedTextDevY, true);

	}

	ArrayList<ChartPhoto> getChartPhotos() {
		return _chartPhotos;
	}

	ArrayList<PhotoGroup> getPhotoGroups() {
		return _photoGroups;
	}

	/**
	 * @return Returns device position where the photos are painted, is <code>null</code> when photo
	 *         is not visible in the graph.
	 */
	ArrayList<PhotoGroup> getPhotoPositions() {
		return _photoGroups;
	}

}
