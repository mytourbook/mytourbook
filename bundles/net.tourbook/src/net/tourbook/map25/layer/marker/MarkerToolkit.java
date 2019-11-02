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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

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

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25ConfigManager;
//import net.tourbook.map25.Map25App.DebugMode;


public class MarkerToolkit {
   //ItemizedLayer<MarkerItem> mMarkerLayer;
   protected int _fgColor = 0xFF000000; // 100 percent black. AARRGGBB
   protected int _bgColor = 0x80FF69B4; // 50 percent pink. AARRGGBB
   protected int _clusterSymbolSizeDP = net.tourbook.map25.layer.marker.MarkerRenderer.MAP_MARKER_CLUSTER_SIZE_DP;
   protected int _clusterForegroundColor = net.tourbook.map25.layer.marker.MarkerRenderer.CLUSTER_COLOR_TEXT;
   protected int _clusterBackgroundColor = net.tourbook.map25.layer.marker.MarkerRenderer.CLUSTER_COLOR_BACK;
   
   private int  _clusterSymbolWeight;
   private float  _clusterOutlineSize;
   private Bitmap _bitmapCluster;
   //private boolean _isBillboard;
   
   public MarkerSymbol _symbol;  //marker symbol circle or star
   protected float _symbolSize = 10f;
   protected int _symbolSizeInt = 10;
   protected int _clusterSymbol_Size;

   private Bitmap _bitmapPoi;
   private Bitmap _bitmapStar;
   private Bitmap _bitmapCircle;
   private Bitmap _BitmapClusterSymbol;
   

   protected Paint _fillPainter = CanvasAdapter.newPaint();
   protected Paint _linePainter = CanvasAdapter.newPaint();

   public MarkerRendererFactory _markerRendererFactory;
   
   
   
   //public enum MarkerShape {STAR, CIRCLE};
   
   //public enum MarkerMode {DEMO, NORMAL};
   
   public boolean _isMarkerClusteredLast;
   
