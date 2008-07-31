/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorItem;
import net.tourbook.mapping.ILegendProvider;
import net.tourbook.mapping.TourPainter;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class GraphColorLabelProvider extends LabelProvider implements ITableLabelProvider {

	private final IColorTreeViewer			fColorTreeViewer;

	private final HashMap<String, Image>	fImageCache	= new HashMap<String, Image>();
	private final HashMap<String, Color>	fColorCache	= new HashMap<String, Color>();

	private final int						fTreeItemHeight;

	/**
	 * @param colorTree
	 */
	GraphColorLabelProvider(final IColorTreeViewer colorTreeViewer) {

		fColorTreeViewer = colorTreeViewer;
		fTreeItemHeight = fColorTreeViewer.getTreeViewer().getTree().getItemHeight();
	}

	@Override
	public void dispose() {

		super.dispose();

		disposeGraphImages();
	}

	void disposeGraphImages() {

		for (final Image image : fImageCache.values()) {
			(image).dispose();
		}
		fImageCache.clear();

		for (final Color color : fColorCache.values()) {
			(color).dispose();
		}
		fColorCache.clear();
	}

	public void disposeResources(final String colorId, final String imageId) {

		final Image image = fImageCache.get(colorId);
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}
		fImageCache.remove(colorId);

		final Color color = fColorCache.get(colorId);
		if (color != null && !color.isDisposed()) {
			color.dispose();
		}
		fColorCache.remove(colorId);

		/*
		 * dispose color image for the graph definition
		 */
		fImageCache.remove(imageId);
	}

	private Image drawColorImage(final GraphColorItem graphColor) {

		final Display display = Display.getCurrent();

		final String colorId = graphColor.getColorId();
		Image image = fImageCache.get(colorId);

		if (image == null || image.isDisposed()) {

			final int imageHeight = fTreeItemHeight;
			final int imageWidth = imageHeight * 4;

			final Rectangle borderRect = new Rectangle(0, 1, imageWidth - 1, imageHeight - 2);

			image = new Image(display, imageWidth, imageHeight);

			final GC gc = new GC(image);
			{
				if (graphColor.isLegend()) {

					// draw legend image

					/*
					 * tell the legend provider with which color the legend should be painted
					 */
					final ILegendProvider legendProvider = fColorTreeViewer.getLegendProvider();
					legendProvider.setLegendColorColors(graphColor.getColorDefinition().getNewLegendColor());

					TourPainter.drawLegendColors(gc, borderRect, legendProvider, false);

				} else {

					// draw 'normal' image

					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
					gc.drawRectangle(borderRect);

					gc.setBackground(getGraphColor(display, graphColor));
					gc.fillRectangle(borderRect.x + 1, borderRect.y + 1, borderRect.width - 1, borderRect.height - 1);
				}
			}
			gc.dispose();

			fImageCache.put(colorId, image);
		}

		return image;
	}

	private Image drawDefinitionImage(final ColorDefinition colorDefinition) {

		final Display display = Display.getCurrent();
		final GraphColorItem[] graphColors = colorDefinition.getGraphColorParts();

		final String imageId = colorDefinition.getImageId();
		Image definitionImage = fImageCache.get(imageId);

		if (definitionImage == null || definitionImage.isDisposed()) {

			final int imageHeight = fTreeItemHeight;
			final int imageWidth = 4 * imageHeight;

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

					if (graphColorItem.isLegend()) {

						// tell the legend provider how to draw the legend
						final ILegendProvider legendProvider = fColorTreeViewer.getLegendProvider();
						legendProvider.setLegendColorColors(graphColorItem.getColorDefinition().getNewLegendColor());

						TourPainter.drawLegendColors(gc, borderRect, legendProvider, false);

					} else {

						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
						gc.setBackground(getGraphColor(display, graphColorItem));

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

			fImageCache.put(imageId, definitionImage);
		}

		return definitionImage;
	}

	public Image getColumnImage(final Object element, final int columnIndex) {

		if (columnIndex == 1 && element instanceof ColorDefinition) {

			return drawDefinitionImage((ColorDefinition) element);

		} else if (columnIndex == 2 && element instanceof GraphColorItem) {

			return drawColorImage((GraphColorItem) element);
		}

		return null;
	}

	public String getColumnText(final Object element, final int columnIndex) {

		if (columnIndex == 0 && element instanceof ColorDefinition) {
			return ((ColorDefinition) (element)).getVisibleName();
		}

		if (columnIndex == 0 && element instanceof GraphColorItem) {
			return ((GraphColorItem) (element)).getName();
		}
		return null;
	}

	/**
	 * @param display
	 * @param graphColor
	 * @return return the {@link Color} for the graph
	 */
	private Color getGraphColor(final Display display, final GraphColorItem graphColor) {

		final String colorId = graphColor.getColorId();

		Color imageColor = fColorCache.get(colorId);

		if (imageColor == null) {
			imageColor = new Color(display, graphColor.getNewRGB());
			fColorCache.put(colorId, imageColor);
		}

		return imageColor;
	}

}
