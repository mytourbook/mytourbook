/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
/**
 * @author Wolfgang Schramm Created: 06.07.2005
 */
package net.tourbook.ui.tourChart;

import java.text.NumberFormat;
import java.util.ArrayList;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

/**
 * This layer displays the average values for a segment.
 */
public class ChartLayerSegmentValue implements IChartLayer {

   private TourChart _tourChart;
   private TourData  _tourData;

   // hide small values
   private boolean _isHideSmallValues;
   private double  _smallValue;

   // show lines
   private boolean                                _isShowSegmenterLine;
   private int                                    _lineOpacity;

   private boolean                                _isShowDecimalPlaces;
   private boolean                                _isShowSegmenterValues;
   private int                                    _stackedValues;
   private double[]                               _xDataSerie;

   /**
    * Area where the graph is painted.
    */
   private ArrayList<Rectangle>                   _allGraphAreas      = new ArrayList<>();

   private ArrayList<ArrayList<SegmenterSegment>> _allPaintedSegments = new ArrayList<>();

   private int                                    _paintedGraphIndex;

   private final NumberFormat                     _nf1                = NumberFormat.getNumberInstance();

   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   public ChartLayerSegmentValue(final TourChart tourChart) {

      _tourChart = tourChart;
   }

   /**
    */
   @Override
   public void draw(final GC gc,
                    final GraphDrawingData graphDrawingData,
                    final Chart chart,
                    final PixelConverter pixelConverter) {

      /**
       * Cleanup hovering data.
       * <p>
       * Remove previous data, this is a bit tricky because the first graph which is painted in this
       * layer can be 1 or 0.
       */
      if (graphDrawingData.graphIndex <= _paintedGraphIndex) {
         _allGraphAreas.clear();
         _allPaintedSegments.clear();
      }
      _paintedGraphIndex = graphDrawingData.graphIndex;

      final ChartDataYSerie yData = graphDrawingData.getYData();

      final Object segmentConfigObject = yData.getCustomData(TourManager.CUSTOM_DATA_SEGMENT_VALUES);
      if ((segmentConfigObject instanceof ConfigGraphSegment) == false) {
         return;
      }

      final ConfigGraphSegment segmentConfig = (ConfigGraphSegment) segmentConfigObject;
      final float[] segmentValues = segmentConfig.segmentDataSerie;

      // check segment values
      if (segmentValues == null) {
         return;
      }

      _tourChart.setLineSelectionDirty();

      final IValueLabelProvider segmentLabelProvider = segmentConfig.labelProvider;

      boolean toggleAboveBelow = false;

      final int graphWidth = graphDrawingData.getChartDrawingData().devVisibleChartWidth;
      final int devYTop = graphDrawingData.getDevYTop();
      final int devYBottom = graphDrawingData.getDevYBottom();
      final long devGraphImageXOffset = chart.getXXDevViewPortLeftBorder();

      final float graphYBottom = graphDrawingData.getGraphYBottom();

      final int valueDivisor = yData.getValueDivisor();
      final double scaleX = graphDrawingData.getScaleX();
      final double scaleY = graphDrawingData.getScaleY();

      final double minValueAdjustment = segmentConfig.minValueAdjustment;
      final double maxValue = yData.getAvgPositiveValue();
      final double hideThreshold = maxValue * _smallValue * minValueAdjustment;

      final int[] segmentSerieIndex = _tourData.segmentSerieIndex;
      final int segmentSerieSize = segmentSerieIndex.length;

      final Long[] multipleTourIds = _tourData.multipleTourIds;
      final int[] multipleTourStartIndex = _tourData.multipleTourStartIndex;
      int tourIndex = 0;
      int nextTourStartIndex = 0;

      final ValueOverlapChecker overlapChecker = new ValueOverlapChecker(_stackedValues);

      int devXPrev = Integer.MIN_VALUE;

      final Display display = Display.getCurrent();

      // setup font for values
      final Font fontBackup = gc.getFont();
      gc.setFont(_tourChart.getValueFont());

      gc.setLineStyle(SWT.LINE_SOLID);

      final Rectangle graphArea = new Rectangle(0, devYTop, graphWidth, devYBottom - devYTop);
      _allGraphAreas.add(graphArea);

      // painted labels for each graph
      final ArrayList<SegmenterSegment> paintedSegment = new ArrayList<>();
      _allPaintedSegments.add(paintedSegment);

      // do not draw over the graph area
      gc.setClipping(graphArea);

      final RGB segmentRGB = segmentConfig.segmentLineRGB;
      final Color segmentColor = new Color(display, segmentRGB);
      gc.setForeground(segmentColor);
      {
         int segmentIndex;

         for (segmentIndex = 0; segmentIndex < segmentSerieSize; segmentIndex++) {

            // get current value
            final int dataSerieIndex = segmentSerieIndex[segmentIndex];
            final int devXSegment = (int) (_xDataSerie[dataSerieIndex] * scaleX - devGraphImageXOffset);
            final int segmentWidth = devXSegment - devXPrev;

            // optimize performance
            if (devXSegment < 0 || devXSegment > graphWidth) {

               // get next value
               if (segmentIndex < segmentSerieSize - 2) {

                  final int serieIndexNext = segmentSerieIndex[segmentIndex + 1];
                  final int devXValueNext = (int) (_xDataSerie[serieIndexNext] * scaleX - devGraphImageXOffset);

                  if (devXValueNext < 0) {

                     // current and next value are outside of the visible area
                     continue;
                  }
               }

               // get previous value
               if (segmentIndex > 0) {

                  final int serieIndexPrev = segmentSerieIndex[segmentIndex - 1];
                  final int devXValuePrev = (int) (_xDataSerie[serieIndexPrev] * scaleX - devGraphImageXOffset);

                  if (devXValuePrev > graphWidth) {

                     // current and previous value are outside of the visible area
                     break;
                  }
               }
            }

            final float graphYValue = segmentValues[segmentIndex] * valueDivisor;
            final int devYGraph = (int) (scaleY * (graphYValue - graphYBottom));
            final int devYSegment = devYBottom - devYGraph;

            boolean isShowValueText = true;

            if (_isHideSmallValues) {

               // check values if they are small enough

               if (graphYValue >= 0) {
                  if (graphYValue < hideThreshold) {
                     isShowValueText = false;
                  }
               } else {

                  // value <0
                  if (-graphYValue < hideThreshold) {
                     isShowValueText = false;
                  }
               }
            }

            /*
             * Get value text
             */
            String valueText;
            if (segmentLabelProvider != null) {

               // get value text from a label provider
               valueText = segmentLabelProvider.getLabel(graphYValue);

            } else {

               if (graphYValue < 0.0 || graphYValue > 0.0) {

                  valueText = _isShowDecimalPlaces //
                        ? _nf1.format(graphYValue)
                        : Integer.toString((int) (graphYValue > 0 //
                              ? (graphYValue + 0.5)
                              : (graphYValue - 0.5)));

               } else {

                  // hide digits
                  valueText = UI.ZERO;
               }
            }

            final SegmenterSegment segmenterSegment = new SegmenterSegment();

            /*
             * Connect two segments with a line
             */
            if (devXPrev == Integer.MIN_VALUE) {

               // first visible segment

               devXPrev = devXSegment;

            } else {

               if (_isShowSegmenterLine /* && isShowValueText */) {

                  gc.setAlpha(_lineOpacity);
                  gc.drawLine(//
                        devXPrev,
                        devYSegment,
                        devXSegment,
                        devYSegment);
               }

               segmenterSegment.paintedX1 = devXPrev;
               segmenterSegment.paintedY1 = devYSegment;

               segmenterSegment.paintedX2 = devXSegment;
               segmenterSegment.paintedY2 = devYSegment;

               segmenterSegment.hoveredLineRect = new Rectangle(//
                     devXPrev,
                     devYSegment - SegmenterSegment.EXPANDED_HOVER_SIZE2,
                     segmentWidth,
                     SegmenterSegment.EXPANDED_HOVER_SIZE);

               segmenterSegment.paintedRGB = segmentRGB;

               if (_isShowSegmenterValues) {

                  final Point textExtent = gc.textExtent(valueText);

                  final int textWidth = textExtent.x;
                  final int textHeight = textExtent.y;

                  final int devXText = devXPrev + segmentWidth / 2 - textWidth / 2;

                  final boolean isValueUp = graphYValue > 0;
                  int devYText;
                  boolean isDrawAbove;

                  if (segmentConfig.canHaveNegativeValues) {

                     isDrawAbove = isValueUp;

                  } else {

                     // toggle above/below segment line
                     isDrawAbove = toggleAboveBelow = !toggleAboveBelow;
                  }

                  if (isDrawAbove) {

                     // draw above segment line
                     devYText = devYSegment - textHeight;

                  } else {

                     // draw below segment line
                     devYText = devYSegment + 2;
                  }

                  final int borderWidth = 5;
                  final int borderWidth2 = 2 * borderWidth;
                  final int borderHeight = 0;
                  final int borderHeight2 = 2 * borderHeight;
                  final int textHeightWithBorder = textHeight + borderHeight2;

                  /*
                   * Ensure the value text do not overlap, if possible :-)
                   */
                  final Rectangle textRect = new Rectangle(//
                        devXText - borderWidth2,
                        devYText - borderHeight,
                        textWidth + borderWidth2,
                        textHeightWithBorder);

                  final Rectangle validRect = overlapChecker.getValidRect(
                        textRect,
                        isValueUp,
                        textHeightWithBorder,
                        valueText);

                  // don't draw over the graph borders
                  if (validRect != null && validRect.y > devYTop && validRect.y + textHeight < devYBottom) {

                     if (isShowValueText) {

                        // keep current valid rectangle
                        overlapChecker.setupNext(validRect, isValueUp);

                        gc.setAlpha(0xff);
                        gc.drawText(//
                              valueText,
                              devXText,
                              validRect.y,
                              true);

                        // ensure that only visible labels can be hovered
                        segmenterSegment.isValueVisible = true;
                     }

                     segmenterSegment.paintedLabel = validRect;

                     // keep area to detect hovered segments, enlarge it with the hover border to easier hit the label
//							final Rectangle hoveredRect = new Rectangle(//
//									(validRect.x + borderWidth),
//									(validRect.y + borderHeight - SegmenterSegment.EXPANDED_HOVER_SIZE2),
//									(validRect.width - borderWidth2 + SegmenterSegment.EXPANDED_HOVER_SIZE),
//									(validRect.height - borderHeight2 + SegmenterSegment.EXPANDED_HOVER_SIZE));

                     final Rectangle hoveredRect = new Rectangle(
                           (validRect.x),
                           (validRect.y + borderHeight - SegmenterSegment.EXPANDED_HOVER_SIZE2),
                           (validRect.width - borderWidth2 + SegmenterSegment.EXPANDED_HOVER_SIZE),
                           (validRect.height - borderHeight2 + SegmenterSegment.EXPANDED_HOVER_SIZE));

                     segmenterSegment.hoveredLabelRect = hoveredRect;

//								/*
//								 * Debugging
//								 */
//								gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
//								gc.drawRectangle(hoveredRect);
                  }
               }
            }

            /*
             * Set slider positions
             */
            final int leftIndex;
            final int prevSegmentIndex = segmentIndex - 1;
            if (prevSegmentIndex < 0) {

               leftIndex = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

            } else {

               final int leftSerieIndex = segmentSerieIndex[prevSegmentIndex];

               leftIndex = leftSerieIndex == 0 ? 0

                     // ensure that the correct time slice is selected in the tour editor
                     : leftSerieIndex + 1;
            }

            segmenterSegment.xSliderSerieIndexLeft = leftIndex;
            segmenterSegment.xSliderSerieIndexRight = dataSerieIndex;

            segmenterSegment.segmentIndex = segmentIndex;

            segmenterSegment.serieIndex = dataSerieIndex;
            segmenterSegment.graphX = _xDataSerie[dataSerieIndex];

            segmenterSegment.devGraphWidth = graphWidth;
            segmenterSegment.devYGraphTop = devYTop;

            /*
             * Get tour id for each segment
             */
            if (_tourData.isMultipleTours()) {

               // multiple tours are displayed

               /*
                * This algorithm is highly complicated because of the complex start values but now
                * it seams to work.
                */
               for (; tourIndex < multipleTourStartIndex.length;) {

                  if (nextTourStartIndex == 0) {

                     // 1st segment

                     nextTourStartIndex = multipleTourStartIndex[1];

                     segmenterSegment.tourId = multipleTourIds[0];
                     break;

                  } else {

                     // 2nd...n segments

                     if (dataSerieIndex <= nextTourStartIndex) {

                        segmenterSegment.tourId = multipleTourIds[tourIndex];
                        break;

                     } else {

                        tourIndex++;

                        if (tourIndex >= multipleTourStartIndex.length - 1) {
                           nextTourStartIndex = Integer.MAX_VALUE;
                        } else {
                           nextTourStartIndex = multipleTourStartIndex[tourIndex + 1];
                        }
                     }
                  }
               }

            } else {

               // a single tour is displayed

               segmenterSegment.tourId = _tourData.getTourId();
            }

            paintedSegment.add(segmenterSegment);

            // advance to the next point
            devXPrev = devXSegment;
         }
      }
      segmentColor.dispose();

      // reset clipping
      gc.setClipping((Rectangle) null);

      // restore font
      gc.setFont(fontBackup);

      gc.setAlpha(0xff);
   }

