/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.CurveType;
import net.tourbook.photo.Histogram;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoAdjustments;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.TourPhotoReference;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.PageBook;

/**
 * Slideout for the 2D map photo tooltip
 */
public class SlideoutMap2_PhotoHistogram extends AdvancedSlideout implements IActionResetToDefault {

   private static final String          ID                         = "net.tourbook.map2.view.SlideoutMap2_PhotoHistogram"; //$NON-NLS-1$

   private static final int             THREE_POINT_DEFAULT_MIN    = 0;
   private static final int             THREE_POINT_DEFAULT_MAX    = 255;

   private static final int             THREE_POINT_DEFAULT_DARK   = 0;
   private static final int             THREE_POINT_DEFAULT_BRIGHT = 255;
   private static final int             THREE_POINT_DEFAULT_MIDDLE = 50;                                                   // %

   private static final char            NL                         = UI.NEW_LINE;

   private final static IDialogSettings _state                     = TourbookPlugin.getState(ID);

   /**
    * Filter operator MUST be in sync with filter labels
    */
   private static CurveType[]           _allCurveTypes_Value       = {

         CurveType.THREE_POINTS,
         CurveType.MULTIPLE_POINTS,
   };

   /**
    * Filter labels MUST be in sync with filter operator
    */
   private static String[]              _allCurveTypes_Label       = {

         "3 Points",
         "Multiple Points",
   };

   private Map2                         _map2;

   private FocusListener                _keepOpenListener;
   private MouseWheelListener           _mouseWheelListener3Points;
   private SelectionListener            _selectedListener3Points;

   private PaintedMapPoint              _hoveredMapPoint;
   private PaintedMapPoint              _previousHoveredMapPoint;
   private Photo                        _photo;

   private ActionReset3Point            _actionReset3Point_Bright;
   private ActionReset3Point            _actionReset3Point_Dark;
   private ActionReset3Point            _actionReset3Point_MiddleX;
   private ActionReset3Point            _actionReset3Point_MiddleY;

   /*
    * UI controls
    */
   private PageBook  _pageBook;

   private Composite _pageNoPhoto;
   private Composite _pagePhoto;

   private Composite _container3Points;

   private Button    _chkAdjustTonality;

   private Combo     _comboCurveType;

   private Label     _lblBright;
   private Label     _lblDark;
   private Label     _lblMiddleX;
   private Label     _lblMiddleY;
   private Label     _lblCurveType;
   private Label     _labelMessage;

   private Spinner   _spinnerLevel_Bright;
   private Spinner   _spinnerLevel_Dark;
   private Spinner   _spinnerLevel_MiddleX;
   private Spinner   _spinnerLevel_MiddleY;

   private Histogram _histogram;

   private ToolBar   _toolbarReset3Point_Bright;
   private ToolBar   _toolbarReset3Point_Dark;
   private ToolBar   _toolbarReset3Point_MiddleX;
   private ToolBar   _toolbarReset3Point_MiddleY;

   private class ActionReset3Point extends Action {

      Spinner spinner;

      public ActionReset3Point() {

         super("X", AS_PUSH_BUTTON);
      }

      @Override
      public void runWithEvent(final Event event) {

         on3Points_Reset(spinner);
      }
   }

   public SlideoutMap2_PhotoHistogram(final Map2 map2) {

      super(map2, _state, null);

      _map2 = map2;

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);

      // ensure the tooltip header actions are displayed with the dark theme icons
      setDarkThemeForToolbarActions();
   }

   @Override
   public void close() {

      Map2PointManager.setMapLocationSlideout(null);

      super.close();
   }

   private void createActions() {

      _actionReset3Point_Dark = new ActionReset3Point();
      _actionReset3Point_Bright = new ActionReset3Point();
      _actionReset3Point_MiddleX = new ActionReset3Point();
      _actionReset3Point_MiddleY = new ActionReset3Point();
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);
      createActions();

      createUI(parent);

      updateActions();
      fillUI();

      setupPhoto_UI(_hoveredMapPoint);

      restoreState();

      // show dialog with dark colors, this looks better for photos with the bright theme
      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      UI.setChildColors(parent.getShell(),
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND),
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

      _pagePhoto = createUI_10_WithPhoto(_pageBook);
      _pageNoPhoto = createUI_90_NoPhoto(_pageBook);

      _pageBook.showPage(_pageNoPhoto);
   }

   private Composite createUI_10_WithPhoto(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .spacing(0, 0)
            .applyTo(container);
      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         createUI_20_Histogram(container);
         createUI_30_HistogramControls(container);
      }

      return container;
   }

   private void createUI_20_Histogram(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1)
            .extendedMargins(0, 0, 0, 5)
            .numColumns(2)
            .applyTo(container);
