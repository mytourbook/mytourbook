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

import java.util.Arrays;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;

public class TourMerger {

   private TourData _sourceTour;
   private TourData _targetTour;

   private boolean  mergeAltitude;
   private boolean  mergeCadence;
   private boolean  mergePulse;
   private boolean  mergeSpeed;
   private boolean  mergeTemperature;
   private boolean  adjustAltiFromStart;
   private boolean  adjustAltiFromSource;
   private boolean  adjustAltiSmoothly;
   private float[]  _backupTargetPulseSerie;
   private int[]    _backupTargetTimeSerie;
   private float[]  _backupSourceSpeedSerie;
   private float[]  _backupTargetTemperatureSerie;
   private float[]  _backupTargetCadenceSerie;
   private int      _tourChart_LeftSliderValueIndex;
   private boolean  _synchStartTime;
   private int      _tourStartTimeSynchOffset;

   public TourMerger(final TourData sourceTour, final TourData targetTour) {

      _sourceTour = sourceTour;
      _targetTour = targetTour;

   }

   private void assignMergedSeries(final float[] newSourceAltitudeSerie,
                                   final float[] newSourceAltiDiffSerie,
                                   final float[] newTargetPulseSerie,
                                   final float[] newTargetTemperatureSerie,
                                   final float[] newTargetCadenceSerie,
                                   final int[] targetTimeSerie) {

      // check if the data series are available
      final boolean isTargetAltitude = _targetTour.altitudeSerie != null;
      final boolean isSourceAltitude = _sourceTour.altitudeSerie != null;

      _sourceTour.dataSerieAdjustedAlti = null;

      if (isSourceAltitude) {
         _sourceTour.dataSerie2ndAlti = newSourceAltitudeSerie;
      } else {
         _sourceTour.dataSerie2ndAlti = null;
      }

      if (isSourceAltitude && isTargetAltitude) {
         _sourceTour.dataSerieDiffTo2ndAlti = newSourceAltiDiffSerie;
      } else {
         _sourceTour.dataSerieDiffTo2ndAlti = null;
      }

      if (mergePulse) {
         _targetTour.pulseSerie = newTargetPulseSerie;
      } else {
         _targetTour.pulseSerie = _backupTargetPulseSerie;
      }

      if (mergeSpeed) {
         _targetTour.timeSerie = targetTimeSerie;
         _targetTour.setSpeedSerie(null);
      } else {
         _targetTour.timeSerie = _backupTargetTimeSerie;
         _targetTour.setSpeedSerie(_backupSourceSpeedSerie);
      }

      if (mergeTemperature) {
         _targetTour.temperatureSerie = newTargetTemperatureSerie;
      } else {
         _targetTour.temperatureSerie = _backupTargetTemperatureSerie;
      }

      if (mergeCadence) {
         _targetTour.setCadenceSerie(newTargetCadenceSerie);
      } else {
         _targetTour.setCadenceSerie(_backupTargetCadenceSerie);
      }
   }

   private void assignTargetSeriesValue(final float[] newTargetPulseSerie,
                                        final float[] newTargetTemperatureSerie,
                                        final float[] newTargetCadenceSerie,
                                        final int sourceIndex,
                                        final int targetIndex) {

      final float[] sourceCadenceSerie = _sourceTour.getCadenceSerie();
      final float[] sourcePulseSerie = _sourceTour.pulseSerie;
      final float[] sourceTemperatureSerie = _sourceTour.temperatureSerie;
      // check if the data series are available
      final boolean isSourceCadence = sourceCadenceSerie != null;
      final boolean isSourcePulse = sourcePulseSerie != null;
      final boolean isSourceTemperature = sourceTemperatureSerie != null;

      if (isSourceCadence) {
         newTargetCadenceSerie[targetIndex] = sourceCadenceSerie[sourceIndex];
      }
      if (isSourcePulse) {
         newTargetPulseSerie[targetIndex] = sourcePulseSerie[sourceIndex];
      }
      if (isSourceTemperature) {
         newTargetTemperatureSerie[targetIndex] = sourceTemperatureSerie[sourceIndex];
      }
   }

