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

import net.tourbook.data.TourData;

public class TourMerger {

   private TourData _sourceTour;
   private TourData _targetTour;

   private boolean  _mergeCadence;
   private boolean  _mergePulse;
   private boolean  _mergeSpeed;
   private boolean  _mergeTemperature;
   private boolean  _adjustAltiFromStart;
   private boolean  _adjustAltiFromSource;
   private boolean  _adjustAltiSmoothly;
   private int      _tourChart_LeftSliderValueIndex;
   private boolean  _synchStartTime;
   private int      _tourStartTimeSynchOffset;
   private float[]  _newSourceAltitudeSerie;
   private float[]  _newTargetPulseSerie;
   private float[]  _newTargetCadenceSerie;
   private float[]  _newTargetTemperatureSerie;
   private float[]  _newSourceAltiDiffSerie;

   public TourMerger(final TourData sourceTour,
                     final TourData targetTour,
                     final boolean synchStartTime,
                     final int tourStartTimeSynchOffset,
                     final boolean mergeCadence,
                     final boolean mergePulse,
                     final boolean mergeSpeed,
                     final boolean mergeTemperature,
                     final boolean adjustAltiFromStart,
                     final boolean adjustAltiFromSource,
                     final boolean adjustAltiSmoothly,
                     final int tourChart_LeftSliderValueIndex) {

      _sourceTour = sourceTour;
      _targetTour = targetTour;
      _synchStartTime = synchStartTime;
      _mergeCadence = mergeCadence;
      _mergePulse = mergePulse;
      _mergeSpeed = mergeSpeed;
      _mergeTemperature = mergeTemperature;
      _adjustAltiFromStart = adjustAltiFromStart;
      _adjustAltiFromSource = adjustAltiFromSource;
      _adjustAltiSmoothly = adjustAltiSmoothly;
      _tourStartTimeSynchOffset = tourStartTimeSynchOffset;
      _tourChart_LeftSliderValueIndex = tourChart_LeftSliderValueIndex;
   }

//   private void assignMergedSeries(final float[] newSourceAltitudeSerie,
//                                   final float[] newSourceAltiDiffSerie,
//                                   final float[] newTargetPulseSerie,
//                                   final float[] newTargetTemperatureSerie,
//                                   final float[] newTargetCadenceSerie,
//                                   final int[] targetTimeSerie) {
//
//      // check if the data series are available
//      final boolean isTargetAltitude = _targetTour.altitudeSerie != null;
//      final boolean isSourceAltitude = _sourceTour.altitudeSerie != null;
//
//      _sourceTour.dataSerieAdjustedAlti = null;
//
//      if (isSourceAltitude) {
//         _sourceTour.dataSerie2ndAlti = newSourceAltitudeSerie;
//      } else {
//         _sourceTour.dataSerie2ndAlti = null;
//      }
//
//      if (isSourceAltitude && isTargetAltitude) {
//         _sourceTour.dataSerieDiffTo2ndAlti = newSourceAltiDiffSerie;
//      } else {
//         _sourceTour.dataSerieDiffTo2ndAlti = null;
//      }
//
//      if (_mergePulse) {
//         _targetTour.pulseSerie = newTargetPulseSerie;
//      } else {
//         _targetTour.pulseSerie = _sourceTour.pulseSerie;
//      }
//
//      if (_mergeSpeed) {
//         _targetTour.timeSerie = targetTimeSerie;
//         _targetTour.setSpeedSerie(null);
//      } else {
//         _targetTour.timeSerie = _sourceTour.timeSerie;
//         _targetTour.setSpeedSerie(_sourceTour.getSpeedSerie());
//      }
//
//      if (_mergeTemperature) {
//         _targetTour.temperatureSerie = newTargetTemperatureSerie;
//      } else {
//         _targetTour.temperatureSerie = _sourceTour.temperatureSerie;
//      }
//
//      if (_mergeCadence) {
//         _targetTour.setCadenceSerie(newTargetCadenceSerie);
//      } else {
//         _targetTour.setCadenceSerie(_sourceTour.getCadenceSerie());
//      }
//   }

   private void assignTargetSeriesValue(
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
         _newTargetCadenceSerie[targetIndex] = sourceCadenceSerie[sourceIndex];
      }
      if (isSourcePulse) {
         _newTargetPulseSerie[targetIndex] = sourcePulseSerie[sourceIndex];
      }
      if (isSourceTemperature) {
         _newTargetTemperatureSerie[targetIndex] = sourceTemperatureSerie[sourceIndex];
      }
   }

   private int checkArrayBounds(int sourceIndex, final int lastSourceIndex) {

      sourceIndex = (sourceIndex <= lastSourceIndex) ? sourceIndex : lastSourceIndex;
      return sourceIndex;
   }

