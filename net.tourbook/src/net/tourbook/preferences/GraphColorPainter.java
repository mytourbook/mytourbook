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
package net.tourbook.preferences;

import java.util.HashMap;

import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorItem;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.map2.view.TourMapPainter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class GraphColorPainter {

	static final int						GRAPH_COLOR_SPACING	= 5;

	private final IColorTreeViewer			_colorTreeViewer;

	private final HashMap<String, Image>	_imageCache			= new HashMap<String, Image>();
	private final HashMap<String, Color>	_colorCache			= new HashMap<String, Color>();

	private final int						_treeItemHeight;

	/**
	 * @param colorTree
	 */
	GraphColorPainter(final IColorTreeViewer colorTreeViewer) {

		_colorTreeViewer = colorTreeViewer;
		_treeItemHeight = _colorTreeViewer.getTreeViewer().getTree().getItemHeight();
	}

	void disposeAllResources() {

		for (final Image image : _imageCache.values()) {
			(image).dispose();
		}
		_imageCache.clear();

		for (final Color color : _colorCache.values()) {
			(color).dispose();
		}
		_colorCache.clear();
	}

	public void disposeResources(final String colorId, final String imageId) {

		final Image image = _imageCache.get(colorId);
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}
		_imageCache.remove(colorId);

		final Color color = _colorCache.get(colorId);
		if (color != null && !color.isDisposed()) {
			color.dispose();
		}
		_colorCache.remove(colorId);

		/*
		 * dispose color image for the graph definition
		 */
		_imageCache.remove(imageId);
	}

	Image drawColorImage(final GraphColorItem graphColorItem, final int horizontalImages) {

		final Display display = Display.getCurrent();

		final String colorId = graphColorItem.getColorId();
		Image image = _imageCache.get(colorId);

		if (image == null || image.isDisposed()) {

			final int borderSize = 0;

			final int imageSize = _treeItemHeight - 2;
			final int imageSpacing = GRAPH_COLOR_SPACING;
			final int imageOffsetX = imageSize + imageSpacing;

			final int imageWidth = (horizontalImages * imageSize) + ((horizontalImages - 1) * imageSpacing);
			final int imageHeight = imageSize;

			image = new Image(//
					display,
					imageWidth,
					imageHeight);

			final Rectangle drawableBounds = new Rectangle(//
					imageOffsetX,
					0,
					imageWidth - imageOffsetX,
					imageHeight);

			final GC gc = new GC(image);
			{
				if (graphColorItem.isMapColor()) {

					// draw map image

					/*
					 * tell the legend provider with which color the legend should be painted
					 */
					final IGradientColorProvider colorProvider = _colorTreeViewer.getMapLegendColorProvider();
					colorProvider.setColorProfile(graphColorItem.getColorDefinition().getMap2Color_New());

					TourMapPainter.drawMap2Legend(//
							gc,
							drawableBounds,
							colorProvider,
							false);

				} else {

					// draw graph color image

					final Color graphColor = getGraphColor(display, graphColorItem);

					// draw border
//					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
					gc.drawRectangle(//
							drawableBounds.x,
							drawableBounds.y,
							drawableBounds.width - 1,
							drawableBounds.height - 1);

					// draw graph color
					gc.setBackground(graphColor);
					gc.fillRectangle(//
							drawableBounds.x + borderSize,
							drawableBounds.y + borderSize,
							drawableBounds.width - 2 * borderSize - 0,
							drawableBounds.height - 2 * borderSize - 0);

				}
			}
			gc.dispose();

			_imageCache.put(colorId, image);
		}

		return image;
	}

	/**
	 * Draw graph and map colors into the defintion image.
	 * 
	 * @param horizontalImages
	 */
	Image drawDefinitionImage(final ColorDefinition colorDefinition, final int horizontalImages) {

		final Display display = Display.getCurrent();
		final GraphColorItem[] graphColors = colorDefinition.getGraphColorParts();

		final String imageId = colorDefinition.getImageId();
		Image defImage = _imageCache.get(imageId);

		if (defImage == null || defImage.isDisposed()) {

			final int borderSize = 0;

			final int imageSpacing = GRAPH_COLOR_SPACING;
			final int imageSize = _treeItemHeight - 2;

			defImage = new Image(//
					display,
					(horizontalImages * imageSize) + ((horizontalImages - 1) * imageSpacing),
					imageSize);

			final GC gc = new GC(defImage);
			{
				for (int colorIndex = 0; colorIndex < graphColors.length; colorIndex++) {

					final int colorX = colorIndex * (imageSize + imageSpacing);
					final int colorY = 0;

					final int contentWidth = imageSize - 2 * borderSize;
					final int contentHeight = imageSize - 2 * borderSize;

					final Rectangle imageBounds = new Rectangle(//
							colorX,
							colorY,
							imageSize,
							imageSize);

					final GraphColorItem graphColorItem = graphColors[colorIndex];

					if (graphColorItem.isMapColor()) {

						// draw 2D map color

						// tell the legend provider how to draw the legend
						final IGradientColorProvider colorProvider = _colorTreeViewer.getMapLegendColorProvider();
						colorProvider.setColorProfile(graphColorItem.getColorDefinition().getMap2Color_New());

						TourMapPainter.drawMap2Legend(//
								gc,
								imageBounds,
								colorProvider,
								false);

					} else {

						// draw graph color

						final Color graphColor = getGraphColor(display, graphColorItem);

						// draw border
//						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
						gc.drawRectangle(//
								colorX,
								colorY,
								imageBounds.width - 1,
								imageBounds.height - 1);

						// draw graph color
						gc.setBackground(graphColor);
						gc.fillRectangle(//
								colorX + borderSize,
								colorY + borderSize,
								contentWidth,
								contentHeight);
					}
				}
			}
			gc.dispose();

			_imageCache.put(imageId, defImage);
		}

		return defImage;
	}

	/**
	 * @param display
	 * @param graphColor
	 * @return return the {@link Color} for the graph
	 */
	private Color getGraphColor(final Display display, final GraphColorItem graphColor) {

		final String colorId = graphColor.getColorId();

		Color imageColor = _colorCache.get(colorId);

		if (imageColor == null) {
			imageColor = new Color(display, graphColor.getNewRGB());
			_colorCache.put(colorId, imageColor);
		}

		return imageColor;
	}

}
