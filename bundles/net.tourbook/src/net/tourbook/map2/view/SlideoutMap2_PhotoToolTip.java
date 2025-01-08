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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.Histogram;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.TourPhotoReference;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.part.PageBook;

/**
 * Slideout for the 2D map photo tooltip
 */
public class SlideoutMap2_PhotoToolTip extends AdvancedSlideout implements IActionResetToDefault {

   private static final String          ID                                = "net.tourbook.map2.view.MapPointToolTip_Photo"; //$NON-NLS-1$

   private static final char            NL                                = UI.NEW_LINE;

   private static final String          STATE_TOOLTIP_SIZE_INDEX          = "STATE_TOOLTIP_SIZE_INDEX";                     //$NON-NLS-1$

   private static final String          STATE_TOOLTIP_SIZE_LARGE          = "STATE_TOOLTIP_SIZE_LARGE";                     //$NON-NLS-1$
   private static final String          STATE_TOOLTIP_SIZE_MEDIUM         = "STATE_TOOLTIP_SIZE_MEDIUM";                    //$NON-NLS-1$
   private static final String          STATE_TOOLTIP_SIZE_SMALL          = "STATE_TOOLTIP_SIZE_SMALL";                     //$NON-NLS-1$
   private static final String          STATE_TOOLTIP_SIZE_TINY           = "STATE_TOOLTIP_SIZE_TINY";                      //$NON-NLS-1$

   private static final int[]           STATE_TOOLTIP_SIZE_TINY_DEFAULT   = new int[] { 250, 250 };
   private static final int[]           STATE_TOOLTIP_SIZE_SMALL_DEFAULT  = new int[] { 500, 500 };
   private static final int[]           STATE_TOOLTIP_SIZE_MEDIUM_DEFAULT = new int[] { 800, 800 };
   private static final int[]           STATE_TOOLTIP_SIZE_LARGE_DEFAULT  = new int[] { 1000, 1000 };

   private final static IDialogSettings _state                            = TourbookPlugin.getState(ID);

   private static final String          STATE_IS_TOOLTIP_EXPANDED         = "STATE_IS_TOOLTIP_EXPANDED";                    //$NON-NLS-1$

   private Map2                         _map2;

   private PaintedMapPoint              _hoveredMapPoint;
   private PaintedMapPoint              _previousHoveredMapPoint;
   private Photo                        _photo;

   private final NumberFormat           _nfMByte                          = NumberFormat.getNumberInstance();
   {
      _nfMByte.setMinimumFractionDigits(3);
      _nfMByte.setMaximumFractionDigits(3);
      _nfMByte.setMinimumIntegerDigits(1);
   }

   private FocusListener         _keepOpenListener;

   private boolean               _isAutoResizeTooltip;
   private boolean               _isExpandCollapseModified;
   private boolean               _isTooltipExpanded;

   private ToolBarManager        _toolbarManagerExpandCollapseSlideout;

   private ActionExpandSlideout  _actionExpandCollapseSlideout;
   private ActionResetToDefaults _actionRestoreDefaults;

   private ImageDescriptor       _imageDescriptor_SlideoutCollapse;
   private ImageDescriptor       _imageDescriptor_SlideoutExpand;

   private int[]                 _selectedTooltipSize;

   /** Photo position and size of the photo image within the photo canvas */
   private Rectangle             _photoImageBounds;
   private Rectangle             _photoImageBounds_OnResize;

   /**
    * x = X1<br>
    * y = Y1<br>
    * width = X2<br>
    * height = Y2
    */
   private Rectangle             _devCanvas_CropArea;
   private Rectangle             _devCanvas_CropArea_Top;
   private Rectangle             _devCanvas_CropArea_TopLeft;
   private Rectangle             _devCanvas_CropArea_TopRight;
   private Rectangle             _devCanvas_CropArea_Bottom;
   private Rectangle             _devCanvas_CropArea_BottomLeft;
   private Rectangle             _devCanvas_CropArea_BottomRight;
   private Rectangle             _devCanvas_CropArea_Left;
   private Rectangle             _devCanvas_CropArea_Right;

   /**
    * x = X1<br>
    * y = Y1<br>
    * width = X2<br>
    * height = Y2
    */
   private Rectangle2D.Float     _relPhoto_CropArea;

   private Point                 _devCanvas_Offset;
   private Point                 _devCanvas_SetCropArea_Start;
   private Point                 _devCanvas_SetCropArea_End;
   private Point2D.Float         _relPhoto_Offset;
   private Point2D.Float         _relPhoto_SetCropArea_Start;
   private Point2D.Float         _relPhoto_SetCropArea_End;

   private boolean               _isCanvasListenerSet;
   private boolean               _isSettingCropArea;

   private boolean               _isMouse_InCropArea_All;
   private boolean               _isMouse_InCropArea_Top;
   private boolean               _isMouse_InCropArea_TopLeft;
   private boolean               _isMouse_InCropArea_TopRight;
   private boolean               _isMouse_InCropArea_Bottom;
   private boolean               _isMouse_InCropArea_BottomLeft;
   private boolean               _isMouse_InCropArea_BottomRight;
   private boolean               _isMouse_InCropArea_Left;
   private boolean               _isMouse_InCropArea_Right;

   private boolean               _isMouseDown_InCropArea_All;
   private boolean               _isMouseDown_InCropArea_Top;
   private boolean               _isMouseDown_InCropArea_TopLeft;
   private boolean               _isMouseDown_InCropArea_TopRight;
   private boolean               _isMouseDown_InCropArea_Bottom;
   private boolean               _isMouseDown_InCropArea_BottomLeft;
   private boolean               _isMouseDown_InCropArea_BottomRight;
   private boolean               _isMouseDown_InCropArea_Left;
   private boolean               _isMouseDown_InCropArea_Right;

   private MouseMoveListener     _photoMouseMoveListener;
   private MouseListener         _photoMouseDownListener;
   private MouseListener         _photoMouseUpListener;
   private MouseTrackListener    _photoMouseExitListener;
   private ControlListener       _photoResizeListener;

   /*
    * UI controls
    */
   private PageBook         _pageBook;

   private Button           _chkCropPhoto;

   private Composite        _containerPhotoOptions;
   private Composite        _containerHeader_1;
   private Composite        _containerHeader_2;
   private Composite        _pageNoPhoto;
   private Composite        _pagePhoto;

   private Combo            _comboTooltipSize;

   private Label            _labelMessage;

   private Histogram        _histogram;
   private PhotoImageCanvas _photoImageCanvas;

   private Cursor           _currentCursor;
   private Cursor           _photoCursor_Arrow;
   private Cursor           _photoCursor_Cross;
   private Cursor           _photoCursor_Size_All;
   private Cursor           _photoCursor_Size_ESE;
   private Cursor           _photoCursor_Size_NESW;
   private Cursor           _photoCursor_Size_NS;
   private Cursor           _photoCursor_Size_WE;

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

      @Override
      public void paintControl(final PaintEvent event) {

         super.paintControl(event);

         onPhoto_PaintCropping(event);
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

   public SlideoutMap2_PhotoToolTip(final Map2 map2) {

      super(map2, _state, null);

      _map2 = map2;

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);

      // ensure the tooltip header actions are displayed with the dark theme icons
      setDarkThemeForToolbarActions();
   }

   private void actionExpandCollapseSlideout() {

      // toggle expand state
      _isTooltipExpanded = !_isTooltipExpanded;

      updateUI_ExpandedCollapsed_Action();

      _isExpandCollapseModified = true;
      onTTShellResize(null);
   }

   @Override
   public void close() {

      Map2PointManager.setMapLocationSlideout(null);

      super.close();
   }

   private void createActions() {

      _actionExpandCollapseSlideout = new ActionExpandSlideout();
      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      createUI_00_Tooltip(parent);

      fillUI();

      setupPhoto_UI(_hoveredMapPoint);

      restoreState();

      updateUI_ExpandedCollapsed_Action();
      updateUI_ExpandedCollapsed_Layout();

      updateUI_Toolbar();

      // show dialog with dark colors, this looks better for photos with the bright theme
      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
//      UI.setChildColors(parent.getShell(),
//            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND),
//            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));
   }

   @Override
   protected void createTitleBar_FirstControls(final Composite parent) {

      // this method is called 1st !!!

      initUI(parent);
      createActions();

      _containerHeader_1 = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(_containerHeader_1);
      GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).applyTo(_containerHeader_1);
//      _containerHeader_1.setBackground(UI.SYS_COLOR_GREEN);

