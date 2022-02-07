/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.ui.views.rawData;

import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;

public class TourMerger {

   private TourData _sourceTour;
   private TourData _targetTour;

   private boolean  _adjustAltiFromSource;
   private boolean  _adjustAltiSmoothly;
   private boolean  _synchronizeStartTime;
   private int      _tourStartTimeSynchronizeOffset;

   private float[]  _newSourceAltitudeDifferencesSerie;
   private float[]  _newSourceAltitudeSerie;

   private float[]  _newTargetPulseSerie;
   private float[]  _newTargetCadenceSerie;
   private float[]  _newTargetTemperatureSerie;
   private int[]    _newTargetTimeSerie;

   public TourMerger(final TourData sourceTour,
                     final TourData targetTour,
                     final boolean adjustAltiFromSource,
                     final boolean adjustAltiSmoothly,
                     final boolean synchronizeStartTime,
                     final int tourStartTimeSynchronizeOffset) {

      _sourceTour = sourceTour;
      _targetTour = targetTour;
      _adjustAltiFromSource = adjustAltiFromSource;
      _adjustAltiSmoothly = adjustAltiSmoothly;
      _synchronizeStartTime = synchronizeStartTime;
      _tourStartTimeSynchronizeOffset = tourStartTimeSynchronizeOffset;
   }

   private void assignTargetSeriesValue(final int sourceIndex, final int targetIndex) {

      final float[] sourceCadenceSerie = _sourceTour.getCadenceSerie();
      final float[] sourcePulseSerie = _sourceTour.pulseSerie;
      final float[] sourceTemperatureSerie = _sourceTour.temperatureSerie;

      if (sourceCadenceSerie != null) {
         _newTargetCadenceSerie[targetIndex] = sourceCadenceSerie[sourceIndex];
      }
      if (sourcePulseSerie != null) {
         _newTargetPulseSerie[targetIndex] = sourcePulseSerie[sourceIndex];
      }
      if (sourceTemperatureSerie != null) {
         _newTargetTemperatureSerie[targetIndex] = sourceTemperatureSerie[sourceIndex];
      }
   }

   private int checkArrayBounds(int sourceIndex, final int lastSourceIndex) {

      sourceIndex = (sourceIndex <= lastSourceIndex) ? sourceIndex : lastSourceIndex;
      return sourceIndex;
   }

   public void computeMergedData(final boolean mergeSpeed) {

      _newTargetTimeSerie = mergeSpeed ? mergeSpeed() : Util.createIntegerCopy(_targetTour.timeSerie);

      final int serieLength = _newTargetTimeSerie.length;
      _newSourceAltitudeSerie = new float[serieLength];
      _newSourceAltitudeDifferencesSerie = new float[serieLength];

      final boolean isSourceCadence = _sourceTour.getCadenceSerie() != null;
      if (isSourceCadence) {
         _newTargetCadenceSerie = new float[serieLength];
      }
      final boolean isSourcePulse = _sourceTour.pulseSerie != null;
      if (isSourcePulse) {
         _newTargetPulseSerie = new float[serieLength];
      }
      final boolean isSourceTemperature = _sourceTour.temperatureSerie != null;
      if (isSourceTemperature) {
         _newTargetTemperatureSerie = new float[serieLength];
      }

      int xMergeOffset = _targetTour.getMergedTourTimeOffset();
      if (_synchronizeStartTime) {
         // synchronize start time
         xMergeOffset = _tourStartTimeSynchronizeOffset;
      }
      final int yMergeOffset = _targetTour.getMergedAltitudeOffset();

      createNewTimeAndDistanceSerie(xMergeOffset, yMergeOffset);

      if (mergeSpeed) {

         _targetTour.setTourDeviceTime_Elapsed(_newTargetTimeSerie[_newTargetTimeSerie.length - 1] - _newTargetTimeSerie[0] * 1L);
         _targetTour.setTourDeviceTime_Recorded(_targetTour.getTourDeviceTime_Elapsed());
         _targetTour.computeTourMovingTime();
      }
   }

   private float computeNewSourceAltitude(final float sourceAltitude,
                                          final float previousSourceAltitude,
                                          final int previousSourceTime,
                                          final int sourceTime,
                                          final int targetTime) {

      float newSourceAltitude;

      if (isLinearInterpolation()) {

         newSourceAltitude = linearInterpolate(
               previousSourceTime,
               targetTime,
               sourceTime,
               previousSourceAltitude,
               sourceAltitude);

      } else {

         /*
          * the interpolated altitude is not exact above the none interpolate altitude, it is
          * in the middle of the previous and current altitude
          */
         // newSourceAltitude = sourceAlti;
         newSourceAltitude = previousSourceAltitude;
      }

      return newSourceAltitude;
   }

