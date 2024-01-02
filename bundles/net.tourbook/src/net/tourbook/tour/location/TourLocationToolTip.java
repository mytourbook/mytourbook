/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.ui.SelectionCellLabelProvider_WithLocationTooltip;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.NatTable_LabelProvider;
import net.tourbook.common.util.NatTable_LabelProvider_WithLocationTooltip;
import net.tourbook.common.util.ToolTip;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.location.TourLocationView.LocationItem;
import net.tourbook.ui.Messages;
import net.tourbook.ui.views.tourBook.TVITourBookTour;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class TourLocationToolTip extends ToolTip {

   private static final String USAGE_VALUES = "%d   ∙   %d   ∙   %d"; //$NON-NLS-1$

   private static final int    SHELL_MARGIN = 5;

   private Control             _ttControl;

   private ColumnViewer        _columnViewer;
   private TourBookView        _tourBookView;
   private TourLocationView    _tourLocationView;

   private boolean             _isLocationView;
   private boolean             _isTourBookView;

   private boolean             _isNatTableView;
   private boolean             _isStartLocation;
   private boolean             _isShowTooltip;

   // COL: Column viewer
   private ViewerCell   _colViewerCell;
   private LocationItem _colLocationItem;

   // NAT: Nattable viewer
   private NatTable     _natNatTable;
   private Rectangle    _natHoveredBounds;
   private Long         _natHoveredTourId;
   private Point        _natTooltipCellPos;

   private TourLocation _tourLocation;

   /*
    * UI controls
    */
   private Composite _ttContainer;

   public TourLocationToolTip(final TourBookView tourBookView, final boolean isNatTable) {

      super(isNatTable

            ? tourBookView.getNatTable()
            : tourBookView.getTreeViewer().getTree(),

            NO_RECREATE,
            false);

      _isTourBookView = true;

      _tourBookView = tourBookView;
      _isNatTableView = isNatTable;

      if (_isNatTableView) {

         _natNatTable = tourBookView.getNatTable();

         _ttControl = _natNatTable;

      } else {

         _ttControl = tourBookView.getTreeViewer().getTree();

         _columnViewer = tourBookView.getTreeViewer();
      }

      setHideOnMouseDown(false);

      setPopupDelay(20);
   }

   public TourLocationToolTip(final TourLocationView tourLocationView) {

      super(tourLocationView.getLocationViewer().getTable(), NO_RECREATE, false);

      _isLocationView = true;

      _tourLocationView = tourLocationView;

      final TableViewer locationViewer = tourLocationView.getLocationViewer();

      _ttControl = locationViewer.getTable();
      _columnViewer = locationViewer;

      setHideOnMouseDown(false);

      setPopupDelay(20);
   }

   @Override
   protected void afterHideToolTip(final Event event) {

      super.afterHideToolTip(event);

      _colViewerCell = null;
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

      final Composite container = createUI(parent);

      // compute width for all controls and equalize column width for the different sections
      _ttContainer.layout(true, true);

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         _ttContainer = new Composite(shellContainer, SWT.NONE);
         GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(_ttContainer);
         {
            createUI_10_Info(_ttContainer);
         }
      }

      final Display display = parent.getDisplay();

      final Color bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      final Color fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      UI.setChildColors(shellContainer.getShell(), fgColor, bgColor);

      return shellContainer;
   }

   private void createUI_10_Info(final Composite parent) {

      final GridDataFactory headerIndent = GridDataFactory.fillDefaults()

            .span(2, 1)

            // indent to the left that this text is aligned with the labels
            .indent(-4, 0);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 3).applyTo(container);
      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Title
             */

            String locationTitle;

            if (_isTourBookView) {

               locationTitle = _isStartLocation

                     ? Messages.Tour_Tooltip_Label_LocationStart
                     : Messages.Tour_Tooltip_Label_LocationEnd;

            } else {

               locationTitle = Messages.Tour_Location_Title;
            }

            // using text control that & is not displayed as mnemonic
            final Text headerText = new Text(container, SWT.READ_ONLY);
            headerIndent.applyTo(headerText);
            MTFont.setBannerFont(headerText);

            headerText.setText(locationTitle);
         }

         UI.createSpacer_Vertical(container, 8, 2);

         {
            /*
             * Display name
             */

            final String displayName = _tourLocation.display_name;

            if (displayName != null && displayName.length() > 0) {

               final Text text = new Text(container, SWT.READ_ONLY | SWT.WRAP);
               headerIndent.applyTo(text);

               text.setText(displayName);

               setMaxContentWidth(text);
            }
         }

         UI.createSpacer_Vertical(container, 16, 2);

