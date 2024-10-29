/*******************************************************************************
 * Copyright 2019, 2024 Wolfgang Schramm and Contributors
 * Copyright 2019, 2021 Thomas Theussing
 * Copyright 2016-2018 devemux86
 * Copyright 2017 nebular
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
package net.tourbook.map25.photo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapImageSize;
import net.tourbook.map2.view.SlideoutMap2_PhotoOptions;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;
import net.tourbook.map25.layer.marker.MarkerConfig;
import net.tourbook.map25.layer.marker.MarkerShape;
import net.tourbook.map25.layer.marker.MarkerToolkit;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.internal.DPIUtil;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.marker.MarkerSymbol.HotspotPlace;

public class PhotoToolkit extends MarkerToolkit implements ItemizedLayer.OnItemGestureListener<MarkerInterface> {

   private IDialogSettings _state;

   private Map25App        _mapApp;

   private boolean         _isShowHQPhotoImages;
   private boolean         _isShowPhotos;
   private boolean         _isShowPhotoTitle;

   /**
    * This image is displayed when a photo is not yet loaded
    */
   private Bitmap          _notLoadedPhoto_Bitmap;
   private MarkerSymbol    _notLoadedPhoto_Symbol;

   private class LoadCallbackImage implements ILoadCallBack {

      private MarkerItem _markerItem;
      private Photo      _photo;

      public LoadCallbackImage(final MarkerItem markerItem, final Photo photo) {

         _markerItem = markerItem;
         _photo = photo;
      }

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         if (isUpdateUI == false) {
            return;
         }

         // create map bitmap from photo image
         createPhotoItems_10_SetBitmapFromPhoto(_markerItem, _photo);

         /**
          * EXTREEMLY IMPORTANT otherwise the photo size is not updated, it took me a while to fix
          * this issue !!!
          */
         _mapApp.updateLayer_PhotoLayer();

         _mapApp.updateMap();
      }
   }

   public PhotoToolkit(final Map25App map25App, final IDialogSettings state) {

      super(MarkerShape.CIRCLE);

      _mapApp = map25App;
      _state = state;

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      getFillPainter().setStyle(Paint.Style.FILL);

      _notLoadedPhoto_Bitmap = createShapeBitmap(MarkerShape.CIRCLE);
      _notLoadedPhoto_Symbol = new MarkerSymbol(_notLoadedPhoto_Bitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER, false);

      setIsMarkerClusteredLast(config.isMarkerClustered);
      setMarkerRenderer();
   }

   private GeoPoint createPhoto_Location(final Photo photo) {

      Double photoLat = 0.0;
      Double photoLon = 0.0;

      final PhotoImageMetadata imageMetaData = photo.getImageMetaData();

      if (photo.isGeoFromExif
            && Math.abs(imageMetaData.latitude) > 0
            && Math.abs(imageMetaData.longitude) > 0) {

         // photo contains valid (>0) GPS position in the EXIF

         photoLat = imageMetaData.latitude;
         photoLon = imageMetaData.longitude;

      } else {

         // using position via time marker

         photoLat = photo.getTourLatitude();
         photoLon = photo.getTourLongitude();
      }

      return new GeoPoint(photoLat, photoLon);
   }

   private String createPhoto_Name(final Photo photo) {

      final String photoName = TimeTools.getZonedDateTime(photo.imageExifTime).format(TimeTools.Formatter_Time_S)
            + UI.SPACE
            + createPhoto_Stars(photo);

      return photoName;
   }

   private String createPhoto_Stars(final Photo photo) {

      String starText = UI.EMPTY_STRING;

      switch (photo.ratingStars) {
      case 1:
         starText = "*"; //$NON-NLS-1$
         break;

      case 2:
         starText = "**"; //$NON-NLS-1$
         break;

      case 3:
         starText = "***"; //$NON-NLS-1$
         break;

      case 4:
         starText = "****"; //$NON-NLS-1$
         break;

      case 5:
         starText = "*****"; //$NON-NLS-1$
         break;
      }

      return starText;
   }

   /**
    * Creates a LIST with tourphotos, which can directly be added to the photoLayer via addItems
    *
    * @param allPhotos
    *
    * @return
    */
   public List<MarkerInterface> createPhotoItems(final List<Photo> allPhotos) {

      /**
       * [2.5D Map] Stopped implementation because it would need a lot of time to implement it
       * correctly. Currently it is working OK for a few images but not for too many images
       */

      System.out.println(UI.timeStamp() + " createPhotoItems: ");
// TODO remove SYSTEM.OUT.PRINTLN

      final List<MarkerInterface> allPhotoItems = new ArrayList<>();

      if (allPhotos == null || allPhotos.isEmpty()) {

         return allPhotoItems;
      }

      for (final Photo photo : allPhotos) {

// SET_FORMATTING_OFF

         final UUID photoKey           = UUID.randomUUID();
         final String photoName        = createPhoto_Name(photo);
         final String photoDescription = "Ratingstars: " + Integer.toString(photo.ratingStars); //$NON-NLS-1$
         final GeoPoint geoPoint       = createPhoto_Location(photo);

// SET_FORMATTING_ON

         final MarkerItem markerItem = new MarkerItem(
               photoKey,
               photoName, // time as name
               photoDescription, // rating stars as description
               geoPoint);

         createPhotoItems_10_SetBitmapFromPhoto(markerItem, photo);

         allPhotoItems.add(markerItem);
      }

      return allPhotoItems;
   }

   /**
    * The photo bitmap is set into the {@link MarkerItem}
    *
    * @param markerItem
    * @param photo
    */
   private void createPhotoItems_10_SetBitmapFromPhoto(final MarkerItem markerItem,
                                                       final Photo photo) {

      Bitmap bitmapImage = createPhotoItems_20_CreateBitmap(markerItem, photo);

      if (bitmapImage == null) {
         bitmapImage = _notLoadedPhoto_Bitmap;
      }

      final MarkerSymbol markerSymbol = createPhotoMapItem(markerItem, bitmapImage);

      markerItem.setMarker(markerSymbol);
   }

   /**
    * @param markerItem
    * @param photo
    *
    * @return OSCIM bitmap
    */
   private Bitmap createPhotoItems_20_CreateBitmap(final MarkerItem markerItem,
                                                   final Photo photo) {

      Bitmap oscimPhotoBitmap = null;

      final BufferedImage awtImage = createPhotoItems_30_GetImage(markerItem, photo);

      if (awtImage != null) {

         try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            ImageIO.write(awtImage, "png", output);
            final InputStream is = new ByteArrayInputStream(output.toByteArray());

            oscimPhotoBitmap = CanvasAdapter.decodeBitmap(is);

         } catch (final IOException e) {
            StatusUtil.log(e);
         }
      }

      return oscimPhotoBitmap;
   }

   /**
    * @param photo
    * @param map
    * @param tile
    *
    * @return Returns the photo image or <code>null</code> when image is not loaded.
    */
   private BufferedImage createPhotoItems_30_GetImage(final MarkerItem markerItem,
                                                      final Photo photo) {

      BufferedImage awtThumbImage = null;
      BufferedImage awtPhotoImageThumbHQ = null;

      /*
       * 1. The thumbs MUST be loaded firstly because they are also loading the image orientation
       */

      // check if image has an loading error
      final PhotoLoadingState thumbPhotoLoadingState = photo.getLoadingState(ImageQuality.THUMB);

      if (thumbPhotoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not invalid and not yet loaded

         // check if image is in the cache
         awtThumbImage = PhotoImageCache.getImage_AWT(photo, ImageQuality.THUMB);

         if (awtThumbImage == null
               && thumbPhotoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            PhotoLoadManager.putImageInLoadingQueueThumb_Map(
                  photo,
                  ImageQuality.THUMB,
                  new LoadCallbackImage(markerItem, photo),
                  true // is AWT image
            );

            return null;
         }
      }

      if (_isShowHQPhotoImages == false) {

         return awtThumbImage;
      }

      /*
       * 2. Display thumb HQ image
       */

      // check if image has an loading error
      final PhotoLoadingState thumbHqPhotoLoadingState = photo.getLoadingState(ImageQuality.THUMB_HQ);

      if (thumbHqPhotoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not invalid and not yet loaded

         // check if image is in the cache
         awtPhotoImageThumbHQ = PhotoImageCache.getImage_AWT(photo, ImageQuality.THUMB_HQ);

         if (awtPhotoImageThumbHQ == null
               && thumbHqPhotoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            PhotoLoadManager.putImageInLoadingQueueHQThumb_Map(
                  photo,
                  Photo.getMap25ImageRequestedSize(),
                  new LoadCallbackImage(markerItem, photo));
         }
      }

      if (awtPhotoImageThumbHQ != null) {

         return awtPhotoImageThumbHQ;
      }

      return awtThumbImage;
   }

   /**
    * Creates a transparent symbol with text and description for photos
    *
    * @param markerItem
    * @param photoBitmap
    *
    * @return MarkerSymbol with title, description and symbol
    */
   private MarkerSymbol createPhotoMapItem(final MarkerItem markerItem,
                                           final Bitmap photoBitmap) {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final int markerForegroundColor = ColorUtil.getARGB(config.markerOutline_Color, config.markerOutline_Opacity);
      final int markerBackgroundColor = ColorUtil.getARGB(config.markerFill_Color, config.markerFill_Opacity);

      final Paint textPainter = CanvasAdapter.newPaint();
      textPainter.setStyle(Paint.Style.STROKE);
      textPainter.setColor(markerForegroundColor);

      final Paint fillPainter = CanvasAdapter.newPaint();
      fillPainter.setStyle(Paint.Style.FILL);
      fillPainter.setColor(markerBackgroundColor);

      // adjust font to 4k display, otherwise it is really small
      final float fontHeight = textPainter.getFontHeight();
      final float scaledFontHeight = DPIUtil.autoScaleUp(fontHeight);
      textPainter.setTextSize(scaledFontHeight);

      final int margin = 5;
      final int dist2symbol = 40;
      final String markerTitle = markerItem.title;

      final float titleWidth = textPainter.getTextWidth(markerTitle) + 2 * margin;
      final float titleHeight = textPainter.getTextHeight(markerTitle) + 2 * margin;

      final int photoWidth = photoBitmap.getWidth();
      final int photoHeight = photoBitmap.getHeight();

      // total size of all elements
      final float markerWidth = Math.max(titleWidth, photoWidth);
      final float markerHeight = titleHeight + photoHeight + dist2symbol;
      final float markerWidth2 = markerWidth / 2;

      // markerCanvas, the drawing area for all: title, description and symbol
      final Bitmap markerBitmap = CanvasAdapter.newBitmap((int) markerWidth, (int) markerHeight, 0);
      final Canvas markerCanvas = CanvasAdapter.newCanvas();
      markerCanvas.setBitmap(markerBitmap);

      Bitmap titleBitmap = null;

      if (_isShowPhotoTitle) {

         // titleCanvas for the title text
         titleBitmap = CanvasAdapter.newBitmap((int) (titleWidth + 0 * margin), (int) (titleHeight + 0 * margin), 0);
         final Canvas titleCanvas = CanvasAdapter.newCanvas();
         titleCanvas.setBitmap(titleBitmap);

         // draw an oversized transparent circle, so the canvas is completely filled with a transparent color
         // titleCanvas.fillRectangle() does not support transparency
         titleCanvas.drawCircle(0, 0, markerWidth * 2, fillPainter);

         // finetune text position otherwise it is too near to the border, it is still not perfect !!!
         titleCanvas.drawText(markerTitle,
               0.6f * margin,
               titleHeight - 1.3f * margin,
               textPainter);

// SET_FORMATTING_OFF

         // draw border

         titleCanvas.drawLine(         0,             0,          0, titleHeight, textPainter);
         titleCanvas.drawLine(         0,             0, titleWidth,           0, textPainter);
         titleCanvas.drawLine(         0,   titleHeight, titleWidth, titleHeight, textPainter);
         titleCanvas.drawLine(titleWidth,             0, titleWidth, titleHeight, textPainter);

// SET_FORMATTING_ON
      }

      {
         /*
          * Draw photo pole
          */
         final float poleLineWidth = 2;

         final int poleHeight = (int) (dist2symbol * 0.8f);
         final int poleWidth = (int) (3 * poleLineWidth);

         textPainter.setStrokeWidth(poleLineWidth);

         final float x0 = poleLineWidth;
         final float x1 = poleLineWidth * 2;

         final Bitmap poleBitmap = CanvasAdapter.newBitmap(poleWidth, poleHeight, 0);
         final Canvas poleCanvas = CanvasAdapter.newCanvas();

         poleCanvas.setBitmap(poleBitmap);

         poleCanvas.drawLine(x0, 0, x0, poleHeight, textPainter);
         poleCanvas.drawLine(x1, 0, x1, poleHeight, fillPainter);

         markerCanvas.drawBitmap(poleBitmap,
               markerWidth2 - poleWidth / 2,
               markerHeight - poleHeight);
      }

      if (_isShowPhotoTitle) {
         markerCanvas.drawBitmap(titleBitmap, markerWidth2 - (titleWidth / 2), 0);
      }

      markerCanvas.drawBitmap(photoBitmap,
            markerWidth2 - (photoWidth / 2),
            markerHeight / 2 - (photoHeight / 2));

      return new MarkerSymbol(markerBitmap, HotspotPlace.BOTTOM_CENTER);
   }

   public MarkerSymbol getSymbolNotLoadedPhoto() {

      return _notLoadedPhoto_Symbol;
   }

   public boolean isShowPhotos() {

      return _isShowPhotos;
   }

   @Override
   public boolean onItemLongPress(final int index, final MarkerInterface mi) {

      return false;
   }

   @Override
   public boolean onItemSingleTapUp(final int index, final MarkerInterface mi) {

      return false;
   }

   public void restoreState() {

      _isShowPhotos = Util.getStateBoolean(_state, Map25View.STATE_IS_LAYER_PHOTO_VISIBLE, true);
      _isShowPhotoTitle = Util.getStateBoolean(_state, Map25View.STATE_IS_SHOW_PHOTO_TITLE, true);
      _isShowHQPhotoImages = Util.getStateBoolean(_state, Map25View.STATE_IS_SHOW_THUMB_HQ_IMAGES, false);

      setMapImageSize();
   }

   public void saveState() {

      _state.put(Map25View.STATE_IS_LAYER_PHOTO_VISIBLE, _isShowPhotos);

   }

   private void setMapImageSize() {

      final Enum<MapImageSize> imageSize = Util.getStateEnum(_state,
            SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE,
            MapImageSize.MEDIUM);

      int requestedPhotoImageSize;

      if (imageSize.equals(MapImageSize.LARGE)) {

         requestedPhotoImageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_LARGE,
               Map25App.MAP_IMAGE_DEFAULT_SIZE_LARGE);

      } else if (imageSize.equals(MapImageSize.MEDIUM)) {

         requestedPhotoImageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_MEDIUM,
               Map25App.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);

      } else if (imageSize.equals(MapImageSize.SMALL)) {

         requestedPhotoImageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_SMALL,
               Map25App.MAP_IMAGE_DEFAULT_SIZE_SMALL);

      } else {

         requestedPhotoImageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_TINY,
               Map25App.MAP_IMAGE_DEFAULT_SIZE_TINY);
      }

      Photo.setMap25ImageRequestedSize(requestedPhotoImageSize);
   }

   public void setPhotoIsVisible(final boolean isPhotoVisible) {

      _isShowPhotos = isPhotoVisible;
   }

}
