package net.tourbook.map25.layer.marker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
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
import net.tourbook.photo.Photo;

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

   final Paint _fillPainter = CanvasAdapter.newPaint();

   public MarkerRendererFactory _markerRendererFactory;
   
   public enum MarkerShape {STAR, CIRCLE};
   
   public enum MarkerMode {DEMO, NORMAL};
   
   public boolean _isMarkerClusteredLast;
   
   
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
   
 
   public Bitmap createPhotoBitmapFromFile(String photofile) {
      System.out.println(" PhotoToolkit + *** createPhotoBitmapFromFile for file: " + photofile);
      Bitmap photoBitmap = CanvasAdapter.newBitmap(120, 90, 0);
      final File photoFile = new File(photofile);
      Bitmap bitmapFromPhotoFile = null;
      FileInputStream fileStream;
      try {
         fileStream = new FileInputStream(photoFile);
         bitmapFromPhotoFile = CanvasAdapter.decodeBitmap(fileStream, 120, 90, 100);
      } catch (FileNotFoundException e1) {
         e1.printStackTrace();
         return _bitmapPhoto;
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         return _bitmapPhoto;
      }

      org.oscim.backend.canvas.Canvas PhotoCanvas = CanvasAdapter.newCanvas();  
      PhotoCanvas.setBitmap(photoBitmap);
      PhotoCanvas.drawBitmap(bitmapFromPhotoFile, 0, 0);
      //PhotoCanvas.drawBitmapScaled(bitmapFromPhotoFile);
      float half = _symbolSizeInt/2;

      return photoBitmap;
   }
   
   
//   public List<MarkerItem> createPhotoItemList(ArrayList<Photo> galleryPhotos, PhotoMode PhotoMode){
//      loadConfig();
//      
//      List<MarkerItem> pts = new ArrayList<>();
//      System.out.println(" PhotoToolkit + *** paintPhotoSelection: Path: " + galleryPhotos.get(0).imagePathName);
//      for (final  Photo photo : galleryPhotos) {
//         //UUID photoKey = UUID.fromString(Photo.getImageKeyThumb(photo.imageFilePathName));
//         UUID photoKey = UUID.randomUUID();
//         String photoName = photo.imageFileName;
//         String photoDescription = photo.getDimensionText();
//         Double photoLat = photo.getTourLatitude();
//         Double photoLon = photo.getTourLongitude();
//         
//         MarkerItem item = new MarkerItem(photoKey, photoName, photoDescription,
//               new GeoPoint(photoLat, photoLon)
//               );
//         item.setMarker(new MarkerSymbol(createPhotoBitmap(), HotspotPlace.CENTER));
//         System.out.println("item lat: " + item.geoPoint.getLatitude() + " lon: " + item.geoPoint.getLongitude());
//         //item.get
//         pts.add(item);
//
//        /* System.out.println(" PhotoToolkit + *** createMarkerItemList, adding: " + " " + photo.imageFileName +
//               " tour lat: " + photo.getTourLatitude() + " lon: " + photo.getTourLongitude() +
//               " dimtext:" + photo.getDimensionText() +
//               " keythumb:" + Photo.getImageKeyThumb(photo.imageFilePathName)
//               );*/
//      } 
//      
//
//      if (PhotoMode == PhotoMode.NORMAL) {
//         this._photo_pts = pts;
//         return pts;};
//
//      int COUNT = 5;
//      float STEP = 100f / 110000f; // roughly 100 meters
//
//      double demo_lat = 47.2266239;
//      double demo_lon = 8.8184374;
//
//      for (int x = -COUNT; x < COUNT; x++) {
//         for (int y = -COUNT; y < COUNT; y++) {
//            double random = STEP * Math.random() * 2;
//            MarkerItem item = new MarkerItem(y + ", " + x, "Title " + demo_lat + "/" + demo_lon,"Description "  + x + "/" + y, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
//                  new GeoPoint(demo_lat + y * STEP + random, demo_lon + x * STEP + random)
//                  );
//      //      item.setMarker(createAdvanceSymbol(item, _bitmapPoi));
//            pts.add(item);
//         }
//      }
//      this._photo_pts = pts;
//      this._galleryPhotos = galleryPhotos;
//      return pts;  
//   }
   
}
