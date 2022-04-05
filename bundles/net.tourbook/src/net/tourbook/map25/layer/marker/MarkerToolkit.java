/*******************************************************************************
 * Copyright (C) 2019, 2022 Wolfgang Schramm and Contributors
 * Copyright 2016-2018 devemux86
 * Copyright 2017 nebular
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
import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.marker.cluster.ClusterMarkerRenderer;

import org.eclipse.swt.internal.DPIUtil;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerRendererFactory;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.marker.MarkerSymbol.HotspotPlace;

public class MarkerToolkit implements ItemizedLayer.OnItemGestureListener<MarkerInterface> {

   private int                  _poiColor    = 0xFFFFFF00;              // yellow is better to see                                                             // 100percent yellow

   public MarkerSymbol          _symbol;                                //marker symbol circle or star

   private Bitmap               _bitmapPoi;
   private Bitmap               _bitmapStar;
   private Bitmap               _bitmapCircle;
   private Bitmap               _bitmapClusterSymbol;

   protected Paint              _fillPainter = CanvasAdapter.newPaint();
   private Paint                _linePainter = CanvasAdapter.newPaint();

   public MarkerRendererFactory _markerRendererFactory;

   public boolean               _isMarkerClusteredLast;

   public MarkerToolkit(final MarkerShape shape) {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final int clusterSymbol_Size = config.clusterSymbol_Size;

      //_mapApp.debugPrint("*** Markertoolkit:  entering constructor"); //$NON-NLS-1$
      _fillPainter.setStyle(Paint.Style.FILL);
      _linePainter.setStyle(Paint.Style.STROKE);
      _linePainter.setStrokeWidth(4);
      _bitmapPoi = createPoiBitmap(shape);

      if (shape == MarkerShape.CIRCLE) {
         _bitmapClusterSymbol = drawCircle(clusterSymbol_Size);
      } else {
         _bitmapClusterSymbol = drawStar(clusterSymbol_Size, _poiColor);
      }

      _symbol = new MarkerSymbol(_bitmapPoi, MarkerSymbol.HotspotPlace.CENTER, false);

      _isMarkerClusteredLast = config.isMarkerClustered;

      _markerRendererFactory = new MarkerRendererFactory() {
         @Override
         public org.oscim.layers.marker.MarkerRenderer create(final org.oscim.layers.marker.MarkerLayer markerLayer) {

            return new ClusterMarkerRenderer(markerLayer, _symbol, new ClusterMarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE)) {
               @Override
               protected Bitmap getClusterBitmap(final int size) {

                  return createClusterBitmap(size);
               }
            };
         }
      };
   }

   /**
    * creates a transparent symbol with text and description. for marker.
    * short version of createAdvanceSymbol(final MarkerItem mItem, final Bitmap poiBitmap, false,
    * false)
    *
    * @param mItem
    * @param poiBitmap
    * @return
    */
   public MarkerSymbol createAdvanceSymbol(final MarkerItem mItem, final Bitmap poiBitmap) {
      return createAdvanceSymbol(mItem, poiBitmap, false, false);
   }

   /**
    * Creates a transparent symbol with text and description. for photos and marker.
    *
    * @param mItem
    *           -> the MarkerItem to process, containing title and description
    *           if description starts with a '#' the first line of the description is drawn.
    * @param poiBitmap
    *           -> poi bitmap for the center
    * @param isPhoto
    *           -> called from PhotoToolkit yes/no
    * @param showPhotoTitle
    *           -> when isPhoto = true show photo title on/off. when isPhoto = false, this param is
    *           ignored
    * @return MarkerSymbol with title, description and symbol
    */
   MarkerSymbol createAdvanceSymbol(final MarkerItem mItem,
                                    final Bitmap poiBitmap,
                                    final Boolean isPhoto,
                                    final boolean showPhotoTitle) {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      final int markerForegroundColor = ColorUtil.getARGB(config.markerOutline_Color, config.markerOutline_Opacity);
      final int markerBackgroundColor = ColorUtil.getARGB(config.markerFill_Color, config.markerFill_Opacity);

      final boolean isBillboard = config.markerOrientation == Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD;

      createPoiBitmap(MarkerShape.STAR);

      final Paint textPainter = CanvasAdapter.newPaint();
      textPainter.setStyle(Paint.Style.STROKE);
      textPainter.setColor(markerForegroundColor);

      // adjust font to 4k display, otherwise it is really small
      final float fontHeight = textPainter.getFontHeight();
      final float scaledFontHeight = DPIUtil.autoScaleUp(fontHeight);
      textPainter.setTextSize(scaledFontHeight);

      final Paint fillPainter = CanvasAdapter.newPaint();
      fillPainter.setStyle(Paint.Style.FILL);
      fillPainter.setColor(markerBackgroundColor);

      final int margin = 3;
      final int dist2symbol = 30;

      final Point titleSize = new Point(
            (int) textPainter.getTextWidth(mItem.title) + 2 * margin,
            (int) textPainter.getTextHeight(mItem.title) + 2 * margin);

      final Point symbolSize = new Point(poiBitmap.getWidth(), poiBitmap.getHeight());
      final Point subtitleSize = new Point();
      final Point size = new Point(); //total  size of all elements

      String subtitle = UI.EMPTY_STRING;
      boolean hasSubtitle = false;
      if (mItem.description.length() > 1) {
         if (mItem.description.startsWith("#")) { //$NON-NLS-1$
            subtitle = mItem.description.substring(1); // not the first # char
            subtitle = subtitle.split("\\R", 2)[0]; // only first line //$NON-NLS-1$
            subtitleSize.x = ((int) textPainter.getTextWidth(subtitle)) + 2 * margin;
            subtitleSize.y = ((int) textPainter.getTextHeight(subtitle)) + 2 * margin;
            hasSubtitle = true;
         }
      }

      size.x = java.lang.Math.max(titleSize.x, subtitleSize.x);
      size.x = java.lang.Math.max(size.x, symbolSize.x);

      size.y = titleSize.y + symbolSize.y + dist2symbol;

      // markerCanvas, the drawing area for all: title, description and symbol
      final Bitmap markerBitmap = CanvasAdapter.newBitmap(size.x, size.y, 0);
      final Canvas markerCanvas = CanvasAdapter.newCanvas();
      markerCanvas.setBitmap(markerBitmap);

      //titleCanvas for the title text
      final Bitmap titleBitmap = CanvasAdapter.newBitmap(titleSize.x + margin, titleSize.y + margin, 0);
      final Canvas titleCanvas = CanvasAdapter.newCanvas();
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
      titleCanvas.drawCircle(0, 0, size.x * 2, fillPainter);

      titleCanvas.drawText(mItem.title, margin, titleSize.y - margin, textPainter);

      if (hasSubtitle) {

         final Bitmap subtitleBitmap = CanvasAdapter.newBitmap(subtitleSize.x + margin, subtitleSize.y + margin, 0);
         final Canvas subtitleCanvas = CanvasAdapter.newCanvas();
         subtitleCanvas.setBitmap(subtitleBitmap);
         subtitleCanvas.drawCircle(0, 0, size.x * 2, fillPainter);
         subtitleCanvas.drawText(subtitle, margin, titleSize.y - margin, textPainter);
         markerCanvas.drawBitmap(subtitleBitmap, size.x / 2 - (subtitleSize.x / 2), size.y - (subtitleSize.y + margin));

      } else if (isPhoto) {

         final int lineLength = 20;
         textPainter.setStrokeWidth(2);
         final Bitmap subtitleBitmap = CanvasAdapter.newBitmap(lineLength, lineLength, 0); //heigth as title
         final Canvas subtitleCanvas = CanvasAdapter.newCanvas();
         subtitleCanvas.setBitmap(subtitleBitmap);
         subtitleCanvas.drawLine(lineLength / 2, 0, lineLength / 2, lineLength, textPainter);
         markerCanvas.drawBitmap(subtitleBitmap, size.x / 2 - (lineLength / 2), size.y - lineLength);
      }

      if (isPhoto) {
         if (showPhotoTitle) {
            markerCanvas.drawBitmap(titleBitmap, size.x / 2 - (titleSize.x / 2), 0);
         }
      } else {
         markerCanvas.drawBitmap(titleBitmap, size.x / 2 - (titleSize.x / 2), 0);
      }

      markerCanvas.drawBitmap(poiBitmap, size.x / 2 - (symbolSize.x / 2), size.y / 2 - (symbolSize.y / 2));

      if (isPhoto) {
         return (new MarkerSymbol(markerBitmap, HotspotPlace.BOTTOM_CENTER));
      }

      if (isBillboard) {
         return (new MarkerSymbol(markerBitmap, HotspotPlace.CENTER));
      } else {
         return (new MarkerSymbol(markerBitmap, HotspotPlace.CENTER, false));
      }

   }

   /**
    * this creates the bitmap for clustering a draw the size as text in the middle
    *
    * @param size
    * @return
    */
   public Bitmap createClusterBitmap(final int size) {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final ScreenUtils.ClusterDrawable drawable = new ScreenUtils.ClusterDrawable(

            config.clusterSymbol_Size,
            ColorUtil.getARGB(config.clusterOutline_Color, config.clusterOutline_Opacity),
            ColorUtil.getARGB(config.clusterFill_Color, config.clusterFill_Opacity),
            Integer.toString(size),
            config.clusterSymbol_Weight,
            config.clusterOutline_Size);

      final Bitmap paintedBitmap = drawable.getBitmap(_bitmapClusterSymbol);

      return paintedBitmap;
   }

   public List<MarkerInterface> createMarkerItemList(final MarkerMode markerMode) {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final int clusterSymbol_Size = config.clusterSymbol_Size;

      createPoiBitmap(MarkerShape.STAR);

      _bitmapClusterSymbol = drawStar(clusterSymbol_Size, _poiColor);

      final List<MarkerInterface> pts = new ArrayList<>();

      for (final MapBookmark mapBookmark : net.tourbook.map.bookmark.MapBookmarkManager.getAllBookmarks()) {
         //debugPrint("*** Markertoolkit:  mapbookmark name: " + mapBookmark.name + " lat: " +  mapBookmark.get_mapPositionMarkerLatitude() + " lon: " + mapBookmark.get_mapPositionMarkerLongitude()); //$NON-NLS-1$
         //debugPrint("*** Markertoolkit: " + mapBookmark.toString());
         final MarkerItem item = new MarkerItem(mapBookmark.id,
               mapBookmark.name,
               UI.EMPTY_STRING,
               //new GeoPoint(mapBookmark.getLatitude(), mapBookmark.getLongitude())
               new GeoPoint(mapBookmark.get_mapPositionMarkerLatitude(), mapBookmark.get_mapPositionMarkerLongitude()));
         item.setMarker(createAdvanceSymbol(item, _bitmapPoi));
         pts.add(item);
      }

      if (markerMode == MarkerMode.NORMAL) {
         return pts;
      }

      final int COUNT = 5;
      final float STEP = 100f / 110000f; // roughly 100 meters
      // Create some markers spaced STEP degrees
      //Berlin: 52.513452, 13.363791
      //Rapperswil  47.2266239, 8.8184374
      final double demo_lat = 47.2266239;
      final double demo_lon = 8.8184374;
      //List<MarkerItem> pts = new ArrayList<>();
      for (int x = -COUNT; x < COUNT; x++) {
         for (int y = -COUNT; y < COUNT; y++) {
            final double random = STEP * Math.random() * 2;
            final MarkerItem item = new MarkerItem(y + ", " + x, //$NON-NLS-1$
                  "Title " + demo_lat + "/" + demo_lon, //$NON-NLS-1$//$NON-NLS-2$
                  "Description " + x + "/" + y, //$NON-NLS-1$ //$NON-NLS-2$
                  new GeoPoint(demo_lat + y * STEP + random, demo_lon + x * STEP + random));
            item.setMarker(createAdvanceSymbol(item, _bitmapPoi));
            pts.add(item);
         }
      }

      return pts;
   }

   Bitmap createPoiBitmap(final MarkerShape shape) {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final int _symbolSize = (int) Math.ceil(ScreenUtils.getPixels(config.markerSymbol_Size));

      _bitmapPoi = CanvasAdapter.newBitmap(_symbolSize, _symbolSize, 0);

      if (shape == MarkerShape.CIRCLE) {
         _bitmapPoi = drawCircle(_symbolSize);
      } else {
         _bitmapPoi = drawStar(_symbolSize, _poiColor);
      }
      return _bitmapPoi;

   }

   private void debugPrint(final String debugText) {
      net.tourbook.map25.Map25App.debugPrint(debugText);
   }

   private Bitmap drawCircle(final int bitmapCircleSize) {

      final float half = bitmapCircleSize / 2;

      _bitmapCircle = CanvasAdapter.newBitmap(bitmapCircleSize, bitmapCircleSize, 0);
      final Canvas defaultMarkerCanvas = CanvasAdapter.newCanvas();
      defaultMarkerCanvas.setBitmap(_bitmapCircle);
      _linePainter.setColor(0xA0000000); //gray like the PhotoSymbol in the UI
      _linePainter.setStrokeWidth(2);
      defaultMarkerCanvas.drawCircle(half, half, half * 0.8f, _linePainter);

      return _bitmapCircle;
   }

   private Bitmap drawStar(final int bitmapStarSize, final int starColor) {

      //_mapApp.debugPrint("*** Markertoolkit:  drawstar: "); //$NON-NLS-1$
      _bitmapStar = CanvasAdapter.newBitmap(bitmapStarSize, bitmapStarSize, 0);
      final Canvas defaultMarkerCanvas = CanvasAdapter.newCanvas();
      defaultMarkerCanvas.setBitmap(_bitmapStar);
      final float half = bitmapStarSize / 2;
      _fillPainter.setColor(starColor);
      _fillPainter.setStrokeWidth(2);

      /*
       * link: https://stackoverflow.com/questions/16327588/how-to-make-star-shape-in-java
       */
      defaultMarkerCanvas.drawLine(half * 0.1f, half * 0.65f, half * 1.9f, half * 0.65f, _fillPainter);
      defaultMarkerCanvas.drawLine(half * 1.9f, half * 0.65f, half * 0.40f, half * 1.65f, _fillPainter);
      defaultMarkerCanvas.drawLine(half * 0.40f, half * 1.65f, half, 0, _fillPainter);
      defaultMarkerCanvas.drawLine(half, 0, half * 1.60f, half * 1.65f, _fillPainter);
      defaultMarkerCanvas.drawLine(half * 1.60f, half * 1.65f, half * 0.1f, half * 0.65f, _fillPainter);

      return _bitmapStar;
   }

   public Bitmap drawTrackArrow(final int bitmapArrowSize, final int arrowColor) {

      //final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();
      final Bitmap bitmapTrackArrow = CanvasAdapter.newBitmap(bitmapArrowSize, bitmapArrowSize, 0);
      final float bitmapArrowSizeF = bitmapArrowSize - 1;
      final Canvas defaultMarkerCanvas = CanvasAdapter.newCanvas();
      defaultMarkerCanvas.setBitmap(bitmapTrackArrow);
      final Paint trackArrowPainter = CanvasAdapter.newPaint();
      trackArrowPainter.setStyle(Paint.Style.STROKE);
      trackArrowPainter.setStrokeWidth(6);
      trackArrowPainter.setColor(arrowColor);

      defaultMarkerCanvas.drawLine(bitmapArrowSizeF, bitmapArrowSizeF / 2, 1f, bitmapArrowSizeF, trackArrowPainter);
      //defaultMarkerCanvas.drawLine(1f, bitmapArrowSizeF, 1f, 1f, trackArrowPainter); //looks better when  arrow is open
      defaultMarkerCanvas.drawLine(1f, 1f, bitmapArrowSizeF, bitmapArrowSizeF / 2, trackArrowPainter);
      defaultMarkerCanvas.drawLine(bitmapArrowSizeF, bitmapArrowSizeF / 2, 1, bitmapArrowSizeF / 2, trackArrowPainter);

      return bitmapTrackArrow;
   }

   /**
    * longpress on a mapbookmark
    * this method is moved from map25App to here
    *
    * @param index
    * @param MarkerItem
    * @return true, when clicked
    */
   @Override
   public boolean onItemLongPress(final int index, final MarkerInterface mi) {

      final MarkerItem item = (MarkerItem) mi;

      debugPrint(
            " Markertoolkit: " //$NON-NLS-1$
                  +
                  (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
                  + ("\tonItemLongpress") //$NON-NLS-1$
                  + ("\tMapbookmark") //$NON-NLS-1$
                  + ("\tTitle:" + item.getTitle()) //$NON-NLS-1$
                  + ("\tDescription:" + item.description) //$NON-NLS-1$
                  + ("\tindex:" + index) //$NON-NLS-1$
      //+ ("\t_isMapItemHit:" + _isMapItemHit + " -> true") //$NON-NLS-1$ //$NON-NLS-2$

      //Pref_Map25_Encoding_Mapsforge
      );

      if (item.getMarker() == null) {

         // item.setMarker(symbol);
      } else {

         // item.setMarker(null);
      }

      //debugPrint(" map25: " + "Marker long press " + item.getTitle()); //$NON-NLS-1$
      return true;

   }

   /**
    * clicking on a mapbookmark
    * this method is moved from map25App to here
    *
    * @param index
    * @param MarkerItem
    * @return true, when clicked
    */
   @Override
   public boolean onItemSingleTapUp(final int index, final MarkerInterface mi) {

      final MarkerItem item = (MarkerItem) mi;

      debugPrint(
            " MarkerToolkit: " //$NON-NLS-1$
                  +
                  (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
                  + ("\tonItemSingleTapUp") //$NON-NLS-1$
                  + ("\tMapbookmark") //$NON-NLS-1$
                  + ("\tTitle:" + item.getTitle()) //$NON-NLS-1$
                  + ("\tDescription:" + item.description) //$NON-NLS-1$
                  + ("\tindex:" + index) //$NON-NLS-1$
      //+ ("\t_isMapItemHit:" + _isMapItemHit + " -> true") //$NON-NLS-1$ //$NON-NLS-2$

      //Pref_Map25_Encoding_Mapsforge
      );

      if (item.getMarker() == null) {

         // item.setMarker(symbol);
      } else {

         // item.setMarker(null);
      }
      //debugPrint(" map25: " + "Marker tap, index:title " + item.getTitle()); //$NON-NLS-1$
      return false;

   }

}
