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

import com.garmin.fit.Event;
import com.garmin.fit.EventMesg;
import com.garmin.fit.EventMesgListener;
import com.garmin.fit.EventType;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.data.GearData;
import net.tourbook.device.garmin.fit.FitData;

/**
 * Set gear data
 */
public class MesgListener_Event extends AbstractMesgListener implements EventMesgListener {

   List<GearData>     _gearData;
   private List<Long> _pausedTime_Start = new ArrayList<>();
   private List<Long> _pausedTime_End   = new ArrayList<>();

   public MesgListener_Event(final FitData fitData) {

      super(fitData);

      _gearData = fitData.getGearData();
      _pausedTime_Start = fitData.getPausedTime_Start();
      _pausedTime_End = fitData.getPausedTime_End();

   }

   @SuppressWarnings("incomplete-switch")
   @Override
   public void onMesg(final EventMesg mesg) {

      final Event event = mesg.getEvent();
      final EventType eventType = mesg.getEventType();
      if (event != null && event == Event.TIMER && eventType != null) {

         final int numberOfPausedTime_Start = _pausedTime_Start.size();
         final int numberOfPausedTime_End = _pausedTime_End.size();

         switch (eventType) {

         case START:

            if (numberOfPausedTime_Start == 0) {
               return;
            }

            final long lastPausedTime_Start = _pausedTime_Start.get(numberOfPausedTime_Start - 1);

            final long pausedTime_End = mesg.getTimestamp().getTimestamp() * 1000 + com.garmin.fit.DateTime.OFFSET;
            if (pausedTime_End - lastPausedTime_Start > 1000) {
               _pausedTime_End.add(pausedTime_End);
            }
            break;

         case STOP:
         case STOP_ALL:

            // We need to avoid the cases where stops are consecutive events.
            // In this case, we take the latest and remove the previous one.
            if (numberOfPausedTime_Start > numberOfPausedTime_End) {
               _pausedTime_Start.remove(numberOfPausedTime_Start - 1);
            }
            final long javaTime = mesg.getTimestamp().getTimestamp() * 1000 + com.garmin.fit.DateTime.OFFSET;
            _pausedTime_Start.add(javaTime);
            break;

         //The Garmin usage of START/STOP/STOP_ALL is described here:
         //https://www.thisisant.com/forum/viewthread/4319

         }
      }

      final Long gearChangeData = mesg.getGearChangeData();

      // check if gear data are available, it can be null
      if (gearChangeData != null) {

         // create gear data for the current time
         final GearData gearData = new GearData();

         final com.garmin.fit.DateTime garminTime = mesg.getTimestamp();

         // convert garmin time into java time
         final long garminTimeS = garminTime.getTimestamp();
         final long garminTimeMS = garminTimeS * 1000;
         final long javaTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

         gearData.absoluteTime = javaTime;
         gearData.gears = gearChangeData;

         _gearData.add(gearData);
      }
   }

}
