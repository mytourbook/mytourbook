/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import com.garmin.fit.HrvMesg;
import com.garmin.fit.HrvMesgListener;

import gnu.trove.list.array.TIntArrayList;

import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.device.garmin.fit.FitData;

/**
 *
 */
public class MesgListener_Hrv extends AbstractMesgListener implements HrvMesgListener {

   public MesgListener_Hrv(final FitData fitData) {
      super(fitData);
   }

   @Override
   public void onMesg(final HrvMesg mesg) {

      onMesg_Hrv(mesg);
   }

   /**
    * @param mesg
    */
   private void onMesg_Hrv(final HrvMesg mesg) {

      final TIntArrayList pulseTime = new TIntArrayList();

      final Float[] allTimes = mesg.getTime();

      for (final Float time : allTimes) {

         // skip invalid values which occurred
         if (time == 65535.0) {
            continue;
         }

         // add pulse time in ms, sec -> ms, time values are not more precise than 1 ms
         pulseTime.add((int) (time * 1_000));
      }

      if (pulseTime.size() > 0) {

         final TimeData timeData = fitData.getLastAdded_TimeData();

         if (timeData != null) {

            if (timeData.pulseTime == null) {

               timeData.pulseTime = pulseTime.toArray();

            } else {

               // append to existing values

               timeData.pulseTime = Util.concatInt(timeData.pulseTime, pulseTime.toArray());
            }
         }
      }
   }
}
