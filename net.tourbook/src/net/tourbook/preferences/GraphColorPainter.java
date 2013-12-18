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
import net.tourbook.common.color.IGradientColors;
import net.tourbook.map2.view.TourMapPainter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class GraphColorPainter {

	private final IColorTreeViewer			_colorTreeViewer;

	private final HashMap<String, Image>	_imageCache	= new HashMap<String, Image>();
	private final HashMap<String, Color>	_colorCache	= new HashMap<String, Color>();

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

	Image drawColorImage(final GraphColorItem graphColor) {

		final Display display = Display.getCurrent();

		final String colorId = graphColor.getColorId();
		Image image = _imageCache.get(colorId);

		if (image == null || image.isDisposed()) {

			final int imageHeight = _treeItemHeight;
			final int imageWidth = imageHeight * 5;

//			final Rectangle borderRect = new Rectangle(0, 1, imageWidth - 1, imageHeight - 2);
			final Rectangle borderRect = new Rectangle(10, 1, imageWidth - 11, imageHeight - 3);

			image = new Image(display, imageWidth, imageHeight);

			final GC gc = new GC(image);
			{
				if (graphColor.isMapColor()) {

					// draw legend image

					/*
					 * tell the legend provider with which color the legend should be painted
					 */
					final IGradientColors legendProvider = _colorTreeViewer.getMapLegendColorProvider();
					legendProvider.setColorProfile(graphColor.getColorDefinition().getNewMapColor());

					TourMapPainter.drawMapLegend(gc, borderRect, legendProvider, false);

				} else {

					// draw 'normal' image

					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
					gc.drawRectangle(borderRect);

					gc.setBackground(getGraphColor(display, graphColor));
					gc.fillRectangle(borderRect.x + 1, borderRect.y + 1, borderRect.width - 1, borderRect.height - 1);
				}
			}
			gc.dispose();

			_imageCache.put(colorId, image);
		}

		return image;
	}

	Image drawDefinitionImage(final ColorDefinition colorDefinition) {

		final Display display = Display.getCurrent();
		final GraphColorItem[] graphColors = colorDefinition.getGraphColorParts();

		final String imageId = colorDefinition.getImageId();
		Image definitionImage = _imageCache.get(imageId);

		if (definitionImage == null || definitionImage.isDisposed()) {

			final int imageHeight = _treeItemHeight;
			final int imageWidth = 5 * imageHeight;

			final int colorHeight = imageHeight - 2;
			final int colorWidth = imageHeight - 2;

			definitionImage = new Image(display, imageWidth, imageHeight);

			final GC gc = new GC(definitionImage);
			{
//				gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
//				gc.fillRectangle(definitionImage.getBounds());

				int colorIndex = 0;
				for (final GraphColorItem graphColorItem : graphColors) {

					final int xPosition = colorIndex * imageHeight;
					final int yPosition = 0;

					final Rectangle borderRect = new Rectangle(xPosition, //
							yPosition,
							colorHeight,
							colorWidth);

					if (graphColorItem.isMapColor()) {

						// tell the legend provider how to draw the legend
						final IGradientColors legendProvider = _colorTreeViewer.getMapLegendColorProvider();
						legendProvider.setColorProfile(graphColorItem.getColorDefinition().getNewMapColor());

						TourMapPainter.drawMapLegend(gc, borderRect, legendProvider, false);

					} else {

						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));

						final Color graphColor = getGraphColor(display, graphColorItem);

						gc.setBackground(graphColor);

						gc.fillRectangle(xPosition + 1, //
								yPosition + 1,
								colorHeight - 1,
								colorWidth - 1);

						gc.drawRectangle(borderRect);
					}

					colorIndex++;
				}
			}
			gc.dispose();

			_imageCache.put(imageId, definitionImage);
		}

		return definitionImage;
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