      {
         {
            /*
             * Tooltip size
             */
            _comboTooltipSize = new Combo(_containerHeader_1, SWT.READ_ONLY | SWT.BORDER);
            _comboTooltipSize.setVisibleItemCount(10);
            _comboTooltipSize.setToolTipText(Messages.Slideout_MapPoint_PhotoToolTip_Combo_TooltipSize_Tooltip);
            _comboTooltipSize.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_TooltipSize(selectionEvent)));
            _comboTooltipSize.addFocusListener(_keepOpenListener);

            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_comboTooltipSize);
         }

         UI.createLabel(_containerHeader_1, UI.SPACE3);
      }
   }

   @Override
   protected void createTitleBarControls(final Composite parent) {

      _containerHeader_2 = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(_containerHeader_2);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(_containerHeader_2);
//      _containerHeader_2.setBackground(UI.SYS_COLOR_BLUE);
      {
         {
            /*
             * Expand/collapse slideout
             */
            final ToolBar toolbar = new ToolBar(_containerHeader_2, SWT.FLAT);

            _toolbarManagerExpandCollapseSlideout = new ToolBarManager(toolbar);
            _toolbarManagerExpandCollapseSlideout.add(_actionExpandCollapseSlideout);
            _toolbarManagerExpandCollapseSlideout.update(true);

            GridDataFactory.fillDefaults().grab(true, false)
                  .indent(5, 0)
                  .align(SWT.END, SWT.CENTER).applyTo(toolbar);
         }
      }
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
      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         createUI_20_PhotoOptions(container);
         createUI_50_PhotoImage(container);
      }

      return container;
   }

   private void createUI_20_PhotoOptions(final Composite parent) {

      _containerPhotoOptions = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(_containerPhotoOptions);
      GridLayoutFactory.fillDefaults().numColumns(1)
            .extendedMargins(0, 0, 0, 5)
            .numColumns(3)
            .applyTo(_containerPhotoOptions);
//      _containerPhotoOptions.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            /*
             * Crop photo
             */
            _chkCropPhoto = new Button(_containerPhotoOptions, SWT.CHECK);
            _chkCropPhoto.setText("&Crop photo image");
            _chkCropPhoto.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onPhoto_Crop()));
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(_chkCropPhoto);

         }
         {
            final Link link = new Link(_containerPhotoOptions, SWT.NONE);
            link.setText(UI.createLinkText(Messages.Slideout_MapPoint_PhotoToolTip_Link_ResizeTooltip));
            link.setToolTipText(Messages.Slideout_MapPoint_PhotoToolTip_Link_ResizeTooltip_Tooltip);
            link.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_TooltipResize()));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(link);
         }
         {
            /*
             * Expand/collapse slideout
             */
            UI.createToolbarAction(_containerPhotoOptions, _actionRestoreDefaults);
         }
         {
            _histogram = new Histogram(_containerPhotoOptions);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .span(3, 1)
                  .hint(SWT.DEFAULT, 100)
                  .applyTo(_histogram);

         }
      }
   }

   private void createUI_50_PhotoImage(final Composite parent) {

      _photoImageCanvas = new PhotoImageCanvas(parent, SWT.DOUBLE_BUFFERED);
      _photoImageCanvas.setIsSmoothImages(true);
      _photoImageCanvas.setStyle(SWT.CENTER);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_photoImageCanvas);
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

   private void fillUI() {

      _comboTooltipSize.add(OtherMessages.APP_SIZE_TINY_TEXT);
      _comboTooltipSize.add(OtherMessages.APP_SIZE_SMALL_TEXT);
      _comboTooltipSize.add(OtherMessages.APP_SIZE_MEDIUM_TEXT);
      _comboTooltipSize.add(OtherMessages.APP_SIZE_LARGE_TEXT);
   }

   private Point fixupDisplayBounds(final Point ttSize_Unscaled, final Point ttPos_Scaled) {

      final float deviceScaling = _map2.getDeviceScaling();

      final Rectangle mapBounds = _map2.getBounds();
      final Point mapDisplayPosition = _map2.toDisplay(mapBounds.x, mapBounds.y);

      final int mapDisplayPosX_Scaled = (int) (mapDisplayPosition.x * deviceScaling);

      final Point ttPosn_Unscaled = new Point(
            (int) (ttPos_Scaled.x / deviceScaling),
            (int) (ttPos_Scaled.y / deviceScaling));

      final Rectangle displayBounds = UI.getDisplayBounds(_map2, ttPosn_Unscaled);

      final Rectangle displayBounds_Scaled = new Rectangle(
            (int) (displayBounds.x * deviceScaling),
            (int) (displayBounds.y * deviceScaling),
            (int) (displayBounds.width * deviceScaling),
            (int) (displayBounds.height * deviceScaling));

      final int ttWidth = (int) (ttSize_Unscaled.x * deviceScaling);
      final int ttHeight = (int) (ttSize_Unscaled.y * deviceScaling);
      final Point ttBottomRight = new Point(ttPos_Scaled.x + ttWidth, ttPos_Scaled.y + ttHeight);

      final boolean isTooltipInDisplay = displayBounds_Scaled.contains(ttPos_Scaled);
      final boolean isTTBottomRightInDisplay = displayBounds_Scaled.contains(ttBottomRight);

      final int displayWidth_Scaled = displayBounds_Scaled.width;
      final int displayHeight_Scaled = displayBounds_Scaled.height;
      final int displayX_Scaled = displayBounds_Scaled.x;
      final int displayY_Scaled = displayBounds_Scaled.y;

      final Rectangle photoBounds_Scaled = _hoveredMapPoint.labelRectangle;

      final int photoWidth = photoBounds_Scaled.width;
      final int photoHeight = photoBounds_Scaled.height;

      final int photoLeft = photoBounds_Scaled.x;
      final int photoRight = photoLeft + photoWidth;
      final int photoTop = photoBounds_Scaled.y;

      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;
      final int mapPointDevY = mapPoint.geoPointDevY;

      int ttDevX = ttPos_Scaled.x;
      int ttDevY = ttPos_Scaled.y;

      if ((isTooltipInDisplay && isTTBottomRightInDisplay) == false) {

         if (ttBottomRight.x > displayX_Scaled + displayWidth_Scaled) {

            ttDevX -= ttBottomRight.x - (displayX_Scaled + displayWidth_Scaled);

            // do not overlap the photo with the tooltip
            if (photoTop > mapPointDevY) {

               ttDevY += photoHeight;

            } else {

               ttDevY -= photoHeight;
            }
         }

         if (ttBottomRight.y > displayY_Scaled + displayHeight_Scaled - photoHeight) {

            ttDevY -= ttBottomRight.y - (displayY_Scaled + displayHeight_Scaled);

            ttDevX = displayX_Scaled + mapDisplayPosX_Scaled + photoLeft - ttWidth;
         }

         if (ttDevX < displayX_Scaled) {

            ttDevX = displayX_Scaled + mapDisplayPosX_Scaled + photoRight;
         }

         if (ttDevY < displayY_Scaled) {

            ttDevY = displayY_Scaled;

            ttDevX = displayX_Scaled + mapDisplayPosX_Scaled + photoLeft - ttWidth;
         }

         if (ttDevX < displayX_Scaled) {

            ttDevX = displayX_Scaled + mapDisplayPosX_Scaled + photoRight;
         }
      }

      // return unscaled position
      return new Point(
            (int) (ttDevX / deviceScaling),
            (int) (ttDevY / deviceScaling));
   }

   private Rectangle getHistogramCropArea() {

      if (_photo.isCropped == false || _photoImageBounds == null) {

         return null;
      }

      final int photoX1 = _photoImageBounds.x;
      final int photoX2 = _photoImageBounds.width;
      final int photoY1 = _photoImageBounds.y;
      final int photoY2 = _photoImageBounds.height;

      final int cropX1 = _devCanvas_CropArea.x;
      final int cropX2 = _devCanvas_CropArea.width;
      final int cropY1 = _devCanvas_CropArea.y;
      final int cropY2 = _devCanvas_CropArea.height;

      final int histX1 = cropX1 - photoX1;
      final int histY1 = cropY1 - photoY1;

      final int histX2 = cropX2 - photoX1;
      final int histY2 = cropY2 - photoY1;

      final Rectangle histCropRect = new Rectangle(

            histX1,
            histY1,
            histX2,
            histY2

      );

      return histCropRect;
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
         photoImage = PhotoImageCache.getImage_SWT(photo, requestedImageQuality);

         if ((photoImage == null || photoImage.isDisposed())
               && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            final ILoadCallBack imageLoadCallback = new PhotoImageLoaderCallback();

            PhotoLoadManager.putImageInLoadingQueueHQ_Map(photo,
                  requestedImageQuality,
                  imageLoadCallback);
         }
      }

      return photoImage;
   }

   /**
    * @param devMouseX
    * @param devMouseY
    *
    * @return Returns mouse position relative to the photo and not to the canvas
    */
   private Point2D.Float getRelativeMousePhotoPosition(final int devMouseX, final int devMouseY) {

      final int devCanvas_PhotoX = _photoImageBounds.x;
      final int devCanvas_PhotoY = _photoImageBounds.y;

      final float devPhotoWidth = _photoImageBounds.width;
      final float devPhotoHeight = _photoImageBounds.height;

      float relCropX = (devMouseX - devCanvas_PhotoX) / devPhotoWidth;
      float relCropY = (devMouseY - devCanvas_PhotoY) / devPhotoHeight;

      /*
       * Fix bounds, mouse could be outside of the photo bounds
       */

      if (relCropX < 0) {
         relCropX = 0;
      }
      if (relCropX > 1) {
         relCropX = 1;
      }
      if (relCropY < 0) {
         relCropY = 0;
      }
      if (relCropY > 1) {
         relCropY = 1;
      }

      return new Point2D.Float(relCropX, relCropY);
   }

   private int getSelectedTooltipSizeIndex() {

      final int selectionIndex = _comboTooltipSize.getSelectionIndex();

      return selectionIndex < 0
            ? 0
            : selectionIndex;
   }

   @Override
   public Point getToolTipLocation(final Point ttSize_Unscaled) {

      if (_hoveredMapPoint == null) {
         return null;
      }

      final float deviceScaling = _map2.getDeviceScaling();

      final Rectangle photoBounds = _hoveredMapPoint.labelRectangle;
      final Map2Point mapPoint = _hoveredMapPoint.mapPoint;

      final int tooltipWidth = (int) (ttSize_Unscaled.x * deviceScaling);
      final int tooltipHeight = (int) (ttSize_Unscaled.y * deviceScaling);

      final int photoWidth = photoBounds.width;
      final int photoHeight = photoBounds.height;

      final int photoLeft = photoBounds.x;
      final int photoRight = photoLeft + photoWidth;
      final int photoTop = photoBounds.y;
      final int photoBottom = photoTop + photoHeight;

      final int mapPointDevX = mapPoint.geoPointDevX;
      final int mapPointDevY = mapPoint.geoPointDevY;

      // set photo bottom/left as default position
      int ttPosX = photoLeft - tooltipWidth;
      int ttPosY = photoBottom - tooltipHeight;

      // adjust x/y to not overlap the map point position
      if (photoLeft > mapPointDevX) {
         ttPosX = photoRight;
      }

      if (photoTop > mapPointDevY) {
         ttPosY = photoTop;
      }

      // adjust to display position
      final Rectangle mapBounds = _map2.getBounds();
      final Point mapDisplayPosition = _map2.toDisplay(mapBounds.x, mapBounds.y);

      ttPosX += mapDisplayPosition.x * deviceScaling;
      ttPosY += mapDisplayPosition.y * deviceScaling;

      final Point ttPos_Scaled = new Point(ttPosX, ttPosY);

      final Point fixedDisplayBounds = fixupDisplayBounds(ttSize_Unscaled, ttPos_Scaled);

      return fixedDisplayBounds;
   }

   private void initUI(final Composite parent) {

      final Display display = parent.getDisplay();

// SET_FORMATTING_OFF

      _imageDescriptor_SlideoutCollapse   = CommonActivator.getThemedImageDescriptor_Dark(CommonImages.Slideout_Collapse);
      _imageDescriptor_SlideoutExpand     = CommonActivator.getThemedImageDescriptor_Dark(CommonImages.Slideout_Expand);

      _photoMouseMoveListener    = event -> onPhoto_Mouse_30_Move(          event);
      _photoMouseUpListener      = MouseListener.mouseUpAdapter(           event -> onPhoto_Mouse_20_Up(event));
      _photoMouseDownListener    = MouseListener.mouseDownAdapter(         event -> onPhoto_Mouse_10_Down(event));
      _photoMouseExitListener    = MouseTrackListener.mouseExitAdapter(    event -> onPhoto_Mouse_Exit());
      _photoResizeListener       = ControlListener.controlResizedAdapter(  event -> onPhoto_Resize(event));

      _photoCursor_Arrow         = display.getSystemCursor(SWT.CURSOR_ARROW);
      _photoCursor_Cross         = display.getSystemCursor(SWT.CURSOR_CROSS);
      _photoCursor_Size_All      = display.getSystemCursor(SWT.CURSOR_SIZEALL);
      _photoCursor_Size_ESE      = display.getSystemCursor(SWT.CURSOR_SIZESE);
      _photoCursor_Size_NESW     = display.getSystemCursor(SWT.CURSOR_SIZENESW);
      _photoCursor_Size_NS       = display.getSystemCursor(SWT.CURSOR_SIZENS);
      _photoCursor_Size_WE       = display.getSystemCursor(SWT.CURSOR_SIZEWE);

// SET_FORMATTING_ON

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };

   }

   @Override
   protected void onDispose() {

      super.onDispose();
   }

   @Override
   protected void onFocus() {

   }

   private void onImageIsLoaded() {

      final PaintedMapPoint hoveredMapPoint = _hoveredMapPoint;

      Display.getDefault().asyncExec(() -> {

         if (_hoveredMapPoint != null) {

            setupPhoto_UI(hoveredMapPoint);

         } else if (_previousHoveredMapPoint != null) {

            /*
             * This happens when an image is loading and the mouse has exited the tooltip -> paint
             * loaded image
             */

            setupPhoto_UI(_previousHoveredMapPoint);
         }
      });
   }

   private void onPhoto_Crop() {

      final boolean isCropped = _chkCropPhoto.getSelection();

      _photo.isCropped = isCropped;

      if (isCropped) {
         _photo.isAdjustmentModified = true;
      }

      _photo.updateMapImageRenderSize();

      updateTourPhotoInDB(_photo);

      setupPhotoCanvasListener();

      _photoImageCanvas.redraw();

      _histogram.updateCropArea(getHistogramCropArea());
   }

   private void onPhoto_Mouse_10_Down(final MouseEvent mouseEvent) {

      final int devMouseX = mouseEvent.x;
      final int devMouseY = mouseEvent.y;

      final Point devMousePosition = new Point(devMouseX, devMouseY);

// SET_FORMATTING_OFF

      _isMouseDown_InCropArea_All         = false;

      _isMouseDown_InCropArea_Top         = false;
      _isMouseDown_InCropArea_Bottom      = false;
      _isMouseDown_InCropArea_Left        = false;
      _isMouseDown_InCropArea_Right       = false;

      _isMouseDown_InCropArea_TopLeft     = false;
      _isMouseDown_InCropArea_TopRight    = false;
      _isMouseDown_InCropArea_BottomLeft  = false;
      _isMouseDown_InCropArea_BottomRight = false;


      if        (_isMouse_InCropArea_Top) {           _isMouseDown_InCropArea_Top         = true;
      } else if (_isMouse_InCropArea_Bottom) {        _isMouseDown_InCropArea_Bottom      = true;
      } else if (_isMouse_InCropArea_Left) {          _isMouseDown_InCropArea_Left        = true;
      } else if (_isMouse_InCropArea_Right) {         _isMouseDown_InCropArea_Right       = true;

      } else if (_isMouse_InCropArea_TopLeft) {       _isMouseDown_InCropArea_TopLeft     = true;
      } else if (_isMouse_InCropArea_TopRight) {      _isMouseDown_InCropArea_TopRight    = true;
      } else if (_isMouse_InCropArea_BottomLeft) {    _isMouseDown_InCropArea_BottomLeft  = true;
      } else if (_isMouse_InCropArea_BottomRight) {   _isMouseDown_InCropArea_BottomRight = true;

// SET_FORMATTING_ON

      } else if (_isMouse_InCropArea_All) {

         _isMouseDown_InCropArea_All = true;

         final Point2D.Float relMouse = getRelativeMousePhotoPosition(devMouseX, devMouseY);

         final float relMouseX = relMouse.x;
         final float relMouseY = relMouse.y;

         _devCanvas_Offset = new Point(
               _devCanvas_CropArea.x - devMouseX,
               _devCanvas_CropArea.y - devMouseY);

         _relPhoto_Offset = new Point2D.Float(
               _relPhoto_CropArea.x - relMouseX,
               _relPhoto_CropArea.y - relMouseY);

      } else {

         // start new croping area -> reset all states

         _isSettingCropArea = false;

         _devCanvas_SetCropArea_Start = null;
         _devCanvas_SetCropArea_End = null;

         _relPhoto_SetCropArea_Start = null;
         _relPhoto_SetCropArea_End = null;

         if (_photoImageBounds.contains(devMousePosition)) {

            // mouse is within the photo

            _isSettingCropArea = true;

            _devCanvas_SetCropArea_Start = devMousePosition;
            _relPhoto_SetCropArea_Start = getRelativeMousePhotoPosition(devMouseX, devMouseY);
         }
      }

      _photoImageCanvas.redraw();
   }

   private void onPhoto_Mouse_20_Up(final MouseEvent mouseEvent) {

      final int devMouseX = mouseEvent.x;
      final int devMouseY = mouseEvent.y;

      final Point devMousePosition = new Point(devMouseX, devMouseY);

      if (_isSettingCropArea) {

         _isSettingCropArea = false;

         _devCanvas_SetCropArea_End = devMousePosition;
         _relPhoto_SetCropArea_End = getRelativeMousePhotoPosition(devMouseX, devMouseY);

         updateCropArea_FromStartEnd();

         updateCropArea_InPhoto();
      }

      _isMouseDown_InCropArea_All = false;

      _isMouseDown_InCropArea_Top = false;
      _isMouseDown_InCropArea_Bottom = false;
      _isMouseDown_InCropArea_Left = false;
      _isMouseDown_InCropArea_Right = false;

      _isMouseDown_InCropArea_TopLeft = false;
      _isMouseDown_InCropArea_TopRight = false;
      _isMouseDown_InCropArea_BottomLeft = false;
      _isMouseDown_InCropArea_BottomRight = false;

      _photoImageCanvas.redraw();
   }

   private void onPhoto_Mouse_30_Move(final MouseEvent mouseEvent) {

      if (_photoImageBounds == null) {
         return;
      }

      // keep states
      final boolean isMouse_InCropArea_All = _isMouse_InCropArea_All;
      final boolean isMouse_InCropArea_Top = _isMouse_InCropArea_Top;
      final boolean isMouse_InCropArea_TopLeft = _isMouse_InCropArea_TopLeft;
      final boolean isMouse_InCropArea_TopRight = _isMouse_InCropArea_TopRight;
      final boolean isMouse_InCropArea_Bottom = _isMouse_InCropArea_Bottom;
      final boolean isMouse_InCropArea_BottomLeft = _isMouse_InCropArea_BottomLeft;
      final boolean isMouse_InCropArea_BottomRight = _isMouse_InCropArea_BottomRight;
      final boolean isMouse_InCropArea_Left = _isMouse_InCropArea_Left;
      final boolean isMouse_InCropArea_Right = _isMouse_InCropArea_Right;

      // reset states
      _isMouse_InCropArea_All = false;
      _isMouse_InCropArea_Top = false;
      _isMouse_InCropArea_TopLeft = false;
      _isMouse_InCropArea_TopRight = false;
      _isMouse_InCropArea_Bottom = false;
      _isMouse_InCropArea_BottomLeft = false;
      _isMouse_InCropArea_BottomRight = false;
      _isMouse_InCropArea_Left = false;
      _isMouse_InCropArea_Right = false;

      final int devMouseX = mouseEvent.x;
      final int devMouseY = mouseEvent.y;
      final Point devMousePosition = new Point(devMouseX, devMouseY);

      final boolean isMouseWithinPhoto = _photoImageBounds.contains(devMousePosition);

      if (isMouseWithinPhoto == false) {

         updateCursor(_photoCursor_Arrow);

         return;
      }

      /*
       * Mouse is within the photo
       */

      int devStartX = _devCanvas_CropArea.x;
      int devStartY = _devCanvas_CropArea.y;
      int devEndX = _devCanvas_CropArea.width;
      int devEndY = _devCanvas_CropArea.height;

      boolean isRedraw = false;

      Cursor hoveredCursor = _photoCursor_Cross;

      if (false

            || _isMouseDown_InCropArea_All

            || _isMouseDown_InCropArea_Top
            || _isMouseDown_InCropArea_Bottom
            || _isMouseDown_InCropArea_Left
            || _isMouseDown_InCropArea_Right

            || _isMouseDown_InCropArea_TopLeft
            || _isMouseDown_InCropArea_TopRight
            || _isMouseDown_InCropArea_BottomLeft
            || _isMouseDown_InCropArea_BottomRight

      ) {

         onPhoto_Mouse_32_Move_InCropArea(devMouseX, devMouseY);

         updateCropArea_TopBottomLeftRight();
         updateCropArea_InPhoto();

         _histogram.updateCropArea(getHistogramCropArea());

         isRedraw = true;

      } else if (_isSettingCropArea) {

         // the cropping area is currently created

         _devCanvas_SetCropArea_End = devMousePosition;
         _relPhoto_SetCropArea_End = getRelativeMousePhotoPosition(devMouseX, devMouseY);

         updateCropArea_FromStartEnd();

         devStartX = _devCanvas_CropArea.x;
         devStartY = _devCanvas_CropArea.y;
         devEndX = _devCanvas_CropArea.width;
         devEndY = _devCanvas_CropArea.height;

         isRedraw = true;

         /*
          * Set cursor direction according to the mouse moving, with try and error I found the
          * correct cursor :-)
          */
         if (devEndX > devStartX) {

            if (devEndY > devStartY) {

               hoveredCursor = _photoCursor_Size_ESE;

            } else {

               hoveredCursor = _photoCursor_Size_NESW;
            }

         } else {

            if (devEndY > devStartY) {

               hoveredCursor = _photoCursor_Size_NESW;

            } else {

               hoveredCursor = _photoCursor_Size_ESE;
            }
         }
      }

      if (_devCanvas_CropArea_TopLeft.contains(devMousePosition)) {

         _isMouse_InCropArea_TopLeft = true;

         hoveredCursor = _photoCursor_Size_ESE;

      } else if (_devCanvas_CropArea_TopRight.contains(devMousePosition)) {

         _isMouse_InCropArea_TopRight = true;

         hoveredCursor = _photoCursor_Size_NESW;

      } else if (_devCanvas_CropArea_BottomLeft.contains(devMousePosition)) {

         _isMouse_InCropArea_BottomLeft = true;

         hoveredCursor = _photoCursor_Size_NESW;

      } else if (_devCanvas_CropArea_BottomRight.contains(devMousePosition)) {

         _isMouse_InCropArea_BottomRight = true;

         hoveredCursor = _photoCursor_Size_ESE;

      } else if (_devCanvas_CropArea_Top.contains(devMousePosition)) {

         _isMouse_InCropArea_Top = true;

         hoveredCursor = _photoCursor_Size_NS;

      } else if (_devCanvas_CropArea_Bottom.contains(devMousePosition)) {

         _isMouse_InCropArea_Bottom = true;

         hoveredCursor = _photoCursor_Size_NS;

      } else if (_devCanvas_CropArea_Left.contains(devMousePosition)) {

         _isMouse_InCropArea_Left = true;

         hoveredCursor = _photoCursor_Size_WE;

      } else if (_devCanvas_CropArea_Right.contains(devMousePosition)) {

         _isMouse_InCropArea_Right = true;

         hoveredCursor = _photoCursor_Size_WE;

      } else {

         final int devWidth = devEndX - devStartX;
         final int devHeight = devEndY - devStartY;

         final Rectangle devCropRectangle = new Rectangle(devStartX, devStartY, devWidth, devHeight);

         if (devCropRectangle.contains(devMousePosition)) {

            _isMouse_InCropArea_All = true;

            hoveredCursor = _photoCursor_Size_All;
         }
      }

      // optimize redrawing
      if (false

            || isMouse_InCropArea_All != _isMouse_InCropArea_All

            || isMouse_InCropArea_Top != _isMouse_InCropArea_Top
            || isMouse_InCropArea_Bottom != _isMouse_InCropArea_Bottom
            || isMouse_InCropArea_Left != _isMouse_InCropArea_Left
            || isMouse_InCropArea_Right != _isMouse_InCropArea_Right

            || isMouse_InCropArea_TopLeft != _isMouse_InCropArea_TopLeft
            || isMouse_InCropArea_TopRight != _isMouse_InCropArea_TopRight
            || isMouse_InCropArea_BottomLeft != _isMouse_InCropArea_BottomLeft
            || isMouse_InCropArea_BottomRight != _isMouse_InCropArea_BottomRight

      ) {

         isRedraw = true;
      }

      if (isRedraw) {
         _photoImageCanvas.redraw();
      }

      updateCursor(hoveredCursor);
   }

   private void onPhoto_Mouse_32_Move_InCropArea(final int devMouseX, final int devMouseY) {

      final Point2D.Float relMouse = getRelativeMousePhotoPosition(devMouseX, devMouseY);

      final float relMouseX = relMouse.x;
      final float relMouseY = relMouse.y;

      if (_isMouseDown_InCropArea_Top) {

         if (relMouseY < _relPhoto_CropArea.height) {

            _devCanvas_CropArea.y = devMouseY;
            _relPhoto_CropArea.y = relMouseY;
         }

      } else if (_isMouseDown_InCropArea_Bottom) {

         if (relMouseY > _relPhoto_CropArea.y) {

            _devCanvas_CropArea.height = devMouseY;
            _relPhoto_CropArea.height = relMouseY;
         }

      } else if (_isMouseDown_InCropArea_Left) {

         if (relMouseX < _relPhoto_CropArea.width) {

            _devCanvas_CropArea.x = devMouseX;
            _relPhoto_CropArea.x = relMouseX;
         }

      } else if (_isMouseDown_InCropArea_Right) {

         if (relMouseX > _relPhoto_CropArea.x) {

            _devCanvas_CropArea.width = devMouseX;
            _relPhoto_CropArea.width = relMouseX;
         }

      } else if (_isMouseDown_InCropArea_TopLeft) {

         if (relMouseY < _relPhoto_CropArea.height && relMouseX < _relPhoto_CropArea.width) {

            _devCanvas_CropArea.x = devMouseX;
            _devCanvas_CropArea.y = devMouseY;

            _relPhoto_CropArea.x = relMouseX;
            _relPhoto_CropArea.y = relMouseY;
         }

      } else if (_isMouseDown_InCropArea_TopRight) {

         if (relMouseX > _relPhoto_CropArea.x && relMouseY < _relPhoto_CropArea.height) {

            _devCanvas_CropArea.y = devMouseY;
            _devCanvas_CropArea.width = devMouseX;

            _relPhoto_CropArea.y = relMouseY;
            _relPhoto_CropArea.width = relMouseX;
         }

      } else if (_isMouseDown_InCropArea_BottomLeft) {

         if (relMouseX < _relPhoto_CropArea.width && relMouseY > _relPhoto_CropArea.y) {

            _devCanvas_CropArea.x = devMouseX;
            _devCanvas_CropArea.height = devMouseY;

            _relPhoto_CropArea.x = relMouseX;
            _relPhoto_CropArea.height = relMouseY;
         }

      } else if (_isMouseDown_InCropArea_BottomRight) {

         if (relMouseX > _relPhoto_CropArea.x && relMouseY > _relPhoto_CropArea.y) {

            _devCanvas_CropArea.width = devMouseX;
            _devCanvas_CropArea.height = devMouseY;

            _relPhoto_CropArea.width = relMouseX;
            _relPhoto_CropArea.height = relMouseY;
         }

      } else if (_isMouseDown_InCropArea_All) {

         // crop area is moved

         final float relWidth = _relPhoto_CropArea.width - _relPhoto_CropArea.x;
         final float relHeight = _relPhoto_CropArea.height - _relPhoto_CropArea.y;

         final float relX1 = relMouseX + _relPhoto_Offset.x;
         final float relY1 = relMouseY + _relPhoto_Offset.y;

         final float relX2 = relX1 + relWidth;
         final float relY2 = relY1 + relHeight;

         // check bounds
         if (relX1 > 0 && relX1 < 1
               && relX2 > 0 && relX2 < 1
               && relY1 > 0 && relY1 < 1
               && relY2 > 0 && relY2 < 1

         ) {

            final int devWidth = _devCanvas_CropArea.width - _devCanvas_CropArea.x;
            final int devHeight = _devCanvas_CropArea.height - _devCanvas_CropArea.y;

            final int devX1 = devMouseX + _devCanvas_Offset.x;
            final int devY1 = devMouseY + _devCanvas_Offset.y;

            final int devX2 = devX1 + devWidth;
            final int devY2 = devY1 + devHeight;

            _devCanvas_CropArea.x = devX1;
            _devCanvas_CropArea.y = devY1;

            _devCanvas_CropArea.width = devX2;
            _devCanvas_CropArea.height = devY2;

            _relPhoto_CropArea.x = relX1;
            _relPhoto_CropArea.y = relY1;

            _relPhoto_CropArea.width = relX2;
            _relPhoto_CropArea.height = relY2;
         }
      }
   }

   private void onPhoto_Mouse_Exit() {

      updateCursor(null);
   }

   private void onPhoto_PaintCropping(final PaintEvent mouseEvent) {

      if (_photo.isCropped == false) {
         return;
      }

      // keep photo image position after the photo is painted in the parent class
      _photoImageBounds = _photoImageCanvas.getImageBounds();

      if (_devCanvas_CropArea == null) {

         // this happens when the dialog is opened

         updateCropArea_FromRelative(_photoImageBounds);

         if (_devCanvas_CropArea == null) {

            return;
         }
      }

      // fix bounds when window was resized
      if (_photoImageBounds.equals(_photoImageBounds_OnResize) == false) {

         _photoImageBounds_OnResize = _photoImageBounds;

         updateCropArea_FromRelative(_photoImageBounds);
      }

      final GC gc = mouseEvent.gc;

      final int devXStart = _devCanvas_CropArea.x;
      final int devYStart = _devCanvas_CropArea.y;

      final int devXEnd = _devCanvas_CropArea.width;
      final int devYEnd = _devCanvas_CropArea.height;

      int devXTopLeft;
      int devYTopLeft;
      int devWidth;
      int devHeight;

      if (devXStart < devXEnd) {

         devXTopLeft = devXStart;
         devWidth = devXEnd - devXStart;

      } else {

         devXTopLeft = devXEnd;
         devWidth = devXStart - devXEnd;
      }

      if (devYStart < devYEnd) {

         devYTopLeft = devYStart;
         devHeight = devYEnd - devYStart;

      } else {

         devYTopLeft = devYEnd;
         devHeight = devYStart - devYEnd;
      }

      gc.setLineWidth(1);

      /*
       * Paint crop rectangle
       */
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawRectangle(

            devXTopLeft,
            devYTopLeft,

            devWidth,
            devHeight);

      gc.setForeground(UI.SYS_COLOR_WHITE);
      gc.drawRectangle(

            devXTopLeft - 1,
            devYTopLeft - 1,

            devWidth + 2,
            devHeight + 2);

      /*
       * Paint hovered borders
       */
      final Color mouseDownColor = UI.SYS_COLOR_YELLOW;
      final Color mouseHoverColor = UI.SYS_COLOR_MAGENTA;

      if (_isMouse_InCropArea_All || _isMouse_InCropArea_Top || _isMouse_InCropArea_TopLeft || _isMouse_InCropArea_TopRight) {

         final boolean isMouseDown = _isMouseDown_InCropArea_All
               || _isMouseDown_InCropArea_Top
               || _isMouseDown_InCropArea_TopLeft
               || _isMouseDown_InCropArea_TopRight;

         gc.setForeground(isMouseDown ? mouseDownColor : mouseHoverColor);

         gc.drawLine(

               devXTopLeft - 1,
               devYTopLeft - 1,

               devXTopLeft + devWidth + 2,
               devYTopLeft - 1);

      }

      if (_isMouse_InCropArea_All || _isMouse_InCropArea_Bottom || _isMouse_InCropArea_BottomLeft || _isMouse_InCropArea_BottomRight) {

         final boolean isMouseDown = _isMouseDown_InCropArea_All
               || _isMouseDown_InCropArea_Bottom
               || _isMouseDown_InCropArea_BottomLeft
               || _isMouseDown_InCropArea_BottomRight;

         gc.setForeground(isMouseDown ? mouseDownColor : mouseHoverColor);

         gc.drawLine(

               devXTopLeft - 1,
               devYTopLeft + devHeight + 1,

               devXTopLeft + devWidth + 2,
               devYTopLeft + devHeight + 1);

      }

      if (_isMouse_InCropArea_All || _isMouse_InCropArea_Left || _isMouse_InCropArea_TopLeft || _isMouse_InCropArea_BottomLeft) {

         final boolean isMouseDown = _isMouseDown_InCropArea_All
               || _isMouseDown_InCropArea_Left
               || _isMouseDown_InCropArea_TopLeft
               || _isMouseDown_InCropArea_BottomLeft;

         gc.setForeground(isMouseDown ? mouseDownColor : mouseHoverColor);

         gc.drawLine(

               devXTopLeft - 1,
               devYTopLeft - 1,

               devXTopLeft - 1,
               devYTopLeft + devHeight + 1);

      }

      if (_isMouse_InCropArea_All || _isMouse_InCropArea_Right || _isMouse_InCropArea_TopRight || _isMouse_InCropArea_BottomRight) {

         final boolean isMouseDown = _isMouseDown_InCropArea_All
               || _isMouseDown_InCropArea_Right
               || _isMouseDown_InCropArea_TopRight
               || _isMouseDown_InCropArea_BottomRight;

         gc.setForeground(isMouseDown ? mouseDownColor : mouseHoverColor);

         gc.drawLine(

               devXTopLeft + devWidth + 1,
               devYTopLeft - 1,

               devXTopLeft + devWidth + 1,
               devYTopLeft + devHeight + 1);
      }
   }

   private void onPhoto_Resize(final ControlEvent event) {

      if (_relPhoto_CropArea == null) {

         return;
      }

      _photoImageBounds_OnResize = _photoImageBounds;

      updateCropArea_FromRelative(_photoImageBounds_OnResize);
   }

   @Override
   protected void onReparentShell(final Shell reparentedShell) {

      updateUI_Toolbar();

      // set default focus
      super.onReparentShell(reparentedShell);
   }

   @Override
   protected Point onResize(final int contentWidth, final int contentHeight) {

      final Point newSize = new Point(contentWidth, contentHeight);

      boolean isUpdateState = true;

      if (_selectedTooltipSize != null) {

         newSize.x = _selectedTooltipSize[0];
         newSize.y = _selectedTooltipSize[1];

         isUpdateState = false;

         _selectedTooltipSize = null;

      } else if (_isAutoResizeTooltip) {

         _isAutoResizeTooltip = false;

         onResize_AutoResize(newSize);

      } else if (_isExpandCollapseModified) {

         _isExpandCollapseModified = false;

         final GridData gd = (GridData) _containerPhotoOptions.getLayoutData();

         // get options container default height, this also makes the options visible/expanded
         gd.heightHint = SWT.DEFAULT;
         _containerPhotoOptions.getParent().layout(true);

         final Point optionsSize = _containerPhotoOptions.getSize();
         final int optionsHeight = optionsSize.y;

         if (_isTooltipExpanded) {

            // slideout is expanded

            newSize.y += optionsHeight;

         } else {

            // slideout is collappsed

            // hide options
            gd.heightHint = 0;
            _containerPhotoOptions.getParent().layout(true);

            newSize.y -= optionsHeight;
         }
      }

      /*
       * Keep tooltip size
       */
      if (isUpdateState) {

         saveState_TooltipSize(newSize.x, newSize.y);
      }

      return newSize;
   }

   private void onResize_AutoResize(final Point tooltipSize) {

      final Rectangle imageBounds = _photoImageCanvas.getImageBounds();

      if (imageBounds == null) {
         return;
      }

      final int tooltipWidth = tooltipSize.x;
      final int tooltipHeight = tooltipSize.y;

      final int photoWidth = imageBounds.width;
      final int photoHeight = imageBounds.height;

      final Rectangle canvasBounds = _photoImageCanvas.getBounds();
      final int canvasHeight = canvasBounds.height;

      final Point optionsSize = _containerPhotoOptions.getSize();

      final int optionsWidth = optionsSize.x;
      final int optionsHeight = optionsSize.y;

      final int contentWidth = optionsWidth;
      final int contentHeight = optionsHeight + canvasHeight;

      final int trimWidth = tooltipWidth - contentWidth;
      final int trimHeight = tooltipHeight - contentHeight;

      final int newWidth = trimWidth + photoWidth;
      final int newHeight = trimHeight + optionsHeight + photoHeight;

      tooltipSize.x = newWidth;
      tooltipSize.y = newHeight;
   }

   private void onSelect_TooltipResize() {

      _isAutoResizeTooltip = true;

      onTTShellResize(null);
   }

   private void onSelect_TooltipSize(final SelectionEvent selectionEvent) {

      // save selected size
      _state.put(STATE_TOOLTIP_SIZE_INDEX, getSelectedTooltipSizeIndex());

      // set size from state
      setTooltipSize();

      onTTShellResize(null);
   }

   @Override
   public void resetToDefaults() {

      switch (getSelectedTooltipSizeIndex()) {

      case 0 -> _selectedTooltipSize = STATE_TOOLTIP_SIZE_TINY_DEFAULT;
      case 1 -> _selectedTooltipSize = STATE_TOOLTIP_SIZE_SMALL_DEFAULT;
      case 2 -> _selectedTooltipSize = STATE_TOOLTIP_SIZE_MEDIUM_DEFAULT;
      case 3 -> _selectedTooltipSize = STATE_TOOLTIP_SIZE_LARGE_DEFAULT;

      }

      saveState_TooltipSize(_selectedTooltipSize[0], _selectedTooltipSize[1]);

      onTTShellResize(null);
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      final int tooltipSizeIndex = Util.getStateInt(_state, STATE_TOOLTIP_SIZE_INDEX, 0);
      _isTooltipExpanded         = Util.getStateBoolean(_state, STATE_IS_TOOLTIP_EXPANDED, false);

// SET_FORMATTING_ON

      _comboTooltipSize.select(tooltipSizeIndex);

      setupPhotoCanvasListener();
      setTooltipSize();
   }

   @Override
   protected void saveState() {

      _state.put(STATE_IS_TOOLTIP_EXPANDED, _isTooltipExpanded);

      super.saveState();
   }

   private void saveState_TooltipSize(final int width, final int height) {

      final int[] tooltipSize = new int[] { width, height };

      switch (getSelectedTooltipSizeIndex()) {

      case 0 -> Util.setState(_state, STATE_TOOLTIP_SIZE_TINY, tooltipSize);
      case 1 -> Util.setState(_state, STATE_TOOLTIP_SIZE_SMALL, tooltipSize);
      case 2 -> Util.setState(_state, STATE_TOOLTIP_SIZE_MEDIUM, tooltipSize);
      case 3 -> Util.setState(_state, STATE_TOOLTIP_SIZE_LARGE, tooltipSize);

      }
   }

   private void setTooltipSize() {

      switch (getSelectedTooltipSizeIndex()) {

      case 0 -> _selectedTooltipSize = Util.getStateIntArray(_state, STATE_TOOLTIP_SIZE_TINY, STATE_TOOLTIP_SIZE_TINY_DEFAULT);
      case 1 -> _selectedTooltipSize = Util.getStateIntArray(_state, STATE_TOOLTIP_SIZE_SMALL, STATE_TOOLTIP_SIZE_SMALL_DEFAULT);
      case 2 -> _selectedTooltipSize = Util.getStateIntArray(_state, STATE_TOOLTIP_SIZE_MEDIUM, STATE_TOOLTIP_SIZE_MEDIUM_DEFAULT);
      case 3 -> _selectedTooltipSize = Util.getStateIntArray(_state, STATE_TOOLTIP_SIZE_LARGE, STATE_TOOLTIP_SIZE_LARGE_DEFAULT);

      }
   }

   /**
    * Display a photo
    *
    * @param hoveredMapPoint
    *           Can be <code>null</code> to hide the tooltip but currently this works only when the
    *           tooltip is not pinned !!!
    */
   public void setupPhoto(final PaintedMapPoint hoveredMapPoint) {

      if (Map2PainterConfig.isShowPhotoTooltip == false) {

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

      final boolean isOtherPhoto = _hoveredMapPoint != hoveredMapPoint;

      if (isOtherPhoto && isVisible) {

         hide();
      }

      if (_hoveredMapPoint != null) {
         _previousHoveredMapPoint = _hoveredMapPoint;
      }

      if (isOtherPhoto) {

         _hoveredMapPoint = hoveredMapPoint;

         doNotStopAnimation();
         showShell();

         setupPhoto_UI(hoveredMapPoint);
      }
   }

   private void setupPhoto_UI(final PaintedMapPoint hoveredMapPoint) {

      if (hoveredMapPoint == null || _photoImageCanvas.isDisposed()) {

         _pageBook.showPage(_pageNoPhoto);

         return;
      }

      _photo = hoveredMapPoint.mapPoint.photo;

      setupPhotoCanvasListener();

      final ZonedDateTime adjustedTime_Tour_WithZone = _photo.adjustedTime_Tour_WithZone;
      if (adjustedTime_Tour_WithZone != null) {

         final String photoDateTime = "%s  %s".formatted( //$NON-NLS-1$
               adjustedTime_Tour_WithZone.format(TimeTools.Formatter_Weekday),
               adjustedTime_Tour_WithZone.format(TimeTools.Formatter_DateTime_M));

         updateTitleText(photoDateTime);
      }

      final boolean isPhotoCropped = _photo.isCropped;

      _chkCropPhoto.setSelection(isPhotoCropped);

      /*
       * Get crop area from the tour photo
       */
      float cropAreaX1 = _photo.cropAreaX1;
      float cropAreaY1 = _photo.cropAreaY1;
      float cropAreaX2 = _photo.cropAreaX2;
      float cropAreaY2 = _photo.cropAreaY2;

      if (cropAreaX1 == 0 && cropAreaX2 == 0
            || cropAreaY1 == 0 && cropAreaY2 == 0) {

         // set initial and valid crop areas

         final float defaultCrop = 0.35f;
         final float defaultCrop2 = 1 - defaultCrop;

         _photo.cropAreaX1 = cropAreaX1 = defaultCrop;
         _photo.cropAreaY1 = cropAreaY1 = defaultCrop;

         _photo.cropAreaX2 = cropAreaX2 = defaultCrop2;
         _photo.cropAreaY2 = cropAreaY2 = defaultCrop2;
      }

      _relPhoto_CropArea = new Rectangle2D.Float(

            cropAreaX1,
            cropAreaY1,
            cropAreaX2,
            cropAreaY2);

      updateCropArea_FromRelative(_photoImageBounds);

      final Image photoImage = getPhotoImage(_photo);

      _photoImageCanvas.setImage(photoImage, false);
      _histogram.setImage(photoImage, getHistogramCropArea());

      if (photoImage == null || photoImage.isDisposed()) {

         updateUI_LoadingMessage();

      } else {

         _pageBook.showPage(_pagePhoto);
      }
   }

   private void setupPhotoCanvasListener() {

      if (_photo == null) {
         return;
      }

// SET_FORMATTING_OFF

      if (_photo.isCropped) {

         if (_isCanvasListenerSet == false) {

            _isCanvasListenerSet = true;

            _photoImageCanvas.addControlListener(        _photoResizeListener);

            _photoImageCanvas.addMouseMoveListener(      _photoMouseMoveListener);
            _photoImageCanvas.addMouseListener(          _photoMouseDownListener);
            _photoImageCanvas.addMouseListener(          _photoMouseUpListener);
            _photoImageCanvas.addMouseTrackListener(     _photoMouseExitListener);
         }

      } else {

         if (_isCanvasListenerSet) {

            _isCanvasListenerSet = false;

            _photoImageCanvas.removeControlListener(     _photoResizeListener);

            _photoImageCanvas.removeMouseMoveListener(   _photoMouseMoveListener);
            _photoImageCanvas.removeMouseListener(       _photoMouseDownListener);
            _photoImageCanvas.removeMouseListener(       _photoMouseUpListener);
            _photoImageCanvas.removeMouseTrackListener(  _photoMouseExitListener);
         }

         // set default cursor
         updateCursor(_photoCursor_Arrow);
      }

// SET_FORMATTING_ON
   }

   /**
    * Create absolute position {@link #_devCanvas_CropArea} from relative position
    * {@link #_relPhoto_CropArea}
    *
    * @param photoImageBounds
    */
   private void updateCropArea_FromRelative(final Rectangle photoImageBounds) {

      if (photoImageBounds == null || _relPhoto_CropArea == null) {

         return;
      }

      final float relCropStartX = _relPhoto_CropArea.x;
      final float relCropStartY = _relPhoto_CropArea.y;

      final float relCropEndX = _relPhoto_CropArea.width;
      final float relCropEndY = _relPhoto_CropArea.height;

      final int devPhotoX = photoImageBounds.x;
      final int devPhotoY = photoImageBounds.y;

      final float devPhotoWidth = photoImageBounds.width;
      final float devPhotoHeight = photoImageBounds.height;

      final int devCropStartX = (int) (devPhotoX + relCropStartX * devPhotoWidth);
      final int devCropStartY = (int) (devPhotoY + relCropStartY * devPhotoHeight);

      final int devCropEndX = (int) (devPhotoX + relCropEndX * devPhotoWidth);
      final int devCropEndY = (int) (devPhotoY + relCropEndY * devPhotoHeight);

      _devCanvas_CropArea = new Rectangle(devCropStartX, devCropStartY, devCropEndX, devCropEndY);

      updateCropArea_TopBottomLeftRight();

      _histogram.updateCropArea(getHistogramCropArea());
   }

   /**
    * Update the relative and device crop areas from the start/end positions
    */
   private void updateCropArea_FromStartEnd() {

      updateCropArea_FromStartEnd_Dev();
      updateCropArea_FromStartEnd_Rel();
   }

   private void updateCropArea_FromStartEnd_Dev() {

      int devCropAreaX1 = _devCanvas_SetCropArea_Start.x;
      int devCropAreaY1 = _devCanvas_SetCropArea_Start.y;
      int devCropAreaX2 = _devCanvas_SetCropArea_End.x;
      int devCropAreaY2 = _devCanvas_SetCropArea_End.y;

      // swap values that x/y 2 is larger than x/y 1
      if (devCropAreaX1 > devCropAreaX2) {

         final int tmpCropArea = devCropAreaX1;

         devCropAreaX1 = devCropAreaX2;
         devCropAreaX2 = tmpCropArea;
      }

      if (devCropAreaY1 > devCropAreaY2) {

         final int tmpCropArea = devCropAreaY1;

         devCropAreaY1 = devCropAreaY2;
         devCropAreaY2 = tmpCropArea;
      }

      final int devPhotoX = _photoImageBounds.x;
      final int devPhotoY = _photoImageBounds.y;

      final int devPhotoWidth = _photoImageBounds.width;
      final int devPhotoHeight = _photoImageBounds.height;

      // check/fix bounds
      if (devCropAreaX1 < devPhotoX) {
         devCropAreaX1 = devPhotoX;
      }

      if (devCropAreaY1 < devPhotoY) {
         devCropAreaY1 = devPhotoY;
      }

      if (devCropAreaX2 > devPhotoX + devPhotoWidth) {
         devCropAreaX2 = devPhotoX + devPhotoWidth;
      }

      if (devCropAreaY2 > devPhotoY + devPhotoHeight) {
         devCropAreaY2 = devPhotoY + devPhotoHeight;
      }

      _devCanvas_CropArea = new Rectangle(
            devCropAreaX1,
            devCropAreaY1,
            devCropAreaX2,
            devCropAreaY2);

      updateCropArea_TopBottomLeftRight();

      _histogram.updateCropArea(getHistogramCropArea());
   }

   private void updateCropArea_FromStartEnd_Rel() {

      float relCropAreaX1 = _relPhoto_SetCropArea_Start.x;
      float relCropAreaY1 = _relPhoto_SetCropArea_Start.y;
      float relCropAreaX2 = _relPhoto_SetCropArea_End.x;
      float relCropAreaY2 = _relPhoto_SetCropArea_End.y;

      // swap values that x/y 2 is larger than x/y 1
      if (relCropAreaX1 > relCropAreaX2) {

         final float tmpCropArea = relCropAreaX1;

         relCropAreaX1 = relCropAreaX2;
         relCropAreaX2 = tmpCropArea;
      }

      if (relCropAreaY1 > relCropAreaY2) {

         final float tmpCropArea = relCropAreaY1;

         relCropAreaY1 = relCropAreaY2;
         relCropAreaY2 = tmpCropArea;
      }

      _relPhoto_CropArea = new Rectangle2D.Float(

            relCropAreaX1,
            relCropAreaY1,
            relCropAreaX2,
            relCropAreaY2);
   }

   /**
    * Set cropping area into the photo/tour photo
    */
   private void updateCropArea_InPhoto() {

      _photo.isAdjustmentModified = true;

      _photo.cropAreaX1 = _relPhoto_CropArea.x;
      _photo.cropAreaY1 = _relPhoto_CropArea.y;

      _photo.cropAreaX2 = _relPhoto_CropArea.width;
      _photo.cropAreaY2 = _relPhoto_CropArea.height;

      _photo.updateMapImageRenderSize();

      updateTourPhotoInDB(_photo);
   }

   private void updateCropArea_TopBottomLeftRight() {

      final int devCropAreaX1 = _devCanvas_CropArea.x;
      final int devCropAreaY1 = _devCanvas_CropArea.y;
      final int devCropAreaX2 = _devCanvas_CropArea.width;
      final int devCropAreaY2 = _devCanvas_CropArea.height;

      final int devCropAreaWidth = devCropAreaX2 - devCropAreaX1;
      final int devCropAreaHeight = devCropAreaY2 - devCropAreaY1;

      final int hoverMargin = 5;
      final int hoverMargin2 = 2 * hoverMargin;

      /*
       * Sides
       */
      _devCanvas_CropArea_Top = new Rectangle(
            devCropAreaX1,
            devCropAreaY1 - hoverMargin,
            devCropAreaWidth,
            hoverMargin2);

      _devCanvas_CropArea_TopLeft = new Rectangle(
            devCropAreaX1 - hoverMargin,
            devCropAreaY1 - hoverMargin,
            hoverMargin2,
            hoverMargin2);

      _devCanvas_CropArea_Left = new Rectangle(
            devCropAreaX1 - hoverMargin,
            devCropAreaY1,
            hoverMargin2,
            devCropAreaHeight);

      _devCanvas_CropArea_Right = new Rectangle(
            devCropAreaX1 + devCropAreaWidth - hoverMargin,
            devCropAreaY1,
            hoverMargin2,
            devCropAreaHeight);

      /*
       * Corners
       */

      _devCanvas_CropArea_TopRight = new Rectangle(
            devCropAreaX1 + devCropAreaWidth - hoverMargin,
            devCropAreaY1 - hoverMargin,
            hoverMargin2,
            hoverMargin2);

      _devCanvas_CropArea_Bottom = new Rectangle(
            devCropAreaX1,
            devCropAreaY1 + devCropAreaHeight - hoverMargin,
            devCropAreaWidth,
            hoverMargin2);

      _devCanvas_CropArea_BottomLeft = new Rectangle(
            devCropAreaX1 - hoverMargin,
            devCropAreaY1 + devCropAreaHeight - hoverMargin,
            hoverMargin2,
            hoverMargin2);

      _devCanvas_CropArea_BottomRight = new Rectangle(
            devCropAreaX1 + devCropAreaWidth - hoverMargin,
            devCropAreaY1 + devCropAreaHeight - hoverMargin,
            hoverMargin2,
            hoverMargin2);
   }

   /**
    * Update cursor only when it was modified
    *
    * @param cursor
    */
   private void updateCursor(final Cursor cursor) {

      if (_currentCursor != cursor) {

         _currentCursor = cursor;

         _photoImageCanvas.setCursor(cursor);
      }
   }

   /**
    * Update tour photo in the db and fire an modify event
    *
    * @param photo
    */
   private void updateTourPhotoInDB(final Photo photo) {

      final String sql = UI.EMPTY_STRING

            + "UPDATE " + TourDatabase.TABLE_TOUR_PHOTO + NL //$NON-NLS-1$

            + " SET" + NL //                                //$NON-NLS-1$

            + " isPhotoCropped = ?,    " + NL //            //$NON-NLS-1$

            + " cropAreaX1 = ?,        " + NL //            //$NON-NLS-1$
            + " cropAreaY1 = ?,        " + NL //            //$NON-NLS-1$

            + " cropAreaX2 = ?,        " + NL //            //$NON-NLS-1$
            + " cropAreaY2 = ?         " + NL //            //$NON-NLS-1$

            + " WHERE photoId = ?      " + NL //            //$NON-NLS-1$
      ;

      try (final Connection conn = TourDatabase.getInstance().getConnection();
            final PreparedStatement sqlUpdate = conn.prepareStatement(sql)) {

         final ArrayList<Photo> updatedPhotos = new ArrayList<>();

         final Collection<TourPhotoReference> photoRefs = photo.getTourPhotoReferences().values();

         if (photoRefs.size() > 0) {

            for (final TourPhotoReference photoRef : photoRefs) {

               /*
                * Update db
                */
               sqlUpdate.setBoolean(1, photo.isCropped);

               sqlUpdate.setFloat(2, photo.cropAreaX1);
               sqlUpdate.setFloat(3, photo.cropAreaY1);

               sqlUpdate.setFloat(4, photo.cropAreaX2);
               sqlUpdate.setFloat(5, photo.cropAreaY2);

               sqlUpdate.setLong(6, photoRef.photoId);

               sqlUpdate.executeUpdate();

               /*
                * Update tour photo
                */
               final TourData tourData = TourManager.getInstance().getTourData(photoRef.tourId);
               final Set<TourPhoto> allTourPhotos = tourData.getTourPhotos();

               for (final TourPhoto tourPhoto : allTourPhotos) {

                  if (tourPhoto.getPhotoId() == photoRef.photoId) {

                     tourPhoto.setPhotoCropped(photo.isCropped);

                     tourPhoto.setCropAreaX1(photo.cropAreaX1);
                     tourPhoto.setCropAreaY1(photo.cropAreaY1);

                     tourPhoto.setCropAreaX2(photo.cropAreaX2);
                     tourPhoto.setCropAreaY2(photo.cropAreaY2);

                     break;
                  }
               }
            }

            updatedPhotos.add(photo);
         }

         if (updatedPhotos.size() > 0) {

            // fire notification to update all galleries with the modified crop size

            PhotoManager.firePhotoEvent(null, PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED, updatedPhotos);
         }

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }
   }

   private void updateUI_ExpandedCollapsed_Action() {

      if (_isTooltipExpanded) {

         _actionExpandCollapseSlideout.setToolTipText(OtherMessages.SLIDEOUT_ACTION_COLLAPSE_SLIDEOUT_TOOLTIP);
         _actionExpandCollapseSlideout.setImageDescriptor(_imageDescriptor_SlideoutCollapse);

      } else {

         _actionExpandCollapseSlideout.setToolTipText(OtherMessages.SLIDEOUT_ACTION_EXPAND_SLIDEOUT_TOOLTIP);
         _actionExpandCollapseSlideout.setImageDescriptor(_imageDescriptor_SlideoutExpand);
      }

      _toolbarManagerExpandCollapseSlideout.update(true);
   }

   private void updateUI_ExpandedCollapsed_Layout() {

      final GridData gd = (GridData) _containerPhotoOptions.getLayoutData();

      if (_isTooltipExpanded) {

         // slideout is expanded

         gd.heightHint = SWT.DEFAULT;

      } else {

         // slideout is collappsed

         // hide options
         gd.heightHint = 0;
      }

      _containerPhotoOptions.getParent().layout(true);
   }

   private void updateUI_LoadingMessage() {

      if (_hoveredMapPoint == null) {

         _labelMessage.setText(UI.EMPTY_STRING);

      } else {

         final Photo photo = _hoveredMapPoint.mapPoint.photo;

         final String photoText = Messages.Slideout_MapPoint_PhotoToolTip_Label_LoadingMessage + UI.SPACE + photo.imageFilePathName;

         _labelMessage.setText(photoText);
      }

      _pageBook.showPage(_pageNoPhoto);
   }

   private boolean updateUI_ShowLoadingImage(final GC gc, final Rectangle rectangle) {

      updateUI_LoadingMessage();

      return true;
   }

   private void updateUI_Toolbar() {

      final boolean isResizableShell = isResizableShell();

      final GridData gd1 = (GridData) _containerHeader_1.getLayoutData();
      final GridData gd2 = (GridData) _containerHeader_2.getLayoutData();

      if (isResizableShell) {

         gd1.exclude = false;
         gd2.exclude = false;

         _containerHeader_1.setVisible(true);
         _containerHeader_2.setVisible(true);

      } else {

         gd1.exclude = true;
         gd2.exclude = true;

         _containerHeader_1.setVisible(false);
         _containerHeader_2.setVisible(false);
      }

      _containerHeader_1.getParent().layout(true);

      showDefaultActions(isResizableShell);
   }

}
