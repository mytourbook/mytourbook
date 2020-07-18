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
package net.tourbook.ui.views;

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.NatTable_LabelProvider;
import net.tourbook.common.util.NatTable_LabelProvider_WithTourTooltip;
import net.tourbook.common.util.ToolTip;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourInfoUI;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.tourBook.TVITourBookTour;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class NatTableViewer_TourInfo_ToolTip extends ToolTip implements ITourProvider, ITourToolTipProvider {

   private final TourInfoUI   _tourInfoUI = new TourInfoUI();

   private TourData           _tourData;

   /**
    * Tooltip control
    */
   private NatTable           _ttControl;

   private ITooltipUIProvider _tooltipUIProvider;

   private NatTable           _natTable;

   private TourBookView       _tourBookView;

   private Point              _tooltipCellPos;
   private Long               _hoveredTourId;
   private Rectangle          _hoveredBounds;

   private Object             _viewerCell_Data;

   public NatTableViewer_TourInfo_ToolTip(final TourBookView tourBookView, final int style) {

      super(tourBookView.getNatTable(), style, false);

      _tourBookView = tourBookView;
      _natTable = tourBookView.getNatTable();

      _ttControl = _natTable;

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

      _tooltipCellPos = null;
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

      Composite container;

      if (_viewerCell_Data != null && _tooltipUIProvider != null) {

         // a cell with custom data is hovered

         container = _tooltipUIProvider.createTooltipUI(parent, _viewerCell_Data, this);

         // allow the actions to be selected
         setHideOnMouseDown(false);

      } else {

         // a tour is hovered

         if (_hoveredTourId != null && _hoveredTourId != -1) {

            // first get data from the tour id when it is set
            _tourData = TourManager.getInstance().getTourData(_hoveredTourId);
         }

         if (_tourData == null) {

            // there are no data available

            container = _tourInfoUI.createUI_NoData(parent);

            // allow the actions to be selected
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
      }

      return container;
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      // try to position the tooltip at the bottom of the cell

      if (_hoveredBounds != null) {

         final Rectangle cellBounds = _hoveredBounds;
         final int cellWidth2 = cellBounds.width / 2;
         final int cellHeight = cellBounds.height;

         final int devXDefault = cellBounds.x + cellWidth2;// + cellBounds.width; //event.x;
         final int devY = cellBounds.y + cellHeight;

         /*
          * check if the tooltip is outside of the control, this can happen when the column is very
          * wide and partly hidden
          */
         final Rectangle controlBounds = _ttControl.getBounds();
         boolean isDevXAdjusted = false;
         int devX = devXDefault;

         if (devXDefault >= controlBounds.width) {
            devX = controlBounds.width - 40;
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

      _hoveredTourId = null;
      _hoveredBounds = null;
      _tooltipCellPos = null;

      final int colPosByX = _natTable.getColumnPositionByX(event.x);
      final int rowPosByY = _natTable.getRowPositionByY(event.y);

      if (colPosByX <= 0 || rowPosByY <= 0) {

         // first column or first row (this is the row number or table header) or an empty nattable (rowPosByY == -1)

         return null;
      }

// !!! this do not work for freezed columns !!!
//      _hoveredCellPos = _tourBookView.getNatTableLayer_Hover().getCurrentHoveredCellPosition();

// NatTable advanced: With lot of debugging found solution to get absolute row from relative row
      final int hoveredRowPosition = _tourBookView.getNatTableLayer_Viewport().localToUnderlyingRowPosition(rowPosByY - 1);

      // get hovered label provider from the column, this is needed to show the tour tooltip only for specific columns
      final int hoveredColumnIndex = _natTable.getColumnIndexByPosition(colPosByX);
      if (hoveredColumnIndex == -1) {

         // a cell is not hovered

         _tooltipCellPos = null;

      } else {

         _tooltipCellPos = new Point(colPosByX, rowPosByY);

         final ArrayList<ColumnDefinition> visibleAndSortedColumns = _tourBookView.getNatTable_ColumnManager().getVisibleAndSortedColumns();
         final ColumnDefinition colDef = visibleAndSortedColumns.get(hoveredColumnIndex);

//         System.out.println((System.currentTimeMillis()
//               + "  colPosByX:" + colPosByX
//               + "  rowPosByY:" + rowPosByY
//               + "  hoveredRowPosition:" + hoveredRowPosition
//               + "  colIdx:" + hoveredColumnIndex
//               + "  " + colDef.getColumnId()));
//// TODO remove SYSTEM.OUT.PRINTLN

         // hide current tooltip when a cell without tooltip is hovered
         final NatTable_LabelProvider labelProvider = colDef.getNatTable_LabelProvider();
         if (labelProvider instanceof NatTable_LabelProvider_WithTourTooltip) {

            final NatTable_LabelProvider_WithTourTooltip tooltipLabelProvider = (NatTable_LabelProvider_WithTourTooltip) labelProvider;
            if (tooltipLabelProvider.isShowTooltip() == false) {
               _tooltipCellPos = null;
            }

         } else {

            _tooltipCellPos = null;
         }

      }

      if (_tooltipCellPos != null) {

         // get hovered tour id
         final TVITourBookTour hoveredTourItem = _tourBookView.getNatTable_DataProvider().getRowObject(hoveredRowPosition);
         _hoveredTourId = hoveredTourItem.tourId;

         final int devX = _natTable.getStartXOfColumnPosition(colPosByX);
         final int devY = _natTable.getStartYOfRowPosition(rowPosByY);
         final int cellWidth = _natTable.getColumnWidthByPosition(colPosByX);
         final int cellHeight = _natTable.getRowHeightByPosition(rowPosByY);

         _hoveredBounds = new Rectangle(devX, devY, cellWidth, cellHeight);
      }

      return _tooltipCellPos;
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

   public void setTooltipUIProvider(final ITooltipUIProvider tooltipUIProvider) {
      _tooltipUIProvider = tooltipUIProvider;
   }

   @Override
   public void setTourToolTip(final TourToolTip tourToolTip) {
      // not used
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {


      if (!super.shouldCreateToolTip(event)) {

         // ensure default tooltip is displayed

// it seems that a default tooltip is not supported in natnable
//         _ttControl.setToolTipText(null);

         return false;
      }

      if (_tooltipCellPos == null) {
         return false;
      }

      boolean isShowTooltip = false;

      if (_hoveredTourId == null && _tooltipCellPos == null) {

         // show default tooltip
         _ttControl.setToolTipText(null);

      } else {

         // hide default tooltip and display the custom tooltip
         _ttControl.setToolTipText(UI.EMPTY_STRING);

         isShowTooltip = true;
      }

      return isShowTooltip;
   }
}
