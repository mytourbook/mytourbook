/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IChartOverlay;
import net.tourbook.common.UI;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;

public class ChartLayerPause implements IChartLayer, IChartOverlay {

   private int              LABEL_OFFSET;
   private int              PAUSE_HOVER_SIZE;
   private int              PAUSE_POINT_SIZE;

   private TourChart        _tourChart;
   private ChartPauseConfig _chartPauseConfig;

   private long             _hoveredEventTime;

   private ChartLabelPause  _hoveredLabel;

   private int              _devXPause;
   private int              _devYPause;

   public ChartLayerPause() {
      //Nothing to do
   }

   /**
    * Adjust label to the requested position
    *
    * @param labelWidth
    * @param labelHeight
    */
   private void adjustLabelPosition(final int labelWidth,
                                    final int labelHeight) {

      final int pausePointSize2 = PAUSE_POINT_SIZE / 2 + 0;

      //LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED:
      _devXPause -= labelWidth / 2;
      _devYPause -= labelHeight + LABEL_OFFSET + pausePointSize2;
   }

   /**
    * This paints the pause(s) for the current graph configuration.
    */
   @Override
   public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

      final int devYTop = drawingData.getDevYTop();
      final int devGraphHeight = drawingData.devGraphHeight;
      gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

      final int devYBottom = drawingData.getDevYBottom();
      final long devVirtualGraphImageOffset = chart.getXXDevViewPortLeftBorder();
      final long devVirtualGraphWidth = drawingData.devVirtualGraphWidth;
      final int devVisibleChartWidth = drawingData.getChartDrawingData().devVisibleChartWidth;
      final boolean isGraphZoomed = devVirtualGraphWidth != devVisibleChartWidth;
      LABEL_OFFSET = PAUSE_POINT_SIZE = PAUSE_HOVER_SIZE = pc.convertVerticalDLUsToPixels(2);
      final int pausePointSize2 = PAUSE_POINT_SIZE / 2;
      final float graphYBottom = drawingData.getGraphYBottom();
      final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
      final double scaleX = drawingData.getScaleX();
      final double scaleY = drawingData.getScaleY();
      final Color colorDefault = UI.IS_DARK_THEME
            ? new Color(new RGB(0xa0, 0xa0, 0xa0))
            : new Color(new RGB(0x60, 0x60, 0x60));
      final ValueOverlapChecker overlapChecker = new ValueOverlapChecker(2);
      /*
       * Draw pause point and label
       */
      for (final ChartLabelPause chartLabelPause : _chartPauseConfig.chartLabelPauses) {

         final float yValue = yValues[chartLabelPause.serieIndex];
         final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

         final double virtualXPos = chartLabelPause.graphX * scaleX;
         _devXPause = (int) (virtualXPos - devVirtualGraphImageOffset);
         _devYPause = devYBottom - devYGraph;

         final Point labelExtend = gc.textExtent(chartLabelPause.getPauseDuration());

         /*
          * Get pause point top/left position
          */
         final int devXPauseTopLeft = _devXPause - pausePointSize2;
         final int devYPauseTopLeft = _devYPause - pausePointSize2;

         /*
          * Draw pause point
          */
         gc.setBackground(colorDefault);

         // draw pause point
         gc.fillRectangle(devXPauseTopLeft, devYPauseTopLeft, PAUSE_POINT_SIZE, PAUSE_POINT_SIZE);

         /*
          * Draw pause label
          */

         gc.setForeground(colorDefault);

         final int labelWidth = labelExtend.x;
         final int labelHeight = labelExtend.y;

         adjustLabelPosition(labelWidth, labelHeight);

         /*
          * label is horizontal
          */

         // don't draw the pause to the left of the chart
         if (devVirtualGraphImageOffset == 0 && _devXPause < 0) {
            _devXPause = 0;
         }

         // don't draw the pause to the right of the chart
         final double devPauseRightPos = isGraphZoomed
               ? virtualXPos + labelWidth
               : _devXPause + labelWidth;
         if (devPauseRightPos > devVirtualGraphWidth) {
            _devXPause = (int) (devVirtualGraphWidth - labelWidth - devVirtualGraphImageOffset - 2);
         }

         chartLabelPause.devXPause = _devXPause;
         chartLabelPause.devYPause = _devYPause;

         // force label to be not below the bottom
         if (_devYPause + labelHeight > devYBottom) {
            _devYPause = devYBottom - labelHeight;
         }

         // force label to be not above the top
         if (_devYPause < devYTop) {
            _devYPause = devYTop;
         }

         final String pauseDurationText = chartLabelPause.getPauseDuration();
         final Point textExtent = gc.textExtent(pauseDurationText);
         final int textWidth = textExtent.x;
         final int textHeight = textExtent.y;
         final int borderWidth = 5;
         final int borderWidth2 = 2 * borderWidth;
         final int borderHeight = 0;
         final int borderHeight2 = 2 * borderHeight;
         final int textHeightWithBorder = textHeight + borderHeight2;

         /*
          * Ensure the value text do not overlap, if possible :-)
          */
         final Rectangle textRect = new Rectangle(//
               _devXPause,
               _devYPause,
               textWidth + borderWidth2,
               textHeightWithBorder);

         // keep painted positions to identify and paint the hovered positions
         chartLabelPause.paintedLabel = textRect;

         final Rectangle validRect = overlapChecker.getValidRect(
               textRect,
               true,
               textHeightWithBorder,
               pauseDurationText);

         // don't draw over the graph borders
         if (validRect != null && validRect.y > devYTop && validRect.y + textHeight < devYBottom) {

            // keep current valid rectangle
            overlapChecker.setupNext(validRect, true);

            gc.setAlpha(0xff);

            // draw label
            gc.drawText(pauseDurationText, validRect.x, validRect.y, true);
         }
      }

