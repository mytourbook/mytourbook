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
import java.util.Iterator;

import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColor;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

public class ColorLabelProvider extends LabelProvider implements ITableLabelProvider {

	private final IColorTreeViewer	fColorTreeViewer;

	private HashMap<String, Image>	fImageCache	= new HashMap<String, Image>();
	private HashMap<String, Color>	fColorCache	= new HashMap<String, Color>();

	/**
	 * @param colorTree
	 */
	ColorLabelProvider(IColorTreeViewer colorTreeViewer) {
		fColorTreeViewer = colorTreeViewer;
	}

	private int	fTreeItemHeight	= -1;
	private int	fUsableImageHeight;

	private int	fColorImageWidth;
	private int	fUsableImageWidth;

	private int	fDefinitionImageWidth;

	/**
	 * @param display
	 * @return
	 */
	private void ensureImageSize(Display display) {

		Tree colorTree = fColorTreeViewer.getTreeViewer().getTree();

		fTreeItemHeight = colorTree.getItemHeight();
		fUsableImageHeight = Math.max(1, fTreeItemHeight - 4);

		fColorImageWidth = fTreeItemHeight * 4;
		fUsableImageWidth = Math.max(1, fColorImageWidth - 4);

		fDefinitionImageWidth = Math.max(1, fTreeItemHeight - 4);
	}

	public Image getColumnImage(Object element, int columnIndex) {

		Control treeControl = fColorTreeViewer.getTreeViewer().getControl();
		Display display = treeControl.getDisplay();

		if (columnIndex == 1 && element instanceof ColorDefinition) {

			ColorDefinition colorDefinition = (ColorDefinition) element;

			GraphColor[] graphColors = colorDefinition.getGraphColorParts();

			String imageId = colorDefinition.getImageId();
			Image definitionImage = fImageCache.get(imageId);

			if (definitionImage == null) {

				ensureImageSize(display);

				definitionImage = new Image(display, 4 * fTreeItemHeight, fTreeItemHeight);

				GC gc = new GC(definitionImage);
				{

					int colorIndex = 0;
					for (GraphColor graphColor : graphColors) {

						int colorOffset = colorIndex * fTreeItemHeight;

						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
						gc.setBackground(getGraphColor(display, graphColor));

						int colorOffsetWidth = colorOffset + ((fTreeItemHeight - fDefinitionImageWidth) / 2);
						int offsetWidth = (fTreeItemHeight - fDefinitionImageWidth) / 2;
						int offsetHeight = (fTreeItemHeight - fUsableImageHeight) / 2;

						gc.drawRectangle(colorOffsetWidth,
								offsetHeight,
								(fDefinitionImageWidth - offsetWidth),
								fUsableImageHeight - offsetHeight);

						gc.fillRectangle(colorOffsetWidth + 1, offsetHeight + 1, fDefinitionImageWidth
								- offsetWidth
								- 1, fUsableImageHeight - offsetHeight - 1);

						colorIndex++;
					}
				}
				gc.dispose();

				fImageCache.put(imageId, definitionImage);
			}

			return definitionImage;

		} else if (columnIndex == 2 && element instanceof GraphColor) {

			GraphColor graphColor = (GraphColor) element;

			String colorId = graphColor.getColorId();
			Image image = fImageCache.get(colorId);

			if (image == null || image.isDisposed()) {

				ensureImageSize(display);

				image = new Image(display, fColorImageWidth, fTreeItemHeight);

				GC gc = new GC(image);
				{
					gc.setBackground(treeControl.getBackground());
					gc.setForeground(treeControl.getBackground());
					gc.drawRectangle(0, 0, fColorImageWidth - 1, fTreeItemHeight - 1);

					gc.setForeground(treeControl.getForeground());
					gc.setBackground(getGraphColor(display, graphColor));

					int offsetWidth = (fColorImageWidth - fUsableImageWidth) / 2;
					int offsetHeight = (fTreeItemHeight - fUsableImageHeight) / 2;

					gc.drawRectangle(offsetWidth, offsetHeight, (fUsableImageWidth - offsetWidth), fUsableImageHeight
							- offsetHeight);

					gc.fillRectangle(offsetWidth + 1,
							offsetHeight + 1,
							fUsableImageWidth - offsetWidth - 1,
							fUsableImageHeight - offsetHeight - 1);
				}
				gc.dispose();

				fImageCache.put(colorId, image);
			}

			return image;
		}

		return null;
	}

	public String getColumnText(Object element, int columnIndex) {

		if (columnIndex == 0 && element instanceof ColorDefinition) {
			return ((ColorDefinition) (element)).getVisibleName();
		}

		if (columnIndex == 0 && element instanceof GraphColor) {
			return ((GraphColor) (element)).getName();
		}
		return null;
	}

	// public Color getForeground(Object element, int columnIndex) {
	// return null;
	// }

	/**
	 * @param display
	 * @param graphColor
	 * @return return the color for the graph
	 */
	private Color getGraphColor(Display display, GraphColor graphColor) {

		String colorId = graphColor.getColorId();

		Color imageColor = fColorCache.get(colorId);

		if (imageColor == null) {
			imageColor = new Color(display, graphColor.getNewRGB());
			fColorCache.put(colorId, imageColor);
		}
		return imageColor;
	}

	@Override
	public void dispose() {

		super.dispose();

		disposeGraphImages();
	}

	void disposeGraphImages() {

		for (Iterator<Image> i = fImageCache.values().iterator(); i.hasNext();) {
			((Image) i.next()).dispose();
		}
		fImageCache.clear();

		for (Iterator<Color> i = fColorCache.values().iterator(); i.hasNext();) {
			((Color) i.next()).dispose();
		}
		fColorCache.clear();
	}

	public void disposeColor(String colorId, String imageId) {

		Image image = fImageCache.get(colorId);
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}
		fImageCache.remove(colorId);

		Color color = fColorCache.get(colorId);
		if (color != null && !color.isDisposed()) {
			color.dispose();
		}
		fColorCache.remove(colorId);

		/*
		 * dispose color image for the graph definition
		 */
		fImageCache.remove(imageId);
	}

}
