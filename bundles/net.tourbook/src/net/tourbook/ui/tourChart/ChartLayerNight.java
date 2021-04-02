/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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

package net.tourbook.ui.tourChart;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IChartOverlay;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public class ChartLayerNight implements IChartLayer, IChartOverlay {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private ChartNightConfig              _cnc;

   public ChartLayerNight() {
      //Nothing to do
   }

   /**
    * This paints the night sections for the current graph configuration.
    */
   @Override
   public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

      final int opacity = _prefStore.getInt(ITourbookPreferences.GRAPH_OPACITY_NIGHT_SECTIONS);

      final int devYTop = drawingData.getDevYTop();
      final int devGraphHeight = drawingData.devGraphHeight;

      gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);
      gc.setBackground(new Color(gc.getDevice(), 0x8c, 0x8c, 0x8c, opacity));
      gc.setAlpha(opacity);

      final double scaleX = drawingData.getScaleX();
      final long devVirtualGraphImageOffset = chart.getXXDevViewPortLeftBorder();
      final int devYBottom = drawingData.getDevYBottom();
      for (final ChartLabel chartLabel : _cnc.chartLabels) {

         final double virtualXPos = chartLabel.graphX * scaleX;
         final int devXNightSectionStart = (int) (virtualXPos - devVirtualGraphImageOffset);
         final double virtualXPosEnd = chartLabel.graphXEnd * scaleX;
         final int devXNightSectionEnd = (int) (virtualXPosEnd - devVirtualGraphImageOffset);

         gc.fillRectangle(devXNightSectionStart, devYTop, devXNightSectionEnd - devXNightSectionStart, devYBottom - devYTop);
      }

      gc.setClipping((Rectangle) null);
   }

   /**
    * This is painting the hovered night section.
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {
      //Nothing to do
   }

   public void setChartNightConfig(final ChartNightConfig chartNightConfig) {
      _cnc = chartNightConfig;
   }
}
