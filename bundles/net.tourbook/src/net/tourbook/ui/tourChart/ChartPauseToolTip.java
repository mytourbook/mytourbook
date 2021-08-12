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

import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartComponentGraph;
import net.tourbook.chart.ColorCache;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.ui.ITourProvider;
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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

   private static final String  GRAPH_LABEL_TIME    = net.tourbook.common.Messages.Graph_Label_Time;

   private static final int     DEFAULT_TEXT_WIDTH  = 50;
   private static final int     DEFAULT_TEXT_HEIGHT = 20;

   /**
    * Visual position for marker tooltip, they must correspond to the position id
    * TOOLTIP_POSITION_*.
    */
   public static final String[] TOOLTIP_POSITIONS;

   static {

      TOOLTIP_POSITIONS = new String[] {

            Messages.Tour_Marker_TooltipPosition_Left, //            0
            Messages.Tour_Marker_TooltipPosition_Right, //           1
            Messages.Tour_Marker_TooltipPosition_Top, //             2
            Messages.Tour_Marker_TooltipPosition_Bottom, //          3
            Messages.Tour_Marker_TooltipPosition_ChartTop, //        4
            Messages.Tour_Marker_TooltipPosition_ChartBottom, //     5
      };
   }

   private static final int                TOOLTIP_POSITION_LEFT         = 0;
   private static final int                TOOLTIP_POSITION_RIGHT        = 1;
   private static final int                TOOLTIP_POSITION_ABOVE        = 2;
   private static final int                TOOLTIP_POSITION_BELOW        = 3;
   private static final int                TOOLTIP_POSITION_CHART_TOP    = 4;
   private static final int                TOOLTIP_POSITION_CHART_BOTTOM = 5;

   public static final int                 DEFAULT_TOOLTIP_POSITION      = TOOLTIP_POSITION_BELOW;

   private static final int                _textStyle                    = SWT.WRAP               //
         | SWT.MULTI
         | SWT.READ_ONLY
