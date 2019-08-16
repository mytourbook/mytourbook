package net.tourbook.map25.layer.marker;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.imgscalr.Scalr.Rotation;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.marker.ClusterMarkerRenderer;
//import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerRendererFactory;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.marker.MarkerSymbol.HotspotPlace;

import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Tile;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.marker.MarkerToolkit.MarkerShape;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.photo.ImageUtils;


public class PhotoToolkit extends MarkerToolkit{


   private Bitmap _bitmapCluster;
   //private boolean _isBillboard;
   
   public MarkerSymbol _symbol;  //marker symbol, circle or star

   private Bitmap _bitmapPhoto;  //normaly the photo as Bitmap
   private Bitmap _bitmapDefaultPhotoSymbol;  // default bitmap circle or star. used when no photo loaded
   private Bitmap _BitmapClusterPhoto;  // The Bitmap when markers are clustered

   public MarkerRendererFactory _markerRendererFactory;
   
   public boolean _isMarkerClusteredLast;
   
   Display                       _display;

   private class LoadCallbackImage implements ILoadCallBack {

      private Map  __map;
      private Tile __tile;

      public LoadCallbackImage() {
         __map = null;
         __tile = null;
      }

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         debugPrint("???? PhotoToolkit: LoadCallbackImage"); //$NON-NLS-1$
         
         if (isUpdateUI == false) {
            return;
         }

         //__map.queueOverlayPainting(__tile);
//       __map.paint();
      }
   }   
   
   
   public PhotoToolkit() {
      super(MarkerShape.CIRCLE);
      debugPrint(" ?? PhotoToolkit + *** Constructor"); //$NON-NLS-1$
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
         public org.oscim.layers.marker.MarkerRenderer create(org.oscim.layers.marker.MarkerLayer markerLayer) {
            return new ClusterMarkerRenderer(markerLayer, _symbol, new ClusterMarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE)) {
               @Override
               protected Bitmap getClusterBitmap(int size) {
                  // Can customize cluster bitmap here
                  //debugPrint("??? Markertoolkit:  cluster size: " + size); //$NON-NLS-1$
                  _bitmapCluster = createClusterBitmap(size);
                  return _bitmapCluster;
               }
            };
         }
      };
   }


   /**
    * same as in TourMapPainter, but for 2.5D maps
    * @param photo
    * @return the bitmap
    */
   public  Bitmap getPhotoImage(final Photo photo) {
      Image photoImage = null;
      Bitmap photoBitmap = null;
    
      final ImageQuality requestedImageQuality = ImageQuality.THUMB;
      
      // check if image has an loading error
      final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

      if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {
         //debugPrint("??? entering getPhotoImage"); //$NON-NLS-1$
         // image is not yet loaded

         // check if image is in the cache
         photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);
         
         if ((photoImage == null || photoImage.isDisposed())
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            final ILoadCallBack imageLoadCallback = new LoadCallbackImage();
            
            PhotoLoadManager.putImageInLoadingQueueThumbMap(photo, requestedImageQuality, imageLoadCallback);
         }
                      
         if (photoImage != null){
            
            Rectangle imageBounds = photoImage.getBounds();
            final int originalImageWidth = imageBounds.width;
            final int originalImageHeight = imageBounds.height;  
            
            int imageWidth = originalImageWidth;
            int imageHeight = originalImageHeight;
            
            final int thumbSize = PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;
            boolean isRotated = false;
            
            final Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, thumbSize, thumbSize);
            Rotation thumbRotation = null;  
            if (isRotated == false) {
               isRotated = true;
               //thumbRotation = getRotation();
            }
            
            final Image scaledThumbImage = ImageUtils.resize(
                  _display,
                  photoImage,
                  bestSize.x,
                  bestSize.y,
                  SWT.ON,
                  SWT.LOW,
                  thumbRotation);          
            
            try {
               
               //photoBitmap = CanvasAdapter.decodeBitmap(new ByteArrayInputStream(ImageUtils.formatImage(photoImage, org.eclipse.swt.SWT.IMAGE_BMP)));
               photoBitmap = CanvasAdapter.decodeBitmap(new ByteArrayInputStream(ImageUtils.formatImage(scaledThumbImage, org.eclipse.swt.SWT.IMAGE_BMP)));
               //debugPrint("??? getPhotoImage created photoBitmap heigth: " + photoBitmap.getHeight() + " width: " +  photoBitmap.getWidth());               
            } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }

         }  
      }
      
      return photoBitmap;
   }
   
   
   public Bitmap createPhotoBitmapFromPhoto(Photo photo) {
      Bitmap bitmap = null;
      bitmap = getPhotoImage(photo);
      /*
      final org.eclipse.swt.graphics.Point photoSize = photo.getMapImageSize();
        
      debugPrint(" ??????????? PhotoToolkit *** createPhotoBitmapfromPhoto: target size x / y: " + photoSize.x + " / " + photoSize.y); //$NON-NLS-1$
      
      debugPrint(" ??????????? PhotoToolkit *** createPhotoBitmapfromPhoto: " + photo.imageFileName); //$NON-NLS-1$
      if(bitmap != null) {
         debugPrint(" ??????????? PhotoToolkit *** createPhotoBitmapfromPhoto width: " + bitmap.getWidth()); //$NON-NLS-1$
      } else {
         debugPrint(" ??????????? PhotoToolkit *** createPhotoBitmapfromPhoto was null"); //$NON-NLS-1$
      }
      */
      return bitmap;
   }
 
   
   
}
