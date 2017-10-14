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

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.util.ImageConverter;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

/**
 * Tour type images are painted with AWT because it has better antialiasing with transparency than
 * SWT.
 */
public class TourTypeImage {

	private static final String								TOUR_TYPE_PREFIX		= "tourType";						//$NON-NLS-1$

// SET_FORMATTING_OFF
	
	private final static HashMap<String, Image>				_imageCache				= new HashMap<String, Image>();
	private final static HashMap<String, ImageDescriptor>	_imageCacheDescriptor	= new HashMap<String, ImageDescriptor>();
	private final static HashMap<String, Boolean>			_dirtyImages			= new HashMap<String, Boolean>();
	
// SET_FORMATTING_ON

	private static Image createTourTypeImage(final long typeId, final String colorId, final Image existingImage) {

		final Image swtTourTypeImage = createTourTypeImage_Create(typeId, existingImage);

		// keep image in cache
		_imageCache.put(colorId, swtTourTypeImage);

		return swtTourTypeImage;
	}

	private static Image createTourTypeImage_Create(final long typeId, final Image existingImageSWT) {

		final int imageSize = TourType.TOUR_TYPE_IMAGE_SIZE;

		final BufferedImage awtImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_4BYTE_ABGR);

		final Graphics2D g2d = awtImage.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, 100);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		try {

			drawTourTypeImage(typeId, g2d);

			final Image newImageSWT = ImageConverter.convertIntoSWT(awtImage);

			if (existingImageSWT == null) {

				return newImageSWT;

			} else {

				// draw into existing image

				final GC gc = new GC(existingImageSWT);
				{
					gc.drawImage(newImageSWT, 0, 0);
				}
				gc.dispose();

				return existingImageSWT;
			}

		} finally {
			g2d.dispose();
		}
	}

	/**
	 * Dispose images
	 */
	public static void dispose() {

		for (final Image image : _imageCache.values()) {
			image.dispose();
		}

		_imageCache.clear();
		_imageCacheDescriptor.clear();
	}

	private static void drawTourTypeImage(final long typeId, final Graphics2D g2d) {

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
		final DrawingColorsAWT drawingColors = getTourTypeColors(typeId);

		drawTourTypeImage_Background(g2d, imageLayout, borderWidth, imageSize, drawingColors);

		if (borderWidth > 0) {
			drawTourTypeImage_Border(g2d, borderLayout, borderWidth, imageSize, drawingColors);
		}
	}

	private static void drawTourTypeImage_Background(	final Graphics2D g2d,
														final TourTypeLayout imageLayout,
														final int borderWidth,
														final int imageSize,
														final DrawingColorsAWT drawingColors) {

		final java.awt.Color colorBright = drawingColors.colorBright;
		final java.awt.Color colorDark = drawingColors.colorDark;
		final java.awt.Color colorLine = drawingColors.colorLine;

		final int imageSize2 = imageSize / 2;

		boolean isRectangle = false;
		boolean isOval = false;

		boolean isBrightColor = false;
		boolean isDarkColor = false;
		boolean isLineColor = false;

		switch (imageLayout) {

		case FILL_RECT_BRIGHT:
			isRectangle = true;
			isBrightColor = true;
			break;
		case FILL_RECT_DARK:
			isRectangle = true;
			isDarkColor = true;
			break;
		case FILL_RECT_BORDER:
			isRectangle = true;
			isLineColor = true;
			break;

		case FILL_CIRCLE_BRIGHT:
			isOval = true;
			isBrightColor = true;
			break;
		case FILL_CIRCLE_DARK:
			isOval = true;
			isDarkColor = true;
			break;
		case FILL_CIRCLE_BORDER:
			isOval = true;
			isLineColor = true;
			break;

		case GRADIENT_LEFT_RIGHT:
			isRectangle = true;
			g2d.setPaint(new GradientPaint(0, imageSize2, colorBright, imageSize, imageSize2, colorDark));
			break;
		case GRADIENT_RIGHT_LEFT:
			isRectangle = true;
			g2d.setPaint(new GradientPaint(0, imageSize2, colorDark, imageSize, imageSize2, colorBright));
			break;
		case GRADIENT_TOP_BOTTOM:
			isRectangle = true;
			g2d.setPaint(new GradientPaint(imageSize2, 0, colorBright, imageSize2, imageSize, colorDark));
			break;
		case GRADIENT_BOTTOM_TOP:
			isRectangle = true;
			g2d.setPaint(new GradientPaint(imageSize2, 0, colorDark, imageSize2, imageSize, colorBright));
			break;

		case NOTHING:
		default:
			break;
		}

		if (isBrightColor) {

			g2d.setColor(colorBright);

		} else if (isDarkColor) {

			g2d.setColor(colorDark);

		} else if (isLineColor) {

			g2d.setColor(colorLine);
		}

		if (isRectangle) {

			g2d.fillRect(0, 0, imageSize, imageSize);

		} else if (isOval) {

			g2d.fillOval(0, 0, imageSize, imageSize);
		}
	}

	private static void drawTourTypeImage_Border(	final Graphics2D g2d,
													final TourTypeBorder borderLayout,
													final int borderWidth,
													final int imageSize,
													final DrawingColorsAWT drawingColors) {

		final java.awt.Color colorLine = drawingColors.colorLine;

		boolean isCircle = false;

		boolean isLeft = false;
		boolean isRight = false;
		boolean isTop = false;
		boolean isBottom = false;

		switch (borderLayout) {

		case BORDER_RECTANGLE:
			isLeft = true;
			isRight = true;
			isTop = true;
			isBottom = true;
			break;

		case BORDER_CIRCLE:
			isCircle = true;
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

		case BORDER_TOP:
			isTop = true;
			break;
		case BORDER_BOTTOM:
			isBottom = true;
			break;
		case BORDER_TOP_BOTTOM:
			isTop = true;
			isBottom = true;
			break;

		default:
			break;
		}

		if (isCircle) {

			g2d.setStroke(new BasicStroke(borderWidth));
			g2d.setColor(colorLine);

			/*
			 * Highly complicated formula that the oval outline has the correct size, needed some
			 * experimenting time to get it.
			 */
			final float ovalPos = (borderWidth / 2f) + 0.5f;
			final int ovalPosInt = (int) ovalPos;
			final float ovalSize = imageSize - ovalPosInt - borderWidth / 2f;
			final int ovalSizeInt = (int) ovalSize;

			g2d.drawOval(//
					ovalPosInt,
					ovalPosInt,
					ovalSizeInt,
					ovalSizeInt);

		} else if (isLeft || isRight || isTop || isBottom) {

			g2d.setColor(colorLine);

			if (isLeft) {

				g2d.fillRect(0, 0, borderWidth, imageSize);
			}

			if (isRight) {

				g2d.fillRect(imageSize - borderWidth, 0, borderWidth, imageSize);
			}

			if (isTop) {

				g2d.fillRect(0, 0, imageSize, borderWidth);
			}

			if (isBottom) {

				g2d.fillRect(0, imageSize - borderWidth, imageSize, borderWidth);
			}
		}

	}

	/**
	 * @param graphColor
	 * @return return the color for the graph
	 */
	private static DrawingColorsAWT getTourTypeColors(final long tourTypeId) {

		final DrawingColorsAWT drawingColors = new DrawingColorsAWT();
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

			drawingColors.colorBright = java.awt.Color.YELLOW;
			drawingColors.colorDark = java.awt.Color.PINK;
			drawingColors.colorLine = java.awt.Color.DARK_GRAY;

		} else {

			final RGB rgbBright = colorTourType.getRGBBright();
			final RGB rgbDark = colorTourType.getRGBDark();
			final RGB rgbLine = colorTourType.getRGBLine();

			drawingColors.colorBright = new java.awt.Color(rgbBright.red, rgbBright.green, rgbBright.blue);
			drawingColors.colorDark = new java.awt.Color(rgbDark.red, rgbDark.green, rgbDark.blue);
			drawingColors.colorLine = new java.awt.Color(rgbLine.red, rgbLine.green, rgbLine.blue);
		}

		return drawingColors;
	}

	/**
	 * @param typeId
	 * @return Returns an image which represents the tour type. This image must not be disposed,
	 *         this is done when the app closes.
	 */
	public static Image getTourTypeImage(final long typeId) {

		return getTourTypeImage(typeId, false);
	}

	public static Image getTourTypeImage(final long typeId, final boolean canDisposeOldImage) {

		final String keyColorId = TOUR_TYPE_PREFIX + typeId;
		final Image existingImage = _imageCache.get(keyColorId);

		// check if image is available
		if (existingImage != null && existingImage.isDisposed() == false) {

			// check if the image is dirty
			if (_dirtyImages.containsKey(keyColorId) == false) {

				// image is available and not dirty
				return existingImage;

			} else {

				// image is available and dirty

				if (canDisposeOldImage) {

					existingImage.dispose();
				}
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
