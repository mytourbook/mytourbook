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

import com.garmin.fit.LengthMesg;
import com.garmin.fit.LengthMesgListener;
import com.garmin.fit.LengthType;
import com.garmin.fit.SwimStroke;

import java.util.List;

import net.tourbook.data.SwimData;
import net.tourbook.device.garmin.fit.FitData;

/**
 * Set swim data
 */
public class MesgListener_Length extends AbstractMesgListener implements LengthMesgListener {

   private List<SwimData> _swimData;

   public MesgListener_Length(final FitData fitData) {

      super(fitData);

      _swimData = fitData.getSwimData();
   }

   @Override
   public void onMesg(final LengthMesg mesg) {

      // create gear data for the current time
      final SwimData swimData = new SwimData();

      _swimData.add(swimData);

      final com.garmin.fit.DateTime garminTime = mesg.getTimestamp();

      // convert garmin time into java time
      final long garminTimeS = garminTime.getTimestamp();
      final long garminTimeMS = garminTimeS * 1000;
      final long javaTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

      final Short avgSwimmingCadence = mesg.getAvgSwimmingCadence();
      final LengthType lengthType = mesg.getLengthType();
      final SwimStroke swimStrokeStyle = mesg.getSwimStroke();
      final Integer numStrokes = mesg.getTotalStrokes();

      swimData.absoluteTime = javaTime;

      if (lengthType != null) {
         swimData.swim_LengthType = lengthType.getValue();
      }

      if (avgSwimmingCadence != null) {
         swimData.swim_Cadence = avgSwimmingCadence;
      }

      if (numStrokes != null) {
         swimData.swim_Strokes = numStrokes.shortValue();
      }

      if (swimStrokeStyle != null) {
         swimData.swim_StrokeStyle = swimStrokeStyle.getValue();
      }
   }

}
