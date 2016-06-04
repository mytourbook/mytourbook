/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal;

import java.text.NumberFormat;

import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.RendererHelper;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * created: 4.10.2012
 */
public class PhotoGalleryToolTip extends AnimatedToolTipShell {

	private static final int		DEFAULT_TEXT_WIDTH	= 50;

	private GalleryMT20				_gallery;

	private Photo					_photo;

	/**
	 * Contains gallery item which is currently hovered with the mouse or <code>null</code> when
	 * mouse is not hovering a gallery item.
	 */
	private GalleryMT20Item			_currentHoveredGalleryItem;

	private int						_imageWidth			= 0;
	private int						_imageHeight		= 0;

	private int						_imagePaintedWidth	= 0;
	private int						_imagePaintedHeight	= 0;

	private int						_galleryImageSize	= 120;

	private int						_defaultTextWidthPixel;

	private final DateTimeFormatter	_dtFormatter		= DateTimeFormat.forStyle("ML");	//$NON-NLS-1$
	private final DateTimeFormatter	_dtWeekday			= DateTimeFormat.forPattern("E");	//$NON-NLS-1$

	private final NumberFormat		_nfMByte			= NumberFormat.getNumberInstance();
	{
		_nfMByte.setMinimumFractionDigits(3);
		_nfMByte.setMaximumFractionDigits(3);
		_nfMByte.setMinimumIntegerDigits(1);
	}

	private PixelConverter			_pc;

	/*
	 * UI resources
	 */
	private Image					_photoImage;

	private Canvas					_canvas;
	private Composite				_canvasContainer;

	private Label					_labelError;

	public PhotoGalleryToolTip(final GalleryMT20 gallery) {

		super(gallery);

		_gallery = gallery;
	}

	@Override
	protected void beforeHideToolTip() {

		reset(false);
	}

	@Override
	protected boolean canShowToolTip() {
		return _photo != null;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		if (_photo == null) {
			return null;
		}

		_pc = new PixelConverter(parent);
		_defaultTextWidthPixel = _pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH);

		final Composite container = createUI(parent);

		// set colors for all controls
		updateUI_colors(parent);