   /**
    * create new time/distance serie for the source tour according to the time of the target tour
    */
   private void createNewTimeAndDistanceSerie(final int xMergeOffset,
                                              final int yMergeOffset) {

      final float[] sourceAltitudeSerie = _sourceTour.altitudeSerie;
      final float[] targetAltitudeSerie = _targetTour.altitudeSerie;
      // check if the data series are available
      final boolean isSourceAltitude = sourceAltitudeSerie != null;
      final boolean isTargetAltitude = targetAltitudeSerie != null;

      float sourceAltitude = 0;
      float previousSourceAltitude = 0;
      if (isSourceAltitude) {
         sourceAltitude = _sourceTour.altitudeSerie[0] + yMergeOffset;
         previousSourceAltitude = sourceAltitude;
      }

      final int[] sourceTimeSerie = _sourceTour.timeSerie;
      final int lastSourceIndex = sourceTimeSerie.length - 1;
      int previousSourceTime = 0;
      int sourceTime = sourceTimeSerie[0] + xMergeOffset;
      int sourceIndex = 0;
      for (int targetIndex = 0; targetIndex < _newTargetTimeSerie.length; targetIndex++) {

         final int targetTime = _newTargetTimeSerie[targetIndex];

         /*
          * target tour is the leading time data serie, move source time forward to reach target
          * time
          */
         while (sourceTime < targetTime) {

            sourceIndex++;

            sourceIndex = checkArrayBounds(sourceIndex, lastSourceIndex);

            if (sourceIndex == lastSourceIndex) {
               //prevent endless loops
               break;
            }

            previousSourceTime = sourceTime;
            sourceTime = sourceTimeSerie[sourceIndex] + xMergeOffset;

            if (isSourceAltitude) {
               previousSourceAltitude = sourceAltitude;
               sourceAltitude = sourceAltitudeSerie[sourceIndex] + yMergeOffset;
            }
         }

         if (isSourceAltitude) {

            final float newSourceAltitude = computeNewSourceAltitude(
                  sourceAltitude,
                  previousSourceAltitude,
                  previousSourceTime,
                  sourceTime,
                  targetTime);

            _newSourceAltitudeSerie[targetIndex] = newSourceAltitude;

            if (isTargetAltitude) {
               _newSourceAltitudeDifferencesSerie[targetIndex] = newSourceAltitude - targetAltitudeSerie[targetIndex];
            }
         }

         assignTargetSeriesValue(sourceIndex, targetIndex);
      }
   }

   public float[] getNewSourceAltitudeDifferencesSerie() {
      return _newSourceAltitudeDifferencesSerie;
   }

   public float[] getNewSourceAltitudeSerie() {
      return _newSourceAltitudeSerie;
   }

   public float[] getNewTargetCadenceSerie() {
      return _newTargetCadenceSerie;
   }

   public float[] getNewTargetPulseSerie() {
      return _newTargetPulseSerie;
   }

   public float[] getNewTargetTemperatureSerie() {
      return _newTargetTemperatureSerie;
   }

   public int[] getNewTargetTimeSerie() {
      return _newTargetTimeSerie;
   }

   private boolean isLinearInterpolation() {

      return _adjustAltiFromSource && _adjustAltiSmoothly;
   }

   /**
    * Compute a linear interpolation based on the below formula
    * <p>
    * y2 = (x2-x1)(y3-y1)/(x3-x1) + y1
    * Note: Apache Commons has an interpolate function but given that it is not
    * used a lot in MyTourbook, it is not worth adding a library for now.
    * (https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/analysis/interpolation/LinearInterpolator.html#interpolate(double[],%20double[]))
    */
   private float linearInterpolate(final float x1,
                                   final float x2,
                                   final float x3,
                                   final float y1,
                                   final float y3) {
      final float xDiff = x3 - x1;

      final float interpolatedValue = xDiff == 0 ? y1 : (x2 - x1) * (y3 - y1) / xDiff + y1;

      return interpolatedValue;
   }

   /**
    * Creates new time serie for the target tour according to the distance of the target tour
    */
   private int[] mergeSpeed() {

      final int[] sourceTimeSerie = _sourceTour.timeSerie;
      final boolean isSourceTime = sourceTimeSerie != null && sourceTimeSerie.length > 0;
      final float[] sourceDistanceSerie = _sourceTour.distanceSerie;
      final boolean isSourceDistance = sourceDistanceSerie != null;
      final int[] targetTimeSerie = _targetTour.timeSerie;

      if (!isSourceTime || !isSourceDistance) {
         return targetTimeSerie;
      }

      int sourceTime = sourceTimeSerie[0];
      int previousSourceTime = 0;
      float previousSourceDistance = 0;
      float sourceDistance = 0;
      int sourceIndex = 0;
      final int lastSourceIndex = sourceDistanceSerie.length - 1;
      final float[] targetDistanceSerie = _targetTour.distanceSerie;

      for (int targetIndex = 0; targetIndex < targetDistanceSerie.length; targetIndex++) {

         final float targetDistance = targetDistanceSerie[targetIndex];

         /*
          * target tour is the leading distance data serie, move source distance
          * forward to reach target distance
          */
         while (sourceDistance < targetDistance) {

            sourceIndex++;

            sourceIndex = checkArrayBounds(sourceIndex, lastSourceIndex);

            if (sourceIndex == lastSourceIndex) {
               //prevent endless loops
               break;
            }

            sourceTime = sourceTimeSerie[sourceIndex];
            sourceDistance = sourceDistanceSerie[sourceIndex];

            previousSourceTime = sourceTimeSerie[sourceIndex - 1];
            previousSourceDistance = sourceDistanceSerie[sourceIndex - 1];
         }

         targetTimeSerie[targetIndex] = Math.round(
               linearInterpolate(previousSourceDistance,
                     targetDistance,
                     sourceDistance,
                     previousSourceTime,
                     sourceTime));
      }

      return targetTimeSerie;
   }
}
