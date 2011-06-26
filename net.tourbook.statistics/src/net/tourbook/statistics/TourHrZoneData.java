/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.statistics;

import java.util.ArrayList;
import java.util.HashMap;

class TourHrZoneData {

	long[]							tourIds;
	long[]							typeIds;

	int[]							typeColorIndex;

	int[]							years;

	/**
	 * number for all days in each year
	 */
	int[]							daysInEachYear;

	/**
	 * number of days for all years
	 */
	int								allDaysInAllYears;

	int[]							tourYear;
	int[]							tourMonth;
	int[]							tourDOY;
	int[]							tourWeek;

	int[]							tourTimeStart;
	int[]							tourTimeEnd;

	int[]							tourAltitude;
	int[]							tourDistance;

	int								numberOfHrZones;
	ArrayList<int[]>				hrZones;

	ArrayList<Integer>				tourRecordingTime;
	ArrayList<Integer>				tourDrivingTime;

	ArrayList<String>				tourTitle;
	ArrayList<String>				tourDescription;

	/**
	 * hashmap contains the tags for the tour where the key is the tour ID
	 */
	HashMap<Long, ArrayList<Long>>	tagIds;
	public int[][]					timeLow;
	public int[][]					timeHigh;

}
