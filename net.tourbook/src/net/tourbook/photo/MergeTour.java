/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class MergeTour {

	private static PeriodType	_tourPeriodTemplate	= PeriodType.yearMonthDayTime()
													// hide these components
															.withMonthsRemoved()
															.withMinutesRemoved()
															.withSecondsRemoved()
															.withMillisRemoved();

	boolean						isDummyTour;

	long						tourId				= Long.MIN_VALUE;
	long						tourTypeId			= -1;

	long						tourStartTime;

	/**
	 * Tour end time is {@link Long#MAX_VALUE} when not yet set.
	 */
	long						tourEndTime			= Long.MAX_VALUE;

	DateTime					tourStartDateTime;
	DateTime					tourEndDateTime;
	Period						tourPeriod;

	int							numberOfPhotos;

	/**
	 * Constructor for a real tour.
	 */
	MergeTour() {}

	/**
	 * Constructor for a dummy tour.
	 * 
	 * @param notUsed
	 */
	MergeTour(final Object notUsed) {
		isDummyTour = true;
	}

	void setTourEndTime(long time) {

		// ensure that a time difference of at least 1 second is set for a tour
		if (time < (tourStartTime + 1000)) {
			time = tourStartTime + 1000;
		}

		tourEndTime = time;
		tourEndDateTime = new DateTime(time);

		tourPeriod = new Period(tourStartTime, tourEndTime, _tourPeriodTemplate);
	}

	void setTourStartTime(final long time) {

		tourStartTime = time;
		tourStartDateTime = new DateTime(time);
	}

}