//      _containerPhotoOptions.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            /*
             * Adjust curves
             */
            _chkAdjustTonality = new Button(container, SWT.CHECK);
            _chkAdjustTonality.setText("&Adjust tonality");
            _chkAdjustTonality.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onAdjustTonality()));
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.BEGINNING)
                  .span(2, 1)
                  .applyTo(_chkAdjustTonality);

         }
         {
            /*
             * Curve type
             */
            _lblCurveType = new Label(container, SWT.NONE);
            _lblCurveType.setText("Curve &type");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblCurveType);

            _comboCurveType = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _comboCurveType.setVisibleItemCount(10);
            _comboCurveType.setToolTipText("");
            _comboCurveType.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelectCurveType(selectionEvent)));
            _comboCurveType.addFocusListener(_keepOpenListener);

            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_comboCurveType);
         }
         {
            _histogram = new Histogram(container);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .span(2, 1)
                  .hint(SWT.DEFAULT, 100)
                  .applyTo(_histogram);
         }

      }
   }

   private void createUI_30_HistogramControls(final Composite parent) {

      _container3Points = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_container3Points);
      GridLayoutFactory.fillDefaults().numColumns(7).applyTo(_container3Points);
//      _container3Points.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            /*
             * Dark
             */

            // create a label for keyboard access
            _lblDark = new Label(_container3Points, SWT.NONE);
            _lblDark.setText("&Dark");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblDark);

            _spinnerLevel_Dark = new Spinner(_container3Points, SWT.BORDER);
            _spinnerLevel_Dark.setMinimum(THREE_POINT_DEFAULT_MIN);
            _spinnerLevel_Dark.setMaximum(THREE_POINT_DEFAULT_MAX);
            _spinnerLevel_Dark.setIncrement(1);
            _spinnerLevel_Dark.setPageIncrement(10);
            _spinnerLevel_Dark.addSelectionListener(_selectedListener3Points);
            _spinnerLevel_Dark.addMouseWheelListener(_mouseWheelListener3Points);

            _toolbarReset3Point_Dark = UI.createToolbarAction(_container3Points, _actionReset3Point_Dark);
            GridDataFactory.fillDefaults()
                  .indent(-5, 0)
                  .applyTo(_toolbarReset3Point_Dark);

         }

         createUI_40_3Point_Middle(_container3Points);

         {
            /*
             * Bright
             */

            // create a label for keyboard access
            _lblBright = new Label(_container3Points, SWT.NONE);
            _lblBright.setText("&Bright");
            GridDataFactory.fillDefaults()
//                  .grab(true, false)
                  .align(SWT.END, SWT.CENTER).applyTo(_lblBright);

            _spinnerLevel_Bright = new Spinner(_container3Points, SWT.BORDER);
            _spinnerLevel_Bright.setMinimum(THREE_POINT_DEFAULT_MIN);
            _spinnerLevel_Bright.setMaximum(THREE_POINT_DEFAULT_MAX);
            _spinnerLevel_Bright.setIncrement(1);
            _spinnerLevel_Bright.setPageIncrement(10);
            _spinnerLevel_Bright.addSelectionListener(_selectedListener3Points);
            _spinnerLevel_Bright.addMouseWheelListener(_mouseWheelListener3Points);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_spinnerLevel_Bright);

            _toolbarReset3Point_Bright = UI.createToolbarAction(_container3Points, _actionReset3Point_Bright);
            GridDataFactory.fillDefaults()
                  .indent(-5, 0)
                  .applyTo(_toolbarReset3Point_Bright);
         }
      }
   }

   private void createUI_40_3Point_Middle(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.CENTER, SWT.FILL)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(6)
            .spacing(3, 0)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            /*
             * Middle X
             */
            _lblMiddleX = new Label(container, SWT.NONE);
            _lblMiddleX.setText("&X");
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_lblMiddleX);

            _spinnerLevel_MiddleX = new Spinner(container, SWT.BORDER);
            _spinnerLevel_MiddleX.setMinimum(0);
            _spinnerLevel_MiddleX.setMaximum(100_0);
            _spinnerLevel_MiddleX.setIncrement(10);
            _spinnerLevel_MiddleX.setPageIncrement(100);
            _spinnerLevel_MiddleX.setDigits(1);
            _spinnerLevel_MiddleX.addSelectionListener(_selectedListener3Points);
            _spinnerLevel_MiddleX.addMouseWheelListener(_mouseWheelListener3Points);

            _toolbarReset3Point_MiddleX = UI.createToolbarAction(container, _actionReset3Point_MiddleX);
            GridDataFactory.fillDefaults()
                  .indent(-3, 0)
                  .applyTo(_toolbarReset3Point_MiddleX);
         }
         {
            /*
             * Middle Y
             */
            _lblMiddleY = new Label(container, SWT.NONE);
            _lblMiddleY.setText("&Y");
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .indent(5, 0)
                  .applyTo(_lblMiddleY);

            _spinnerLevel_MiddleY = new Spinner(container, SWT.BORDER);
            _spinnerLevel_MiddleY.setMinimum(0);
            _spinnerLevel_MiddleY.setMaximum(100_0);
            _spinnerLevel_MiddleY.setIncrement(10);
            _spinnerLevel_MiddleY.setPageIncrement(100);
            _spinnerLevel_MiddleY.setDigits(1);
            _spinnerLevel_MiddleY.addSelectionListener(_selectedListener3Points);
            _spinnerLevel_MiddleY.addMouseWheelListener(_mouseWheelListener3Points);

            _toolbarReset3Point_MiddleY = UI.createToolbarAction(container, _actionReset3Point_MiddleY);
            GridDataFactory.fillDefaults()
                  .indent(-3, 0)
                  .applyTo(_toolbarReset3Point_MiddleY);
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

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean isAdjustTonality = _chkAdjustTonality.getSelection();

      _actionReset3Point_Bright  .setEnabled(isAdjustTonality);
      _actionReset3Point_Dark    .setEnabled(isAdjustTonality);
      _actionReset3Point_MiddleX .setEnabled(isAdjustTonality);
      _actionReset3Point_MiddleY .setEnabled(isAdjustTonality);
      _comboCurveType            .setEnabled(isAdjustTonality);
      _lblCurveType              .setEnabled(isAdjustTonality);
      _lblBright                 .setEnabled(isAdjustTonality);
      _lblDark                   .setEnabled(isAdjustTonality);
      _lblMiddleX                .setEnabled(isAdjustTonality);
      _lblMiddleY                .setEnabled(isAdjustTonality);
      _spinnerLevel_Bright       .setEnabled(isAdjustTonality);
      _spinnerLevel_Dark         .setEnabled(isAdjustTonality);
      _spinnerLevel_MiddleX      .setEnabled(isAdjustTonality);
      _spinnerLevel_MiddleY      .setEnabled(isAdjustTonality);

// SET_FORMATTING_ON
   }

   private void fillUI() {

      for (final String curveTypeLabel : _allCurveTypes_Label) {

         _comboCurveType.add(curveTypeLabel);
      }
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

            PhotoLoadManager.putImageInLoadingQueueHQ_Map(

                  photo,
                  requestedImageQuality,
                  _map2.getPhotoTooltipImageLoaderCallback());
         }
      }

      return photoImage;
   }

   private CurveType getSelectedCurveType() {

      final int selectedIndex = _comboCurveType.getSelectionIndex();

      if (selectedIndex >= 0) {

         return _allCurveTypes_Value[selectedIndex];

      } else {

         return CurveType.THREE_POINTS;
      }
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

      _selectedListener3Points = SelectionListener.widgetSelectedAdapter(selectionEvent -> {
         on3Points_Change(selectionEvent.widget);
      });

      _mouseWheelListener3Points = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         on3Points_Change(mouseEvent.widget);
      };

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

   private void on3Points_Change(final Widget widget) {

      validate3Points(widget);

      _histogram.update3Points(_photo);

      updateModelAndUI();
   }

   private void on3Points_Reset(final Spinner spinner) {

      if (spinner == _spinnerLevel_Dark) {

         _photo.threePoint_Dark = THREE_POINT_DEFAULT_DARK;

         _spinnerLevel_Dark.setFocus();
         _spinnerLevel_Dark.setSelection(_photo.threePoint_Dark);

      } else if (spinner == _spinnerLevel_MiddleX) {

         _photo.threePoint_MiddleX = THREE_POINT_DEFAULT_MIDDLE;

         _spinnerLevel_MiddleX.setFocus();
         _spinnerLevel_MiddleX.setSelection((int) (_photo.threePoint_MiddleX * 10));

      } else if (spinner == _spinnerLevel_MiddleY) {

         _photo.threePoint_MiddleY = THREE_POINT_DEFAULT_MIDDLE;

         _spinnerLevel_MiddleY.setFocus();
         _spinnerLevel_MiddleY.setSelection((int) (_photo.threePoint_MiddleY * 10));

      } else if (spinner == _spinnerLevel_Bright) {

         _photo.threePoint_Bright = THREE_POINT_DEFAULT_BRIGHT;

         _spinnerLevel_Bright.setFocus();
         _spinnerLevel_Bright.setSelection(_photo.threePoint_Bright);
      }

      validate3Points(spinner);

      _histogram.update3Points(_photo);

      updateModelAndUI();
   }

   private void onAdjustTonality() {

      final boolean isAdjustTonality = _chkAdjustTonality.getSelection();

      _photo.isSetTonality = isAdjustTonality;

      enableControls();

      updateModelAndUI();
   }

   @Override
   protected void onDispose() {

      super.onDispose();
   }

   @Override
   protected void onFocus() {

   }

   public void onImageIsLoaded() {

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

   private void onSelectCurveType(final SelectionEvent selectionEvent) {

      _photo.curveType = getSelectedCurveType();

      updateModelAndUI();
   }

   @Override
   public void resetToDefaults() {

   }

   private void restoreState() {

   }

   @Override
   protected void saveState() {

      super.saveState();
   }

   private void selectCurveType(final Enum<CurveType> requestedCurveType) {

      int selectionIndex = 0;

      for (int operatorIndex = 0; operatorIndex < _allCurveTypes_Value.length; operatorIndex++) {

         final CurveType curveType = _allCurveTypes_Value[operatorIndex];

         if (curveType.equals(requestedCurveType)) {
            selectionIndex = operatorIndex;
            break;
         }
      }

      _comboCurveType.select(selectionIndex);
   }

   /**
    * Display a photo
    *
    * @param hoveredMapPoint
    *           Can be <code>null</code> to hide the tooltip but currently this works only when the
    *           tooltip is not pinned !!!
    */
   public void setupPhoto(final PaintedMapPoint hoveredMapPoint) {

      if (Map2PainterConfig.isShowPhotoHistogram == false) {

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

      if (hoveredMapPoint == null || _histogram.isDisposed()) {

         _pageBook.showPage(_pageNoPhoto);

         return;
      }

      _photo = hoveredMapPoint.mapPoint.photo;

      /*
       * Update UI from model
       */

      // update title
      final ZonedDateTime adjustedTime_Tour_WithZone = _photo.adjustedTime_Tour_WithZone;
      if (adjustedTime_Tour_WithZone != null) {

         final String photoDateTime = "%s  %s".formatted( //$NON-NLS-1$
               adjustedTime_Tour_WithZone.format(TimeTools.Formatter_Weekday),
               adjustedTime_Tour_WithZone.format(TimeTools.Formatter_DateTime_M));

         updateTitleText(photoDateTime);
      }

      // update tonality
      _chkAdjustTonality.setSelection(_photo.isSetTonality);
      _spinnerLevel_Dark.setSelection(_photo.threePoint_Dark);
      _spinnerLevel_Bright.setSelection(_photo.threePoint_Bright);
      _spinnerLevel_MiddleX.setSelection((int) (_photo.threePoint_MiddleX * 10));
      _spinnerLevel_MiddleY.setSelection((int) (_photo.threePoint_MiddleY * 10));
      selectCurveType(_photo.curveType);
      updateUI_3PointActions();

      final Image photoImage = getPhotoImage(_photo);
      final Float relCropArea = _photo.getValidCropArea();

      _histogram.setImage(photoImage, _photo.isCropped ? relCropArea : null);
      _histogram.update3Points(_photo);
      _histogram.updateCurvesFilter(_photo);

      if (photoImage == null || photoImage.isDisposed()) {

         updateUI_LoadingMessage();

      } else {

         _pageBook.showPage(_pagePhoto);
      }

      enableControls();
   }

   private void updateActions() {

      // set spinner which should be reset
      _actionReset3Point_Dark.spinner = _spinnerLevel_Dark;
      _actionReset3Point_Bright.spinner = _spinnerLevel_Bright;
      _actionReset3Point_MiddleX.spinner = _spinnerLevel_MiddleX;
      _actionReset3Point_MiddleY.spinner = _spinnerLevel_MiddleY;
   }

   public void updateCropArea(final Rectangle2D.Float histogramCropArea) {

      _histogram.updateCropArea(histogramCropArea);
   }

   /**
    * Update cursor only when it was modified
    *
    * @param cursor
    */
   private void updateCursor(final Cursor cursor) {

//      if (_currentCursor != cursor) {
//
//         _currentCursor = cursor;
//
//         _photoImageCanvas.setCursor(cursor);
//      }
   }

   public void updateCurves() {

      _histogram.updateCurvesFilter(_photo);
   }

   private void updateModelAndUI() {

      _photo.isAdjustmentModified = true;

      updateTourPhotoInDB(_photo);

      // show/hide reset buttons
      updateUI_3PointActions();

      _histogram.redraw();
   }

   /**
    * Update tour photo in the db and fire an modify event
    *
    * @param photo
    */
   private void updateTourPhotoInDB(final Photo photo) {

      final String sql = UI.EMPTY_STRING

            + "UPDATE " + TourDatabase.TABLE_TOUR_PHOTO + NL //$NON-NLS-1$

            + " SET" + NL //                                   //$NON-NLS-1$

            + " photoAdjustmentsJSON = ?  " + NL //            //$NON-NLS-1$

            + " WHERE photoId = ?         " + NL //            //$NON-NLS-1$
      ;

      try (final Connection conn = TourDatabase.getInstance().getConnection();
            final PreparedStatement sqlUpdate = conn.prepareStatement(sql)) {

         final ArrayList<Photo> updatedPhotos = new ArrayList<>();

         final Collection<TourPhotoReference> photoRefs = photo.getTourPhotoReferences().values();

         if (photoRefs.size() > 0) {

            for (final TourPhotoReference photoRef : photoRefs) {

               TourPhoto dbTourPhoto = null;

               /*
                * Update tour photo
                */
               final TourData tourData = TourManager.getInstance().getTourData(photoRef.tourId);
               final Set<TourPhoto> allTourPhotos = tourData.getTourPhotos();

               for (final TourPhoto tourPhoto : allTourPhotos) {

                  if (tourPhoto.getPhotoId() == photoRef.photoId) {

                     dbTourPhoto = tourPhoto;

                     /*
                      * Set photo adjustments from the photo into the tour photo
                      */

                     final PhotoAdjustments photoAdjustments = tourPhoto.getPhotoAdjustments(true);

                     photoAdjustments.isSetTonality = photo.isSetTonality;
                     photoAdjustments.curveType = photo.curveType;
                     photoAdjustments.threePoint_Dark = photo.threePoint_Dark;
                     photoAdjustments.threePoint_MiddleX = photo.threePoint_MiddleX;
                     photoAdjustments.threePoint_MiddleY = photo.threePoint_MiddleY;
                     photoAdjustments.threePoint_Bright = photo.threePoint_Bright;

                     break;
                  }
               }

               /*
                * Update db
                */
               if (dbTourPhoto != null) {

                  // update json
                  dbTourPhoto.updateAllPhotoAdjustments();

                  final String photoAdjustmentsJSON = dbTourPhoto.getPhotoAdjustmentsJSON();

                  sqlUpdate.setString(1, photoAdjustmentsJSON);
                  sqlUpdate.setLong(2, photoRef.photoId);

                  sqlUpdate.executeUpdate();
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

   private void updateUI_3PointActions() {

      final boolean isDarkDefaultValue = _photo.threePoint_Dark == THREE_POINT_DEFAULT_DARK;
      final boolean isBrightDefaultValue = _photo.threePoint_Bright == THREE_POINT_DEFAULT_BRIGHT;
      final boolean isMiddleXDefaultValue = _photo.threePoint_MiddleX == THREE_POINT_DEFAULT_MIDDLE;
      final boolean isMiddleYDefaultValue = _photo.threePoint_MiddleY == THREE_POINT_DEFAULT_MIDDLE;

      _toolbarReset3Point_Dark.setVisible(isDarkDefaultValue == false);
      _toolbarReset3Point_Bright.setVisible(isBrightDefaultValue == false);
      _toolbarReset3Point_MiddleX.setVisible(isMiddleXDefaultValue == false);
      _toolbarReset3Point_MiddleY.setVisible(isMiddleYDefaultValue == false);
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

   private void validate3Points(final Widget widget) {

      int dark = _spinnerLevel_Dark.getSelection();
      int bright = _spinnerLevel_Bright.getSelection();
      final int middleX = _spinnerLevel_MiddleX.getSelection();
      final int middleY = _spinnerLevel_MiddleY.getSelection();

      if (widget == _spinnerLevel_Dark) {

         if (dark >= bright) {

            dark = bright - 1;

            _spinnerLevel_Dark.setSelection(dark);
         }
      }

      if (widget == _spinnerLevel_Bright) {

         if (bright <= dark) {

            bright = dark + 1;

            _spinnerLevel_Bright.setSelection(bright);
         }
      }

      _photo.threePoint_Dark = dark;
      _photo.threePoint_Bright = bright;
      _photo.threePoint_MiddleX = middleX / 10f;
      _photo.threePoint_MiddleY = middleY / 10f;
   }

}
