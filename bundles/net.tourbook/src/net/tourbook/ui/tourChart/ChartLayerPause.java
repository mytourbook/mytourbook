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

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IChartOverlay;
import net.tourbook.common.UI;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

public class ChartLayerPause implements IChartLayer, IChartOverlay {

   private int              LABEL_OFFSET;
   private int              MARKER_HOVER_SIZE;
   private int              MARKER_POINT_SIZE;
   private int              PAUSE_POINT_SIZE;

   private TourChart        _tourChart;
   private ChartPauseConfig _chartPauseConfig;

   private long             _hoveredEventTime;

   private ChartLabel       _hoveredLabel;

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
      LABEL_OFFSET = PAUSE_POINT_SIZE = pc.convertVerticalDLUsToPixels(2);
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
      for (final ChartLabel chartLabel : _chartPauseConfig.chartLabels) {

         final float yValue = yValues[chartLabel.serieIndex];
         final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

         final double virtualXPos = chartLabel.graphX * scaleX;
         _devXPause = (int) (virtualXPos - devVirtualGraphImageOffset);
         _devYPause = devYBottom - devYGraph;

         final Point labelExtend = gc.textExtent(chartLabel.pauseDuration);

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

         // add an additional offset which is defined for all pauses in the pause properties slideout
         _devXPause += chartLabel.labelXOffset;
         _devYPause -= chartLabel.labelYOffset;

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

         // force label to be not below the bottom
         if (_devYPause + labelHeight > devYBottom) {
            _devYPause = devYBottom - labelHeight;
         }

         // force label to be not above the top
         if (_devYPause < devYTop) {
            _devYPause = devYTop;
         }

         final String pauseDurationText = chartLabel.pauseDuration;
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

   }


   private ChartLabel retrieveHoveredLabel_10(final int devXMouse, final int devYMouse) {

      /*
       * Check sign images first, they have a higher priority
       */
      for (final ChartLabel chartLabel : _chartPauseConfig.chartLabels) {

         final Rectangle imageBounds = chartLabel.devMarkerSignImageBounds;
         if (imageBounds != null) {

            final int devXImage = imageBounds.x;
            final int devYImage = imageBounds.y;
            final int imageWidth = imageBounds.width;
            final int imageHeight = imageBounds.height;

            if (devXMouse > devXImage
                  && devXMouse < devXImage + imageWidth
                  && devYMouse > devYImage
                  && devYMouse < devYImage + imageHeight) {

               // marker sign image is hit
               return chartLabel;
            }
         }
      }

      for (final ChartLabel chartLabel : _chartPauseConfig.chartLabels) {

         /*
          * Check sign label
          */
         final Rectangle paintedLabel = chartLabel.paintedLabel;
         if (paintedLabel != null) {

            final int devXLabel = paintedLabel.x;
            final int devYLabel = paintedLabel.y;

            if (devXMouse > devXLabel - MARKER_HOVER_SIZE
                  && devXMouse < devXLabel + paintedLabel.width + MARKER_HOVER_SIZE
                  && devYMouse > devYLabel - MARKER_HOVER_SIZE
                  && devYMouse < devYLabel + paintedLabel.height + MARKER_HOVER_SIZE) {

               // horizontal label is hit
               return chartLabel;
            }
         }

         /*
          * Check marker point
          */
         final int devXMarker = chartLabel.devXMarker;
         final int devYMarker = chartLabel.devYMarker;

         if (devXMouse > devXMarker - MARKER_HOVER_SIZE
               && devXMouse < devXMarker + MARKER_POINT_SIZE + MARKER_HOVER_SIZE
               && devYMouse > devYMarker - MARKER_HOVER_SIZE
               && devYMouse < devYMarker + MARKER_POINT_SIZE + MARKER_HOVER_SIZE) {

            // marker point is hit
            return chartLabel;
         }
      }

      return null;
   }

   public ChartLabel retrieveHoveredPause(final ChartMouseEvent mouseEvent) {

      if (mouseEvent.eventTime == _hoveredEventTime) {
         return _hoveredLabel;
      }

      _hoveredEventTime = mouseEvent.eventTime;

      // marker is dirty -> retrieve again
      _hoveredLabel = retrieveHoveredLabel_10(mouseEvent.devXMouse, mouseEvent.devYMouse);

      return _hoveredLabel;
   }
   public void setChartPauseConfig(final ChartPauseConfig chartPauseConfig) {
      _chartPauseConfig = chartPauseConfig;
   }
}
