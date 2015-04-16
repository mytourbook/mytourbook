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

import com.garmin.fit.EventMesg;
import com.garmin.fit.EventMesgListener;

public class EventMesgListenerImpl extends AbstractMesgListener implements EventMesgListener {

	public EventMesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final EventMesg mesg) {

//		System.out.println(String.format(
//				"EventMesg\t%s : %-5s%-5s%-5s%-5s%-20h",
//				mesg.getTimestamp().getTimestamp(),
//				mesg.getFrontGear(),
//				mesg.getFrontGearNum(),
//				mesg.getRearGear(),
//				mesg.getRearGearNum(),
//				mesg.getGearChangeData()
//				));
//		// TODO remove SYSTEM.OUT.PRINTLN

		context.mesgEvent(mesg);
	}

}
