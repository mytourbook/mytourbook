/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.PaintedMapPoint;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.photo.internal.Messages;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.PageBook;

/**
 * Slideout for all 2D map locations and marker
 */
public class MapPointToolTip_Photo extends AdvancedSlideout {

   private static final IDialogSettings _state                = TourbookPlugin.getState("net.tourbook.map2.view.MapPointToolTip_Photo"); //$NON-NLS-1$

   private static final int             DEFAULT_TEXT_WIDTH    = 50;

   private Map2                         _map2;

   private PaintedMapPoint              _hoveredMapPoint;
   private PaintedMapPoint              _previousHoveredMapPoint;
   private Photo                        _photo;

   private final ColorRegistry          _colorRegistry        = JFaceResources.getColorRegistry();
   private final Color                  _photoForegroundColor = _colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
   private final Color                  _photoBackgroundColor = _colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

   private final NumberFormat           _nfMByte              = NumberFormat.getNumberInstance();
   {
      _nfMByte.setMinimumFractionDigits(3);
      _nfMByte.setMaximumFractionDigits(3);
      _nfMByte.setMinimumIntegerDigits(1);
   }

   private PixelConverter _pc;
   private int            _defaultTextWidthPixel;

   /*
    * UI controls
    */
   private PageBook         _pageBook;

   private Composite        _pageNoPhoto;
   private Composite        _pagePhoto;

   private Label            _labelError;
   private Label            _labelMessage;

   private PhotoImageCanvas _photoImageCanvas;

   private class PhotoImageCanvas extends ImageCanvas {

      public PhotoImageCanvas(final Composite parent, final int style) {
         super(parent, style);
      }

      @Override
      public boolean drawInvalidImage(final GC gc, final Rectangle clientArea) {

         return updateUI_ShowLoadingImage(gc, clientArea);
      }
   }

