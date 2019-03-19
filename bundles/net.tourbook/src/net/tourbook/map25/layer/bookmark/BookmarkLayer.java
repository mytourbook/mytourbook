package net.tourbook.map25.layer.bookmark;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ClusterMarkerRenderer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerLayer;
import org.oscim.layers.marker.MarkerRenderer;
import org.oscim.layers.marker.MarkerRendererFactory;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.marker.MarkerSymbol.HotspotPlace;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.bitmap.DefaultSources;

import net.tourbook.map25.layer.bookmark.BookmarkLayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BookmarkLayer extends GdxMapApp implements ItemizedLayer.OnItemGestureListener<MarkerItem> {// extends MarkerLayerTest {

   static final boolean BILLBOARDS = true;
   MarkerSymbol mFocusMarker;
   ItemizedLayer<MarkerItem> mMarkerLayer;
   private static final int COUNT = 5;
   private static final float STEP = 100f / 110000f; // roughly 100 meters

   @Override
   public void createLayers() {
       try {
           // Map events receiver
           mMap.layers().add(new MapEventsReceiver(mMap));

           TileSource tileSource = DefaultSources.OPENSTREETMAP
                   .httpFactory(new OkHttpEngine.OkHttpFactory())
                   .build();
           mMap.layers().add(new BitmapTileLayer(mMap, tileSource));

           mMap.setMapPosition(53.08, 8.83, 1 << 15);

           Bitmap bitmapPoi = CanvasAdapter.decodeBitmap(getClass().getResourceAsStream("/res/marker_poi.png"));
           final MarkerSymbol symbol;
           if (BILLBOARDS)
               symbol = new MarkerSymbol(bitmapPoi, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
           else
               symbol = new MarkerSymbol(bitmapPoi, MarkerSymbol.HotspotPlace.CENTER, false);

           Bitmap bitmapFocus = CanvasAdapter.decodeBitmap(getClass().getResourceAsStream("/res/marker_focus.png"));
           if (BILLBOARDS)
               mFocusMarker = new MarkerSymbol(bitmapFocus, HotspotPlace.BOTTOM_CENTER);
           else
               mFocusMarker = new MarkerSymbol(bitmapFocus, HotspotPlace.CENTER, false);

           
           MarkerRendererFactory markerRendererFactory = new MarkerRendererFactory() {
               @Override
               public MarkerRenderer create(MarkerLayer markerLayer) {
                   return new ClusterMarkerRenderer(markerLayer, symbol, new ClusterMarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE)) {
                       @Override
                       protected Bitmap getClusterBitmap(int size) {
                           // Can customize cluster bitmap here
                           return super.getClusterBitmap(size);
                       }
                   };
               }
           };
           
           mMarkerLayer = new ItemizedLayer<>(
                   mMap,
                   new ArrayList<MarkerItem>(),
                   markerRendererFactory,
                   this);
           mMap.layers().add(mMarkerLayer);

           // Create some markers spaced STEP degrees
           List<MarkerItem> pts = new ArrayList<>();
           GeoPoint center = mMap.getMapPosition().getGeoPoint();
           for (int x = -COUNT; x < COUNT; x++) {
               for (int y = -COUNT; y < COUNT; y++) {
                   double random = STEP * Math.random() * 2;
                   MarkerItem item = new MarkerItem(y + ", " + x, "Title " + center.getLatitude() + "/" + center.getLongitude(),"Description "  + x + "/" + y,
                           new GeoPoint(center.getLatitude() + y * STEP + random, center.getLongitude() + x * STEP + random)
                   );
                   pts.add(item);
               }
           }
           mMarkerLayer.addItems(pts);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   @Override
   public boolean onItemSingleTapUp(int index, MarkerItem item) {
       if (item.getMarker() == null)
           item.setMarker(mFocusMarker);
       else
           item.setMarker(null);

       System.out.println("Marker tap " + item.getTitle());
       return true;
   }

   @Override
   public boolean onItemLongPress(int index, MarkerItem item) {
       if (item.getMarker() == null)
           item.setMarker(mFocusMarker);
       else
           item.setMarker(null);

       System.out.println("Marker long press " + item.getTitle());
       return true;
   }
   
   public static void main(String[] args) {
       GdxMapApp.init();
       GdxMapApp.run(new BookmarkLayer());
   }
   
   class MapEventsReceiver extends Layer implements GestureListener {

      MapEventsReceiver(Map map) {
          super(map);
      }

      @Override
      public boolean onGesture(Gesture g, MotionEvent e) {
          if (g instanceof Gesture.Tap) {
              GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
              System.out.println("Map tap " + p);
              return true;
          }
          if (g instanceof Gesture.LongPress) {
              GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
              System.out.println("Map long press " + p);
              return true;
          }
          if (g instanceof Gesture.TripleTap) {
              GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
              System.out.println("Map triple tap " + p);
              return true;
          }
          return false;
      }
  } 
   
   
}
