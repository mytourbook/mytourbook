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
package net.tourbook.statistics.graphs;

import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;

public class TourData_Time {

	long[]							tourIds;
	long[]							typeIds;

	int[]							typeColorIndex;

	/**
	 * number for all days in each year
	 */
	int[]							yearDays;

	int[]							years;
	int								allDaysInAllYears;

	int[]							tourYearValues;
	int[]							tourMonthValues;
	int[]							tourDOYValues;
	int[]							weekValues;

	int[]							tourTimeStartValues;
	int[]							tourTimeEndValues;
	ArrayList<DateTime>				tourStartDateTimes;
	int[]							tourUtcTimeOffset;

	int[]							tourAltitudeValues;
	int[]							tourDistanceValues;

	int[]							tourRecordingTimeValues;
	int[]							tourDrivingTimeValues;

	ArrayList<String>				tourTitle;
	ArrayList<String>				tourDescription;

	/**
	 * Contains the tags for the tour, key is the tour ID
	 */
	HashMap<Long, ArrayList<Long>>	tagIds;
}
