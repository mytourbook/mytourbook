package net.tourbook.map25.layer.marker;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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


public class PhotoToolkit {

   private int _fgColor = 0xFF000000; // 100 percent black. AARRGGBB
   private int _bgColor = 0x80FF69B4; // 50 percent pink. AARRGGBB
   private int _clusterSymbolSizeDP = net.tourbook.map25.layer.marker.MarkerRenderer.MAP_MARKER_CLUSTER_SIZE_DP;
   private int _clusterForegroundColor = net.tourbook.map25.layer.marker.MarkerRenderer.CLUSTER_COLOR_TEXT;
   private int _clusterBackgroundColor = net.tourbook.map25.layer.marker.MarkerRenderer.CLUSTER_COLOR_BACK;
   
   private int  _clusterSymbolWeight;
   private float  _clusterOutlineSize;
   private Bitmap _clusterBitmap;
   //private boolean _isBillboard;
   
   public MarkerSymbol _symbol;
   private float _symbolSize = 20f;
   private int _symbolSizeInt = 20;
   private int _clusterSymbol_Size;

   private Bitmap _bitmapPhoto;
   private Bitmap _bitmapStar;
   private Bitmap _BitmapClusterStar;
   
//   private static ImageUtils _imageUtils;

   final Paint _fillPainter = CanvasAdapter.newPaint();

   public MarkerRendererFactory _markerRendererFactory;
   
   public enum MarkerShape {STAR, CIRCLE};
   
   public enum MarkerMode {DEMO, NORMAL};
   
   public boolean _isMarkerClusteredLast;
   
   Display                       _display;
   
   //private Map25App                      _mapApp;
   public enum PhotoMode {DEMO, NORMAL};
  // public List<MarkerItem> _photo_pts = new ArrayList<>();

  // private ArrayList<Photo> _galleryPhotos;
   
  // public ArrayList<Photo> get_galleryPhotos() {
  //    return _galleryPhotos;
  // }

   public PhotoToolkit() {
      System.out.println(" PhotoToolkit + *** Constructor");
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      loadConfig();
      
      _fillPainter.setStyle(Paint.Style.FILL);
      
      _clusterBitmap = createClusterBitmap(1);
      
      _bitmapPhoto = createPhotoBitmap();
      
      //_BitmapClusterStar = createPoiBitmap(); //must be replaced later, like MarkerToolkit
      
      //_symbol = new MarkerSymbol(_bitmapPhoto, MarkerSymbol.HotspotPlace.BOTTOM_CENTER, false);
      
      //_isMarkerClusteredLast = config.isMarkerClustered;

      
      _markerRendererFactory = new MarkerRendererFactory() {
         @Override
         public org.oscim.layers.marker.MarkerRenderer create(org.oscim.layers.marker.MarkerLayer markerLayer) {
            return new ClusterMarkerRenderer(markerLayer, _symbol, new ClusterMarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE)) {
               @Override
               protected Bitmap getClusterBitmap(int size) {
                  // Can customize cluster bitmap here
                  //System.out.println("*** Markertoolkit:  cluster size: " + size); //$NON-NLS-1$
                  _clusterBitmap = createClusterBitmap(size);
                  return _clusterBitmap;
               }
            };
         }
      };
   }

   public final void loadConfig () {
      //System.out.println(" PhotoToolkit + *** loadConfig");
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      _fgColor = ColorUtil.getARGB(config.markerOutline_Color, (int) (config.markerOutline_Opacity / 100.0 * 0xff));
      _bgColor = ColorUtil.getARGB(config.markerFill_Color,    (int) (config.markerFill_Opacity    / 100.0 * 0xff));
   }
   
   public Bitmap createPhotoBitmap() {
      //loadConfig();
      //System.out.println("createPhotoBitmap size: " + _symbolSizeInt);
      _bitmapPhoto = CanvasAdapter.newBitmap(_symbolSizeInt, _symbolSizeInt, 0);

      org.oscim.backend.canvas.Canvas defaultPhotoCanvas = CanvasAdapter.newCanvas();  
      defaultPhotoCanvas.setBitmap(_bitmapPhoto);
      float half = _symbolSizeInt/2;

      _fillPainter.setColor(0x80FF0000);
      defaultPhotoCanvas.drawCircle(half, half, half, _fillPainter);

     return _bitmapPhoto;
      
   }

   public Bitmap createClusterBitmap(int size) {
      
      final ScreenUtils.ClusterDrawable drawable = new ScreenUtils.ClusterDrawable(
            _clusterSymbolSizeDP,
            _clusterForegroundColor,
            _clusterBackgroundColor,
            Integer.toString(size),
            _clusterSymbolWeight,
            _clusterOutlineSize);

      final Bitmap paintedBitmap = drawable.getBitmap(_BitmapClusterStar);
      return paintedBitmap;
   }

   /**
    * same as above, but for 2.5D maps
    * @param photo
    * @return the image
    */
   public  Bitmap getPhotoImage(final Photo photo) {
      Image photoImage = null;
      Bitmap photoBitmap = null;
      //final org.eclipse.swt.graphics.Point photoSize = photo.getMapImageSize();
      
      final ImageQuality requestedImageQuality = ImageQuality.THUMB;
      
      // check if image has an loading error
      final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

 
      if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {
         System.out.println("!!! entering getPhotoImage");
         // image is not yet loaded

         // check if image is in the cache
         photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);
         
         if ((photoImage == null || photoImage.isDisposed())
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            //final ILoadCallBack imageLoadCallback = new LoadCallbackImage(map, tile);

            //PhotoLoadManager.putImageInLoadingQueueThumbMap(photo, requestedImageQuality, imageLoadCallback);
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
               System.out.println("!!! getPhotoImage created photoBitmap heigth: " + photoBitmap.getHeight() + " width: " +  photoBitmap.getWidth());               
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
      
      final org.eclipse.swt.graphics.Point photoSize = photo.getMapImageSize();
      
      
      System.out.println(" ??????????? PhotoToolkit *** createPhotoBitmapfromPhoto: target size x / y: " + photoSize.x + " / " + photoSize.y);
      
      System.out.println(" ??????????? PhotoToolkit *** createPhotoBitmapfromPhoto: " + photo.imageFileName);
      if(bitmap != null) {
         System.out.println(" ??????????? PhotoToolkit *** createPhotoBitmapfromPhoto width: " + bitmap.getWidth());
      } else {
         System.out.println(" ??????????? PhotoToolkit *** createPhotoBitmapfromPhoto was null");
      }
      return bitmap;
   }
 
   
   
}