//                                                               | SWT.BORDER
   ;

   private PixelConverter                  _pc;

   private int                             _defaultTextWidth;
   private int                             _defaultTextHeight;

   private TourChart                       _tourChart;
   private TourData                        _tourData;

   private ChartLabelPause                 _hoveredLabel;

   /**
    * When <code>true</code> the actions are displayed, e.g. to open the marker dialog.
    */
   private boolean                         _isShowActions;

   private ChartPauseConfig                _chartPauseConfig;

   private ActionOpenMarkerDialogInTooltip _actionOpenMarkerDialog;

   /*
    * UI resources
    */
   private Color      _fgBorder;
   private ColorCache _colorCache;

   /*
    * UI controls
    */
   private Composite _shellContainer;
   private Composite _ttContainer;

   private class ActionOpenMarkerDialogInTooltip extends ActionOpenMarkerDialog {

      public ActionOpenMarkerDialogInTooltip() {
         super(ChartPauseToolTip.this, true);
      }

      @Override
      public void run() {

         hideNow();

         super.run();
      }
   }

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
       * This is the tricky part that the hovered marker is reset before the tooltip is closed and
       * not when nothing is hovered. This ensures that the tooltip has a valid state.
       */
      _hoveredLabel = null;
   }

   @Override
   protected boolean canShowToolTip() {

      return _hoveredLabel != null;
   }

   private void createActions() {

      final boolean isSingleTour = _tourData.isMultipleTours() == false;

      _actionOpenMarkerDialog = new ActionOpenMarkerDialogInTooltip();

      // setup action for the current tour marker
      _actionOpenMarkerDialog.setEnabled(isSingleTour);
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

      createActions();

      final Composite container = createUI(shell);

      setColors(container);

      // compute width for all controls and equalize column width for the different sections
//      _ttContainer.layout(true, true);
//      UI.setEqualizeColumWidths(_firstColumnControls, 10);

//      _ttContainer.layout(true, true);

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
//            .spacing(0, 0)
//            .numColumns(2)
            // set margin to draw the border
            .extendedMargins(1, 1, 1, 1)
            .applyTo(_shellContainer);
//      _shellContainer.setForeground(_fgColor);
//      _shellContainer.setBackground(_bgColor);
      _shellContainer.addPaintListener(this::onPaintShellContainer);
      {
         _ttContainer = new Composite(_shellContainer, SWT.NONE);
         GridLayoutFactory.fillDefaults()
               .extendedMargins(2, 5, 2, 5)
               .numColumns(1)
               .applyTo(_ttContainer);
//         _ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
//         _ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            createUI_70_Values(_ttContainer);
         }
      }

      return _shellContainer;
   }

   private void createUI_70_Values(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(3, 0)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .applyTo(container);
      {

         UI.createLabel(container, "Start time");//GRAPH_LABEL_TIME);

         createUI_72_ValueField(
               container,
               _hoveredLabel.getPausedTime_Start(),
               _hoveredLabel.getTimeZoneId());

         UI.createLabel(container, "End time");//GRAPH_LABEL_TIME);
         createUI_72_ValueField(
               container,
               _hoveredLabel.getPausedTime_End(),
               _hoveredLabel.getTimeZoneId());

      }
   }

   private void createUI_72_ValueField(final Composite parent,
                                       final long value,
                                       final String dbTimeZoneId) {

      // Value
      final Label label = new Label(parent, SWT.TRAIL);
      GridDataFactory.fillDefaults()
            .align(SWT.END, SWT.FILL)
            .indent(10, 0)
            .applyTo(label);

      final ZonedDateTime tourZonedDateTime = TimeTools.createTourDateTime(value, dbTimeZoneId).tourZonedDateTime;

      final String format_hh_mm_ss = UI.format_yyyymmdd_hhmmss(tourZonedDateTime.getYear(),
            tourZonedDateTime.getMonthValue(),
            tourZonedDateTime.getDayOfMonth(),
            tourZonedDateTime.getHour(),
            tourZonedDateTime.getMinute(),
            tourZonedDateTime.getSecond());
      label.setText(format_hh_mm_ss);
   }

   /**
    * This is copied from {@link ChartLayerMarker#drawOverlay()}.
    * <p>
    * The region which is computed in drawOverlay() cannot be used because the overlay is painted
    * <b>after</b> the tooltip is displayed and <b>not before.</b>
    */
   private Rectangle getHoveredRect() {

      final int hoverSize = _hoveredLabel.devHoverSize;

      Rectangle rectHovered = new Rectangle(_hoveredLabel.devXPause, _hoveredLabel.devYPause, 1, 1);

      int devMarkerPointSize = 1;
      if (devMarkerPointSize < 1) {
         devMarkerPointSize = 1;
      }

      final int devMarkerX = _hoveredLabel.devXPause - hoverSize;
      final int devMarkerY = _hoveredLabel.devYPause - hoverSize;
      final int devMarkerSize = devMarkerPointSize + 2 * hoverSize;

      final Rectangle rectMarker = new Rectangle(devMarkerX, devMarkerY, devMarkerSize, devMarkerSize);
      rectHovered = rectHovered.union(rectMarker);

      // add label rect
      if (_hoveredLabel.paintedLabel != null) {

         final Rectangle paintedLabel = _hoveredLabel.paintedLabel;
         final int devLabelWidthRaw = paintedLabel.width;

         if (devLabelWidthRaw > 0) {

            final int devLabelX = paintedLabel.x - hoverSize;
            final int devLabelY = paintedLabel.y - hoverSize;
            final int devLabelWidth = devLabelWidthRaw + 2 * hoverSize;
            final int devLabelHeight = paintedLabel.height + 2 * hoverSize;

            final Rectangle rectLabel = new Rectangle(devLabelX, devLabelY, devLabelWidth, devLabelHeight);

            rectHovered = rectHovered.union(rectLabel);
         }
      }

      return rectHovered;
   }

   /**
    * @param hoveredLabel
    * @return Returns a {@link TourMarker} when a chart label (marker) is hovered or
    *         <code>null</code> when a marker is not hovered.
    */
   private TourMarker getHoveredTourMarker(final ChartLabel hoveredLabel) {

      TourMarker tourMarker = null;

      if (hoveredLabel.data instanceof TourMarker) {
         tourMarker = (TourMarker) hoveredLabel.data;
      }

      return tourMarker;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> tours = new ArrayList<>();
      tours.add(_tourChart.getTourData());

      return tours;
   }

   /**
    * By default the tooltip is located to the left side of the tour marker point, when not visible
    * it is displayed to the right side of the tour marker point.
    */
   @Override
   public Point getToolTipLocation(final Point tipSize) {

      final Rectangle hoveredRect = getHoveredRect();

      final int devHoveredX = hoveredRect.x;
      int devHoveredY = hoveredRect.y;
      final int devHoveredWidth = hoveredRect.width;
      final int devHoveredHeight = hoveredRect.height;
      final int devHoverSize = _hoveredLabel.devHoverSize;

      final int devYTop = _hoveredLabel.devYTop;
      final int devYBottom = _hoveredLabel.devYBottom;
      final boolean isVertical = _hoveredLabel.devIsVertical;

      final int tipWidth = tipSize.x;
      final int tipHeight = tipSize.y;

      int ttPosX;
      int ttPosY;

      if (devHoveredY < devYTop) {
         // remove hovered size
         devHoveredY = devYTop;
      }

      ttPosX = devHoveredX - tipWidth - 1;

      if (isVertical) {
         ttPosY = devHoveredY;
      } else {
         ttPosY = devHoveredY + devHoveredHeight / 2 - tipHeight / 2;
      }

      // ckeck if tooltip is left to the chart border
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
//      final Point dispPos = chartComponentGraph.toDisplay(ttPosX, ttPosY);

//      if (dispPos.x < 0) {
//
//         // tooltip is outside of the display, set tooltip to the right of the tour marker
//         ttPosX = devHoveredRight + 1;
//      }

      final Point graphLocation = chartComponentGraph.toDisplay(0, 0);
      final Point ttLocation = chartComponentGraph.toDisplay(ttPosX, ttPosY);

      /*
       * Fixup display bounds
       */
      final Rectangle displayBounds = UI.getDisplayBounds(chartComponentGraph, ttLocation);
      final Point rightBottomBounds = new Point(tipSize.x + ttLocation.x, tipSize.y + ttLocation.y);

      if (!(displayBounds.contains(ttLocation) && displayBounds.contains(rightBottomBounds))) {

         final int displayX = displayBounds.x;
         final int displayY = displayBounds.y;
         final int displayWidth = displayBounds.width;
         final int displayHeight = displayBounds.height;

         if (ttLocation.x < displayX) {

            ttLocation.x = ttLocation.x + tipWidth + devHoveredWidth + 2;

         }

         if (rightBottomBounds.x > displayX + displayWidth) {

            ttLocation.x = ttLocation.x - tipWidth - devHoveredWidth - 2;

         }

         if (ttLocation.y < displayY) {

            ttLocation.y = graphLocation.y + devHoveredY + devHoveredHeight - devHoverSize + 2;

         }

         if (rightBottomBounds.y > displayY + displayHeight) {

            ttLocation.y = graphLocation.y + devHoveredY - tipHeight - 1;

         }
      }

      return ttLocation;
   }

   private void initUI(final Composite parent) {

      final Display display = parent.getDisplay();

      _pc = new PixelConverter(parent);
      _defaultTextWidth = _pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH);
      _defaultTextHeight = _pc.convertHeightInCharsToPixels(DEFAULT_TEXT_HEIGHT);

      _colorCache = new ColorCache();
      _fgBorder = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
   }

   @Override
   protected void onDispose() {

      _colorCache.dispose();
   }

   @Override
   protected void onMouseExitToolTip(final MouseEvent mouseEvent) {

      /*
       * When exit tooltip, hide hovered label
       */
      final ChartLayerMarker markerLayer = _tourChart.getLayerTourMarker();
      markerLayer.setTooltipLabel(null);
   }

   @Override
   protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

      /*
       * When in tooltip, the hovered label state is not displayed, keep it displayed
       */
      final ChartLayerMarker markerLayer = _tourChart.getLayerTourMarker();
      //markerLayer.setTooltipLabel(_hoveredLabel);
   }

   private void onPaintShellContainer(final PaintEvent paintEvent) {

      final GC gc = paintEvent.gc;
      final Point shellSize = _shellContainer.getSize();

      // draw border
      gc.setForeground(_fgBorder);
      gc.drawRectangle(0, 0, shellSize.x - 1, shellSize.y - 1);
   }

   private void onSelectUrl(final String address) {

      WEB.openUrl(address);

      // close tooltip when a link is selected
      hideNow();
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

      if (hoveredLabel == null /* || hoveredLabel.paintedLabel == null */) {

         // a marker is not hovered or is hidden, hide tooltip

         hide();

      } else {

         // another marker is hovered, show tooltip

         _hoveredLabel = hoveredLabel;
         //_hoveredTourMarker = getHoveredTourMarker(hoveredLabel);

         System.out.println("HOVEREDPAUSE");
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

      UI.setColorForAllChildren(container,
            foregroundColor,
            backgroundColor);
   }

   void setIsShowMarkerActions(final boolean isShowMarkerActions) {

      _isShowActions = isShowMarkerActions;
   }

}
