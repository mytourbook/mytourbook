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

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.ActivityMesgListener;

import net.tourbook.tour.TourLogManager;

public class MesgListener_Activity extends AbstractMesgListener implements ActivityMesgListener {

   public MesgListener_Activity(final FitData fitData) {
      super(fitData);
	}

	@Override
	public void onMesg(final ActivityMesg mesg) {

		final Integer numSessions = mesg.getNumSessions();

		if (numSessions == null || numSessions < 1) {

			final String message = "%s - Invalid number of sessions: %d, expected at least one session."; //$NON-NLS-1$

			TourLogManager.logSubInfo(String.format(
					message,
               fitData.getTourTitle(),
					numSessions));

			/*
			 * Do not throw an exception because the import can still be successful.
			 */
//			throw new FitDataReaderException(message); //$NON-NLS-1$
		}

	}

}
