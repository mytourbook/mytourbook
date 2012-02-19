/*******************************************************************************
 * Copyright (c) 2006-2007 Nicolas Richeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors :
 *    Nicolas Richeton (nicolas.richeton@gmail.com) - initial API and implementation
 *******************************************************************************/
package net.tourbook.photo.gallery;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * <p>
 * Base class used to implement a custom gallery item renderer.
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 */
public abstract class AbstractGalleryItemRenderer {

	/**
	 * Id for decorators located at the bottom right of the item image Example : item.setData(
	 * AbstractGalleryItemRenderer.OVERLAY_BOTTOM_RIGHT , <Image or Image[]> );
	 */
	public final static String		OVERLAY_BOTTOM_RIGHT	= "org.eclipse.nebula.widget.gallery.bottomRightOverlay";	//$NON-NLS-1$

	/**
	 * Id for decorators located at the bottom left of the item image Example : item.setData(
	 * AbstractGalleryItemRenderer.OVERLAY_BOTTOM_RIGHT , <Image or Image[]> );
	 */
	public final static String		OVERLAY_BOTTOM_LEFT		= "org.eclipse.nebula.widget.gallery.bottomLeftOverlay";	//$NON-NLS-1$

	/**
	 * Id for decorators located at the top right of the item image Example : item.setData(
	 * AbstractGalleryItemRenderer.OVERLAY_BOTTOM_RIGHT , <Image or Image[]> );
	 */
	public final static String		OVERLAY_TOP_RIGHT		= "org.eclipse.nebula.widget.gallery.topRightOverlay";		//$NON-NLS-1$

	/**
	 * Id for decorators located at the top left of the item image Example : item.setData(
	 * AbstractGalleryItemRenderer.OVERLAY_BOTTOM_RIGHT , <Image or Image[]> );
	 */
	public final static String		OVERLAY_TOP_LEFT		= "org.eclipse.nebula.widget.gallery.topLeftOverlay";		//$NON-NLS-1$

	protected static final String	EMPTY_STRING			= "";														//$NON-NLS-1$

	protected GalleryMT				gallery;

	Color							galleryBackgroundColor, galleryForegroundColor;

	protected boolean				selected;

	public abstract void dispose();

	/**
	 * Draws an item.
	 * 
	 * @param gc
	 * @param item
	 * @param index
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public abstract void draw(GC gc, GalleryMTItem item, int index, int x, int y, int width, int height);

	/**
	 * Draw image overlays. Overlays are defined with image.setData using the following keys :
	 * <ul>
	 * <li>org.eclipse.nebula.widget.gallery.bottomLeftOverlay</li>
	 * <li>org.eclipse.nebula.widget.gallery.bottomRightOverlay</li>
	 * <li>org.eclipse.nebula.widget.gallery.topLeftOverlay</li>
	 * <li>org.eclipse.nebula.widget.gallery.topRightOverlay</li>
	 * </ul>
	 */
	protected void drawAllOverlays(final GC gc, final GalleryMTItem item, final int x, final int y, final Point imageSize, final int xShift, final int yShift) {
		final Image[] imagesBottomLeft = getImageOverlay(item, OVERLAY_BOTTOM_LEFT);
		final Image[] imagesBottomRight = getImageOverlay(item, OVERLAY_BOTTOM_RIGHT);
		final Image[] imagesTopLeft = getImageOverlay(item, OVERLAY_TOP_LEFT);
		final Image[] imagesTopRight = getImageOverlay(item, OVERLAY_TOP_RIGHT);

		final Point overlaySizeBottomLeft = getOverlaySize(imagesBottomLeft);
		final Point overlaySizeBottomRight = getOverlaySize(imagesBottomRight);
		final Point overlaySizeTopLeft = getOverlaySize(imagesTopLeft);
		final Point overlaySizeTopRight = getOverlaySize(imagesTopRight);

		final double ratio = getOverlayRatio(
				imageSize,
				overlaySizeTopLeft,
				overlaySizeTopRight,
				overlaySizeBottomLeft,
				overlaySizeBottomRight);

		drawOverlayImages(gc, x + xShift, y + yShift, ratio, imagesTopLeft);

		drawOverlayImages(
				gc,
				(int) (x + xShift + imageSize.x - overlaySizeTopRight.x * ratio),
				y + yShift,
				ratio,
				imagesTopRight);

		drawOverlayImages(
				gc,
				x + xShift,
				(int) (y + yShift + imageSize.y - overlaySizeBottomLeft.y * ratio),
				ratio,
				imagesBottomLeft);

		drawOverlayImages(gc, (int) (x + xShift + imageSize.x - overlaySizeBottomRight.x * ratio), (int) (y
				+ yShift
				+ imageSize.y - overlaySizeBottomRight.y * ratio), ratio, imagesBottomRight);
	}

	/**
	 * Draw overlay images for one corner.
	 * 
	 * @param gc
	 * @param x
	 * @param y
	 * @param ratio
	 * @param images
	 */
	protected void drawOverlayImages(final GC gc, final int x, final int y, final double ratio, final Image[] images) {
		if (images == null) {
			return;
		}

		int position = 0;
		for (final Image img : images) {
			gc.drawImage(
					img,
					0,
					0,
					img.getBounds().width,
					img.getBounds().height,
					x + position,
					y,
					(int) (img.getBounds().width * ratio),
					(int) (img.getBounds().height * ratio));

			position += img.getBounds().width * ratio;
		}
	}

