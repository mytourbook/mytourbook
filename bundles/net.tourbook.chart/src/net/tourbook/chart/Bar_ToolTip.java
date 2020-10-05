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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

public class Bar_ToolTip extends AnimatedToolTipShell2 {

   private int _devX;
   private int _devY;

   private int _hoveredBar_VerticalIndex;
   private int _hoveredBar_HorizontalIndex = -1;

   /*
    * UI controls
    */
   private Chart _chart;

   public Bar_ToolTip(final Chart chart) {

      super(chart);

      _chart = chart;

      setFadeInSteps(0);

      setFadeOutSteps(0);
      setFadeOutDelaySteps(0);

//      setBehaviourOnMouseOver(MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
   }

   @Override
   protected void beforeHideToolTip() {

      /*
       * This is the tricky part that the hovered marker is reset before the tooltip is closed and
       * not when nothing is hovered. This ensures that the tooltip has a valid state.
       */
      _hoveredBar_HorizontalIndex = -1;

   }

   @Override
   protected boolean canShowToolTip() {

      return true;
   }

   @Override
   protected Composite createToolTipContentArea(final Composite shell) {

      return createUI(shell);
   }

   private Composite createUI(final Composite parent) {

      System.out.println((System.currentTimeMillis() + " createUI()"));
// TODO remove SYSTEM.OUT.PRINTLN

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         final Label label = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
         label.setText("Bar Tooltip\n\n"
               + "serieIndex:" + _hoveredBar_VerticalIndex + "\n"
               + "valueIndex:" + _hoveredBar_HorizontalIndex + "\n");

      }

      return container;
   }

   /**
    * By default the tooltip is located to the left side of the tour marker point, when not visible
    * it is displayed to the right side of the tour marker point.
    */
   @Override
   public Point getToolTipLocation(final Point tipSize) {

      final ChartComponentGraph graphControl = _chart.getChartComponents().getChartComponentGraph();
      final IToolBarManager iTbm = _chart.getToolBarManager();

      final ToolBarManager tbm = (ToolBarManager) iTbm;
      final ToolBar toolbarControl = tbm.getControl();

      final int tipWidth = tipSize.x;
      final int tipHeight = tipSize.y;

      final int ttPosX = _devX;
      final int ttPosY = _devY;

      final Point ttLocationX = graphControl.toDisplay(ttPosX, ttPosY);
      final Point ttLocationY = toolbarControl.toDisplay(ttPosX, ttPosY);

      final Point ttLocation = new Point(ttLocationX.x, ttLocationY.y - 1);

      /*
       * Fixup display bounds
       */
      final Rectangle displayBounds = UI.getDisplayBounds(toolbarControl, ttLocation);
      final Point rightBottomBounds = new Point(tipSize.x + ttLocation.x, tipSize.y + ttLocation.y);

      final boolean isLocationInDisplay = displayBounds.contains(ttLocation);
      final boolean isBottomInDisplay = displayBounds.contains(rightBottomBounds);

      if (!(isLocationInDisplay && isBottomInDisplay)) {

         final int displayX = displayBounds.x;
         final int displayY = displayBounds.y;
         final int displayWidth = displayBounds.width;

         if (ttLocation.x < displayX) {
            ttLocation.x = displayX;
         }

         if (rightBottomBounds.x > displayX + displayWidth) {
            ttLocation.x = displayWidth - tipWidth;
         }

         if (ttLocation.y < displayY) {
            // position evaluated with try and error until it fits
            ttLocation.y = ttLocationX.y - ttPosY + graphControl.getSize().y;
         }
      }

      return ttLocation;
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

   public void open(final int devX, final int devY, final int hoveredBar_VerticalIndex, final int hoveredBar_HorizontalIndex) {

      boolean isKeepOpened = false;

      if (_hoveredBar_HorizontalIndex != -1 && isTooltipClosing()) {

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
            && hoveredBar_HorizontalIndex == _hoveredBar_HorizontalIndex
            && hoveredBar_VerticalIndex == _hoveredBar_VerticalIndex) {

         // nothing has changed

         return;
      }

      // another bar is hovered, show tooltip

      _devX = devX;
      _devY = devY;

      _hoveredBar_HorizontalIndex = hoveredBar_HorizontalIndex;
      _hoveredBar_VerticalIndex = hoveredBar_VerticalIndex;

      showToolTip();
   }

}
