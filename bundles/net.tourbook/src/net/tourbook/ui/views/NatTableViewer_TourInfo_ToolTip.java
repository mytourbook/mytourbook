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
package net.tourbook.ui.views;

import java.util.ArrayList;

import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.ToolTip;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourInfoUI;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class NatTableViewer_TourInfo_ToolTip extends ToolTip implements ITourProvider, ITourToolTipProvider {

   private final TourInfoUI _tourInfoUI = new TourInfoUI();

   private Long             _tourId;
   private TourData         _tourData;

   /**
    * Tooltip control
    */
   private NatTable         _ttControl;

//   private ColumnViewer       _columnViewer;
//   private ViewerCell         _viewerCell;
//   private Object             _viewerCell_Data;

   private ITooltipUIProvider _tooltipUIProvider;

   private NatTable           _natTable;

   private TourBookView       _tourBookView;

   public NatTableViewer_TourInfo_ToolTip(final TourBookView tourBookView, final int style) {

      super(tourBookView.getNatTable(), style, false);

      _tourBookView = tourBookView;
      _natTable = tourBookView.getNatTable();

      _ttControl = _natTable;
//      _columnViewer = columnViewer;

      setHideOnMouseDown(false);
   }

   @Override
   public void afterHideToolTip() {
      // not used
   }

   @Override
   protected void afterHideToolTip(final Event event) {

      super.afterHideToolTip(event);

//      _viewerCell = null;
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

//      Composite container;
//
//      if (_viewerCell_Data != null && _tooltipUIProvider != null) {
//
//         // a cell with custom data is hovered
//
//         container = _tooltipUIProvider.createTooltipUI(parent, _viewerCell_Data, this);
//
//         // allow the actions to be selected
//         setHideOnMouseDown(false);
//
//      } else {
//
//         // a tour is hovered
//
//         if (_tourId != null && _tourId != -1) {
//
//            // first get data from the tour id when it is set
//            _tourData = TourManager.getInstance().getTourData(_tourId);
//         }
//
//         if (_tourData == null) {
//
//            // there are no data available
//
//            container = _tourInfoUI.createUI_NoData(parent);
//
//            // allow the actions to be selected
//            setHideOnMouseDown(true);
//
//         } else {
//
//            // tour data is available
//
//            container = _tourInfoUI.createContentArea(parent, _tourData, this, this);
//
//            _tourInfoUI.setActionsEnabled(true);
//
//            // allow the actions to be selected
//            setHideOnMouseDown(false);
//         }
//
//         parent.addDisposeListener(new DisposeListener() {
//            @Override
//            public void widgetDisposed(final DisposeEvent e) {
//               _tourInfoUI.dispose();
//            }
//         });
//      }
//
//      return container;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         final Label label = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(label);
         label.setText("nat table tooltip");
      }

      return container;
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

//      // try to position the tooltip at the bottom of the cell
//      final ViewerCell cell = _columnViewer.getCell(new Point(event.x, event.y));
//
//      if (cell != null) {
//
//         final Rectangle cellBounds = cell.getBounds();
//         final int cellWidth2 = cellBounds.width / 2;
//         final int cellHeight = cellBounds.height;
//
//         final int devXDefault = cellBounds.x + cellWidth2;// + cellBounds.width; //event.x;
//         final int devY = cellBounds.y + cellHeight;
//
//         /*
//          * check if the tooltip is outside of the tree, this can happen when the column is very
//          * wide and partly hidden
//          */
//         final Rectangle treeBounds = _ttControl.getBounds();
//         boolean isDevXAdjusted = false;
//         int devX = devXDefault;
//
//         if (devXDefault >= treeBounds.width) {
//            devX = treeBounds.width - 40;
//            isDevXAdjusted = true;
//         }
//
//         final Rectangle displayBounds = _ttControl.getDisplay().getBounds();
//
//         Point ttDisplayLocation = _ttControl.toDisplay(devX, devY);
//         final int tipSizeWidth = tipSize.x;
//         final int tipSizeHeight = tipSize.y;
//
//         if (ttDisplayLocation.x + tipSizeWidth > displayBounds.width) {
//
//            /*
//             * adjust horizontal position, it is outside of the display, prevent default
//             * repositioning
//             */
//
//            if (isDevXAdjusted) {
//
//               ttDisplayLocation = _ttControl.toDisplay(devXDefault - cellWidth2 + 20 - tipSizeWidth, devY);
//
//            } else {
//               ttDisplayLocation.x = ttDisplayLocation.x - tipSizeWidth;
//            }
//         }
//
//         if (ttDisplayLocation.y + tipSizeHeight > displayBounds.height) {
//
//            /*
//             * adjust vertical position, it is outside of the display, prevent default
//             * repositioning
//             */
//
//            ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - cellHeight;
//         }
//
//         return fixupDisplayBoundsWithMonitor(tipSize, ttDisplayLocation);
//      }

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

      if (event.data == null) {
         final MouseEvent mouseEvent = new MouseEvent(event);
         event.data = NatEventData.createInstanceFromEvent(mouseEvent);
      }

      final NatEventData natEventData = (NatEventData) event.data;

      final int columnPosition = natEventData.getColumnPosition();
      int rowPosition = natEventData.getRowPosition();

      final int columnIndex = _natTable.getColumnIndexByPosition(columnPosition);

      rowPosition = _tourBookView.getNatTableLayer_Viewport().getRowPositionByY(event.y);
      final Point cellPos = _tourBookView.getNatTableLayer_Hover().getCurrentHoveredCellPosition();

//      final ColumnDefinition colDef = _ttControl._columnManager.getVisibleAndSortedColumns().get(columnIndex);

//      return colDef;

      System.out.println((System.currentTimeMillis() + " " + columnPosition + " / " + rowPosition));
      System.out.println((System.currentTimeMillis() + " " + cellPos.x + " / " + cellPos.y));
      // TODO remove SYSTEM.OUT.PRINTLN

      return null;

//      _viewerCell = _columnViewer.getCell(new Point(event.x, event.y));
//      _viewerCell_Data = null;
//
//      if (_viewerCell != null) {
//
//         /*
//          * Get tour id from hovered cell label provider
//          */
//         Long tourId = null;
//         final CellLabelProvider labelProvider = _columnViewer.getLabelProvider(_viewerCell.getColumnIndex());
//
//         if (labelProvider instanceof IColumnViewerTourIdProvider) {
//
//            final IColumnViewerTourIdProvider columnViewerTourIdProvider = (IColumnViewerTourIdProvider) labelProvider;
//
//            tourId = columnViewerTourIdProvider.getTourId(_viewerCell);
//
//            _viewerCell_Data = columnViewerTourIdProvider.getData(_viewerCell);
//         }
//
//         _tourId = tourId;
//
//         // hide current tooltip when a cell without tooltip is hovered
//         if (tourId == null && _viewerCell_Data == null) {
//            _viewerCell = null;
//         }
//      }
//
//      return _viewerCell;
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
         return false;
      }

//      if (_viewerCell == null) {
//         return false;
//      }

      final boolean isShowTooltip = false;

//      if (_tourId == null && _viewerCell_Data == null) {
//
//         // show default tooltip
//         _ttControl.setToolTipText(null);
//
//      } else {
//
//         // hide default tooltip and display the custom tooltip
//         _ttControl.setToolTipText(UI.EMPTY_STRING);
//
//         isShowTooltip = true;
//      }

      return isShowTooltip;
   }
}
