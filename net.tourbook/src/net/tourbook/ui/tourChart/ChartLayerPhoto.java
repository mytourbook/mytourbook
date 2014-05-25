/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import net.tourbook.chart.IChartOverlay;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayerPhoto implements IChartLayer, IChartOverlay {

	private static final int			PHOTO_ICON_SIZE			= 3;
	private static final int			PHOTO_ICON_SPACING		= 2;

	static final int					GROUP_HORIZONTAL_WIDTH	= 40;

	private ArrayList<PhotoCategory>	_photoCategories;

	/**
	 * Time when hovered photos are created.
	 */
	private long						_hoveredPhotoEventTime;

	private ArrayList<ChartPhoto>		_hoveredPhotos			= new ArrayList<ChartPhoto>();
	private PhotoCategory				_hoveredPhotoCategory;
	private PhotoPaintGroup				_hoveredPaintGroup;

	private Color						_bgColorLink;
	private Color						_bgColorTour;
	private Display						_display;

	public ChartLayerPhoto(final ArrayList<PhotoCategory> photoCategories) {

		_photoCategories = photoCategories;
	}

	private void createGroupPositions(final GraphDrawingData graphDrawingData, final long devGraphImageOffset) {

		final ChartDrawingData chartDrawingData = graphDrawingData.getChartDrawingData();
		final int devVisibleChartWidth = chartDrawingData.devDevVisibleChartWidth;
		final long devVirtualGraphWidth = graphDrawingData.devVirtualGraphWidth;
		final double zoomRatio = (double) devVirtualGraphWidth / devVisibleChartWidth;

		// adjust group with to zoom ratio
		double groupWidth = GROUP_HORIZONTAL_WIDTH * zoomRatio;

		// ensure a group is not too large
		while (groupWidth > GROUP_HORIZONTAL_WIDTH * 2) {
			groupWidth /= 2;
		}

		for (final PhotoCategory photoCategorie : _photoCategories) {

			/*
			 * ensure the groups always starts at the same position, otherwise a group can contain
			 * different number of photo when graph is zoomed in and horizontally moved
			 */
			double groupHGrid = -devGraphImageOffset % groupWidth;

			final ArrayList<PhotoPaintGroup> paintGroups = photoCategorie.paintGroups;
			paintGroups.clear();

			final Point[] photoPositions = photoCategorie.photoPositions;

			PhotoPaintGroup paintGroup = null;

			for (int positionIndex = 0; positionIndex < photoPositions.length; positionIndex++) {

				final Point photoPosition = photoPositions[positionIndex];

				if (photoPosition == null) {
					// photo is not in the graph viewport
					continue;
				}

				final int photoPosX = photoPosition.x;

				if (photoPosX <= groupHGrid) {

					// current photo is in the current group

					if (paintGroup == null) {
						paintGroup = new PhotoPaintGroup();
						paintGroups.add(paintGroup);
					}

					// keep photo index within the group
					paintGroup.addPhoto(positionIndex);

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
					paintGroup = new PhotoPaintGroup();
					paintGroups.add(paintGroup);

					paintGroup.hGridStart = (int) (groupHGrid - groupWidth + 1);
					paintGroup.hGridEnd = (int) groupHGrid;

					// keep photo index within the group
					paintGroup.addPhoto(positionIndex);
				}
			}

			/*
			 * set position for the group average positions
			 */
			for (final PhotoPaintGroup paintGroup2 : paintGroups) {

				final ArrayList<Integer> photoIndizes = paintGroup2.photoIndex;

				final Point firstPosition = photoPositions[photoIndizes.get(0)];

				if (firstPosition == null) {
					// photo is not in the graph viewport
					continue;
				}

				int minX = firstPosition.x;
				int minY = firstPosition.y;
				int maxX = minX;
				int maxY = minY;

				for (final int photoIndex : photoIndizes) {

					final Point photoPosition = photoPositions[photoIndex];

					if (photoPosition == null) {
						// photo is not in the graph viewport
						continue;
					}

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

				paintGroup2.groupCenterPosition = new Point(posX, posY);
			}
		}
	}

	/**
	 * Creates a list with hovered photos.
	 */
	private void createHoveredPhotoList(final long eventTime, final int devXMouseMove, final int devYMouseMove) {

		// initialize hovered photos
		_hoveredPhotos.clear();
		_hoveredPhotoEventTime = eventTime;

		_hoveredPhotoCategory = null;
		_hoveredPaintGroup = null;

		if (_photoCategories == null || _photoCategories.size() == 0) {
			// photo positions are not initialized
			return;
		}

		final int hoveredXPos = devXMouseMove;
		final int hoveredYPos = devYMouseMove;

		final int numberOfCategories = _photoCategories.size();
		final boolean isSingleCategory = numberOfCategories == 1;

		boolean isFound = false;

		category:

		for (int categoryIndex = 0; categoryIndex < _photoCategories.size(); categoryIndex++) {

			final PhotoCategory photoCategory = _photoCategories.get(categoryIndex);

			final boolean isLastCategory = categoryIndex == numberOfCategories - 1;

			final ArrayList<ChartPhoto> chartPhotos = photoCategory.chartPhotos;

			for (final PhotoPaintGroup paintGroup : photoCategory.paintGroups) {

				if (isSingleCategory) {

					// when it's a single category, the whole chart height is checked if a group is hit

					if (hoveredXPos >= paintGroup.hGridStart && hoveredXPos <= paintGroup.hGridEnd) {

						// photo is within current hovered area

						isFound = true;
					}

				} else {

					final int devYHoverTop = paintGroup.paintedGroupDevY;
					final int devYHoverBottom = paintGroup.paintedGroupDevY + paintGroup.paintedGroupHeight;

					if (isLastCategory) {

						// vertical hovering is allowed from the photo group down to the bottom

						if (hoveredXPos >= paintGroup.hGridStart
								&& hoveredXPos <= paintGroup.hGridEnd
								&& hoveredYPos >= devYHoverTop) {

							// photo is within current hovered area

							isFound = true;
						}

					} else {

						if (hoveredXPos >= paintGroup.hGridStart
								&& hoveredXPos <= paintGroup.hGridEnd
								&& hoveredYPos >= devYHoverTop
								&& hoveredYPos <= devYHoverBottom) {

							// photo is within current hovered area

							isFound = true;
						}
					}
				}

				if (isFound) {

					for (final int photoIndex : paintGroup.photoIndex) {

						final ChartPhoto chartPhoto = chartPhotos.get(photoIndex);

						_hoveredPhotos.add(chartPhoto);
					}

					_hoveredPhotoCategory = photoCategory;
					_hoveredPaintGroup = paintGroup;

					break category;
				}
			}
		}
	}

	/**
	 * get all photo positions within the graph viewport (client area)
	 */
	private void createPhotoPositions(	final GraphDrawingData graphDrawingData,
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

		int yPhotoCategoryOffset = _photoCategories.size() * (PHOTO_ICON_SIZE + PHOTO_ICON_SPACING);

		for (final PhotoCategory photoCategorie : _photoCategories) {

			yPhotoCategoryOffset -= PHOTO_ICON_SIZE + PHOTO_ICON_SPACING;

			final ArrayList<ChartPhoto> chartPhotos = photoCategorie.chartPhotos;

			int photoIndex = 0;
			final Point[] photoPositions = photoCategorie.photoPositions = new Point[chartPhotos.size()];

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

					devYPhoto -= yPhotoCategoryOffset;

				} else {

					final int serieIndex = chartPhoto.serieIndex;

					// check bounds

					final float yValue = yValues[serieIndex];
					final int devYGraph = (int) ((yValue - graphYBottom) * scaleY);
					final int devYValue = devYBottom - devYGraph;

					devYPhoto = devYValue - PHOTO_ICON_SIZE - 2;
					devYPhoto -= yPhotoCategoryOffset;

					// force photo marker to be not below the bottom
					if (devYPhoto + PHOTO_ICON_SIZE > devYBottom) {
						devYPhoto = devYBottom - PHOTO_ICON_SIZE;
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

	}

	/**
	 * Draw photos into the current graph.
	 */
	public void draw(	final GC gc,
						final GraphDrawingData graphDrawingData,
						final Chart chart,
						final PixelConverter pixelConverter) {

		_display = Display.getCurrent();

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

		createPhotoPositions(
				graphDrawingData,
				devYTop,
				devGraphImageOffset,
				devGraphHeight,
				isHistory,
				lineWidth,
				devVisibleChartWidth,
				groupHGrid);

		// convert all photo positions into grouped photo positions
		createGroupPositions(//
				graphDrawingData,
				devGraphImageOffset);

		gc.setClipping(0, devYTop, devVisibleChartWidth, devGraphHeight);
		gc.setAntialias(SWT.ON);

		draw_10( //
				gc,
				devYTop,
				devGraphHeight,
				isHistory,
				lineWidth);

		gc.setClipping((Rectangle) null);
		gc.setLineWidth(1);
	}

	private void draw_10(	final GC gc,
							final int devYTop,
							final int devGraphHeight,
							final boolean isHistory,
							final int lineWidth) {

		int groupHeightOffset = 0;
		boolean isFirst = true;

		for (final PhotoCategory photoCategory : _photoCategories) {

			boolean isSetGroupHeightOffset = false;

			/*
			 * set color depending on photo type and number of photo categories
			 */
			gc.setForeground(_display.getSystemColor(SWT.COLOR_WHITE));
			gc.setBackground(getPhotoGroupBackgroundColor(photoCategory.photoType, false));

			for (final PhotoPaintGroup paintGroup : photoCategory.paintGroups) {

				final int numberOfPhotos = paintGroup.photoIndex.size();

				final String groupText = Integer.toString(numberOfPhotos);
				final Point textSize = gc.textExtent(groupText);
				final int textWidth = textSize.x;
				final int textHeight = textSize.y;

				final int groupWidth = textWidth + 4;
				final int groupHeight = textHeight - 2;

				if (isFirst) {
					isFirst = false;
					isSetGroupHeightOffset = true;
				}

				if (isSetGroupHeightOffset == false) {
					isSetGroupHeightOffset = true;
					groupHeightOffset += groupHeight + PHOTO_ICON_SPACING;
				}

				int groupX = paintGroup.groupCenterPosition.x - (groupWidth / 2);

				/*
				 * ensure that group text do no overlap another group
				 */
				final int gridBorder = 1;
				if (groupX + groupWidth >= paintGroup.hGridEnd) {
					groupX = paintGroup.hGridEnd - groupWidth - gridBorder;
				} else if (groupX <= paintGroup.hGridStart) {
					groupX = paintGroup.hGridStart + gridBorder;
				}

				final int textX = groupX + 3;

				int groupY;
				if (isHistory) {
					groupY = devYTop + (devGraphHeight / 3) - groupHeight / 2;
				} else {
					groupY = devYTop + lineWidth;
				}

				groupY += groupHeightOffset;

				final int textY = groupY - 1;

				/*
				 * keep painted positions which is used when group is hovered and painted with
				 * another color
				 */
				paintGroup.paintedGroupDevX = groupX;
				paintGroup.paintedGroupDevY = groupY;
				paintGroup.paintedTextDevX = textX;
				paintGroup.paintedTextDevY = textY;
				paintGroup.paintedGroupWidth = groupWidth;
				paintGroup.paintedGroupHeight = groupHeight;
				paintGroup.paintedGroupText = groupText;

				drawPhotoAndGroup(gc, paintGroup, photoCategory);

//				/*
//				 * debug: draw grid
//				 */
//				final int yHitHeight = groupY + groupHeight;// + 2 * GROUP_Y_HIT_BORDER;
//				gc.setLineWidth(1);
//				gc.setForeground(_display.getSystemColor(SWT.COLOR_RED));
//				gc.drawLine(paintGroup.hGridStart, groupY, paintGroup.hGridStart, yHitHeight);
//
//				gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_BLUE));
//				gc.drawLine(paintGroup.hGridEnd, groupY, paintGroup.hGridEnd, yHitHeight);
			}
		}
	}

	@Override
	public void drawOverlay(final GC gcOverlay) {

		if (_hoveredPaintGroup == null) {
			return;
		}

		final Device display = gcOverlay.getDevice();

		gcOverlay.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gcOverlay.setBackground(getPhotoGroupBackgroundColor(_hoveredPhotoCategory.photoType, true));

		drawPhotoAndGroup(gcOverlay, _hoveredPaintGroup, _hoveredPhotoCategory);
	}

	private void drawPhotoAndGroup(final GC gc, final PhotoPaintGroup paintGroup, final PhotoCategory photoCategory) {

		final Point[] photoPositions = photoCategory.photoPositions;

		int prevDevYPhoto = Integer.MIN_VALUE;
		int prevDevXPhoto = Integer.MIN_VALUE;

		// draw photo marker at the graph vertical position
		for (final int photoIndex : paintGroup.photoIndex) {

			final Point photoPosition = photoPositions[photoIndex];

			if (photoPosition == null) {
				// this can happen for photo positions which are outside of the chart viewport
				break;
			}

			final int devXPhoto = photoPosition.x;
			final int devYPhoto = photoPosition.y;

			// optimize painting
			if (devXPhoto == prevDevXPhoto && devYPhoto == prevDevYPhoto) {
				continue;
			}

			gc.fillRectangle(//
					devXPhoto - (PHOTO_ICON_SIZE / 2),
					devYPhoto,
					PHOTO_ICON_SIZE,
					PHOTO_ICON_SIZE);

			prevDevXPhoto = devXPhoto;
			prevDevYPhoto = devYPhoto;
		}

		// draw group
		gc.fillRoundRectangle(
				paintGroup.paintedGroupDevX,
				paintGroup.paintedGroupDevY,
				paintGroup.paintedGroupWidth,
				paintGroup.paintedGroupHeight,
				6,
				6);

		// draw text
		gc.drawText(paintGroup.paintedGroupText, paintGroup.paintedTextDevX, paintGroup.paintedTextDevY, true);
	}

	PhotoPaintGroup getHoveredPaintGroup() {
		return _hoveredPaintGroup;
	}

	PhotoCategory getHoveredPhotoCategory(final long eventTime, final int devXMouse, final int devYMouse) {

		if (eventTime == _hoveredPhotoEventTime) {
			return _hoveredPhotoCategory;
		}

		// list is dirty -> recreate hovered photos list
		createHoveredPhotoList(eventTime, devXMouse, devYMouse);

		return _hoveredPhotoCategory;
	}

	/**
	 * @param eventTime
	 * @param devXMouseMove
	 * @param devYMouseMove
	 * @return Returns photos which are currently be hovered. 0 means no photo is hovered.
	 */
	ArrayList<ChartPhoto> getHoveredPhotos(final long eventTime, final int devXMouseMove, final int devYMouseMove) {

		if (eventTime == _hoveredPhotoEventTime) {
			return _hoveredPhotos;
		}

		createHoveredPhotoList(eventTime, devXMouseMove, devYMouseMove);

		return _hoveredPhotos;
	}

	private Color getPhotoGroupBackgroundColor(final ChartPhotoType photoType, final boolean isHovered) {

		if (_photoCategories.size() == 1) {

			// only ONE category is available

			if (isHovered) {

				if (photoType == ChartPhotoType.LINK) {
					return _bgColorLink;
				} else {
					return _bgColorTour;
				}

			} else {
				return _display.getSystemColor(SWT.COLOR_DARK_GRAY);
			}

		} else {

			if (isHovered) {

				return _display.getSystemColor(SWT.COLOR_DARK_GRAY);

			} else {
				if (photoType == ChartPhotoType.LINK) {
					return _bgColorLink;
				} else {
					return _bgColorTour;
				}
			}
		}
	}

	public void setBackgroundColor(final Color bgColorLink, final Color bgColorTour) {
		_bgColorLink = bgColorLink;
		_bgColorTour = bgColorTour;
	}

	void setHoveredData(final PhotoCategory hoveredPhotoCategory, final PhotoPaintGroup hoveredPhotoGroup) {

		_hoveredPhotoCategory = hoveredPhotoCategory;
		_hoveredPaintGroup = hoveredPhotoGroup;
	}

}
