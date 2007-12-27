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
import net.tourbook.ui.UI;
import net.tourbook.util.ArrayListToArray;

public class ProviderTourDay extends DataProvider /* implements IBarSelectionProvider */{

	private static ProviderTourDay	fInstance;

	private TourPerson				fActivePerson;
	private int						fCurrentYear;
//	private int						fCurrentMonth;

	private TourDataTour			fTourDataTour;
//	private final Calendar			fCalendar	= GregorianCalendar.getInstance();

	private TourTypeFilter			fActiveTourTypeFilter;

//	private Long					fSelectedTourId;

	private ProviderTourDay() {}

	public static ProviderTourDay getInstance() {
		if (fInstance == null) {
			fInstance = new ProviderTourDay();
		}
		return fInstance;
	}

	TourDataTour getDayData(final TourPerson person,
							final TourTypeFilter tourTypeFilter,
							final int year,
							final boolean refreshData) {

		// dont reload data which are already here
		if (person == fActivePerson
				&& tourTypeFilter == fActiveTourTypeFilter
				&& year == fCurrentYear
				&& refreshData == false) {
			return fTourDataTour;
		}

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		fTourDataTour = new TourDataTour();

		final String sqlString = //
		"SELECT " // //$NON-NLS-1$
				+ "TOURID				, "// 1 //$NON-NLS-1$
				+ "STARTMONTH			, "// 2 //$NON-NLS-1$
				+ "STARTDAY				, "// 3 //$NON-NLS-1$
				+ "STARTHOUR			, "// 4 //$NON-NLS-1$
				+ "STARTMINUTE			, "// 5 //$NON-NLS-1$
				+ "TOURDISTANCE			, "// 6 //$NON-NLS-1$
				+ "TOURALTUP			, "// 7 //$NON-NLS-1$
				+ "TOURDRIVINGTIME		, "// 8 //$NON-NLS-1$
				+ "tourRecordingTime	, "// 9 //$NON-NLS-1$
				+ "tourType_typeId 		\n" // 10 //$NON-NLS-1$
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE STARTYEAR = " + Integer.toString(year)) //$NON-NLS-1$
				+ getSQLFilter(person, tourTypeFilter)
				+ (" ORDER BY StartMonth, StartDay, StartHour , StartMinute "); //$NON-NLS-1$

		try {
			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			final Calendar calendar = GregorianCalendar.getInstance();

			final ArrayList<Long> dbTourIds = new ArrayList<Long>();
			final ArrayList<Integer> dbDOY = new ArrayList<Integer>();
			final ArrayList<Integer> dbMonths = new ArrayList<Integer>();

			final ArrayList<Integer> dbDistance = new ArrayList<Integer>();
			final ArrayList<Integer> dbAltitude = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourTime = new ArrayList<Integer>();

			final ArrayList<Long> dbTypeIds = new ArrayList<Long>();
			final ArrayList<Integer> dbTypeColorIndex = new ArrayList<Integer>();

			while (result.next()) {

				final int tourMonth = result.getInt(2) - 1;

				calendar.set(year, tourMonth, result.getShort(3));
				final int tourDOY = calendar.get(Calendar.DAY_OF_YEAR) - 1;

				dbTourIds.add(result.getLong(1));
				dbDOY.add(tourDOY);
				dbMonths.add(tourMonth);

				dbDistance.add((int) (result.getInt(6) / 1000 / UI.UNIT_VALUE_DISTANCE));
				dbAltitude.add((int) (result.getInt(7) / UI.UNIT_VALUE_ALTITUDE));

				/*
				 * set the tour time
				 */
				final int drivingTime = result.getInt(8);
				final int recordingTime = result.getInt(9);
				dbTourTime.add(drivingTime == 0 ? recordingTime : drivingTime);

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */
				int colorIndex = 0;
				final Object dbTypeIdObject = result.getObject(10);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(10);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
							colorIndex = typeIndex + StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
							break;
						}
					}
				}