   private class PhotoImageLoaderCallback implements ILoadCallBack {

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         if (isUpdateUI) {
            onImageIsLoaded();
         }
      }
   }

   public MapPointToolTip_Photo(final Map2 map2) {

      super(map2, _state, null);

      _map2 = map2;

      setTitleText("Photo");

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   @Override
   public void close() {

      Map2PointManager.setMapLocationSlideout(null);

      super.close();
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI_Photo(parent);

      createUI_00_Tooltip(parent);

      UI.setChildColors(parent.getShell(), _photoForegroundColor, _photoBackgroundColor);

      updateUI_Photo(_hoveredMapPoint);
   }

   private void createUI_00_Tooltip(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

      _pagePhoto = createUI_10_Photo(_pageBook);
      _pageNoPhoto = createUI_90_NoPhoto(_pageBook);

      _pageBook.showPage(_pageNoPhoto);
   }

   private Composite createUI_10_Photo(final Composite parent) {

//      final boolean isImageFileAvailable = _photo.isImageFileAvailable();
//      final boolean isImageFileNotAvailable = isImageFileAvailable == false;
//      final boolean isLoadingError = _photo.isLoadingError() || isImageFileNotAvailable;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults()
//            .extendedMargins(5, 5, 5, 5)
//            .spacing(3, 1)
            .numColumns(1)
            .applyTo(container);
      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
//         {
//            /*
//             * Image file
//             */
//
//            if (isImageFileAvailable) {
//
//               // image file is available
//
//               final Composite containerHeader = new Composite(container, SWT.NONE);
//               GridDataFactory.fillDefaults().grab(true, false).applyTo(containerHeader);
//               GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 0).applyTo(containerHeader);
//               {
//                  // image filename
//                  Label label = new Label(containerHeader, SWT.NONE);
//                  GridDataFactory.fillDefaults().applyTo(label);
//                  label.setText(_photo.imageFileName);
//
//                  if (_photo.isImageSizeAvailable()) {
//
//                     // dimension
//                     label = new Label(containerHeader, SWT.NONE);
//                     GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(label);
//                     label.setText(_photo.getDimensionText());
//                  }
//               }
//
//               if (isLoadingError) {
//
//                  // draw image folder
//                  final Label label = new Label(containerHeader, SWT.NONE);
//                  GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
//                  label.setText(_photo.imagePathName);
//
//               } else {
//
//                  final PhotoImageMetadata metaData = _photo.getImageMetaDataRaw();
//                  if (metaData != null) {
//                     createUI_20_Metadata(container, metaData);
//                  }
//               }
//            }
//         }
//         {
//            /*
//             * Loading error
//             */
//            if (isLoadingError) {
//
//               _labelError = new Label(container, SWT.WRAP);
//               GridDataFactory.fillDefaults()
//                     .indent(0, 5)
////                .hint(DEFAULT_TEXT_WIDTH, SWT.DEFAULT)
//                     .applyTo(_labelError);
//
//               _labelError.setText(isImageFileNotAvailable ? NLS.bind(
//                     Messages.Pic_Dir_Label_ImageLoadingFailed_FileNotAvailable,
//                     _photo.imageFilePathName) : Messages.Pic_Dir_Label_ImageLoadingFailed);
//            }
//         }
         {
            /*
             * Photo
             */
            _photoImageCanvas = new PhotoImageCanvas(container, SWT.DOUBLE_BUFFERED);
            _photoImageCanvas.setIsSmoothImages(true);
            _photoImageCanvas.setStyle(SWT.CENTER);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_photoImageCanvas);
         }
      }

      return container;
   }

   private void createUI_20_Metadata(final Composite parent, final PhotoImageMetadata metaData) {

      final LocalDateTime exifDateTime = _photo.getExifDateTime();
      final LocalDateTime imageFileDateTime = _photo.getImageFileDateTime();

      final boolean isTitle = metaData.objectName != null;
      final boolean isDescription = metaData.captionAbstract != null;
      final boolean isModel = metaData.model != null;
      final boolean isExifDate = exifDateTime != null;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(5, 1).numColumns(2).applyTo(container);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         boolean isDrawFileDate = true;

//       if (isDrawImage == false) {
         // draw image folder
         createUI_30_MetadataLine(container, Messages.Photo_ToolTip_ImagePath, _photo.imagePathName);
//       }

         if (isExifDate) {

            createUI_30_MetadataLine(container,
                  Messages.Photo_ToolTip_ExifDate,
                  exifDateTime.format(TimeTools.Formatter_Weekday)
                        + UI.SPACE2
                        + exifDateTime.format(TimeTools.Formatter_DateTime_M));

            // display modified date only when it differs from the exif/original date

            final Duration duration = Duration.between(exifDateTime, imageFileDateTime);
            final long durationSeconds = duration.getSeconds();

            /*
             * sometimes the difference is 1 second but it does not make sense to display it
             */
            if (Math.abs(durationSeconds) <= 2) {
               isDrawFileDate = false;
            }

//          final LocalDateTime exifLocal = exifDateTime.toLocalDateTime();
//          final LocalDateTime fileLocal = imageFileDateTime.toLocalDateTime();
//          System.out.println("exif\t" + exifLocal);
//          System.out.println("file\t" + fileLocal);
//          System.out.println("diff\t" + durationSeconds + " s");
         }

         if (isDrawFileDate) {
            createUI_30_MetadataLine(container, //
                  Messages.Photo_ToolTip_FileDate,
                  imageFileDateTime.format(TimeTools.Formatter_Weekday)
                        + UI.SPACE2
                        + imageFileDateTime.format(TimeTools.Formatter_DateTime_M));
         }

         /*
          * size + cardinal direction
          */
         final double photoImageDirection = _photo.getImageDirection();
         final int degreeDirectionInt = (int) (photoImageDirection);
         final String imageDirection = photoImageDirection == Double.MIN_VALUE
               ? UI.EMPTY_STRING
               : UI.getCardinalDirectionText(degreeDirectionInt)
                     + UI.SPACE4
                     + (degreeDirectionInt + UI.SPACE + UI.SYMBOL_DEGREE);

         createUI_40_MetadataLine(container,
               Messages.Photo_ToolTip_Size,
               _nfMByte.format(_photo.imageFileSize / 1024.0 / 1024.0) + UI.SPACE2 + UI.UNIT_MBYTE,
               imageDirection);

         if (isTitle) {
            createUI_30_MetadataLine(container, Messages.Photo_ToolTip_Title, metaData.objectName);
         }

         if (isDescription) {
            createUI_30_MetadataLine(container, Messages.Photo_ToolTip_Description, metaData.captionAbstract);
         }

         if (isModel) {
            createUI_30_MetadataLine(container, Messages.Photo_ToolTip_Model, metaData.model);
         }
      }
   }

   private void createUI_30_MetadataLine(final Composite container, final String name, final String value) {

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
      label.setText(name);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

      /*
       * value
       */
      label = new Label(container, SWT.WRAP);
      label.setText(valueText);
      GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.BEGINNING)
            .hint(hintX, SWT.DEFAULT)
            .applyTo(label);
   }

   private void createUI_40_MetadataLine(final Composite container,
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
         label.setText(value);
         GridDataFactory.fillDefaults()
               .align(SWT.BEGINNING, SWT.BEGINNING)
               .hint(hintX, SWT.DEFAULT)
               .applyTo(label);

         /*
          * value 2
          */
         label = new Label(valueContainer, SWT.WRAP);
         label.setText(value2);
         GridDataFactory.fillDefaults()
               .align(SWT.END, SWT.BEGINNING)
               .grab(true, false)
               .applyTo(label);
      }
   }

   private Composite createUI_90_NoPhoto(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         _labelMessage = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults()
               .align(SWT.CENTER, SWT.CENTER)
               .grab(true, true)
               .applyTo(_labelMessage);
      }

      return container;
   }

   private Point fixupDisplayBounds(final Point tipSize, final Point location) {

      final int tipWidth = tipSize.x;
      final int tipHeight = tipSize.y;

      final Rectangle displayBounds = UI.getDisplayBounds(_map2, location);
      final Point tipRightBottom = new Point(location.x + tipWidth, location.y + tipHeight);

      final Rectangle photoBounds = _hoveredMapPoint.labelRectangle;

      final int photoWidth = photoBounds.width;
      final int photoHeight = photoBounds.height;

      final int photoLeft = photoBounds.x;
      final int photoRight = photoLeft + photoWidth;
      final int photoTop = photoBounds.y;

      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;
      final int mapPointDevY = mapPoint.geoPointDevY;

      final Rectangle mapBounds = _map2.getBounds();
      final Point mapDisplayPosition = _map2.toDisplay(mapBounds.x, mapBounds.y);

      final boolean isTooltipInDisplay = displayBounds.contains(location);
      final boolean isTTBottomRightInDisplay = displayBounds.contains(tipRightBottom);

      final int displayWidth = displayBounds.width;
      final int displayHeight = displayBounds.height;
      final int displayX = displayBounds.x;
      final int displayY = displayBounds.y;

      if ((isTooltipInDisplay && isTTBottomRightInDisplay) == false) {

         if (tipRightBottom.x > displayX + displayWidth) {

            location.x -= tipRightBottom.x - (displayX + displayWidth);

            // adjust x/y to not overlap the map point position

            if (photoTop > mapPointDevY) {

               location.y += photoHeight;

            } else {

               location.y -= photoHeight;
            }

         }

         if (tipRightBottom.y > displayY + displayHeight - photoHeight) {

            location.y -= tipRightBottom.y - (displayY + displayHeight);

            location.x = displayX + mapDisplayPosition.x + photoLeft - tipWidth;
         }

         if (location.x < displayX) {

            location.x = displayX + mapDisplayPosition.x + photoRight;
         }

         if (location.y < displayY) {

            location.y = displayY;

            location.x = displayX + mapDisplayPosition.x + photoLeft - tipWidth;
         }

         if (location.x < displayX) {

            location.x = displayX + mapDisplayPosition.x + photoRight;
         }
      }

      return location;
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
   protected Rectangle getParentBounds() {

      // ignore, is overwritten with getToolTipLocation()
      return null;
   }

   /**
    * @param photo
    * @param map
    * @param tile
    *
    * @return Returns the photo image or <code>null</code> when image is not loaded.
    */
   private Image getPhotoImage(final Photo photo) {

      Image photoImage = null;

      final ImageQuality requestedImageQuality = ImageQuality.HQ;

      // check if image has an loading error
      final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

      if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not yet loaded

         // check if image is in the cache
         photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

         if ((photoImage == null || photoImage.isDisposed())
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            final ILoadCallBack imageLoadCallback = new PhotoImageLoaderCallback();

            PhotoLoadManager.putImageInLoadingQueueHQ_Map(photo, requestedImageQuality, imageLoadCallback);
         }
      }

      return photoImage;
   }

   @Override
   public Point getToolTipLocation(final Point tooltipSize) {

      if (_hoveredMapPoint == null) {
         return null;
      }

      final int tooltipWidth = tooltipSize.x;
      final int tooltipHeight = tooltipSize.y;

      final Rectangle mapBounds = _map2.getBounds();
      final Point mapDisplayPosition = _map2.toDisplay(mapBounds.x, mapBounds.y);

      final Rectangle photoBounds = _hoveredMapPoint.labelRectangle;
      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;

      final int photoWidth = photoBounds.width;
      final int photoHeight = photoBounds.height;

      final int photoLeft = photoBounds.x;
      final int photoRight = photoLeft + photoWidth;
      final int photoTop = photoBounds.y;
      final int photoBottom = photoTop + photoHeight;

      final int mapPointDevX = mapPoint.geoPointDevX;
      final int mapPointDevY = mapPoint.geoPointDevY;

      // set top/left as default
      int devX = photoLeft - tooltipWidth;
      int devY = photoBottom - tooltipHeight;

      // adjust x/y to not overlap the map point position
      if (photoLeft > mapPointDevX) {
         devX = photoRight;
      }

      if (photoTop > mapPointDevY) {
         devY = photoTop;
      }

      // adjust to display position
      devX += mapDisplayPosition.x;
      devY += mapDisplayPosition.y;

      final Point location = new Point(devX, devY);
      final Point fixedDisplayBounds = fixupDisplayBounds(tooltipSize, location);

      return fixedDisplayBounds;
   }

   private void initUI_Photo(final Composite parent) {

      _pc = new PixelConverter(parent);
      _defaultTextWidthPixel = _pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH);
   }

   @Override
   protected void onFocus() {

   }

   private void onImageIsLoaded() {

      final PaintedMapPoint hoveredMapPoint = _hoveredMapPoint;

      Display.getDefault().asyncExec(() -> {

         if (_hoveredMapPoint != null) {

            updateUI_Photo(hoveredMapPoint);

         } else if (_previousHoveredMapPoint != null) {

            /*
             * This happens when an image is loading and the mouse has exited the tooltip -> paint
             * loaded image
             */

            updateUI_Photo(_previousHoveredMapPoint);
         }
      });
   }

   public void setupPhoto(final PaintedMapPoint hoveredMapPoint) {

      final boolean isVisible = isVisible();

      if (hoveredMapPoint == null) {

         if (isVisible) {

            if (_hoveredMapPoint != null) {
               _previousHoveredMapPoint = _hoveredMapPoint;
            }

            _hoveredMapPoint = null;

            hide();
         }

         return;
      }

      final boolean isOtherMapPoint = _hoveredMapPoint != hoveredMapPoint;

      if (isOtherMapPoint && isVisible) {

         hide();
      }

      if (_hoveredMapPoint != null) {
         _previousHoveredMapPoint = _hoveredMapPoint;
      }

      _hoveredMapPoint = hoveredMapPoint;

      doNotStopAnimation();
      showShell();

      updateUI_Photo(hoveredMapPoint);
   }

   private void updateUI_Photo(final PaintedMapPoint hoveredMapPoint) {

      if (hoveredMapPoint == null) {
         return;
      }

      if (_photoImageCanvas.isDisposed()) {
         return;
      }

      _photo = hoveredMapPoint.mapPoint.photo;

      final Image photoImage = getPhotoImage(_photo);

      _photoImageCanvas.setImage(photoImage, false);

      _pageBook.showPage(_pagePhoto);
   }

   private boolean updateUI_ShowLoadingImage(final GC gc, final Rectangle rectangle) {

      if (_hoveredMapPoint == null) {

         _labelMessage.setText(UI.EMPTY_STRING);

      } else {

         final Photo photo = _hoveredMapPoint.mapPoint.photo;

         final String photoText = "Loading " + photo.imageFilePathName;

         _labelMessage.setText(photoText);
      }

      _pageBook.showPage(_pageNoPhoto);

      return true;
   }

}
