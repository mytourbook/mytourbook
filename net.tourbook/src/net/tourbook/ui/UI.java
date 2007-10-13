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
package net.tourbook.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;

public class UI {

	private static final String				TOUR_TYPE_PREFIX	= "tourType";					//$NON-NLS-1$

	private static UI						instance;

	private final HashMap<String, Image>	fImageCache			= new HashMap<String, Image>();

	private UI() {}

	public static ColumnPixelData getColumnPixelWidth(PixelConverter pixelConverter, int width) {
		return new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(width), false);
	}

	public static UI getInstance() {

		if (instance == null) {
			instance = new UI();
		}

		return instance;
	}

	/**
	 * Restore the sash weight from a memento
	 * 
	 * @param sash
	 * @param fMemento
	 * @param weightKey
	 * @param sashDefaultWeight
	 */
	public static void restoreSashWeight(	SashForm sash,
											IMemento fMemento,
											String weightKey,
											int[] sashDefaultWeight) {

		int[] sashWeights = sash.getWeights();
		int[] newWeights = new int[sashWeights.length];

		for (int weightIndex = 0; weightIndex < sashWeights.length; weightIndex++) {

			Integer mementoWeight = fMemento.getInteger(weightKey + Integer.toString(weightIndex));

			if (mementoWeight == null) {
				try {
					newWeights[weightIndex] = sashDefaultWeight[weightIndex];

				} catch (ArrayIndexOutOfBoundsException e) {
					newWeights[weightIndex] = 100;
				}
			} else {
				newWeights[weightIndex] = mementoWeight;
			}
		}

		sash.setWeights(newWeights);
	}

	/**
	 * Change the title for the application
	 * 
	 * @param newTitle
	 *        new title for the application or <code>null</code> to set the original title
	 */
	public static void changeAppTitle(String newTitle) {

		Display display = Display.getDefault();

		if (display != null) {

			// Look at all the shells and pick the first one that is a workbench window.
			Shell shells[] = display.getShells();
			for (int shellIdx = 0; shellIdx < shells.length; shellIdx++) {

				Object data = shells[shellIdx].getData();

				// Check whether this shell points to the Application main window's shell:
				if (data instanceof IWorkbenchWindow) {

					String title;
					if (newTitle == null) {
						title = Messages.App_Title;
					} else {
						title = newTitle;
					}

					shells[shellIdx].setText(title);
					break;
				}
			}
		}
	}

	/**
	 * Store the weights for the sash in a memento
	 * 
	 * @param sash
	 * @param memento
	 * @param weightKey
	 */
	public static void saveSashWeight(SashForm sash, IMemento memento, String weightKey) {

		int[] weights = sash.getWeights();

		for (int weightIndex = 0; weightIndex < weights.length; weightIndex++) {
			memento.putInteger(weightKey + Integer.toString(weightIndex), weights[weightIndex]);
		}
	}

	/**
	 * Set grid layout with no margins for a composite
	 * 
	 * @param composite
	 */
	public static void set0GridLayout(Composite composite) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
	}

	public static void setDefaultColor(Control control) {
		control.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
		control.setBackground(null);
	}

	public static void setErrorColor(Text control) {
		control.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		control.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
	}

	public static GridData setFieldWidth(Composite parent, StringFieldEditor field, int width) {
		GridData gd = new GridData();
		gd.widthHint = width;
		field.getTextControl(parent).setLayoutData(gd);
		return gd;
	}

	public static void setHorizontalSpacer(Composite parent, int columns) {
		Label label = new Label(parent, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = columns;
		label.setLayoutData(gd);
	}

	public static GridData setWidth(Control control, int width) {
		GridData gd = new GridData();
		gd.widthHint = width;
		control.setLayoutData(gd);
		return gd;
	}

	public static VerifyListener verifyListenerTypeLong() {

		return new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if (e.text.equals("")) { //$NON-NLS-1$
					return;
				}
				try {
					Long.parseLong(e.text);
				} catch (NumberFormatException e1) {
					e.doit = false;
				}
			}
		};
	}

	/**
	 * dispose resources
	 */
	public void dispose() {
		disposeImages();
	}

	private void disposeImages() {

		for (final Iterator<Image> iterator = fImageCache.values().iterator(); iterator.hasNext();) {
			iterator.next().dispose();
		}
		fImageCache.clear();
	}

	/**
	 * dispose all tour type images
	 */
	public void disposeTourTypeImages() {

		for (Iterator<String> iterator = fImageCache.keySet().iterator(); iterator.hasNext();) {

			final String imageId = iterator.next();

			if (imageId.startsWith(TOUR_TYPE_PREFIX)) {
				fImageCache.get(imageId).dispose();
				iterator.remove();
			}
		}
	}

	/**
	 * @param display
	 * @param graphColor
	 * @return return the color for the graph
	 */
	private DrawingColors getTourTypeColors(final Display display, final long tourTypeId) {

		final DrawingColors drawingColors = new DrawingColors();
		final ArrayList<TourType> tourTypes = TourbookPlugin.getDefault().getAllTourTypes();

		TourType colorTourType = null;

		for (final TourType tourType : tourTypes) {
			if (tourType.getTypeId() == tourTypeId) {
				colorTourType = tourType;
			}
		}

		if (colorTourType == null || colorTourType.getTypeId() == TourType.TOUR_TYPE_ID_NOT_DEFINED) {

			// tour type was not found

			drawingColors.colorBright = display.getSystemColor(SWT.COLOR_WHITE);
			drawingColors.colorDark = display.getSystemColor(SWT.COLOR_WHITE);
			drawingColors.colorLine = display.getSystemColor(SWT.COLOR_DARK_GRAY);

			// prevent disposing the colors
			drawingColors.mustBeDisposed = false;

		} else {

			drawingColors.colorBright = new Color(display, colorTourType.getRGBBright());
			drawingColors.colorDark = new Color(display, colorTourType.getRGBDark());
			drawingColors.colorLine = new Color(display, colorTourType.getRGBLine());
		}

		return drawingColors;
	}

	/**
	 * @param typeId
	 * @return Returns an image which represents the tour type
	 */
	public Image getTourTypeImage(final long typeId) {

		final String colorId = TOUR_TYPE_PREFIX + typeId;
		Image image = fImageCache.get(colorId);

		if (image == null || image.isDisposed()) {

			/*
			 * create image for the tour type
			 */

			final Display display = Display.getCurrent();

			final int imageWidth = 16;
			final int imageHeight = 16;

			image = new Image(display, imageWidth, imageHeight);

			final GC gc = new GC(image);
			{
//				final int arcSize = 4;

				final DrawingColors drawingColors = getTourTypeColors(display, typeId);

				gc.setForeground(drawingColors.colorBright);
				gc.setBackground(drawingColors.colorDark);

//				gc.setAlpha(0x80);
//				gc.fillRectangle(0, 0, imageWidth, imageHeight);
//				
//				gc.setAlpha(0xff);
//				gc.fillGradientRectangle(4, 1, 8, imageHeight - 3, false);
//				
//				gc.setForeground(drawingColors.colorLine);
//				gc.drawRoundRectangle(4, 0, 7, imageHeight - 2, arcSize, arcSize);

				gc.fillGradientRectangle(4, 4, imageWidth - 8, imageHeight - 8, false);

				gc.setForeground(drawingColors.colorLine);
				gc.drawRectangle(3, 3, imageWidth - 7, imageHeight - 7);

				drawingColors.dispose();
			}
			gc.dispose();

			fImageCache.put(colorId, image);
		}

		return image;
	}

}
