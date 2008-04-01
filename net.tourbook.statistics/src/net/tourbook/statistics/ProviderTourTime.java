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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;
import net.tourbook.util.ArrayListToArray;

public class ProviderTourTime extends DataProvider {

	private static ProviderTourTime	fInstance;

	private ArrayList<Long>			fTourIds;

	private Long					fSelectedTourId;

	private TourTimeData			fTourDataTime;

	private ProviderTourTime() {}

	public static ProviderTourTime getInstance() {
		if (fInstance == null) {
			fInstance = new ProviderTourTime();
		}
		return fInstance;
	}

	public Long getSelectedTourId() {
		return fSelectedTourId;
	}

	/**
	 * Retrieve chart data from the database
	 * 
	 * @param person
	 * @param tourTypeFilter
	 * @param lastYear
	 * @param numberOfYears
	 * @return
	 */
	TourTimeData getTourTimeData(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int lastYear,
									int numberOfYears,
									final boolean refreshData) {

		// dont reload data which are already here
		if (fActivePerson == person
				&& fActiveTourTypeFilter == tourTypeFilter
				&& fLastYear == lastYear
				&& fNumberOfYears == numberOfYears
				&& refreshData == false) {
			return fTourDataTime;
		}

		fActivePerson = person;
		fActiveTourTypeFilter = tourTypeFilter;

		fLastYear = lastYear;
		fNumberOfYears = numberOfYears;

		initYearDOYs();

		final ArrayList<TourType> tourTypeList = TourDatabase.getTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final String sqlString = "SELECT " //$NON-NLS-1$
				+ "TourId, " //				1 //$NON-NLS-1$
				+ "StartYear, " //			2 //$NON-NLS-1$
				+ "StartMonth, " //			3 //$NON-NLS-1$
				+ "StartDay, " //			4 //$NON-NLS-1$
				+ "StartHour, " //			5 //$NON-NLS-1$
				+ "StartMinute, " //		6 //$NON-NLS-1$
				+ "TourDistance, " //		7 //$NON-NLS-1$
				+ "TourAltUp, " //			8 //$NON-NLS-1$
				+ "TourRecordingTime, " //	9 //$NON-NLS-1$
				+ "TourDrivingTime, "//		10 //$NON-NLS-1$
				+ "TourType_typeId "//		11 //$NON-NLS-1$
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")\n") //$NON-NLS-1$
				+ getSQLFilter(person, tourTypeFilter)
				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			final ArrayList<Integer> dbYear = new ArrayList<Integer>();
			final ArrayList<Integer> dbMonths = new ArrayList<Integer>();
			final ArrayList<Integer> dbAllYearsDOY = new ArrayList<Integer>(); // DOY...Day Of Year

			final ArrayList<Integer> startTimeList = new ArrayList<Integer>();
			final ArrayList<Integer> endTimeList = new ArrayList<Integer>();

			final ArrayList<Integer> distanceList = new ArrayList<Integer>();
			final ArrayList<Integer> altitudeList = new ArrayList<Integer>();
			final ArrayList<Integer> durationList = new ArrayList<Integer>();

			final ArrayList<Long> dbTypeIds = new ArrayList<Long>();
			final ArrayList<Integer> dbTypeColorIndex = new ArrayList<Integer>();

			fTourIds = new ArrayList<Long>();

