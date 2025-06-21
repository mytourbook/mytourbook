/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

import de.byteholder.geoclipse.map.DirectPainterContext;
import de.byteholder.geoclipse.map.IDirectPainter;
import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.MapLegend;
import de.byteholder.geoclipse.map.PaintedMapPoint;
import de.byteholder.geoclipse.mapprovider.MP;

import java.util.List;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.NoAutoScalingImageDataProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.map2.Messages;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoUI;
import net.tourbook.photo.RatingStars;
import net.tourbook.tour.photo.TourPhotoManager;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class DirectMappingPainter implements IDirectPainter {

   private Map2                   _map2;
   private TourData               _tourData;

   private int                    _leftSliderValueIndex;
   private int                    _rightSliderValueIndex;
   private int                    _externalValuePointIndex;

   private boolean                _isTourVisible;
   private boolean                _isShowSliderInMap;
   private boolean                _isShowSliderInLegend;
   private boolean                _isShowValuePoint;

   private SliderPathPaintingData _sliderPathPaintingData;

   private final ColorRegistry    _colorRegistry        = JFaceResources.getColorRegistry();
   private final Color            _photoBackgroundColor = _colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

   private Rectangle              _imageMapLocationBounds;
   private int                    _ratingStarImageSize;

   /*
    * UI resources
    */
   private final Image _imageMapLocation_Hovered;
   private final Image _imageSlider_Left;
   private final Image _imageSlider_Right;
   private final Image _imageValuePoint;

   private Image       _imageRatingStar;
   private Image       _imageRatingStarAndHovered;
   private Image       _imageRatingStarDelete;
   private Image       _imageRatingStarHovered;
   private Image       _imageRatingStarNotHovered;
   private Image       _imageRatingStarNotHoveredButSet;

   {
      final ImageRegistry imageRegistry = UI.IMAGE_REGISTRY;

// SET_FORMATTING_OFF

      _imageRatingStar                  = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR);
      _imageRatingStarAndHovered        = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_AND_HOVERED);
      _imageRatingStarDelete            = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_DELETE);
      _imageRatingStarHovered           = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_HOVERED);
      _imageRatingStarNotHovered        = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_NOT_HOVERED);
      _imageRatingStarNotHoveredButSet  = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_NOT_HOVERED_BUT_SET);

// SET_FORMATTING_ON

      _ratingStarImageSize = _imageRatingStar.getBounds().width;
   }

   /**
    * @param map2
    * @param state
    *
    */
   public DirectMappingPainter(final Map2 map2) {

      _map2 = map2;

      // get unscaled image
      final ImageDescriptor imageDescriptor = TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Hovered);

// THIS NEEDS TO BE USED WHEN THE LOCATION ICON IS UNSCALES
//      final ImageData imageData = imageDescriptor.getImageData(deviceZoom);

      final ImageData imageData = imageDescriptor.getImageData(100);

      _imageMapLocation_Hovered = new Image(Display.getDefault(), new NoAutoScalingImageDataProvider(imageData));

// SET_FORMATTING_OFF

      _imageSlider_Left          = TourbookPlugin.getImageDescriptor(Messages.Image_Map_MarkerSliderLeft).createImage();
      _imageSlider_Right         = TourbookPlugin.getImageDescriptor(Messages.Image_Map_MarkerSliderRight).createImage();
      _imageValuePoint           = TourbookPlugin.getImageDescriptor(Images.Map_ValuePoint).createImage();

