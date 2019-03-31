/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.map;

import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;

import net.tourbook.chart.ColorCache;
import net.tourbook.common.PointLong;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.IPinned_ToolTip;
import net.tourbook.common.tooltip.IPinned_Tooltip_Owner;
import net.tourbook.common.tooltip.Pinned_ToolTip_PinLocation;
import net.tourbook.common.tooltip.Pinned_ToolTip_Shell;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * This tooltip is displayed when mouse is hovering a tour in the map.
 */
public class HoveredTour_ToolTip_UI extends Pinned_ToolTip_Shell implements IPinned_ToolTip {

   private int             _devMouseX;
   private int             _devMouseY;

   /**
    * State if the tooltip is visible.
    */
   private boolean         _isToolTipVisible;

   private int[]           _updateCounter = new int[] { 0 };
   private long            _lastUpdateUITime;
   private boolean         _isHorizontal;

   private IDialogSettings _state;

   private HoveredTourData _currentHoverData;

   /*
    * UI resources
    */

   private final ArrayList<Control> _firstColumnControls          = new ArrayList<>();
   private final ArrayList<Control> _firstColumnContainerControls = new ArrayList<>();

   private final ColorCache         _colorCache                   = new ColorCache();
   private Color                    _fgBorder;

   /*
    * UI controls
    */
   private Composite _shellContainer;
   private Label     _label;

   public HoveredTour_ToolTip_UI(final IPinned_Tooltip_Owner tooltipOwner, final IDialogSettings state) {

      super(tooltipOwner, state);

      _state = state;

      // get state if the tooltip is visible or hidden
      _isToolTipVisible = Util.getStateBoolean(_state,
            Map2View.STATE_IS_SHOW_HOVERED_TOUR_TOOLTIP,
            Map2View.STATE_IS_SHOW_HOVERED_TOUR_TOOLTIP_DEFAULT);

      setPinLocation(Pinned_ToolTip_PinLocation.Screen);
   }

   void actionHideToolTip() {

      _state.put(Map2View.STATE_IS_SHOW_HOVERED_TOUR_TOOLTIP, false);

      _isToolTipVisible = false;

      hide();
   }

   private void createActions() {

   }

   @Override
   protected Composite createToolTipContentArea(final Event event, final Composite parent) {

      initUI(parent);

      createActions();

      final Composite shell = createUI(parent);

      return shell;
   }

   private Composite createUI(final Composite parent) {

      _firstColumnControls.clear();
      _firstColumnContainerControls.clear();

      final Composite shell = createUI_010_Shell(parent);

      updateUI(_currentHoverData);

      if (_isHorizontal == false) {

         // compute width for all controls and equalize column width for the different sections
         _shellContainer.layout(true, true);
         UI.setEqualizeColumWidths(_firstColumnControls);

         _shellContainer.layout(true, true);
         UI.setEqualizeColumWidths(_firstColumnContainerControls);
      }

      return shell;

   }

   private Composite createUI_010_Shell(final Composite parent) {

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory
            .fillDefaults()//
            .spacing(0, 0)
//            .numColumns(2)

            // set margin to draw the border
            .extendedMargins(1, 1, 1, 1)
            .applyTo(_shellContainer);

      _shellContainer.addPaintListener(new PaintListener() {
         @Override
         public void paintControl(final PaintEvent e) {
            onPaintShellContainer(e);
         }
      });
      _shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
         {
            _label = new Label(container, SWT.NONE);
            _label.setText("Tooltip content");
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .hint(200, 50)
                  .applyTo(_label);
         }
      }

