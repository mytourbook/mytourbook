/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
import de.byteholder.geoclipse.map.Map2Painter;
import de.byteholder.geoclipse.map.MapLegend;
import de.byteholder.geoclipse.map.PaintedMapLocation;
import de.byteholder.geoclipse.mapprovider.MP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.ui.FormattedWord;
import net.tourbook.common.ui.TextWrapPainter;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.map2.Messages;
import net.tourbook.tour.location.TourLocationExtended;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class DirectMappingPainter implements IDirectPainter {

   private Map2                       _map;
   private TourData                   _tourData;

   private int                        _leftSliderValueIndex;
   private int                        _rightSliderValueIndex;
   private int                        _externalValuePointIndex;

   private boolean                    _isTourVisible;
   private boolean                    _isShowSliderInMap;
   private boolean                    _isShowSliderInLegend;
   private boolean                    _isShowValuePoint;

   private boolean                    _isShowMapLocation;
   private boolean                    _isShowMapLocations_BoundingBox;
   private boolean                    _isShowLocations_Address;
   private boolean                    _isShowLocations_Tour;

   private List<TourLocationExtended> _allAddressLocations;
   private List<TourLocationExtended> _allTourLocations;
   private int                        _mapLocationLineHeight    = MTFont.getTitleFontHeight() + 3;

   private SliderPathPaintingData     _sliderPathPaintingData;

   private Map<Long, Color>           _locationColors           = new HashMap<>();
   private int                        _colorSwitchCounter;

   private boolean                    _isMapBackgroundDark;

   private Rectangle                  _imageMapLocationBounds;

   private TextWrapPainter            _textWrapPainter;

   private Color                      _nameColor_Dark           = new Color(222, 255, 134);
   private Color                      _nameColor_Dark_Shadow    = new Color(0, 0, 0);
   private Color                      _nameColor_Dark_Hovered   = new Color(255, 255, 255);
   private Color                      _nameColor_Bright         = new Color(41, 54, 0);
   private Color                      _nameColor_Bright_Shadow  = new Color(255, 255, 255);
   private Color                      _nameColor_Bright_Hovered = new Color(137, 173, 39);

   /*
    * UI resources
    */
   private final Image _imageMapLocation;
   private final Image _imageMapLocation_Address;
   private final Image _imageMapLocation_Start;
   private final Image _imageMapLocation_End;
   private final Image _imageMapLocation_Hovered;
   private final Image _imageSlider_Left;
   private final Image _imageSlider_Right;
   private final Image _imageValuePoint;

   /**
    * @param map
    * @param state
    *
    */
   public DirectMappingPainter(final Map2 map) {

      _map = map;

// SET_FORMATTING_OFF

      _imageMapLocation          = TourbookPlugin.getImageDescriptor(Images.MapLocationMarker).createImage();
      _imageMapLocation_Address  = TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Address).createImage();
      _imageMapLocation_Start    = TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Start).createImage();
      _imageMapLocation_End      = TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_End).createImage();
      _imageMapLocation_Hovered  = TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Hovered).createImage();

      _imageSlider_Left          = TourbookPlugin.getImageDescriptor(Messages.Image_Map_MarkerSliderLeft).createImage();
      _imageSlider_Right         = TourbookPlugin.getImageDescriptor(Messages.Image_Map_MarkerSliderRight).createImage();
      _imageValuePoint           = TourbookPlugin.getImageDescriptor(Images.Map_ValuePoint).createImage();

