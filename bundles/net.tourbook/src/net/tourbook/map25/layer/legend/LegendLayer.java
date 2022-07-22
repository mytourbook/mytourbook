/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.layer.legend;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.map.MapUI.LegendUnitLayout;
import net.tourbook.data.TourData;
import net.tourbook.map.MapUtils;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig.LineColorMode;

import org.oscim.awt.AwtBitmap;
import org.oscim.backend.CanvasAdapter;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport.Position;

public class LegendLayer extends Layer {

   private Map                  _map;

   private final LegendRenderer _legendRenderer;
   private IMapColorProvider    _mapColorProvider;

   private List<TourData>       _allTourData;

   public LegendLayer(final Map map) {

      this(map, CanvasAdapter.getScale());
   }

   public LegendLayer(final Map map, final float scale) {

      super(map);

      _map = map;

      mRenderer = _legendRenderer = new LegendRenderer();

      _legendRenderer.setPosition(Position.BOTTOM_LEFT);
      _legendRenderer.setOffset(10, 10);
   }

   @Override
   public BitmapRenderer getRenderer() {
      return _legendRenderer;
   }

   public void setColorProvider(final IMapColorProvider mapColorProvider) {

      _mapColorProvider = mapColorProvider;
   }

   public void updateLegend() {

      updateLegend(_allTourData);
   }

   public void updateLegend(final List<TourData> allTourData) {

      if (allTourData == null) {
         return;
      }

      _allTourData = allTourData;

      // check if layer is visible
      if (isEnabled() == false) {
         return;
      }

      // check if line is valid
      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();
      if (trackConfig.lineColorMode != LineColorMode.GRADIENT || trackConfig.isShowDirectionArrow) {

         _legendRenderer.setLegendVisible(false);

         return;
      }

      IGradientColorProvider gradientColorProvider;

      if (_mapColorProvider instanceof IGradientColorProvider) {

         gradientColorProvider = (IGradientColorProvider) _mapColorProvider;

      } else {

         // other color providers are not yet supported

         _legendRenderer.setLegendVisible(false);

         return;
      }

      _legendRenderer.setLegendVisible(true);

      final int mapHeight = _map.getHeight();

      final int legendWidth = 150;

      final int legendMinHeight = Math.min(IMapColorProvider.DEFAULT_LEGEND_HEIGHT, mapHeight - 40);
      final int legendHeight = Math.max(40, legendMinHeight);

      final BufferedImage awtImage = new BufferedImage(
            legendWidth,
            legendHeight,
            BufferedImage.TYPE_INT_ARGB);

      final Graphics2D g2d = awtImage.createGraphics();

      try {

         final boolean isDataAvailable = MapUtils.configureColorProvider(

               allTourData,
               gradientColorProvider,
               ColorProviderConfig.MAP3_TOUR,
               legendHeight);

         if (isDataAvailable) {

            final LegendUnitLayout legendUnitLayout = trackConfig.legendUnitLayout;

            boolean isDarkBackground;
            boolean isShowUnitShadow;

            switch (legendUnitLayout) {

            case BRIGHT_BACKGROUND__NO_SHADOW:
               isDarkBackground = false;
               isShowUnitShadow = false;
               break;

            case BRIGHT_BACKGROUND__WITH_SHADOW:
               isDarkBackground = false;
               isShowUnitShadow = true;
               break;

            case DARK_BACKGROUND__NO_SHADOW:
               isDarkBackground = true;
               isShowUnitShadow = false;
               break;

            case DARK_BACKGROUND__WITH_SHADOW:
            default:
               isDarkBackground = true;
               isShowUnitShadow = true;
               break;
            }

            TourMapPainter.drawMap3_Legend(

                  g2d,
                  gradientColorProvider,
                  ColorProviderConfig.MAP3_TOUR,
                  legendWidth,
                  legendHeight,

                  isDarkBackground,
                  isShowUnitShadow

            );
         }

      } finally {
         g2d.dispose();
      }

      final AwtBitmap rendererImage = new AwtBitmap(awtImage);

      _legendRenderer.setBitmap(rendererImage, legendWidth, legendHeight);
   }
}
