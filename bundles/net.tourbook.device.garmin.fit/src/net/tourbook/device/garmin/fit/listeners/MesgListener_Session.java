/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.DateTime;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;
import com.garmin.fit.Sport;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.FitData;
import net.tourbook.device.garmin.fit.FitDataReaderException;

public class MesgListener_Session extends AbstractMesgListener implements SessionMesgListener {

   public MesgListener_Session(final FitData fitData) {
      super(fitData);
   }

   @Override
   public void onMesg(final SessionMesg mesg) {

      fitData.setSessionIndex(mesg);

      final DateTime startTime = mesg.getStartTime();
      if (startTime == null) {
         throw new FitDataReaderException("Missing session start date"); //$NON-NLS-1$
      }

      final TourData tourData = fitData.getTourData();

// since FIT SDK > 12 the tour start time is different with the records, therefore the tour start time is set later
//
// !!!!
//      This problem is corrected in FIT SDK 14.10 but it took me several days to investigate it
//    and then came the idea to check for a new FIT SDK which solved this problem.
// !!!!

      final ZonedDateTime tourStartTime = TimeTools.getZonedDateTime(startTime.getDate().getTime());

      fitData.setSessionStartTime(tourStartTime);
      tourData.setTourStartTime(tourStartTime);

      final Sport sport = mesg.getSport();
      if (sport != null) {
         final String sportName = sport.name().trim();
         tourData.setDeviceModeName(sportName);
         fitData.setSportname(sportName);
      }

      final Short avgHeartRate = mesg.getAvgHeartRate();
      if (avgHeartRate != null) {
         tourData.setAvgPulse(avgHeartRate);
      }

      final Short avgCadence = mesg.getAvgCadence();
      if (avgCadence != null) {
         tourData.setAvgCadence(avgCadence);
      }

      final Integer totalCalories = mesg.getTotalCalories();
      if (totalCalories != null) {

         // convert kcal -> cal
         tourData.setCalories(totalCalories * 1000);
      }

      final Float totalDistance = mesg.getTotalDistance();
      if (totalDistance != null) {
         tourData.setTourDistance(Math.round(totalDistance));
      }

      final Integer totalAscent = mesg.getTotalAscent();
      if (totalAscent != null) {
         tourData.setTourAltUp(totalAscent);
      }

      final Integer totalDescent = mesg.getTotalDescent();
      if (totalDescent != null) {
         tourData.setTourAltDown(totalDescent);
      }

      final Float totalElapsedTime = mesg.getTotalElapsedTime();
      if (totalElapsedTime != null) {
         tourData.setTourDeviceTime_Elapsed(Math.round(totalElapsedTime));
      }

      final Float totalTimerTime = mesg.getTotalTimerTime();
      if (totalTimerTime != null) {

         int roundedTotalTimerTime = Math.round(totalTimerTime);
         tourData.setTourComputedTime_Moving(roundedTotalTimerTime);

         final long tourDeviceTimeElapsed = tourData.getTourDeviceTime_Elapsed();
         if (roundedTotalTimerTime > tourDeviceTimeElapsed) {
            roundedTotalTimerTime = (int) tourDeviceTimeElapsed;
         }

         tourData.setTourDeviceTime_Recorded(roundedTotalTimerTime);
         tourData.setTourDeviceTime_Paused(tourData.getTourDeviceTime_Elapsed() - tourData.getTourDeviceTime_Recorded());
      }

      // -----------------------POWER -----------------------
      final Integer avgPower = mesg.getAvgPower();
      if (avgPower != null) {
         tourData.setPower_Avg(avgPower);
      }

      Integer maxPower = mesg.getMaxPower();
      if (maxPower != null) {
         tourData.setPower_Max(maxPower);
      }

      // Looking if the power was retrieved from the developer fields
      if (mesg.getMaxPower() == null && mesg.getAvgPower() == null) {
         final ArrayList<Float> powerDataList = new ArrayList<>();
         for (final TimeData timeData : fitData.getAllTimeData()) {
            if (timeData.power != Float.MIN_VALUE) {
               powerDataList.add(timeData.power);
            }

            if (tourData.getPower_DataSource() == null && timeData.powerDataSource != null) {
               tourData.setPower_DataSource(timeData.powerDataSource);
            }
         }

         if (powerDataList.size() > 0) {

            fitData.isComputeAveragePower = true;

            maxPower = (int) powerDataList.stream().mapToDouble(Float::doubleValue).max().getAsDouble();
            tourData.setPower_Max(maxPower);

            tourData.setIsStrideSensorPresent(true);
         }
      }

      final Integer normalizedPower = mesg.getNormalizedPower();
      if (normalizedPower != null) {
         tourData.setPower_Normalized(normalizedPower);
      }

      final Integer leftRightBalance = mesg.getLeftRightBalance();
      if (leftRightBalance != null) {
         tourData.setPower_PedalLeftRightBalance(leftRightBalance);
      }

      final Float avgLeftTorqueEffectiveness = mesg.getAvgLeftTorqueEffectiveness();
      if (avgLeftTorqueEffectiveness != null) {
         tourData.setPower_AvgLeftTorqueEffectiveness(avgLeftTorqueEffectiveness);
      }

      final Float avgRightTorqueEffectiveness = mesg.getAvgRightTorqueEffectiveness();
      if (avgRightTorqueEffectiveness != null) {
         tourData.setPower_AvgRightTorqueEffectiveness(avgRightTorqueEffectiveness);
      }

      final Float avgLeftPedalSmoothness = mesg.getAvgLeftPedalSmoothness();
      if (avgLeftPedalSmoothness != null) {
         tourData.setPower_AvgLeftPedalSmoothness(avgLeftPedalSmoothness);
      }

      final Float avgRightPedalSmoothness = mesg.getAvgRightPedalSmoothness();
      if (avgRightPedalSmoothness != null) {
         tourData.setPower_AvgRightPedalSmoothness(avgRightPedalSmoothness);
      }

      final Long totalWork = mesg.getTotalWork();
      if (totalWork != null) {
         tourData.setPower_TotalWork(totalWork);
      }

      final Float trainingStressScore = mesg.getTrainingStressScore();
      if (trainingStressScore != null) {
         tourData.setPower_TrainingStressScore(trainingStressScore);
      }

      final Float intensityFactor = mesg.getIntensityFactor();
      if (intensityFactor != null) {
         tourData.setPower_IntensityFactor(intensityFactor);
      }

      final Integer ftp = mesg.getThresholdPower();
      if (ftp != null) {
         tourData.setPower_FTP(ftp);
      }

      // -----------------------TRAINING -----------------------

      final Float totalTrainingEffect = mesg.getTotalTrainingEffect();
      if (totalTrainingEffect != null) {
         tourData.setTraining_TrainingEffect_Aerob(totalTrainingEffect);
      }

      final Float totalAnaerobicTrainingEffect = mesg.getTotalAnaerobicTrainingEffect();
      if (totalAnaerobicTrainingEffect != null) {
         tourData.setTraining_TrainingEffect_Anaerob(totalAnaerobicTrainingEffect);
      }

      fitData.onSetup_Session_20_Finalize();
   }

}
