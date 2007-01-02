/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

public class TourDataTime {

	int		fYear;

	long[]	fTourIds;
	long[]	fTypeIds;

	int[]	fTypeColorIndex;

	int[]	fTourDOYValues;
	int[]	fTourMonthValues;

	int[]	fTourTimeStartValues;
	int[]	fTourTimeEndValues;

	int[]	fTourTimeAltitudeValues;

	int[]	fTourTimeDistanceValues;
	int[]	fTourTimeDurationValues;

	public TourDataTime(int year) {
		fYear = year;
	}

}
