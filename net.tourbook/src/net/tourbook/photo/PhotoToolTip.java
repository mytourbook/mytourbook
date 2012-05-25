/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import net.tourbook.photo.gallery.RendererHelper;
import net.tourbook.photo.gallery.MT20.GalleryMT20;
import net.tourbook.photo.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.manager.ImageQuality;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoLoadManager;
import net.tourbook.photo.manager.PhotoWrapper;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.ToolTip;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

public class PhotoToolTip extends ToolTip {

	private GalleryMT20		_gallery;

	private PhotoWrapper	_photoWrapper;
	private Photo			_photo;

	/**
	 * Contains gallery item which is currently hovered with the mouse or <code>null</code> when
	 * mouse is not hovering a gallery item.
	 */
	private GalleryMT20Item	_currentHoveredGalleryItem;

	/*
	 * UI resources
	 */
	private Color			_bgColor;
	private Color			_fgColor;
//	private Font			_boldFont;

	private Image			_photoImage;
	private int				_centerOffsetX		= 0;
	private int				_centerOffsetY		= 0;

	private int				_imageWidth			= 0;
	private int				_imageHeight		= 0;

	private int				_imagePaintedWidth	= 0;
	private int				_imagePaintedHeight	= 0;

	public PhotoToolTip(final GalleryMT20 control) {

		super(control, NO_RECREATE, false);

		_gallery = control;

		initUI(control);
	}

	@Override
	protected void afterHideToolTip(final Event event) {

//		if (_photoImage != null) {
//			_photoImage.dispose();
//		}
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(3, 3).numColumns(1).applyTo(container);
		{
			// image filename
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(_photoWrapper.imageFileName);

			// dimension
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(_photo.getWidth() + " x " + _photo.getHeight());

			createUI_PhotoImage(container);
		}

		/*
		 * set colors for all controls
		 */
		updateUI_colors(parent);

		return container;
	}

	/**
	 * Photo image will only be displayed when image is loaded and available in the image cache.
	 * 
	 * @param parent
	 */
	private void createUI_PhotoImage(final Composite parent) {

		// check if image is in the cache
		_photoImage = PhotoImageCache.getImage(_photo, ImageQuality.THUMB);

		if ((_photoImage == null || _photoImage.isDisposed())) {

			// the requested image is not available in the image cache -> image must be loaded

			_photoImage = PhotoImageCache.getImage(_photo, ImageQuality.HQ);

			if ((_photoImage == null || _photoImage.isDisposed())) {

				// the requested image is not available in the image cache
				return;
			}
		}

		/*
		 * an exception can occure because the image could be disposed before it is drawn
		 */
		try {

			final Rectangle imageBounds = _photoImage.getBounds();
			_imageWidth = imageBounds.width;
			_imageHeight = imageBounds.height;

			final int imageCanvasWidth = PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;
			final int imageCanvasHeight = (int) (PhotoLoadManager.IMAGE_SIZE_THUMBNAIL / _gallery.getItemRatio());

			final Point bestSize = RendererHelper.getBestSize(
					_imageWidth,
					_imageHeight,
					imageCanvasWidth,
					imageCanvasHeight);

			_imagePaintedWidth = bestSize.x;
			_imagePaintedHeight = bestSize.y;

			final int photoWidthRotated = _photo.getWidthRotated();
			final int photoHeightRotated = _photo.getHeightRotated();

			/*
			 * the photo image should not be displayed larger than the original photo even when the
			 * thumb image is larger, this can happen when image is resized
			 */
			if (photoWidthRotated != Integer.MIN_VALUE && photoHeightRotated != Integer.MIN_VALUE) {

				// photo is loaded

				if (_imagePaintedWidth > photoWidthRotated || _imagePaintedHeight > photoHeightRotated) {

					_imagePaintedWidth = photoWidthRotated;
					_imagePaintedHeight = photoHeightRotated;
				}
			} else if (_imagePaintedWidth > _imageWidth || _imagePaintedHeight > _imageHeight) {

				_imagePaintedWidth = _imageWidth;
				_imagePaintedHeight = _imageHeight;
			}

			// Draw image
			if (_imagePaintedWidth > 0 && _imagePaintedHeight > 0) {

				// center image
				_centerOffsetX = (imageCanvasWidth - _imagePaintedWidth) / 2;
				_centerOffsetY = (imageCanvasHeight - _imagePaintedHeight) / 2;

			}
		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			final Canvas canvas = new Canvas(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.CENTER, SWT.CENTER)
					.hint(_imagePaintedWidth, _imagePaintedHeight)
					.applyTo(canvas);

			canvas.addPaintListener(new PaintListener() {

				@Override
				public void paintControl(final PaintEvent event) {
					try {
						event.gc.drawImage(_photoImage, //
								0,
								0,
								_imageWidth,
								_imageHeight,
//								_centerOffsetX,
//								_centerOffsetY,
								0,
								0,
								_imagePaintedWidth,
								_imagePaintedHeight);

					} catch (final Exception e) {
						// this bug is covered here: https://bugs.eclipse.org/bugs/show_bug.cgi?id=375845
					}
				}
			});
		}
	}

	private void initUI(final Control control) {

		final Display display = control.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
//		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

	}

	void reset() {
		_currentHoveredGalleryItem = null;
		_photoWrapper = null;
		_photo = null;
	}

	public void setGalleryItem(final GalleryMT20Item galleryItem) {

		if (galleryItem == null) {
			_photo = null;
			return;
		}
	}

	@Override
	protected boolean shouldCreateToolTip(final Event event) {

		if (super.shouldCreateToolTip(event) == false) {
			return false;
		}

		return _photo != null;
	}

	void show(final GalleryMT20Item hoveredItem) {

		if (hoveredItem == _currentHoveredGalleryItem) {
			// nothing has changed
			return;
		}

		if (hoveredItem == null) {

			// hide tooltip

			reset();

			hide();

		} else {

			// show tooltip

			_photoWrapper = (PhotoWrapper) hoveredItem.customData;

			if (_photoWrapper == null) {
				reset();
				return;
			}

			if (_currentHoveredGalleryItem != null) {
				hide();
			}

			_currentHoveredGalleryItem = hoveredItem;
			_photo = _photoWrapper.photo;

			final Point location = new Point(//
					hoveredItem.viewPortX,
					hoveredItem.viewPortY + hoveredItem.height + 0);

			show(location);
		}

	}

	private void updateUI_colors(final Control child) {

		child.setBackground(_bgColor);
		child.setForeground(_fgColor);

		if (child instanceof Composite) {
			final Control[] children = ((Composite) child).getChildren();
			for (final Control element : children) {
				updateUI_colors(element);
			}
		}
	}

}
