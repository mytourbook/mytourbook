/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
import com.garmin.fit.LapMesg;
import com.garmin.fit.LapMesgListener;
import com.garmin.fit.Mesg;

import java.util.Date;

import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.FitData;

/**
 * A {@link TourMarker} is set for each lap.
 */
public class MesgListener_Lap extends AbstractMesgListener implements LapMesgListener {

   private int   _lapCounter;

   private float _lapDistance;
   private int   _lapTime;

   public MesgListener_Lap(final FitData fitData) {
      super(fitData);
   }

   private Integer getLapMessageIndex(final Mesg mesg) {
      return mesg.getFieldIntegerValue(254);
   }

   @Override
   public void onMesg(final LapMesg lapMesg) {

      fitData.onSetup_Lap_10_Initialize();
      {
         setMarker(lapMesg);
      }
      fitData.onSetup_Lap_20_Finalize();
   }

   private void setMarker(final LapMesg lapMesg) {

      final Integer messageIndex = getLapMessageIndex(lapMesg);
      final TourMarker tourMarker = fitData.getCurrent_TourMarker();

      tourMarker.setLabel(messageIndex == null //
            ? Integer.toString(++_lapCounter)
            : messageIndex.toString());

      float lapDistance = -1;
      final Float totalDistance = lapMesg.getTotalDistance();
      if (totalDistance != null) {

         lapDistance = _lapDistance;
         lapDistance += totalDistance;

         _lapDistance = lapDistance;
         tourMarker.setDistance(lapDistance);
      }

      /*
       * Set lap time, later the time slice position (serie index) will be set.
       */
      final DateTime garminTime = lapMesg.getTimestamp();
      if (garminTime != null) {

         final Date javaTime = garminTime.getDate();

         tourMarker.setDeviceLapTime(javaTime.getTime());

      } else {

         final Float totalElapsedTime = lapMesg.getTotalElapsedTime();
         if (totalElapsedTime != null) {

            int lapTime = -1;

            lapTime = _lapTime;
//          lapTime += Math.round(totalElapsedTime);
            lapTime += totalElapsedTime;

            _lapTime = lapTime;
//
//         // the correct absolute time will be set later
            tourMarker.setTime(lapTime, Long.MIN_VALUE);
         }
      }
   }
}