// SET_FORMATTING_OFF

         createUI_Content(container,   _tourLocation.name,                  Messages.Tour_Location_Part_OsmName);

         createUI_Content(container,   _tourLocation.country,               Messages.Tour_Location_Part_Country);
         createUI_Content(container,   _tourLocation.country_code,          Messages.Tour_Location_Part_CountryCode);
         createUI_Content(container,   _tourLocation.continent,             Messages.Tour_Location_Part_Continent);

         createUI_Content(container,   _tourLocation.region,                Messages.Tour_Location_Part_Region);
         createUI_Content(container,   _tourLocation.state,                 Messages.Tour_Location_Part_State);
         createUI_Content(container,   _tourLocation.state_district,        Messages.Tour_Location_Part_StateDistrict);
         createUI_Content(container,   _tourLocation.county,                Messages.Tour_Location_Part_County);

         createUI_Content(container,   _tourLocation.municipality,          Messages.Tour_Location_Part_Municipality);
         createUI_Content(container,   _tourLocation.city,                  Messages.Tour_Location_Part_City);
         createUI_Content(container,   _tourLocation.town,                  Messages.Tour_Location_Part_Town);
         createUI_Content(container,   _tourLocation.village,               Messages.Tour_Location_Part_Village);
         createUI_Content(container,   _tourLocation.postcode,              Messages.Tour_Location_Part_Postcode);

         createUI_Content(container,   _tourLocation.road,                  Messages.Tour_Location_Part_Road);
         createUI_Content(container,   _tourLocation.house_number,          Messages.Tour_Location_Part_HouseNumber);
         createUI_Content(container,   _tourLocation.house_name,            Messages.Tour_Location_Part_HouseName);

         createUI_Content(container,   _tourLocation.city_district,         Messages.Tour_Location_Part_CityDistrict);
         createUI_Content(container,   _tourLocation.district,              Messages.Tour_Location_Part_District);
         createUI_Content(container,   _tourLocation.borough,               Messages.Tour_Location_Part_Borough);
         createUI_Content(container,   _tourLocation.suburb,                Messages.Tour_Location_Part_Suburb);
         createUI_Content(container,   _tourLocation.subdivision,           Messages.Tour_Location_Part_Subdivision);

         createUI_Content(container,   _tourLocation.hamlet,                Messages.Tour_Location_Part_Hamlet);
         createUI_Content(container,   _tourLocation.croft,                 Messages.Tour_Location_Part_Croft);
         createUI_Content(container,   _tourLocation.isolated_dwelling,     Messages.Tour_Location_Part_IsolatedDwelling);

         createUI_Content(container,   _tourLocation.neighbourhood,         Messages.Tour_Location_Part_Neighbourhood);
         createUI_Content(container,   _tourLocation.allotments,            Messages.Tour_Location_Part_Allotments);
         createUI_Content(container,   _tourLocation.quarter,               Messages.Tour_Location_Part_Quarter);

         createUI_Content(container,   _tourLocation.city_block,            Messages.Tour_Location_Part_CityBlock);
         createUI_Content(container,   _tourLocation.residential,           Messages.Tour_Location_Part_Residential);
         createUI_Content(container,   _tourLocation.farm,                  Messages.Tour_Location_Part_Farm);
         createUI_Content(container,   _tourLocation.farmyard,              Messages.Tour_Location_Part_Farmyard);
         createUI_Content(container,   _tourLocation.industrial,            Messages.Tour_Location_Part_Industrial);
         createUI_Content(container,   _tourLocation.commercial,            Messages.Tour_Location_Part_Commercial);
         createUI_Content(container,   _tourLocation.retail,                Messages.Tour_Location_Part_Retail);

         createUI_Content(container,   _tourLocation.aerialway,             Messages.Tour_Location_Part_Aerialway);
         createUI_Content(container,   _tourLocation.aeroway,               Messages.Tour_Location_Part_Aeroway);
         createUI_Content(container,   _tourLocation.amenity,               Messages.Tour_Location_Part_Amenity);
         createUI_Content(container,   _tourLocation.boundary,              Messages.Tour_Location_Part_Boundary);
         createUI_Content(container,   _tourLocation.bridge,                Messages.Tour_Location_Part_Bridge);
         createUI_Content(container,   _tourLocation.club,                  Messages.Tour_Location_Part_Club);
         createUI_Content(container,   _tourLocation.craft,                 Messages.Tour_Location_Part_Craft);
         createUI_Content(container,   _tourLocation.emergency,             Messages.Tour_Location_Part_Emergency);
         createUI_Content(container,   _tourLocation.historic,              Messages.Tour_Location_Part_Historic);
         createUI_Content(container,   _tourLocation.landuse,               Messages.Tour_Location_Part_Landuse);
         createUI_Content(container,   _tourLocation.leisure,               Messages.Tour_Location_Part_Leisure);
         createUI_Content(container,   _tourLocation.man_made,              Messages.Tour_Location_Part_ManMade);
         createUI_Content(container,   _tourLocation.military,              Messages.Tour_Location_Part_Military);
         createUI_Content(container,   _tourLocation.mountain_pass,         Messages.Tour_Location_Part_MountainPass);
         createUI_Content(container,   _tourLocation.natural2,              Messages.Tour_Location_Part_Natural);
         createUI_Content(container,   _tourLocation.office,                Messages.Tour_Location_Part_Office);
         createUI_Content(container,   _tourLocation.place,                 Messages.Tour_Location_Part_Place);
         createUI_Content(container,   _tourLocation.railway,               Messages.Tour_Location_Part_Railway);
         createUI_Content(container,   _tourLocation.shop,                  Messages.Tour_Location_Part_Shop);
         createUI_Content(container,   _tourLocation.tourism,               Messages.Tour_Location_Part_Tourism);
         createUI_Content(container,   _tourLocation.tunnel,                Messages.Tour_Location_Part_Tunnel);
         createUI_Content(container,   _tourLocation.waterway,              Messages.Tour_Location_Part_Waterway);

