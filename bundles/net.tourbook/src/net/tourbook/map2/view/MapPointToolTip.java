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
import net.tourbook.common.font.MTFont;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.ToolTip;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourMarker;
import net.tourbook.map.location.LocationType;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.tour.location.TourLocationUI;
import net.tourbook.ui.Messages;
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class MapPointToolTip extends ToolTip {

   private static final int DEFAULT_TEXT_WIDTH  = 50;
   private static final int DEFAULT_TEXT_HEIGHT = 20;

   private static final int _textStyle          = SWT.WRAP   //
         | SWT.MULTI
         | SWT.READ_ONLY
//       | SWT.BORDER
   ;

   private Map2             _map2;

   private PaintedMapPoint  _hoveredMapPoint;
   private boolean          _isPhotoTooltip;

   private PixelConverter   _pc;
   private int              _defaultTextWidth;
   private int              _defaultTextHeight;
   private Font             _boldFont;

   /*
    * UI controls
    */
   private ImageCanvas _photoImageCanvas;

   private class PhotoImageLoaderCallback implements ILoadCallBack {

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         if (isUpdateUI) {

            if (_hoveredMapPoint != null) {

               getShell().getDisplay().asyncExec(() -> {

                  if (_hoveredMapPoint != null) {

                     updateUI_Photo(_hoveredMapPoint.mapPoint);
                  }
               });
            }
         }
      }
   }

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

      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;

// SET_FORMATTING_OFF

      switch (mapPoint.pointType) {

      case COMMON_LOCATION:
      case TOUR_LOCATION:     createUI_TourLocation(parent, mapPoint);  break;

      case TOUR_MARKER:       createUI_TourMarker(parent, mapPoint);    break;

      case TOUR_PAUSE:        createUI_TourPause(parent, mapPoint);     break;

      case TOUR_PHOTO:        createUI_TourPhoto(parent, mapPoint);     break;
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

            createUI_TourMarker_10_Link(container, tourMarker);
         }
      }
   }

   /**
    * Url
    *
    * @param parent
    * @param tourMarker
    */
   private void createUI_TourMarker_10_Link(final Composite parent, final TourMarker tourMarker) {

      final String urlText = tourMarker.getUrlText();
      final String urlAddress = tourMarker.getUrlAddress();

      final boolean isText = urlText.length() > 0;
      final boolean isAddress = urlAddress.length() > 0;

      if (isText || isAddress) {

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

   private void createUI_TourPhoto(final Composite parent, final Map2Point mapPoint) {

      final Label label = new Label(parent, SWT.NONE);
      label.setText("Photo");
      GridDataFactory.fillDefaults().applyTo(label);

      _photoImageCanvas = new ImageCanvas(parent, SWT.NONE);
      _photoImageCanvas.setIsSmoothImages(true);
      _photoImageCanvas.setStyle(SWT.CENTER);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_photoImageCanvas);

      updateUI_Photo(mapPoint);
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      if (_hoveredMapPoint != null) {

         final Display display = _map2.getDisplay();

         final int tooltipWidth = tipSize.x;
         final int tooltipHeight = tipSize.y;

         final Point devMouse = _map2.toControl(display.getCursorLocation());
         final int devMouseX = devMouse.x;

         final Rectangle markerBounds = _hoveredMapPoint.labelRectangle;
         final int markerWidth = markerBounds.width;
         final int markerWidth2 = markerWidth / 2;
         final int markerHeight = markerBounds.height;

         final int markerLeft = markerBounds.x;
         final int markerRight = markerLeft + markerWidth;
         final int markerTop = markerBounds.y;
         final int markerBottom = markerBounds.y + markerHeight;

         final Map2Point mapMarker = _hoveredMapPoint.mapPoint;
         final int geoPointDevX = mapMarker.geoPointDevX;
         final int geoPointDevY = mapMarker.geoPointDevY;

         // center horizontally to the mouse position
         int devX = devMouseX - tooltipWidth / 2;
         int devY = markerTop + markerHeight;
         int noCoverHeight;
         final int photoOffset = _isPhotoTooltip

               // a resized tooltip has a border
               ? 7

               : 0;

         if (geoPointDevX <= markerLeft) {

            // label is on the right site

            devX = markerRight - photoOffset;

         } else {

            // label is on the left site -

            devX = markerLeft - tooltipWidth + photoOffset;

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

   /**
    * @param photo
    * @param map
    * @param tile
    *
    * @return Returns the photo image or <code>null</code> when image is not loaded.
    */
   private Image getPhotoImage(final Photo photo) {

      Image photoImage = null;

      final ImageQuality requestedImageQuality = ImageQuality.HQ;

      // check if image has an loading error
      final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

      if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not yet loaded

         // check if image is in the cache
         photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

         if ((photoImage == null || photoImage.isDisposed())
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            final ILoadCallBack imageLoadCallback = new PhotoImageLoaderCallback();

            PhotoLoadManager.putImageInLoadingQueueThumbMap(photo, requestedImageQuality, imageLoadCallback);
         }
      }

      return photoImage;
   }

   @Override
   protected int getShellStyle() {

      if (_isPhotoTooltip) {

         // allow tooltip resize

         return SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS

               | SWT.RESIZE;

      } else {

         return super.getShellStyle();
      }
   }

   @Override
   protected Object getToolTipArea(final Event event) {

      _hoveredMapPoint = _map2.getHoveredMapPoint();

      _isPhotoTooltip = _hoveredMapPoint != null
            && _hoveredMapPoint.mapPoint.pointType.equals(MapPointType.TOUR_PHOTO) ? true : false;

      return _hoveredMapPoint;
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

   @Override
   protected Point setupShellSize(final Shell ttShell) {

      // TODO Auto-generated method stub

      if (_isPhotoTooltip) {

         final Point shellSize = getShellSize();

         if (shellSize == null) {

            return super.setupShellSize(ttShell);
         }

         final int maxSize = 1500;
         final int shellWidth = Math.min(shellSize.x, maxSize);
         final int shellHeight = Math.min(shellSize.y, maxSize);

         ttShell.setSize(ttShell.computeSize(shellWidth, shellHeight, true));

         return new Point(shellWidth, shellHeight);

      } else {

         return super.setupShellSize(ttShell);
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

//      if (_map2.isShowMapLocationTooltip() == false) {
//         return false;
//      }

      if (super.shouldCreateToolTip(event) == false) {
         return false;
      }

      if (_hoveredMapPoint == null) {
         return false;
      }

      return true;
   }

   private void updateUI_Photo(final Map2Point mapPoint) {

      if (_photoImageCanvas.isDisposed()) {
         return;
      }

      final Image photoImage = getPhotoImage(mapPoint.photo);

      if (photoImage != null) {
         _photoImageCanvas.setImage(photoImage, false);
      }
   }
}
