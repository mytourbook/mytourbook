/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import java.util.GregorianCalendar;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.util.ArrayListToArray;

public class ProviderTourTime extends DataProvider /* implements IBarSelectionProvider */{

	private static ProviderTourTime	fInstance;

	private ArrayList<Long>			fTourIds;

	private TourPerson				fActivePerson;
	private TourTypeFilter			fActiveTourTypeFilter;

	private int						fCurrentYear;
	private int						fCurrentMonth;

	private Long					fSelectedTourId;

	private final Calendar			fCalendar	= GregorianCalendar.getInstance();

	private TourDataTime			fTourTimeData;

	private ProviderTourTime() {}

	public static ProviderTourTime getInstance() {
		if (fInstance == null) {
			fInstance = new ProviderTourTime();
		}
		return fInstance;
	}

	public Integer getSelectedMonth() {
		return fCurrentMonth;
	}

	public Long getSelectedTourId() {
		return fSelectedTourId;
	}

	/**
	 * Retrieve chart data from the database
	 * 
	 * @param person
	 * @param tourTypeFilter
	 * @param year
	 * @return
	 */
	TourDataTime getTourTimeData(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int year,
									final boolean refreshData) {

		// dont reload data which are already here
		if (fActivePerson == person
				&& fActiveTourTypeFilter == tourTypeFilter
				&& fCurrentYear == year
				&& refreshData == false) {
			return fTourTimeData;
		}

		final ArrayList<TourType> tourTypeList = TourDatabase.getTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final String sqlString = "SELECT " //$NON-NLS-1$
				+ "TOURID, " //$NON-NLS-1$
				+ "STARTYEAR, " //$NON-NLS-1$
				+ "STARTMONTH, " //$NON-NLS-1$
				+ "STARTDAY, " //$NON-NLS-1$
				+ "STARTHOUR, " //$NON-NLS-1$
				+ "STARTMINUTE, " //$NON-NLS-1$
				+ "TOURDISTANCE, " //$NON-NLS-1$
				+ "TOURALTUP, " //$NON-NLS-1$
				+ "tourRecordingTime, " // 9 //$NON-NLS-1$
				+ "tourDrivingTime, "// 10 //$NON-NLS-1$
				+ "tourType_typeId "// 11 //$NON-NLS-1$
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE STARTYEAR = " + Integer.toString(year)) //$NON-NLS-1$
				+ getSQLFilter(person, tourTypeFilter)
				+ (" ORDER BY StartMonth, StartDay, StartHour, StartMinute"); //$NON-NLS-1$

		try {
			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			final ArrayList<Integer> monthList = new ArrayList<Integer>();
			final ArrayList<Integer> doyList = new ArrayList<Integer>();

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

				final int tourMonth = result.getShort(3) - 1;
				final short startHour = result.getShort(5);
				final short startMinute = result.getShort(6);
				final int startTime = startHour * 3600 + startMinute * 60;

				final int recordingTime = result.getInt(9);

				/*
				 * disabled driving time, wolfgang 17.8.07
				 */
//				final int drivingTime = result.getInt(10);
//				final int duration = drivingTime == 0 ? recordingTime : drivingTime;
				final int duration = recordingTime;

				// get date
				fCalendar.set(result.getShort(2),
						tourMonth,
						result.getShort(4),
						startHour,
						startMinute);

				// create data lists for the chart, start with 0
				doyList.add(fCalendar.get(Calendar.DAY_OF_YEAR) - 1);
				monthList.add(tourMonth);
				startTimeList.add(startTime);
				endTimeList.add((startTime + duration));

				distanceList.add(result.getInt(7) / 1000);
				altitudeList.add(result.getInt(8));
				durationList.add(duration);

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */
				int colorIndex = 0;
				final Long dbTypeIdObject = (Long) result.getObject(11);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(11);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
							colorIndex = typeIndex;
							break;
						}
					}
				}
				dbTypeColorIndex.add(colorIndex);

				dbTypeIds.add(dbTypeIdObject == null
						? TourType.TOUR_TYPE_ID_NOT_DEFINED
						: dbTypeIdObject);
			}

			conn.close();

			/*
			 * keep the data for the current year
			 */
			fTourTimeData = new TourDataTime(year);

			fTourTimeData.fTourIds = ArrayListToArray.toLong(fTourIds);
			fTourTimeData.fTypeIds = ArrayListToArray.toLong(dbTypeIds);
			fTourTimeData.fTypeColorIndex = ArrayListToArray.toInt(dbTypeColorIndex);

			fTourTimeData.fTourDOYValues = ArrayListToArray.toInt(doyList);
			fTourTimeData.fTourMonthValues = ArrayListToArray.toInt(monthList);

			fTourTimeData.fTourTimeStartValues = ArrayListToArray.toInt(startTimeList);
			fTourTimeData.fTourTimeEndValues = ArrayListToArray.toInt(endTimeList);

			fTourTimeData.fTourTimeDistanceValues = ArrayListToArray.toInt(distanceList);
			fTourTimeData.fTourTimeAltitudeValues = ArrayListToArray.toInt(altitudeList);
			fTourTimeData.fTourTimeDurationValues = ArrayListToArray.toInt(durationList);

			fActivePerson = person;
			fCurrentYear = year;
			fActiveTourTypeFilter = tourTypeFilter;

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return fTourTimeData;
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
