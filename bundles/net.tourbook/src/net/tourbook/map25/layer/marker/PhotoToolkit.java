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
import net.tourbook.map25.layer.marker.cluster.ClusterMarkerRenderer;
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
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerRendererFactory;
import org.oscim.layers.marker.MarkerSymbol;

public class PhotoToolkit extends MarkerToolkit implements ItemizedLayer.OnItemGestureListener<MarkerInterface> {

   private Map25App _mapApp;
   private Display  _display;

   /**
    * This image is displayed when a photo is not yet loaded
    */
   private Bitmap   _bitmapNotLoadedPhoto;
   private Bitmap   _bitmapCluster;
// private Bitmap   _bitmapClusterPhoto;   // The Bitmap when markers are clustered

   public MarkerSymbol          _symbol;               //marker symbol, circle or star

   public MarkerRendererFactory _markerRendererFactory;

// private boolean _isBillboard;
   public boolean _isMarkerClusteredLast;

   public class ImageState {

      Image   _photoImage;
      boolean _isMustDisposeImage;

      private ImageState(final Image photoImage, final boolean isMustDisposeImage) {

         _photoImage = photoImage;
         _isMustDisposeImage = isMustDisposeImage;
      }
   }

   private class LoadCallbackImage implements ILoadCallBack {

