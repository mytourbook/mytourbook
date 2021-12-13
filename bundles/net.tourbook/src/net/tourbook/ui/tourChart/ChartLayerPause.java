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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Display;

public class ChartLayerPause implements IChartLayer, IChartOverlay {

   private int              LABEL_OFFSET;
   private int              PAUSE_HOVER_SIZE;
   private int              PAUSE_POINT_SIZE;

   private ChartPauseConfig _chartPauseConfig;

   private long             _hoveredEventTime;

   private ChartLabelPause  _hoveredLabel;
   private ChartLabelPause  _tooltipLabel;

   private TourChart        _tourChart;

   public ChartLayerPause(final TourChart tourChart) {

      _tourChart = tourChart;
   }

   /**
    * This paints the pause(s) for the current graph configuration.
    */
   @Override
   public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

      final int devYTop = drawingData.getDevYTop();
      final int devGraphHeight = drawingData.devGraphHeight;
      gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

      LABEL_OFFSET = PAUSE_POINT_SIZE = pc.convertVerticalDLUsToPixels(2);
      PAUSE_HOVER_SIZE = pc.convertVerticalDLUsToPixels(4);

      gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

      draw_PausePointAndLabel(gc, drawingData, chart);

      gc.setClipping((Rectangle) null);
   }

   private void draw_PausePointAndLabel(final GC gc,
                                        final GraphDrawingData graphDrawingData,
                                        final Chart chart) {

      final int devYTop = graphDrawingData.getDevYTop();
      final int devYBottom = graphDrawingData.getDevYBottom();
      final long devVirtualGraphImageOffset = chart.getXXDevViewPortLeftBorder();
      final long devVirtualGraphWidth = graphDrawingData.devVirtualGraphWidth;
      final int devVisibleChartWidth = graphDrawingData.getChartDrawingData().devVisibleChartWidth;
      final boolean isGraphZoomed = devVirtualGraphWidth != devVisibleChartWidth;
      final float graphYBottom = graphDrawingData.getGraphYBottom();
      final float[] yValues = graphDrawingData.getYData().getHighValuesFloat()[0];
      final double scaleX = graphDrawingData.getScaleX();
      final double scaleY = graphDrawingData.getScaleY();
      final int pausePointSize2 = PAUSE_POINT_SIZE / 2;

      final ValueOverlapChecker overlapChecker = new ValueOverlapChecker(500);

      for (final ChartLabelPause chartLabelPause : _chartPauseConfig.chartLabelPauses) {

         // check if a pause should be displayed
         if (chartLabelPause.isVisible == false && _chartPauseConfig.isShowPauseLabel == false) {
            continue;
         }

         final float yValue = yValues[chartLabelPause.serieIndex];
         final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

         final double virtualXPos = chartLabelPause.graphX * scaleX;
         int devXPause = (int) (virtualXPos - devVirtualGraphImageOffset);
         int devYPause = devYBottom - devYGraph;

         final String pauseDurationText = chartLabelPause.getPauseDuration();
         final Point labelExtend = gc.textExtent(pauseDurationText);

         /*
          * Get pause point top/left position
          */
         final int devXPauseTopLeft = devXPause - pausePointSize2;
         final int devYPauseTopLeft = devYPause - pausePointSize2;

         chartLabelPause.devXPause = devXPauseTopLeft;
         chartLabelPause.devYPause = devYPauseTopLeft;

         final Color labelColor = getLabelColor(chartLabelPause.isAutoPause());

         /*
          * Draw pause point
          */
         gc.setBackground(labelColor);
         gc.fillRectangle(devXPauseTopLeft, devYPauseTopLeft, PAUSE_POINT_SIZE, PAUSE_POINT_SIZE);

         /*
          * Draw pause label
          */
         gc.setForeground(labelColor);
         final int labelWidth = labelExtend.x;
         final int labelHeight = labelExtend.y;

         // align label to the pause point
         final int labelWidth2 = labelWidth / 2;
         final LabelAlignment labelAlignment = chartLabelPause.getLabelAlignment();
         final int border = 0;
         devXPause += labelAlignment.equals(LabelAlignment.LEFT)

               // align left
               ? -labelWidth - border

               : labelAlignment.equals(LabelAlignment.RIGHT)

                     // align right
                     ? +border

                     // center label to the pause point
                     : -labelWidth2;

         /*
          * Debug: Make alignment visible
          */
//         if (labelAlignment.equals(LabelAlignment.LEFT)) {
//            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//         } else if (labelAlignment.equals(LabelAlignment.RIGHT)) {
//            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
//         }

         devYPause -= labelHeight + LABEL_OFFSET + pausePointSize2;

         // add an additional offset which is defined for all pauses in the pause properties slideout
         devXPause += chartLabelPause.labelXOffset;
         devYPause -= chartLabelPause.labelYOffset;

         // don't draw the pause to the left of the chart
         if (devVirtualGraphImageOffset == 0 && devXPause < 0) {
            devXPause = 0;
         }

         // don't draw the pause to the right of the chart
         final double devPauseRightPos = isGraphZoomed
               ? virtualXPos + labelWidth
               : devXPause + labelWidth;
         if (devPauseRightPos > devVirtualGraphWidth) {
            devXPause = (int) (devVirtualGraphWidth - labelWidth - devVirtualGraphImageOffset - 2);
         }

         // force label to be not below the bottom
         if (devYPause + labelHeight > devYBottom) {
            devYPause = devYBottom - labelHeight;
         }

         // force label to be not above the top
         if (devYPause < devYTop) {
            devYPause = devYTop;
         }

         /*
          * Ensure the value text do not overlap, if possible :-)
          */
         final int borderWidth = PAUSE_HOVER_SIZE;
         final int borderWidth2 = 2 * borderWidth;
         final int borderHeight = -1;
         final int borderHeight2 = 2 * borderHeight;
         final int textHeightWithBorder = labelHeight + borderHeight2;

         final Rectangle textRect = new Rectangle(
               devXPause - borderWidth2,
               devYPause - borderHeight,
               labelWidth + borderWidth2,
               textHeightWithBorder);

         final Rectangle notOverlappedRectangle = overlapChecker.getNotOverlappedRectangle(
               textRect,
               true, // isValueUp,
               textHeightWithBorder);

         final boolean isNotOverlappedRectangle = notOverlappedRectangle == null;

         final int devYPauseNotOverlapped = isNotOverlappedRectangle ? textRect.y : notOverlappedRectangle.y;

         // draw label
         gc.drawText(pauseDurationText, devXPause, devYPauseNotOverlapped, true);

         // keep current valid rectangle
         if (isNotOverlappedRectangle == false) {
            overlapChecker.setupNext(notOverlappedRectangle);
         }

         // keep painted positions to identify and paint hovered positions
         chartLabelPause.paintedLabel = new Rectangle(devXPause, devYPauseNotOverlapped, labelWidth, labelHeight);
         chartLabelPause.devPointSize = PAUSE_POINT_SIZE;
         chartLabelPause.devHoverSize = PAUSE_HOVER_SIZE;
         chartLabelPause.devYBottom = devYBottom;
         chartLabelPause.devYTop = devYTop;
         chartLabelPause.devGraphWidth = devVisibleChartWidth;
      }
   }

   /**
    * This is painting the hovered pause.
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {

      final ChartLabelPause hoveredLabel = _hoveredLabel;

      if (hoveredLabel == null) {
         return;
      }

      // the label is hovered
      drawOverlay_Label(hoveredLabel, gc);
   }

   private void drawOverlay_Label(final ChartLabelPause chartLabelPause,
                                  final GC gc) {

      if (chartLabelPause == null) {
         return;
      }

      gc.setAlpha(0x30);

      gc.setBackground(getLabelColor(chartLabelPause.isAutoPause()));

      /*
       * Rectangles can be merged into a union with regions, took me some time to find this solution
       * :-)
       */
      final Region region = new Region(gc.getDevice());

      final Rectangle paintedLabel = chartLabelPause.paintedLabel;
      if (paintedLabel != null) {

         final int devLabelX = paintedLabel.x - PAUSE_HOVER_SIZE;
         final int devLabelY = paintedLabel.y - PAUSE_HOVER_SIZE;
         final int devLabelWidth = paintedLabel.width + 2 * PAUSE_HOVER_SIZE;
         final int devLabelHeight = paintedLabel.height + 2 * PAUSE_HOVER_SIZE;

         region.add(devLabelX, devLabelY, devLabelWidth, devLabelHeight);
      }

      final int devPauseX = chartLabelPause.devXPause - PAUSE_HOVER_SIZE;
      final int devPauseY = chartLabelPause.devYPause - PAUSE_HOVER_SIZE;
      final int devPauseSize = PAUSE_POINT_SIZE + 2 * PAUSE_HOVER_SIZE;

      region.add(devPauseX, devPauseY, devPauseSize, devPauseSize);

      // get whole chart rectangle
      final Rectangle clientRectangle = gc.getClipping();

      gc.setClipping(region);
      {
         gc.fillRectangle(clientRectangle);
      }
      region.dispose();
      gc.setClipping((Region) null);
   }

   public ChartLabelPause getHoveredLabel() {
      return _hoveredLabel;
   }

   private Color getLabelColor(final boolean isAutoPause) {

      Color color;

      if (isAutoPause) {

         color = UI.isDarkTheme()
               ? new Color(new RGB(0xa0, 0xa0, 0xa0))
               : new Color(new RGB(0x60, 0x60, 0x60));

      } else {

         color = UI.isDarkTheme()
               ? Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW)
               : Display.getCurrent().getSystemColor(SWT.COLOR_RED);
      }

      return color;
   }

   /**
    * Set state in pause layer so that nothing is hovered.
    */
   void resetHoveredState() {

      _hoveredLabel = null;
      _tooltipLabel = null;
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
          * Check pause point
          */
         final int devXPause = chartLabelPause.devXPause;
         final int devYPause = chartLabelPause.devYPause;

         if (devXMouse > devXPause - PAUSE_HOVER_SIZE
               && devXMouse < devXPause + PAUSE_POINT_SIZE + PAUSE_HOVER_SIZE
               && devYMouse > devYPause - PAUSE_HOVER_SIZE
               && devYMouse < devYPause + PAUSE_POINT_SIZE + PAUSE_HOVER_SIZE) {

            // pause point is hit
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

   public void setTooltipLabel(final ChartLabelPause tooltipLabel) {

      if (_tooltipLabel == tooltipLabel) {
         return;
      }

      _tooltipLabel = tooltipLabel;

      _tourChart.setChartOverlayDirty();
   }
}