   private int checkArrayBounds(int sourceIndex, final int lastSourceIndex) {

      sourceIndex = (sourceIndex <= lastSourceIndex) ? sourceIndex : lastSourceIndex;
      return sourceIndex;
   }

   private float[] computeAdjustedAltitude(final int[] targetTimeSerie,
                                           final float[] newSourceAltitudeSerie,
                                           final float[] newSourceAltiDiffSerie) {

      final float[] targetDistanceSerie = _targetTour.distanceSerie;

      final boolean isSourceAltitude = _sourceTour.altitudeSerie != null;
      final boolean isTargetAltitude = _targetTour.altitudeSerie != null;
      final boolean isTargetDistance = targetDistanceSerie != null;

      final float[] altitudeDifferences = new float[2];
      if (!isSourceAltitude || !isTargetAltitude || !isTargetDistance) {
         return altitudeDifferences;
      }

      final int serieLength = targetTimeSerie.length;

      if (adjustAltiFromStart) {

         /*
          * adjust start altitude until left slider
          */

         final float[] adjustedTargetAltitudeSerie = new float[serieLength];

         float startAltiDiff = newSourceAltiDiffSerie[0];
         final int endIndex = _tourChart_LeftSliderValueIndex;
         final float distanceDiff = targetDistanceSerie[endIndex];

         final float[] altitudeSerie = _targetTour.altitudeSerie;

         for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

            if (serieIndex < endIndex) {

               // add adjusted altitude

               final float targetDistance = targetDistanceSerie[serieIndex];
               final float distanceScale = 1 - targetDistance / distanceDiff;

               final float adjustedAltiDiff = startAltiDiff * distanceScale;
               final float newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

               adjustedTargetAltitudeSerie[serieIndex] = newAltitude;
               newSourceAltiDiffSerie[serieIndex] = newSourceAltitudeSerie[serieIndex] - newAltitude;

            } else {

               // add altitude which are not adjusted

               adjustedTargetAltitudeSerie[serieIndex] = altitudeSerie[serieIndex];
            }
         }

         _sourceTour.dataSerieAdjustedAlti = adjustedTargetAltitudeSerie;

         startAltiDiff /= UI.UNIT_VALUE_ELEVATION;

         final int targetEndTime = targetTimeSerie[endIndex];
         final float targetEndDistance = targetDistanceSerie[endIndex];

         // meter/min
         altitudeDifferences[0] = targetEndTime == 0 ? //
               0f
               : startAltiDiff / targetEndTime * 60;

         // meter/meter
         altitudeDifferences[1] = targetEndDistance == 0 ? //
               0f
               : ((startAltiDiff * 1000) / targetEndDistance) / UI.UNIT_VALUE_DISTANCE;

      } else if (adjustAltiFromSource) {

         /*
          * adjust target altitude from source altitude
          */
         _sourceTour.dataSerieAdjustedAlti = Arrays.copyOf(newSourceAltitudeSerie, serieLength);
      }

