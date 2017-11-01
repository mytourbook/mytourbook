/*******************************************************************************
 * Copyright (C) 2011-2017 Matthias Helmling and Contributors
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
package net.tourbook.ui.views.calendar;

import java.util.ArrayList;

public class CalendarTourData {

	LoadingState	loadingState	= LoadingState.NOT_LOADED;

	long			tourId;
	long			typeId;

	int				typeColorIndex;

	int				year;
	int				month;
	int				day;
	int				week;
	int				dayOfWeek;

	int				startTime;
	int				endTime;

	int				altitude;
	int				distance;

	int				recordingTime;
	int				drivingTime;

	String			tourTitle;
	String			tourDescription;

	ArrayList<Long>	tagIds;

	/**
	 * Number of tours in a week
	 */
	int				numTours;

	/**
	 * Currently only manually created tours can be copied/dragged
	 */
	boolean			isManualTour;

	@Override
	public String toString() {
		return "CalendarTourData [" //$NON-NLS-1$

				+ "loadingState=" + loadingState + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "year=" + year + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "month=" + month + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "day=" + day + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "week=" + week + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "numTours=" + numTours //$NON-NLS-1$

				+ "]"; //$NON-NLS-1$
	}

}
