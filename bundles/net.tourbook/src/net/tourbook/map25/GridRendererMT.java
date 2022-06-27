/*
 * Copyright 2012 Hannes Janetzek
 * Copyright 2016 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map25;

import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;

public class GridRendererMT extends BucketRenderer {

   private final TextStyle      _textStyle;

   private final TextBucket     _textBucket;
   private final LineBucket     _lineBucket;

   private final GeometryBuffer _geoBufferLines;

   private TileGridLayerMT      _tileGridLayerMT;

   private int                  _oldX, _oldY, _oldZoomLevel;

   public GridRendererMT(final float scale) {

      this(
            1,

            new LineStyle(Color.RED, 2f, Cap.BUTT),

            TextStyle
                  .builder()
                  .fontSize(26 * scale)
                  .fontStyle(Paint.FontStyle.NORMAL)
                  .color(Color.RED)
                  .build());
   }

   public GridRendererMT(final int numLines, final LineStyle lineStyle, final TextStyle textStyle) {

      final int tileSize = Tile.SIZE;

      /* not needed to set but we know: 16 lines 'a' two points */
      _geoBufferLines = new GeometryBuffer(2 * 16, 16);

      final float pos = -tileSize * 4;

      /* 8 vertical lines */
      for (int i = 0; i < 8 * numLines; i++) {
         final float x = pos + i * tileSize / numLines;
         _geoBufferLines.startLine();
         _geoBufferLines.addPoint(x, pos);
         _geoBufferLines.addPoint(x, pos + tileSize * 8);
      }

      /* 8 horizontal lines */
      for (int j = 0; j < 8 * numLines; j++) {
         final float y = pos + j * tileSize / numLines;
         _geoBufferLines.startLine();
         _geoBufferLines.addPoint(pos, y);
         _geoBufferLines.addPoint(pos + tileSize * 8, y);
      }

      _textStyle = textStyle;

      _lineBucket = new LineBucket(0);
      _lineBucket.line = lineStyle;

      if (_textStyle != null) {
         _textBucket = new TextBucket();
         _textBucket.next = _lineBucket;
      } else {
         _textBucket = null;
         _lineBucket.addLine(_geoBufferLines);
         buckets.set(_lineBucket);
      }
   }

   public GridRendererMT(final TileGridLayerMT tileGridLayerMT) {

      this(1);

      _tileGridLayerMT = tileGridLayerMT;
   }

   private void addLabels(final int x, final int y, final int zoomLevel, final MapPosition mapPosition) {

      final int tileSize = Tile.SIZE;

      final int mapScale = 1 << zoomLevel;
      final float lineHeight = _textStyle.fontSize + 1;

      final TextBucket textBucket = _textBucket;
      textBucket.clear();

      for (int yOffset = -4; yOffset < 3; yOffset++) {
         for (int xOffset = -5; xOffset < 4; xOffset++) {

            final int tileX = x + xOffset;
            final int tileY = y + yOffset;

            final double latitude = MercatorProjection.toLatitude((double) tileY / mapScale);
            final double longitude = MercatorProjection.toLongitude((double) tileX / mapScale);

            final String labelTile = String.format("%d / X:%d / Y:%d", zoomLevel, tileX, tileY); //$NON-NLS-1$
            final String labelLat = String.format("lat %.4f", latitude); //$NON-NLS-1$
            final String labelLon = String.format("lon %.4f", longitude); //$NON-NLS-1$

            // center within the tile
            final int textX = tileSize * xOffset + tileSize / 2;
            final int textY = tileSize * yOffset + tileSize / 2 - (int) lineHeight;

            TextItem textItem = TextItem.pool.get();
            textItem.set(textX, textY, labelTile, _textStyle);
            textBucket.addText(textItem);

            /*
             * Lat/Lon
             */
            textItem = TextItem.pool.get();
            textItem.set(textX, textY + lineHeight, labelLat, _textStyle);
            textBucket.addText(textItem);

            textItem = TextItem.pool.get();
            textItem.set(textX, textY + lineHeight * 2, labelLon, _textStyle);
            textBucket.addText(textItem);
         }
      }
   }

   @Override
   public void update(final GLViewport viewport) {

      if (_tileGridLayerMT.isEnabled() == false) {
         return;
      }

      final MapPosition currentMapPosition = viewport.pos;

      /*
       * Scale coordinates relative to current 'zoom-level' to get the position as the nearest
       * tile coordinate
       */
      final int currentScale = 1 << currentMapPosition.zoomLevel;
      final int currentX = (int) (currentMapPosition.x * currentScale);
      final int currentY = (int) (currentMapPosition.y * currentScale);

      // update buckets when map moved by at least one tile
      if (currentX == _oldX && currentY == _oldY && currentScale == _oldZoomLevel) {
         return;
      }

      _oldX = currentX;
      _oldY = currentY;
      _oldZoomLevel = currentScale;

      /*
       * Overwrite map position in this renderer
       */
      mMapPosition.copy(currentMapPosition);
      mMapPosition.x = (double) currentX / currentScale;
      mMapPosition.y = (double) currentY / currentScale;
      mMapPosition.scale = currentScale;

//      System.out.println((System.currentTimeMillis() + " " + mMapPosition));
//      // TODO remove SYSTEM.OUT.PRINTLN

      if (_textStyle != null) {

         buckets.set(_textBucket);

         addLabels(currentX, currentY, currentMapPosition.zoomLevel, currentMapPosition);

         _lineBucket.addLine(_geoBufferLines);
         buckets.prepare();

         setReady(false);
      }

      if (isReady() == false) {
         compile();
      }
   }
}