      return altitudeDifferences;
   }

   private void computeMergedData() {

      final int serieLength = _targetTour.timeSerie.length;
      final float[] newSourceAltitudeSerie = new float[serieLength];
      final float[] newSourceAltiDiffSerie = new float[serieLength];

      final float[] newTargetPulseSerie = new float[serieLength];
      final float[] newTargetTemperatureSerie = new float[serieLength];
      final float[] newTargetCadenceSerie = new float[serieLength];

      final int[] targetTimeSerie = mergeSpeed();

      int xMergeOffset = _targetTour.getMergedTourTimeOffset();
      if (_synchStartTime) {

         // synchronize start time
         xMergeOffset = _tourStartTimeSynchOffset;
      }

      createNewTimeAndDistanceSerie(xMergeOffset,
            newSourceAltitudeSerie,
            newSourceAltiDiffSerie,
            newTargetPulseSerie,
            newTargetTemperatureSerie,
            newTargetCadenceSerie,
            targetTimeSerie);

      assignMergedSeries(newSourceAltitudeSerie,
            newSourceAltiDiffSerie,
            newTargetPulseSerie,
            newTargetTemperatureSerie,
            newTargetCadenceSerie,
            targetTimeSerie);

      final float[] altitudeDifferences = computeAdjustedAltitude(targetTimeSerie,
            newSourceAltitudeSerie,
            newSourceAltiDiffSerie);

      //updateUI(altitudeDifferences[0], altitudeDifferences[1]);
   }

   private float computeNewSourceAltitude(final float sourceAlti,
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
               sourceAlti);

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
                                              final float[] newSourceAltitudeSerie,
                                              final float[] newSourceAltiDiffSerie,
                                              final float[] newTargetPulseSerie,
                                              final float[] newTargetTemperatureSerie,
                                              final float[] newTargetCadenceSerie,
                                              final int[] targetTimeSerie) {

      final float[] sourceAltitudeSerie = _sourceTour.altitudeSerie;
      final int[] sourceTimeSerie = _sourceTour.timeSerie;
      final float[] targetAltitudeSerie = _targetTour.altitudeSerie;
      // check if the data series are available
      final boolean isSourceAltitude = sourceAltitudeSerie != null;
      final boolean isTargetAltitude = targetAltitudeSerie != null;

      final int yMergeOffset = _targetTour.getMergedAltitudeOffset();

      float sourceAlti = 0;
      float previousSourceAltitude = 0;
      if (isSourceAltitude) {
         sourceAlti = _sourceTour.altitudeSerie[0] + yMergeOffset;
         previousSourceAltitude = sourceAlti;
      }

      int sourceIndex = 0;
      final int lastSourceIndex = sourceTimeSerie.length - 1;
      int previousSourceTime = 0;
      int sourceTime = sourceTimeSerie[0] + xMergeOffset;
      for (int targetIndex = 0; targetIndex < _targetTour.timeSerie.length; targetIndex++) {

         final int targetTime = targetTimeSerie[targetIndex];

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
               previousSourceAltitude = sourceAlti;
               sourceAlti = sourceAltitudeSerie[sourceIndex] + yMergeOffset;
            }
         }

         if (isSourceAltitude) {

            final float newSourceAltitude = computeNewSourceAltitude(
                  sourceAlti,
                  previousSourceAltitude,
                  previousSourceTime,
                  sourceTime,
                  targetTime);

            newSourceAltitudeSerie[targetIndex] = newSourceAltitude;

            if (isTargetAltitude) {
               newSourceAltiDiffSerie[targetIndex] = newSourceAltitude - targetAltitudeSerie[targetIndex];
            }
         }

         assignTargetSeriesValue(newTargetPulseSerie,
               newTargetTemperatureSerie,
               newTargetCadenceSerie,
               sourceIndex,
               targetIndex);
      }
   }

   private boolean isLinearInterpolation() {

      return adjustAltiFromSource && adjustAltiSmoothly;
   }

   /**
    * Compute a linear interpolation based on the below formula
    * <p>
    * y2 = (x2-x1)(y3-y1)/(x3-x1) + y1
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

      final float[] targetDistanceSerie = _targetTour.distanceSerie;
      final int serieLength = targetTimeSerie.length;

      int sourceTime = sourceTimeSerie[0];
      int previousSourceTime = 0;
      float previousSourceDistance = 0;
      float sourceDistance = 0;
      int sourceIndex = 0;
      final int lastSourceIndex = sourceDistanceSerie.length - 1;

      for (int targetIndex = 0; targetIndex < serieLength; targetIndex++) {

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
