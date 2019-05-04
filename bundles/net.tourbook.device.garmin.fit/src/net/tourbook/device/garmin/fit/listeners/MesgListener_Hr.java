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
import com.garmin.fit.HrMesg;
import com.garmin.fit.HrMesgListener;

import java.util.List;

import net.tourbook.data.TimeData;
import net.tourbook.device.garmin.fit.FitData;
import net.tourbook.tour.TourLogManager;

/**
 *
 */
public class MesgListener_Hr extends AbstractMesgListener implements HrMesgListener {

   public MesgListener_Hr(final FitData fitData) {
      super(fitData); 
   }

   @Override
   public void onMesg(final HrMesg mesg) {

      onMesg_Hr(mesg);
   }

   /**
    * This is called AFTER the session is closed. Swimming is producing the hr event separately in
    * it's own device.
    *
    * @param mesg
    */
   private void onMesg_Hr(final HrMesg mesg) {

      final DateTime hrTime = mesg.getTimestamp();

//    System.out.println(String.format(""
//
//          + "[%s]"
//
//          + " NumEventTimestamp %-3d"
//          + " NumFilteredBpm %-3d"
//
//          + " Timestamp %-29s"
//          + " timestamp %-15s"
//          + " FractionalTimestamp: %-7.5f"
////           + (" Time256: " + mesg.getTime256())
//
//          + " EventTimestamp %-90s "
//          + " FilteredBpm %-45s "
//
//          + (" NumEventTimestamp12 %-5d")
//          + (" EventTimestamp12 " + Arrays.toString(mesg.getEventTimestamp12())),
//
//          getClass().getSimpleName(),
//
//          mesg.getNumEventTimestamp(),
//          mesg.getNumFilteredBpm(),
//
//          hrTime,
//          hrTime == null ? "" : hrTime.getTimestamp(),
//          mesg.getFractionalTimestamp() == null ? null : mesg.getFractionalTimestamp(),
//
//          Arrays.toString(mesg.getEventTimestamp()),
//          Arrays.toString(mesg.getFilteredBpm()),
//
//          mesg.getNumEventTimestamp12())
////TODO remove SYSTEM.OUT.PRINTLN
//    );

      boolean isTimeAvailable = false;

      final int numEventTimestamp = mesg.getNumEventTimestamp();
      if (numEventTimestamp < 1) {
         return;
      }

      final Float[] allEventTime = mesg.getEventTimestamp();
      final Short[] allFilteredBpm = mesg.getFilteredBpm();

      if (allFilteredBpm.length != allEventTime.length) {

         TourLogManager.logError(String.format("Fit file has different filtered data: EventTimestamp: %d - FilteredBpm: %d", //$NON-NLS-1$
               allEventTime.length,
               allFilteredBpm.length));

         return;
      }

      final List<TimeData> allTimeData = fitData.getAllTimeData();

      /*
       * Get time diff between tour and hr recording. It is complicated because it also contains the
       * garmin time offset. After several try and error processes this algorithm seems to be now
       * correct.
       */
      if (hrTime != null && fitData.getTimeDiffMS() == Long.MIN_VALUE && allEventTime.length > 0) {

         final long firstTourTimeMS = allTimeData.get(0).absoluteTime;
         final long firstHrTimestampMS = hrTime.getDate().getTime();

         final long hr2TourTimeDiffMS = firstTourTimeMS - firstHrTimestampMS;

         final long firstHrTimeSec = Float.valueOf(allEventTime[0]).longValue();
         final long firstHrTimeMS = firstHrTimeSec * 1000;

         fitData.setTimeDiffMS(firstTourTimeMS - firstHrTimeMS - hr2TourTimeDiffMS);
      }

      final long timeDiffMS = fitData.getTimeDiffMS();

      for (int timeStampIndex = 0; timeStampIndex < allEventTime.length; timeStampIndex++) {

         final Float eventTimeStamp = allEventTime[timeStampIndex];
         final Short filteredBpm = allFilteredBpm[timeStampIndex];

         final double sliceGarminTimeS = eventTimeStamp;
         final long sliceGarminTimeMS = (long) (sliceGarminTimeS * 1000);
         final long sliceJavaTime = sliceGarminTimeMS + timeDiffMS;

         // merge HR data into an already existing time data
         for (final TimeData timeData : allTimeData) {

            if (timeData.absoluteTime == sliceJavaTime) {

               timeData.pulse = filteredBpm;
               isTimeAvailable = true;

//             System.out.println(String.format(""
//
//                   + "[%s]"
//
//                   + (" eventTimeStamp %-8.2f   ")
//                   + (" sliceJavaTime %d   ")
//                   + (" localDT %s   ")
//                   + (" bpm %d"),
//
//                   getClass().getSimpleName(),
//
//                   eventTimeStamp,
//                   sliceJavaTime,
//                   new LocalDateTime(sliceJavaTime),
//                   filteredBpm
//
//// TODO remove SYSTEM.OUT.PRINTLN
//             ));

               break;
            }
         }

         if (isTimeAvailable == false) {

            // timeslice is not yet created for this heartrate

//          System.out.println(String.format(""
//
//                + "[%s]"
//
//                + (" eventTimeStamp %-8.2f   ")
//                + (" sliceJavaTime %d   ")
//                + (" localDT %s   ")
//                + (" bpm %d - no timeslice"),
//
//                getClass().getSimpleName(),
//
//                eventTimeStamp,
//                sliceJavaTime,
//                new LocalDateTime(sliceJavaTime),
//                filteredBpm
//
//// TODO remove SYSTEM.OUT.PRINTLN
//          ));
         }
      }
   }
}
