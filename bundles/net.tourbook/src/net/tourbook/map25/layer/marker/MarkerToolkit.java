/*******************************************************************************
 * Copyright (C) 2019 Wolfgang Schramm and Contributors
 * Copyright 2016-2018 devemux86
 * Copyright 2017 nebular
 * Copyright 2019 Thomas Theussing
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

import net.tourbook.common.color.ColorUtil;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map25.Map25ConfigManager;


public class MarkerToolkit {
   //ItemizedLayer<MarkerItem> mMarkerLayer;
   private int _fgColor = 0xFF000000; // 100 percent black. AARRGGBB
   private int _bgColor = 0x80FF69B4; // 50 percent pink. AARRGGBB
   private int _clusterSymbolSizeDP = net.tourbook.map25.layer.marker.MarkerRenderer.MAP_MARKER_CLUSTER_SIZE_DP;
   private int _clusterForegroundColor = net.tourbook.map25.layer.marker.MarkerRenderer.CLUSTER_COLOR_TEXT;
   private int _clusterBackgroundColor = net.tourbook.map25.layer.marker.MarkerRenderer.CLUSTER_COLOR_BACK;
   private int  _clusterSymbolWeight;
   private float  _clusterOutlineSize;
   private Bitmap _clusterBitmap;
   
   public MarkerSymbol _symbol;
   private float _symbolSize = 10f;
   private int _symbolSizeInt = 10;

   private Bitmap _bitmapPoi;

   final Paint _fillPainter = CanvasAdapter.newPaint();

   public MarkerRendererFactory _markerRendererFactory;
   
   public static int shape_star = 0;
   public static int shape_circle = 1;
   
   public static int modeDemo = 0;
   public static int modeNormal = 1;
   
   public MarkerToolkit(int shape) {
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      loadConfig();
      //System.out.println("*** Markertoolkit:  entering constructor"); //$NON-NLS-1$

      _fillPainter.setStyle(Paint.Style.FILL);
      
      _clusterBitmap = createClusterBitmap(1);
      
      _bitmapPoi = createPoiBitmap(shape_star);
      
      _symbol = new MarkerSymbol(_bitmapPoi, MarkerSymbol.HotspotPlace.CENTER, false);
      
      
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
   
   public void loadConfig () {
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      _fgColor = ColorUtil.getARGB(config.markerOutline_Color, (int) (config.markerOutline_Opacity / 100.0 * 0xff));
      _bgColor = ColorUtil.getARGB(config.markerFill_Color,    (int) (config.markerFill_Opacity    / 100.0 * 0xff));
      _clusterSymbolSizeDP = config.clusterSymbol_Size;
      _clusterForegroundColor = ColorUtil.getARGB(
            config.clusterOutline_Color,
            (int) (config.clusterOutline_Opacity / 100.0 * 0xff));
      _clusterBackgroundColor = ColorUtil.getARGB(
            config.clusterFill_Color,
            (int) (config.clusterFill_Opacity / 100.0 * 0xff));
      _clusterSymbolWeight = config.clusterSymbol_Weight;
      _clusterOutlineSize = config.clusterOutline_Size;
      _symbolSize = ScreenUtils.getPixels(config.markerSymbol_Size);
      _symbolSizeInt = (int) Math.ceil(_symbolSize);
   }

   public Bitmap createPoiBitmap(int shape) {
      loadConfig();

      _bitmapPoi = CanvasAdapter.newBitmap(_symbolSizeInt, _symbolSizeInt, 0);

      org.oscim.backend.canvas.Canvas defaultMarkerCanvas = CanvasAdapter.newCanvas();  
      defaultMarkerCanvas.setBitmap(_bitmapPoi);
      float half = _symbolSizeInt/2;
      
      if(shape == shape_circle) {
         _fillPainter.setColor(0x80FF69B4); // 50percent pink
         defaultMarkerCanvas.drawCircle(half, half, half, _fillPainter);
      } else {
         /*
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
     return _bitmapPoi;
      
   }
   
   public Bitmap createClusterBitmap(int size) {
      //final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      final ScreenUtils.ClusterDrawable drawable = new ScreenUtils.ClusterDrawable(
            _clusterSymbolSizeDP,
            _clusterForegroundColor,
            _clusterBackgroundColor,
            Integer.toString(size),
            _clusterSymbolWeight,
            _clusterOutlineSize);
      final Bitmap paintedBitmap = drawable.getBitmap();
      return paintedBitmap;
   }
   
   
   public List<MarkerItem> createMarkerItemList(int mode){
      loadConfig();
      createPoiBitmap(shape_star);
      List<MarkerItem> pts = new ArrayList<>();
     
      for (final MapBookmark mapBookmark : net.tourbook.map.bookmark.MapBookmarkManager.getAllBookmarks()) {
         //System.out.println("*** Markertoolkit:  mapbookmark name: " + mapBookmark.name); //$NON-NLS-1$
         MarkerItem item = new MarkerItem(mapBookmark.id, mapBookmark.name, "",
               new GeoPoint(mapBookmark.getLatitude(), mapBookmark.getLongitude())
               );
         item.setMarker(createAdvanceSymbol(item, _bitmapPoi));
         pts.add(item);
      }

      if (mode == modeNormal) {return pts;};

      int COUNT = 5;
      float STEP = 100f / 110000f; // roughly 100 meters
      // Create some markers spaced STEP degrees
      //Berlin: 52.513452, 13.363791
      //Rapperswil  47.2266239, 8.8184374
      double demo_lat = 47.2266239;
      double demo_lon = 8.8184374;
      //List<MarkerItem> pts = new ArrayList<>();
      for (int x = -COUNT; x < COUNT; x++) {
         for (int y = -COUNT; y < COUNT; y++) {
            double random = STEP * Math.random() * 2;
            MarkerItem item = new MarkerItem(y + ", " + x, "Title " + demo_lat + "/" + demo_lon,"Description "  + x + "/" + y,
                  new GeoPoint(demo_lat + y * STEP + random, demo_lon + x * STEP + random)
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
      loadConfig();
      createPoiBitmap(shape_star);
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
