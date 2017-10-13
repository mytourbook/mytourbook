/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.tourType;

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.ImagePainter;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.DrawingColors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class TourTypeImage {

	private static final String								TOUR_TYPE_PREFIX		= "tourType";	//$NON-NLS-1$

	private final static HashMap<String, Image>				_imageCache				=
			new HashMap<String, Image>();
	private final static HashMap<String, ImageDescriptor>	_imageCacheDescriptor	=
			new HashMap<String, ImageDescriptor>();
	private final static HashMap<String, Boolean>			_dirtyImages			=
			new HashMap<String, Boolean>();

//	final BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_4BYTE_ABGR);
//
//	final Graphics2D g2d = image.createGraphics();
//	try {
//
////		g2d.setColor(java.awt.Color.ORANGE);
////		g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
//
//		drawMapLegend_GradientColors_AWT(
//				g2d,
//				colorProvider,
//				config,
//				imageWidth,
//				imageHeight,
//				isVertical,
//				isDrawUnits,
//				isDrawBorder);
//
//	} finally {
//		g2d.dispose();
//	}
//
//	return ImageConverter.convertIntoSWT(image);

	private static Image createTourTypeImage(final long typeId, final String colorId, final Image existingImage) {

		final Image tourTypeImage = net.tourbook.common.UI.createTransparentImage(
				TourType.TOUR_TYPE_IMAGE_SIZE,
				TourType.TOUR_TYPE_IMAGE_SIZE,
				existingImage,
				new ImagePainter() {

					@Override
					public void drawImage(final GC gc) {
						drawTourTypeImage(typeId, gc);
					}
				});

		// keep image in cache
		_imageCache.put(colorId, tourTypeImage);

		return tourTypeImage;
	}

	/**
	 * dispose resources
	 */
	public static void dispose() {
		disposeImages();
	}

	private static void disposeImages() {

//		System.out.println("disposeImages:\t");
		for (final Image image : _imageCache.values()) {
			image.dispose();
		}

		_imageCache.clear();
		_imageCacheDescriptor.clear();
	}

	private static void drawTourTypeImage(final long typeId, final GC gc) {

		if (typeId == TourType.IMAGE_KEY_DIALOG_SELECTION) {

			// create a default image

		} else if (typeId == TourDatabase.ENTITY_IS_NOT_SAVED) {

			// make the image invisible
			return;
		}

		final TourTypeLayout imageLayout = TourTypeManager.getCurrentImageLayout();
		final TourTypeBorder borderLayout = TourTypeManager.getCurrentBorderLayout();
		final int borderWidth = TourTypeManager.getCurrentBorderWidth();

		final int imageSize = TourType.TOUR_TYPE_IMAGE_SIZE;
		final Display display = Display.getCurrent();

		final DrawingColors drawingColors = getTourTypeColors(display, typeId);
		{
			final Color colorBright = drawingColors.colorBright;
			final Color colorDark = drawingColors.colorDark;
			final Color colorLine = drawingColors.colorLine;

			drawTourTypeImage_Background(gc, imageLayout, borderWidth, imageSize, colorBright, colorDark);

			if (borderWidth > 0) {
				drawTourTypeImage_Border(gc, borderLayout, borderWidth, imageSize, colorLine);
			}
		}
		drawingColors.dispose();
	}

	private static void drawTourTypeImage_Background(	final GC gc,
												final TourTypeLayout imageLayout,
												final int borderWidth,
												final int imageSize,
												final Color colorBright,
												final Color colorDark) {

		boolean isFillRectangle = false;
		boolean isOval = false;
		boolean isGradient = false;
		boolean isVertical = false;

		switch (imageLayout) {

		case FILL_RECT_BRIGHT:
			isFillRectangle = true;
			gc.setBackground(colorBright);
			break;
		case FILL_RECT_DARK:
			isFillRectangle = true;
			gc.setBackground(colorDark);
			break;

		case FILL_CIRCLE_BRIGHT:
			isOval = true;
			gc.setBackground(colorBright);
			break;
		case FILL_CIRCLE_DARK:
			isOval = true;
			gc.setBackground(colorDark);
			break;

		case GRADIENT_LEFT_RIGHT:
			isGradient = true;
			gc.setBackground(colorBright);
			gc.setForeground(colorDark);
			break;
		case GRADIENT_RIGHT_LEFT:
			isGradient = true;
			gc.setBackground(colorDark);
			gc.setForeground(colorBright);
			break;

		case GRADIENT_TOP_BOTTOM:
			isGradient = true;
			isVertical = true;
			gc.setBackground(colorBright);
			gc.setForeground(colorDark);
			break;
		case GRADIENT_BOTTOM_TOP:
			isGradient = true;
			isVertical = true;
			gc.setBackground(colorDark);
			gc.setForeground(colorBright);
			break;

		case NOTHING:
		default:
			break;
		}

		if (isFillRectangle) {

			gc.fillRectangle(0, 0, imageSize, imageSize);

		} else if (isGradient) {

			gc.fillGradientRectangle(0, 0, imageSize, imageSize, isVertical);

		} else if (isOval) {

			final int ovalSize = imageSize - 0;

			gc.setAntialias(SWT.ON);
			gc.fillOval(//
					imageSize / 2 - ovalSize / 2,
					imageSize / 2 - ovalSize / 2,
					ovalSize,
					ovalSize);
		}
	}

	private static void drawTourTypeImage_Border(	final GC gc,
											final TourTypeBorder borderLayout,
											final int borderSize,
											final int imageSize,
											final Color colorLine) {

		boolean isLeft = false;
		boolean isRight = false;
		boolean isTop = false;
		boolean isBottom = false;
		boolean isCircle = false;

		switch (borderLayout) {

		case BORDER_RECTANGLE:
			isLeft = true;
			isRight = true;
			isTop = true;
			isBottom = true;
			break;

		case BORDER_LEFT:
			isLeft = true;
			break;
		case BORDER_RIGHT:
			isRight = true;
			break;
		case BORDER_LEFT_RIGHT:
			isLeft = true;
			isRight = true;
			break;

		case BORDER_BOTTOM:
			isBottom = true;
			break;
		case BORDER_TOP:
			isTop = true;
			break;
		case BORDER_TOP_BOTTOM:
			isTop = true;
			isBottom = true;
			break;

		case BORDER_CIRCLE:
			isCircle = true;
			break;

		default:
			break;
		}

		if (isLeft || isRight || isTop || isBottom) {

			gc.setBackground(colorLine);

			if (isLeft) {
				gc.fillRectangle(0, 0, borderSize, imageSize);
			}

			if (isRight) {
				gc.fillRectangle(imageSize - borderSize, 0, borderSize, imageSize);
			}

			if (isTop) {
				gc.fillRectangle(0, 0, imageSize, borderSize);
			}

			if (isBottom) {
				gc.fillRectangle(0, imageSize - borderSize, imageSize, borderSize);
			}

		} else if (isCircle) {

			final int ovalSize = imageSize - 0;

			gc.setForeground(colorLine);
			gc.setAntialias(SWT.ON);

			gc.drawOval(//
					imageSize / 2 - ovalSize / 2 - 0,
					imageSize / 2 - ovalSize / 2 - 0,
					ovalSize - 1,
					ovalSize - 1);

		}
	}

	/**
	 * @param display
	 * @param graphColor
	 * @return return the color for the graph
	 */
	private static DrawingColors getTourTypeColors(final Display display, final long tourTypeId) {

		final DrawingColors drawingColors = new DrawingColors();
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		TourType colorTourType = null;

		for (final TourType tourType : tourTypes) {
			if (tourType.getTypeId() == tourTypeId) {
				colorTourType = tourType;
				break;
			}
		}

		if (tourTypeId == TourType.IMAGE_KEY_DIALOG_SELECTION
				|| colorTourType == null
				|| colorTourType.getTypeId() == TourDatabase.ENTITY_IS_NOT_SAVED) {

			// tour type was not found use default color

			drawingColors.colorBright = display.getSystemColor(SWT.COLOR_YELLOW);
			drawingColors.colorDark = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
			drawingColors.colorLine = display.getSystemColor(SWT.COLOR_BLACK);

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
	 * @return Returns an image which represents the tour type. This image must not be disposed,
	 *         this is done when the app closes.
	 */
	public static Image getTourTypeImage(final long typeId) {

		final String keyColorId = TOUR_TYPE_PREFIX + typeId;
		final Image existingImage = _imageCache.get(keyColorId);

		// check if image is available
		if (existingImage != null && existingImage.isDisposed() == false) {

			// check if the image is dirty
			if (_dirtyImages.size() == 0 || _dirtyImages.containsKey(keyColorId) == false) {

				// image is available and not dirty
				return existingImage;
			}
		}

		// create image for the tour type

		if (existingImage == null || existingImage.isDisposed()) {

			return createTourTypeImage(typeId, keyColorId, null);

		} else {

			// old tour type image is available and not disposed but needs to be updated

			return createTourTypeImage(typeId, keyColorId, existingImage);
		}
	}

	/**
	 * The image descriptor is cached because the creation takes system resources and it's called
	 * very often
	 * 
	 * @param tourTypeId
	 *            Tour type id
	 * @return Returns image descriptor for the tour type id
	 */
	public static ImageDescriptor getTourTypeImageDescriptor(final long tourTypeId) {

		final String keyColorId = TOUR_TYPE_PREFIX + tourTypeId;
		final ImageDescriptor existingDescriptor = _imageCacheDescriptor.get(keyColorId);

		if (existingDescriptor != null) {
			return existingDescriptor;
		}

		final Image tourTypeImage = getTourTypeImage(tourTypeId);
		final ImageDescriptor newImageDesc = ImageDescriptor.createFromImage(tourTypeImage);

		_imageCacheDescriptor.put(keyColorId, newImageDesc);

		return newImageDesc;
	}

	/**
	 * Set dirty state for all tour type images, images cannot be disposed because they are
	 * displayed in the UI
	 */
	public static void setTourTypeImagesDirty() {

		for (final String imageId : _imageCache.keySet()) {

			if (imageId.startsWith(TOUR_TYPE_PREFIX)) {
				_dirtyImages.put(imageId, true);
			}
		}

		_imageCacheDescriptor.clear();
	}

}
