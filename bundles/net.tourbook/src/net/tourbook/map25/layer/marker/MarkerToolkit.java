package net.tourbook.map25.layer.marker;

import java.util.ArrayList;
import java.util.List;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ClusterMarkerRenderer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerRendererFactory;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.marker.MarkerSymbol.HotspotPlace;

import net.tourbook.map.bookmark.MapBookmark;



public class MarkerToolkit {
   ItemizedLayer<MarkerItem> mMarkerLayer;
   private int _fgColor = 0xFF000000; // 100 percent black. AARRGGBB
   private int _bgColor = 0x80FF69B4; // 50 percent pink. AARRGGBB
   public MarkerSymbol _symbol;

   private Bitmap _bitmapPoi;
   private int _DefaultIconSize = 20;
   final Paint _fillPainter = CanvasAdapter.newPaint();
   public MarkerRendererFactory _markerRendererFactory;
   public static int shape_star = 0;
   public static int shape_circle = 1;
   
   
   public MarkerToolkit(int shape) {
      System.out.println("*** Markertoolkit:  entering constructor"); //$NON-NLS-1$
      _fillPainter.setStyle(Paint.Style.FILL);
      
      _bitmapPoi = CanvasAdapter.newBitmap(_DefaultIconSize, _DefaultIconSize, 0);
      org.oscim.backend.canvas.Canvas defaultMarkerCanvas = CanvasAdapter.newCanvas();  
      defaultMarkerCanvas.setBitmap(_bitmapPoi);
      float half = _DefaultIconSize/2;
      if(shape == shape_circle) {
         _fillPainter.setColor(0xFFFF69B4); // 100percent pink
         defaultMarkerCanvas.drawCircle(half, half, half, _fillPainter);
      } else {
         /**
          * link: https://stackoverflow.com/questions/16327588/how-to-make-star-shape-in-java
          */
         _fillPainter.setColor(0xFFFFFF00); // 100percent yellow
         _fillPainter.setStrokeWidth(2);
         defaultMarkerCanvas.drawLine(half * 0.1f  , half * 0.65f, half * 1.9f  , half * 0.65f, _fillPainter);
         defaultMarkerCanvas.drawLine(half * 1.9f , half * 0.65f , half * 0.40f , half * 1.65f, _fillPainter);
         defaultMarkerCanvas.drawLine(half * 0.40f , half * 1.65f, half         ,   0         , _fillPainter);
         defaultMarkerCanvas.drawLine(half         ,   0         , half * 1.60f , half * 1.65f, _fillPainter);
         defaultMarkerCanvas.drawLine(half * 1.60f , half * 1.65f, half * 0.1f  , half * 0.65f, _fillPainter);
      }
      
      _symbol = new MarkerSymbol(_bitmapPoi, MarkerSymbol.HotspotPlace.CENTER, false);
      
      _markerRendererFactory = new MarkerRendererFactory() {
         @Override
         public org.oscim.layers.marker.MarkerRenderer create(org.oscim.layers.marker.MarkerLayer markerLayer) {
             return new ClusterMarkerRenderer(markerLayer, _symbol, new ClusterMarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE)) {
                 @Override
                 protected Bitmap getClusterBitmap(int size) {
                     // Can customize cluster bitmap here
                     return super.getClusterBitmap(size);
                 }
             };
         }
     };
     

     
   }
   


   public List<MarkerItem> createMarkerItemList(){
      List<MarkerItem> pts = new ArrayList<>();
      for (final MapBookmark mapBookmark : net.tourbook.map.bookmark.MapBookmarkManager.getAllBookmarks()) {
         System.out.println("*** Markertoolkit:  mapbookmark name: " + mapBookmark.name); //$NON-NLS-1$
              MarkerItem item = new MarkerItem(mapBookmark.id, mapBookmark.name, "",
                      new GeoPoint(mapBookmark.getLatitude(), mapBookmark.getLongitude())
              );
            item.setMarker(createAdvanceSymbol(item, _bitmapPoi));
              pts.add(item);
      }
    return pts;  
   }

   public List<MarkerItem> createDemoMarkerItemList(){
      int COUNT = 5;
      float STEP = 100f / 110000f; // roughly 100 meters
      // Create some markers spaced STEP degrees
      //Berlin: 52.513452, 13.363791
      double berlin_lat = 52.513452;
      double berlin_lon = 13.363791;
      List<MarkerItem> pts = new ArrayList<>();
      for (int x = -COUNT; x < COUNT; x++) {
          for (int y = -COUNT; y < COUNT; y++) {
              double random = STEP * Math.random() * 2;
              MarkerItem item = new MarkerItem(y + ", " + x, "Title " + berlin_lat + "/" + berlin_lon,"#Description "  + x + "/" + y,
                      new GeoPoint(berlin_lat + y * STEP + random, berlin_lon + x * STEP + random)
              );
            item.setMarker(createAdvanceSymbol(item, _bitmapPoi));
              pts.add(item);
          }
      } 
    return pts;  
   }
   
   /**
    * creates a transparent symbol with text and description.
    * @param mItem  -> the MarkerItem to process, containing title and description
    * if description starts with a '#' the first line of the description is drawn. 
    * @param poiBitmap  -> poi bitmap for the center
    * @return MarkerSymbol with title, description and symbol
    * 
    */
   public MarkerSymbol createAdvanceSymbol(MarkerItem mItem, Bitmap poiBitmap) {
      
      final Paint textPainter = CanvasAdapter.newPaint();
      textPainter.setStyle(Paint.Style.STROKE);
      textPainter.setColor(_fgColor);
      
      final Paint fillPainter = CanvasAdapter.newPaint();
      fillPainter.setStyle(Paint.Style.FILL);
      fillPainter.setColor(_bgColor);
      
      int margin = 3;
      int dist2symbol = 30;
      
      int titleWidth  = ((int) textPainter.getTextWidth(mItem.title) + 2 * margin);
      int titleHeight = ((int) textPainter.getTextHeight(mItem.title) + 2 * margin);

      int symbolWidth = poiBitmap.getWidth();
      
      int subtitleWidth = 0;
      int subtitleHeight = 0;
      String subtitle ="";
      boolean hasSubtitle = false;
      if (mItem.description.length()>1) {
         if (mItem.description.startsWith("#")){
            subtitle = mItem.description.substring(1); // not the first # char
            subtitle = subtitle.split("\\R", 2)[0]; // only first line
            subtitleWidth  = ((int) textPainter.getTextWidth(subtitle)) + 2 * margin;
            subtitleHeight = ((int) textPainter.getTextHeight(subtitle)) + 2 * margin;
            hasSubtitle = true;
         }
      }
      
      int xSize = java.lang.Math.max(titleWidth, subtitleWidth);
      xSize = java.lang.Math.max(xSize, symbolWidth);   
      
      int ySize = titleHeight + symbolWidth + dist2symbol;
      
      // markerCanvas, the drawing area for all: title, description and symbol
      Bitmap markerBitmap = CanvasAdapter.newBitmap(xSize, ySize, 0);
      org.oscim.backend.canvas.Canvas markerCanvas = CanvasAdapter.newCanvas();  
      markerCanvas.setBitmap(markerBitmap);
      
      //titleCanvas for the title text
      Bitmap titleBitmap = CanvasAdapter.newBitmap( titleWidth + margin, titleHeight + margin, 0);
      org.oscim.backend.canvas.Canvas titleCanvas = CanvasAdapter.newCanvas();
      titleCanvas.setBitmap(titleBitmap);
      
      { // testing block
      /*
       * the following three lines displaying a transparent box.
       * only for testing purposes, normally uncommented
       */
      //fillPainter.setColor(0x60ffffff);
      //markerCanvas.drawCircle(0, 0, xSize*2, fillPainter);
      //fillPainter.setColor(_bgColor);
      }
      
      // draw an oversized transparent circle, so the canvas is completely filled with a transparent color
      // titleCanvas.fillRectangle() does not support transparency
      titleCanvas.drawCircle(0, 0, xSize*2, fillPainter);
      
      titleCanvas.drawText(mItem.title, margin, titleHeight - margin , textPainter);
      
      if (hasSubtitle) {
         Bitmap subtitleBitmap = CanvasAdapter.newBitmap( subtitleWidth + margin, subtitleHeight + margin, 0);
         org.oscim.backend.canvas.Canvas subtitleCanvas = CanvasAdapter.newCanvas();
         subtitleCanvas.setBitmap(subtitleBitmap); 
         subtitleCanvas.drawCircle(0, 0, xSize*2, fillPainter);
         subtitleCanvas.drawText(subtitle, margin, titleHeight - margin, textPainter);
         markerCanvas.drawBitmap(subtitleBitmap, xSize/2-(subtitleWidth/2), ySize - (subtitleHeight + margin));
      } else {
         ;
      }    
      
      markerCanvas.drawBitmap(titleBitmap, xSize/2-(titleWidth/2), 0);
      markerCanvas.drawBitmap(poiBitmap, xSize/2-(symbolWidth/2), ySize/2-(symbolWidth/2));
      
      return (new MarkerSymbol(markerBitmap, HotspotPlace.CENTER, true));
   }
   
   
   
}
