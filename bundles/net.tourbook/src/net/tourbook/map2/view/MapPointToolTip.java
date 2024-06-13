/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.PaintedMapPoint;
import de.byteholder.geoclipse.map.TourPause;

import java.time.ZonedDateTime;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.ToolTip;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.location.TourLocationUI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class MapPointToolTip extends ToolTip {

   private static final int DEFAULT_TEXT_WIDTH  = 50;
   private static final int DEFAULT_TEXT_HEIGHT = 20;

   private static final int _textStyle          = SWT.WRAP                   //
         | SWT.MULTI
         | SWT.READ_ONLY
//                                                               | SWT.BORDER
   ;

   private Map2             _map2;

   private PaintedMapPoint  _hoveredMarker;

   private PixelConverter   _pc;
   private int              _defaultTextWidth;
   private int              _defaultTextHeight;
   private Font             _boldFont;

   public MapPointToolTip(final Map2 map2) {

      super(map2, NO_RECREATE, true);

      _map2 = map2;

      // allow the actions to be selected
      setHideOnMouseDown(false);

      setPopupDelay(20);
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

      initUI(parent);

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         final Composite _ttContainer = new Composite(shellContainer, SWT.NONE);
         GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(_ttContainer);
         {
            createUI(_ttContainer);
         }
      }

      return shellContainer;
   }

   private void createUI(final Composite parent) {

      final Map2Point mapPoint = _hoveredMarker.mapPoint;

// SET_FORMATTING_OFF

      switch (mapPoint.pointType) {
      case COMMON_LOCATION:
      case TOUR_LOCATION:     createUI_TourLocation(parent, mapPoint);  break;

      case TOUR_MARKER:       createUI_TourMarker(parent, mapPoint);    break;

      case TOUR_PAUSE:        createUI_TourPause(parent, mapPoint);     break;
      }

// SET_FORMATTING_ON
   }

   private void createUI_TourLocation(final Composite parent, final Map2Point mapPoint) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(10, 2)
            .applyTo(container);
      {
         // location fields
         TourLocationUI.createUI(container, mapPoint.tourLocation);
      }
   }

   private void createUI_TourMarker(final Composite parent, final Map2Point mapPoint) {

      final TourMarker tourMarker = mapPoint.tourMarker;
      final TourData tourData = tourMarker.getTourData();

      final ZonedDateTime tourStartTime = tourData.getTourStartTime();
      final ZonedDateTime markerStartTime = TimeTools.getZonedDateTime(tourMarker.getTourTime(), tourStartTime.getZone());

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(5, 3).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Marker label
             */

            String markerLabel = tourMarker.getLabel();
            if (UI.IS_SCRAMBLE_DATA) {
               markerLabel = tourMarker.getScrambledLabel();
            }

            final Label label = new Label(container, SWT.NONE);
            label.setText(markerLabel);
            label.setFont(_boldFont);
         }
         {
            /*
             * Marker description
             */
            String markerDescription = tourMarker.getDescription();
            if (StringUtils.hasContent(markerDescription)) {

               if (UI.IS_SCRAMBLE_DATA) {
                  markerDescription = UI.scrambleText(markerDescription);
               }

               final Text txtDescription = new Text(container, _textStyle);
               txtDescription.setText(markerDescription);
               GridDataFactory.fillDefaults().applyTo(txtDescription);

               setTextControlSize(container, txtDescription, markerDescription);
            }
         }
         {
            /*
             * Marker date/time
             */

            final Label label = new Label(container, SWT.TRAIL);
            label.setText(TimeTools.Formatter_DateTime_FL.format(markerStartTime));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

         }
      }
   }

   private void createUI_TourPause(final Composite parent, final Map2Point mapPoint) {

      final TourPause tourPause = mapPoint.tourPause;
      final TourData tourData = tourPause.tourData;

      final ZonedDateTime tourStartTime = tourData.getTourStartTime();
      final ZonedDateTime pauseStartTime = TimeTools.getZonedDateTime(tourPause.startTime, tourStartTime.getZone());

      {
         /*
          * Pause date/time
          */

         final Label label = new Label(parent, SWT.TRAIL);
         label.setText(TimeTools.Formatter_DateTime_FL.format(pauseStartTime));
      }
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      if (_hoveredMarker != null) {

         final Display display = _map2.getDisplay();

         final int tooltipWidth = tipSize.x;
         final int tooltipHeight = tipSize.y;

         final Point devMouse = _map2.toControl(display.getCursorLocation());
         final int devMouseX = devMouse.x;

         final Rectangle markerBounds = _hoveredMarker.labelRectangle;
         final int markerWidth = markerBounds.width;
         final int markerWidth2 = markerWidth / 2;
         final int markerHeight = markerBounds.height;

         final int markerLeft = markerBounds.x;
         final int markerRight = markerLeft + markerWidth;
         final int markerTop = markerBounds.y;
         final int markerBottom = markerBounds.y + markerHeight;

         final Map2Point mapMarker = _hoveredMarker.mapPoint;
         final int geoPointDevX = mapMarker.geoPointDevX;
         final int geoPointDevY = mapMarker.geoPointDevY;

         // center horizontally to the mouse position
         int devX = devMouseX - tooltipWidth / 2;
         int devY = markerTop + markerHeight;
         int noCoverHeight;

         if (geoPointDevX <= markerLeft) {

            // label is on the right site

            devX = markerRight;

         } else {

            // label is on the left site -

            devX = markerLeft - tooltipWidth;
         }

         if (geoPointDevY <= markerTop) {

            // label is below the geo point

            devY = markerTop;
            noCoverHeight = markerHeight;

         } else {

            // label is above the geo point

            devY = markerBottom - tooltipHeight;
            noCoverHeight = -markerHeight;
         }

         /*
          * Check if the tooltip is outside of the parent
          */
         final Rectangle mapBounds = _map2.getBounds();
         final int mapWidth = mapBounds.width;

         boolean isDevXAdjusted = false;

         if (devX >= mapWidth) {
            devX = mapWidth - 40;
            isDevXAdjusted = true;
         }

         Point ttDisplayLocation = _map2.toDisplay(devX, devY);

         final Rectangle displayBounds = display.getBounds();
         final int displayWidth = displayBounds.width;
         final int displayHeight = displayBounds.height;

         if (ttDisplayLocation.x + tooltipWidth > displayWidth) {

            /*
             * Adjust horizontal position, it is outside of the display, prevent default
             * repositioning
             */

            if (isDevXAdjusted) {

               ttDisplayLocation = _map2.toDisplay(devMouseX - tooltipWidth / 2 - markerWidth2 + 20 - tooltipWidth, devY);

            } else {

               ttDisplayLocation.x = displayWidth - tooltipWidth;
               ttDisplayLocation.y += noCoverHeight;
            }
         }

         if (ttDisplayLocation.y + tooltipHeight > displayHeight) {

            /*
             * Adjust vertical position, it is outside of the display, prevent default
             * repositioning
             */

            ttDisplayLocation.y = ttDisplayLocation.y - tooltipHeight - markerHeight;
         }

         return fixupDisplayBoundsWithMonitor(tipSize, ttDisplayLocation);
      }

      return super.getLocation(tipSize, event);
   }

   @Override
   protected Object getToolTipArea(final Event event) {

      _hoveredMarker = _map2.getHoveredMapPoint();

      return _hoveredMarker;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _defaultTextWidth = _pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH);
      _defaultTextHeight = _pc.convertHeightInCharsToPixels(DEFAULT_TEXT_HEIGHT);

      _boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
   }

   private void setTextControlSize(final Composite parent, final Text txtControl, final String text) {

      Point defaultSize = txtControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);

      // check default width
      if (defaultSize.x > _defaultTextWidth) {

         // check default height
         defaultSize = txtControl.computeSize(_defaultTextWidth, SWT.DEFAULT);
         if (defaultSize.y > _defaultTextHeight) {

            setTextControlSize_RecreateWithVScroll(parent, txtControl, text, _defaultTextWidth);

         } else {

            // limit width
            final GridData gd = (GridData) txtControl.getLayoutData();
            gd.widthHint = _defaultTextWidth;
         }

      } else if (defaultSize.y > _defaultTextHeight) {

         setTextControlSize_RecreateWithVScroll(parent, txtControl, text, SWT.DEFAULT);
      }
   }

   /**
    * Recreate text control with vertical scrollbar and limited height.
    *
    * @param parent
    * @param txtControl
    * @param text
    * @param widthHint
    */
   private void setTextControlSize_RecreateWithVScroll(final Composite parent,
                                                       Text txtControl,
                                                       final String text,
                                                       final int widthHint) {

      txtControl.dispose();

      txtControl = new Text(parent, _textStyle | SWT.V_SCROLL);
      GridDataFactory.fillDefaults()
            .hint(widthHint, _defaultTextHeight)
            .applyTo(txtControl);

      txtControl.setText(text);
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

//      if (_map2.isShowMapLocationTooltip() == false) {
//         return false;
//      }

      if (super.shouldCreateToolTip(event) == false) {
         return false;
      }

      if (_hoveredMarker == null) {
         return false;
      }

      return true;
   }
}