// SET_FORMATTING_ON

      _imageMapLocationBounds = _imageMapLocation_Hovered.getBounds();
   }

   /**
    * set paint context to draw nothing
    */
   public void disablePaintContext() {

      _tourData = null;
      _isTourVisible = false;
   }

   @Override
   public void dispose() {

      disposeImage(_imageMapLocation_Hovered);
      disposeImage(_imageSlider_Left);
      disposeImage(_imageSlider_Right);
      disposeImage(_imageValuePoint);
   }

   private void disposeImage(final Image image) {

      if ((image != null) && !image.isDisposed()) {
         image.dispose();
      }
   }

   private void drawMapPoint_Hovered(final DirectPainterContext painterContext) {

      final GC gc = painterContext.gc;
      final PaintedMapPoint hoveredPoint = _map2.getHoveredMapPoint();

      drawMapPoint_Hovered_LabelItem(gc, hoveredPoint);

      if (Map2PainterConfig.isShowPhotoRating) {
         drawMapPoint_Hovered_RatingStars(gc, hoveredPoint);
      }
   }

   private void drawMapPoint_Hovered_LabelItem(final GC gc, final PaintedMapPoint hoveredPoint) {

      final float deviceScaling = _map2.getDeviceScaling();

      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();
      final Rectangle labelRectangle = hoveredPoint.labelRectangle;

      final int labelDevX_Scaled = (int) (labelRectangle.x / deviceScaling);
      final int labelDevY_Scaled = (int) (labelRectangle.y / deviceScaling);
      final int labelWidth_Scaled = (int) (labelRectangle.width / deviceScaling);
      final int labelHeight_Scaled = (int) (labelRectangle.height / deviceScaling);

      final Map2Point mapPoint = hoveredPoint.mapPoint;
      final MapPointType mapPointType = mapPoint.pointType;

      final int mapPointDevX = mapPoint.geoPointDevX;
      final int mapPointDevY = mapPoint.geoPointDevY;

      final int symbolSize = mapConfig.locationSymbolSize;
      final int symbolSize2 = symbolSize / 2;
      final int lineWidth = (int) (symbolSize / 4 / deviceScaling);
      final int lineWidth2 = (int) (lineWidth / 2f);

      final String markerLabel = mapPoint.getFormattedLabel();

      final int markerSymbolDevX = mapPointDevX - symbolSize2;
      final int markerSymbolDevY = mapPointDevY - symbolSize2;

      final Color lineColor = _map2.isMapBackgroundDark() ? UI.SYS_COLOR_WHITE : UI.SYS_COLOR_BLACK;

      gc.setAntialias(mapConfig.isLabelAntialiased ? SWT.ON : SWT.OFF);

      gc.setForeground(mapPoint.getOutlineColorSWT());
      gc.setBackground(mapPoint.getFillColorSWT());

      /*
       * Draw location bounding box
       */
      if (mapConfig.isShowLocationBoundingBox) {

         // draw original bbox
         final Rectangle boundingBox = mapPoint.boundingBoxSWT;

         if (boundingBox != null) {

            gc.drawRectangle(
                  boundingBox.x - 1,
                  boundingBox.y - 1,
                  boundingBox.width + 2,
                  boundingBox.height + 2

            );
         }

         final Rectangle boundingBox_Resized = mapPoint.boundingBox_ResizedSWT;

         if (boundingBox_Resized != null) {

            // draw resized bbox
            gc.drawRectangle(
                  boundingBox_Resized.x - 1,
                  boundingBox_Resized.y - 1,
                  boundingBox_Resized.width + 2,
                  boundingBox_Resized.height + 2

            );
         }
      }

      /*
       * Draw a line from the marker label to the marker location.
       * Ensure that the line is not crossing the label
       */
      int devX_LineFrom = labelDevX_Scaled;
      int devY_LineFrom = labelDevY_Scaled;
      final int devX_LineTo = (int) (mapPointDevX / deviceScaling);
      final int devY_LineTo = (int) (mapPointDevY / deviceScaling);

      if (devX_LineTo > devX_LineFrom + labelWidth_Scaled) {
         devX_LineFrom += labelWidth_Scaled;
      } else if (devX_LineTo > devX_LineFrom && devX_LineTo < devX_LineFrom + labelWidth_Scaled) {
         devX_LineFrom = devX_LineTo;
      }

      if (devY_LineTo > devY_LineFrom + labelHeight_Scaled) {
         devY_LineFrom += labelHeight_Scaled;
      } else if (devY_LineTo > devY_LineFrom && devY_LineTo < devY_LineFrom + labelHeight_Scaled) {
         devY_LineFrom = devY_LineTo;
      }

      gc.setLineWidth(1);
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawLine(
            devX_LineFrom,
            devY_LineFrom,

            devX_LineTo,
            devY_LineTo);

      gc.setForeground(UI.SYS_COLOR_WHITE);

      int devX_LineFrom2 = devX_LineFrom;
      int devY_LineFrom2 = devY_LineFrom;

      int devX_LineTo2 = devX_LineTo;
      int devY_LineTo2 = devY_LineTo;

      if (devX_LineFrom != devX_LineTo2) {
         devY_LineFrom2 += 1;
         devY_LineTo2 += 1;
      }

      if (devY_LineFrom2 != devY_LineTo2) {
         devX_LineFrom2 += 1;
         devX_LineTo2 += 1;
      }

      gc.drawLine(
            devX_LineFrom2,
            devY_LineFrom2,

            devX_LineTo2,
            devY_LineTo2);

      gc.setForeground(mapPoint.getOutlineColorSWT());

      /*
       * Draw a symbol at the point location
       */
      if (mapPointType.equals(MapPointType.COMMON_LOCATION)
            || mapPointType.equals(MapPointType.TOUR_LOCATION)) {

         // display hovered location image

         final int imageWidth = _imageMapLocationBounds.width;
         final int imageHeight = _imageMapLocationBounds.height;
         final int imageWidth2 = imageWidth / 2;

//         gc.drawImage(_imageMapLocation_Hovered,
//               (int) ((mapPointDevX - imageWidth2) / deviceScaling),
//               (int) ((mapPointDevY - imageHeight) / deviceScaling));

         gc.drawImage(_imageMapLocation_Hovered,
               (int) ((mapPointDevX) / deviceScaling) - imageWidth2,
               (int) ((mapPointDevY) / deviceScaling) - imageHeight);

      } else {

         // draw a symbol

         final Rectangle symbolRectangle = new Rectangle(
               (int) (markerSymbolDevX / deviceScaling),
               (int) (markerSymbolDevY / deviceScaling),
               (int) (symbolSize / deviceScaling),
               (int) (symbolSize / deviceScaling));

         if (mapPointType.equals(MapPointType.TOUR_PHOTO)) {

            final Photo photo = mapPoint.photo;
            final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(photo);

            // set different color for positioned photos
            if (allTourPhotos.size() > 0) {

               final TourPhoto tourPhoto = allTourPhotos.get(0);
               final TourData tourData = tourPhoto.getTourData();
               final Set<Long> tourPhotosWithPositionedGeo = tourData.getTourPhotosWithPositionedGeo();
               final boolean isPositionedPhoto = tourPhotosWithPositionedGeo.contains(tourPhoto.getPhotoId());

               if (isPositionedPhoto) {

                  gc.setForeground(UI.SYS_COLOR_YELLOW);
                  gc.setBackground(UI.SYS_COLOR_RED);
               }
            }

            gc.fillOval(
                  symbolRectangle.x + 1, // fill a larger shape to hide the antialiasing which looks like a light border !!!
                  symbolRectangle.y + 1,
                  symbolRectangle.width - 2,
                  symbolRectangle.height - 2);

            gc.setLineWidth(lineWidth);
            gc.drawOval(
                  symbolRectangle.x + lineWidth2 - 1,
                  symbolRectangle.y + lineWidth2 - 1,
                  symbolRectangle.width - lineWidth + 2,
                  symbolRectangle.height - lineWidth + 2);

         } else {

            gc.fillRectangle(
                  symbolRectangle.x,
                  symbolRectangle.y,
                  symbolRectangle.width,
                  symbolRectangle.height);

            gc.setLineWidth(lineWidth);
            gc.drawRectangle(
                  symbolRectangle.x + lineWidth2 + 0,
                  symbolRectangle.y + lineWidth2 + 0,

                  symbolRectangle.width - lineWidth,
                  symbolRectangle.height - lineWidth

            );
         }
      }

      /*
       * Highlight hovered label/photo
       */

      if (mapPointType.equals(MapPointType.TOUR_PHOTO)) {

         gc.setForeground(lineColor);

         gc.setLineWidth(1);
         gc.drawRectangle(
               labelDevX_Scaled,
               labelDevY_Scaled,
               labelWidth_Scaled,
               labelHeight_Scaled);

      } else {

         // fill label background
         gc.fillRectangle(
               labelDevX_Scaled - Map2.MAP_POINT_BORDER,
               labelDevY_Scaled,
               labelWidth_Scaled + 2 * Map2.MAP_POINT_BORDER,
               labelHeight_Scaled);

         // marker label
         final Font currentFont = gc.getFont();

         gc.setFont(_map2.getLabelFont());
         gc.drawText(

               markerLabel,

               labelDevX_Scaled,
               labelDevY_Scaled

                     // for some fonts, e.g. "Yu Gothic UI Semilight" which looks very similar like "Segoe UI"
                     // this will paint the text at the same position as with AWT
                     + 1,

               true);

         gc.setFont(currentFont);

         // border: horizontal bottom
//         gc.setLineWidth(1);
//         gc.drawLine(
//               labelDevX_Scaled,
//               labelDevY_Scaled + labelHeight_Scaled,
//               labelDevX_Scaled + labelWidth_Scaled,
//               labelDevY_Scaled + labelHeight_Scaled);
      }
   }

   private void drawMapPoint_Hovered_RatingStars(final GC gc, final PaintedMapPoint hoveredPoint) {

      final Photo photo = hoveredPoint.mapPoint.photo;

      if (photo == null

            // this happend when the photo was not yet painted
            || photo.paintedPhoto == null

            // this happend also
            || photo.paintedRatingStars == null

      ) {

         return;
      }

      final float deviceScaling = _map2.getDeviceScaling();

      final Rectangle paintedRatingStars = photo.paintedRatingStars;

      final Rectangle paintedRatingStars_Unscaled = new Rectangle(
            (int) (paintedRatingStars.x / deviceScaling),
            (int) (paintedRatingStars.y / deviceScaling + 2),
            (int) (paintedRatingStars.width / deviceScaling),
            (int) (paintedRatingStars.height / deviceScaling));

      // make the rating stars more visible
      gc.setBackground(_photoBackgroundColor);
      gc.fillRectangle(paintedRatingStars_Unscaled);

      _map2.setPaintedRatingStars(paintedRatingStars);

      final int hoveredStars = photo.hoveredStars;
      final boolean isStarHovered = hoveredStars > 0;

      final int photoDevX = photo.paintedPhoto.x;
      final int photoDevY = photo.paintedPhoto.y;
      final int photoWidth = photo.paintedPhoto.width;

      final int numRatingStars = photo.ratingStars;

      // center ratings stars horizontally in the image
      final int ratingStarsLeftBorder = photoDevX
            + photoWidth / 2
            - _map2.MAX_RATING_STARS_WIDTH / 2;

      final int ratingStarImageSize = (int) (_ratingStarImageSize * deviceScaling);

      for (int starIndex = 0; starIndex < RatingStars.MAX_RATING_STARS; starIndex++) {

         Image starImage;

         if (isStarHovered) {

            if (starIndex < hoveredStars) {

               if (starIndex < numRatingStars) {

                  if (starIndex == numRatingStars - 1) {
                     starImage = _imageRatingStarDelete;
                  } else {
                     starImage = _imageRatingStarAndHovered;
                  }
               } else {
                  starImage = _imageRatingStarHovered;
               }

            } else {
               if (starIndex < numRatingStars) {
                  starImage = _imageRatingStarNotHoveredButSet;
               } else {
                  starImage = _imageRatingStarNotHovered;
               }
            }

         } else {

            if (starIndex < numRatingStars) {
               starImage = _imageRatingStarNotHoveredButSet;
            } else {
               starImage = _imageRatingStarNotHovered;
            }
         }

         // draw stars at the top of the photo

         final int devX = (int) ((ratingStarsLeftBorder + ratingStarImageSize * starIndex) / deviceScaling);
         final int devY = (int) (photoDevY / deviceScaling);

         gc.drawImage(starImage, devX, devY);
      }
   }

   /**
    * @param painterContext
    * @param sliderValueIndex
    * @param markerImage
    * @param isYPosCenter
    *
    * @return Returns <code>true</code> when the marker is visible and painted
    */
   private boolean drawSliderImage(final DirectPainterContext painterContext,
                                   final int sliderValueIndex,
                                   final Image markerImage,
                                   final boolean isYPosCenter) {

      final MP mp = _map2.getMapProvider();
      final int zoomLevel = _map2.getZoomLevel();

      final double[] latitudeSerie = _tourData.latitudeSerie;
      final double[] longitudeSerie = _tourData.longitudeSerie;

      if (latitudeSerie == null) {
         return false;
      }

      // force array bounds
      final int sliderValueIndexAdjusted = Math.min(Math.max(sliderValueIndex, 0), latitudeSerie.length - 1);

      // get world position for the slider coordinates

      final java.awt.Point worldPixelMarkerAWT = mp.geoToPixel(new GeoPosition(
            latitudeSerie[sliderValueIndexAdjusted],
            longitudeSerie[sliderValueIndexAdjusted]), zoomLevel);

      // convert awt to swt point
      final Point worldPixelMarker = new Point(worldPixelMarkerAWT.x, worldPixelMarkerAWT.y);

      // check if slider is visible
      final Rectangle viewport = painterContext.mapViewport;
      if (viewport.contains(worldPixelMarker)) {

         // convert world position into device position
         final int devMarkerPosX = worldPixelMarker.x - viewport.x;
         final int devMarkerPosY = worldPixelMarker.y - viewport.y;

         // get marker size
         final Rectangle bounds = markerImage.getBounds();
         final int markerWidth = bounds.width;
         final int markerWidth2 = markerWidth / 2;
         final int markerHeight = bounds.height;

         final int devX = devMarkerPosX - markerWidth2;
         final int devY = isYPosCenter

               // set y centered
               ? devMarkerPosY - markerHeight / 2

               // set y bottom
               : devMarkerPosY - markerHeight;

         // draw marker for the slider/value point
         painterContext.gc.drawImage(markerImage, devX, devY);

         return true;
      }

      return false;
   }

   private void drawSliderPath(final DirectPainterContext painterContext) {

      // draw marker for the slider
      final GC gc = painterContext.gc;

      // set alpha when requested
      int alpha = 0xff;
      final int opacity = _sliderPathPaintingData.opacity;
      if (opacity <= 0xff) {
         alpha = opacity;
      }

      final Color lineColor = new Color(gc.getDevice(), _sliderPathPaintingData.color);

      gc.setLineWidth(_sliderPathPaintingData.lineWidth);
      gc.setLineCap(SWT.CAP_ROUND);
      gc.setLineJoin(SWT.JOIN_ROUND);

      gc.setForeground(lineColor);

      gc.setAlpha(alpha);
      gc.setAntialias(SWT.ON);
      {
         if (_tourData.isMultipleTours() && _tourData.multipleTourStartIndex.length > 1) {

            drawSliderPath_Multiple(gc, painterContext);

         } else {

            drawSliderPath_One(gc, painterContext);
         }
      }
      gc.setAntialias(SWT.OFF);
      gc.setAlpha(0xff);

      lineColor.dispose();
   }

   private void drawSliderPath_Multiple(final GC gc, final DirectPainterContext painterContext) {

      final MP mp = _map2.getMapProvider();
      final int zoomLevel = _map2.getZoomLevel();

      final double[] latitudeSerie = _tourData.latitudeSerie;
      final double[] longitudeSerie = _tourData.longitudeSerie;

      if (latitudeSerie == null || latitudeSerie.length == 0) {
         return;
      }

      // force array bounds
      final int leftSliderValueIndex = Math.min(Math.max(_leftSliderValueIndex, 0), latitudeSerie.length - 1);
      final int rightSliderValueIndex = Math.min(Math.max(_rightSliderValueIndex, 0), latitudeSerie.length - 1);

      int firstSliderValueIndex;
      int lastSliderValueIndex;
      if (leftSliderValueIndex > rightSliderValueIndex) {
         firstSliderValueIndex = rightSliderValueIndex;
         lastSliderValueIndex = leftSliderValueIndex;
      } else {
         firstSliderValueIndex = leftSliderValueIndex;
         lastSliderValueIndex = rightSliderValueIndex;
      }

      final Rectangle viewport = painterContext.mapViewport;

      final int numMaxSegments = _sliderPathPaintingData.segments;

      final int[] allTourStartIndex = _tourData.multipleTourStartIndex;
      final int numTours = allTourStartIndex.length;

      for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

         // prepare next tour
         int firstTourValueIndex = allTourStartIndex[tourIndex];

         if (firstTourValueIndex > lastSliderValueIndex) {

            // first tour value index is after the last slider value index -> out of scope

            return;
         }

         if (firstTourValueIndex < firstSliderValueIndex) {

            // first tour value index is before the first slider value index -> move to first slider value index

            firstTourValueIndex = firstSliderValueIndex;
         }

         int lastTourValueIndex;
         if (tourIndex == numTours - 1) {
            lastTourValueIndex = lastSliderValueIndex;
         } else {
            lastTourValueIndex = allTourStartIndex[tourIndex + 1] - 1;
         }

         if (lastTourValueIndex < firstSliderValueIndex) {

            // last tour value index is before the first slider value index -> out of scope

            continue;
         }

         if (lastTourValueIndex > lastSliderValueIndex) {

            // last tour value index is after the slider value index -> move to last slider value index

            lastTourValueIndex = lastSliderValueIndex;
         }

         final float numSlices = lastTourValueIndex - firstTourValueIndex;
         final int numSegments = (int) Math.min(numMaxSegments, numSlices);

         // get world position for the slider coordinates
         final java.awt.Point wpLeftSliderAWT = mp.geoToPixel(new GeoPosition(
               latitudeSerie[firstTourValueIndex],
               longitudeSerie[firstTourValueIndex]), zoomLevel);

         // convert awt to swt point
         final Point wpLeftSlider = new Point(wpLeftSliderAWT.x, wpLeftSliderAWT.y);

         // convert world position into device position
         int devPosX1 = wpLeftSlider.x - viewport.x;
         int devPosY1 = wpLeftSlider.y - viewport.y;

         final int[] devXY = new int[numSegments * 2 + 2];

         devXY[0] = devPosX1;
         devXY[1] = devPosY1;

         for (int segmentIndex = 1; segmentIndex <= numSegments; segmentIndex++) {

            int nextValueIndex = (int) (numSlices / numSegments * segmentIndex);
            nextValueIndex += firstTourValueIndex;

            // get world position for the slider coordinates
            final java.awt.Point wpRightSliderAWT = mp.geoToPixel(

                  new GeoPosition(
                        latitudeSerie[nextValueIndex],
                        longitudeSerie[nextValueIndex]),

                  zoomLevel);

            // convert awt to swt point
            final Point wpRightSlider = new Point(wpRightSliderAWT.x, wpRightSliderAWT.y);

            // convert world position into device position
            final int devPosX2 = wpRightSlider.x - viewport.x;
            final int devPosY2 = wpRightSlider.y - viewport.y;

            final int devXYIndex = segmentIndex * 2;

            devXY[devXYIndex + 0] = devPosX2;
            devXY[devXYIndex + 1] = devPosY2;

            // advance to the next segment
            devPosX1 = devPosX2;
            devPosY1 = devPosY2;
         }

         gc.drawPolyline(devXY);

         if (tourIndex >= numTours - 1) {
            break;
         }
      }
   }

   private void drawSliderPath_One(final GC gc, final DirectPainterContext painterContext) {

      final MP mp = _map2.getMapProvider();
      final int zoomLevel = _map2.getZoomLevel();

      final double[] latitudeSerie = _tourData.latitudeSerie;
      final double[] longitudeSerie = _tourData.longitudeSerie;

      if (latitudeSerie == null || latitudeSerie.length == 0) {
         return;
      }

      // force array bounds
      final int leftSliderValueIndex = Math.min(Math.max(_leftSliderValueIndex, 0), latitudeSerie.length - 1);
      final int rightSliderValueIndex = Math.min(Math.max(_rightSliderValueIndex, 0), latitudeSerie.length - 1);

      int firstValueIndex;
      int lastValueIndex;
      if (leftSliderValueIndex > rightSliderValueIndex) {
         firstValueIndex = rightSliderValueIndex;
         lastValueIndex = leftSliderValueIndex;
      } else {
         firstValueIndex = leftSliderValueIndex;
         lastValueIndex = rightSliderValueIndex;
      }

      final int[] devXY;

      final int numMaxSegments = _sliderPathPaintingData.segments;
      final float numSlices = lastValueIndex - firstValueIndex;
      final int numSegments = (int) Math.min(numMaxSegments, numSlices);

      final Rectangle viewport = painterContext.mapViewport;

      // get world position for the slider coordinates
      final java.awt.Point wpLeftSliderAWT = mp.geoToPixel(new GeoPosition(
            latitudeSerie[firstValueIndex],
            longitudeSerie[firstValueIndex]), zoomLevel);

      // convert awt to swt point
      final Point wpLeftSlider = new Point(wpLeftSliderAWT.x, wpLeftSliderAWT.y);

      // convert world position into device position
      int devPosX1 = wpLeftSlider.x - viewport.x;
      int devPosY1 = wpLeftSlider.y - viewport.y;

      devXY = new int[numSegments * 2 + 2];

      devXY[0] = devPosX1;
      devXY[1] = devPosY1;

      for (int segmentIndex = 1; segmentIndex <= numSegments; segmentIndex++) {

         int nextValueIndex = (int) (numSlices / numSegments * segmentIndex);
         nextValueIndex += firstValueIndex;

         // get world position for the slider coordinates
         final java.awt.Point wpRightSliderAWT = mp.geoToPixel(new GeoPosition(
               latitudeSerie[nextValueIndex],
               longitudeSerie[nextValueIndex]), zoomLevel);

         // convert awt to swt point
         final Point wpRightSlider = new Point(wpRightSliderAWT.x, wpRightSliderAWT.y);

         // convert world position into device position
         final int devPosX2 = wpRightSlider.x - viewport.x;
         final int devPosY2 = wpRightSlider.y - viewport.y;

         final int devXYIndex = segmentIndex * 2;

         devXY[devXYIndex + 0] = devPosX2;
         devXY[devXYIndex + 1] = devPosY2;

         // advance to the next segment
         devPosX1 = devPosX2;
         devPosY1 = devPosY2;
      }

      gc.drawPolyline(devXY);
   }

   private void drawValueMarkerInLegend(final DirectPainterContext painterContext) {

      final MapLegend mapLegend = _map2.getLegend();

      if (mapLegend == null) {
         return;
      }

      final Image legendImage = mapLegend.getImage();
      if ((legendImage == null) || legendImage.isDisposed()) {
         return;
      }

      final TourMapPainter tourPainter = _map2.getMapPainter();

      final Rectangle legendImageBounds = legendImage.getBounds();

      final int leftValueInlegendPosition = tourPainter.getLegendValuePosition(
            ColorProviderConfig.MAP2,
            legendImageBounds,
            _leftSliderValueIndex);

      if (leftValueInlegendPosition == Integer.MIN_VALUE) {
         return;
      }

      final int rightValueInlegendPosition = tourPainter.getLegendValuePosition(
            ColorProviderConfig.MAP2,
            legendImageBounds,
            _rightSliderValueIndex);

      if (rightValueInlegendPosition == Integer.MIN_VALUE) {
         return;
      }

      final Point legendInMapPosition = mapLegend.getLegendPosition();
      if (legendInMapPosition == null) {
         return;
      }

      final int positionX = legendInMapPosition.x;
      final int positionY = legendInMapPosition.y + legendImageBounds.height - 2;

      final GC gc = painterContext.gc;

      gc.setLineWidth(2);
      gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

      int linePositionY = positionY - leftValueInlegendPosition;
      gc.drawLine(positionX + 1, linePositionY, positionX + 20, linePositionY);

      linePositionY = positionY - rightValueInlegendPosition;
      gc.drawLine(positionX + 1, linePositionY, positionX + 20, linePositionY);
   }

   @Override
   public void paint(final DirectPainterContext painterContext) {

      if (_map2 == null) {
         return;
      }

      if (_map2.getHoveredMapPoint() != null) {
         drawMapPoint_Hovered(painterContext);
      }

      if (_tourData == null
            || _isTourVisible == false
            || _sliderPathPaintingData == null) {

         return;
      }

      if (_sliderPathPaintingData.isShowSliderPath) {

         // draw slider path even when the sliders are not visible but the tour can be visible !

         drawSliderPath(painterContext);
      }

      if (_isShowSliderInMap) {

         drawSliderImage(painterContext, _rightSliderValueIndex, _imageSlider_Right, false);
         drawSliderImage(painterContext, _leftSliderValueIndex, _imageSlider_Left, false);
      }

      if (_isShowValuePoint

            // check if value point is valid -> do not show invalid point
            && _externalValuePointIndex != -1) {

         drawSliderImage(painterContext, _externalValuePointIndex, _imageValuePoint, true);
      }

      if (_isShowSliderInLegend) {
         drawValueMarkerInLegend(painterContext);
      }
   }

   /**
    * @param map
    * @param isTourVisible
    *           <code>true</code> when tour is visible
    * @param tourData
    * @param leftSliderValuesIndex
    * @param rightSliderValuesIndex
    * @param externalValuePointIndex
    *           When <code>-1</code> then this value point is not displayed, this happens when a
    *           trackpoint is hovered because it is irritating when the external value point has
    *           another position
    * @param isShowSliderInMap
    * @param isShowSliderInLegend
    * @param isShowValuePoint
    * @param sliderRelationPaintingData
    */
   public void setPaintingOptions(final boolean isTourVisible,
                                  final TourData tourData,

                                  final int leftSliderValuesIndex,
                                  final int rightSliderValuesIndex,
                                  final int externalValuePointIndex,

                                  final boolean isShowSliderInMap,
                                  final boolean isShowSliderInLegend,
                                  final boolean isShowValuePoint,

                                  final SliderPathPaintingData sliderRelationPaintingData) {
// SET_FORMATTING_OFF

      _isTourVisible             = isTourVisible;
      _tourData                  = tourData;

      _leftSliderValueIndex      = leftSliderValuesIndex;
      _rightSliderValueIndex     = rightSliderValuesIndex;
      _externalValuePointIndex   = externalValuePointIndex;

      _isShowSliderInMap         = isShowSliderInMap;
      _isShowSliderInLegend      = isShowSliderInLegend;
      _isShowValuePoint          = isShowValuePoint;

      _sliderPathPaintingData    = sliderRelationPaintingData;

// SET_FORMATTING_ON
   }

}
