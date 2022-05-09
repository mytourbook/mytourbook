/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
import de.byteholder.geoclipse.mapprovider.MP;

import java.util.List;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.data.TourData;
import net.tourbook.map2.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class DirectMappingPainter implements IDirectPainter {

   private Map2                   _map;
   private TourData               _tourData;

   private int                    _leftSliderValueIndex;
   private int                    _rightSliderValueIndex;
   private int                    _externalValuePointIndex;

   private boolean                _isTourVisible;
   private boolean                _isShowSliderInMap;
   private boolean                _isShowSliderInLegend;
   private boolean                _isShowValuePoint;

   private SliderPathPaintingData _sliderPathPaintingData;

   private final Image            _imageLeftSlider;
   private final Image            _imageRightSlider;
   private final Image            _imageValuePoint;

   /**
    *
    */
   public DirectMappingPainter() {

      _imageLeftSlider = TourbookPlugin.getImageDescriptor(Messages.Image_Map_MarkerSliderLeft).createImage();
      _imageRightSlider = TourbookPlugin.getImageDescriptor(Messages.Image_Map_MarkerSliderRight).createImage();
      _imageValuePoint = TourbookPlugin.getImageDescriptor(Images.Map_ValuePoint).createImage();
   }

   /**
    * set paint context to draw nothing
    */
   public void disablePaintContext() {

      _map = null;
      _tourData = null;
      _isTourVisible = false;
   }

   @Override
   public void dispose() {

      disposeImage(_imageLeftSlider);
      disposeImage(_imageRightSlider);
      disposeImage(_imageValuePoint);
   }

   private void disposeImage(final Image image) {

      if ((image != null) && !image.isDisposed()) {
         image.dispose();
      }
   }

   /**
    * @param painterContext
    * @param sliderValueIndex
    * @param markerImage
    * @param isYPosCenter
    * @return Returns <code>true</code> when the marker is visible and painted
    */
   private boolean drawMarker(final DirectPainterContext painterContext,
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
      final Rectangle viewport = painterContext.viewport;
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

      final Rectangle viewport = painterContext.viewport;

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

      final Rectangle viewport = painterContext.viewport;

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

      if (_map == null || _tourData == null || _isTourVisible == false || _sliderPathPaintingData == null) {
         return;
      }

      if (_sliderPathPaintingData.isShowSliderPath) {

         // draw it even when the sliders are not visible but the tour can be visible !

         drawSliderPath(painterContext);
      }

      if (_isShowSliderInMap) {
         drawMarker(painterContext, _rightSliderValueIndex, _imageRightSlider, false);
         drawMarker(painterContext, _leftSliderValueIndex, _imageLeftSlider, false);
      }

      if (_isShowValuePoint

            // check if value point is valid -> do not show invalid point
            && _externalValuePointIndex != -1

      ) {

         drawMarker(painterContext, _externalValuePointIndex, _imageValuePoint, true);
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
   public void setPaintContext(final Map2 map,
                               final boolean isTourVisible,
                               final TourData tourData,

                               final int leftSliderValuesIndex,
                               final int rightSliderValuesIndex,
                               final int externalValuePointIndex,

                               final boolean isShowSliderInMap,
                               final boolean isShowSliderInLegend,
                               final boolean isShowValuePoint,

                               final SliderPathPaintingData sliderRelationPaintingData) {

      _map = map;
      _isTourVisible = isTourVisible;
      _tourData = tourData;

      _leftSliderValueIndex = leftSliderValuesIndex;
      _rightSliderValueIndex = rightSliderValuesIndex;
      _externalValuePointIndex = externalValuePointIndex;

      _isShowSliderInMap = isShowSliderInMap;
      _isShowSliderInLegend = isShowSliderInLegend;
      _isShowValuePoint = isShowValuePoint;

      _sliderPathPaintingData = sliderRelationPaintingData;
   }

}
