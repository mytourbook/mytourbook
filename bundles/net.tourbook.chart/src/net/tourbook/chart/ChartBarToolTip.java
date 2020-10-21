/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell2;
import net.tourbook.common.util.IToolTipProvider;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

public class ChartBarToolTip extends AnimatedToolTipShell2 implements IToolTipProvider {

   private Rectangle _barRectangle;

   private int       _hoveredBar_Serie_VerticalIndex;
   private int       _hoveredBar_Value_HorizontalIndex = -1;

   /*
    * UI controls
    */
   private Chart _chart;

   public ChartBarToolTip(final Chart chart) {

      super(chart);

      _chart = chart;

      setFadeInSteps(2);
      setFadeInDelayTime(10);

      setFadeOutSteps(5);
      setFadeOutDelaySteps(20);

      setBehaviourOnMouseOver(MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
   }

   @Override
   protected void beforeHideToolTip() {

      /*
       * This is the tricky part that the hovered marker is reset before the tooltip is closed and
       * not when nothing is hovered. This ensures that the tooltip has a valid state.
       */
      _hoveredBar_Value_HorizontalIndex = -1;

   }

   @Override
   protected boolean canShowToolTip() {

      if (_hoveredBar_Value_HorizontalIndex != -1 && getChartInfoProvider() != null) {
         return true;
      }

      return false;
   }

   @Override
   protected void createToolTipContentArea(final Composite shell) {

      // get the method which computes the bar info

      final IChartInfoProvider toolTipInfoProvider = getChartInfoProvider();

      if (toolTipInfoProvider == null) {
         return;
      }

      toolTipInfoProvider.createToolTipUI(this, shell, _hoveredBar_Serie_VerticalIndex, _hoveredBar_Value_HorizontalIndex);
   }

   private IChartInfoProvider getChartInfoProvider() {

      return (IChartInfoProvider) _chart.getChartDataModel().getCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER);
   }

   /**
    * By default the tooltip is located to the left side of the tour marker point, when not visible
    * it is displayed to the right side of the tour marker point.
    */
   @Override
   public Point getToolTipLocation(final Point tipSize) {

      final ChartComponentGraph graphControl = _chart.getChartComponents().getChartComponentGraph();

      final int tipWidth = tipSize.x;

      final int barWidth = _barRectangle.width;
      final int barHeight = _barRectangle.height;

      final int barHeightOffset = Math.min(20, barHeight / 3);

      final int ttPosX = _barRectangle.x + barWidth - barWidth / 5;
      final int ttPosY = _barRectangle.y + barHeight - barHeightOffset;

      final Point displayTTLocation = graphControl.toDisplay(ttPosX, ttPosY);

      /*
       * Fixup display bounds
       */
      final Rectangle displayBounds = UI.getDisplayBounds(graphControl, displayTTLocation);
      final Point rightBottomBounds = new Point(tipSize.x + displayTTLocation.x, tipSize.y + displayTTLocation.y);

      final boolean isLocationInDisplay = displayBounds.contains(displayTTLocation);
      final boolean isRightBottomInDisplay = displayBounds.contains(rightBottomBounds);

      if (!isLocationInDisplay || !isRightBottomInDisplay) {

         final int displayX = displayBounds.x;
         final int displayY = displayBounds.y;
         final int displayWidth = displayBounds.width;

         if (displayTTLocation.x < displayX) {
            displayTTLocation.x = displayX;
         }

         if (rightBottomBounds.x > displayX + displayWidth) {
            displayTTLocation.x = displayTTLocation.x - tipWidth - barWidth;
         }

         if (displayTTLocation.y < displayY) {
            // position evaluated with try and error until it fits
            displayTTLocation.y = displayTTLocation.y - ttPosY + graphControl.getSize().y;
         }
      }

      return displayTTLocation;
   }

   @Override
   public void hideToolTip() {

      /*
       * MUST be hidden this way otherwise hide() would close another dialog which is opening..
       */

      hideNow();
   }

   /**
    * Returns <code>true</code> when cursor is within the graph area
    */
   @Override
   protected boolean isInNoHideArea(final Point displayCursorLocation) {

      final ChartComponentGraph graphControl = _chart.getChartComponents().componentGraph;

      final Rectangle graphBounds = graphControl.getBounds();
      final Point graphDisplayPosition = graphControl.toDisplay(0, 0);

      final Rectangle graphDisplayRect = new Rectangle(
            graphDisplayPosition.x,
            graphDisplayPosition.y,
            graphBounds.width,
            graphBounds.height);

      return graphDisplayRect.contains(displayCursorLocation);
   }

   public void open(final Rectangle barInfoFocusRectangle, final int serieIndex, final int valueIndex) {

      boolean isKeepOpened = false;

      if (_hoveredBar_Value_HorizontalIndex != -1 && isTooltipClosing()) {

         /**
          * This case occures when the tooltip is opened but is currently closing and the mouse
          * is moved from the tooltip back to the hovered label.
          * <p>
          * This prevents that when the mouse is over the hovered label but not moved, that the
          * tooltip keeps opened.
          */
         isKeepOpened = true;
      }

      final boolean isCanClose = isKeepOpened == false;

      if (isCanClose
            && valueIndex == _hoveredBar_Value_HorizontalIndex
            && serieIndex == _hoveredBar_Serie_VerticalIndex) {

         // nothing has changed

         return;
      }

      // another bar is hovered, show tooltip

      _barRectangle = barInfoFocusRectangle;

      _hoveredBar_Serie_VerticalIndex = serieIndex;
      _hoveredBar_Value_HorizontalIndex = valueIndex;

      showToolTip();
   }

}
