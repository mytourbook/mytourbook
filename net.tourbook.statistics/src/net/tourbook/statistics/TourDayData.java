/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

public class TourDayData {

	long[]				fTourIds;

	long[]				fTypeIds;
	int[]				fTypeColorIndex;

	int[]				fYearValues;
	int[]				fMonthValues;
	int[]				fDOYValues;

	int[]				years;
	int[]				yearDays;
	int					allDaysInAllYears;

	int[]				fDistanceLow;
	int[]				fAltitudeLow;
	int[]				fTimeLow;

	int[]				fDistanceHigh;
	int[]				fAltitudeHigh;
	int[]				fTimeHigh;

	int[]				fTourStartValues;
	int[]				fTourEndValues;

	int[]				fTourDistanceValues;
	int[]				fTourAltitudeValues;

	ArrayList<String>	tourTitle;

	ArrayList<Integer>	fTourRecordingTimeValues;
	ArrayList<Integer>	fTourDrivingTimeValues;

	ArrayList<String>	tourDescription;

	/**
	 * hashmap contains the tags for the tour where the key is the tour ID
	 */
	HashMap<Long, ArrayList<Long>>	fTagIds;

}