		return container;
	}

	private Composite createUI(final Composite parent) {

//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		final boolean isImageFileNotAvailable = _photo.isImageFileAvailable() == false;
		final boolean isLoadingError = _photo.isLoadingError() || isImageFileNotAvailable;
		final boolean isDrawImage = _galleryImageSize < 160;

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(5, 5, 5, 5)
				.spacing(3, 1)
				.numColumns(1)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			if (isImageFileNotAvailable == false) {

				// image file is available

				final Composite containerHeader = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(containerHeader);
				GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 0).applyTo(containerHeader);
//				containerHeader.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
				{
					// image filename
					Label label = new Label(containerHeader, SWT.NONE);
					GridDataFactory.fillDefaults().applyTo(label);
					label.setText(_photo.imageFileName);

					if (_photo.isImageSizeAvailable()) {

						// dimension
						label = new Label(containerHeader, SWT.NONE);
						GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(label);
						label.setText(_photo.getDimensionText());
					}
				}

				if (isLoadingError) {

					// draw image folder
					final Label label = new Label(containerHeader, SWT.NONE);
					GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
					label.setText(_photo.imagePathName);

				} else {

					final PhotoImageMetadata metaData = _photo.getImageMetaDataRaw();
					if (metaData != null) {
						createUI_Metadata(container, metaData, isDrawImage);
					}
				}
			}

			/*
			 * label: loading error
			 */
			if (isLoadingError) {

				_labelError = new Label(container, SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.indent(0, 5)
//						.hint(DEFAULT_TEXT_WIDTH, SWT.DEFAULT)
						.applyTo(_labelError);

				_labelError.setText(isImageFileNotAvailable ? NLS.bind(
						Messages.Pic_Dir_Label_ImageLoadingFailed_FileNotAvailable,
						_photo.imageFilePathName) : Messages.Pic_Dir_Label_ImageLoadingFailed);
			}

			// display thumb image only when the gallery image is smaller than the default thumb size
//			if (_galleryImageSize < PhotoLoadManager.IMAGE_SIZE_THUMBNAIL) {
			if (isDrawImage) {
				createUI_PhotoImage(container);
			}
		}
		return container;
	}

	private void createUI_Metadata(final Composite parent, final PhotoImageMetadata metaData, final boolean isDrawImage) {

		final DateTime exifDateTime = _photo.getExifDateTime();
		final DateTime imageFileDateTime = _photo.getImageFileDateTime();

		final boolean isTitle = metaData.objectName != null;
		final boolean isDescription = metaData.captionAbstract != null;
		final boolean isModel = metaData.model != null;
		final boolean isExifDate = exifDateTime != null;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(5, 1).numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			boolean isDrawFileDate = true;

			if (isDrawImage == false) {
				// draw image folder
				createUI_MetadataLine(container, Messages.Photo_ToolTip_ImagePath, _photo.imagePathName);
			}

			if (isExifDate) {

				createUI_MetadataLine(container, //
						Messages.Photo_ToolTip_ExifDate,
						_dtWeekday.print(exifDateTime) + UI.SPACE2 + _dtFormatter.print(exifDateTime));

				// display modified date only when it differs from the exif/original date

				final Duration duration = new Duration(exifDateTime, imageFileDateTime);
				final long durationMills = duration.getMillis();

				/*
				 * sometimes the difference is 1 second but it does not make sense to display it
				 */
				if (Math.abs(durationMills) <= 2000) {
					isDrawFileDate = false;
				}

//				final LocalDateTime exifLocal = exifDateTime.toLocalDateTime();
//				final LocalDateTime fileLocal = imageFileDateTime.toLocalDateTime();
//				System.out.println("exif\t" + exifLocal);
//				System.out.println("file\t" + fileLocal);
//				System.out.println("\t" + durationMills + " ms");
//				// TODO remove SYSTEM.OUT.PRINTLN
			}

			if (isDrawFileDate) {
				createUI_MetadataLine(container, //
						Messages.Photo_ToolTip_FileDate,
						_dtWeekday.print(imageFileDateTime) + UI.SPACE2 + _dtFormatter.print(imageFileDateTime));
			}

			/*
			 * size + cardinal direction
			 */
			final double photoImageDirection = _photo.getImageDirection();
			final int degreeDirectionInt = (int) (photoImageDirection);
			final String imageDirection = photoImageDirection == Double.MIN_VALUE //
					? UI.EMPTY_STRING
					: UI.getCardinalDirectionText(degreeDirectionInt)
							+ UI.SPACE4
							+ (degreeDirectionInt + UI.SPACE + UI.SYMBOL_DEGREE);

			createUI_MetadataLine2(container, //
					Messages.Photo_ToolTip_Size,
					_nfMByte.format(_photo.imageFileSize / 1024.0 / 1024.0) + UI.SPACE2 + UI.UNIT_MBYTES,
					imageDirection);

			if (isTitle) {
				createUI_MetadataLine(container, Messages.Photo_ToolTip_Title, metaData.objectName);
			}

			if (isDescription) {
				createUI_MetadataLine(container, Messages.Photo_ToolTip_Description, metaData.captionAbstract);
			}

			if (isModel) {
				createUI_MetadataLine(container, Messages.Photo_ToolTip_Model, metaData.model);
			}
		}
	}

	private void createUI_MetadataLine(final Composite container, final String name, final String value) {

		/*
		 * use hint only when text is too large, otherwise it will displays the white space allways
		 */
		final String valueText = getMaxValueText(value);
		final int hintX = valueText.length() > DEFAULT_TEXT_WIDTH ? _defaultTextWidthPixel : SWT.DEFAULT;

		/*
		 * name
		 */
		Label label;
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
		label.setText(name);

		/*
		 * value
		 */
		label = new Label(container, SWT.WRAP);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.BEGINNING)
				.hint(hintX, SWT.DEFAULT)
				.applyTo(label);
		label.setText(valueText);
	}

	private void createUI_MetadataLine2(final Composite container,
										final String name,
										final String value,
										final String value2) {

		/*
		 * use hint only when text is too large, otherwise it will displays the white space allways
		 */
		final int hintX = value.length() > DEFAULT_TEXT_WIDTH ? _defaultTextWidthPixel : SWT.DEFAULT;

		/*
		 * name
		 */
		Label label;
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
		label.setText(name);

		final Composite valueContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(valueContainer);
		{
			/*
			 * value 1
			 */
			label = new Label(valueContainer, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.BEGINNING)
					.hint(hintX, SWT.DEFAULT)
					.applyTo(label);
			label.setText(value);

			/*
			 * value 2
			 */
			label = new Label(valueContainer, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.BEGINNING)
					.grab(true, false)
					.applyTo(label);
			label.setText(value2);
		}
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
					_photo,
					_imageWidth,
					_imageHeight,
					imageCanvasWidth,
					imageCanvasHeight);

			_imagePaintedWidth = bestSize.x;
			_imagePaintedHeight = bestSize.y;

//			final Point bestSize = RendererHelper.getCanvasSize(
//					_imageWidth,
//					_imageHeight,
//					imageCanvasWidth,
//					imageCanvasHeight);
//
//			_imagePaintedWidth = bestSize.x;
//			_imagePaintedHeight = bestSize.y;
//
//			final int photoImageWidth = _photo.getImageWidth();
//			final int photoImageHeight = _photo.getImageHeight();
//
//			/*
//			 * the photo image should not be displayed larger than the original photo even when the
//			 * thumb image is larger, this can happen when image is resized
//			 */
//			if (photoImageWidth != Integer.MIN_VALUE) {
//
//				// photo is loaded
//
//				if (_imagePaintedWidth > photoImageWidth || _imagePaintedHeight > photoImageHeight) {
//
//					_imagePaintedWidth = photoImageWidth;
//					_imagePaintedHeight = photoImageHeight;
//				}
//			} else if (_imagePaintedWidth > _imageWidth || _imagePaintedHeight > _imageHeight) {
//
//				_imagePaintedWidth = _imageWidth;
//				_imagePaintedHeight = _imageHeight;
//			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		_canvasContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(_canvasContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.extendedMargins(0, 0, 5, 5)
				.applyTo(_canvasContainer);
