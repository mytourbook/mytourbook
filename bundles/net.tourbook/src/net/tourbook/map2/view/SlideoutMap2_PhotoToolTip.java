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
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.IPhotoPreferences;
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
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.part.PageBook;

/**
 * Slideout for all 2D map locations and marker
 */
public class SlideoutMap2_PhotoToolTip extends AdvancedSlideout implements IActionResetToDefault {

   private static final String          ID                                = "net.tourbook.map2.view.MapPointToolTip_Photo"; //$NON-NLS-1$

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

   private boolean               _isTooltipExpanded;
   private boolean               _isExpandCollapseModified;
   private boolean               _isAutoResizeTooltip;

   private ToolBarManager        _toolbarManagerExpandCollapseSlideout;

   private ActionExpandSlideout  _actionExpandCollapseSlideout;
   private ActionResetToDefaults _actionRestoreDefaults;

   private ImageDescriptor       _imageDescriptor_SlideoutCollapse;
   private ImageDescriptor       _imageDescriptor_SlideoutExpand;

   private int[]                 _selectedTooltipSize;

   /*
    * UI controls
    */
   private PageBook         _pageBook;

   private Composite        _containerPhotoOptions;
   private Composite        _containerHeader_1;
   private Composite        _containerHeader_2;
   private Composite        _pageNoPhoto;
   private Composite        _pagePhoto;

   private Combo            _comboTooltipSize;

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

      updateUI_SetUIPage(_hoveredMapPoint);

      restoreState();

      updateUI_ExpandedCollapsed_Action();
      updateUI_ExpandedCollapsed_Layout();

      updateUI_Toolbar();

      // show dialog with dark colors, this looks better for photos with the bright theme
      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      UI.setChildColors(parent.getShell(),
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND),
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));
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
      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         createUI_30_PhotoOptions(container);
         createUI_20_PhotoImage(container);
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
            .extendedMargins(0, 0, 0, 5)
            .numColumns(2)
            .applyTo(_containerPhotoOptions);
      {

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
                  imageLoadCallback,

                  false // get SWT image
            );
         }
      }

      return photoImage;
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

// SET_FORMATTING_OFF

      _imageDescriptor_SlideoutCollapse   = CommonActivator.getThemedImageDescriptor_Dark(CommonImages.Slideout_Collapse);
      _imageDescriptor_SlideoutExpand     = CommonActivator.getThemedImageDescriptor_Dark(CommonImages.Slideout_Expand);

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
   protected void onFocus() {

   }

   private void onImageIsLoaded() {

      final PaintedMapPoint hoveredMapPoint = _hoveredMapPoint;

      Display.getDefault().asyncExec(() -> {

         if (_hoveredMapPoint != null) {

            updateUI_SetUIPage(hoveredMapPoint);

         } else if (_previousHoveredMapPoint != null) {

            /*
             * This happens when an image is loading and the mouse has exited the tooltip -> paint
             * loaded image
             */

            updateUI_SetUIPage(_previousHoveredMapPoint);
         }
      });
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

      final Point imageSize = _photoImageCanvas.getResizedImageSize();

      if (imageSize == null) {
         return;
      }

      final int tooltipWidth = tooltipSize.x;
      final int tooltipHeight = tooltipSize.y;

      final int photoWidth = imageSize.x;
      final int photoHeight = imageSize.y;

      final Rectangle imageCanvasBounds = _photoImageCanvas.getBounds();
      final int imageCanvasHeight = imageCanvasBounds.height;

      final Point optionsSize = _containerPhotoOptions.getSize();

      final int optionsWidth = optionsSize.x;
      final int optionsHeight = optionsSize.y;

      final int contentWidth = optionsWidth;
      final int contentHeight = optionsHeight + imageCanvasHeight;

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

      _isTooltipExpanded = Util.getStateBoolean(_state, STATE_IS_TOOLTIP_EXPANDED, false);

      _comboTooltipSize.select(Util.getStateInt(_state, STATE_TOOLTIP_SIZE_INDEX, 0));

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

      updateUI_SetUIPage(hoveredMapPoint);
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

   private void updateUI_SetUIPage(final PaintedMapPoint hoveredMapPoint) {

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
      if (adjustedTime_Tour_WithZone != null) {

         final String photoDateTime = "%s  %s".formatted( //$NON-NLS-1$
               adjustedTime_Tour_WithZone.format(TimeTools.Formatter_Weekday),
               adjustedTime_Tour_WithZone.format(TimeTools.Formatter_DateTime_M));

         updateTitleText(photoDateTime);
      }

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
