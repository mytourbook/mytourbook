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

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.preference.IPreferenceStore;

public abstract class DataProvider {

	TourPerson						_activePerson;
	TourTypeFilter					_activeTourTypeFilter;

	int								_lastYear;
	int								_numberOfYears;

	/**
	 * all years
	 */
	int[]							_years;

	/**
	 * number of days in a year
	 */
	int[]							_yearDays;

	/**
	 * number of weeks in a year
	 */
	int[]							_yearWeeks;

	Calendar						_calendar	= GregorianCalendar.getInstance();

	private static IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	/**
	 * @param finalYear
	 * @param numberOfYears
	 * @return Returns a list with all years
	 */
	static String getYearList(final int finalYear, final int numberOfYears) {

		final StringBuilder sb = new StringBuilder();

		for (int currentYear = finalYear; currentYear >= finalYear - numberOfYears + 1; currentYear--) {

			if (currentYear != finalYear) {
				sb.append(',');
			}

			sb.append(Integer.toString(currentYear));
		}

		return sb.toString();
	}

	/**
	 * @param selectedYear
	 * @param numberOfYears
	 * @return Returns the number of days between {@link #_lastYear} and selectedYear
	 */
	int getYearDOYs(final int selectedYear) {

		int yearDOYs = 0;
		int yearIndex = 0;

		for (int currentYear = _lastYear - _numberOfYears + 1; currentYear < selectedYear; currentYear++) {

			if (currentYear == selectedYear) {
				return yearDOYs;
			}

			yearDOYs += _yearDays[yearIndex];

			yearIndex++;
		}

		return yearDOYs;
	}

	/**
	 * Get different data for each year, data are set into <br>
	 * <br>
	 * All years in {@link #years} <br>
	 * Number of day's in {@link #daysInEachYear} <br>
	 * Number of week's in {@link #_yearWeeks}
	 */
	void initYearNumbers() {

		_calendar.setFirstDayOfWeek(_prefStore//
				.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK));
		_calendar.setMinimalDaysInFirstWeek(_prefStore
				.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK));

		_years = new int[_numberOfYears];
		_yearDays = new int[_numberOfYears];
		_yearWeeks = new int[_numberOfYears];

		final int firstYear = _lastYear - _numberOfYears + 1;
		int yearIndex = 0;

		for (int currentYear = firstYear; currentYear <= _lastYear; currentYear++) {

			_calendar.set(currentYear, 0, 1);

			final int weekOfYear = _calendar.getActualMaximum(Calendar.WEEK_OF_YEAR);
			final int dayOfYear = _calendar.getActualMaximum(Calendar.DAY_OF_YEAR);

			_years[yearIndex] = currentYear;
			_yearDays[yearIndex] = dayOfYear;
			_yearWeeks[yearIndex] = weekOfYear;

			yearIndex++;
		}
	}
}