			while (result.next()) {

				fTourIds.add(result.getLong(1));

				final int tourYear = result.getShort(2);
				final int tourMonth = result.getShort(3) - 1;
				final int startHour = result.getShort(5);
				final int startMinute = result.getShort(6);
				final int startTime = startHour * 3600 + startMinute * 60;

				final int recordingTime = result.getInt(9);

				// create data lists for the chart, start with 0
				fCalendar.set(tourYear, tourMonth, result.getShort(4), startHour, startMinute);
				final int tourDOY = fCalendar.get(Calendar.DAY_OF_YEAR) - 1;

				dbYear.add(tourYear);
				dbMonths.add(tourMonth);
				dbAllYearsDOY.add(getYearDOYs(tourYear) + tourDOY);

//				doyList.add(tourDOY);
				startTimeList.add(startTime);
				endTimeList.add((startTime + recordingTime));

				distanceList.add((int) (result.getInt(7) / 1000 / UI.UNIT_VALUE_DISTANCE));
				altitudeList.add((int) (result.getInt(8) / UI.UNIT_VALUE_ALTITUDE));
				durationList.add(recordingTime);

				/*
				 * convert type id to the type index in the tour type array, this is also the color
				 * index for the tour type
				 */
				int tourTypeColorIndex = 0;
				final Long dbTypeIdObject = (Long) result.getObject(11);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(11);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (tourTypes[typeIndex].getTypeId() == dbTypeId) {
							tourTypeColorIndex = typeIndex + StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
							break;
						}
					}
				}

				dbTypeColorIndex.add(tourTypeColorIndex);
				dbTypeIds.add(dbTypeIdObject == null ? TourType.TOUR_TYPE_ID_NOT_DEFINED : dbTypeIdObject);
			}

			conn.close();

			// get number of days for all years
			int yearDays = 0;
			for (int doy : fYearDays) {
				yearDays += doy;
			}

			/*
			 * create data
			 */
			fTourDataTime = new TourTimeData();

			fTourDataTime.fTourIds = ArrayListToArray.toLong(fTourIds);

			fTourDataTime.fTypeIds = ArrayListToArray.toLong(dbTypeIds);
			fTourDataTime.fTypeColorIndex = ArrayListToArray.toInt(dbTypeColorIndex);

			fTourDataTime.fYearValues = ArrayListToArray.toInt(dbYear);
			fTourDataTime.allDaysInAllYears = yearDays;
			fTourDataTime.yearDays = fYearDays;
			fTourDataTime.years = fYears;

			fTourDataTime.fTourDOYValues = ArrayListToArray.toInt(dbAllYearsDOY);
			fTourDataTime.fTourMonthValues = ArrayListToArray.toInt(dbMonths);

			fTourDataTime.fTourTimeStartValues = ArrayListToArray.toInt(startTimeList);
			fTourDataTime.fTourTimeEndValues = ArrayListToArray.toInt(endTimeList);

			fTourDataTime.fTourTimeDistanceValues = ArrayListToArray.toInt(distanceList);
			fTourDataTime.fTourTimeAltitudeValues = ArrayListToArray.toInt(altitudeList);
			fTourDataTime.fTourTimeDurationValues = ArrayListToArray.toInt(durationList);

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return fTourDataTime;
	}

//	void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {
//
//		chartModel.setCustomData(
//
//		ChartDataModel.BAR_INFO_PROVIDER, new IChartInfoProvider() {
//			
//			public String getInfo(final int serieIndex, int valueIndex) {
//
//				final int[] tourDateValues = fTourTimeData.fTourDOYValues;
//
//				if (valueIndex >= tourDateValues.length) {
//					valueIndex -= tourDateValues.length;
//				}
//
//				if (tourDateValues == null || valueIndex >= tourDateValues.length) {
//					return ""; //$NON-NLS-1$
//				}
//				fCalendar.set(fCurrentYear, 0, 1);
//				fCalendar.set(Calendar.DAY_OF_YEAR, tourDateValues[valueIndex] + 1);
//
//				fCurrentMonth = fCalendar.get(Calendar.MONTH) + 1;
//				fSelectedTourId = fTourTimeData.fTourIds[valueIndex];
//
//				/*
//				 * get tour type name
//				 */
//				final long typeId = fTourTimeData.fTypeIds[valueIndex];
//				final ArrayList<TourType> tourTypes = TourbookPlugin.getDefault().getTourTypes();
//
//				String tourTypeName = ""; //$NON-NLS-1$
//				for (final Iterator<TourType> iter = tourTypes.iterator(); iter.hasNext();) {
//					final TourType tourType = (TourType) iter.next();
//					if (tourType.getTypeId() == typeId) {
//						tourTypeName = tourType.getName();
//					}
//				}
//				final int[] startValue = fTourTimeData.fTourTimeStartValues;
//				final int[] endValue = fTourTimeData.fTourTimeEndValues;
//				final int[] durationValue = fTourTimeData.fTourTimeDurationValues;
//
//				final String barInfo = new Formatter().format(Messages.TOURTIMEINFO_DATE_FORMAT
//						+ Messages.TOURTIMEINFO_DISTANCE
//						+ Messages.TOURTIMEINFO_ALTITUDE
//						+ Messages.TOURTIMEINFO_DURATION
//						+ Messages.TOURTIMEINFO_TOUR_TYPE,
//						fCalendar.get(Calendar.DAY_OF_MONTH),
//						fCalendar.get(Calendar.MONTH) + 1,
//						fCalendar.get(Calendar.YEAR),
//						startValue[valueIndex] / 3600,
//						(startValue[valueIndex] % 3600) / 60,
//						endValue[valueIndex] / 3600,
//						(endValue[valueIndex] % 3600) / 60,
//						fTourTimeData.fTourTimeDistanceValues[valueIndex],
//						fTourTimeData.fTourTimeAltitudeValues[valueIndex],
//						durationValue[valueIndex] / 3600,
//						(durationValue[valueIndex] % 3600) / 60,
//						tourTypeName).toString();
//
//				return barInfo;
//			}
//		});
//
//		// set the menu context provider
//		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER,
//				new TourContextProvider(chartWidget, this));
//	}

	void setSelectedTourId(Long selectedTourId) {
		fSelectedTourId = selectedTourId;
	}

}