//   private float[] computeAdjustedAltitude(final int[] targetTimeSerie,
//                                           final float[] newSourceAltitudeSerie,
//                                           final float[] newSourceAltiDiffSerie) {
//
//      final float[] targetDistanceSerie = _targetTour.distanceSerie;
//
//      final boolean isSourceAltitude = _sourceTour.altitudeSerie != null;
//      final boolean isTargetAltitude = _targetTour.altitudeSerie != null;
//      final boolean isTargetDistance = targetDistanceSerie != null;
//
//      final float[] altitudeDifferences = new float[2];
//      if (!isSourceAltitude || !isTargetAltitude || !isTargetDistance) {
//         return altitudeDifferences;
//      }
//
//      final int serieLength = targetTimeSerie.length;
//
//      if (_adjustAltiFromStart) {
//
//         /*
//          * adjust start altitude until left slider
//          */
//
//         final float[] adjustedTargetAltitudeSerie = new float[serieLength];
//
//         float startAltiDiff = newSourceAltiDiffSerie[0];
//         final int endIndex = _tourChart_LeftSliderValueIndex;
//         final float distanceDiff = targetDistanceSerie[endIndex];
//
//         final float[] altitudeSerie = _targetTour.altitudeSerie;
//
//         for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
//
//            if (serieIndex < endIndex) {
//
//               // add adjusted altitude
//
//               final float targetDistance = targetDistanceSerie[serieIndex];
//               final float distanceScale = 1 - targetDistance / distanceDiff;
//
//               final float adjustedAltiDiff = startAltiDiff * distanceScale;
//               final float newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;
//
//               adjustedTargetAltitudeSerie[serieIndex] = newAltitude;
//               newSourceAltiDiffSerie[serieIndex] = newSourceAltitudeSerie[serieIndex] - newAltitude;
//
//            } else {
//
//               // add altitude which are not adjusted
//
//               adjustedTargetAltitudeSerie[serieIndex] = altitudeSerie[serieIndex];
//            }
//         }
//
//         _sourceTour.dataSerieAdjustedAlti = adjustedTargetAltitudeSerie;
//
//         startAltiDiff /= UI.UNIT_VALUE_ELEVATION;
//
//         final int targetEndTime = targetTimeSerie[endIndex];
//         final float targetEndDistance = targetDistanceSerie[endIndex];
//
//         // meter/min
//         altitudeDifferences[0] = targetEndTime == 0 ? //
//               0f
//               : startAltiDiff / targetEndTime * 60;
//
//         // meter/meter
//         altitudeDifferences[1] = targetEndDistance == 0 ? //
//               0f
//               : ((startAltiDiff * 1000) / targetEndDistance) / UI.UNIT_VALUE_DISTANCE;
//
//      } else if (_adjustAltiFromSource) {
//
//         /*
//          * adjust target altitude from source altitude
//          */
//         _sourceTour.dataSerieAdjustedAlti = Arrays.copyOf(newSourceAltitudeSerie, serieLength);
//      }
//
//      return altitudeDifferences;
//   }

   public TourData computeMergedData_NEWWIP() {

      final int serieLength = _targetTour.timeSerie.length;
      _newSourceAltitudeSerie = new float[serieLength];
      _newSourceAltiDiffSerie = new float[serieLength];

      _newTargetPulseSerie = new float[serieLength];
      _newTargetTemperatureSerie = new float[serieLength];
      _newTargetCadenceSerie = new float[serieLength];

      final int[] targetTimeSerie = _mergeSpeed
            ? mergeSpeed()
            : _targetTour.timeSerie;

      int xMergeOffset = _targetTour.getMergedTourTimeOffset();
      if (_synchStartTime) {
         // synchronize start time
         xMergeOffset = _tourStartTimeSynchOffset;
      }

      createNewTimeAndDistanceSerie(xMergeOffset, targetTimeSerie);

      _targetTour.altitudeSerie = _newSourceAltitudeSerie;
      _targetTour.setCadenceSerie(_newTargetCadenceSerie);
      _targetTour.pulseSerie = _newTargetPulseSerie;
      _targetTour.temperatureSerie = _newTargetTemperatureSerie;

      return _targetTour;
   }

