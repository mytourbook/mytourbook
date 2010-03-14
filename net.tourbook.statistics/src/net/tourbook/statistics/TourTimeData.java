/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

public class TourTimeData {

	long[]							fTourIds;
	long[]							fTypeIds;

	int[]							fTypeColorIndex;

	/**
	 * number for all days in each year
	 */
	int[]							yearDays;

	int[]							years;
	int								allDaysInAllYears;

	int[]							fTourYearValues;
	int[]							fTourMonthValues;
	int[]							fTourDOYValues;
	int[]							weekValues;

	int[]							fTourTimeStartValues;
	int[]							fTourTimeEndValues;

	int[]							fTourAltitudeValues;
	int[]							fTourDistanceValues;

	ArrayList<Integer>				fTourRecordingTimeValues;
	ArrayList<Integer>				fTourDrivingTimeValues;

	ArrayList<String>				fTourTitle;
	ArrayList<String>				tourDescription;

	/**
	 * hashmap contains the tags for the tour where the key is the tour ID
	 */
	HashMap<Long, ArrayList<Long>>	fTagIds;
}
