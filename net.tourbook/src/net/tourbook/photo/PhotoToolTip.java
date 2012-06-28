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

import java.text.NumberFormat;

import net.tourbook.photo.gallery.MT20.GalleryMT20;
import net.tourbook.photo.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.gallery.MT20.RendererHelper;
import net.tourbook.photo.manager.ImageQuality;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoImageMetadata;
import net.tourbook.photo.manager.PhotoLoadManager;
import net.tourbook.photo.manager.PhotoWrapper;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.ToolTip;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PhotoToolTip extends ToolTip {

	private static final int		DEFAULT_TEXT_WIDTH	= 50;

	private GalleryMT20				_gallery;

	private PhotoWrapper			_photoWrapper;
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

	/*
	 * UI resources
	 */
	private Color					_bgColor;
	private Color					_fgColor;
//	private Font					_boldFont;

	private Image					_photoImage;

	private Canvas					_canvas;
	private Composite				_canvasContainer;

	private Label					_labelError;

	public PhotoToolTip(final GalleryMT20 control) {

		super(control, NO_RECREATE, false);

		_gallery = control;

		initUI(control);
	}

	@Override
	protected void afterHideToolTip(final Event event) {
		reset();
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		if (_photo == null) {
			return null;
		}

		final Composite container = createUI(parent);

		/*
		 * set colors for all controls
		 */
		_bgColor = _gallery.getBackground();
		_fgColor = _gallery.getForeground();

		updateUI_colors(parent);

		return container;
	}

	private Composite createUI(final Composite parent) {

		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		final int imageWidth = _photo.getImageWidth();
		final boolean isImageLoaded = imageWidth != Integer.MIN_VALUE;

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(5, 5, 5, 5)
				.spacing(3, 1)
				.numColumns(1)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			final Composite containerHeader = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerHeader);
			GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 0).applyTo(containerHeader);
//			containerHeader.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
			{
				// image filename
				Label label = new Label(containerHeader, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText(_photoWrapper.imageFileName);

				if (isImageLoaded) {
					// dimension
					label = new Label(containerHeader, SWT.NONE);
					GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(label);
					label.setText(imageWidth + " x " + _photo.getImageHeight()); //$NON-NLS-1$
				}
			}

			final PhotoImageMetadata metaData = _photo.getImageMetaDataRaw();
			if (metaData != null) {
				createUI_Metadata(container, metaData);
			}

			/*
			 * label: loading error
			 */
			if (_photo.isLoadingError()) {

				_labelError = new Label(container, SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.indent(0, 5)
//						.hint(DEFAULT_TEXT_WIDTH, SWT.DEFAULT)
						.applyTo(_labelError);
				_labelError.setText(Messages.Pic_Dir_Label_ImageLoadingFailed);
			}

			// display thumb image only when the gallery image is smaller than the default thumb size
//			if (_galleryImageSize < PhotoLoadManager.IMAGE_SIZE_THUMBNAIL) {
			if (_galleryImageSize < 120) {
				createUI_PhotoImage(container);
			}
		}
		return container;
	}

	private void createUI_Metadata(final Composite parent, final PhotoImageMetadata metaData) {

		final DateTime exifDateTime = _photo.getExifDateTime();

		final boolean isTitle = metaData.objectName != null;
		final boolean isDescription = metaData.captionAbstract != null;
		final boolean isModel = metaData.model != null;
		final boolean isExifDate = exifDateTime != null;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(5, 1).numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			final DateTime imageFileDateTime = _photo.getImageFileDateTime();
			boolean isDrawModified = true;

			if (isExifDate) {

				createUI_MetadataLine(container, Messages.Photo_ToolTip_Date, _dtWeekday.print(exifDateTime)
						+ UI.SPACE2
						+ _dtFormatter.print(exifDateTime));

				// display modified date only when it differs from the exif/original date

				final Duration duration = new Duration(exifDateTime, imageFileDateTime);
				final long durationMills = duration.getMillis();

				/*
				 * sometimes the differenz is 1 second but it does not make sense to display it
				 */
				if (Math.abs(durationMills) <= 2000) {
					isDrawModified = false;
				}

//				final LocalDateTime exifLocal = exifDateTime.toLocalDateTime();
//				final LocalDateTime fileLocal = imageFileDateTime.toLocalDateTime();
//				System.out.println("exif\t" + exifLocal);
//				System.out.println("file\t" + fileLocal);
//				System.out.println("\t" + durationMills + " ms");
//				// TODO remove SYSTEM.OUT.PRINTLN
			}

			if (isDrawModified) {
				createUI_MetadataLine(container, //
						Messages.Photo_ToolTip_Modified,
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
							+ (degreeDirectionInt + UI.SPACE + net.tourbook.ui.UI.SYMBOL_DEGREE);

			createUI_MetadataLine2(container, //
					Messages.Photo_ToolTip_Size,
					_nfMByte.format(_photoWrapper.imageFileSize / 1024.0 / 1024.0) + UI.SPACE2 + UI.UNIT_MBYTES,
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
		final int hintX = value.length() > DEFAULT_TEXT_WIDTH ? _defaultTextWidthPixel : SWT.DEFAULT;

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
		label.setText(value);
//		label.pack();
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
		_canvasContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
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

	@Override
	public Point getLocation(final Point tipSize, final Event event) {

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

	private void initUI(final Control control) {

//		final Display display = control.getDisplay();
//
//		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
//		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_bgColor = _gallery.getBackground();
		_fgColor = _gallery.getForeground();

//		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		final PixelConverter pc = new PixelConverter(control);
		_defaultTextWidthPixel = pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH);
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

	void reset() {

		_currentHoveredGalleryItem = null;
		_photoWrapper = null;
		_photo = null;

		hide();
	}

	void setImageSize(final int photoImageSize) {
		_galleryImageSize = photoImageSize;
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

		} else {

			// another item is hovered, show tooltip

			if (_currentHoveredGalleryItem != null) {
				reset();
			}

			_photoWrapper = (PhotoWrapper) hoveredItem.customData;

			if (_photoWrapper == null) {

				reset();

			} else {

				_currentHoveredGalleryItem = hoveredItem;
				_photo = _photoWrapper.photo;

				final Point location = new Point(//
						hoveredItem.viewPortX,
						hoveredItem.viewPortY + hoveredItem.height);

				show(location);
			}
		}
	}

	private void updateUI_colors(final Control child) {

		child.setBackground(_bgColor);
		child.setForeground(_fgColor);

		if (child instanceof Composite) {
			final Control[] children = ((Composite) child).getChildren();
			for (final Control element : children) {

				if (element != null && element.isDisposed() == false) {
					updateUI_colors(element);
				}
			}
		}

		if (_labelError != null && _labelError.isDisposed() == false) {
			_labelError.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		}

// set image preview background
//		_canvas.setBackground(_gallery.getBackground());
//		_canvasContainer.setBackground(_gallery.getBackground());

	}

}
