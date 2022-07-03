/*******************************************************************************
 * Copyright 2019, 2022 Wolfgang Schramm and Contributors
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
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.ui.SlideoutMap25_PhotoOptions;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.ImageUtils;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;

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

   private Map25App _mapApp;
   private Display  _display;

   /**
    * This image is displayed when a photo is not yet loaded
    */
   private Bitmap   _bitmapNotLoadedPhoto;
// private Bitmap   _bitmapClusterPhoto;   // The Bitmap when markers are clustered

   private MarkerSymbol _symbol; // marker symbol, circle or star

// private boolean _isBillboard;

   private class ImageState {

      Image   _photoImage;
      boolean _isMustDisposeImage;

      private ImageState(final Image photoImage, final boolean isMustDisposeImage) {

         _photoImage = photoImage;
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

   public PhotoToolkit(final Map25App map25App) {

      super(MarkerShape.CIRCLE);

      _mapApp = map25App;
      _display = Display.getDefault();

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      _fillPainter.setStyle(Paint.Style.FILL);

//    _bitmapPhoto = createPhotoBitmap();
      _bitmapNotLoadedPhoto = createShapeBitmap(MarkerShape.CIRCLE);
//    _bitmapClusterPhoto = createPoiBitmap(MarkerShape.CIRCLE); //must be replaced later, like MarkerToolkit

      _symbol = new MarkerSymbol(_bitmapNotLoadedPhoto, MarkerSymbol.HotspotPlace.BOTTOM_CENTER, false);

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

      final String photoStars = createPhoto_Stars(photo);
      final String photoName = TimeTools.getZonedDateTime(photo.imageExifTime).format(TimeTools.Formatter_Time_S) + photoStars;

      return photoName;
   }

   private String createPhoto_Stars(final Photo photo) {

      String starText = UI.EMPTY_STRING;

      switch (photo.ratingStars) {
      case 1:
         starText = " *"; //$NON-NLS-1$
         break;
      case 2:
         starText = " **"; //$NON-NLS-1$
         break;
      case 3:
         starText = " ***"; //$NON-NLS-1$
         break;
      case 4:
         starText = " ****"; //$NON-NLS-1$
         break;
      case 5:
         starText = " *****"; //$NON-NLS-1$
         break;
      }

      return starText;
   }

   /**
    * Creates a LIST with tourphotos, which can directly added to the photoLayer via addItems
    *
    * @param galleryPhotos
    *           Arraylist of photos
    * @param isShowPhotoTitle
    *           boolean, show photo with title or not
    * @param isPhotoScaled
    * @return
    */
   public List<MarkerInterface> createPhotoItems(final List<Photo> galleryPhotos) {

      final List<MarkerInterface> allPhotoItems = new ArrayList<>();

      if (galleryPhotos == null || galleryPhotos.isEmpty()) {
         return allPhotoItems;
      }

      for (final Photo photo : galleryPhotos) {

         final UUID photoKey = UUID.randomUUID();
         final String photoName = createPhoto_Name(photo);
         final String photoDescription = "Ratingstars: " + Integer.toString(photo.ratingStars); //$NON-NLS-1$
         final GeoPoint geoPoint = createPhoto_Location(photo);

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

   private void createPhotoItems_10_CreateBitmapFromPhoto(final MarkerItem item,
                                                          final Photo photo,
                                                          final boolean isImageLoaded) {

      Bitmap bitmapImage = createPhotoItems_20_CreateBitmap(item, photo, isImageLoaded);

      if (bitmapImage == null) {
         bitmapImage = _bitmapNotLoadedPhoto;
      }

      final MarkerSymbol bitmapPhoto = createAdvanceSymbol(item, bitmapImage, true, _mapApp.isPhoto_ShowTitle());

      item.setMarker(bitmapPhoto);
   }

   /**
    * same as in TourMapPainter, but for 2.5D maps
    *
    * @param item
    * @param photo
    * @param isImageLoaded
    * @return the bitmap
    */
   private Bitmap createPhotoItems_20_CreateBitmap(final MarkerItem item,
                                                   final Photo photo,
                                                   final boolean isImageLoaded) {

      Bitmap photoBitmap = null;

      // ensure photo minimum size
      final int scaledImageSize = Math.max(SlideoutMap25_PhotoOptions.IMAGE_SIZE_MINIMUM, _mapApp.getPhoto_Size());

      final ImageState imageState = createPhotoItems_30_GetScaledImage(
            item,
            photo,
            scaledImageSize,
            isImageLoaded);

      final Image scaledImage = imageState._photoImage;

      if (scaledImage != null) {

         try {

            final byte[] formattedImage = ImageUtils.formatImage(scaledImage, org.eclipse.swt.SWT.IMAGE_BMP);

            photoBitmap = CanvasAdapter.decodeBitmap(new ByteArrayInputStream(formattedImage));

         } catch (final IOException e) {
            StatusUtil.log(e);
         }

         if (imageState._isMustDisposeImage) {
            scaledImage.dispose();
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
    * @return
    */
   private ImageState createPhotoItems_30_GetScaledImage(final MarkerItem item,
                                                         final Photo photo,
                                                         final int thumbSize,
                                                         final boolean isImageLoaded) {

      Image photoImage = null;
      Image scaledImage = null;

      boolean isMustDisposeImage = false;

//    final ImageQuality requestedImageQuality = ImageQuality.HQ;
      final ImageQuality requestedImageQuality = ImageQuality.THUMB;

      // check if image has an loading error
      final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);
      if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not invalid

         // check if image is in the cache
         photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

         // put photo image in loading queue
         if ((photoImage == null || photoImage.isDisposed())

               // photo image is not in loading queue
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false

               // prevent reloading image
               && isImageLoaded == false) {

            // the requested image is not available in the image cache -> image must be loaded

            final ILoadCallBack imageLoadCallback = new LoadCallbackImage(item, photo);

            PhotoLoadManager.putImageInLoadingQueueThumbMap(photo, requestedImageQuality, imageLoadCallback);
         }

         if (photoImage != null) {

            if (_mapApp.isPhoto_Scaled() == false) {
               return new ImageState(photoImage, false);
            }

            // scale image

            final Rectangle imageBounds = photoImage.getBounds();
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

            scaledImage = ImageUtils.resize(
                  _display,
                  photoImage,
                  bestSize.x,
                  bestSize.y,
                  SWT.ON,
                  SWT.LOW,
                  thumbRotation);

            isMustDisposeImage = true;

         } else {

            // wait until image is loaded
         }
      }

      return new ImageState(scaledImage, isMustDisposeImage);
   }

   public MarkerSymbol getSymbol() {
      return _symbol;
   }

   @Override
   public boolean onItemLongPress(final int index, final MarkerInterface mi) {

//      final MarkerItem photoItem = (MarkerItem) mi;
//
//      debugPrint(" ??????????? PhotoToolkit *** onItemLongPress(int index, MarkerItem photoItem): " + _allPhotos.get( //$NON-NLS-1$
//            index).imageFilePathName + " " + photoItem.getTitle()); //$NON-NLS-1$

      return false;
   }

   @Override
   public boolean onItemSingleTapUp(final int index, final MarkerInterface mi) {

//      final MarkerItem photoItem = (MarkerItem) mi;
//
//      debugPrint(" ??????????? PhotoToolkit *** onItemSingleTapUp(int index, MarkerItem photoItem): " + _allPhotos //$NON-NLS-1$
//            .get(index).imageFilePathName + " " + photoItem.getTitle()); //$NON-NLS-1$

      //showPhoto(_allPhotos.get(index));

      return false;
   }

}
