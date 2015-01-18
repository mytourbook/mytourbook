/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import net.tourbook.device.garmin.fit.FitContext;

import com.garmin.fit.BikeProfileMesg;
import com.garmin.fit.BikeProfileMesgListener;

public class BikeProfileMesgListenerImpl extends AbstractMesgListener implements BikeProfileMesgListener {

	public BikeProfileMesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final BikeProfileMesg mesg) {

		/**
		 * THIS EVENT IS NEVER FIRED
		 */

		final Short frontGear = mesg.getFrontGear(0);
		final Short frontGearNum = mesg.getFrontGearNum();
		final int numFrontGear = mesg.getNumFrontGear();

		final Short rearGear = mesg.getRearGear(0);
		final Short rearGearNum = mesg.getRearGearNum();
		final int numRearGear = mesg.getNumRearGear();

		System.out.println(String.format(
				"BikeProfileMesg\t%-5s%-5s%-5s%-5s%-5s%-5s",
				frontGear,
				frontGearNum,
				numFrontGear,
				rearGear,
				rearGearNum,
				numRearGear));
		// TODO remove SYSTEM.OUT.PRINTLN

	}

}
