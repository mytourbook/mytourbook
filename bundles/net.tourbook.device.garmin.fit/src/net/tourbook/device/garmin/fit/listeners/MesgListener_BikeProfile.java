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

import com.garmin.fit.BikeProfileMesg;
import com.garmin.fit.BikeProfileMesgListener;

import net.tourbook.device.garmin.fit.FitData;

public class MesgListener_BikeProfile extends AbstractMesgListener implements BikeProfileMesgListener {

//   private static final String NL = "\n";//$NON-NLS-1$

   public MesgListener_BikeProfile(final FitData fitData) {
      super(fitData);
   }

   @Override
   public void onMesg(final BikeProfileMesg mesg) {

//      final int numFrontGear = mesg.getNumFrontGear();
//      final int numRearGear = mesg.getNumRearGear();
//
//      final Short frontGear = mesg.getFrontGear(0);
//      final Short rearGear = mesg.getRearGear(0);
//
//      final Short frontGearNum = mesg.getFrontGearNum();
//      final Short rearGearNum = mesg.getRearGearNum();
//
//      System.out.println(String.format(
//
//            "" //$NON-NLS-1$
//                  + "MesgListener_BikeProfile" + NL + NL //$NON-NLS-1$
//
//                  + " Front num gears  %-5s" + NL //$NON-NLS-1$
//                  + " Rear num gears   %-5s" + NL + NL //$NON-NLS-1$
//
//                  + " Front teeth      %-5s" + NL //$NON-NLS-1$
//                  + " Rear teeth       %-5s" + NL + NL //$NON-NLS-1$
//
//                  + " frontGearNum     %-5s" + NL //$NON-NLS-1$
//                  + " rearGearNum      %-5s" + NL //$NON-NLS-1$
//
//            ,
//
//            numFrontGear,
//            numRearGear,
//
//            frontGear,
//            rearGear,
//
//            frontGearNum,
//            rearGearNum
//      ));
   }

}
