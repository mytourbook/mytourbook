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

import com.garmin.fit.SportMesg;
import com.garmin.fit.SportMesgListener;

import net.tourbook.device.garmin.fit.FitData;

public class MesgListener_Sport extends AbstractMesgListener implements SportMesgListener {

   public MesgListener_Sport(final FitData fitData) {
      super(fitData);
   }

   @Override
   public void onMesg(final SportMesg mesg) {

      final String profileName = mesg.getName();
      if (profileName != null) {
         fitData.setProfileName(profileName.trim());
      }

   }

}
