/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.calendar;

import java.util.ArrayList;

import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.ToolTip;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourInfoUI;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.calendar.CalendarGraph.CalendarSelectItem;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class CalendarTourInfoToolTip extends ToolTip implements ITourProvider, ITourToolTipProvider {

   private final TourInfoUI   _tourInfoUI = new TourInfoUI();

   private Long               _tourId;
   private TourData           _tourData;

   private Control            _ttControl;

   private CalendarGraph      _calendarGraph;
   private CalendarView       _calendarView;

   private CalendarSelectItem _hoveredItem;

   public CalendarTourInfoToolTip(final CalendarView calendarView) {

      super(calendarView.getCalendarGraph(), NO_RECREATE, false);

      _calendarView = calendarView;

      _ttControl = calendarView.getCalendarGraph();
      _calendarGraph = calendarView.getCalendarGraph();

      setHideOnMouseDown(false);
      setPopupDelay(20);
   }

   @Override
   public void afterHideToolTip() {
      // not used
   }

   @Override
   protected void afterHideToolTip(final Event event) {

      super.afterHideToolTip(event);

      _hoveredItem = null;
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

      Composite container;

      if (_tourId != null && _tourId != -1) {

         // first get data from the tour id when it is set
         _tourData = TourManager.getInstance().getTourData(_tourId);
      }

      if (_tourData == null) {

         // there are no data available

         container = _tourInfoUI.createUI_NoData(parent);

         // prevent that the actions can be selected
         setHideOnMouseDown(true);

      } else {

         // tour data is available

         container = _tourInfoUI.createContentArea(parent, _tourData, this, this);

         _tourInfoUI.setActionsEnabled(true);

         // allow the actions to be selected
         setHideOnMouseDown(false);
      }

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            _tourInfoUI.dispose();
         }
      });

      return container;
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      final CalendarSelectItem hoveredItem = _calendarGraph.getHoveredTour();

      if (hoveredItem.isTour() && hoveredItem.itemRectangle != null) {

         final Rectangle cellBounds = hoveredItem.itemRectangle;
         final int cellWidth2 = cellBounds.width / 2;
         final int cellHeight = cellBounds.height;

         final int devXDefault = cellBounds.x + cellWidth2;// + cellBounds.width; //event.x;
         final int devY = cellBounds.y + cellHeight;

         /*
          * check if the tooltip is outside of the tree, this can happen when the column is very
          * wide and partly hidden
          */
         final Rectangle treeBounds = _ttControl.getBounds();
         boolean isDevXAdjusted = false;
         int devX = devXDefault;

         if (devXDefault >= treeBounds.width) {
            devX = treeBounds.width - 40;
            isDevXAdjusted = true;
         }

         final Rectangle displayBounds = _ttControl.getDisplay().getBounds();

         Point ttDisplayLocation = _ttControl.toDisplay(devX, devY);
         final int tipSizeWidth = tipSize.x;
         final int tipSizeHeight = tipSize.y;

         if (ttDisplayLocation.x + tipSizeWidth > displayBounds.width) {

            /*
             * adjust horizontal position, it is outside of the display, prevent default
             * repositioning
             */

            if (isDevXAdjusted) {

               ttDisplayLocation = _ttControl.toDisplay(devXDefault - cellWidth2 + 20 - tipSizeWidth, devY);

            } else {
               ttDisplayLocation.x = ttDisplayLocation.x - tipSizeWidth;
            }
         }

         if (ttDisplayLocation.y + tipSizeHeight > displayBounds.height) {

            /*
             * adjust vertical position, it is outside of the display, prevent default
             * repositioning
             */

            ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - cellHeight;
         }

         return fixupDisplayBoundsWithMonitor(tipSize, ttDisplayLocation);
      }

      return super.getLocation(tipSize, event);
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      if (_tourData == null) {
         return null;
      }

      final ArrayList<TourData> list = new ArrayList<>();
      list.add(_tourData);

      return list;
   }

   @Override
   protected Object getToolTipArea(final Event event) {

      // Ensure that the tooltip is hidden when the cell is left
      final CalendarSelectItem ttArea = _hoveredItem = _calendarGraph.getHoveredTour();

      return ttArea;
   }

   @Override
   public void hideToolTip() {
      hide();
   }

   @Override
   public void paint(final GC gc, final Rectangle clientArea) {
      // not used
   }

   @Override
   public boolean setHoveredLocation(final int x, final int y) {

      // not used
      return false;
   }

   public void setNoTourTooltip(final String noTourTooltip) {
      _tourInfoUI.setNoTourTooltip(noTourTooltip);
   }

   @Override
   public void setTourToolTip(final TourToolTip tourToolTip) {
      // not used
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

      if (_calendarView.isShowTourTooltip() == false) {
         return false;
      }

      if (!super.shouldCreateToolTip(event)) {
         return false;
      }

      if (_hoveredItem == null || _hoveredItem.id < 0) {
         return false;
      }

      // get tour id from hovered item
      _tourId = _hoveredItem.id;

      return true;
   }
}
