/*******************************************************************************
 * Copyright 2016-2018 devemux86
 * Copyright 2017 nebular
 * Copyright 2019, 2021 Wolfgang Schramm and Contributors
 * Copyright 2019, 2021 Thomas Theussing
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
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.ImageUtils;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.imgscalr.Scalr.Rotation;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ClusterMarkerRenderer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
//import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerRendererFactory;
import org.oscim.layers.marker.MarkerSymbol;

public class PhotoToolkit extends MarkerToolkit implements ItemizedLayer.OnItemGestureListener<MarkerInterface> {

//   public static final int               IMAGE_SIZE_SMALL         = 160;
//   public static final int               IMAGE_SIZE_MEDIUM        = 320;
//   public static final int               IMAGE_SIZE_LARGE         = 320;

   private Bitmap _bitmapCluster;
   //private boolean _isBillboard;
   public MarkerSymbol          _symbol;               //marker symbol, circle or star
   private Bitmap               _bitmapPhoto;          //normaly the photo as Bitmap

   private Bitmap               _BitmapClusterPhoto;   // The Bitmap when markers are clustered

   private ArrayList<Photo>     _allPhotos;

   public MarkerRendererFactory _markerRendererFactory;

   public boolean               _isMarkerClusteredLast;

   public boolean               _isPhotoShowScaled;
   Display                      _display;


//   private int  _imageSize;
//   private static final String      STATE_PHOTO_PROPERTIES_IMAGE_SIZE      = "STATE_PHOTO_PROPERTIES_IMAGE_SIZE";       //$NON-NLS-1$
//   private IDialogSettings       _state;

   private Map25App _mapApp;

   private class LoadCallbackImage implements ILoadCallBack {

      private Map25App _mapApp;
      //private Map25View = _map25App.

      public LoadCallbackImage() {
         //_map25View = null;
         //_mapApp = null;
      }

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {
         _mapApp.updateUI_PhotoLayer();

      }
   }

   public PhotoToolkit() {
      super(MarkerShape.CIRCLE);
      //debugPrint(" ** PhotoToolkit + *** Constructor"); //$NON-NLS-1$
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      loadConfig();

      _fillPainter.setStyle(Paint.Style.FILL);
      _bitmapCluster = createClusterBitmap(1);
      //_bitmapPhoto = createPhotoBitmap();
      _bitmapPhoto = createPoiBitmap(MarkerShape.CIRCLE);
      _BitmapClusterPhoto = createPoiBitmap(MarkerShape.CIRCLE); //must be replaced later, like MarkerToolkit
      _symbol = new MarkerSymbol(_bitmapPhoto, MarkerSymbol.HotspotPlace.BOTTOM_CENTER, false);
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


   public MarkerSymbol createPhotoBitmapFromPhoto(final Photo photo, final MarkerItem item, final boolean showPhotoTitle) {

      Bitmap bitmapImage = getPhotoBitmap(photo);
      MarkerSymbol bitmapPhoto = null;

      if (bitmapImage == null) {
         bitmapImage = _bitmapPhoto;
      }
      bitmapPhoto = createAdvanceSymbol(item, bitmapImage, true, showPhotoTitle);

      return bitmapPhoto;
   }

   /**
    * creates a LIST with tourphotos, which can directly added to the photoLayer via addItems
    *
    * @param galleryPhotos
    *           Arraylist of photos
    * @param showPhotoTitle
    *           boolean, show photo with title or not
    * @param showPhotoScaled
    * @return
    */
   public List<MarkerInterface> createPhotoItemList(final ArrayList<Photo> galleryPhotos,
                                                    final boolean showPhotoTitle,
                                                    final boolean showPhotoScaled) {

      _isPhotoShowScaled = showPhotoScaled;
      debugPrint(" Phototoolkit createPhotoItemList: entering "); //$NON-NLS-1$

      final List<MarkerInterface> pts = new ArrayList<>();

      if (galleryPhotos == null) {
         debugPrint(" Map25View: *** createPhotoItemList: galleriePhotos was null"); //$NON-NLS-1$
         return pts;
      }

      if (galleryPhotos.isEmpty()) {
         debugPrint(" Map25View: *** createPhotoItemList: galleriePhotos.size() was 0"); //$NON-NLS-1$
         return pts;
      }

      /*
       * if (!_isShowPhoto) {
       * debugPrint(" Map25View: *** createPhotoItemList: photlayer is off");
       * return pts;
       * }
       */

      _allPhotos = galleryPhotos;

      for (final Photo photo : galleryPhotos) {
         int stars = 0;
         String starText = UI.EMPTY_STRING;
         String photoName = UI.EMPTY_STRING;
         final UUID photoKey = UUID.randomUUID();

         stars = photo.ratingStars;
         //starText = UI.EMPTY_STRING;
         switch (stars) {
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
         if (photo.isGeoFromExif &&
               Math.abs(photo.getImageMetaData().latitude) > Double.MIN_VALUE &&
               Math.abs(photo.getImageMetaData().longitude) > Double.MIN_VALUE) {
            //photo contains valid(>0) GPS position in the EXIF
//            debugPrint("PhotoToolkit: *** createPhotoItemList: using exif geo");
            photoLat = photo.getImageMetaData().latitude;
            photoLon = photo.getImageMetaData().longitude;
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
         final MarkerSymbol markerSymbol = createPhotoBitmapFromPhoto(photo, item, showPhotoTitle);

         if (markerSymbol != null) {
            item.setMarker(markerSymbol);
         }

         pts.add(item);
      }
      //_photo_pts = pts;
      _allPhotos = galleryPhotos;

      return pts;
   }

   /**
    * same as in TourMapPainter, but for 2.5D maps
    *
    * @param photo
    * @return the bitmap
    */
   private Bitmap getPhotoBitmap(final Photo photo) {

      Bitmap photoBitmap = null;
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      final int scaledThumbImageSize = config.markerPhoto_Size;
      // using photo image size of 2D map, not working yet
      //_imageSize = Util.getStateInt(_state, STATE_PHOTO_PROPERTIES_IMAGE_SIZE, Photo.MAP_IMAGE_DEFAULT_WIDTH_HEIGHT);
      // ensure that an image is displayed, it happend that image size was 0

      final Image scaledThumbImage = getPhotoImage(photo, scaledThumbImageSize);

      if (scaledThumbImage != null) {
         try {

            final byte[] formattedImage = ImageUtils.formatImage(scaledThumbImage, org.eclipse.swt.SWT.IMAGE_BMP);

            photoBitmap = CanvasAdapter.decodeBitmap(new ByteArrayInputStream(formattedImage));

         } catch (final IOException e) {
            StatusUtil.log(e);
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
   private Image getPhotoImage(final Photo photo, final int thumbSize) {

      Image photoImage = null;
      Image scaledThumbImage = null;

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

         if (!_isPhotoShowScaled) {
            debugPrint(" ??????????? PhotoToolkit getPhotoImage: returned unscaled image"); //$NON-NLS-1$
            return photoImage;
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

            scaledThumbImage = ImageUtils.resize(
                  _display,
                  photoImage,
                  bestSize.x,
                  bestSize.y,
                  SWT.ON,
                  SWT.LOW,
                  thumbRotation);
            //photoImage.dispose();  //gives exception, why?

         } else {

            // wait until image is loaded
            scaledThumbImage = null;
         }
      }
      return scaledThumbImage;
   }

   @Override
   public boolean onItemLongPress(final int index, final MarkerInterface mi) {
      final MarkerItem photoItem = (MarkerItem) mi;
      // TODO Auto-generated method stub
      debugPrint(" ??????????? PhotoToolkit *** onItemLongPress(int index, MarkerItem photoItem): " + _allPhotos.get( //$NON-NLS-1$
            index).imageFilePathName
            + " " + photoItem.getTitle()); //$NON-NLS-1$
      return false;
   }

   @Override
   public boolean onItemSingleTapUp(final int index, final MarkerInterface mi) {
      final MarkerItem photoItem = (MarkerItem) mi;
      // TODO Auto-generated method stub
      debugPrint(" ??????????? PhotoToolkit *** onItemSingleTapUp(int index, MarkerItem photoItem): " + _allPhotos //$NON-NLS-1$
            .get(
                  index).imageFilePathName + " " + photoItem.getTitle()); //$NON-NLS-1$
      //showPhoto(_allPhotos.get(index));
      return false;
   }


   public void showPhoto(final Photo photo) {

      final Image image = getPhotoImage(photo, PhotoLoadManager.IMAGE_SIZE_LARGE_DEFAULT);
      if (image == null) {
         return;
      }

      final Display display = new Display();
      final Shell shell = new Shell(display);
      shell.setSize(image.getBounds().width, image.getBounds().height);
      shell.setText(photo.imageFileName);
      shell.setLayout(new FillLayout());
      final Canvas canvas = new Canvas(shell, SWT.NONE);

      canvas.addPaintListener(new PaintListener() {
         @Override
         public void paintControl(final PaintEvent e) {
            e.gc.drawImage(image, 10, 10);
            image.dispose();
         }
      });

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch()) {
            display.sleep();
         }
      }

      display.dispose();
   }

   public void updatePhotos() {
      //net.tourbook.map25.debugPrint("???? PhotoToolkit: Update Photos");
      _mapApp.updateUI_PhotoLayer();
   }

}