				dbTypeColorIndex.add(colorIndex);
				dbTypeIds.add((Long) dbTypeIdObject);
			}

			conn.close();

			final int[] tourDOYs = ArrayListToArray.toInt(dbDOY);

			final int[] timeHigh = ArrayListToArray.toInt(dbTourTime);
			final int[] distanceHigh = ArrayListToArray.toInt(dbDistance);
			final int[] altitudeHigh = ArrayListToArray.toInt(dbAltitude);

			final int valueLength = timeHigh.length;
			final int[] timeLow = new int[valueLength];
			final int[] distanceLow = new int[valueLength];
			final int[] altitudeLow = new int[valueLength];

			int lastDOY = -1;
			int lastTime = 0;
			int lastDistance = 0;
			int lastAltitude = 0;

			/*
			 * set the low/high values when different tours have the same day
			 */
			int tourIndex = 0;
			for (; tourIndex < tourDOYs.length; tourIndex++) {

				if (lastDOY == tourDOYs[tourIndex]) {

					// current tour is on the same day as the tour before

					timeLow[tourIndex] = lastTime;
					distanceLow[tourIndex] = lastDistance;
					altitudeLow[tourIndex] = lastAltitude;

					lastTime = timeHigh[tourIndex - 1] += lastTime;
					lastDistance = distanceHigh[tourIndex - 1] += lastDistance;
					lastAltitude = altitudeHigh[tourIndex - 1] += lastAltitude;

				} else {

					// current tour is on another day as the tour before

					updateLastTour(tourIndex,
							lastTime,
							lastDistance,
							lastAltitude,
							timeLow,
							distanceLow,
							altitudeLow,
							timeHigh,
							distanceHigh,
							altitudeHigh);

					lastTime = 0;
					lastDistance = 0;
					lastAltitude = 0;

					lastDOY = tourDOYs[tourIndex];
				}
			}

			updateLastTour(tourIndex,
					lastTime,
					lastDistance,
					lastAltitude,
					timeLow,
					distanceLow,
					altitudeLow,
					timeHigh,
					distanceHigh,
					altitudeHigh);

			fTourDataTour.fTourIds = ArrayListToArray.toLong(dbTourIds);
			fTourDataTour.fDOYValues = tourDOYs;

			fTourDataTour.fTypeIds = ArrayListToArray.toLong(dbTypeIds);
			fTourDataTour.fTypeColorIndex = ArrayListToArray.toInt(dbTypeColorIndex);

			fTourDataTour.fMonthValues = ArrayListToArray.toInt(dbMonths);

			fTourDataTour.fTimeLow = timeLow;
			fTourDataTour.fDistanceLow = distanceLow;
			fTourDataTour.fAltitudeLow = altitudeLow;

			fTourDataTour.fTimeHigh = timeHigh;
			fTourDataTour.fDistanceHigh = distanceHigh;
			fTourDataTour.fAltitudeHigh = altitudeHigh;

			fActivePerson = person;
			fActiveTourTypeFilter = tourTypeFilter;
			fCurrentYear = year;

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return fTourDataTour;
	}

////	public Integer getSelectedMonth() {
////		return fCurrentMonth;
////	}
//////
////	public Long getSelectedTourId() {
////		return fSelectedTourId;
////	}
//
//	public void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {
//
//		chartModel.setCustomData(ChartDataModel.BAR_INFO_PROVIDER, new IChartInfoProvider() {
//			public String getInfo(final int serieIndex, final int valueIndex) {
//
//				fCalendar.set(fCurrentYear, 0, 1);
//				fCalendar.set(Calendar.DAY_OF_YEAR, fTourDataTour.fDOYValues[valueIndex] + 1);
//
//				fCurrentMonth = fCalendar.get(Calendar.MONTH) + 1;
//				fSelectedTourId = fTourDataTour.fTourIds[valueIndex];
//
//				final int duration = fTourDataTour.fTimeHigh[valueIndex]
//						- fTourDataTour.fTimeLow[valueIndex];
//
//				final String barInfo = new Formatter().format(Messages.TOURDAYINFO_TOUR_DATE_FORMAT
//						+ Messages.TOURDAYINFO_DISTANCE
//						+ Messages.TOURDAYINFO_ALTITUDE
//						+ Messages.TOURDAYINFO_DURATION,
//						fCalendar.get(Calendar.DAY_OF_MONTH),
//						fCalendar.get(Calendar.MONTH) + 1,
//						fCalendar.get(Calendar.YEAR),
//						fTourDataTour.fDistanceHigh[valueIndex],
//						fTourDataTour.fAltitudeHigh[valueIndex],
//						duration / 3600,
//						(duration % 3600) / 60).toString();
//
//				return barInfo;
//			}
//		});
//
////		// set the menu context provider
////		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER,
////				new TourContextProvider(chartWidget, this));
//	}

//	/**
//	 * Set the tour id which is selected in the statistic
//	 * 
//	 * @param selectedTourId
//	 */
//	public void setSelectedTourId(Long selectedTourId) {
//		fSelectedTourId = selectedTourId;
//	}

	private final void updateLastTour(	final int tourIndex,
										final int lastTime,
										final int lastDistance,
										final int lastAltitude,
										final int[] timeLow,
										final int[] distanceLow,
										final int[] altitudeLow,
										final int[] timeHigh,
										final int[] distanceHigh,
										final int[] altitudeHigh) {
		if (lastTime > 0) {

			// update last time
			final int lastTourIndex = tourIndex - 1;
			timeLow[lastTourIndex] = lastTime;
			distanceLow[lastTourIndex] = lastDistance;
			altitudeLow[lastTourIndex] = lastAltitude;

			timeHigh[lastTourIndex] += lastTime;
			distanceHigh[lastTourIndex] += lastDistance;
			altitudeHigh[lastTourIndex] += lastAltitude;
		}
	}
}