// SET_FORMATTING_ON

         if (_isLocationView) {

            UI.createSpacer_Vertical(container, 16, 2);

            {
               /*
                * Usage
                */

               final String usage = USAGE_VALUES.formatted(

                     _colLocationItem.numTourAllLocations,
                     _colLocationItem.numTourStartLocations,
                     _colLocationItem.numTourEndLocations);

               UI.createLabel(container, Messages.Tour_Location_Label_Usage, Messages.Tour_Location_Label_Usage_Tooltip);
               UI.createLabel(container, usage, Messages.Tour_Location_Label_Usage_Tooltip);
            }
            {
               /*
                * Zoomlevel
                */
               UI.createLabel(container, Messages.Tour_Location_Label_Zoomlevel);
               UI.createLabel(container, Integer.toString(_tourLocation.zoomlevel));
            }
         }
      }
   }

   private Text createUI_Content(final Composite parent, final String contentValue, final String contentLabel) {

      if (contentValue == null || contentValue.length() == 0) {
         return null;
      }

      // label
      final Label label = new Label(parent, SWT.NONE);

      // text
      final Text text = new Text(parent, SWT.READ_ONLY | SWT.WRAP);

      label.setText(contentLabel);
      text.setText(contentValue);

      return text;
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      final int mouseX = event.x;

      final Point mousePosition = new Point(mouseX, event.y);

      if (_isNatTableView) {

         /*
          * Nattable viewer
          */

         if (_ttControl.isDisposed()) {
            return super.getLocation(tipSize, event);
         }

         if (_natHoveredBounds != null) {

            return getLocation_10(tipSize, mouseX, _natHoveredBounds);
         }

      } else {

         /*
          * Column viewer
          */

         // try to position the tooltip at the bottom of the cell
         final ViewerCell cell = _columnViewer.getCell(mousePosition);

         if (cell != null) {

            return getLocation_10(tipSize, mouseX, cell.getBounds());
         }
      }

      return super.getLocation(tipSize, event);
   }

   private Point getLocation_10(final Point tipSize, final int mouseX, final Rectangle cellBounds) {

      final int cellWidth2 = (int) (cellBounds.width * 0.5);
      final int cellHeight = cellBounds.height;

      final int devXDefault = cellBounds.x + cellWidth2;
      final int devY = cellBounds.y + cellHeight;

      /*
       * Check if the tooltip is outside of the control, this can happen when the column is very
       * wide and partly hidden
       */
      final Rectangle ttControlBounds = _ttControl.getBounds();
      boolean isDevXAdjusted = false;
      int devX = devXDefault;

      if (devXDefault >= ttControlBounds.width) {
         devX = ttControlBounds.width - 40;
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

            final int devXAdjusted = devXDefault - cellWidth2 + 20 - tipSizeWidth;

            ttDisplayLocation = _ttControl.toDisplay(devXAdjusted, devY);

         } else {

            int devXAdjusted = ttDisplayLocation.x - tipSizeWidth;

            if (devXAdjusted + tipSizeWidth + 10 > mouseX) {

               // prevent that the tooltip of the adjusted x position is below the mouse

               final Point mouseDisplay = _ttControl.toDisplay(mouseX, devY);

               devXAdjusted = mouseDisplay.x - tipSizeWidth - 10;
            }

            ttDisplayLocation.x = devXAdjusted;
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

   @Override
   protected Object getToolTipArea(final Event event) {

      Object ttArea;

      if (_isNatTableView) {

         ttArea = getToolTipArea_NatTable(event);

      } else {

         // column viewer

         ttArea = getToolTipArea_ColumnViewer(event);
      }

      return _tourLocation == null ? null : ttArea;
   }

   private Object getToolTipArea_ColumnViewer(final Event event) {

      _colViewerCell = _columnViewer.getCell(new Point(event.x, event.y));

      if (_colViewerCell != null) {

         final CellLabelProvider labelProvider = _columnViewer.getLabelProvider(_colViewerCell.getColumnIndex());

         if (labelProvider instanceof final SelectionCellLabelProvider_WithLocationTooltip tooltipLabelProvider) {

            // tourbook view

            _isStartLocation = tooltipLabelProvider.isStartLocation;

            _isShowTooltip = tooltipLabelProvider.isShowTooltip();

            final Object cellElement = _colViewerCell.getElement();

            if (cellElement instanceof final TVITourBookTour tourItem) {

               _tourLocation = getTourLocation(tourItem.tourId);
            }

         } else if (labelProvider instanceof TourLocationView.TooltipLabelProvider) {

            // tour location view

            final Object cellElement = _colViewerCell.getElement();

            if (cellElement instanceof final LocationItem locationItem) {

               _colLocationItem = locationItem;

               _tourLocation = locationItem.tourLocation;
            }

         } else {

            // this tooltip is not dispalyed for the hovered cell

            _colViewerCell = null;
         }
      }

      return _colViewerCell;
   }

   private Object getToolTipArea_NatTable(final Event event) {

      _natHoveredTourId = null;
      _natHoveredBounds = null;
      _natTooltipCellPos = null;

      final int colPosByX = _natNatTable.getColumnPositionByX(event.x);
      final int rowPosByY = _natNatTable.getRowPositionByY(event.y);

      if (colPosByX <= 0 || rowPosByY <= 0) {

         // first column or first row (this is the row number or table header) or an empty nattable (rowPosByY == -1)

         return null;
      }

//!!! this do not work for freezed columns !!!
//   _hoveredCellPos = _tourBookView.getNatTableLayer_Hover().getCurrentHoveredCellPosition();

//NatTable advanced: With lot of debugging found solution to get absolute row from relative row
      final int hoveredRowPosition = _tourBookView.getNatTableLayer_Viewport().localToUnderlyingRowPosition(rowPosByY - 1);

      // get hovered label provider from the column, this is needed to show the tour tooltip only for specific columns
      final int hoveredColumnIndex = _natNatTable.getColumnIndexByPosition(colPosByX);
      if (hoveredColumnIndex == -1) {

         // a cell is not hovered

         _natTooltipCellPos = null;

      } else {

         _natTooltipCellPos = new Point(colPosByX, rowPosByY);

         final ArrayList<ColumnDefinition> visibleAndSortedColumns = _tourBookView.getNatTable_ColumnManager().getVisibleAndSortedColumns();
         final ColumnDefinition colDef = visibleAndSortedColumns.get(hoveredColumnIndex);

         // hide current tooltip when a cell without tooltip is hovered
         final NatTable_LabelProvider labelProvider = colDef.getNatTable_LabelProvider();
         if (labelProvider instanceof final NatTable_LabelProvider_WithLocationTooltip tooltipLabelProvider) {

            if (tooltipLabelProvider.isShowTooltip() == false) {
               _natTooltipCellPos = null;
            }

            _isStartLocation = tooltipLabelProvider.isStartLocation;

         } else {

            _natTooltipCellPos = null;
         }
      }

      if (_natTooltipCellPos != null) {

         // get hovered tour id
         final TVITourBookTour hoveredTourItem = _tourBookView.getNatTable_DataProvider().getRowObject(hoveredRowPosition);
         _natHoveredTourId = hoveredTourItem.tourId;

         final int devX = _natNatTable.getStartXOfColumnPosition(colPosByX);
         final int devY = _natNatTable.getStartYOfRowPosition(rowPosByY);
         final int cellWidth = _natNatTable.getColumnWidthByPosition(colPosByX);
         final int cellHeight = _natNatTable.getRowHeightByPosition(rowPosByY);

         _natHoveredBounds = new Rectangle(devX, devY, cellWidth, cellHeight);

         _tourLocation = getTourLocation(_natHoveredTourId);
      }

      return _natTooltipCellPos;
   }

   /**
    * @param tourId
    *
    * @return Returns start or end location
    */
   private TourLocation getTourLocation(final Long tourId) {

      if (tourId != null && tourId != -1) {

         final TourData tourData = TourManager.getInstance().getTourData(tourId);

         if (tourData != null) {

            // tour data is available

            if (_isStartLocation) {

               return tourData.getTourLocationStart();

            } else {

               return tourData.getTourLocationEnd();
            }
         }
      }

      return null;
   }

   private void setMaxContentWidth(final Control control) {

      final int maxContentWidth = 300;

      final GridData gd = (GridData) control.getLayoutData();
      final Point contentSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);

      if (contentSize.x > maxContentWidth) {

         // adjust max width
         gd.widthHint = maxContentWidth;

      } else {

         // reset layout width
         gd.widthHint = SWT.DEFAULT;
      }
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

      if (_isNatTableView) {

         /*
          * NatTable viewer
          */

         if (!super.shouldCreateToolTip(event)) {
            return false;
         }

         if (_natTooltipCellPos == null || _natHoveredTourId == null) {

            // show default tooltip
            _ttControl.setToolTipText(null);

            return false;

         } else {

            // hide default tooltip and display the custom tooltip
            _ttControl.setToolTipText(UI.EMPTY_STRING);

            return true;
         }

      } else {

         /*
          * Column viewer
          */

         if (_isLocationView && _tourLocationView.isShowLocationTooltip() == false) {
            return false;
         }

         if (!super.shouldCreateToolTip(event)) {
            return false;
         }

         if (_isTourBookView && _isShowTooltip == false) {
            return false;
         }

         if (_colViewerCell == null || _tourLocation == null) {

            // show default tooltip
            _ttControl.setToolTipText(null);

            return false;

         } else {

            // hide default tooltip and display the custom tooltip
            _ttControl.setToolTipText(UI.EMPTY_STRING);

            return true;
         }
      }
   }

}
