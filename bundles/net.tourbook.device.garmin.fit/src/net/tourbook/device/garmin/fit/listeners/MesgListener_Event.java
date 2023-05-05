/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

   private List<GearData> _gearData;

   private List<Long>     _pausedTime_Start = new ArrayList<>();
   private List<Long>     _pausedTime_End   = new ArrayList<>();
   private List<Long>     _pausedTime_Data  = new ArrayList<>();

   private boolean        _isTimerStopped   = true;

   public MesgListener_Event(final FitData fitData) {

      super(fitData);

      _gearData = fitData.getGearData();

      _pausedTime_Start = fitData.getPausedTime_Start();
      _pausedTime_End = fitData.getPausedTime_End();
      _pausedTime_Data = fitData.getPausedTime_Data();
   }

   private void handleTimerStartEvent(final long javaTime) {

      final int numberOfPausedTime_Start = _pausedTime_Start.size();

      if (numberOfPausedTime_Start == 0) {
         //The timer is started for the first time
         _isTimerStopped = false;
         return;
      }
      // We need to avoid the cases where starts are consecutive events.
      // In this case, we ignore the start event if the timer is already started.
      if (!_isTimerStopped) {
         return;
      }

      final long lastPausedTime_Start = _pausedTime_Start.get(numberOfPausedTime_Start - 1);

      if (javaTime - lastPausedTime_Start >= 1000) {

         _pausedTime_End.add(javaTime);
         _isTimerStopped = false;
      }
   }

   private void handleTimerStopEvents(final long javaTime, final Long eventData) {

      // We need to avoid the cases where stops are consecutive events.
      // In this case, we ignore the stop event if the timer is already stopped.
      if (_isTimerStopped) {
         return;
      }

      _pausedTime_Start.add(javaTime);
      _pausedTime_Data.add(eventData);

      _isTimerStopped = true;
   }

   @Override
   public void onMesg(final EventMesg mesg) {

      /*
       * Time data
       */
      final Event event = mesg.getEvent();
      final EventType eventType = mesg.getEventType();
      final long javaTime = mesg.getTimestamp().getDate().getTime();

      if (event != null && event == Event.TIMER && eventType != null) {

         switch (eventType) {

         // The Garmin usage of START/STOP/STOP_ALL is described here:
         // https://www.thisisant.com/forum/viewthread/4319/#7452

         // Garmin: Elapsed, Timer, and Moving Durations
         // https://developer.garmin.com/fit/cookbook/durations/

         case START:

            handleTimerStartEvent(javaTime);
            break;

         case STOP:
         case STOP_ALL:

            /**
             * eventData == 0: user stop<br>
             * eventData == 1: auto-stop
             */
            final Long eventData = mesg.getData();

            if (eventData != null) {

               handleTimerStopEvents(javaTime, eventData);
            }
            break;

         default:
            break;
         }
      }

      /*
       * Gear data
       */
      final Long gearChangeData = mesg.getGearChangeData();

      // check if gear data are available, it can be null
      if (gearChangeData != null) {

         // create gear data for the current time
         final GearData gearData = new GearData();

         gearData.absoluteTime = javaTime;
         gearData.gears = gearChangeData;

         _gearData.add(gearData);
      }
   }
}
