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

import java.text.NumberFormat;
import java.time.ZonedDateTime;

import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.part.PageBook;

/**
 * Slideout for all 2D map locations and marker
 */
public class MapPointToolTip_Photo extends AdvancedSlideout {

   private static final String          ID     = "net.tourbook.map2.view.MapPointToolTip_Photo"; //$NON-NLS-1$

   private final static IDialogSettings _state = TourbookPlugin.getState(ID);

   private Map2                         _map2;

   private PaintedMapPoint              _hoveredMapPoint;
   private PaintedMapPoint              _previousHoveredMapPoint;
   private Photo                        _photo;

//   private final ColorRegistry          _colorRegistry        = JFaceResources.getColorRegistry();
//   private final Color                  _photoForegroundColor = _colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
//   private final Color                  _photoBackgroundColor = _colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

   private final NumberFormat _nfMByte = NumberFormat.getNumberInstance();
   {
      _nfMByte.setMinimumFractionDigits(3);
      _nfMByte.setMaximumFractionDigits(3);
      _nfMByte.setMinimumIntegerDigits(1);
   }

   private boolean              _isSlideoutExpanded;
   private boolean              _isExpandCollapseModified;

   private ActionExpandSlideout _actionExpandCollapseSlideout;

   private ToolBarManager       _toolbarManagerExpandCollapseSlideout;

   private ImageDescriptor      _imageDescriptor_SlideoutCollapse;
   private ImageDescriptor      _imageDescriptor_SlideoutExpand;

   /*
    * UI controls
    */
   private PageBook  _pageBook;

   private Composite _containerPhotoOptions;
   private Composite _pageNoPhoto;
   private Composite _pagePhoto;

//   private Label            _labelError;
   private Label            _labelMessage;

   private PhotoImageCanvas _photoImageCanvas;

   private class ActionExpandSlideout extends Action {

      public ActionExpandSlideout() {

         setToolTipText(UI.SPACE1);
         setImageDescriptor(_imageDescriptor_SlideoutExpand);
      }

      @Override
      public void run() {

         actionExpandCollapseSlideout();
      }
   }

   private class PhotoImageCanvas extends ImageCanvas {

      public PhotoImageCanvas(final Composite parent, final int style) {
         super(parent, style);
      }

      @Override
      public boolean drawInvalidImage(final GC gc, final Rectangle clientArea) {

         return updateUI_ShowLoadingImage(gc, clientArea);
      }
   }