	/**
	 * Check the GalleryItem, Gallery, and Display in order for the active background color for the
	 * given GalleryItem.
	 * 
	 * @param item
	 * @return the background Color to use for this item
	 */
	protected Color getBackground(final GalleryMTItem item) {
		Color backgroundColor = item.background;

		if (backgroundColor == null) {
			backgroundColor = item.getGallery().getBackground();
		}

		return backgroundColor;
	}

	/**
	 * Check the GalleryItem, Gallery, and Display in order for the active font for the given
	 * GalleryItem.
	 * 
	 * @param item
	 * @return the Font to use for this item
	 */
	protected Font getFont(final GalleryMTItem item) {
		Font font = item.getFont(true);
		if (font == null) {
			font = item.getGallery().getFont();
		}
		return font;
	}

	/**
	 * Check the GalleryItem, Gallery, and Display in order for the active foreground color for the
	 * given GalleryItem.
	 * 
	 * @param item
	 * @return the foreground Color to use for this item
	 */
	protected Color getForeground(final GalleryMTItem item) {
		Color foregroundColor = item.getForeground(true);
		if (foregroundColor == null) {
			foregroundColor = item.getGallery().getForeground();
		}

		return foregroundColor;
	}

	/**
	 * Get current gallery.
	 * 
	 * @return
	 */
	public GalleryMT getGallery() {
		return gallery;
	}

	/**
	 * Returns an array of images or null of no overlay was defined for this image.
	 * 
	 * @param item
	 * @param id
	 * @return Image[] or null
	 */
	protected Image[] getImageOverlay(final GalleryMTItem item, final String id) {
		final Object data = item.getData(id);

		if (data == null) {
			return null;
		}

		Image[] result = null;
		if (data instanceof Image) {
			result = new Image[1];
			result[0] = (Image) data;
		}

		if (data instanceof Image[]) {
			result = (Image[]) data;
		}
		return result;
	}

	/**
	 * Returns the best size ratio for overlay images. This ensure that all images can fit without
	 * being drawn on top of others.
	 * 
	 * @param imageSize
	 * @param overlaySizeTopLeft
	 * @param overlaySizeTopRight
	 * @param overlaySizeBottomLeft
	 * @param overlaySizeBottomRight
	 * @return
	 */
	protected double getOverlayRatio(	final Point imageSize,
										final Point overlaySizeTopLeft,
										final Point overlaySizeTopRight,
										final Point overlaySizeBottomLeft,
										final Point overlaySizeBottomRight) {

		double ratio = 1;

		if (overlaySizeTopLeft.x + overlaySizeTopRight.x > imageSize.x) {
			ratio = Math.min(ratio, (double) imageSize.x / (overlaySizeTopLeft.x + overlaySizeTopRight.x));
		}

		if (overlaySizeBottomLeft.x + overlaySizeBottomRight.x > imageSize.x) {
			ratio = Math.min(ratio, (double) imageSize.x / (overlaySizeBottomLeft.x + overlaySizeBottomRight.x));
		}

		if (overlaySizeTopLeft.y + overlaySizeBottomLeft.y > imageSize.y) {
			ratio = Math.min(ratio, (double) imageSize.y / (overlaySizeTopLeft.y + overlaySizeBottomLeft.y));
		}

		if (overlaySizeTopRight.y + overlaySizeBottomRight.y > imageSize.y) {
			ratio = Math.min(ratio, (double) imageSize.y / (overlaySizeTopRight.y + overlaySizeBottomRight.y));
		}

		return ratio;
	}

	/**
	 * Return overlay size, summing all images sizes
	 * 
	 * @param images
	 * @return
	 */
	protected Point getOverlaySize(final Image[] images) {

		if (images == null) {
			return new Point(0, 0);
		}

		final Point result = new Point(0, 0);
		for (final Image image : images) {
			result.x += image.getBounds().width;
			result.y = Math.max(result.y, image.getBounds().height);
		}

		return result;
	}

	/**
	 * true is the current item is selected
	 * 
	 * @return
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * This method is called after drawing the last item. It may be used to cleanup and release
	 * resources created in preDraw().
	 * 
	 * @param gc
	 */
	public void postDraw(final GC gc) {
		galleryForegroundColor = null;
		galleryBackgroundColor = null;
	}

	/**
	 * This method is called before drawing the first item. It may be used to calculate some values
	 * (like font metrics) that will be used for each item.
	 * 
	 * @param gc
	 */
	public void preDraw(final GC gc) {
		// Cache gallery color since this method is resource intensive.
		galleryForegroundColor = gallery.getForeground();
		galleryBackgroundColor = gallery.getBackground();
	}

	/**
	 * Set the current gallery. This method is automatically called by
	 * {@link GalleryMT#setItemRenderer(AbstractGalleryItemRenderer)}. There is not need to call it
	 * from user code.
	 * 
	 * @param gallery
	 */
	public void setGallery(final GalleryMT gallery) {
		this.gallery = gallery;
	}

	public void setSelected(final boolean selected) {
		this.selected = selected;
	}

}
