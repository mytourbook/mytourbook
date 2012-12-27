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
import net.tourbook.chart.ChartType;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayerPhoto implements IChartLayer {

	private static final int		PHOTO_ICON_WIDTH		= 3;

	static final int				GROUP_HORIZONTAL_WIDTH	= 40;

	private ArrayList<ChartPhoto>	_tourChartPhotos		= new ArrayList<ChartPhoto>();
	private ArrayList<PhotoGroup>	_tourPhotoGroups		= new ArrayList<PhotoGroup>();
	private Point[]					_tourPhotoPositions;

	private ArrayList<ChartPhoto>	_linkChartPhotos		= new ArrayList<ChartPhoto>();
	private ArrayList<PhotoGroup>	_linkPhotoGroups		= new ArrayList<PhotoGroup>();
	private Point[]					_linkPhotoPositions;

	public ChartLayerPhoto(final ArrayList<ChartPhoto> chartTourPhotos, final ArrayList<ChartPhoto> chartLinkPhotos) {

		_tourChartPhotos.clear();
		_tourPhotoPositions = null;

		_linkChartPhotos.clear();
		_linkPhotoPositions = null;

		if (chartTourPhotos != null) {
			_tourChartPhotos.addAll(chartTourPhotos);
		}

		if (chartLinkPhotos != null) {
			_linkChartPhotos.addAll(chartLinkPhotos);
		}
	}

	private void createGroupPositions(	final ArrayList<PhotoGroup> photoGroups,
										final Point[] photoPositions,
										final GraphDrawingData graphDrawingData,
										final long devGraphImageOffset) {

		photoGroups.clear();

		final ChartDrawingData chartDrawingData = graphDrawingData.getChartDrawingData();
		final int devVisibleChartWidth = chartDrawingData.devDevVisibleChartWidth;
		final long devVirtualGraphWidth = graphDrawingData.devVirtualGraphWidth;
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
					photoGroups.add(groupPhoto);
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
				photoGroups.add(groupPhoto);

				groupPhoto.hGridStart = (int) (groupHGrid - groupWidth + 1);
				groupPhoto.hGridEnd = (int) groupHGrid;

				// keep photo index within the group
				groupPhoto.addPhoto(positionIndex);
			}
		}

		/*
		 * set position for the group average positions
		 */
		for (final PhotoGroup photoGroup : photoGroups) {

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
	 * get all photo positions within the graph viewport (client area)
	 */
	private void createPhotoPositions(	final ArrayList<ChartPhoto> chartPhotos,
										final Point[] photoPositions,
										final GraphDrawingData graphDrawingData,
										final int devYTop,
										final long devGraphImageOffset,
										final int devGraphHeight,
										final boolean isHistory,
										final int lineWidth,
										final int devVisibleChartWidth,
										final double groupHGrid) {

		final double scaleX = graphDrawingData.getScaleX();
		final double scaleY = graphDrawingData.getScaleY();

		final float graphYBottom = graphDrawingData.getGraphYBottom();
		final float[] yValues = graphDrawingData.getYData().getHighValuesFloat()[0];

		final int devYBottom = graphDrawingData.getDevYBottom();

		int photoIndex = 0;

		for (final ChartPhoto chartPhoto : chartPhotos) {

			final double devXPhotoValue = scaleX * chartPhoto.xValue;
			final int devXPhoto = (int) (devXPhotoValue - devGraphImageOffset);

			// check if photo is visible
			if (devXPhoto < groupHGrid) {

				// skip invisible photos

				photoIndex++;

				continue;
			}

			// check if photo is to the right of the right border
			if (devXPhoto > devVisibleChartWidth) {
				break;
			}

			int devYPhoto;
			if (isHistory) {

				devYPhoto = devYBottom - (devGraphHeight / 3);

			} else {

				final int serieIndex = chartPhoto.serieIndex;

				// check bounds

				final float yValue = yValues[serieIndex];
				final int devYGraph = (int) ((yValue - graphYBottom) * scaleY);
				final int devYValue = devYBottom - devYGraph;
				devYPhoto = devYValue - PHOTO_ICON_WIDTH - 2;

				// force photo marker to be not below the bottom
				if (devYPhoto + PHOTO_ICON_WIDTH > devYBottom) {
					devYPhoto = devYBottom - PHOTO_ICON_WIDTH;
				}

				// force photo marker to be not above the top
				if (devYPhoto < devYTop) {
					devYPhoto = devYTop + lineWidth;
				}
			}

			// keep photo position which is used when tooltip is displayed
			photoPositions[photoIndex++] = new Point(devXPhoto, devYPhoto);
		}
	}

	/**
	 * Draw photos into the current graph.
	 */
	public void draw(final GC gc, final GraphDrawingData graphDrawingData, final Chart chart) {

		final Display display = Display.getCurrent();

		final int devYTop = graphDrawingData.getDevYTop();
		final long devGraphImageOffset = chart.getXXDevViewPortLeftBorder();
		final int devGraphHeight = graphDrawingData.devGraphHeight;

		final ChartDrawingData chartDrawingData = graphDrawingData.getChartDrawingData();
		final int devVisibleChartWidth = chartDrawingData.devDevVisibleChartWidth;
		final long devVirtualGraphWidth = graphDrawingData.devVirtualGraphWidth;
		final double zoomRatio = (double) devVirtualGraphWidth / devVisibleChartWidth;

		final boolean isHistory = graphDrawingData.getChartType() == ChartType.HISTORY;

		final int lineWidth = 2;

		// adjust group with to zoom ratio
		double groupWidth = GROUP_HORIZONTAL_WIDTH * zoomRatio;

		// ensure a group is not too large
		while (groupWidth > GROUP_HORIZONTAL_WIDTH * 2) {
			groupWidth /= 2;
		}

		/*
		 * ensure the groups always starts at the same position, otherwise a group can contain
		 * different number of photo when graph is zoomed in and horizontally moved
		 */
		final double groupHGrid = -devGraphImageOffset % groupWidth;

		gc.setClipping(0, devYTop, devVisibleChartWidth, devGraphHeight);
		gc.setAntialias(SWT.ON);

		if (_linkChartPhotos.size() > 0) {

			_linkPhotoPositions = new Point[_linkChartPhotos.size()];

			createPhotoPositions(_linkChartPhotos, _linkPhotoPositions,//
					graphDrawingData,
					devYTop,
					devGraphImageOffset,
					devGraphHeight,
					isHistory,
					lineWidth,
					devVisibleChartWidth,
					groupHGrid);

			// convert all photo positions into grouped photo positions
			createGroupPositions(_linkPhotoGroups, _linkPhotoPositions, //
					graphDrawingData,
					devGraphImageOffset);

			draw_10(_linkPhotoGroups, _linkPhotoPositions, //
					gc,
					display,
					devYTop,
					devGraphHeight,
					isHistory,
					lineWidth);
		}

		if (_tourChartPhotos.size() > 0) {

			_tourPhotoPositions = new Point[_tourChartPhotos.size()];

			createPhotoPositions(_tourChartPhotos, _tourPhotoPositions,//
					graphDrawingData,
					devYTop,
					devGraphImageOffset,
					devGraphHeight,
					isHistory,
					lineWidth,
					devVisibleChartWidth,
					groupHGrid);

			// convert all photo positions into grouped photo positions
			createGroupPositions(_tourPhotoGroups, _tourPhotoPositions, //
					graphDrawingData,
					devGraphImageOffset);

			draw_10(_tourPhotoGroups, _tourPhotoPositions, //
					gc,
					display,
					devYTop,
					devGraphHeight,
					isHistory,
					lineWidth);
		}

		gc.setClipping((Rectangle) null);
		gc.setLineWidth(1);
	}

	private void draw_10(	final ArrayList<PhotoGroup> photoGroups,
							final Point[] photoPositions,
							final GC gc,
							final Display display,
							final int devYTop,
							final int devGraphHeight,
							final boolean isHistory,
							final int lineWidth) {

		for (final PhotoGroup photoGroup : photoGroups) {

			final int numberOfPhotos = photoGroup.photoIndex.size();

			final String groupText = Integer.toString(numberOfPhotos);
			final Point textSize = gc.textExtent(groupText);
			final int textWidth = textSize.x;
			final int textHeight = textSize.y;

			final int groupTextWidth = textWidth + 4;
			final int groupTextHeight = textHeight - 2;
			int groupX = photoGroup.groupCenterPosition.x - (groupTextWidth / 2);

			/*
			 * ensure that group text do no overlap another group
			 */
			final int gridBorder = 1;
			if (groupX + groupTextWidth >= photoGroup.hGridEnd) {
				groupX = photoGroup.hGridEnd - groupTextWidth - gridBorder;
			} else if (groupX <= photoGroup.hGridStart) {
				groupX = photoGroup.hGridStart + gridBorder;
			}

			final int textX = groupX + 3;

			int groupY;
			if (isHistory) {
				groupY = devYTop + (devGraphHeight / 3) - groupTextHeight / 2;
			} else {
				groupY = devYTop + lineWidth;
			}
			final int textY = groupY - 1;

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

			drawPhotoAndGroup(gc, photoGroup, photoPositions);

//			// debug: draw grid
//			gc.setLineWidth(1);
//			gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
//			gc.drawLine(photoGroup.hGridStart, devYTop, photoGroup.hGridStart, devYBottom);
//
//			gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
//			gc.drawLine(photoGroup.hGridEnd, devYTop, photoGroup.hGridEnd, devYBottom);
		}
	}

	void drawPhotoAndGroup(final GC gc, final PhotoGroup photoGroup, final Point[] photoPositions) {

		int prevDevYPhoto = Integer.MIN_VALUE;
		int prevDevXPhoto = Integer.MIN_VALUE;

		// draw photo icon
		for (final int photoIndex : photoGroup.photoIndex) {

			final Point photoPosition = photoPositions[photoIndex];
			if (photoPosition == null) {
				// this happened
				break;
			}

			final int devXPhoto = photoPosition.x;
			final int devYPhoto = photoPosition.y;

			// optimize painting
			if (devXPhoto == prevDevXPhoto && devYPhoto == prevDevYPhoto) {
				continue;
			}

			gc.fillRectangle(//
					devXPhoto - (PHOTO_ICON_WIDTH / 2),
					devYPhoto,
					PHOTO_ICON_WIDTH,
					PHOTO_ICON_WIDTH);

			prevDevXPhoto = devXPhoto;
			prevDevYPhoto = devYPhoto;
		}

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
		return chartPhotos;
	}

	ArrayList<PhotoGroup> getPhotoGroups() {
		return photoGroups;
	}

}
