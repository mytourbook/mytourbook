/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.time;

import java.time.ZonedDateTime;

import net.tourbook.common.UI;

/**
 * Contains the date/time of a tour or view item.
 */
public class TourDateTime {

	public ZonedDateTime	tourZonedDateTime;

	/**
	 * Time zone offset of this tour, can be empty when a time zone is not set.
	 */
	public String			timeZoneOffsetLabel;

	/**
	 * Tour week day.
	 */
	public String			weekDay;

	/**
	 * @param tourZonedDateTime
	 */
	public TourDateTime(final ZonedDateTime tourZonedDateTime) {

		this.tourZonedDateTime = tourZonedDateTime;
		this.timeZoneOffsetLabel = UI.EMPTY_STRING;
	}

	/**
	 * @param tourZonedDateTime
	 * @param timeZoneOffsetLabel
	 * @param weekDay
	 */
	public TourDateTime(final ZonedDateTime tourZonedDateTime, final String timeZoneOffsetLabel, final String weekDay) {

		this.tourZonedDateTime = tourZonedDateTime;
		this.timeZoneOffsetLabel = timeZoneOffsetLabel;

		this.weekDay = weekDay;
	}

	@Override
	public String toString() {
		return "TourDateTime ["

				+ ("tourZonedDateTime=" + tourZonedDateTime + ", ")
				+ ("timeZoneOffsetLabel=" + timeZoneOffsetLabel + ", ")
				+ ("weekDay=" + weekDay)

				+ "]";
	}

}