      public LoadCallbackImage() {}

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         // photo items must be recreated from loading photo symbol -> real photo
         _mapApp.updateUI_PhotoLayer();
      }
   }

   public PhotoToolkit(final Map25App map25App) {

      super(MarkerShape.CIRCLE);

      _mapApp = map25App;
      _display = Display.getDefault();

      //debugPrint(" ** PhotoToolkit + *** Constructor"); //$NON-NLS-1$
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      loadConfig();

      _fillPainter.setStyle(Paint.Style.FILL);

      _bitmapCluster = createClusterBitmap(1);
//    _bitmapPhoto = createPhotoBitmap();
      _bitmapNotLoadedPhoto = createPoiBitmap(MarkerShape.CIRCLE);
//    _bitmapClusterPhoto = createPoiBitmap(MarkerShape.CIRCLE); //must be replaced later, like MarkerToolkit

      _symbol = new MarkerSymbol(_bitmapNotLoadedPhoto, MarkerSymbol.HotspotPlace.BOTTOM_CENTER, false);
      _isMarkerClusteredLast = config.isMarkerClustered;

      _markerRendererFactory = new MarkerRendererFactory() {

         @Override
         public org.oscim.layers.marker.MarkerRenderer create(final org.oscim.layers.marker.MarkerLayer markerLayer) {

            return new ClusterMarkerRenderer(markerLayer, _symbol, new ClusterMarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE)) {

               @Override
               protected Bitmap getClusterBitmap(final int size) {

                  // Can customize cluster bitmap here
                  //debugPrint("??? Markertoolkit:  cluster size: " + size); //$NON-NLS-1$
                  _bitmapCluster = createClusterBitmap(size);

                  return _bitmapCluster;
               }
            };
         }
      };
   }

   private MarkerSymbol createPhotoBitmapFromPhoto(final Photo photo, final MarkerItem item, final boolean showPhotoTitle) {

      Bitmap bitmapImage = getPhotoBitmap(photo);

      if (bitmapImage == null) {
         bitmapImage = _bitmapNotLoadedPhoto;
      }

      final MarkerSymbol bitmapPhoto = createAdvanceSymbol(item, bitmapImage, true, showPhotoTitle);

      return bitmapPhoto;
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
   public List<MarkerInterface> createPhotoItems(final ArrayList<Photo> galleryPhotos) {

      final List<MarkerInterface> allPhotoMapItems = new ArrayList<>();

      if (galleryPhotos == null || galleryPhotos.isEmpty()) {
         return allPhotoMapItems;
      }

      final boolean isShowPhotoTitle = _mapApp.isPhoto_ShowTitle();

      for (final Photo photo : galleryPhotos) {

         String starText = UI.EMPTY_STRING;
         String photoName = UI.EMPTY_STRING;
         final UUID photoKey = UUID.randomUUID();

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

         photoName = TimeTools.getZonedDateTime(photo.imageExifTime).format(TimeTools.Formatter_Time_S) + starText;

         final String photoDescription = "Ratingstars: " + Integer.toString(photo.ratingStars); //$NON-NLS-1$

         Double photoLat = 0.0;
         Double photoLon = 0.0;
         final PhotoImageMetadata imageMetaData = photo.getImageMetaData();

         if (photo.isGeoFromExif
               && Math.abs(imageMetaData.latitude) > Double.MIN_VALUE
               && Math.abs(imageMetaData.longitude) > Double.MIN_VALUE) {

            //photo contains valid(>0) GPS position in the EXIF

//            debugPrint("PhotoToolkit: *** createPhotoItemList: using exif geo");
            photoLat = imageMetaData.latitude;
            photoLon = imageMetaData.longitude;

         } else {

            //using position via time marker

            debugPrint("PhotoToolkit: *** createPhotoItemList: using tour geo"); //$NON-NLS-1$
            photoLat = photo.getTourLatitude();
            photoLon = photo.getTourLongitude();
         }

         //debugPrint("PhotoToolkit: *** createPhotoItemList Name: " + photo.getImageMetaData().objectName + " Description: " + photo.getImageMetaData().captionAbstract);

         final MarkerItem item = new MarkerItem(photoKey,
               photoName, //time as name
               photoDescription, //rating stars as description
               new GeoPoint(photoLat, photoLon));

         //now create the marker: photoimage with time and stars
         final MarkerSymbol markerSymbol = createPhotoBitmapFromPhoto(photo, item, isShowPhotoTitle);

         if (markerSymbol != null) {
            item.setMarker(markerSymbol);
         }

         allPhotoMapItems.add(item);
      }

      return allPhotoMapItems;
   }

   /**
    * same as in TourMapPainter, but for 2.5D maps
    *
    * @param photo
    * @return the bitmap
    */
   private Bitmap getPhotoBitmap(final Photo photo) {

      Bitmap photoBitmap = null;

      // ensure min photo size
      final int scaledThumbImageSize = Math.max(10, _mapApp.getPhoto_Size());

      // using photo image size of 2D map, not working yet
      //_imageSize = Util.getStateInt(_state, STATE_PHOTO_PROPERTIES_IMAGE_SIZE, Photo.MAP_IMAGE_DEFAULT_WIDTH_HEIGHT);
      // ensure that an image is displayed, it happend that image size was 0

      final ImageState imageState = getPhotoImage(photo, scaledThumbImageSize);
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
    * @param photo
    * @param thumbSize
    *           thumbnail size from slideout
    * @return
    */
   private ImageState getPhotoImage(final Photo photo, final int thumbSize) {

      Image photoImage = null;
      Image scaledImage = null;

      boolean isMustDisposeImage = false;

//      final ImageQuality requestedImageQuality = ImageQuality.HQ; // THUMB;
      final ImageQuality requestedImageQuality = ImageQuality.THUMB;

      // check if image has an loading error
      final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);
      if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not yet loaded

         // check if image is in the cache
         photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

         if ((photoImage == null || photoImage.isDisposed())
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            final ILoadCallBack imageLoadCallback = new LoadCallbackImage();

            PhotoLoadManager.putImageInLoadingQueueThumbMap(photo, requestedImageQuality, imageLoadCallback);
         }

         if (_mapApp.isPhoto_Scaled() == false) {
            return new ImageState(photoImage, false);
         }

         if (photoImage != null) {

            final Rectangle imageBounds = photoImage.getBounds();
            final int originalImageWidth = imageBounds.width;
            final int originalImageHeight = imageBounds.height;

            final int imageWidth = originalImageWidth;
            final int imageHeight = originalImageHeight;

            //final int thumbSize = PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;//    PhotoLoadManager.IMAGE_SIZE_LARGE_DEFAULT;
            boolean isRotated = false;

            final Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, thumbSize, thumbSize);
            final Rotation thumbRotation = null;
            if (isRotated == false) {
               isRotated = true;
               //thumbRotation = getRotation();
            }

            //debugPrint("??? getPhotoImage imageWidth and thumbsize: " + imageWidth + " " + thumbSize);

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
