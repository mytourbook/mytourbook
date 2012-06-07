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
package net.tourbook.photo.gallery.MT20;

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
	 * Get best-fit size for an image drawn in an area of maxX, maxY
	 * 
	 * @param imageWidth
	 * @param imageHeight
	 * @param canvasWidth
	 * @param canvasHeight
	 * @return
	 */
	public static Point getCanvasSize(	final int imageWidth,
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