   private class PhotoImageLoaderCallback implements ILoadCallBack {

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         if (isUpdateUI) {
            onImageIsLoaded();
         }
      }
   }

   public MapPointToolTip_Photo(final Map2 map2) {

      super(map2, _state, null);

      _map2 = map2;

      setTitleText("Photo");

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   private void actionExpandCollapseSlideout() {

      // toggle expand state
      _isSlideoutExpanded = !_isSlideoutExpanded;

      _isExpandCollapseModified = true;

      /*
       * Update actions
       */
      if (_isSlideoutExpanded) {

         _actionExpandCollapseSlideout.setToolTipText(OtherMessages.SLIDEOUT_ACTION_COLLAPSE_SLIDEOUT_TOOLTIP);
         _actionExpandCollapseSlideout.setImageDescriptor(_imageDescriptor_SlideoutCollapse);

      } else {

         _actionExpandCollapseSlideout.setToolTipText(OtherMessages.SLIDEOUT_ACTION_EXPAND_SLIDEOUT_TOOLTIP);
         _actionExpandCollapseSlideout.setImageDescriptor(_imageDescriptor_SlideoutExpand);
      }

      _toolbarManagerExpandCollapseSlideout.update(true);

      onTTShellResize(null);
   }

   @Override
   public void close() {

      Map2PointManager.setMapLocationSlideout(null);

      super.close();
   }

   private void createActions() {

      _actionExpandCollapseSlideout = new ActionExpandSlideout();
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      createUI_00_Tooltip(parent);

//      UI.setChildColors(parent.getShell(), _photoForegroundColor, _photoBackgroundColor);

      updateUI_Photo(_hoveredMapPoint);
   }

   @Override
   protected void createTitleBarControls(final Composite parent) {

      // this method is called 1st !!!

      initUI(parent);
      createActions();

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

      _toolbarManagerExpandCollapseSlideout = new ToolBarManager(toolbar);
      _toolbarManagerExpandCollapseSlideout.add(_actionExpandCollapseSlideout);
      _toolbarManagerExpandCollapseSlideout.update(true);
   }

   private void createUI_00_Tooltip(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

      _pagePhoto = createUI_10_Photo(_pageBook);
      _pageNoPhoto = createUI_90_NoPhoto(_pageBook);

      _pageBook.showPage(_pageNoPhoto);
   }

   private Composite createUI_10_Photo(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .spacing(0, 0)
            .applyTo(container);
      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         createUI_20_PhotoImage(container);
         createUI_30_PhotoOptions(container);
      }

      return container;
   }

   private void createUI_20_PhotoImage(final Composite parent) {

      _photoImageCanvas = new PhotoImageCanvas(parent, SWT.DOUBLE_BUFFERED);
      _photoImageCanvas.setIsSmoothImages(true);
      _photoImageCanvas.setStyle(SWT.CENTER);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_photoImageCanvas);
   }

   private void createUI_30_PhotoOptions(final Composite parent) {

      _containerPhotoOptions = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(_containerPhotoOptions);
      GridLayoutFactory.fillDefaults().numColumns(1)
            .extendedMargins(0, 0, 5, 0)
            .applyTo(_containerPhotoOptions);
      {

         final Label label = new Label(_containerPhotoOptions, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(label);
         label.setText("ajsdklfj akjdsfklaj dsfkaj dfkj ");

         final Label label2 = new Label(_containerPhotoOptions, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(label2);
         label2.setText("lkjj trel retkjlkkj retj ert");

         final Label label3 = new Label(_containerPhotoOptions, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(label3);
         label3.setText("lkjj trel retkjlkkj retj ert");

         final Label label4 = new Label(_containerPhotoOptions, SWT.NONE);
         GridDataFactory.fillDefaults().applyTo(label4);
         label4.setText("lkjj trel retkjlkkj retj ert");
      }
   }

   private Composite createUI_90_NoPhoto(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         _labelMessage = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults()
               .align(SWT.CENTER, SWT.CENTER)
               .grab(true, true)
               .applyTo(_labelMessage);
      }

      return container;
   }

   private Point fixupDisplayBounds(final Point tipSize, final Point location) {

      final int tipWidth = tipSize.x;
      final int tipHeight = tipSize.y;

      final Rectangle displayBounds = UI.getDisplayBounds(_map2, location);
      final Point tipRightBottom = new Point(location.x + tipWidth, location.y + tipHeight);

      final Rectangle photoBounds = _hoveredMapPoint.labelRectangle;

      final int photoWidth = photoBounds.width;
      final int photoHeight = photoBounds.height;

      final int photoLeft = photoBounds.x;
      final int photoRight = photoLeft + photoWidth;
      final int photoTop = photoBounds.y;

      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;
      final int mapPointDevY = mapPoint.geoPointDevY;

      final Rectangle mapBounds = _map2.getBounds();
      final Point mapDisplayPosition = _map2.toDisplay(mapBounds.x, mapBounds.y);

      final boolean isTooltipInDisplay = displayBounds.contains(location);
      final boolean isTTBottomRightInDisplay = displayBounds.contains(tipRightBottom);

      final int displayWidth = displayBounds.width;
      final int displayHeight = displayBounds.height;
      final int displayX = displayBounds.x;
      final int displayY = displayBounds.y;

      if ((isTooltipInDisplay && isTTBottomRightInDisplay) == false) {

         if (tipRightBottom.x > displayX + displayWidth) {

            location.x -= tipRightBottom.x - (displayX + displayWidth);

            // adjust x/y to not overlap the map point position

            if (photoTop > mapPointDevY) {

               location.y += photoHeight;

            } else {

               location.y -= photoHeight;
            }

         }

         if (tipRightBottom.y > displayY + displayHeight - photoHeight) {

            location.y -= tipRightBottom.y - (displayY + displayHeight);

            location.x = displayX + mapDisplayPosition.x + photoLeft - tipWidth;
         }

         if (location.x < displayX) {

            location.x = displayX + mapDisplayPosition.x + photoRight;
         }

         if (location.y < displayY) {

            location.y = displayY;

            location.x = displayX + mapDisplayPosition.x + photoLeft - tipWidth;
         }

         if (location.x < displayX) {

            location.x = displayX + mapDisplayPosition.x + photoRight;
         }
      }

      return location;
   }

   @Override
   protected Rectangle getParentBounds() {

      // ignore, is overwritten with getToolTipLocation()
      return null;
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

            PhotoLoadManager.putImageInLoadingQueueHQ_Map(photo, requestedImageQuality, imageLoadCallback);
         }
      }

      return photoImage;
   }

   @Override
   public Point getToolTipLocation(final Point tooltipSize) {

      if (_hoveredMapPoint == null) {
         return null;
      }

      final int tooltipWidth = tooltipSize.x;
      final int tooltipHeight = tooltipSize.y;

      final Rectangle mapBounds = _map2.getBounds();
      final Point mapDisplayPosition = _map2.toDisplay(mapBounds.x, mapBounds.y);

      final Rectangle photoBounds = _hoveredMapPoint.labelRectangle;
      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;

      final int photoWidth = photoBounds.width;
      final int photoHeight = photoBounds.height;

      final int photoLeft = photoBounds.x;
      final int photoRight = photoLeft + photoWidth;
      final int photoTop = photoBounds.y;
      final int photoBottom = photoTop + photoHeight;

      final int mapPointDevX = mapPoint.geoPointDevX;
      final int mapPointDevY = mapPoint.geoPointDevY;

      // set top/left as default
      int devX = photoLeft - tooltipWidth;
      int devY = photoBottom - tooltipHeight;

      // adjust x/y to not overlap the map point position
      if (photoLeft > mapPointDevX) {
         devX = photoRight;
      }

      if (photoTop > mapPointDevY) {
         devY = photoTop;
      }

      // adjust to display position
      devX += mapDisplayPosition.x;
      devY += mapDisplayPosition.y;

      final Point location = new Point(devX, devY);
      final Point fixedDisplayBounds = fixupDisplayBounds(tooltipSize, location);

      return fixedDisplayBounds;
   }

   private void initUI(final Composite parent) {

// SET_FORMATTING_OFF

      _imageDescriptor_SlideoutCollapse   = CommonActivator.getThemedImageDescriptor(  CommonImages.Slideout_Collapse);
      _imageDescriptor_SlideoutExpand     = CommonActivator.getThemedImageDescriptor(  CommonImages.Slideout_Expand);

// SET_FORMATTING_ON
   }

   @Override
   protected void onFocus() {

   }

   private void onImageIsLoaded() {

      final PaintedMapPoint hoveredMapPoint = _hoveredMapPoint;

      Display.getDefault().asyncExec(() -> {

         if (_hoveredMapPoint != null) {

            updateUI_Photo(hoveredMapPoint);

         } else if (_previousHoveredMapPoint != null) {

            /*
             * This happens when an image is loading and the mouse has exited the tooltip -> paint
             * loaded image
             */

            updateUI_Photo(_previousHoveredMapPoint);
         }
      });
   }

   @Override
   protected Point onResize(final int contentWidth, final int contentHeight) {

      int newContentHeight = contentHeight;

      if (_isExpandCollapseModified) {

         _isExpandCollapseModified = false;

         final GridData gd = (GridData) _containerPhotoOptions.getLayoutData();
         
         // get options container default height, this also makes the options visible/expanded
         gd.heightHint = SWT.DEFAULT;
         _containerPhotoOptions.getParent().layout(true);

         final Point optionsSize = _containerPhotoOptions.getSize();
         final int optionsHeight = optionsSize.y;

         if (_isSlideoutExpanded) {

            // slideout is expanded

            newContentHeight += optionsHeight;

         } else {

            // slideout is collappsed

            // hide options
            gd.heightHint = 0;
            _containerPhotoOptions.getParent().layout(true);

            newContentHeight -= optionsHeight;
         }
      }

      return new Point(contentWidth, newContentHeight);
   }

   public void setupPhoto(final PaintedMapPoint hoveredMapPoint) {

      if (TourPainterConfiguration.isShowPhotoTooltip == false) {

         // photo tooltip is not displayed

         return;
      }

      final boolean isVisible = isVisible();

      if (hoveredMapPoint == null) {

         if (isVisible) {

            if (_hoveredMapPoint != null) {
               _previousHoveredMapPoint = _hoveredMapPoint;
            }

            _hoveredMapPoint = null;

            hide();
         }

         return;
      }

      final boolean isOtherMapPoint = _hoveredMapPoint != hoveredMapPoint;

      if (isOtherMapPoint && isVisible) {

         hide();
      }

      if (_hoveredMapPoint != null) {
         _previousHoveredMapPoint = _hoveredMapPoint;
      }

      _hoveredMapPoint = hoveredMapPoint;

      doNotStopAnimation();
      showShell();

      updateUI_Photo(hoveredMapPoint);
   }

   private void updateUI_LoadingMessage() {

      if (_hoveredMapPoint == null) {

         _labelMessage.setText(UI.EMPTY_STRING);

      } else {

         final Photo photo = _hoveredMapPoint.mapPoint.photo;

         final String photoText = "Loading " + photo.imageFilePathName;

         _labelMessage.setText(photoText);
      }

      _pageBook.showPage(_pageNoPhoto);
   }

   private void updateUI_Photo(final PaintedMapPoint hoveredMapPoint) {

      if (hoveredMapPoint == null) {
         _pageBook.showPage(_pageNoPhoto);
         return;
      }

      if (_photoImageCanvas.isDisposed()) {
         _pageBook.showPage(_pageNoPhoto);
         return;
      }

      _photo = hoveredMapPoint.mapPoint.photo;

      final ZonedDateTime adjustedTime_Tour_WithZone = _photo.adjustedTime_Tour_WithZone;

      final String photoDateTime = "%s  %s".formatted(
            adjustedTime_Tour_WithZone.format(TimeTools.Formatter_Weekday),
            adjustedTime_Tour_WithZone.format(TimeTools.Formatter_DateTime_M));

      updateTitleText(photoDateTime);

      final Image photoImage = getPhotoImage(_photo);

      _photoImageCanvas.setImage(photoImage, false);

      if (photoImage == null || photoImage.isDisposed()) {

         updateUI_LoadingMessage();

      } else {

         _pageBook.showPage(_pagePhoto);
      }
   }

   private boolean updateUI_ShowLoadingImage(final GC gc, final Rectangle rectangle) {

      updateUI_LoadingMessage();

      return true;
   }

}