// SET_FORMATTING_ON

      _imageMapLocationBounds = _imageMapLocation_Hovered.getBounds();

      _textWrapPainter = new TextWrapPainter();
   }

   /**
    * Convert from latitude/longitude to device pixel
    *
    * @param mp
    * @param latitude
    * @param longitude
    * @param zoomLevel
    *
    * @return
    */
   private Point convertGeoPoint(final MP mp, final double latitude, final double longitude, final int zoomLevel) {

      // get world position for the lat/lon coordinates

      final GeoPosition geoPosition = new GeoPosition(latitude, longitude);

      final java.awt.Point locationPixelAWT = mp.geoToPixel(geoPosition, zoomLevel);

      // convert awt to swt point
      return new Point(locationPixelAWT.x, locationPixelAWT.y);
   }

   private Color createBBoxColor() {

      int red = (int) (Math.random() * 255);
      int green = (int) (Math.random() * 255);
      int blue = (int) (Math.random() * 255);

      final float[] hsbValues = java.awt.Color.RGBtoHSB(red, green, blue, null);

      final float hue = hsbValues[0];
      final float saturation = hsbValues[1];
      float brightness = hsbValues[2];

      int adjustedRGB = Integer.MIN_VALUE;

      final float brightnessClipValue = 0.5f;
      final float darknessClipValue = 0.6f;

      if (_isMapBackgroundDark) {

         // background is dark -> ensure that a bright color is used

         if (brightness < brightnessClipValue) {

            brightness = brightnessClipValue;

            adjustedRGB = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
         }

      } else {

         // background is bright -> ensure that a darker color is used

         if (brightness > darknessClipValue) {

            brightness = darknessClipValue;

            adjustedRGB = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
         }
      }

      if (adjustedRGB != Integer.MIN_VALUE) {

         // brightness is adjusted

         final java.awt.Color adjustedColor = new java.awt.Color(adjustedRGB);

         red = adjustedColor.getRed();
         green = adjustedColor.getBlue();
         blue = adjustedColor.getBlue();
      }

      final Color locationColor = new Color(red, green, blue);

      return locationColor;
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

      disposeImage(_imageMapLocation);
      disposeImage(_imageMapLocation_Address);
      disposeImage(_imageMapLocation_Start);
      disposeImage(_imageMapLocation_End);
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

   private void drawMapLocation(final DirectPainterContext painterContext,
                                final List<TourLocationExtended> allTourLocations) {

      final MP mp = _map.getMapProvider();
      final int zoomLevel = _map.getZoom();

      final GC gc = painterContext.gc;
      final Rectangle mapViewport = painterContext.mapViewport;
      final int viewportX = mapViewport.x;
      final int viewportY = mapViewport.y;

      gc.setAntialias(SWT.ON);
      gc.setLineWidth(2);
      gc.setFont(MTFont.getTitleFont());

      final Color textColor = _isMapBackgroundDark ? _nameColor_Dark : _nameColor_Bright;
      final Color hoveredColor = _isMapBackgroundDark ? _nameColor_Dark_Hovered : _nameColor_Bright_Hovered;
      final Color shadowColor = _isMapBackgroundDark ? _nameColor_Dark_Shadow : _nameColor_Bright_Shadow;

      // use different colors each time
      if (_colorSwitchCounter++ % 50 == 0) {
         _locationColors.clear();
      }

      // setup hovered map location
      final PaintedMapLocation hoveredMapLocation = painterContext.hoveredMapLocation;
      Rectangle hoveredLocation = null;
      Rectangle hoveredTextLocation = null;
      List<FormattedWord> hoveredTexts = null;
      int hoveredIconX = 0;
      int hoveredIconY = 0;

      final int imageWidth = _imageMapLocationBounds.width;
      final int imageHeight = _imageMapLocationBounds.height;
      final int imageWidth2 = imageWidth / 2;

      for (final TourLocationExtended tourLocationExtended : allTourLocations) {

         final TourLocation tourLocation = tourLocationExtended.tourLocation;

         final Point requestedLocation = convertGeoPoint(mp, tourLocation.latitude, tourLocation.longitude, zoomLevel);

         final double latitudeMin_Resized = tourLocation.latitudeMin_Resized;
         final double latitudeMax_Resized = tourLocation.latitudeMax_Resized;
         final double longitudeMin_Resized = tourLocation.longitudeMin_Resized;
         final double longitudeMax_Resized = tourLocation.longitudeMax_Resized;

         final Point providedBBox_TopLeft_Resized = convertGeoPoint(mp, latitudeMin_Resized, longitudeMin_Resized, zoomLevel);
         final Point providedBBox_TopRight_Resized = convertGeoPoint(mp, latitudeMin_Resized, longitudeMax_Resized, zoomLevel);
         final Point providedBBox_BottomLeft_Resized = convertGeoPoint(mp, latitudeMax_Resized, longitudeMin_Resized, zoomLevel);
         final Point providedBBox_BottomRight_Resized = convertGeoPoint(mp, latitudeMax_Resized, longitudeMax_Resized, zoomLevel);

         // check if location is visible
         if (mapViewport.contains(requestedLocation)

               || mapViewport.contains(providedBBox_TopLeft_Resized)
               || mapViewport.contains(providedBBox_TopRight_Resized)
               || mapViewport.contains(providedBBox_BottomLeft_Resized)
               || mapViewport.contains(providedBBox_BottomRight_Resized)

         ) {

            // convert world position into device position
            final int requestedDevX = requestedLocation.x - viewportX;
            final int requestedDevY = requestedLocation.y - viewportY;

            if (_isShowMapLocations_BoundingBox) {

               /*
                * Paint each bbox with a different color but use the same color for the same bbox
                */
               final long bboxKey = tourLocation.boundingBoxKey;

               Color locationColor = _locationColors.get(bboxKey);

               if (locationColor == null) {

                  // create bbox color

                  locationColor = createBBoxColor();

                  _locationColors.put(bboxKey, locationColor);
               }

               gc.setForeground(locationColor);
               gc.setBackground(locationColor);

               // draw original bbox

               final double latitudeMin = tourLocation.latitudeMin;
               final double latitudeMax = tourLocation.latitudeMax;
               final double longitudeMin = tourLocation.longitudeMin;
               final double longitudeMax = tourLocation.longitudeMax;

               final Point providedBBox_TopLeft = convertGeoPoint(mp, latitudeMin, longitudeMin, zoomLevel);
               final Point providedBBox_TopRight = convertGeoPoint(mp, latitudeMin, longitudeMax, zoomLevel);
               final Point providedBBox_BottomLeft = convertGeoPoint(mp, latitudeMax, longitudeMin, zoomLevel);

               final int bboxTopLeft_DevX = providedBBox_TopLeft.x - viewportX;
               final int bboxTopRight_DevX = providedBBox_TopRight.x - viewportX;

               final int bboxTopLeft_DevY = providedBBox_TopLeft.y - viewportY;
               final int bboxBottomLeft_DevY = providedBBox_BottomLeft.y - viewportY;

               final int bboxWidth = bboxTopRight_DevX - bboxTopLeft_DevX;
               final int bboxHeight = bboxBottomLeft_DevY - bboxTopLeft_DevY;

               gc.drawRectangle(

                     bboxTopLeft_DevX,
                     bboxTopLeft_DevY,
                     bboxWidth,
                     bboxHeight

               );

               final boolean isBBoxResized = false

                     || latitudeMin != latitudeMin_Resized
                     || latitudeMax != latitudeMax_Resized

                     || longitudeMin != longitudeMin_Resized
                     || longitudeMax != longitudeMax_Resized;

               if (isBBoxResized) {

                  // draw resized bbox

                  final int bboxTopLeft_DevX_Resized = providedBBox_TopLeft_Resized.x - viewportX;
                  final int bboxTopRight_DevX_Resized = providedBBox_TopRight_Resized.x - viewportX;
                  final int bboxTopLeft_DevY_Resized = providedBBox_TopLeft_Resized.y - viewportY;
                  final int bboxBottomLeft_DevY_Resized = providedBBox_BottomLeft_Resized.y - viewportY;

                  final int bboxWidth_Resized = bboxTopRight_DevX_Resized - bboxTopLeft_DevX_Resized;
                  final int bboxHeight_Resized = bboxBottomLeft_DevY_Resized - bboxTopLeft_DevY_Resized;

                  gc.drawRectangle(

                        bboxTopLeft_DevX_Resized,
                        bboxTopLeft_DevY_Resized,
                        bboxWidth_Resized,
                        bboxHeight_Resized

                  );
               }
            }

            final int iconDevX = requestedDevX - imageWidth2;
            final int iconDevY = requestedDevY - imageHeight;

            final Rectangle paintedRectangle = new Rectangle(

                  iconDevX,
                  iconDevY,

                  imageWidth,
                  imageHeight);

            // draw location image
            switch (tourLocationExtended.locationType) {

            case Common   -> gc.drawImage(_imageMapLocation_Address, iconDevX, iconDevY);
            case TourStart -> gc.drawImage(_imageMapLocation_Start, iconDevX, iconDevY);
            case TourEnd   -> gc.drawImage(_imageMapLocation_End, iconDevX, iconDevY);

            default        -> gc.drawImage(_imageMapLocation, iconDevX, iconDevY);
            }

            // check if location name is formatted
            if (tourLocationExtended.allFormattedLocationNameWords == null) {

               setupMapLocationName(gc, tourLocationExtended);

               // reset clipping
               gc.setClipping((Rectangle) null);
            }

            Rectangle paintedTopLeftPos = null;

            final List<FormattedWord> allFormattedLocationNameWords = tourLocationExtended.allFormattedLocationNameWords;
            if (allFormattedLocationNameWords.size() > 0) {

               final Point locationNameBoundingBox = tourLocationExtended.locationNameBoundingBox;

               paintedTopLeftPos = _textWrapPainter.drawPreformattedText(

                     gc,

                     iconDevX,
                     iconDevY,

                     tourLocationExtended.allFormattedLocationNameWords,
                     locationNameBoundingBox,

                     _imageMapLocationBounds,
                     _mapLocationLineHeight,

                     textColor,
                     shadowColor

               );

               paintedRectangle.add(paintedTopLeftPos);
            }

            // keep location for mouse actions
            painterContext.allPaintedMapLocations.add(new PaintedMapLocation(tourLocationExtended, paintedRectangle));

            if (hoveredMapLocation != null && paintedRectangle.equals(hoveredMapLocation.locationRectangle)) {

               // map location is hovered

               hoveredLocation = paintedRectangle;
               hoveredTextLocation = paintedTopLeftPos;
               hoveredIconX = iconDevX;
               hoveredIconY = iconDevY;
               hoveredTexts = tourLocationExtended.allFormattedLocationNameWords;
            }
         }
      }

      // draw hovered location icon + text
      if (hoveredLocation != null) {

         gc.drawImage(_imageMapLocation_Hovered, hoveredIconX, hoveredIconY);

         if (hoveredTextLocation != null) {

            _textWrapPainter.drawFormattedText(gc,
                  hoveredTextLocation.x,
                  hoveredTextLocation.y,
                  hoveredTexts,
                  hoveredColor);
         }
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
   private boolean drawMarkerImage(final DirectPainterContext painterContext,
                                   final int sliderValueIndex,
                                   final Image markerImage,
                                   final boolean isYPosCenter) {

      final MP mp = _map.getMapProvider();
      final int zoomLevel = _map.getZoom();

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

      final MP mp = _map.getMapProvider();
      final int zoomLevel = _map.getZoom();

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

      final MP mp = _map.getMapProvider();
      final int zoomLevel = _map.getZoom();

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

      final MapLegend mapLegend = _map.getLegend();

      if (mapLegend == null) {
         return;
      }

      final Image legendImage = mapLegend.getImage();
      if ((legendImage == null) || legendImage.isDisposed()) {
         return;
      }

      final List<Map2Painter> allMapPainter = _map.getMapPainter();
      if (allMapPainter == null || allMapPainter.isEmpty()) {
         return;
      }

      // get first tour painter
      TourMapPainter tourPainter = null;
      for (final Map2Painter mapPainter : allMapPainter) {
         if (mapPainter instanceof TourMapPainter) {
            tourPainter = (TourMapPainter) mapPainter;
            break;
         }
      }
      if (tourPainter == null) {
         return;
      }

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

      if (_map == null) {
         return;
      }

      if (_isShowMapLocation) {

         painterContext.allPaintedMapLocations.clear();

         // show tour locations
         if (_isShowLocations_Tour) {

            if (_allTourLocations != null && _allTourLocations.size() > 0) {

               drawMapLocation(painterContext, _allTourLocations);
            }
         }

         // show address locations
         if (_isShowLocations_Address) {

            if (_allAddressLocations != null && _allAddressLocations.size() > 0) {

               drawMapLocation(painterContext, _allAddressLocations);
            }
         }
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

         drawMarkerImage(painterContext, _rightSliderValueIndex, _imageSlider_Right, false);
         drawMarkerImage(painterContext, _leftSliderValueIndex, _imageSlider_Left, false);
      }

      if (_isShowValuePoint

            // check if value point is valid -> do not show invalid point
            && _externalValuePointIndex != -1) {

         drawMarkerImage(painterContext, _externalValuePointIndex, _imageValuePoint, true);
      }

      if (_isShowSliderInLegend) {
         drawValueMarkerInLegend(painterContext);
      }
   }

   public void setAddressLocations(final List<TourLocationExtended> allAddressLocations) {

      _allAddressLocations = allAddressLocations;
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

   public void setPaintingOptions_2(final boolean isShowMapLocations,
                                    final boolean isShowMapLocations_BBox,

                                    final boolean isShowAddressLocations,
                                    final boolean isShowTourLocations,

                                    final boolean isMapBackgroundDark) {
// SET_FORMATTING_OFF

      _isShowMapLocation               = isShowMapLocations;
      _isShowMapLocations_BoundingBox  = isShowMapLocations_BBox;

      _isShowLocations_Address         = isShowAddressLocations;
      _isShowLocations_Tour            = isShowTourLocations;

      _isMapBackgroundDark             = isMapBackgroundDark;

// SET_FORMATTING_ON
   }

   public void setTourLocations(final List<TourLocationExtended> allLocations) {

      _allTourLocations = allLocations;
   }

   private void setupMapLocationName(final GC gc, final TourLocationExtended tourLocationExtended) {

      final TourLocation tourLocation = tourLocationExtended.tourLocation;

      final int numLines = 3;
      final int maxLineWidth = 130;

      final int viewportWidth = UI.IS_4K_DISPLAY ? maxLineWidth * 2 : maxLineWidth;
      final int viewportHeight = _mapLocationLineHeight * numLines;

      final List<FormattedWord> allFormattedWords = _textWrapPainter.formatText(

            gc,
            tourLocation.getMapName(),

            viewportWidth,
            viewportHeight,

            _mapLocationLineHeight,
            null, //             overlapRect

            true, //             isTruncateText
            numLines);

      final int numWords = allFormattedWords.size();

      if (numWords == 0) {

         tourLocationExtended.allFormattedLocationNameWords = allFormattedWords;
         tourLocationExtended.locationNameBoundingBox = null;

         return;
      }

      int boundingBoxWidth = 0;
      int boundingBoxHeight = 0;

      /*
       * Get bounding box for the location name words
       */
      for (final FormattedWord formattedWord : allFormattedWords) {

         final int wordXRight = formattedWord.devX + formattedWord.wordWidth;
         final int wordYBottom = formattedWord.devY + _mapLocationLineHeight;

         if (wordXRight > boundingBoxWidth) {

            boundingBoxWidth = wordXRight;
         }

         if (wordYBottom > boundingBoxHeight) {
            boundingBoxHeight = wordYBottom;
         }
      }

      /*
       * Get line width for each line
       */
      final int maxLines = allFormattedWords.get(numWords - 1).line;
      final int[] lineTextWidth = new int[maxLines + 1];

      int currentLine = 0;
      int formattedLineWidth = 0;

      for (final FormattedWord formattedWord : allFormattedWords) {

         if (currentLine == formattedWord.line) {

            formattedLineWidth += formattedWord.wordWidth;

         } else {

            // setup next line
            lineTextWidth[currentLine] = formattedLineWidth;

            currentLine = formattedWord.line;

            formattedLineWidth = formattedWord.wordWidth;
         }
      }

      lineTextWidth[currentLine] = formattedLineWidth;

      /*
       * Set line offset into each word, this offset is used to paint the text right aligned
       */
      for (final FormattedWord formattedWord : allFormattedWords) {

         currentLine = formattedWord.line;

         final int lineOffset = boundingBoxWidth - lineTextWidth[currentLine];

         formattedWord.lineHorizontalOffset = lineOffset;
      }

      tourLocationExtended.allFormattedLocationNameWords = allFormattedWords;
      tourLocationExtended.locationNameBoundingBox = new Point(boundingBoxWidth, boundingBoxHeight);
   }
}
