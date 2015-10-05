/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.collateTours;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.joda.time.DateTime;

public class TVICollatedTour_Event extends TVICollatedTour {

	long		tourId;

	DateTime	eventStart;
	DateTime	eventEnd;

	boolean		isFirstEvent;
	boolean		isLastEvent;

	TVICollatedTour_Event(final CollatedToursView view, final TVICollatedTour parentItem) {

		super(view);

		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "" + //									//$NON-NLS-1$
				//
				"SELECT " //												//$NON-NLS-1$
				//
				+ "tourID, " //											1	//$NON-NLS-1$
				+ "tourPerson_personId, " //							2	//$NON-NLS-1$
				+ "tourType_typeId, " //								3	//$NON-NLS-1$
				+ "jTdataTtag.TourTag_tagId, "//						4	//$NON-NLS-1$
				+ "Tmarker.markerId, "//								5	//$NON-NLS-1$

				+ "TourStartTime, " //									6	//$NON-NLS-1$
				+ "tourDistance, " //									7	//$NON-NLS-1$
				+ "tourRecordingTime, " //								8	//$NON-NLS-1$
				+ "tourDrivingTime, " //								9	//$NON-NLS-1$
				+ "tourAltUp, " //										10	//$NON-NLS-1$
				+ "tourAltDown, " //									11	//$NON-NLS-1$
				+ "startDistance, " //									12	//$NON-NLS-1$
				+ "tourTitle, " //										13	//$NON-NLS-1$
				+ "deviceTimeInterval, " //								14	//$NON-NLS-1$
				+ "maxSpeed, " //										15	//$NON-NLS-1$
				+ "maxAltitude, " //									16	//$NON-NLS-1$
				+ "maxPulse, " //										17	//$NON-NLS-1$
				+ "avgPulse, " //										18	//$NON-NLS-1$
				+ "avgCadence, " //										19	//$NON-NLS-1$
				+ "(DOUBLE(avgTemperature) / temperatureScale), " //	20	//$NON-NLS-1$
				+ "startWeek, " //										21	//$NON-NLS-1$
				+ "startWeekYear, " //									22	//$NON-NLS-1$
				//
				+ "weatherWindDir, " //									23	//$NON-NLS-1$
				+ "weatherWindSpd, " //									24	//$NON-NLS-1$
				+ "weatherClouds, " //									25	//$NON-NLS-1$
				//
				+ "restPulse, " //										26	//$NON-NLS-1$
				+ "calories, " //										27	//$NON-NLS-1$
				//
				+ "numberOfTimeSlices, " //								28	//$NON-NLS-1$
				+ "numberOfPhotos, " //									29	//$NON-NLS-1$
				+ "dpTolerance, " //									30	//$NON-NLS-1$
				//
				+ "frontShiftCount, " //								31	//$NON-NLS-1$
				+ "rearShiftCount" //									32	//$NON-NLS-1$
				//
				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData" + UI.NEW_LINE) //			//$NON-NLS-1$ //$NON-NLS-2$

				// get tag id's
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON TourData.tourId = jTdataTtag.TourData_tourId") //$NON-NLS-1$

				// get marker id's
				+ (" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_MARKER + " Tmarker") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON TourData.tourId = Tmarker.TourData_tourId") //$NON-NLS-1$

				+ (" WHERE TourStartTime >= ? AND TourStartTime < ?") //$NON-NLS-1$
				+ sqlFilter.getWhereClause()

				+ " ORDER BY TourStartTime"; //$NON-NLS-1$

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			statement.setLong(1, eventStart.getMillis());
			statement.setLong(2, eventEnd.getMillis());

			sqlFilter.setParameters(statement, 3);

			long prevTourId = -1;
			HashSet<Long> tagIds = null;
			HashSet<Long> markerIds = null;

			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				final long dbTourId = result.getLong(1);

				final Object dbTagId = result.getObject(4);
				final Object dbMarkerId = result.getObject(5);

				if (dbTourId == prevTourId) {

					// additional result set's for the same tour

					// get tags from outer join
					if (dbTagId instanceof Long) {
						tagIds.add((Long) dbTagId);
					}

					// get markers from outer join
					if (dbMarkerId instanceof Long) {
						markerIds.add((Long) dbMarkerId);
					}

				} else {

					// first resultset for a new tour

					final TVICollatedTour_Tour tourItem = new TVICollatedTour_Tour(collateToursView, this);
					children.add(tourItem);

					tourItem.tourId = dbTourId;
					tourItem.colPersonId = result.getLong(2);
					final Object tourTypeId = result.getObject(3);

					final long dbTourStartTime = result.getLong(6);
					tourItem.colTourStartTime = dbTourStartTime;

					final long dbDistance = tourItem.colDistance = result.getLong(7);
					tourItem.colRecordingTime = result.getLong(8);
					final long dbDrivingTime = tourItem.colDrivingTime = result.getLong(9);
					tourItem.colAltitudeUp = result.getLong(10);
					tourItem.colAltitudeDown = result.getLong(11);

					tourItem.colStartDistance = result.getLong(12);
					tourItem.colTourTitle = result.getString(13);
					tourItem.colTimeInterval = result.getShort(14);

					tourItem.colMaxSpeed = result.getFloat(15);
					tourItem.colMaxAltitude = result.getLong(16);
					tourItem.colMaxPulse = result.getLong(17);
					tourItem.colAvgPulse = result.getFloat(18);
					tourItem.colAvgCadence = result.getFloat(19);
					tourItem.colAvgTemperature = result.getFloat(20);

					tourItem.colWeekNo = result.getInt(21);
					tourItem.colWeekYear = result.getInt(22);

					tourItem.colWindDir = result.getInt(23);
					tourItem.colWindSpd = result.getInt(24);
					tourItem.colClouds = result.getString(25);

					tourItem.colRestPulse = result.getInt(26);
					tourItem.colCalories = result.getInt(27);

					tourItem.colNumberOfTimeSlices = result.getInt(28);
					tourItem.colNumberOfPhotos = result.getInt(29);
					tourItem.colDPTolerance = result.getInt(30);

					tourItem.colFrontShiftCount = result.getInt(31);
					tourItem.colRearShiftCount = result.getInt(32);

					// -----------------------------------------------

					calendar.setTimeInMillis(dbTourStartTime);
					tourItem.colWeekDay = calendar.get(Calendar.DAY_OF_WEEK);

					tourItem.tourTypeId = (tourTypeId == null ? //
							TourDatabase.ENTITY_IS_NOT_SAVED
							: (Long) tourTypeId);

					// compute average speed/pace, prevent divide by 0
					tourItem.colAvgSpeed = dbDrivingTime == 0 ? 0 : 3.6f * dbDistance / dbDrivingTime;
					tourItem.colAvgPace = dbDistance == 0 ? 0 : dbDrivingTime * 1000 / dbDistance;

					tourItem.colPausedTime = tourItem.colRecordingTime - tourItem.colDrivingTime;

					// get first tag id
					if (dbTagId instanceof Long) {

						tagIds = new HashSet<Long>();
						tagIds.add((Long) dbTagId);

						tourItem.setTagIds(tagIds);
					}

					// get first marker id
					if (dbMarkerId instanceof Long) {

						markerIds = new HashSet<Long>();
						markerIds.add((Long) dbMarkerId);

						tourItem.setMarkerIds(markerIds);
					}
				}

				prevTourId = dbTourId;
			}

//			TourDatabase.disableRuntimeStatistic(conn);

		} catch (final SQLException e) {
			SQL.showException(e);
		} finally {
			SQL.close(conn);
		}
	}

	@Override
	public Long getTourId() {
		return tourId;
	}

	@Override
	public boolean hasChildren() {

		if (eventEnd == null) {

			// this occures when the collation task is canceled by the user

			return false;
		}

		return colCounter > 0;
	}

	@Override
	public void setTagIds(final HashSet<Long> tagIds) {
		sqlTagIds = tagIds;
	}

	@Override
	public String toString() {
		return "\nTVICollatedTour_Event\n"//
				+ ("eventStart=" + eventStart + ", \n")
				+ ("eventEnd=" + eventEnd + ", \n")
//				+ ("eventStartText=" + eventStartText)
				+ "\n";
	}

}
