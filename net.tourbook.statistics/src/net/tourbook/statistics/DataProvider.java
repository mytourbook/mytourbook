/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public abstract class DataProvider {

	TourPerson		fActivePerson;
	TourTypeFilter	fActiveTourTypeFilter;

	int				fLastYear;
	int				fNumberOfYears;

	/**
	 * all years
	 */
	int[]			fYears;

	/**
	 * number of days in a year
	 */
	int[]			fYearDays;

	/**
	 * number of weeks in a year
	 */
	int[]			fYearWeeks;

	Calendar		fCalendar	= GregorianCalendar.getInstance();

	/**
	 * @param finalYear
	 * @param numberOfYears
	 * @return Returns a list with all years
	 */
	static String getYearList(final int finalYear, final int numberOfYears) {

		final StringBuilder buffer = new StringBuilder();

		for (int currentYear = finalYear; currentYear >= finalYear - numberOfYears + 1; currentYear--) {

			if (currentYear != finalYear) {
				buffer.append(',');
			}

			buffer.append(Integer.toString(currentYear));
		}

		return buffer.toString();
	}

	/**
	 * @param selectedYear
	 * @param numberOfYears
	 * @return Returns the number of days between {@link #fLastYear} and selectedYear
	 */
	int getYearDOYs(final int selectedYear) {

		int yearDOYs = 0;
		int yearIndex = 0;

		for (int currentYear = fLastYear - fNumberOfYears + 1; currentYear < selectedYear; currentYear++) {

			if (currentYear == selectedYear) {
				return yearDOYs;
			}

			yearDOYs += fYearDays[yearIndex];

			yearIndex++;
		}

		return yearDOYs;
	}

	/**
	 * get numbers for each year <br>
	 * <br>
	 * all years into {@link #fYears} <br>
	 * number of day's into {@link #fYearDays} <br>
	 * number of week's into {@link #fYearWeeks}
	 */
	void initYearNumbers() {

		fYears = new int[fNumberOfYears];
		fYearDays = new int[fNumberOfYears];
		fYearWeeks = new int[fNumberOfYears];

		final int firstYear = fLastYear - fNumberOfYears + 1;

		DateTime dt = (new DateTime()).withYear(firstYear)
				.withWeekOfWeekyear(1)
				.withDayOfWeek(DateTimeConstants.MONDAY);

		int yearIndex = 0;
		for (int currentYear = firstYear; currentYear <= fLastYear; currentYear++) {

			dt = dt.withYear(currentYear);

			fYears[yearIndex] = currentYear;
			fYearDays[yearIndex] = dt.dayOfYear().getMaximumValue();
			fYearWeeks[yearIndex] = dt.weekOfWeekyear().getMaximumValue();

			yearIndex++;
		}
	}
}
