/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
import net.tourbook.data.TourMarker;
import net.tourbook.photo.ILoadCallBack;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
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

   private TourChart        _tourChart;
   private ChartPauseConfig _cpc;

   private boolean          _isVertical;

   private int              _devXPause;
   private int              _devYPause;
   private long             _hoveredEventTime;

   private ChartLabel       _hoveredLabel;
   private ChartLabel       _tooltipLabel;

   public class LoadImageCallback implements ILoadCallBack {

      @Override
      public void callBackImageIsLoaded(final boolean isImageLoaded) {

         if (isImageLoaded == false) {
            return;
         }

         // run in UI thread
         Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {

               // ensure chart is still displayed
               if (_tourChart.getShell().isDisposed()) {
                  return;
               }

               // paint image
               _tourChart.redrawLayer();
            }
         });
      }
   }

   public ChartLayerPause(final TourChart tourChart) {

      _tourChart = tourChart;
   }

   /**
    * Adjust label to the requested position
    *
    * @param chartLabel
    * @param devYTop
    * @param devYBottom
    * @param labelWidth
    * @param labelHeight
    */
   private void adjustLabelPosition(final ChartLabel chartLabel,
                                    final int devYTop,
                                    final int devYBottom,
                                    final int labelWidth,
                                    final int labelHeight) {

      final int pausePointSize2 = PAUSE_POINT_SIZE / 2 + 0;

      //LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED:
      _isVertical = false;
      _devXPause -= labelWidth / 2;
      _devYPause -= labelHeight + LABEL_OFFSET + pausePointSize2;
   }

   /**
    * This paints the pause(s) for the current graph config.
    */
   @Override
   public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

      final Device display = gc.getDevice();

      PAUSE_POINT_SIZE = pc.convertVerticalDLUsToPixels(2);
      PAUSE_HOVER_SIZE = pc.convertVerticalDLUsToPixels(4);
      LABEL_OFFSET = pc.convertVerticalDLUsToPixels(2);

      /*
       * Set the pause point size even that the label positioning has the correct distance otherwise
       * the right alignment looks ugly when the size is not even.
       */
      if (PAUSE_POINT_SIZE % 2 == 1) {
         PAUSE_POINT_SIZE++;
      }
      final int pausePointSize2 = PAUSE_POINT_SIZE / 2;

      final int devYTop = drawingData.getDevYTop();
      final int devYBottom = drawingData.getDevYBottom();
      final long devVirtualGraphImageOffset = chart.getXXDevViewPortLeftBorder();
      final int devGraphHeight = drawingData.devGraphHeight;
      final long devVirtualGraphWidth = drawingData.devVirtualGraphWidth;
      final int devVisibleChartWidth = drawingData.getChartDrawingData().devVisibleChartWidth;
      final boolean isGraphZoomed = devVirtualGraphWidth != devVisibleChartWidth;

      final float graphYBottom = drawingData.getGraphYBottom();
      final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
      final double scaleX = drawingData.getScaleX();
      final double scaleY = drawingData.getScaleY();

      final Color colorDefault = new Color(display, new RGB(0x60, 0x60, 0x60));
      final Color colorDevice = new Color(display, new RGB(0xff, 0x0, 0x80));
      final Color colorHidden = new Color(display, new RGB(0x24, 0x9C, 0xFF));

      gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

      /*
       * Draw pause point and label
       */
      for (final ChartLabel chartLabel : _cpc.chartLabels) {
         //TODO FB somewhere here, use the overlap checker to avoid overlapping pauses

         if (chartLabel.isVisible == false) {

            continue;
         }

         if (chartLabel.isDescription == false) {

            // skip pause which do not have a description
            continue;
         }

         final float yValue = yValues[chartLabel.serieIndex];
         final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

         final double virtualXPos = chartLabel.graphX * scaleX;
         _devXPause = (int) (virtualXPos - devVirtualGraphImageOffset);
         _devYPause = devYBottom - devYGraph;

         final Point labelExtend = gc.textExtent(chartLabel.markerLabel);

         /*
          * Get pause point top/left position
          */
         final int devXPauseTopLeft = _devXPause - pausePointSize2;
         final int devYPauseTopLeft = _devYPause - pausePointSize2;

         chartLabel.devXMarker = devXPauseTopLeft;
         chartLabel.devYMarker = devYPauseTopLeft;

         /*
          * Draw pause point
          */
         if (PAUSE_POINT_SIZE > 0) {

            gc.setBackground(colorDefault);

            // draw pause point
            gc.fillRectangle(//
                  devXPauseTopLeft,
                  devYPauseTopLeft,
                  PAUSE_POINT_SIZE,
                  PAUSE_POINT_SIZE);
         }

         /*
          * Draw pause label
          */

         gc.setForeground(colorDefault);

         final int labelWidth = labelExtend.x;
         final int labelHeight = labelExtend.y;

         adjustLabelPosition(//
               chartLabel,
               devYTop,
               devYBottom,
               labelWidth,
               labelHeight);

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

         // keep painted positions to identify and paint hovered positions
         chartLabel.paintedLabel = new Rectangle(_devXPause, _devYPause, labelWidth, labelHeight);

         // draw label
         gc.drawText(chartLabel.markerLabel, _devXPause, _devYPause, true);

         // keep painted positions to identify and paint hovered positions
         chartLabel.devIsVertical = _isVertical;
         chartLabel.devMarkerPointSize = PAUSE_POINT_SIZE;
         chartLabel.devHoverSize = PAUSE_HOVER_SIZE;
         chartLabel.devYBottom = devYBottom;
         chartLabel.devYTop = devYTop;
         chartLabel.devGraphWidth = devVisibleChartWidth;
      }

      colorDefault.dispose();
      colorDevice.dispose();
      colorHidden.dispose();

      gc.setClipping((Rectangle) null);
   }

   /**
    * This is painting the hovered pause.
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {

      final TourMarker selectedTourMarker = _tourChart.getSelectedTourMarker();

      ChartLabel selectedLabel = null;
      if (selectedTourMarker != null) {
         selectedLabel = getChartLabel(selectedTourMarker);
      }

      final boolean isHovered = _hoveredLabel != null || _tooltipLabel != null;
      final boolean isSelected = selectedTourMarker != null;

      if (isHovered == false && isSelected == false) {
         return;
      }

      ChartLabel hoveredLabel = _hoveredLabel;

      if (hoveredLabel == null) {
         hoveredLabel = _tooltipLabel;
      }

      final int devYTop = graphDrawingData.getDevYTop();
      final int devGraphHeight = graphDrawingData.devGraphHeight;

      final Device device = gc.getDevice();

      gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

      final Color colorDefault = new Color(device, new RGB(0x60, 0x60, 0x60));
      final Color colorDevice = new Color(device, new RGB(0xff, 0x0, 0x80));
      final Color colorHidden = new Color(device, new RGB(0x24, 0x9C, 0xFF));
      {
         if (isHovered && isSelected && hoveredLabel == selectedLabel) {

            // same label is hovered and selected

            drawOverlay_Label(hoveredLabel, gc, colorDefault, colorDevice, colorHidden, true);

         } else if (isHovered && isSelected) {

            // one label is hovered another label is selected

            drawOverlay_Label(hoveredLabel, gc, colorDefault, colorDevice, colorHidden, false);
            drawOverlay_Label(selectedLabel, gc, colorDefault, colorDevice, colorHidden, true);

         } else if (isHovered) {

            // the label is hovered

            drawOverlay_Label(hoveredLabel, gc, colorDefault, colorDevice, colorHidden, false);

         } else if (isSelected) {

            // a pause is selected

            drawOverlay_Label(selectedLabel, gc, colorDefault, colorDevice, colorHidden, true);
         }

      }
      colorDefault.dispose();
      colorDevice.dispose();
      colorHidden.dispose();

      gc.setAlpha(0xff);
      gc.setClipping((Rectangle) null);
   }

   private void drawOverlay_Label(final ChartLabel chartLabel,
                                  final GC gc,
                                  final Color colorDefault,
                                  final Color colorDevice,
                                  final Color colorHidden,
                                  final boolean isSelected) {

      if (chartLabel == null) {
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

      } else if (chartLabel.isDeviceMarker()) {
         gc.setBackground(colorDevice);
      } else if (chartLabel.isVisible) {
         gc.setBackground(colorDefault);
      } else {
         gc.setBackground(colorHidden);
      }

      /*
       * Rectangles can be merged into a union with regions, took me some time to find this solution
       * :-)
       */
      final Region region = new Region(gc.getDevice());

      final Rectangle paintedLabel = chartLabel.paintedLabel;
      if (paintedLabel != null) {

         final int devLabelX = paintedLabel.x - PAUSE_HOVER_SIZE;
         final int devLabelY = paintedLabel.y - PAUSE_HOVER_SIZE;
         final int devLabelWidth = paintedLabel.width + 2 * PAUSE_HOVER_SIZE;
         final int devLabelHeight = paintedLabel.height + 2 * PAUSE_HOVER_SIZE;

         region.add(devLabelX, devLabelY, devLabelWidth, devLabelHeight);
      }

      final int devPauseX = chartLabel.devXMarker - PAUSE_HOVER_SIZE;
      final int devPauseY = chartLabel.devYMarker - PAUSE_HOVER_SIZE;
      final int devPauseSize = PAUSE_POINT_SIZE + 2 * PAUSE_HOVER_SIZE;

      region.add(devPauseX, devPauseY, devPauseSize, devPauseSize);

      // get whole chart rect
      final Rectangle clientRect = gc.getClipping();

      gc.setClipping(region);
      {
         gc.fillRectangle(clientRect);
      }
      region.dispose();
      gc.setClipping((Region) null);
   }

   private ChartLabel getChartLabel(final TourMarker tourMarker) {

      for (final ChartLabel chartLabel : _cpc.chartLabels) {

         final Object chartLabelData = chartLabel.data;

         if (chartLabelData instanceof TourMarker) {
            final TourMarker chartTourMarker = (TourMarker) chartLabelData;

            if (chartTourMarker == tourMarker) {

               // marker is found

               return chartLabel;
            }
         }
      }

      return null;
   }

   public ChartLabel getHoveredLabel() {
      return _hoveredLabel;
   }

   /**
    * Set state in marker layer that nothing is hovered.
    */
   void resetHoveredState() {

      _hoveredLabel = null;
      _tooltipLabel = null;
   }

   /**
    * @param mouseEvent
    * @return Returns the hovered {@link ChartLabel} or <code>null</code> when a {@link ChartLabel}
    *         is not hovered.
    */
   ChartLabel retrieveHoveredLabel(final ChartMouseEvent mouseEvent) {

      if (mouseEvent.eventTime == _hoveredEventTime) {
         return _hoveredLabel;
      }

      _hoveredEventTime = mouseEvent.eventTime;

      // pause is dirty -> retrieve again
      _hoveredLabel = retrieveHoveredLabel_10(mouseEvent.devXMouse, mouseEvent.devYMouse);

      return _hoveredLabel;
   }

   private ChartLabel retrieveHoveredLabel_10(final int devXMouse, final int devYMouse) {

      /*
       * Check sign images first, they have a higher priority
       */
      for (final ChartLabel chartLabel : _cpc.chartLabels) {

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

               // pause sign image is hit
               return chartLabel;
            }
         }
      }

      for (final ChartLabel chartLabel : _cpc.chartLabels) {

         /*
          * Check sign label
          */
         final Rectangle paintedLabel = chartLabel.paintedLabel;
         if (paintedLabel != null) {

            final int devXLabel = paintedLabel.x;
            final int devYLabel = paintedLabel.y;

            if (devXMouse > devXLabel - PAUSE_HOVER_SIZE
                  && devXMouse < devXLabel + paintedLabel.width + PAUSE_HOVER_SIZE
                  && devYMouse > devYLabel - PAUSE_HOVER_SIZE
                  && devYMouse < devYLabel + paintedLabel.height + PAUSE_HOVER_SIZE) {

               // horizontal label is hit
               return chartLabel;
            }
         }

         /*
          * Check pause point
          */
         final int devXPause = chartLabel.devXMarker;
         final int devYPause = chartLabel.devYMarker;

         if (devXMouse > devXPause - PAUSE_HOVER_SIZE
               && devXMouse < devXPause + PAUSE_POINT_SIZE + PAUSE_HOVER_SIZE
               && devYMouse > devYPause - PAUSE_HOVER_SIZE
               && devYMouse < devYPause + PAUSE_POINT_SIZE + PAUSE_HOVER_SIZE) {

            // pause point is hit
            return chartLabel;
         }
      }

      return null;
   }

   public void setChartPauseConfig(final ChartPauseConfig chartPauseConfig) {

      _cpc = chartPauseConfig;
   }
}
