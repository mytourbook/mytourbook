/*******************************************************************************
 * Copyright (C) 2024, 2025 Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;
import java.time.ZonedDateTime;

import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.ToolTip;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourMarkerType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.map.location.LocationType;
import net.tourbook.tour.location.TourLocationUI;
import net.tourbook.ui.Messages;
import net.tourbook.web.WEB;

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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class MapPointToolTip extends ToolTip {

   private static final int   DEFAULT_TEXT_WIDTH  = 50;
   private static final int   DEFAULT_TEXT_HEIGHT = 20;

   private static final int   _textStyle          = SWT.WRAP                         //
         | SWT.MULTI
         | SWT.READ_ONLY
//       | SWT.BORDER
   ;

   private Map2               _map2;

   private PaintedMapPoint    _hoveredMapPoint;

   private PixelConverter     _pc;
   private int                _defaultTextWidth;
   private int                _defaultTextHeight;

   private final NumberFormat _nf_1_1             = NumberFormat.getNumberInstance();
   {
      _nf_1_1.setMinimumFractionDigits(1);
      _nf_1_1.setMaximumFractionDigits(1);
   }
   /*
    * UI controls
    */
   private Font _boldFont;

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
//      shellContainer.setBackground(UI.SYS_COLOR_BLUE);
      {
         final Composite ttContainer = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(ttContainer);

         GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(ttContainer);
//         ttContainer.setBackground(UI.SYS_COLOR_GREEN);
         {
            createUI(ttContainer);
         }
      }

      return shellContainer;
   }

   private void createUI(final Composite parent) {

      if (_hoveredMapPoint == null) {
         return;
      }

      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;

// SET_FORMATTING_OFF

      switch (mapPoint.pointType) {

      case COMMON_LOCATION:
      case TOUR_LOCATION:     createUI_TourLocation(parent, mapPoint);  break;

      case TOUR_MARKER:       createUI_TourMarker(parent, mapPoint);    break;

      case TOUR_PAUSE:        createUI_TourPause(parent, mapPoint);     break;

      case TOUR_WAY_POINT:    createUI_WayPoint(parent, mapPoint);     break;

      case TOUR_PHOTO:        break;
      }

// SET_FORMATTING_ON
   }

   private void createUI_TourLocation(final Composite parent, final Map2Point mapPoint) {

      final GridDataFactory gdHeaderIndent = GridDataFactory.fillDefaults()

            .span(2, 1)

            // indent to the left that this text is aligned with the labels
            .indent(-4, 0);

      final TourLocation tourLocation = mapPoint.tourLocation;
      final int numDuplicates_Start = mapPoint.numDuplicates_Start;
      final int numDuplicates_End = mapPoint.numDuplicates_End;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(10, 2)
            .applyTo(container);
      {
         {
            /*
             * Title
             */

            String locationTitle;

            if (numDuplicates_Start > 0 && numDuplicates_End > 0) {

               // start & end location

               locationTitle = Messages.Tour_Tooltip_Label_TourLocation_StartEnd;

            } else if (numDuplicates_Start > 0) {

               // start location

               locationTitle = Messages.Tour_Tooltip_Label_TourLocation_Start;

            } else if (numDuplicates_End > 0) {

               // end location

               locationTitle = Messages.Tour_Tooltip_Label_TourLocation_End;

            } else {

               if (mapPoint.locationType.equals(LocationType.Common)) {

                  // common location

                  locationTitle = Messages.Tour_Location_Label_CommonLocation;

               } else {

                  // tour location

                  locationTitle = Messages.Tour_Location_Tooltip_Title;
               }
            }

            // using text control that & is not displayed as mnemonic
            final Text headerText = new Text(container, SWT.READ_ONLY);
            gdHeaderIndent.applyTo(headerText);
            MTFont.setBannerFont(headerText);

            headerText.setText(locationTitle);
         }

         UI.createSpacer_Vertical(container, 8, 2);

         {
            /*
             * Location fields
             */
            TourLocationUI.createUI(container, tourLocation);
         }
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
             * Marker Type
             */

            createUI_TourMarker_10_Type(container, tourMarker);
         }
         {
            /*
             * Marker date/time
             */

            final Label label = new Label(container, SWT.TRAIL);
            label.setText(TimeTools.Formatter_DateTime_FL.format(markerStartTime));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
         }
         {
            /*
             * Link
             */

            createUI_TourMarker_20_Link(container, tourMarker);
         }
      }
   }

   private void createUI_TourMarker_10_Type(final Composite parent, final TourMarker tourMarker) {

      final TourMarkerType tourMarkerType = tourMarker.getTourMarkerType();

      if (tourMarkerType == null) {
         return;
      }

      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(label);

      label.setForeground(tourMarkerType.getForegroundColorSWT());
      label.setBackground(tourMarkerType.getBackgroundColorSWT());
      label.setText(tourMarkerType.getTypeName());
   }

   /**
    * Url
    *
    * @param parent
    * @param tourMarker
    */
   private void createUI_TourMarker_20_Link(final Composite parent, final TourMarker tourMarker) {

      final String urlText = tourMarker.getUrlText();
      final String urlAddress = tourMarker.getUrlAddress();

      final boolean isText = urlText.length() > 0;
      final boolean isAddress = urlAddress.length() > 0;

      if (isText == false && isAddress == false) {
         return;
      }

      String linkText;

      if (isAddress == false) {

         // only text is in the link -> this is not a internet address but create a link of it

         linkText = UI.createLinkText(urlText, urlText);

      } else if (isText == false) {

         linkText = UI.createLinkText(urlAddress, urlAddress);

      } else {

         linkText = UI.createLinkText(urlAddress, urlText);
      }

      final Link linkUrl = new Link(parent, SWT.NONE);
      linkUrl.addListener(SWT.Selection, event -> onSelectUrl(event.text));
      linkUrl.setText(linkText);

      GridDataFactory.fillDefaults().applyTo(linkUrl);
      setLinkWidth(linkUrl);
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

   private void createUI_WayPoint(final Composite parent, final Map2Point mapPoint) {

      final TourWayPoint wayPoint = mapPoint.tourWayPoint;

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(5, 2) // reduce vertical spacing
            .applyTo(container);
//       container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         {
            /*
             * Name
             */
            final String name = wayPoint.getName();
            if (name != null) {

               final Label label = UI.createLabel(container, name);
               label.setFont(_boldFont);
               GridDataFactory.fillDefaults().span(2, 1).indent(0, -5).applyTo(label);
            }
         }
         {
            /*
             * Comment/description
             */
            final String description = wayPoint.getDescription();
            if (description != null) {
               createUITextarea(container, Messages.Tooltip_WayPoint_Label_Description, description, _pc);
            }

            final String comment = wayPoint.getComment();
            if (comment != null) {

               // ignore comment when it has the same content as the description

               if (description == null || (description != null && description.equals(comment) == false)) {
                  createUITextarea(container, Messages.Tooltip_WayPoint_Label_Comment, comment, _pc);
               }
            }
         }

         createUIItem(container, Messages.Tooltip_WayPoint_Label_Category, wayPoint.getCategory());
         createUIItem(container, Messages.Tooltip_WayPoint_Label_Symbol, wayPoint.getSymbol());

         {
            /*
             * Elevation
             */
            final float elevation = wayPoint.getAltitude();
            if (elevation != Float.MIN_VALUE) {

               final float altitude = elevation / UI.UNIT_VALUE_ELEVATION;

               createUIItem(container,
                     Messages.Tooltip_WayPoint_Label_Altitude,
                     _nf_1_1.format(altitude) + UI.SPACE + UI.UNIT_LABEL_ELEVATION);
            }
         }
         {
            /*
             * Date/time
             */
            final long time = wayPoint.getTime();

            if (time != 0) {

               final Label label = UI.createLabel(container, TimeTools.getZonedDateTime(time).format(TimeTools.Formatter_DateTime_FL));

               GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
            }
         }
      }
   }

   private void createUIItem(final Composite parent, final String label, final String value) {

      if (value != null) {

         UI.createLabel(parent, label);
         UI.createLabel(parent, value);
      }
   }

   private void createUITextarea(final Composite parent,
                                 final String labelText,
                                 final String areaText,
                                 final PixelConverter pc) {
      Label label;
      final int horizontalHint = areaText.length() > 80 ? pc.convertWidthInCharsToPixels(80) : SWT.DEFAULT;

      label = UI.createLabel(parent, labelText);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

      label = UI.createLabel(parent, areaText, SWT.WRAP);
      GridDataFactory.fillDefaults().hint(horizontalHint, SWT.DEFAULT).applyTo(label);
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      if (_hoveredMapPoint == null) {

         return super.getLocation(tipSize, event);
      }

      final Display display = _map2.getDisplay();
      final float deviceScaling = _map2.getDeviceScaling();

      final int tooltipWidth = (int) (tipSize.x * deviceScaling);
      final int tooltipHeight = (int) (tipSize.y * deviceScaling);

      final Point devMouse = _map2.toControl(display.getCursorLocation());
      final int devMouseX = devMouse.x;

      final Rectangle labelBounds = _hoveredMapPoint.labelRectangle;
      final int labelWidth = labelBounds.width;
      final int labelWidth2 = labelWidth / 2;
      final int labelHeight = labelBounds.height;

      final int labelLeft = labelBounds.x;
      final int labelRight = labelLeft + labelWidth;
      final int labelTop = labelBounds.y;
      final int labelBottom = labelBounds.y + labelHeight;

      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;
      final int geoPointDevX = mapPoint.geoPointDevX;
      final int geoPointDevY = mapPoint.geoPointDevY;

      // center horizontally to the mouse position
      int devX = devMouseX - tooltipWidth / 2;
      int devY = labelTop + labelHeight;
      int noCoverHeight;

      if (geoPointDevX <= labelLeft) {

         // label is on the right site

         devX = labelRight;

      } else {

         // label is on the left site -

         devX = labelLeft - tooltipWidth;

      }

      if (geoPointDevY <= labelTop) {

         // label is below the geo point

         devY = labelTop;
         noCoverHeight = labelHeight;

      } else {

         // label is above the geo point

         devY = labelBottom - tooltipHeight;
         noCoverHeight = -labelHeight;
      }

      /*
       * Check if the tooltip is outside of the parent
       */
      final Rectangle mapBounds = _map2.getBounds();
      final int mapWidth = (int) (mapBounds.width * deviceScaling);

      boolean isDevXAdjusted = false;

      if (devX >= mapWidth) {

         devX = mapWidth - 40;
         isDevXAdjusted = true;
      }

      devX /= deviceScaling;
      devY /= deviceScaling;

      Point ttDisplayLocation = _map2.toDisplay(devX, devY);

      final Rectangle displayBounds = display.getBounds();

      final int displayWidth = (int) (displayBounds.width * deviceScaling);
      final int displayHeight = (int) (displayBounds.height * deviceScaling);

      float ttLocationX = ttDisplayLocation.x * deviceScaling;
      float ttLocationY = ttDisplayLocation.y * deviceScaling;

      if (ttLocationX + tooltipWidth > displayWidth) {

         /*
          * Adjust horizontal position, it is outside of the display, prevent default
          * repositioning
          */

         if (isDevXAdjusted) {

            ttDisplayLocation = _map2.toDisplay(
                  devMouseX - tooltipWidth / 2 - labelWidth2 + 20 - tooltipWidth,
                  devY);

            ttLocationX = ttDisplayLocation.x * deviceScaling;
            ttLocationY = ttDisplayLocation.y * deviceScaling;

         } else {

            ttLocationX = displayWidth - tooltipWidth;
            ttLocationY += noCoverHeight;
         }
      }

      if (ttLocationY + tooltipHeight > displayHeight) {

         /*
          * Adjust vertical position, it is outside of the display, prevent default
          * repositioning
          */

         ttLocationY = ttLocationY - tooltipHeight - labelHeight;
      }

      ttDisplayLocation.x = (int) (ttLocationX / deviceScaling);
      ttDisplayLocation.y = (int) (ttLocationY / deviceScaling);

      return fixupDisplayBoundsWithMonitor(tipSize, ttDisplayLocation);
   }

   @Override
   protected Object getToolTipArea(final Event event) {

      _hoveredMapPoint = _map2.getHoveredMapPoint();

      final boolean isPhotoTooltip = _hoveredMapPoint != null
            && _hoveredMapPoint.mapPoint.pointType.equals(MapPointType.TOUR_PHOTO) ? true : false;

      // this tooltip is not used for photos

      return isPhotoTooltip ? null : _hoveredMapPoint;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _defaultTextWidth = _pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH);
      _defaultTextHeight = _pc.convertHeightInCharsToPixels(DEFAULT_TEXT_HEIGHT);

      _boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
   }

   private void onSelectUrl(final String address) {

      WEB.openUrl(address);

      // close tooltip when a link is selected
      hide();
   }

   private void setLinkWidth(final Control control) {

      final Point defaultSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);

      // check default width
      if (defaultSize.x > _defaultTextWidth) {

         // limit width
         final GridData gd = (GridData) control.getLayoutData();
         gd.widthHint = _defaultTextWidth;
      }
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

      if (super.shouldCreateToolTip(event) == false) {
         return false;
      }

      if (_hoveredMapPoint == null) {
         return false;
      }

      return true;
   }

}
