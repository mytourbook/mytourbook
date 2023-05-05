/*******************************************************************************
 * Copyright (C) 2021, 2023 Frédéric Bard
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

import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.chart.ChartComponentGraph;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.data.TourData;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class ChartPauseToolTip extends AnimatedToolTipShell implements ITourProvider {

   /**
    * Visual position for the pause tooltip, they must correspond to the position id
    * TOOLTIP_POSITION_*.
    */
   protected static final String[] TOOLTIP_POSITIONS;

   static {

      TOOLTIP_POSITIONS = new String[] {

            Messages.Tooltip_Position_Left, //            0
            Messages.Tooltip_Position_Right, //           1
            Messages.Tooltip_Position_Top, //             2
            Messages.Tooltip_Position_Bottom, //          3
            Messages.Tooltip_Position_ChartTop, //        4
            Messages.Tooltip_Position_ChartBottom, //     5
      };
   }

   private static final int TOOLTIP_POSITION_LEFT         = 0;
   private static final int TOOLTIP_POSITION_RIGHT        = 1;
   private static final int TOOLTIP_POSITION_ABOVE        = 2;
   private static final int TOOLTIP_POSITION_BELOW        = 3;
   private static final int TOOLTIP_POSITION_CHART_TOP    = 4;
   private static final int TOOLTIP_POSITION_CHART_BOTTOM = 5;

   public static final int  DEFAULT_TOOLTIP_POSITION      = TOOLTIP_POSITION_BELOW;

   private TourChart        _tourChart;
   private TourData         _tourData;

   private ChartLabelPause  _hoveredLabel;

   private ChartPauseConfig _chartPauseConfig;

   /*
    * UI resources
    */
   private Color _fgBorder;

   /*
    * UI controls
    */
   private Composite _shellContainer;
   private Composite _tooltipContainer;

   public ChartPauseToolTip(final TourChart tourChart) {

      super(tourChart);

      _tourChart = tourChart;

      setReceiveMouseExitEvent(true);
      setReceiveMouseMoveEvent(true);

      setIsShowShellTrimStyle(false);
      setIsAnimateLocation(false);
   }

   @Override
   protected void beforeHideToolTip() {

      /*
       * This is the tricky part that the hovered pause is reset before the tooltip is closed and
       * not when nothing is hovered. This ensures that the tooltip has a valid state.
       */
      _hoveredLabel = null;
   }

   @Override
   protected boolean canShowToolTip() {

      return _hoveredLabel != null;
   }

   @Override
   protected Composite createToolTipContentArea(final Composite shell) {

      setFadeInSteps(1);
      setFadeOutSteps(10);
      setFadeOutDelaySteps(5);

      if (_hoveredLabel == null) {
         return null;
      }
      _tourData = _tourChart.getTourData();

      final Composite container = createUI(shell);

      setColors(container);

      return container;
   }

   private Composite createUI(final Composite shell) {

      initUI(shell);

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      _shellContainer = new Composite(shell, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(1, 1, 1, 1)
            .applyTo(_shellContainer);
      _shellContainer.addPaintListener(this::onPaintShellContainer);
      {
         _tooltipContainer = new Composite(_shellContainer, SWT.NONE);
         GridLayoutFactory.fillDefaults()
               .extendedMargins(2, 5, 2, 5)
               .applyTo(_tooltipContainer);
         {
            createUI_10_Values();
         }
      }

      return _shellContainer;
   }

   private void createUI_10_Values() {

      final Composite container = new Composite(_tooltipContainer, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(3, 0)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(5, 1)
            .applyTo(container);
      {
         UI.createLabel(container, OtherMessages.GRAPH_LABEL_TIME_START);
         createUI_11_TimeField(
               container,
               _hoveredLabel.getPausedTime_Start(),
               _hoveredLabel.getTimeZoneId());

         UI.createLabel(container, OtherMessages.GRAPH_LABEL_TIME_END);
         createUI_11_TimeField(
               container,
               _hoveredLabel.getPausedTime_End(),
               _hoveredLabel.getTimeZoneId());
      }
   }

   private void createUI_11_TimeField(final Composite parent,
                                      final long value,
                                      final String dbTimeZoneId) {

      // Value
      final Label label = new Label(parent, SWT.NONE);

      final ZonedDateTime tourZonedDateTime = TimeTools.createTourDateTime(value, dbTimeZoneId).tourZonedDateTime;

      final StringBuilder labelBuilder = new StringBuilder();

      if (_tourData.isMultipleTours()) {

         labelBuilder.append(TimeTools.Formatter_YearMonthDay.format(tourZonedDateTime));
      }

      final String format_hh_mm_ss = UI.format_hh_mm_ss(
            tourZonedDateTime.getHour() * 3600L + tourZonedDateTime.getMinute() * 60 + tourZonedDateTime.getSecond());

      labelBuilder.append(UI.SPACE + format_hh_mm_ss);

      label.setText(labelBuilder.toString());
   }

   private void FixupDisplayBounds(final Point rightBottomBounds,
                                   final int devHoveredY,
                                   final int devHoveredWidth,
                                   final int devHoveredHeight,
                                   final int devHoverSize,
                                   final int tipWidth,
                                   final int tipHeight,
                                   final Point tooltipLocation) {

      final ChartComponentGraph chartComponentGraph = _tourChart.getChartComponents().getChartComponentGraph();
      final Point graphLocation = chartComponentGraph.toDisplay(0, 0);
      final Rectangle displayBounds = UI.getDisplayBounds(chartComponentGraph, tooltipLocation);

      final int displayX = displayBounds.x;
      final int displayY = displayBounds.y;
      final int displayWidth = displayBounds.width;
      final int displayHeight = displayBounds.height;
      if (tooltipLocation.x < displayX) {

         switch (_chartPauseConfig.pauseTooltipPosition) {

         case TOOLTIP_POSITION_LEFT:
            tooltipLocation.x = tooltipLocation.x + tipWidth + devHoveredWidth + 2;
            break;

         case TOOLTIP_POSITION_BELOW:
         case TOOLTIP_POSITION_ABOVE:
         case TOOLTIP_POSITION_CHART_TOP:
         case TOOLTIP_POSITION_CHART_BOTTOM:
         default:
            tooltipLocation.x = displayX;
            break;
         }
      }
      if (rightBottomBounds.x > displayX + displayWidth) {

         if (_chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_BELOW || _chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_ABOVE
               || _chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_CHART_TOP
               || _chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_CHART_BOTTOM) {

            tooltipLocation.x = displayWidth - tipWidth;
         } else if (_chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_RIGHT) {

            tooltipLocation.x = tooltipLocation.x - tipWidth - devHoveredWidth - 2;
         }
      }
      if (tooltipLocation.y < displayY &&
            (_chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_ABOVE
                  || _chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_CHART_TOP)) {

         tooltipLocation.y = graphLocation.y + devHoveredY + devHoveredHeight - devHoverSize + 2;
      }
      if (rightBottomBounds.y > displayY + displayHeight &&
            (_chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_BELOW
                  || _chartPauseConfig.pauseTooltipPosition == TOOLTIP_POSITION_CHART_BOTTOM)) {

         tooltipLocation.y = graphLocation.y + devHoveredY - tipHeight - 1;
      }
   }

   private Rectangle getHoveredRectangle() {

      final int hoverSize = _hoveredLabel.devHoverSize;
      final int devPausePointSizeRaw = _hoveredLabel.devPointSize;

      Rectangle hoveredRectangle = new Rectangle(_hoveredLabel.devXPause, _hoveredLabel.devYPause, 1, 1);

      if (devPausePointSizeRaw > 0) {

         final int devXPause = _hoveredLabel.devXPause - hoverSize;
         final int devYPause = _hoveredLabel.devYPause - hoverSize;
         final int devPauseSize = devPausePointSizeRaw + 2 * hoverSize;

         final Rectangle rectMarker = new Rectangle(devXPause, devYPause, devPauseSize, devPauseSize);
         hoveredRectangle = hoveredRectangle.union(rectMarker);
      }

      // add label rectangle
      if (_hoveredLabel.paintedLabel != null) {

         final Rectangle paintedLabel = _hoveredLabel.paintedLabel;
         final int devLabelWidthRaw = paintedLabel.width;

         if (devLabelWidthRaw > 0) {

            final int devLabelX = paintedLabel.x - hoverSize;
            final int devLabelY = paintedLabel.y - hoverSize;
            final int devLabelWidth = devLabelWidthRaw + 2 * hoverSize;
            final int devLabelHeight = paintedLabel.height + 2 * hoverSize;

            final Rectangle rectLabel = new Rectangle(devLabelX, devLabelY, devLabelWidth, devLabelHeight);

            hoveredRectangle = hoveredRectangle.union(rectLabel);
         }
      }

      return hoveredRectangle;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> tours = new ArrayList<>();
      tours.add(_tourChart.getTourData());

      return tours;
   }

   /**
    * By default the tooltip is located to the left side of the tour pause point, when not visible
    * it is displayed to the right side of the tour pause point.
    */
   @Override
   public Point getToolTipLocation(final Point tipSize) {

      final Rectangle hoveredRect = getHoveredRectangle();

      final int devHoveredX = hoveredRect.x;
      int devHoveredY = hoveredRect.y;
      final int devHoveredWidth = hoveredRect.width;
      final int devHoveredHeight = hoveredRect.height;
      final int devHoverSize = _hoveredLabel.devHoverSize;

      final int devYTop = _hoveredLabel.devYTop;
      final int devYBottom = _hoveredLabel.devYBottom;

      final int tipWidth = tipSize.x;
      final int tipHeight = tipSize.y;

      int ttPosX;
      int ttPosY;

      if (devHoveredY < devYTop) {

         // remove hovered size
         devHoveredY = devYTop;
      }

      switch (_chartPauseConfig.pauseTooltipPosition) {

      case TOOLTIP_POSITION_LEFT:

         ttPosX = devHoveredX - tipWidth - 1;

         ttPosY = devHoveredY + devHoveredHeight / 2 - tipHeight / 2;

         break;

      case TOOLTIP_POSITION_RIGHT:

         ttPosX = devHoveredX + devHoveredWidth + 1;

         ttPosY = devHoveredY + devHoveredHeight / 2 - tipHeight / 2;

         break;

      case TOOLTIP_POSITION_ABOVE:

         ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
         ttPosY = devHoveredY - tipHeight - 1;

         break;

      case TOOLTIP_POSITION_CHART_TOP:

         ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
         ttPosY = devYTop - tipHeight;

         break;
      case TOOLTIP_POSITION_CHART_BOTTOM:

         ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
         ttPosY = devYBottom;

         break;

      case TOOLTIP_POSITION_BELOW:
      default:

         ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
         ttPosY = devHoveredY + devHoveredHeight + 1;

         break;
      }

      // check if tooltip is left to the chart border
      if (ttPosX + tipWidth < 0) {

         // set tooltip to the graph left border
         ttPosX = -tipWidth - 1;

      } else if (ttPosX > _hoveredLabel.devGraphWidth) {

         // set tooltip to the graph right border
         ttPosX = _hoveredLabel.devGraphWidth;
      }

      if (ttPosY + tipHeight < devYTop) {

         // tooltip is above the graph

         ttPosY = devYTop - tipHeight;

      } else if (ttPosY > devYBottom) {

         // tooltip is below the graph

         ttPosY = devYBottom;
      }

      // check display bounds
      final ChartComponentGraph chartComponentGraph = _tourChart.getChartComponents().getChartComponentGraph();
      final Point tooltipLocation = chartComponentGraph.toDisplay(ttPosX, ttPosY);
      final Point rightBottomBounds = new Point(tipSize.x + tooltipLocation.x, tipSize.y + tooltipLocation.y);
      final Rectangle displayBounds = UI.getDisplayBounds(chartComponentGraph, tooltipLocation);

      if (displayBounds.contains(tooltipLocation) && displayBounds.contains(rightBottomBounds)) {
         return tooltipLocation;
      }

      FixupDisplayBounds(rightBottomBounds,
            devHoveredY,
            devHoveredWidth,
            devHoveredHeight,
            devHoverSize,
            tipWidth,
            tipHeight,
            tooltipLocation);

      return tooltipLocation;
   }

   private void initUI(final Composite parent) {

      final Display display = parent.getDisplay();

      _fgBorder = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
   }

   @Override
   protected void onDispose() {/* Nothing to do */}

   @Override
   protected void onMouseExitToolTip(final MouseEvent mouseEvent) {

      /*
       * When exit tooltip, hide hovered label
       */
      final ChartLayerPause pauseLayer = _tourChart.getLayerTourPause();
      pauseLayer.setTooltipLabel(null);
   }

   @Override
   protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

      /*
       * When in tooltip, the hovered label state is not displayed, keep it displayed
       */
      final ChartLayerPause pauseLayer = _tourChart.getLayerTourPause();
      pauseLayer.setTooltipLabel(_hoveredLabel);
   }

   private void onPaintShellContainer(final PaintEvent paintEvent) {

      final GC gc = paintEvent.gc;
      final Point shellSize = _shellContainer.getSize();

      // draw border
      gc.setForeground(_fgBorder);
      gc.drawRectangle(0, 0, shellSize.x - 1, shellSize.y - 1);
   }

   void open(final ChartLabelPause hoveredLabel) {

      boolean isKeepOpened = false;

      if (hoveredLabel != null && isTooltipClosing()) {

         /**
          * This case occurs when the tooltip is opened but is currently closing and the mouse is
          * moved from the tooltip back to the hovered label.
          * <p>
          * This prevents that when the mouse is over the hovered label but not moved, that the
          * tooltip keeps opened.
          */
         isKeepOpened = true;
      }

      if (hoveredLabel == _hoveredLabel && isKeepOpened == false) {
         // nothing has changed
         return;
      }

      if (hoveredLabel == null) {

         // a pause is not hovered or is hidden, hide tooltip

         hide();

      } else {

         // a pause is hovered, show tooltip
         _hoveredLabel = hoveredLabel;

         showToolTip();
      }
   }

   void setChartPauseConfig(final ChartPauseConfig chartPauseConfig) {

      _chartPauseConfig = chartPauseConfig;
   }

   private void setColors(final Composite container) {

      final Display display = container.getDisplay();

      Color foregroundColor;
      Color backgroundColor;

      if (UI.isDarkTheme()) {

         final Shell shell = _shellContainer.getShell();

         foregroundColor = shell.getForeground();
         backgroundColor = shell.getBackground();

      } else {

         foregroundColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
         backgroundColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      }

      UI.setColorForAllChildren(container, foregroundColor, backgroundColor);
   }

}
