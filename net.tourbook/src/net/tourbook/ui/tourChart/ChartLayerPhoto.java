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
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayerPhoto implements IChartLayer {

	private static final int		GROUP_HORIZONTAL_WIDTH	= 40;

	private ArrayList<ChartPhoto>	_chartPhotos;

	private ArrayList<PhotoGroup>	_groupedPhotos			= new ArrayList<PhotoGroup>();

	public ChartLayerPhoto(final ArrayList<ChartPhoto> chartPhotos) {
		_chartPhotos = chartPhotos;
	}

	private void createGroupedPhotos(	final Point[] photoPositions,
										final int devGraphWidth,
										final int devGraphImageOffset) {

		_groupedPhotos.clear();

		final int groupWidth = GROUP_HORIZONTAL_WIDTH;

		/*
		 * ensure the groups always starts at the same position, otherwise a group can contain
		 * different number of photo when graph is zoomed in and horizontally moved
		 */
		int groupHGrid = -devGraphImageOffset % groupWidth;

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
					_groupedPhotos.add(groupPhoto);
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
				_groupedPhotos.add(groupPhoto);

				groupPhoto.hGridStart = groupHGrid - groupWidth + 1;
				groupPhoto.hGridEnd = groupHGrid;

				// keep photo index within the group
				groupPhoto.addPhoto(positionIndex);
			}
		}

		/*
		 * set position for the group average positions
		 */
		for (final PhotoGroup photoGroup : _groupedPhotos) {

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

			photoGroup.photoPosition = new Point(posX, posY);
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

		/*
		 * get all photo positions within the graph viewport (clientarea)
		 */
		final Point[] photoPositions = new Point[_chartPhotos.size()];

		for (final ChartPhoto chartPhoto : _chartPhotos) {

			final float yValue = yValues[chartPhoto.serieIndex];
			final int devYGraph = (int) ((yValue - graphYBottom) * scaleY);

			final int devXValue = (int) (chartPhoto.xValue * scaleX) - devGraphImageOffset;
			final int devYValue = devYBottom - devYGraph;

			final int photoIconWidthNoBorder = 10;
			final int photoIconWidth = photoIconWidthNoBorder + lineWidth;
			final int photoIconWidth2 = photoIconWidth / 2;

			final int devXPhoto = devXValue;
			int devYPhoto = devYValue - photoIconWidth - 20;

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
		}

		// convert all photo positions into grouped photo positions
		createGroupedPhotos(photoPositions, devGraphWidth, devGraphImageOffset);

		gc.setClipping(0, devYTop, devGraphWidth, devGraphHeight);

		gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));

//		// draw photo
//		gc.setLineWidth(lineWidth);
//		gc.drawOval(//
//				devXPhoto - photoIconWidth2,
//				devYPhoto,
//				photoIconWidth,
//				photoIconWidth);

		gc.setAntialias(SWT.ON);

		for (final PhotoGroup photoGroup : _groupedPhotos) {

			final int numberOfPhotos = photoGroup.photoIndex.size();
			final Point photoPosition = photoGroup.photoPosition;

			final String groupText = Integer.toString(numberOfPhotos);
			final Point textSize = gc.textExtent(groupText);
			final int textWidth = textSize.x;
			final int textHeight = textSize.y;

			int posY = photoPosition.y - 20;
			if (posY < devYTop) {
				posY = devYTop;
			}

// draw at original position
//			gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
//			gc.drawText(groupText, photoPosition.x, posY);

			// drag group border

			final int groupWidth = textWidth + 4;
			final int groupHeight = textHeight - 2;
			final int groupOffsetX = (GROUP_HORIZONTAL_WIDTH - groupWidth) / 2;
			final int groupX = photoGroup.hGridStart + groupOffsetX;
			final int groupY = posY + lineWidth;

			gc.setLineWidth(lineWidth);
//			gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
//			gc.drawRoundRectangle(groupX, groupY, groupWidth, groupHeight, 6, 6);

			gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
			gc.fillRoundRectangle(groupX, groupY, groupWidth, groupHeight, 6, 6);

			final int textX = groupX + 3;
			final int textY = posY - 1 + lineWidth;
			gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.drawText(groupText, textX, textY, true);

//			// debug: draw grid
//			gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
//			gc.drawLine(photoGroup.hGrid, devYTop, photoGroup.hGrid, devYBottom);
		}

		gc.setClipping((Rectangle) null);
		gc.setLineWidth(1);
	}

	public ArrayList<ChartPhoto> getChartPhotos() {
		return _chartPhotos;
	}

	/**
	 * @return Returns device position where the photos are painted, is <code>null</code> when photo
	 *         is not visible in the graph.
	 */
	ArrayList<PhotoGroup> getPhotoPositions() {
		return _groupedPhotos;
	}

}
