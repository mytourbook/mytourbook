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

import com.garmin.fit.EventMesg;
import com.garmin.fit.EventMesgListener;

import java.util.List;

import net.tourbook.data.GearData;

/**
 * Set gear data
 */
public class MesgListener_Event extends AbstractMesgListener implements EventMesgListener {

   List<GearData> _gearData;

   public MesgListener_Event(final FitData fitData) {

      super(fitData);

      _gearData = fitData.getGearData();
   }

   @Override
   public void onMesg(final EventMesg mesg) {

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