      return _shellContainer;
   }

   @Override
   public Shell getToolTipShell() {
      return super.getToolTipShell();
   }

   private void initUI(final Composite parent) {

      _fgBorder = _colorCache.getColor(new RGB(0xe5, 0xe5, 0xcb));
   }

   @Override
   protected boolean isHideTooltipWhenOwnerShellIsInactive() {
      return false;
   }

   public boolean isVisible() {
      return _isToolTipVisible;
   }

   @Override
   protected void onDispose() {

      _colorCache.dispose();

      _firstColumnControls.clear();
      _firstColumnContainerControls.clear();

      super.onDispose();
   }

   private void onPaintShellContainer(final PaintEvent event) {

      final GC gc = event.gc;
      final Point shellSize = _shellContainer.getSize();

      // draw tooltip border
//      gc.setForeground(_fgBorder);
      gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
      gc.drawRectangle(0, 0, shellSize.x - 1, shellSize.y - 1);
   }

   /**
    * Reopens the tooltip at the current position, this will not show the tooltip when it is set to
    * be hidden.
    */
   public void reopen() {

      // hide and recreate it
      hide();
      show(new Point(_devMouseX, _devMouseX));
   }

   @Override
   public void setHoveredData(final int devMouseX, final int devMouseY, final Object hoveredData) {

      if (_isToolTipVisible == false) {
         return;
      }

      _devMouseX = devMouseX;
      _devMouseY = devMouseY;

      if (_shellContainer == null || _shellContainer.isDisposed()) {

         /*
          * tool tip is disposed, this happens on a mouse exit, display the tooltip again
          */
         show(new Point(devMouseX, devMouseY));
      }

      // check again
      if (_shellContainer != null && !_shellContainer.isDisposed()) {

         PointLong devHoveredTourPointLong = null;

         if (hoveredData instanceof HoveredTourData) {

            final HoveredTourData hoveredTourData = (HoveredTourData) hoveredData;
            final Point devHoveredTourPoint = hoveredTourData.allDevHoveredTourPoint.get(0);

            devHoveredTourPointLong = new PointLong(devHoveredTourPoint.x, devHoveredTourPoint.y);

//            System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ()")
//                  + ("\tdevHoveredTourPoint: " + devHoveredTourPoint)
//                  + ("\tdevHoveredTourRect: " + hoveredTourData.devHoveredTourRect)
////                  + ("\t: " + )
//            );
//// TODO remove SYSTEM.OUT.PRINTLN

         }

         setTTShellLocation(devMouseX, devMouseY, devHoveredTourPointLong);

         updateUI(hoveredData);
      }
   }

   public void setIsToolTipVisible(final boolean isToolTipVisible) {

      _isToolTipVisible = isToolTipVisible;

      setShellVisible(isToolTipVisible);
   }

   @Override
   public void setSnapBorder(final int marginTop, final int marginBottom) {

      this.snapBorder_Top = marginTop;
      this.snapBorder_Bottom = marginBottom;
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

      // hide until it displays valid data
      return false;

//      if (_currentHoverData == null) {
//         return false;
//      }

//      return super.shouldCreateToolTip(event);
   }

   @Override
   public void show(final Point location) {

      if (_isToolTipVisible) {
         super.show(location);
      }
   }

   private void updateUI(final Object hoveredData) {

      // get time when the redraw is requested
      final long requestedRedrawTime = System.currentTimeMillis();

      if (requestedRedrawTime > _lastUpdateUITime + 100) {

         // force a redraw

         updateUI_Runnable(hoveredData);

      } else {

         _updateCounter[0]++;

         _shellContainer.getDisplay().asyncExec(new Runnable() {

            final int __runnableCounter = _updateCounter[0];

            @Override
            public void run() {

               // update UI delayed
               if (__runnableCounter != _updateCounter[0]) {
                  // a new update UI occured
                  return;
               }

               updateUI_Runnable(hoveredData);
            }
         });
      }

   }

   private void updateUI_Runnable(final Object hoveredData) {

      if (_shellContainer == null || _shellContainer.isDisposed()) {
         return;
      }

      HoveredTourData hoveredTourData = null;

      if (hoveredData instanceof HoveredTourData) {
         hoveredTourData = (HoveredTourData) hoveredData;
      }

      _currentHoverData = hoveredTourData;

      _lastUpdateUITime = System.currentTimeMillis();

      if (hoveredTourData == null) {

         // nothing is hovered

         _label.setText("");

      } else {

         // a tour is hovered

         final TLongArrayList allHoveredTours = hoveredTourData.allHoveredTourIds;

         final String debugText = allHoveredTours.size() > 0 ? "" + allHoveredTours.get(0) : "";

         _label.setText(debugText);
      }

      _shellContainer.layout(true, true);
//      _shellContainer.pack(true);
   }

}