//   public float[] computeMergedData_Original() {
//
//      int xMergeOffset = _targetTour.getMergedTourTimeOffset();
//      final int yMergeOffset = _targetTour.getMergedAltitudeOffset();
//
//      final int[] targetTimeSerie = _targetTour.timeSerie;
//      final float[] targetDistanceSerie = _targetTour.distanceSerie;
//      final float[] targetAltitudeSerie = _targetTour.altitudeSerie;
//
//      final int[] sourceTimeSerie = _sourceTour.timeSerie;
//      final float[] sourceAltitudeSerie = _sourceTour.altitudeSerie;
//      final float[] sourcePulseSerie = _sourceTour.pulseSerie;
//      final float[] sourceTemperatureSerie = _sourceTour.temperatureSerie;
//      final float[] sourceCadenceSerie = _sourceTour.getCadenceSerie();
//
//      if (_synchStartTime) {
//
//         // synchronize start time
//
//         xMergeOffset = _tourStartTimeSynchOffset;
//      }
//
//      // check if the data series are available
//      final boolean isSourceTemperature = sourceTemperatureSerie != null;
//      final boolean isSourcePulse = sourcePulseSerie != null;
//      final boolean isSourceCadence = sourceCadenceSerie != null;
//      final boolean isSourceAltitude = sourceAltitudeSerie != null;
//
//      final boolean isTargetDistance = targetDistanceSerie != null;
//      final boolean isTargetAltitude = targetAltitudeSerie != null;
//
//      final int lastSourceIndex = sourceTimeSerie.length - 1;
//      final int serieLength = targetTimeSerie.length;
//
//      final float[] newSourceAltitudeSerie = new float[serieLength];
//      final float[] newSourceAltiDiffSerie = new float[serieLength];
//
//      final float[] newTargetPulseSerie = new float[serieLength];
//      final float[] newTargetTemperatureSerie = new float[serieLength];
//      final float[] newTargetCadenceSerie = new float[serieLength];
//
//      int sourceIndex = 0;
//      int sourceTime = sourceTimeSerie[0] + xMergeOffset;
//      int sourceTimePrev = 0;
//      float sourceAlti = 0;
//      float sourceAltiPrev = 0;
//
//      int targetTime = targetTimeSerie[0];
//      float newSourceAltitude;
//
//      if (isSourceAltitude) {
//         sourceAlti = sourceAltitudeSerie[0] + yMergeOffset;
//         sourceAltiPrev = sourceAlti;
//         newSourceAltitude = sourceAlti;
//      }
//
//      /*
//       * create new time/distance serie for the source tour according to the time of the target tour
//       */
//      for (int targetIndex = 0; targetIndex < serieLength; targetIndex++) {
//
//         targetTime = targetTimeSerie[targetIndex];
//
//         /*
//          * target tour is the leading time data serie, move source time forward to reach target
//          * time
//          */
//         while (sourceTime < targetTime) {
//
//            sourceIndex++;
//
//            // check array bounds
//            sourceIndex = (sourceIndex <= lastSourceIndex) ? sourceIndex : lastSourceIndex;
//
//            if (sourceIndex == lastSourceIndex) {
//               //prevent endless loops
//               break;
//            }
//
//            sourceTimePrev = sourceTime;
//            sourceTime = sourceTimeSerie[sourceIndex] + xMergeOffset;
//
//            if (isSourceAltitude) {
//               sourceAltiPrev = sourceAlti;
//               sourceAlti = sourceAltitudeSerie[sourceIndex] + yMergeOffset;
//            }
//         }
//
//         if (isSourceAltitude) {
//
//            if (isLinearInterpolation()) {
//
//               /**
//                * do linear interpolation for the altitude
//                * <p>
//                * y2 = (x2-x1)(y3-y1)/(x3-x1) + y1
//                */
//               final int x1 = sourceTimePrev;
//               final int x2 = targetTime;
//               final int x3 = sourceTime;
//               final float y1 = sourceAltiPrev;
//               final float y3 = sourceAlti;
//
//               final int xDiff = x3 - x1;
//
//               newSourceAltitude = xDiff == 0 ? sourceAltiPrev : (x2 - x1) * (y3 - y1) / xDiff + y1;
//
//            } else {
//
//               /*
//                * the interpolited altitude is not exact above the none interpolite altitude, it is
//                * in the middle of the previous and current altitude
//                */
//               // newSourceAltitude = sourceAlti;
//               newSourceAltitude = sourceAltiPrev;
//            }
//
//            newSourceAltitudeSerie[targetIndex] = newSourceAltitude;
//
//            if (isTargetAltitude) {
//               newSourceAltiDiffSerie[targetIndex] = newSourceAltitude - targetAltitudeSerie[targetIndex];
//            }
//         }
//
//         if (isSourcePulse) {
//            newTargetPulseSerie[targetIndex] = sourcePulseSerie[sourceIndex];
//         }
//         if (isSourceTemperature) {
//            newTargetTemperatureSerie[targetIndex] = sourceTemperatureSerie[sourceIndex];
//         }
//         if (isSourceCadence) {
//            newTargetCadenceSerie[targetIndex] = sourceCadenceSerie[sourceIndex];
//         }
//      }
//
//      _sourceTour.dataSerieAdjustedAlti = null;
//
//      if (isSourceAltitude) {
//         _sourceTour.dataSerie2ndAlti = newSourceAltitudeSerie;
//      } else {
//         _sourceTour.dataSerie2ndAlti = null;
//      }
//
//      if (isSourceAltitude && isTargetAltitude) {
//         _sourceTour.dataSerieDiffTo2ndAlti = newSourceAltiDiffSerie;
//      } else {
//         _sourceTour.dataSerieDiffTo2ndAlti = null;
//      }
//
//      if (_mergePulse) {
//         _targetTour.pulseSerie = newTargetPulseSerie;
//      } else {
//         _targetTour.pulseSerie = _backupTargetPulseSerie;
//      }
//
//      if (_mergeTemperature) {
//         _targetTour.temperatureSerie = newTargetTemperatureSerie;
//      } else {
//         _targetTour.temperatureSerie = _backupTargetTemperatureSerie;
//      }
//
//      if (_mergeCadence) {
//         _targetTour.setCadenceSerie(newTargetCadenceSerie);
//      } else {
//         _targetTour.setCadenceSerie(_backupTargetCadenceSerie);
//      }
//
//      final float[] altitudeDifferences = new float[2];
//
//      if (isSourceAltitude && isTargetAltitude && isTargetDistance) {
//
//         /*
//          * compute adjusted altitude
//          */
//
//         if (_adjustAltiFromStart) {
//
//            /*
//             * adjust start altitude until left slider
//             */
//
//            final float[] adjustedTargetAltitudeSerie = new float[serieLength];
//
//            float startAltiDiff = newSourceAltiDiffSerie[0];
//            final int endIndex = _tourChart_LeftSliderValueIndex;
//            final float distanceDiff = targetDistanceSerie[endIndex];
//
//            final float[] altitudeSerie = _targetTour.altitudeSerie;
//
//            for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
//
//               if (serieIndex < endIndex) {
//
//                  // add adjusted altitude
//
//                  final float targetDistance = targetDistanceSerie[serieIndex];
//                  final float distanceScale = 1 - targetDistance / distanceDiff;
//
//                  final float adjustedAltiDiff = startAltiDiff * distanceScale;
//                  final float newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;
//
//                  adjustedTargetAltitudeSerie[serieIndex] = newAltitude;
//                  newSourceAltiDiffSerie[serieIndex] = newSourceAltitudeSerie[serieIndex] - newAltitude;
//
//               } else {
//
//                  // add altitude which are not adjusted
//
//                  adjustedTargetAltitudeSerie[serieIndex] = altitudeSerie[serieIndex];
//               }
//            }
//
//            _sourceTour.dataSerieAdjustedAlti = adjustedTargetAltitudeSerie;
//
//            startAltiDiff /= UI.UNIT_VALUE_ELEVATION;
//
//            final int targetEndTime = targetTimeSerie[endIndex];
//            final float targetEndDistance = targetDistanceSerie[endIndex];
//
//            // meter/min
//            altitudeDifferences[0] = targetEndTime == 0 ? //
//                  0f
//                  : startAltiDiff / targetEndTime * 60;
//
//            // meter/meter
//            altitudeDifferences[1] = targetEndDistance == 0 ? //
//                  0f
//                  : ((startAltiDiff * 1000) / targetEndDistance) / UI.UNIT_VALUE_DISTANCE;
//
//         } else if (_adjustAltiFromSource) {
//
//            /*
//             * adjust target altitude from source altitude
//             */
//            _sourceTour.dataSerieAdjustedAlti = Arrays.copyOf(newSourceAltitudeSerie, serieLength);
//         }
//      }
//
//      return altitudeDifferences;
//   }

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
      for (int targetIndex = 0; targetIndex < targetTimeSerie.length; targetIndex++) {

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

            _newSourceAltitudeSerie[targetIndex] = newSourceAltitude;

            if (isTargetAltitude) {
               _newSourceAltiDiffSerie[targetIndex] = newSourceAltitude - targetAltitudeSerie[targetIndex];
            }
         }

         assignTargetSeriesValue(sourceIndex, targetIndex);
      }
   }

   public TourData getMergedTour() {
      return _targetTour;
   }

   public float[] getNewSourceAltiDiffSerie() {
      return _newSourceAltiDiffSerie;
   }

   private boolean isLinearInterpolation() {

      return _adjustAltiFromSource && _adjustAltiSmoothly;
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
      final int serieLength = targetDistanceSerie.length;

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
