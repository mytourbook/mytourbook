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

import com.jhlabs.image.CurveValues;

import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.PaintedMapPoint;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;
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
import net.tourbook.photo.Histogram;
import net.tourbook.photo.IHistogramListener;
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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.PageBook;

/**
 * Slideout for the 2D map photo tooltip
 */
public class SlideoutMap2_PhotoHistogram extends AdvancedSlideout implements

      IActionResetToDefault,
      IHistogramListener {

   private static final String          ID     = "net.tourbook.map2.view.SlideoutMap2_PhotoHistogram"; //$NON-NLS-1$

   private static final char            NL     = UI.NEW_LINE;

   private final static IDialogSettings _state = TourbookPlugin.getState(ID);

   private Map2                         _map2;

   private PaintedMapPoint              _hoveredMapPoint;
   private PaintedMapPoint              _previousHoveredMapPoint;
   private Photo                        _photo;

   /*
    * UI controls
    */
   private PageBook  _pageBook;
   private PageBook  _pageBookAdjustment;

   private Composite _pageNoPhoto;
   private Composite _pagePhoto;

   private Button    _chkAdjustTonality;

   private Label     _labelMessage;
   private Label     _labelWarning;

   private Histogram _histogram;

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

      super.close();
   }

   private void createActions() {

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
      }

      return container;
   }

   private void createUI_20_Histogram(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1)
            .numColumns(1)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            _pageBookAdjustment = new PageBook(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_pageBookAdjustment);
            {
               /*
                * Adjust curves
                */
               _chkAdjustTonality = new Button(_pageBookAdjustment, SWT.CHECK);
               _chkAdjustTonality.setText(Messages.Slideout_PhotoHistogram_Checkbox_AdjustTonality);
               _chkAdjustTonality.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onAdjustTonality()));
               GridDataFactory.fillDefaults()
                     .align(SWT.FILL, SWT.BEGINNING)
                     .applyTo(_chkAdjustTonality);

            }
            {
               /*
                * Warning
                */
               _labelWarning = new Label(_pageBookAdjustment, SWT.WRAP);
               _labelWarning.setText(Messages.Slideout_PhotoImage_Label_AdjustmentIsDisabled);
               _labelWarning.setToolTipText(Messages.Slideout_PhotoImage_Label_AdjustmentIsDisabled_Tooltip);
            }
         }
         {
            _histogram = new Histogram(container);
            _histogram.addHistogramListener(this);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .hint(SWT.DEFAULT, 100)
                  .applyTo(_histogram);
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


// SET_FORMATTING_ON
   }

   private void fillUI() {

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

//      _keepOpenListener = new FocusListener() {
//
//         @Override
//         public void focusGained(final FocusEvent e) {
//
//            /*
//             * This will fix the problem that when the list of a combobox is displayed, then the
//             * slideout will disappear :-(((
//             */
//            setIsAnotherDialogOpened(true);
//         }
//
//         @Override
//         public void focusLost(final FocusEvent e) {
//            setIsAnotherDialogOpened(false);
//         }
//      };
   }

   private void onAdjustTonality() {

      final boolean isAdjustTonality = _chkAdjustTonality.getSelection();

      _photo.isSetTonality = isAdjustTonality;

      _histogram.updateCurvesFilter(_photo);

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

   @Override
   public void pointIsModified() {

      updateModelAndUI();
   }

   @Override
   public void resetToDefaults() {

   }

   private void restoreState() {

      setShowPhotoAdjustements(_map2.isShowPhotoAdjustments());
   }

   @Override
   protected void saveState() {

      super.saveState();
   }

   public void setShowPhotoAdjustements(final boolean isShowPhotoAdjustments) {

      if (_chkAdjustTonality != null) {

         _pageBookAdjustment.showPage(isShowPhotoAdjustments

               // display checkbox
               ? _chkAdjustTonality

               // display warning
               : _labelWarning);

         _histogram.setEnabled(isShowPhotoAdjustments);
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

      if (Map2PainterConfig.isShowPhotoHistogram == false) {

         // photo histogram should not be displayed

         return;
      }

      final boolean isHistogramVisible = isVisible();

      if (hoveredMapPoint == null) {

         if (isHistogramVisible) {

            if (_hoveredMapPoint != null) {

               // keep previous map point

               _previousHoveredMapPoint = _hoveredMapPoint;
            }

            _hoveredMapPoint = null;

            hide();
         }

         return;
      }

      final boolean isOtherPhoto = _hoveredMapPoint != hoveredMapPoint;

      if (isOtherPhoto && isHistogramVisible) {

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
      updateUI_ShowHide3PointActions();

      final Image photoImage = getPhotoImage(_photo);
      final Float relCropArea = _photo.isCropped ? _photo.getValidCropArea() : null;

      _histogram.setImage(photoImage, relCropArea);
      _histogram.updateCurvesFilter(_photo);

      updateAdjustedImage();
      _histogram.redraw();

      if (photoImage == null || photoImage.isDisposed()) {

         updateUI_LoadingMessage();

      } else {

         _pageBook.showPage(_pagePhoto);
      }

      enableControls();
   }

   private void updateActions() {

   }

   private void updateAdjustedImage() {

      // update histogram with the adjusted image
      BufferedImage adjustedImage = null;

      if (_photo.isSetTonality) {
         adjustedImage = PhotoImageCache.getImage_AWT(_photo, ImageQuality.THUMB_HQ_ADJUSTED);
      }

      _histogram.setAdjustedImage(adjustedImage);
   }

   public void updateCropArea(final Rectangle2D.Float histogramCropArea) {

      if (_histogram == null) {
         return;
      }

      _histogram.updateCropArea(histogramCropArea);
   }

   public void updateCurves() {

      if (_histogram == null) {
         return;
      }

      updateAdjustedImage();

      _histogram.updateCurvesFilter(_photo);
   }

   private void updateModelAndUI() {

      // set flag that the map photo is recomputed
      _photo.isAdjustmentModified = true;

      updateTourPhotoInDB(_photo);

      updateUI_ShowHide3PointActions();

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

                     final CurveValues curveValues = photo.getToneCurvesFilter().getCurves().getActiveCurve().curveValues;

                     final PhotoAdjustments photoAdjustments = tourPhoto.getPhotoAdjustments(true);

                     photoAdjustments.isSetTonality = photo.isSetTonality;

                     photoAdjustments.curveValuesX = curveValues.allValuesX;
                     photoAdjustments.curveValuesY = curveValues.allValuesY;

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

   private void updateUI_LoadingMessage() {

      if (_hoveredMapPoint == null) {

         _labelMessage.setText(UI.EMPTY_STRING);

      } else {

         final Photo photo = _hoveredMapPoint.mapPoint.photo;

         final String photoText = Messages.Slideout_PhotoImage_Label_LoadingMessage + UI.SPACE + photo.imageFilePathName;

         _labelMessage.setText(photoText);
      }

      _pageBook.showPage(_pageNoPhoto);
   }

   private void updateUI_ShowHide3PointActions() {

//      final boolean isDarkDefaultValue = _photo.threePoint_Dark == THREE_POINT_DEFAULT_DARK;
//      final boolean isBrightDefaultValue = _photo.threePoint_Bright == THREE_POINT_DEFAULT_BRIGHT;
//      final boolean isMiddleXDefaultValue = _photo.threePoint_MiddleX == THREE_POINT_DEFAULT_MIDDLE;
//      final boolean isMiddleYDefaultValue = _photo.threePoint_MiddleY == THREE_POINT_DEFAULT_MIDDLE;
//
//      _toolbarReset3Point_Dark.setVisible(isDarkDefaultValue == false);
//      _toolbarReset3Point_Bright.setVisible(isBrightDefaultValue == false);
//      _toolbarReset3Point_MiddleX.setVisible(isMiddleXDefaultValue == false);
//      _toolbarReset3Point_MiddleY.setVisible(isMiddleYDefaultValue == false);
   }

}