      gc.setClipping((Rectangle) null);
   }

   /**
    * This is painting the hovered pause.
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {
      //Nothing to do
      System.out.println("TOTO");

      final ChartLabelPause hoveredLabel = _hoveredLabel;

      if (hoveredLabel == null) {
         return;
      }

      // the label is hovered
      final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

      drawOverlay_Label(hoveredLabel,
            gc,
            new Color(PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT_DARK)),
            new Color(PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT_DARK)),
            false);

   }

   private void drawOverlay_Label(final ChartLabelPause chartLabelMarker,
                                  final GC gc,
                                  final Color colorDefault,
                                  final Color colorHidden,
                                  final boolean isSelected) {

      if (chartLabelMarker == null) {
         return;
      }

      if (isSelected) {
         gc.setAlpha(0x60);
      } else {
         gc.setAlpha(0x30);
      }

      if (isSelected) {

         final Color selectedColorBg = gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY);
         gc.setBackground(selectedColorBg);

      } else if (chartLabelMarker.isVisible) {
         gc.setBackground(colorDefault);
      } else {
         gc.setBackground(colorHidden);
      }

      /*
       * Rectangles can be merged into a union with regions, took me some time to find this solution
       * :-)
       */
      final Region region = new Region(gc.getDevice());

      final Rectangle paintedLabel = chartLabelMarker.paintedLabel;
      if (paintedLabel != null) {

         final int devLabelX = paintedLabel.x - PAUSE_HOVER_SIZE;
         final int devLabelY = paintedLabel.y - PAUSE_HOVER_SIZE;
         final int devLabelWidth = paintedLabel.width + 2 * PAUSE_HOVER_SIZE;
         final int devLabelHeight = paintedLabel.height + 2 * PAUSE_HOVER_SIZE;

         region.add(devLabelX, devLabelY, devLabelWidth, devLabelHeight);
      }

      final int devMarkerX = chartLabelMarker.devXPause - PAUSE_HOVER_SIZE;
      final int devMarkerY = chartLabelMarker.devYPause - PAUSE_HOVER_SIZE;
      final int devMarkerSize = PAUSE_POINT_SIZE + 2 * PAUSE_HOVER_SIZE;

      region.add(devMarkerX, devMarkerY, devMarkerSize, devMarkerSize);

      // get whole chart rect
      final Rectangle clientRect = gc.getClipping();

      gc.setClipping(region);
      {
         gc.fillRectangle(clientRect);
      }
      region.dispose();
      gc.setClipping((Region) null);
   }

   public ChartLabel getHoveredLabel() {
      return _hoveredLabel;
   }

   private ChartLabelPause retrieveHoveredLabel(final int devXMouse, final int devYMouse) {

      for (final ChartLabelPause chartLabelPause : _chartPauseConfig.chartLabelPauses) {

         /*
          * Check sign label
          */
         final Rectangle paintedLabel = chartLabelPause.paintedLabel;
         if (paintedLabel != null) {

            final int devXLabel = paintedLabel.x;
            final int devYLabel = paintedLabel.y;

            if (devXMouse > devXLabel - PAUSE_HOVER_SIZE
                  && devXMouse < devXLabel + paintedLabel.width + PAUSE_HOVER_SIZE
                  && devYMouse > devYLabel - PAUSE_HOVER_SIZE
                  && devYMouse < devYLabel + paintedLabel.height + PAUSE_HOVER_SIZE) {

               // horizontal label is hit
               return chartLabelPause;
            }
         }

         /*
          * Check marker point
          */
         final int devXMarker = chartLabelPause.devXPause;
         final int devYMarker = chartLabelPause.devYPause;

         if (devXMouse > devXMarker - PAUSE_HOVER_SIZE
               && devXMouse < devXMarker + PAUSE_POINT_SIZE + PAUSE_HOVER_SIZE
               && devYMouse > devYMarker - PAUSE_HOVER_SIZE
               && devYMouse < devYMarker + PAUSE_POINT_SIZE + PAUSE_HOVER_SIZE) {

            // marker point is hit
            return chartLabelPause;
         }
      }

      return null;
   }

   public ChartLabelPause retrieveHoveredPause(final ChartMouseEvent mouseEvent) {

      if (mouseEvent.eventTime == _hoveredEventTime) {
         return _hoveredLabel;
      }

      _hoveredEventTime = mouseEvent.eventTime;

      _hoveredLabel = retrieveHoveredLabel(mouseEvent.devXMouse, mouseEvent.devYMouse);

      return _hoveredLabel;
   }

   public void setChartPauseConfig(final ChartPauseConfig chartPauseConfig) {
      _chartPauseConfig = chartPauseConfig;
   }
}