   ArrayList<Rectangle> getAllGraphAreas() {
      return _allGraphAreas;
   }

   /**
    * @param mouseEvent
    * @return Returns the hovered {@link ChartLabel} or <code>null</code> when a {@link ChartLabel}
    *         is not hovered.
    */
   SegmenterSegment getHoveredSegment(final ChartMouseEvent mouseEvent) {

      for (int graphIndex = 0; graphIndex < _allGraphAreas.size(); graphIndex++) {

         final Rectangle graphArea = _allGraphAreas.get(graphIndex);

         if (graphArea.contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

            // mouse is hovering the graph area

            final SegmenterSegment hoveredSegment = getHoveredSegment_10(
                  graphIndex,
                  mouseEvent.devXMouse,
                  mouseEvent.devYMouse);

            if (hoveredSegment != null) {
               // hovered segment is hit
               return hoveredSegment;
            }
         }
      }

      return null;
   }

   private SegmenterSegment getHoveredSegment_10(final int graphIndex, final int devXMouse, final int devYMouse) {

      final ArrayList<SegmenterSegment> paintedSegments = _allPaintedSegments.get(graphIndex);

      for (final SegmenterSegment paintedSegment : paintedSegments) {

         final Rectangle hoveredLabel = paintedSegment.hoveredLabelRect;
         final Rectangle hoveredRect = paintedSegment.hoveredLineRect;

         if ((hoveredLabel != null && hoveredLabel.contains(devXMouse, devYMouse))
               || (hoveredRect != null && hoveredRect.contains(devXMouse, devYMouse))) {

            // segment is hit
            return paintedSegment;
         }
      }

      return null;
   }

   ArrayList<ArrayList<SegmenterSegment>> getPaintedSegments() {
      return _allPaintedSegments;
   }

   void setIsShowDecimalPlaces(final boolean isShowDecimalPlaces) {
      _isShowDecimalPlaces = isShowDecimalPlaces;
   }

   void setIsShowSegmenterValues(final boolean isShowSegmenterValues) {
      _isShowSegmenterValues = isShowSegmenterValues;
   }

   void setLineProperties(final boolean isShowSegmenterLine, final int lineOpacity) {

      _isShowSegmenterLine = isShowSegmenterLine;
      _lineOpacity = (int) (lineOpacity / 100.0 * 255);
   }

   void setSmallHiddenValuesProperties(final boolean isHideSmallValues, final int smallValue) {

      _isHideSmallValues = isHideSmallValues;
      _smallValue = smallValue / 100.0;
   }

   void setStackedValues(final int stackedValues) {
      _stackedValues = stackedValues;
   }

   void setTourData(final TourData tourData) {

      _tourData = tourData;

      // initialize painted labels
      _allPaintedSegments.clear();
      _allGraphAreas.clear();
   }

   void setXDataSerie(final double[] dataSerie) {
      _xDataSerie = dataSerie;
   }
}
