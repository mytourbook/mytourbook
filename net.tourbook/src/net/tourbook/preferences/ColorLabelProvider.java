/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

	HashMap<String, Image>			fImages	= new HashMap<String, Image>();
	HashMap<String, Color>			fColors	= new HashMap<String, Color>();

	/**
	 * @param colorTree
	 */
	ColorLabelProvider(IColorTreeViewer colorTreeViewer) {
		fColorTreeViewer = colorTreeViewer;
	}

	private int	fTreeItemHeight	= -1;
	private int	fUsableImageHeight;

	private int	fColorImageWidth;
	private int	fColorImageUsableWidth;

	private int	fImageWidth;
	private int	fUsableDefImageWidth;

	/**
	 * @param display
	 * @return
	 */
	private void ensureImageSize(Display display) {
		if (fTreeItemHeight == -1) {

			Tree colorTree = fColorTreeViewer.getTreeViewer().getTree();

			fTreeItemHeight = colorTree.getItemHeight();
			fUsableImageHeight = Math.max(1, fTreeItemHeight - 4);

			int graphColors = 4;
			fColorImageWidth = colorTree.getItemHeight() * graphColors;
			fColorImageUsableWidth = Math.max(1, fColorImageWidth - 4);

			fImageWidth = colorTree.getItemHeight();
			fUsableDefImageWidth = Math.max(1, fImageWidth - 4);
		}
	}

	public Image getColumnImage(Object element, int columnIndex) {

		Control treeControl = fColorTreeViewer.getTreeViewer().getControl();
		Display display = treeControl.getDisplay();

		if (columnIndex == 2 && element instanceof GraphColor) {

			GraphColor graphColor = (GraphColor) element;

			String colorId = graphColor.getColorId();
			Image image = fImages.get(colorId);

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

					int offsetWidth = (fColorImageWidth - fColorImageUsableWidth) / 2;
					int offsetHeight = (fTreeItemHeight - fUsableImageHeight) / 2;

					gc.drawRectangle(offsetWidth,
							offsetHeight,
							(fColorImageUsableWidth - offsetWidth),
							fUsableImageHeight - offsetHeight);

					gc.fillRectangle(offsetWidth + 1, offsetHeight + 1, fColorImageUsableWidth
							- offsetWidth
							- 1, fUsableImageHeight - offsetHeight - 1);
				}
				gc.dispose();

				fImages.put(colorId, image);
			}

			return image;

		} else if (columnIndex == 1 && element instanceof ColorDefinition) {

			ColorDefinition colorDefinition = (ColorDefinition) element;

			GraphColor[] graphColors = colorDefinition.getChildren();

			String imageId = colorDefinition.getImageId();
			Image definitionImage = fImages.get(imageId);

			if (definitionImage == null) {

				ensureImageSize(display);

				definitionImage = new Image(display,
						graphColors.length * fImageWidth,
						fTreeItemHeight);

				GC gc = new GC(definitionImage);
				{

					int colorIndex = 0;
					for (GraphColor graphColor : graphColors) {

						int colorOffset = colorIndex * fImageWidth;

						// gc.setForeground(treeControl.getForeground());
						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
						gc.setBackground(getGraphColor(display, graphColor));

						int colorOffsetWidth = colorOffset
								+ ((fImageWidth - fUsableDefImageWidth) / 2);
						int offsetWidth = (fImageWidth - fUsableDefImageWidth) / 2;
						int offsetHeight = (fTreeItemHeight - fUsableImageHeight) / 2;

						gc.drawRectangle(colorOffsetWidth,
								offsetHeight,
								(fUsableDefImageWidth - offsetWidth),
								fUsableImageHeight - offsetHeight);

						gc.fillRectangle(colorOffsetWidth + 1,
								offsetHeight + 1,
								fUsableDefImageWidth - offsetWidth - 1,
								fUsableImageHeight - offsetHeight - 1);

//						gc.setAntialias(SWT.ON);
//						gc.fillOval(colorOffsetWidth + 0, offsetHeight + 0, fUsableDefImageWidth
//								- offsetWidth
//								+ 3, fUsableImageHeight - offsetHeight + 3);
//						gc.setAntialias(SWT.OFF);
//
//						gc.drawOval(colorOffsetWidth, offsetHeight, (fUsableDefImageWidth
//								- offsetWidth + 2), fUsableImageHeight - offsetHeight + 2);

						colorIndex++;
					}
				}
				gc.dispose();

				fImages.put(imageId, definitionImage);
			}

			return definitionImage;
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

		Color imageColor = fColors.get(colorId);

		if (imageColor == null) {
			imageColor = new Color(display, graphColor.getNewRGB());
			fColors.put(colorId, imageColor);
		}
		return imageColor;
	}

	@Override
	public void dispose() {

		super.dispose();

		disposeGraphImages();
	}

	void disposeGraphImages() {

		for (Iterator<Image> i = fImages.values().iterator(); i.hasNext();) {
			((Image) i.next()).dispose();
		}
		fImages.clear();

		for (Iterator<Color> i = fColors.values().iterator(); i.hasNext();) {
			((Color) i.next()).dispose();
		}
		fColors.clear();
	}

	public void disposeColor(String colorId, String imageId) {

		Image image = fImages.get(colorId);
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}
		fImages.remove(colorId);

		Color color = fColors.get(colorId);
		if (color != null && !color.isDisposed()) {
			color.dispose();
		}
		fColors.remove(colorId);

		/*
		 * dispose color image for the graph definition
		 */
		fImages.remove(imageId);
	}

}
