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

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.ImageUtils;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.imgscalr.Scalr.Rotation;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;

public class PhotoToolkit extends MarkerToolkit implements ItemizedLayer.OnItemGestureListener<MarkerInterface> {

   private static IPreferenceStore _prefStore = PhotoActivator.getPrefStore();
   private IDialogSettings         _state;

   private Map25App                _mapApp;

   private Display                 _display;

   private int                     _imageSize;

   private boolean                 _isPhotoVisible;
   private boolean                 _isTitleVisible;

   /**
    * This image is displayed when a photo is not yet loaded
    */
   private Bitmap                  _bitmapNotLoadedPhoto;

   private MarkerSymbol            _symbolNotLoadedPhoto;                     // marker symbol, circle or star

   private class ImageState {

      Image   _swtPhotoImage;
      boolean _isMustDisposeImage;

      private ImageState(final Image swtPhotoImage, final boolean isMustDisposeImage) {

         _swtPhotoImage = swtPhotoImage;
         _isMustDisposeImage = isMustDisposeImage;
      }
   }

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
         createPhotoItems_10_CreateBitmapFromPhoto(_markerItem, _photo, true);

         _mapApp.updateMap();
      }
   }

   public PhotoToolkit(final Map25App map25App, final IDialogSettings state) {

      super(MarkerShape.CIRCLE);

      _mapApp = map25App;
      _state = state;

      _display = Display.getDefault();

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
         createPhotoItems_10_CreateBitmapFromPhoto(markerItem, photo, false);

         allPhotoItems.add(markerItem);
      }

      return allPhotoItems;
   }

   private void createPhotoItems_10_CreateBitmapFromPhoto(final MarkerItem markerItem,
                                                          final Photo photo,
                                                          final boolean isImageLoaded) {

      Bitmap bitmapImage = createPhotoItems_20_CreateBitmap(markerItem, photo, isImageLoaded);

      if (bitmapImage == null) {
         bitmapImage = _bitmapNotLoadedPhoto;
      }

      final MarkerSymbol bitmapPhoto = createMarkerSymbol(markerItem, bitmapImage, true, _isTitleVisible);

      markerItem.setMarker(bitmapPhoto);
   }

   /**
    * same as in TourMapPainter, but for 2.5D maps
    *
    * @param item
    * @param photo
    * @param isImageLoaded
    *
    * @return the bitmap
    */
   private Bitmap createPhotoItems_20_CreateBitmap(final MarkerItem item,
                                                   final Photo photo,
                                                   final boolean isImageLoaded) {

      Bitmap photoBitmap = null;

      final ImageState imageState = createPhotoItems_30_GetScaledImage(
            item,
            photo,
            _imageSize,
            isImageLoaded);

      final Image swtPhotoImage = imageState._swtPhotoImage;

      if (swtPhotoImage != null) {

         try {

            final byte[] formattedImage = ImageUtils.formatImage(swtPhotoImage, org.eclipse.swt.SWT.IMAGE_BMP);

            photoBitmap = CanvasAdapter.decodeBitmap(new ByteArrayInputStream(formattedImage));

         } catch (final IOException e) {
            StatusUtil.log(e);
         }

         if (imageState._isMustDisposeImage) {
            swtPhotoImage.dispose();
         }
      }

      return photoBitmap;
   }

   /**
    * @param item
    * @param photo
    * @param thumbSize
    *           thumbnail size from slideout
    * @param isImageLoaded
    *
    * @return
    */
   private ImageState createPhotoItems_30_GetScaledImage(final MarkerItem item,
                                                         final Photo photo,
                                                         final int thumbSize,
                                                         final boolean isImageLoaded) {

      Image swtPhotoImage = null;
      Image swtScaledImage = null;

      boolean isMustDisposeImage = false;

//    final ImageQuality requestedImageQuality = ImageQuality.HQ;
      final ImageQuality requestedImageQuality = ImageQuality.THUMB;

      // check if image has an loading error
      final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);
      if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not invalid

         // check if image is in the cache
         swtPhotoImage = PhotoImageCache.getImage_SWT(photo, requestedImageQuality);

         // put photo image in loading queue
         if ((swtPhotoImage == null || swtPhotoImage.isDisposed())

               // photo image is not in loading queue
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false

               // prevent reloading image
               && isImageLoaded == false) {

            // the requested image is not available in the image cache -> image must be loaded

            final ILoadCallBack imageLoadCallback = new LoadCallbackImage(item, photo);

            PhotoLoadManager.putImageInLoadingQueueThumb_Map(photo, requestedImageQuality, imageLoadCallback, false);
         }

         if (swtPhotoImage != null && swtPhotoImage.isDisposed() == false) {

            boolean isScaled = false;
            isScaled = false;
            if (isScaled == false) {
               return new ImageState(swtPhotoImage, false);
            }

            // scale image

            final Rectangle imageBounds = swtPhotoImage.getBounds();
            final int originalImageWidth = imageBounds.width;
            final int originalImageHeight = imageBounds.height;

            final int imageWidth = originalImageWidth;
            final int imageHeight = originalImageHeight;

            //final int thumbSize = PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;//    PhotoLoadManager.IMAGE_SIZE_LARGE_DEFAULT;
            final Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, thumbSize, thumbSize);

            boolean isRotated = false;
            final Rotation thumbRotation = null;
            if (isRotated == false) {
               isRotated = true;
               //thumbRotation = getRotation();
            }

            final boolean isRotateImageAutomatically = _prefStore.getBoolean(IPhotoPreferences.PHOTO_SYSTEM_IS_ROTATE_IMAGE_AUTOMATICALLY);

            swtScaledImage = net.tourbook.common.util.ImageUtils.resize(
                  _display,
                  swtPhotoImage,
                  bestSize.x,
                  bestSize.y,
                  SWT.ON,
                  SWT.LOW,
                  thumbRotation,
                  isRotateImageAutomatically);

            isMustDisposeImage = true;

         } else {

            // wait until image is loaded
         }
      }

      return new ImageState(swtScaledImage, isMustDisposeImage);
   }

   public MarkerSymbol getSymbolNotLoadedPhoto() {

      return _symbolNotLoadedPhoto;
   }

   public boolean isShowPhotos() {

      return _isPhotoVisible;
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

      _isPhotoVisible = Util.getStateBoolean(_state, Map25View.STATE_IS_LAYER_PHOTO_VISIBLE, true);
      _isTitleVisible = Util.getStateBoolean(_state, Map25View.STATE_IS_SHOW_PHOTO_TITLE, true);

      final Enum<MapImageSize> imageSize = Util.getStateEnum(_state,
            SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE,
            MapImageSize.MEDIUM);

      if (imageSize.equals(MapImageSize.LARGE)) {

         _imageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_LARGE,
               Map25App.MAP_IMAGE_DEFAULT_SIZE_LARGE);

      } else if (imageSize.equals(MapImageSize.MEDIUM)) {

         _imageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_MEDIUM,
               Map25App.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);

      } else if (imageSize.equals(MapImageSize.SMALL)) {

         _imageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_SMALL,
               Map25App.MAP_IMAGE_DEFAULT_SIZE_SMALL);

      } else {

         _imageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_TINY,
               Map25App.MAP_IMAGE_DEFAULT_SIZE_TINY);
      }
   }

   public void saveState() {

      _state.put(Map25View.STATE_IS_LAYER_PHOTO_VISIBLE, _isPhotoVisible);

   }

   public void setPhotoIsVisible(final boolean isPhotoVisible) {

      _isPhotoVisible = isPhotoVisible;
   }

}
