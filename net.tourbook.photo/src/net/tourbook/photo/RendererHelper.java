/*******************************************************************************
 * Copyright (c) 2006-2008 Nicolas Richeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors :
 *    Nicolas Richeton (nicolas.richeton@gmail.com) - initial API and implementation
 *    Richard Michalsky - bug 195439
 *******************************************************************************/
package net.tourbook.photo;

import org.eclipse.swt.graphics.Point;

/**
 * <p>
 * Utility methods for Gallery item and group renderers
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 * @contributor Richard Michalsky
 */
public class RendererHelper {

	/**
	 * Compute image size for the canvas size.
	 * 
	 * @param photo
	 * @param imageHeight
	 * @param imageWidth
	 * @param imageCanvasWidth
	 * @param imageCanvasHeight
	 * @return
	 */
	public static Point getBestSize(final Photo photo,
									final int imageWidth,
									final int imageHeight,
									final int imageCanvasWidth,
									final int imageCanvasHeight) {

		final Point canvasSize = getCanvasSize(imageWidth, imageHeight, imageCanvasWidth, imageCanvasHeight);

		int imagePaintedWidth = canvasSize.x;
		int imagePaintedHeight = canvasSize.y;

		/*
		 * the photo image should not be displayed larger than the original photo even when the
		 * thumb image is larger, this can happen when image is resized
		 */

		final int photoImageWidth = photo.getAvailableImageWidth();
		final int photoImageHeight = photo.getAvailableImageHeight();

		if (photoImageWidth != Integer.MIN_VALUE) {

			// photo is loaded

			if (imagePaintedWidth > photoImageWidth || imagePaintedHeight > photoImageHeight) {

				imagePaintedWidth = photoImageWidth;
				imagePaintedHeight = photoImageHeight;
			}
		} else

		// photo is NOT loaded

		if (imageWidth != Integer.MIN_VALUE && (imagePaintedWidth > imageWidth || imagePaintedHeight > imageHeight)) {

			imagePaintedWidth = imageWidth;
			imagePaintedHeight = imageHeight;
		}

		return new Point(imagePaintedWidth, imagePaintedHeight);
	}

	/**
	 * Get best-fit size for an image drawn in an area of maxX, maxY
	 * 
	 * @param imageWidth
	 * @param imageHeight
	 * @param canvasWidth
	 * @param canvasHeight
	 * @return
	 */
	private static Point getCanvasSize(	final int imageWidth,
										final int imageHeight,
										final int canvasWidth,
										final int canvasHeight) {

		final double widthRatio = (double) imageWidth / (double) canvasWidth;
		final double heightRatio = (double) imageHeight / (double) canvasHeight;

		final double bestRatio = widthRatio > heightRatio ? widthRatio : heightRatio;

		final int newWidth = (int) (imageWidth / bestRatio);
		final int newHeight = (int) (imageHeight / bestRatio);

		return new Point(newWidth, newHeight);
	}
}
