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
package net.tourbook.map25.layer.marker;

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
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapImageSize;
import net.tourbook.map2.view.SlideoutMap2_PhotoOptions;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;

public class PhotoToolkit extends MarkerToolkit implements ItemizedLayer.OnItemGestureListener<MarkerInterface> {

   private IDialogSettings _state;

   private Map25App        _mapApp;

   private boolean         _isShowHQPhotoImages;
   private boolean         _isShowPhotos;
   private boolean         _isShowPhotoTitle;

   /**
    * This image is displayed when a photo is not yet loaded
    */
   private Bitmap          _bitmapNotLoadedPhoto;

   private MarkerSymbol    _symbolNotLoadedPhoto; // marker symbol, circle or star

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
         createPhotoItems_10_CreateBitmapFromPhoto(_markerItem, _photo);

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

//    _bitmapPhoto = createPhotoBitmap();
      _bitmapNotLoadedPhoto = createShapeBitmap(MarkerShape.CIRCLE);
//    _bitmapClusterPhoto = createPoiBitmap(MarkerShape.CIRCLE); //must be replaced later, like MarkerToolkit

      _symbolNotLoadedPhoto = new MarkerSymbol(_bitmapNotLoadedPhoto, MarkerSymbol.HotspotPlace.BOTTOM_CENTER, false);

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
    * Creates a LIST with tourphotos, which can directly added to the photoLayer via addItems
    *
    * @param allPhotos
    *
    * @return
    */
   public List<MarkerInterface> createPhotoItems(final List<Photo> allPhotos) {

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

         // the photo bitmap is set into the markerItem
         createPhotoItems_10_CreateBitmapFromPhoto(markerItem, photo);

         allPhotoItems.add(markerItem);
      }

      return allPhotoItems;
   }

   private void createPhotoItems_10_CreateBitmapFromPhoto(final MarkerItem markerItem,
                                                          final Photo photo) {

      Bitmap bitmapImage = createPhotoItems_20_CreateBitmap(markerItem, photo);

      if (bitmapImage == null) {
         bitmapImage = _bitmapNotLoadedPhoto;
      }

      final MarkerSymbol bitmapPhoto = createMarkerSymbol(markerItem, bitmapImage, true, _isShowPhotoTitle);

      markerItem.setMarker(bitmapPhoto);
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

   public MarkerSymbol getSymbolNotLoadedPhoto() {

      return _symbolNotLoadedPhoto;
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