//		_canvasContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			_canvas = new Canvas(_canvasContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.CENTER, SWT.CENTER)
					.hint(_imagePaintedWidth, _imagePaintedHeight)
//					.indent(0, 5)
					.applyTo(_canvas);

			_canvas.addPaintListener(new PaintListener() {

				@Override
				public void paintControl(final PaintEvent event) {
					onPaintImage(event);
				}
			});
		}
	}

	private String getMaxValueText(final String value) {

		/*
		 * use hint only when text is too large, otherwise it will displays the white space allways
		 */
		final boolean isLargeText = value.length() > DEFAULT_TEXT_WIDTH;
		if (isLargeText) {

			// ensure the text is not longer than 5 lines, this should fix bug #82

			final int maxText = Math.min(value.length(), DEFAULT_TEXT_WIDTH * 5);

			return value.substring(0, maxText);
		}

		return value;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		if (_currentHoveredGalleryItem.imagePaintedWidth == -1) {

			// image is not yet painted, show at the bottom of the gallery item
		}

		final int margin = 0;//10;

		final int itemPosX = _currentHoveredGalleryItem.viewPortX;
		final int itemPosY = _currentHoveredGalleryItem.viewPortY;
		final int itemWidth = _currentHoveredGalleryItem.width;
		final int itemHeight = _currentHoveredGalleryItem.height;

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		final int itemWidth2 = itemWidth / 2;
		final int tipWidth2 = tipWidth / 2;

		// center tooltip horizontally

		final int ttPosX = itemPosX + itemWidth2 - tipWidth2;

		int ttPosY = itemPosY + itemHeight + margin;

		// check gallery bottom
		final int galleryHeight = _gallery.getBounds().height;
		if (ttPosY > galleryHeight) {
			// tooltip is below the gallery bottom
			ttPosY = galleryHeight + margin;
		}

		// check display height
		final Rectangle displayBounds = _gallery.getDisplay().getBounds();
//		final Point ttDisplay = _gallery.toDisplay(ttPosX, ttPosY + tipHeight);
		final Point galleryDisplay = _gallery.toDisplay(0, 0);

		if (galleryDisplay.y + ttPosY + tipHeight > displayBounds.height) {
			ttPosY = itemPosY - tipHeight - margin;
		}

		// check display top
		final int aboveGallery = -tipHeight - margin;
		if (ttPosY < aboveGallery) {
			ttPosY = aboveGallery;
		}

		return _gallery.toDisplay(ttPosX, ttPosY);
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

		_gallery.onMouseMoveExternal(mouseEvent);
	}

	private void onPaintImage(final PaintEvent event) {

		final GC gc = event.gc;

		try {

//			final Rectangle clipRect = gc.getClipping();
//
//			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
//			gc.fillRectangle(clipRect);

			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);

			gc.drawImage(_photoImage, //
					0,
					0,
					_imageWidth,
					_imageHeight,
					0,
					0,
					_imagePaintedWidth,
					_imagePaintedHeight);

		} catch (final Exception e) {
			// this bug is covered here: https://bugs.eclipse.org/bugs/show_bug.cgi?id=375845
		}
	}

	public void reset(final boolean isHide) {

		_currentHoveredGalleryItem = null;
		_photo = null;

		if (isHide) {
			hide();
		}
	}

	public void setGalleryImageSize(final int photoImageSize) {
		_galleryImageSize = photoImageSize;
	}

	public void show(final GalleryMT20Item hoveredItem) {

		if (hoveredItem == _currentHoveredGalleryItem) {
			// nothing has changed
			return;
		}

//		System.out.println(UI.timeStampNano() + " show " + hoveredItem + "   " + _currentHoveredGalleryItem);
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (hoveredItem == null) {

			// hide tooltip

			reset(true);

		} else {

			// another item is hovered, show tooltip

			if (_currentHoveredGalleryItem != null) {
				reset(true);
			}

			_photo = hoveredItem.photo;

			if (_photo == null) {

				reset(true);

			} else {

				_currentHoveredGalleryItem = hoveredItem;

				showToolTip();
			}
		}
	}

	private void updateUI_colors(final Control child) {

		final Color bgColor = _gallery.getBackground();
		final Color fgColor = _gallery.getForeground();

		UI.setColorForAllChildren(child, fgColor, bgColor);

		if (_labelError != null && _labelError.isDisposed() == false) {
//			_labelError.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			_labelError.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		}

// set image preview background
//		_canvas.setBackground(_gallery.getBackground());
//		_canvasContainer.setBackground(_gallery.getBackground());

	}



}
