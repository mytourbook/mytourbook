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
package net.tourbook.ui.views.tourBook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;

public class TVITourBookMonth extends TVITourBookItem {

	public TVITourBookMonth(final TourBookView view, final TVITourBookItem parentItem) {

		super(view);

		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the month item, these are tour items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final TVITourBookYear yearItem = (TVITourBookYear) (getParentItem());
		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "" + // //$NON-NLS-1$
				//
				"SELECT " //		//$NON-NLS-1$
				+ "startYear," //				1	//$NON-NLS-1$
				+ "startMonth," //				2	//$NON-NLS-1$
				+ "startDay," //				3	//$NON-NLS-1$
				+ "tourDistance," //			4	//$NON-NLS-1$
				+ "tourRecordingTime," //		5	//$NON-NLS-1$
				+ "tourDrivingTime," //			6	//$NON-NLS-1$
				+ "tourAltUp," //				7	//$NON-NLS-1$
				+ "tourAltDown," //				8	//$NON-NLS-1$
				+ "startDistance," //			9	//$NON-NLS-1$
				+ "tourID," //					10	//$NON-NLS-1$
				+ "tourType_typeId," //			11	//$NON-NLS-1$
				+ "tourTitle," //				12	//$NON-NLS-1$
				+ "deviceTimeInterval," //		13	//$NON-NLS-1$
				+ "maxSpeed," //				14	//$NON-NLS-1$
				+ "maxAltitude," //				15	//$NON-NLS-1$
				+ "maxPulse," //				16	//$NON-NLS-1$
				+ "avgPulse," //				17	//$NON-NLS-1$
				+ "avgCadence," //				18	//$NON-NLS-1$
				+ "avgTemperature," //			19	//$NON-NLS-1$
				+ "jTdataTtag.TourTag_tagId,"//	20	//$NON-NLS-1$ 
				+ "startHour," //				21	//$NON-NLS-1$
				+ "startMinute" //				22	//$NON-NLS-1$

				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData" + UI.NEW_LINE) //			//$NON-NLS-1$ //$NON-NLS-2$

				// get tag id's
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON TourData.tourID = jTdataTtag.TourData_tourId") //$NON-NLS-1$

				+ (" WHERE STARTYEAR = ?")//				//$NON-NLS-1$
				+ (" AND STARTMONTH = ?")//					//$NON-NLS-1$
				+ sqlFilter.getWhereClause()

				+ " ORDER BY StartDay, StartHour, StartMinute"; //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();

//			TourDatabase.enableRuntimeStatistics(conn);

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			statement.setInt(1, yearItem.fTourYear);
			statement.setInt(2, fTourMonth);
			sqlFilter.setParameters(statement, 3);

			long lastTourId = -1;
			ArrayList<Long> tagIds = null;

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long tourId = result.getLong(10);
				final Object resultTagId = result.getObject(20);

				if (tourId == lastTourId) {

					// get tags from outer join

					if (resultTagId instanceof Long) {
						tagIds.add((Long) resultTagId);
					}

				} else {

					// new tour is in the resultset
					final TVITourBookTour tourItem = new TVITourBookTour(fView, this);
					children.add(tourItem);

					tourItem.fTourId = tourId;

					final int dbYear = result.getInt(1);
					final int dbMonth = result.getInt(2);
					final int dbDay = result.getInt(3);

					tourItem.treeColumn = Integer.toString(dbDay);

					tourItem.fTourYear = dbYear;
					tourItem.fTourMonth = dbMonth;
					tourItem.fTourDay = dbDay;

					final long dbDistance = tourItem.colDistance = result.getLong(4);
					tourItem.colRecordingTime = result.getLong(5);
					final long dbDrivingTime = tourItem.colDrivingTime = result.getLong(6);
					tourItem.colAltitudeUp = result.getLong(7);
					tourItem.colAltitudeDown = result.getLong(8);

					tourItem.fColumnStartDistance = result.getLong(9);
					final Object tourTypeId = result.getObject(11);
					tourItem.fTourTitle = result.getString(12);
					tourItem.fColumnTimeInterval = result.getShort(13);

					tourItem.colMaxSpeed = result.getFloat(14);
					tourItem.colMaxAltitude = result.getLong(15);
					tourItem.colMaxPulse = result.getLong(16);
					tourItem.colAvgPulse = result.getLong(17);
					tourItem.colAvgCadence = result.getLong(18);
					tourItem.colAvgTemperature = result.getLong(19);

					final int dbHour = result.getInt(21);
					final int dbMinute = result.getInt(22);

					fCalendar.set(dbYear, dbMonth - 1, dbDay, dbHour, dbMinute);
					tourItem.fTourDate = fCalendar.getTimeInMillis();

					tourItem.fTourTypeId = (tourTypeId == null ? //
							TourDatabase.ENTITY_IS_NOT_SAVED
							: (Long) tourTypeId);

					// compute average speed/pace, prevent divide by 0
					tourItem.colAvgSpeed = dbDrivingTime == 0 ? 0 : 3.6f * dbDistance / dbDrivingTime;
					tourItem.colAvgPace = dbDistance == 0 ? 0 : dbDrivingTime * 166.66f / dbDistance;

					tourItem.colPausedTime = tourItem.colRecordingTime - tourItem.colDrivingTime;

					if (resultTagId instanceof Long) {
						tourItem.fTagIds = tagIds = new ArrayList<Long>();
						tagIds.add((Long) resultTagId);
					}
				}

				lastTourId = tourId;
			}

//			TourDatabase.disableRuntimeStatistic(conn); 

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	@Override
	protected void remove() {}

}