   public MarkerToolkit(MarkerShape shape) {
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      loadConfig();
      //_mapApp.debugPrint("*** Markertoolkit:  entering constructor"); //$NON-NLS-1$

      _fillPainter.setStyle(Paint.Style.FILL);
      
      _linePainter.setStyle(Paint.Style.STROKE);
      _linePainter.setStrokeWidth(4);
      
      _bitmapCluster = createClusterBitmap(1);
      
      _bitmapPoi = createPoiBitmap(shape);
      
      if(shape == MarkerShape.CIRCLE) {
         _BitmapClusterSymbol = drawCircle(_clusterSymbol_Size);
      } else {
         _BitmapClusterSymbol = drawStar(_clusterSymbol_Size);
      }
      
      _symbol = new MarkerSymbol(_bitmapPoi, MarkerSymbol.HotspotPlace.CENTER, false);
      
      _isMarkerClusteredLast = config.isMarkerClustered;
      
      _markerRendererFactory = new MarkerRendererFactory() {
         @Override
         public org.oscim.layers.marker.MarkerRenderer create(org.oscim.layers.marker.MarkerLayer markerLayer) {
            return new ClusterMarkerRenderer(markerLayer, _symbol, new ClusterMarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE)) {
               @Override
               protected Bitmap getClusterBitmap(int size) {
                  // Can customize cluster bitmap here
                  //_mapApp.debugPrint("*** Markertoolkit:  cluster size: " + size); //$NON-NLS-1$
                  _bitmapCluster = createClusterBitmap(size);
                  return _bitmapCluster;
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
      
      _clusterSymbol_Size = config.clusterSymbol_Size;
      
      //_mapApp.debugPrint("*** Markertoolkit:  fillradius for star: " + config.clusterSymbol_Size + " " + config.clusterSymbol_Weight); //$NON-NLS-1$
      //_mapApp.debugPrint("*** Markertoolkit:  _clusterOutlineSize for star: " + _clusterOutlineSize + " , _clusterSymbol_Size: " + _clusterSymbol_Size); //$NON-NLS-1$

   }

   public Bitmap createPoiBitmap(MarkerShape shape) {
      loadConfig();

      _bitmapPoi = CanvasAdapter.newBitmap(_symbolSizeInt, _symbolSizeInt, 0);

      if(shape == MarkerShape.CIRCLE) {
         _bitmapPoi = drawCircle(_symbolSizeInt);
      } else {
         _bitmapPoi = drawStar(_symbolSizeInt);
      }
     return _bitmapPoi;
      
   }
   
   public Bitmap drawCircle(int bitmapCircleSize) {
      _bitmapCircle = CanvasAdapter.newBitmap(bitmapCircleSize, bitmapCircleSize, 0);
      org.oscim.backend.canvas.Canvas defaultMarkerCanvas = CanvasAdapter.newCanvas();
      defaultMarkerCanvas.setBitmap(_bitmapCircle);
      float half = bitmapCircleSize/2;
      _linePainter.setColor(0xA0000000); //gray like the PhotoSymbol in the UI
      _linePainter.setStrokeWidth(2);
      defaultMarkerCanvas.drawCircle(half, half, half * 0.8f, _linePainter);
      return _bitmapCircle;
   }
   
   public Bitmap drawStar(int bitmapStarSize) {
      //_mapApp.debugPrint("*** Markertoolkit:  drawstar: "); //$NON-NLS-1$
      _bitmapStar = CanvasAdapter.newBitmap(bitmapStarSize, bitmapStarSize, 0);
      org.oscim.backend.canvas.Canvas defaultMarkerCanvas = CanvasAdapter.newCanvas();
      defaultMarkerCanvas.setBitmap(_bitmapStar);
      float half = bitmapStarSize/2;
      _fillPainter.setColor(0xFFFFFF00); // 100percent yellow
      _fillPainter.setStrokeWidth(2);
      /*
       * link: https://stackoverflow.com/questions/16327588/how-to-make-star-shape-in-java
       */
      defaultMarkerCanvas.drawLine(half * 0.1f  , half * 0.65f, half * 1.9f  , half * 0.65f, _fillPainter);
      defaultMarkerCanvas.drawLine(half * 1.9f , half * 0.65f , half * 0.40f , half * 1.65f, _fillPainter);
      defaultMarkerCanvas.drawLine(half * 0.40f , half * 1.65f, half         ,   0         , _fillPainter);
      defaultMarkerCanvas.drawLine(half         ,   0         , half * 1.60f , half * 1.65f, _fillPainter);
      defaultMarkerCanvas.drawLine(half * 1.60f , half * 1.65f, half * 0.1f  , half * 0.65f, _fillPainter);
      return _bitmapStar;
   }
   
   /**
    * this creates the bitmap for clustering a draw the size as text in the middle
    * @param size 
    * @return
    */
   public Bitmap createClusterBitmap(int size) {
      
      final ScreenUtils.ClusterDrawable drawable = new ScreenUtils.ClusterDrawable(
            _clusterSymbolSizeDP,
            _clusterForegroundColor,
            _clusterBackgroundColor,
            Integer.toString(size),
            _clusterSymbolWeight,
            _clusterOutlineSize);

      final Bitmap paintedBitmap = drawable.getBitmap(_BitmapClusterSymbol);
      return paintedBitmap;
   }
   
   
   public List<MarkerItem> createMarkerItemList(MarkerMode MarkerMode){
      loadConfig();
      createPoiBitmap(MarkerShape.STAR);
      _BitmapClusterSymbol = drawStar(_clusterSymbol_Size);
      List<MarkerItem> pts = new ArrayList<>();
     
      for (final MapBookmark mapBookmark : net.tourbook.map.bookmark.MapBookmarkManager.getAllBookmarks()) {
         //_mapApp.debugPrint("*** Markertoolkit:  mapbookmark name: " + mapBookmark.name); //$NON-NLS-1$
         MarkerItem item = new MarkerItem(mapBookmark.id, mapBookmark.name, "", //$NON-NLS-1$
               new GeoPoint(mapBookmark.getLatitude(), mapBookmark.getLongitude())
               );
         item.setMarker(createAdvanceSymbol(item, _bitmapPoi, false));
         pts.add(item);
      }

      if (MarkerMode == MarkerMode.NORMAL) {return pts;};

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
            MarkerItem item = new MarkerItem(y + ", " + x, "Title " + demo_lat + "/" + demo_lon,"Description "  + x + "/" + y, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                  new GeoPoint(demo_lat + y * STEP + random, demo_lon + x * STEP + random)
                  );
            item.setMarker(createAdvanceSymbol(item, _bitmapPoi, false));
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
   public MarkerSymbol createAdvanceSymbol(MarkerItem mItem, Bitmap poiBitmap, Boolean isPhoto) {
      loadConfig();
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      final boolean isBillboard = config.markerOrientation == Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD;
      createPoiBitmap(MarkerShape.STAR);
      final Paint textPainter = CanvasAdapter.newPaint();
      textPainter.setStyle(Paint.Style.STROKE);
      textPainter.setColor(_fgColor);
      
      final Paint fillPainter = CanvasAdapter.newPaint();
      fillPainter.setStyle(Paint.Style.FILL);
      fillPainter.setColor(_bgColor);
      
      int margin = 3;
      int dist2symbol = 30;
      
      //int titleWidth  = ((int) textPainter.getTextWidth(mItem.title) + 2 * margin);
      //int titleHeight = ((int) textPainter.getTextHeight(mItem.title) + 2 * margin);
      
      
      Point titleSize = new Point((int) textPainter.getTextWidth(mItem.title) + 2 * margin, (int) textPainter.getTextHeight(mItem.title) + 2 * margin);
      Point symbolSize = new Point(poiBitmap.getWidth(),poiBitmap.getHeight());
      Point subtitleSize = new Point();
      Point size = new Point();  //total  size of all elements
      //int symbolWidth = poiBitmap.getWidth();
      
      //int subtitleWidth = 0;
      //int subtitleHeight = 0;
      String subtitle =""; //$NON-NLS-1$
      boolean hasSubtitle = false;
      if (mItem.description.length()>1) {
         if (mItem.description.startsWith("#")){ //$NON-NLS-1$
            subtitle = mItem.description.substring(1); // not the first # char
            subtitle = subtitle.split("\\R", 2)[0]; // only first line //$NON-NLS-1$
            subtitleSize.x  = ((int) textPainter.getTextWidth(subtitle)) + 2 * margin;
            subtitleSize.y = ((int) textPainter.getTextHeight(subtitle)) + 2 * margin;
            hasSubtitle = true;
         }
      }
      
      size.x = java.lang.Math.max(titleSize.x, subtitleSize.x);
      size.x = java.lang.Math.max(size.x, symbolSize.x);   
      
      size.y = titleSize.y + symbolSize.y + dist2symbol;
      
      // markerCanvas, the drawing area for all: title, description and symbol
      Bitmap markerBitmap = CanvasAdapter.newBitmap(size.x, size.y, 0);
      org.oscim.backend.canvas.Canvas markerCanvas = CanvasAdapter.newCanvas();  
      markerCanvas.setBitmap(markerBitmap);
      
      //titleCanvas for the title text
      Bitmap titleBitmap = CanvasAdapter.newBitmap( titleSize.x + margin, titleSize.y + margin, 0);
      org.oscim.backend.canvas.Canvas titleCanvas = CanvasAdapter.newCanvas();
      titleCanvas.setBitmap(titleBitmap);
      
      { // testing block
      /*
       * the following three lines displaying a transparent box.
       * only for testing purposes, normally uncommented
       */
      //fillPainter.setColor(0x60ffffff);
      //markerCanvas.drawCircle(0, 0, size.x*2, fillPainter);
      //fillPainter.setColor(_bgColor);
      }
      
      // draw an oversized transparent circle, so the canvas is completely filled with a transparent color
      // titleCanvas.fillRectangle() does not support transparency
      titleCanvas.drawCircle(0, 0, size.x*2, fillPainter);

      titleCanvas.drawText(mItem.title, margin, titleSize.y - margin , textPainter);
      
      if (hasSubtitle) {
         Bitmap subtitleBitmap = CanvasAdapter.newBitmap( subtitleSize.x + margin, subtitleSize.y + margin, 0);
         org.oscim.backend.canvas.Canvas subtitleCanvas = CanvasAdapter.newCanvas();
         subtitleCanvas.setBitmap(subtitleBitmap); 
         subtitleCanvas.drawCircle(0, 0, size.x*2, fillPainter);
         subtitleCanvas.drawText(subtitle, margin, titleSize.y - margin, textPainter);
         markerCanvas.drawBitmap(subtitleBitmap, size.x/2-(subtitleSize.x/2), size.y - (subtitleSize.y + margin));
      } else if (isPhoto){
         int lineLength = 20;
         textPainter.setStrokeWidth(2);
         Bitmap subtitleBitmap = CanvasAdapter.newBitmap( lineLength, lineLength, 0); //heigth as title
         org.oscim.backend.canvas.Canvas subtitleCanvas = CanvasAdapter.newCanvas();
         subtitleCanvas.setBitmap(subtitleBitmap);
         subtitleCanvas.drawLine(lineLength/2, 0, lineLength/2, lineLength, textPainter);
         markerCanvas.drawBitmap(subtitleBitmap, size.x/2-(lineLength / 2), size.y - lineLength);
      }    
      
      if (config.isShowTourMarker) {
         markerCanvas.drawBitmap(titleBitmap, size.x/2-(titleSize.x/2), 0);
      }
      
      markerCanvas.drawBitmap(poiBitmap, size.x/2-(symbolSize.x/2), size.y/2-(symbolSize.y/2));
      
      if (isPhoto) {
         return (new MarkerSymbol(markerBitmap, HotspotPlace.BOTTOM_CENTER));
      }
      
      if (isBillboard) {
         return (new MarkerSymbol(markerBitmap, HotspotPlace.CENTER));
      } else {
         return (new MarkerSymbol(markerBitmap, HotspotPlace.CENTER, false));
      }

   }
   
   public void debugPrint(String debugText) {
      net.tourbook.map25.Map25App.debugPrint(debugText);
   }
   
}